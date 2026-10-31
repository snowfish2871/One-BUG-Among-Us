// 控制台观感：配色、边框、图标、对齐。
// 命令行、报告文本、脚本输出都从这里取样式，改一处全变。
// 这个类只用 JDK 的类，游戏内也能安全加载（游戏那边自己套颜色）。
package dev.oba.core;

public final class Theme {

    // 图标。都挑了等宽字体里有的字符，缺字形也不会串行
    public static final String MARK = "◈";
    public static final String DOT = "●";
    public static final String DOT_OFF = "○";
    public static final String PLAY = "▶";
    public static final String BULLET = "•";
    public static final String ARROW = "→";
    public static final String CROSS = "✗";
    public static final String CHECK = "✓";
    public static final String MORE = "›";
    public static final String SEP = "·";

    // 标题框宽度。按显示宽度算，中文占两格
    public static final int BOX_WIDTH = 64;

    // 缩进：正文两格，子项六格
    public static final int INDENT = 2;
    public static final int SUB_INDENT = 6;

    private final boolean on;

    private Theme(boolean on) {
        this.on = on;
    }

    public static Theme of(boolean color) {
        return new Theme(color);
    }

    // 命令行默认配色。顺序是：命令行明确指定 > 环境变量 > 是不是终端 > NO_COLOR
    public static boolean colorSupported() {
        String forced = System.getProperty("oba.color");
        if (forced == null || forced.isBlank()) {
            forced = System.getenv("OBA_COLOR");
        }
        if (forced != null && !forced.isBlank()) {
            return switch (forced.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "1", "true", "yes", "always", "on" -> true;
                case "0", "false", "no", "never", "off" -> false;
                default -> autoColor();
            };
        }
        return autoColor();
    }

    private static boolean autoColor() {
        // NO_COLOR 是约定俗成的开关，它优先
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null && !noColor.isBlank()) {
            return false;
        }
        // 重定向到文件或管子的时候不该塞控制码进去
        if (System.console() == null) {
            return false;
        }
        String term = System.getenv("TERM");
        if (term != null && term.equalsIgnoreCase("dumb")) {
            return false;
        }
        return true;
    }

    public boolean enabled() {
        return on;
    }

    // 套一段控制码
    public String wrap(String code, String s) {
        if (!on || s == null) {
            return s == null ? "" : s;
        }
        return "\u001B[" + code + "m" + s + "\u001B[0m";
    }

    // 前景色
    public String black(String s) {
        return wrap("30", s);
    }

    public String red(String s) {
        return wrap("31", s);
    }

    public String green(String s) {
        return wrap("32", s);
    }

    public String yellow(String s) {
        return wrap("33", s);
    }

    public String blue(String s) {
        return wrap("34", s);
    }

    public String magenta(String s) {
        return wrap("35", s);
    }

    public String cyan(String s) {
        return wrap("36", s);
    }

    public String white(String s) {
        return wrap("37", s);
    }

    public String gray(String s) {
        return wrap("90", s);
    }

    // 256 色，用来调出更贴近参考图的那种亮青和亮品红
    public String c256(int index, String s) {
        return wrap("38;5;" + index + "m", s);
    }

    // 粗体 / 暗色 / 斜体 / 下划线
    public String bold(String s) {
        return wrap("1", s);
    }

    public String dim(String s) {
        return wrap("2", s);
    }

    public String italic(String s) {
        return wrap("3", s);
    }

    public String underline(String s) {
        return wrap("4", s);
    }

    // 反白，用来画高亮条上的字
    public String inverse(String s) {
        return wrap("7", s);
    }

    // 语义色：名字好记，改配色只动这里
    public String brand(String s) {
        return wrap("1;38;5;51", s);
    }

    public String name(String s) {
        return wrap("1;38;5;231", s);
    }

    public String key(String s) {
        return wrap("38;5;245", s);
    }

    public String value(String s) {
        return wrap("38;5;252", s);
    }

    public String good(String s) {
        return wrap("38;5;42", s);
    }

    public String warn(String s) {
        return wrap("38;5;214", s);
    }

    public String bad(String s) {
        return wrap("1;38;5;203", s);
    }

    public String accent(String s) {
        return wrap("38;5;213", s);
    }

    public String frame(String s) {
        return wrap("38;5;240", s);
    }

    // 结构

    // 双线顶框，右上角挂版本号
    public String headline(String title, String right) {
        String t = " " + MARK + " " + title;
        int inner = BOX_WIDTH;
        StringBuilder sb = new StringBuilder();
        sb.append(frame("╔" + "═".repeat(inner) + "╗")).append('\n');
        int gap = Math.max(1, inner - displayWidth(t) - displayWidth(right) - 2);
        sb.append(frame("║")).append(' ')
          .append(name(t))
          .append(" ".repeat(gap))
          .append(dim(right)).append(' ')
          .append(frame("║")).append('\n');
        sb.append(frame("╚" + "═".repeat(inner) + "╝")).append('\n');
        return sb.toString();
    }

    // 二级小框，用圆角
    public String box(String text) {
        String t = text;
        int inner = Math.max(displayWidth(t) + 2, 24);
        String body = t + " ".repeat(Math.max(0, inner - displayWidth(t) - 1));
        return frame("╭" + "─".repeat(inner) + "╮") + "\n"
                + frame("│") + " " + body + frame("│") + "\n"
                + frame("╰" + "─".repeat(inner) + "╯");
    }

    // 分段标题：左边一个小方块，右边一条横线，和参考图一致
    public String section(String title) {
        String head = brand(BULLET) + " " + bold(title);
        int used = 2 + displayWidth(title);
        return head + " " + frame("─".repeat(Math.max(4, BOX_WIDTH - used)));
    }

    public String rule() {
        return frame("─".repeat(BOX_WIDTH));
    }

    // 左标签右值的一对，标签固定宽度对齐。
    // 前面空出两格，和状态行一样宽，这样带圆点和不带圆点的行标签能对齐
    public String row(String label, String val, int labelWidth) {
        return " ".repeat(INDENT) + "  " + key(padTo(label, labelWidth)) + "  " + value(val);
    }

    // 带圆点的状态行
    public String status(String label, String val, boolean ok, int labelWidth) {
        String dot = ok ? good(DOT) : warn(DOT_OFF);
        return " ".repeat(INDENT) + dot + " " + key(padTo(label, labelWidth)) + "  " + value(val);
    }

    // 高亮条：菜单当前项、重要结论都用它
    public String bar(String text, int width) {
        String body = text;
        int w = displayWidth(body);
        String padded = body + " ".repeat(Math.max(0, width - w));
        return bold(inverse(padded));
    }

    // 项目符号行
    public String item(String text) {
        return " ".repeat(INDENT) + dim(MORE) + " " + text;
    }

    // 超链接

    // OSC 8 超链接。支持的终端里点一下就能打开，不支持的终端会忽略控制码，
    // 只显示文字，所以加了不会把界面弄坏
    public String link(String url, String text) {
        if (!on || url == null || url.isBlank()) {
            return text;
        }
        return "\u001B]8;;" + url + "\u0007" + text + "\u001B]8;;\u0007";
    }

    // 对齐

    // 掐掉颜色码和 OSC 超链接，只留看得见的文字。
    // 游戏内聊天栏不认这些控制码，得先剥干净再往组件里塞
    public static String strip(String s) {
        if (s == null || s.indexOf('\u001B') < 0) {
            return s == null ? "" : s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != '\u001B') {
                sb.append(c);
                i++;
                continue;
            }
            if (i + 1 < s.length() && s.charAt(i + 1) == ']') {
                // OSC 以 BEL 或 ESC\ 收尾
                int end = s.indexOf('\u0007', i);
                int alt = s.indexOf("\u001B\\", i);
                if (end < 0 || (alt >= 0 && alt < end)) {
                    end = alt;
                }
                i = end < 0 ? s.length() : end + 1;
                continue;
            }
            int end = s.indexOf('m', i);
            i = end < 0 ? s.length() : end + 1;
        }
        return sb.toString();
    }

    public static int displayWidth(String s) {
        if (s == null) {
            return 0;
        }
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += charWidth(s.charAt(i));
        }
        return w;
    }

    // 一个字符在终端里占几格。
    // CJK 及其标点算两格，但 U+2500 到 U+259F 要排除——
    // 制表符、方块元素、几何图形都在这个区间，它们实际只占一格，
    // 不排除的话边框右边会对不齐
    public static int charWidth(char c) {
        if (c >= 0x2500 && c <= 0x259F) {
            return 1;
        }
        if (c >= 0x1100 && (c <= 0x115F
                || (c >= 0x2E80 && c <= 0xA4CF)
                || (c >= 0xAC00 && c <= 0xD7A3)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFE30 && c <= 0xFE6F)
                || (c >= 0xFF00 && c <= 0xFF60)
                || (c >= 0xFFE0 && c <= 0xFFE6))) {
            return 2;
        }
        return 1;
    }

    public static String padTo(String s, int width) {
        String v = s == null ? "" : s;
        StringBuilder sb = new StringBuilder(v);
        int w = displayWidth(v);
        while (w < width) {
            sb.append(' ');
            w++;
        }
        return sb.toString();
    }

    public static String padLeft(String s, int width) {
        String v = s == null ? "" : s;
        StringBuilder sb = new StringBuilder();
        int w = displayWidth(v);
        while (w + sb.length() < width) {
            sb.append(' ');
        }
        return sb.append(v).toString();
    }

    // 按显示宽度截断，超出补省略号
    public static String clip(String s, int width) {
        String v = s == null ? "" : s;
        if (displayWidth(v) <= width) {
            return v;
        }
        StringBuilder sb = new StringBuilder();
        int w = 0;
        for (int i = 0; i < v.length(); i++) {
            int cw = charWidth(v.charAt(i));
            if (w + cw > width - 1) {
                break;
            }
            sb.append(v.charAt(i));
            w += cw;
        }
        return sb.append('…').toString();
    }
}
