// 聊天栏里的界面：配色、可点按钮、报告转成游戏能显示的样子。
//
// 这里只碰原版的组件类，没有加载器的东西，所以 Forge 和 NeoForge 共用同一份。
// 报告正文是引擎按终端配色拼出来的字符串，带 ANSI 控制码，
// 直接塞进聊天栏会露出一堆乱码，所以下面那层解析把颜色换成组件样式。
package dev.oba.mod;

import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.core.Theme;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class GameUi {

    // 引用要显示的东西
    public static final String WIKI = "https://github.com/snowfish2871/One-BUG-Among-Us";

    // 配色。跟控制台那套对齐：亮青做品牌色，红黄绿分别对应坏、警告、正常
    public static final int ACCENT = 0x4DF3FF;
    public static final int BRAND = 0x4DF3FF;
    public static final int OK = 0x7CE38B;
    public static final int WARN = 0xFFC24D;
    public static final int BAD = 0xFF6B6B;
    public static final int NAME = 0xFFFFFF;
    public static final int VALUE = 0xE4E4E4;
    public static final int KEY = 0x9AA0A6;
    public static final int FRAME = 0x5A5F66;

    // 一行最多占多少像素。原版聊天栏宽 320 像素，留点余量免得自动折行
    public static final int LINE_PX = 296;

    // 游戏字体不是等宽的：空格 4 像素、小写字母 6、汉字 9。
    // 终端那套「一格一个字」的宽度算到聊天栏里全对不上，
    // 所以下面一律按像素算：客户端在的时候用真字体量（各轨道在客户端侧装进来），
    // 装不上就查这张照原版 ascii.png 量出来的表
    public interface Measurer {
        int width(String text);
    }

    private static volatile Measurer measurer;

    public static void setMeasurer(Measurer m) {
        measurer = m;
    }

    // U+0020 到 U+007E 的逐字宽度，空格按原版 space provider 里的 4
    private static final String ASCII_W =
            "42466662444626266666666666225656766666666466666666666666666464663666665662653666666646666664247";
    // 表外还要用的符号。位图图集里的那些是量出来的；
    // ╭╮╯╰◈ 五个走 unifont，表里的值是实机核对（UiShot 里那段
    // 表 vs 真字体的比对）量出来的真值，改之前先跑一遍那个核对。
    // 这两行必须一样长：查表是按位置对的，短一个后面的全错位
    public static final String EXTRA = "●○▶•→›·↑↓─═║╔╗╚╝█▌│┌┐└┘╭╮╯╰◈";
    private static final String EXTRA_W = "5673842669989898956969633334";

    public static int px(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        Measurer m = measurer;
        if (m != null) {
            try {
                int w = m.width(s);
                if (w > 0) {
                    return w;
                }
            } catch (Throwable ignored) {
                // 客户端还没起来或者已经退出，退回下面那张表
            }
        }
        int total = 0;
        for (int i = 0; i < s.length(); i++) {
            total += pxOf(s.charAt(i));
        }
        return total;
    }

    // 只问真字体，不查表：实机拿它跟表对比，看表有没有量错
    public static int realWidth(String s) {
        Measurer m = measurer;
        if (m == null || s == null || s.isEmpty()) {
            return 0;
        }
        try {
            return m.width(s);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    // 查表宽度：自测和实机核对用
    public static int pxOf(char c) {
        if (c >= ' ' && c < 0x7F) {
            return ASCII_W.charAt(c - ' ') - '0';
        }
        int i = EXTRA.indexOf(c);
        if (i >= 0 && i < EXTRA_W.length()) {
            return EXTRA_W.charAt(i) - '0';
        }
        // 汉字和别的整宽字形都是 9 像素
        return 9;
    }

    // 用空格把内容补到指定像素宽。空格正好 4 像素，除不尽就往下取，宁可窄一点也不超出去
    public static String padPx(String s, int targetPx) {
        int need = targetPx - px(s);
        return need < 4 ? s : s + " ".repeat(need / 4);
    }

    // 加粗的宽度：原版每个字形往右多描 1 像素，量宽度时加上去，
    // 不然加粗的那段会把整行顶出聊天栏、被原版折成两行
    public static int pxBold(String s) {
        return s == null ? 0 : px(s) + s.length();
    }

    // 横线按像素铺满，免得长标题把线挤没。除不尽往下取，不超目标宽度
    public static String fillPx(String glyph, int targetPx) {
        return glyph.repeat(Math.max(1, targetPx / Math.max(1, px(glyph))));
    }

    private GameUi() {
    }

    // 组装

    // 带颜色的短消息
    public static MutableComponent t(String text, int color) {
        MutableComponent c = Component.empty();
        append(c, text, color, false, false);
        return c;
    }

    public static MutableComponent ok(String text) {
        return t(text, OK);
    }

    public static MutableComponent warn(String text) {
        return t(text, WARN);
    }

    public static MutableComponent bad(String text) {
        return t(text, BAD);
    }

    public static MutableComponent key(String text) {
        return t(text, KEY);
    }

    public static MutableComponent value(String text) {
        return t(text, VALUE);
    }

    public static MutableComponent accent(String text) {
        return t(text, ACCENT);
    }

    // 双线顶框，右上角挂版本号，和命令行的标题条同一套写法。
    // 三行必须一样宽，不然右边框会歪。所以先按内容定下总宽，
    // 再回头挑顶框画几格：═ 一格 9 像素、空格 4 像素，
    // 往后多试几格总能凑到分毫不差
    public static List<Component> header(String title) {
        String right = Oba.ABBR + " v" + Oba.VERSION;
        String left = MARK_TEXT + " " + title;
        int mid = px("║ ") + px(left) + px(right) + px(" ║");
        int bars = 8;
        int total = px("╔") + px("═") * bars + px("╗");
        while (total < mid + 4 || (total - mid) % 4 != 0) {
            bars++;
            total = px("╔") + px("═") * bars + px("╗");
            if (bars > 200) {
                break;
            }
        }
        int gap = Math.max(1, (total - mid) / 4);
        List<Component> out = new ArrayList<>();
        out.add(t("╔" + "═".repeat(bars) + "╗", FRAME));
        out.add(Component.empty()
                .append(t("║ ", FRAME))
                .append(t(left, BRAND))
                .append(t(" ".repeat(gap), VALUE))
                .append(t(right, KEY))
                .append(t(" ║", FRAME)));
        out.add(t("╚" + "═".repeat(bars) + "╝", FRAME));
        return out;
    }

    private static final String MARK_TEXT = "◈";

    // 分段标题：左边一个小方块，右边一条横线
    public static Component section(String title) {
        // 标题是加粗的，量宽度得按加粗算，否则整行会超出去被原版折行
        int head = px("• ") + pxBold(title) + px(" ");
        return Component.empty()
                .append(t("• ", BRAND))
                .append(t(title, NAME).withStyle(ChatFormatting.BOLD))
                .append(t(" " + fillPx("─", LINE_PX - head), FRAME));
    }

    public static Component rule() {
        return t(fillPx("─", LINE_PX), FRAME);
    }

    // 左标签右值。labelWidth 还是按终端格数给的，一格在游戏里大约 6 像素
    public static Component row(String label, String val, int labelWidth) {
        return row(label, val, labelWidth, true);
    }

    public static Component row(String label, String val, int labelWidth, boolean on) {
        return Component.empty()
                .append(t("  ", KEY))
                .append(t(on ? "●" : "○", on ? OK : WARN))
                .append(t(" " + padPx(label, labelWidth * 6) + "  ", KEY))
                .append(value(val == null ? "" : val));
    }

    // 不带圆点的一对
    public static Component pair(String label, String val, int labelWidth) {
        return Component.empty()
                .append(t("  " + padPx(label, labelWidth * 6) + "  ", KEY))
                .append(value(val == null ? "" : val));
    }

    // 整行反白用来标记当前项，但游戏里反白写不出来，
    // 换成亮色描边加粗字，看起来一样醒目
    public static Component bar(String text) {
        int pad = Math.max(4, LINE_PX - px("█ ") - pxBold(text) - px(" █"));
        return Component.empty()
                .append(t("█ ", BAD))
                .append(t(text, NAME).withStyle(ChatFormatting.BOLD))
                // 空格 4 像素一个，除不尽就往下取，宁可窄一点也不超出去
                .append(t(" ".repeat(Math.max(1, pad / 4)), VALUE))
                .append(t(" █", BAD));
    }

    // 可点的菜单项。点整行都算，鼠标划过有提示。
    // 亮色那项是「就按这个走」的默认项，左边挂个箭头
    public static Component action(int index, String label, String hint, String command,
                                   boolean highlight) {
        String mark = highlight ? "▶ " : "  ";
        MutableComponent row = Component.empty()
                .append(t("  ", FRAME))
                .append(t(mark, BAD))
                .append(t("[" + index + "] ", BRAND))
                .append(highlight ? t(label, NAME).withStyle(ChatFormatting.BOLD) : t(label, VALUE));
        if (!hint.isBlank()) {
            String head = "  " + mark + "[" + index + "] " + label;
            // 高亮那项的标签是加粗的，宽度得按加粗算；空格 4 像素一个，除不尽往下取
            int pad = Math.max(4, LINE_PX - px(head) - (highlight ? label.length() : 0) - px(hint));
            row.append(t(" ".repeat(Math.max(1, pad / 4)), VALUE)).append(t(hint, KEY));
        }
        return click(row, command, Lang.get("gameui.click", "/" + command));
    }

    // 提示行。给的是能直接点的链接或命令，没法点的地方就老老实实报路径
    public static Component hint(Component content, String command) {
        return command == null || command.isBlank()
                ? content
                : click(content, command, Lang.get("gameui.click", "/" + command));
    }

    // 单独把点击事件摘出来，是为了自测：鼠标提示 HoverEvent 一构造就去碰原版注册表，
    // 离线自测里初始化不起来（NeoForge 那边 LoadingModList.get() 是 null），
    // 光造 ClickEvent 不需要注册表，能照样验「点一下跑的是哪条命令」。
    public static ClickEvent clickEvent(ClickEvent.Action action, String value) {
        // 1.20.1 ~ 1.21.1：ClickEvent 是普通类，一个构造器就够
        try {
            return (ClickEvent) ClickEvent.class
                    .getConstructor(ClickEvent.Action.class, String.class)
                    .newInstance(action, value);
        } catch (NoSuchMethodException newer) {
            // 26.x 起 ClickEvent 变成接口，每个动作一个 record，只能按动作挑子类
            return clickEventRecord(action, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("造不出点击事件: " + action, e);
        }
    }

    // 接口版的 ClickEvent：RunCommand / SuggestCommand / OpenUrl / CopyToClipboard / ChangePage
    // 各是一个 record。这些子类只在 26.x 之后才有，所以按名字反射，编译期不认它们。
    private static ClickEvent clickEventRecord(ClickEvent.Action action, String value) {
        String type;
        Object arg;
        switch (action) {
            case RUN_COMMAND -> {
                type = "RunCommand";
                arg = value;
            }
            case SUGGEST_COMMAND -> {
                type = "SuggestCommand";
                arg = value;
            }
            case OPEN_URL -> {
                type = "OpenUrl";
                arg = java.net.URI.create(value);
            }
            case COPY_TO_CLIPBOARD -> {
                type = "CopyToClipboard";
                arg = value;
            }
            case CHANGE_PAGE -> {
                type = "ChangePage";
                arg = Integer.parseInt(value.trim());
            }
            default -> {
                return null;
            }
        }
        try {
            Class<?> record = Class.forName("net.minecraft.network.chat.ClickEvent$" + type);
            Class<?> parameter = arg instanceof Integer ? int.class : arg.getClass();
            return (ClickEvent) record.getConstructor(parameter).newInstance(arg);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("造不出点击事件: " + action, e);
        }
    }

    // 鼠标提示同理：1.21.1 是 new HoverEvent(SHOW_TEXT, text)，26.x 是 HoverEvent.ShowText
    private static HoverEvent hoverText(Component text) {
        try {
            return (HoverEvent) HoverEvent.class
                    .getConstructor(HoverEvent.Action.class, Component.class)
                    .newInstance(HoverEvent.Action.SHOW_TEXT, text);
        } catch (NoSuchMethodException newer) {
            try {
                return (HoverEvent) Class.forName("net.minecraft.network.chat.HoverEvent$ShowText")
                        .getConstructor(Component.class).newInstance(text);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("造不出鼠标提示", e);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("造不出鼠标提示", e);
        }
    }

    // 点击/提示要挂到整棵组件树上，不能只挂根节点：
    // 行里每一小段（缩进、▶、[2]、正文、右侧提示）都是独立的兄弟节点，
    // 各自带着颜色样式，光给根节点挂事件的话点正文那一段不一定算数。
    private static void styleTree(MutableComponent node, UnaryOperator<Style> change) {
        node.withStyle(change);
        for (Component sibling : node.getSiblings()) {
            if (sibling instanceof MutableComponent mutable) {
                styleTree(mutable, change);
            }
        }
    }

    // 把一段内容变成可点的：样式铺满整棵树。自测直接调它，
    // 传一个只带 ClickEvent 的样式就能验「整行都算数」，不必碰注册表。
    public static MutableComponent clickable(Component content, Style style) {
        MutableComponent copy = content.copy();
        styleTree(copy, existing -> existing
                .withClickEvent(style.getClickEvent())
                .withHoverEvent(style.getHoverEvent()));
        return copy;
    }

    public static MutableComponent click(Component content, String command, String hover) {
        return clickable(content, Style.EMPTY
                .withClickEvent(clickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(hoverText(Component.literal(hover))));
    }

    public static MutableComponent url(Component content, String link) {
        return clickable(content, Style.EMPTY
                .withClickEvent(clickEvent(ClickEvent.Action.OPEN_URL, link))
                .withHoverEvent(hoverText(Component.literal(link))));
    }

    public static MutableComponent suggest(Component content, String command) {
        return clickable(content, Style.EMPTY
                .withClickEvent(clickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(hoverText(Component.literal(Lang.get("gameui.insert", command)))));
    }

    // 底部按键提示
    public static Component keys() {
        return Component.empty()
                .append(t("  ↑ ↓ ", KEY))
                .append(t(Lang.get("gameui.keys.move"), VALUE))
                .append(t("   ENTER ", KEY))
                .append(t(Lang.get("gameui.keys.pick"), VALUE))
                .append(t("   Q ", KEY))
                .append(t(Lang.get("gameui.keys.quit"), VALUE));
    }

    // 报告正文

    // 把引擎拼好的报告转成聊天栏能显示的组件。
    // 引擎那边是照终端格子拼的（`Theme.BOX_WIDTH` 列），一行多少像素跟聊天栏没关系：
    // 超宽的行会被原版自动折行，折出来的残段看上去就是一串乱码 ——
    // 比如「── 致命 ─────…」这种分隔线，在聊天栏里会散成好几行横杠。
    // 所以这里按聊天栏宽度重排：整行几乎只有横线的收成一条满宽分隔线，
    // 其余按像素折行，样式跟着每一段单独带上
    public static List<Component> lines(String text) {
        List<Component> out = new ArrayList<>();
        for (String raw : text.split("\n", -1)) {
            String visible = stripAnsi(raw).stripTrailing();
            if (visible.isBlank()) {
                continue;
            }
            if (isRule(visible)) {
                out.add(line(ruleLine(raw)));
                continue;
            }
            out.addAll(wrapRuns(raw, LINE_PX));
        }
        return out;
    }

    // 终端那种分隔线用到的横线字符
    private static final String RULE_GLYPHS = "─═━┄┅╌╍▬—–-_";

    // 一整行一半以上都是横线：终端里那种分隔线
    private static boolean isRule(String visible) {
        String s = visible.strip();
        if (s.length() < 8) {
            return false;
        }
        int rule = 0;
        for (int i = 0; i < s.length(); i++) {
            if (RULE_GLYPHS.indexOf(s.charAt(i)) >= 0) {
                rule++;
            }
        }
        return rule * 2 >= s.length();
    }

    // 分隔线重排：把线上的文字（「致命」/「• 问题清单」）挑出来，右边用 ─ 铺满聊天栏。
    // 终端那种分隔线是照 100 列拼的，聊天栏只有 320 像素，原样送进去会被折成一堆横杠 ——
    // 折出来的残段看上去就像乱码，所以这里整行重建
    private static String ruleLine(String raw) {
        String s = stripAnsi(raw).stripTrailing().strip();
        if (s.isEmpty()) {
            return "";
        }
        // 以横线开头的（`── 致命 ──…`）要保留开头的记号，以文字开头的（`• 结论 ──…`）不用加
        boolean leadingRule = RULE_GLYPHS.indexOf(s.charAt(0)) >= 0;
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (RULE_GLYPHS.indexOf(c) < 0) {
                label.append(c);
            }
        }
        String text = label.toString().strip();
        String head = text.isEmpty() ? "" : (leadingRule ? "── " : "") + text + " ";
        int fill = Math.max(4, (LINE_PX - px(head)) / Math.max(1, px("─")));
        return stylePrefix(raw, ruleAt(raw)) + head + "─".repeat(fill);
    }

    // raw 里第一个横线字符的位置（跳过控制码），用来取分隔线本身的颜色
    private static int ruleAt(String raw) {
        boolean escape = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escape) {
                if (c == 'm') {
                    escape = false;
                }
                continue;
            }
            if (c == '\u001B') {
                escape = true;
                continue;
            }
            if (RULE_GLYPHS.indexOf(c) >= 0) {
                return i;
            }
        }
        return raw.length();
    }

    // 把 raw[0,end) 里的控制码按语义重放一遍，好让折出来的每一行自己带上样式。
    // 0 清空、1 加粗、2 变暗、22 取消加粗和变暗，其余当颜色码
    private static String stylePrefix(String raw, int end) {
        boolean bold = false;
        boolean dim = false;
        String color = null;
        int i = 0;
        while (i < end && i < raw.length()) {
            if (raw.charAt(i) != '\u001B') {
                i++;
                continue;
            }
            int m = raw.indexOf('m', i);
            if (m < 0 || m >= end) {
                break;
            }
            String code = raw.substring(i + 2, m);
            switch (code) {
                case "0" -> {
                    bold = false;
                    dim = false;
                    color = null;
                }
                case "1" -> bold = true;
                case "2" -> dim = true;
                case "22" -> {
                    bold = false;
                    dim = false;
                }
                case "7", "27" -> {
                    // 反白在聊天栏里没法表达，忽略
                }
                default -> color = code;
            }
            i = m + 1;
        }
        StringBuilder sb = new StringBuilder("\u001B[0m");
        if (bold) {
            sb.append("\u001B[1m");
        }
        if (dim) {
            sb.append("\u001B[2m");
        }
        if (color != null) {
            sb.append('\u001B').append('[').append(color).append('m');
        }
        return sb.toString();
    }

    // 加粗的字每个多占 1 像素（原版是把字形往右再描一遍），量宽度得算上
    private static int charPx(char c, boolean bold) {
        return pxOf(c) + (bold ? 1 : 0);
    }

    // 按聊天栏宽度折行，颜色跟着每一段走。
    // 折的时候优先在空格处断（英文堆栈、路径断在词中间很难读），找不到空格才按像素硬断
    private static List<Component> wrapRuns(String raw, int widthPx) {
        List<Component> out = new ArrayList<>();
        String indent = leadingSpaces(stripAnsi(raw));
        StringBuilder buf = new StringBuilder();
        boolean bold = false;
        int w = 0;
        int i = 0;
        int lastSpace = -1;
        int lastSpaceWidth = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '\u001B') {
                int m = raw.indexOf('m', i);
                if (m < 0) {
                    break;
                }
                String code = raw.substring(i + 2, m);
                if (code.equals("1")) {
                    bold = true;
                } else if (code.equals("0") || code.equals("22")) {
                    bold = false;
                }
                buf.append(raw, i, m + 1);
                i = m + 1;
                continue;
            }
            int cw = charPx(c, bold);
            if (w + cw > widthPx && w > 0) {
                // 空格在行宽四分之三往后才算数：太靠前就只剩一点内容，不如硬断
                int cut = lastSpace > 0 && lastSpaceWidth >= widthPx * 3 / 4 ? lastSpace : -1;
                String tail = cut > 0 ? buf.substring(cut + 1) : "";
                if (cut > 0) {
                    buf.setLength(cut);
                }
                out.add(line(buf.toString()));
                buf.setLength(0);
                buf.append(stylePrefix(raw, i)).append(indent).append(tail);
                w = px(indent) + px(tail);
                lastSpace = -1;
            }
            if (c == ' ') {
                lastSpace = buf.length();
                lastSpaceWidth = w + cw;
            }
            buf.append(c);
            w += cw;
            i++;
        }
        if (!stripAnsi(buf.toString()).isBlank()) {
            out.add(line(buf.toString()));
        }
        return out;
    }

    private static String leadingSpaces(String s) {
        int n = 0;
        while (n < s.length() && n < 6 && s.charAt(n) == ' ') {
            n++;
        }
        return " ".repeat(n);
    }

    // 整行实际占多少像素：按样式遍历整棵树，加粗的按加粗算。
    // 自测拿它断言「送进聊天栏的每一行都不会被原版折行」
    public static int visualWidth(Component c) {
        int[] total = new int[1];
        c.visit((style, text) -> {
            if (!text.isEmpty()) {
                total[0] += px(text) + (style.isBold() ? text.length() : 0);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return total[0];
    }


    // 一行文本：把 ANSI 控制码解成组件样式
    private static Component line(String s) {
        MutableComponent root = Component.empty();
        int color = 0;
        boolean bold = false;
        boolean dim = false;
        boolean inverse = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\u001B') {
                int end = s.indexOf('m', i);
                if (end < 0) {
                    break;
                }
                String code = s.substring(i + 2, end);
                switch (code) {
                    case "0" -> {
                        color = 0;
                        bold = false;
                        dim = false;
                        inverse = false;
                    }
                    case "1" -> bold = true;
                    case "2" -> dim = true;
                    case "7" -> inverse = true;
                    case "22" -> {
                        bold = false;
                        dim = false;
                    }
                    case "27" -> inverse = false;
                    default -> {
                        int mapped = mapAnsi(code);
                        if (mapped != 0) {
                            color = mapped;
                        }
                    }
                }
                i = end + 1;
                continue;
            }
            int run = i;
            while (run < s.length() && s.charAt(run) != '\u001B') {
                run++;
            }
            append(root, s.substring(i, run), color, bold, dim || inverse);
            i = run;
        }
        return root;
    }

    // 往组件上贴一段字，带样式
    private static void append(MutableComponent root, String text, int color, boolean bold, boolean dim) {
        if (text.isEmpty()) {
            return;
        }
        // 控制码之间的空串不带样式，留着只会多出空组件
        MutableComponent part = Component.literal(text);
        Style style = Style.EMPTY;
        if (bold) {
            style = style.withBold(true);
        }
        if (dim) {
            style = style.withColor(ChatFormatting.DARK_GRAY);
        } else if (color != 0) {
            style = style.withColor(color);
        }
        root.append(part.withStyle(style));
    }

    // 256 色和 16 色都收敛到几个语义色，够区分就行
    private static int mapAnsi(String code) {
        if (code.startsWith("38;5;")) {
            return map256(parseInt(code.substring(5), 245));
        }
        return switch (code) {
            case "30", "90" -> KEY;
            case "31" -> BAD;
            case "32" -> OK;
            case "33" -> WARN;
            case "34", "36" -> ACCENT;
            case "35" -> ACCENT;
            case "37" -> VALUE;
            default -> 0;
        };
    }

    private static int map256(int index) {
        return switch (index) {
            case 51 -> ACCENT;
            case 231 -> NAME;
            case 252 -> VALUE;
            case 245, 240 -> KEY;
            case 42 -> OK;
            case 214 -> WARN;
            case 203 -> BAD;
            case 213 -> ACCENT;
            default -> 0;
        };
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // 掐掉控制码和 OSC 超链接，只留可见文字
    public static String stripAnsi(String s) {
        return Theme.strip(s);
    }

    // 按像素折行，中英混排也不会挤爆聊天栏。widthPx 里要自己扣掉行首缩进
    public static List<String> wrap(String text, int widthPx) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.add(line.toString());
                line.setLength(0);
                w = 0;
                continue;
            }
            int cw = pxOf(c);
            if (w + cw > widthPx && line.length() > 0) {
                out.add(line.toString());
                line.setLength(0);
                w = 0;
            }
            line.append(c);
            w += cw;
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        return out;
    }

    // 路径在聊天栏里点一下就能塞进输入框，省得手抄
    public static Component path(String p) {
        return suggest(t(p, VALUE), "/oba analyze \"" + p + "\"");
    }

    // 数字、百分比这些统一走这里，省得各处格式不一致
    public static String num(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }
}
