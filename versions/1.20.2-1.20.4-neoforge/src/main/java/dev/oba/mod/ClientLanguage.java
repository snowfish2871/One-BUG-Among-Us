// 把玩家在游戏里选的语言告诉引擎，让报告语言跟着游戏走。
// 只在客户端生效：专用服务器没有语言选项，那边由 JVM 区域或 -Doba.lang 决定。
//
// 这里没用 @EventBusSubscriber：那个注解是 NeoForge 20.4 才有的，
// 20.2 上编译不过。改成由 ObaMod 在确认是客户端之后手动注册，
// 既跨版本可用，也顺带避免了服务端加载客户端专属的类。
package dev.oba.mod;

import dev.oba.core.Lang;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.minecraft.client.Minecraft;

public final class ClientLanguage {

    // 每次进世界或连服务器时同步一次，玩家中途改语言也能跟上
    @SubscribeEvent
    public void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        applyFromOptions();
        // 开发机上给界面拍照用，没设参数就不会碰这个类
        if (System.getProperty("oba.ui.shot") != null) {
            UiShot.arm();
        }
    }

    // 读当前语言设置并推给引擎
    public static void applyFromOptions() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.getLanguageManager() == null) {
                return;
            }
            String code = minecraft.getLanguageManager().getSelected();
            if (code != null && !code.isBlank()) {
                Lang.setGameLanguage(code);
            }
        } catch (Throwable ignored) {
            // 客户端刚起来或没有界面时拿不到设置，这时用 JVM 区域兜底就行
        }
    }
}
