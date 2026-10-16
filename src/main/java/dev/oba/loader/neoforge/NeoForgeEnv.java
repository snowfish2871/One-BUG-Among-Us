// NeoForge 轨道的加载器适配实现（1.20.2 起，含 1.21.x）。
package dev.oba.loader.neoforge;

import dev.oba.loader.LoadedMod;
import dev.oba.loader.Loader;
import dev.oba.loader.LoaderEnv;
import dev.oba.loader.TickHook;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class NeoForgeEnv implements LoaderEnv {

    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public List<LoadedMod> mods() {
        List<LoadedMod> out = new ArrayList<>();
        try {
            ModList modList = ModList.get();
            if (modList == null) {
                return out;
            }
            for (IModInfo info : modList.getMods()) {
                try {
                    // getFilePath 返回的是加载器自己的路径抽象，
                    // toString 一下才是 jar 的真实文件系统路径
                    Object raw = info.getOwningFile().getFile().getFilePath();
                    Path file = raw instanceof Path p ? p : Path.of(String.valueOf(raw));
                    if (file == null) {
                        continue;
                    }
                    out.add(new LoadedMod(info.getModId(), info.getDisplayName(),
                            String.valueOf(info.getVersion()), file));
                } catch (Throwable ignored) {
                    // 解析不了的条目就不进索引，没别的办法
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
        // NeoForge 把模组事件总线直接交给构造函数，共享代码用不到，
        // 这里留空即可
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

    // 造一个适合这个版本的 tick 挂载点
    public static TickHook tickHook(Runnable onTick, Runnable onStopping) {
        return new NeoForgeTickHook(onTick, onStopping);
    }
}
