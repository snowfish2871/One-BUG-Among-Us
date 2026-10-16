// 命令行里让菜单项可点：为每一项写一个一次性启动脚本，再把脚本路径包成 OSC 8 超链接。
// 终端支持超链接就能点一下跑起来，不支持的话控制码会被忽略，只显示文字，不影响界面。
package dev.oba.cli;

import dev.oba.core.Lang;
import dev.oba.core.Oba;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CliLauncher {

    // 一个菜单项对应的启动脚本
    private record Entry(String id, String href, Path script) {
    }

    private final List<Entry> entries = new ArrayList<>();
    private final Path dir;
    private final Path self;
    private final Path gameDir;

    private CliLauncher(Path dir, Path self, Path gameDir) {
        this.dir = dir;
        this.self = self;
        this.gameDir = gameDir;
    }

    // 拿得到当前 jar 才做得成脚本；拿不到（比如在 IDE 里跑）就返回 null
    public static CliLauncher create(Path self, Path gameDir) {
        if (self == null || !Files.isRegularFile(self)) {
            return null;
        }
        try {
            Path dir = Files.createTempDirectory("oba-menu-");
            CliLauncher l = new CliLauncher(dir, self, gameDir);
            Runtime.getRuntime().addShutdownHook(new Thread(l::cleanup));
            return l;
        } catch (IOException ignored) {
            // 临时目录都建不出来就直接放弃可点，不影响菜单本身
            return null;
        }
    }

    // 给一个菜单项写脚本。写不成返回 null，那一项就退化成普通文字
    public String href(String id, String subcommand) {
        for (Entry e : entries) {
            if (e.id().equals(id)) {
                return e.href();
            }
        }
        Path script = dir.resolve("oba-" + id.replaceAll("[^A-Za-z0-9_.-]", "_") + ".cmd");
        try {
            Files.writeString(script, body(subcommand), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
        String href = script.toUri().toString();
        entries.add(new Entry(id, href, script));
        return href;
    }

    // 脚本内容。路径统一用引号括起来，带空格或中文目录也不会散架
    private String body(String subcommand) {
        StringBuilder sb = new StringBuilder();
        sb.append("@echo off\r\n");
        sb.append("title ").append(Oba.NAME).append(" - ").append(subcommand).append("\r\n");
        sb.append("chcp 65001 > nul\r\n");
        sb.append("echo ").append(Lang.get("cli.launcher.head", Oba.NAME, Oba.VERSION)).append("\r\n");
        sb.append("echo.\r\n");
        sb.append('"').append(javaExe()).append('"');
        sb.append(" -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8");
        sb.append(" -jar \"").append(self).append('"');
        if (gameDir != null) {
            sb.append(" --game-dir \"").append(gameDir).append('"');
        }
        sb.append(' ').append(subcommand).append("\r\n");
        sb.append("echo.\r\n");
        sb.append("echo ").append(Lang.get("cli.launcher.done")).append("\r\n");
        sb.append("pause\r\n");
        return sb.toString();
    }

    // 用当前这个 JVM 的 java，省得用户机器上没配 PATH
    private static String javaExe() {
        String home = System.getProperty("java.home", "");
        Path exe = Path.of(home, "bin", "javaw.exe");
        return Files.isRegularFile(exe) ? exe.toString() : Path.of(home, "bin", "java.exe").toString();
    }

    public Path directory() {
        return dir;
    }

    // 收摊：脚本本身没用了，顺手删掉临时目录
    private void cleanup() {
        for (Entry e : entries) {
            try {
                Files.deleteIfExists(e.script());
            } catch (IOException ignored) {
                // 删不掉就留着，反正在临时目录里
            }
        }
        try {
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
            // 目录非空或占用，交给系统清理
        }
    }
}
