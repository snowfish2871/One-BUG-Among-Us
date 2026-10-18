// 模组生命周期的共享部分。
//
// Forge 和 NeoForge 的入口点长得很像但类名全不一样：@Mod 在
// net.minecraftforge.fml.common 还是 net.neoforged.fml.common、
// 事件总线叫 MinecraftForge.EVENT_BUS 还是 NeoForge.EVENT_BUS。
// 所以入口类按轨道各写一份，真正干活的逻辑全在这里，
// 两边只是各自把事件转进来。
package dev.oba.mod;

import dev.oba.core.Diagnosis;
import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.loader.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ObaLifecycle {

    private static final Logger LOGGER = LogManager.getLogger(Oba.MOD_ID);

    private static volatile ObaLifecycle instance;

    private final Diagnostics diagnostics = Diagnostics.get();
    private final Watchdog watchdog = new Watchdog(diagnostics);
    private final ObaCommands commands = new ObaCommands(diagnostics);
    // 事件注册失败过就记下来，状态命令里能看见
    private volatile boolean initialized;

    private ObaLifecycle() {
    }

    public static ObaLifecycle get() {
        ObaLifecycle local = instance;
        if (local == null) {
            synchronized (ObaLifecycle.class) {
                local = instance;
                if (local == null) {
                    local = new ObaLifecycle();
                    instance = local;
                }
            }
        }
        return local;
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }

    public Watchdog watchdog() {
        return watchdog;
    }

    public ObaCommands commands() {
        return commands;
    }

    public boolean initialized() {
        return initialized;
    }

    // 构造函数里第一件事：装采集器、建索引、挂退出钩子。
    // 这一段和加载器无关，两边共用
    public void start() {
        // 自己的输出走标准 logger，这样和别人的日志落在一处，
        // 采集器也才能认出并忽略它
        diagnostics.setOutput(line -> LOGGER.info(line));

        // 整个初始化都包起来。这是个诊断工具，最不该做的事就是自己把游戏弄崩；
        // 而它又要在多个小版本和多套加载器上跑，万一哪个 API 变了，
        // 正确的表现是安静地不工作，而不是拦住启动
        try {
            // 采集器要在构造函数里就挂上。在构造阶段就抛异常的模组并不少见，
            // 而 FMLCommonSetupEvent 要等所有模组构造完才触发 —— 挂在那边的话，
            // 只要有模组在构造阶段就炸，这一整场运行什么都采集不到
            DiagnosticAppender.install(diagnostics);
            // 索引挪到后台建。它要把每一个模组 jar 解开读一遍类名，
            // 整合包大的时候这就是启动变慢的主因；而索引没好之前撞上的致命错误
            // 会先攒在 verdictPending 里，建完立刻补一份结论，所以不用再抢时间
            diagnostics.reindex(true, diagnostics::armImmediateAnalysis, false);
            // 记下此刻已有的崩溃报告，之后新出现的才算这次的
            diagnostics.rememberExistingCrashReports();
            // 索引还没建的时候先盯文件
            diagnostics.startTailing();
            diagnostics.installExitHook();
            initialized = true;
        } catch (Throwable t) {
            LOGGER.warn("{}" + Lang.get("mod.initFailed", t), Oba.LOG_PREFIX);
        }
    }

    // 所有模组都构造完了。建索引、启动看门狗
    public void onCommonSetup() {
        try {
            // 客户端这时可能已经能读到玩家语言了；服务端就用 JVM 区域
            if (Loader.env().mods().isEmpty()) {
                // 加载器还没准备好列模组，等一下服务器启动事件再补
                LOGGER.debug("{}mod list not ready yet, will retry on server start", Oba.LOG_PREFIX);
            }
            // 构造函数里已经建过一份，这里只在 mods 目录变过时才重扫
            diagnostics.reindex(true, null, false);
            watchdog.start();
            LOGGER.info("{}" + Lang.get("mod.enabled", Oba.VERSION, Loader.env().loaderName()),
                    Oba.LOG_PREFIX);
            LOGGER.info("{}" + Lang.get("mod.reportFolder", diagnostics.reportDirectory()),
                    Oba.LOG_PREFIX);
            LOGGER.info("{}" + Lang.get("mod.helpHint"), Oba.LOG_PREFIX);
        } catch (Throwable t) {
            LOGGER.warn("{}" + Lang.get("mod.initFailed", t), Oba.LOG_PREFIX);
        }
    }

    // 客户端初始化完成，可以读玩家选的语言了。
    // 具体怎么读是客户端侧的事，各轨道的入口把动作塞进来 ——
    // 共享代码不该知道 Minecraft 客户端那些类
    private volatile Runnable clientReady;

    // 由各轨道的入口调用，登记"客户端就绪时做什么"
    public void setClientReadyAction(Runnable action) {
        this.clientReady = action;
    }

    public void onClientReady() {
        Runnable action = clientReady;
        if (action == null) {
            return;
        }
        try {
            action.run();
        } catch (Throwable t) {
            LOGGER.debug("{}" + Lang.get("mod.clientReadyFailed", t), Oba.LOG_PREFIX);
        }
    }

    // 服务器起来了，索引缺了或者过期就补一次
    public void onServerStarted() {
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

    // 服务器要停了
    public void onServerStopping() {
        printSessionSummary();
    }

    // 游戏进程要结束时兜底。崩到 FML 都收不了场的情况下，
    // 采集到的堆栈和分析结果也能留一份在 logs 目录里
    public void onGameShuttingDown() {
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

    // 注册一个东西，失败就记一句，不影响其他部分
    public static void registerSafely(Runnable action, String what) {
        try {
            action.run();
        } catch (Throwable t) {
            LOGGER.warn("{}" + Lang.get("mod.registerFailed", what, t), Oba.LOG_PREFIX);
        }
    }
}
