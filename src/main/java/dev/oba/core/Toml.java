// 一个够用的 TOML 读取器，只为了读 neoforge.mods.toml 里的那几个字段。
// 解析不了的地方不报错，记一条 warning 就算了，不能因为描述文件格式怪就让诊断挂掉。
package dev.oba.core;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Toml {

    private Toml() {
    }

    // 解析结果，外面通过那几个 at/string/tables 方法取值
    public static final class Doc {
        private final Map<String, Object> root;
        private final List<String> warnings;

        Doc(Map<String, Object> root, List<String> warnings) {
            this.root = root;
            this.warnings = warnings;
        }

        public Map<String, Object> root() {
            return root;
        }

        // 读的时候遇到的小问题，都不致命
        public List<String> warnings() {
            return Collections.unmodifiableList(warnings);
        }

        // 按点分路径往下走，比如 at("dependencies", "examplemod")
        @SuppressWarnings("unchecked")
        public Optional<Object> at(String... path) {
            Object cur = root;
            for (String seg : path) {
                if (!(cur instanceof Map)) {
                    return Optional.empty();
                }
                cur = ((Map<String, Object>) cur).get(seg);
                if (cur == null) {
                    return Optional.empty();
                }
            }
            return Optional.of(cur);
        }

        public Optional<String> string(String... path) {
            return at(path).map(Toml::asString);
        }

        public Optional<Boolean> bool(String... path) {
            return at(path).map(v -> {
                if (v instanceof Boolean b) {
                    return b;
                }
                return Boolean.parseBoolean(String.valueOf(v).trim());
            });
        }

        // 值可能是单个也可能是列表，两种都收：authors = "a" 或 authors = ["a", "b"]
        @SuppressWarnings("unchecked")
        public List<String> list(String... path) {
            Object v = at(path).orElse(null);
            if (v == null) {
                return List.of();
            }
            if (v instanceof List<?> l) {
                List<String> out = new ArrayList<>(l.size());
                for (Object o : l) {
                    out.add(asString(o));
                }
                return out;
            }
            return List.of(asString(v));
        }

        // 取一棵子树，值保持原样
        @SuppressWarnings("unchecked")
        public Map<String, Object> table(String... path) {
            Object v = at(path).orElse(null);
            if (v instanceof Map) {
                return (Map<String, Object>) v;
            }
            return Map.of();
        }

        // 取数组表 [[xxx]] 的每一项
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> tables(String... path) {
            Object v = at(path).orElse(null);
            if (v instanceof List<?> l) {
                List<Map<String, Object>> out = new ArrayList<>(l.size());
                for (Object o : l) {
                    if (o instanceof Map) {
                        out.add((Map<String, Object>) o);
                    }
                }
                return out;
            }
            return List.of();
        }
    }

    public static Doc parse(String text) {
        return new Builder().run(text);
    }

    public static Doc parse(Path file) throws IOException {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder(4096);
            char[] buf = new char[8192];
            int n;
            while ((n = r.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
            return parse(sb.toString());
        }
    }

    // 实现

    private static final class Builder {
        final Map<String, Object> root = new LinkedHashMap<>();
        final List<String> warnings = new ArrayList<>();
        // 当前位置：裸键值对写进哪张表
        Map<String, Object> cursor = root;

        Doc run(String text) {
            String[] lines = text.split("\r\n|\n|\r", -1);
            int lineNo = 0;
            while (lineNo < lines.length) {
                String raw = lines[lineNo];
                lineNo++;
                String line = stripComment(raw).trim();
                if (line.isEmpty()) {
                    continue;
                }
                try {
                    if (line.startsWith("[[") && line.endsWith("]]")) {
                        cursor = openArrayTable(splitPath(line.substring(2, line.length() - 2).trim()));
                    } else if (line.startsWith("[") && line.endsWith("]")) {
                        cursor = openTable(splitPath(line.substring(1, line.length() - 1).trim()));
                    } else {
                        int eq = indexOfTopLevelEquals(line);
                        if (eq < 0) {
                            warnings.add("line " + lineNo + ": not a key/value pair, skipped");
                            continue;
                        }
                        String key = line.substring(0, eq).trim();
                        String rest = line.substring(eq + 1).trim();
                        String lit = takeLiteral(rest, lines, lineNo);
                        if (lit != null) {
                            // 多行字符串把后面的行吃掉了，行号要往前推
                            int consumed = countLines(rest, lines, lineNo);
                            lineNo += consumed;
                        }
                        put(cursor, splitPath(key), parseValue(lit == null ? rest : lit));
                    }
                } catch (RuntimeException ex) {
                    warnings.add("line " + lineNo + ": " + ex.getMessage());
                }
            }
            return new Doc(root, warnings);
        }

        // 取出 ''' 或 """ 包起来的内容，没有就返回 null
        private String takeLiteral(String rest, String[] lines, int lineNo) {
            String delim = null;
            if (rest.startsWith("'''")) {
                delim = "'''";
            } else if (rest.startsWith("\"\"\"")) {
                delim = "\"\"\"";
            }
            if (delim == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            String tail = rest.substring(3);
            // 同一行就收尾的常见情况
            if (tail.endsWith(delim) && tail.length() >= 3) {
                return tail.substring(0, tail.length() - 3);
            }
            sb.append(tail);
            for (int i = lineNo; i < lines.length; i++) {
                String l = lines[i];
                int end = l.indexOf(delim);
                if (end >= 0) {
                    sb.append('\n').append(l, 0, end);
                    return sb.toString();
                }
                sb.append('\n').append(l);
            }
            warnings.add("unterminated multi line string");
            return sb.toString();
        }

        // 这个多行字符串后面还跟着几行，算出来好跳过
        private int countLines(String rest, String[] lines, int lineNo) {
            String tail = rest.substring(3);
            if (tail.endsWith("'''") || tail.endsWith("\"\"\"")) {
                return 0;
            }
            String delim = rest.startsWith("'''") ? "'''" : "\"\"\"";
            for (int i = lineNo; i < lines.length; i++) {
                if (lines[i].contains(delim)) {
                    return (i - lineNo) + 1;
                }
            }
            return lines.length - lineNo;
        }

        private Map<String, Object> openTable(List<String> path) {
            Map<String, Object> cur = root;
            for (String seg : path) {
                Object next = cur.get(seg);
                if (next instanceof Map) {
                    cur = cast(next);
                } else if (next instanceof List<?> l && !l.isEmpty() && l.get(l.size() - 1) instanceof Map) {
                    // 已经开了数组表，就接着往最后一项里写
                    cur = cast(l.get(l.size() - 1));
                } else {
                    Map<String, Object> fresh = new LinkedHashMap<>();
                    cur.put(seg, fresh);
                    cur = fresh;
                }
            }
            return cur;
        }

        // 开一个新的数组表项，追加到同名列表末尾
        private Map<String, Object> openArrayTable(List<String> path) {
            Map<String, Object> parent = openTable(path.subList(0, path.size() - 1));
            String last = path.get(path.size() - 1);
            Map<String, Object> fresh = new LinkedHashMap<>();
            Object existing = parent.get(last);
            if (existing instanceof List) {
                castList(existing).add(fresh);
            } else {
                List<Object> list = new ArrayList<>();
                list.add(fresh);
                parent.put(last, list);
            }
            return fresh;
        }

        private void put(Map<String, Object> table, List<String> path, Object value) {
            Map<String, Object> cur = table;
            for (int i = 0; i < path.size() - 1; i++) {
                String seg = path.get(i);
                Object next = cur.get(seg);
                if (next instanceof Map) {
                    cur = cast(next);
                } else {
                    Map<String, Object> fresh = new LinkedHashMap<>();
                    cur.put(seg, fresh);
                    cur = fresh;
                }
            }
            cur.put(path.get(path.size() - 1), value);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> cast(Object o) {
            return (Map<String, Object>) o;
        }

        @SuppressWarnings("unchecked")
        private static List<Object> castList(Object o) {
            return (List<Object>) o;
        }
    }

    private static Object parseValue(String v) {
        if (v.isEmpty()) {
            return "";
        }
        char c = v.charAt(0);
        if (c == '"' || c == '\'') {
            return unquote(v);
        }
        if (c == '[') {
            return parseArray(v);
        }
        if (c == '{') {
            return parseInlineTable(v);
        }
        String lower = v.toLowerCase();
        if (lower.equals("true")) {
            return Boolean.TRUE;
        }
        if (lower.equals("false")) {
            return Boolean.FALSE;
        }
        return v;
    }

    private static List<Object> parseArray(String v) {
        List<Object> out = new ArrayList<>();
        String body = v.substring(1, v.endsWith("]") ? v.length() - 1 : v.length());
        for (String part : splitTopLevel(body, ',')) {
            String p = part.trim();
            if (!p.isEmpty()) {
                out.add(parseValue(p));
            }
        }
        return out;
    }

    private static Map<String, Object> parseInlineTable(String v) {
        Map<String, Object> out = new LinkedHashMap<>();
        String body = v.substring(1, v.endsWith("}") ? v.length() - 1 : v.length());
        for (String part : splitTopLevel(body, ',')) {
            int eq = indexOfTopLevelEquals(part);
            if (eq > 0) {
                out.put(part.substring(0, eq).trim(), parseValue(part.substring(eq + 1).trim()));
            }
        }
        return out;
    }

    // 按分隔符切，但引号和括号里的不算
    private static List<String> splitTopLevel(String s, char sep) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        char quote = 0;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (quote != 0) {
                cur.append(ch);
                // 前面是反斜杠的引号不算结束
                if (ch == quote && (i == 0 || s.charAt(i - 1) != '\\')) {
                    quote = 0;
                }
                continue;
            }
            if (ch == '"' || ch == '\'') {
                quote = ch;
                cur.append(ch);
            } else if (ch == '[' || ch == '{' || ch == '(') {
                depth++;
                cur.append(ch);
            } else if (ch == ']' || ch == '}' || ch == ')') {
                depth--;
                cur.append(ch);
            } else if (ch == sep && depth == 0) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }

    // 找第一个不在引号里的等号，就是键和值的分界
    private static int indexOfTopLevelEquals(String s) {
        char quote = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (quote != 0) {
                if (ch == quote && s.charAt(Math.max(0, i - 1)) != '\\') {
                    quote = 0;
                }
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '=') {
                return i;
            }
        }
        return -1;
    }

    // 去掉 # 后面的注释，同样要看引号
    private static String stripComment(String line) {
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quote != 0) {
                if (ch == quote && (i == 0 || line.charAt(i - 1) != '\\')) {
                    quote = 0;
                }
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '#') {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static List<String> splitPath(String key) {
        List<String> out = new ArrayList<>();
        for (String seg : splitTopLevel(key, '.')) {
            String s = seg.trim();
            if (!s.isEmpty()) {
                out.add(unquote(s));
            }
        }
        if (out.isEmpty()) {
            out.add(key.trim());
        }
        return out;
    }

    private static String unquote(String s) {
        String t = s.trim();
        if (t.length() >= 2) {
            char first = t.charAt(0);
            char last = t.charAt(t.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                t = t.substring(1, t.length() - 1);
            }
        }
        if (t.indexOf('\\') >= 0) {
            t = t.replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"")
                 .replace("\\\\", "\\").replace("\\'", "'");
        }
        return t;
    }

    private static String asString(Object o) {
        if (o == null) {
            return "";
        }
        if (o instanceof Map<?, ?> m) {
            return m.toString();
        }
        if (o instanceof List<?> l) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(asString(l.get(i)));
            }
            return sb.toString();
        }
        return String.valueOf(o);
    }
}
