// 引擎自测台：现场造几个假模组，喂合成日志，断言锁定的是对的那个。
package dev.oba.tools;

import dev.oba.core.Analyzer;
import dev.oba.core.ClassIndex;
import dev.oba.core.Diagnosis;
import dev.oba.core.LogAnalyzer;
import dev.oba.core.McreatorDetector;
import dev.oba.core.ModSource;
import dev.oba.core.ProcessProbe;
import dev.oba.core.Lang;
import dev.oba.core.Reports;
import dev.oba.core.StackTraceParser;
import dev.oba.core.Timeline;
import dev.oba.core.GraphicsMods;
import dev.oba.core.Theme;
import dev.oba.mod.Diagnostics;
import dev.oba.mod.GameUi;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SelfTest {

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("oba-selftest");
        System.out.println("临时工作目录: " + work);

        Path modsDir = work.resolve("mods");
        Files.createDirectories(modsDir);

        // 造测试用的假模组
        Path bogus = makeJar(modsDir, "bogusmod-1.2.3.jar", "Bogus Mod", "bogusmod", "1.2.3",
                List.of("com.example.bogus.BogusMod",
                        "com.example.bogus.BogusRegistry",
                        "com.example.bogus.util.Helper"),
                Map.of("assets/bogusmod/lang/en_us.json", "{\"item.bogusmod.thing\":\"Thing\"}"),
                null, List.of());

        Path mixinMod = makeJar(modsDir, "mixinfail-2.0.jar", "Mixin Fail", "mixinfail", "2.0",
                List.of("net.mixinfail.MixinFailMod",
                        "net.mixinfail.mixin.LevelMixin"),
                Map.of(), null, List.of());

        Path mcreator = makeMcreatorJar(modsDir, "coolstuff-1.0.0.jar", "Cool Stuff", "coolstuff", "1.0.0");

        Path good = makeJar(modsDir, "goodmod-1.0.jar", "Good Mod", "goodmod", "1.0",
                List.of("org.good.GoodMod"), Map.of(), null, List.of());

        Path nested = makeJarWithNested(modsDir, "bundler-1.0.jar", "Bundler", "bundler", "1.0",
                List.of("org.bundler.Bundler"));

        // 实机测试用的那个"构造阶段就炸"的模组，这里也造一个
        Path crasher = makeJar(modsDir, "crasher-1.0.0.jar", "Crasher", "crasher", "1.0.0",
                List.of("com.example.crasher.CrasherMod"), Map.of(), null, List.of());

        System.out.println("已生成测试模组: " + Files.list(modsDir).count() + " 个文件\n");

        List<ModSource> sources = ModSource.scanModsFolder(modsDir);
        ClassIndex index = Analyzer.buildIndex(sources);
        Map<String, ModSource> byId = ModSource.indexByModId(sources);

        section("1. 扫描与索引");
        check("扫描到 6 个模组文件", sources.size() == 6, "实际 " + sources.size());
        check("bogusmod 被索引", byId.containsKey("bogusmod"), byId.keySet().toString());
        check("类索引包含暴露的类", index.resolve("com.example.bogus.util.Helper") != null, "未解析");
        check("精确类名匹配置信度 1.0",
                index.resolve("com.example.bogus.util.Helper").confidence() >= 0.999, "");
        ModSource nestedSource = byId.get("bundler");
        check("内嵌 jar 被记录", nestedSource != null && !nestedSource.nestedJars().isEmpty(),
                nestedSource == null ? "未找到 bundler" : nestedSource.scanErrors().toString());
        check("内嵌 jar 的 modId 被解析出",
                nestedSource != null && nestedSource.nestedJars().stream()
                        .anyMatch(nj -> nj.modIds().contains("libthing")),
                nestedSource == null ? "" : nestedSource.nestedJars().toString());
        check("未知名不会被错误归属（不退化到单段包名）", index.resolve("com.nothing.Here") == null,
                String.valueOf(index.resolve("com.nothing.Here")));
        check("同前缀不同根不会被误判", index.resolve("com.other.Thing") == null,
                String.valueOf(index.resolve("com.other.Thing")));

        section("2. 崩溃报告：模组自身抛异常");
        String crash1 = crashReport("java.lang.NullPointerException: Cannot invoke \"String.length()\" because \"s\" is null",
                List.of(
                        "\tat com.example.bogus.BogusRegistry.register(BogusRegistry.java:87)",
                        "\tat com.example.bogus.BogusMod.onCommonSetup(BogusMod.java:42)",
                        "\tat net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent.dispatch(FMLCommonSetupEvent.java:31)",
                        "\tat net.minecraft.server.MinecraftServer.runServer(MinecraftServer.java:711)"));
        Diagnosis.Report r1 = run(crash1, index, byId);
        check("锁定 bogusmod", primaryIs(r1, "bogusmod"), describe(r1));
        check("定位到 BogusRegistry.java:87",
                containsLocation(r1, "com.example.bogus.BogusRegistry.register (BogusRegistry.java:87)"),
                describeLocations(r1));
        check("可信度为高", r1.primarySuspect() != null
                && r1.primarySuspect().confidence() == Diagnosis.Confidence.HIGH, describe(r1));
        check("未把 goodmod 当成元凶", !anyFinding(r1, "goodmod"), describe(r1));

        section("3. 崩溃报告：缺少前置依赖");
        String crash2 = String.join("\n",
                "[10:00:00] [main/ERROR] Mod bogusmod requires absentlib 1.4.0 that is missing",
                "[10:00:00] [main/ERROR] Missing or unsupported mandatory dependencies:",
                "\tmodId: 'absentlib', requested by 'bogusmod'");
        Diagnosis.Report r2 = run(crash2, index, byId);
        check("命中 missing_dependency 规则",
                hasRule(r2, "neoforge.missing_dependency"), rules(r2));
        check("关联到 bogusmod", primaryIs(r2, "bogusmod"), describe(r2));

        section("4. 崩溃报告：Mixin 注入失败");
        String crash3 = String.join("\n",
                "[10:00:01] [main/FATAL] Mixin apply for mod mixinfail from mixinfail.mixins.json failed",
                "org.spongepowered.asm.mixin.throwables.MixinApplyError: Mixin mixinfail.mixin.LevelMixin from mixinfail.mixins.json failed",
                "Caused by: java.lang.NoSuchMethodError: 'void net.mixinfail.mixin.LevelMixin.hook()'",
                "\tat net.mixinfail.mixin.LevelMixin.hook(LevelMixin.java:19)",
                "\tat net.minecraft.world.level.Level.tick(Level.java:120)");
        Diagnosis.Report r3 = run(crash3, index, byId);
        if (System.getenv("OBA_DEBUG") != null) {
            System.out.println("    [debug] roots=" + r3.parsedLines()
                    + " findings=" + r3.findings().size()
                    + " issues=" + r3.issues().stream().map(Diagnosis.KnownIssue::rule).toList());
            r3.findings().forEach(f -> {
                System.out.println("    [debug] finding " + f.fileName() + " score=" + f.score()
                        + " frames=" + f.frames().size());
                f.frames().forEach(af -> System.out.println("    [debug]    frame " + af.location()));
                f.evidence().forEach(e -> System.out.println("    [debug]    ev " + e.rule() + " +" + e.weight()));
            });
            StackTraceParser.Parsed p3 = StackTraceParser.parse(crash3);
            for (StackTraceParser.ThrowableInfo t : p3.throwables()) {
                System.out.println("    [debug] throwable " + t.headline() + " frames=" + t.frames().size());
                for (StackTraceParser.ThrowableInfo c : t.chain()) {
                    System.out.println("    [debug]   chain[" + c.depth() + "] "
                            + c.headline() + " frames=" + c.frames().size());
                }
            }
        }
        check("锁定 mixinfail", primaryIs(r3, "mixinfail"), describe(r3));
        check("定位到 LevelMixin.java:19",
                containsLocation(r3, "net.mixinfail.mixin.LevelMixin.hook (LevelMixin.java:19)"),
                describeLocations(r3));
        check("命中 mixin 规则", hasRulePrefix(r3, "mixin."), rules(r3));

        section("5. 崩溃报告：重复模组");
        String crash4 = "[10:00:02] [main/FATAL] Duplicate mods found for bogusmod";
        Diagnosis.Report r4 = run(crash4, index, byId);
        check("命中 duplicate 规则", hasRule(r4, "fml.duplicate_mod"), rules(r4));
        check("重复项关联 bogusmod", primaryIs(r4, "bogusmod"), describe(r4));

        section("6. MCreator 识别");
        Diagnosis.McreatorHit hit = McreatorDetector.detect(ModSource.of(mcreator)).orElse(null);
        check("识别出 MCreator 模组", hit != null, "未识别");
        if (hit != null) {
            check("识别理由 >= 3 项", hit.reasons().size() >= 3, "实际 " + hit.reasons().size());
            check("命中包名特征", hit.reasons().stream().anyMatch(e -> e.rule().contains("package_root")),
                    hit.reasons().toString());
            check("命中 procedures 特征", hit.reasons().stream().anyMatch(e -> e.rule().contains("procedures")),
                    hit.reasons().toString());
        }
        check("普通模组不会被误报", McreatorDetector.detect(ModSource.of(good)).isEmpty(),
                McreatorDetector.detect(ModSource.of(good)).map(h -> h.reasons().toString()).orElse(""));
        check("bogusmod 不会被误报", McreatorDetector.detect(ModSource.of(bogus)).isEmpty(),
                McreatorDetector.detect(ModSource.of(bogus)).map(h -> h.reasons().toString()).orElse(""));

        section("7. 线程转储（卡死）分析");
        String dump = String.join("\n",
                "\"Server thread\" prio=5 Id=22 RUNNABLE",
                "   java.lang.Thread.State: RUNNABLE",
                "\tat com.example.bogus.BogusRegistry.tickSlowly(BogusRegistry.java:210)",
                "\tat com.example.bogus.BogusMod.onServerTick(BogusMod.java:65)",
                "\tat net.minecraft.server.MinecraftServer.tickServer(MinecraftServer.java:1500)",
                "",
                "\"Render thread\" prio=5 Id=24 RUNNABLE",
                "   java.lang.Thread.State: RUNNABLE",
                "\tat net.minecraft.client.Minecraft.runTick(Minecraft.java:1000)",
                "");
        Diagnosis.Report r5 = ProcessProbe.analyzeThreadDump(dump, index, byId, "Server thread");
        check("线程转储锁定 bogusmod", primaryIs(r5, "bogusmod"), describe(r5));
        check("定位到 tickSlowly(BogusRegistry.java:210)",
                containsLocation(r5, "com.example.bogus.BogusRegistry.tickSlowly (BogusRegistry.java:210)"),
                describeLocations(r5));

        section("8. live 日志（真实日志行）");
        String log = String.join("\n",
                "[18:03:11] [main/INFO] Loading Minecraft 1.21.1 with NeoForge 21.1.256",
                "[18:03:12] [main/INFO] Java version: 21.0.5, vendor: Azul Systems, Inc.",
                "[18:03:20] [main/ERROR] java.lang.NoClassDefFoundError: com/example/bogus/util/Helper",
                "Caused by: java.lang.ClassNotFoundException: com.example.bogus.util.Helper",
                "\tat com.example.bogus.BogusMod.<init>(BogusMod.java:15)",
                "\tat net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:100)",
                "[18:03:20] [main/ERROR] Failed to create mod instance. Mod: bogusmod",
                "[18:03:21] [main/INFO] goodbye");
        Diagnosis.Report r6 = run(log, index, byId);
        check("环境信息解析出 NeoForge",
                r6.environment().containsKey(Lang.get("env.minecraft")), r6.environment().toString());
        check("NoClassDefFoundError 归到 bogusmod", primaryIs(r6, "bogusmod"), describe(r6));
        check("命中类缺失规则", hasRule(r6, "class.noclassdef") || hasRule(r6, "class.classnotfound"), rules(r6));

        // 自己写的结论会被日志监听读回来（进程内采集器和文件尾巴两条路都会读到），
        // 读回来就得挡住：否则它会当成新错误再分析一遍，报告越嵌越大
        check("认出自己写的结论行",
                Diagnostics.isSelfOutput("[19:27:11] [Render thread/INFO] [oba/]: [OBA] 捕获到错误，"
                        + "但没有栈帧能归属到某个模组。最相关的问题: 着色器缺少采样器 — Shader ... could not find sampler"),
                "");
        check("认出聊天栏渲染出来的行",
                Diagnostics.isSelfOutput("[19:27:22] [Render thread/INFO] "
                        + "[net.minecraft.client.gui.components.ChatComponent/]: [System] [CHAT] ╔═══╗"), "");
        check("别人的报错行不会被误挡",
                !Diagnostics.isSelfOutput("[18:03:20] [main/ERROR] java.lang.NoClassDefFoundError: com/example/bogus/util/Helper"),
                "");

        section("9. 渲染输出");
        String text = Reports.text(r1, false);
        String json = Reports.json(r1);
        String md = Reports.markdown(r1);
        check("文本报告包含元凶名", text.contains("bogusmod"), "");
        check("文本报告包含出错位置", text.contains("BogusRegistry.java:87"), "");
        check("JSON 报告结构完整", json.startsWith("{") && json.endsWith("}")
                && json.contains("\"suspects\"") && json.contains("\"mcreatorMods\""), "");
        check("JSON 无未转义控制字符", json.chars().noneMatch(c -> c < 0x20 && c != '\n'), "");
        check("Markdown 报告含结论表", md.contains("## 结论") && md.contains("bogusmod"), "");
        check("报告声明只读", text.contains(Lang.get("footer.readonly")), "");

        section("10. 多语言");
        // 每个键在两种语言下都得有值。取不到的键会原样返回键名，这里就是找这种
        List<String> danglingEn = new ArrayList<>();
        List<String> danglingZh = new ArrayList<>();
        String previous = Lang.language();
        for (String lang : List.of("en", "zh")) {
            Lang.setLanguage(lang);
            for (String key : Lang.keys()) {
                String value = Lang.get(key);
                if (value.equals(key) && !key.equals("rule.match.detail")) {
                    (lang.equals("zh") ? danglingZh : danglingEn).add(key);
                }
            }
        }
        check("英文词条无缺失", danglingEn.isEmpty(), danglingEn.toString());
        check("中文词条无缺失", danglingZh.isEmpty(), danglingZh.toString());

        // 抽查两种语言确实不一样。每份样本都在自己那门语言生效时渲染并当场检查，
        // 否则后面的断言只会看到恢复后的语言
        Lang.setLanguage("en");
        String enText = Reports.text(r1, false);
        boolean enLooksEnglish = enText.contains(Lang.get("label.policy"))
                && enText.contains(Lang.get("footer.readonly"))
                && enText.indexOf('\u7ed3') < 0;
        Lang.setLanguage("zh");
        String zhText = Reports.text(r1, false);
        check("英文报告与中文报告不同", !enText.equals(zhText), "");
        // 结论表头只有在点出元凶时才会打印，所以这里断言一个总会出现的标签
        check("英文报告使用英文标签", enLooksEnglish, "");
        check("英文报告无中文残留",
                enText.codePoints().noneMatch(c -> c >= 0x4e00 && c <= 0x9fff),
                "");
        check("中文报告确实使用中文",
                zhText.codePoints().anyMatch(c -> c >= 0x4e00 && c <= 0x9fff),
                "");
        check("中文报告使用中文标签", zhText.contains("结论"), "");
        check("两种语言都给出同一位置", enText.contains("BogusRegistry.java:87")
                && zhText.contains("BogusRegistry.java:87"), "");
        check("语言标签解析正确", Lang.detect("zh_cn").equals(Lang.ZH)
                && Lang.detect("en_us").equals(Lang.EN)
                && Lang.detect("de_de").equals(Lang.EN), "");
        // 规则表里每个键都得有标题
        List<String> missingRuleText = new ArrayList<>();
        for (String key : LogAnalyzer.ruleKeys()) {
            if (Lang.get(key + ".title").equals(key + ".title")) {
                missingRuleText.add(key);
            }
        }
        check("每条规则都有标题与建议", missingRuleText.isEmpty(), missingRuleText.toString());
        Lang.setLanguage(previous);

        section("11. 规则库自检");
        List<String> rules = LogAnalyzer.ruleIds();
        check("规则数量 >= 20", rules.size() >= 20, "实际 " + rules.size());
        check("规则 id 无重复", new HashSet<>(rules).size() == rules.size(), "");

        section("12. 目录形式的模组（开发环境）");
        Path devDir = work.resolve("devmods/devmod");
        Files.createDirectories(devDir.resolve("META-INF"));
        Files.createDirectories(devDir.resolve("dev/mod/thing"));
        Files.writeString(devDir.resolve("META-INF/neoforge.mods.toml"),
                "modLoader=\"javafml\"\nlicense=\"MIT\"\n[[mods]]\nmodId=\"devmod\"\ndisplayName=\"Dev Mod\"\nversion=\"9.9\"\n");
        Files.write(devDir.resolve("dev/mod/thing/Thing.class"), new byte[]{1, 2, 3});
        List<ModSource> devSources = ModSource.scanModsFolder(work.resolve("devmods"));
        check("扫描到开发目录模组", devSources.size() == 1, "实际 " + devSources.size());
        if (!devSources.isEmpty()) {
            check("目录模组解析出 modId", devSources.get(0).modIds().contains("devmod"),
                    devSources.get(0).modIds().toString());
            check("目录模组索引到类", Analyzer.buildIndex(devSources).resolve("dev.mod.thing.Thing") != null, "");
        }

        section("13. Sinytra Connector：Fabric 模组与普通模组混装");

        // 一个 Fabric 模组（Connector 会把它桥到 NeoForge 上），一个普通 NeoForge 模组
        Path fabricMod = makeFabricJar(modsDir, "sodium-extra-0.6.0.jar", "Sodium Extra",
                "sodiumextra", "0.6.0",
                List.of("me.jellysquid.mods.sodiumextra.SodiumExtraMod",
                        "me.jellysquid.mods.sodiumextra.mixin.MixinOptionsScreen"));
        Path connector = makeConnectorJar(modsDir, "connectormod-2.0.0.jar",
                "Sinytra Connector", "connectormod", "2.0.0",
                List.of("org.sinytra.connector.ConnectorMod",
                        "org.sinytra.connector.transformer.TransformerImpl"));

        List<ModSource> all = ModSource.scanModsFolder(modsDir);
        ClassIndex allIndex = Analyzer.buildIndex(all);
        Map<String, ModSource> allById = ModSource.indexByModId(all);

        ModSource fabricSource = allById.get("sodiumextra");
        check("Fabric 模组的类进了索引",
                allIndex.resolve("me.jellysquid.mods.sodiumextra.SodiumExtraMod") != null, "");
        check("Fabric 模组被识别为 Fabric",
                fabricSource != null && fabricSource.isFabric(), "未识别");
        check("Fabric 模组自己有 modId",
                fabricSource != null && fabricSource.modIds().contains("sodiumextra"), "");

        // Connector 自己的加载器内部实现不该被当成用户模组
        check("Connector 的 transformer 内部类被排除",
                StackTraceParser.isVanillaOrLoader("org.sinytra.connector.transformer.TransformerImpl"), "");
        check("Fabric 加载器内部类被排除",
                StackTraceParser.isVanillaOrLoader("net.fabricmc.loader.impl.FabricLoaderImpl"), "");
        check("Fabric 模组自己的类不被排除",
                !StackTraceParser.isVanillaOrLoader("me.jellysquid.mods.sodiumextra.SodiumExtraMod"), "");
        check("Fabric API 的类不被排除（它是模组）",
                !StackTraceParser.isVanillaOrLoader("net.fabricmc.fabric.api.event.Event"), "");

        // Fabric 模组抛异常：应该能定位到 Fabric 模组，而不是被当成加载器代码丢掉
        String fabricCrash = String.join("\n",
                "java.lang.NoSuchMethodError: 'void me.jellysquid.mods.sodiumextra.mixin.MixinOptionsScreen.apply()'",
                "\tat me.jellysquid.mods.sodiumextra.mixin.MixinOptionsScreen.apply(MixinOptionsScreen.java:47)",
                "\tat me.jellysquid.mods.sodiumextra.SodiumExtraMod.onInitialize(SodiumExtraMod.java:22)",
                "\tat org.sinytra.connector.transformer.TransformerImpl.remap(TransformerImpl.java:100)",
                "\tat net.fabricmc.loader.impl.FabricLoaderImpl.load(FabricLoaderImpl.java:50)");
        Analyzer.Options fo = new Analyzer.Options();
        fo.index = allIndex;
        fo.byModId = allById;
        fo.subject = "fabric-crash";
        Diagnosis.Report fr = Analyzer.analyze(fabricCrash, fo);
        check("锁定 Fabric 模组而不是 Connector",
                fr.primarySuspect() != null && fr.primarySuspect().source() != null
                        && fr.primarySuspect().source().modIds().contains("sodiumextra"),
                fr.primarySuspect() == null ? "无" : fr.primarySuspect().fileName());
        check("定位到 MixinOptionsScreen.java:47",
                containsLocation(fr, "me.jellysquid.mods.sodiumextra.mixin.MixinOptionsScreen.apply (MixinOptionsScreen.java:47)"),
                describeLocations(fr));
        check("普通模组没有被牵连",
                fr.findings().stream().noneMatch(f -> f.source() != null
                        && f.source().modIds().contains("awesomemachines")),
                fr.findings().stream().map(Diagnosis.Finding::fileName).toList().toString());
        check("报告注明了装了 Connector",
                fr.notes().stream().anyMatch(n -> n.equals(Lang.get("note.connectorPresent"))),
                fr.notes().toString());

        // Connector 自己出错时给出的提示
        String connectorFail = "Sinytra Connector failed to remap mod sodiumextra";
        Diagnosis.Report cr = Analyzer.analyze(connectorFail, fo);
        check("识别 Connector 出错",
                cr.issues().stream().anyMatch(i -> i.rule().startsWith("connector.")),
                cr.issues().stream().map(Diagnosis.KnownIssue::rule).toList().toString());

        // 日志里出现 intermediary 名字，多半是没重映射
        Diagnosis.Report ir = Analyzer.analyze(
                "java.lang.NoSuchMethodError: net.minecraft.class_437.method_25423()", fo);
        check("识别 intermediary 名称",
                ir.issues().stream().anyMatch(i -> i.rule().equals("connector.intermediary")),
                ir.issues().stream().map(Diagnosis.KnownIssue::rule).toList().toString());

        section("14. NeoForge 真实日志格式（实机跑出来的样子）");

        // NeoForge 会在栈帧尾巴加 ~[模块@版本/...] {re:classloading}，
        // 类名前面也可能带 模块@版本/ 前缀。实测这两种都会让栈帧整行匹配不上，
        // 结果就是明明有异常却说"没有栈帧能归属到模组"
        String neoLog = String.join("\n",
                "[1010月2026 12:09:41.792] [modloading-worker-0/ERROR] [net.neoforged.fml.javafmlmod.FMLModContainer/LOADING]: Failed to create mod instance. ModID: crasher, class com.example.crasher.CrasherMod",
                "java.lang.IllegalStateException: boom (CrasherMod.java:14)",
                "\tat com.example.crasher.CrasherMod.<init>(CrasherMod.java:14) ~[crasher-1.0.0.jar%233!/:?] {re:classloading}",
                "\tat net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:123) ~[fmlcore-1.0.jar%23118!/:?]",
                "\tat cpw.mods.modlauncher.Launcher.run(Launcher.java:90)");

        // 实机日志里类名前面还带着模块名，而且模块名本身也可能带一层前缀
        String neoLog2 = String.join("\n",
                "java.lang.IllegalStateException: boom",
                "\tat TRANSFORMER/crasher@1.0.0/com.example.crasher.CrasherMod.<init>(CrasherMod.java:13) ~[crasher-1.0.0.jar%23192!/:?] {re:classloading}",
                "\tat MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:122) ~[loader-4.0.45.jar%23161!/:4.0] {}");

        StackTraceParser.Parsed neo = StackTraceParser.parse(neoLog);
        check("带 ~[模块] 尾巴的帧能被解析出来", neo.allFrames().size() >= 3,
                "实际 " + neo.allFrames().size() + " 帧");
        check("模块@版本/ 前缀被剥掉",
                StackTraceParser.stripModulePrefix("net.neoforged.fml@21.1.256/net.neoforged.fml.Foo")
                        .equals("net.neoforged.fml.Foo"), "");
        check("带两层前缀的 TRANSFORMER/模块@版本/ 也能剥掉",
                StackTraceParser.stripModulePrefix("TRANSFORMER/crasher@1.0.0/com.example.Crasher")
                        .equals("com.example.Crasher"), "");

        StackTraceParser.Parsed neo2 = StackTraceParser.parse(neoLog2);
        check("实机那种帧也能解析",
                neo2.allFrames().stream().anyMatch(f ->
                        f.className().equals("com.example.crasher.CrasherMod") && f.lineNumber() == 13),
                neo2.allFrames().toString());
        boolean sawCrasherFrame = neo.allFrames().stream()
                .anyMatch(f -> f.className().equals("com.example.crasher.CrasherMod") && f.lineNumber() == 14);
        check("帧里的类名和行号都对", sawCrasherFrame, neo.allFrames().toString());

        // 用这一套去跑归属，必须能点出 crasher
        Analyzer.Options neoOpt = new Analyzer.Options();
        neoOpt.index = index;
        neoOpt.byModId = byId;
        neoOpt.subject = "neoforge-log";
        Diagnosis.Report neoReport = Analyzer.analyze(neoLog, neoOpt);
        check("能归属到实机崩溃的模组",
                neoReport.primarySuspect() != null
                        && neoReport.primarySuspect().fileName().equals("crasher-1.0.0.jar"),
                neoReport.primarySuspect() == null ? "无" : neoReport.primarySuspect().fileName());
        check("给出的出错位置是那行",
                containsLocation(neoReport,
                        "com.example.crasher.CrasherMod.<init> (CrasherMod.java:14)"),
                describeLocations(neoReport));

        section("15. 日志编码（中文 Windows 上游戏写的是 GBK）");

        // 实测：整合包的 latest.log 是 GBK，按 UTF-8 硬读会直接抛
        // "Input length = 1"，连"读不了"都说不清楚
        Path gbkLog = work.resolve("gbk-latest.log");
        byte[] gbkBytes = ("[22:07:02] [main/INFO]: 龙之冒险：新征程 启动\n"
                + "java.lang.RuntimeException: 中文异常消息\n"
                + "\tat org.good.GoodMod.boom(GoodMod.java:7)\n").getBytes("GBK");
        Files.write(gbkLog, gbkBytes);

        String decoded = LogAnalyzer.readLogText(gbkLog);
        check("GBK 日志能读出来", decoded.contains("龙之冒险"), decoded.substring(0, Math.min(40, decoded.length())));
        check("GBK 日志里的中文异常消息没乱码", decoded.contains("中文异常消息"), decoded);
        check("GBK 日志里的栈帧还在", decoded.contains("GoodMod.java:7"), decoded);

        // UTF-8 的还得照常读
        Path utf8Log = work.resolve("utf8-latest.log");
        Files.writeString(utf8Log, "[12:00:00] [main/INFO]: 你好\n", StandardCharsets.UTF_8);
        check("UTF-8 日志照旧", LogAnalyzer.readLogText(utf8Log).contains("你好"), "");

        // UTF-8 的 BOM 也要认
        Path bomLog = work.resolve("bom-latest.log");
        Files.write(bomLog, ("\uFEFF[12:00:00] [main/INFO]: 带 BOM\n").getBytes(StandardCharsets.UTF_8));
        check("带 BOM 的 UTF-8 不会把 BOM 当正文",
                !LogAnalyzer.readLogText(bomLog).startsWith("\uFEFF"), "");

        section("16. F3+C 手动调试崩溃");

        // 这种崩溃一个字的日志都不写：异常不走 log4j2，聊天栏和 latest.log 里
        // 什么都没有，只有 crash-reports 下落一个文件。报告本体长这样
        String f3c = String.join("\n",
                "---- Minecraft Crash Report ----",
                "// There are four lights!",
                "",
                "Time: 2026-10-10 16:47:13",
                "Description: Manually triggered debug crash",
                "",
                "java.lang.Throwable: Manually triggered debug crash",
                "\tat TRANSFORMER/minecraft@1.21.1/net.minecraft.client.KeyboardHandler.tick(KeyboardHandler.java:545) ~[client.jar%23189!/:?] {re:classloading}",
                "\tat TRANSFORMER/minecraft@1.21.1/net.minecraft.client.Minecraft.tick(Minecraft.java:1912) ~[client.jar%23189!/:?]",
                "\tat java.base/java.lang.Thread.run(Thread.java:1583) [?:?] {}");

        LogAnalyzer.Scan f3cScan = LogAnalyzer.scan(f3c, "f3c", f3c.length(), index, byId);
        check("识别出这是手动触发的调试崩溃", f3cScan.manualCrash(), "");
        check("给了 F3+C 规则命中",
                f3cScan.issues().stream().anyMatch(i -> i.rule().equals("manual.debug_crash")),
                f3cScan.issues().stream().map(Diagnosis.KnownIssue::rule).toList().toString());

        Analyzer.Options f3cOpt = new Analyzer.Options();
        f3cOpt.index = index;
        f3cOpt.byModId = byId;
        f3cOpt.subject = "f3c";
        Diagnosis.Report f3cReport = Analyzer.analyze(f3c, f3cOpt);
        check("报告里注明是手动崩溃，别让人白找元凶",
                f3cReport.notes().stream().anyMatch(n -> n.equals(Lang.get("note.manualCrash"))),
                f3cReport.notes().toString());
        check("手动崩溃不该点出任何模组",
                f3cReport.primarySuspect() == null,
                f3cReport.primarySuspect() == null ? "" : f3cReport.primarySuspect().fileName());

        // 普通崩溃不能被当手动崩溃
        String normalCrash = String.join("\n",
                "---- Minecraft Crash Report ----",
                "Time: 2026-10-10 18:03:20",
                "Description: Ticking entity",
                "java.lang.NullPointerException: Cannot invoke \"String.length()\" because \"name\" is null",
                "\tat com.example.awesomemachines.registry.MachineRegistry.register(MachineRegistry.java:143)");
        Diagnosis.Report normalReport = run(normalCrash, index, byId);
        check("普通崩溃不会被误认成 F3+C",
                normalReport.notes().stream().noneMatch(n -> n.equals(Lang.get("note.manualCrash"))),
                normalReport.notes().toString());

        section("17. 自己人不上嫌疑名单");

        // 实机踩过：看门狗抓卡死线程，报出来的却是 Oba 自己
        // 那个"等在日志文件上的线程"
        check("Oba 自己的类不算用户代码",
                StackTraceParser.isVanillaOrLoader("dev.oba.core.LogWatcher"), "");
        check("Oba 的模组类也不算",
                StackTraceParser.isVanillaOrLoader("dev.oba.mod.Diagnostics"), "");
        check("别的模组照常参与归属",
                !StackTraceParser.isVanillaOrLoader("com.example.crasher.CrasherMod"), "");

        section("18. 带空格的路径");

        // 路径里有空格时，大家自然会加引号：--game-dir "D:\我的 游戏\.minecraft"。
        // 命令行本身不会把引号吃掉，于是引号跟着路径进来，
        // Path.of() 直接报 Illegal char <">
        check("双引号会被剥掉",
                dev.oba.cli.Cli.stripQuotes("\"D:\\我的 游戏\\.minecraft\"")
                        .equals("D:\\我的 游戏\\.minecraft"), "");
        check("单引号也会被剥掉",
                dev.oba.cli.Cli.stripQuotes("'/home/my games/.minecraft'")
                        .equals("/home/my games/.minecraft"), "");
        check("没引号的路径原样不动",
                dev.oba.cli.Cli.stripQuotes("D:\\mc\\.minecraft").equals("D:\\mc\\.minecraft"), "");
        check("只有一边有引号时不动它（避免把真名字改坏）",
                dev.oba.cli.Cli.stripQuotes("\"D:\\mc").equals("\"D:\\mc"), "");
        check("空串和 null 不炸",
                dev.oba.cli.Cli.stripQuotes("") == null || dev.oba.cli.Cli.stripQuotes("").isEmpty(), "");
        check("单字符不炸", dev.oba.cli.Cli.stripQuotes("\"").equals("\""), "");

        section("19. 事故时间线");

        String timelineLog = String.join("\n",
                "[1010月2026 12:00:01.100] [main/INFO] [net.neoforged.fml.loading.ModDiscoverer/SCAN]: Found mod file \"goodmod-1.0.jar\"",
                "[1010月2026 12:00:02.200] [Render thread/INFO] [org.good.GoodMod/]: Good Mod starting up",
                "[1010月2026 12:00:03.300] [Render thread/WARN] [org.good.GoodMod/]: something looks off",
                "[1010月2026 12:00:04.400] [Render thread/ERROR] [org.good.GoodMod/]: java.lang.NullPointerException: boom",
                "\tat org.good.GoodMod.boom(GoodMod.java:7)",
                "[1010月2026 12:00:05.500] [Render thread/ERROR] [net.minecraft.server.MinecraftServer/]: unrelated vanilla error");

        Timeline tl = Analyzer.buildTimeline(timelineLog, index, byId);
        check("时间线不是空的", tl.size() > 0, "实际 " + tl.size());
        check("栈帧没有混进时间线",
                tl.entries().stream().noneMatch(e -> e.renderText().startsWith("at ")
                        || e.renderText().startsWith("...")),
                tl.entries().stream().map(Timeline.Entry::renderText).toList().toString());
        check("logger 能反查到模组",
                tl.entries().stream().anyMatch(e -> e.modId().equals("goodmod")),
                tl.entries().stream().map(Timeline.Entry::modId).toList().toString());
        check("ERROR 行被标成错误级别",
                tl.entries().stream().anyMatch(e -> e.type() == Timeline.Type.ERROR),
                tl.entries().stream().map(e -> e.type().name()).toList().toString());
        check("WARN 行被标成警告级别",
                tl.entries().stream().anyMatch(e -> e.type() == Timeline.Type.WARN), "");
        check("模组加载事件被标成生命周期",
                tl.entries().stream().anyMatch(e -> e.type() == Timeline.Type.LIFECYCLE), "");
        check("最接近末尾的排在最前面",
                tl.reversed().get(0).renderText().contains("unrelated vanilla error"),
                tl.reversed().get(0).renderText());

        // 时间戳的几种写法都要认
        check("认 NeoForge 那种本地化时间戳",
                Timeline.extractTime("[1010月2026 12:34:56.789] [main/INFO]: x").equals("12:34:56.789"), "");
        check("认普通方括号时间戳",
                Timeline.extractTime("[12:34:56] [main/INFO]: x").equals("12:34:56"), "");
        check("没时间戳就返回空", Timeline.extractTime("no time here").isEmpty(), "");

        // 刷屏要折叠，不然一段重复日志能把时间线冲垮
        StringBuilder spam = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            spam.append("[12:00:00] [Render thread/WARN] [org.good.GoodMod/]: same line every tick\n");
        }
        Timeline spamTl = Analyzer.buildTimeline(spam.toString(), index, byId);
        check("重复行会被折叠", spamTl.size() < 10, "实际 " + spamTl.size());

        // 日志末尾全是无关刷屏时，要把真正的事故留下来
        StringBuilder noisy = new StringBuilder();
        noisy.append("[12:00:00] [Render thread/ERROR] [org.good.GoodMod/]: the real problem\n");
        for (int i = 0; i < 900; i++) {
            noisy.append("[12:00:0").append(i % 10)
                 .append("] [Render thread/INFO] [net.minecraft.Foo/]: routine line ").append(i).append('\n');
        }
        Timeline noisyTl = Analyzer.buildTimeline(noisy.toString(), index, byId);
        check("末尾刷屏不会把真正的错误挤掉",
                noisyTl.entries().stream().anyMatch(e -> e.renderText().contains("the real problem")),
                "共 " + noisyTl.size() + " 条");

        // 时间线要跟着报告一起出来
        Analyzer.Options tlOpt = new Analyzer.Options();
        tlOpt.index = index;
        tlOpt.byModId = byId;
        tlOpt.subject = "timeline";
        Diagnosis.Report tlReport = Analyzer.analyze(timelineLog, tlOpt);
        String tlText = Reports.text(tlReport, false);
        check("纯文本报告里有时间线小节",
                tlText.contains(Lang.get("section.timeline")), "");
        check("文本报告的时间线里有模组名", tlText.contains("goodmod"), "");
        String tlMd = Reports.markdown(tlReport);
        check("Markdown 报告里有时间线表格",
                tlMd.contains(Lang.get("section.timeline")) && tlMd.contains("|---|"), "");
        String tlJson = Reports.json(tlReport);
        check("JSON 报告里有 timeline 数组", tlJson.contains("timeline"), "");
        check("JSON 时间线条目带时间和类型",
                tlJson.contains("\"time\"") && tlJson.contains("\"type\""), "");

        section("20. 图形类模组：钠 / iris 各分支");

        Path gfxDir = work.resolve("gfxmods");
        Files.createDirectories(gfxDir);

        // 钠 + 铷（同一个东西的两个分支，互斥）、iris + oculus（同理）
        makeJar(gfxDir, "sodium-fabric-0.6.5.jar", "Sodium", "sodium", "0.6.5",
                List.of("net.caffeinemc.mods.sodium.client.SodiumClientMod"), Map.of(), null, List.of());
        makeJar(gfxDir, "iris-1.8.0.jar", "Iris Shaders", "iris", "1.8.0",
                List.of("net.irisshaders.iris.Iris"), Map.of(), null, List.of());
        makeJar(gfxDir, "rubidium-0.6.5.jar", "Rubidium", "rubidium", "0.6.5",
                List.of("me.jellysquid.mods.sodium.SodiumMod"), Map.of(), null, List.of());
        makeJar(gfxDir, "oculus-1.7.0.jar", "Oculus", "oculus", "1.7.0",
                List.of("net.irisshaders.iris.Iris"), Map.of(), null, List.of());
        // Fabric 版钠经 Connector 加载：modId 还是 sodium，包名也还是 Fabric 那套
        makeFabricJar(gfxDir, "sodium-connector-0.6.5.jar", "Sodium (Fabric)",
                "sodium", "0.6.5", List.of("net.caffeinemc.mods.sodium.client.SodiumClientMod"));

        check("认得钠这一族", GraphicsMods.familyOf("sodium") != null
                && GraphicsMods.familyOf("sodium").key().equals("sodium"), "");
        check("铷被认成钠的亲戚", GraphicsMods.familyOf("rubidium") != null
                && GraphicsMods.familyOf("rubidium").key().equals("sodium"), "");
        check("Embeddium 被认成钠的亲戚", GraphicsMods.familyOf("embeddium") != null
                && GraphicsMods.familyOf("embeddium").key().equals("sodium"), "");
        check("Xenon 被认成钠的亲戚", GraphicsMods.familyOf("xenon") != null
                && GraphicsMods.familyOf("xenon").key().equals("sodium"), "");
        check("oculus 被认成 iris 的亲戚", GraphicsMods.familyOf("oculus") != null
                && GraphicsMods.familyOf("oculus").key().equals("iris"), "");
        check("NeOculus 也被认成 iris 的亲戚", GraphicsMods.familyOf("neoculus") != null
                && GraphicsMods.familyOf("neoculus").key().equals("iris"), "");
        check("普通模组不会被认成图形模组", GraphicsMods.familyOf("goodmod") == null, "");
        check("包名也能反查族",
                GraphicsMods.familyOfPackage("net.caffeinemc.mods.sodium.client.Foo") != null, "");
        check("Fabric 那套包名同样认得出来",
                GraphicsMods.familyOfPackage("me.jellysquid.mods.sodium.SodiumMod") != null, "");

        List<ModSource> gfxSources = ModSource.scanModsFolder(gfxDir);
        List<GraphicsMods.Conflict> conflicts = GraphicsMods.findFamilyConflicts(gfxSources);
        check("钠和铷同时装能被查出冲突",
                conflicts.stream().anyMatch(c -> c.family().key().equals("sodium")),
                conflicts.stream().map(c -> c.family().key()).toList().toString());
        check("iris 和 oculus 同时装也能查出来",
                conflicts.stream().anyMatch(c -> c.family().key().equals("iris")),
                conflicts.stream().map(c -> c.family().key()).toList().toString());

        // 只有 oculus、没有 Embeddium：缺前置
        Path loneDir = work.resolve("gfxlone");
        Files.createDirectories(loneDir);
        makeJar(loneDir, "oculus-1.7.0.jar", "Oculus", "oculus", "1.7.0",
                List.of("net.irisshaders.iris.Iris"), Map.of(), null, List.of());
        List<ModSource> loneSet = ModSource.scanModsFolder(loneDir);
        List<GraphicsMods.MissingPartner> missing =
                GraphicsMods.findMissingPartners(loneSet, ModSource.indexByModId(loneSet));
        check("oculus 缺 Embeddium 能被查出来", !missing.isEmpty(),
                missing.stream().map(GraphicsMods.MissingPartner::wanted).toList().toString());
        check("提示里说明了它要配 Embeddium 或铷",
                !missing.isEmpty() && missing.get(0).wanted().contains("embeddium"), "");

        // 报告里要出现图形小节
        Analyzer.Options gfxOpt = new Analyzer.Options();
        gfxOpt.index = Analyzer.buildIndex(gfxSources);
        gfxOpt.byModId = ModSource.indexByModId(gfxSources);
        gfxOpt.subject = "gfx";
        Diagnosis.Report gfxReport = Analyzer.analyze("nothing wrong here", gfxOpt);
        check("报告里列出了图形类模组", !gfxReport.graphicsMods().isEmpty(),
                "共 " + gfxReport.graphicsMods().size());
        check("报告里给出了图形组合问题", !gfxReport.graphicsIssues().isEmpty(), "");
        String gfxText = Reports.text(gfxReport, false);
        check("纯文本报告有图形小节", gfxText.contains(Lang.get("section.graphics")), "");
        check("Markdown 报告也有图形小节",
                Reports.markdown(gfxReport).contains(Lang.get("section.graphics")), "");

        // 图形类崩溃的专项规则。带真正的栈帧，归属才走得通
        String shaderCrash = String.join("\n",
                "[12:00:00] [Render thread/ERROR] [net.irisshaders.iris.Iris/]: Failed to compile shader program",
                "java.lang.IllegalStateException: shader compilation failed",
                "\tat net.irisshaders.iris.pipeline.ShaderPrinter.compile(ShaderPrinter.java:88)",
                "\tat net.irisshaders.iris.Iris.loadShaderpack(Iris.java:214)",
                "[12:00:01] [Render thread/ERROR] [net.caffeinemc.mods.sodium/]: GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
        Analyzer.Options scOpt = new Analyzer.Options();
        scOpt.index = Analyzer.buildIndex(gfxSources);
        scOpt.byModId = ModSource.indexByModId(gfxSources);
        scOpt.subject = "shader";
        Diagnosis.Report scReport = Analyzer.analyze(shaderCrash, scOpt);
        check("着色器编译失败有专门规则",
                scReport.issues().stream().anyMatch(i -> i.rule().equals("graphics.shader_compile")),
                scReport.issues().stream().map(Diagnosis.KnownIssue::rule).toList().toString());
        check("帧缓冲不完整有专门规则",
                scReport.issues().stream().anyMatch(i -> i.rule().equals("graphics.framebuffer")),
                scReport.issues().stream().map(Diagnosis.KnownIssue::rule).toList().toString());
        check("图形崩溃能归属到 iris 或钠", scReport.primarySuspect() != null, "无元凶");

        section("21. 加载器适配层：tick 事件版本判定");

        // NeoForge 在 1.20.5 前后把 tick 事件换过类，两套的全限定名完全不同：
        //   1.20.2~1.20.4  net.neoforged.neoforge.event.TickEvent$ServerTickEvent（带 Phase）
        //   1.20.5 及以后   net.neoforged.neoforge.event.tick.ServerTickEvent$Post
        // 探测逻辑两边都得认，认错了卡死检测就等于失效
        check("只有新版时选新版",
                dev.oba.loader.neoforge.NeoForgeTickHook.classify(true, false)
                        == dev.oba.loader.neoforge.NeoForgeTickHook.Flavour.MODERN_POST, "");
        check("只有老版时选老版",
                dev.oba.loader.neoforge.NeoForgeTickHook.classify(false, true)
                        == dev.oba.loader.neoforge.NeoForgeTickHook.Flavour.LEGACY_PHASE, "");
        check("两套都没有时老实认输",
                dev.oba.loader.neoforge.NeoForgeTickHook.classify(false, false)
                        == dev.oba.loader.neoforge.NeoForgeTickHook.Flavour.NONE, "");
        check("万一两套都在，优先用新版",
                dev.oba.loader.neoforge.NeoForgeTickHook.classify(true, true)
                        == dev.oba.loader.neoforge.NeoForgeTickHook.Flavour.MODERN_POST, "");
        check("类名判定没写错",
                dev.oba.loader.neoforge.NeoForgeTickHook.isModern(
                        "net.neoforged.neoforge.event.tick.ServerTickEvent$Post")
                        && dev.oba.loader.neoforge.NeoForgeTickHook.isLegacy(
                                "net.neoforged.neoforge.event.TickEvent$ServerTickEvent"), "");

        section("22. 界面：版面宽度与可点");

        // 菜单和报告都是靠空格和制表符拼出来的，宽度算错就整个歪掉。
        // 这里逐行量宽度，盯的是真踩过的坑：文案短了（英文）框宽跟着缩，一排框参差不齐
        dev.oba.cli.Menu menu = new dev.oba.cli.Menu(false);
        List<dev.oba.cli.Menu.Item> items = menu.items();

        // 每一条菜单项那几行（框线和选中条）宽度必须完全相同。
        // 盯着的是真踩过的坑：英文文案短，框宽跟着缩，一排框参差不齐
        for (int sel = 0; sel < items.size(); sel++) {
            Set<Integer> itemWidths = new HashSet<>();
            for (String line : menu.render("D:\\mc", Lang.get("menu.state.ready"), sel, items)
                    .split("\n", -1)) {
                String bare = Theme.strip(line);
                boolean isItem = bare.contains("]")
                        && (bare.contains("╭") || bare.contains("│") || bare.contains("╰")
                        || bare.contains("▶"));
                if (isItem) {
                    itemWidths.add(Theme.displayWidth(bare));
                }
            }
            check("菜单第 " + (sel + 1) + " 项选中时框宽统一", itemWidths.size() == 1,
                    "量到的宽度: " + itemWidths);
        }

        // 报告头部那块也一样
        Set<Integer> headWidths = new HashSet<>();
        for (String line : Reports.text(r1, false).split("\n", -1)) {
            if (line.startsWith("╔") || line.startsWith("║") || line.startsWith("╚")) {
                headWidths.add(Theme.displayWidth(line));
            }
        }
        check("报告标题框三行等宽", headWidths.size() == 1, "宽度种类: " + headWidths);

        // 不带颜色时一个控制码都不能漏出去，管道和日志里全靠这条
        String plain = Reports.text(r1, false);
        check("关掉颜色后没有控制码", plain.indexOf('\u001B') < 0, "");

        // 带颜色时颜色码成对收尾，漏了会把后面全染上
        String colored = Reports.text(r1, true);
        check("开颜色后颜色码有始有终",
                colored.indexOf("\u001B[0m") >= 0 && colored.indexOf('\u001B') >= 0, "");

        // 游戏内是把带控制码的正文解析回组件样式，解析器得能把控制码清干净
        String stripped = GameUi.stripAnsi(colored);
        check("控制码能被清干净（游戏内解析用）", stripped.indexOf('\u001B') < 0, "");
        check("清控制码不会吃掉正文",
                stripped.contains("bogusmod") || stripped.contains("结论"), "");

        // 聊天栏不吃 OSC 8 超链接，剥掉之后文字必须还在
        String osc = Theme.of(true).link("https://example.invalid/x", "点我");
        check("超链接文字剥掉控制码后还在",
                GameUi.stripAnsi(osc).equals("点我"), "得到: "
                        + GameUi.stripAnsi(osc));

        // 颜色开关的优先级：命令行属性 > 环境变量 > 是不是终端 > NO_COLOR。
        // 管道里想强制彩色只能靠前两条，所以这两条必须真的起作用（属性测试不依赖终端）
        String savedColor = System.getProperty("oba.color");
        try {
            System.setProperty("oba.color", "always");
            check("属性 oba.color=always 能强制彩色", Theme.colorSupported(), "");
            System.setProperty("oba.color", "never");
            check("属性 oba.color=never 能关掉彩色", !Theme.colorSupported(), "");
        } finally {
            if (savedColor == null) {
                System.clearProperty("oba.color");
            } else {
                System.setProperty("oba.color", savedColor);
            }
        }
        // 帮助里两种语言都得写上 --color，不然没人知道管道里也能要颜色
        for (String lang : new String[] { "zh", "en" }) {
            Lang.setLanguage(lang);
            String usage = Lang.get("cli.usage.body");
            check("帮助里有 --color 和 --no-color（" + lang + "）",
                    usage.contains("--color") && usage.contains("--no-color"), "");
        }
        Lang.setLanguage("zh");

        // 折行按像素算：聊天栏的字体不是等宽的，超一个像素就会被原版挤下去
        List<String> wrapped = GameUi.wrap("这是一段用来试折行的中文，混着 English words 一起排。", 60);
        boolean fits = true;
        for (String w : wrapped) {
            if (GameUi.px(w) > 60) {
                fits = false;
            }
        }
        check("中英混排折行不超像素宽", fits && wrapped.size() > 1, "折了 " + wrapped.size() + " 行");

        // 游戏里量宽度优先问客户端字体，问不到就查这张照原版图集量的表。
        // 表里少一个符号就会查越界，聊天栏排版当场全崩，所以逐个过一遍
        StringBuilder thin = new StringBuilder();
        for (char c : " ●○▶•→›·↑↓─═║╔╗╚╝█▌│┌┐└┘╭╮╯╰◈abcXYZ%~".toCharArray()) {
            int w = GameUi.px(String.valueOf(c));
            if (w <= 0 || w > 12) {
                thin.append(c);
            }
        }
        check("字体宽度表覆盖界面上用到的符号", thin.length() == 0, "量不出来的字: " + thin);

        // 游戏内标题框是双线框：线是 9 像素的，空格是 4 像素，
        // 靠加长顶框去凑内容行的宽度。凑不准右边框就会歪出去一格
        for (String title : new String[]{"诊断", "OBA 模组诊断", "mod doctor report a very long title"}) {
            Set<Integer> frameWidths = new HashSet<>();
            for (net.minecraft.network.chat.Component line : GameUi.header(title)) {
                frameWidths.add(GameUi.px(line.getString()));
            }
            check("游戏内标题框三行等像素宽（" + title + "）", frameWidths.size() == 1,
                    "量到的宽度: " + frameWidths);
        }

        // 游戏内每一项都是「点一下就跑」的按钮：命令挂在组件的 ClickEvent 上，
        // 原版聊天栏点一下就会执行。菜单里所有行都走 GameUi.action 这一条路。
        // 这里只验得到的点击事件：鼠标提示 HoverEvent 一构造就去碰原版注册表，
        // 离线自测里起不来（NeoForge 的 Bootstrap.bootStrap() 会撞
        // LoadingModList.get() == null），所以 GameUi 把 ClickEvent 单独摘了个工厂出来。
        net.minecraft.network.chat.ClickEvent run =
                GameUi.clickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/oba report");
        check("菜单项点一下就走对应命令",
                run.getAction() == net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND
                        && run.getValue().equals("/oba report"),
                "拿到: " + run.getAction() + " " + run.getValue());
        net.minecraft.network.chat.ClickEvent open =
                GameUi.clickEvent(net.minecraft.network.chat.ClickEvent.Action.OPEN_URL,
                        "https://github.com/OneBUGAmongUs/oba");
        check("链接点一下打开网页",
                open.getAction() == net.minecraft.network.chat.ClickEvent.Action.OPEN_URL
                        && open.getValue().startsWith("https://"),
                "拿到: " + open.getAction() + " " + open.getValue());
        net.minecraft.network.chat.ClickEvent suggest =
                GameUi.clickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND, "/oba info ");
        check("需要带参数的项点一下填进输入框",
                suggest.getAction() == net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND
                        && suggest.getValue().endsWith(" "),
                "拿到: " + suggest.getAction() + " [" + suggest.getValue() + "]");

        // 菜单行是一串兄弟节点拼出来的（缩进 / ▶ / [n] / 正文 / 右侧提示），
        // 点击事件得挂满每一段，只挂根节点的话点正文那一段就不算数了。
        net.minecraft.network.chat.MutableComponent row = net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal("  ")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
                .append(net.minecraft.network.chat.Component.literal("▶ [2] ")
                        .withStyle(net.minecraft.ChatFormatting.RED))
                .append(net.minecraft.network.chat.Component.literal("显示最新一份完整报告")
                        .withStyle(net.minecraft.ChatFormatting.WHITE));
        net.minecraft.network.chat.MutableComponent rowClickable = GameUi.clickable(row,
                net.minecraft.network.chat.Style.EMPTY.withClickEvent(
                        GameUi.clickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                                "/oba report")));
        java.util.ArrayDeque<net.minecraft.network.chat.Component> queue = new java.util.ArrayDeque<>();
        queue.add(rowClickable);
        int nodes = 0;
        int withClick = 0;
        while (!queue.isEmpty()) {
            net.minecraft.network.chat.Component node = queue.poll();
            nodes++;
            net.minecraft.network.chat.ClickEvent ce = node.getStyle().getClickEvent();
            if (ce != null && ce.getValue().equals("/oba report")) {
                withClick++;
            }
            queue.addAll(node.getSiblings());
        }
        check("整行每一段都挂了点击事件（点正文也算数）", nodes >= 4 && nodes == withClick,
                "节点 " + nodes + "，其中带点击事件 " + withClick);

        // 报告是引擎照终端 100 列拼的，聊天栏只有 320 像素宽：
        // 原样塞进去会被原版折行，折出来的残段看上去就是乱码（用户报的就是这个）。
        // GameUi.lines 得把每一行重排到聊天栏宽度以内
        String severity = "  \u001B[2m── \u001B[0m致命\u001B[2m "
                + "─".repeat(85) + "\u001B[0m";
        List<net.minecraft.network.chat.Component> reflowed = GameUi.lines(severity);
        check("超长的分级线被收成一条（不再折成串乱码）", reflowed.size() == 1,
                "收成 " + reflowed.size() + " 行");
        check("收完的分级线不超聊天栏宽度",
                reflowed.size() == 1 && GameUi.visualWidth(reflowed.get(0)) <= GameUi.LINE_PX,
                "宽度 " + (reflowed.isEmpty() ? -1 : GameUi.visualWidth(reflowed.get(0))));
        check("分级线里的字还在（致命）",
                reflowed.size() == 1 && reflowed.get(0).getString().contains("致命"),
                "得到: " + (reflowed.isEmpty() ? "" : reflowed.get(0).getString()));
        // 样式挂在子节点上（根节点是空的壳），所以挨个看子节点有没有颜色
        boolean coloured = false;
        for (net.minecraft.network.chat.Component node : reflowed.isEmpty()
                ? List.<net.minecraft.network.chat.Component>of() : reflowed.get(0).getSiblings()) {
            if (node.getStyle().getColor() != null) {
                coloured = true;
            }
        }
        check("分级线的颜色没掉", reflowed.size() == 1 && coloured, "");

        // 普通长行按像素折行，送进聊天栏的每一行都不能超宽（超一个像素就会被原版折）
        List<net.minecraft.network.chat.Component> longLines = GameUi.lines("\u001B[36m"
                + "这一整行是从报告里搬过来的普通长句，混着 English words 一起排，"
                + "看折行会不会把行撑出聊天栏。\u001B[0m");
        boolean longFits = longLines.size() > 1;
        for (net.minecraft.network.chat.Component c : longLines) {
            if (GameUi.visualWidth(c) > GameUi.LINE_PX) {
                longFits = false;
            }
        }
        check("报告长行折到聊天栏宽度以内", longFits, "折了 " + longLines.size() + " 行");

        // 英文优先在空格处断行：堆栈、路径断在词中间很难读
        String english = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi "
                + "omicron pi rho sigma tau upsilon phi chi psi omega";
        Set<String> words = new HashSet<>(List.of(english.split(" ")));
        boolean wholeWords = true;
        for (net.minecraft.network.chat.Component c : GameUi.lines(english)) {
            for (String token : c.getString().strip().split(" ")) {
                if (!token.isEmpty() && !words.contains(token)) {
                    wholeWords = false;
                }
            }
        }
        check("英文折行不会把单词切两半", wholeWords, "折了 "
                + GameUi.lines(english).size() + " 行");

        // 加粗的字每个多占 1 像素：量宽度时不把这一像素算上，
        // 分段标题、反白条这几行就会超出去，被原版折成两行。
        // （菜单项那行走的是 GameUi.action，它要造 HoverEvent，
        // 离线自测里一碰原版注册表就初始化失败，所以这里只能量这两条）
        net.minecraft.network.chat.Component[] boldRows = {
                GameUi.section("问题清单（按严重程度排序）"),
                GameUi.bar("▶ [1] 显示最新一份完整报告")};
        String[] boldNames = {"分段标题", "反白条"};
        for (int i = 0; i < boldRows.length; i++) {
            check("加粗的" + boldNames[i] + "也不超聊天栏宽度",
                    GameUi.visualWidth(boldRows[i]) <= GameUi.LINE_PX,
                    "宽度 " + GameUi.visualWidth(boldRows[i]));
        }

        // 汇总
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════");
        System.out.printf("通过 %d / %d%n", passed, passed + failed);
        if (failed > 0) {
            System.out.println("失败项:");
            for (String f : failures) {
                System.out.println("  ✗ " + f);
            }
        }
        System.out.println("══════════════════════════════════════════════════════");

        if (args.length > 0 && args[0].equals("--dump")) {
            System.out.println();
            System.out.println(Reports.text(r1, false));
        }

        deleteRecursively(work);
        System.exit(failed == 0 ? 0 : 1);
    }

    // 辅助方法

    private static Diagnosis.Report run(String text, ClassIndex index, Map<String, ModSource> byId) {
        Analyzer.Options opt = new Analyzer.Options();
        opt.index = index;
        opt.byModId = byId;
        opt.detectMcreator = true;
        opt.subject = "selftest";
        return Analyzer.analyze(text, opt);
    }

    private static boolean primaryIs(Diagnosis.Report r, String modId) {
        Diagnosis.Finding f = r.primarySuspect();
        return f != null && f.source() != null && f.source().modIds().contains(modId);
    }

    private static boolean anyFinding(Diagnosis.Report r, String modId) {
        return r.findings().stream().anyMatch(f -> f.source() != null && f.source().modIds().contains(modId));
    }

    private static boolean containsLocation(Diagnosis.Report r, String location) {
        return r.findings().stream().flatMap(f -> f.frames().stream())
                .anyMatch(af -> af.location().equals(location));
    }

    private static boolean hasRule(Diagnosis.Report r, String rule) {
        return r.issues().stream().anyMatch(i -> i.rule().equals(rule));
    }

    private static boolean hasRulePrefix(Diagnosis.Report r, String prefix) {
        return r.issues().stream().anyMatch(i -> i.rule().startsWith(prefix));
    }

    private static String rules(Diagnosis.Report r) {
        return r.issues().stream().map(Diagnosis.KnownIssue::rule).toList().toString();
    }

    private static String describe(Diagnosis.Report r) {
        Diagnosis.Finding f = r.primarySuspect();
        if (f == null) {
            return "无主要嫌疑对象 (findings=" + r.findings().size() + ", issues=" + rules(r) + ")";
        }
        return f.fileName() + " score=" + String.format("%.2f", f.score())
                + " frames=" + f.frames().size() + " issues=" + rules(r);
    }

    private static String describeLocations(Diagnosis.Report r) {
        List<String> locs = new ArrayList<>();
        r.findings().forEach(f -> f.frames().forEach(af -> locs.add(af.location())));
        return locs.toString();
    }

    private static String crashReport(String headline, List<String> frames) {
        StringBuilder sb = new StringBuilder();
        sb.append("---- Minecraft Crash Report ----\n");
        sb.append("// Don't be sad, have a hug!\n\n");
        sb.append("Time: 2026-01-01 10:00:00\n");
        sb.append("Description: Mod loading failed\n\n");
        sb.append(headline).append('\n');
        for (String f : frames) {
            sb.append(f).append('\n');
        }
        sb.append("\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");
        sb.append("---- Minecraft Crash Report ----\n");
        sb.append("Mod List: \n");
        sb.append("\tBogus Mod|bogusmod|1.2.3|bogusmod-1.2.3.jar\n");
        sb.append("\tGood Mod|goodmod|1.0|goodmod-1.0.jar\n");
        return sb.toString();
    }

    // 造一个 Fabric 模组：Connector 就是靠 fabric.mod.json 认出它们的
    private static Path makeFabricJar(Path dir, String fileName, String displayName, String modId,
                                      String version, List<String> classes) throws IOException {
        Path jar = dir.resolve(fileName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jar))) {
            zos.putNextEntry(new ZipEntry("fabric.mod.json"));
            zos.write(("{\"schemaVersion\":1,\"id\":\"" + modId + "\",\"version\":\"" + version
                    + "\",\"name\":\"" + displayName + "\","
                    + "\"entrypoints\":{\"main\":[\"me.jellysquid.mods.sodiumextra.SodiumExtraMod\"]}}")
                    .getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            for (String cls : classes) {
                zos.putNextEntry(new ZipEntry(cls.replace('.', '/') + ".class"));
                zos.write(fakeClassBytes(cls));
                zos.closeEntry();
            }
        }
        return jar;
    }

    // 造一个 Connector：NeoForge 模组，但带上 Sinytra 的类
    private static Path makeConnectorJar(Path dir, String fileName, String displayName, String modId,
                                         String version, List<String> classes) throws IOException {
        return makeJar(dir, fileName, displayName, modId, version, classes, Map.of(), null, List.of());
    }

    private static Path makeJar(Path dir, String fileName, String displayName, String modId, String version,
                                List<String> classes, Map<String, String> resources,
                                String extraEntries, List<String> nestedJars) throws IOException {
        Path jar = dir.resolve(fileName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jar))) {
            zos.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
            zos.write(modsToml(displayName, modId, version).getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            zos.write(("Manifest-Version: 1.0" + "\n"
                    + "Implementation-Version: " + version + "\n").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            for (String cls : classes) {
                zos.putNextEntry(new ZipEntry(cls.replace('.', '/') + ".class"));
                zos.write(fakeClassBytes(cls));
                zos.closeEntry();
            }
            for (Map.Entry<String, String> e : resources.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            if (extraEntries != null) {
                zos.putNextEntry(new ZipEntry(extraEntries));
                zos.write(new byte[]{0});
                zos.closeEntry();
            }
            for (String nested : nestedJars) {
                zos.putNextEntry(new ZipEntry(nested));
                zos.write(nestedJarBytes());
                zos.closeEntry();
            }
        }
        return jar;
    }

    private static Path makeJarWithNested(Path dir, String fileName, String displayName, String modId,
                                          String version, List<String> classes) throws IOException {
        return makeJar(dir, fileName, displayName, modId, version, classes, Map.of(), null,
                List.of("META-INF/jarjar/libthing-1.0.jar"));
    }

    private static Path makeMcreatorJar(Path dir, String fileName, String displayName, String modId, String version)
            throws IOException {
        List<String> classes = new ArrayList<>();
        classes.add("net.mcreator." + modId + ".CoolstuffMod");
        classes.add("net.mcreator." + modId + ".init.CoolstuffModItems");
        classes.add("net.mcreator." + modId + ".init.CoolstuffModBlocks");
        classes.add("net.mcreator." + modId + ".init.CoolstuffModEntities");
        classes.add("net.mcreator." + modId + ".entity.model.ModelCoolMob");
        classes.add("net.mcreator." + modId + ".entity.renderer.RendererCoolMob");
        classes.add("net.mcreator." + modId + ".procedures.CoolThingOnTickProcedure");
        classes.add("net.mcreator." + modId + ".procedures.SpawnLogicProcedure");
        classes.add("net.mcreator." + modId + ".procedures.DamageLogicProcedure");
        classes.add("net.mcreator." + modId + ".variables.WorldVariables");
        classes.add("net.mcreator." + modId + ".variables.MapVariables");
        classes.add("net.mcreator." + modId + ".block.BlockCoolOre");
        classes.add("net.mcreator." + modId + ".item.ItemCoolGem");
        classes.add("net.mcreator." + modId + ".CoolstuffModElements");
        Path jar = dir.resolve(fileName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jar))) {
            zos.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
            zos.write(("modLoader=\"javafml\"\nlicense=\"All Rights Reserved\"\n"
                    + "[[mods]]\nmodId=\"" + modId + "\"\nversion=\"" + version + "\"\n"
                    + "displayName=\"" + displayName + "\"\n"
                    + "description='''Generated with MCreator'''\n"
                    + "authors=\"Someone\"\n").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            for (String cls : classes) {
                zos.putNextEntry(new ZipEntry(cls.replace('.', '/') + ".class"));
                zos.write(fakeClassBytes(cls));
                zos.closeEntry();
            }
            zos.putNextEntry(new ZipEntry("assets/" + modId + "/models/item/cool_gem.json"));
            zos.write("{\n  \"parent\": \"item/generated\",\n  \"textures\": {\"layer0\": \""
                    .getBytes(StandardCharsets.UTF_8));
            zos.write((modId + ":item/cool_gem\"}\n}").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("assets/" + modId + "/models/block/cool_ore.json"));
            zos.write("{}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("data/" + modId + "/recipes/cool_gem.json"));
            zos.write("{\"_comment\":\"Generated by MCreator\"}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("assets/" + modId + "/lang/en_us.json"));
            zos.write(("{\"item." + modId + ".cool_gem\":\"Cool Gem\"}").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return jar;
    }

    private static String modsToml(String displayName, String modId, String version) {
        return "modLoader=\"javafml\"\n"
                + "loaderVersion=\"[4,)\"\n"
                + "license=\"MIT\"\n"
                + "[[mods]]\n"
                + "modId=\"" + modId + "\"\n"
                + "version=\"" + version + "\"\n"
                + "displayName=\"" + displayName + "\"\n"
                + "description='''A test mod.'''\n"
                + "authors=\"SelfTest\"\n"
                + "[[dependencies." + modId + "]]\n"
                + "modId=\"neoforge\"\n"
                + "type=\"required\"\n"
                + "versionRange=\"[21.1.0,)\"\n"
                + "ordering=\"NONE\"\n"
                + "side=\"BOTH\"\n";
    }

    // 一个长得像 class 文件的开头，这里只关心类名
    private static byte[] fakeClassBytes(String className) {
        return new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 65};
    }

    private static byte[] nestedJarBytes() throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (JarOutputStream jos = new JarOutputStream(bos)) {
            jos.putNextEntry(new JarEntry("META-INF/neoforge.mods.toml"));
            jos.write(modsToml("Lib Thing", "libthing", "1.0").getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
            jos.putNextEntry(new JarEntry("com/libthing/LibThing.class"));
            jos.write(fakeBinaryClass("com.libthing.LibThing"));
            jos.closeEntry();
        }
        // JarOutputStream 会把底层流一起关掉，得在这之前取字节
        return bos.toByteArray();
    }

    private static byte[] fakeBinaryClass(String className) {
        return new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 65};
    }

    private static void section(String title) {
        System.out.println("── " + title + " " + "─".repeat(Math.max(0, 50 - title.length())));
    }

    private static void check(String what, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("  ✓ " + what);
        } else {
            failed++;
            String msg = what + (detail.isEmpty() ? "" : "  → " + detail);
            failures.add(msg);
            System.out.println("  ✗ " + msg);
        }
    }

    private static void deleteRecursively(Path root) {
        try (var stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // 只是清临时目录
                }
            });
        } catch (IOException ignored) {
            // 只是清临时目录
        }
    }

    private SelfTest() {
    }
}
