// 诊断结果的数据模型：一条证据、一个可疑模组、一条规则命中、一份报告。
package dev.oba.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class Diagnosis {

    private Diagnosis() {
    }

    public enum Confidence {
        CERTAIN("confidence.certain", "certain"),
        HIGH("confidence.high", "high"),
        MEDIUM("confidence.medium", "medium"),
        LOW("confidence.low", "low");

        private final String key;
        private final String id;

        Confidence(String key, String id) {
            this.key = key;
            this.id = id;
        }

        // 当前语言下的名字
        public String label() {
            return Lang.get(key);
        }

        // JSON 里用的固定标识，不翻译
        public String id() {
            return id;
        }

        public static Confidence of(double score) {
            if (score >= 2.0) {
                return CERTAIN;
            }
            if (score >= 0.9) {
                return HIGH;
            }
            if (score >= 0.45) {
                return MEDIUM;
            }
            return LOW;
        }

        // 分数换算成百分比。分数是累加的权重不是概率，所以封顶，
        // 免得显示成"213%"这种让人误解的数字
        public static int percent(double score) {
            return (int) Math.round(Math.min(0.99, score / 2.5) * 100);
        }
    }

    // 指向某个模组的一条证据。
    // rule 是规则标识（如 frame.owner），detailKey/detailArgs 是说明文字，
    // weight 是给分数加了多少，line 是文档里的行号（0 表示没有），
    // location 是证据来自栈帧时的类/方法/行号
    public record Evidence(String rule, String detailKey, List<Object> detailArgs,
                           double weight, int line, String location) {

        // 说明文字，渲染时才按语言解析
        public String detail() {
            return Lang.get(detailKey, detailArgs.toArray());
        }

        public static Evidence of(String rule, String detailKey, List<Object> args, double weight) {
            return new Evidence(rule, detailKey, args, weight, 0, null);
        }

        public static Evidence at(String rule, String detailKey, List<Object> args,
                                  double weight, int line, String location) {
            return new Evidence(rule, detailKey, args, weight, line, location);
        }
    }

    public static final class Finding {
        private final ModSource source;
        private final List<Evidence> evidence = new ArrayList<>();
        private final List<StackTraceParser.AttributedFrame> frames = new ArrayList<>();
        private final List<String> throwables = new ArrayList<>();
        private double score;

        public Finding(ModSource source) {
            this.source = source;
        }

        public ModSource source() {
            return source;
        }

        public String fileName() {
            return source == null ? "?" : source.fileName();
        }

        public String display() {
            return source == null ? fileName() : source.display();
        }

        public List<Evidence> evidence() {
            return evidence;
        }

        public List<StackTraceParser.AttributedFrame> frames() {
            return frames;
        }

        public List<String> throwables() {
            return throwables;
        }

        public double score() {
            return score;
        }

        public Confidence confidence() {
            return Confidence.of(score);
        }

        public void addEvidence(Evidence e) {
            evidence.add(e);
        }

        public void addFrame(StackTraceParser.AttributedFrame f) {
            frames.add(f);
        }

        public void addThrowable(String headline) {
            if (!throwables.contains(headline)) {
                throwables.add(headline);
            }
        }

        public void addScore(double delta) {
            this.score += delta;
        }

        public void setScore(double score) {
            this.score = score;
        }

        // 第一个带真实行号的已归属栈帧
        public StackTraceParser.AttributedFrame firstLocatedFrame() {
            for (StackTraceParser.AttributedFrame f : frames) {
                if (f.frame().hasLineNumber()) {
                    return f;
                }
            }
            return frames.isEmpty() ? null : frames.get(0);
        }

        // 去重后的出错位置，形如 类.方法 (文件.java:行号)
        public List<String> distinctLocations(int limit) {
            List<String> out = new ArrayList<>();
            for (StackTraceParser.AttributedFrame f : frames) {
                String loc = f.location();
                if (!out.contains(loc)) {
                    out.add(loc);
                }
                if (out.size() >= limit) {
                    break;
                }
            }
            return out;
        }

        public void sortEvidence() {
            evidence.sort(Comparator.comparingDouble(Evidence::weight).reversed());
        }
    }

    public enum Severity {
        FATAL("severity.fatal", "fatal"),
        ERROR("severity.error", "error"),
        WARN("severity.warn", "warn"),
        INFO("severity.info", "info");

        private final String key;
        private final String id;

        Severity(String key, String id) {
            this.key = key;
            this.id = id;
        }

        // 当前语言下的名字
        public String label() {
            return Lang.get(key);
        }

        // JSON 里用的固定标识，不翻译
        public String id() {
            return id;
        }
    }

    // 在日志或崩溃报告里命中一条规则的结果，跟栈帧无关，
    // 比如缺前置、mixin 失败、重复加载。
    // 文案存的是键加参数而不是现成的句子，这样同一份结论换语言也能重新渲染。
    // ruleKey 是文案前缀（如 rule.missing_dependency），
    // detailKey/detailArgs 是详情那行用的键和参数
    public record KnownIssue(
            String rule,
            Severity severity,
            String ruleKey,
            String titleKey,
            List<Object> titleArgs,
            String detailKey,
            List<Object> detailArgs,
            ModSource source,
            int line,
            List<String> suspects) {

        // 标题不需要参数的简写形式
        public KnownIssue(String rule, Severity severity, String ruleKey, String titleKey,
                          String detailKey, List<Object> detailArgs,
                          ModSource source, int line, List<String> suspects) {
            this(rule, severity, ruleKey, titleKey, List.of(), detailKey, detailArgs, source, line, suspects);
        }

        // 这条结论的简短标题
        public String title() {
            return Lang.get(titleKey, titleArgs.toArray());
        }

        // 命中的具体内容，包含那行原文
        public String detail() {
            return Lang.get(detailKey, detailArgs.toArray());
        }

        // 给用户的建议。永远只是建议，不会自动动手
        public String advice() {
            return Lang.get(ruleKey + ".advice");
        }

        public KnownIssue withSource(ModSource s) {
            return new KnownIssue(rule, severity, ruleKey, titleKey, titleArgs,
                    detailKey, detailArgs, s, line, suspects);
        }

        public KnownIssue withLine(int l) {
            return new KnownIssue(rule, severity, ruleKey, titleKey, titleArgs,
                    detailKey, detailArgs, source, l, suspects);
        }
    }

    // 一个图形类模组在谱系里的位置。
    // 钠 / iris 这一系换名字的分支太多，光看文件名认不出谁是谁的亲戚。
    public record GraphicsModInfo(String displayName, String fileName, String version,
                                  String familyName, String modId, String loader, String note,
                                  boolean canonical) {
    }

    // 图形类模组的组合问题。这类问题和堆栈无关，是"装的东西本身不对"。
    public record GraphicsIssue(Kind kind, String familyName, String canonicalId,
                                String subject, String fileName, String wanted,
                                List<String> conflictingFiles, List<String> memberIds) {

        public enum Kind {
            // 同一族装了多个互斥分支
            FAMILY_CONFLICT,
            // 光影加载器缺了配套的渲染主体
            MISSING_PARTNER
        }

        public static GraphicsIssue conflict(String familyName, String canonicalId,
                                             List<String> files, List<String> memberIds) {
            return new GraphicsIssue(Kind.FAMILY_CONFLICT, familyName, canonicalId,
                    "", "", canonicalId, List.copyOf(files), List.copyOf(memberIds));
        }

        public static GraphicsIssue missingPartner(String subject, String fileName, String wanted) {
            return new GraphicsIssue(Kind.MISSING_PARTNER, "", "", subject, fileName,
                    wanted, List.of(), List.of());
        }

        // 冲突的那几个分支叫什么，用来拼提示文案
        public String memberList() {
            return memberIds.isEmpty() ? String.valueOf(conflictingFiles.size())
                    : String.join(" / ", memberIds);
        }
    }

    public record McreatorHit(ModSource source, int score, List<Evidence> reasons) {
        public Confidence confidence() {
            return Confidence.of(Math.min(1.0, score / 10.0));
        }

        public String label() {
            return source.display();
        }
    }

    public static final class Report {
        // 标题和分析对象存的是文案键而不是现成文本：同一份报告可能在换语言之后再渲染一次
        //（CLI 传 --lang 时就是这样）
        private String titleKey = "report.title.diagnosis";
        private List<Object> titleArgs = List.of();
        private String subjectKey;
        private List<Object> subjectArgs = List.of();
        private String subjectText = "";
        private final java.util.Map<String, String> environment = new java.util.LinkedHashMap<>();
        private final List<Finding> findings = new ArrayList<>();
        private final List<KnownIssue> issues = new ArrayList<>();
        private final List<McreatorHit> mcreatorMods = new ArrayList<>();
        private final List<ModSource> scannedMods = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private final List<String> unmatchedModPackages = new ArrayList<>();
        // 出事前后各个模组都在干什么
        private Timeline timeline = new Timeline(0);
        // 图形类模组：装了哪些、属于哪一族、有没有互斥或缺前置
        private final List<GraphicsModInfo> graphicsMods = new ArrayList<>();
        private final List<GraphicsIssue> graphicsIssues = new ArrayList<>();
        private String rootCauseHeadline = "";
        private int parsedLines;
        private int frameCount;
        private int attributedFrameCount;
        private long generatedAt = System.currentTimeMillis();

        // 报告标题，取的时候才按语言解析
        public String title() {
            return Lang.get(titleKey, titleArgs.toArray());
        }

        public void setTitleKey(String key, Object... args) {
            this.titleKey = key;
            this.titleArgs = List.of(args);
        }

        // 分析对象：给了具体路径就是路径，否则按语言渲染
        public String subject() {
            return subjectKey != null ? Lang.get(subjectKey, subjectArgs.toArray()) : subjectText;
        }

        public void setSubjectKey(String key, Object... args) {
            this.subjectKey = key;
            this.subjectArgs = List.of(args);
        }

        // 设一个不翻译的分析对象，比如文件路径
        public void setSubject(String s) {
            this.subjectText = s == null ? "" : s;
            this.subjectKey = null;
        }

        public java.util.Map<String, String> environment() {
            return environment;
        }

        public List<Finding> findings() {
            return findings;
        }

        public List<KnownIssue> issues() {
            return issues;
        }

        public List<McreatorHit> mcreatorMods() {
            return mcreatorMods;
        }

        public List<ModSource> scannedMods() {
            return scannedMods;
        }

        public List<String> notes() {
            return notes;
        }

        public List<String> unmatchedModPackages() {
            return unmatchedModPackages;
        }

        public Timeline timeline() {
            return timeline;
        }

        public void setTimeline(Timeline t) {
            this.timeline = t == null ? new Timeline(0) : t;
        }

        public List<GraphicsModInfo> graphicsMods() {
            return graphicsMods;
        }

        public void addGraphicsMod(GraphicsModInfo info) {
            graphicsMods.add(info);
        }

        public List<GraphicsIssue> graphicsIssues() {
            return graphicsIssues;
        }

        public void addGraphicsIssue(GraphicsIssue issue) {
            graphicsIssues.add(issue);
        }

        public String rootCauseHeadline() {
            return rootCauseHeadline;
        }

        public void setRootCauseHeadline(String s) {
            this.rootCauseHeadline = s == null ? "" : s;
        }

        public int parsedLines() {
            return parsedLines;
        }

        public void setParsedLines(int n) {
            this.parsedLines = n;
        }

        public int frameCount() {
            return frameCount;
        }

        public void setFrameCount(int n) {
            this.frameCount = n;
        }

        public int attributedFrameCount() {
            return attributedFrameCount;
        }

        public void setAttributedFrameCount(int n) {
            this.attributedFrameCount = n;
        }

        public long generatedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(long t) {
            this.generatedAt = t;
        }

        public void addNote(String note) {
            notes.add(note);
        }

        public void addIssue(KnownIssue issue) {
            issues.add(issue);
        }

        public void addFinding(Finding f) {
            findings.add(f);
        }

        public void addMcreatorHit(McreatorHit hit) {
            mcreatorMods.add(hit);
        }

        // 结论排序并把噪声清掉
        public void finish(double noiseFloor) {
            findings.removeIf(f -> f.score() < noiseFloor);
            findings.sort(Comparator.comparingDouble(Finding::score).reversed());
            for (Finding f : findings) {
                f.sortEvidence();
            }
            issues.sort(Comparator.comparingInt(i -> i.severity().ordinal()));
            mcreatorMods.sort(Comparator.comparingInt(McreatorHit::score).reversed());
        }

        // 最可疑的那一个，可能没有
        public Finding primarySuspect() {
            return findings.isEmpty() ? null : findings.get(0);
        }

        public boolean hasErrors() {
            return !findings.isEmpty() || issues.stream()
                    .anyMatch(i -> i.severity() == Severity.FATAL || i.severity() == Severity.ERROR);
        }

        public String confidenceText(double score) {
            return Confidence.of(score).label() + " (" + Confidence.percent(score) + "%)";
        }
    }
}
