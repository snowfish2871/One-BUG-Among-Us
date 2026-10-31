// 给聊天栏界面拍照用的辅助，只在开发机上开。
//
// 为什么需要它：这台机器上外来的按键进不了游戏窗口 —— 往消息队列塞 WM_KEYDOWN、
// keybd_event 造真按键、AttachThreadInput 抢键盘焦点三条路都试过，游戏那边一概没反应
// （tools/ui-shot.ps1 末尾记了详细结论）。界面截图又是必须肉眼确认的东西，
// 所以让游戏自己进世界之后替他跑一条命令。
//
// 只有 JVM 参数 -Doba.ui.shot=<命令> 才会挂上（值就是命令，留空则跑 oba help），
// 正常玩永远不会触发。客户端专属：专用服务器上没有 Minecraft 这个类，
// 装载路径由调用方把住（只有确认是客户端、且设了那个参数时才会碰这个类）。
package dev.oba.mod;

import dev.oba.core.Oba;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;

public final class UiShot {

    // 进世界之后再等一会儿：刚进去那几秒 HUD 还在铺，聊天栏的字会一闪而过
    private static final long SETTLE_MS = 2000L;
    private static final long STEP_MS = 500L;
    private static final long WAIT_LIMIT_MS = 15L * 60L * 1000L;

    public static void arm() {
        String configured = System.getProperty("oba.ui.shot");
        if (configured == null) {
            return;
        }
        // 模组构造的时候世界还没加载，得等玩家真进去
        String command = configured.isEmpty() ? "oba help" : configured;
        Thread worker = new Thread(() -> run(command), "oba-ui-shot");
        worker.setDaemon(true);
        worker.start();
    }

    private static void run(String command) {
        long deadline = System.currentTimeMillis() + WAIT_LIMIT_MS;
        while (System.currentTimeMillis() < deadline && !inWorld()) {
            sleep(STEP_MS);
        }
        if (!inWorld()) {
            LogManager.getLogger(Oba.MOD_ID).warn("{}" + "界面截图辅助没等到进世界，这次放弃", Oba.LOG_PREFIX);
            return;
        }
        sleep(SETTLE_MS);

        auditWidths();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        // 发包得在客户端线程上做
        minecraft.execute(() -> {
            try {
                if (minecraft.player != null && minecraft.player.connection != null) {
                    minecraft.player.connection.sendCommand(command);
                    LogManager.getLogger(Oba.MOD_ID).warn(
                            "{}" + "界面截图辅助执行了 /" + command, Oba.LOG_PREFIX);
                }
            } catch (Throwable ignored) {
                // 截图辅助，失败就算了
            }
        });

        // 顺便让游戏自己截一张：从窗口外面抓屏抓到的是过期的画面
        // （窗口被盖住时 DWM 不再合成新帧），只有游戏自己的帧缓冲是准的
        for (int i = 0; i < 2; i++) {
            sleep(1500L);
            Minecraft game = Minecraft.getInstance();
            if (game == null) {
                return;
            }
            final int round = i + 1;
            game.execute(() -> {
                try {
                    grab(game);
                    LogManager.getLogger(Oba.MOD_ID).warn(
                            "{}" + "界面截图辅助自己截了一张（第 " + round + " 张）", Oba.LOG_PREFIX);
                } catch (Throwable t) {
                    LogManager.getLogger(Oba.MOD_ID).warn(
                            "{}" + "界面截图辅助自己截图失败: " + t, Oba.LOG_PREFIX);
                }
            });
        }
    }

    // 进游戏之后，宽度表就有真字体可以对了：表里的值跟客户端量出来的差多少、
    // 标题框那三行是不是真的等像素宽 —— 这两件事只有实机才验得了，
    // 界面上又常常看不到顶部（聊天栏默认只露最后十几行），所以直接把数字写进日志。
    private static void auditWidths() {
        try {
            StringBuilder mismatches = new StringBuilder();
            int total = 0;
            for (char c = 0x20; c <= 0x7e; c++) {
                total++;
                int table = GameUi.pxOf(c);
                int real = GameUi.realWidth(String.valueOf(c));
                if (table != real) {
                    mismatches.append(" [").append(c).append(' ').append(table).append("->").append(real).append(']');
                }
            }
            for (char c : GameUi.EXTRA.toCharArray()) {
                total++;
                int table = GameUi.pxOf(c);
                int real = GameUi.realWidth(String.valueOf(c));
                if (table != real) {
                    mismatches.append(" [").append(c).append(' ').append(table).append("->").append(real).append(']');
                }
            }
            StringBuilder header = new StringBuilder();
            boolean equal = true;
            Integer first = null;
            for (net.minecraft.network.chat.Component line : GameUi.header("One BUG Among Us")) {
                int width = GameUi.px(line.getString());
                if (first == null) {
                    first = width;
                } else if (first != width) {
                    equal = false;
                }
                header.append(header.length() == 0 ? "" : " / ").append(width);
            }
            LogManager.getLogger(Oba.MOD_ID).warn("{}" + "宽度核对：表 vs 真字体，共 " + total
                    + " 个字符，不一致 " + (mismatches.length() == 0 ? "0 项" : mismatches.toString()),
                    Oba.LOG_PREFIX);
            LogManager.getLogger(Oba.MOD_ID).warn("{}" + "标题框三行像素宽：" + header
                    + (equal ? "（等宽 ✓）" : "（不等宽 ✗）"), Oba.LOG_PREFIX);
        } catch (Throwable t) {
            LogManager.getLogger(Oba.MOD_ID).warn("{}" + "宽度核对没跑成: " + t, Oba.LOG_PREFIX);
        }
    }

    private static boolean inWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.player != null && minecraft.player.connection != null;
    }

    // 截图接口在各版本之间改过：1.21.1 及以前把 Minecraft.getMainRenderTarget() 交给
    // Screenshot.grab(File, RenderTarget, Consumer)；26.x 的渲染层重写之后主渲染目标
    // 不再挂在 Minecraft 上，只剩 F2 那条路 Screenshot.grab(Minecraft, boolean)。
    // 两条都试一次，能成一条就行 —— 这个类只在开发机上开，失败也只是少一张图。
    private static void grab(Minecraft game) throws ReflectiveOperationException {
        try {
            java.lang.reflect.Method key = net.minecraft.client.Screenshot.class
                    .getMethod("grab", Minecraft.class, boolean.class);
            key.invoke(null, game, true);
            return;
        } catch (NoSuchMethodException notOnThisVersion) {
            // 老版本没有这条路，往下走
        }
        Object renderTarget = Minecraft.class.getMethod("getMainRenderTarget").invoke(game);
        net.minecraft.client.Screenshot.grab(game.gameDirectory,
                (com.mojang.blaze3d.pipeline.RenderTarget) renderTarget, message -> { });
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private UiShot() {
    }
}
