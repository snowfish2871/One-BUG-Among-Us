// /oba 命令树。所有子命令只读不写，没有任何能禁用模组的操作。
package dev.oba.mod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.oba.core.Analyzer;
import dev.oba.core.Diagnosis;
import dev.oba.core.Lang;
import dev.oba.core.McreatorDetector;
import dev.oba.core.Oba;
import dev.oba.core.ModSource;
import dev.oba.core.Reports;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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

    private final Diagnostics diagnostics;

    public ObaCommands(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
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
                                                StringArgumentType.getString(ctx, "file")))))));
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

    private int help(CommandSourceStack src) {
        String[] lines = {
                Lang.get("cmd.help.title", Oba.VERSION),
                "/oba report    " + Lang.get("cmd.help.report"),
                "/oba status    " + Lang.get("cmd.help.status"),
                "/oba mcreator  " + Lang.get("cmd.help.mcreator"),
                "/oba mods      " + Lang.get("cmd.help.mods"),
                "/oba info <id> " + Lang.get("cmd.help.info"),
                "/oba threads   " + Lang.get("cmd.help.threads"),
                "/oba crash     " + Lang.get("cmd.help.crash"),
                "/oba analyze <file>  " + Lang.get("cmd.help.analyze"),
                "/oba reindex   " + Lang.get("cmd.help.reindex"),
        };
        for (String l : lines) {
            src.sendSuccess(() -> Component.literal(l), false);
        }
        return 1;
    }

    private int report(CommandSourceStack src) {
        Optional<Diagnosis.Report> report = diagnostics.lastReport();
        if (report.isEmpty()) {
            // 先试试磁盘上的崩溃报告，实在没有再说
            Optional<Diagnosis.Report> fromDisk = diagnostics.analyzeNewestCrashReport();
            if (fromDisk.isEmpty()) {
                src.sendSuccess(() -> Component.literal(Lang.get("cmd.report.none")), false);
                return 0;
            }
            report = fromDisk;
        }
        Diagnosis.Finding primary = report.get().primarySuspect();
        if (primary != null) {
            String summary = Lang.get("cmd.report.summary", primary.display(), primary.fileName(),
                    primary.firstLocatedFrame() == null ? "?" : primary.firstLocatedFrame().location(),
                    primary.confidence().label());
            src.sendSuccess(() -> Component.literal(summary), false);
        }
        sendLong(src, Reports.text(report.get(), false));
        return 1;
    }

    private int status(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(Lang.get("cmd.status.title")), false);
        for (Map.Entry<String, String> e : diagnostics.statusFacts().entrySet()) {
            src.sendSuccess(() -> Component.literal("  " + e.getKey() + ": " + e.getValue()), false);
        }
        if (!diagnostics.lastCrashSummary().isBlank()) {
            src.sendSuccess(() -> Component.literal("  " + Lang.get("cmd.status.last", diagnostics.lastCrashSummary())), false);
        }
        src.sendSuccess(() -> Component.literal("  " + Lang.get("cmd.status.gameDir", diagnostics.resolveGameDir())), false);
        return 1;
    }

    private int mcreator(CommandSourceStack src) {
        List<ModSource> sources = diagnostics.sources();
        if (sources.isEmpty()) {
            src.sendSuccess(() -> Component.literal(Lang.get("cmd.index.empty")), false);
            return 0;
        }
        List<Diagnosis.McreatorHit> hits = new ArrayList<>();
        for (ModSource s : sources) {
            McreatorDetector.detect(s).ifPresent(hits::add);
        }
        hits.sort((a, b) -> Integer.compare(b.score(), a.score()));
        if (hits.isEmpty()) {
            src.sendSuccess(() -> Component.literal(
                    Lang.get("cli.mcreator.none") + " (" + sources.size() + ")"), false);
            return 1;
        }
        src.sendSuccess(() -> Component.literal(
                Lang.get("cli.mcreator.found", hits.size(), sources.size())), false);
        for (Diagnosis.McreatorHit hit : hits) {
            src.sendSuccess(() -> Component.literal(
                    "  * " + hit.label() + "  [" + hit.source().fileName() + "]  "
                            + Lang.get("cli.confidenceScore", hit.confidence().label(), hit.score())), false);
            for (Diagnosis.Evidence e : hit.reasons()) {
                src.sendSuccess(() -> Component.literal("      - " + e.detail()), false);
            }
        }
        src.sendSuccess(() -> Component.literal(Lang.get("mcreator.note")), false);
        return 1;
    }

    private int mods(CommandSourceStack src) {
        List<ModSource> sources = diagnostics.sources();
        if (sources.isEmpty()) {
            src.sendSuccess(() -> Component.literal(Lang.get("cmd.index.empty")), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal(Lang.get("cmd.mods.loaded", sources.size())), false);
        for (ModSource s : sources) {
            // 模组名做成可点的：点一下就展开这个模组的简介和元数据。
            // 聊天栏一行放不下简介，所以这里给的是入口而不是正文
            String id = s.modIds().isEmpty() ? "" : s.modIds().get(0);
            String head = String.format(Locale.ROOT, "  %-38s %-20s %-10s %4d",
                    truncate(s.fileName(), 38),
                    truncate(String.join(",", s.modIds()), 20),
                    truncate(s.version(), 10),
                    s.classNames().size());
            Component line = id.isBlank()
                    ? Component.literal(head)
                    : Component.literal(head).withStyle(st -> st
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/oba info " + id))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal(Lang.get("cmd.info.hint")))));
            src.sendSuccess(() -> line, false);
        }
        List<Diagnosis.KnownIssue> dups = Analyzer.findDuplicates(sources);
        if (!dups.isEmpty()) {
            for (Diagnosis.KnownIssue dup : dups) {
                src.sendSuccess(() -> Component.literal("  " + Lang.get("cmd.mods.duplicate", dup.detail())), false);
            }
        }
        src.sendSuccess(() -> Component.literal(Lang.get("cmd.info.hint")), false);
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
            String msg = Lang.get("cmd.info.unknown", wanted);
            src.sendSuccess(() -> Component.literal(Oba.LOG_PREFIX + msg), false);
            return 0;
        }

        ModSource s = found;
        List<String> lines = new ArrayList<>();
        String id = s.modIds().isEmpty() ? "?" : String.join(", ", s.modIds());
        lines.add(Lang.get("cmd.info.title", s.display(), id));

        // 简介正文：优先用元数据里的描述，没有就直说
        String desc = s.description();
        if (desc == null || desc.isBlank()) {
            lines.add("  " + Lang.get("cmd.info.none"));
        } else {
            for (String wrapped : wrap(desc, 68)) {
                lines.add("  " + wrapped);
            }
        }

        lines.add("");
        lines.add("  " + field(Lang.get("cmd.info.file"), s.fileName()));
        lines.add("  " + field(Lang.get("cmd.info.version"), s.version()));
        if (!s.authors().isBlank()) {
            lines.add("  " + field(Lang.get("cmd.info.author"), s.authors()));
        }
        if (!s.license().isBlank()) {
            lines.add("  " + field(Lang.get("cmd.info.license"), s.license()));
        }
        if (!s.loader().isBlank()) {
            lines.add("  " + field(Lang.get("cmd.info.loader"), s.loader()));
        }
        lines.add("  " + field(Lang.get("cmd.info.classes"), String.valueOf(s.classNames().size())));
        lines.add("  " + field(Lang.get("cmd.info.size"), humanSize(s.size())));
        lines.add("  " + field(Lang.get("cmd.info.sha1"), s.sha1()));

        // 重复声明同一个 modId 的文件，这个很值得当场看到
        int dupes = 0;
        for (ModSource other : sources) {
            if (other != s && !java.util.Collections.disjoint(other.modIds(), s.modIds())) {
                dupes++;
            }
        }
        if (dupes > 0) {
            lines.add("  " + Lang.get("cmd.info.duplicate", dupes));
        }

        // MCreator 判定结果
        McreatorDetector.detect(s).ifPresent(hit ->
                lines.add("  " + Lang.get("cmd.info.mcreator", hit.score())));

        // 图形谱系
        for (dev.oba.core.GraphicsMods.Family f : dev.oba.core.GraphicsMods.familiesOf(s)) {
            dev.oba.core.GraphicsMods.Member m =
                    dev.oba.core.GraphicsMods.memberOf(id.split(",")[0].trim());
            String role = m == null ? "" : roleLabel(m.role());
            lines.add("  " + Lang.get("cmd.info.graphics", familyLabel(f.key()), role));
            if (m != null && !m.note().isBlank()) {
                lines.add("      " + m.note());
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
            lines.add("");
            lines.add("  " + Lang.get("cmd.info.issues", issues.size()));
            for (String i : issues) {
                lines.add("    - " + i);
            }
        }

        // 聊天栏一次别刷太多，超了就指到报告文件
        int limit = 24;
        List<String> shown = lines.size() > limit ? lines.subList(0, limit) : lines;
        for (String line : shown) {
            src.sendSuccess(() -> Component.literal(line), false);
        }
        if (lines.size() > limit) {
            int rest = lines.size() - limit;
            src.sendSuccess(() -> Component.literal(a(Lang.get("cmd.info.tooLong", rest))), false);
        }
        return 1;
    }

    private static String field(String label, String value) {
        return String.format(Locale.ROOT, "%-12s %s", label, value == null ? "" : value);
    }

    private static String a(String s) {
        return Oba.LOG_PREFIX + s;
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

    // 按宽度折行，中文按两个字宽算
    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.add(line.toString());
                line.setLength(0);
                w = 0;
                continue;
            }
            int cw = c < 0x2E80 ? 1 : 2;
            if (w + cw > width) {
                out.add(line.toString());
                line.setLength(0);
                w = 0;
            }
            line.append(c);
            w += cw;
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
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
        src.sendSuccess(() -> Component.literal(Lang.get("cmd.threads.collecting")), false);
        String dump = dev.oba.core.ProcessProbe.captureThreadDump(false);
        Diagnosis.Report report = dev.oba.core.ProcessProbe.analyzeThreadDump(
                dump, diagnostics.index(), diagnostics.byModId(), null);
        report.scannedMods().clear();
        report.scannedMods().addAll(diagnostics.sources());
        report.setTitleKey("report.title.threads");
        Diagnosis.Finding primary = report.primarySuspect();
        if (primary == null) {
            src.sendSuccess(() -> Component.literal(Lang.get("mod.thread.none")), false);
        } else {
            src.sendSuccess(() -> Component.literal(Lang.get("mod.thread.suspect",
                    primary.display(), primary.fileName(), primary.confidence().label())), false);
        }
        sendLong(src, Reports.text(report, false));
        return 1;
    }

    private int crash(CommandSourceStack src) {
        Optional<Diagnosis.Report> report = diagnostics.analyzeNewestCrashReport();
        if (report.isEmpty()) {
            src.sendSuccess(() -> Component.literal(Lang.get("cmd.crash.none")), false);
            return 0;
        }
        Diagnosis.Finding primary = report.get().primarySuspect();
        if (primary != null) {
            src.sendSuccess(() -> Component.literal(
                    Lang.get("mod.locate", primary.display(), primary.fileName())), false);
        }
        sendLong(src, Reports.text(report.get(), false));
        return 1;
    }

    private int analyze(CommandSourceStack src, String fileArg) {
        String cleaned = fileArg.trim().replace("\"", "");
        Path requested = Paths.get(cleaned);
        final Path path = requested.isAbsolute()
                ? requested
                : diagnostics.resolveGameDir().resolve(cleaned);
        if (!Files.isRegularFile(path)) {
            src.sendSuccess(() -> Component.literal(Lang.get("cmd.analyze.missing", path)), false);
            return 0;
        }
        try {
            Diagnosis.Report report = diagnostics.analyzeFile(path);
            Diagnosis.Finding primary = report.primarySuspect();
            if (primary != null) {
                src.sendSuccess(() -> Component.literal(Lang.get("cmd.report.summary",
                        primary.display(), primary.fileName(),
                        primary.firstLocatedFrame() == null ? "?" : primary.firstLocatedFrame().location(),
                        primary.confidence().label())), false);
            } else {
                src.sendSuccess(() -> Component.literal(Lang.get("cmd.analyze.none")), false);
            }
            sendLong(src, Reports.text(report, false));
            return 1;
        } catch (IOException e) {
            src.sendSuccess(() -> Component.literal(Lang.get("cmd.analyze.failed", e.getMessage())), false);
            return 0;
        }
    }

    private int reindex(CommandSourceStack src) {
        src.sendSuccess(() -> Component.literal(Lang.get("cmd.reindex.start")), false);
        Diagnostics d = diagnostics;
        d.reindex(true, () -> src.sendSuccess(() -> Component.literal(
                Lang.get("cmd.reindex.done", d.sources().size(), d.index().classCountTotal())), false));
        return 1;
    }

    // 辅助方法

    // 聊天栏会把长行截断，报告按行发
    private void sendLong(CommandSourceStack src, String text) {
        for (String line : text.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            src.sendSuccess(() -> Component.literal(line), false);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
