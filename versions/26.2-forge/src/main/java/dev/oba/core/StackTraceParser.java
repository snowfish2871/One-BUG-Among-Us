// 解析 Java 堆栈，把每个栈帧归属到具体的模组文件。
package dev.oba.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StackTraceParser {

    // 栈帧。NeoForge 会在每帧尾巴上加 "~[模块@版本/...] {re:classloading}" 这类
    // 附加信息，所以括号后面不能再要求行尾，否则整行匹配不上、栈帧全丢
    private static final Pattern FRAME = Pattern.compile("^\\s*at\\s+([^\\s(]+)\\((.*)\\).*$");
    private static final Pattern EXCEPTION_HEADER = Pattern.compile(
            "^\\s*(?:Caused by:\\s*|Suppressed:\\s*)?([\\w.$]+(?:Exception|Error|Throwable|Failure|ExceptionGroup))(?::\\s?(.*))?\\s*$");
    private static final Pattern LINE_INFO = Pattern.compile("^(.*?):(\\d+)$");

    private StackTraceParser() {
    }

    public record Frame(
            String className,
            String methodName,
            String sourceFile,
            int lineNumber,
            boolean nativeMethod,
            boolean elided,
            int elidedCount,
            String raw,
            ThrowableInfo owner) {

        public String location() {
            if (nativeMethod) {
                return className + "." + methodName + " (native)";
            }
            if (elided) {
                return className + "." + methodName + " (+" + elidedCount + " frames)";
            }
            if (sourceFile != null) {
                return className + "." + methodName + " (" + sourceFile
                        + (lineNumber > 0 ? ":" + lineNumber : "") + ")";
            }
            return className + "." + methodName;
        }

        public boolean hasLineNumber() {
            return lineNumber > 0;
        }
    }

    public static final class ThrowableInfo {
        private final String exceptionClass;
        private final String message;
        private int depth;
        private ThrowableInfo cause;
        private final List<Frame> frames = new ArrayList<>();
        private final List<String> annotations = new ArrayList<>();
        private final int startLine;

        ThrowableInfo(String exceptionClass, String message, int depth, int startLine) {
            this(exceptionClass, message, depth, startLine, null);
        }

        private ThrowableInfo(String exceptionClass, String message, int depth, int startLine, ThrowableInfo cause) {
            this.exceptionClass = exceptionClass;
            this.message = message == null ? "" : message;
            this.depth = depth;
            this.startLine = startLine;
            this.cause = cause;
        }

        // 连上 cause。只在搭链的时候调，连好之后不再改
        void setCause(ThrowableInfo cause) {
            this.cause = cause;
        }

        public String exceptionClass() {
            return exceptionClass;
        }

        // 简单类名，比如 NullPointerException
        public String simpleName() {
            int dot = exceptionClass.lastIndexOf('.');
            return dot < 0 ? exceptionClass : exceptionClass.substring(dot + 1);
        }

        public String message() {
            return message;
        }

        public int depth() {
            return depth;
        }

        void setDepth(int depth) {
            this.depth = depth;
        }

        public Optional<ThrowableInfo> cause() {
            return Optional.ofNullable(cause);
        }

        public List<Frame> frames() {
            return frames;
        }

        // 栈帧前面那些说明性文字，比如 Mixin apply failed ...
        public List<String> annotations() {
            return annotations;
        }

        // 这个异常在文档里的起始行号，从 1 开始
        public int startLine() {
            return startLine;
        }

        public ThrowableInfo rootCause() {
            ThrowableInfo cur = this;
            while (cur.cause != null) {
                cur = cur.cause;
            }
            return cur;
        }

        // 从这个异常一路到根因的完整链
        public List<ThrowableInfo> chain() {
            List<ThrowableInfo> out = new ArrayList<>();
            ThrowableInfo cur = this;
            while (cur != null) {
                out.add(cur);
                cur = cur.cause;
            }
            return out;
        }

        public String headline() {
            return exceptionClass + (message.isEmpty() ? "" : ": " + message);
        }

        @Override
        public String toString() {
            return headline() + " (" + frames.size() + " frames)";
        }
    }

    public record Parsed(List<ThrowableInfo> throwables, List<Frame> allFrames, int totalLines) {
        public List<ThrowableInfo> roots() {
            return throwables;
        }

        public boolean isEmpty() {
            return throwables.isEmpty();
        }
    }

    public static Parsed parseFile(Path file) throws IOException {
        return parse(Files.readString(file));
    }

    // 解析任意一段日志或崩溃报告
    public static Parsed parse(String text) {
        List<ThrowableInfo> roots = new ArrayList<>();
        List<Frame> allFrames = new ArrayList<>();
        String[] lines = text.split("\r\n|\n|\r", -1);

        // 栈帧先攒着，等下一个表头出现才知道它们属于哪个异常。
        // Caused by 会先给它上面那个异常收尾，再开一个新组，
        // 所以每遇到表头就得把缓冲清空一次，不能等到最后
        ThrowableInfo lastThrowable = null;
        List<String> pendingAnnotations = new ArrayList<>();
        List<Frame> buffer = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            Matcher fm = FRAME.matcher(line);
            if (fm.matches()) {
                Frame f = parseFrame(fm.group(1), fm.group(2).trim(), line, lastThrowable);
                if (f != null) {
                    buffer.add(f);
                    allFrames.add(f);
                }
                continue;
            }

            if (line.contains("... ") && line.trim().endsWith(" more")) {
                Frame f = new Frame("<elided>", "", null, 0, false, true, parseElided(line), line, null);
                buffer.add(f);
                allFrames.add(f);
                continue;
            }

            Matcher em = EXCEPTION_HEADER.matcher(line);
            boolean causeMarker = isCauseMarker(line);
            String exClass = null;
            String msg = null;
            if (em.matches() && (causeMarker || mentionsThrowable(line))) {
                // NeoForge 把异常类名也写成 模块@版本/包.类 的形式
                exClass = stripModulePrefix(em.group(1));
                msg = em.group(2);
                if (msg == null) {
                    // JVM 会把多行异常消息原样打出来。这里一路吃到只可能是新内容开头的行为止，
                    // 不然紧跟其后的栈帧会被当成消息一起吞掉
                    StringBuilder cont = new StringBuilder();
                    int j = i + 1;
                    while (j < lines.length && !startsTraceComponent(lines[j]) && !isHeaderLine(lines[j])) {
                        cont.append(cont.length() == 0 ? "" : "\n").append(lines[j]);
                        j++;
                    }
                    if (cont.length() > 0) {
                        msg = cont.toString();
                    }
                }
            }

            if (exClass != null) {
                // 已经攒下的栈帧属于正在收尾的这个异常
                attachGroup(buffer, lastThrowable);
                buffer.clear();

                ThrowableInfo info = new ThrowableInfo(exClass, msg, 0, i + 1);
                if (causeMarker && lastThrowable != null) {
                    // Caused by 接的是链上的下一环：上一个异常不是新异常的 cause，
                    // 而是反过来拥有它。这样从根往外走就能按顺序访问每个 cause
                    ThrowableInfo tail = lastThrowable.rootCause();
                    info.setDepth(tail.depth() + 1);
                    tail.setCause(info);
                } else {
                    roots.add(info);
                }
                info.annotations().addAll(pendingAnnotations);
                pendingAnnotations.clear();
                lastThrowable = info;
                continue;
            }

            // 既不是栈帧也不是表头的行算上下文；空行表示一段堆栈结束
            if (line.isBlank()) {
                attachGroup(buffer, lastThrowable);
                buffer.clear();
                lastThrowable = null;
            } else {
                String trimmed = line.trim();
                if (looksInteresting(trimmed)) {
                    pendingAnnotations.add(trimmed);
                    if (pendingAnnotations.size() > 24) {
                        pendingAnnotations.remove(0);
                    }
                }
            }
        }
        attachGroup(buffer, lastThrowable);
        return new Parsed(List.copyOf(roots), List.copyOf(allFrames), lines.length);
    }

    private static boolean isCauseMarker(String line) {
        String t = line.strip();
        return t.startsWith("Caused by:") || t.startsWith("Suppressed:")
                || t.startsWith("ADDITIONAL CAUSES:");
    }

    private static boolean mentionsThrowable(String line) {
        return line.contains("Exception") || line.contains("Error")
                || line.contains("Throwable") || line.contains("Failure");
    }

    // 整行就是一个异常类型（后面可能跟 Caused by: 之类的前缀和消息）时为真
    private static boolean isHeaderLine(String line) {
        Matcher m = EXCEPTION_HEADER.matcher(line);
        return m.matches() && (isCauseMarker(line) || mentionsThrowable(line));
    }

    // 这一行只可能是一段新内容的开头（栈帧、省略号、cause 表头、空行）时为真。
    // 用来判断多行异常消息到哪里结束
    private static boolean startsTraceComponent(String line) {
        String t = line.strip();
        if (t.isEmpty()) {
            return true;
        }
        if (t.startsWith("at ") || t.startsWith("... ")) {
            return true;
        }
        if (t.startsWith("Caused by:") || t.startsWith("Suppressed:")) {
            return true;
        }
        // 一个新的根异常，比如 java.lang.NullPointerException: x
        return EXCEPTION_HEADER.matcher(t).matches()
                && (t.contains("Exception") || t.contains("Error")
                    || t.contains("Throwable") || t.contains("Failure"));
    }

    private static boolean looksInteresting(String trimmed) {
        if (trimmed.length() < 8 || trimmed.length() > 400) {
            return false;
        }
        String l = trimmed.toLowerCase(Locale.ROOT);
        return l.contains("mixin") || l.contains("failed") || l.contains("error")
                || l.contains("exception") || l.contains("missing") || l.contains("duplicate")
                || l.contains("cannot") || l.contains("unable") || l.contains("not found")
                || l.contains("incompatible") || l.contains("crash") || l.contains("fatal");
    }

    // 把缓冲区里那组栈帧挂到它们所属的异常上
    private static void attachGroup(List<Frame> buffer, ThrowableInfo owner) {
        if (owner == null || buffer.isEmpty()) {
            return;
        }
        for (Frame f : buffer) {
            if (!f.elided()) {
                owner.frames().add(f);
            }
        }
    }

    private static int parseElided(String line) {
        try {
            String s = line.trim();
            s = s.substring(s.indexOf("... ") + 4).replace("more", "").trim();
            return Integer.parseInt(s);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    // 去掉 NeoForge 加在类名前面的模块前缀。实测有这两种写法：
    // 模块@版本/包.类（net.neoforged.fml@21.1.256/net.neoforged.fml.Foo）
    // 模块/模块@版本/包.类（TRANSFORMER/crasher@1.0.0/com.example.Crasher）
    // 所以取最后一个斜杠之后的部分，不能只看 @ 后面第一个斜杠，
    // 否则类名会带上一截模块名，跟索引里的任何模组都对不上。
    public static String stripModulePrefix(String className) {
        if (className.indexOf('@') < 0) {
            return className;
        }
        int slash = className.lastIndexOf('/');
        return slash >= 0 ? className.substring(slash + 1) : className;
    }

    private static Frame parseFrame(String signature, String paren, String raw, ThrowableInfo owner) {        int parenIdx = signature.indexOf('(');
        String classAndMethod = parenIdx >= 0 ? signature.substring(0, parenIdx) : signature;
        String forcedArgs = parenIdx >= 0 ? signature.substring(parenIdx) : "";
        int lastDot = classAndMethod.lastIndexOf('.');
        if (lastDot <= 0) {
            return null;
        }
        String className = stripModulePrefix(classAndMethod.substring(0, lastDot));
        String methodName = classAndMethod.substring(lastDot + 1) + forcedArgs;

        boolean nativeMethod = paren.contains("Native Method");
        String sourceFile = null;
        int line = 0;
        if (!nativeMethod && !paren.contains("Unknown Source") && !paren.isEmpty()) {
            Matcher lm = LINE_INFO.matcher(paren);
            if (lm.matches()) {
                sourceFile = lm.group(1).trim();
                try {
                    line = Integer.parseInt(lm.group(2));
                } catch (NumberFormatException ignored) {
                    line = 0;
                }
            } else {
                sourceFile = paren;
            }
        }
        return new Frame(className, methodName, sourceFile, line, nativeMethod, false, 0, raw, owner);
    }

    // 归属

    // 这些包前缀永远不该算到用户头上
    private static final List<String> VANILLA_PREFIXES = List.of(
            "java.", "javax.", "jdk.", "sun.", "com.sun.", "org.w3c.", "org.xml.",
            "net.minecraft.", "com.mojang.", "net.neoforged.", "cpw.mods.", "net.minecraftforge.",
            "org.spongepowered.asm.", "org.objectweb.asm.", "org.apache.logging.", "org.lwjgl.",
            "org.slf4j.", "it.unimi.", "com.google.", "org.jetbrains.", "org.gradle.",
            "org.apache.commons.", "io.netty.", "oshi.", "com.mojang.blaze3d.", "paulscode.",
            "kotlin.", "scala.", "gnu.trove.", "org.joml.", "net.minecraftforge.fml.");

    // 只有加载器自己这些包才算"不是用户的代码"。
    // net.fabricmc 下面既有加载器也有模组（Fabric API 之类），
    // 所以不能整个前缀一棍子打死，否则通过 Sinytra Connector 跑起来的
    // Fabric 模组出问题时，栈帧会被当成加载器代码直接丢掉。
    private static final List<String> LOADER_INTERNALS = List.of(
            "net.fabricmc.loader.",
            "net.fabricmc.mappingio.",
            "net.fabricmc.accesswidener.",
            "net.fabricmc.tinyremapper.",
            "org.sinytra.connector.locator.",
            "org.sinytra.connector.transformer.",
            "org.sinytra.connector.util.",
            "org.sinytra.adapter.");

    // 自己永远不该被当成元凶。
    // 实机遇到过：看门狗抓卡死线程时，报出来的居然是 Oba 自己那个
    // "等在日志文件上的线程"，等于自己指控自己，玩家看了只会更糊涂
    private static final List<String> SELF_PREFIXES = List.of(
            "dev.oba.");

    public static boolean isVanillaOrLoader(String className) {
        if (className == null) {
            return true;
        }
        if (className.equals("<elided>") || className.startsWith("<")) {
            return true;
        }
        for (String p : SELF_PREFIXES) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        for (String p : VANILLA_PREFIXES) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        for (String p : LOADER_INTERNALS) {
            if (className.startsWith(p)) {
                return true;
            }
        }
        // net.minecraft 相关的都算原版
        return className.startsWith("net.minecraft.");
    }

    public record AttributedFrame(ModSource source, ClassIndex.Match match, Frame frame, String location) {
    }

    // 把整条异常链上的每个栈帧都归属一次。
    // index 是类到模组的对照表，允许是空表
    public static List<AttributedFrame> attribute(ThrowableInfo info, ClassIndex index) {
        List<AttributedFrame> out = new ArrayList<>();
        for (ThrowableInfo t : info.chain()) {
            for (Frame f : t.frames()) {
                if (f.elided() || isVanillaOrLoader(f.className())) {
                    continue;
                }
                ClassIndex.Match m = index.resolve(f.className());
                if (m != null) {
                    out.add(new AttributedFrame(m.source(), m, f, f.location()));
                }
            }
        }
        return out;
    }

    // 同一条链上，每个模组各自分到的栈帧，按出现顺序
    public static Map<ModSource, List<AttributedFrame>> groupByMod(List<AttributedFrame> frames) {
        Map<ModSource, List<AttributedFrame>> out = new LinkedHashMap<>();
        for (AttributedFrame af : frames) {
            out.computeIfAbsent(af.source(), k -> new ArrayList<>()).add(af);
        }
        return out;
    }
}
