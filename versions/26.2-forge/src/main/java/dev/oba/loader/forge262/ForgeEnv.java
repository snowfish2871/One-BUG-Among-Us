// Forge 26.2 轨道的加载器适配实现。
//
// 和 1.20.1 那份（src/loader/forge）不能共用：Forge 26.x 把事件总线换成了
// net.minecraftforge.eventbus.api.bus.*，旧的 net.minecraftforge.eventbus.api.IEventBus
// 已经不存在；ModList.get() 也没了，改成 ModList.getModFiles() 逐个文件取模组。
// 所以这条轨道单独一份实现，别的 Forge 轨道照旧用旧的那份。
package dev.oba.loader.forge262;

import dev.oba.loader.LoadedMod;
import dev.oba.loader.Loader;
import dev.oba.loader.LoaderEnv;
import dev.oba.loader.TickHook;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ForgeEnv implements LoaderEnv {

    @Override
    public String loaderName() {
        return "Forge";
    }

    @Override
    public List<LoadedMod> mods() {
        List<LoadedMod> out = new ArrayList<>();
        try {
            // 26.x 没有 ModList.get()：一个 jar 可能声明多个模组，所以先拿文件再拿模组
            for (IModFileInfo fileInfo : ModList.getModFiles()) {
                Path file;
                try {
                    file = fileInfo.getFile().getFilePath();
                } catch (Throwable ignored) {
                    continue;
                }
                if (file == null) {
                    continue;
                }
                for (IModInfo info : fileInfo.getMods()) {
                    try {
                        out.add(new LoadedMod(info.getModId(), info.getDisplayName(),
                                String.valueOf(info.getVersion()), file));
                    } catch (Throwable ignored) {
                        // 解析不了的条目就不进索引
                    }
                }
            }
        } catch (Throwable ignored) {
            // 加载器还没起来，返回空表
        }
        return out;
    }

    @Override
    public Path gameDir() {
        try {
            return FMLPaths.GAMEDIR.get();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public Object modEventBus() {
        // 模组事件总线由构造函数自己取，共享代码用不到
        return null;
    }

    @Override
    public void init() {
        Loader.bind(this);
    }

    @Override
    public void onServerStopping(Runnable callback) {
        // 由 TickHook 一并挂，这里不用重复
    }

    public static TickHook tickHook(Runnable onTick, Runnable onStopping) {
        return new ForgeTickHook(onTick, onStopping);
    }
}
