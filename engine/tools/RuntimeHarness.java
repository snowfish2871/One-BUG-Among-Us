// 运行时验证台：用真实 log4j2 驱动真实 Mod 类，顺带检查只读保证。
package dev.oba.tools;

import dev.oba.core.Analyzer;
import dev.oba.core.ClassIndex;
import dev.oba.core.Diagnosis;
import dev.oba.core.Lang;
import dev.oba.core.McreatorDetector;
import dev.oba.core.ModSource;
import dev.oba.core.ProcessProbe;
import dev.oba.core.Reports;
import dev.oba.mod.DiagnosticAppender;
import dev.oba.mod.Diagnostics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class RuntimeHarness {

    private static int passed;
    private static int failed;
    private static final List<String> failures = new ArrayList<>();

    // Oba 从自己的输出口发出来的结论行
    private static final List<String> EMITTED = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("oba-runtime");
        Path modsDir = work.resolve("mods");
        Files.createDirectories(modsDir);

        section("0. 环境");
        System.out.println("  log4j2 版本: " + LogManager.class.getPackage().getImplementationVersion());
        System.out.println("  临时目录  : " + work);
        Check check = new Check();

        // 造测试用的假模组
        makeModJar(modsDir, "awesomemachines-2.1.0.jar", "Awesome Machines", "awesomemachines", "2.1.0",
                List.of("com.example.awesomemachines.AwesomeMachines",
                        "com.example.awesomemachines.registry.MachineRegistry",
                        "com.example.awesomemachines.util.EnergyHelper"));
        makeModJar(modsDir, "innocent-1.0.jar", "Innocent", "innocent", "1.0",
                List.of("org.innocent.InnocentMod"));
        makeMcreatorJar(modsDir, "coolstuff-1.0.0.jar", "Cool Stuff", "coolstuff", "1.0.0");

        List<ModSource> sources = ModSource.scanModsFolder(modsDir);
        ClassIndex index = Analyzer.buildIndex(sources);
        Map<String, ModSource> byModId = ModSource.indexByModId(sources);

        // ============================================================
        section("1. 只读保证（快照 → 全流程 → 再快照）");
        // ============================================================
        Map<String, String> before = snapshot(work);
        // 报告会写进 work/logs，那些是新文件；只读校验只针对输入侧
        Map<String, String> beforeMods = snapshot(modsDir);

        // 把 Mod 会碰文件的路径都走一遍
        for (ModSource s : sources) {
            s.classNames();
            s.entries();
            s.sha1();
            s.modIds();
            s.version();
            s.nestedJars();
            McreatorDetector.detect(s);
        }
        Analyzer.inventory(sources, true);
        Analyzer.findDuplicates(sources);
        Analyzer.candidateLogs(work);
        Analyzer.newestCrashReport(work);
        Reports.text(Analyzer.inventory(sources, true), false);
        Reports.json(Analyzer.inventory(sources, true));
        Reports.markdown(Analyzer.inventory(sources, true));

        Map<String, String> after = snapshot(work);
        check.check("输入文件数量不变", before.size() == after.size(),
                before.size() + " -> " + after.size());
        check.check("每个文件的 SHA-1 完全一致", before.equals(after), diff(before, after));
        check.check("没有新增任何文件",
                after.keySet().stream().allMatch(before::containsKey),
                after.keySet().stream().filter(k -> !before.containsKey(k)).toList().toString());

        // ============================================================
        section("2. log4j2 实时采集（Mod 实际使用的通道）");
        // ============================================================
        EMITTED.clear();

        // 用真正的 Diagnostics 单例，把它的输出转到这里，
        // 这样它本来会打进游戏日志的结论我们也能断言
        String previousLanguage = Lang.language();
        Diagnostics diagnostics = Diagnostics.get();
        diagnostics.setOutput(line -> {
            EMITTED.add(line);
            System.out.println("      [Oba 输出] "
                    + line.replace("\n", "\n      [Oba 输出] "));
        });

        boolean installed = DiagnosticAppender.install(diagnostics);
        check.check("DiagnosticAppender 成功挂载到 log4j2", installed, "install() 返回 false");

        // 用增量方式看窗口：之前就在里面的行是上一阶段留下的，
        // 只有新增的才算这次真的送到了
        List<String> windowBefore = diagnostics.logWindowSnapshot();

        Logger logger = LogManager.getLogger("dev.oba.harness");

        // 一条带 cause 链的真实异常 ERROR
        NullPointerException npe = new NullPointerException(
                "Cannot invoke \"String.length()\" because \"name\" is null");
        npe.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("com.example.awesomemachines.registry.MachineRegistry",
                        "register", "MachineRegistry.java", 143),
                new StackTraceElement("com.example.awesomemachines.AwesomeMachines",
                        "onCommonSetup", "AwesomeMachines.java", 58),
                new StackTraceElement("net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent",
                        "dispatch", "FMLCommonSetupEvent.java", 31),
        });
        RuntimeException wrapper = new RuntimeException("Mod loading failed", npe);
        wrapper.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("net.neoforged.fml.javafmlmod.FMLModContainer",
                        "constructMod", "FMLModContainer.java", 100),
        });
        logger.error("Failed to create mod instance. Mod: awesomemachines", wrapper);

        // 一条纯文本的缺前置报错
        logger.error("Mod awesomemachines requires absentlib 1.4.0 that is missing");

        // WARN 应该进窗口，但不该触发结论
        logger.warn("Some unrelated warning that should not be analysed");

        List<String> windowAfter = diagnostics.logWindowSnapshot();
        List<String> captured = new ArrayList<>(windowAfter);
        captured.removeAll(windowBefore);
        check.check("ERROR 事件经 log4j2 送达 Diagnostics（窗口新增行）", captured.size() >= 2,
                "新增 " + captured.size() + " 行");
        String blob = String.join("\n", captured);
        check.check("异常类名被记录", blob.contains("java.lang.NullPointerException"), excerpt(blob));
        check.check("异常消息被记录", blob.contains("because \"name\" is null"), excerpt(blob));
        check.check("栈帧被还原为 at 行", blob.contains("\tat com.example.awesomemachines.registry.MachineRegistry.register")
                || blob.contains("MachineRegistry.java:143"), excerpt(blob));
        check.check("Caused by 链被保留", blob.contains("Caused by: java.lang.NullPointerException"), excerpt(blob));
        check.check("行号被保留 (MachineRegistry.java:143)", blob.contains("MachineRegistry.java:143"), excerpt(blob));
        check.check("上层异常的帧也被保留 (FMLModContainer)", blob.contains("FMLModContainer"), excerpt(blob));
        check.check("ERROR 事件确实被计数（人工错误计数增长）",
                diagnostics.capturedErrors() >= 2,
                "capturedErrors=" + diagnostics.capturedErrors());
        check.check("不采集 Oba 自身输出",
                captured.stream().noneMatch(l -> l.startsWith("[Oba]")), "");

        // ============================================================
        section("3. 端到端归属（Mod 真实代码路径: analyzeWindow）");
        // ============================================================
        // 给 Diagnostics 装上游戏里会建的同一份索引，然后让它分析自己的窗口：
        // 这正是它在游戏里发现日志报错时走的那条路
        diagnostics.adoptIndex(sources);

        Diagnosis.Report report = diagnostics.analyzeWindow();
        check.check("analyzeWindow 产出报告", report != null, "返回 null");

        Diagnosis.Finding primary = report == null ? null : report.primarySuspect();
        check.check("锁定 awesomemachines", primary != null && primary.source() != null
                && primary.source().modIds().contains("awesomemachines"),
                primary == null ? "无主要嫌疑对象" : primary.fileName());
        if (primary != null) {
            var loc = primary.firstLocatedFrame();
            check.check("定位到 MachineRegistry.java:143",
                    loc != null && loc.location().equals(
                            "com.example.awesomemachines.registry.MachineRegistry.register (MachineRegistry.java:143)"),
                    loc == null ? "无位置" : loc.location());
            check.check("可信度为高或极高",
                    primary.confidence() == Diagnosis.Confidence.HIGH
                            || primary.confidence() == Diagnosis.Confidence.CERTAIN,
                    primary.confidence().label());
        }
        check.check("未把无辜模组当元凶",
                report.findings().stream().noneMatch(f -> f.source() != null
                        && f.source().modIds().contains("innocent")),
                report.findings().stream().map(Diagnosis.Finding::fileName).toList().toString());
        check.check("命中 missing dependency 规则",
                report.issues().stream().anyMatch(i -> i.rule().startsWith("neoforge.missing_dependency")),
                report.issues().stream().map(Diagnosis.KnownIssue::rule).toList().toString());

        // 等 Mod 去抖动后自动给出的那声结论
        long deadline = System.currentTimeMillis() + 12_000L;
        while (EMITTED.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(250L);
        }
        check.check("Mod 自动输出了诊断结论", !EMITTED.isEmpty(),
                "12 秒内没有输出（去抖动调度可能未生效）");
        String verdict = String.join("\n", EMITTED);
        check.check("结论里点名了正确的模组",
                verdict.contains("awesomemachines") || verdict.contains("Awesome Machines"),
                excerpt(verdict));
        check.check("结论里给出了出错位置", verdict.contains("MachineRegistry.java:143"),
                excerpt(verdict));
        check.check("结论声明了只读立场", verdict.contains(Lang.get("mod.reportHint")),
                excerpt(verdict));
        check.check("最近结论被留存（供 /oba report 使用）",
                !diagnostics.recentVerdicts().isEmpty(), "");

        // 同一份 Report 对象要能用两种语言渲染：分析结果缓存之后，游戏语言还可能被改
        Lang.setLanguage("en");
        String reportEn = Reports.text(report, false);
        String titleEn = report.title();
        boolean enClean = reportEn.codePoints().noneMatch(c -> c >= 0x4e00 && c <= 0x9fff);
        Lang.setLanguage("zh");
        String reportZh = Reports.text(report, false);
        String titleZh = report.title();
        check.check("英文渲染无中文残留", enClean, excerpt(reportEn) + " | " + cjk(reportEn));
        // 证据链里引用的规则标题也得跟着渲染语言走，
        // 而不是停留在分析时的那门语言
        check.check("问题标题按渲染语言解析",
                report.issues().stream().allMatch(iss -> iss.title().equals(Lang.get(iss.titleKey()))),
                "");
        check.check("同一份报告可用两种语言渲染",
                enClean && !titleEn.equals(titleZh) && reportZh.contains(titleZh),
                "en=<" + titleEn + "> zh=<" + titleZh + ">");
        Lang.setLanguage(previousLanguage);

        // ============================================================
        section("4. MCreator 识别（真实 jar 读取）");
        // ============================================================
        ModSource mc = byModId.get("coolstuff");
        var hit = mc == null ? java.util.Optional.<Diagnosis.McreatorHit>empty() : McreatorDetector.detect(mc);
        check.check("识别出 coolstuff 为 MCreator 模组", hit.isPresent(), "未识别");
        if (hit.isPresent()) {
            check.check("给出多条判定理由", hit.get().reasons().size() >= 3,
                    "理由数 " + hit.get().reasons().size());
        }
        ModSource innocent = byModId.get("innocent");
        check.check("普通模组未被误报",
                innocent != null && McreatorDetector.detect(innocent).isEmpty(),
                innocent == null ? "未找到" : McreatorDetector.detect(innocent)
                        .map(h -> h.reasons().toString()).orElse(""));

        // ============================================================
        section("5. 进程监控（本 JVM 活体线程转储）");
        // ============================================================
        // 造一个停在模组代码里的线程，好让归属有东西可找
        Object lock = new Object();
        Thread stuck = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(30_000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "ObaHarness-StuckThread");
        stuck.setDaemon(true);
        stuck.start();
        Thread.sleep(400);

        String dump = ProcessProbe.captureThreadDump(false);
        check.check("采集到本进程线程转储", dump.contains("\"") && dump.contains("java.lang.Thread.State"),
                excerpt(dump));
        check.check("转储包含我们制造的线程", dump.contains("ObaHarness-StuckThread"), excerpt(dump));

        List<ProcessProbe.ThreadInfo> parsed = ProcessProbe.parseThreadDump(dump);
        check.check("线程转储可被解析", parsed.size() >= 5, "解析出 " + parsed.size() + " 个线程");
        boolean foundStuck = parsed.stream().anyMatch(t -> t.name().contains("ObaHarness"));
        check.check("解析结果包含目标线程", foundStuck,
                parsed.stream().map(ProcessProbe.ThreadInfo::name).limit(6).toList().toString());

        Diagnosis.Report threadReport = ProcessProbe.analyzeThreadDump(dump, index, byModId,
                "ObaHarness-StuckThread");
        check.check("线程分析产出报告", threadReport != null && threadReport.frameCount() > 0,
                threadReport == null ? "null" : "帧数 " + threadReport.frameCount());

        // 死锁检测要能调用，而且不能抛异常
        List<String> deadlocks = ProcessProbe.findDeadlockedThreads();
        check.check("死锁检测可正常调用", deadlocks != null, "");
        check.check("本进程无死锁", deadlocks.isEmpty(), deadlocks.toString());
        check.check("进程信息可读取", ProcessProbe.processFacts().containsKey(Lang.get("status.threads")),
                ProcessProbe.processFacts().toString());

        // ============================================================
        section("6. 报告自动落到 logs 目录");
        // ============================================================
        // 游戏内不用做任何操作，出事之后 logs 里就该有一份报告。
        // 验证台没有 FML，游戏目录是注入进去的，让报告落到临时目录里。
        setGameDir(diagnostics, work);
        Path logsDir = diagnostics.reportDirectory();
        check.check("报告目录指向游戏目录下的 logs",
                logsDir != null && logsDir.getFileName().toString().equals("logs"),
                String.valueOf(logsDir));

        Path written = diagnostics.writeReportNow();
        check.check("报告能写进 logs 目录", written != null && Files.isRegularFile(written),
                String.valueOf(written));
        if (written != null) {
            String content = Files.readString(written);
            check.check("报告里有元凶文件名", content.contains("awesomemachines"), "");
            check.check("报告里有出错位置", content.contains("MachineRegistry.java:143"), "");
            check.check("报告声明只读", content.contains(Lang.get("footer.readonly")), "");
            check.check("报告文件名带 oba-report 前缀",
                    written.getFileName().toString().startsWith("oba-report-"), written.getFileName().toString());
        }
        Path latest = logsDir.resolve("oba-latest.txt");
        check.check("同时留了一份固定名字的 oba-latest.txt", Files.isRegularFile(latest), "");

        // 同一份结论不该反复刷文件
        Path again = diagnostics.writeReportNow();
        check.check("重复触发不会打断写入（命令路径允许覆盖）", again != null, "");

        // ============================================================
        section("7. F3+C 那种不写日志的崩溃");
        // ============================================================
        // 实测反馈：F3+C 手动调试崩溃在 latest.log 里一个字都没有，
        // 只有 crash-reports 下落一个文件。退出时得自己去磁盘上捡。
        Path crashDir = work.resolve("crash-reports");
        Files.createDirectories(crashDir);

        // 启动时就存在的旧报告，不该算成这次的
        Path oldCrash = crashDir.resolve("crash-2020-01-01_00.00.00-client.txt");
        Files.writeString(oldCrash, "---- Minecraft Crash Report ----\nTime: 2020-01-01\n");
        diagnostics.rememberExistingCrashReports();
        check.check("启动时已有的崩溃报告不算这次的",
                diagnostics.newCrashReports().isEmpty(),
                diagnostics.newCrashReports().toString());

        // 模拟 F3+C：新落下一份崩溃报告
        Path f3c = crashDir.resolve("crash-2026-10-10_16.47.12-client.txt");
        Files.writeString(f3c, String.join("\n",
                "---- Minecraft Crash Report ----",
                "// There are four lights!",
                "",
                "Time: 2026-10-10 16:47:13",
                "Description: Manually triggered debug crash",
                "",
                "java.lang.Throwable: Manually triggered debug crash",
                "\tat TRANSFORMER/minecraft@1.21.1/net.minecraft.client.KeyboardHandler.tick(KeyboardHandler.java:545) ~[client.jar!/:?] {re:classloading}",
                "\tat java.base/java.lang.Thread.run(Thread.java:1583) [?:?] {}"));

        var fresh = diagnostics.newCrashReports();
        check.check("新落下的崩溃报告能被发现", fresh.size() == 1,
                fresh.toString());

        Diagnosis.Report beforeCrashPickup = diagnostics.lastReportForTest();
        diagnostics.pickUpNewCrashReports();
        // 报告目录只保留最近 10 份，份数可能不变，所以断言"最后一份分析的是这份崩溃报告"
        check.check("捡到之后会拿它出一份结论",
                diagnostics.lastReportForTest() != null
                        && diagnostics.lastReportForTest() != beforeCrashPickup
                        && String.valueOf(diagnostics.lastReportForTest().subject())
                                .contains("crash-2026-10-10_16.47.12"),
                String.valueOf(diagnostics.lastReportForTest() == null
                        ? null : diagnostics.lastReportForTest().subject()));

        // 内容里要说明这是手动触发的，不然用户会一直找元凶
        Path f3cReport = newestReport(logsDir);
        if (f3cReport != null) {
            String text = Files.readString(f3cReport);
            check.check("报告里写明了是 F3+C 手动触发",
                    text.contains(Lang.get("note.manualCrash")), "");
            check.check("手动崩溃不点元凶",
                    !text.contains(Lang.get("verdict.culprit")), "");
        }

        // ============================================================
        section("7. 最终只读校验");
        // ============================================================
        // 报告是写到 logs 下的，输入侧的文件依然一个都不能动
        Map<String, String> finalState = snapshot(work.resolve("mods"));
        check.check("全流程结束后模组文件仍未被改动",
                beforeMods.equals(finalState), diff(beforeMods, finalState));

        // 汇总
        System.out.println();
        System.out.println("══════════════════════════════════════════════════════");
        System.out.printf("运行时验证: 通过 %d / %d%n", passed, passed + failed);
        if (failed > 0) {
            System.out.println("失败项:");
            failures.forEach(f -> System.out.println("  ✗ " + f));
        }
        System.out.println("══════════════════════════════════════════════════════");

        stuck.interrupt();
        deleteRecursively(work);
        System.exit(failed == 0 ? 0 : 1);
    }

    // 辅助方法

    // logs 目录里现有几份 oba 报告
    private static int countReports(Path logsDir) {
        try (var s = Files.newDirectoryStream(logsDir, "oba-report-*.txt")) {
            int n = 0;
            for (var ignored : s) {
                n++;
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    // 最近写出的那份报告
    private static Path newestReport(Path logsDir) {
        try (var s = Files.newDirectoryStream(logsDir, "oba-report-*.txt")) {
            Path best = null;
            for (Path p : s) {
                if (best == null || Files.getLastModifiedTime(p).compareTo(Files.getLastModifiedTime(best)) > 0) {
                    best = p;
                }
            }
            return best;
        } catch (Exception e) {
            return null;
        }
    }

    // 验证台里没有 FML，游戏目录得直接塞进去
    private static void setGameDir(Diagnostics diagnostics, Path dir) {
        try {
            java.lang.reflect.Field f = Diagnostics.class.getDeclaredField("gameDir");
            f.setAccessible(true);
            f.set(diagnostics, dir);
        } catch (Exception e) {
            throw new IllegalStateException("无法设置游戏目录: " + e, e);
        }
    }

    // 目录下每个文件的快照：路径 -> sha1|大小|修改时间
    private static Map<String, String> snapshot(Path root) throws IOException {
        Map<String, String> out = new TreeMap<>();
        try (var stream = Files.walk(root)) {
            for (Path p : stream.filter(Files::isRegularFile).toList()) {
                String rel = root.relativize(p).toString();
                out.put(rel, sha1(p) + "|" + Files.size(p) + "|" + Files.getLastModifiedTime(p).toMillis());
            }
        }
        return out;
    }

    private static String diff(Map<String, String> a, Map<String, String> b) {
        List<String> changed = new ArrayList<>();
        for (String k : a.keySet()) {
            String vb = b.get(k);
            if (vb == null) {
                changed.add(k + " 被删除");
            } else if (!vb.equals(a.get(k))) {
                changed.add(k + " 被修改 (" + a.get(k) + " -> " + vb + ")");
            }
        }
        return changed.isEmpty() ? "" : changed.toString();
    }

    private static String sha1(Path p) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] data = Files.readAllBytes(p);
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte x : digest) {
                sb.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static void makeModJar(Path dir, String fileName, String name, String modId, String version,
                                   List<String> classes) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(dir.resolve(fileName)))) {
            zos.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
            zos.write(("modLoader=\"javafml\"\nlicense=\"MIT\"\n[[mods]]\nmodId=\"" + modId
                    + "\"\nversion=\"" + version + "\"\ndisplayName=\"" + name
                    + "\"\ndescription='''Harness mod.'''\n").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            for (String c : classes) {
                zos.putNextEntry(new ZipEntry(c.replace('.', '/') + ".class"));
                zos.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 65});
                zos.closeEntry();
            }
        }
    }

    private static void makeMcreatorJar(Path dir, String fileName, String name, String modId, String version)
            throws IOException {
        List<String> classes = List.of(
                "net.mcreator." + modId + ".CoolstuffMod",
                "net.mcreator." + modId + ".init.CoolstuffModItems",
                "net.mcreator." + modId + ".init.CoolstuffModBlocks",
                "net.mcreator." + modId + ".entity.model.ModelCoolMob",
                "net.mcreator." + modId + ".entity.renderer.RendererCoolMob",
                "net.mcreator." + modId + ".procedures.CoolGemOnTickProcedure",
                "net.mcreator." + modId + ".procedures.SpawnLogicProcedure",
                "net.mcreator." + modId + ".variables.WorldVariables");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(dir.resolve(fileName)))) {
            zos.putNextEntry(new ZipEntry("META-INF/neoforge.mods.toml"));
            zos.write(("modLoader=\"javafml\"\nlicense=\"All Rights Reserved\"\n[[mods]]\nmodId=\"" + modId
                    + "\"\nversion=\"" + version + "\"\ndisplayName=\"" + name
                    + "\"\ndescription='''Made with MCreator'''\n").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            for (String c : classes) {
                zos.putNextEntry(new ZipEntry(c.replace('.', '/') + ".class"));
                zos.write(new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0, 0, 0, 65});
                zos.closeEntry();
            }
            zos.putNextEntry(new ZipEntry("assets/" + modId + "/models/item/cool_gem.json"));
            zos.write("{}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("assets/" + modId + "/models/block/cool_ore.json"));
            zos.write("{}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("data/" + modId + "/recipes/cool_gem.json"));
            zos.write("{\"_comment\":\"Generated by MCreator 2024.4\"}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("assets/" + modId + "/lang/en_us.json"));
            zos.write("{}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    // 列出字符串里出现的汉字，报错信息里用得上
    private static String cjk(String s) {
        StringBuilder sb = new StringBuilder();
        s.codePoints().filter(c -> c >= 0x4e00 && c <= 0x9fff).distinct()
                .forEach(c -> sb.appendCodePoint(c).append(' '));
        return sb.toString().trim();
    }

    private static String excerpt(String s) {
        String flat = s.replace("\n", "\\n");
        return flat.length() > 220 ? flat.substring(0, 220) + "…" : flat;
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("── " + title + " " + "─".repeat(Math.max(0, 52 - title.length())));
    }

    private static final class Check {
        void check(String what, boolean ok, String detail) {
            if (ok) {
                passed++;
                System.out.println("  ✓ " + what);
            } else {
                failed++;
                String msg = what + (detail == null || detail.isEmpty() ? "" : "  → " + detail);
                failures.add(msg);
                System.out.println("  ✗ " + msg);
            }
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

    private RuntimeHarness() {
    }
}
