// /oba 命令树。所有子命令只读不写，没有任何能禁用模组的操作。
//
// 聊天栏这块的观感统一走 GameUi：标题条、分段线、可点的菜单项都从那里来，
// 这里只负责把数据摆进去。带颜色标记的地方点一下就能执行对应命令。
package dev.oba.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.oba.core.Analyzer;
import dev.oba.core.Diagnosis;
import dev.oba.core.Lang;
import dev.oba.core.McreatorDetector;
import dev.oba.core.ModSource;
import dev.oba.core.Oba;
import dev.oba.core.Reports;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ObaCommands {

    // 聊天栏一次别刷太多，超了就指到报告文件
    private static final int INFO_LIMIT = 22;
    private static final int REPORT_LIMIT = 60;

    private final Diagnostics diagnostics;

    public ObaCommands(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    // 由各轨道的入口把事件里的 dispatcher 转进来。
    // 这样这个方法本身不依赖任何加载器的类，Forge 和 NeoForge 共用
    public void onRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 每个子命令都套一层保护。命令树本身要注册成功，
        // 但里面某个命令因为版本差异出错时，只该那一条不能用，
        // 不能让玩家敲一下就吃一个原始堆栈
        dispatcher.register(Commands.literal("oba")
                .executes(ctx -> guard(ctx.getSource(), "help", () -> help(ctx.getSource())))
                .then(Commands.literal("help")
                        .executes(ctx -> guard(ctx.getSource(), "help", () -> help(ctx.getSource()))))
                .then(Commands.literal("report")
                        .executes(ctx -> guard(ctx.getSource(), "report", () -> report(ctx.getSource()))))
                .then(Commands.literal("status")
                        .executes(ctx -> guard(ctx.getSource(), "status", () -> status(ctx.getSource()))))
                .then(Commands.literal("mcreator")
                        .executes(ctx -> guard(ctx.getSource(), "mcreator", () -> mcreator(ctx.getSource()))))
                .then(Commands.literal("mods")
                        .executes(ctx -> guard(ctx.getSource(), "mods", () -> mods(ctx.getSource()))))
                .then(Commands.literal("info")
                        .then(Commands.argument("mod", StringArgumentType.greedyString())
                                .executes(ctx -> guard(ctx.getSource(), "info",
                                        () -> info(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mod"))))))
                .then(Commands.literal("threads")
                        .executes(ctx -> guard(ctx.getSource(), "threads", () -> threads(ctx.getSource()))))
                .then(Commands.literal("crash")
                        .executes(ctx -> guard(ctx.getSource(), "crash", () -> crash(ctx.getSource()))))
                .then(Commands.literal("reindex")
                        .executes(ctx -> guard(ctx.getSource(), "reindex", () -> reindex(ctx.getSource()))))
                .then(Commands.literal("analyze")
                        .then(Commands.argument("file", StringArgumentType.greedyString())
                                .executes(ctx -> guard(ctx.getSource(), "analyze",
                                        () -> analyze(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "file")))))
                        // 不带文件名时分析最近一次崩到窗口里的东西，方便做成可点的
                        .executes(ctx -> guard(ctx.getSource(), "analyze",
                                () -> analyzeNewest(ctx.getSource())))));
    }

    // 跑一个子命令，出错就把原因回给玩家，别抛出去
    private int guard(CommandSourceStack src, String what, java.util.function.IntSupplier body) {
        try {
            return body.getAsInt();
        } catch (Throwable t) {
            src.sendFailure(Component.literal(Oba.LOG_PREFIX
                    + Lang.get("cmd.failed", what, String.valueOf(t))));
            return 0;
        }
    }

    // 各个子命令

    // 主菜单。标题条和终端里的那一套一样，下面每一项点一下就执行
    private int help(CommandSourceStack src) {
        send(src, GameUi.header(Lang.get("cmd.help.title.short", Oba.NAME)));

        List<Component> menu = new ArrayList<>();
        menu.add(Component.empty());
        menu.add(GameUi.section(Lang.get("menu.section")));
        menu.add(Component.empty());
        // 推荐项做成反白条：它既是第 1 项，本身也点得动，
        // 所以下面从 [2] 接着编号，不再重复列一遍
        menu.add(GameUi.click(GameUi.bar("▶ [1] " + Lang.get("gameui.action.report")), "/oba report",
                Lang.get("gameui.click", "/oba report")));
        menu.add(Component.empty());
        menu.add(item(2, "status", Lang.get("gameui.action.status"), false));
        menu.add(item(3, "mods", Lang.get("gameui.action.mods"), false));
        menu.add(item(4, "mcreator", Lang.get("gameui.action.mcreator"), false));
        menu.add(item(5, "threads", Lang.get("gameui.action.threads"), false));
        menu.add(item(6, "crash", Lang.get("gameui.action.crash"), false));
        menu.add(item(7, "reindex", Lang.get("gameui.action.reindex"), false));
        menu.add(Component.empty());
        menu.add(GameUi.rule());
        menu.add(Component.empty());
        menu.add(GameUi.section(Lang.get("cmd.help.more")));
        menu.add(Component.empty());
        // 需要再带参数的这两条做成往聊天框里填，填完自己补名字
        menu.add(Component.empty()
                .append(GameUi.key("  "))
                .append(GameUi.suggest(GameUi.accent("/oba info <id>"), "/oba info "))
                .append(GameUi.key("  " + Lang.get("gameui.action.info"))));
        menu.add(Component.empty()
                .append(GameUi.key("  "))
                .append(GameUi.suggest(GameUi.accent("/oba analyze <file>"), "/oba analyze \""))
                .append(GameUi.key("  " + Lang.get("gameui.action.analyze"))));
        menu.add(Component.empty());
        menu.add(GameUi.hint(GameUi.key(Lang.get("gameui.tip.clickable")), null));
        menu.add(GameUi.url(GameUi.key("  " + GameUi.WIKI), GameUi.WIKI));
        send(src, menu);
        return 1;
    }

    // 主菜单里的一行
    private Component item(int index, String command, String label, boolean highlight) {
        return GameUi.action(index, label, Lang.get("menu.hint.run"), "/oba " + command, highlight);
    }

    private int report(CommandSourceStack src) {
        Optional<Diagnosis.Report> report = diagnostics.lastReport();
        if (report.isEmpty()) {
            // 先试试磁盘上的崩溃报告，实在没有再说
            Optional<Diagnosis.Report> fromDisk = diagnostics.analyzeNewestCrashReport();
            if (fromDisk.isEmpty()) {
                send(src, GameUi.warn("  " + Lang.get("cmd.report.none")));
                return 0;
            }
            report = fromDisk;
        }
        verdict(src, report.get());
        sendReport(src, Reports.text(report.get(), true));
        return 1;
    }

    private int analyzeNewest(CommandSourceStack src) {
        Optional<Diagnosis.Report> report = diagnostics.analyzeNewestCrashReport();
        if (report.isEmpty()) {
            send(src, GameUi.warn("  " + Lang.get("cmd.crash.none")));
            return 0;
        }
        verdict(src, report.get());
        sendReport(src, Reports.text(report.get(), true));
        return 1;
    }

    private int status(CommandSourceStack src) {
        send(src, GameUi.header(Lang.get("cmd.status.title")));
        List<Component> body = new ArrayList<>();
        body.add(Component.empty());
        body.add(GameUi.section(Lang.get("menu.label.status")));
        body.add(Component.empty());
        for (Map.Entry<String, String> e : diagnostics.statusFacts().entrySet()) {
            body.add(GameUi.row(e.getKey(), e.getValue(), 12, true));
        }
        body.add(GameUi.row(Lang.get("gameui.status.records"),
                Lang.get("gameui.status.records.value",
                        diagnostics.capturedErrors() + diagnostics.capturedFatals(),
                        diagnostics.windowSize()), 12, true));
        if (!diagnostics.lastCrashSummary().isBlank()) {
            body.add(GameUi.row(Lang.get("cmd.status.last"), diagnostics.lastCrashSummary(), 12, false));
        }
        body.add(Component.empty());
        body.add(GameUi.section(Lang.get("menu.label.workspace")));
        body.add(Component.empty());
        body.add(Component.empty()
                .append(GameUi.key("  " + GameUi.padPx(Lang.get("cmd.status.gameDir"), 72) + "  "))
                .append(GameUi.path(diagnostics.resolveGameDir().toString())));
        body.add(Component.empty());
        body.add(GameUi.hint(GameUi.accent("  [1] " + Lang.get("gameui.action.report")), "/oba report"));
        body.add(GameUi.hint(GameUi.accent("  [2] " + Lang.get("gameui.action.reindex")), "/oba reindex"));
        send(src, body);
        return 1;
    }

    private int mcreator(CommandSourceStack src) {
        List<ModSource> sources = diagnostics.sources();
        if (sources.isEmpty()) {
            send(src, GameUi.warn("  " + Lang.get("cmd.index.empty")));
            return 0;
        }
        List<Diagnosis.McreatorHit> hits = new ArrayList<>();
        for (ModSource s : sources) {
            McreatorDetector.detect(s).ifPresent(hits::add);
        }
        hits.sort((a, b) -> Integer.compare(b.score(), a.score()));
        send(src, GameUi.header(Lang.get("cmd.mcreator.title")));
        List<Component> body = new ArrayList<>();
        body.add(Component.empty());
        if (hits.isEmpty()) {
            body.add(GameUi.ok("  ● " + Lang.get("cli.mcreator.none") + " (" + sources.size() + ")"));
            send(src, body);
            return 1;
        }
        body.add(GameUi.accent("  ● " + Lang.get("cli.mcreator.found", hits.size(), sources.size())));
        body.add(Component.empty());
        for (Diagnosis.McreatorHit hit : hits) {
            String id = hit.source().modIds().isEmpty() ? "" : hit.source().modIds().get(0);
            MutableComponent head = Component.empty()
                    .append(GameUi.bad("  ▶ "))
                    .append(GameUi.t(hit.label(), GameUi.NAME))
                    .append(GameUi.key("  [" + hit.source().fileName() + "]  "))
                    .append(GameUi.warn(Lang.get("cli.confidenceScore",
                            hit.confidence().label(), hit.score())));
            body.add(id.isBlank() ? head : GameUi.click(head, "/oba info " + id,
                    Lang.get("gameui.click", "/oba info " + id)));
            for (Diagnosis.Evidence e : hit.reasons()) {
                body.add(GameUi.key("      - " + e.detail()));
            }
        }
        body.add(Component.empty());
        body.add(GameUi.key("  " + Lang.get("mcreator.note")));
        send(src, body);
        return 1;
    }

    private int mods(CommandSourceStack src) {
        List<ModSource> sources = diagnostics.sources();
        if (sources.isEmpty()) {
            send(src, GameUi.warn("  " + Lang.get("cmd.index.empty")));
            return 0;
        }
        send(src, GameUi.header(Lang.get("cmd.mods.title")));
        List<Component> body = new ArrayList<>();
        body.add(Component.empty());
        body.add(GameUi.ok("  ● " + Lang.get("cmd.mods.loaded", sources.size())));
        body.add(GameUi.key("  " + Lang.get("cmd.info.hint")));
        body.add(Component.empty());
        for (ModSource s : sources) {
            String id = s.modIds().isEmpty() ? "" : s.modIds().get(0);
            // 模组名做成可点的：点一下就展开这个模组的简介和元数据。
            // 聊天栏一行放不下简介，所以这里给的是入口而不是正文
            MutableComponent line = Component.empty()
                    .append(GameUi.bad("  ▶ "))
                    .append(GameUi.t(truncate(s.display(), 26), GameUi.NAME))
                    .append(GameUi.key("  " + truncate(id, 26)))
                    .append(GameUi.key("  " + truncate(s.version(), 12)));
            body.add(id.isBlank() ? line : GameUi.click(line, "/oba info " + id,
                    Lang.get("cmd.info.hint")));
        }
        List<Diagnosis.KnownIssue> dups = Analyzer.findDuplicates(sources);
        if (!dups.isEmpty()) {
            body.add(Component.empty());
            for (Diagnosis.KnownIssue dup : dups) {
                body.add(GameUi.warn("  ○ " + Lang.get("cmd.mods.duplicate", dup.detail())));
            }
        }
        send(src, body);
        return 1;
    }

    // 单个模组的简介。
    // 聊天栏不适合刷屏，所以这里是有取舍的：先给元数据里那段简介，
    // 再给能用来判断的东西（版本、来源、类数量、SHA-1、重复情况、
    // 是不是 MCreator、属于哪个图形谱系），最后才提已知问题。
    private int info(CommandSourceStack src, String wanted) {
        List<ModSource> sources = diagnostics.sources();
        ModSource found = null;
        for (ModSource s : sources) {
            for (String id : s.modIds()) {
                if (id.equalsIgnoreCase(wanted)) {
                    found = s;
                    break;
                }
            }
            if (found == null && s.fileName().equalsIgnoreCase(wanted)) {
                found = s;
            }
            if (found != null) {
                break;
            }
        }
        if (found == null) {
            send(src, GameUi.warn(Oba.LOG_PREFIX + Lang.get("cmd.info.unknown", wanted)));
            return 0;
        }

        ModSource s = found;
        String id = s.modIds().isEmpty() ? "?" : String.join(", ", s.modIds());
        send(src, GameUi.header(truncate(s.display(), 40)));
        List<Component> body = new ArrayList<>();
        body.add(Component.empty());
        body.add(GameUi.accent("  " + Lang.get("cmd.info.id", id)));
        body.add(Component.empty());

        // 简介正文：优先用元数据里的描述，没有就直说
        String desc = s.description();
        if (desc == null || desc.isBlank()) {
            body.add(GameUi.key("  " + Lang.get("cmd.info.none")));
        } else {
            for (String wrapped : GameUi.wrap(desc.replace('\n', ' '), GameUi.LINE_PX - GameUi.px("  "))) {
                body.add(GameUi.value("  " + wrapped));
            }
        }

        body.add(Component.empty());
        body.add(GameUi.section(Lang.get("cmd.info.meta")));
        body.add(Component.empty());
        body.add(GameUi.pair(Lang.get("cmd.info.file"), s.fileName(), 10));
        body.add(GameUi.pair(Lang.get("cmd.info.version"), s.version(), 10));
        if (!s.authors().isBlank()) {
            body.add(GameUi.pair(Lang.get("cmd.info.author"), s.authors(), 10));
        }
        if (!s.license().isBlank()) {
            body.add(GameUi.pair(Lang.get("cmd.info.license"), s.license(), 10));
        }
        if (!s.loader().isBlank()) {
            body.add(GameUi.pair(Lang.get("cmd.info.loader"), s.loader(), 10));
        }
        body.add(GameUi.pair(Lang.get("cmd.info.classes"), String.valueOf(s.classNames().size()), 10));
        body.add(GameUi.pair(Lang.get("cmd.info.size"), humanSize(s.size()), 10));
        body.add(GameUi.pair(Lang.get("cmd.info.sha1"), s.sha1(), 10));

        // 路径做成可点的，点一下填进聊天框，省得手抄
        body.add(Component.empty()
                .append(GameUi.key("  " + GameUi.padPx(Lang.get("cmd.info.path"), 60) + "  "))
                .append(GameUi.path(s.path().toString())));

        // 重复声明同一个 modId 的文件，这个很值得当场看到
        int dupes = 0;
        for (ModSource other : sources) {
            if (other != s && !java.util.Collections.disjoint(other.modIds(), s.modIds())) {
                dupes++;
            }
        }
        if (dupes > 0) {
            body.add(Component.empty());
            body.add(GameUi.bad("  ✗ " + Lang.get("cmd.info.duplicate", dupes)));
        }

        // MCreator 判定结果
        Diagnosis.McreatorHit mcreator = McreatorDetector.detect(s).orElse(null);
        if (mcreator != null) {
            body.add(GameUi.accent("  ◆ " + Lang.get("cmd.info.mcreator", mcreator.score())));
        }

        // 图形谱系
        for (dev.oba.core.GraphicsMods.Family f : dev.oba.core.GraphicsMods.familiesOf(s)) {
            dev.oba.core.GraphicsMods.Member m =
                    dev.oba.core.GraphicsMods.memberOf(id.split(",")[0].trim());
            String role = m == null ? "" : roleLabel(m.role());
            body.add(GameUi.accent("  ◆ " + Lang.get("cmd.info.graphics", familyLabel(f.key()), role)));
            if (m != null && !m.note().isBlank()) {
                for (String wrapped : GameUi.wrap(m.note(), GameUi.LINE_PX - GameUi.px("      "))) {
                    body.add(GameUi.key("      " + wrapped));
                }
            }
        }

        // 已知问题
        List<String> issues = new ArrayList<>();
        for (Diagnosis.GraphicsIssue gi : diagnostics.graphicsIssuesFor(s)) {
            boolean conflict = gi.kind() == Diagnosis.GraphicsIssue.Kind.FAMILY_CONFLICT;
            issues.add(conflict
                    ? Lang.get("gfx.issue.conflict.title", familyLabel(gi.canonicalId()))
                    : Lang.get("gfx.issue.missing.title", gi.subject()));
        }
        if (!issues.isEmpty()) {
            body.add(Component.empty());
            body.add(GameUi.warn("  ○ " + Lang.get("cmd.info.issues", issues.size())));
            for (String i : issues) {
                body.add(GameUi.warn("    - " + i));
            }
        }

        if (body.size() > INFO_LIMIT) {
            int rest = body.size() - INFO_LIMIT;
            body = new ArrayList<>(body.subList(0, INFO_LIMIT));
            body.add(Component.empty());
            body.add(GameUi.key("  " + Lang.get("gameui.report.more", rest)));
            body.add(GameUi.hint(GameUi.accent("  [1] " + Lang.get("gameui.action.report")), "/oba report"));
        }
        send(src, body);
        return 1;
    }

    private static String roleLabel(dev.oba.core.GraphicsMods.Role r) {
        return switch (r) {
            case RENDER_OPTIMIZER -> Lang.get("gfx.role.optimizer");
            case SHADER_LOADER -> Lang.get("gfx.role.shader");
            case RENDER_ADDON -> Lang.get("gfx.role.addon");
            case OTHER_OPTIMIZER -> Lang.get("gfx.role.other");
        };
    }

    private static String familyLabel(String key) {
        return switch (key == null ? "" : key.toLowerCase(Locale.ROOT)) {
            case "sodium" -> Lang.get("gfx.family.sodium");
            case "iris" -> Lang.get("gfx.family.iris");
            case "lithium" -> Lang.get("gfx.family.lithium");
            case "krypton" -> Lang.get("gfx.family.krypton");
            case "phosphor" -> Lang.get("gfx.family.phosphor");
            default -> key == null ? "" : key;
        };
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0);
    }

    private int threads(CommandSourceStack src) {
        send(src, GameUi.key("  " + Lang.get("cmd.threads.collecting")));
        String dump = dev.oba.core.ProcessProbe.captureThreadDump(false);
        Diagnosis.Report report = dev.oba.core.ProcessProbe.analyzeThreadDump(
                dump, diagnostics.index(), diagnostics.byModId(), null);
        report.scannedMods().clear();
        report.scannedMods().addAll(diagnostics.sources());
        report.setTitleKey("report.title.threads");
        verdict(src, report);
        sendReport(src, Reports.text(report, true));
        return 1;
    }

    private int crash(CommandSourceStack src) {
        Optional<Diagnosis.Report> report = diagnostics.analyzeNewestCrashReport();
        if (report.isEmpty()) {
            send(src, GameUi.warn("  " + Lang.get("cmd.crash.none")));
            return 0;
        }
        verdict(src, report.get());
        sendReport(src, Reports.text(report.get(), true));
        return 1;
    }

    private int analyze(CommandSourceStack src, String fileArg) {
        String cleaned = fileArg.trim().replace("\"", "");
        Path requested = Paths.get(cleaned);
        final Path path = requested.isAbsolute()
                ? requested
                : diagnostics.resolveGameDir().resolve(cleaned);
        if (!Files.isRegularFile(path)) {
            send(src, GameUi.warn("  " + Lang.get("cmd.analyze.missing", path)));
            return 0;
        }
        try {
            Diagnosis.Report report = diagnostics.analyzeFile(path);
            verdict(src, report);
            if (report.primarySuspect() == null) {
                send(src, GameUi.ok("  ● " + Lang.get("cmd.analyze.none")));
            }
            sendReport(src, Reports.text(report, true));
            return 1;
        } catch (IOException e) {
            send(src, GameUi.bad("  ✗ " + Lang.get("cmd.analyze.failed", e.getMessage())));
            return 0;
        }
    }

    private int reindex(CommandSourceStack src) {
        send(src, GameUi.key("  " + Lang.get("cmd.reindex.start")));
        Diagnostics d = diagnostics;
        d.reindex(true, () -> src.sendSuccess(() -> GameUi.ok("  ● "
                + Lang.get("cmd.reindex.done", d.sources().size(), d.index().classCountTotal())), false));
        return 1;
    }

    // 辅助方法

    // 结论那一条。游戏里反白画不出来，所以用一条红杠把元凶顶出来
    private void verdict(CommandSourceStack src, Diagnosis.Report report) {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        Diagnosis.Finding primary = report.primarySuspect();
        if (primary == null) {
            out.add(GameUi.ok("  ● " + Lang.get("verdict.none")));
            out.add(GameUi.key("    " + Lang.get("verdict.none.hint")));
            send(src, out);
            return;
        }
        out.add(GameUi.bar(Lang.get("verdict.culprit") + "  " + primary.display()));
        out.add(Component.empty());
        if (primary.source() != null) {
            out.add(GameUi.pair(Lang.get("verdict.modFile"), primary.source().fileName(), 10));
            if (!primary.source().version().isBlank()) {
                out.add(GameUi.pair(Lang.get("verdict.version"), primary.source().version(), 10));
            }
            out.add(GameUi.pair(Lang.get("verdict.sha1"), primary.source().sha1(), 10));
        }
        out.add(GameUi.pair(Lang.get("verdict.confidence"),
                report.confidenceText(primary.score()), 10));
        dev.oba.core.StackTraceParser.AttributedFrame loc = primary.firstLocatedFrame();
        if (loc != null) {
            out.add(Component.empty()
                    .append(GameUi.key("  " + GameUi.padPx(Lang.get("verdict.location"), 60) + "  "))
                    .append(GameUi.warn(loc.location())));
        }
        if (!primary.throwables().isEmpty()) {
            out.add(GameUi.pair(Lang.get("verdict.exception"), primary.throwables().get(0), 10));
        }
        out.add(Component.empty());
        if (primary.source() != null) {
            out.add(GameUi.warn("  → " + Lang.get("gameui.advice", primary.source().fileName())));
        }
        send(src, out);
    }

    // 报告正文。太长就截断，剩下的指到文件
    private void sendReport(CommandSourceStack src, String text) {
        List<Component> lines = GameUi.lines(text);
        List<Component> out = new ArrayList<>();
        out.add(Component.empty());
        out.add(GameUi.rule());
        if (lines.size() > REPORT_LIMIT) {
            out.addAll(lines.subList(0, REPORT_LIMIT));
            out.add(Component.empty());
            out.add(GameUi.key("  " + Lang.get("gameui.report.more", lines.size() - REPORT_LIMIT)));
        } else {
            out.addAll(lines);
        }
        Path written = diagnostics.reportDirectory();
        out.add(Component.empty());
        // 报告都落在 logs 里，把目录摆出来让玩家自己能找到文件
        out.add(Component.empty()
                .append(GameUi.key("  " + GameUi.padPx(Lang.get("cmd.report.dir"), 60) + "  "))
                .append(GameUi.path(written.toString())));
        send(src, out);
    }

    private void send(CommandSourceStack src, Component line) {
        List<Component> one = new ArrayList<>(1);
        one.add(line);
        send(src, one);
    }

    private void send(CommandSourceStack src, List<Component> lines) {
        for (Component line : lines) {
            src.sendSuccess(() -> line, false);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
