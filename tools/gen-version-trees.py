"""按每个版本目录的真实内容重写 README 里的「项目结构」一节（中英两份）。

版本 README 只描述它自己那个目录：树也是从该目录真实存在的文件长出来的，
不提仓库根、不提其他版本。
"""
import pathlib

R = pathlib.Path(r"D:\dshgowork\moddoctor")
V = R / "versions"

# 文件 -> (中文说明, 英文说明)
DESC = {
    "README.md": ("本文档（简体中文）", "Simplified Chinese README"),
    "README.en.md": ("English README", "This file (English)"),
    "CHANGELOG.md": ("这条轨道每一版改了什么、修了什么、还剩什么",
                     "What each release on this track changed, fixed and left open"),
    "LICENSE": ("GPL-3.0 许可协议全文", "Full GPL-3.0 license text"),
    "build.gradle": ("构建脚本", "Build script"),
    "settings.gradle": ("插件仓库与根项目名", "Plugin repositories and root project name"),
    "gradle.properties": ("版本号、模组元数据、兼容版本范围",
                          "Version, mod metadata, supported version ranges"),
    "gradlew": ("Gradle 包装器（8.14.3）", "Gradle wrapper (8.14.3)"),
    "gradlew.bat": ("同上，Windows 用", "Same, for Windows"),
    "mirror.init.gradle": ("把失效的 maven 主机改写到官方 CDN 镜像",
                           "Rewrites the dead maven host to the official CDN mirror"),
    "examples": ("故意做坏的样例：假模组、几种崩溃报告",
                 "Deliberately broken samples: fake mods, several crash reports"),
    "banner.svg": ("README 用的标题条", "Title banner used by the READMEs"),

    "Launcher.java": ("唯一入口：判断是游戏内还是命令行，然后分派",
                      "Single entry point: picks in-game or CLI, then dispatches"),
    "Cli.java": ("命令行工具：参数解析、子命令、输出",
                 "The CLI: argument parsing, subcommands, output"),
    "Oba.java": ("常量：名字、版本号、modId、日志前缀",
                 "Constants: name, version, modId, log prefix"),
    "Lang.java": ("中英文文案表", "Chinese/English message table"),
    "Diagnosis.java": ("数据模型：Finding / KnownIssue / Report",
                       "Data model: Finding / KnownIssue / Report"),
    "ModSource.java": ("单个模组文件的只读视图（jar 与目录两种形态）",
                       "Read-only view of one mod file (jar or exploded directory)"),
    "Toml.java": ("极简 TOML 解析（读模组描述文件）",
                  "Minimal TOML parsing (reads the mod descriptor)"),
    "ClassIndex.java": ("类名 / 包名 → 模组 的索引与解析",
                        "Class/package to mod index and resolution"),
    "StackTraceParser.java": ("堆栈解析（含 Caused by 链、模块前缀剥离）",
                              "Stack parsing (cause chains, module prefix stripping)"),
    "LogAnalyzer.java": ("日志 / 崩溃报告解析 + 规则表",
                         "Log and crash report parsing plus the rule table"),
    "Analyzer.java": ("编排：建索引、归属、打分、时间线、出报告",
                      "Orchestration: index, attribution, scoring, timeline, report"),
    "Timeline.java": ("事故时间线：事件抽取、时间戳与级别识别",
                      "Incident timeline: event extraction, timestamps, levels"),
    "McreatorDetector.java": ("MCreator 多信号识别", "Multi-signal MCreator detection"),
    "Theme.java": ("终端与游戏内聊天栏的配色和排版",
                   "Colours and layout for the terminal and the in-game chat"),
    "ClientFont.java": ("在游戏内按真实字形宽度排版",
                        "Lays text out by real glyph widths inside the game"),
    "GraphicsMods.java": ("图形类模组谱系：钠 / iris 各分支与冲突判定",
                          "Graphics families: Sodium/Iris forks and conflict detection"),
    "ProcessProbe.java": ("线程转储解析 + 活体 JVM 采集",
                          "Thread dump parsing and live JVM capture"),
    "LogWatcher.java": ("类 tail -f 的日志监听（处理轮转与编码）",
                        "tail -f style log watching (rotation and encoding)"),
    "Reports.java": ("文本 / Markdown / JSON 渲染", "Text / Markdown / JSON rendering"),
    "Loader.java": ("存放当前版本装进来的加载器实现",
                    "Holds whichever loader implementation this build installed"),
    "LoaderEnv.java": ("加载器适配接口：列模组、取游戏目录",
                       "Loader adapter interface: list mods, resolve the game directory"),
    "LoadedMod.java": ("加载器报出来的一条模组信息", "One mod as reported by the loader"),
    "TickHook.java": ("服务器 tick 的挂载点接口", "Server tick hook interface"),
    "ObaLifecycle.java": ("共享的生命周期：初始化、索引、看门狗、退出钩子",
                          "Shared lifecycle: init, index, watchdog, exit hook"),
    "EarlyLanguage.java": ("尽早从 options.txt 定下报告语言",
                           "Pins the report language early, from options.txt"),
    "Diagnostics.java": ("索引、日志窗口、报告缓存、去抖动调度",
                         "Index, log window, report cache, debounced scheduling"),
    "DiagnosticAppender.java": ("log4j2 采集器（实时抓 stack trace）",
                                "log4j2 appender capturing stack traces live"),
    "CrashReporter.java": ("把报告写进 logs 目录，自动清理旧报告，不覆盖强结论",
                           "Writes reports into logs/, prunes old ones, never overwrites a strong verdict"),
    "Watchdog.java": ("进程看门狗：tick 间隔 / 死锁 / 内存",
                      "Process watchdog: tick gaps, deadlocks, memory"),
    "ObaCommands.java": ("/oba 命令树", "The /oba command tree"),
    "ClientLanguage.java": ("把游戏语言同步给引擎（仅客户端）",
                            "Feeds the game language to the engine (client only)"),
    "ObaMod.java": ("模组入口：注册事件、启动索引与看门狗",
                    "Mod entry point: event registration, index, watchdog"),
    "GameUi.java": ("游戏内界面：/oba 菜单、报告分页、聊天栏渲染",
                    "In-game interface: the /oba menu, report paging, chat rendering"),
    "UiShot.java": ("开发用：把界面渲染成图片，方便离线看排版",
                    "Developer aid: renders the interface to an image for offline layout checks"),
    "NeoForgeEnv.java": ("NeoForge 的加载器适配实现", "NeoForge loader adapter"),
    "NeoForgeTickHook.java": ("NeoForge 的 tick 挂载（含跨小版本探测）",
                              "NeoForge tick hook, with cross-version probing"),
    "ForgeEnv.java": ("Forge 的加载器适配实现", "Forge loader adapter"),
    "ForgeTickHook.java": ("Forge 的 tick 挂载", "Forge tick hook"),
}

LOADER = {
    "1.21.1-neoforge": ("neoforge", "NeoForge"),
    "1.20.2-1.20.4-neoforge": ("neoforge", "NeoForge"),
    "1.20.5-1.20.6-neoforge": ("neoforge", "NeoForge"),
    "1.21.1-forge": ("forge", "Forge"),
    "1.20.2-1.20.4-forge": ("forge", "Forge"),
    "1.20.5-1.20.6-forge": ("forge", "Forge"),
    "1.20.1-forge": ("forge", "Forge"),
}

TOML = {
    "1.21.1-neoforge": ("neoforge.mods.toml", None),
    "1.20.2-1.20.4-neoforge": ("mods.toml", None),
    "1.20.5-1.20.6-neoforge": ("neoforge.mods.toml", None),
    "1.21.1-forge": ("mods-forge.toml", "占位符模板，构建时展开成 mods.toml"),
    "1.20.2-1.20.4-forge": ("mods-forge.toml", "占位符模板，构建时展开成 mods.toml"),
    "1.20.5-1.20.6-forge": ("mods-forge.toml", "占位符模板，构建时展开成 mods.toml"),
    "1.20.1-forge": ("mods-forge.toml", "占位符模板，构建时展开成 mods.toml"),
}

L = {
    "zh": dict(
        wrapper="Gradle 包装器的引导文件",
        engine="诊断引擎（与本版本共编的一份）",
        loader="加载器适配层",
        mod="游戏内那一半",
        toml="模组描述文件（构建时展开占位符）",
        icon="模组图标（256×256，模组列表里显示的那张）",
        icon128="同上，小尺寸一份",
        docs="README 用的标题条",
        intro="""本目录是**自包含的**：下面列的就是这个版本要编的全部源码和资源，拷出去单独构建也行。""",
        footer="> 游戏外的命令行用法：本目录的 jar 同时是可执行的，`java -jar build/libs/*.jar help`。",
        title="## 项目结构",
    ),
    "en": dict(
        wrapper="Gradle wrapper bootstrap files",
        engine="The diagnostic engine (compiled into this build)",
        loader="Loader adapter layer",
        mod="The in-game half",
        toml="Mod descriptor (placeholders expanded at build time)",
        icon="Mod icon (256x256, the one shown in the mod list)",
        icon128="Same icon, small size",
        docs="Title banner used by the READMEs",
        intro="""This directory is **self-contained**: everything below is what this build compiles, and it can be copied out and built on its own.""",
        footer="> The jar from this directory is also executable: `java -jar build/libs/*.jar help`.",
        title="## Project structure",
    ),
}


def d(name, lang, key=None):
    """取说明，取不到就用文件名占位"""
    v = DESC.get(name)
    if v is None:
        return ""
    return v[0] if lang == "zh" else v[1]


def tree_for(name, lang):
    t = L[lang]
    d0 = V / name
    loader, loader_label = LOADER[name]
    toml, toml_note = TOML[name]
    out = [f"{name}/", "│"]

    for f in ("README.md", "README.en.md", "CHANGELOG.md", "LICENSE",
              "build.gradle", "settings.gradle", "gradle.properties",
              "gradlew", "gradlew.bat"):
        if (d0 / f).exists():
            out.append(f"├── {f:<33}{d(f, lang)}")
    if (d0 / "mirror.init.gradle").exists():
        out.append(f"├── {'mirror.init.gradle':<33}{d('mirror.init.gradle', lang)}")
    if (d0 / "examples").is_dir():
        out.append(f"├── {'examples/':<33}{d('examples', lang)}")

    out += ["│", f"├── gradle/wrapper/                 {t['wrapper']}", "│"]
    if (d0 / "docs" / "banner.svg").exists():
        out += [f"├── docs/banner.svg                 {t['docs']}", "│"]

    out += ["├── src/main/java/dev/oba/",
            f"│   ├── {'Launcher.java':<29}{d('Launcher.java', lang)}",
            "│   │",
            "│   ├── cli/",
            f"│   │   └── {'Cli.java':<25}{d('Cli.java', lang)}",
            "│   │",
            f"│   ├── core/                          {t['engine']}"]

    core = sorted((d0 / "src/main/java/dev/oba/core").glob("*.java"))
    for i, f in enumerate(core):
        mark = "└──" if i == len(core) - 1 else "├──"
        out.append(f"│   │   {mark} {f.name:<25}{d(f.name, lang)}")

    out += ["│   │", f"│   ├── loader/                        {t['loader']}"]
    shared = sorted((d0 / "src/main/java/dev/oba/loader").glob("*.java"))
    for f in shared:
        out.append(f"│   │   ├── {f.name:<25}{d(f.name, lang)}")
    out.append(f"│   │   └── {loader}/")
    impl = sorted((d0 / f"src/main/java/dev/oba/loader/{loader}").glob("*.java"))
    for i, f in enumerate(impl):
        mark = "└──" if i == len(impl) - 1 else "├──"
        out.append(f"│   │       {mark} {f.name:<23}{d(f.name, lang)}")

    out += ["│   │", f"│   └── mod/                           {t['mod']}"]
    mods = sorted((d0 / "src/main/java/dev/oba/mod").glob("*.java"))
    for i, f in enumerate(mods):
        mark = "└──" if i == len(mods) - 1 else "├──"
        out.append(f"│       {mark} {f.name:<25}{d(f.name, lang)}")

    out += ["│",
            "├── src/main/resources/",
            f"│   ├── {'oba-icon.png':<29}{t['icon']}",
            f"│   ├── {'oba-icon-128.png':<29}{t['icon128']}",
            "│   └── META-INF/",
            f"│       └── {toml:<25}{t['toml']}"]
    if toml_note:
        out.append(f"│           （{toml_note}）" if lang == "zh"
                   else f"│           (the source file has this name; it ships as mods.toml)")
    return "\n".join(out)


for name in LOADER:
    for lang, fname in (("zh", "README.md"), ("en", "README.en.md")):
        p = V / name / fname
        t = p.read_text(encoding="utf-8")
        title = L[lang]["title"]
        start = t.index(title)
        rest = t[start + len(title):]
        m = rest.find("\n## ")
        if m < 0:
            m = len(rest)
        block = f"""

{L[lang]['intro']}

```
{tree_for(name, lang)}
```

{L[lang]['footer']}
"""
        t = t[:start + len(title)] + block + rest[m:]
        p.write_text(t, encoding="utf-8", newline="")
    print("重写", name, "的中英结构一节")
