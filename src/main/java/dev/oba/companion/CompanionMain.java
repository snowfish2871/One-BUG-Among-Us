// 附属进程的入口。它在**另一个 JVM** 里跑：游戏那边用 ProcessBuilder 把它拉起来，
// 传进来的只有游戏 PID、游戏目录、这一场开始的时刻和交接目录。
//
// 这个进程不认识 Minecraft，也不加载任何模组。它只做三件事：
// 等游戏进程消失、读交接文件、把窗口画出来。
package dev.oba.companion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CompanionMain {

    private CompanionMain() {
    }

    public static void main(String[] args) throws Exception {
        Args a = Args.parse(args);

        // 只在 Windows 上有意义；别的系统上照常跑（Swing 到处都有）
        if (a.demo) {
            show(Args.demoHandoff(), null, a);
            return;
        }
        if (!a.openFile.isEmpty()) {
            Path file = Path.of(a.openFile);
            Handoff h = Handoff.read(file);
            show(h, h == null ? readText(file) : null, a);
            return;
        }

        waitForGame(a.gamePid);

        Handoff handoff = a.handoffDir.isBlank() ? null
                : waitForHandoff(Path.of(a.handoffDir), a.startedAt);
        String crashReport = newestCrashReport(a.gameDir, a.startedAt);

        // 这一场算“出过事”的三种情况：有新的崩溃报告、交接文件说崩了、或者这一路捕获过错误行。
        // 最后一种最常见：模组一路刷 ERROR 但游戏是自己关掉的，以前这种情况不弹窗。
        boolean crashed = crashReport != null
                || (handoff != null && (handoff.isCrash() || handoff.hadErrors()));
        if (!crashed && !a.always) {
            return;
        }

        String fallback = crashReport == null ? null : readText(Path.of(crashReport));
        if (handoff == null && fallback == null && !a.always) {
            // 没有交接文件、也没有新的崩溃报告：按正常退出处理，不打扰
            return;
        }
        show(handoff, fallback, a);
    }

    private static void waitForGame(long pid) {
        if (pid <= 0) {
            return;
        }
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty()) {
            return;
        }
        try {
            handle.get().onExit().join();
        } catch (RuntimeException ignored) {
            // 拿不到退出事件就算了，后面按磁盘上的东西判断
        }
    }

    private static Handoff waitForHandoff(Path dir, long startedAt) {
        if (dir == null || !Files.isDirectory(dir)) {
            return null;
        }
        // 退出钩子写完文件到进程消失之间有几十毫秒的缝，这里给最多六秒
        for (int attempt = 0; attempt < 40; attempt++) {
            Handoff h = newestHandoff(dir, startedAt);
            if (h != null) {
                return h;
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private static Handoff newestHandoff(Path dir, long startedAt) {
        Path best = null;
        long bestTime = Long.MIN_VALUE;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "handoff-*.json")) {
            for (Path p : ds) {
                long time = Files.getLastModifiedTime(p).toMillis();
                if (startedAt > 0 && time < startedAt) {
                    continue;
                }
                if (time > bestTime) {
                    bestTime = time;
                    best = p;
                }
            }
        } catch (IOException ignored) {
            return null;
        }
        return best == null ? null : Handoff.read(best);
    }

    private static String newestCrashReport(String gameDir, long startedAt) {
        if (gameDir == null || gameDir.isBlank()) {
            return null;
        }
        Path dir = Path.of(gameDir).resolve("crash-reports");
        if (!Files.isDirectory(dir)) {
            return null;
        }
        List<Path> found = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "crash-*.txt")) {
            for (Path p : ds) {
                if (startedAt > 0 && Files.getLastModifiedTime(p).toMillis() < startedAt) {
                    continue;
                }
                found.add(p);
            }
        } catch (IOException ignored) {
            return null;
        }
        found.sort(Comparator.comparingLong(p -> {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }));
        return found.isEmpty() ? null : found.get(found.size() - 1).toAbsolutePath().toString();
    }

    private static String readText(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return text.length() <= 20000 ? text : text.substring(0, 20000) + "\n…（后面还有，点「打开报告文件」看全文）";
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    private static void show(Handoff handoff, String fallbackText, Args args) {
        javax.swing.SwingUtilities.invokeLater(() -> new CompanionWindow(handoff, fallbackText, args).open());
    }

    // 命令行参数。Java 侧拉起本进程时按下面的顺序传，全部是绝对路径
    static final class Args {
        String handoffDir = "";
        String gameDir = "";
        long gamePid;
        long startedAt;
        String mode = "auto";
        String modVersion = "";
        String loader = "";
        String openFile = "";
        // 打开时先显示哪一页：report / timeline / environment / json
        String tab = "report";
        boolean demo;

        boolean always;

        static Args parse(String[] argv) {
            Args a = new Args();
            for (int i = 0; i < argv.length; i++) {
                String key = argv[i];
                String value = null;
                int eq = key.indexOf('=');
                if (eq > 0) {
                    value = key.substring(eq + 1);
                    key = key.substring(0, eq);
                }
                if ("--demo".equals(key)) {
                    a.demo = true;
                    continue;
                }
                if (value == null) {
                    value = i + 1 < argv.length ? argv[++i] : "";
                }
                switch (key) {
                    case "--handoff-dir" -> a.handoffDir = value;
                    case "--game-dir" -> a.gameDir = value;
                    case "--game-pid" -> a.gamePid = parseLong(value);
                    case "--started-at" -> a.startedAt = parseLong(value);
                    case "--mode" -> a.mode = value.toLowerCase(java.util.Locale.ROOT);
                    case "--mod-version" -> a.modVersion = value;
                    case "--loader" -> a.loader = value;
                    case "--open" -> a.openFile = value;
                    case "--tab" -> a.tab = value;
                    default -> {
                        // 不认识的参数直接忽略：这个进程只认自己那几个
                    }
                }
            }
            a.always = "always".equals(a.mode);
            return a;
        }

        private static long parseLong(String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        // 演示数据：不接游戏也能检查排版
        static Handoff demoHandoff() {
            Handoff h = Handoff.fromJson("""
                    {"schema":1,"writtenAt":0,"startedAt":0,"modVersion":"1.2.0",
                     "loader":"NeoForge 21.1.256","language":"zh","gameDir":"D:\\\\hmcl\\\\.minecraft",
                     "gamePid":0,"reason":"crash","capturedErrors":88,"crashReports":[],
                     "reportPath":"D:\\\\hmcl\\\\.minecraft\\\\logs\\\\oba-report-demo.txt",
                     "reportText":"（演示数据）这份正文用来检查等宽排版。\\n\\n实际运行时这里是 OBA 写出来的完整报告。",
                     "headline":"Waystones 读配置时类型转换失败，把 balm 的配置读取一起带崩了",
                     "verdicts":["定位到出错模组 balm，可信度 极高 (99%)"],
                     "culprit":{"name":"Balm (balm)","file":"balm-forge-1.20.6-10.3.6-all.jar",
                                "modIds":"balm","version":"10.3.6","score":0.99,
                                "confidence":"极高 (99%)",
                                "location":"net.blay09.mods.balm.forge.config.ForgeBalmConfig.lambda$readConfigValues$8 (ForgeBalmConfig.java:149)"},
                     "issues":[
                       {"severity":"fatal","rule":"world.ticking_entity","title":"存档里的实体坏了",
                        "detail":"Ticking entity java.lang.ClassCastException: class net.blay09.mods.waystones.api.WaystoneOrigin cannot be cast to class java.lang.String",
                        "advice":"演示条目，真报告里会给具体建议。","source":"balm-forge-1.20.6-10.3.6-all.jar"},
                       {"severity":"warn","rule":"server.client_class","title":"服务端装了仅客户端模组",
                        "detail":"Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER",
                        "advice":"把这个模组从服务端 mods 目录里拿掉，客户端保留。","source":""}],
                     "timeline":[
                       {"time":"20:09:58","modName":"balm","type":"INFO","text":"读取配置文件","detail":""},
                       {"time":"20:10:00","modName":"waystones","type":"ERROR","text":"WaystoneOrigin 不能转成 String","detail":""}],
                     "environment":{"Java":"21.0.5 (Zulu)","操作系统":"Windows 11 26100","内存上限":"4096 MB","模组数量":"18"}}
                    """);
            return h;
        }
    }
}
