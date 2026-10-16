// 读日志和崩溃报告：抽环境信息、跑 25 条常见问题规则、顺便解析堆栈。
package dev.oba.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public final class LogAnalyzer {

    private record Hit(int line, String excerpt, Matcher matcher) {
    }

    private static final int EXCERPT_MAX = 400;

    private LogAnalyzer() {
    }

    public static final class Scan {
        private final StackTraceParser.Parsed parsed;
        private final List<Diagnosis.KnownIssue> issues;
        private final Map<String, String> environment;
        private final String path;
        private final long size;
        // F3+C 手动触发的调试崩溃，不是模组的问题
        private final boolean manualCrash;

        Scan(StackTraceParser.Parsed parsed, List<Diagnosis.KnownIssue> issues,
             Map<String, String> environment, String path, long size, boolean manualCrash) {
            this.parsed = parsed;
            this.issues = issues;
            this.environment = environment;
            this.path = path;
            this.size = size;
            this.manualCrash = manualCrash;
        }

        public StackTraceParser.Parsed parsed() {
            return parsed;
        }

        public List<Diagnosis.KnownIssue> issues() {
            return issues;
        }

        public Map<String, String> environment() {
            return environment;
        }

        public String path() {
            return path;
        }

        public long size() {
            return size;
        }

        public boolean manualCrash() {
            return manualCrash;
        }
    }

    // 读文件

    // 判断文件大概是什么编码：取开头一段试 UTF-8，失败就当系统默认
    private static java.nio.charset.Charset detector(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(8192);
            try {
                StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(head));
                return StandardCharsets.UTF_8;
            } catch (CharacterCodingException notUtf8) {
                return java.nio.charset.Charset.defaultCharset();
            }
        } catch (IOException e) {
            return StandardCharsets.UTF_8;
        }
    }

    // 读日志或崩溃报告，.gz 也认
    public static String readLogText(Path file) throws IOException {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".gz")) {
            try (InputStream in = new GZIPInputStream(Files.newInputStream(file))) {
                return decode(in.readAllBytes());
            }
        }
        return decode(Files.readAllBytes(file));
    }

    // 按内容猜编码。中文 Windows 上游戏写的日志是 GBK，直接当 UTF-8 读会炸
    // （Input length = 1），连"读不了"都说不清楚。
    // 先试 UTF-8；不像 UTF-8 就试几个常见编码，挑乱码最少的那个。
    // 不能只退回系统默认编码——在一台默认编码是 UTF-8 的机器上，
    // 拿到的 GBK 日志还是花屏。
    public static String decode(byte[] data) {
        if (data.length == 0) {
            return "";
        }
        // BOM 优先
        if (data.length >= 3 && (data[0] & 0xFF) == 0xEF
                && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
            return new String(data, 3, data.length - 3, StandardCharsets.UTF_8);
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException notUtf8) {
            // 不是 UTF-8。挨个试候选编码，取乱码最少的结果
            String best = null;
            int bestScore = Integer.MAX_VALUE;
            for (java.nio.charset.Charset cs : candidates()) {
                String text;
                try {
                    text = new String(data, cs);
                } catch (RuntimeException ignored) {
                    continue;
                }
                int score = mojibakeScore(text);
                if (score < bestScore) {
                    bestScore = score;
                    best = text;
                }
                if (score == 0) {
                    break;
                }
            }
            return best != null ? best
                    : new String(data, java.nio.charset.Charset.defaultCharset());
        }
    }

    // 候选编码：系统默认、GBK、GB18030，最后 Latin-1 兜底
    private static List<java.nio.charset.Charset> candidates() {
        List<java.nio.charset.Charset> out = new ArrayList<>();
        out.add(java.nio.charset.Charset.defaultCharset());
        for (String name : List.of("GBK", "GB18030", "windows-1252")) {
            try {
                java.nio.charset.Charset cs = java.nio.charset.Charset.forName(name);
                if (!out.contains(cs)) {
                    out.add(cs);
                }
            } catch (RuntimeException ignored) {
                // 这个 JVM 不支持就跳过
            }
        }
        return out;
    }

    // 数一数解出来有多少乱码痕迹：替换符、Latin-1 补集里的控制字符，
    // 以及"UTF-8 字节被按 Latin-1 读"留下的 Ã Â 这类字符。
    // 分越低越像正确编码。
    private static int mojibakeScore(String text) {
        int bad = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\uFFFD') {
                bad += 10;
            } else if (c >= '\u0080' && c <= '\u009F') {
                bad += 5;
            } else if (c == '\u00C3' || c == '\u00C2' || c == '\u00E2' || c == '\u00E5') {
                bad += 2;
            }
        }
        return bad;
    }

    // 大日志只读结尾那段
    public static String readTail(Path file, long maxBytes) throws IOException {
        long size = Files.size(file);
        if (size <= maxBytes) {
            return readLogText(file);
        }
        byte[] buf = new byte[(int) maxBytes];
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file.toFile(), "r")) {
            raf.seek(size - maxBytes);
            raf.readFully(buf);
        }
        String text = decode(buf);
        int nl = text.indexOf('\n');
        return nl >= 0 ? text.substring(nl + 1) : text;
    }

    // 逐行读最后 count 行，不把整个文件塞进内存
    public static List<String> tailLines(Path file, int count) throws IOException {
        java.util.ArrayDeque<String> deque = new java.util.ArrayDeque<>(count + 1);
        // 同样按内容猜编码，不能假定 UTF-8
        try (BufferedReader r = new BufferedReader(
                new java.io.InputStreamReader(Files.newInputStream(file), detector(file)))) {
            String line;
            while ((line = r.readLine()) != null) {
                deque.addLast(line);
                if (deque.size() > count) {
                    deque.removeFirst();
                }
            }
        }
        return new ArrayList<>(deque);
    }

    // 扫描

    // 对一份文档跑完整套规则
    public static Scan scan(String text, String label, long size, ClassIndex index,
                            Map<String, ModSource> byModId) {
        StackTraceParser.Parsed parsed = StackTraceParser.parse(text);
        List<String> lines = List.of(text.split("\r\n|\n|\r", -1));
        List<Diagnosis.KnownIssue> issues = new ArrayList<>();
        Map<String, String> env = new LinkedHashMap<>();

        captureEnvironment(lines, env);

        Set<String> seen = new LinkedHashSet<>();
        for (Rule rule : RULES) {
            for (Hit hit : find(lines, rule.pattern())) {
                Matcher m = hit.matcher();
                List<String> groups = new ArrayList<>();
                for (int g = 1; g <= m.groupCount(); g++) {
                    groups.add(m.group(g));
                }
                String modId = rule.modIdGroup() > 0 && rule.modIdGroup() <= m.groupCount()
                        ? m.group(rule.modIdGroup()) : null;
                ModSource blamed = null;
                List<String> suspects = new ArrayList<>();
                if (modId != null && byModId != null) {
                    blamed = byModId.get(modId.trim());
                    if (blamed == null) {
                        // 这个 id 可能对应一个根本没装的模组，这正是要报告的情况
                        suspects.add(modId.trim());
                    }
                }
                if (blamed == null && index != null && rule.packageGroup() > 0
                        && rule.packageGroup() <= m.groupCount()) {
                    String pkg = m.group(rule.packageGroup());
                    if (pkg != null) {
                        String cls = pkg.trim();
                        ClassIndex.Match match = index.resolve(cls);
                        if (match != null) {
                            blamed = match.source();
                        } else {
                            for (ModSource s : index.sourcesForPackageFragment(rootOf(cls))) {
                                suspects.add(s.fileName());
                            }
                        }
                    }
                }
                String key = rule.id() + "|" + hit.excerpt();
                if (!seen.add(key)) {
                    continue;
                }
                // 命中的那一行本身就是最有用的信息，规则只负责提供措辞
                issues.add(new Diagnosis.KnownIssue(
                        rule.id(), rule.severity(), rule.key(), rule.key() + ".title",
                        "rule.match.detail", List.of(hit.excerpt()),
                        blamed, hit.line(), List.copyOf(suspects)));
            }
        }
        boolean manualCrash = text.contains("Manually triggered debug crash");
        return new Scan(parsed, issues, env, label, size, manualCrash);
    }

    public static Scan scanFile(Path file, ClassIndex index, Map<String, ModSource> byModId) throws IOException {
        String text = readLogText(file);
        long size;
        try {
            size = Files.size(file);
        } catch (IOException e) {
            size = text.length();
        }
        return scan(text, file.toString(), size, index, byModId);
    }

    private static String rootOf(String className) {
        int dot = className.indexOf('.');
        if (dot < 0) {
            return className;
        }
        int second = className.indexOf('.', dot + 1);
        return second < 0 ? className : className.substring(0, second);
    }

    private static void captureEnvironment(List<String> lines, Map<String, String> env) {
        Pattern loadingMc = Pattern.compile("Loading Minecraft\\s+(\\S+)\\s+with NeoForge\\s+(\\S+)");
        Pattern forgeVer = Pattern.compile("Forge version\\s+([\\w.\\-+]+)");
        Pattern javaVer = Pattern.compile("Java version:\\s*([^,]+),.*?Java VM Version:\\s*([^,]+)");
        Pattern os = Pattern.compile("OS:\\s*(.+?)\\s*$");
        Pattern memory = Pattern.compile("JVM Flags:.*?-Xmx(\\S+)");
        Pattern cpu = Pattern.compile("CPU:\\s*(.+?)\\s*$");
        int checked = 0;
        for (String line : lines) {
            if (++checked > 4000) {
                break;
            }
            matchInto(loadingMc, line, env, Lang.get("env.minecraft"), m -> m.group(1) + " / " + m.group(2));
            matchInto(forgeVer, line, env, Lang.get("env.neoforge"), m -> m.group(1));
            matchInto(javaVer, line, env, Lang.get("env.java"), m -> m.group(1).trim() + " (" + m.group(2).trim() + ")");
            matchInto(os, line, env, Lang.get("env.os"), m -> m.group(1).trim());
            matchInto(memory, line, env, Lang.get("env.memory"), m -> m.group(1));
            matchInto(cpu, line, env, Lang.get("env.cpu"), m -> m.group(1).trim());
        }
    }

    private static void matchInto(Pattern p, String line, Map<String, String> env, String key,
                                 Function<Matcher, String> value) {
        if (env.containsKey(key)) {
            return;
        }
        Matcher m = p.matcher(line);
        if (m.find()) {
            env.put(key, value.apply(m));
        }
    }

    private static List<Hit> find(List<String> lines, Pattern pattern) {
        List<Hit> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty() || isCrashReportBoilerplate(line)) {
                continue;
            }
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                String excerpt = line.trim();
                if (excerpt.length() > EXCERPT_MAX) {
                    excerpt = excerpt.substring(0, EXCERPT_MAX) + "…";
                }
                out.add(new Hit(i + 1, excerpt, m));
                if (out.size() >= 12) {
                    break;
                }
            }
        }
        return out;
    }

    // 崩溃报告里只是"描述"这次崩溃的结构性文字。拿规则去匹配它们只会产生噪声：
    // Description: Mod loading failed 是报告自己的表头，不是游戏真的记了一条错误
    private static boolean isCrashReportBoilerplate(String line) {
        String t = line.strip();
        if (t.isEmpty()) {
            return true;
        }
        return t.startsWith("Description:")
                || t.startsWith("Time:")
                || t.startsWith("//")
                || t.startsWith("----")
                || t.startsWith("A detailed walkthrough")
                || t.startsWith("-- Head --")
                || t.startsWith("-- System Details --")
                || t.startsWith("-- Mod List --")
                || t.startsWith("-- Affected level --")
                || t.startsWith("Stacktrace:")
                || t.startsWith("Suspected Mods:")
                || t.startsWith("Minecraft Version:")
                || t.startsWith("NeoForge Version:")
                || t.startsWith("Mod List:")
                || t.startsWith("Loaded Mods:");
    }

    // 规则表

    // 一条检测规则。
    // id 是稳定标识，JSON 输出里也用；key 是文案表的前缀，.title 和 .advice 由它派生；
    // modIdGroup 是第几个捕获组里放着被点名的 modId（0 表示没有），
    // packageGroup 同理，放着需要解析归属的类名
    private record Rule(String id, String key, Diagnosis.Severity severity, Pattern pattern,
                        int modIdGroup, int packageGroup) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("neoforge.missing_dependency", "rule.missing_dependency", Diagnosis.Severity.FATAL,
                    Pattern.compile("Mod (\\S+) requires (\\S+?)(?:\\s+([\\d.\\-+\\[\\],)]+))?\\s*(?:that is missing|which is missing)"),
                    1, 0),

            new Rule("neoforge.missing_dependency_generic", "rule.missing_dependency_generic", Diagnosis.Severity.FATAL,
                    Pattern.compile("Missing or unsupported mandatory dependencies?:\\s*(.+)"),
                    0, 0),

            new Rule("neoforge.incompatible_mod", "rule.incompatible_mod", Diagnosis.Severity.FATAL,
                    Pattern.compile("Mod (\\S+) is incompatible with (\\S+)"),
                    1, 0),

            new Rule("neoforge.dependency_version", "rule.dependency_version", Diagnosis.Severity.ERROR,
                    Pattern.compile("Mod (\\S+) requires (\\S+) (\\S+) but (?:only )?(\\S+) is present"),
                    1, 0),

            new Rule("fml.duplicate_mod", "rule.duplicate_mod", Diagnosis.Severity.FATAL,
                    Pattern.compile("Duplicate mods? found for (\\S+)"),
                    1, 0),

            new Rule("fml.duplicate_mod_alt", "rule.duplicate_mod", Diagnosis.Severity.FATAL,
                    Pattern.compile("Found duplicate mod (\\S+)"),
                    1, 0),

            new Rule("mixin.apply_failed", "rule.mixin_apply", Diagnosis.Severity.FATAL,
                    Pattern.compile("Mixin apply for mod \\(?([^\\s)]+)\\)? from ([^\\s]+) failed"),
                    1, 0),

            new Rule("mixin.apply_failed_alt", "rule.mixin_apply_alt", Diagnosis.Severity.FATAL,
                    Pattern.compile("Mixin apply(?: failed)? for ([\\w.$]+) from ([\\w.$]+\\.mixins\\.json) failed"),
                    0, 0),

            new Rule("mixin.transformer_error", "rule.mixin_transform", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:MixinTransformerError|MixinApplyError|MixinPreProcessorException|InvalidMixinException)"),
                    0, 0),

            new Rule("mixin.config_failed", "rule.mixin_config", Diagnosis.Severity.FATAL,
                    Pattern.compile("Mixin config (\\S+) (?:failed|is invalid|could not be)"),
                    0, 0),

            new Rule("mixin.crash", "rule.mixin_apply", Diagnosis.Severity.FATAL,
                    Pattern.compile("Mixin (?:apply|transformation) (?:failed|error).*?(?:in|for) (\\S+)"),
                    0, 0),

            new Rule("class.noclassdef", "rule.noclassdef", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:java\\.lang\\.)?NoClassDefFoundError: (\\S+)"),
                    0, 1),

            new Rule("class.classnotfound", "rule.classnotfound", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:java\\.lang\\.)?ClassNotFoundException: (\\S+)"),
                    0, 1),

            new Rule("class.nosuchmethod", "rule.nosuchmethod", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:java\\.lang\\.)?NoSuchMethodError: (?:'?)([\\w.$]+)\\.([\\w$<>]+)"),
                    0, 1),

            new Rule("class.nosuchfield", "rule.nosuchfield", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:java\\.lang\\.)?NoSuchFieldError: ([\\w.$]+)"),
                    0, 1),

            new Rule("registry.missing", "rule.registry_missing", Diagnosis.Severity.ERROR,
                    Pattern.compile("Missing registry entries?[^:]*:\\s*(.+)"),
                    0, 0),

            new Rule("registry.duplicate", "rule.registry_duplicate", Diagnosis.Severity.ERROR,
                    Pattern.compile("Duplicate registration|already registered|Duplicate entry for (\\S+)"),
                    0, 0),

            new Rule("datapack.load_failed", "rule.datapack", Diagnosis.Severity.ERROR,
                    Pattern.compile("Failed to load (?:data ?pack|advancements?|recipes?|loot tables?)", Pattern.CASE_INSENSITIVE),
                    0, 0),

            new Rule("resourcepack.load_failed", "rule.resourcepack", Diagnosis.Severity.WARN,
                    Pattern.compile("Failed to load resource pack|Unable to load resource pack", Pattern.CASE_INSENSITIVE),
                    0, 0),

            new Rule("jarjar.missing", "rule.jarjar", Diagnosis.Severity.FATAL,
                    Pattern.compile("Unable to find (?:jar|file) (\\S+) in (\\S+)"),
                    0, 0),

            new Rule("jar.corrupt", "rule.corruptjar", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:java\\.util\\.zip\\.ZipException|invalid LOC header|zip END header not found|Error reading (\\S+\\.jar))"),
                    0, 0),

            new Rule("mixinextras.version", "rule.mixinextras", Diagnosis.Severity.ERROR,
                    Pattern.compile("MixinExtras (\\S+) is too old|requires MixinExtras (\\S+)"),
                    0, 0),

            new Rule("memory.outofmemory", "rule.oom", Diagnosis.Severity.FATAL,
                    Pattern.compile("java\\.lang\\.OutOfMemoryError(?:\\s*:\\s*(.+))?"),
                    0, 0),

            new Rule("watchdog.timeout", "rule.watchdog", Diagnosis.Severity.FATAL,
                    Pattern.compile("A single server tick took (\\S+) seconds|Server thread dump|Watching Server thread"),
                    0, 0),

            new Rule("opengl.unsupported", "rule.opengl", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:OpenGL|GLFW) (?:error|version).*?(\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE),
                    0, 0),

            // 图形类模组的专项。钠 / iris 这一系出问题的样子和普通模组不一样：
            // 多数时候没有异常，而是渲染阶段直接崩、画面黑掉、或者光影加载失败
            new Rule("graphics.shader_compile", "rule.shader_compile", Diagnosis.Severity.ERROR,
                    Pattern.compile("(?:Failed to compile|Error compiling) (?:shader|program)|"
                            + "shader compilation failed|Could not compile shader|"
                            + "GLSL.*?compile error", Pattern.CASE_INSENSITIVE),
                    0, 0),

            new Rule("graphics.shader_pack", "rule.shader_pack", Diagnosis.Severity.ERROR,
                    Pattern.compile("(?:Failed to load|Unable to load|Error while loading) shader ?pack|"
                            + "shaderpack .* (?:failed|invalid)|"
                            + "Could not find (?:shader|include) file", Pattern.CASE_INSENSITIVE),
                    0, 0),

            new Rule("graphics.sampler_missing", "rule.sampler", Diagnosis.Severity.WARN,
                    Pattern.compile("could not find sampler named (\\S+)", Pattern.CASE_INSENSITIVE),
                    1, 0),

            new Rule("graphics.framebuffer", "rule.framebuffer", Diagnosis.Severity.ERROR,
                    Pattern.compile("(?:framebuffer|FBO) (?:is )?(?:incomplete|not complete|invalid)|"
                            + "GL_FRAMEBUFFER_(?:INCOMPLETE|UNDEFINED)", Pattern.CASE_INSENSITIVE),
                    0, 0),

            // 模组互相改对方渲染类，是钠生态里最常见的"隐患"提示
            new Rule("graphics.mixin_conflict", "rule.gfxmixin", Diagnosis.Severity.WARN,
                    Pattern.compile("Mod\\(s\\) \\[([^\\]]+)\\] are modifying .*? class (\\S+)",
                            Pattern.CASE_INSENSITIVE),
                    1, 0),

            new Rule("graphics.unsupported_driver", "rule.gfxdriver", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:unsupported|outdated|too old) (?:GPU|graphics )?driver|"
                            + "Your (?:GPU|graphics card) does not support|"
                            + "requires OpenGL (\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE),
                    0, 0),

            new Rule("graphics.glfw_init", "rule.glfw", Diagnosis.Severity.FATAL,
                    Pattern.compile("GLFW (?:error|failed)|Failed to initialize GLFW|"
                            + "Pixel format not accelerated", Pattern.CASE_INSENSITIVE),
                    0, 0),

            new Rule("graphics.context_lost", "rule.gfxctx", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:OpenGL )?context (?:lost|is lost)|GL_CONTEXT_LOST|"
                            + "Device lost|DXGI_ERROR_DEVICE_(?:REMOVED|RESET|HUNG)",
                            Pattern.CASE_INSENSITIVE),
                    0, 0),

            // 同一个族的两个分支同时被加载
            new Rule("graphics.family_conflict", "rule.gfxfamily", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:multiple|more than one) (?:sodium|iris|embeddium|rubidium|oculus)|"
                            + "duplicate (?:sodium|embeddium)", Pattern.CASE_INSENSITIVE),
                    0, 0),

            new Rule("mod.loading_failed", "rule.modload", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:Failed to create mod instance|Mod loading (?:error|failed)|Error during pre-loading|Loading errors encountered)"),
                    0, 0),

            new Rule("manual.debug_crash", "rule.manualcrash", Diagnosis.Severity.WARN,
                    Pattern.compile("Manually triggered debug crash|Debug crash \\(F3\\+C\\)"),
                    0, 0),

            new Rule("mod.construct_error", "rule.modconstruct", Diagnosis.Severity.FATAL,
                    Pattern.compile("Failed to create mod instance\\. Mod: (\\S+)"),                    1, 0),

            // Sinytra Connector 相关：在 NeoForge 上跑 Fabric 模组的桥
            new Rule("connector.failure", "rule.connector", Diagnosis.Severity.FATAL,
                    Pattern.compile("(?:Sinytra )?Connector.{0,40}(?:failed|error|could not)", Pattern.CASE_INSENSITIVE),
                    0, 0),

            new Rule("connector.missing_mod", "rule.connector_missing", Diagnosis.Severity.FATAL,
                    Pattern.compile("Fabric mod (\\S+) (?:is not present|not found|is missing)"),
                    1, 0),

            new Rule("connector.intermediary", "rule.connector_intermediary", Diagnosis.Severity.ERROR,
                    Pattern.compile("\\b(?:class_\\d+|method_\\d+|field_\\d+)\\b"),
                    0, 0)
    );

    // 给实时监听用的入口：单行文本也能立刻分类
    public static Optional<Diagnosis.KnownIssue> classifyLine(String line) {
        List<String> lines = List.of(line);
        for (Rule rule : RULES) {
            List<Hit> hits = find(lines, rule.pattern());
            if (!hits.isEmpty()) {
                return Optional.of(new Diagnosis.KnownIssue(rule.id(), rule.severity(), rule.key(),
                        rule.key() + ".title", "rule.match.detail",
                        List.of(hits.get(0).excerpt()), null, 0, List.of()));
            }
        }
        return Optional.empty();
    }

    public static List<String> ruleIds() {
        List<String> out = new ArrayList<>();
        for (Rule r : RULES) {
            out.add(r.id());
        }
        return out;
    }

    // 规则表用到的所有文案键，测试靠它检查有没有漏
    public static List<String> ruleKeys() {
        List<String> out = new ArrayList<>();
        for (Rule r : RULES) {
            out.add(r.key());
        }
        return out;
    }
}
