// 崩溃之后弹出来的那个窗口。
//
// 用 Swing 而不是 WinUI 3：附带一个 .NET 程序意味着多一个文件（或者把它塞进 jar
// 再解到临时目录，几十 MB 起步，还得机器上装了 .NET 与 Windows App SDK 运行时）。
// Swing 是 JDK 自带的，一个 jar 就能跑，代价是观感只能做到「接近」Fluent：
// 圆角卡片、系统强调色、分段标题这些都在，动效和 Mica 没有。
package dev.oba.companion;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompanionWindow {

    // Fluent 的那套颜色，浅色主题。深色只换底色和前景，强调色一致
    private static final Color ACCENT = new Color(0x00, 0x67, 0xC0);
    private static final Color DANGER = new Color(0xC4, 0x2B, 0x1C);
    private static final Color WARN = new Color(0x9D, 0x5D, 0x00);
    private static final Color INFO = new Color(0x0F, 0x6C, 0xBD);

    private final Handoff handoff;
    private final String fallbackText;
    private final CompanionMain.Args args;
    private final boolean dark;
    private final Color bg;
    private final Color card;
    private final Color line;
    private final Color fg;
    private final Color muted;
    private final Font uiFont;
    private final Font titleFont;
    private final Font monoFont;
    private JFrame frame;
    private JTabbedPane tabs;
    private final boolean glass;

    public CompanionWindow(Handoff handoff, String fallbackText, CompanionMain.Args args) {
        this.handoff = handoff;
        this.fallbackText = fallbackText;
        this.args = args;
        this.dark = Boolean.getBoolean("oba.companion.dark");
        this.glass = GlassShell.supported() && !Boolean.getBoolean("oba.companion.opaque");
        // 玻璃模式下底色交给 GlassShell 画（半透明着色 + 模糊底图），这里必须是全透明，
        // 否则 JPanel 会把那层半透明盖掉，看起来就成了一块死板的灰板
        this.bg = glass ? new Color(0, 0, 0, 0)
                : dark ? new Color(0x20, 0x20, 0x20) : new Color(0xF6, 0xF6, 0xF6);
        this.card = glass ? (dark ? new Color(38, 38, 40, 150) : new Color(255, 255, 255, 150))
                : dark ? new Color(0x2B, 0x2B, 0x2B) : Color.WHITE;
        this.line = dark ? new Color(0x3A, 0x3A, 0x3A) : new Color(0xE2, 0xE2, 0xE2);
        this.fg = dark ? new Color(0xF3, 0xF3, 0xF3) : new Color(0x1A, 0x1A, 0x1A);
        this.muted = dark ? new Color(0xA0, 0xA0, 0xA0) : new Color(0x5C, 0x5C, 0x5C);
        this.uiFont = pickFont("Microsoft YaHei UI", "Segoe UI Variable Text", "Segoe UI");
        this.titleFont = uiFont.deriveFont(Font.BOLD, 20f);
        this.monoFont = pickFont("Sarasa Mono SC", "Microsoft YaHei UI", "Consolas", "Cascadia Mono");
    }

    public void open() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 系统皮肤拿不到就用默认的，界面还能用
        }
        UIManager.put("TabbedPane.font", uiFont);
        UIManager.put("Table.font", uiFont);
        UIManager.put("Button.font", uiFont);

        frame = new JFrame("One BUG Among Us · 崩溃复盘");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        GlassShell shell = null;
        if (glass) {
            shell = GlassShell.install(frame, root(), dark);
        } else {
            frame.setContentPane(root());
        }
        // 系统缩放开大时（150% 这种），1120×740 会被系统再放大一遍而顶出屏幕。
        // 用屏幕的实际像素除以缩放比，换算成 Java 的用户坐标再收一次。
        java.awt.GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        java.awt.Rectangle physical = gc.getBounds();
        java.awt.geom.AffineTransform tf = gc.getDefaultTransform();
        int usableW = (int) Math.round(physical.width / Math.max(0.5, tf.getScaleX()));
        int usableH = (int) Math.round(physical.height / Math.max(0.5, tf.getScaleY()));
        // 调试时可以用 -Doba.companion.w / .h 指定一个更小的窗口，方便把窗口摆到花哨的背景上看毛玻璃
        int w = Math.min(Integer.getInteger("oba.companion.w", 1120), Math.max(640, usableW - 60));
        int h = Math.min(Integer.getInteger("oba.companion.h", 740), Math.max(440, usableH - 100));
        frame.setMinimumSize(new Dimension(Math.min(880, w), Math.min(520, h)));
        frame.setSize(w, h);
        // 摆在左上一点的位置：居中在缩放环境下容易被算到屏幕外面去
        // 调试时可以用 -Doba.companion.x / .y 摆到别处，方便对着花一点的背景看毛玻璃效果
        int px = Integer.getInteger("oba.companion.x", 60);
        int py = Integer.getInteger("oba.companion.y", 50);
        frame.setLocation(px, py);
        if (shell != null) {
            shell.fadeIn();
        } else {
            frame.setVisible(true);
        }
        // --tab 指定先显示哪一页；名字认不出来就留在第一页
        int start = switch (args.tab == null ? "" : args.tab.toLowerCase(java.util.Locale.ROOT)) {
            case "timeline" -> 1;
            case "environment", "env" -> 2;
            case "json" -> 3;
            default -> 0;
        };
        if (start > 0 && tabs != null) {
            tabs.setSelectedIndex(start);
        }
        // 顶部再放开置顶：直接一直置顶很烦人，但刚出来这一下要抢到最前面
        frame.setAlwaysOnTop(true);
        frame.toFront();
        frame.requestFocus();
        javax.swing.Timer release = new javax.swing.Timer(1200, e -> frame.setAlwaysOnTop(false));
        release.setRepeats(false);
        release.start();
    }

    // 整个窗口的骨架：顶栏 + 中间两列 + 底栏
    private JPanel root() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.add(header(), BorderLayout.NORTH);

        JPanel columns = new JPanel(new BorderLayout(14, 0));
        columns.setBackground(bg);
        columns.setBorder(new EmptyBorder(0, 18, 0, 18));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setPreferredSize(new Dimension(380, 0));
        left.add(card(headlineCard()));
        left.add(Box.createVerticalStrut(12));
        left.add(card(culpritCard()));
        left.add(Box.createVerticalStrut(12));
        left.add(card(issuesCard()));
        left.add(Box.createVerticalStrut(12));
        left.add(card(metaCard()));

        JScrollPane leftScroll = new JScrollPane(left,
                javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScroll.setBorder(null);
        leftScroll.setOpaque(false);
        leftScroll.getViewport().setOpaque(false);
        leftScroll.getVerticalScrollBar().setUnitIncrement(24);
        leftScroll.setPreferredSize(new Dimension(396, 0));
        columns.add(leftScroll, BorderLayout.WEST);
        columns.add(tabs(), BorderLayout.CENTER);

        root.add(columns, BorderLayout.CENTER);
        root.add(footer(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel header() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(bg);
        bar.setBorder(new EmptyBorder(16, 18, 14, 18));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        String kickerText = "诊断报告 · 正常退出";
        if (handoff != null) {
            if (handoff.isCrash()) {
                kickerText = "诊断报告 · 崩溃";
            } else if (handoff.hadErrors()) {
                kickerText = "诊断报告 · 出过错误";
            }
        } else if (fallbackText != null) {
            kickerText = "诊断报告 · 崩溃";
        }
        JLabel kicker = label(kickerText,
                uiFont.deriveFont(Font.BOLD, 11f), ACCENT);
        JLabel title = label("One BUG Among Us", titleFont, fg);
        JLabel sub = label(subtitleText(), uiFont.deriveFont(12f), muted);
        left.add(kicker);
        left.add(Box.createVerticalStrut(2));
        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        // 右上角的强调色圆点，纯粹为了不显得空
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(handoff != null && handoff.isCrash() ? DANGER : ACCENT);
                g2.fillOval(0, 0, 14, 14);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(16, 16));

        JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(dot);
        if (glass) {
            actions.add(GlassShell.windowButton("—", false, () -> frame.setState(JFrame.ICONIFIED)));
            actions.add(GlassShell.windowButton("✕", true, () -> frame.dispose()));
        }
        bar.add(left, BorderLayout.WEST);
        bar.add(actions, BorderLayout.EAST);

        if (glass) {
            // 无边框窗口没有标题栏可拖，整条顶栏连同上面的文字都拿来拖动
            GlassShell.enableDrag(frame, bar);
            GlassShell.enableDrag(frame, left);
            for (java.awt.Component c : left.getComponents()) {
                if (c instanceof javax.swing.JComponent) {
                    GlassShell.enableDrag(frame, (javax.swing.JComponent) c);
                }
            }
        }
        return bar;
    }

    private JPanel headlineCard() {
        JPanel body = column();
        body.add(sectionTitle("结论"));
        javax.swing.JTextArea head = wrapped(handoff == null ? "OBA 没来得及给出结论" : handoff.headlineOrDefault(),
                uiFont.deriveFont(Font.BOLD, 15f), fg);
        body.add(Box.createVerticalStrut(6));
        body.add(head);
        body.add(Box.createVerticalStrut(8));
        String verdict = handoff != null && !handoff.verdicts.isEmpty()
                ? firstLine(handoff.verdicts.get(handoff.verdicts.size() - 1))
                : "这一场没有播报过结论。";
        body.add(wrapped(verdict, uiFont.deriveFont(12f), muted));
        return body;
    }

    private JPanel culpritCard() {
        JPanel body = column();
        body.add(sectionTitle("最可能的元凶"));
        Handoff.Culprit c = handoff == null ? null : handoff.culprit;
        body.add(Box.createVerticalStrut(6));
        body.add(label(c == null ? "没有定位到具体模组" : c.name,
                uiFont.deriveFont(Font.BOLD, 17f), fg));
        if (c != null) {
            body.add(Box.createVerticalStrut(4));
            body.add(label(c.file, monoFont.deriveFont(11f), muted));
            body.add(Box.createVerticalStrut(10));
            body.add(confidenceRow(c));
            body.add(Box.createVerticalStrut(8));
            body.add(wrapped(c.location, monoFont.deriveFont(11f), fg));
        } else {
            body.add(Box.createVerticalStrut(8));
            body.add(wrapped("游戏目录里没有任何栈帧落在某个模组上。", uiFont.deriveFont(12f), muted));
        }
        return body;
    }

    private JPanel confidenceRow(Handoff.Culprit c) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(percent(c));
        bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(0, 6));
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.setBackground(dark ? new Color(0x3A, 0x3A, 0x3A) : new Color(0xEA, 0xEA, 0xEA));
        bar.setForeground(percent(c) >= 80 ? DANGER : ACCENT);
        String text = c.confidence.isBlank()
                ? String.format(Locale.ROOT, "分数 %.2f", c.score)
                : c.confidence;
        row.add(bar, BorderLayout.CENTER);
        row.add(label(text, uiFont.deriveFont(11f), muted), BorderLayout.EAST);
        return row;
    }

    /** 控制台里那块结论是多行的，卡片上只要第一行。 */
    private static String firstLine(String text) {
        if (text == null) {
            return "";
        }
        int cut = text.indexOf('\n');
        String line = cut < 0 ? text.trim() : text.substring(0, cut).trim();
        // 控制台原文以 [OBA] 打头，窗口里不用再报一遍家门
        return line.startsWith("[OBA] ") ? line.substring(6).trim() : line;
    }

    private JPanel issuesCard() {
        JPanel body = column();
        body.add(sectionTitle("问题清单"));
        body.add(Box.createVerticalStrut(6));
        List<Handoff.Issue> issues = handoff == null ? List.of() : handoff.issues;
        if (issues.isEmpty()) {
            body.add(wrapped("没有命中规则的问题。", uiFont.deriveFont(12f), muted));
            return body;
        }
        for (Handoff.Issue issue : issues) {
            JPanel item = column();
            JPanel head = new JPanel(new BorderLayout(8, 0));
            head.setOpaque(false);
            head.add(label(severityText(issue.severity), uiFont.deriveFont(Font.BOLD, 10f),
                    severityColor(issue.severity)), BorderLayout.WEST);
            head.add(label(issue.title, uiFont.deriveFont(12f), fg), BorderLayout.CENTER);
            item.add(head);
            if (!issue.detail.isBlank()) {
                item.add(Box.createVerticalStrut(2));
                item.add(wrapped(issue.detail, uiFont.deriveFont(11f), muted));
            }
            if (issue.rule != null && !issue.rule.isBlank()) {
                item.add(Box.createVerticalStrut(2));
                item.add(wrapped("规则：" + issue.rule, monoFont.deriveFont(10f), muted));
            }
            body.add(item);
            body.add(Box.createVerticalStrut(10));
        }
        return body;
    }

    private JPanel metaCard() {
        JPanel body = column();
        body.add(sectionTitle("这一场的信息"));
        String gameDir = handoff != null && !handoff.gameDir.isBlank() ? handoff.gameDir : args.gameDir;
        String loader = handoff != null && !handoff.loader.isBlank() ? handoff.loader : args.loader;
        String version = handoff != null && !handoff.modVersion.isBlank() ? handoff.modVersion : args.modVersion;
        String report = handoff == null || handoff.reportPath.isBlank() ? "这一场没有写出报告" : handoff.reportPath;
        body.add(Box.createVerticalStrut(6));
        body.add(wrapped("游戏目录：" + gameDir, uiFont.deriveFont(11f), muted));
        body.add(wrapped("加载器：" + (loader.isBlank() ? "未知" : prettyLoader(loader)) + " · OBA " + version,
                uiFont.deriveFont(11f), muted));
        body.add(wrapped("报告文件：" + report, monoFont.deriveFont(11f), muted));
        return body;
    }

    private JTabbedPane tabs() {
        JTabbedPane pane = new JTabbedPane();
        this.tabs = pane;
        pane.setFont(uiFont);
        pane.setBackground(glass ? new Color(255, 255, 255, dark ? 40 : 70) : card);
        pane.setForeground(fg);
        pane.addTab("报告", page(new JScrollPane(reportText())));
        pane.addTab("时间线", page(new JScrollPane(timelineTable())));
        pane.addTab("环境", page(new JScrollPane(environmentTable())));
        pane.addTab("交接 JSON", page(new JScrollPane(textArea(handoff == null ? "" : handoff.raw, false))));
        if (glass) {
            pane.setOpaque(false);
        }
        return pane;
    }

    /** 玻璃模式下让整块内容区透出后面的模糊底图，只留一层很淡的白。 */
    private javax.swing.JComponent page(JScrollPane scroll) {
        scroll.setBorder(null);
        if (glass) {
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            java.awt.Component inner = scroll.getViewport().getView();
            if (inner instanceof javax.swing.JComponent) {
                javax.swing.JComponent c = (javax.swing.JComponent) inner;
                c.setOpaque(false);
                if (inner instanceof JTable) {
                    JTable t = (JTable) inner;
                    t.setOpaque(false);
                    t.setBackground(new Color(255, 255, 255, 0));
                    t.setDefaultRenderer(Object.class, new GlassCellRenderer());
                }
                if (inner instanceof JTextArea) {
                    ((JTextArea) inner).setBackground(new Color(255, 255, 255, 0));
                }
            }
        }
        return scroll;
    }

    /** 表格单元格：透明底，只保留文字与选中态。 */
    private static final class GlassCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        GlassCellRenderer() {
            setOpaque(false);
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                               boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setOpaque(selected);
            setBackground(new Color(0x00, 0x67, 0xC0, 70));
            setForeground(selected ? Color.WHITE : table.getForeground());
            return this;
        }
    }

    private JTextArea reportText() {
        if (handoff != null && !handoff.reportText.isBlank()) {
            return textArea(handoff.reportText, true);
        }
        if (fallbackText != null && !fallbackText.isBlank()) {
            JTextArea area = textArea("OBA 没来得及给出结论（游戏在模组加载阶段就断了）。下面是崩溃报告原文：\n\n"
                    + fallbackText, true);
            return area;
        }
        return textArea("没有报告，也没有新的崩溃报告。可能只是正常关掉了游戏。", true);
    }

    private JTable timelineTable() {
        String[] cols = {"时间", "模组", "类型", "发生了什么"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        if (handoff != null) {
            for (Handoff.Entry e : handoff.timeline) {
                String text = e.text + (e.detail.isBlank() ? "" : "\n" + e.detail);
                model.addRow(new Object[]{e.time, e.modName, e.type, text});
            }
        }
        return table(model, new int[]{80, 160, 90, 620});
    }

    private JTable environmentTable() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"项", "值"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        if (handoff != null) {
            handoff.environment.forEach((k, v) -> model.addRow(new Object[]{k, v}));
        }
        return table(model, new int[]{200, 600});
    }

    private JTable table(DefaultTableModel model, int[] widths) {
        JTable table = new JTable(model);
        table.setFont(uiFont.deriveFont(12f));
        table.setRowHeight(26);
        table.setShowGrid(false);
        table.setBackground(card);
        table.setForeground(fg);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(uiFont.deriveFont(Font.BOLD, 11f));
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        return table;
    }

    private JPanel footer() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(bg);
        bar.setBorder(new EmptyBorder(10, 18, 14, 18));
        bar.add(label("OBA 只读诊断：不改动、不移动、不删除任何模组文件。",
                uiFont.deriveFont(11f), muted), BorderLayout.WEST);

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.add(button("打开报告文件", () -> openPath(handoff == null ? "" : handoff.reportPath)));
        buttons.add(Box.createHorizontalStrut(8));
        String gameDir = handoff != null && !handoff.gameDir.isBlank() ? handoff.gameDir : args.gameDir;
        buttons.add(button("打开 logs 目录", () -> openPath(gameDir.isBlank() ? "" : gameDir + File.separator + "logs")));
        buttons.add(Box.createHorizontalStrut(8));
        buttons.add(button("关闭", () -> {
            if (frame != null) {
                frame.dispose();
            }
        }));
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(uiFont.deriveFont(12f));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(dark ? new Color(0x33, 0x33, 0x33) : Color.WHITE);
        b.setForeground(fg);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(line),
                new EmptyBorder(6, 14, 6, 14)));
        b.addActionListener(e -> action.run());
        return b;
    }

    private void openPath(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        try {
            File f = new File(path);
            if (!f.exists()) {
                return;
            }
            Desktop.getDesktop().open(f);
        } catch (Exception ignored) {
            // 打不开就算了，界面上还有路径可以自己拷
        }
    }

    // 一张圆角卡片。Swing 没有现成的卡片控件，圆角得自己画
    private JPanel card(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(card);
                g2.fill(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 14f, 14f));
                g2.setColor(line);
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, 14f, 14f));
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(14, 14, 14, 14));
        wrapper.add(content, BorderLayout.CENTER);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrapper;
    }

    private JPanel column() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = label(text, uiFont.deriveFont(Font.BOLD, 11f), muted);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /**
     * Swing 的 HTML 不会给长串折行，类的全名和栈帧会一路顶到卡片外面被切掉。
     * 每隔一段塞一个零宽空格，让它有地方断。（这里用 (char) 0x200B，不能写转义，
     * javac 在分词之前就会把源码里的 Unicode 转义处理掉。）
     */
    private static String breakLong(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        int run = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                run = 0;
                out.append(c);
                continue;
            }
            if (run >= 24) {
                out.append((char) 0x200B);
                run = 0;
            }
            out.append(c);
            run++;
        }
        return out.toString();
    }

    private JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /**
     * 长文本要能换行。JLabel 不换行，HTML 里的宽度又跟实际字体尺寸对不上（高 DPI 下会被切掉），
     * 所以用一个只读、透明、关掉边框的 JTextArea：它按真实字体宽度折行，高度自己算。
     */
    private JTextArea wrapped(String text, Font font, Color color) {
        int width = uiWidth() - 110;
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        area.setFont(font);
        area.setForeground(color);
        area.setSize(new Dimension(width, Short.MAX_VALUE));
        int height = area.getPreferredSize().height;
        area.setPreferredSize(new Dimension(width, height));
        area.setMaximumSize(new Dimension(width, height));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        return area;
    }

    private int uiWidth() {
        return 360;
    }

    private JTextArea textArea(String text, boolean wrap) {
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setEditable(false);
        area.setFont(monoFont.deriveFont(12f));
        area.setBackground(card);
        area.setForeground(fg);
        area.setCaretColor(fg);
        area.setBorder(new EmptyBorder(12, 14, 12, 14));
        area.setLineWrap(wrap);
        area.setWrapStyleWord(wrap);
        return area;
    }

    private String subtitleText() {
        String loader = handoff != null && !handoff.loader.isBlank() ? handoff.loader : args.loader;
        String version = handoff != null && !handoff.modVersion.isBlank() ? handoff.modVersion : args.modVersion;
        String time = handoff == null ? "" : handoff.writtenText();
        StringBuilder b = new StringBuilder();
        b.append("OBA ").append(version.isBlank() ? "?" : version);
        if (!loader.isBlank()) {
            b.append(" · ").append(prettyLoader(loader));
        }
        if (!time.isBlank()) {
            b.append(" · 写于 ").append(time);
        }
        return b.toString();
    }

    /** 交接文件里写的是 neoforge / forge262 这种内部名，界面上还是写成人看得懂的。 */
    private static String prettyLoader(String raw) {
        return switch (raw) {
            case "neoforge" -> "NeoForge";
            case "forge", "forge262" -> "Forge";
            default -> raw;
        };
    }

    private static int percent(Handoff.Culprit c) {
        Matcher m = Pattern.compile("(\\d{1,3})\\s*%").matcher(c.confidence == null ? "" : c.confidence);
        if (m.find()) {
            return Math.max(0, Math.min(100, Integer.parseInt(m.group(1))));
        }
        return (int) Math.round(Math.max(0, Math.min(1, c.score)) * 100);
    }

    private static String severityText(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase(Locale.ROOT)) {
            case "fatal" -> "致命";
            case "error" -> "错误";
            case "warn" -> "警告";
            default -> "信息";
        };
    }

    private static Color severityColor(String severity) {
        return switch (severity == null ? "" : severity.toLowerCase(Locale.ROOT)) {
            case "fatal", "error" -> DANGER;
            case "warn" -> WARN;
            default -> INFO;
        };
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * 挑一个能显示中文的字体。名字好看的字体往往缺汉字，缺了就会画成方块，
     * 所以先看能不能显示中文，能显示的才用；一个都不行就退回逻辑字体（自带中文回退）。
     */
    private static Font pickFont(String... names) {
        String[] available = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        Font fallback = null;
        for (String name : names) {
            for (String candidate : available) {
                if (!candidate.equalsIgnoreCase(name)) {
                    continue;
                }
                Font f = new Font(candidate, Font.PLAIN, 12);
                if (f.canDisplayUpTo("中文报告") < 0) {
                    return f;
                }
                if (fallback == null) {
                    fallback = f;
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        return new Font(Font.DIALOG, Font.PLAIN, 12);
    }
}
