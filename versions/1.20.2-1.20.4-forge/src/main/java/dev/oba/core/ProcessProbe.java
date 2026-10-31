// 进程层面的诊断：解析线程转储，判断卡住时是哪个模组的代码在跑。
package dev.oba.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProcessProbe {

    private static final Pattern THREAD_HEADER = Pattern.compile("^\"([^\"]+)\"(?:.*?)(?:\\s+prio=\\S+)?.*$");
    private static final Pattern STATE = Pattern.compile("java\\.lang\\.Thread\\.State:\\s*(\\w+)");
    private static final Pattern FRAME = Pattern.compile("^\\s*at\\s+([^\\s(]+)\\((.*)\\)\\s*$");

    // 优先怀疑的线程，按顺序来
    private static final List<String> PRIMARY_THREADS = List.of(
            "Server thread", "Render thread", "main", "Client thread", "Netty Server IO", "Worker-Main");

    private ProcessProbe() {
    }

    public record ThreadInfo(String name, String state, List<StackTraceParser.Frame> frames, boolean daemon) {
        public String shortName() {
            return name.length() > 42 ? name.substring(0, 41) + "…" : name;
        }
    }

    // 解析标准 jstack 文本格式的线程转储
    public static List<ThreadInfo> parseThreadDump(String dump) {
        List<ThreadInfo> out = new ArrayList<>();
        String currentName = null;
        String currentState = "";
        boolean daemon = false;
        List<StackTraceParser.Frame> frames = new ArrayList<>();
        boolean inTrace = false;

        for (String line : dump.split("\r\n|\n|\r", -1)) {
            if (line.isBlank()) {
                if (currentName != null && inTrace) {
                    out.add(new ThreadInfo(currentName, currentState, List.copyOf(frames), daemon));
                }
                currentName = null;
                frames = new ArrayList<>();
                inTrace = false;
                continue;
            }
            if (line.startsWith("\"")) {
                if (currentName != null && inTrace) {
                    out.add(new ThreadInfo(currentName, currentState, List.copyOf(frames), daemon));
                }
                Matcher m = THREAD_HEADER.matcher(line);
                currentName = m.matches() ? m.group(1) : line.replace("\"", "").trim();
                daemon = line.contains("daemon");
                currentState = "";
                frames = new ArrayList<>();
                inTrace = true;
                continue;
            }
            Matcher sm = STATE.matcher(line.trim());
            if (sm.find()) {
                currentState = sm.group(1);
                continue;
            }
            Matcher fm = FRAME.matcher(line);
            if (fm.matches()) {
                String sig = fm.group(1);
                String paren = fm.group(2);
                int lastDot = sig.lastIndexOf('.');
                if (lastDot > 0) {
                    boolean nativeMethod = paren.contains("Native Method");
                    String src = null;
                    int ln = 0;
                    if (!nativeMethod) {
                        int colon = paren.lastIndexOf(':');
                        if (colon > 0) {
                            src = paren.substring(0, colon).trim();
                            try {
                                ln = Integer.parseInt(paren.substring(colon + 1).trim());
                            } catch (NumberFormatException ignored) {
                                ln = 0;
                            }
                        } else {
                            src = paren.trim();
                        }
                    }
                    frames.add(new StackTraceParser.Frame(sig.substring(0, lastDot), sig.substring(lastDot + 1),
                            src, ln, nativeMethod, false, 0, line.trim(), null));
                }
            }
        }
        if (currentName != null && inTrace) {
            out.add(new ThreadInfo(currentName, currentState, List.copyOf(frames), daemon));
        }
        return out;
    }

    // 把线程转储归属到模组。首要怀疑对象就是卡住那个线程当时在跑的代码。
    // hint 是卡住的线程名，通常看门狗已经知道了
    public static Diagnosis.Report analyzeThreadDump(String dump, ClassIndex index,
                                                     Map<String, ModSource> byModId, String hint) {
        List<ThreadInfo> threads = parseThreadDump(dump);
        Diagnosis.Report report = new Diagnosis.Report();
        report.setTitleKey("report.title.stall");
        report.setSubject(hint == null ? Lang.get("cli.threads.dump") : hint);
        report.setGeneratedAt(System.currentTimeMillis());

        if (threads.isEmpty()) {
            report.addNote(Lang.get("note.thread.empty"));
            report.finish(0.0);
            return report;
        }

        Map<ModSource, Diagnosis.Finding> findings = new LinkedHashMap<>();
        int attributed = 0;
        int totalFrames = 0;
        List<String> unmatched = new ArrayList<>();

        // 挑一个背锅的线程：先用明确给的线程名，再看几个常见线程名，
        // 最后退到拥有最多模组栈帧的那个
        ThreadInfo primary = pickPrimary(threads, hint, index);

        for (ThreadInfo t : threads) {
            boolean isPrimary = t == primary;
            double threadWeight = isPrimary ? 1.0 : 0.25;
            int depth = 0;
            for (StackTraceParser.Frame f : t.frames()) {
                totalFrames++;
                depth++;
                if (StackTraceParser.isVanillaOrLoader(f.className())) {
                    continue;
                }
                ClassIndex.Match m = index.resolve(f.className());
                if (m == null) {
                    if (isPrimary && !unmatched.contains(rootOf(f.className()))) {
                        unmatched.add(rootOf(f.className()));
                    }
                    continue;
                }
                attributed++;
                Diagnosis.Finding fd = findings.computeIfAbsent(m.source(), s -> new Diagnosis.Finding(s));
                fd.addFrame(new StackTraceParser.AttributedFrame(m.source(), m, f, f.location()));
                fd.addThrowable(Lang.get("note.thread.summary", t.name(), t.state()));
                double weight = (0.35 + 0.08 * depth) * threadWeight;
                if (isPrimary) {
                    weight += 0.6;
                }
                if (m.confidence() >= 0.999) {
                    weight *= 1.2;
                }
                fd.addScore(weight);
                fd.addEvidence(Diagnosis.Evidence.at("thread.frame", "evidence.thread",
                        java.util.List.of(
                                Lang.get(isPrimary ? "evidence.thread.primary" : "evidence.thread.other"),
                                t.shortName(), t.state(), f.location()),
                        weight, 0, f.location()));
            }
        }

        findings.values().forEach(report::addFinding);
        report.setFrameCount(totalFrames);
        report.setAttributedFrameCount(attributed);
        report.unmatchedModPackages().addAll(unmatched);

        if (primary != null) {
            report.setRootCauseHeadline(Lang.get("note.thread.summary", primary.name(), primary.state()));
            report.addNote(Lang.get("note.stall.notException"));
        }
        report.finish(0.12);
        return report;
    }

    private static ThreadInfo pickPrimary(List<ThreadInfo> threads, String hint, ClassIndex index) {
        if (hint != null && !hint.isBlank()) {
            for (ThreadInfo t : threads) {
                if (t.name().equalsIgnoreCase(hint) || t.name().toLowerCase(Locale.ROOT)
                        .contains(hint.toLowerCase(Locale.ROOT))) {
                    return t;
                }
            }
        }
        for (String wanted : PRIMARY_THREADS) {
            for (ThreadInfo t : threads) {
                if (t.name().equals(wanted)) {
                    return t;
                }
            }
        }
        return threads.stream()
                .max(Comparator.comparingInt(t -> countModFrames(t, index)))
                .orElse(threads.get(0));
    }

    private static int countModFrames(ThreadInfo t, ClassIndex index) {
        int n = 0;
        for (StackTraceParser.Frame f : t.frames()) {
            if (!StackTraceParser.isVanillaOrLoader(f.className()) && index.resolve(f.className()) != null) {
                n++;
            }
        }
        return n;
    }

    private static String rootOf(String className) {
        String[] segs = className.split("\\.");
        return segs.length <= 2 ? className : segs[0] + "." + segs[1];
    }

    // 采集运行中的 JVM

    // 导出当前 JVM 的线程，游戏不再 tick 时靠它看现场。
    // onlyBlocked 为真时只导出持有或等待锁的线程
    public static String captureThreadDump(boolean onlyBlocked) {
        try {
            java.lang.management.ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
            java.lang.management.ThreadInfo[] infos;
            if (onlyBlocked) {
                long[] ids = bean.findMonitorDeadlockedThreads();
                infos = ids == null ? bean.dumpAllThreads(false, true)
                        : bean.getThreadInfo(ids, true, true);
            } else {
                infos = bean.dumpAllThreads(true, false);
            }
            StringBuilder sb = new StringBuilder(64 * 1024);
            sb.append("Oba thread dump @ ").append(java.time.Instant.now()).append('\n');
            for (java.lang.management.ThreadInfo info : infos) {
                if (info == null) {
                    continue;
                }
                sb.append('"').append(info.getThreadName()).append('"')
                  .append(info.isDaemon() ? " daemon" : "")
                  .append(" id=").append(info.getThreadId())
                  .append('\n');
                sb.append("   java.lang.Thread.State: ").append(info.getThreadState().name()).append('\n');
                for (StackTraceElement e : info.getStackTrace()) {
                    sb.append("\tat ").append(e.getClassName()).append('.').append(e.getMethodName())
                      .append('(').append(e.getFileName() == null ? "Unknown Source" : e.getFileName())
                      .append(e.getLineNumber() < 0 ? "" : ":" + e.getLineNumber()).append(")\n");
                }
                sb.append('\n');
            }
            return sb.toString();
        } catch (Throwable t) {
            return Lang.get("cli.threads.failed") + ": " + t;
        }
    }

    // 检测当前进程里的死锁，返回涉及的线程名
    public static List<String> findDeadlockedThreads() {
        List<String> out = new ArrayList<>();
        try {
            java.lang.management.ThreadMXBean bean = java.lang.management.ManagementFactory.getThreadMXBean();
            long[] ids = bean.findDeadlockedThreads();
            if (ids == null) {
                return out;
            }
            for (java.lang.management.ThreadInfo info : bean.getThreadInfo(ids, true, true)) {
                if (info != null) {
                    out.add(info.getThreadName() + " (" + info.getThreadState() + ")");
                }
            }
        } catch (Throwable ignored) {
            // 不是所有 JVM 都支持
        }
        return out;
    }

    // 报告表头要用的进程信息
    public static Map<String, String> processFacts() {
        Map<String, String> out = new LinkedHashMap<>();
        try {
            Runtime rt = Runtime.getRuntime();
            long max = rt.maxMemory() / (1024 * 1024);
            long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
            out.put(Lang.get("status.maxHeap"), max + " MB");
            out.put(Lang.get("status.usedHeap"), used + " MB (" + (max > 0 ? used * 100 / max : 0) + "%)");
            java.lang.management.OperatingSystemMXBean os =
                    java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            out.put(Lang.get("status.processors"), String.valueOf(os.getAvailableProcessors()));
            java.lang.management.ThreadMXBean tb = java.lang.management.ManagementFactory.getThreadMXBean();
            out.put(Lang.get("status.threads"), String.valueOf(tb.getThreadCount()));
            out.put(Lang.get("status.daemons"), String.valueOf(tb.getDaemonThreadCount()));
        } catch (Throwable ignored) {
            // 尽力而为
        }
        try {
            java.io.File dir = new java.io.File(".").getAbsoluteFile();
            out.put(Lang.get("status.workingDir"), dir.toString());
        } catch (Throwable ignored) {
            // 尽力而为
        }
        return out;
    }

    // 把采集结果写到文件，用户要的时候才用
    public static void save(java.nio.file.Path target, String content) throws IOException {
        java.nio.file.Files.createDirectories(target.toAbsolutePath().getParent());
        java.nio.file.Files.writeString(target, content);
    }
}
