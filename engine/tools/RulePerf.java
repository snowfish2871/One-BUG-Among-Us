package dev.oba.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 性能探针：把一份真实日志逐行喂给 LogAnalyzer 的每条规则，单独计时。
 *
 * 用途：实机上出现「分析卡住几分钟、日志线程被拖死」时，用它定位是哪条正则
 * 在某一行上回溯爆炸。不参与打包，只放在 engine/tools 里手动跑。
 *
 * 用法：java -cp <jar> dev.oba.tools.RulePerf <日志路径> [单条规则预算毫秒]
 */
public final class RulePerf {

    public static void main(String[] args) throws Exception {
        Path file = Path.of(args[0]);
        long budgetMs = args.length > 1 ? Long.parseLong(args[1]) : 200L;
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        String[] lines = text.split("\r?\n", -1);
        System.out.println("日志 " + file + " 共 " + lines.length + " 行");

        Field f = Class.forName("dev.oba.core.LogAnalyzer").getDeclaredField("RULES");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> rules = (List<Object>) f.get(null);
        System.out.println("规则 " + rules.size() + " 条，单条预算 " + budgetMs + " ms");

        for (Object rule : rules) {
            Class<?> rc = rule.getClass();
            Method idM = rc.getDeclaredMethod("id");
            Method patM = rc.getDeclaredMethod("pattern");
            idM.setAccessible(true);
            patM.setAccessible(true);
            String id = (String) idM.invoke(rule);
            Pattern p = (Pattern) patM.invoke(rule);
            long total = 0;
            long worst = 0;
            int worstLine = -1;
            for (int i = 0; i < lines.length; i++) {
                long t0 = System.nanoTime();
                Matcher m = p.matcher(lines[i]);
                m.find();
                long dt = (System.nanoTime() - t0) / 1_000_000L;
                total += dt;
                if (dt > worst) {
                    worst = dt;
                    worstLine = i;
                }
                if (dt > budgetMs) {
                    System.out.printf("!! %-30s 第 %d 行耗时 %d ms%n    行内容: %s%n",
                            id, i + 1, dt, lines[i].length() > 300 ? lines[i].substring(0, 300) + "…" : lines[i]);
                    break;
                }
            }
            if (worst > 5) {
                System.out.printf("%-30s 合计 %5d ms，最慢 %5d ms（第 %d 行）%n", id, total, worst, worstLine + 1);
            }
        }
        System.out.println("结束");
    }
}
