// 尽早确定报告语言。
//
// 为什么要有这一步：模组在构造阶段抛异常时，整套加载流程当场结束，
// FMLCommonSetupEvent 根本不会触发 —— 而"读客户端语言"原本挂在那里。
// 结果就是崩溃报告用的是 JVM 区域的语言，而不是玩家在游戏里选的那个。
//
// Options 要等客户端构造完才加载，所以那时读不到。好在语言选择本身就写在
// options.txt 里，构造函数阶段就能直接读文件拿到。等客户端起来之后，
// ClientLanguage 会用 LanguageManager 的真实值再覆盖一次，两边不冲突。
package dev.oba.mod;

import dev.oba.core.Lang;
import dev.oba.core.Oba;
import dev.oba.loader.Loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EarlyLanguage {

    // 只读一次，读不到就算了
    private static volatile boolean attempted;

    // 客户端才需要：专用服务器没有语言选项
    public static void seedIfClient() {
        if (attempted) {
            return;
        }
        attempted = true;
        try {
            Path gameDir = Loader.env().gameDir();
            if (gameDir == null) {
                return;
            }
            Path options = gameDir.resolve("options.txt");
            if (!Files.isRegularFile(options)) {
                return;
            }
            // options.txt 是 UTF-8 的纯文本，很小的文件
            List<String> lines = Files.readAllLines(options, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.startsWith("lang:")) {
                    continue;
                }
                String tag = trimmed.substring("lang:".length()).trim();
                if (!tag.isEmpty()) {
                    Lang.setGameLanguage(tag);
                }
                return;
            }
        } catch (Throwable t) {
            // 读不到就继续用 JVM 区域，不影响别的
            System.err.println(Oba.LOG_PREFIX + Lang.get("mod.clientReadyFailed", t));
        }
    }

    private EarlyLanguage() {
    }
}
