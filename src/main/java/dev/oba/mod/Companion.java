// 附属进程的启动器，外加一份交接文件。
//
// 设计上它跟游戏是两个进程：游戏正常退出、崩掉、甚至被任务管理器杀掉，
// 附属进程都能自己发现"游戏没了"，然后决定要不要弹窗。
// 这一侧只做三件事，而且每件都包在 try 里 —— 附属进程是加分项，
// 它出任何问题都不该影响游戏，更不该拦住启动：
//
//   1. 在客户端启动时另起一个 JVM，参数里带 PID、游戏目录、启动时刻；
//   2. 退出钩子里写一份 JSON（结论、元凶、问题清单、报告全文路径）；
//   3. 什么都不做的时候保持安静：跑不起来就是普通的一次启动。
//
// 第二个 JVM 用的是**这个 jar 自己**：界面代码（dev.oba.companion.*）就打在同一个
// jar 里，`java -cp <本 jar> dev.oba.companion.CompanionMain` 就能把它拉起来。
// 所以分发物只有一个 jar，没有 exe、没有解压出来的临时文件。
//
// 开关（JVM 参数或环境变量，默认 auto）：
//   -Doba.companion=off|auto|always   /  OBA_COMPANION=off|auto|always
//   auto   只在真崩了之后弹窗（有新的 crash-reports 文件，或者交接文件里标了 crash）
//   always 每次退出都弹，用来演示界面
//   off    完全不启动
package dev.oba.mod;

import dev.oba.core.Diagnosis;
import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.core.Timeline;
import dev.oba.loader.Loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class Companion {

    // 交接文件和附属进程自己的日志都放这个子目录，不往 logs 根目录丢东西
    public static final String DIR_NAME = "oba-companion";
    // 第二个 JVM 的入口。跟本类在同一个 jar 里
    public static final String MAIN_CLASS = "dev.oba.companion.CompanionMain";

    // 这次会话开始的时刻。附属进程拿它当分界线：比它新的崩溃报告才算这一场的
    private static final long SESSION_START = System.currentTimeMillis();

    private static volatile Process process;
    private static volatile Consumer<String> output = line -> {
    };
    // 退出钩子和 GameShuttingDownEvent 都会走到这里，写一次就够
    private static volatile boolean handoffWritten;

    private Companion() {
    }

    // 生命周期把日志出口塞进来，跟 Diagnostics 一样的做法
    public static void setOutput(Consumer<String> sink) {
        output = sink == null ? line -> {
        } : sink;
    }

    // 这次要不要启附属进程
    public static String mode() {
        String raw = System.getProperty("oba.companion");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("OBA_COMPANION");
        }
        if (raw == null || raw.isBlank()) {
            return "auto";
        }
        String v = raw.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (v) {
            case "off", "false", "0", "no" -> "off";
            case "always", "all", "on" -> "always";
            default -> "auto";
        };
    }

    // 是不是客户端。服务端没有窗口可以弹，装不装都不启动。
    // 这里不能去 Class.forName("net.minecraft.client.Minecraft")：专用服务端上那一下会被
    // RuntimeDistCleaner 拦下来并写一行 ERROR（Attempted to load class ... for invalid dist），
    // 然后 OBA 又会把这行自己的错误诊断成「服务端装了仅客户端模组」。
    // 改问加载器的 dist：Forge 在 net.minecraftforge.fml.loading.FMLEnvironment，
    // NeoForge 在 net.neoforged.fml.loading.FMLEnvironment，都只在加载器那一层。
    public static boolean clientSide() {
        String dist = distName();
        if (dist != null) {
            return "CLIENT".equalsIgnoreCase(dist);
        }
        // 问不到 dist 再看资源在不在（不加载类，也就不会惊动 dist cleaner）
        try {
            return Companion.class.getClassLoader()
                    .getResource("net/minecraft/client/Minecraft.class") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private static String distName() {
        for (String name : new String[]{"net.neoforged.fml.loading.FMLEnvironment",
                "net.minecraftforge.fml.loading.FMLEnvironment"}) {
            try {
                Class<?> env = Class.forName(name, false, Companion.class.getClassLoader());
                Object dist = env.getField("dist").get(null);
                if (dist != null) {
                    return String.valueOf(dist);
                }
            } catch (Throwable ignored) {
                // 换下一个包名
            }
        }
        return null;
    }

    // 启动附属进程。已经起过就直接返回
    public static synchronized void start() {
        if (process != null && process.isAlive()) {
            return;
        }
        if ("off".equals(mode()) || !clientSide()) {
            return;
        }
        try {
            Path gameDir = Loader.env().gameDir();
            if (gameDir == null) {
                return;
            }
            Path jar = selfJar(gameDir);
            if (jar == null) {
                // 只在明确要求 always 的时候吭一声，默认路径下找不到就当没这回事
                if ("always".equals(mode())) {
                    output.accept(Oba.LOG_PREFIX + Lang.get("companion.missing", gameDir));
                }
                return;
            }
            Path java = javaExecutable();
            if (java == null) {
                return;
            }
            Path dir = handoffDir(gameDir);
            Files.createDirectories(dir);
            List<String> cmd = new ArrayList<>();
            cmd.add(java.toString());
            // 第二个 JVM 只画一个窗口，给 192 MB 够了；不设的话会跟着游戏的大堆走
            cmd.add("-Xmx192m");
            cmd.add("-cp");
            cmd.add(jar.toAbsolutePath().toString());
            cmd.add(MAIN_CLASS);
            cmd.add("--game-pid");
            cmd.add(String.valueOf(ProcessHandle.current().pid()));
            cmd.add("--game-dir");
            cmd.add(gameDir.toAbsolutePath().toString());
            cmd.add("--started-at");
            cmd.add(String.valueOf(SESSION_START));
            cmd.add("--handoff-dir");
            cmd.add(dir.toAbsolutePath().toString());
            cmd.add("--mode");
            cmd.add(mode());
            cmd.add("--mod-version");
            cmd.add(Oba.VERSION);
            cmd.add("--loader");
            cmd.add(Loader.env().loaderName());
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(gameDir.toFile());
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(
                    dir.resolve("companion.log").toFile()));
            process = pb.start();
            output.accept(Oba.LOG_PREFIX + Lang.get("companion.started", process.pid(), jar.getFileName()));
        } catch (Throwable t) {
            // 起不来就算了。附属进程不该让一次正常启动多出一条崩溃
            output.accept(Oba.LOG_PREFIX + Lang.get("companion.failed", String.valueOf(t)));
        }
    }

    // 退出前写交接文件。调用方是 JVM 的 shutdown hook，所以这里只碰文件系统
    public static void writeHandoff(Diagnostics diagnostics) {
        if ("off".equals(mode()) || handoffWritten) {
            return;
        }
        handoffWritten = true;
        try {
            Path gameDir = diagnostics.resolveGameDir();
            if (gameDir == null) {
                return;
            }
            Path dir = handoffDir(gameDir);
            Files.createDirectories(dir);
            String json = toJson(diagnostics, gameDir);
            String stamp = SESSION_START + "-" + ProcessHandle.current().pid();
            Path target = dir.resolve("handoff-" + stamp + ".json");
            Path tmp = dir.resolve("handoff-" + stamp + ".json.tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            // 再留一份固定名字的，方便手动看
            Path latestTmp = dir.resolve("latest.json.tmp");
            Files.writeString(latestTmp, json, StandardCharsets.UTF_8);
            Files.move(latestTmp, dir.resolve("latest.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable ignored) {
            // 退出阶段写不进去就算了
        }
    }

    // 交接文件最后写到哪，状态命令里显示用
    public static Path handoffDir(Path gameDir) {
        return gameDir.resolve("logs").resolve(DIR_NAME);
    }

    // 找本模组自己的 jar。先问类的来源，拿不到再在 mods 目录里按内容认
    public static Path selfJar(Path gameDir) {
        try {
            java.security.CodeSource cs = Companion.class.getProtectionDomain().getCodeSource();
            if (cs != null && cs.getLocation() != null) {
                Path p = toJarPath(cs.getLocation().toString());
                if (p != null && Files.isRegularFile(p)) {
                    return p;
                }
            }
        } catch (Throwable ignored) {
            // 来源拿不到不稀奇：有的加载器给的是虚拟路径
        }
        // NeoForge 的联合文件系统给出的路径往往不是普通文件，退回 mods 目录按内容找：
        // 哪个 jar 里有 dev/oba/companion/CompanionMain.class，哪个就是它
        if (gameDir == null) {
            return null;
        }
        Path mods = gameDir.resolve("mods");
        if (!Files.isDirectory(mods)) {
            return null;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(mods, "*.jar")) {
            for (Path p : ds) {
                if (containsCompanion(p)) {
                    return p.toAbsolutePath();
                }
            }
        } catch (IOException ignored) {
            // 目录读不了就当找不到
        }
        return null;
    }

    // 把 code source 的 URL 变成磁盘上的 jar 路径。
    // Forge 一般是 file:/…/oba.jar；NeoForge 的联合文件系统是 jar:file:/…!/ 或 union:/…
    private static Path toJarPath(String location) {
        String s = location == null ? "" : location;
        if (s.startsWith("jar:")) {
            s = s.substring(4);
        }
        int bang = s.indexOf("!/");
        if (bang > 0) {
            s = s.substring(0, bang);
        }
        if (s.startsWith("union:")) {
            s = s.substring(6);
            int cut = s.indexOf(".jar");
            if (cut > 0) {
                s = s.substring(0, cut + 4);
            }
        }
        if (s.startsWith("file:")) {
            try {
                return Path.of(java.net.URI.create(s));
            } catch (Throwable t) {
                return null;
            }
        }
        if (s.endsWith(".jar")) {
            try {
                return Path.of(s);
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    private static boolean containsCompanion(Path jar) {
        String entry = MAIN_CLASS.replace('.', '/') + ".class";
        if (!dev.oba.core.ModFiles.isPlainFile(jar)) {
            // 套在别的 zip 里的模组：toFile() 会抛异常，改成流式找
            return dev.oba.core.ModFiles.contains(jar, entry);
        }
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
            return zip.getEntry(entry) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // 第二个 JVM 用哪个 java 可执行文件。Windows 上优先 javaw，免得闪一个黑框
    private static Path javaExecutable() {
        String home = System.getProperty("java.home");
        if (home == null || home.isBlank()) {
            return null;
        }
        Path bin = Path.of(home, "bin");
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(java.util.Locale.ROOT).contains("win");
        List<Path> candidates = new ArrayList<>();
        if (windows) {
            candidates.add(bin.resolve("javaw.exe"));
            candidates.add(bin.resolve("java.exe"));
        }
        candidates.add(bin.resolve("java"));
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    // 这一场新落下来的报告文件（退出时写的那一份）
    private static Path newestReport(Path gameDir) {
        Path logs = gameDir.resolve("logs");
        if (!Files.isDirectory(logs)) {
            return null;
        }
        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(logs, "oba-report-*.txt")) {
            for (Path p : ds) {
                found.add(p);
            }
        } catch (IOException ignored) {
            return null;
        }
        found.sort(Comparator.comparing(Path::toString));
        for (int i = found.size() - 1; i >= 0; i--) {
            try {
                if (Files.getLastModifiedTime(found.get(i)).toMillis() >= SESSION_START) {
                    return found.get(i);
                }
            } catch (IOException ignored) {
                // 看不了时间就当不是这一场的
            }
        }
        return null;
    }

    // 手写 JSON：这条路径上没有可以依赖的序列化库，字段又少，直接拼更省事
    private static String toJson(Diagnostics d, Path gameDir) {
        List<Path> crashes = d.newCrashReports();
        StringBuilder b = new StringBuilder(4096);
        b.append('{');
        field(b, "schema", 1).append(',');
        field(b, "writtenAt", System.currentTimeMillis()).append(',');
        field(b, "startedAt", SESSION_START).append(',');
        field(b, "modVersion", Oba.VERSION).append(',');
        field(b, "loader", Loader.env().loaderName()).append(',');
        field(b, "language", Lang.language()).append(',');
        field(b, "gameDir", gameDir.toAbsolutePath().toString()).append(',');
        field(b, "gamePid", ProcessHandle.current().pid()).append(',');
        field(b, "reason", crashes.isEmpty() ? "exit" : "crash").append(',');
        // 这一场捕获到多少行错误：窗口弹不弹，客户端就靠这个数判断
        int captured = d.capturedErrors() + d.capturedFatals();
        field(b, "hadErrors", captured > 0).append(',');
        field(b, "capturedErrors", captured).append(',');

        b.append('"').append("crashReports").append('"').append(':').append('[');
        for (int i = 0; i < crashes.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(quote(crashes.get(i).toAbsolutePath().toString()));
        }
        b.append(']').append(',');

        Path reportFile = newestReport(gameDir);
        field(b, "reportPath", reportFile == null ? "" : reportFile.toAbsolutePath().toString());
        b.append(',');

        Diagnosis.Report report = d.lastReport().orElse(null);
        String reportText = "";
        if (report != null) {
            try {
                reportText = dev.oba.core.Reports.text(report, false);
            } catch (Throwable ignored) {
                reportText = "";
            }
        }
        field(b, "reportText", reportText).append(',');

        String headline = report == null ? "" : report.rootCauseHeadline();
        field(b, "headline", headline == null ? "" : headline).append(',');

        List<String> verdicts = d.recentVerdicts();
        b.append('"').append("verdicts").append('"').append(':').append('[');
        for (int i = 0; i < verdicts.size(); i++) {
            if (i > 0) {
                b.append(',');
            }
            b.append(quote(verdicts.get(i)));
        }
        b.append(']').append(',');

        b.append('"').append("culprit").append('"').append(':');
        Diagnosis.Finding primary = report == null ? null : report.primarySuspect();
        if (primary == null) {
            b.append("null");
        } else {
            b.append('{');
            field(b, "name", primary.display()).append(',');
            field(b, "file", primary.fileName()).append(',');
            String modIds = primary.source() == null ? "" : String.join(", ", primary.source().modIds());
            field(b, "modIds", modIds).append(',');
            field(b, "version", primary.source() == null ? "" : primary.source().version()).append(',');
            field(b, "score", primary.score()).append(',');
            field(b, "confidence", report.confidenceText(primary.score())).append(',');
            String location = primary.firstLocatedFrame() == null ? "" : primary.firstLocatedFrame().location();
            field(b, "location", location);
            b.append('}');
        }
        b.append(',');

        b.append('"').append("issues").append('"').append(':').append('[');
        if (report != null) {
            List<Diagnosis.KnownIssue> issues = report.issues();
            for (int i = 0; i < issues.size(); i++) {
                Diagnosis.KnownIssue issue = issues.get(i);
                if (i > 0) {
                    b.append(',');
                }
                b.append('{');
                field(b, "severity", issue.severity().id()).append(',');
                field(b, "rule", issue.rule()).append(',');
                field(b, "title", safe(() -> issue.title()));
                b.append(',');
                field(b, "detail", safe(() -> issue.detail())).append(',');
                field(b, "advice", safe(() -> issue.advice())).append(',');
                field(b, "source", issue.source() == null ? "" : issue.source().fileName());
                b.append('}');
            }
        }
        b.append(']').append(',');

        b.append('"').append("timeline").append('"').append(':').append('[');
        if (report != null && report.timeline() != null) {
            List<Timeline.Entry> entries = report.timeline().entries();
            for (int i = 0; i < entries.size(); i++) {
                Timeline.Entry entry = entries.get(i);
                if (i > 0) {
                    b.append(',');
                }
                b.append('{');
                field(b, "time", entry.time() == null ? "" : entry.time()).append(',');
                field(b, "modName", entry.modName() == null ? "" : entry.modName()).append(',');
                b.append(quote("type")).append(':').append(quote(entry.type() == null
                        ? "" : entry.type().name())).append(',');
                field(b, "text", safe(() -> entry.renderText())).append(',');
                field(b, "detail", entry.detail() == null ? "" : entry.detail());
                b.append('}');
            }
        }
        b.append(']').append(',');

        b.append('"').append("environment").append('"').append(':').append('{');
        int envCount = 0;
        envCount = baseEnv(b, envCount, "Java", safe(() -> System.getProperty("java.version")));
        envCount = baseEnv(b, envCount, "系统", safe(() -> System.getProperty("os.name"))
                + " " + safe(() -> System.getProperty("os.arch")));
        envCount = baseEnv(b, envCount, "加载器", safe(() -> Loader.env().loaderName()));
        envCount = baseEnv(b, envCount, "模组数", safe(() -> String.valueOf(d.sources().size())));
        if (report != null) {
            for (Map.Entry<String, String> e : report.environment().entrySet()) {
                if (envCount++ > 0) {
                    b.append(',');
                }
                b.append(quote(e.getKey())).append(':').append(quote(e.getValue()));
            }
        }
        b.append('}');

        b.append('}');
        return b.toString();
    }

    /** 环境小表里的固定几项：没有报告的时候也让窗口有东西可显示。 */
    private static int baseEnv(StringBuilder b, int count, String key, String value) {
        if (count > 0) {
            b.append(',');
        }
        b.append(quote(key)).append(':').append(quote(value));
        return count + 1;
    }

    private static String safe(java.util.function.Supplier<String> supplier) {        try {
            String s = supplier.get();
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    private static StringBuilder field(StringBuilder b, String key, Object value) {
        b.append(quote(key)).append(':');
        if (value instanceof Number n) {
            b.append(n.longValue());
        } else {
            b.append(quote(String.valueOf(value)));
        }
        return b;
    }

    // JSON 字符串转义。控制字符一律写成四位十六进制的转义形式，
    // 免得附属进程那边读进非法 JSON
    private static String quote(String s) {
        StringBuilder b = new StringBuilder(s.length() + 16);
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        // 反斜杠和 u 分开拼：Java 在分词之前就把源码里的 Unicode 转义
                        // 扫一遍，连注释都算，写成一整个转义序列会直接编译不过
                        b.append(String.format("\\" + "u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        b.append('"');
        return b.toString();
    }
}
