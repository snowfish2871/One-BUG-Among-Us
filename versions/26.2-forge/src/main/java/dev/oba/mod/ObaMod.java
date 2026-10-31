// Forge 26.2 轨道的模组入口。
//
// 逻辑都在共享的 ObaLifecycle 里，这里只负责把这一版的事件挂上去。
// 26.x 和 1.20.1 的差别：
//   事件总线    net.minecraftforge.eventbus.api.bus.EventBus，每个事件自带静态 BUS
//   生命周期    FMLCommonSetupEvent.getBus(模组总线组)
//   @Mod 构造   仍然必须无参
package dev.oba.mod;

import dev.oba.core.Oba;
import dev.oba.loader.forge262.ForgeEnv;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Oba.MOD_ID)
public final class ObaMod {

    private final ObaLifecycle lifecycle = ObaLifecycle.get();

    public ObaMod() {
        // 先把加载器实现装进共享代码，后面取模组列表、取游戏目录都走它
        ObaLifecycle.registerSafely(() -> new ForgeEnv().init(), "loaderEnv");
        // 语言要在最早的时候定下来：模组如果在构造阶段就崩，
        // 后面的 common setup 不会触发，那次读语言就没机会跑了
        EarlyLanguage.seedIfClient();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            lifecycle.setClientReadyAction(ClientLanguage::applyFromOptions);
            ObaLifecycle.registerSafely(ClientFont::install, "clientFont");
            // 开发机上给聊天栏界面拍照用。不加这个 JVM 参数连 UiShot 都不会被装载
            if (System.getProperty("oba.ui.shot") != null) {
                ObaLifecycle.registerSafely(UiShot::arm, "uiShot");
            }
        }

        lifecycle.start();

        // 每类事件单独兜一层：少了某一个只该让对应功能失效
        ObaLifecycle.registerSafely(() -> {
            BusGroup group = FMLJavaModLoadingContext.get().getModBusGroup();
            FMLCommonSetupEvent.getBus(group).addListener(this::onCommonSetup);
        }, "commonSetup");
        ObaLifecycle.registerSafely(
                () -> lifecycle.watchdog().tickHook(ForgeEnv::tickHook).register(), "tick");
        ObaLifecycle.registerSafely(
                () -> RegisterCommandsEvent.BUS.addListener(
                        event -> lifecycle.commands().onRegisterCommands(event.getDispatcher())),
                "commands");
        ObaLifecycle.registerSafely(
                () -> ServerStartedEvent.BUS.addListener(event -> lifecycle.onServerStarted()),
                "serverStarted");
        ObaLifecycle.registerSafely(
                () -> ServerStoppingEvent.BUS.addListener(event -> lifecycle.onServerStopping()),
                "serverStopping");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ObaLifecycle.registerSafely(ClientLanguage::install, "clientLanguage");
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 建索引要读所有模组 jar，别占着加载线程
        event.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                lifecycle.onClientReady();
            }
            lifecycle.onCommonSetup();
        });
    }
}
