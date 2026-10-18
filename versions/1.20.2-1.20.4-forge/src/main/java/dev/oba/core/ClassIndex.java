// 类名/包名到模组的对照表：把堆栈里的一个类翻回它所在的 jar。
package dev.oba.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassIndex {

    // 命中结果：命中的 jar、靠哪个键命中的、有多确定
    public record Match(ModSource source, String matchedKey, double confidence) {
    }

    private final Map<String, List<ModSource>> byClass;
    private final Map<String, List<ModSource>> byPackage;
    private final Map<String, Integer> classCounts;
    private final List<ModSource> sources;
    // 同一个类会被反复解析（一条日志里几十次），缓存一下省事
    private final Map<String, Match> cache = new ConcurrentHashMap<>();

    private ClassIndex(Map<String, List<ModSource>> byClass,
                       Map<String, List<ModSource>> byPackage,
                       Map<String, Integer> classCounts,
                       List<ModSource> sources) {
        this.byClass = byClass;
        this.byPackage = byPackage;
        this.classCounts = classCounts;
        this.sources = List.copyOf(sources);
    }

    // 没有任何模组时的空表，省得调用方到处判空
    public static ClassIndex empty() {
        return new ClassIndex(Map.of(), Map.of(), Map.of(), List.of());
    }

    public List<ModSource> sources() {
        return sources;
    }

    // 按全限定类名解析；查不到就返回 null
    public Match resolve(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }
        return cache.computeIfAbsent(className, this::doResolve);
    }

    private Match doResolve(String className) {
        String normalized = normalize(className);
        if (normalized == null) {
            return null;
        }
        // 先按完整类名找，命中就是最确定的
        List<ModSource> exact = byClass.get(normalized);
        if (exact != null && !exact.isEmpty()) {
            return new Match(best(exact, normalized), normalized, 1.0);
        }
        // 退而求其次按包名一层层往上找
        String key = normalized;
        while (true) {
            int dot = key.lastIndexOf('.');
            if (dot < 0) {
                break;
            }
            key = key.substring(0, dot);
            // 绝不退到只有一段的包名（com、org 这种）：
            // 那样任何一个 com.* 的模组都会把别人的堆栈认领走
            if (key.indexOf('.') < 0) {
                break;
            }
            List<ModSource> pkgHits = byPackage.get(key);
            if (pkgHits != null && !pkgHits.isEmpty()) {
                // 包名越深越可信，但最高只给到 0.9，毕竟只是前缀匹配
                double depth = 0.55 + Math.min(0.3, 0.1 * key.split("\\.").length);
                return new Match(best(pkgHits, key), key, Math.min(0.9, depth));
            }
        }
        return null;
    }

    // 去掉 lambda、匿名类和数组后缀，让 a.b.C$1、a.b.C$$Lambda$12/0x... 和 a.b.C[]
    // 在精确匹配失败时都能归到 a.b.C 上
    private static String normalize(String raw) {
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        while (s.endsWith("[]")) {
            s = s.substring(0, s.length() - 2);
        }
        int lambda = s.indexOf("$$Lambda$");
        if (lambda > 0) {
            s = s.substring(0, lambda);
        }
        if (s.indexOf('/') > 0) {
            s = s.substring(0, s.indexOf('/'));
        }
        return s.isEmpty() ? null : s;
    }

    // 一个包被多个 jar 认领时，谁在这个包下的类多就归谁
    private ModSource best(List<ModSource> candidates, String key) {
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return candidates.stream()
                .max(Comparator.comparingInt(s -> classCounts.getOrDefault(key + "\u0000" + s.fileName(), 0)))
                .orElse(candidates.get(0));
    }

    // 某个 jar 在这个键下有多少个类，用来打平局
    public int classCount(String key, ModSource source) {
        return classCounts.getOrDefault(key + "\u0000" + source.fileName(), 0);
    }

    public int classCountTotal() {
        return byClass.size();
    }

    public int packageCountTotal() {
        return byPackage.size();
    }

    // 给日志文本里出现的裸包名找主人（不是栈帧，只能尽力猜）
    public List<ModSource> sourcesForPackageFragment(String fragment) {
        Set<ModSource> out = new LinkedHashSet<>();
        if (fragment == null || fragment.length() < 3) {
            return List.of();
        }
        String f = fragment.toLowerCase();
        for (Map.Entry<String, List<ModSource>> e : byPackage.entrySet()) {
            if (e.getKey().toLowerCase().contains(f)) {
                out.addAll(e.getValue());
            }
        }
        return new ArrayList<>(out);
    }

    // 建表

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, List<ModSource>> byClass = new LinkedHashMap<>();
        private final Map<String, List<ModSource>> byPackage = new LinkedHashMap<>();
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private final List<ModSource> sources = new ArrayList<>();

        // 加入一个模组文件，顺便读一遍它的类列表（只是读 jar，不写）
        public Builder add(ModSource source) {
            sources.add(source);
            for (String cls : source.classNames()) {
                byClass.computeIfAbsent(cls, k -> new ArrayList<>()).add(source);
                // 每一层父包都登记一次，解析时才能逐层往上看
                String key = cls;
                while (true) {
                    int dot = key.lastIndexOf('.');
                    if (dot < 0) {
                        break;
                    }
                    key = key.substring(0, dot);
                    byPackage.computeIfAbsent(key, k -> new ArrayList<>()).add(source);
                    counts.merge(key + "\u0000" + source.fileName(), 1, Integer::sum);
                }
            }
            return this;
        }

        public Builder addAll(List<ModSource> list) {
            list.forEach(this::add);
            return this;
        }

        public ClassIndex build() {
            return new ClassIndex(byClass, byPackage, counts, sources);
        }
    }
}
