// 中英文文案表，报告和命令行里的每一句话都从这里取。
package dev.oba.core;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class Lang {

    public static final String EN = "en";
    public static final String ZH = "zh";

    private static volatile String language = detect();
    // 玩家在游戏里选的语言。引擎读不到游戏设置，所以由 Mod 主动推进来；
    // 它优先于 JVM 区域，毕竟是玩家自己选的
    private static volatile String gameLanguage;

    private static final Map<String, String[]> MESSAGES = new LinkedHashMap<>();

    private Lang() {
    }

    // 按语言标签判断用哪套文案，比如 zh_cn、en_us、zh
    public static String detect(String tag) {
        if (tag == null || tag.isBlank()) {
            return detect();
        }
        String t = tag.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return t.startsWith("zh") ? ZH : EN;
    }

    public static String detect() {
        String forced = System.getProperty("oba.lang");
        if (forced == null || forced.isBlank()) {
            forced = System.getenv("OBA_LANG");
        }
        if (forced != null && !forced.isBlank()) {
            return detect(forced);
        }
        String tag = System.getProperty("user.language", EN);
        return detect(tag);
    }

    public static String language() {
        return language;
    }

    // 强制指定语言，比如来自 --lang
    public static void setLanguage(String tag) {
        language = detect(tag);
    }

    // 游戏内 Mod 知道玩家语言后调这个。传 null 就撤销覆盖，回到 JVM 区域
    public static void setGameLanguage(String tag) {
        gameLanguage = (tag == null || tag.isBlank()) ? null : detect(tag);
        language = gameLanguage != null ? gameLanguage : detect();
    }

    public static String gameLanguage() {
        return gameLanguage;
    }

    // 当前输出中文时为真
    public static boolean isChinese() {
        return ZH.equals(language);
    }

    public record Ref(String key, Object... args) {
        // 按当前语言把嵌套的那条消息渲染出来
        public String resolve() {
            return Lang.get(key, args);
        }
    }

    // 把一条嵌套消息包起来，留到渲染时再解析
    public static Ref ref(String key, Object... args) {
        return new Ref(key, args);
    }

    // 按当前语言取一条文案。{0}、{1} 会被 args 依次替换。
    // 键不存在时直接返回键名而不抛异常：一个标签写错不该让整次诊断挂掉
    public static String get(String key, Object... args) {
        String[] entry = MESSAGES.get(key);
        String raw;
        if (entry == null) {
            raw = key;
        } else {
            raw = ZH.equals(language) ? entry[1] : entry[0];
        }
        if (args.length == 0) {
            return raw;
        }
        String out = raw;
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            String value = arg instanceof Ref nested ? nested.resolve() : String.valueOf(arg);
            out = out.replace("{" + i + "}", value);
        }
        return out;
    }

    // get 的简写
    public static String t(String key, Object... args) {
        return get(key, args);
    }

    // 所有已定义的键，测试用它查漏
    public static java.util.Set<String> keys() {
        return java.util.Collections.unmodifiableSet(MESSAGES.keySet());
    }

    private static void put(String key, String en, String zh) {
        MESSAGES.put(key, new String[]{en, zh});
    }

    static {
        // 报告骨架
        put("report.title.diagnosis", "One BUG Among Us - Diagnosis", "One BUG Among Us —— 诊断报告");
        put("report.title.inventory", "One BUG Among Us - Mod Inventory", "One BUG Among Us —— 模组清单");
        put("report.title.runtime", "One BUG Among Us - Live Diagnosis", "One BUG Among Us —— 运行时诊断");
        put("report.title.threads", "One BUG Among Us - Thread Analysis", "One BUG Among Us —— 线程归属分析");
        put("report.title.stall", "One BUG Among Us - Stall Diagnosis", "One BUG Among Us —— 卡死诊断");
        put("report.title.log", "One BUG Among Us - Log Diagnosis", "One BUG Among Us —— 日志诊断");
        put("report.subject.modsDir", "mods directory", "mods 目录");
        put("report.subject.gameLog", "game log (last {0} lines)", "游戏运行日志（最近 {0} 行）");

        put("label.generatedAt", "Generated", "生成时间");
        put("header.tagline", "read-only crash diagnosis", "只读报错诊断");
        put("label.subject", "Analysed", "分析对象");
        put("label.size", "Document", "文档规模");
        put("label.size.value", "{0} lines, {1} frames ({2} attributed to mods)",
                "{0} 行，{1} 个栈帧（其中 {2} 个归属到模组）");
        put("label.scanned", "Scanned", "已扫描");
        put("label.scanned.value", "{0} mod files, {1} classes indexed",
                "{0} 个模组文件，索引 {1} 个类");
        put("label.policy", "Policy", "策略");
        put("label.policy.value", "read only - no file is modified, disabled or deleted",
                "只读诊断，不会修改 / 禁用 / 删除任何文件");

        put("section.verdict", "Verdict", "结论");
        put("section.suspects", "Suspects by confidence", "可疑模组排序");
        put("section.detail", "Detail #{0}", "详情 #{0}");
        put("section.issues", "Findings", "问题清单");
        put("section.issues.sorted", "Findings (most severe first)", "问题清单（按严重程度排序）");
        put("section.mcreator", "Mods generated by MCreator", "MCreator 生成的模组");
        put("section.unmatched", "Packages that could not be attributed", "无法归属的包");
        put("section.unmatched.hint", "probably libraries or coremods outside the mods folder",
                "可能是不在 mods 目录里的库或核心模组");

        // 事故时间线
        put("section.timeline", "Incident timeline", "事故时间线");
        put("section.timeline.hint", "most recent event first", "最近发生的排在最前面");
        put("timeline.columns", "time         mod                what happened",
                "时间         模组               发生了什么");
        put("timeline.col.time", "time", "时间");
        put("timeline.col.mod", "mod", "模组");
        put("timeline.col.event", "what happened", "发生了什么");
        put("timeline.more", "... and {0} earlier events", "…… 另有 {0} 条更早的事件");
        put("timeline.readHint",
                "Read from the top: the first line is what happened last, closest to the failure.",
                "从上往下读：第一行就是最接近崩溃的那一刻。");
        put("timeline.raw", "{0}", "{0}");
        put("timeline.repeated", "(the same line repeated {0} times, folded)", "（同一行重复了 {0} 次，已折叠）");
        put("timeline.none", "nothing worth recording in this window", "这段窗口里没有值得记录的事件");

        // 图形类模组专项：钠 / iris 这一系的分支与组合问题
        put("section.graphics", "Graphics mods", "图形类模组");
        put("section.graphics.hint", "Sodium, Iris and their many forks",
                "钠、iris 及其各分支");
        put("gfx.col.mod", "mod", "模组");
        put("gfx.col.family", "family", "所属族");
        put("gfx.col.loader", "for", "面向");
        put("gfx.col.note", "note", "说明");
        put("gfx.canonical", "upstream", "正统");
        put("gfx.fork", "fork", "分支");
        put("gfx.issue.conflict.title", "Two members of the {0} are installed at once",
                "{0} 同时装了多个互斥分支");
        put("gfx.issue.conflict.advice",
                "{0} are the same renderer under different names, or two copies of one version line. They are mutually exclusive: keep exactly one and remove the rest. Having two is a common cause of a black screen or a crash on the render thread.",
                "{0} 要么是换名字的同一个渲染器，要么是同一个模组的两个版本。它们互斥，只保留其中一个、其余移除。同时装两个是黑屏或者崩在渲染线程的常见原因。");        put("gfx.issue.missing.title", "{0} has no renderer to attach to",
                "{0} 没有可以挂靠的渲染主体");
        put("gfx.issue.missing.advice",
                "{0} is a shader loader with no renderer of its own; it needs {1}. Without it the game usually fails with NoClassDefFoundError before reaching the main menu.",
                "{0} 是光影加载器，自己不带渲染实现，必须配合 {1}。缺了它通常在主菜单之前就抛 NoClassDefFoundError。");
        put("gfx.family.sodium", "Sodium family", "钠系（Sodium）");
        put("gfx.family.iris", "Iris family", "iris 系（光影）");
        put("gfx.family.lithium", "Lithium family", "锂系（Lithium）");
        put("gfx.family.krypton", "Krypton family", "氪系（Krypton）");
        put("gfx.family.phosphor", "Phosphor family", "磷系（Phosphor）");
        put("gfx.note.bridged",
                "Fabric build loaded through Connector; its classes still live in the Fabric packages.",
                "经 Connector 加载的 Fabric 版本，类名仍在 Fabric 那套包名下。");
        put("cli.gfx.issue", "Graphics: {0}", "图形：{0}");
        put("cli.gfx.item", "- {0}", "- {0}");
        put("cli.gfx.advice", "  -> {0}", "  -> {0}");
        put("gfx.role.optimizer", "render optimizer", "渲染优化主体");
        put("gfx.role.shader", "shader loader", "光影加载器");
        put("gfx.role.addon", "add-on", "附加组件");
        put("gfx.role.other", "other optimizer", "其它优化");

        // 游戏内点开某个模组时给的简介
        put("cmd.info.title", "{0}  ({1})", "{0}  （{1}）");
        put("cmd.info.file", "File", "文件");
        put("cmd.info.version", "Version", "版本");
        put("cmd.info.id", "Mod ID", "模组 ID");
        put("cmd.info.classes", "Classes", "类数量");
        put("cmd.info.size", "Size", "文件大小");
        put("cmd.info.sha1", "SHA-1", "SHA-1");
        put("cmd.info.author", "Author", "作者");
        put("cmd.info.license", "License", "许可");
        put("cmd.info.loader", "Loader", "加载器");
        put("cmd.info.mcreator", "Looks MCreator generated ({0} points)", "疑似 MCreator 生成（{0} 分）");
        put("cmd.info.graphics", "Graphics family: {0} ({1})", "图形类模组：{0}（{1}）");
        put("cmd.info.duplicate", "Duplicate: {0} other file(s) declare the same mod ID",
                "重复：另有 {0} 个文件声明了同一个 mod ID");
        put("cmd.info.issues", "Known issues ({0})", "已知问题（{0}）");
        put("cmd.info.none", "No description in this mod's metadata.", "这个模组的元数据里没有写简介。");
        put("cmd.info.hint", "Click a mod name in /oba mods to see this.",
                "在 /oba mods 里点模组名就能看到这些。");
        put("cmd.info.unknown", "No loaded mod with id \"{0}\".", "没有找到 id 为「{0}」的已加载模组。");
        put("cmd.help.info", "show a mod's description and metadata", "查看某个模组的简介与元数据");
        put("cmd.info.tooLong", "... ({0} more lines, see the report file)",
                "…… 还有 {0} 行，见报告文件");

        put("verdict.culprit", "Most likely culprit", "最可能的元凶");
        put("verdict.modFile", "Mod file", "模组文件");
        put("verdict.version", "Version", "版本");
        put("verdict.path", "Full path", "文件路径");
        put("verdict.sha1", "SHA-1", "SHA-1");
        put("verdict.confidence", "Confidence", "可信度");
        put("verdict.location", "Failure location", "出错位置");
        put("verdict.exception", "Exception", "异常");
        put("verdict.mcreatorTrait", "Mod trait", "该模组特征");
        put("verdict.advice", "Next step: move {0} out of the mods folder and start once more to confirm.",
                "建议先处理：把 {0} 移出 mods 目录再启动一次，确认问题是否消失。");
        put("verdict.advice.hint", "(OBA only diagnoses; this step is yours to take)",
                "（OBA 只做诊断，这个操作需要你自己执行）");
        put("verdict.none", "Nothing could be attributed to a specific mod.", "未发现可归属到具体模组的错误。");
        put("verdict.none.hint", "If the game did crash, feed OBA the newest crash report, or run with --watch.",
                "如果游戏确实崩了，请把 crash-reports 里的最新文件喂给 OBA，或使用 --watch 实时监听。");
        put("verdict.onlyRules", "No stack frame belonged to a mod, but {0} rule findings were matched - see below.",
                "没有栈帧能归属到某个模组，但发现了 {0} 条规则命中的问题，见下方「问题清单」。");

        put("detail.file", "File", "文件");
        put("detail.throwables", "Throwables involved", "涉及异常");
        put("detail.locations", "Failure locations (inside this mod)", "出错位置（该模组自己的代码）");
        put("detail.moreFrames", "... {0} more frames belong to this mod", "… 另有 {0} 个栈帧属于该模组");
        put("detail.evidence", "Evidence", "证据链");
        put("detail.frames", "{0} frames", "{0} 帧");
        put("detail.score", "total evidence weight {0}", "证据权重合计 {0}");
        put("detail.line", "line {0}", "第 {0} 行");
        put("detail.matchExact", "exact class match", "精确匹配类名");
        put("detail.matchPackage", "package match {0} ({1}%)", "包名匹配 {0} ({1}%)");
        put("detail.suggest", "Advice: {0}", "建议：{0}");

        put("issue.relatedMod", "Related mod", "关联模组");
        put("issue.relatedFiles", "Related files", "相关文件");

        put("mcreator.score", "score {0} / {1} signals", "{0} 分 / {1} 项特征");
        put("mcreator.note", "These mods are not broken by definition, but they are a common source of crashes; "
                        + "when hunting a problem they are worth testing alone first.",
                "MCreator 生成的模组本身不是错误，但它是崩溃的常见来源之一；如果你在排查问题，这类模组值得优先单独测试。");

        put("note.prefix", "Note: {0}", "注：{0}");
        put("footer.readonly", "One BUG Among Us (OBA) - read only diagnosis - no file was changed",
                "One BUG Among Us (OBA) · 只读诊断 · 未对任何文件做出改动");

        // 可信度和严重程度
        put("confidence.certain", "very high", "极高");
        put("confidence.high", "high", "高");
        put("confidence.medium", "medium", "中");
        put("confidence.low", "low", "低");
        put("severity.fatal", "fatal", "致命");
        put("severity.error", "error", "错误");
        put("severity.warn", "warning", "警告");
        put("severity.info", "info", "提示");

        // 规则通用
        put("rule.match.detail", "{0}", "{0}");

        // 规则标题与建议
        put("rule.missing_dependency.title", "Missing required mod", "缺少前置模组");
        put("rule.missing_dependency.advice",
                "Install the mod named above, or switch to a build that does not need it. Do not simply delete the mod that reported the error.",
                "安装提示中缺失的模组，或改用不需要该前置的版本；不要只删除报错的模组，先补齐依赖。");
        put("rule.missing_dependency_generic.title", "Missing mandatory dependency", "缺少必需的前置依赖");
        put("rule.missing_dependency_generic.advice",
                "Install the mod ids listed below; the version range has to match as well.",
                "按下面列出的 modId 补齐前置模组，版本范围也要匹配。");
        put("rule.incompatible_mod.title", "Incompatible mods", "模组互斥");
        put("rule.incompatible_mod.advice", "The two mods declare each other incompatible; only one can stay.",
                "两个模组声明了互斥关系，只能保留其中一个。");
        put("rule.dependency_version.title", "Dependency version mismatch", "前置模组版本不匹配");
        put("rule.dependency_version.advice", "Upgrade or downgrade the dependency into the required range.",
                "把前置模组升级/降级到要求的版本区间。");
        put("rule.duplicate_mod.title", "The same modId is loaded twice", "同一个 modId 被重复加载");
        put("rule.duplicate_mod.advice",
                "Two files provide this mod (often a Forge and a NeoForge build, or an old copy that was never deleted). Keep one.",
                "mods 目录里存在同一模组的两个文件（常见于同时放了 Forge 与 NeoForge 版本，或旧版本没删）。只保留一个。");
        put("rule.duplicate_mod.advice.short", "Delete the duplicate mod file so only one version remains.",
                "删除 mods 目录中重复的模组文件，只保留一个版本。");
        put("rule.mixin_apply.title", "Mixin injection failed", "Mixin 注入失败");
        put("rule.mixin_apply.advice",
                "The mixin conflicts with another mod or with this game version. Usually two mods patch the same class.",
                "该模组的 Mixin 与其它模组或游戏版本冲突。通常是两个模组修改了同一个类，或该模组与当前游戏版本不匹配。");
        put("rule.mixin_apply_alt.advice",
                "Check that the mod matches this game version, and whether another mod patches the same class.",
                "该模组的 Mixin 无法应用，请核对它是否与当前游戏版本匹配，以及是否有另一个模组改了同一个类。");
        put("rule.mixin_apply_alt.title", "Mixin injection failed", "Mixin 注入失败");
        put("rule.mixin_config.title", "Mixin configuration failed to load", "Mixin 配置加载失败");
        put("rule.mixin_config.advice",
                "The mixin config itself is bad (syntax error, or a target class that does not exist). Needs a fix or a different build from the author.",
                "Mixin 配置文件本身有问题（语法错误或目标类不存在），需要该模组作者修复或换版本。");
        put("rule.mixin_transform.title", "Mixin transformation threw", "Mixin 变换阶段异常");
        put("rule.mixin_transform.advice",
                "Two mods stepping on each other, or a mod built for another game version. Look at the first mod-owned class in the trace.",
                "Mixin 处理类时抛异常，绝大多数是两个模组冲突或模组与游戏版本不匹配。看下方堆栈里第一个属于某个模组的类。");
        put("rule.noclassdef.title", "Class not found (missing or mismatched dependency)", "类找不到（依赖缺失或版本不匹配）");
        put("rule.noclassdef.advice",
                "Something references a class that is not there: a missing prerequisite, a wrong dependency version, or a library the author forgot to bundle.",
                "有模组引用了不存在的类：可能是缺少前置、前置版本不对，或作者打包时漏了库。");
        put("rule.classnotfound.title", "Class not found (missing dependency)", "类找不到（依赖缺失）");
        put("rule.classnotfound.advice", "The mod or library providing that class is not installed.",
                "缺少提供该类的模组或库，通常是前置没装。");
        put("rule.nosuchmethod.title", "Method not found (mod built for another version)", "方法不存在（模组与游戏/前置版本不匹配）");
        put("rule.nosuchmethod.advice",
                "The mod was compiled against a different game or dependency version. Get the build that matches this one.",
                "模组是按另一个游戏版本或前置版本编译的。换成与当前游戏版本匹配的模组版本。");
        put("rule.nosuchfield.title", "Field not found (version mismatch)", "字段不存在（模组与游戏/前置版本不匹配）");
        put("rule.nosuchfield.advice", "Same as above: the mod build does not match this game or dependency version.",
                "同上：模组版本与当前游戏或前置版本不匹配。");
        put("rule.registry_missing.title", "Missing registry entries", "注册表缺少条目");
        put("rule.registry_missing.advice",
                "A mod references a block or item that is not registered. Often a mod was removed while a save still mentions it, or load order is off.",
                "某个模组引用了未注册的方块/物品。多为模组被移除但存档仍在引用，或模组加载顺序问题。");
        put("rule.registry_duplicate.title", "Registry conflict", "注册冲突");
        put("rule.registry_duplicate.advice", "Two mods registered the same id; that is a conflict between them.",
                "两个模组注册了相同的 ID，属于模组之间的冲突。");
        put("rule.datapack.title", "Data pack failed to load", "数据包加载失败");
        put("rule.datapack.advice",
                "A mod ships data files that are malformed or from another version. The JSON error right after it names the file.",
                "某个模组自带的数据文件格式错误或版本不匹配。查看紧随其后的 JSON 解析报错可定位到具体模组。");
        put("rule.resourcepack.title", "Resource pack failed to load", "资源包加载失败");
        put("rule.resourcepack.advice", "A pack or mod asset is broken; usually harmless for startup.",
                "资源包或模组素材有问题，通常不影响启动。");
        put("rule.jarjar.title", "Bundled library missing", "内嵌库缺失");
        put("rule.jarjar.advice",
                "A jar-in-jar library is gone, so the file was edited or the download is incomplete. Download it again.",
                "模组内嵌的 jar-in-jar 库丢失，说明该 jar 被改动过或下载不完整。重新下载该模组。");
        put("rule.corruptjar.title", "Mod file is corrupt", "模组文件损坏");
        put("rule.corruptjar.advice", "The jar is incomplete or damaged. Downloading it again fixes this.",
                "jar 文件下载不完整或已损坏。重新下载对应的模组文件即可。");
        put("rule.mixinextras.title", "MixinExtras out of date", "MixinExtras 版本过低");
        put("rule.mixinextras.advice", "Update NeoForge, or drop the mod that ships the old MixinExtras.",
                "升级 NeoForge 或安装了旧版 MixinExtras 的模组。");
        put("rule.oom.title", "Out of memory", "内存不足");
        put("rule.oom.advice", "Raise -Xmx (6G or more for a big pack) or load fewer mods.",
                "调高分配给游戏的 -Xmx（整合包建议 6G 以上），或减少同时加载的模组。");
        put("rule.watchdog.title", "Server frozen (a single tick took too long)", "服务器卡死（单刻耗时过长）");
        put("rule.watchdog.advice",
                "Look at the dumped thread stack: the mod in it is where it hung. OBA attributes this automatically.",
                "看下面被 dump 的线程堆栈：堆栈里出现的模组就是卡住的位置。OBA 会自动对此类卡死做归属分析。");
        put("rule.opengl.title", "Graphics driver / OpenGL problem", "图形驱动/OpenGL 问题");
        put("rule.opengl.advice", "Update the GPU driver, and check whether a mod touched the render pipeline.",
                "更新显卡驱动，或检查是否有模组改动了渲染管线。");

        // 图形类模组专项规则。钠 / iris 这一系出问题的样子和普通模组不一样
        put("rule.shader_compile.title", "Shader failed to compile", "着色器编译失败");
        put("rule.shader_compile.advice",
                "The shader pack does not match this Iris/Sodium build, or the driver rejected the GLSL. Try the pack on its own first, then update the graphics driver.",
                "光影包和当前的 iris/钠版本对不上，或者驱动拒绝了这段 GLSL。先把光影包单独试一次，再考虑更新显卡驱动。");
        put("rule.shader_pack.title", "Shader pack failed to load", "光影包加载失败");
        put("rule.shader_pack.advice",
                "The pack is incomplete, is built for another Minecraft version, or needs an option the loader does not provide.",
                "光影包不完整、是按别的游戏版本做的，或者用到了当前加载器没有提供的选项。");
        put("rule.sampler.title", "A shader sampler is missing", "着色器缺少采样器");
        put("rule.sampler.advice",
                "Almost always a shader pack written for a different Iris version. Normally harmless, but it means the pack is not fully supported.",
                "基本都是光影包按别的 iris 版本写的。通常无害，但说明这个包没有被完整支持。");
        put("rule.framebuffer.title", "Framebuffer incomplete", "帧缓冲不完整");
        put("rule.framebuffer.advice",
                "A render target could not be created, which is usually a graphics mod ordering problem or a driver limit. Black screens and missing world rendering follow from this.",
                "创建渲染目标失败，通常是图形模组之间的加载顺序问题，或者撞上了驱动限制。接着往往就是黑屏、世界渲染不出来。");
        put("rule.gfxmixin.title", "Mods are modifying each other's render classes", "模组之间在改对方的渲染类");
        put("rule.gfxmixin.advice",
                "Mods patching another graphics mod's internals is the usual source of visual glitches and render-thread crashes. Keep only one member of each family and its own add-ons.",
                "一个模组去改另一个图形模组的内部实现，是画面异常和渲染线程崩溃的常见来源。每一族只留一个分支，附加组件也要配同一支。");
        put("rule.gfxdriver.title", "Graphics driver too old", "显卡驱动过旧");
        put("rule.gfxdriver.advice",
                "Update the GPU driver. Sodium and Iris need a reasonably modern OpenGL 4.5 driver.",
                "更新显卡驱动。钠和 iris 需要比较新的 OpenGL 4.5 驱动。");
        put("rule.glfw.title", "GLFW could not start", "GLFW 起不来");
        put("rule.glfw.advice",
                "The game could not create a window or a GL context at all. This happens before mods load, so it is a driver or launcher problem rather than a mod one.",
                "游戏连窗口或 GL 上下文都建不出来。这一步发生在模组加载之前，所以是驱动或启动器的问题，不是模组的问题。");
        put("rule.gfxctx.title", "Graphics context lost", "图形上下文丢失");
        put("rule.gfxctx.advice",
                "The driver reset the device, usually from a GPU hang. Reduce shader settings, lower render distance, and check for driver updates.",
                "驱动重置了设备，通常是 GPU 挂住了。降低光影设置和渲染距离，再看看有没有驱动更新。");
        put("rule.gfxfamily.title", "Two members of the same graphics family", "同一图形族装了多个分支");
        put("rule.gfxfamily.advice",
                "Sodium, Rubidium, Embeddium and Xenon are the same renderer under different names, and so are Iris, Oculus and NeOculus. Keep exactly one of each.",
                "钠、铷、Embeddium、Xenon 换名字但干的是同一件事，iris、Oculus、NeOculus 同理。每一族只保留一个。");
        put("rule.modload.title", "Mod failed to load", "模组加载失败");
        put("rule.modload.advice", "A mod threw while being constructed; the stack below names the class and line.",
                "有个模组在构造阶段抛出了异常，看下面的堆栈即可定位到具体类和行号。");
        put("rule.modconstruct.title", "Mod construction failed", "模组实例化失败");
        put("rule.modconstruct.advice", "The mod threw inside its own constructor, which almost always means it does not match this game version.",
                "模组的构造函数里抛了异常，绝大多数情况是该模组与当前游戏版本不兼容。");

        // Sinytra Connector：在 NeoForge 上跑 Fabric 模组的桥
        put("rule.connector.title", "Sinytra Connector failed", "Sinytra Connector 出错");
        put("rule.connector.advice",
                "Connector bridges Fabric mods onto NeoForge. Check the Connector and Fabric API versions, and whether the Fabric mod in the trace supports this Minecraft version.",
                "Connector 负责让 Fabric 模组跑在 NeoForge 上。先核对 Connector 与 Fabric API 的版本，再看堆栈里那个 Fabric 模组是否支持当前游戏版本。");
        put("rule.connector_missing.title", "Fabric mod required by Connector is absent", "Connector 需要的 Fabric 模组缺失");
        put("rule.connector_missing.advice",
                "Install the Fabric mod named above, or remove the mod that depends on it.",
                "装上提示里缺失的那个 Fabric 模组，或者把依赖它的模组拿掉。");
        put("rule.connector_intermediary.title", "Intermediary names leaked into the log", "日志里出现了 Intermediary 名称");
        put("rule.connector_intermediary.advice",
                "Names like class_1234 come from Fabric's intermediary mappings. Usually a Fabric mod was loaded without Connector remapping it, or it was built for a different Minecraft version.",
                "class_1234 这类名字来自 Fabric 的 intermediary 映射。通常是某个 Fabric 模组没经过 Connector 重映射就被加载了，也可能是它按别的游戏版本编译的。");

        put("fabric.bridged", "Fabric mod, loaded through Sinytra Connector", "Fabric 模组，经 Sinytra Connector 加载");
        put("fabric.native", "Fabric mod (needs Sinytra Connector on NeoForge)", "Fabric 模组（在 NeoForge 上需要 Sinytra Connector）");
        put("note.connectorPresent",
                "Sinytra Connector is installed, so Fabric mods show up here under their own jars. Attribution works the same way, but a Fabric mod's class names may have been remapped.",
                "检测到 Sinytra Connector，报告里的 Fabric 模组会以各自的 jar 出现。归属方式不变，但 Fabric 模组的类名可能经过重映射。");

        put("note.manualCrash",
                "This is a debug crash you triggered yourself with F3+C. Nothing is broken, so there is no mod to blame; the trace is all vanilla and loader frames.",
                "这是你自己按 F3+C 触发的调试崩溃，不是模组出错，没有元凶可找——堆栈里全是原版和加载器的帧。");

        put("rule.manualcrash.title", "Debug crash triggered manually (F3+C)", "手动触发的调试崩溃（F3+C）");
        put("rule.manualcrash.advice",
                "You pressed F3+C yourself. This report is what the game produced on purpose, not a fault.",
                "是你自己按的 F3+C。这份报告是游戏按你的要求生成的，不是故障。");

        // 退出时发现的、本次运行新产生的崩溃报告
        put("report.crashFound", "Found a crash report written during this session: {0}",
                "发现本次运行期间产生的崩溃报告：{0}");
        put("report.crashFailed", "Could not read the crash report {0}: {1}",
                "读不了崩溃报告 {0}：{1}");
        put("report.reason.newCrash", "A new crash report appeared during this session",
                "本次运行期间出现了新的崩溃报告");

        // 写进 logs 目录的报告
        put("report.reason.crash", "Triggered by a captured error", "由捕获到的错误触发");
        put("report.reason.shutdown", "Written when the game exited", "游戏退出时写入");
        put("report.reason.manual", "Requested from the in-game command", "游戏内命令触发");
        put("report.written", "Report written to {0}", "报告已写入 {0}");
        put("report.writeFailed", "Could not write the report: {0}", "报告写入失败: {0}");
        put("mod.reportFolder", "Diagnosis reports go to {0}",
                "诊断报告会写到 {0}");
        put("mod.initFailed", "OBA could not start up on this version and will stay quiet: {0}",
                "OBA 在这个版本上没能启动，之后会保持安静：{0}");
        put("mod.registerFailed", "Could not register {0}, that part is disabled: {1}",
                "{0} 注册失败，对应的功能已停用：{1}");
        put("cmd.failed", "/oba {0} failed: {1}", "/oba {0} 执行失败：{1}");
        put("report.triggered", "Report saved after a captured error: {0}",
                "捕获到错误后已保存报告: {0}");

        // MCreator 特征
        put("mc.package", "Classes live under MCreator's default package: {0}",
                "类位于 MCreator 默认包名下：{0}");
        put("mc.procedures", "Has a procedures package (MCreator compiles each visual procedure into a class)",
                "包含 procedures 包（MCreator 把每个可视化“过程/流程”编译成一个类）");
        put("mc.modelement", "Has {0} *ModElement classes (MCreator's mod element registration)",
                "包含 {0} 个 *ModElement 类（MCreator 的“模组元素”注册代码）");
        put("mc.variables", "Has a variables package (MCreator's global variable system)",
                "包含 variables 包（MCreator 的全局变量系统）");
        put("mc.models", "Has {0} *Model/*Renderer classes (auto-generated entity or armour models)",
                "包含 {0} 个 *Model/*Renderer 类（MCreator 自动生成的实体/盔甲模型）");
        put("mc.registry", "Has consolidated registry classes such as ModItems/ModBlocks/ModEntities",
                "包含 ModItems/ModBlocks/ModEntities 之类的集中注册类（MCreator 生成风格）");
        put("mc.metadata", "The mod metadata mentions \"mcreator\" (name, description, author or link)",
                "neoforge.mods.toml 的元数据中出现 \"mcreator\"（名称/描述/作者/链接）");
        put("mc.filename", "The file name itself contains \"mcreator\"", "文件名本身包含 \"mcreator\"");
        put("mc.structure", "Resource layout matches a MCreator export (models paired with procedure classes)",
                "资源目录结构符合 MCreator 导出布局（models/item 与 procedures 同名成对出现）");
        put("mc.banner", "A resource file carries the MCreator banner: {0}",
                "资源文件中带有 MCreator 生成标记：{0}");
        put("mc.banner.mark", "_mcreator_ marker", "_mcreator_ 标记");
        put("mc.generatorVersion", "A resource records the generator version: {0}",
                "资源中记录了生成器版本：{0}");

        // 分析说明与证据
        put("evidence.frame.root", "a frame in the root cause chain belongs to this mod: {0}",
                "根因异常链中存在属于该模组的栈帧：{0}");
        put("evidence.frame.outer", "a frame in an outer exception chain belongs to this mod: {0}",
                "上层异常链中存在属于该模组的栈帧：{0}");
        put("evidence.rule", "matched rule \"{0}\": {1}", "命中规则「{0}」：{1}");
        put("evidence.mention", "the log text mentions this mod's id \"{0}\"",
                "日志文本中出现了该模组的 modId「{0}」");
        put("evidence.mcreator", "flagged as MCreator generated ({0} signals)",
                "该模组被识别为 MCreator 生成（{0} 项特征）");
        put("evidence.thread", "{0} thread \"{1}\" ({2}) was executing this mod's code: {3}",
                "{0}线程 \"{1}\"（{2}）正在执行该模组的代码：{3}");
        put("evidence.thread.primary", "stalled ", "卡死");
        put("evidence.thread.other", "", "");

        put("note.stall.notException", "A freeze does not throw, so this says who was running at the time, not who is certainly at fault. "
                        + "The mod at the top of that stack is the first thing to test.",
                "卡死（一次 tick 长时间未完成）不会抛出异常，因此这里给出的是「当时正在执行谁」的推断，而不是确定的错误来源。"
                        + "栈顶属于哪个模组，那个模组就是首要排查对象。");
        put("note.thread.empty", "No thread could be parsed; this does not look like a standard thread dump.",
                "没有从输入中解析出任何线程，可能不是标准线程转储格式。");
        put("note.thread.summary", "Stalled thread \"{0}\" in state {1}", "线程 \"{0}\" 停在状态 {1}");
        put("note.scanError", "Problem while reading a mod", "读取模组时出现问题");
        put("note.scanError.advice", "The file may be corrupt or locked by another process; downloading it again usually helps.",
                "该文件可能损坏或被占用；重新下载可解决。");

        put("inventory.duplicate.title", "Duplicate modId: {0}", "重复的 modId：{0}");
        put("inventory.duplicate.advice",
                "Keep one file and move the rest out of the mods folder (OBA will not touch them).",
                "只保留一个文件，其余移出 mods 目录（OBA 不会替你改动）。");
        put("inventory.duplicate.detail", "These files all declare the modId {0}: {1}",
                "以下文件都声明了 modId {0}：{1}");

        // 命令行文案
        put("cli.usage", "usage", "用法");
        put("cli.analyze", "analyse a log or crash report and name the failing mod",
                "分析日志/崩溃报告，锁定出错的模组与位置");
        put("cli.mods", "list the mods folder and check for common problems",
                "列出 mods 目录内容并检查常见问题");
        put("cli.mcreator", "only run the MCreator detection", "只做 MCreator 生成模组的识别");
        put("cli.watch", "follow the log and report problems as they happen",
                "实时监听日志，边跑边报错");
        put("cli.threads", "analyse a thread dump (freeze hunting)", "分析线程转储（卡死排查）");
        put("cli.help", "show this help", "显示本帮助");
        put("cli.autoSelected", "No file given, using: {0}", "未指定文件，自动选择: {0}");
        put("cli.noGameDir", "Could not locate the game directory; pass a log file or --game-dir.",
                "无法自动定位游戏目录，请显式指定日志文件或使用 --game-dir。");
        put("cli.noLogs", "No crash reports or logs/latest.log under {0}.", "在 {0} 下没有找到 crash-reports 或 logs/latest.log。");
        put("cli.noMods", "No mods found. Use --mods-dir to point at a mods folder.",
                "没有扫描到任何模组。用 --mods-dir 指定 mods 目录。");
        put("cli.fileMissing", "File does not exist: {0}", "文件不存在: {0}");
        put("cli.badArg", "Bad argument: {0}", "参数错误: {0}");
        put("cli.unknownCommand", "Unknown command: {0}", "未知命令: {0}");
        put("cli.readFailed", "Read failed: {0}", "读取失败: {0}");
        put("cli.internalError", "Internal error: {0}", "内部错误: {0}");
        put("cli.reportWritten", "Report written to {0}", "报告已写入 {0}");
        put("cli.version", "One BUG Among Us (OBA) {0}", "One BUG Among Us (OBA) {0}");
        put("cli.badOption", "Unknown option {0}", "未知选项 {0}");
        put("cli.optionNeedsValue", "{0} needs a value", "{0} 需要一个值");
        put("cli.usage.body", """
                One BUG Among Us (OBA) {0} - crash and error diagnostician for Minecraft (read only)

                Usage:
                  oba analyze [log or crash report...]  name the failing mod and the line
                  oba mods                              list the mods folder and check for problems
                  oba mcreator                          run only the MCreator detection
                  oba watch [latest.log]                follow the log and report as it happens
                  oba threads [dump.txt]                analyse a thread dump
                  oba help                              show this help

                Options:
                  --mods-dir <path>          mods folder (auto-detected by default)
                  --game-dir <path>          game root containing mods/ logs/ crash-reports/
                  --format text|md|json      output format (default text)
                  --output <file|dir>        also write the report to a file
                  --lang <en|zh>             report language (default: the system locale)
                  --no-color                 disable colours
                  --no-mcreator              skip the MCreator check (faster)
                  --thread <name>            stalled thread to focus on, e.g. "Server thread"
                  -q / --quiet               terse output
                  -v / --verbose             print stack traces on errors

                Exit codes: 0 clean / 2 problems found / 64 bad arguments / 70 internal error

                Examples:
                  oba analyze
                  oba analyze crash-reports/crash-2026-01-01_10.00.00-client.txt
                  oba analyze --game-dir "D:\\\\MC\\\\.minecraft" --format json --output report.json
                  oba mods --mods-dir "D:\\\\MC\\\\.minecraft\\\\mods"
                  oba watch --lang en "D:\\\\MC\\\\.minecraft\\\\logs\\\\latest.log"
                  jstack 12345 > dump.txt && oba threads dump.txt --thread "Server thread"

                The reported "failure location" looks like
                  com.example.mod.SomeClass.method (SomeClass.java:87)
                which maps straight onto the class, method and line in the author's source.
                """,
                """
                One BUG Among Us (OBA) {0} —— Minecraft 报错诊断工具（只读，不会修改任何文件）

                用法:
                  oba analyze [日志或崩溃报告...]   分析日志/崩溃报告，锁定出错的模组与位置
                  oba mods                          列出 mods 目录内容并检查常见问题
                  oba mcreator                      只做 MCreator 生成模组的识别
                  oba watch [latest.log]            实时监听日志，边跑边报错
                  oba threads [dump.txt]            分析线程转储（卡死排查）
                  oba help                          显示本帮助

                选项:
                  --mods-dir <路径>         指定 mods 目录（默认自动探测）
                  --game-dir <路径>         指定游戏根目录（含 mods/ logs/ crash-reports/）
                  --format text|md|json     输出格式（默认 text）
                  --output <文件|目录>      同时把报告写入文件
                  --lang <en|zh>            报告语言（默认跟随系统区域）
                  --no-color                关闭颜色
                  --no-mcreator             跳过 MCreator 检查（更快）
                  --thread <名称>           指定卡死线程名，如 "Server thread"
                  -q / --quiet              精简输出
                  -v / --verbose            出错时打印堆栈

                退出码: 0 正常 / 2 发现问题 / 64 参数错误 / 70 内部错误

                示例:
                  oba analyze
                  oba analyze crash-reports/crash-2026-01-01_10.00.00-client.txt
                  oba analyze --game-dir "D:\\\\MC\\\\.minecraft" --format json --output report.json
                  oba mods --mods-dir "D:\\\\MC\\\\.minecraft\\\\mods"
                  oba watch --lang en "D:\\\\MC\\\\.minecraft\\\\logs\\\\latest.log"
                  jstack 12345 > dump.txt && oba threads dump.txt --thread "Server thread"

                输出中的「出错位置」形如
                  com.example.mod.SomeClass.method (SomeClass.java:87)
                直接对应模组作者源码里的类、方法和行号。
                """);
        put("cli.inventory.header", "OBA mod inventory - {0} files, {1} classes",
                "OBA 模组清单 — {0} 个文件，{1} 个类");
        put("cli.col.file", "file", "文件");
        put("cli.col.modId", "modId", "modId");
        put("cli.col.version", "version", "版本");
        put("cli.col.classes", "classes", "类数");
        put("cli.col.notes", "notes", "备注");
        put("cli.note.mcreator", "MCreator", "MCreator生成");
        put("cli.note.fabric", "Fabric mod", "Fabric模组");
        put("cli.note.fabricBridged", "Fabric mod (Connector)", "Fabric模组（Connector）");
        put("cli.note.nested", "{0} nested jars", "内嵌{0}个jar");
        put("cli.note.readError", "read error", "读取异常");
        put("cli.issuesFound", "Found {0} problem(s):", "发现 {0} 个问题:");
        put("cli.disabled.found", "{0} file(s) in the mods folder have been renamed away:",
                "mods 目录下还有 {0} 个已被重命名禁用的文件:");
        put("cli.mcreator.checking", "Checking {0} mod files for MCreator signals...",
                "正在检查 {0} 个模组文件是否有 MCreator 特征…");
        put("cli.mcreator.found", "Detected {0} MCreator generated mod(s) out of {1} scanned:",
                "检测到 {0} 个 MCreator 生成的模组（共扫描 {1} 个）:");
        put("cli.mcreator.none", "No MCreator generated mods detected.", "没有检测到 MCreator 生成的模组。");
        put("cli.confidenceScore", "confidence {0} ({1} points)", "可信度: {0}（{1} 分）");
        put("cli.watch.started", "OBA is following {0}", "OBA 实时监听: {0}");
        put("cli.watch.indexed", "{0} mods / {1} classes indexed. Press Ctrl+C to stop.",
                "已索引 {0} 个模组 / {1} 个类。按 Ctrl+C 结束。");
        put("cli.watch.readonly", "Read only: no file is modified, disabled or deleted.",
                "只读模式：不会修改、禁用或删除任何文件。");
        put("cli.watch.issue", "Problem detected [{0}] {1}", "检测到问题 [{0}] {1}");
        put("cli.watch.raw", "raw log: {0}", "原始日志: {0}");
        put("cli.watch.means", "meaning: {0}", "说明: {0}");
        put("cli.watch.advice", "advice: {0}", "建议: {0}");
        put("cli.watch.attributed", "Attributed to: {0}  [{1}]  confidence {2} ({3}%)",
                "归属: {0}  [{1}]  可信度 {2} ({3}%)");
        put("cli.watch.location", "failure location: {0}", "出错位置: {0}");
        put("cli.watch.stopped", "Stopped. Matched {0} rule finding(s), {1} of them fatal.",
                "监听结束。共命中规则 {0} 次，其中致命 {1} 次。");
        put("cli.watch.hint", "Tip: `oba analyze` runs a full pass over a whole log.",
                "提示: 用 `oba analyze` 可以对整份日志做一次完整分析。");
        put("cli.threads.collecting", "Collecting a thread dump of this process...", "正在采集当前进程的线程转储…");
        put("cli.threads.dump", "thread dump", "线程转储");
        put("cli.threads.failed", "Could not capture a thread dump", "无法获取线程转储");
        put("cli.threads.saved", "Saved to {0}", "已保存到 {0}");
        put("cli.threads.deadlock", "Deadlocked threads: {0}", "检测到死锁线程: {0}");
        put("cli.threads.otherProcess", "To analyse another process, run `jstack <pid> > dump.txt` and then",
                "若要分析别的进程，请用 jstack <pid> > dump.txt 然后运行");
        put("cli.threads.otherProcess2", "oba threads dump.txt --mods-dir <path>",
                "oba threads dump.txt --mods-dir <path>");
        put("cli.threads.none", "No thread is running mod code.", "没有任何线程正在执行模组代码。");
        put("cli.threads.suspect", "Most suspicious: {0} [{1}] confidence {2}",
                "最可疑: {0} [{1}] 可信度 {2}");

        // 游戏内命令的反馈
        put("cmd.help.title", "One BUG Among Us (OBA) {0} - read only diagnosis (nothing is modified)",
                "One BUG Among Us (OBA) {0} —— 只读报错诊断（不会修改任何文件）");
        put("cmd.help.report", "show the most recent full diagnosis", "显示最近一次捕获到的完整诊断报告");
        put("cmd.help.status", "process and index status", "进程与索引状态");
        put("cmd.help.mcreator", "list mods that look MCreator generated", "列出被识别为 MCreator 生成的模组");
        put("cmd.help.mods", "list loaded mods and duplicate ids", "列出已加载模组与重复 modId");
        put("cmd.help.threads", "capture and attribute a thread dump", "采集线程转储并分析卡死位置");
        put("cmd.help.crash", "analyse the newest crash report on disk", "分析 crash-reports 里最新的崩溃报告");
        put("cmd.help.analyze", "analyse a specific log or crash report", "分析指定日志或崩溃报告");
        put("cmd.help.reindex", "rescan mods and rebuild the class index", "重新扫描模组并重建类索引");
        put("cmd.report.none", "Nothing captured yet. Reproduce the crash, or use /oba crash.",
                "还没有捕获到错误。可以先复现一次崩溃，或用 /oba crash 分析磁盘上的崩溃报告。");
        put("cmd.report.summary", "Culprit: {0} [{1}] @ {2}  confidence {3}",
                "元凶: {0} [{1}] @ {2}  可信度 {3}");
        put("cmd.status.title", "OBA status", "OBA 状态");
        put("cmd.status.last", "last located: {0}", "最近定位: {0}");
        put("cmd.status.gameDir", "game directory: {0}", "游戏目录: {0}");
        put("cmd.mcreator.none", "No MCreator signals in {0} mods.", "在 {0} 个模组中没有检测到 MCreator 生成的特征。");
        put("cmd.mods.loaded", "{0} mod file(s) loaded:", "已加载 {0} 个模组文件:");
        put("cmd.mods.duplicate", "[duplicate] {0}", "[重复] {0}");
        put("cmd.threads.collecting", "Collecting a thread dump...", "正在采集线程转储…");
        put("cmd.crash.none", "No crash report found in crash-reports.", "在 crash-reports 目录里没有找到崩溃报告。");
        put("cmd.analyze.missing", "File does not exist: {0}", "文件不存在: {0}");
        put("cmd.analyze.none", "Nothing attributable to a mod.", "没有可归属到模组的错误。");
        put("cmd.analyze.failed", "Read failed: {0}", "读取失败: {0}");
        put("cmd.reindex.start", "Rescanning mods...", "正在重新扫描模组…");
        put("cmd.reindex.done", "Index ready: {0} mods / {1} classes", "索引完成: {0} 个模组 / {1} 个类");
        put("cmd.index.empty", "The mod index is empty; run /oba reindex first.",
                "模组索引为空，先执行 /oba reindex。");
        put("cmd.badPath", "Indexing failed: {0}", "建立索引时出错: {0}");

        // 进程状态字段
        put("status.maxHeap", "max heap", "最大堆内存");
        put("status.usedHeap", "used heap", "已用堆内存");
        put("status.processors", "available processors", "可用处理器");
        put("status.threads", "live threads", "活动线程数");
        put("status.daemons", "daemon threads", "守护线程数");
        put("status.workingDir", "working directory", "工作目录");
        put("status.indexedMods", "mods indexed", "已索引模组");
        put("status.indexedClasses", "classes indexed", "已索引类");
        put("status.logWindow", "log window", "日志窗口");
        put("status.captured", "captured error lines", "捕获错误行");
        put("status.deadlocks", "deadlocked threads", "死锁线程");
        put("status.reportDir", "report folder", "报告目录");
        put("status.none", "none", "无");
        put("status.lines", "{0} of {1} lines", "{0} / {1} 行");
        put("status.errorLines", "{0} lines ({1} fatal)", "{0} 行（其中致命 {1} 行）");

        // 从日志表头解析出来的环境信息
        put("env.minecraft", "Minecraft / NeoForge", "Minecraft / NeoForge");
        put("env.neoforge", "NeoForge", "NeoForge");
        put("env.java", "Java", "Java");
        put("env.os", "OS", "操作系统");
        put("env.memory", "max memory", "最大内存");
        put("env.cpu", "CPU", "CPU");

        // 读模组文件时的错误
        put("jar.unreadable", "cannot read {0}: {1}", "无法读取 {0}: {1}");
        put("jar.walkFailed", "could not walk directory {0}: {1}", "遍历目录失败 {0}: {1}");
        put("jar.nestedFailed", "could not read nested jar {0}: {1}", "读取内嵌 jar 失败 {0}: {1}");
        put("jar.openFailed", "could not open jar {0}: {1}", "无法打开 jar {0}: {1}");
        put("jar.nestedParseFailed", "could not parse the nested jar descriptor {0}: {1}",
                "解析内嵌 jar 描述文件失败 {0}: {1}");
        put("jar.readFailed", "could not read {0}: {1}", "读取 {0} 失败: {1}");
        put("jar.parseFailed", "{0} could not be parsed: {1}", "{0} 解析失败: {1}");
        put("jar.noModsSection", "no [[mods]] section in {0}", "{0} 中没有 [[mods]] 段");
        put("jar.missingModId", "a [[mods]] entry in {0} has no modId", "{0} 中某个 [[mods]] 缺少 modId");
        put("jar.fabricNoId", "fabric.mod.json in {0} has no id", "{0} 里的 fabric.mod.json 没有 id");

        // Mod 往日志里的输出
        put("mod.enabled", "enabled: {0} (read only; no mod file is ever modified)",
                "已启用：{0}（只读诊断，不会修改任何模组文件）");
        put("mod.helpHint", "type /oba help for the available commands",
                "输入 /oba help 查看可用命令");
        put("mod.indexing", "Indexing mods...", "正在建立模组索引…");
        put("mod.indexDone", "Index ready: {0} mod files / {1} classes / {2} packages ({3} ms)",
                "索引完成：{0} 个模组文件 / {1} 个类 / {2} 个包（耗时 {3} ms）");
        put("mod.indexFailed", "Could not read the FML mod list, falling back to scanning the mods folder: {0}",
                "读取 FML 模组列表失败，改用 mods 目录扫描: {0}");
        put("mod.locate", "Culprit: {0}  [{1}]", "定位到出错模组: {0}  [{1}]");
        put("mod.location", "Failure location: {0}", "出错位置: {0}");
        put("mod.confidence", "Confidence: {0} ({1}%)", "可信度: {0} ({1}%)");
        put("mod.exception", "Exception: {0}", "异常: {0}");
        put("mod.reportHint", "Run /oba report for the full report (read only, nothing is changed)",
                "输入 /oba report 查看完整报告（只读，不会改动任何文件）");
        put("mod.capturedNoOwner", "Captured an error, but no frame belonged to a mod. Most relevant finding: {0} - {1}",
                "捕获到错误，但没有栈帧能归属到某个模组。最相关的问题: {0} — {1}");
        put("mod.analysisFailed", "Analysis failed: {0}", "分析失败: {0}");
        put("mod.tailingStarted", "Now following the log file: {0}", "已开始监听日志文件: {0}");
        put("mod.tailingFailed", "Could not follow the log file: {0}", "监听日志文件失败: {0}");
        put("mod.readFailed", "Could not read the crash report: {0}", "读取崩溃报告失败: {0}");
        put("mod.appenderFailed", "Could not attach the log collector ({0}); falling back to file watching",
                "无法挂载日志采集器（{0}），将退回文件监听模式");
        put("mod.appenderNotStarted", "The log collector did not start; falling back to file watching",
                "日志采集器未能启动，实时监听将退回文件模式");
        put("mod.previousCrash", "The last crash report blames {0} [{1}] at {2}",
                "上次崩溃报告显示元凶是 {0} [{1}]，位置 {2}");
        put("mod.noErrors", "No error was captured this session.", "本次会话未捕获到错误。");
        put("mod.sessionSummary", "Captured {0} error lines this session ({1} fatal).",
                "本次会话共捕获 {0} 行错误（致命 {1} 行）。");
        put("mod.memoryWarn", "Memory warning: {0} MB used of {1} MB ({2}%)",
                "内存告警: 已用 {0} MB / 上限 {1} MB ({2}%)");
        put("mod.lagWarn", "Lag detected: {0} s since the last tick", "检测到卡顿: 距上次 tick 已 {0} 秒");
        put("mod.stall", "The server thread has been stuck for {0} s (episode {1})",
                "服务器线程卡住 {0} 秒（第 {1} 次）");
        put("mod.stall.deadlock", "Thread deadlock detected", "检测到线程死锁");
        put("mod.stall.executing", "At that moment it was running: {0}  [{1}]",
                "卡住时正在执行: {0}  [{1}]");
        put("mod.stall.location", "Location: {0}", "位置: {0}");
        put("mod.stall.threadsHint", "Run /oba threads for the full thread attribution",
                "输入 /oba threads 查看完整线程归属分析");
        put("mod.stall.notModCode", ", and no thread was running mod code (vanilla or GC, most likely)",
                "，且没有线程执行模组代码（可能是原版或 GC 问题）");
        put("mod.stall.failed", "Stall analysis failed: {0}", "卡死分析失败: {0}");
        put("mod.thread.none", "No thread is running mod code.", "没有任何线程正在执行模组代码。");
        put("mod.thread.suspect", "Most suspicious: {0} [{1}] confidence {2}",
                "最可疑: {0} [{1}] 可信度 {2}");
    }
}
