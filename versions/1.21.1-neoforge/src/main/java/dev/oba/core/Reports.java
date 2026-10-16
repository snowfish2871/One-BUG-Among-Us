// 把一份报告渲染成纯文本、Markdown 或 JSON。
package dev.oba.core;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.oba.core.Diagnosis.Confidence;
import dev.oba.core.Diagnosis.Finding;
import dev.oba.core.Diagnosis.KnownIssue;
import dev.oba.core.Diagnosis.McreatorHit;
import dev.oba.core.Diagnosis.Report;
import dev.oba.core.Diagnosis.Severity;

public final class Reports {

    // 时间线在纯文本报告里最多画这么多行
    private static final int TIMELINE_LINES = 40;

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private Reports() {
    }

    // 纯文本

    // 报告开头的品牌框。标题居左，版本号贴右上角，
    // 宽度按显示宽度算（中文占两格），这样中英文两套文案都能对齐
    private static void appendHeader(StringBuilder sb, Theme a, Report r) {
        sb.append(a.headline(r.title(), Oba.ABBR + " v" + Oba.VERSION));
    }

    // 纯文本报告。color 为真时加上终端颜色码
    public static String text(Report r, boolean color) {
        Theme a = Theme.of(color);
        StringBuilder sb = new StringBuilder(16 * 1024);

        appendHeader(sb, a, r);

        // 概览：左标签右值，标签固定 9 格对齐，前面一个圆点表示状态
        sb.append('\n');
        sb.append(a.status(Lang.get("label.generatedAt"),
                TIME.format(Instant.ofEpochMilli(r.generatedAt())), true, 9)).append('\n');
        if (!r.subject().isBlank()) {
            sb.append(a.row(Lang.get("label.subject"), r.subject(), 9)).append('\n');
        }
        if (r.parsedLines() > 0) {
            sb.append(a.row(Lang.get("label.size"),
                    Lang.get("label.size.value", r.parsedLines(), r.frameCount(),
                            r.attributedFrameCount()), 9)).append('\n');
        }
        if (!r.scannedMods().isEmpty()) {
            sb.append(a.row(Lang.get("label.scanned"),
                    Lang.get("label.scanned.value", r.scannedMods().size(),
                            r.environment().getOrDefault("__classes", "?")), 9)).append('\n');
        }
        for (Map.Entry<String, String> e : r.environment().entrySet()) {
            if (e.getKey().startsWith("__")) {
                continue;
            }
            sb.append(a.row(e.getKey(), e.getValue(), 9)).append('\n');
        }
        sb.append(a.row(Lang.get("label.policy"), a.dim(Lang.get("label.policy.value")), 9)).append('\n');

        // 结论
        sb.append('\n').append(a.section(Lang.get("section.verdict"))).append('\n');
        Finding primary = r.primarySuspect();
        if (primary == null) {
            if (r.issues().isEmpty()) {
                sb.append('\n').append("  ").append(a.good(a.DOT + " " + Lang.get("verdict.none"))).append('\n');
                sb.append('\n').append(a.dim("    " + Lang.get("verdict.none.hint"))).append('\n');
            } else {
                sb.append('\n').append("  ").append(a.warn(a.DOT_OFF + " "
                        + Lang.get("verdict.onlyRules", r.issues().size()))).append('\n');
            }
        } else {
            // 元凶用高亮条单独占一行，扫报告时一眼就看见
            String line = " " + a.PLAY + "  " + Lang.get("verdict.culprit") + "  "
                    + primary.display();
            sb.append('\n').append("  ").append(a.bar(line, Theme.BOX_WIDTH - 2)).append('\n').append('\n');
            if (primary.source() != null) {
                sb.append(a.row(Lang.get("verdict.modFile"), primary.source().fileName(), 11)).append('\n');
                String ver = primary.source().version();
                if (!ver.isBlank()) {
                    sb.append(a.row(Lang.get("verdict.version"), ver, 11)).append('\n');
                }
                sb.append(a.row(Lang.get("verdict.path"), a.dim(primary.source().path().toString()), 11))
                  .append('\n');
                sb.append(a.row(Lang.get("verdict.sha1"), a.dim(primary.source().sha1()), 11)).append('\n');
            }
            sb.append(a.row(Lang.get("verdict.confidence"),
                    a.bold(r.confidenceText(primary.score())), 11)).append('\n');
            StackTraceParser.AttributedFrame loc = primary.firstLocatedFrame();
            if (loc != null) {
                sb.append(a.row(Lang.get("verdict.location"), a.warn(loc.location()), 11)).append('\n');
            }
            if (!primary.throwables().isEmpty()) {
                sb.append(a.row(Lang.get("verdict.exception"), primary.throwables().get(0), 11)).append('\n');
            }
            int mcreatorRank = rankOfMcreator(r, primary.source());
            if (mcreatorRank >= 0) {
                McreatorHit hit = r.mcreatorMods().get(mcreatorRank);
                sb.append(a.row(Lang.get("verdict.mcreatorTrait"),
                        a.accent(hit.confidence().label())
                                + " / " + Lang.get("mcreator.score", hit.score(), hit.reasons().size()),
                        11)).append('\n');
            }
            sb.append('\n').append("  ").append(a.good(a.ARROW + " "
                    + Lang.get("verdict.advice", primary.fileName()))).append('\n');
            sb.append(a.dim("    " + Lang.get("verdict.advice.hint"))).append('\n');
        }

        // 可疑模组排序
        if (r.findings().size() > 1) {
            sb.append('\n').append(a.section(Lang.get("section.suspects"))).append('\n');
            int i = 1;
            for (Finding f : r.findings()) {
                // 第一名给个记号，其余留空，宽度不变所以还是对齐的
                String rank = i == 1 ? a.bad(a.PLAY) : " ";
                sb.append("  ").append(rank).append(' ')
                  .append(a.dim(String.format(Locale.ROOT, "%2d. ", i++)))
                  .append(padRight(f.display(), 40))
                  .append(a.dim(f.fileName())).append("  ")
                  .append(a.key(Lang.get("verdict.confidence"))).append(' ')
                  .append(a.bold(f.confidence().label()))
                  .append(a.dim(" (" + Confidence.percent(f.score()) + "%)"))
                  .append("  ").append(a.dim(Lang.get("detail.frames", f.frames().size()))).append('\n');
            }
        }

        // 每个可疑模组的详情
        for (int idx = 0; idx < r.findings().size(); idx++) {
            Finding f = r.findings().get(idx);
            sb.append('\n').append(a.section(Lang.get("section.detail", idx + 1)
                    + "  " + a.SEP + "  " + f.display())).append('\n');
            sb.append('\n');
            sb.append(a.row(Lang.get("detail.file"), a.dim(f.source() == null ? "?" : f.source().path().toString()), 11))
              .append('\n');
            sb.append(a.row(Lang.get("verdict.confidence"),
                    a.bold(f.confidence().label())
                            + a.dim(" (" + Confidence.percent(f.score()) + "%)  "
                                    + Lang.get("detail.score", String.format(Locale.ROOT, "%.2f", f.score()))),
                    11)).append('\n');

            if (!f.throwables().isEmpty()) {
                sb.append('\n').append("  ").append(a.key(Lang.get("detail.throwables"))).append('\n');
                for (String t : f.throwables()) {
                    sb.append(a.item(a.bad(t))).append('\n');
                }
            }

            sb.append('\n').append("  ").append(a.key(Lang.get("detail.locations"))).append('\n');
            List<String> shown = new ArrayList<>();
            for (StackTraceParser.AttributedFrame af : f.frames()) {
                String loc = af.location();
                if (shown.contains(loc)) {
                    continue;
                }
                shown.add(loc);
                sb.append("    ").append(a.warn(a.ARROW + " ")).append(a.value(loc))
                  .append(a.dim("   [" + matchLabel(af.match()) + "]")).append('\n');
                if (shown.size() >= 8) {
                    int rest = f.frames().size() - shown.size();
                    if (rest > 0) {
                        sb.append(a.dim("    " + Lang.get("detail.moreFrames", rest))).append('\n');
                    }
                    break;
                }
            }

            sb.append('\n').append("  ").append(a.key(Lang.get("detail.evidence"))).append('\n');
            for (Diagnosis.Evidence e : f.evidence()) {
                sb.append("    ").append(a.dim(String.format(Locale.ROOT, "+%-5.2f ", e.weight())))
                  .append(a.value(e.detail()));
                if (e.line() > 0) {
                    sb.append(a.dim("  (" + Lang.get("detail.line", e.line()) + ")"));
                }
                sb.append('\n');
            }
        }

        // 事故时间线：出事当时和出事之前，各个模组分别做了什么
        if (!r.timeline().entries().isEmpty()) {
            sb.append('\n').append(a.section(Lang.get("section.timeline"))).append('\n');
            sb.append('\n').append(a.dim("  " + Lang.get("section.timeline.hint"))).append('\n');
            sb.append(a.dim("  " + Lang.get("timeline.columns"))).append('\n');
            List<Timeline.Entry> entries = r.timeline().reversed();
            int shown = 0;
            boolean elided = false;
            for (Timeline.Entry e : entries) {
                if (shown >= TIMELINE_LINES) {
                    elided = true;
                    break;
                }
                sb.append("  ").append(a.dim(padLeft(e.time(), 12))).append(' ')
                  .append(a.brand(pad(e.modId(), 18))).append(' ')
                  .append(typeMark(a, e.type())).append(' ');
                String text = e.renderText();
                sb.append(e.type() == Timeline.Type.ERROR || e.type() == Timeline.Type.FATAL
                        ? a.bad(text) : a.value(text));
                sb.append('\n');
                shown++;
            }
            if (elided) {
                sb.append(a.dim("  " + Lang.get("timeline.more", entries.size() - shown))).append('\n');
            }
            sb.append(a.dim("  " + Lang.get("timeline.readHint"))).append('\n');
        }

        // 图形类模组：装了哪些、有没有互斥或缺前置
        if (!r.graphicsMods().isEmpty() || !r.graphicsIssues().isEmpty()) {
            sb.append('\n').append(a.section(Lang.get("section.graphics"))).append('\n');
            sb.append('\n').append(a.dim("  " + Lang.get("section.graphics.hint"))).append('\n');

            for (Diagnosis.GraphicsIssue issue : r.graphicsIssues()) {
                boolean conflict = issue.kind() == Diagnosis.GraphicsIssue.Kind.FAMILY_CONFLICT;
                String title = conflict
                        ? Lang.get("gfx.issue.conflict.title", familyLabel(issue.familyName()))
                        : Lang.get("gfx.issue.missing.title", issue.subject());
                sb.append('\n').append("  ").append(a.bad(a.CROSS + " ")).append(a.bold(title)).append('\n');
                if (conflict) {
                    for (String f : issue.conflictingFiles()) {
                        sb.append("      ").append(a.dim("- ")).append(a.value(f)).append('\n');
                    }
                    sb.append("    ").append(a.good(Lang.get("detail.suggest",
                            Lang.get("gfx.issue.conflict.advice", issue.memberList()))))
                      .append('\n');
                } else {
                    sb.append("    ").append(a.dim(issue.fileName())).append('\n');
                    sb.append("    ").append(a.good(Lang.get("detail.suggest",
                            Lang.get("gfx.issue.missing.advice", issue.subject(), issue.wanted()))))
                      .append('\n');
                }
            }

            if (!r.graphicsMods().isEmpty()) {
                sb.append('\n');
                for (Diagnosis.GraphicsModInfo g : r.graphicsMods()) {
                    sb.append(a.item(a.value(pad(g.displayName(), 26)) + " "
                            + a.dim(pad(g.fileName(), 34)) + " "
                            + a.dim(pad(g.version(), 12)) + " "
                            + a.brand(familyLabel(g.familyName())) + " "
                            + a.dim(g.canonical() ? Lang.get("gfx.canonical") : Lang.get("gfx.fork"))))
                      .append('\n');
                    if (g.note() != null && !g.note().isBlank()) {
                        sb.append(a.dim("      " + g.note())).append('\n');
                    }
                }
            }
        }

        // 规则命中的问题
        if (!r.issues().isEmpty()) {
            sb.append('\n').append(a.section(Lang.get("section.issues.sorted"))).append('\n');
            Severity lastSeverity = null;
            for (KnownIssue issue : r.issues()) {
                if (issue.severity() != lastSeverity) {
                    lastSeverity = issue.severity();
                    sb.append('\n').append("  ").append(a.dim("── "))
                      .append(severityLabel(a, issue.severity()))
                      .append(a.dim(" " + "─".repeat(Math.max(2,
                              Theme.BOX_WIDTH - 8 - displayWidth(issue.severity().label())))))
                      .append('\n');
                }
                sb.append(a.item(a.bold(issue.title()) + a.dim("  [" + issue.rule() + "]"))).append('\n');
                sb.append("      ").append(a.value(issue.detail())).append('\n');
                if (issue.source() != null) {
                    sb.append("      ").append(a.brand(Lang.get("issue.relatedMod") + ": "))
                      .append(a.value(issue.source().display())).append("  ")
                      .append(a.dim(issue.source().fileName())).append('\n');
                } else if (!issue.suspects().isEmpty()) {
                    sb.append("      ").append(a.brand(Lang.get("issue.relatedFiles") + ": "))
                      .append(a.value(String.join(", ", issue.suspects()))).append('\n');
                }
                sb.append("      ").append(a.good(Theme.ARROW + " " + issue.advice())).append('\n');
            }
        }

        // MCreator 识别结果
        if (!r.mcreatorMods().isEmpty()) {
            sb.append('\n').append(a.section(Lang.get("section.mcreator")
                    + " (" + r.mcreatorMods().size() + ")")).append('\n');
            for (McreatorHit hit : r.mcreatorMods()) {
                sb.append('\n').append(a.item(a.accent(hit.label()) + "  "
                        + a.dim(hit.source().fileName()) + "  "
                        + a.key(Lang.get("verdict.confidence")) + " "
                        + a.bold(hit.confidence().label())
                        + a.dim(" (" + Lang.get("mcreator.score", hit.score(), hit.reasons().size()) + ")")))
                  .append('\n');
                for (Diagnosis.Evidence e : hit.reasons()) {
                    sb.append("      ").append(a.dim("- ")).append(a.value(e.detail())).append('\n');
                }
            }
            sb.append('\n').append(a.dim("  " + Lang.get("mcreator.note"))).append('\n');
        }

        // 没能归属的包
        if (!r.unmatchedModPackages().isEmpty()) {
            sb.append('\n').append(a.section(Lang.get("section.unmatched"))).append('\n');
            sb.append('\n').append(a.dim("  " + Lang.get("section.unmatched.hint"))).append('\n');
            List<String> list = r.unmatchedModPackages();
            for (int i = 0; i < Math.min(list.size(), 20); i++) {
                sb.append("  ").append(a.dim("- ")).append(a.value(list.get(i)))
                  .append(i % 4 == 3 ? "\n" : "    ");
            }
            if (list.size() % 4 != 0) {
                sb.append('\n');
            }
        }

        for (String note : r.notes()) {
            sb.append('\n').append(a.dim(Lang.get("note.prefix", note)));
        }

        sb.append('\n').append(a.rule()).append('\n');
        sb.append("  ").append(a.good(a.CHECK + " ")).append(a.dim(Lang.get("footer.readonly"))).append('\n');
        return sb.toString();
    }

    private static int rankOfMcreator(Report r, ModSource source) {
        if (source == null) {
            return -1;
        }
        for (int i = 0; i < r.mcreatorMods().size(); i++) {
            if (r.mcreatorMods().get(i).source() == source) {
                return i;
            }
        }
        return -1;
    }

    private static String matchLabel(ClassIndex.Match m) {
        if (m == null) {
            return "?";
        }
        if (m.confidence() >= 0.999) {
            return Lang.get("detail.matchExact");
        }
        return Lang.get("detail.matchPackage", m.matchedKey(),
                String.format(Locale.ROOT, "%.0f", m.confidence() * 100));
    }

    private static String severityLabel(Theme a, Severity s) {
        return switch (s) {
            case FATAL -> a.bad(s.label());
            case ERROR -> a.warn(s.label());
            case WARN -> a.brand(s.label());
            case INFO -> a.dim(s.label());
        };
    }

    // 族的名字也走文案表，报告换语言时跟着换
    private static String familyLabel(String familyName) {
        if (familyName == null || familyName.isBlank()) {
            return "";
        }
        return switch (familyName.toLowerCase(Locale.ROOT)) {
            case "sodium" -> Lang.get("gfx.family.sodium");
            case "iris" -> Lang.get("gfx.family.iris");
            case "lithium" -> Lang.get("gfx.family.lithium");
            case "krypton" -> Lang.get("gfx.family.krypton");
            case "phosphor" -> Lang.get("gfx.family.phosphor");
            default -> familyName;
        };
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder(s);
        while (displayWidth(sb.toString()) < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // 往左补空格，时间戳那一列要右对齐才好对
    private static String padLeft(String s, int width) {
        if (s == null) {
            s = "";
        }
        StringBuilder sb = new StringBuilder();
        int w = displayWidth(s);
        while (w + sb.length() < width) {
            sb.append(' ');
        }
        return sb.append(s).toString();
    }

    // 时间线上每条前面那个标记
    private static String typeMark(Theme a, Timeline.Type t) {
        return switch (t) {
            case ERROR, FATAL -> a.bad("!!");
            case WARN -> a.warn(" !");
            case RULE -> a.warn(" *");
            case LIFECYCLE -> a.dim(" +");
            case INFO -> a.dim(" ·");
        };
    }

    private static String padRight(String s, int width) {
        if (s == null) {
            return "";
        }
        String t = s.length() > width ? s.substring(0, width - 1) + "…" : s;
        StringBuilder sb = new StringBuilder(t);
        while (displayWidth(sb.toString()) < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // 算显示宽度时把中日韩字符当两个字符宽，好对齐
    public static int displayWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += charWidth(s.charAt(i));
        }
        return w;
    }

    // 一个字符在终端里占几格。
    // CJK 及其标点算两格，但 U+2500 到 U+259F 这一段要排除掉——
    // 制表符、方块元素、几何图形都落在这个区间里，它们实际只占一格。
    // 不排除的话报告边框会右边对不齐（制表符被当成两格算了）。
    private static int charWidth(char c) {
        if (c >= 0x2500 && c <= 0x259F) {
            return 1;
        }
        if (c >= 0x1100 && (c <= 0x115F
                || (c >= 0x2E80 && c <= 0xA4CF)
                || (c >= 0xAC00 && c <= 0xD7A3)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0xFE30 && c <= 0xFE6F)
                || (c >= 0xFF00 && c <= 0xFF60)
                || (c >= 0xFFE0 && c <= 0xFFE6))) {
            return 2;
        }
        return 1;
    }

    // 补齐到指定显示宽度，宽字符算两个
    public static String padTo(String s, int width) {
        String v = s == null ? "" : s;
        StringBuilder sb = new StringBuilder(v);
        int w = displayWidth(v);
        while (w < width) {
            sb.append(' ');
            w++;
        }
        return sb.toString();
    }

    // 画一张左对齐的文本表格，单元格里是中文也能对齐。
    // indents 是第一列前面留的空格数
    public static String table(List<String> headers, List<List<String>> rows, int indents) {
        int cols = headers.size();
        int[] widths = new int[cols];
        for (int c = 0; c < cols; c++) {
            widths[c] = displayWidth(headers.get(c));
        }
        for (List<String> row : rows) {
            for (int c = 0; c < cols && c < row.size(); c++) {
                widths[c] = Math.max(widths[c], displayWidth(row.get(c)));
            }
        }
        String indent = " ".repeat(Math.max(0, indents));
        StringBuilder sb = new StringBuilder();
        StringBuilder head = new StringBuilder(indent);
        for (int c = 0; c < cols; c++) {
            head.append(padTo(headers.get(c), widths[c]));
            if (c < cols - 1) {
                head.append("  ");
            }
        }
        sb.append(head.toString().stripTrailing()).append('\n');
        int total = displayWidth(head.toString().stripTrailing());
        sb.append(indent).append("─".repeat(Math.max(10, total))).append('\n');
        for (List<String> row : rows) {
            StringBuilder line = new StringBuilder(indent);
            for (int c = 0; c < cols; c++) {
                line.append(padTo(c < row.size() ? row.get(c) : "", widths[c]));
                if (c < cols - 1) {
                    line.append("  ");
                }
            }
            sb.append(line.toString().stripTrailing()).append('\n');
        }
        return sb.toString();
    }


    // Markdown 版的图形模组小节
    private static void markdownGraphics(StringBuilder sb, Report r) {
        if (r.graphicsMods().isEmpty() && r.graphicsIssues().isEmpty()) {
            return;
        }
        sb.append("## ").append(Lang.get("section.graphics")).append("\n\n");

        for (Diagnosis.GraphicsIssue issue : r.graphicsIssues()) {
            boolean conflict = issue.kind() == Diagnosis.GraphicsIssue.Kind.FAMILY_CONFLICT;
            sb.append("> ⚠️ **").append(conflict
                            ? Lang.get("gfx.issue.conflict.title", familyLabel(issue.familyName()))
                            : Lang.get("gfx.issue.missing.title", issue.subject()))
              .append("**\n>\n> ").append(conflict
                            ? Lang.get("gfx.issue.conflict.advice", issue.memberList())
                            : Lang.get("gfx.issue.missing.advice", issue.subject(), issue.wanted()))
              .append("\n\n");
            if (conflict) {
                for (String f : issue.conflictingFiles()) {
                    sb.append("- `").append(f).append("`\n");
                }
                sb.append('\n');
            }
        }

        if (!r.graphicsMods().isEmpty()) {
            sb.append("| ").append(Lang.get("gfx.col.mod")).append(" | ")
              .append(Lang.get("gfx.col.family")).append(" | ")
              .append(Lang.get("gfx.col.loader")).append(" | ")
              .append(Lang.get("gfx.col.note")).append(" |\n|---|---|---|---|\n");
            for (Diagnosis.GraphicsModInfo g : r.graphicsMods()) {
                sb.append("| ").append(mdEscape(g.displayName())).append(" | ")
                  .append(familyLabel(g.familyName())).append(" | ")
                  .append(mdEscape(g.loader())).append(" | ")
                  .append(g.canonical() ? Lang.get("gfx.canonical") : Lang.get("gfx.fork"))
                  .append(g.note() == null || g.note().isBlank() ? "" : " — " + mdEscape(g.note()))
                  .append(" |\n");
            }
            sb.append('\n');
        }
    }

    // 时间线在 Markdown 报告里最多列这么多行
    private static final int TIMELINE_MD_LINES = 30;

    // Markdown 版的时间线：一个三列表格
    private static void markdownTimeline(StringBuilder sb, Report r) {
        if (r.timeline().entries().isEmpty()) {
            return;
        }
        sb.append("## ").append(Lang.get("section.timeline")).append("\n\n");
        sb.append("> ").append(Lang.get("section.timeline.hint")).append("\n\n");
        sb.append("| ").append(Lang.get("timeline.col.time"))
          .append(" | ").append(Lang.get("timeline.col.mod"))
          .append(" | ").append(Lang.get("timeline.col.event")).append(" |\n");
        sb.append("|---|---|---|\n");
        List<Timeline.Entry> entries = r.timeline().reversed();
        int shown = 0;
        for (Timeline.Entry e : entries) {
            if (shown >= TIMELINE_MD_LINES) {
                break;
            }
            sb.append("| `").append(e.time()).append("` | ")
              .append(e.modId()).append(" | ")
              .append(mdEscape(e.renderText())).append(" |\n");
            shown++;
        }
        if (entries.size() > shown) {
            sb.append("\n").append(Lang.get("timeline.more", entries.size() - shown)).append("\n");
        }
        sb.append('\n');
    }

    // 表格里竖线会把列切断，转义一下
    private static String mdEscape(String s) {
        return s == null ? "" : s.replace("|", "\\|").replace("\n", " ");
    }

    // JSON 版的时间线：数组，字段名保持英文稳定，方便脚本消费
    private static void jsonTimeline(Json j, Report r) {
        j.key("timeline");
        j.arrStart();
        for (Timeline.Entry e : r.timeline().reversed()) {
            j.objStart();
            j.field("time", e.time());
            j.field("modId", e.modId());
            if (e.modName() != null && !e.modName().isBlank()) {
                j.field("modName", e.modName());
            }
            j.field("type", e.type().name().toLowerCase(Locale.ROOT));
            j.field("event", e.renderText());
            j.objEnd();
        }
        j.arrEnd();
    }

    // JSON

    // 给程序读的报告。手写的，免得为这点东西引一个 JSON 库
    public static String json(Report r) {
        Json j = new Json();
        j.objStart();
        j.field("tool", "Oba");
        j.field("version", Oba.VERSION);
        j.field("generatedAt", TIME.format(Instant.ofEpochMilli(r.generatedAt())));
        j.field("subject", r.subject());
        j.field("rootCause", r.rootCauseHeadline());
        j.field("parsedLines", r.parsedLines());
        j.field("frames", r.frameCount());
        j.field("attributedFrames", r.attributedFrameCount());

        j.key("environment");
        j.objStart();
        for (Map.Entry<String, String> e : r.environment().entrySet()) {
            if (!e.getKey().startsWith("__")) {
                j.field(e.getKey(), e.getValue());
            }
        }
        j.objEnd();

        j.key("suspects");
        j.arrStart();
        for (Finding f : r.findings()) {
            j.objStart();
            j.field("modId", f.source() == null ? "" : String.join(",", f.source().modIds()));
            j.field("name", f.display());
            j.field("file", f.fileName());
            j.field("path", f.source() == null ? "" : f.source().path().toString());
            j.field("version", f.source() == null ? "" : f.source().version());
            j.field("sha1", f.source() == null ? "" : f.source().sha1());
            j.field("score", Math.round(f.score() * 1000) / 1000.0);
            j.field("confidence", f.confidence().id());
            StackTraceParser.AttributedFrame loc = f.firstLocatedFrame();
            j.field("location", loc == null ? "" : loc.location());
            j.field("exception", f.throwables().isEmpty() ? "" : f.throwables().get(0));
            j.key("frames");
            j.arrStart();
            for (StackTraceParser.AttributedFrame af : f.frames()) {
                j.objStart();
                j.field("location", af.location());
                j.field("class", af.frame().className());
                j.field("method", af.frame().methodName());
                j.field("file", af.frame().sourceFile());
                j.field("line", af.frame().lineNumber());
                j.field("matchKey", af.match() == null ? "" : af.match().matchedKey());
                j.field("matchConfidence", af.match() == null ? 0.0 : af.match().confidence());
                j.objEnd();
            }
            j.arrEnd();
            j.key("evidence");
            j.arrStart();
            for (Diagnosis.Evidence e : f.evidence()) {
                j.objStart();
                j.field("rule", e.rule());
                j.field("detail", e.detail());
                j.field("weight", Math.round(e.weight() * 1000) / 1000.0);
                j.field("line", e.line());
                j.objEnd();
            }
            j.arrEnd();
            j.objEnd();
        }
        j.arrEnd();

        j.key("issues");
        j.arrStart();
        for (KnownIssue i : r.issues()) {
            j.objStart();
            j.field("rule", i.rule());
            j.field("severity", i.severity().id());
            j.field("title", i.title());
            j.field("detail", i.detail());
            j.field("advice", i.advice());
            j.field("modId", i.source() == null ? "" : String.join(",", i.source().modIds()));
            j.field("file", i.source() == null ? "" : i.source().fileName());
            j.field("line", i.line());
            j.key("suspects");
            j.arrStart();
            for (String s : i.suspects()) {
                j.value(s);
            }
            j.arrEnd();
            j.objEnd();
        }
        j.arrEnd();

        j.key("mcreatorMods");
        j.arrStart();
        for (McreatorHit h : r.mcreatorMods()) {
            j.objStart();
            j.field("modId", String.join(",", h.source().modIds()));
            j.field("name", h.label());
            j.field("file", h.source().fileName());
            j.field("score", h.score());
            j.field("confidence", h.confidence().id());
            j.key("reasons");
            j.arrStart();
            for (Diagnosis.Evidence e : h.reasons()) {
                j.value(e.detail());
            }
            j.arrEnd();
            j.objEnd();
        }
        j.arrEnd();

        jsonTimeline(j, r);

        j.key("mods");
        j.arrStart();
        for (ModSource s : r.scannedMods()) {
            j.objStart();
            j.field("file", s.fileName());
            j.field("modIds", String.join(",", s.modIds()));
            j.field("version", s.version());
            j.field("classes", s.classNames().size());
            j.field("size", s.size());
            j.field("sha1", s.sha1());
            j.objEnd();
        }
        j.arrEnd();

        j.key("unmatchedPackages");
        j.arrStart();
        for (String s : r.unmatchedModPackages()) {
            j.value(s);
        }
        j.arrEnd();

        j.objEnd();
        return j.toString();
    }

    private static final class Json {
        private final StringBuilder sb = new StringBuilder(8192);
        private final java.util.ArrayDeque<Boolean> first = new java.util.ArrayDeque<>();
        private boolean pendingKey;

        void objStart() {
            comma();
            sb.append('{');
            first.push(Boolean.TRUE);
        }

        void objEnd() {
            sb.append('}');
            first.pop();
        }

        void arrStart() {
            comma();
            sb.append('[');
            first.push(Boolean.TRUE);
        }

        void arrEnd() {
            sb.append(']');
            first.pop();
        }

        void key(String k) {
            comma();
            sb.append('"').append(escape(k)).append("\":");
            pendingKey = true;
        }

        void value(String v) {
            comma();
            sb.append('"').append(escape(v)).append('"');
        }

        void field(String k, String v) {
            key(k);
            sb.append('"').append(escape(v == null ? "" : v)).append('"');
            pendingKey = false;
        }

        void field(String k, double v) {
            key(k);
            sb.append(v);
            pendingKey = false;
        }

        void field(String k, int v) {
            key(k);
            sb.append(v);
            pendingKey = false;
        }

        void field(String k, long v) {
            key(k);
            sb.append(v);
            pendingKey = false;
        }

        private void comma() {
            if (pendingKey) {
                pendingKey = false;
                return;
            }
            Boolean f = first.peek();
            if (f == null) {
                return;
            }
            if (f) {
                first.pop();
                first.push(Boolean.FALSE);
            } else {
                sb.append(',');
            }
        }

        private static String escape(String s) {
            StringBuilder o = new StringBuilder(s.length() + 8);
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '"' -> o.append("\\\"");
                    case '\\' -> o.append("\\\\");
                    case '\n' -> o.append("\\n");
                    case '\r' -> o.append("\\r");
                    case '\t' -> o.append("\\t");
                    default -> {
                        if (c < 0x20) {
                            o.append(String.format("\\u%04x", (int) c));
                        } else {
                            o.append(c);
                        }
                    }
                }
            }
            return o.toString();
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    // Markdown

    // Markdown 格式，适合贴到 issue 或求助帖里
    public static String markdown(Report r) {
        StringBuilder sb = new StringBuilder(12 * 1024);
        sb.append("# ").append(r.title()).append("\n\n");
        sb.append("- ").append(Lang.get("label.generatedAt")).append(": ")
          .append(TIME.format(Instant.ofEpochMilli(r.generatedAt()))).append('\n');
        if (!r.subject().isBlank()) {
            sb.append("- ").append(Lang.get("label.subject")).append(": `")
              .append(r.subject()).append("`\n");
        }
        for (Map.Entry<String, String> e : r.environment().entrySet()) {
            if (!e.getKey().startsWith("__")) {
                sb.append("- ").append(e.getKey()).append(": ").append(e.getValue()).append('\n');
            }
        }
        sb.append('\n');

        Finding primary = r.primarySuspect();
        sb.append("## ").append(Lang.get("section.verdict")).append("\n\n");
        if (primary == null) {
            sb.append(Lang.get("verdict.none")).append("\n\n");
        } else {
            sb.append("**").append(Lang.get("verdict.culprit")).append(": ")
              .append(primary.display()).append("**\n\n");
            sb.append("| ").append(Lang.get("label.subject")).append(" | ")
              .append(Lang.get("verdict.confidence")).append(" |\n|---|---|\n");
            sb.append("| `").append(primary.fileName()).append("` | ")
              .append(primary.confidence().label())
              .append(" (").append(Confidence.percent(primary.score())).append("%) |\n");
            if (!primary.source().version().isBlank()) {
                sb.append("| ").append(Lang.get("verdict.version")).append(": ")
                  .append(primary.source().version()).append(" | |\n");
            }
            StackTraceParser.AttributedFrame loc = primary.firstLocatedFrame();
            if (loc != null) {
                sb.append("| ").append(Lang.get("verdict.location")).append(" | `")
                  .append(loc.location()).append("` |\n");
            }
            sb.append('\n');
        }

        // 时间线紧跟结论：不管有没有点出元凶，出事前后发生了什么都是最有用的信息
        markdownTimeline(sb, r);
        markdownGraphics(sb, r);

        if (r.findings().size() > 1) {
            sb.append("## ").append(Lang.get("section.suspects")).append("\n\n| # | ")
              .append(Lang.get("cli.col.modId")).append(" | ").append(Lang.get("cli.col.file"))
              .append(" | ").append(Lang.get("verdict.confidence")).append(" | ")
              .append(Lang.get("cli.col.classes")).append(" |\n|---|---|---|---|---|\n");
            int i = 1;
            for (Finding f : r.findings()) {
                sb.append("| ").append(i++).append(" | ").append(f.display()).append(" | `")
                  .append(f.fileName()).append("` | ").append(f.confidence().label())
                  .append(" (").append(Confidence.percent(f.score())).append("%) | ")
                  .append(f.frames().size()).append(" |\n");
            }
            sb.append('\n');
        }

        for (int idx = 0; idx < r.findings().size(); idx++) {
            Finding f = r.findings().get(idx);
            sb.append("## ").append(Lang.get("section.detail", idx + 1)).append(": ")
              .append(f.display()).append("\n\n");
            if (!f.throwables().isEmpty()) {
                sb.append(Lang.get("verdict.exception")).append(": `")
                  .append(f.throwables().get(0)).append("`\n\n");
            }
            sb.append("### ").append(Lang.get("detail.locations")).append("\n\n```\n");
            List<String> shown = new ArrayList<>();
            for (StackTraceParser.AttributedFrame af : f.frames()) {
                if (shown.contains(af.location())) {
                    continue;
                }
                shown.add(af.location());
                sb.append(af.location()).append('\n');
                if (shown.size() >= 10) {
                    break;
                }
            }
            sb.append("```\n\n### ").append(Lang.get("detail.evidence")).append("\n\n");
            for (Diagnosis.Evidence e : f.evidence()) {
                sb.append("- `").append(e.rule()).append("` (+")
                  .append(String.format(Locale.ROOT, "%.2f", e.weight())).append(") ")
                  .append(e.detail()).append('\n');
            }
            sb.append('\n');
        }

        if (!r.issues().isEmpty()) {
            sb.append("## ").append(Lang.get("section.issues")).append("\n\n");
            for (KnownIssue i : r.issues()) {
                sb.append("### [").append(i.severity().label()).append("] ")
                  .append(i.title()).append("\n\n");
                sb.append("- ").append(i.rule()).append('\n');
                sb.append("- ").append(i.detail()).append('\n');
                if (i.source() != null) {
                    sb.append("- ").append(Lang.get("issue.relatedMod")).append(": `")
                      .append(i.source().fileName()).append("`\n");
                }
                sb.append("- ").append(Lang.get("detail.suggest", i.advice())).append("\n\n");
            }
        }

        if (!r.mcreatorMods().isEmpty()) {
            sb.append("## ").append(Lang.get("section.mcreator")).append("\n\n| ")
              .append(Lang.get("cli.col.modId")).append(" | ").append(Lang.get("cli.col.file"))
              .append(" | ").append(Lang.get("verdict.confidence")).append(" | ")
              .append(Lang.get("section.mcreator")).append(" |\n|---|---|---|---|\n");
            for (McreatorHit h : r.mcreatorMods()) {
                sb.append("| ").append(h.label()).append(" | `").append(h.source().fileName()).append("` | ")
                  .append(h.confidence().label()).append(" (").append(h.score()).append(") | ")
                  .append(h.reasons().stream().map(Diagnosis.Evidence::detail)
                          .reduce((x, y) -> x + "; " + y).orElse("")).append(" |\n");
            }
            sb.append('\n');
        }

        sb.append("---\n\n*").append(Lang.get("footer.readonly")).append("*\n");
        return sb.toString();
    }

    // 把报告写到文件旁边，返回写入的路径
    public static Path writeSidecar(Report r, String baseName, String suffix, String content,
                                    Path directory) throws java.io.IOException {
        Path dir = directory;
        if (dir == null) {
            dir = Paths.get(".").toAbsolutePath();
        }
        java.nio.file.Files.createDirectories(dir);
        String name = baseName + "-oba" + suffix;
        Path out = dir.resolve(name);
        java.nio.file.Files.writeString(out, content);
        return out;
    }
}
