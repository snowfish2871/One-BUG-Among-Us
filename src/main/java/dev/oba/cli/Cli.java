// 独立命令行工具：不开游戏也能分析日志、崩溃报告和 mods 目录。
package dev.oba.cli;

import dev.oba.core.Analyzer;
import dev.oba.core.ClassIndex;
import dev.oba.core.Diagnosis;
import dev.oba.core.LogAnalyzer;
import dev.oba.core.Lang;
import dev.oba.core.LogWatcher;
import dev.oba.core.ModSource;
import dev.oba.core.Oba;
import dev.oba.core.ProcessProbe;
import dev.oba.core.Reports;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class Cli {

    private static final int EXIT_OK = 0;
    private static final int EXIT_FOUND_PROBLEMS = 2;
    private static final int EXIT_USAGE = 64;
    private static final int EXIT_ERROR = 70;

    private final PrintStream out;
    private final PrintStream err;

    private Cli(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        System.exit(run(args)
        );
    }

    // 命令行入口，返回进程退出码。Launcher 也走这里。
    public static int run(String[] args) {
        PrintStream out = new PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out),
                true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.err),
                true, StandardCharsets.UTF_8);
        int code = new Cli(out, err).dispatch(args);
        out.flush();
        err.flush();
        return code;
    }

    private int dispatch(String[] argv) {
        if (argv.length == 0) {
            printUsage();
            return EXIT_USAGE;
        }
        String command = argv[0];
        List<String> rest = new ArrayList<>(List.of(argv).subList(1, argv.length));
        Options o;
        try {
            o = Options.parse(rest);
        } catch (IllegalArgumentException e) {
            err.println(Lang.get("cli.badArg", e.getMessage()));
            err.println("oba help");
            return EXIT_USAGE;
        }
        if (o.help) {
            printUsage();
            return EXIT_OK;
        }
        try {
            return switch (command.toLowerCase(Locale.ROOT)) {
                case "analyze", "a" -> cmdAnalyze(o);
                case "scan", "s" -> cmdScan(o);
                case "mods", "list", "l" -> cmdMods(o);
                case "mcreator", "mc" -> cmdMcreator(o);
                case "watch", "w" -> cmdWatch(o);
                case "threads", "t" -> cmdThreads(o);
                case "help", "-h", "--help" -> {
                    printUsage();
                    yield EXIT_OK;
                }
                case "version", "--version", "-v" -> {
                    out.println(Lang.get("cli.version", Oba.VERSION));
                    yield EXIT_OK;
                }
                default -> {
                    err.println(Lang.get("cli.unknownCommand", command));
                    printUsage();
                    yield EXIT_USAGE;
                }
            };
        } catch (IOException e) {
            err.println(Lang.get("cli.readFailed", e.getMessage()));
            return EXIT_ERROR;
        } catch (RuntimeException e) {
            err.println(Lang.get("cli.internalError", e));
            if (o.verbose) {
                e.printStackTrace(err);
            }
            return EXIT_ERROR;
        }
    }

    // 各个子命令

    // analyze [文件...]
    private int cmdAnalyze(Options o) throws IOException {
        Path gameDir = resolveGameDir(o);
        List<Path> targets = new ArrayList<>();
        for (String t : o.positional) {
            Path p = Paths.get(t);
            if (!Files.exists(p)) {
                err.println(Lang.get("cli.fileMissing", p));
                return EXIT_USAGE;
            }
            targets.add(p);
        }
        if (targets.isEmpty()) {
            if (gameDir == null) {
                err.println(Lang.get("cli.noGameDir"));
                return EXIT_USAGE;
            }
            Optional<Path> crash = Analyzer.newestCrashReport(gameDir);
            Optional<Path> latest = Analyzer.candidateLogs(gameDir).stream()
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase("latest.log"))
                    .findFirst();
            crash.ifPresent(targets::add);
            if (targets.isEmpty()) {
                latest.ifPresent(targets::add);
            }
            if (targets.isEmpty()) {
                err.println(Lang.get("cli.noLogs", gameDir));
                return EXIT_USAGE;
            }
            if (!o.quiet) {
                out.println(Lang.get("cli.autoSelected", targets.get(0)));
            }
        }

        ScanContext ctx = buildContext(gameDir, o);
        int worst = EXIT_OK;
        for (int i = 0; i < targets.size(); i++) {
            Path target = targets.get(i);
            if (targets.size() > 1 && !o.quiet) {
                out.println();
                out.println("################ " + target + " ################");
            }
            Analyzer.Options ao = new Analyzer.Options();
            ao.index = ctx.index;
            ao.byModId = ctx.byModId;
            ao.detectMcreator = true;
            ao.subject = target.toString();
            ao.titleKey = "report.title.diagnosis";
            Diagnosis.Report report = Analyzer.analyzeFile(target, ao);
            report.scannedMods().clear();
            report.scannedMods().addAll(ctx.sources);
            report.environment().put("__classes", String.valueOf(ctx.index.classCountTotal()));

            emit(report, o, baseName(target));
            // 顺手往游戏的 logs 目录写一份，和游戏内崩溃时落盘的位置保持一致
            if (report.hasErrors()) {
                writeIntoLogs(report, gameDir, o);
            }
            if (report.hasErrors() && worst == EXIT_OK) {
                worst = EXIT_FOUND_PROBLEMS;
            }
        }
        return worst;
    }

    // 把报告写进 logs 目录。指定了 --output 就不重复写，用户自己知道要放哪。
    private void writeIntoLogs(Diagnosis.Report report, Path gameDir, Options o) {
        if (o.save != null || gameDir == null) {
            return;
        }
        try {
            Path dir = gameDir.resolve("logs");
            Files.createDirectories(dir);
            Path file = dir.resolve("oba-report.txt");
            Files.writeString(file, Reports.text(report, false));
            if (!o.quiet) {
                err.println(Lang.get("report.written", file));
            }
        } catch (IOException e) {
            err.println(Lang.get("report.writeFailed", e.getMessage()));
        }
    }

    // scan，analyze 的别名
    private int cmdScan(Options o) throws IOException {
        return cmdAnalyze(o);
    }

    // mods，不需要日志的清单
    private int cmdMods(Options o) throws IOException {
        ScanContext ctx = buildContext(resolveGameDir(o), o);
        if (ctx.sources.isEmpty()) {
            err.println(Lang.get("cli.noMods"));
            return EXIT_ERROR;
        }
        Diagnosis.Report report = Analyzer.inventory(ctx.sources, o.mcreator);
        for (Diagnosis.KnownIssue dup : Analyzer.findDuplicates(ctx.sources)) {
            report.addIssue(dup);
        }
        // 图形类模组的组合问题也要在这里查：钠和铷同时装这种事
        // 只有看模组清单才看得出来，分析日志是发现不了的
        Analyzer.analyzeGraphics(report, ctx.sources);
        report.finish(0.0);
        report.environment().put("__classes", String.valueOf(ctx.index.classCountTotal()));

        if (o.format == Format.JSON) {
            out.println(Reports.json(report));
        } else if (o.format == Format.MARKDOWN) {
            out.println(Reports.markdown(report));
        } else {
            out.println(Lang.get("cli.inventory.header", ctx.sources.size(),
                    ctx.index.classCountTotal()));
            // 有 Connector 就说明一下，免得看到一堆 Fabric 模组觉得奇怪
            boolean hasConnector = ctx.sources.stream().anyMatch(Cli::isConnector);
            if (hasConnector) {
                out.println(Lang.get("note.connectorPresent"));
            }
            out.println();

            // 图形类组合有问题的，先说，这是能直接动手的
            for (Diagnosis.GraphicsIssue gi : report.graphicsIssues()) {
                boolean conflict = gi.kind() == Diagnosis.GraphicsIssue.Kind.FAMILY_CONFLICT;
                out.println(Lang.get("cli.gfx.issue", conflict
                        ? Lang.get("gfx.issue.conflict.title", familyLabel(gi.familyName()))
                        : Lang.get("gfx.issue.missing.title", gi.subject())));
                if (conflict) {
                    for (String f : gi.conflictingFiles()) {
                        out.println("    " + Lang.get("cli.gfx.item", f));
                    }
                } else {
                    out.println("    " + Lang.get("cli.gfx.item", gi.fileName()));
                }
                out.println(Lang.get("cli.gfx.advice", conflict
                        ? Lang.get("gfx.issue.conflict.advice", gi.memberList())
                        : Lang.get("gfx.issue.missing.advice", gi.subject(), gi.wanted())));
            }
            if (!report.graphicsIssues().isEmpty()) {
                out.println();
            }

            List<List<String>> rows = new ArrayList<>();
            for (ModSource s : sortByName(ctx.sources)) {
                List<String> notes = new ArrayList<>();
                if (report.mcreatorMods().stream().anyMatch(h -> h.source() == s)) {
                    notes.add(Lang.get("cli.note.mcreator"));
                }
                if (s.isFabric()) {
                    notes.add(Lang.get(hasConnector ? "cli.note.fabricBridged" : "cli.note.fabric"));
                }
                if (!s.nestedJars().isEmpty()) {
                    notes.add(Lang.get("cli.note.nested", s.nestedJars().size()));
                }
                // 图形类模组标出所属族，方便一眼看出钠 / iris 这一系装了哪些
                for (dev.oba.core.GraphicsMods.Family f
                        : dev.oba.core.GraphicsMods.familiesOf(s)) {
                    notes.add(familyLabel(f.key()));
                }
                if (!s.scanErrors().isEmpty()) {
                    notes.add(Lang.get("cli.note.readError"));
                }
                rows.add(List.of(
                        truncate(s.fileName(), 44),
                        truncate(String.join(",", s.modIds()), 26),
                        truncate(s.version(), 12),
                        String.valueOf(s.classNames().size()),
                        String.join(" ", notes)));
            }
            out.print(Reports.table(List.of(
                    Lang.get("cli.col.file"), Lang.get("cli.col.modId"), Lang.get("cli.col.version"),
                    Lang.get("cli.col.classes"), Lang.get("cli.col.notes")), rows, 0));
            if (!report.issues().isEmpty()) {
                out.println();
                out.println(Lang.get("cli.issuesFound", report.issues().size()));
                for (Diagnosis.KnownIssue i : report.issues()) {
                    out.println();
                    out.println("  [" + i.severity().label() + "] " + i.title());
                    out.println("      " + i.detail());
                    out.println("      " + Lang.get("detail.suggest", i.advice()));
                }
            }
        }
        if (Files.isDirectory(modsDirOf(ctx))) {
            List<Path> disabled = ModSource.scanDisabled(modsDirOf(ctx));
            if (!disabled.isEmpty() && o.format == Format.TEXT && !o.quiet) {
                out.println();
                out.println(Lang.get("cli.disabled.found", disabled.size()));
                for (Path p : disabled) {
                    out.println("  · " + p.getFileName());
                }
            }
        }
        return report.issues().isEmpty() ? EXIT_OK : EXIT_FOUND_PROBLEMS;
    }

    // mcreator，只跑 MCreator 识别
    private int cmdMcreator(Options o) throws IOException {
        ScanContext ctx = buildContext(resolveGameDir(o), o);
        if (ctx.sources.isEmpty()) {
            err.println(Lang.get("cli.noMods"));
            return EXIT_ERROR;
        }
        out.println(Lang.get("cli.mcreator.checking", ctx.sources.size()));
        out.println();
        List<Diagnosis.McreatorHit> hits = new ArrayList<>();
        for (ModSource s : sortByName(ctx.sources)) {
            dev.oba.core.McreatorDetector.detect(s).ifPresent(hits::add);
        }
        hits.sort(Comparator.comparingInt(Diagnosis.McreatorHit::score).reversed());
        if (o.format == Format.JSON) {
            Map<String, Object> wrapper = new LinkedHashMap<>();
            out.println("[");
            for (int i = 0; i < hits.size(); i++) {
                Diagnosis.McreatorHit h = hits.get(i);
                out.print("  {\"modId\":\"" + String.join(",", h.source().modIds())
                        + "\",\"file\":\"" + h.source().fileName()
                        + "\",\"score\":" + h.score()
                        + ",\"confidence\":\"" + h.confidence().id() + "\",\"reasons\":[");
                for (int r = 0; r < h.reasons().size(); r++) {
                    out.print("\"" + h.reasons().get(r).detail().replace("\"", "\\\"") + "\"");
                    if (r < h.reasons().size() - 1) {
                        out.print(",");
                    }
                }
                out.print("]}");
                if (i < hits.size() - 1) {
                    out.print(",");
                }
                out.println();
            }
            out.println("]");
            return hits.isEmpty() ? EXIT_OK : EXIT_FOUND_PROBLEMS;
        }
        if (hits.isEmpty()) {
            out.println(Lang.get("cli.mcreator.none"));
            return EXIT_OK;
        }
        out.println(Lang.get("cli.mcreator.found", hits.size(), ctx.sources.size()));
        out.println("─".repeat(88));
        for (Diagnosis.McreatorHit h : hits) {
            out.println("* " + h.label() + "   [" + h.source().fileName() + "]");
            out.println("  " + Lang.get("cli.confidenceScore", h.confidence().label(), h.score()));
            for (Diagnosis.Evidence e : h.reasons()) {
                out.println("    - " + e.detail());
            }
            out.println();
        }
        out.println(Lang.get("mcreator.note"));
        return EXIT_FOUND_PROBLEMS;
    }

    // watch，实时跟着 latest.log 并即时分类
    private int cmdWatch(Options o) throws IOException {
        Path gameDir = resolveGameDir(o);
        Path logFile;
        if (!o.positional.isEmpty()) {
            logFile = Paths.get(o.positional.get(0));
        } else if (gameDir != null) {
            logFile = gameDir.resolve("logs/latest.log");
        } else {
            err.println(Lang.get("cli.noGameDir"));
            return EXIT_USAGE;
        }
        if (!Files.isRegularFile(logFile)) {
            err.println(Lang.get("cli.fileMissing", logFile));
            return EXIT_USAGE;
        }

        ScanContext ctx = buildContext(gameDir, o);
        boolean color = o.color && !o.noColor;
        out.println(Lang.get("cli.watch.started", logFile));
        out.println(Lang.get("cli.watch.indexed", ctx.sources.size(), ctx.index.classCountTotal()));
        out.println(Lang.get("cli.watch.readonly"));
        out.println("─".repeat(96));

        LogWatcher.RingBuffer ring = new LogWatcher.RingBuffer(60);
        AtomicInteger issueCount = new AtomicInteger();
        AtomicInteger fatalCount = new AtomicInteger();
        Object lock = new Object();

        LogWatcher watcher = new LogWatcher(logFile, event -> {
            ring.add(event.line());
            if (!event.isIssue() && !event.fatalHint()) {
                return;
            }
            synchronized (lock) {
                if (event.issue() != null) {
                    Diagnosis.KnownIssue issue = event.issue();
                    String tag = "[" + issue.severity().label() + "]";
                    issueCount.incrementAndGet();
                    if (issue.severity() == Diagnosis.Severity.FATAL) {
                        fatalCount.incrementAndGet();
                    }
                    out.println();
                    out.println(ansi(color, "33", Lang.get("cli.watch.issue", tag, issue.title())));
                    out.println("  " + Lang.get("cli.watch.raw", event.line()));
                    out.println("  " + Lang.get("cli.watch.means", issue.detail()));
                    out.println("  " + Lang.get("cli.watch.advice", issue.advice()));
                    // 顺手把这段日志前面的上下文也归属一次
                    List<String> context = ring.snapshot();
                    Analyzer.Options ao = new Analyzer.Options();
                    ao.index = ctx.index;
                    ao.byModId = ctx.byModId;
                    ao.detectMcreator = false;
                    ao.subject = logFile.toString();
                    Diagnosis.Report r = Analyzer.analyze(String.join("\n", context), ao);
                    Diagnosis.Finding f = r.primarySuspect();
                    if (f != null) {
                        out.println(ansi(color, "31", "  " + Lang.get("cli.watch.attributed",
                                f.display(), f.fileName(), f.confidence().label(),
                                Diagnosis.Confidence.percent(f.score()))));
                        var firstFrame = f.firstLocatedFrame();
                        if (firstFrame != null) {
                            out.println(ansi(color, "33", "  " + Lang.get("cli.watch.location",
                                    firstFrame.location())));
                        }
                    }
                    out.println(ansi(color, "2", "  " + "-".repeat(92)));
                    out.flush();
                } else if (event.fatalHint()) {
                    out.println(ansi(color, "2", "… " + event.line()));
                }
            }
        }, 150);
        watcher.startDaemon("oba-watch");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            watcher.close();
            out.println();
            out.println(Lang.get("cli.watch.stopped", issueCount.get(), fatalCount.get()));
            out.println(Lang.get("cli.watch.hint"));
        }));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return EXIT_OK;
    }

    // threads，抓当前进程的线程转储并分析
    private int cmdThreads(Options o) throws IOException {
        out.println(Lang.get("cli.threads.collecting"));
        String dump = ProcessProbe.captureThreadDump(false);
        if (o.save != null) {
            Files.writeString(Paths.get(o.save), dump);
            out.println(Lang.get("cli.threads.saved", o.save));
        }
        List<String> deadlocks = ProcessProbe.findDeadlockedThreads();
        if (!deadlocks.isEmpty()) {
            out.println(Lang.get("cli.threads.deadlock", String.join(", ", deadlocks)));
        }
        Map<String, String> facts = ProcessProbe.processFacts();
        for (Map.Entry<String, String> e : facts.entrySet()) {
            out.println("  " + e.getKey() + ": " + e.getValue());
        }
        out.println();
        if (o.positional.isEmpty()) {
            out.println(Lang.get("cli.threads.otherProcess"));
            out.println("      " + Lang.get("cli.threads.otherProcess2"));
            return EXIT_OK;
        }
        Path source = Paths.get(o.positional.get(0));
        String text = Files.readString(source);
        ScanContext ctx = buildContext(resolveGameDir(o), o);
        Diagnosis.Report report = ProcessProbe.analyzeThreadDump(text, ctx.index, ctx.byModId, o.threadHint);
        emit(report, o, baseName(source));
        return report.findings().isEmpty() ? EXIT_OK : EXIT_FOUND_PROBLEMS;
    }

    // 杂项

    private enum Format {
        TEXT, MARKDOWN, JSON
    }

    private static final class Options {
        final List<String> positional = new ArrayList<>();
        Path modsDir;
        Path gameDir;
        Format format = Format.TEXT;
        boolean help;
        boolean verbose;
        boolean quiet;
        boolean color = System.console() != null;
        boolean noColor;
        boolean mcreator = true;
        String save;
        String threadHint;

        static Options parse(List<String> args) {
            Options o = new Options();
            for (int i = 0; i < args.size(); i++) {
                String a = args.get(i);
                if (a.startsWith("--")) {
                    String key = a.substring(2);
                    String inline = null;
                    int eq = key.indexOf('=');
                    if (eq >= 0) {
                        inline = key.substring(eq + 1);
                        key = key.substring(0, eq);
                    }
                    switch (key) {
                        case "lang", "language" -> Lang.setLanguage(value(key, inline, args, ++i));
                        case "mods-dir", "mods" -> o.modsDir = Paths.get(value(key, inline, args, ++i));
                        case "game-dir", "game" -> o.gameDir = Paths.get(value(key, inline, args, ++i));
                        case "format", "fmt" -> o.format = parseFormat(value(key, inline, args, ++i));
                        case "output", "out", "save" -> o.save = value(key, inline, args, ++i);
                        case "thread", "thread-name" -> o.threadHint = value(key, inline, args, ++i);
                        case "no-mcreator" -> o.mcreator = false;
                        case "no-color" -> o.noColor = true;
                        case "color" -> o.color = true;
                        case "verbose" -> o.verbose = true;
                        case "quiet" -> o.quiet = true;
                        case "help" -> o.help = true;
                        default -> throw new IllegalArgumentException(Lang.get("cli.badOption", "--" + key));
                    }
                    if (inline != null) {
                        i--;
                    }
                } else if (a.startsWith("-") && a.length() > 1 && !a.equals("-")) {
                    switch (a) {
                        case "-h" -> o.help = true;
                        case "-v" -> o.verbose = true;
                        case "-q" -> o.quiet = true;
                        case "-j" -> o.format = Format.JSON;
                        case "-m" -> o.format = Format.MARKDOWN;
                        default -> throw new IllegalArgumentException(Lang.get("cli.badOption", a));
                    }
                } else {
                    o.positional.add(stripQuotes(a));
                }
            }
            return o;
        }

        private static String value(String key, String inline, List<String> args, int i) {
            if (inline != null) {
                return stripQuotes(inline);
            }
            if (i >= args.size()) {
                throw new IllegalArgumentException(Lang.get("cli.optionNeedsValue", "--" + key));
            }
            return stripQuotes(args.get(i));
        }

        private static Format parseFormat(String s) {
            return switch (s.toLowerCase(Locale.ROOT)) {
                case "json" -> Format.JSON;
                case "md", "markdown" -> Format.MARKDOWN;
                default -> Format.TEXT;
            };
        }
    }

    // 去掉值首尾的引号。路径里有空格时大家自然会写成
    // --game-dir "D:\我的 游戏\.minecraft" 或 --game-dir="D:\我的 游戏\.minecraft"，
    // 但命令行本身不会把引号吃掉，引号就跟着路径进来了，
    // 到 Path.of() 那一步会直接报 Illegal char <">。公开是为了让自测能引用
    public static String stripQuotes(String s) {
        if (s == null || s.length() < 2) {
            return s;
        }
        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private record ScanContext(List<ModSource> sources, ClassIndex index, Map<String, ModSource> byModId) {
    }

    private ScanContext buildContext(Path gameDir, Options o) {
        List<Path> dirs = new ArrayList<>();
        if (o.modsDir != null) {
            dirs.add(o.modsDir);
        } else if (gameDir != null) {
            dirs.addAll(Analyzer.candidateModsDirs(gameDir));
            dirs.add(0, gameDir.resolve("mods"));
        }
        List<Path> unique = new ArrayList<>();
        for (Path p : dirs) {
            if (Files.isDirectory(p) && !unique.contains(p)) {
                unique.add(p);
            }
        }
        List<ModSource> sources = Analyzer.loadMods(unique);
        return new ScanContext(sources, Analyzer.buildIndex(sources), ModSource.indexByModId(sources));
    }

    private Path resolveGameDir(Options o) {
        if (o.gameDir != null) {
            return o.gameDir;
        }
        if (o.modsDir != null) {
            Path parent = o.modsDir.toAbsolutePath().getParent();
            if (parent != null) {
                return parent;
            }
        }
        return Analyzer.findGameDir(Paths.get("")).orElse(null);
    }

    private Path modsDirOf(ScanContext ctx) {
        if (ctx.sources.isEmpty()) {
            return null;
        }
        Path p = ctx.sources.get(0).path().getParent();
        return p;
    }

    private void emit(Diagnosis.Report report, Options o, String baseName) throws IOException {
        String rendered = switch (o.format) {
            case JSON -> Reports.json(report);
            case MARKDOWN -> Reports.markdown(report);
            case TEXT -> Reports.text(report, o.color && !o.noColor);
        };
        if (o.save != null) {
            String suffix = switch (o.format) {
                case JSON -> ".json";
                case MARKDOWN -> ".md";
                case TEXT -> ".txt";
            };
            Path target = Paths.get(o.save);
            Path outPath;
            if (Files.isDirectory(target)) {
                outPath = target.resolve(baseName + "-oba" + suffix);
            } else if (o.positional.size() > 1 || Files.exists(target)) {
                outPath = target;
            } else {
                outPath = target;
            }
            Files.createDirectories(outPath.toAbsolutePath().getParent());
            Files.writeString(outPath, rendered);
            out.println(rendered);
            err.println(Lang.get("cli.reportWritten", outPath.toAbsolutePath()));
        } else {
            out.println(rendered);
        }
    }

    private static String baseName(Path p) {
        String n = p.getFileName().toString();
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    private static List<ModSource> sortByName(List<ModSource> in) {
        List<ModSource> copy = new ArrayList<>(in);
        copy.sort(Comparator.comparing(ModSource::fileName, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    // 这个文件是不是 Sinytra Connector
    private static boolean isConnector(ModSource s) {
        for (String id : s.modIds()) {
            if (id.equals("connectormod") || id.equals("connector") || id.equals("sinytraconnector")) {
                return true;
            }
        }
        return false;
    }

    private static String familyLabel(String familyName) {
        if (familyName == null || familyName.isBlank()) {
            return "";
        }
        return switch (familyName.toLowerCase(Locale.ROOT)) {
            case "sodium" -> Lang.get("gfx.family.sodium");
            case "iris" -> Lang.get("gfx.family.iris");
            case "lithium" -> Lang.get("gfx.family.lithium");
            case "krypton" -> Lang.get("gfx.family.krypton");
            case "phosphor" -> Lang.get("gfx.family.phosphor");
            default -> familyName;
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static String ansi(boolean on, String code, String s) {
        return on ? "\u001B[" + code + "m" + s + "\u001B[0m" : s;
    }

    private void printUsage() {
        out.println(Lang.get("cli.usage.body", Oba.VERSION));
    }
}
