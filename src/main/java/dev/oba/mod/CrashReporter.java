// 把诊断报告写进游戏的 logs 目录。崩了或者退出时自动落盘，
// 玩家不用做任何操作，事后直接去 logs 里翻。
package dev.oba.mod;

import dev.oba.core.Diagnosis;
import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.core.Reports;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CrashReporter {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    // 文件名统一在 Oba 里定，别处也要用（清理旧报告、测试断言）
    private static final String PREFIX = Oba.REPORT_PREFIX;
    // 只留最近这么多份，免得日志目录被我们塞满
    private static final int KEEP = 10;

    private final Diagnostics diagnostics;
    private volatile String lastSignature = "";
    // 启动时 crash-reports 里已经有哪些文件。之后新出现的才是这次的
    private final java.util.Set<String> knownCrashReports = new java.util.HashSet<>();
    private boolean crashReportsScanned;

    public CrashReporter(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    // 报告的存放目录，就是游戏的 logs 目录。
    Path reportDir() {
        return diagnostics.resolveGameDir().resolve("logs");
    }

    // 记下启动时已有的崩溃报告，之后的才算这次运行产生的
    synchronized void rememberExistingCrashReports() {
        if (crashReportsScanned) {
            return;
        }
        crashReportsScanned = true;
        for (Path p : listCrashReports()) {
            knownCrashReports.add(p.getFileName().toString());
        }
    }

    // 找出这次运行期间新产生的崩溃报告。
    // F3+C 那种手动调试崩溃根本不走日志：异常不经过 log4j2，
    // 聊天栏和 latest.log 里一个字都没有，只有 crash-reports 下落了一个文件。
    // 所以退出时得自己去看一眼磁盘，不然这类崩溃只能被完全漏掉。
    synchronized List<Path> newCrashReports() {
        List<Path> out = new ArrayList<>();
        for (Path p : listCrashReports()) {
            if (!knownCrashReports.contains(p.getFileName().toString())) {
                out.add(p);
            }
        }
        return out;
    }

    private List<Path> listCrashReports() {
        Path dir = diagnostics.resolveGameDir().resolve("crash-reports");
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "crash-*.txt")) {
            for (Path p : ds) {
                out.add(p);
            }
        } catch (IOException ignored) {
            // 读不了就当没有
        }
        out.sort(Comparator.comparing(Path::toString));
        return out;
    }

    // 把一份报告写进 logs 目录。reason 会记在报告开头，说明为什么写这一份；
    // 返回写出的路径，写失败返回 null
    public Path write(Diagnosis.Report report, String reason) {
        if (report == null) {
            return null;
        }
        try {
            Path dir = reportDir();
            Files.createDirectories(dir);
            LocalDateTime now = LocalDateTime.now();
            Path file = dir.resolve(PREFIX + STAMP.format(now) + ".txt");

            StringBuilder head = new StringBuilder();
            head.append("Oba v").append(Oba.VERSION).append('\n');
            head.append(reason).append('\n');
            head.append("报告内容为只读诊断结果，未对任何文件做出改动。\n");
            head.append("=".repeat(72)).append("\n\n");
            Files.writeString(file, head + Reports.text(report, false));

            // 再留一份固定名字的，方便脚本或用户直接找最近的
            Files.writeString(dir.resolve(Oba.LATEST_REPORT),
                    head + Reports.text(report, false));
            pruneOldReports(dir);
            return file;
        } catch (IOException e) {
            diagnostics.onLogLine(Oba.LOG_PREFIX
                    + Lang.get("report.writeFailed", e.getMessage()), true, false);
            return null;
        }
    }

    // 出错时调用。同一份结论只写一次，避免一个错误刷出一堆几乎一样的报告；
    // 重复或者没内容可写时返回 null
    public Path writeIfNew(Diagnosis.Report report, String reason) {
        String signature = signatureOf(report);
        if (signature.isEmpty() || signature.equals(lastSignature)) {
            return null;
        }
        lastSignature = signature;
        return write(report, reason);
    }

    // 退出时兜底：只要这次会话抓到过东西，就落一份盘。
    public Path writeOnShutdown(Diagnosis.Report report) {
        if (report == null) {
            return null;
        }
        return write(report, Lang.get("report.reason.shutdown"));
    }

    // 用"元凶 + 异常"当指纹，同一类错误只写一次
    private static String signatureOf(Diagnosis.Report report) {
        Diagnosis.Finding primary = report.primarySuspect();
        if (primary == null) {
            return report.issues().isEmpty() ? "" : "issues:" + report.issues().size();
        }
        return primary.fileName() + "|"
                + (primary.throwables().isEmpty() ? "?" : primary.throwables().get(0));
    }

    // 清理旧报告，按修改时间从新到旧保留 KEEP 份
    private void pruneOldReports(Path dir) {
        List<Path> mine = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, PREFIX + "*.txt")) {
            for (Path p : ds) {
                mine.add(p);
            }
        } catch (IOException ignored) {
            return;
        }
        if (mine.size() <= KEEP) {
            return;
        }
        mine.sort(Comparator.comparingLong(CrashReporter::modified).reversed());
        for (int i = KEEP; i < mine.size(); i++) {
            try {
                Files.deleteIfExists(mine.get(i));
            } catch (IOException ignored) {
                // 删不掉就算了，留着也不影响
            }
        }
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
