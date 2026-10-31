// 交互式菜单的驱动：画屏、读键、跑选中的那一项，再回到菜单。
package dev.oba.cli;

import dev.oba.core.Analyzer;
import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.core.Theme;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MenuRunner {

    // 跑一个子命令，返回退出码。命令行就是这么执行菜单项的
    public interface Runner {
        int run(String subcommand);
    }

    private final PrintStream out;
    private final PrintStream err;
    private final Menu menu;
    private final ConsoleInput in;
    private final boolean color;
    private final Path gameDir;
    private Path selfJar;
    private boolean clickable = true;
    private int selected;

    public MenuRunner(PrintStream out, PrintStream err, boolean color, Path gameDir) {
        this.out = out;
        this.err = err;
        this.color = color;
        this.gameDir = gameDir;
        this.menu = new Menu(color);
        this.menu.width(themeColumns());
        this.in = ConsoleInput.open();
    }

    // 命令行上跑的 jar 路径，用来生成可点的启动脚本
    public MenuRunner selfJar(Path jar) {
        this.selfJar = jar;
        return this;
    }

    // 关掉之后菜单项就是纯文字，不生成任何脚本
    public MenuRunner clickable(boolean on) {
        this.clickable = on;
        return this;
    }

    public Menu menu() {
        return menu;
    }

    public ConsoleInput input() {
        return in;
    }

    public int loop(Runner runner) throws IOException {
        List<Menu.Item> items = menu.items();
        CliLauncher launcher = clickable ? CliLauncher.create(selfJar, gameDir) : null;
        Menu.Linker link = launcher == null ? null : it -> launcher.href(it.id(), it.target());
        String state = Lang.get("menu.state.ready");
        if (launcher != null) {
            out.println();
            out.println("  " + menu.theme().dim(Lang.get("menu.clickable")));
        }
        while (true) {
            List<String> view = new ArrayList<>(List.of(
                    menu.render(gameDir == null ? "-" : gameDir.toString(), state, selected, items, link)
                            .split("\n", -1)));
            view.add(menu.prompt(Lang.get("menu.footer", Lang.get("menu.key.exit"))));
            int height = view.size();
            for (String line : view) {
                out.println(line);
            }
            out.flush();

            ConsoleInput.Key key = in.read();
            clear(height);
            state = Lang.get("menu.state.ready");

            switch (key.kind()) {
                case EOF -> {
                    out.println(Lang.get("menu.exited"));
                    return 0;
                }
                case UP -> selected = (selected + items.size() - 1) % items.size();
                case DOWN -> selected = (selected + 1) % items.size();
                case ENTER -> {
                    Menu.Item it = items.get(selected);
                    if (it.id().equals(Menu.EXIT)) {
                        out.println(Lang.get("menu.exited"));
                        return 0;
                    }
                    state = Lang.get("menu.state.working");
                    int code = dispatch(it, runner);
                    state = Lang.get("menu.state.done");
                    if (code == EXIT_MENU) {
                        out.println(Lang.get("menu.exited"));
                        return 0;
                    }
                    waitKey();
                }
                case NUMBER, CHAR -> {
                    char c = key.ch();
                    if (c == 'q' || c == 'Q') {
                        out.println(Lang.get("menu.exited"));
                        return 0;
                    }
                    int pick = Menu.indexOfKey(items, c);
                    if (pick >= 0) {
                        selected = pick;
                    }
                }
                case ESC -> {
                    // ESC 单独按不做事，免得误触退出
                }
            }
        }
    }

    // 菜单项跑完之后回到菜单用的标记
    private static final int EXIT_MENU = -1;

    private int dispatch(Menu.Item it, Runner runner) throws IOException {
        return switch (it.id()) {
            case Menu.REPORT -> openReport();
            case Menu.LANG -> switchLanguage();
            case "analyze" -> analyzeInteractive(runner);
            default -> runner.run(it.target());
        };
    }

    // 诊断：先问要不要指定文件，直接回车就用最新的
    private int analyzeInteractive(Runner runner) throws IOException {
        out.println("  " + menu.theme().value(Lang.get("menu.analyze.ask")));
        out.flush();
        String answer = readLine();
        if (answer.isBlank()) {
            return runner.run("analyze");
        }
        Path p = Path.of(stripQuotes(answer));
        if (!p.isAbsolute() && gameDir != null) {
            p = gameDir.resolve(answer);
        }
        return runner.run("analyze \"" + p + "\"");
    }

    private int switchLanguage() throws IOException {
        out.println("  " + menu.theme().value(Lang.get("menu.lang.current", Lang.language())));
        out.flush();
        String answer = readLine().trim();
        if (answer.equals("1") || answer.equalsIgnoreCase("zh")) {
            Lang.setLanguage(Lang.ZH);
        } else if (answer.equals("2") || answer.equalsIgnoreCase("en")) {
            Lang.setLanguage(Lang.EN);
        }
        out.println("  " + menu.theme().good(Lang.get("menu.lang.switched", Lang.language())));
        return 0;
    }

    // 报告文件：给出路径并在支持的终端里做成可点的链接
    private int openReport() {
        List<Path> candidates = new ArrayList<>();
        if (gameDir != null) {
            candidates.add(gameDir.resolve("logs/oba-latest.txt"));
            candidates.add(gameDir.resolve("logs/oba-report.txt"));
        }
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                String url = p.toUri().toString();
                out.println("  " + Lang.get("menu.report.opened",
                        menu.theme().link(url, p.toString())));
                return 0;
            }
        }
        out.println("  " + menu.theme().warn(Lang.get("menu.report.none")));
        return 0;
    }

    // 读一行，走同一套输入。没有 JLine 时退化成逐字节拼
    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        while (true) {
            ConsoleInput.Key k = in.read();
            if (k.kind() == ConsoleInput.Key.Kind.EOF) {
                break;
            }
            if (k.kind() == ConsoleInput.Key.Kind.ENTER) {
                break;
            }
            if (k.kind() == ConsoleInput.Key.Kind.CHAR && k.ch() == 'q') {
                break;
            }
            if (k.ch() != '\0') {
                sb.append(k.ch());
            }
        }
        return sb.toString();
    }

    // 等任意键，让用户看完输出再回菜单
    private void waitKey() throws IOException {
        out.println();
        out.println("  " + menu.theme().dim(Lang.get("menu.prompt")));
        out.flush();
        in.read();
        clear(2);
    }

    // 把刚才画的那几行擦掉，下一轮原地重画
    private void clear(int lines) {
        if (!in.interactive() || !color) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            sb.append("\u001B[1A\u001B[2K");
        }
        out.print(sb);
        out.flush();
    }

    // 终端宽度，拿不到就用默认值
    private static int themeColumns() {
        String cols = System.getenv("COLUMNS");
        if (cols != null && !cols.isBlank()) {
            try {
                return Integer.parseInt(cols.trim());
            } catch (NumberFormatException ignored) {
                // 环境变量不可信，往下走
            }
        }
        return Theme.BOX_WIDTH;
    }

    private static String stripQuotes(String s) {
        return Cli.stripQuotes(s.trim());
    }

    // 报告文件在哪儿，游戏内那份菜单也用这个说法
    public static Optional<Path> newestReport(Path gameDir) {
        if (gameDir == null) {
            return Optional.empty();
        }
        Path p = gameDir.resolve("logs/oba-latest.txt");
        return Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
    }

    // 最新的日志或崩溃报告，菜单里显示一下当前会诊断什么
    public static String subjectHint(Path gameDir) {
        if (gameDir == null) {
            return "";
        }
        Optional<Path> crash = Analyzer.newestCrashReport(gameDir);
        if (crash.isPresent()) {
            return crash.get().getFileName().toString();
        }
        Path latest = gameDir.resolve("logs/latest.log");
        return Files.isRegularFile(latest) ? "logs/latest.log" : "";
    }

    // 品牌行，游戏内聊天栏也用同一句
    public static String title() {
        return Oba.NAME + " " + Oba.ABBR + " v" + Oba.VERSION;
    }
}
