// Forge 轨道的加载器适配实现（1.20.1）。
//
// 和 NeoForge 那边的差别主要在包名：
//   @Mod 注解   net.minecraftforge.fml.common.Mod
//   ModList     net.minecraftforge.fml.ModList（在 fmlcore 里）
//   模组信息    net.minecraftforge.forgespi.language.IModInfo
//   事件总线    net.minecraftforge.common.MinecraftForge.EVENT_BUS
package dev.oba.loader.forge;

import dev.oba.loader.LoadedMod;
import dev.oba.loader.Loader;
import dev.oba.loader.LoaderEnv;
import dev.oba.loader.TickHook;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
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
            ModList modList = ModList.get();
            if (modList == null) {
                return out;
            }
            for (IModInfo info : modList.getMods()) {
                try {
                    // Forge 这里返回的就是个 Path，不像 NeoForge 是路径抽象，
                    // 但两边都按"可能是 Path 也可能要 toString"处理，省得再分家
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
        // Forge 的模组事件总线由构造函数注入，共享代码用不到
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
        return new ForgeTickHook(onTick, onStopping);
    }
}
