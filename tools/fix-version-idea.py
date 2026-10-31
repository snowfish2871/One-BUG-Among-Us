"""各版本 README 的 IDEA 一节改成"打开本目录"。

版本目录已经是独立工程了，再让用户去开仓库根就跑不通了。
顺带把该版本要的 JDK、产物名都写死进去。
"""
import pathlib

R = pathlib.Path(r"D:\dshgowork\moddoctor")
V = R / "versions"

TRACKS = {
    "1.21.1-neoforge": dict(
        jdk=21, jar="oba-mc1.21.1-1.0.0.jar", loader="NeoForge",
        short="1.21.x",
    ),
    "1.20.2-1.20.4-neoforge": dict(
        jdk=17, jar="oba-mc1.20.2-1.20.4-1.0.0.jar", loader="NeoForge",
        short="1.20.2 ~ 1.20.4",
    ),
    "1.20.5-1.20.6-neoforge": dict(
        jdk=21, jar="oba-mc1.20.5-1.20.6-1.0.0.jar", loader="NeoForge",
        short="1.20.5 ~ 1.20.6",
    ),
    "1.20.1-forge": dict(
        jdk=17, jar="oba-mc1.20.1-forge-1.0.0.jar", loader="Forge",
        short="1.20.1",
    ),
}

ZH = """### 用 IntelliJ IDEA 构建

不需要装任何插件，IDEA 自带的 Gradle 支持就够了。

**打开的就是这个目录**（`versions/{name}/`），它本身就是一个完整的 Gradle 工程。
不要去开仓库根 —— 根目录只是各版本的集合，没有 Gradle 工程。
首次同步会下载并反编译 Minecraft {short}，**要等几分钟**。

同步完成后确认：

- `File → Project Structure → Project`：SDK 选 **{jdk}**，Language level 对齐
- `Settings → Build Tools → Gradle`：Gradle JVM 也选 **{jdk}**
  （用错了会在编译时报 `invalid source release`）

**构建**：右侧 `Gradle` 面板 → `Tasks` → `build`，双击。
产物在 `build/libs/{jar}`。

**跑游戏**：导入后运行配置会自动生成，右上角能看到 `runClient` / `runServer`。
想验证报告落盘，跑一次客户端，出问题后去 `run/client/logs/` 找 `oba-report-*.txt`。

**跑命令行**（不启动游戏）：

```bash
./gradlew runCli --args="analyze --game-dir ../../examples"
./gradlew runCli --args="mods --game-dir ../../examples --lang en"
```

也可以直接用 jar：

```bash
java -jar build/libs/{jar} analyze --game-dir ../../examples
```

**不要**把 `dev.oba.mod.ObaMod` 当主类 —— 那是模组入口，单独跑会因为找不到 FML 而报错。
主类是 `dev.oba.Launcher`。

**改引擎代码时注意**：本目录的 `core/` 是仓库根 `engine/` 的同步副本。
改完 `engine/` 之后跑一次 `pwsh tools/sync-versions.ps1`，否则这里还是旧代码。

**常见问题**

| 现象 | 原因与处理 |
|---|---|
| 同步卡在 `neoFormDecompile` | 第一次要反编译 Minecraft，耐心等；卡太久检查网络 |
| `invalid source release` | Gradle JVM 不是 **{jdk}**，改 `Settings → Build Tools → Gradle → Gradle JVM` |
| 运行配置列表里没有 `runClient` | Gradle 没同步完，`Reload Gradle Project` |
| 编译找不到 `net.neoforged` / `net.minecraftforge` | 依赖没下完，删掉 `.gradle` 目录重新同步 |
| 改了 `engine/` 但这里没生效 | 忘了跑 `tools/sync-versions.ps1` |

---
"""

EN = """### Building with IntelliJ IDEA

No plugin needed; IDEA's built-in Gradle support is enough.

**Open this directory** (`versions/{name}/`) — it is a complete Gradle project on its own.
Do not open the repository root: the root is just a collection of versions and has no Gradle
project. The first sync downloads and decompiles Minecraft {short}, so **it takes a few
minutes**.

Then check:

- `File -> Project Structure -> Project`: SDK **{jdk}**, language level matching
- `Settings -> Build Tools -> Gradle`: Gradle JVM **{jdk}** as well
  (a mismatch shows up as `invalid source release` at compile time)

**Build.** Gradle tool window -> `Tasks` -> `build`, double click. The jar lands in
`build/libs/{jar}`.

**Run the game.** The run configurations `runClient` / `runServer` are generated on import.
To watch a report being written, run a client and then look for `oba-report-*.txt` under
`run/client/logs/`.

**Run the CLI** (without starting the game):

```bash
./gradlew runCli --args="analyze --game-dir ../../examples"
./gradlew runCli --args="mods --game-dir ../../examples --lang en"
```

Or use the jar directly:

```bash
java -jar build/libs/{jar} analyze --game-dir ../../examples
```

Do **not** use `dev.oba.mod.ObaMod` as the main class — that is the mod entry point and running
it standalone fails on missing FML. The main class is `dev.oba.Launcher`.

**When editing the engine:** the `core/` here is a synced copy of `engine/` at the repository
root. After editing `engine/`, run `pwsh tools/sync-versions.ps1`, or this directory keeps the
old code.

**Troubleshooting**

| Symptom | Cause and fix |
|---|---|
| Sync stuck on `neoFormDecompile` | the first run decompiles Minecraft; wait, and check the network if it stalls |
| `invalid source release` | Gradle JVM is not **{jdk}**; fix it under `Settings -> Build Tools -> Gradle -> Gradle JVM` |
| No `runClient` in the run configurations | Gradle has not finished syncing; `Reload Gradle Project` |
| `net.neoforged` / `net.minecraftforge` not found | dependencies not fully downloaded; delete `.gradle` and resync |
| Edited `engine/` but this directory did not change | you forgot `tools/sync-versions.ps1` |

---
"""

for name, t in TRACKS.items():
    d = V / name
    for fname, tpl in (("README.md", ZH), ("README.en.md", EN)):
        p = d / fname
        txt = p.read_text(encoding="utf-8")
        marker = "### 用 IntelliJ IDEA 构建" if fname == "README.md" else "### Building in IntelliJ IDEA"
        if marker not in txt:
            print("跳过（没这一节）", name, fname)
            continue
        s = txt.index(marker)
        e = txt.index("\n## ", s + 5)
        body = tpl.format(name=name, **t)
        txt = txt[:s] + body + txt[e:]
        p.write_text(txt, encoding="utf-8", newline="")
    print("重写", name, "的 IDEA 一节")
