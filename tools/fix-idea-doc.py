"""根 README 的 IDEA 一节按新结构重写：导入的是版本目录，不是仓库根。"""
import pathlib

R = pathlib.Path(r"D:\dshgowork\moddoctor")

ZH_OLD_START = "### 用 IntelliJ IDEA 构建"
ZH_NEW = """### 用 IntelliJ IDEA 构建

不需要装任何插件，IDEA 自带的 Gradle 支持就够了。

**导入的是某个版本目录，不是仓库根。** 仓库根现在只是各版本的集合，
真正的 Gradle 工程在 `versions/<版本>-<加载器>/` 里。

**1. 导入**：`File → Open`，选你想开发的**那一个版本目录**，例如
`versions/1.21.1-neoforge`（里面有 `build.gradle` 和 `settings.gradle`）。
首次同步会下载并反编译对应版本的 Minecraft，**要等几分钟**。

同步完成后确认：

- `File → Project Structure → Project`：SDK 选该版本要的 JDK，Language level 对齐
- `Settings → Build Tools → Gradle`：Gradle JVM 也要一致
  （用错了会在编译时报 `invalid source release`）

各版本需要的 JDK：

| 版本 | JDK |
|---|---|
| 1.21.x（NeoForge） | **21** |
| 1.20.5 ~ 1.20.6（NeoForge） | **21** |
| 1.20.2 ~ 1.20.4（NeoForge） | **17** |
| 1.20.1（Forge） | **17** |

**2. 构建**：右侧 `Gradle` 面板 → `Tasks` → `build`，双击。
产物在 `build/libs/` 下。

**3. 跑游戏**：NeoForge 那三个版本导入后运行配置会自动生成，右上角能看到
`runClient` / `runServer`。Forge 那个也有 `runClient` / `runServer`。
想验证报告落盘，跑一次客户端，出问题后去 `run/client/logs/` 找 `oba-report-*.txt`。

**4. 跑命令行工具**：NeoForge 那三个版本有 `runCli` 任务：

```bash
cd versions/1.21.1-neoforge
./gradlew runCli --args="analyze --game-dir ../../examples"
./gradlew runCli --args="mods --game-dir ../../examples --lang en"
```

也可以直接用 jar：

```bash
java -jar build/libs/oba-mc1.21.1-1.0.0.jar analyze --game-dir ../../examples
```

**不要**把 `dev.oba.mod.ObaMod` 当主类——那是模组入口，单独跑会因为找不到 FML 而报错。
主类是 `dev.oba.Launcher`。

**改引擎代码时注意**：版本目录里的 `core/` 是 `engine/` 的同步副本。
改完 `engine/` 之后跑一次 `pwsh tools/sync-versions.ps1`，
否则各版本目录里的还是旧代码。

**常见问题**

| 现象 | 原因与处理 |
|---|---|
| 同步卡在 `neoFormDecompile` | 第一次要反编译 Minecraft，耐心等；卡太久检查网络 |
| `invalid source release` | Gradle JVM 和该版本要的 JDK 不一致 |
| 运行配置列表里没有 `runClient` | Gradle 没同步完，`Reload Gradle Project` |
| 改了 `build.gradle` 后行为没变 | 手动 `Reload Gradle Project` |
| 编译找不到 `net.neoforged` / `net.minecraftforge` | 依赖没下完，删掉 `.gradle` 目录重新同步 |
| 改了 `engine/` 但版本里没生效 | 忘了跑 `tools/sync-versions.ps1` |

---
"""

EN_OLD_START = "### Building in IntelliJ IDEA"
EN_NEW = """### Building with IntelliJ IDEA

No plugin needed; IDEA's built-in Gradle support is enough.

**Open a version directory, not the repository root.** The root is now just a collection of
versions; the actual Gradle projects live in `versions/<version>-<loader>/`.

**1. Open.** `File -> Open` and pick the **version directory** you want to work on, for example
`versions/1.21.1-neoforge` (it contains `build.gradle` and `settings.gradle`). The first sync
downloads and decompiles that Minecraft version, so **it takes a few minutes**.

Then check:

- `File -> Project Structure -> Project`: SDK set to the JDK that version needs
- `Settings -> Build Tools -> Gradle`: Gradle JVM matching
  (a mismatch shows up as `invalid source release` at compile time)

| Version | JDK |
|---|---|
| 1.21.x (NeoForge) | **21** |
| 1.20.5 - 1.20.6 (NeoForge) | **21** |
| 1.20.2 - 1.20.4 (NeoForge) | **17** |
| 1.20.1 (Forge) | **17** |

**2. Build.** Gradle tool window -> `Tasks` -> `build`, double click. The jar lands in
`build/libs/`.

**3. Run the game.** The three NeoForge versions generate `runClient` / `runServer` run
configurations; so does the Forge one. To watch a report being written, run a client and then
look for `oba-report-*.txt` under `run/client/logs/`.

**4. Run the CLI.** The NeoForge versions have a `runCli` task:

```bash
cd versions/1.21.1-neoforge
./gradlew runCli --args="analyze --game-dir ../../examples"
./gradlew runCli --args="mods --game-dir ../../examples --lang en"
```

Or use the jar directly:

```bash
java -jar build/libs/oba-mc1.21.1-1.0.0.jar analyze --game-dir ../../examples
```

Do **not** use `dev.oba.mod.ObaMod` as the main class — that is the mod entry point and running
it standalone fails on missing FML. The main class is `dev.oba.Launcher`.

**When editing the engine:** the `core/` inside a version directory is a synced copy of
`engine/`. After editing `engine/`, run `pwsh tools/sync-versions.ps1`, or the version
directories keep the old code.

**Troubleshooting**

| Symptom | Cause and fix |
|---|---|
| Sync stuck on `neoFormDecompile` | the first run decompiles Minecraft; wait, and check the network if it stalls |
| `invalid source release` | Gradle JVM does not match the JDK that version needs |
| No `runClient` in the run configurations | Gradle has not finished syncing; `Reload Gradle Project` |
| `build.gradle` edits have no effect | `Reload Gradle Project` manually |
| `net.neoforged` / `net.minecraftforge` not found | dependencies not fully downloaded; delete `.gradle` and resync |
| Edited `engine/` but the version did not change | you forgot `tools/sync-versions.ps1` |

---
"""


def replace_section(text, start_marker, new_body):
    s = text.index(start_marker)
    e = text.index("\n## ", s + 5)
    return text[:s] + new_body + text[e:]


for name, marker, body in (("README.md", ZH_OLD_START, ZH_NEW),
                           ("README.en.md", EN_OLD_START, EN_NEW)):
    p = R / name
    t = p.read_text(encoding="utf-8")
    if marker in t:
        t = replace_section(t, marker, body)
        p.write_text(t, encoding="utf-8", newline="")
        print("重写", name, "的 IDEA 一节")
    else:
        print(name, ": 没找到该节")
