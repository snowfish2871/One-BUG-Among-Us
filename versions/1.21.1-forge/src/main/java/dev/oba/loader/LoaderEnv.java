// 加载器适配层的入口。共享代码只认这个接口，具体实现按轨道提供。
package dev.oba.loader;

import java.nio.file.Path;
import java.util.List;

public interface LoaderEnv {

    // 这次实际在哪个加载器上跑，只用于状态显示（"Forge 47.4.26" 这种）
    String loaderName();

    // 加载器眼里的全部模组。用这个而不是直接扫 mods 目录，
    // 是因为开发环境和内嵌 jar 里的模组不在 mods 目录下
    List<LoadedMod> mods();

    // 游戏目录。走加载器自己的实现，比按当前工作目录猜准
    Path gameDir();

    // 模组事件总线（构造阶段那个）
    Object modEventBus();

    // 把这条轨道的加载器实现装进共享代码。构造函数里第一件事就调它
    void init();

    // 把自己记进"服务器停下来了"这件事，看门狗靠它解除武装。
    // 事件类两边不一样，所以由实现方自己挂
    void onServerStopping(Runnable callback);
}
