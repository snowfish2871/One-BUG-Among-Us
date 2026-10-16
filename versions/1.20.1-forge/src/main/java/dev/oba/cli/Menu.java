// 交互式主菜单：纯文本渲染加按键处理，命令行和游戏内共用同一份内容。
package dev.oba.cli;

import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.core.Theme;

import java.util.ArrayList;
import java.util.List;

public final class Menu {

    // 一个可执行项。id 是内部标识，target 是命令行上等价的那一串参数
    public record Item(String id, String target, String label) {
    }

    // 内置的三项：跑命令、看报告文件、切语言
    public static final String EXIT = "__exit";
    public static final String REPORT = "__report";
    public static final String LANG = "__lang";

    private final Theme a;
    private int width = Theme.BOX_WIDTH;

    // 状态区左侧标签的对齐宽度
    private static final int LABEL = 10;

    public Menu(boolean color) {
        this.a = Theme.of(color);
    }

    public Theme theme() {
        return a;
    }

    // 按终端宽度收窄，太窄就守住下限
    public Menu width(int columns) {
        width = Math.max(46, Math.min(Theme.BOX_WIDTH, columns - 1));
        return this;
    }

    public int width() {
        return width;
    }

    // 菜单项。命令行的键依次是 1..n，和游戏里显示的数字一致
    public List<Item> items() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("analyze", "analyze", Lang.get("menu.item.analyze")));
        items.add(new Item("mods", "mods", Lang.get("menu.item.mods")));
        items.add(new Item("mcreator", "mcreator", Lang.get("menu.item.mcreator")));
        items.add(new Item("watch", "watch", Lang.get("menu.item.watch")));
        items.add(new Item("threads", "threads", Lang.get("menu.item.threads")));
        items.add(new Item("status", "status", Lang.get("menu.item.status")));
        items.add(new Item(LANG, LANG, Lang.get("menu.item.lang")));
        items.add(new Item(REPORT, REPORT, Lang.get("menu.item.report")));
        items.add(new Item(EXIT, EXIT, Lang.get("menu.item.exit")));
        return items;
    }

    // 终端里点一下菜单项就能跑：给每一项一个可以点的地址。游戏里用不着，传 null
    public interface Linker {
        String href(Item item);
    }

    // 排好版的一行：编号、名称、名称之后的补白、右侧动作提示、行尾补白
    private record Row(String num, String label, String gap, String action, String tail) {

        String text() {
            return num + "  " + label + gap + action + tail;
        }
    }

    // 先按最宽的那一项定下列宽，正文才能左右对齐
    private List<Row> layout(List<Item> items) {
        int fixed = 0;
        for (int i = 0; i < items.size(); i++) {
            fixed = Math.max(fixed, Theme.displayWidth("[" + (i + 1) + "]") + 2
                    + Theme.displayWidth(actionHint(items.get(i).id())));
        }
        List<Row> rows = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            String num = "[" + (i + 1) + "]";
            String action = actionHint(it.id());
            String label = it.label();
            int room = width - 9 - fixed;
            if (Theme.displayWidth(label) > room) {
                label = Theme.clip(label, Math.max(4, room));
            }
            int gap = Math.max(1, width - 8 - fixed - Theme.displayWidth(label));
            // 行尾补到满宽。不补的话每行框宽跟着文字长短走，长短不一会很难看
            int used = Theme.displayWidth(num) + 2 + Theme.displayWidth(label) + gap
                    + Theme.displayWidth(action);
            rows.add(new Row(num, label, " ".repeat(gap), action,
                    " ".repeat(Math.max(0, width - 8 - used))));
        }
        return rows;
    }

    // 画整屏。selected 是当前高亮的那一项，link 为 null 时就是纯文本
    public String render(String gameDir, String state, int selected, List<Item> items) {
        return render(gameDir, state, selected, items, null);
    }

    public String render(String gameDir, String state, int selected, List<Item> items, Linker link) {
        List<Row> rows = layout(items);
        StringBuilder sb = new StringBuilder(2048);
        sb.append(headline()).append('\n');
        sb.append('\n');
        sb.append(a.row(Lang.get("menu.label.workspace"), clip(gameDir), LABEL)).append('\n');
        sb.append(a.row(Lang.get("menu.label.status"), a.good(Theme.DOT + " " + state), LABEL)).append('\n');
        sb.append(a.row(Lang.get("menu.label.version"), Oba.NAME + " " + Oba.VERSION, LABEL)).append('\n');
        sb.append('\n');
        sb.append("  ").append(a.section(Lang.get("menu.section"))).append('\n');
        sb.append('\n');
        for (int i = 0; i < items.size(); i++) {
            sb.append(renderItem(items.get(i), rows.get(i), i == selected, link)).append('\n');
        }
        sb.append('\n');
        sb.append("  ").append(a.dim(Lang.get("menu.footer", Lang.get("menu.key.exit")))).append('\n');
        return sb.toString();
    }

    // 选中项整条反白，未选中项套一层圆角框，这一层就是参考图里的观感
    private String renderItem(Item it, Row row, boolean selected, Linker link) {
        String href = link == null ? null : link.href(it);
        if (selected) {
            // 反白条左边多一个 ▶ 记号，正文得再窄两格，宽度也要和下面的框对齐
            String text = " " + Theme.PLAY + "  " + row.text();
            int inner = width - 4;
            int pad = inner - Theme.displayWidth(text);
            return "  " + a.bar(Theme.clip(text + " ".repeat(Math.max(0, pad)), inner), inner);
        }
        String body = a.accent(row.num()) + "  " + cell(row.label(), href) + row.gap()
                + a.key(row.action());
        String rule = "─".repeat(Math.max(0, Theme.displayWidth(row.text()) + 2));
        return "  " + a.frame("╭" + rule + "╮") + '\n'
                + "  " + a.frame("│") + " " + body + " " + a.frame("│") + '\n'
                + "  " + a.frame("╰" + rule + "╯");
    }

    // 有地址就包一层可点，没有就原样输出
    private String cell(String text, String href) {
        return href == null ? text : a.link(href, text);
    }

    // 每项右边那句提示，说明按下去会发生什么
    private static String actionHint(String id) {
        return switch (id) {
            case EXIT -> Lang.get("menu.hint.exit");
            case REPORT -> Lang.get("menu.hint.report");
            case LANG -> Lang.get("menu.hint.lang");
            case "watch" -> Lang.get("menu.hint.watch");
            case "threads" -> Lang.get("menu.hint.threads");
            default -> Lang.get("menu.hint.run");
        };
    }

    // 标题框。宽度跟着当前宽度走，右上角挂版本号
    private String headline() {
        String title = " " + Theme.MARK + " " + Oba.NAME + " CLI";
        String right = Oba.ABBR + " v" + Oba.VERSION;
        int inner = width;
        int gap = Math.max(1, inner - Theme.displayWidth(title) - Theme.displayWidth(right) - 2);
        return a.frame("╔" + "═".repeat(inner) + "╗") + '\n'
                + a.frame("║") + " " + a.name(title) + " ".repeat(gap)
                + a.dim(right) + " " + a.frame("║") + '\n'
                + a.frame("╚" + "═".repeat(inner) + "╝");
    }

    // 提示行。等待按键时显示
    public String prompt(String text) {
        return "  " + a.brand(Theme.PLAY + " ") + a.value(text);
    }

    // 按一个字符找对应的菜单项，找不到返回 -1
    public static int indexOfKey(List<Item> items, char c) {
        int n = c - '0';
        if (n >= 1 && n <= items.size()) {
            return n - 1;
        }
        return -1;
    }

    private String clip(String s) {
        return Theme.clip(s == null ? "" : s, width - LABEL - 4);
    }
}
