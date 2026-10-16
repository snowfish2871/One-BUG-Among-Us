// 故意写坏的测试模组（Forge 版）：启动时在构造函数里抛异常。
// 用来验证 OBA 能不能定位到它，以及崩溃时报告会不会自动落盘。
//
// 和 NeoForge 那份唯一的区别就是 @Mod 注解的包名：
// Forge 在 net.minecraftforge.fml.common，NeoForge 在 net.neoforged.fml.common。
package com.example.crasher;

import net.minecraftforge.fml.common.Mod;

@Mod("crasher")
public class CrasherMod {

    public CrasherMod() {
        // 构造函数里就炸，让加载阶段直接失败
        throw new IllegalStateException("OBA 测试模组：故意在构造阶段抛异常");
    }
}
