// 交接文件（handoff-*.json）的读取者。
//
// 为什么自己写一个 JSON 解析：这条路径跑在一个没有任何第三方库的 JVM 里，
// 而交接文件是我们自己写的，结构固定、层级很浅。为了它引一个依赖不值得。
// 解析器只实现 JSON 的完整语法，不认注释、不认单引号——写的时候也没用这些。
package dev.oba.companion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Handoff {

    public int schema = 1;
    public long writtenAt;
    public long startedAt;
    public String modVersion = "";
    public String loader = "";
    public String language = "";
    public String gameDir = "";
    public long gamePid;
    public String reason = "exit";
    /** 这一场有没有捕获到错误行；老版本写的交接文件里没有这个字段，按错误数推。 */
    public boolean hadErrors;
    public int capturedErrors;
    public String reportPath = "";
    public String reportText = "";
    public String headline = "";
    public final List<String> verdicts = new ArrayList<>();
    public final List<String> crashReports = new ArrayList<>();
    public final List<Issue> issues = new ArrayList<>();
    public final List<Entry> timeline = new ArrayList<>();
    public final Map<String, String> environment = new LinkedHashMap<>();
    public Culprit culprit;

    // 原始 JSON 文本，界面上「交接 JSON」那一页直接显示它
    public String raw = "";

    /** 捕获过错误就算“有事儿”，哪怕最后是点关闭正常退出的。 */
    public boolean hadErrors() {
        return hadErrors || capturedErrors > 0;
    }

    public boolean isCrash() {
        return "crash".equalsIgnoreCase(reason);
    }

    public static final class Culprit {
        public String name = "";
        public String file = "";
        public String modIds = "";
        public String version = "";
        public double score;
        public String confidence = "";
        public String location = "";
    }

    public static final class Issue {
        public String severity = "info";
        public String rule = "";
        public String title = "";
        public String detail = "";
        public String advice = "";
        public String source = "";
    }

    public static final class Entry {
        public String time = "";
        public String modName = "";
        public String type = "";
        public String text = "";
        public String detail = "";
    }

    private Handoff() {
    }

    // 读一份交接文件。读坏了返回 null，调用方退回去显示崩溃报告原文
    public static Handoff read(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return fromJson(text);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public static Handoff fromJson(String text) {
        Object root = Json.parse(text);
        if (!(root instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) root;
        Handoff h = new Handoff();
        h.raw = text;
        h.schema = (int) num(map.get("schema"), 1);
        h.writtenAt = (long) num(map.get("writtenAt"), 0);
        h.startedAt = (long) num(map.get("startedAt"), 0);
        h.modVersion = str(map.get("modVersion"));
        h.loader = str(map.get("loader"));
        h.language = str(map.get("language"));
        h.gameDir = str(map.get("gameDir"));
        h.gamePid = (long) num(map.get("gamePid"), 0);
        h.reason = str(map.get("reason"), "exit");
        h.hadErrors = Boolean.TRUE.equals(map.get("hadErrors"));
        h.capturedErrors = (int) num(map.get("capturedErrors"), 0);
        h.reportPath = str(map.get("reportPath"));
        h.reportText = str(map.get("reportText"));
        h.headline = str(map.get("headline"));

        for (Object o : list(map.get("verdicts"))) {
            h.verdicts.add(str(o));
        }
        for (Object o : list(map.get("crashReports"))) {
            h.crashReports.add(str(o));
        }

        if (map.get("culprit") instanceof Map<?, ?> c) {
            Culprit culprit = new Culprit();
            culprit.name = str(c.get("name"));
            culprit.file = str(c.get("file"));
            culprit.modIds = str(c.get("modIds"));
            culprit.version = str(c.get("version"));
            culprit.score = num(c.get("score"), 0);
            culprit.confidence = str(c.get("confidence"));
            culprit.location = str(c.get("location"));
            h.culprit = culprit;
        }

        for (Object o : list(map.get("issues"))) {
            if (o instanceof Map<?, ?> m) {
                Issue issue = new Issue();
                issue.severity = str(m.get("severity"), "info").toLowerCase(java.util.Locale.ROOT);
                issue.rule = str(m.get("rule"));
                issue.title = str(m.get("title"));
                issue.detail = str(m.get("detail"));
                issue.advice = str(m.get("advice"));
                issue.source = str(m.get("source"));
                h.issues.add(issue);
            }
        }

        for (Object o : list(map.get("timeline"))) {
            if (o instanceof Map<?, ?> m) {
                Entry entry = new Entry();
                entry.time = str(m.get("time"));
                entry.modName = str(m.get("modName"));
                entry.type = str(m.get("type"));
                entry.text = str(m.get("text"));
                entry.detail = str(m.get("detail"));
                h.timeline.add(entry);
            }
        }

        if (map.get("environment") instanceof Map<?, ?> env) {
            for (Map.Entry<?, ?> e : env.entrySet()) {
                h.environment.put(str(e.getKey()), str(e.getValue()));
            }
        }
        return h;
    }

    public String writtenText() {
        if (writtenAt <= 0) {
            return "";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(writtenAt));
    }

    public String headlineOrDefault() {
        if (!headline.isBlank()) {
            return headline;
        }
        return isCrash() ? "游戏崩溃了，但日志里没有能定位到模组的线索。" : "这场游戏正常退出。";
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
        return o instanceof List ? (List<Object>) o : List.of();
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String str(Object o, String fallback) {
        String s = o == null ? "" : String.valueOf(o);
        return s.isEmpty() ? fallback : s;
    }

    private static double num(Object o, double fallback) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    // 够用的 JSON 解析器：对象、数组、字符串、数字、true/false/null
    static final class Json {
        private final String s;
        private int i;

        private Json(String s) {
            this.s = s;
        }

        static Object parse(String text) {
            Json p = new Json(text);
            p.ws();
            Object v = p.value();
            p.ws();
            if (p.i != p.s.length()) {
                throw new IllegalArgumentException("trailing data at " + p.i);
            }
            return v;
        }

        private Object value() {
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> {
                    expect("true");
                    yield Boolean.TRUE;
                }
                case 'f' -> {
                    expect("false");
                    yield Boolean.FALSE;
                }
                case 'n' -> {
                    expect("null");
                    yield null;
                }
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            i++; // {
            ws();
            if (peek() == '}') {
                i++;
                return map;
            }
            while (true) {
                ws();
                String key = string();
                ws();
                if (s.charAt(i) != ':') {
                    throw new IllegalArgumentException("expected ':' at " + i);
                }
                i++;
                ws();
                map.put(key, value());
                ws();
                char c = s.charAt(i++);
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("expected ',' at " + (i - 1));
                }
            }
        }

        private List<Object> array() {
            List<Object> out = new ArrayList<>();
            i++; // [
            ws();
            if (peek() == ']') {
                i++;
                return out;
            }
            while (true) {
                ws();
                out.add(value());
                ws();
                char c = s.charAt(i++);
                if (c == ']') {
                    return out;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("expected ',' at " + (i - 1));
                }
            }
        }

        private String string() {
            if (s.charAt(i) != '"') {
                throw new IllegalArgumentException("expected string at " + i);
            }
            i++;
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') {
                    return b.toString();
                }
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"' -> b.append('"');
                    case '\\' -> b.append('\\');
                    case '/' -> b.append('/');
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case 'u' -> {
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw new IllegalArgumentException("bad escape \\" + e);
                }
            }
        }

        private Double number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            return Double.parseDouble(s.substring(start, i));
        }

        private void expect(String word) {
            if (!s.startsWith(word, i)) {
                throw new IllegalArgumentException("expected " + word + " at " + i);
            }
            i += word.length();
        }

        private char peek() {
            return i < s.length() ? s.charAt(i) : '\0';
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
    }
}
