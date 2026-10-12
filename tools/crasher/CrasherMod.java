// 故意写坏的测试模组：启动时抛异常，用来验证 Oba 能不能定位到它。
package com.example.crasher;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod("crasher")
public class CrasherMod {

    public CrasherMod() {
        // 构造函数里就炸，让加载阶段直接失败
        throw new IllegalStateException("Oba 测试模组：故意在构造阶段抛异常 (CrasherMod.java:14)");
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        // 走不到这里
    }
}
