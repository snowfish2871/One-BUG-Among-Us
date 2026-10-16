// NeoForge 轨道的模组入口（1.20.2 起，含 1.21.x）。
//
// 这一份是各轨道自己的：@Mod 注解和事件类都来自 net.neoforged，
// Forge 那边没有这些类。真正干活的逻辑都在共享的 ObaLifecycle 里。
package dev.oba.mod;

import dev.oba.core.Oba;
import dev.oba.loader.neoforge.NeoForgeEnv;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod(Oba.MOD_ID)
public final class ObaMod {

    private final ObaLifecycle lifecycle = ObaLifecycle.get();

    public ObaMod(IEventBus modBus) {
        // 先把加载器实现装进共享代码，后面所有取模组列表、取游戏目录的调用都走它
        ObaLifecycle.registerSafely(() -> new NeoForgeEnv().init(), "loaderEnv");
        // 语言要在最早的时候定下来：模组如果在构造阶段就崩，
        // FMLCommonSetupEvent 不会触发，后面那次读语言就没机会跑了
        EarlyLanguage.seedIfClient();

        // 客户端怎么读语言是这一侧的事，塞给共享的生命周期去调
        if (FMLEnvironment.dist == Dist.CLIENT) {
            lifecycle.setClientReadyAction(ClientLanguage::applyFromOptions);
            // 聊天栏的排版得按真字体宽度算，服务端没有字体这个概念
            ObaLifecycle.registerSafely(ClientFont::install, "clientFont");
            // 开发机上给聊天栏界面拍照用（tools/ui-shot.ps1）。不加这个 JVM 参数
            // 连 UiShot 这个类都不会被装载，正常玩永远不触发
            if (System.getProperty("oba.ui.shot") != null) {
                ObaLifecycle.registerSafely(UiShot::arm, "uiShot");
            }
        }

        lifecycle.start();

        // 事件注册同样单独兜一层：少了某个事件类型只该让对应功能失效
        ObaLifecycle.registerSafely(() -> modBus.addListener(this::onCommonSetup), "commonSetup");
        // tick 事件在 1.20.4 和 1.20.5 之间换过类，交给适配层按运行时探到的版本挂
        ObaLifecycle.registerSafely(
                () -> lifecycle.watchdog().tickHook(NeoForgeEnv::tickHook).register(), "tick");
        // 命令树由下面那个 onRegisterCommands 挂，别再往事件总线上注册 commands 实例：
        // 它身上没有 @SubscribeEvent，NeoForge 会直接抛
        // “class dev.oba.mod.ObaCommands has no @SubscribeEvent methods”
        ObaLifecycle.registerSafely(() -> NeoForge.EVENT_BUS.register(this), "self");
        // 语言同步只在客户端挂：服务端没有语言设置，
        // 而且客户端的类在服务端根本不该被加载
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ObaLifecycle.registerSafely(
                    () -> NeoForge.EVENT_BUS.register(new ClientLanguage()), "clientLanguage");
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

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        lifecycle.commands().onRegisterCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        lifecycle.onServerStarted();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        lifecycle.onServerStopping();
    }

    // 游戏进程要结束时兜底。崩到 FML 都收不了场的情况下，
    // 采集到的堆栈和分析结果也能留一份在 logs 目录里
    @SubscribeEvent
    public void onGameShuttingDown(GameShuttingDownEvent event) {
        lifecycle.onGameShuttingDown();
    }
}
