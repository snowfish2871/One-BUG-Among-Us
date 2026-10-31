// 服务器 tick 的挂载点。
//
// 三套加载器/版本上这个事件长得完全不一样：
//   Forge 1.20.1        net.minecraftforge.event.TickEvent$ServerTickEvent，带 Phase
//   NeoForge 20.2~20.4  net.neoforged.neoforge.event.TickEvent$ServerTickEvent，带 Phase
//   NeoForge 20.5+      （新版）net.neoforged.neoforge.event.tick.ServerTickEvent$Post
// 所以共享代码只认这个接口，由各轨道的实现在运行时挑一套挂上去。
package dev.oba.loader;

public interface TickHook {

    // 挂到事件总线上。挂不上就安静地退化成"没有 tick 感知"
    void register();

    // 用的是哪一套，状态命令里显示；空串表示一套都没找到
    String describe();

    // 一次 tick 走完了
    void onTick();

    // 服务器不会再 tick 了
    void onStopping();
}
