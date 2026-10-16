"""给每个版本目录生成 README。

做法是照现有的 README.md 复制一份，然后：
  - 标题下面插一段"本目录是哪条轨道"的说明
  - 把兼容性表格换成只剩这一行的版本
  - 把构建命令换成本目录的
  - 把"其他轨道"那一节点明
"""
import pathlib
import re

R = pathlib.Path(r"D:\dshgowork\moddoctor")
V = R / "versions"
ZH = (R / "README.md").read_text(encoding="utf-8")
EN = (R / "README.en.md").read_text(encoding="utf-8")

TRACKS = {
    "1.21.1-neoforge": dict(
        zh_mc="1.21 ~ 1.21.x", zh_loader="NeoForge 21.1+", zh_java="21",
        zh_target="`1.21.1` / `21.1.256`", zh_status="**已实机验证**",
        en_mc="1.21 - 1.21.x", en_loader="NeoForge 21.1 or newer", en_java="21",
        en_target="`1.21.1` / `21.1.256`", en_status="**verified on a live client**",
        jar="oba-mc1.21.1-1.0.1.jar",
        zh_note="这是 **1.21.x 轨道**，加载器是 NeoForge。实机验证是在 1.21.1 + NeoForge 21.1.256 上做的。",
        en_note="This is the **1.21.x track**, running on NeoForge. Verified in game on 1.21.1 + NeoForge 21.1.256.",
    ),
    "1.20.2-1.20.4-neoforge": dict(
        zh_mc="1.20.2 / 1.20.3 / 1.20.4", zh_loader="NeoForge 20.2 / 20.3 / 20.4 线", zh_java="17",
        zh_target="`1.20.2` / `20.2.93`", zh_status="**已实机验证**",
        en_mc="1.20.2 / 1.20.3 / 1.20.4", en_loader="the NeoForge 20.2 / 20.3 / 20.4 lines", en_java="17",
        en_target="`1.20.2` / `20.2.93`", en_status="**verified on a live client**",
        jar="oba-mc1.20.2-1.20.4-1.0.1.jar",
        zh_note="这是 **1.20.2 ~ 1.20.4 轨道**，加载器是 NeoForge。实机验证是在 1.20.2 + NeoForge 20.2.93 上做的。",
        en_note="This is the **1.20.2 - 1.20.4 track**, running on NeoForge. Verified in game on 1.20.2 + NeoForge 20.2.93.",
    ),
    "1.20.5-1.20.6-neoforge": dict(
        zh_mc="1.20.5 / 1.20.6", zh_loader="NeoForge 20.5 / 20.6 线", zh_java="21",
        zh_target="`1.20.5` / `20.5.21-beta`", zh_status="**已实机验证**",
        en_mc="1.20.5 / 1.20.6", en_loader="the NeoForge 20.5 / 20.6 lines", en_java="21",
        en_target="`1.20.5` / `20.5.21-beta`", en_status="**verified on a live client**",
        jar="oba-mc1.20.5-1.20.6-1.0.1.jar",
        zh_note="这是 **1.20.5 ~ 1.20.6 轨道**，加载器是 NeoForge。实机验证是在 1.20.5 + NeoForge 20.5.21-beta 上做的。",
        en_note="This is the **1.20.5 - 1.20.6 track**, running on NeoForge. Verified in game on 1.20.5 + NeoForge 20.5.21-beta.",
    ),
    "1.20.1-forge": dict(
        zh_mc="1.20.1", zh_loader="**Forge** 47 线", zh_java="17",
        zh_target="`1.20.1` / `47.4.26`", zh_status="**已实机验证**",
        en_mc="1.20.1", en_loader="the **Forge** 47 line", en_java="17",
        en_target="`1.20.1` / `47.4.26`", en_status="**verified on a live client**",
        jar="oba-mc1.20.1-forge-1.0.1.jar",
        zh_note="这是 **1.20.1 轨道**，加载器是 **Forge**（不是 NeoForge —— 1.20.1 那会儿加载器还叫 MinecraftForge）。实机验证是在 1.20.1 + Forge 47.4.26 上做的。",
        en_note="This is the **1.20.1 track**, running on **Forge** (not NeoForge — back then the loader was still MinecraftForge). Verified in game on 1.20.1 + Forge 47.4.26.",
    ),
}

BUILD_ZH = """### 构建

本目录是自包含的，直接在这里构建：

```bash
./gradlew jar
```

产物在 `build/libs/` 下。构建脚本用的是本目录 `src/main` 里的源码。
"""

BUILD_EN = """### Building

This directory is self-contained; build it right here:

```bash
./gradlew jar
```

The jar lands in `build/libs/`. The build uses the sources under this directory's `src/main`.
"""

OTHER_ZH = """### 其他轨道

同一个模组按**版本轨道**分开发布，每条轨道一个独立目录：

| 目录 | Minecraft | 加载器 |
|---|---|---|
| `versions/1.21.1-neoforge/` | 1.21 ~ 1.21.x | NeoForge 21.1+ |
| `versions/1.20.5-1.20.6-neoforge/` | 1.20.5 / 1.20.6 | NeoForge 20.5 / 20.6 |
| `versions/1.20.2-1.20.4-neoforge/` | 1.20.2 / 1.20.3 / 1.20.4 | NeoForge 20.2 / 20.3 / 20.4 |
| `versions/1.20.1-forge/` | 1.20.1 | Forge 47 |

引擎源码只有一份，放在 `engine/`。各版本目录里的源码是按加载器拼出来的副本，
用 `tools/sync-versions.ps1` 同步 —— 改引擎请改 `engine/`，然后重跑那个脚本。

远端每个轨道各有一条同名分支（`1.21.1-neoforge`、`1.20.1-forge` 等），
里面就是这个版本自包含的工程；只关心一个版本的话，直接从那条分支克隆更省事。
"""

OTHER_EN = """### Other tracks

The same mod is released per **version track**, one directory each:

| Directory | Minecraft | Loader |
|---|---|---|
| `versions/1.21.1-neoforge/` | 1.21 - 1.21.x | NeoForge 21.1+ |
| `versions/1.20.5-1.20.6-neoforge/` | 1.20.5 / 1.20.6 | NeoForge 20.5 / 20.6 |
| `versions/1.20.2-1.20.4-neoforge/` | 1.20.2 / 1.20.3 / 1.20.4 | NeoForge 20.2 / 20.3 / 20.4 |
| `versions/1.20.1-forge/` | 1.20.1 | Forge 47 |

There is one copy of the engine source, under `engine/`. The sources inside each version
directory are per-loader copies assembled from it by `tools/sync-versions.ps1` — edit the engine
there and re-run that script.

Each track also has a branch of the same name (`1.21.1-neoforge`, `1.20.1-forge`, and so on)
holding that version's self-contained project; if you only care about one version, cloning that
branch is the shorter route.
"""

for name, t in TRACKS.items():
    d = V / name

    # ---------- 中文 ----------
    # 横幅里不再嵌加粗：1.20.1 那条的加载器名字本身带 **Forge**，套进 **…** 里会变成嵌套加粗
    zh_loader_plain = t['zh_loader'].replace('**', '')
    zh = ZH
    header_end = zh.index("---\n\n## 兼容性")
    banner = f"""# One BUG Among Us

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

**面向 Minecraft {t['zh_mc']}（{zh_loader_plain}）的崩溃诊断工具** · 缩写 **OBA**

指出是哪个模组文件把游戏搞崩的，错在哪个类的哪一行，以及崩溃前后各个模组分别做了什么。

> {t['zh_note']}
> 本目录的产物是 `{t['jar']}`。

[English](README.en.md) | 简体中文

> **本项目只做诊断，绝不改动任何文件。**
> 不会禁用、移动、重命名或删除任何一个模组。所有结论只是打印出来并写成报告，
> 是否采取行动由你决定。

---

"""
    # 兼容性表换成只剩本轨道
    zh = banner + zh[header_end + len("---\n\n"):]
    zh = zh.replace(
        """| 轨道 | Minecraft | 加载器 | Java | 编译目标 | 状态 |
|---|---|---|---|---|---|
| **1.21.x** | 1.21 ~ 1.21.x | NeoForge 21.1+ | 21 | `1.21.1` / `21.1.256` | 已实机验证 |
| **1.20.2 ~ 1.20.4** | 1.20.2 / 1.20.3 / 1.20.4 | NeoForge 20.2 / 20.3 / 20.4 线 | 17 | `1.20.2` / `20.2.93` | **已实机验证** |
| **1.20.5 ~ 1.20.6** | 1.20.5 / 1.20.6 | NeoForge 20.5 / 20.6 线 | 21 | `1.20.5` / `20.5.21-beta` | **已实机验证** |
| **1.20.1** | 1.20.1 | **Forge** 47 线 | 17 | `1.20.1` / `47.4.26` | **已实机验证** |""",
        f"""| 项目 | 值 |
|---|---|
| Minecraft | {t['zh_mc']} |
| 加载器 | {t['zh_loader']} |
| Java | {t['zh_java']} |
| 编译目标 | {t['zh_target']} |
| 状态 | {t['zh_status']} |""")
    # 构建命令 + 其他轨道，插在兼容性一节后面
    anchor = "### 加载器适配层"
    if anchor in zh:
        zh = zh.replace(anchor, BUILD_ZH + "\n" + OTHER_ZH + "\n" + anchor, 1)
    (d / "README.md").write_text(zh, encoding="utf-8", newline="")

    # ---------- 英文 ----------
    en = EN
    hdr = en.index("---\n\n## Compatibility")
    en_loader_plain = t['en_loader'].replace('**', '')
    en_banner = f"""# One BUG Among Us

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

**Crash diagnostics for Minecraft {t['en_mc']} on {en_loader_plain}** · abbreviated **OBA**

It names the mod file that broke the game, the class and line where it broke, and what every
mod was doing in the moments before and during the failure.

> {t['en_note']}
> This directory produces `{t['jar']}`.

English | [简体中文](README.md)

> **Diagnosis only. This project never modifies a single file.**
> It does not disable, move, rename or delete any mod. Everything it concludes is printed
> and written to a report; acting on it is your decision.

---

"""
    en = en_banner + en[hdr + len("---\n\n"):]
    en = en.replace(
        """| Track | Minecraft | Loader | Java | Built against | Status |
|---|---|---|---|---|---|
| **1.21.x** | 1.21 – 1.21.x | 21.1 or newer | 21 | `1.21.1` / `21.1.256` | verified on a live client |
| **1.20.2 – 1.20.4** | 1.20.2 / 1.20.3 / 1.20.4 | the 20.2 / 20.3 / 20.4 lines | 17 | `1.20.2` / `20.2.93` | **verified on a live client** |
| **1.20.5 – 1.20.6** | 1.20.5 / 1.20.6 | the 20.5 / 20.6 lines | 21 | `1.20.5` / `20.5.21-beta` | **verified on a live client** |
| **1.20.1** | 1.20.1 | the **Forge** 47 line | 17 | `1.20.1` / `47.4.26` | **verified on a live client** |""",
        f"""| | |
|---|---|
| Minecraft | {t['en_mc']} |
| Loader | {t['en_loader']} |
| Java | {t['en_java']} |
| Built against | {t['en_target']} |
| Status | {t['en_status']} |""")
    anchor_en = "### The loader adapter layer"
    if anchor_en in en:
        en = en.replace(anchor_en, BUILD_EN + "\n" + OTHER_EN + "\n" + anchor_en, 1)
    (d / "README.en.md").write_text(en, encoding="utf-8", newline="")

    print("写好", name)
