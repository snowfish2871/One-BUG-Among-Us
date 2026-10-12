// Mod 入口：挂事件、启动索引和看门狗，退出时把报告落盘。
package dev.oba.mod;

import dev.oba.core.Diagnosis;
import dev.oba.core.Lang;
import dev.oba.core.Oba;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Oba.MOD_ID)
public final class ObaMod {

    private static final Logger LOGGER = LogManager.getLogger(Oba.MOD_ID);

    private final Diagnostics diagnostics = Diagnostics.get();
    private final Watchdog watchdog = new Watchdog(diagnostics);
    private final ObaCommands commands = new ObaCommands(diagnostics);

    public ObaMod(IEventBus modBus) {
        // 自己的输出走标准 logger，这样和别人的日志落在一处，采集器也才能认出并忽略它
        diagnostics.setOutput(line -> LOGGER.info(line));

        // 整个初始化都包起来。这是个诊断工具，最不该做的事就是自己把游戏弄崩；
        // 而它又要在多个 1.21.x 小版本上跑，万一哪个 API 变了，
        // 正确的表现是安静地不工作，而不是拦住启动
        try {
            // 采集器要在构造函数里就挂上。在构造阶段就抛异常的模组并不少见，
            // 而 FMLCommonSetupEvent 要等所有模组构造完才触发——挂在那边的话，
            // 只要有模组在构造阶段就炸，这一整场运行什么都采集不到
            DiagnosticAppender.install(diagnostics);
            // 索引也要在这时候先建一份，而且要同步建完：异步的话会和下面那句
            // armImmediateAnalysis 抢时间，第一份结论可能还是索引空空的时候出的。
            // 实机验证过：只要有模组在构造阶段抛异常，整个 setup 阶段都不会触发，
            // 等那边的 reindex 就等于永远等不到，报告里只会写"没有栈帧能归属到某个模组"
            diagnostics.reindex(false, null);
            diagnostics.armImmediateAnalysis();
            // 记下此刻已有的崩溃报告，之后新出现的才算这次的
            diagnostics.rememberExistingCrashReports();
            // 索引还没建的时候先盯文件
            diagnostics.startTailing();
            diagnostics.installExitHook();
        } catch (Throwable t) {
            LOGGER.warn("{}" + Lang.get("mod.initFailed", t), Oba.LOG_PREFIX);
        }

        // 事件注册同样单独兜一层：少了某个事件类型只该让对应功能失效
        registerSafely(() -> modBus.addListener(this::onCommonSetup), "commonSetup");
        registerSafely(() -> NeoForge.EVENT_BUS.register(this), "self");
        registerSafely(() -> NeoForge.EVENT_BUS.register(watchdog), "watchdog");
        registerSafely(() -> NeoForge.EVENT_BUS.register(commands), "commands");
    }

    // 注册一个东西，失败就记一句，不影响其他部分
    private static void registerSafely(Runnable action, String what) {
        try {
            action.run();
        } catch (Throwable t) {
            LOGGER.warn("{}" + Lang.get("mod.registerFailed", what, t), Oba.LOG_PREFIX);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        // 建索引要读所有模组 jar，别占着加载线程
        event.enqueueWork(() -> {
            try {
                // 客户端这时可能已经能读到玩家语言了；服务端就用 JVM 区域
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    ClientLanguage.applyFromOptions();
                }
                diagnostics.reindex(true, null);
                watchdog.start();
                LOGGER.info("{}" + Lang.get("mod.enabled", Oba.VERSION), Oba.LOG_PREFIX);
                LOGGER.info("{}" + Lang.get("mod.reportFolder", diagnostics.reportDirectory()),
                        Oba.LOG_PREFIX);
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    LOGGER.info("{}" + Lang.get("mod.helpHint"), Oba.LOG_PREFIX);
                }
            } catch (Throwable t) {
                LOGGER.warn("{}" + Lang.get("mod.initFailed", t), Oba.LOG_PREFIX);
            }
        });
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // 服务器起来了，索引缺了或者过期就补一次
        if (diagnostics.sources().isEmpty()) {
            diagnostics.reindex(true, null);
        }
        // 把上次留下的崩溃报告顺手报一下
        diagnostics.analyzeNewestCrashReport().ifPresent(report -> {
            Diagnosis.Finding primary = report.primarySuspect();
            if (primary != null) {
                LOGGER.info("{}" + Lang.get("mod.previousCrash", primary.display(),
                                primary.fileName(), primary.firstLocatedFrame() == null
                                        ? "?" : primary.firstLocatedFrame().location()),
                        Oba.LOG_PREFIX);
            }
        });
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        printSessionSummary();
    }

    // 游戏进程要结束时兜底。崩到 FML 都收不了场的情况下，
    // 采集到的堆栈和分析结果也能留一份在 logs 目录里。
    @SubscribeEvent
    public void onGameShuttingDown(GameShuttingDownEvent event) {
        diagnostics.writeReportOnExit();
        diagnostics.shutdown();
    }

    private void printSessionSummary() {
        if (diagnostics.capturedFatals() == 0 && diagnostics.capturedErrors() == 0) {
            LOGGER.info("{}" + Lang.get("mod.noErrors"), Oba.LOG_PREFIX);
            return;
        }
        LOGGER.info("{}" + Lang.get("mod.sessionSummary",
                        diagnostics.capturedErrors(), diagnostics.capturedFatals()),
                Oba.LOG_PREFIX);
        for (String verdict : diagnostics.recentVerdicts()) {
            LOGGER.info("{}", verdict);
        }
    }
}
