// 这个小文件只放全局常量：名字、版本号、modId、日志前缀。
package dev.oba.core;

public final class Oba {

    // 显示名，报告标题和游戏内提示都用它
    public static final String NAME = "One BUG Among Us";

    // 缩写，界面窄的地方用它
    public static final String ABBR = "OBA";

    // 版本号，会出现在每一种输出格式里
    public static final String VERSION = "1.0.1";

    // modId，必须和 neoforge.mods.toml 里的一致
    public static final String MOD_ID = "oba";

    // 自己输出的日志都带这个前缀，分析时好把自己的话过滤掉。
    // 用缩写是因为聊天栏和日志行首都窄，全名会把正文挤没
    public static final String LOG_PREFIX = "[OBA] ";

    // 报告文件名的前缀
    public static final String REPORT_PREFIX = "oba-report-";

    // 固定名字的那一份，永远指向最新的报告
    public static final String LATEST_REPORT = "oba-latest.txt";

    public static String title() {
        return NAME + " (" + ABBR + ")";
    }

    private Oba() {
    }
}
