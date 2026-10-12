// 把玩家在游戏里选的语言告诉引擎，让报告语言跟着游戏走。
// 只在客户端生效：专用服务器没有语言选项，那边由 JVM 区域或 -Doba.lang 决定。
package dev.oba.mod;

import dev.oba.core.Lang;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.minecraft.client.Minecraft;

@EventBusSubscriber(modid = dev.oba.core.Oba.MOD_ID, value = Dist.CLIENT)
public final class ClientLanguage {

    private ClientLanguage() {
    }

    // 每次进世界或连服务器时同步一次，玩家中途改语言也能跟上
    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        applyFromOptions();
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
