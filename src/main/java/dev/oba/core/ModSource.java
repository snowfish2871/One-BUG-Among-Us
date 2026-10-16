// 单个模组文件的只读视图：读 jar 的条目表，拿到 modId、类名和各种元数据。
package dev.oba.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public final class ModSource {

    public record Descriptor(
            String modId,
            String namespace,
            String displayName,
            String version,
            String description,
            String authors,
            String credits,
            String license,
            String modLoader,
            String loaderVersion,
            String displayUrl,
            String issueTrackerUrl,
            String logoFile,
            List<String> mixinConfigs,
            List<Dependency> dependencies) {

        public String label() {
            if (displayName == null || displayName.isBlank() || displayName.equalsIgnoreCase(modId)) {
                return modId;
            }
            return displayName + " (" + modId + ")";
        }

        // 这条描述来自 META-INF/mods.toml（Forge 老布局）时为真
        public boolean isLegacyLayout() {
            return false;
        }
    }

    public record Dependency(String modId, String type, String versionRange, String ordering, String side, String reason) {
        public boolean isRequired() {
            return type == null || type.isBlank() || type.equalsIgnoreCase("required");
        }
    }

    public record NestedJar(String path, List<String> modIds, String displayName, String version) {
        public String label() {
            if (displayName != null && !displayName.isBlank()) {
                return displayName + (version == null || version.isBlank() ? "" : " " + version);
            }
            return path;
        }
    }

    private final Path path;
    private final boolean directory;

    private volatile boolean scanned;
    private volatile List<String> classNames = List.of();
    private volatile List<String> entries = List.of();
    private volatile List<String> packages = List.of();
    private volatile List<Descriptor> descriptors = List.of();
    private volatile List<NestedJar> nestedJars = List.of();
    private volatile List<String> scanErrors = List.of();
    private volatile String sha1;
    private volatile String manifestVersion;
    private volatile long size = -1L;
    private volatile long modified = -1L;

    private ModSource(Path path, boolean directory) {
        this.path = path;
        this.directory = directory;
    }

    public static ModSource of(Path path) {
        return new ModSource(path.toAbsolutePath().normalize(), Files.isDirectory(path));
    }

    public Path path() {
        return path;
    }

    public String fileName() {
        return path.getFileName().toString();
    }

    public boolean isDirectory() {
        return directory;
    }

    public long size() {
        ensureStat();
        return size;
    }

    public long modifiedMillis() {
        ensureStat();
        return modified;
    }

    private void ensureStat() {
        if (size < 0) {
            try {
                size = Files.size(path);
                modified = Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                size = 0L;
                modified = 0L;
            }
        }
    }

    // 文件 SHA-1，用来确认这个 jar 没被改动过
    public String sha1() {
        String cached = sha1;
        if (cached != null) {
            return cached;
        }
        String digest = "<unreadable>";
        if (!directory) {
            try (InputStream in = Files.newInputStream(path)) {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    md.update(buf, 0, n);
                }
                digest = hex(md.digest());
            } catch (Exception e) {
                digest = "<error: " + e.getClass().getSimpleName() + ">";
            }
        }
        sha1 = digest;
        return digest;
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 0xF, 16)).append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString();
    }

    // 描述文件

    // 这个文件声明的全部 [[mods]] 条目
    public List<Descriptor> descriptors() {
        scanIfNeeded();
        return descriptors;
    }

    // 这个文件提供的所有 modId（一个 jar 里可能打包了好几个）。
    // 同一个 id 出现两次是元数据写重了，对外只算一次
    public List<String> modIds() {
        return descriptors().stream()
                .map(Descriptor::modId)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
    }

    // 能拿到的最好看的名字
    public String display() {
        List<Descriptor> ds = descriptors();
        if (ds.isEmpty()) {
            return fileName();
        }
        if (ds.size() == 1) {
            return ds.get(0).label();
        }
        return ds.stream().map(Descriptor::modId).collect(Collectors.joining(" + "));
    }

    public String version() {
        List<Descriptor> ds = descriptors();
        if (ds.isEmpty()) {
            return "";
        }
        if (ds.size() == 1) {
            return nullToEmpty(ds.get(0).version());
        }
        return ds.stream().map(d -> d.modId() + "@" + nullToEmpty(d.version())).collect(Collectors.joining(", "));
    }

    // 元数据里的简介，游戏内点开模组时显示的就是它
    public String description() {
        List<Descriptor> ds = descriptors();
        if (ds.isEmpty()) {
            return "";
        }
        // 一个 jar 里打包了多个模组时，把各自的简介拼起来
        if (ds.size() == 1) {
            return nullToEmpty(ds.get(0).description());
        }
        return ds.stream()
                .filter(d -> d.description() != null && !d.description().isBlank())
                .map(d -> d.modId() + ": " + d.description())
                .collect(Collectors.joining("\n"));
    }

    public String authors() {
        List<Descriptor> ds = descriptors();
        return ds.isEmpty() ? "" : nullToEmpty(ds.get(0).authors());
    }

    public String license() {
        List<Descriptor> ds = descriptors();
        return ds.isEmpty() ? "" : nullToEmpty(ds.get(0).license());
    }

    // 面向哪个加载器：javafml（Forge/NeoForge）还是 fabric
    public String loader() {
        List<Descriptor> ds = descriptors();
        if (ds.isEmpty()) {
            return "";
        }
        String l = nullToEmpty(ds.get(0).modLoader());
        if (l.isBlank()) {
            return "";
        }
        // fabric.mod.json 读出来的统一标成 fabric
        return l.equals("fabric") ? "Fabric" : l;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // 这个文件自称是 Fabric 模组时为真
    public boolean isFabric() {
        scanIfNeeded();
        return entries.stream().anyMatch(e -> e.equals("fabric.mod.json"));
    }

    // 这个文件自称是 Quilt 模组时为真
    public boolean isQuilt() {
        scanIfNeeded();
        return entries.stream().anyMatch(e -> e.equals("quilt.mod.json"));
    }

    // 内容

    // 里面包含的全限定类名
    public List<String> classNames() {
        scanIfNeeded();
        return classNames;
    }

    // 归档里的所有路径（目录形式的模组就是所有文件）
    public List<String> entries() {
        scanIfNeeded();
        return entries;
    }

    // 至少含一个类的包名
    public List<String> packages() {
        scanIfNeeded();
        return packages;
    }

    // 值得进索引的前几层包名（最多四层）。更深的层用 classNames() 也能拿到，
    // 四层足够覆盖真实模组的布局，又不至于把索引撑得很大
    public Set<String> indexablePackages() {
        Set<String> out = new LinkedHashSet<>();
        for (String cls : classNames()) {
            int dot = cls.lastIndexOf('.');
            if (dot <= 0) {
                continue;
            }
            String pkg = cls.substring(0, dot);
            String[] segs = pkg.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < segs.length && i < 4; i++) {
                if (i > 0) {
                    sb.append('.');
                }
                sb.append(segs[i]);
                out.add(sb.toString());
            }
        }
        return out;
    }

    // 内嵌的 jar-in-jar
    public List<NestedJar> nestedJars() {
        scanIfNeeded();
        return nestedJars;
    }

    public List<String> scanErrors() {
        scanIfNeeded();
        return scanErrors;
    }

    // 内部实现

    private void scanIfNeeded() {
        if (scanned) {
            return;
        }
        synchronized (this) {
            if (scanned) {
                return;
            }
            List<String> classes = new ArrayList<>();
            List<String> entryList = new ArrayList<>();
            List<String> pkgs = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            List<Descriptor> descs = new ArrayList<>();
            List<NestedJar> nested = new ArrayList<>();
            try {
                if (directory) {
                    walkDirectory(classes, entryList, errors);
                    readDescriptorFiles((name) -> {
                        Path f = path.resolve(name);
                        return Files.isRegularFile(f) ? Files.readAllBytes(f) : null;
                    }, descs, errors, nested);
                } else {
                    readJar(classes, entryList, errors, descs, nested);
                }
            } catch (Exception e) {
                errors.add(Lang.get("jar.unreadable", fileName(), e));
            }
            Set<String> pkgSet = new LinkedHashSet<>();
            for (String cls : classes) {
                int dot = cls.lastIndexOf('.');
                if (dot > 0) {
                    pkgSet.add(cls.substring(0, dot));
                }
            }
            pkgs.addAll(pkgSet);
            this.classNames = List.copyOf(classes);
            this.entries = List.copyOf(entryList);
            this.packages = List.copyOf(pkgs);
            this.descriptors = List.copyOf(descs);
            this.nestedJars = List.copyOf(nested);
            this.scanErrors = List.copyOf(errors);
            this.scanned = true;
        }
    }

    private void walkDirectory(List<String> classes, List<String> entryList, List<String> errors) throws IOException {
        try (var stream = Files.walk(path)) {
            for (Path p : stream.filter(Files::isRegularFile).toList()) {
                String rel = path.relativize(p).toString().replace('\\', '/');
                entryList.add(rel);
                if (rel.endsWith(".class")) {
                    classes.add(rel.substring(0, rel.length() - 6).replace('/', '.'));
                }
            }
        } catch (IOException e) {
            errors.add(Lang.get("jar.walkFailed", path, e));
        }
    }

    private void readJar(List<String> classes, List<String> entryList, List<String> errors,
                         List<Descriptor> descs, List<NestedJar> nested) {
        try (JarFile jar = new JarFile(path.toFile())) {
            var it = jar.entries();
            List<JarEntry> nestedEntries = new ArrayList<>();
            while (it.hasMoreElements()) {
                JarEntry e = it.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                String name = e.getName();
                entryList.add(name);
                if (name.endsWith(".class") && !name.equals("module-info.class")
                        && !name.startsWith("META-INF/versions/")) {
                    classes.add(name.substring(0, name.length() - 6).replace('/', '.'));
                }
                if (name.startsWith("META-INF/jarjar/") && name.endsWith(".jar")) {
                    nestedEntries.add(e);
                }
            }
            readDescriptorFromJar(jar, descs, errors);
            for (JarEntry je : nestedEntries) {
                try (InputStream in = jar.getInputStream(je)) {
                    byte[] data = in.readAllBytes();
                    List<Descriptor> inner = descriptorsOfNestedJar(data, je.getName(), errors);
                    for (Descriptor d : inner) {
                        nested.add(new NestedJar(je.getName(), List.of(d.modId()),
                                d.displayName(), d.version()));
                    }
                    if (inner.isEmpty()) {
                        nested.add(new NestedJar(je.getName(), List.of(), null, null));
                    }
                } catch (Exception e) {
                    errors.add(Lang.get("jar.nestedFailed", je.getName(), e));
                }
            }
        } catch (IOException e) {
            errors.add(Lang.get("jar.openFailed", fileName(), e));
        }
    }

    // 用 zip 流读内存里的内嵌 jar 描述，内嵌包不会被写到磁盘上
    private List<Descriptor> descriptorsOfNestedJar(byte[] data, String innerName, List<String> errors) {
        List<Descriptor> out = new ArrayList<>();
        try (java.util.zip.ZipInputStream zin =
                     new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(data))) {
            java.util.zip.ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                String name = ze.getName();
                if (name.equals("META-INF/neoforge.mods.toml") || name.equals("META-INF/mods.toml")) {
                    parseDescriptor(new String(zin.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8),
                            innerName + "!" + name, out, errors);
                }
            }
        } catch (Exception e) {
            errors.add(Lang.get("jar.nestedParseFailed", innerName, e.getClass().getSimpleName()));
        }
        return out;
    }

    @FunctionalInterface
    private interface EntryReader {
        byte[] read(String name) throws IOException;
    }

    private void readDescriptorFiles(EntryReader reader,
                                     List<Descriptor> descs, List<String> errors, List<NestedJar> nested) {
        boolean found = false;
        for (String candidate : List.of("META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
            try {
                byte[] data = reader.read(candidate);
                if (data != null) {
                    found = true;
                    parseDescriptor(new String(data, java.nio.charset.StandardCharsets.UTF_8),
                            candidate, descs, errors);
                }
            } catch (IOException e) {
                errors.add(Lang.get("jar.readFailed", candidate, e));
            }
        }
        if (!found) {
            try {
                byte[] data = reader.read("fabric.mod.json");
                if (data != null) {
                    parseFabricDescriptor(new String(data, java.nio.charset.StandardCharsets.UTF_8),
                            descs, errors);
                }
            } catch (IOException e) {
                errors.add(Lang.get("jar.readFailed", "fabric.mod.json", e));
            }
        }
    }

    private void readDescriptorFromJar(JarFile jar, List<Descriptor> descs, List<String> errors) {
        boolean found = false;
        for (String candidate : List.of("META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
            JarEntry e = jar.getJarEntry(candidate);
            if (e == null) {
                continue;
            }
            found = true;
            try (InputStream in = jar.getInputStream(e)) {
                parseDescriptor(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8),
                        candidate, descs, errors);
            } catch (IOException ex) {
                errors.add(Lang.get("jar.readFailed", candidate, ex));
            }
        }
        if (!found) {
            readFabricDescriptor(jar, descs, errors);
        }
    }

    // Fabric 模组的身份写在 fabric.mod.json 里。装了 Sinytra Connector 时它们
    // 和普通 NeoForge 模组混在一起，没有这一段它们就只剩个文件名，没法参与归属。
    private void readFabricDescriptor(JarFile jar, List<Descriptor> descs, List<String> errors) {
        JarEntry e = jar.getJarEntry("fabric.mod.json");
        if (e == null) {
            return;
        }
        try (InputStream in = jar.getInputStream(e)) {
            String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            parseFabricDescriptor(json, descs, errors);
        } catch (IOException ex) {
            errors.add(Lang.get("jar.readFailed", "fabric.mod.json", ex));
        }
    }

    // fabric.mod.json 的解析，jar 和目录两种形态共用
    private void parseFabricDescriptor(String json, List<Descriptor> descs, List<String> errors) {
        {
            String id = jsonString(json, "id");
            if (id == null || id.isBlank()) {
                errors.add(Lang.get("jar.fabricNoId", fileName()));
                return;
            }
            descs.add(new Descriptor(
                    id,
                    id,
                    orDefault(jsonString(json, "name"), id),
                    orDefault(jsonString(json, "version"), ""),
                    orDefault(jsonString(json, "description"), ""),
                    joinAuthors(json),
                    null,
                    orDefault(jsonString(json, "license"), ""),
                    "fabric",
                    "",
                    orDefault(jsonString(json, "contact_homepage"), ""),
                    orDefault(jsonString(json, "contact_sources"), ""),
                    orDefault(jsonString(json, "icon"), ""),
                    List.of(),
                    List.of()));
        }
    }

    // 从 JSON 里抠一个顶层字符串字段。fabric.mod.json 结构很简单，
    // 为它引一整个 JSON 库不划算，这里做个够用的取值
    private static String jsonString(String json, String field) {
        String needle = "\"" + field + "\"";
        int at = json.indexOf(needle);
        if (at < 0) {
            return null;
        }
        int colon = json.indexOf(':', at + needle.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int j = i + 1; j < json.length(); j++) {
            char ch = json.charAt(j);
            if (ch == '\\' && j + 1 < json.length()) {
                sb.append(json.charAt(++j));
            } else if (ch == '"') {
                return sb.toString();
            } else {
                sb.append(ch);
            }
        }
        return null;
    }

    private static String joinAuthors(String json) {
        int at = json.indexOf("\"authors\"");
        if (at < 0) {
            return null;
        }
        int end = json.indexOf(']', at);
        if (end < 0) {
            return null;
        }
        List<String> names = new ArrayList<>();
        String block = json.substring(at, end);
        for (String part : block.split("\"")) {
            // 作者块里形如 {"name":"xxx"}，把 name 后面的值捞出来
            if (!part.isBlank() && !part.contains("{") && !part.contains("}")
                    && !part.equals("authors") && !part.contains("name")
                    && !part.contains(":") && !part.contains("[") && !part.contains("]")) {
                names.add(part.trim());
            }
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void parseDescriptor(String text, String origin, List<Descriptor> descs, List<String> errors) {
        Toml.Doc doc;
        try {
            doc = Toml.parse(text);
        } catch (RuntimeException e) {
            errors.add(Lang.get("jar.parseFailed", origin, e.getMessage()));
            return;
        }
        String modLoader = doc.string("modLoader").orElse("javafml");
        String loaderVersion = doc.string("loaderVersion").orElse("");
        String license = doc.string("license").orElse("");
        List<Map<String, Object>> modEntries = doc.tables("mods");
        if (modEntries.isEmpty()) {
            // 描述文件里没有 [[mods]] 就是坏的，能记多少记多少
            errors.add(Lang.get("jar.noModsSection", origin));
            return;
        }
        for (Map<String, Object> entry : modEntries) {
            String modId = str(entry.get("modId"));
            if (modId == null || modId.isBlank()) {
                errors.add(Lang.get("jar.missingModId", origin));
                continue;
            }
            List<Dependency> deps = new ArrayList<>();
            for (Map<String, Object> d : doc.tables("dependencies", modId)) {
                deps.add(new Dependency(str(d.get("modId")), str(d.get("type")), str(d.get("versionRange")),
                        str(d.get("ordering")), str(d.get("side")), str(d.get("reason"))));
            }
            List<String> mixins = new ArrayList<>();
            for (Map<String, Object> m : doc.tables("mixins")) {
                String cfg = str(m.get("config"));
                if (cfg != null) {
                    mixins.add(cfg);
                }
            }
            descs.add(new Descriptor(
                    modId,
                    str(entry.get("namespace")),
                    str(entry.get("displayName")),
                    resolveVersion(str(entry.get("version"))),
                    str(entry.get("description")),
                    str(entry.get("authors")),
                    str(entry.get("credits")),
                    license,
                    modLoader,
                    loaderVersion,
                    str(entry.get("displayURL")),
                    str(entry.get("issueTrackerURL")),
                    str(entry.get("logoFile")),
                    List.copyOf(mixins),
                    List.copyOf(deps)));
        }
    }

    // 有些模组的 mods.toml 里写着 ${file.jarVersion} 这种占位符，是留给
    // 打包工具展开的。没展开的时候显示出来很难看，就退回 manifest 里的版本号。
    private String resolveVersion(String declared) {
        if (declared == null || declared.isBlank() || declared.indexOf("${") < 0) {
            return declared;
        }
        String fromManifest = manifestVersion();
        return fromManifest != null && !fromManifest.isBlank() ? fromManifest : declared;
    }

    // manifest 里的 Implementation-Version，jar 和目录两种形态都支持
    private String manifestVersion() {
        String cached = manifestVersion;
        if (cached != null) {
            return cached;
        }
        String found = null;
        try {
            if (directory) {
                Path mf = path.resolve("META-INF/MANIFEST.MF");
                if (Files.isRegularFile(mf)) {
                    found = readManifestVersion(Files.readString(mf));
                }
            } else {
                try (JarFile jar = new JarFile(path.toFile())) {
                    java.util.jar.Manifest mf = jar.getManifest();
                    if (mf != null) {
                        found = mf.getMainAttributes().getValue("Implementation-Version");
                    }
                }
            }
        } catch (Exception ignored) {
            // 读不到就算了，顶多显示成占位符
        }
        manifestVersion = found;
        return found;
    }

    private static String readManifestVersion(String text) {
        for (String line : text.split("\r\n|\n|\r")) {
            if (line.regionMatches(true, 0, "Implementation-Version:", 0, 23)) {
                return line.substring(23).trim();
            }
        }
        return null;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    // 发现文件

    // mods 目录里所有模组文件：散放的 .jar，以及解压开的目录
    public static List<ModSource> scanModsFolder(Path modsDir) throws IOException {
        if (modsDir == null || !Files.isDirectory(modsDir)) {
            return List.of();
        }
        List<ModSource> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(modsDir)) {
            for (Path p : ds) {
                String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (Files.isDirectory(p)) {
                    if (Files.isRegularFile(p.resolve("META-INF/neoforge.mods.toml"))
                            || Files.isRegularFile(p.resolve("META-INF/mods.toml"))) {
                        out.add(of(p));
                    }
                } else if (name.endsWith(".jar")) {
                    out.add(of(p));
                }
            }
        }
        out.sort((a, b) -> a.fileName().compareToIgnoreCase(b.fileName()));
        return out;
    }

    // 被改名关掉的模组文件（一般是加了 .disabled）。只是列出来给个上下文，
    // Oba 不会创建也不会改动它们
    public static List<Path> scanDisabled(Path modsDir) throws IOException {
        if (modsDir == null || !Files.isDirectory(modsDir)) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(modsDir)) {
            for (Path p : ds) {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (Files.isRegularFile(p) && !n.endsWith(".jar")
                        && (n.contains(".jar.") || n.endsWith(".disabled") || n.endsWith(".bak"))) {
                    out.add(p);
                }
            }
        }
        out.sort((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()));
        return out;
    }

    // modId 到提供它的文件的映射，内嵌 jar 也算
    public static Map<String, ModSource> indexByModId(List<ModSource> sources) {
        Map<String, ModSource> out = new LinkedHashMap<>();
        for (ModSource s : sources) {
            for (String id : s.modIds()) {
                out.putIfAbsent(id, s);
            }
        }
        return out;
    }

    public Optional<Descriptor> primaryDescriptor() {
        List<Descriptor> ds = descriptors();
        return ds.isEmpty() ? Optional.empty() : Optional.of(ds.get(0));
    }

    @Override
    public String toString() {
        return "ModSource[" + fileName() + "]";
    }

    // 给报告用的只读视图
    public static <T> List<T> freeze(List<T> in) {
        return Collections.unmodifiableList(in);
    }
}
