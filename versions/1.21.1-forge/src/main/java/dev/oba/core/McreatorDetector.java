// 识别 MCreator 生成的模组：多个特征一起打分，命中哪些理由都会列出来。
package dev.oba.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class McreatorDetector {

    // 每个特征给多少分。分值故意压得不高，宁可漏报也别乱报
    private static final int P_PACKAGE_ROOT = 6;
    private static final int P_PROCEDURES_PACKAGE = 4;
    private static final int P_MODELEMENT = 4;
    private static final int P_VARIABLES = 2;
    private static final int P_METADATA_TEXT = 4;
    private static final int P_FILENAME = 3;
    private static final int P_CONTENT_BANNER = 5;

    // 支撑性特征，只在已经确认是 MCreator 之后才加分，本身不足以触发判定
    private static final int P_MODEL_CLASSES = 2;
    private static final int P_INIT_CLASS = 2;
    private static final int P_STRUCTURE = 2;

    // 低于这个分数就不认为是 MCreator 生成的。
    public static final int MIN_SCORE = 6;

    private McreatorDetector() {
    }

    // 对一个 jar 或目录跑一遍所有特征
    public static Optional<Diagnosis.McreatorHit> detect(ModSource source) {
        List<Diagnosis.Evidence> reasons = new ArrayList<>();
        int score = 0;

        List<String> classNames = source.classNames();
        List<String> entries = source.entries();

        // 1. 决定性的特征：MCreator 特有的包名和生成物
        boolean mcreatorPackage = false;
        String packageRoot = findPackageRoot(classNames);
        if (packageRoot != null) {
            if (packageRoot.startsWith("net.mcreator") || packageRoot.contains(".mcreator")) {
                mcreatorPackage = true;
                score += P_PACKAGE_ROOT;
                reasons.add(Diagnosis.Evidence.of("mcreator.package_root",
                        "mc.package", java.util.List.of(packageRoot), P_PACKAGE_ROOT));
            }
        }
        if (hasPackageSegment(classNames, "procedures")) {
            score += P_PROCEDURES_PACKAGE;
            reasons.add(Diagnosis.Evidence.of("mcreator.procedures_package",
                    "mc.procedures", java.util.List.of(), P_PROCEDURES_PACKAGE));
        }
        int modElementClasses = countMatchingClasses(classNames, "ModElement");
        if (modElementClasses > 0) {
            score += P_MODELEMENT;
            reasons.add(Diagnosis.Evidence.of("mcreator.modelement",
                    "mc.modelement", java.util.List.of(modElementClasses), P_MODELEMENT));
        }
        if (hasPackageSegment(classNames, "variables")) {
            score += P_VARIABLES;
            reasons.add(Diagnosis.Evidence.of("mcreator.variables_package",
                    "mc.variables", java.util.List.of(), P_VARIABLES));
        }

        // 2. 描述文件里自己写着 mcreator
        boolean metadataSays = false;
        for (ModSource.Descriptor d : source.descriptors()) {
            String blob = String.join(" ", nullSafe(d.displayName()), nullSafe(d.description()),
                    nullSafe(d.credits()), nullSafe(d.authors()), nullSafe(d.modId()),
                    nullSafe(d.displayUrl())).toLowerCase(Locale.ROOT);
            if (blob.contains("mcreator")) {
                metadataSays = true;
                score += P_METADATA_TEXT;
                reasons.add(Diagnosis.Evidence.of("mcreator.metadata",
                        "mc.metadata", java.util.List.of(), P_METADATA_TEXT));
                break;
            }
        }

        // 3. 文件名
        if (source.fileName().toLowerCase(Locale.ROOT).contains("mcreator")) {
            score += P_FILENAME;
            reasons.add(Diagnosis.Evidence.of("mcreator.filename",
                    "mc.filename", java.util.List.of(), P_FILENAME));
        }

        // 4. 资源里留着生成器的标记，这个也很硬
        boolean banner = false;
        String bannerText = probeForBanner(source);
        if (bannerText != null) {
            banner = true;
            score += P_CONTENT_BANNER;
            reasons.add(Diagnosis.Evidence.of("mcreator.banner",
                    "mc.banner", java.util.List.of(bannerText), P_CONTENT_BANNER));
        }

        // 到这里为止都还是"可能"。下面这些特征手写模组也常中，
        // 只有在上面已经出现 MCreator 特有痕迹时才作为佐证加分，
        // 否则 Alex 的生物、JEI 这类普通模组会被大量误报。
        boolean circumstantial = mcreatorPackage || metadataSays || banner
                || modElementClasses > 0 || score >= MIN_SCORE;
        if (!circumstantial) {
            return Optional.empty();
        }

        int modelClasses = countMatchingClasses(classNames, "Model") + countMatchingClasses(classNames, "Renderer");
        if (modelClasses >= 2 && mcreatorPackage) {
            score += P_MODEL_CLASSES;
            reasons.add(Diagnosis.Evidence.of("mcreator.model_classes",
                    "mc.models", java.util.List.of(modelClasses), P_MODEL_CLASSES));
        }
        if (mcreatorPackage && classNames.stream().anyMatch(c -> c.endsWith(".ModEntities")
                || c.endsWith(".ModItems") || c.endsWith(".ModBlocks")
                || c.endsWith(".ModTabs") || c.endsWith(".ModSounds")
                || c.endsWith(".ModPaintings") || c.endsWith(".ModParticles"))) {
            score += P_INIT_CLASS;
            reasons.add(Diagnosis.Evidence.of("mcreator.registry_classes",
                    "mc.registry", java.util.List.of(), P_INIT_CLASS));
        }
        if (mcreatorPackage && looksLikeGeneratedStructure(entries, source)) {
            score += P_STRUCTURE;
            reasons.add(Diagnosis.Evidence.of("mcreator.structure",
                    "mc.structure", java.util.List.of(), P_STRUCTURE));
        }

        String mcreatorVersion = probeForMetadataVersion(source);
        if (mcreatorVersion != null) {
            reasons.add(Diagnosis.Evidence.of("mcreator.version",
                    "mc.generatorVersion", java.util.List.of(mcreatorVersion), 0));
        }

        if (score < MIN_SCORE) {
            return Optional.empty();
        }
        return Optional.of(new Diagnosis.McreatorHit(source, score, List.copyOf(reasons)));
    }

    // 一批模组里所有像 MCreator 的
    public static List<Diagnosis.McreatorHit> detectAll(List<ModSource> sources) {
        List<Diagnosis.McreatorHit> out = new ArrayList<>();
        for (ModSource s : sources) {
            detect(s).ifPresent(out::add);
        }
        out.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return out;
    }

    // 各项特征

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    // 取类的包名前两段当"包根"，MCreator 的包根就是 net.mcreator
    private static String findPackageRoot(List<String> classNames) {
        Set<String> roots = new LinkedHashSet<>();
        for (String c : classNames) {
            int dot = c.lastIndexOf('.');
            if (dot <= 0) {
                continue;
            }
            String pkg = c.substring(0, dot);
            String[] segs = pkg.split("\\.");
            int take = Math.min(2, segs.length);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < take; i++) {
                if (i > 0) {
                    sb.append('.');
                }
                sb.append(segs[i]);
            }
            roots.add(sb.toString());
        }
        // 有 mcreator 味道的包根优先，别的只是备选
        for (String r : roots) {
            if (r.startsWith("net.mcreator") || r.contains(".mcreator")) {
                return r;
            }
        }
        return roots.isEmpty() ? null : roots.iterator().next();
    }

    private static boolean hasPackageSegment(List<String> classNames, String segment) {
        String needle = "." + segment + ".";
        for (String c : classNames) {
            if (c.startsWith(segment + ".") || c.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    // 数一数有多少个类以某个后缀结尾，内部类按外层类算
    private static int countMatchingClasses(List<String> classNames, String simpleSuffix) {
        int n = 0;
        for (String c : classNames) {
            int dot = c.lastIndexOf('.');
            String simple = dot < 0 ? c : c.substring(dot + 1);
            int dollar = simple.indexOf('$');
            if (dollar > 0) {
                simple = simple.substring(0, dollar);
            }
            if (simple.endsWith(simpleSuffix)) {
                n++;
            }
        }
        return n;
    }

    // 模型 JSON 和成对的 procedure 类同时出现，是生成器导出的典型布局
    private static boolean looksLikeGeneratedStructure(List<String> entries, ModSource source) {
        boolean hasAssets = entries.stream().anyMatch(e -> e.startsWith("assets/") && e.contains("/models/"));
        boolean hasData = entries.stream().anyMatch(e -> e.startsWith("data/") && e.contains("/recipes/"));
        boolean hasLang = entries.stream().anyMatch(e -> e.contains("/lang/") && e.endsWith(".json"));
        int procedureClasses = countMatchingClasses(source.classNames(), "Procedure");
        return hasAssets && (hasData || hasLang) && procedureClasses >= 2;
    }

    // 抽几个小的文本资源看看有没有生成标记，不往磁盘上解压任何东西
    private static String probeForBanner(ModSource source) {
        if (source.isDirectory()) {
            return probeDirectory(source, 24);
        }
        try (JarFile jar = new JarFile(source.path().toFile())) {
            int inspected = 0;
            var it = jar.entries();
            while (it.hasMoreElements() && inspected < 40) {
                JarEntry e = it.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                String name = e.getName().toLowerCase(Locale.ROOT);
                if (!(name.endsWith(".json") || name.endsWith(".mcmeta") || name.endsWith(".txt"))) {
                    continue;
                }
                // 大文件跳过，免得为了一个标记读进来一堆东西
                if (e.getSize() > 64 * 1024) {
                    continue;
                }
                inspected++;
                try (InputStream in = jar.getInputStream(e)) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    String hit = bannerIn(text);
                    if (hit != null) {
                        return hit;
                    }
                } catch (IOException ignored) {
                    // 这个条目读不了就算了，继续看下一个
                }
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private static String probeDirectory(ModSource source, int limit) {
        try (var stream = java.nio.file.Files.walk(source.path())) {
            List<java.nio.file.Path> files = stream
                    .filter(java.nio.file.Files::isRegularFile)
                    .limit(limit)
                    .toList();
            for (java.nio.file.Path p : files) {
                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!(name.endsWith(".json") || name.endsWith(".mcmeta") || name.endsWith(".txt"))) {
                    continue;
                }
                try {
                    if (java.nio.file.Files.size(p) > 64 * 1024) {
                        continue;
                    }
                    String hit = bannerIn(java.nio.file.Files.readString(p));
                    if (hit != null) {
                        return hit;
                    }
                } catch (IOException ignored) {
                    // 继续看下一个
                }
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }

    private static String bannerIn(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf("generated by mcreator");
        if (idx >= 0) {
            // 只取到标记本身，后面可能跟个版本号，但别把周围的 JSON 标点也带上
            int end = idx + "generated by mcreator".length();
            while (end < text.length() && (Character.isDigit(text.charAt(end))
                    || text.charAt(end) == '.' || text.charAt(end) == ' '
                    || text.charAt(end) == '-' || text.charAt(end) == '_')) {
                end++;
            }
            return text.substring(idx, end).trim();
        }
        if (lower.contains("mcreator") && lower.contains("_mcreator_")) {
            return Lang.get("mc.banner.mark");
        }
        return null;
    }

    // 找找有没有记录生成器版本的文件
    private static String probeForMetadataVersion(ModSource source) {
        for (String entry : source.entries()) {
            String lower = entry.toLowerCase(Locale.ROOT);
            if (lower.endsWith("mcreator.json") || lower.contains("mcreator_workspace")) {
                return entry;
            }
        }
        return null;
    }
}
