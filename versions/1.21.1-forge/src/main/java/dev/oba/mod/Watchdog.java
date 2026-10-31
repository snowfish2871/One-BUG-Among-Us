// 看着进程本身：tick 间隔、死锁、内存，卡住时抓一份线程转储。
package dev.oba.mod;

import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.core.ProcessProbe;
import dev.oba.loader.TickHook;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class Watchdog {

    // 超过这个耗时就记一笔。原版目标是 50ms
    private static final long LAG_WARN_MILLIS = 3000L;
    // 超过这个耗时就直接抓线程转储
    private static final long LAG_DUMP_MILLIS = 12000L;
    private static final long CHECK_INTERVAL_SECONDS = 15L;

    // 这些调用一出现，就说明服务器线程在干原版的重活，不是卡死。
    // 都是实机日志里见过的场景
    private static final java.util.List<String> ROUTINE_HEAVY_WORK = java.util.List.of(
            "ThreadedAnvilChunkStorage",
            "ChunkMap.save",
            "ServerChunkCache.save",
            "MinecraftServer.saveAllChunks",
            "MinecraftServer.stopServer",
            "LevelStorageSource",
            "ChunkStorage.",
            "StructureTemplateManager",
            "MinecraftServer.waitUntilNextTick");

    private final Diagnostics diagnostics;
    private final AtomicLong lastTickNanos = new AtomicLong(System.nanoTime());
    private final AtomicLong lastDumpNanos = new AtomicLong(0L);
    private final AtomicInteger lagEpisodes = new AtomicInteger();
    // 只有服务器真的开始 tick 了，按 tick 判断卡死才有意义，
    // 否则一个没进世界的客户端会被永远当成卡死
    private final AtomicBoolean serverTicking = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "Oba-watchdog");
                t.setDaemon(true);
                return t;
            });

    public Watchdog(Diagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    // 服务器 tick 了一下。具体是哪一套事件由各轨道的 TickHook 决定，
    // 这里只管记时间
    public void onServerTick() {
        lastTickNanos.set(System.nanoTime());
        serverTicking.set(true);
    }

    // 服务器停了，不会再有 tick 了，先解除武装，
    // 免得下一次检查把"服务器停了"当成卡死
    public void onServerStopping() {
        serverTicking.set(false);
    }

    // 造一个绑定到本看门狗的 tick 挂载点。各轨道的加载器实现负责真正挂上去
    public TickHook tickHook(java.util.function.BiFunction<Runnable, Runnable, TickHook> factory) {
        return factory.apply(this::onServerTick, this::onServerStopping);
    }

    // 开始周期性检查进程状态
    public void start() {
        scheduler.scheduleWithFixedDelay(this::check, CHECK_INTERVAL_SECONDS,
                CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private void check() {
        try {
            long now = System.nanoTime();

            // 内存吃紧是先卡后崩的常见原因
            Runtime rt = Runtime.getRuntime();
            long max = rt.maxMemory();
            long used = rt.totalMemory() - rt.freeMemory();
            if (max > 0 && used * 100 / max >= 93) {
                diagnostics.onLogLine(Oba.LOG_PREFIX + Lang.get("mod.memoryWarn",
                        used / 1048576, max / 1048576, used * 100 / max), true, false);
            }

            java.util.List<String> deadlocks = ProcessProbe.findDeadlockedThreads();
            if (!deadlocks.isEmpty()) {
                if (now - lastDumpNanos.get() > TimeUnit.SECONDS.toNanos(60)) {
                    lastDumpNanos.set(now);
                    reportStall(Lang.get("mod.stall.deadlock"), deadlocks.toString());
                }
            }

            if (!serverTicking.get()) {
                return;
            }
            long sinceTickMillis = (now - lastTickNanos.get()) / 1_000_000L;
            if (sinceTickMillis >= LAG_DUMP_MILLIS) {
                if (now - lastDumpNanos.get() > TimeUnit.SECONDS.toNanos(60)) {
                    lastDumpNanos.set(now);
                    int n = lagEpisodes.incrementAndGet();
                    reportStall(Lang.get("mod.stall", sinceTickMillis / 1000, n), "Server thread");
                }
            } else if (sinceTickMillis >= LAG_WARN_MILLIS) {
                diagnostics.onLogLine(Oba.LOG_PREFIX
                        + Lang.get("mod.lagWarn", sinceTickMillis / 1000), true, false);
            }
        } catch (Throwable ignored) {
            // 看门狗无论如何不能把游戏搞挂
        }
    }

    private void reportStall(String reason, String hint) {
        try {
            String dump = ProcessProbe.captureThreadDump(false);
            var report = ProcessProbe.analyzeThreadDump(dump, diagnostics.index(),
                    diagnostics.byModId(), hint.contains("Server") ? "Server thread" : null);
            report.scannedMods().clear();
            report.scannedMods().addAll(diagnostics.sources());
            report.setTitleKey("report.title.stall");

            // 服务器线程如果只是在做原版的重活（存档退出、刷地图、自动保存），
            // 那不算卡死。实机遇到过退出世界时卡 17 秒，全在存区块，
            // 报出来只会是噪音
            if (isRoutineHeavyWork(dump)) {
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(Oba.LOG_PREFIX).append(reason);
            var primary = report.primarySuspect();
            if (primary != null) {
                sb.append("\n").append(Oba.LOG_PREFIX)
                  .append(Lang.get("mod.stall.executing", primary.display(), primary.fileName()));
                var frame = primary.firstLocatedFrame();
                if (frame != null) {
                    sb.append("\n").append(Oba.LOG_PREFIX).append(Lang.get("mod.stall.location", frame.location()));
                }
                sb.append("\n").append(Oba.LOG_PREFIX)
                  .append(Lang.get("mod.stall.threadsHint"));
            } else {
                sb.append(Lang.get("mod.stall.notModCode"));
            }
            diagnostics.keepStallReport(sb.toString(), report);
        } catch (Throwable t) {
            diagnostics.onLogLine(Oba.LOG_PREFIX + Lang.get("mod.stall.failed", t), true, false);
        }
    }

    // 服务器线程是不是正在做原版的重活。
    // 存区块、卸载世界、结构生成这些东西一跑就是十几秒，服务器线程确实不 tick，
    // 但这不是故障。判断依据是转储里能不能看到这些标志性的调用。
    static boolean isRoutineHeavyWork(String dump) {
        if (dump == null) {
            return false;
        }
        for (String marker : ROUTINE_HEAVY_WORK) {
            if (dump.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
