# -*- coding: utf-8 -*-
"""Generate the root README and the nine per-track READMEs, in Chinese and English.

The prose lives in tools/readme_zh.py and tools/readme_en.py; this script assembles the parts that
depend on a track (table, build chain, adapter, limits) and writes the files.

    python tools/gen-readmes.py
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))

import readme_en
import readme_zh
import track_info

TRACKS = track_info.TRACKS

CAT_EN = {
    "加载器与依赖": "Loader and dependencies",
    "模组文件本身": "The mod file itself",
    "Mixin": "Mixin",
    "类加载与链接": "Class loading and linking",
    "注册表与资源": "Registries and resources",
    "存档与地图": "Saves and maps",
    "渲染与图形": "Rendering and graphics",
    "内存与卡死": "Memory and stalls",
    "服务端与客户端": "Server and client",
    "模组生命周期": "Mod lifecycle",
    "Sinytra Connector": "Sinytra Connector",
}

ORDER = [
    "1.20.1-forge",
    "1.20.2-1.20.4-forge",
    "1.20.2-1.20.4-neoforge",
    "1.20.5-1.20.6-forge",
    "1.20.5-1.20.6-neoforge",
    "1.21.1-forge",
    "1.21.1-neoforge",
    "26.2-forge",
    "26.2-neoforge",
]

PLUGIN_RE = re.compile(r"id\s+'([\w.]+)'\s+version\s+'([^']+)'")


def as_text(v):
    if isinstance(v, str):
        return v
    return "\n".join(str(x) for x in v)


def status_pair(t, lang):
    key = "status_zh" if lang == "zh" else "status_en"
    if t.get(key):
        return t[key]
    built, verified = t["built"], t["verified"]
    if lang == "zh":
        if built and verified:
            return "已出包，已实机验证"
        if built:
            return "已出包，未实机"
        return "代码就绪，未出包"
    if built and verified:
        return "built, verified in game"
    if built:
        return "built, not tested in game"
    return "code ready, no jar yet"


def build_chain(name, t_mc):
    """Read the real build script: plugin id/version and whether reobf is involved."""
    gradle = ROOT / "versions" / name / "build.gradle"
    plugin = ""
    if gradle.is_file():
        text = gradle.read_text(encoding="utf-8", errors="replace")
        for pid, ver in PLUGIN_RE.findall(text):
            if pid in ("net.minecraftforge.gradle", "net.neoforged.gradle.userdev", "net.neoforged.moddev"):
                plugin = "%s %s" % (pid, ver)
                break
        reobf = "reobfJar" in text and "neoforge" not in name and (
            "1.20.1" in t_mc or "1.20.2" in t_mc)
    else:
        reobf = False
    return plugin, reobf


def bullets(text):
    out = []
    for ln in as_text(text).strip().splitlines():
        ln = ln.strip()
        if not ln:
            continue
        if ln[0] in "-*|":
            out.append(ln)
        else:
            out.append("- " + ln)
    return "\n".join(out)


def cats_table(lang):
    rows = []
    for cat, rules in readme_zh.RULES:
        ids = ", ".join("`%s`" % r[0] for r in rules[:2]) if lang == "en" \
            else "、".join("`%s`" % r[0] for r in rules[:2])
        name = CAT_EN.get(cat, cat) if lang == "en" else cat
        rows.append("| %s | %d | %s |" % (name, len(rules), ids))
    return "\n".join(rows)


def intro(name, t, lang):
    loader = t["loader_" + ("en" if lang == "en" else "zh")].replace("**", "")
    if lang == "en":
        return ("> One self-contained track: **%s**, loader %s, Java %s. Sources, resources and the build\n"
                "> script all live in this directory, so it can be copied out and built on its own. The\n"
                "> repository overview and the full rule set are in the [root README](../../README.en.md)."
                % (t["mc_en"], loader, t["java"]))
    return ("> 这个目录是**%s**（%s）的一条独立轨道：源码、资源、构建脚本都在这里，单独拷出去也能构建。\n"
            "> 仓库总览和完整的规则说明在[根 README](../../README.md)。" % (t["mc_zh"], loader))


def track_zh(name, t):
    plugin, reobf = build_chain(name, t["mc_zh"])
    plugin_line = plugin or "（按轨道自带的 `build.gradle`）"
    if reobf:
        reobf_zh = "需要 reobf：1.20.4 及更早的 Forge 用 SRG 成员名，构建最后一步要把类名换回 SRG。"
    elif "neoforge" in name:
        reobf_zh = ""
    else:
        reobf_zh = "不需要 reobf：1.20.5 起 Forge 取消了这一层。"
    reobf_zh_sep = ("；" + reobf_zh) if reobf_zh else ""

    rows = [
        ("Minecraft", t["mc_zh"]),
        ("加载器", t["loader_zh"]),
        ("Java", t["java"]),
        ("编译目标", t["target"]),
        ("描述文件", "`%s`" % t["toml"]),
        ("产物", "`%s`" % t["jar"]),
        ("状态", status_pair(t, "zh")),
    ]
    table = "\n".join("| %s | %s |" % (k, v) for k, v in rows)

    note_block = ""

    build_note = bullets(t.get("build_note_zh", "")) if t.get("build_note_zh") else (
        "- 首次构建要从 `maven.neoforged.net` / `maven.minecraftforge.net` 拉依赖；走代理时在 "
        "`gradle.properties` 里加 `systemProp.https.proxyHost` 和 `systemProp.https.proxyPort`。")

    return f"""# One BUG Among Us · {name}

{intro(name, t, 'zh')}

## 这一条轨道

| 项 | 值 |
|---|---|
{table}
{note_block}
## 适配了哪些差异

- **描述文件**：`{t['toml']}`。它是占位符模板，构建时把版本号展开再打进 jar；换成别的名字加载器就认不出来。
- **构建链**：{plugin_line}{reobf_zh_sep}
- **tick 适配**：{as_text(t['tick_zh']).strip()}
- **适配层代码**：{as_text(t['adapter_zh']).strip()}

## 引擎怎么工作（所有轨道共用同一份代码）

1. 采集：log4j2 appender 接住 ERROR / FATAL；`crash-reports/` 里的新文件同样进来。
2. 建索引：扫 `mods/*.jar`，得到「类名 → 模组」与「包前缀 → 模组」两张表。
3. 匹配规则：49 条正则逐行跑，一行日志只出一条最具体的结论。
4. 认栈帧：每帧的类名先查索引，查不到用包前缀缩小范围。
5. 打分：离根因的距离、规则严重度、模组 id 是否出现在正文、多帧是否互相印证。
6. 出报告：一句话结论、元凶卡片（模组 / 文件 / 首帧 / 可信度）、问题清单、建议。

索引不能只信加载器给的路径（NeoForge 的联合文件系统路径不是普通文件，只按它扫 16 个模组只认出
3 个），所以还会直接扫 `mods/*.jar` 合并去重；`libraries/` 下的平台 jar 一律排除，否则结论会指到
加载器头上；建索引在后台线程池里跑，模组目录（文件名、大小、修改时间）没变就不重扫。

归因只走两条路：整类名在索引里，或包前缀在索引里。两条都不通时只给建议、不硬指模组。可信度按
信号加权（首帧落在该模组、异常链多帧指向同一处、规则是致命级、模组 id 出现在正文 → 加分；只有
包片段匹配、栈帧落在库代码 → 减分），低于阈值不下结论。

## 处理得了的情况

| 类别 | 规则数 | 例子 |
|---|---|---|
{cats_table('zh')}

看门狗那两条（`watchdog.timeout` 与卡死阶段的判定）靠 tick 间隔推断，能不能用取决于上面写的
tick 适配。库命名空间里的缺类（`org.jetbrains`、`kotlin`、`com.google` 等）不算症状，除非类名以
`net.minecraft.client.` 开头。

## 构建

```bash
./gradlew jar
```

产物在 `build/libs/`，文件名是 `{t['jar']}`。

{build_note}

## 已知限制

{bullets(t['issues_zh'])}

## 变更记录

这条轨道每一版改了什么、修了什么，见同目录的 [CHANGELOG.md](CHANGELOG.md)。
"""


def track_en(name, t):
    plugin, reobf = build_chain(name, t["mc_en"])
    plugin_line = plugin or "(from this track's `build.gradle`)"
    if reobf:
        reobf_en = ("Reobf is required: Forge 1.20.4 and earlier use SRG member names, so the last build "
                    "step maps them back.")
    elif "neoforge" in name:
        reobf_en = ""
    else:
        reobf_en = "No reobf: Forge dropped it from 1.20.5 on."
    reobf_en_sep = ("; " + reobf_en) if reobf_en else ""

    rows = [
        ("Minecraft", t["mc_en"]),
        ("Loader", t["loader_en"]),
        ("Java", t["java"]),
        ("Target", t["target"]),
        ("Descriptor", "`%s`" % t["toml"]),
        ("Artifact", "`%s`" % t["jar"]),
        ("Status", status_pair(t, "en")),
    ]
    table = "\n".join("| %s | %s |" % (k, v) for k, v in rows)

    note_block = ""

    build_note = bullets(t.get("build_note_en", "")) if t.get("build_note_en") else (
        "- The first build downloads from `maven.neoforged.net` / `maven.minecraftforge.net`; behind a "
        "proxy add `systemProp.https.proxyHost` and `systemProp.https.proxyPort` to `gradle.properties`.")

    return f"""# One BUG Among Us · {name}

{intro(name, t, 'en')}

## This track

| Item | Value |
|---|---|
{table}
{note_block}
## What differs here

- **Descriptor**: `{t['toml']}`. It is a placeholder template, expanded at build time; renaming it makes the loader ignore the jar.
- **Build chain**: {plugin_line}{reobf_en_sep}
- **Tick adapter**: {as_text(t['tick_en']).strip()}
- **Adapter code**: {as_text(t['adapter_en']).strip()}

## How the engine works (one shared code base)

1. Collect: a log4j2 appender catches ERROR / FATAL; new files in `crash-reports/` come in too.
2. Index: scan `mods/*.jar` into two maps, class name to mod and package prefix to mod.
3. Match: 49 regex rules run over each line; one line yields one most-specific verdict.
4. Frames: each `at` class name is looked up in the index, falling back to the package prefix.
5. Score: root-cause distance, rule severity, mod id in the text, agreement between frames.
6. Report: headline, culprit card (mod / file / first frame / confidence), issue list, advice.

The index cannot trust only the loader's own list (NeoForge hands out union-file-system paths that are
not regular files, and trusting them alone found 3 of 16 mods), so `mods/*.jar` is scanned as well and
de-duplicated; platform jars under `libraries/` are excluded or the verdict points at the loader; and
indexing runs on a background pool, skipped when the mods folder is unchanged.

Attribution takes two routes only: the full class name is in the index, or the package prefix is. When
neither works the verdict names nobody. Confidence is weighted (first frame inside the mod, several
frames agreeing, a fatal rule, the mod id in the text add; a bare package match or library frames
subtract), and below the threshold there is no verdict.

## Situations this track handles

| Category | Rules | Examples |
|---|---|---|
{cats_table('en')}

The watchdog rules infer a stall from tick intervals, so they only work when the tick adapter above
does. Missing classes in library namespaces (`org.jetbrains`, `kotlin`, `com.google` and friends) are
not treated as symptoms unless the class starts with `net.minecraft.client.`.

## Building

```bash
./gradlew jar
```

The jar lands in `build/libs/` as `{t['jar']}`.

{build_note}

## Known limits

{bullets(t['issues_en'])}

## Changelog

What each release of this track changed and fixed is in [CHANGELOG.md](CHANGELOG.md).
"""


def main():
    ordered = {name: TRACKS[name] for name in ORDER}
    zh_root = readme_zh.root(ordered)
    en_root = readme_en.root(ordered)
    (ROOT / "README.md").write_text(zh_root, encoding="utf-8", newline="\n")
    (ROOT / "README.en.md").write_text(en_root, encoding="utf-8", newline="\n")
    print("README.md %d 行 / README.en.md %d 行" % (zh_root.count("\n"), en_root.count("\n")))

    for name, t in TRACKS.items():
        d = ROOT / "versions" / name
        zh = track_zh(name, t)
        en = track_en(name, t)
        (d / "README.md").write_text(zh, encoding="utf-8", newline="\n")
        (d / "README.en.md").write_text(en, encoding="utf-8", newline="\n")
        print("%-24s %d / %d 行" % (name, zh.count("\n"), en.count("\n")))


if __name__ == "__main__":
    main()
