// 游戏内那一半：维护类索引和日志窗口，出错时分析并给出结论。
package dev.oba.mod;

import dev.oba.core.Analyzer;
import dev.oba.core.ClassIndex;
import dev.oba.core.Diagnosis;
import dev.oba.core.LogAnalyzer;
import dev.oba.core.LogWatcher;
import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.core.ModSource;
import dev.oba.core.ProcessProbe;
import dev.oba.core.Reports;
import dev.oba.loader.LoadedMod;
import dev.oba.loader.Loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class Diagnostics {

    private static final int LOG_WINDOW = 400;
    private static final long DEBOUNCE_MILLIS = 2500L;

    private static Diagnostics instance;

    private final LogWatcher.RingBuffer window = new LogWatcher.RingBuffer(LOG_WINDOW);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Oba-analyser");
                t.setDaemon(true);
                return t;
            });
    private final AtomicBoolean analysisQueued = new AtomicBoolean(false);
    private final AtomicInteger capturedErrors = new AtomicInteger();
    private final AtomicInteger capturedFatals = new AtomicInteger();
    private final List<String> recentVerdicts = new ArrayList<>();

    private volatile Path gameDir;
    private volatile List<ModSource> sources = List.of();
    private volatile ClassIndex index = ClassIndex.empty();
    private volatile Map<String, ModSource> byModId = Map.of();
    private volatile Diagnosis.Report lastReport;
    // 点出元凶时的错误累计数，-1 表示还没点出过元凶
    private final java.util.concurrent.atomic.AtomicInteger errorsAtVerdict =
            new java.util.concurrent.atomic.AtomicInteger(-1);
    // 点出过元凶的那一份分析结果。退出时优先写它：
    // 帧滚出日志窗口之后，最后几次分析往往什么都找不到，
    // 拿那份空结论去覆盖有用的那一份是本末倒置
    private volatile Diagnosis.Report culpritReport;
    private volatile String lastCrashSummary = "";
    private volatile boolean indexing;
    private volatile Consumer<String> output = s -> { };

    private final List<LogWatcher> watchers = new ArrayList<>();
    private final CrashReporter reporter = new CrashReporter(this);
    // 致命错误是否立刻分析。索引建好之前不乱分析，出来的报告没有归属信息
    private volatile boolean armed;
    // 索引建好之前就撞上了致命错误，先记着，等索引好了补一次分析
    private volatile boolean verdictPending;
    private final java.util.concurrent.atomic.AtomicBoolean analysisRunning =
            new java.util.concurrent.atomic.AtomicBoolean();

    private Diagnostics() {
    }

    public static synchronized Diagnostics get() {
        if (instance == null) {
            instance = new Diagnostics();
        }
        return instance;
    }

    // 自己的输出往哪写：游戏日志，或者测试里接的收集器
    public void setOutput(Consumer<String> sink) {
        this.output = sink == null ? s -> { } : sink;
    }

    public Path gameDir() {
        return gameDir;
    }

    public ClassIndex index() {
        return index;
    }

    public Map<String, ModSource> byModId() {
        return byModId;
    }

    public List<ModSource> sources() {
        return sources;
    }

    public Optional<Diagnosis.Report> lastReport() {
        return Optional.ofNullable(lastReport);
    }

    public String lastCrashSummary() {
        return lastCrashSummary;
    }

    public int capturedErrors() {
        return capturedErrors.get();
    }

    public int capturedFatals() {
        return capturedFatals.get();
    }

    public List<String> recentVerdicts() {
        synchronized (recentVerdicts) {
            return List.copyOf(recentVerdicts);
        }
    }

    // 建索引

    // 装一份别处建好的模组清单。Mod 自己走 reindex() 去发现模组；
    // 这个入口是给已经知道清单的调用方用的，比如没有 FML 运行时的验证台
    public synchronized void adoptIndex(List<ModSource> newSources) {
        List<ModSource> copy = List.copyOf(newSources);
        ClassIndex built = Analyzer.buildIndex(copy);
        this.sources = copy;
        this.index = built;
        this.byModId = ModSource.indexByModId(copy);
    }

    // 已经采集到的那些行，原样返回，只读
    public List<String> logWindowSnapshot() {
        return window.snapshot();
    }

    // 游戏目录。FML 说不上来时就退回当前工作目录
    public Path resolveGameDir() {
        Path cached = gameDir;
        if (cached != null) {
            return cached;
        }
        Path resolved = null;
        try {
            // 交给各轨道的加载器实现：Forge 和 NeoForge 的取法不一样
            resolved = Loader.env().gameDir();
        } catch (Throwable ignored) {
            // 走下面的兜底逻辑
        }
        if (resolved == null || !Files.isDirectory(resolved)) {
            resolved = Analyzer.findGameDir(java.nio.file.Paths.get(""))
                    .orElse(java.nio.file.Paths.get("").toAbsolutePath());
        }
        gameDir = resolved;
        return resolved;
    }

    // 按 FML 自己的模组列表重建索引，这样不在 mods 目录里的 jar
    //（开发环境、内嵌 jar）也能覆盖到
    public synchronized void reindex(boolean async, Runnable whenDone) {
        if (indexing) {
            return;
        }
        indexing = true;
        Runnable job = () -> {
            try {
                doReindex();
            } catch (Throwable t) {
                output.accept(Oba.LOG_PREFIX + Lang.get("cmd.badPath", t));
            } finally {
                indexing = false;
                if (whenDone != null) {
                    whenDone.run();
                }
            }
        };
        if (async) {
            scheduler.execute(job);
        } else {
            job.run();
        }
    }

    private void doReindex() {
        long start = System.currentTimeMillis();
        List<ModSource> found = new ArrayList<>();
        try {
            for (LoadedMod mod : Loader.env().mods()) {
                try {
                    Path file = mod.file();
                    if (file != null && !containsPath(found, file)) {
                        found.add(ModSource.of(file));
                    }
                } catch (Throwable ignored) {
                    // 解析不了的条目就不进索引，没别的办法
                }
            }
        } catch (Throwable t) {
            output.accept(Oba.LOG_PREFIX + Lang.get("mod.indexFailed", t));
        }
        if (found.isEmpty()) {
            // 开发环境或者调用得太早，退回扫磁盘
            Path dir = resolveGameDir().resolve("mods");
            found.addAll(Analyzer.loadMods(List.of(dir)));
        }
        ClassIndex built = Analyzer.buildIndex(found);
        this.sources = List.copyOf(found);
        this.index = built;
        this.byModId = ModSource.indexByModId(this.sources);
        long ms = System.currentTimeMillis() - start;
        output.accept(Oba.LOG_PREFIX + Lang.get("mod.indexDone",
                sources.size(), built.classCountTotal(), built.packageCountTotal(), ms));
        // 索引建好之前撞上的致命错误，现在可以给一份像样的结论了
        if (verdictPending) {
            verdictPending = false;
            scheduler.execute(this::runAnalysis);
        }
    }

    private static boolean containsPath(List<ModSource> list, Path p) {
        for (ModSource s : list) {
            if (s.path().equals(p.toAbsolutePath().normalize())) {
                return true;
            }
        }
        return false;
    }

    // 收日志

    // 每采集到一行就会调一次，所以这里要便宜
    public void onLogLine(String line, boolean error, boolean fatal) {
        if (line == null || line.isEmpty()) {
            return;
        }
        if (isSelfOutput(line)) {
            return;
        }
        window.add(line);
        if (error) {
            capturedErrors.incrementAndGet();
        }
        if (fatal) {
            capturedFatals.incrementAndGet();
        }
        if (fatal && armed) {
            // 已经是致命错误，多半马上就崩了。等去抖动可能等不到进程退出，
            // 所以立刻出一份结论，别把这次运行白丢
            runAnalysis();
        } else if (error) {
            // 不一定致命但也是错误的行同样值得看一眼，靠去抖动让一串报错只出一份报告
            scheduleAnalysis();
        } else if (fatal) {
            // 索引还没建好就出错了。这时候分析只会得到一份"没有栈帧能归属到模组"的
            // 结论，先记下来，等索引建好再补一次
            verdictPending = true;
        }
    }

    // 判断这一行是不是我们自己打出来的。
    // 实机踩过两次坑：一是我们往日志里写的结论会被文件监听读回来，正文里的
    // {@code java.lang.XxxException} 看着就是异常开头；二是 {@code /oba analyze}
    // 的结果会走聊天栏渲染一遍，那一份连日志前缀都没有，只有 {@code [CHAT]}。
    // 这两种都会当成新错误再分析一遍，滚出一串越来越大的报告。
    public static boolean isSelfOutput(String line) {
        if (line.contains(Oba.LOG_PREFIX)) {
            return true;
        }
        // 聊天栏渲染的行不进窗口。模组往聊天栏发言的情况很少，
        // 但自己诊断自己这个坑一定会踩，先挡掉
        return line.contains("[CHAT]");
    }

    // 致命错误立刻分析，而不是等去抖动。装好索引之后才启用，
    // 否则模组还没加载完就分析，只会得到一份没有归属结果的报告
    public void armImmediateAnalysis() {
        armed = true;
        if (verdictPending) {
            verdictPending = false;
            runAnalysis();
        }
    }

    private void runAnalysis() {
        if (!analysisRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            Diagnosis.Report report = analyzeWindow();
            if (report != null) {
                announce(report);
            }
        } catch (Throwable t) {
            output.accept(Oba.LOG_PREFIX + Lang.get("mod.analysisFailed", t));
        } finally {
            analysisRunning.set(false);
        }
    }

    private void scheduleAnalysis() {
        if (!analysisQueued.compareAndSet(false, true)) {
            return;
        }
        scheduler.schedule(() -> {
            analysisQueued.set(false);
            runAnalysis();
        }, DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS);
    }

    // 用引擎跑一遍当前日志窗口。
    //
    // 光看日志窗口是不够的：模组加载阶段失败时，真正带栈帧的异常只写进
    // crash-reports 下的文件，latest.log 里只有一行"Mod Loading has failed"。
    // 而且加载阶段的报错发生在启动早期，等日志窗口滚过 400 行，
    // 那几帧就被挤出去了 —— 实测出现过第一次分析能定位到元凶、
    // 之后的几次反而说"没有栈帧能归属到某个模组"的情况。
    //
    // 所以分两步：先按日志窗口分析；如果什么都没归属到，而本次运行又落了
    // 崩溃报告，就改用崩溃报告本身再分析一次。崩溃报告是完整文档，
    // 不受窗口滚动影响，这才是可靠的证据来源。
    public Diagnosis.Report analyzeWindow() {
        List<String> lines = new ArrayList<>(window.snapshot());
        if (lines.isEmpty()) {
            return null;
        }

        // 先把上一次的结论留下来：下面这次分析会把 lastReport 顶掉
        Diagnosis.Report previous = lastReport;

        Diagnosis.Report fromWindow = analyzeLines(lines, "report.subject.gameLog");
        if (fromWindow != null && fromWindow.primarySuspect() != null) {
            return fromWindow;
        }

        // 日志窗口没定位到元凶时，不要急着用这份"什么都没找到"的结论
        // 覆盖掉上一次已经定位到元凶的结果：帧滚出窗口之后，
        // 后续的窗口分析本来就会逐步失去证据，越分析越不准。
        if (previous != null && previous.primarySuspect() != null
                && capturedErrors.get() + capturedFatals.get() > 0) {
            return previous;
        }

        // 本次运行落了崩溃报告的话，改用崩溃报告本身再分析一次。
        // 它是完整文档，不受日志窗口滚动影响，是更可靠的证据来源
        Path crash = newestSessionCrashReport();
        if (crash != null) {
            try {
                Diagnosis.Report fromCrash = analyzeFile(crash);
                if (fromCrash != null && fromCrash.primarySuspect() != null) {
                    return fromCrash;
                }
            } catch (Throwable ignored) {
                // 读不出来就还是用日志窗口的结果
            }
        }
        return fromWindow;
    }

    // 本次运行期间新落下来的崩溃报告里最新的那一份，没有就返回 null
    private Path newestSessionCrashReport() {
        Path best = null;
        for (Path p : reporter.newCrashReports()) {
            if (best == null || p.getFileName().toString().compareTo(best.getFileName().toString()) > 0) {
                best = p;
            }
        }
        return best;
    }

    // 把一组行喂给引擎跑一遍
    private Diagnosis.Report analyzeLines(List<String> lines, String subjectKey) {
        Analyzer.Options opt = new Analyzer.Options();
        opt.index = index;
        opt.byModId = byModId;
        opt.detectMcreator = false;
        opt.subjectKey = subjectKey;
        opt.subjectArgs = java.util.List.of(lines.size());
        opt.titleKey = "report.title.runtime";
        Diagnosis.Report report = Analyzer.analyze(String.join("\n", lines), opt);
        report.scannedMods().clear();
        report.scannedMods().addAll(sources);
        report.environment().put("__classes", String.valueOf(index.classCountTotal()));
        this.lastReport = report;
        if (report.primarySuspect() != null) {
            this.culpritReport = report;
        }
        return report;
    }

    // 完整分析用户指定的文件
    public Diagnosis.Report analyzeFile(Path file) throws IOException {
        Analyzer.Options opt = new Analyzer.Options();
        opt.index = index;
        opt.byModId = byModId;
        opt.detectMcreator = true;
        opt.subject = file.toString();
        opt.titleKey = "report.title.log";
        Diagnosis.Report report = Analyzer.analyzeFile(file, opt);
        report.scannedMods().clear();
        report.scannedMods().addAll(sources);
        report.environment().put("__classes", String.valueOf(index.classCountTotal()));
        this.lastReport = report;
        if (report.primarySuspect() != null) {
            this.culpritReport = report;
        }
        return report;
    }

    // 分析磁盘上最新的崩溃报告，有的话
    public Optional<Diagnosis.Report> analyzeNewestCrashReport() {
        Optional<Path> crash = Analyzer.newestCrashReport(resolveGameDir());
        if (crash.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(analyzeFile(crash.get()));
        } catch (IOException e) {
            output.accept(Oba.LOG_PREFIX + Lang.get("mod.readFailed", e.getMessage()));
            return Optional.empty();
        }
    }

    // 往日志里打一句简短结论，完整报告留着随时可查
    private void announce(Diagnosis.Report report) {
        Diagnosis.Finding primary = report.primarySuspect();
        StringBuilder sb = new StringBuilder();
        if (primary == null) {
            if (report.issues().isEmpty()) {
                return;
            }
            Diagnosis.KnownIssue first = report.issues().get(0);
            sb.append(Oba.LOG_PREFIX)
              .append(Lang.get("mod.capturedNoOwner", first.title(), first.detail()));
        } else {
            sb.append(Oba.LOG_PREFIX)
              .append(Lang.get("mod.locate", primary.display(), primary.fileName()));
            var frame = primary.firstLocatedFrame();
            if (frame != null) {
                sb.append("\n").append(Oba.LOG_PREFIX).append(Lang.get("mod.location", frame.location()));
            }
            sb.append("\n").append(Oba.LOG_PREFIX).append(Lang.get("mod.confidence",
                    primary.confidence().label(), Diagnosis.Confidence.percent(primary.score())));
            if (!primary.throwables().isEmpty()) {
                sb.append("\n").append(Oba.LOG_PREFIX)
              .append(Lang.get("mod.exception", primary.throwables().get(0)));
            }
            sb.append("\n").append(Oba.LOG_PREFIX)
              .append(Lang.get("mod.reportHint"));
            lastCrashSummary = primary.fileName() + " → " + (frame == null ? "?" : frame.location());
        }
        // 出了事就把完整报告落到 logs 目录，玩家什么都不用做
        Path written = reporter.writeIfNew(report, Lang.get("report.reason.crash"));
        if (written != null) {
            sb.append("\n").append(Oba.LOG_PREFIX).append(Lang.get("report.written", written));
        }
        String text = sb.toString();
        output.accept(text);
        synchronized (recentVerdicts) {
            recentVerdicts.add(text);
            while (recentVerdicts.size() > 20) {
                recentVerdicts.remove(0);
            }
        }
    }

    // 检查这次运行期间有没有新落下来的崩溃报告，有就分析并出一份报告。
    // F3+C 那种手动调试崩溃完全不经过日志：异常不走 log4j2，聊天栏和 latest.log
    // 里一个字都没有，只有 crash-reports 下多了一个文件。不在退出时补这一刀，
    // 这类崩溃就只能被整个漏掉。
    public void pickUpNewCrashReports() {
        for (Path crash : reporter.newCrashReports()) {
            try {
                Diagnosis.Report report = analyzeFile(crash);
                Path written = reporter.write(report, Lang.get("report.reason.newCrash"));
                output.accept(Oba.LOG_PREFIX + Lang.get("report.crashFound", crash));
                if (written != null) {
                    output.accept(Oba.LOG_PREFIX + Lang.get("report.written", written));
                }
            } catch (Throwable t) {
                output.accept(Oba.LOG_PREFIX
                        + Lang.get("report.crashFailed", crash, String.valueOf(t)));
            }
        }
    }

    // 手动把最近一份报告写进 logs 目录，游戏内命令用。
    public Path writeReportNow() {
        Diagnosis.Report report = lastReport;
        if (report == null) {
            report = analyzeWindow();
        }
        return reporter.write(report, Lang.get("report.reason.manual"));
    }

    // 游戏退出时兜底写一份，保证这次会话的诊断结果留得下来。
    public void writeReportOnExit() {
        // 先看有没有新崩溃报告。日志里什么都抓不到的那种（比如 F3+C），
        // 以及模组加载阶段只把栈帧写进报告文件、日志里只有一行的那种，
        // 都只能靠这一步补上
        pickUpNewCrashReports();
        Diagnosis.Report report = lastReport;
        // 最后一次分析没点出元凶、但之前点出过，就写之前那一份
        if ((report == null || report.primarySuspect() == null) && culpritReport != null) {
            report = culpritReport;
        }
        if (report == null) {
            // 什么都没分析过（比如刚进主菜单就退出），这时才真的没东西可写。
            // 注意不能拿"捕获到多少条错误"当条件：崩溃报告里已经带着
            // 完整的异常和栈帧，但日志侧可能一条 ERROR 都没记下来，
            // 那样会把最有用的一份报告挡掉
            return;
        }
        Path written = reporter.writeOnShutdown(report);
        if (written != null) {
            output.accept(Oba.LOG_PREFIX + Lang.get("report.written", written));
        }
    }

    // 报告会写到哪，状态命令里显示用。
    public Path reportDirectory() {
        return reporter.reportDir();
    }

    // 点出过元凶的那一份，没有就是 null
    Diagnosis.Report culpritReport() {
        return culpritReport;
    }

    // 记下启动时已有的崩溃报告
    public void rememberExistingCrashReports() {
        reporter.rememberExistingCrashReports();
    }

    // 点出元凶的那一刻，记下当时累计的错误条数。
    // 之后如果又冒出新的错误，说明可能是另一个独立故障，空结论就允许覆盖了
    void markVerdict() {
        errorsAtVerdict.set(capturedErrors.get() + capturedFatals.get());
    }

    // 点出元凶之后又出现了新的错误行吗
    boolean hasNewErrorsSinceVerdict() {
        int at = errorsAtVerdict.get();
        return at >= 0 && capturedErrors.get() + capturedFatals.get() > at;
    }

    // 这次运行期间新出现的崩溃报告
    public java.util.List<Path> newCrashReports() {
        return reporter.newCrashReports();
    }

    // 某个模组涉及到的图形类问题，游戏内 info 命令要显示
    public java.util.List<Diagnosis.GraphicsIssue> graphicsIssuesFor(ModSource s) {
        if (s == null || lastReport == null) {
            return java.util.List.of();
        }
        java.util.List<Diagnosis.GraphicsIssue> out = new java.util.ArrayList<>();
        java.util.Set<dev.oba.core.GraphicsMods.Family> fams =
                dev.oba.core.GraphicsMods.familiesOf(s);
        for (Diagnosis.GraphicsIssue gi : lastReport.graphicsIssues()) {
            if (gi.kind() == Diagnosis.GraphicsIssue.Kind.FAMILY_CONFLICT) {
                // 只有这个文件确实属于那一族，才算跟它有关
                for (dev.oba.core.GraphicsMods.Family f : fams) {
                    if (f.key().equals(gi.canonicalId())) {
                        out.add(gi);
                        break;
                    }
                }
            } else if (gi.kind() == Diagnosis.GraphicsIssue.Kind.MISSING_PARTNER) {
                if (s.fileName().equals(gi.fileName())) {
                    out.add(gi);
                }
            }
        }
        return out;
    }

    // 给验证台看的：最近分析出的那份报告
    public Diagnosis.Report lastReportForTest() {
        return lastReport;
    }

    // 盯文件

    // 把 logs/latest.log 当第二个来源盯着。进程内的日志采集器已经覆盖了，
    // 这里主要是采集器挂不上时的兜底
    public synchronized void startTailing() {
        try {
            Path log = resolveGameDir().resolve("logs/latest.log");
            if (!Files.isRegularFile(log)) {
                return;
            }
            LogWatcher watcher = new LogWatcher(log, event -> {
                // 文件里同样会出现我们自己写进去的结论。这里必须和进程内采集器用同一个
                // 判断挡掉，否则结论会被读回来当成新错误再分析一遍，滚出一串
                // 越嵌越大的报告（实机在 1.21.1 上踩到过，一秒能滚几十行）
                if (isSelfOutput(event.line())) {
                    return;
                }
                window.add(event.line());
                if (event.isIssue() && event.issue().severity() == Diagnosis.Severity.FATAL) {
                    scheduleAnalysis();
                }
            }, 400);
            watcher.startDaemon("Oba-logwatch");
            watchers.add(watcher);
            output.accept(Oba.LOG_PREFIX + Lang.get("mod.tailingStarted", log));
        } catch (Throwable t) {
            output.accept(Oba.LOG_PREFIX + Lang.get("mod.tailingFailed", t));
        }
    }

    // 日志窗口当前有多少行，状态里显示用
    public int windowSize() {
        return window.size();
    }

    // 进程信息加上 Oba 自己的计数
    public Map<String, String> statusFacts() {
        Map<String, String> out = new LinkedHashMap<>(ProcessProbe.processFacts());
        out.put(Lang.get("status.indexedMods"), String.valueOf(sources.size()));
        out.put(Lang.get("status.indexedClasses"), String.valueOf(index.classCountTotal()));
        out.put(Lang.get("status.logWindow"), Lang.get("status.lines", windowSize(), LOG_WINDOW));
        out.put(Lang.get("status.captured"), Lang.get("status.errorLines", capturedErrors.get(), capturedFatals.get()));
        List<String> deadlocks = ProcessProbe.findDeadlockedThreads();
        out.put(Lang.get("status.deadlocks"), deadlocks.isEmpty() ? Lang.get("status.none") : String.join(", ", deadlocks));
        out.put(Lang.get("status.reportDir"), String.valueOf(reportDirectory()));
        return out;
    }

    // 渲染最近一份报告；没有的话说明为什么没有
    public String renderLastReport() {
        Diagnosis.Report report = lastReport;
        if (report == null) {
            return Lang.get("cmd.report.none");
        }
        return Reports.text(report, false);
    }

    // 存下看门狗给出的卡死结论（来自线程转储）
    public void keepStallReport(String verdict, Diagnosis.Report report) {
        this.lastReport = report;
        String frame = report.primarySuspect() == null ? "" : report.primarySuspect().fileName();
        if (!frame.isBlank()) {
            this.lastCrashSummary = Lang.get("mod.stall.executing", frame, "");
        }
        Path written = reporter.writeIfNew(report, Lang.get("report.reason.crash"));
        if (written != null) {
            verdict = verdict + "\n" + Oba.LOG_PREFIX + Lang.get("report.written", written);
        }
        output.accept(verdict);
        synchronized (recentVerdicts) {
            recentVerdicts.add(verdict);
            while (recentVerdicts.size() > 20) {
                recentVerdicts.remove(0);
            }
        }
    }

    // 游戏退出时把后台线程收掉
    public void shutdown() {
        for (LogWatcher w : watchers) {
            w.close();
        }
        watchers.clear();
        scheduler.shutdownNow();
    }

    // 装一个 JVM 关闭钩子。崩溃到 FML 都来不及收场时，
    // 事件可能不会触发，但关闭钩子基本都会跑，报告还能留下来。
    public void installExitHook() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    writeReportOnExit();
                } catch (Throwable t) {
                    // 退出阶段，能写则写，写不了就算了。
                    // 这一句走 stderr 而不是 log4j2：此刻日志系统可能已经关了，
                    // 走日志的话这句本身就会丢掉
                    System.err.println(Oba.LOG_PREFIX + Lang.get("mod.exitHookFailed", t));
                }
            }, "Oba-exit"));
        } catch (Throwable ignored) {
            // 已经在关闭过程中，加不上钩子也无妨
        }
    }
}