"""按「这一条轨道」重组每条轨道的 README.md / README.en.md。

根 README 是整个仓库的总文档：仓库结构、七条轨道的兼容性表、多版本构建流程。
版本 README 只描述它自己那一条轨道：

- 这一条轨道是什么（Minecraft 版本、加载器、Java、产物、状态）
- 这一条轨道的加载器适配和 tick 挂载是怎么实现的
- 这一条轨道怎么构建、验证到哪一步、还剩什么问题
- 引擎级说明（怎么用、报告长什么样、规则有哪些、原理、限制）——
  这部分每个版本都一样，直接从根 README 对应章节搬过来，只把 jar 名换成自己的

仓库级的章节（仓库与分支、兼容性表、其他轨道目录、多版本构建、加新轨道的流程）
一律不进版本 README。

用法（改完根 README 或 tools/track_info.py 之后重跑）：

    python tools/gen-version-readmes.py
    python tools/gen-version-trees.py      # 再补「项目结构」一节
"""
import pathlib
import re
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
from track_info import TRACKS  # noqa: E402

R = pathlib.Path(r"D:\dshgowork\moddoctor")
CANON_JAR = "oba-mc1.21.1-1.1.0.jar"

ENGINE_ASSERT = "199"
RUNTIME_ASSERT = "52"

ROOT_ZH = (R / "README.md").read_text(encoding="utf-8")
ROOT_EN = (R / "README.en.md").read_text(encoding="utf-8")


def split_sections(text):
    parts = re.split(r"\n(?=## )", text)
    sect, order = {}, []
    for p in parts[1:]:
        title = p.split("\n", 1)[0][3:].strip()
        sect[title] = p.rstrip("\n")
        order.append(title)
    return sect, order


ZH_SECT, ZH_ORDER = split_sections(ROOT_ZH)
EN_SECT, EN_ORDER = split_sections(ROOT_EN)


def sub_block(section_text, heading):
    """取 section 里某个 ### 小节（到下一个 ### 或结尾）"""
    start = section_text.index(heading)
    rest = section_text[start + len(heading):]
    m = rest.find("\n### ")
    if m >= 0:
        rest = rest[:m]
    return (heading + rest).rstrip("\n")


def anchor(title):
    s = re.sub(r"[^\w\u4e00-\u9fff\- ]", "", title).lower()
    return s.replace(" ", "-")


def tail_block(text):
    """根 README 结尾那段居中的落款，原样搬走"""
    return text[text.rindex("<div"):].rstrip("\n")


ZH_TAIL = tail_block(ROOT_ZH)
EN_TAIL = tail_block(ROOT_EN)

# 各章节在这一条轨道的 README 里的顺序；None 表示整节由脚本自己写
ZH_PLAN = [
    ("这一条轨道", "this"),
    ("目录", "toc"),
    ("它解决什么问题", "copy"),
    ("能做什么", "copy"),
    ("快速开始", "copy"),
    ("界面", "copy"),
    ("事故时间线", "copy"),
    ("诊断报告", "copy"),
    ("使用方式", "copy"),
    ("实现原理", "copy"),
    ("定位原理", "copy"),
    ("MCreator 识别", "copy"),
    ("图形类模组专项", "copy"),
    ("Sinytra Connector 适配", "copy"),
    ("规则库", "copy"),
    ("项目结构", "structure"),
    ("构建", "build"),
    ("验证", "verify"),
    ("已知限制", "copy"),
    ("常见问题", "copy"),
    ("变更记录", "changelog"),
    ("参与贡献", "copy"),
    ("致谢", "copy"),
    ("许可协议", "copy"),
    ("附录 B：术语表", "copy"),
]
EN_PLAN = [
    ("This track", "this"),
    ("Contents", "toc"),
    ("What it solves", "copy"),
    ("What it does", "copy"),
    ("Quick start", "copy"),
    ("The interface", "copy"),
    ("Incident timeline", "copy"),
    ("Diagnostic reports", "copy"),
    ("Using it", "copy"),
    ("How it works", "copy"),
    ("How attribution works", "copy"),
    ("MCreator detection", "copy"),
    ("Graphics mods", "copy"),
    ("Sinytra Connector support", "copy"),
    ("Rule set", "copy"),
    ("Project structure", "structure"),
    ("Building", "build"),
    ("Verification", "verify"),
    ("Known limits", "copy"),
    ("FAQ", "copy"),
    ("Changelog", "changelog"),
    ("Contributing", "copy"),
    ("Credits", "copy"),
    ("License", "copy"),
    ("Appendix B: glossary", "copy"),
]

LABEL_ZH = {
    "1.21.1-neoforge": "1.21.1（NeoForge）",
    "1.20.5-1.20.6-neoforge": "1.20.5 ~ 1.20.6（NeoForge）",
    "1.20.2-1.20.4-neoforge": "1.20.2 ~ 1.20.4（NeoForge）",
    "1.20.1-forge": "1.20.1（Forge）",
    "1.21.1-forge": "1.21.1（Forge）",
    "1.20.5-1.20.6-forge": "1.20.5 ~ 1.20.6（Forge）",
    "1.20.2-1.20.4-forge": "1.20.2 ~ 1.20.4（Forge）",
}
LABEL_EN = {
    "1.21.1-neoforge": "1.21.1 (NeoForge)",
    "1.20.5-1.20.6-neoforge": "1.20.5 – 1.20.6 (NeoForge)",
    "1.20.2-1.20.4-neoforge": "1.20.2 – 1.20.4 (NeoForge)",
    "1.20.1-forge": "1.20.1 (Forge)",
    "1.21.1-forge": "1.21.1 (Forge)",
    "1.20.5-1.20.6-forge": "1.20.5 – 1.20.6 (Forge)",
    "1.20.2-1.20.4-forge": "1.20.2 – 1.20.4 (Forge)",
}
LOADER_DIR = {k: ("forge" if k.endswith("-forge") else "neoforge") for k in TRACKS}

STATUS_ZH = {
    (True, True): "已出包，已实机验证",
    (True, False): "已出包，未实机",
    (False, False): "代码就绪，未出包",
}
STATUS_EN = {
    (True, True): "built and verified in game",
    (True, False): "built, not tested in game",
    (False, False): "code ready, no jar yet",
}
RESULT_ZH_UNBUILT = "没有出包，所以没有实机结果。"
RESULT_ZH_NOTRUN = "出了包，但还没有在客户端或专用服务端里跑过。"
RESULT_EN_UNBUILT = "No jar was produced, so there are no in-game results."
RESULT_EN_NOTRUN = "The jar builds, but it has not been run in a client or on a server yet."
PLUGIN_ZH = {"forge": "ForgeGradle 6（`net.minecraftforge.gradle`）",
             "neoforge": "NeoForgeGradle（`net.neoforged.gradle.userdev` 7.1.38）"}
PLUGIN_EN = {"forge": "ForgeGradle 6 (`net.minecraftforge.gradle`)",
             "neoforge": "NeoForgeGradle (`net.neoforged.gradle.userdev` 7.1.38)"}

DISCLAIMER_ZH = ("**该MOD正在处于初期构建状态尚属未完成作品，请不要将其应用于您的整合包或者存档，"
                 "造成的任何问题OBA contributors不会承担！**")
INTRO_ZH = ("游戏崩了，日志里几千行堆栈看不出是谁干的。OBA 把这件事收敛成一句能照着做的结论：\n"
            "**哪个 jar、哪个类的哪一行、以及为什么怀疑它。**")
ONLY_DIAG_ZH = ("> **本项目只做诊断，绝不改动任何文件。**\n"
                "> 不会禁用、移动、重命名或删除任何一个模组。所有结论只是打印出来并写成报告，\n"
                "> 是否采取行动由你决定。")
DISCLAIMER_EN = ("**This mod is still in the early stages of development and is an unfinished work, "
                 "so please don't apply it to your modpacks or save files. OBA contributors won't "
                 "be responsible for any issues it causes!**")
INTRO_EN = ("The game dies, the log is a few thousand lines of stack trace, and none of it says who "
            "did it.\nOBA boils that down to one sentence you can act on: **which jar, which class, "
            "which line, and\nwhy that one is suspected.**")
ONLY_DIAG_EN = ("> **Diagnosis only. This project never modifies a single file.**\n"
                "> It does not disable, move, rename or delete any mod. Everything it concludes is "
                "printed\n> and written to a report; acting on it is your decision.")

# 根 README 里和「多轨道」绑在一起的那两段，换成本轨道的说法
SYNC_ZH = """换一个大版本，改的是这一层，引擎照旧。`tools/sync-versions.ps1` 把 `engine/` 原样复制进
每条轨道，就是为了让「改一处、七条轨道一起生效」这件事只有一种做法。"""
TRACK_ZH = """换一个大版本，改的是这一层，引擎照旧。本目录里 `src/main/java/dev/oba/core/` 是引擎，
`src/main/java/dev/oba/loader/{loader}/` 是这一条轨道的适配实现，换版本只动后者。"""
SYNC_EN = """Move to another major version and only that layer changes; the engine stays as it is.
`tools/sync-versions.ps1` copies `engine/` verbatim into every track, so "change once,
apply to seven tracks" has exactly one right way to happen."""
TRACK_EN = """Move to another major version and only that layer changes; the engine stays as it is.
In this directory `src/main/java/dev/oba/core/` is the engine and
`src/main/java/dev/oba/loader/{loader}/` is this track's adapter: a new version touches only the
latter."""

OTHER_JARS = [t["jar"] for t in TRACKS.values()]
BANNED_ZH = ["七条轨道", "七个版本", "四条轨道", "其他轨道", "各轨道", "sync-versions",
             "build-all", "versions/", "engine/tools"]
BANNED_EN = ["seven tracks", "four tracks", "other tracks", "every track", "sync-versions",
             "build-all", "versions/", "engine/tools"]


def clean(text, t, name, lang):
    # 根 README 的章节之间用 --- 分隔，切出来会挂在上一节尾巴上
    text = re.sub(r"\n+---\s*$", "", text)
    text = text.replace(CANON_JAR, t["jar"])
    text = text.replace("engine/main/java", "src/main/java")
    text = text.replace("engine/main/resources/oba-icon.png", "src/main/resources/oba-icon.png")
    loader = LOADER_DIR[name]
    if lang == "zh":
        text = text.replace("诊断逻辑全在 `engine/` 里",
                            "诊断逻辑全在 `src/main/java/dev/oba/core/` 里")
        if SYNC_ZH in text:
            text = text.replace(SYNC_ZH, TRACK_ZH.format(loader=loader))
    else:
        text = text.replace("The diagnostics live in `engine/`",
                            "The diagnostics live in `src/main/java/dev/oba/core/`")
        if SYNC_EN in text:
            text = text.replace(SYNC_EN, TRACK_EN.format(loader=loader))
    for jar in OTHER_JARS:
        if jar != t["jar"] and jar in text:
            raise SystemExit(f"{name}: 正文里混进了别的轨道的 jar 名 {jar}")
    for word in (BANNED_ZH if lang == "zh" else BANNED_EN):
        if word in text:
            raise SystemExit(f"{name}: 正文里混进了跨轨道说法「{word}」")
    return text


# ---------------------------------------------------------------- 这一条轨道

def track_table_zh(t):
    st = t.get("status_zh") or STATUS_ZH[(t["built"], t["verified"])]
    return f"""| 项目 | 值 |
|---|---|
| Minecraft | {t['mc_zh']} |
| 加载器 | {t['loader_zh']} |
| Java | {t['java']} |
| 编译目标 | {t['target']} |
| 描述文件 | `{t['toml']}` |
| 产物 | `{t['jar']}` |
| 状态 | {st} |"""


def track_table_en(t):
    st = t.get("status_en") or STATUS_EN[(t["built"], t["verified"])]
    return f"""| Item | Value |
|---|---|
| Minecraft | {t['mc_en']} |
| Loader | {t['loader_en']} |
| Java | {t['java']} |
| Built against | {t['target']} |
| Descriptor | `{t['toml']}` |
| Artifact | `{t['jar']}` |
| Status | {st} |"""


ADAPTER_ZH = """这一条轨道只有一份加载器适配，在 `src/main/java/dev/oba/loader/{loader}/`。
引擎不引用任何加载器类，只认 `dev.oba.loader` 里的三个接口：

| 接口 | 作用 |
|---|---|
| `LoaderEnv` | 列模组、取游戏目录、加载器名字 |
| `TickHook` | 挂服务器 tick |
| `Loader` | 存放当前轨道装进来的实现 |

{adapter}"""
ADAPTER_EN = """This track has exactly one loader adapter, under
`src/main/java/dev/oba/loader/{loader}/`. The engine references no loader class of its own; it only
knows the three interfaces in `dev.oba.loader`:

| Interface | Role |
|---|---|
| `LoaderEnv` | list mods, resolve the game directory, name the loader |
| `TickHook` | hook the server tick |
| `Loader` | holds whichever implementation this build installed |

{adapter}"""


def this_track_zh(name, t):
    body = [f"""## 这一条轨道

{track_table_zh(t)}

{t['note_zh']}

### 这一条轨道的加载器适配

{ADAPTER_ZH.format(loader=LOADER_DIR[name], adapter=t['adapter_zh'])}

{t['tick_zh']}

### 这一条轨道的实机结果

{chr(10).join('- ' + line for line in t['verify_zh']) if t['verify_zh'] else (RESULT_ZH_NOTRUN if t['built'] else RESULT_ZH_UNBUILT)}

### 这一条轨道还没解决的问题

{chr(10).join('- ' + line for line in t['issues_zh'])}"""]
    return "\n".join(body)


def this_track_en(name, t):
    body = [f"""## This track

{track_table_en(t)}

{t['note_en']}

### The loader adapter on this track

{ADAPTER_EN.format(loader=LOADER_DIR[name], adapter=t['adapter_en'])}

{t['tick_en']}

### In-game results on this track

{chr(10).join('- ' + line for line in t['verify_en']) if t['verify_en'] else (RESULT_EN_NOTRUN if t['built'] else RESULT_EN_UNBUILT)}

### Still open on this track

{chr(10).join('- ' + line for line in t['issues_en'])}"""]
    return "\n".join(body)


# ---------------------------------------------------------------- 构建

def build_zh(name, t):
    loader = LOADER_DIR[name]
    cmd = "./gradlew jar"
    notes = [f"- JDK：{t['java']}", f"- 构建插件：{PLUGIN_ZH[loader]}"]
    if (R / "versions" / name / "mirror.init.gradle").exists():
        cmd = "./gradlew jar -I mirror.init.gradle"
        notes.append("- 本机直连 `maven.neoforged.net` 会超时，`mirror.init.gradle` 把请求转到官方 CDN；"
                     "网络正常时不加 `-I` 也行。")
    if loader == "forge":
        notes.append("- 描述文件 `META-INF/mods-forge.toml` 里全是 `${}` 占位符，构建时按 "
                     "`gradle.properties` 展开，最终落成 `mods.toml`。")
        notes.append("- 构建要下载 Forge 的 userdev 链并做反混淆，需要能访问 "
                     "`maven.minecraftforge.net`，首次耗时几分钟。")
        notes.append("- 第一次构建若卡在下载 userdev：Java 不读系统代理，把 `gradle.properties` "
                     "里注释掉的 `systemProp.https.proxyHost` / `proxyPort` 放开并改成自己的代理。")
    else:
        notes.append(f"- 描述文件就是 `{t['toml']}`，内容同样由 `gradle.properties` 展开。")
    text = f"""## 构建

本目录是自包含的，不需要仓库里的其他东西：

```bash
{cmd}
```

产物在 `build/libs/{t['jar']}`。

{chr(10).join(notes)}"""
    if not t["built"]:
        text += "\n\n> 这条轨道在 1.1.0 里没有在本机出包，原因见上面「这一条轨道」里还没解决的问题。"
    return text


def build_en(name, t):
    loader = LOADER_DIR[name]
    cmd = "./gradlew jar"
    notes = [f"- JDK: {t['java']}", f"- Build plugin: {PLUGIN_EN[loader]}"]
    if (R / "versions" / name / "mirror.init.gradle").exists():
        cmd = "./gradlew jar -I mirror.init.gradle"
        notes.append("- `maven.neoforged.net` times out from some networks; `mirror.init.gradle` "
                     "redirects the requests to the official CDN. Drop `-I` when the network is fine.")
    if loader == "forge":
        notes.append("- `META-INF/mods-forge.toml` is nothing but `${}` placeholders; the build "
                     "expands them from `gradle.properties` and ships the result as `mods.toml`.")
        notes.append("- The build downloads Forge's userdev chain and deobfuscates it, so it needs "
                     "`maven.minecraftforge.net` to be reachable and takes a few minutes the first "
                     "time.")
        notes.append("- If the first build stalls on that download: Java ignores the system proxy, so "
                     "uncomment the `systemProp.https.proxyHost` / `proxyPort` lines in "
                     "`gradle.properties` and point them at your own proxy.")
    else:
        notes.append(f"- The descriptor is `{t['toml']}`, expanded from `gradle.properties` the same "
                     "way.")
    text = f"""## Building

This directory is self-contained; nothing else in the repository is needed:

```bash
{cmd}
```

The artifact lands in `build/libs/{t['jar']}`.

{chr(10).join(notes)}"""
    if not t["built"]:
        text += "\n\n> No jar was produced for this track in 1.1.0; see the open issues under " \
                "\"This track\" above."
    return text


# ---------------------------------------------------------------- 验证

def verify_zh(name, t):
    build = (f"通过，产物 `build/libs/{t['jar']}`" if t["built"]
             else "未出包，原因见上面「这一条轨道」")
    game = ("见上面「这一条轨道的实机结果」" if t["verified"]
            else ("出了包，但还没跑过" if t["built"] else "没有 jar，未跑"))
    table = f"""| 验证项 | 结果 |
|---|---|
| 引擎自测（`{ENGINE_ASSERT}` 条断言） | 通过 |
| 运行时验证台（`{RUNTIME_ASSERT}` 条断言） | 通过 |
| 本目录编译与出包 | {build} |
| 本轨道实机 | {game} |"""
    found = sub_block(ZH_SECT["验证"], "### 这套验证揪出过什么")
    return f"""## 验证

{table}

两套验证都是引擎级的，跑的是同一份 `core/` 代码，和加载器无关：断言覆盖的正是前面写的
那些规则、归属打分和报告格式。

{clean(found, t, name, 'zh')}"""


def verify_en(name, t):
    build = (f"pass, `build/libs/{t['jar']}`" if t["built"]
             else "no jar; see \"This track\" above")
    game = ("see \"In-game results on this track\" above" if t["verified"]
            else ("built, not run yet" if t["built"] else "no jar, not run"))
    table = f"""| What | Result |
|---|---|
| Engine self-test (`{ENGINE_ASSERT}` assertions) | pass |
| Runtime harness (`{RUNTIME_ASSERT}` assertions) | pass |
| This directory compiles and produces a jar | {build} |
| In-game on this track | {game} |"""
    found = sub_block(EN_SECT["Verification"], "### What this verification has caught")
    return f"""## Verification

{table}

Both suites are engine-level: they run the same `core/` code and know nothing about the loader.
The assertions cover the rules, the attribution scoring and the report formats described above.

{clean(found, t, name, 'en')}"""


CHANGELOG_ZH = """## 变更记录

这条轨道每一版改了什么、修了什么、还剩什么问题，记在同目录的 [CHANGELOG.md](CHANGELOG.md)。"""
CHANGELOG_EN = """## Changelog

What each release changed on this track, what it fixed and what is still open lives in
[CHANGELOG.md](CHANGELOG.md), next to this file."""
STRUCT_ZH = "## 项目结构\n\n（本目录的文件清单）"
STRUCT_EN = "## Project structure\n\n(file list of this directory)"


# ---------------------------------------------------------------- 拼装

def compose(name, t, lang):
    plan = ZH_PLAN if lang == "zh" else EN_PLAN
    sect = ZH_SECT if lang == "zh" else EN_SECT
    toc = "\n".join(f"- [{title}](#{anchor(title)})" for title, _ in plan)
    blocks = []
    for title, kind in plan:
        if kind is None:
            continue
        if kind == "copy":
            blocks.append(clean(sect[title], t, name, lang))
        elif kind == "this":
            blocks.append(this_track_zh(name, t) if lang == "zh" else this_track_en(name, t))
        elif kind == "toc":
            blocks.append(("## 目录\n\n" if lang == "zh" else "## Contents\n\n") + toc)
        elif kind == "structure":
            blocks.append(STRUCT_ZH if lang == "zh" else STRUCT_EN)
        elif kind == "build":
            blocks.append(build_zh(name, t) if lang == "zh" else build_en(name, t))
        elif kind == "verify":
            blocks.append(verify_zh(name, t) if lang == "zh" else verify_en(name, t))
        elif kind == "changelog":
            blocks.append(CHANGELOG_ZH if lang == "zh" else CHANGELOG_EN)
        else:
            raise SystemExit("unknown kind " + kind)

    if lang == "zh":
        head = f"""# One BUG Among Us   一个BUG隐藏在我们当中

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

{DISCLAIMER_ZH}

**面向 Minecraft {t['mc_zh']} 的模组崩溃诊断工具**（{'Forge' if LOADER_DIR[name] == 'forge' else 'NeoForge'}）

{INTRO_ZH}

[English](README.en.md) | 简体中文

{ONLY_DIAG_ZH}

> **这是 {LABEL_ZH[name]} 这条轨道自己的 README**：只讲这条轨道的 Minecraft 版本、加载器适配、
> 构建方式和验证结果。仓库总览、分支列表和版本兼容表在根 README。

<img src="src/main/resources/oba-icon.png" alt="图标" width="128" align="right">
"""
        tail = ZH_TAIL
    else:
        head = f"""# One BUG Among Us

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

{DISCLAIMER_EN}

**Crash diagnostics for Minecraft {t['mc_en']}** ({'Forge' if LOADER_DIR[name] == 'forge' else 'NeoForge'}).

{INTRO_EN}

English | [简体中文](README.md)

{ONLY_DIAG_EN}

> **This is the README for the {LABEL_EN[name]} track only.** It covers this track's Minecraft
> versions, loader adapter, build and verification results. The repository overview, the branch
> list and the version matrix live in the root README.

<img src="src/main/resources/oba-icon.png" alt="icon" width="128" align="right">
"""
        tail = EN_TAIL

    return head.rstrip() + "\n\n---\n\n" + "\n\n---\n\n".join(blocks) + "\n\n---\n\n" + tail + "\n"


def main():
    for name, t in TRACKS.items():
        d = R / "versions" / name
        zh = compose(name, t, "zh")
        en = compose(name, t, "en")
        (d / "README.md").write_text(zh, encoding="utf-8", newline="\n")
        (d / "README.en.md").write_text(en, encoding="utf-8", newline="\n")
        print(f"{name}: README.md {len(zh)} 字符 / README.en.md {len(en)} 字符")
    print("七条轨道的 README 已重写；接着跑 python tools/gen-version-trees.py 补「项目结构」")


if __name__ == "__main__":
    main()
