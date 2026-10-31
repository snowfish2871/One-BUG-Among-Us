// 故意写坏的测试模组：启动时在构造函数里抛异常。
// 用来验证 OBA 能不能定位到它，以及崩溃时报告会不会自动落盘。
//
// 只留 @Mod 一个注解，不引任何生命周期事件 —— 这样 1.20.2 和 1.21.1
// 都能直接编译，同一份测试模组可以跨轨道复用。
package com.example.crasher;

import net.neoforged.fml.common.Mod;

@Mod("crasher")
public class CrasherMod {

    public CrasherMod() {
        // 构造函数里就炸，让加载阶段直接失败
        throw new IllegalStateException("OBA 测试模组：故意在构造阶段抛异常");
    }
}
