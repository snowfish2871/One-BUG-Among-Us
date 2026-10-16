// 分析的总调度：建索引、归属栈帧、算可疑度，最后拼出一份报告。
package dev.oba.core;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Analyzer {

    public static final class Options {
        public Path modsDir;
        public List<Path> extraModDirs = new ArrayList<>();
        public ClassIndex index;
        public Map<String, ModSource> byModId = Map.of();
        public boolean detectMcreator = true;
        // 低于这个分数的结论丢掉，算噪声
        public double noiseFloor = 0.18;
        public int maxFindings = 12;
        public String titleKey = "report.title.diagnosis";
        // 直接给出的分析对象，比如一个文件路径，不翻译
        public String subject = "";
        // 没有具体路径时用它描述分析对象，是个文案键
        public String subjectKey;
        public List<Object> subjectArgs = List.of();
    }

    public record FrameDetail(
            String exception,
            String exceptionClass,
            String message,
            int chainDepth,
            String location,
            String className,
            String methodName,
            String sourceFile,
            int lineNumber,
            double matchConfidence,
            String matchedKey,
            String modFile) {

        public boolean inRootCause() {
            return chainDepth > 0;
        }
    }

    private Analyzer() {
    }

    // 找路径

    // 找那个装着 mods/、logs/、crash-reports/ 的目录
    public static Optional<Path> findGameDir(Path startFrom) {
        Path cur = (startFrom == null ? Paths.get("").toAbsolutePath() : startFrom.toAbsolutePath().normalize());
        for (int depth = 0; depth < 4 && cur != null; depth++) {
            if (looksLikeGameDir(cur)) {
                return Optional.of(cur);
            }
            // 启动器实例：里面还有一层 .minecraft，或者 logs/ 和 mods/ 就在旁边
            cur = cur.getParent();
        }
        Path home = Paths.get(System.getProperty("user.home", "."));
        List<Path> candidates = List.of(
                home.resolve("AppData/Roaming/.minecraft"),
                home.resolve(".minecraft"),
                home.resolve("Library/Application Support/minecraft"),
                home.resolve(".local/share/minecraft"));
        for (Path c : candidates) {
            if (looksLikeGameDir(c)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private static boolean looksLikeGameDir(Path dir) {
        return dir != null && Files.isDirectory(dir)
                && (Files.isDirectory(dir.resolve("mods"))
                    || Files.isRegularFile(dir.resolve("logs/latest.log"))
                    || Files.isDirectory(dir.resolve("crash-reports")));
    }

    // 这个位置附近可能放模组的目录，最像的排前面
    public static List<Path> candidateModsDirs(Path gameDir) {
        List<Path> out = new ArrayList<>();
        if (gameDir == null) {
            return out;
        }
        addIfDir(out, gameDir.resolve("mods"));
        addIfDir(out, gameDir.resolve("versions"));
        // 各种启动器的目录布局
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(gameDir)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    addIfDir(out, p.resolve("mods"));
                    addIfDir(out, p.resolve(".minecraft/mods"));
                }
            }
        } catch (IOException ignored) {
            // 尽力而为，失败就算了
        }
        return out;
    }

    private static void addIfDir(List<Path> list, Path p) {
        if (Files.isDirectory(p) && !list.contains(p)) {
            list.add(p);
        }
    }

    // 可以拿来分析的日志文件，最新的排前面
    public static List<Path> candidateLogs(Path gameDir) {
        List<Path> out = new ArrayList<>();
        if (gameDir == null) {
            return out;
        }
        collectLogs(gameDir.resolve("logs"), out);
        collectLogs(gameDir.resolve("crash-reports"), out);
        for (Path extra : candidateModsDirs(gameDir)) {
            Path parent = extra.getParent();
            if (parent != null) {
                collectLogs(parent.resolve("logs"), out);
                collectLogs(parent.resolve("crash-reports"), out);
            }
        }
        out.sort(Comparator.comparingLong(Analyzer::lastModified).reversed());
        return out;
    }

    private static void collectLogs(Path dir, List<Path> out) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (n.endsWith(".log") || n.endsWith(".log.gz") || n.endsWith(".txt")) {
                    if (!out.contains(p)) {
                        out.add(p);
                    }
                }
            }
        } catch (IOException ignored) {
            // 尽力而为，失败就算了
        }
    }

    private static long lastModified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    // 最新的那份崩溃报告，有的话
    public static Optional<Path> newestCrashReport(Path gameDir) {
        List<Path> logs = candidateLogs(gameDir);
        return logs.stream()
                .filter(p -> p.getParent() != null
                        && p.getParent().getFileName().toString().equalsIgnoreCase("crash-reports"))
                .findFirst();
    }

    // 建索引

    // 扫描给定的几个 mods 目录。只打开归档，不改动
    public static List<ModSource> loadMods(List<Path> modsDirs) {
        List<ModSource> all = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Path dir : modsDirs) {
            try {
                for (ModSource s : ModSource.scanModsFolder(dir)) {
                    if (seen.add(s.path().toString())) {
                        all.add(s);
                    }
                }
            } catch (IOException ignored) {
                // 读不了的目录由调用方记进 notes
            }
        }
        return all;
    }

    public static ClassIndex buildIndex(List<ModSource> sources) {
        return ClassIndex.builder().addAll(sources).build();
    }

    // 分析

    // 对一份文本做完整分析
    public static Diagnosis.Report analyze(String text, Options options) {
        Options opt = options == null ? new Options() : options;
        ClassIndex index = opt.index == null ? ClassIndex.empty() : opt.index;

        Diagnosis.Report report = new Diagnosis.Report();
        report.setTitleKey(opt.titleKey);
        if (opt.subjectKey != null) {
            report.setSubjectKey(opt.subjectKey, opt.subjectArgs.toArray());
        } else {
            report.setSubject(opt.subject);
        }

        LogAnalyzer.Scan scan = LogAnalyzer.scan(text, opt.subject, text.length(), index, opt.byModId);
        report.setParsedLines(text.isEmpty() ? 0 : text.split("\r\n|\n|\r", -1).length);
        report.setFrameCount(scan.parsed().allFrames().size());
        report.environment().putAll(scan.environment());
        // 把日志串成时间线：出事当时和出事之前，各个模组分别做了什么
        report.setTimeline(buildTimeline(text, index, opt.byModId));

        Map<ModSource, Diagnosis.Finding> findings = new LinkedHashMap<>();
        int attributed = 0;
        Set<String> unmatchedRoots = new LinkedHashSet<>();

        // 1. 归属栈帧
        for (StackTraceParser.ThrowableInfo t : scan.parsed().roots()) {
            for (StackTraceParser.ThrowableInfo inChain : t.chain()) {
                int depth = inChain.depth();
                boolean rootCause = inChain.cause().isEmpty();
                boolean firstInChain = true;
                for (StackTraceParser.Frame frame : inChain.frames()) {
                    if (frame.elided() || StackTraceParser.isVanillaOrLoader(frame.className())) {
                        continue;
                    }
                    ClassIndex.Match match = index.resolve(frame.className());
                    if (match == null) {
                        unmatchedRoots.add(rootOf(frame.className()));
                        continue;
                    }
                    attributed++;
                    Diagnosis.Finding f = findings.computeIfAbsent(match.source(),
                            src -> new Diagnosis.Finding(src));
                    f.addFrame(new StackTraceParser.AttributedFrame(match.source(), match, frame, frame.location()));
                    f.addThrowable(inChain.headline());

                    double weight = 0.30 + 0.10 * depth;
                    if (!rootCause) {
                        weight *= 0.45;
                    }
                    if (match.confidence() >= 0.999) {
                        weight *= 1.25;
                    } else {
                        weight *= match.confidence() + 0.4;
                    }
                    if (firstInChain && frame.hasLineNumber()) {
                        weight += rootCause ? 0.85 : 0.35;
                        firstInChain = false;
                    }
                    f.addScore(weight);
                    f.addEvidence(Diagnosis.Evidence.at("frame.owner",
                            rootCause ? "evidence.frame.root" : "evidence.frame.outer",
                            java.util.List.of(frame.location()),
                            weight, inChain.startLine(), frame.location()));
                }
            }
        }

        // 2. 规则命中的问题
        for (Diagnosis.KnownIssue issue : scan.issues()) {
            ModSource blamed = issue.source();
            if (blamed == null) {
                for (String suspect : issue.suspects()) {
                    ModSource s = opt.byModId.get(suspect);
                    if (s != null) {
                        blamed = s;
                        break;
                    }
                }
            }
            report.addIssue(issue.withSource(blamed));
            if (blamed != null) {
                Diagnosis.Finding f = findings.computeIfAbsent(blamed, src -> new Diagnosis.Finding(src));
                double bonus = switch (issue.severity()) {
                    case FATAL -> 1.6;
                    case ERROR -> 1.0;
                    case WARN -> 0.4;
                    case INFO -> 0.1;
                };
                f.addScore(bonus);
                f.addEvidence(Diagnosis.Evidence.at("rule." + issue.rule(), "evidence.rule",
                        java.util.List.of(Lang.ref(issue.titleKey()), issue.detail()),
                        bonus, issue.line(), null));
            }
        }

        // 3. 文本里提到了哪个模组
        for (ModSource s : index.sources()) {
            for (String id : s.modIds()) {
                if (id.length() < 4 || id.equals("minecraft") || id.equals("neoforge")) {
                    continue;
                }
                MentionProbe p = MentionProbe.of(id);
                if (p.mentions(text)) {
                    Diagnosis.Finding f = findings.get(s);
                    if (f != null) {
                        f.addScore(0.15);
                        f.addEvidence(Diagnosis.Evidence.of("text.mention", "evidence.mention",
                                java.util.List.of(id), 0.15));
                    }
                }
            }
        }

        report.setAttributedFrameCount(attributed);
        findings.values().forEach(report::addFinding);

        // 4. MCreator 识别
        if (opt.detectMcreator) {
            for (ModSource s : index.sources()) {
                McreatorDetector.detect(s).ifPresent(hit -> {
                    report.addMcreatorHit(hit);
                    Diagnosis.Finding f = findings.get(s);
                    if (f != null) {
                        f.addEvidence(Diagnosis.Evidence.of("mcreator.detected", "evidence.mcreator",
                                java.util.List.of(hit.reasons().size()), 0));
                    }
                });
            }
        }

        report.unmatchedModPackages().addAll(unmatchedRoots);
        report.finish(opt.noiseFloor);

        // F3+C 是玩家自己按出来的调试崩溃，不是模组的问题。
        // 这种报告里所有栈帧都是原版和加载器的，本来也归属不到模组，
        // 但不加这一句的话会让人以为"查不出元凶"，白折腾一轮
        if (isManualDebugCrash(scan)) {
            report.addNote(Lang.get("note.manualCrash"));
        }

        // 装了 Sinytra Connector 就说明白：Fabric 模组也会出现在报告里。
        // 归属逻辑不变，但看到 net.fabricmc 的包名不必当成异常
        if (hasConnector(index)) {
            report.addNote(Lang.get("note.connectorPresent"));
        }

        // 图形类模组单独过一遍：钠 / iris 这一系的分支互斥和缺前置
        // 光看堆栈是发现不了的，得看装了什么
        analyzeGraphics(report, index.sources());

        if (report.findings().size() > opt.maxFindings) {
            List<Diagnosis.Finding> trimmed = new ArrayList<>(report.findings().subList(0, opt.maxFindings));
            report.findings().clear();
            report.findings().addAll(trimmed);
        }

        report.setRootCauseHeadline(scan.parsed().roots().stream().findFirst()
                .map(StackTraceParser.ThrowableInfo::headline).orElse(""));

        if (report.scannedMods().isEmpty() && opt.index != null && !opt.index.sources().isEmpty()) {
            report.scannedMods().addAll(opt.index.sources());
        }

        // 每份报告都带上类数量，用户据此判断索引有没有真的覆盖到他的整合包
        report.environment().put("__classes", String.valueOf(index.classCountTotal()));
        report.environment().put("__modFiles", String.valueOf(index.sources().size()));
        return report;
    }

    // 分析磁盘上的一个文件
    public static Diagnosis.Report analyzeFile(Path file, Options options) throws IOException {
        String text = LogAnalyzer.readLogText(file);
        Options opt = options == null ? new Options() : options;
        if (opt.subject == null || opt.subject.isBlank()) {
            opt.subject = file.toString();
        }
        return analyze(text, opt);
    }

    // 只看模组不看日志的清单
    public static Diagnosis.Report inventory(List<ModSource> sources, boolean detectMcreator) {
        Diagnosis.Report report = new Diagnosis.Report();
        report.setTitleKey("report.title.inventory");
        report.setSubjectKey("report.subject.modsDir");
        report.scannedMods().addAll(sources);
        int classes = 0;
        for (ModSource s : sources) {
            classes += s.classNames().size();
            for (String err : s.scanErrors()) {
                report.addIssue(new Diagnosis.KnownIssue("scan.error", Diagnosis.Severity.WARN,
                        "note.scanError", "note.scanError", "rule.match.detail",
                        List.of(err), s, 0, List.of()));
            }
        }
        report.setFrameCount(classes);
        if (detectMcreator) {
            for (Diagnosis.McreatorHit hit : McreatorDetector.detectAll(sources)) {
                report.addMcreatorHit(hit);
            }
        }
        report.finish(0.0);
        return report;
    }

    // 找出声明了同一个 modId 的多个文件
    public static List<Diagnosis.KnownIssue> findDuplicates(List<ModSource> sources) {
        Map<String, List<ModSource>> byId = new LinkedHashMap<>();
        for (ModSource s : sources) {
            // 一个 jar 里可能有两个描述文件都声明了同一个 modId（实测见过：
            // Embeddium 那个 jar 同时以 embeddium 和 rubidium 两个 id 注册）。
            // 同一个文件算一次，否则会报出"这个文件和它自己重复"
            for (String id : new java.util.LinkedHashSet<>(s.modIds())) {
                byId.computeIfAbsent(id, k -> new ArrayList<>()).add(s);
            }
        }
        List<Diagnosis.KnownIssue> out = new ArrayList<>();
        for (Map.Entry<String, List<ModSource>> e : byId.entrySet()) {
            List<ModSource> list = e.getValue();
            // 去重后还是同一个文件的话，那不是重复安装，是这个文件自己的描述有问题
            List<ModSource> distinct = new ArrayList<>();
            for (ModSource s : list) {
                if (distinct.stream().noneMatch(d -> d.path().equals(s.path()))) {
                    distinct.add(s);
                }
            }
            if (distinct.size() < 2) {
                continue;
            }
            List<String> files = distinct.stream().map(ModSource::fileName).toList();
            out.add(new Diagnosis.KnownIssue("inventory.duplicate", Diagnosis.Severity.FATAL,
                    "inventory.duplicate", "inventory.duplicate.title", List.of(e.getKey()),
                    "inventory.duplicate.detail", List.of(e.getKey(), String.join(", ", files)),
                    distinct.get(0), 0, files.subList(1, files.size())));
        }
        return out;
    }

    // 图形类模组的专项检查。只看装了什么，不看堆栈，
    // 所以它和"分析某份日志"是两件独立的事——mods 这类清单命令也要用。
    public static void analyzeGraphics(Diagnosis.Report report, List<ModSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }

        // 1. 同一族装了多个互斥分支
        for (GraphicsMods.Conflict c : GraphicsMods.findFamilyConflicts(sources)) {
            report.addGraphicsIssue(Diagnosis.GraphicsIssue.conflict(
                    c.family().displayName(), c.family().canonicalId(),
                    c.fileNames(), c.memberIds()));
        }

        // 2. 光影加载器缺了配套的渲染主体
        for (GraphicsMods.MissingPartner m
                : GraphicsMods.findMissingPartners(sources, ModSource.indexByModId(sources))) {
            report.addGraphicsIssue(Diagnosis.GraphicsIssue.missingPartner(
                    m.shaderLoader().display(), m.shaderLoader().fileName(), m.wanted()));
        }

        // 3. 列一下装了哪些图形类模组，出渲染问题时这份清单最有用
        for (ModSource s : sources) {
            Set<GraphicsMods.Family> fams = GraphicsMods.familiesOf(s);
            if (fams.isEmpty()) {
                continue;
            }
            String id = s.modIds().isEmpty() ? "" : s.modIds().get(0);
            GraphicsMods.Member member = GraphicsMods.memberOf(id);
            for (GraphicsMods.Family f : fams) {
                report.addGraphicsMod(new Diagnosis.GraphicsModInfo(
                        s.display(), s.fileName(), s.version(),
                        f.displayName(),
                        member == null ? f.canonicalId() : member.modId(),
                        member == null ? "" : member.loader(),
                        member == null ? "" : member.note(),
                        member != null && member.isCanonical()));
            }
        }
    }

    // F3+C 手动触发的调试崩溃不是模组的问题，报告里得说清楚
    static boolean isManualDebugCrash(LogAnalyzer.Scan scan) {
        return scan.manualCrash();
    }

    // 图形类模组的专项检查。
    // 钠、铷、Embeddium、Xenon 是同一件东西的不同分支，iris、Oculus、NeOculus 同理。
    // 它们的冲突不会在堆栈里留下痕迹——游戏要么直接崩在渲染线程，
    // 要么画面黑掉、光影加载失败，日志里只有一句看不懂的 NoClassDefFoundError。
    // 所以这里不看堆栈，只看装了什么。
    private static void analyzeGraphics(Diagnosis.Report report, ClassIndex index) {
        List<ModSource> sources = index.sources();
        if (sources.isEmpty()) {
            return;
        }

        // 1. 同一族装了多个互斥分支
        for (GraphicsMods.Conflict c : GraphicsMods.findFamilyConflicts(sources)) {
            report.addGraphicsIssue(Diagnosis.GraphicsIssue.conflict(
                    c.family().displayName(), c.family().canonicalId(),
                    c.fileNames(), c.memberIds()));
        }

        // 2. 光影加载器缺了配套的渲染主体
        for (GraphicsMods.MissingPartner m
                : GraphicsMods.findMissingPartners(sources, ModSource.indexByModId(sources))) {
            report.addGraphicsIssue(Diagnosis.GraphicsIssue.missingPartner(
                    m.shaderLoader().display(), m.shaderLoader().fileName(), m.wanted()));
        }

        // 3. 列一下装了哪些图形类模组，出渲染问题时这份清单最有用
        for (ModSource s : sources) {
            Set<GraphicsMods.Family> fams = GraphicsMods.familiesOf(s);
            if (fams.isEmpty()) {
                continue;
            }
            String id = s.modIds().isEmpty() ? "" : s.modIds().get(0);
            GraphicsMods.Member member = GraphicsMods.memberOf(id);
            for (GraphicsMods.Family f : fams) {
                report.addGraphicsMod(new Diagnosis.GraphicsModInfo(
                        s.display(), s.fileName(), s.version(),
                        f.displayName(),
                        member == null ? f.canonicalId() : member.modId(),
                        member == null ? "" : member.loader(),
                        member == null ? "" : member.note(),
                        member != null && member.isCanonical()));
            }
        }
    }

    // 时间线最多记这么多条，再多报告就没人看了
    private static final int TIMELINE_CAPACITY = 400;
    // 同一条文案连着重复超过这么多次就折叠掉，否则一段刷屏会把时间线冲垮
    private static final int TIMELINE_REPEAT_LIMIT = 3;

    // 把日志正文串成一条时间线。
    // 只收有意义的那几类：模组自己的输出、命中了规则的、以及 ERROR/WARN 级别的行。
    // 每一条都尽量标出是哪个模组在说话——先拿 logger 名去索引里查，
    // 查不到再看这一行的文本里提到了哪个 modId。
    public static Timeline buildTimeline(String text, ClassIndex index, Map<String, ModSource> byModId) {
        Timeline timeline = new Timeline(TIMELINE_CAPACITY);
        if (text == null || text.isEmpty()) {
            return timeline;
        }
        List<Timeline.Entry> collected = new ArrayList<>();
        String lastText = "";
        int repeats = 0;
        String pendingTime = "";
        for (String raw : text.split("\r\n|\n|\r", -1)) {
            String time = Timeline.extractTime(raw);
            if (!time.isEmpty()) {
                pendingTime = time;
            }

            // 级别要从原始行里读：剪掉前缀之后 "线程/级别" 那一段就没了，
            // 再去正文里找 /ERROR 是找不到的
            String level = Timeline.extractLevel(raw);
            Timeline.Type type = classifyForTimeline(trimPrefix(raw), level);
            if (type == null) {
                continue;
            }
            String logger = Timeline.extractLogger(raw);
            ModSource owner = Timeline.ownerOf(logger, byModId, index);
            if (owner == null) {
                owner = ownerByMention(raw, byModId);
            }

            // 把时间戳和级别前缀剪掉，只留正文，报告里好读
            String body = trimPrefix(raw);
            if (body.isBlank()) {
                continue;
            }
            if (body.equals(lastText)) {
                repeats++;
                if (repeats > TIMELINE_REPEAT_LIMIT) {
                    continue;
                }
                if (repeats == TIMELINE_REPEAT_LIMIT) {
                    collected.add(new Timeline.Entry(pendingTime, "-", "",
                            Lang.ref("timeline.repeated", TIMELINE_REPEAT_LIMIT),
                            Timeline.Type.INFO, ""));
                    continue;
                }
            } else {
                repeats = 0;
            }
            lastText = body;

            String modId = owner == null ? "-" : firstId(owner);
            String modName = owner == null ? "" : displayName(owner);
            collected.add(new Timeline.Entry(pendingTime, modId, modName,
                    Lang.ref("timeline.raw", shorten(body, 200)), type, logger));
        }

        // 日志动辄几万行，最后那几百条多半是无关的例行输出。
        // 真正要看的是"出错那一刻以及紧挨着它之前"，所以先按重要程度挑，
        // 再按原始顺序放回去，保证读起来还是时间顺序
        keepMostRelevant(collected, TIMELINE_CAPACITY).forEach(timeline::add);
        return timeline;
    }

    // 从一堆事件里挑出最值得看的那些。
    // 排序依据是"离结尾多近"和"有多严重"：致命和错误一律留，普通信息太少时
    // 才轮到它们。这样即使日志末尾全是例行刷屏，也能把出事前后的关键几条留下来。
    static List<Timeline.Entry> keepMostRelevant(List<Timeline.Entry> all, int limit) {
        if (all.size() <= limit) {
            return all;
        }
        record Scored(int index, Timeline.Entry entry, long score) {
        }
        List<Scored> scored = new ArrayList<>(all.size());
        int n = all.size();
        for (int i = 0; i < n; i++) {
            Timeline.Entry e = all.get(i);
            // 越靠后（越接近出事）分越高
            long recency = i;
            long severity = switch (e.type()) {
                case FATAL -> 1_000_000L;
                case ERROR -> 500_000L;
                case RULE -> 200_000L;
                case WARN -> 50_000L;
                case LIFECYCLE -> 5_000L;
                case INFO -> 0L;
            };
            // 归属到具体模组的事件更值钱
            long owned = "-".equals(e.modId()) ? 0L : 20_000L;
            scored.add(new Scored(i, e, severity + owned + recency));
        }
        scored.sort((a, b) -> Long.compare(b.score(), a.score()));
        List<Scored> top = new ArrayList<>(scored.subList(0, limit));
        top.sort(java.util.Comparator.comparingInt(Scored::index));
        List<Timeline.Entry> out = new ArrayList<>(limit);
        for (Scored s : top) {
            out.add(s.entry());
        }
        return out;
    }

    // 这一行值不值得进时间线；不值得返回 null。
    // body 应该是**剪掉日志前缀之后**的正文。栈帧在日志里往往写成
    // {@code [12:00:00] [Render thread/ERROR]:   at xxx}，缩进在前缀后面，
    // 只看原始行的缩进是拦不住的。
    // level 从原始行里单独读出来，因为剪掉前缀之后级别标记就没了。
    private static Timeline.Type classifyForTimeline(String body, String level) {
        String t = body == null ? "" : body.trim();
        if (t.isEmpty()) {
            return null;
        }
        // 栈帧本身不进时间线，但异常头要进：崩在哪儿是最关键的一件事。
        // 崩溃报告里没有日志行，整条时间线基本就靠这几条撑起来
        if (t.startsWith("at ") || t.startsWith("...")) {
            return null;
        }
        if (t.startsWith("Caused by:") || t.startsWith("Suppressed:")) {
            return Timeline.Type.FATAL;
        }
        if (EXCEPTION_HEADLINE.matcher(t).matches()) {
            return Timeline.Type.FATAL;
        }
        if (t.startsWith("/") || t.startsWith("#") || t.startsWith("----")) {
            return null;
        }
        // 崩溃报告和日志里的结构行不算事件
        if (t.startsWith("Time:") || t.startsWith("Description:")
                || t.startsWith("-- ") || t.startsWith("//")) {
            return null;
        }
        if ("FATAL".equals(level)) {
            return Timeline.Type.FATAL;
        }
        if ("ERROR".equals(level)) {
            return Timeline.Type.ERROR;
        }
        if ("WARN".equals(level)) {
            return Timeline.Type.WARN;
        }
        // 命中了规则的照样收，哪怕级别只是 INFO
        if (LogAnalyzer.classifyLine(t).isPresent()) {
            return Timeline.Type.RULE;
        }
        // 模组加载、注册这类结构性事件值得记一笔
        if (t.contains("Found mod file") || t.contains("Loading ") && t.contains("mods")
                || t.contains("Mod List") || t.contains("registered")) {
            return Timeline.Type.LIFECYCLE;
        }
        return null;
    }

    // 异常头，形如 java.lang.NullPointerException: 消息
    private static final java.util.regex.Pattern EXCEPTION_HEADLINE =
            java.util.regex.Pattern.compile(
                    "[\\w.$]+(?:Exception|Error|Throwable|Failure)(?::.*)?");

    // 这一行里提到了哪个模组的 modId
    private static ModSource ownerByMention(String line, Map<String, ModSource> byModId) {
        if (byModId == null || byModId.isEmpty()) {
            return null;
        }
        String lower = line.toLowerCase(java.util.Locale.ROOT);
        for (Map.Entry<String, ModSource> e : byModId.entrySet()) {
            String id = e.getKey();
            // 太短的 modId 容易误伤，跳过
            if (id.length() < 4) {
                continue;
            }
            if (lower.contains(id.toLowerCase(java.util.Locale.ROOT))) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String firstId(ModSource s) {
        List<String> ids = s.modIds();
        return ids.isEmpty() ? s.fileName() : ids.get(0);
    }

    private static String displayName(ModSource s) {
        String n = s.display();
        return n == null || n.isBlank() ? s.fileName() : n;
    }

    // 剪掉 "[时间] [线程/级别] [logger]:" 这一截，只留正文
    private static String trimPrefix(String raw) {
        String t = raw.trim();
        int depth = 0;
        int i = 0;
        // 最多跳三段方括号
        while (i < t.length() && depth < 3) {
            if (t.charAt(i) == '[') {
                int close = t.indexOf(']', i);
                if (close < 0) {
                    break;
                }
                i = close + 1;
                depth++;
                while (i < t.length() && t.charAt(i) == ' ') {
                    i++;
                }
            } else {
                break;
            }
        }
        String rest = i > 0 ? t.substring(i) : t;
        if (rest.startsWith(":")) {
            rest = rest.substring(1).trim();
        }
        return rest;
    }

    private static String shorten(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    // 模组列表里有没有 Sinytra Connector。有的话 Fabric 模组也会出现在栈帧里。
    private static boolean hasConnector(ClassIndex index) {
        if (index == null) {
            return false;
        }
        for (ModSource s : index.sources()) {
            for (String id : s.modIds()) {
                if (id.equals("connectormod") || id.equals("connector")
                        || id.equals("sinytraconnector")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String rootOf(String className) {
        if (className == null || className.startsWith("<")) {
            return "?";
        }
        String[] segs = className.split("\\.");
        if (segs.length <= 2) {
            return className;
        }
        return segs[0] + "." + segs[1];
    }

    private record MentionProbe(String needle) {
        static MentionProbe of(String id) {
            return new MentionProbe(id);
        }

        boolean mentions(String haystack) {
            int from = 0;
            while (true) {
                int idx = haystack.indexOf(needle, from);
                if (idx < 0) {
                    return false;
                }
                boolean leftOk = idx == 0 || !Character.isLetterOrDigit(haystack.charAt(idx - 1));
                int end = idx + needle.length();
                boolean rightOk = end >= haystack.length() || !Character.isLetterOrDigit(haystack.charAt(end));
                if (leftOk && rightOk) {
                    return true;
                }
                from = idx + 1;
            }
        }
    }
}
