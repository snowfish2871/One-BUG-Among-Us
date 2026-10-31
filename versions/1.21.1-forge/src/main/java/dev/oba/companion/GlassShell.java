package dev.oba.companion;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * 无边框的毛玻璃外壳：窗口整体是逐像素透明的，内容用圆角裁剪，
 * 后面垫一层把桌面截下来再模糊的底图，加一层半透明着色、一圈高光边和一圈外阴影。
 *
 * <p>为什么不用 JavaFX：一个 jar 里塞 JavaFX 要带上几十兆的本地库，还要按平台分片；
 * 这里的窗口只需要一层模糊底图加几个控件，用 Java2D 就够，而且 Java 17 到 25 都能跑。
 *
 * <p>真正的「背景模糊」只能靠截屏再模糊：Java 没有让系统把窗口背后的画面模糊掉的公开 API。
 * 截屏拿不到（无头环境、权限被拒）时退化成纯半透明渐变，界面仍然可用。
 */
final class GlassShell {

    /** 外阴影占的边距，内容区从这一圈往里缩。 */
    private static final int SHADOW = 24;
    /** 窗口圆角半径。 */
    private static final int RADIUS = 18;
    /** 底图模糊的缩放比：先缩小再模糊，最后拉回来，效果够用又快。 */
    private static final int BLUR_SCALE = 4;

    private final JFrame frame;
    private final Host host;
    private final boolean dark;
    private Timer settle;

    private GlassShell(JFrame frame, Host host, boolean dark) {
        this.frame = frame;
        this.host = host;
        this.dark = dark;
    }

    /** 当前环境能不能做逐像素透明；不能就只能老老实实画不透明窗口。 */
    static boolean supported() {
        try {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (!gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
                return false;
            }
            return gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 把内容包进玻璃壳，并把窗口改成无边框透明。
     *
     * @return 外壳组件，调用方把它设成 contentPane
     */
    static GlassShell install(JFrame frame, JComponent content, boolean dark) {
        Host host = new Host(dark);
        host.setLayout(new BorderLayout());
        host.setOpaque(false);
        host.setBorder(javax.swing.BorderFactory.createEmptyBorder(SHADOW, SHADOW, SHADOW, SHADOW));
        host.add(content, BorderLayout.CENTER);

        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 0));
        frame.setContentPane(host);

        GlassShell shell = new GlassShell(frame, host, dark);
        host.shell = shell;

        // 窗口一出现就先截一张：这时候还没有自己挡在前面，拿到的就是干净的桌面
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                shell.captureAsync(true);
            }
        });
        // 拖动结束后重新截一次，让模糊跟着走
        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
                shell.scheduleCapture();
            }
        });
        return shell;
    }

    /** 让某块区域可以拖动窗口（无边框窗口没有系统标题栏可拖）。 */
    static void enableDrag(JFrame frame, JComponent handle) {
        final Point[] origin = new Point[2];
        handle.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                origin[0] = e.getPoint();
                origin[1] = frame.getLocation();
            }
        });
        handle.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin[0] == null) {
                    return;
                }
                frame.setLocation(origin[1].x + e.getX() - origin[0].x,
                        origin[1].y + e.getY() - origin[0].y);
            }
        });
    }

    /**
     * 直接显示。
     *
     * <p>本来想做淡入，但 Windows 上「逐像素透明」和「整窗不透明度」是互斥的：
     * 一旦调用 setOpacity，窗口可能直接变成完全看不见并且再也调不回来。
     * 宁可硬蹦出来，也不能让崩溃报告窗口不出现。
     */
    void fadeIn() {
        frame.setVisible(true);
    }

    private void scheduleCapture() {
        if (settle != null) {
            settle.stop();
        }
        settle = new Timer(380, e -> captureAsync(false));
        settle.setRepeats(false);
        settle.start();
    }

    private void captureAsync(boolean first) {
        new Thread(() -> {
            BufferedImage shot = capture(first);
            if (shot == null) {
                return;
            }
            BufferedImage frosted = frosted(shot);
            if (Boolean.getBoolean("oba.companion.dumpGlass")) {
                dump(frosted, new java.io.File(System.getProperty("java.io.tmpdir"), "oba-glass.png"));
            }
            javax.swing.SwingUtilities.invokeLater(() -> {
                host.blurred = frosted;
                host.repaint();
            });
        }, "oba-glass").start();
    }

    /**
     * 截窗口下面那块桌面。移动之后再截的时候自己已经挡在上面了，
     * 所以先把窗口调到全透明，截完再调回来——一帧的闪动，比糊上一层自己要好。
     */
    private BufferedImage capture(boolean first) {
        Rectangle bounds = frame.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) {
            return null;
        }
        boolean hidden = false;
        try {
            if (!first) {
                // 挪过位置之后再截，自己已经挡在上面了：先藏起来，截完再露出来
                hidden = hideWindow();
            }
            Robot robot = new Robot();
            BufferedImage shot = robot.createScreenCapture(bounds);
            // 高 DPI 下截图按物理像素走，跟用户坐标不是一个尺度，按比例缩回去
            GraphicsConfiguration gc = frame.getGraphicsConfiguration();
            double sx = gc.getDefaultTransform().getScaleX();
            double sy = gc.getDefaultTransform().getScaleY();
            if (sx > 1.01 || sy > 1.01) {
                int w = (int) Math.round(shot.getWidth() / sx);
                int h = (int) Math.round(shot.getHeight() / sy);
                BufferedImage scaled = new BufferedImage(Math.max(1, w), Math.max(1, h),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaled.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(shot, 0, 0, w, h, null);
                g.dispose();
                shot = scaled;
            }
            return shot;
        } catch (Throwable t) {
            return null;
        } finally {
            if (hidden) {
                showWindow();
            }
        }
    }

    private boolean hideWindow() {
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> frame.setVisible(false));
            Thread.sleep(80);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void showWindow() {
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                frame.setVisible(true);
                frame.toFront();
            });
        } catch (Throwable ignored) {
            // 露不出来也只是下一帧的事
        }
    }

    /** 缩小 → 两遍方框模糊 → 再拉大，等价于一层开销很小的毛玻璃。 */
    private static BufferedImage frosted(BufferedImage src) {
        int w = Math.max(1, src.getWidth() / BLUR_SCALE);
        int h = Math.max(1, src.getHeight() / BLUR_SCALE);
        BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        int[] px = small.getRGB(0, 0, w, h, null, 0, w);
        boxBlur(px, w, h, 2);
        small.setRGB(0, 0, w, h, px, 0, w);
        return small;
    }

    /** 就地做一遍方框模糊，radius 是半径。 */
    private static void boxBlur(int[] px, int w, int h, int radius) {
        int[] src = px.clone();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 0;
                int g = 0;
                int b = 0;
                int n = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= h) {
                        continue;
                    }
                    for (int dx = -radius; dx <= radius; dx++) {
                        int xx = x + dx;
                        if (xx < 0 || xx >= w) {
                            continue;
                        }
                        int c = src[yy * w + xx];
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                        n++;
                    }
                }
                if (n == 0) {
                    continue;
                }
                int a = (src[y * w + x] >>> 24) & 0xFF;
                px[y * w + x] = (a << 24) | ((r / n) << 16) | ((g / n) << 8) | (b / n);
            }
        }
    }

    /** 外壳本体：画阴影、模糊底图、着色、高光边，并把子组件按圆角裁掉。 */
    private static final class Host extends JPanel {
        private final boolean dark;
        private BufferedImage blurred;
        private GlassShell shell;

        Host(boolean dark) {
            this.dark = dark;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();
            int cw = w - SHADOW * 2;
            int ch = h - SHADOW * 2;
            if (cw < 8 || ch < 8) {
                g2.dispose();
                return;
            }

            // 外阴影：从外到内画一圈逐渐变实的圆角矩形，充当 Windows 无边框窗口丢掉的系统阴影
            for (int i = 0; i < SHADOW; i++) {
                double a = 0.055 * (1.0 - (double) i / SHADOW);
                g2.setColor(new Color(0, 0, 0, (int) Math.round(a * 255)));
                g2.fill(new RoundRectangle2D.Float(SHADOW - i, SHADOW - i + 2,
                        cw + i * 2, ch + i * 2, RADIUS + i, RADIUS + i));
            }

            RoundRectangle2D body = new RoundRectangle2D.Float(SHADOW, SHADOW, cw, ch, RADIUS, RADIUS);

            // 底图：桌面截屏的模糊版；拿不到就画一层渐变顶上
            Graphics2D bg = (Graphics2D) g2.create();
            bg.clip(body);
            if (blurred != null) {
                bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                bg.drawImage(blurred, SHADOW, SHADOW, cw, ch, null);
                // 模糊之后叠一层极淡的磨砂颗粒感，纯色块容易显脏。
                // 间距和透明度都调小过：早先 3 像素一条、alpha 26，看起来像条纹窗帘
                bg.setColor(new Color(255, 255, 255, dark ? 6 : 12));
                for (int y = SHADOW; y < SHADOW + ch; y += 4) {
                    bg.drawLine(SHADOW, y, SHADOW + cw, y);
                }
            } else {
                bg.setPaint(new java.awt.GradientPaint(0, SHADOW,
                        dark ? new Color(0x3A, 0x3A, 0x3E) : new Color(0xF7, 0xF8, 0xFA),
                        0, SHADOW + ch,
                        dark ? new Color(0x20, 0x20, 0x23) : new Color(0xE8, 0xEA, 0xEE)));
                bg.fill(body);
            }
            // 着色层：玻璃到底还是要有底色，不然字压在亮背景上没法看
            bg.setColor(dark ? new Color(26, 26, 29, 108) : new Color(250, 250, 253, 58));
            bg.fill(body);
            bg.dispose();

            // 斜向高光：左上亮、右下透明，aero 的那层“塑料片”质感主要靠它
            java.awt.GradientPaint sheen = new java.awt.GradientPaint(
                    SHADOW, SHADOW, new Color(255, 255, 255, dark ? 26 : 96),
                    SHADOW + cw * 0.75f, SHADOW + ch * 0.75f, new Color(255, 255, 255, 0));
            Graphics2D sh = (Graphics2D) g2.create();
            sh.clip(body);
            sh.setPaint(sheen);
            sh.fill(body);
            sh.dispose();

            // 高光边：上沿亮、下沿暗，看起来才像有一层玻璃
            Graphics2D edge = (Graphics2D) g2.create();
            edge.setStroke(new java.awt.BasicStroke(1f));
            edge.setColor(new Color(255, 255, 255, dark ? 40 : 210));
            edge.draw(new RoundRectangle2D.Float(SHADOW + 0.5f, SHADOW + 0.5f,
                    cw - 1, ch - 1, RADIUS, RADIUS));
            edge.setColor(new Color(0, 0, 0, dark ? 90 : 40));
            edge.draw(new RoundRectangle2D.Float(SHADOW + 0.5f, SHADOW + ch - 1.5f,
                    cw - 1, 1.2f, RADIUS, RADIUS));
            edge.dispose();
            g2.dispose();
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.clip(new RoundRectangle2D.Float(SHADOW, SHADOW,
                    getWidth() - SHADOW * 2, getHeight() - SHADOW * 2, RADIUS, RADIUS));
            super.paintChildren(g2);
            g2.dispose();
        }
    }

    /** 给窗口按钮用的一小块统一外观的方按钮。 */
    static JComponent windowButton(String glyph, boolean danger, Runnable action) {
        final boolean[] hover = {false};
        JPanel b = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hover[0]) {
                    g2.setColor(danger ? new Color(0xC4, 0x2B, 0x1C, 220) : new Color(0, 0, 0, 26));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        b.setLayout(new BorderLayout());
        javax.swing.JLabel l = new javax.swing.JLabel(glyph, javax.swing.SwingConstants.CENTER);
        l.setOpaque(false);
        l.setForeground(new Color(0x44, 0x44, 0x44));
        l.setFont(new java.awt.Font("Segoe UI Symbol", java.awt.Font.PLAIN, 13));
        b.add(l, BorderLayout.CENTER);
        b.setPreferredSize(new java.awt.Dimension(30, 26));
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        final JPanel p = b;
        p.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover[0] = true;
                p.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover[0] = false;
                p.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                action.run();
            }
        });
        return b;
    }

    /** 让组件完全不透明地铺满，方便嵌入标题栏右侧。 */
    static JComponent spacer(int w) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new java.awt.Dimension(w, 1));
        return p;
    }

    /** 未使用但保留：调试时把模糊底图导出成 png。 */
    static void dump(BufferedImage img, java.io.File out) {
        try {
            javax.imageio.ImageIO.write(img, "png", out);
        } catch (Exception ignored) {
            // 调试用，失败无所谓
        }
    }

    static Insets shadowInsets() {
        return new Insets(SHADOW, SHADOW, SHADOW, SHADOW);
    }

    static int shadow() {
        return SHADOW;
    }

    static int radius() {
        return RADIUS;
    }

    static AlphaComposite alpha(float a) {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a);
    }
}
