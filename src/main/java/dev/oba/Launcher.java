// 唯一的入口。判断当前是"被游戏加载"还是"命令行直接跑"，然后分派过去。
// 这样同一个 jar 既能丢进 mods/ 当模组，也能 java -jar 当诊断工具。
package dev.oba;

import dev.oba.core.Oba;

import java.util.Locale;

public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        if (isInsideGame()) {
            // 走到这里说明有人在游戏里直接调了 main，正常加载不会经过这里。
            // 提示一下正确的用法，别让人以为模组没生效。
            System.out.println(Oba.LOG_PREFIX
                    + "检测到游戏环境，本 jar 作为模组时由 NeoForge 自动加载，不需要手动运行。");
            return;
        }
        dev.oba.cli.Cli.run(args);
    }

    // 判断是不是在游戏进程里。
    // 主要看 FML 设的启动目标属性：专用服务器和开发环境都会设，单纯的 java -jar 没有。
    // 再用类路径兜一层，防止启动器改了属性名。
    // 想强制走命令行加 -Doba.mode=cli
    static boolean isInsideGame() {
        String forced = System.getProperty("oba.mode");
        if (forced != null && !forced.isBlank()) {
            return !"cli".equals(forced.trim().toLowerCase(Locale.ROOT));
        }
        if (System.getProperty("fml.launchTarget") != null
                || System.getProperty("neoforge.dist") != null) {
            return true;
        }
        return isGameOnClasspath();
    }

    // 类路径上能看见 FML 的启动类，基本就是在游戏里
    private static boolean isGameOnClasspath() {
        try {
            Class.forName("net.neoforged.fml.loading.FMLLoader", false,
                    Launcher.class.getClassLoader());
            return true;
        } catch (Throwable notThere) {
            return false;
        }
    }
}
