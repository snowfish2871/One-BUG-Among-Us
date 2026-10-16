// 出问题前后那段时间到底发生了什么：按时间把日志串成一条线，
// 每条记下是哪个模组在说话、说的是什么、算不算异常。
package dev.oba.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Timeline {

    // 时间戳的几种常见写法，例如 [12:34:56]、[12:34:56.789]、[2026-10-10 12:34:56]
    private static final Pattern TIME_BRACKET = Pattern.compile(
            "\\[(\\d{1,2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?)\\]");
    // NeoForge 的实际格式：[1010月2026 12:34:56.789]
    private static final Pattern TIME_NEOLOCAL = Pattern.compile(
            "\\[\\d{1,4}月\\d{1,4}\\s+(\\d{1,2}:\\d{2}:\\d{2}(?:\\.\\d{1,3})?)\\]");
    // 带 logger 的日志行，例如 [12:34:56] [main/ERROR] [some.logger/]: message
    private static final Pattern LOGGER = Pattern.compile(
            "\\[[^\\]]*?/([\\w.$]+)/?[^\\]]*?\\]\\s*:?\\s*");

    // 一条时间线记录。text 存的是文案键，换语言之后还能正确重渲
    public record Entry(String time, String modId, String modName, Lang.Ref text,
                        Type type, String detail) {
        public String renderText() {
            return text.resolve();
        }
    }

    public enum Type {
        // 普通信息
        INFO,
        // 警告
        WARN,
        // 错误
        ERROR,
        // 致命
        FATAL,
        // 命中了规则
        RULE,
        // 模组加载、卸载这类结构性事件
        LIFECYCLE
    }

    private final Deque<Entry> entries = new ArrayDeque<>();
    private final int capacity;

    public Timeline(int capacity) {
        this.capacity = Math.max(16, capacity);
    }

    public synchronized void add(Entry e) {
        entries.addLast(e);
        while (entries.size() > capacity) {
            entries.removeFirst();
        }
    }

    public synchronized List<Entry> entries() {
        return List.copyOf(entries);
    }

    public synchronized int size() {
        return entries.size();
    }

    // 按离出事有多近排一份，最靠近末尾的排前面。
    // 报告里先讲出事当时，再往前追。
    public synchronized List<Entry> reversed() {
        List<Entry> all = new ArrayList<>(entries);
        Collections.reverse(all);
        return List.copyOf(all);
    }

    // 日志级别。NeoForge 写成 [线程/级别]，方括号里最后一个斜杠之后那段
    private static final Pattern LEVEL = Pattern.compile(
            "\\[\\s*[^\\]]*?/(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s*\\]",
            Pattern.CASE_INSENSITIVE);

    // 崩溃报告的表头也是先给时间戳，正文里再出异常
    private static final Pattern TIME_REPORT = Pattern.compile(
            "^\\s*Time:\\s*\\d{4}-\\d{2}-\\d{2}[ T](\\d{1,2}:\\d{2}:\\d{2})");

    // 从一行日志里抠出级别，抠不到返回空串
    public static String extractLevel(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        Matcher m = LEVEL.matcher(line);
        return m.find() ? m.group(1).toUpperCase(java.util.Locale.ROOT) : "";
    }

    // 从一行日志里抠出时间戳，抠不到返回空串
    public static String extractTime(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        Matcher m = TIME_NEOLOCAL.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        // 崩溃报告没有逐行时间戳，只有开头一个 Time:，
        // 抽出来给整份报告当时间，不然时间线那一列全是空的
        m = TIME_REPORT.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        m = TIME_BRACKET.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    // 从一行日志里抠出 logger 名，用它反查是哪个模组在说话。
    // 形如 [12:00:00] [main/ERROR] [com.example.Foo/]: ...
    public static String extractLogger(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }
        // 先去掉时间那段，免得把时间当成 logger
        int firstClose = line.indexOf(']');
        if (firstClose < 0) {
            return "";
        }
        String rest = line.substring(firstClose + 1);
        Matcher m = LOGGER.matcher(rest);
        if (m.find()) {
            String logger = m.group(1);
            // 末尾的类名段没意义，取到最后一个点之前；取不到就用整段
            int dot = logger.lastIndexOf('.');
            return dot > 0 ? logger.substring(0, dot) : logger;
        }
        return "";
    }

    // 根据 logger 名猜是哪个模组。日志里的 logger 通常是模组的包名，
    // 拿它和每个模组索引里的包名对一下。
    public static ModSource ownerOf(String logger, java.util.Map<String, ModSource> byModId,
                                    ClassIndex index) {
        if (logger == null || logger.isEmpty()) {
            return null;
        }
        if (index != null) {
            ClassIndex.Match m = index.resolve(logger + ".X");
            if (m != null) {
                return m.source();
            }
        }
        // 有些模组直接拿 modId 当 logger 名
        if (byModId != null) {
            for (String id : candidateIds(logger)) {
                ModSource s = byModId.get(id);
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    // logger 名里可能出现的 modId：整段、第一段、最后一段
    private static List<String> candidateIds(String logger) {
        List<String> out = new ArrayList<>();
        out.add(logger);
        int dot = logger.indexOf('.');
        if (dot > 0) {
            out.add(logger.substring(0, dot));
        }
        int last = logger.lastIndexOf('.');
        if (last > 0 && last + 1 < logger.length()) {
            out.add(logger.substring(last + 1));
        }
        return out;
    }
}
