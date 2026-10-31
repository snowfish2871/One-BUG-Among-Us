// 图形类模组的谱系表：钠、锂、iris 这些有一大堆分支和移植版，
// 认不出亲戚关系就会把「装了同一个东西的两个分支」当成正常组合。
package dev.oba.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GraphicsMods {

    // 一个模组属于哪一族、什么角色
    public enum Role {
        // 渲染优化主体（钠及其各分支）
        RENDER_OPTIMIZER,
        // 光影加载器（iris 及其各分支）
        SHADER_LOADER,
        // 配套的渲染扩展 / 附加组件
        RENDER_ADDON,
        // 其它性能类（锂、氪之类）
        OTHER_OPTIMIZER
    }

    // 一族里的一条分支。aliases 是同一个东西用过的别的 modId——
    // 实测见过 rubidium-extra 这个文件名下面声明的是 embeddium_extra，
    // 只认一个 id 就会漏掉
    public record Member(String modId, String displayName, String loader, Role role,
                         boolean isCanonical, String note, List<String> aliases) {

        public Member(String modId, String displayName, String loader, Role role,
                      boolean isCanonical, String note) {
            this(modId, displayName, loader, role, isCanonical, note, List.of());
        }

        // 这个 id 是不是本条分支声明的（含别名）
        public boolean matches(String id) {
            return modId.equals(id) || aliases.contains(id);
        }
    }

    // 一族模组。canonicalId 是这一族的"正统"，其它都是分支或移植。
    // packages 是这个族常见的类名前缀，用来在 modId 认不出来时兜底。
    public record Family(String key, String canonicalId, String displayName,
                         List<Member> members, List<String> packages) {
    }

    private GraphicsMods() {
    }

    // 钠这一族。Fabric 的钠、Forge/NeoForge 上的铷和 Embeddium、
    // 还有一批换名字重新发布的衍生版，全部互斥
    private static final Family SODIUM = new Family(
            "sodium", "sodium", "Sodium",
            List.of(
                    new Member("sodium", "Sodium", "Fabric / NeoForge", Role.RENDER_OPTIMIZER, true,
                            "原版，CaffeineMC 维护"),
                    new Member("rubidium", "Rubidium", "Forge", Role.RENDER_OPTIMIZER, false,
                            "钠在 Forge 上的早期移植，1.20.1 之后由 Embeddium 接手"),
                    new Member("embeddium", "Embeddium", "Forge / NeoForge", Role.RENDER_OPTIMIZER, false,
                            "钠在 Forge/NeoForge 上的主力移植"),
                    new Member("xenon", "Xenon", "Forge", Role.RENDER_OPTIMIZER, false,
                            "Embeddium 的衍生版"),
                    new Member("sodiumreforged", "Sodium Reforged", "Forge", Role.RENDER_OPTIMIZER, false,
                            "钠的另一个 Forge 移植"),
                    new Member("sodiumforge", "Sodium Forge", "Forge", Role.RENDER_OPTIMIZER, false,
                            "早期非官方移植"),
                    new Member("sodiumextra", "Sodium Extra", "Fabric / NeoForge", Role.RENDER_ADDON, false,
                            "钠的附加设置项，跟随钠"),
                    new Member("embeddiumextra", "Embeddium Extra", "Forge", Role.RENDER_ADDON, false,
                            "Embeddium 的附加设置项", List.of("embeddium_extra", "rubidium_extra")),
                    new Member("sodiumdynamiclights", "Sodium Dynamic Lights", "Fabric / NeoForge",
                            Role.RENDER_ADDON, false, "钠生态的动态光源，常与钠的其它附加组件冲突",
                            List.of("dynamiclights", "sodium_dynamic_lights")),
                    new Member("sodiumoptionsapi", "Sodium Options API", "Fabric / NeoForge",
                            Role.RENDER_ADDON, false, "钠的选项界面扩展，很多附加组件依赖它"),
                    new Member("reeses-sodium-options", "Reese's Sodium Options", "Fabric / NeoForge",
                            Role.RENDER_ADDON, false, "钠的选项界面重做"),
                    new Member("moreculling", "More Culling", "Fabric / NeoForge", Role.RENDER_ADDON, false,
                            "剔除优化，常和钠一起装")),
            List.of("net.caffeinemc.", "me.jellysquid.mods.sodium.", "org.embeddedt.embeddium.",
                    "me.jellysquid.mods.rubidium.", "com.moepus.", "toni.sodium"));

    // iris 这一族。光影加载器，必须配一个渲染优化主体才能工作，
    // 且只认自己那一支对应的优化模组
    private static final Family IRIS = new Family(
            "iris", "iris", "Iris Shaders",
            List.of(
                    new Member("iris", "Iris Shaders", "Fabric / NeoForge", Role.SHADER_LOADER, true,
                            "原版，需要钠"),
                    new Member("oculus", "Oculus", "Forge", Role.SHADER_LOADER, false,
                            "iris 在 Forge 上的移植，需要 Embeddium 或铷"),
                    new Member("neoculus", "NeOculus", "NeoForge", Role.SHADER_LOADER, false,
                            "iris 在 NeoForge 上的移植，需要 Embeddium"),
                    new Member("irisflw", "Iris Flywheel Compat", "Forge / NeoForge", Role.RENDER_ADDON, false,
                            "让 iris 与 Flywheel 共存，缺了会让 Create 一类模组渲染出错")),
            List.of("net.irisshaders.", "net.coderbot.iris.", "com.gtnewhorizons.angelica.",
                    "org.taumc.celeritas."));

    // 其它性能类，用来给"装了哪些优化模组"这个上下文补全
    private static final Family LITHIUM = new Family(
            "lithium", "lithium", "Lithium",
            List.of(
                    new Member("lithium", "Lithium", "Fabric / NeoForge", Role.OTHER_OPTIMIZER, true,
                            "服务端与通用逻辑优化"),
                    new Member("radium", "Radium", "Forge", Role.OTHER_OPTIMIZER, false,
                            "锂在 Forge 上的移植"),
                    new Member("canary", "Canary", "Forge", Role.OTHER_OPTIMIZER, false,
                            "锂在 Forge 上的另一支移植"),
                    new Member("embeddiumplus", "Embeddium++", "Forge", Role.OTHER_OPTIMIZER, false,
                            "Embeddium 的增强整合")),
            List.of("net.caffeinemc.mods.lithium.", "me.jellysquid.mods.lithium."));

    private static final Family KRYPTON = new Family(
            "krypton", "krypton", "Krypton",
            List.of(
                    new Member("krypton", "Krypton", "Fabric / NeoForge", Role.OTHER_OPTIMIZER, true,
                            "网络栈优化"),
                    new Member("kryptonreforged", "Krypton Reforged", "Forge", Role.OTHER_OPTIMIZER, false,
                            "氪在 Forge 上的移植")),
            List.of("me.steinborn.krypton."));

    private static final Family PHOSPHOR = new Family(
            "phosphor", "phosphor", "Phosphor",
            List.of(
                    new Member("phosphor", "Phosphor", "Fabric", Role.OTHER_OPTIMIZER, true,
                            "光照引擎优化，已被钠生态取代"),
                    new Member("phosphorcreate", "Phosphor Create", "Forge", Role.OTHER_OPTIMIZER, false,
                            "磷的 Forge 衍生")),
            List.of("net.caffeinemc.phosphor."));

    private static final List<Family> FAMILIES = List.of(SODIUM, IRIS, LITHIUM, KRYPTON, PHOSPHOR);

    public static List<Family> families() {
        return FAMILIES;
    }

    // 这个 modId 属于哪一族；认不出来返回 null
    public static Family familyOf(String modId) {
        if (modId == null || modId.isBlank()) {
            return null;
        }
        String id = modId.toLowerCase(Locale.ROOT);
        for (Family f : FAMILIES) {
            for (Member m : f.members()) {
                if (m.matches(id)) {
                    return f;
                }
            }
        }
        return null;
    }

    // 从类名反查属于哪一族，modId 认不出来时兜底
    public static Family familyOfPackage(String className) {
        if (className == null) {
            return null;
        }
        for (Family f : FAMILIES) {
            for (String p : f.packages()) {
                if (className.startsWith(p)) {
                    return f;
                }
            }
        }
        return null;
    }

    public static Member memberOf(String modId) {
        if (modId == null) {
            return null;
        }
        String id = modId.toLowerCase(Locale.ROOT);
        for (Family f : FAMILIES) {
            for (Member m : f.members()) {
                if (m.matches(id)) {
                    return m;
                }
            }
        }
        return null;
    }

    // 一个模组文件里所有能认出来的族。
    // 认包名要遍历全部类名，而报告里会反复问同一个模组，所以缓存一下
    private static final Map<ModSource, Set<Family>> FAMILY_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static Set<Family> familiesOf(ModSource s) {
        return FAMILY_CACHE.computeIfAbsent(s, GraphicsMods::scanFamilies);
    }

    private static Set<Family> scanFamilies(ModSource s) {
        Set<Family> out = new LinkedHashSet<>();
        for (String id : s.modIds()) {
            Family f = familyOf(id);
            if (f != null) {
                out.add(f);
            }
        }
        if (out.isEmpty()) {
            // modId 认不出来就看包名。只扫前若干个类就够了——
            // 一个模组的包结构是均匀的，看到第一个能认出来的就可以停
            for (String cls : s.classNames()) {
                Family f = familyOfPackage(cls);
                if (f != null) {
                    out.add(f);
                    break;
                }
            }
        }
        return out;
    }

    // 是不是图形类模组
    public static boolean isGraphics(ModSource s) {
        return !familiesOf(s).isEmpty();
    }

    // 同一族里装了不止一个**互斥**的分支，这是明确的冲突。
    // 钠、铷、Embeddium、Xenon 换名字但干的是同一件事，同时装必然打架。
    // 但附加组件（Sodium Extra、动态光源之类）不算——它们本来就是配合主体用的，
    // 和主体共存是正常情况，和另一个附加组件共存通常也没事。
    public static List<Conflict> findFamilyConflicts(List<ModSource> sources) {
        List<Conflict> out = new ArrayList<>();
        for (Family f : FAMILIES) {
            List<ModSource> hits = new ArrayList<>();
            for (ModSource s : sources) {
                if (familiesOf(s).contains(f) && isExclusive(s)) {
                    hits.add(s);
                }
            }
            // 一个 jar 里打包了多个同族 id 不算冲突，那是整合发布
            if (hits.size() < 2) {
                continue;
            }
            List<String> names = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            for (ModSource s : hits) {
                names.add(s.fileName());
                for (String id : s.modIds()) {
                    Member m = memberOf(id);
                    if (m != null && (m.role() == Role.RENDER_OPTIMIZER
                            || m.role() == Role.SHADER_LOADER)) {
                        ids.add(id);
                    }
                }
            }
            out.add(new Conflict(f, List.copyOf(hits), List.copyOf(names),
                    List.copyOf(new LinkedHashSet<>(ids))));
        }
        return out;
    }

    // 这个模组是不是"占着主体位置"的那种：渲染优化主体或光影加载器
    private static boolean isExclusive(ModSource s) {
        for (String id : s.modIds()) {
            Member m = memberOf(id);
            if (m != null && (m.role() == Role.RENDER_OPTIMIZER || m.role() == Role.SHADER_LOADER)) {
                return true;
            }
        }
        // modId 认不出来时按包名判：附加组件一般不会带主体的包
        for (String cls : s.classNames()) {
            Family f = familyOfPackage(cls);
            if (f != null) {
                return true;
            }
            break;
        }
        return false;
    }

    // 光影加载器缺了它要的渲染主体。
    // 实机上 oculus 没有 Embeddium、iris 没有钠，表现是一进游戏就崩，
    // 而且日志里只有一句 NoClassDefFoundError，很难看出来是缺前置。
    public static List<MissingPartner> findMissingPartners(List<ModSource> sources,
                                                           Map<String, ModSource> byModId) {
        List<MissingPartner> out = new ArrayList<>();
        boolean hasSodiumFamily = hasAny(sources, SODIUM);
        boolean hasIrisFamily = hasAny(sources, IRIS);
        if (!hasIrisFamily) {
            return out;
        }
        if (!hasSodiumFamily) {
            // 装了光影加载器但一个渲染优化主体都没有
            for (ModSource s : sources) {
                if (!familiesOf(s).contains(IRIS)) {
                    continue;
                }
                if (memberOf(firstId(s)) == null
                        || memberOf(firstId(s)).role() != Role.SHADER_LOADER) {
                    continue;
                }
                out.add(new MissingPartner(s, wantedPartner(firstId(s))));
            }
        }
        return out;
    }

    // 这个光影加载器需要哪一族配合
    public static String wantedPartner(String shaderLoaderId) {
        if (shaderLoaderId == null) {
            return "sodium";
        }
        return switch (shaderLoaderId.toLowerCase(Locale.ROOT)) {
            case "oculus", "neoculus" -> "embeddium 或 rubidium";
            default -> "sodium";
        };
    }

    private static boolean hasAny(List<ModSource> sources, Family f) {
        for (ModSource s : sources) {
            if (familiesOf(s).contains(f)) {
                return true;
            }
        }
        return false;
    }

    private static String firstId(ModSource s) {
        List<String> ids = s.modIds();
        return ids.isEmpty() ? "" : ids.get(0);
    }

    public record Conflict(Family family, List<ModSource> sources, List<String> fileNames,
                           List<String> memberIds) {
    }

    public record MissingPartner(ModSource shaderLoader, String wanted) {
    }
}
