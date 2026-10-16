// Forge 轨道的模组入口（1.20.1）。
//
// 和 NeoForge 那份长得像，但注解和事件类全在 net.minecraftforge.* 下，
// 两边没有共同的父类可用，所以只能各写一份。逻辑都在共享的 ObaLifecycle 里。
package dev.oba.mod;

import dev.oba.core.Oba;
import dev.oba.loader.forge.ForgeEnv;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Oba.MOD_ID)
public final class ObaMod {

    private final ObaLifecycle lifecycle = ObaLifecycle.get();

    // Forge 的 @Mod 构造函数必须是**无参**的，这和 NeoForge 不一样：
    // NeoForge 会把模组事件总线当参数注入，Forge 得自己从加载上下文里取。
    // 写成带参数的会直接报 NoSuchMethodException: <init>()
    public ObaMod() {
        // 先把加载器实现装进共享代码，后面所有取模组列表、取游戏目录的调用都走它
        ObaLifecycle.registerSafely(() -> new ForgeEnv().init(), "loaderEnv");
        // 语言要在最早的时候定下来：模组如果在构造阶段就崩，
        // FMLCommonSetupEvent 不会触发，后面那次读语言就没机会跑了
        EarlyLanguage.seedIfClient();

        // 客户端怎么读语言是这一侧的事，塞给共享的生命周期去调。
        // 服务端没有语言设置，而且客户端的类在服务端根本不该被加载，
        // 所以先判环境再登记
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
        ObaLifecycle.registerSafely(() -> {
            IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
            modBus.addListener(this::onCommonSetup);
        }, "commonSetup");
        // 1.20.1 的 tick 只有一套事件，直接挂
        ObaLifecycle.registerSafely(
                () -> lifecycle.watchdog().tickHook(ForgeEnv::tickHook).register(), "tick");
        // 命令树由下面那个 onRegisterCommands 挂，别再注册 commands 实例：
        // 它身上没有 @SubscribeEvent，事件总线会当作无效监听器
        ObaLifecycle.registerSafely(() -> MinecraftForge.EVENT_BUS.register(this), "self");
        // 玩家进世界/连服务器时同步一次语言，中途改语言也能跟上
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ObaLifecycle.registerSafely(
                    () -> MinecraftForge.EVENT_BUS.register(new ClientLanguage()), "clientLanguage");
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
}
