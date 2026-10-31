// 挂在 log4j2 上的采集器，游戏写到日志的每一行这里都能拿到。
package dev.oba.mod;

import dev.oba.core.Lang;
import dev.oba.core.Oba;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

public final class DiagnosticAppender extends AbstractAppender {

    private static final String NAME = "ObaCapture";
    private static final int MAX_LINE = 4000;

    private final Diagnostics diagnostics;

    private DiagnosticAppender(Diagnostics diagnostics) {
        super(NAME, null, PatternLayout.createDefaultLayout(), false, null);
        this.diagnostics = diagnostics;
    }

    // 挂载采集器，重复调用也没事
    public static synchronized boolean install(Diagnostics diagnostics) {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();
            if (config.getAppender(NAME) != null) {
                return true;
            }
            DiagnosticAppender appender = new DiagnosticAppender(diagnostics);
            appender.start();
            config.addAppender(appender);

            // 挂到 root logger 上。这个接口会同时维护 appender 表和真正的派发路径。
            // 注意：直接改 LoggerConfig，或者只调 Configuration.addAppender，
            // 都只是装上了但事件到不了；而 LoggerConfig.getAppenderRefs()
            // 是个过期快照，不能拿它判断挂载成功没有
            org.apache.logging.log4j.core.Logger rootLogger =
                    context.getLogger(LogManager.ROOT_LOGGER_NAME);
            config.addLoggerAppender(rootLogger, appender);
            context.updateLoggers();

            if (!appender.isStarted()) {
                LogManager.getLogger(Oba.MOD_ID).warn(
                        "{}" + Lang.get("mod.appenderNotStarted"), Oba.LOG_PREFIX);
                return false;
            }
            return true;
        } catch (Throwable t) {
            // 日志后端不是 log4j2 core。不致命，退回文件监听照样能用
            LogManager.getLogger(Oba.MOD_ID)
                    .warn("{}" + Lang.get("mod.appenderFailed", t.toString()), Oba.LOG_PREFIX);
            return false;
        }
    }

    @Override
    public void append(LogEvent event) {
        try {
            Level level = event.getLevel();
            // WARN 及以上都进滚动窗口（不少模组把问题报在 WARN）；但只有 ERROR 和
            // FATAL 才触发分析，免得普通警告刷出一堆结论。
            //
            // 注意别用 isInRange：log4j2 的严重度是反着用 int 值的（FATAL=100 比
            // INFO=400 更严重），isInRange(ERROR, ALL) 会被读成"从 ERROR 一直到 ALL"，
            // 等于把 INFO/DEBUG 全算成错误。专用服务端实测过这个写法：一次 112 行、
            // 一行 ERROR 都没有的启动，它数出「捕获到 2284 行错误」。isMoreSpecificThan
            // 是"同等或更严重"，配 OFF 的排除就够了
            boolean isError = isErrorLevel(level);
            boolean isFatal = isFatalLevel(level);

            StringBuilder sb = new StringBuilder(160);
            if (event.getMessage() != null) {
                String formatted;
                try {
                    formatted = event.getMessage().getFormattedMessage();
                } catch (Throwable t) {
                    formatted = String.valueOf(event.getMessage());
                }
                sb.append(formatted);
            }
            Throwable thrown = event.getThrown();
            if (thrown != null) {
                sb.append('\n').append(thrown.getClass().getName());
                if (thrown.getMessage() != null) {
                    sb.append(": ").append(thrown.getMessage());
                }
                StackTraceElement[] trace = thrown.getStackTrace();
                int limit = Math.min(trace.length, 60);
                for (int i = 0; i < limit; i++) {
                    sb.append("\n\tat ").append(trace[i]);
                }
                for (Throwable cause = thrown.getCause(); cause != null; cause = cause.getCause()) {
                    sb.append("\nCaused by: ").append(cause.getClass().getName());
                    if (cause.getMessage() != null) {
                        sb.append(": ").append(cause.getMessage());
                    }
                    StackTraceElement[] ct = cause.getStackTrace();
                    int cl = Math.min(ct.length, 30);
                    for (int i = 0; i < cl; i++) {
                        sb.append("\n\tat ").append(ct[i]);
                    }
                }
            }

            String line = sb.length() > MAX_LINE ? sb.substring(0, MAX_LINE) : sb.toString();
            // 忽略自己打的日志，别自己吃自己。
            // 用 contains 而不是 startsWith：我们自己那行前面可能还带着
            // "[时间] [线程/级别] [logger]:" 之类的格式前缀，从行首判断会漏掉，
            // 漏掉就会把自己的结论当成新错误再分析一遍，滚出一串越来越大的报告
            if (line.contains(Oba.LOG_PREFIX)
                    || Oba.MOD_ID.equals(event.getLoggerName())) {
                return;
            }
            diagnostics.onLogLine(line, isError, isFatal);
        } catch (Throwable ignored) {
            // 诊断绝不能让日志本身出错
        }
    }

    /** 这条日志算不算"错误"：ERROR 及更严重（FATAL）。OFF 不算。 */
    public static boolean isErrorLevel(Level level) {
        return level != null && level != Level.OFF
                && level.isMoreSpecificThan(Level.ERROR);
    }

    /** 只有 FATAL 才叫致命：ERROR 已经有人接住了，不需要立刻重算结论。 */
    public static boolean isFatalLevel(Level level) {
        return level != null && level != Level.OFF
                && level.isMoreSpecificThan(Level.FATAL);
    }
}
