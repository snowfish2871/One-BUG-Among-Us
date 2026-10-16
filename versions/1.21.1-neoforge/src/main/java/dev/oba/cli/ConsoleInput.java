// 键盘输入。终端里能挂上 JLine 就用它的按键读取，挂不上就退回逐字节读原生输入。
package dev.oba.cli;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

public final class ConsoleInput {

    // 一次按键。菜单只关心方向、回车、数字和退出
    public record Key(Kind kind, char ch) {
        public enum Kind { UP, DOWN, ENTER, ESC, NUMBER, CHAR, EOF }
    }

    private final Object reader;
    private final InputStream raw;
    private final boolean interactive;

    private ConsoleInput(Object reader, InputStream raw, boolean interactive) {
        this.reader = reader;
        this.raw = raw;
        this.interactive = interactive;
    }

    // 终端可用时用 JLine，否则退回原生输入
    public static ConsoleInput open() {
        InputStream in = System.in;
        if (!in.markSupported()) {
            in = new java.io.BufferedInputStream(in);
        }
        Object reader = tryJline();
        return new ConsoleInput(reader, in, reader != null || System.console() != null);
    }

    public boolean interactive() {
        return interactive;
    }

    // JLine 是可选的：游戏里没有它，命令行有就用上。反射调用避免硬依赖
    private static Object tryJline() {
        try {
            Class<?> builder = Class.forName("org.jline.terminal.TerminalBuilder");
            Object termBuilder = builder.getMethod("builder").invoke(null);
            termBuilder.getClass().getMethod("system", boolean.class).invoke(termBuilder, true);
            termBuilder.getClass().getMethod("dumb", boolean.class).invoke(termBuilder, true);
            Object terminal = termBuilder.getClass().getMethod("build").invoke(termBuilder);
            Class<?> lrBuilder = Class.forName("org.jline.reader.LineReaderBuilder");
            Object lrb = lrBuilder.getMethod("builder").invoke(null);
            lrb.getClass().getMethod("terminal", Class.forName("org.jline.terminal.Terminal"))
                    .invoke(lrb, terminal);
            Object reader = lrb.getClass().getMethod("build").invoke(lrb);
            // 能构建出来不代表能读，先探一下
            if (reader != null) {
                return reader;
            }
        } catch (Throwable ignored) {
            // 没有 JLine 或者终端不认，走原生
        }
        return null;
    }

    // 读一个按键。JLine 走 readKey，原生走字节
    public Key read() throws IOException {
        if (reader != null) {
            Key k = readJline();
            if (k != null) {
                return k;
            }
            // JLine 挂了，退回原生读一次
            raw.mark(1);
            return readRaw();
        }
        return readRaw();
    }

    private Key readJline() {
        try {
            Method readKey = reader.getClass().getMethod("readKey");
            readKey.setAccessible(true);
            Object binding = readKey.invoke(reader);
            Object key = binding.getClass().getMethod("key").invoke(binding);
            String name = String.valueOf(key);
            return switch (name) {
                case "Up" -> new Key(Key.Kind.UP, '\0');
                case "Down", "Tab" -> new Key(Key.Kind.DOWN, '\0');
                case "Enter" -> new Key(Key.Kind.ENTER, '\n');
                case "Escape" -> new Key(Key.Kind.ESC, '\u001B');
                case "q", "Q", "CtrlC" -> new Key(Key.Kind.CHAR, 'q');
                default -> {
                    Object ch = binding.getClass().getMethod("getChar").invoke(binding);
                    if (ch instanceof Character c && c != 0) {
                        if (c >= '1' && c <= '9') {
                            yield new Key(Key.Kind.NUMBER, c);
                        }
                        yield new Key(Key.Kind.CHAR, c);
                    }
                    yield null;
                }
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    // 原生读。方向键是 ESC [ A/B 三个字节，别的就当普通字符
    private Key readRaw() throws IOException {
        int c = raw.read();
        if (c < 0) {
            return new Key(Key.Kind.EOF, '\0');
        }
        char ch = (char) c;
        switch (ch) {
            case '\n', '\r':
                return new Key(Key.Kind.ENTER, '\n');
            case 'q', 'Q', 3:
                return new Key(Key.Kind.CHAR, 'q');
            case '\u001B':
                raw.mark(3);
                int b1 = raw.read();
                int b2 = raw.read();
                if (b1 == '[' && (b2 == 'A' || b2 == 'B')) {
                    return new Key(b2 == 'A' ? Key.Kind.UP : Key.Kind.DOWN, '\0');
                }
                if (b1 < 0) {
                    return new Key(Key.Kind.ESC, '\u001B');
                }
                raw.reset();
                return new Key(Key.Kind.ESC, '\u001B');
            default:
                if (ch >= '1' && ch <= '9') {
                    return new Key(Key.Kind.NUMBER, ch);
                }
                return new Key(Key.Kind.CHAR, ch);
        }
    }

    // 命令行直接跳某一项：--menu 后面带数字时不用交互
    public static int pick(List<Menu.Item> items, String want) {
        if (want == null || want.isBlank()) {
            return -1;
        }
        String w = want.trim().toLowerCase(java.util.Locale.ROOT);
        int n = -1;
        try {
            n = Integer.parseInt(w);
        } catch (NumberFormatException ignored) {
            // 不是数字就按名字找
        }
        if (n >= 1 && n <= items.size()) {
            return n - 1;
        }
        for (int i = 0; i < items.size(); i++) {
            Menu.Item it = items.get(i);
            if (it.id().equalsIgnoreCase(w) || it.target().equalsIgnoreCase(w)) {
                return i;
            }
        }
        return -1;
    }
}
