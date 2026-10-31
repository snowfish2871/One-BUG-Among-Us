"""按仓库真实内容重写根 README 的「项目结构」一节（中英两份）。

根 README 描述的是整个仓库：引擎 + 加载器实现 + 各版本目录 + 工具。
版本目录内部的细节不在这里展开，各版本 README 自己有一份。
"""
import pathlib

R = pathlib.Path(r"D:\dshgowork\moddoctor")

DESC = {
    "zh": {
        "README.md": "本文档（简体中文）",
        "README.en.md": "English README",
        "LICENSE": "MIT 许可协议全文",
        "dist": "四个版本的 jar 都收在这里（跑 tools/build-all.ps1 生成）",
        "docs": "README 用的标题条",
        "examples": "故意做坏的样例目录：假模组、几种崩溃报告",
        "engine": "诊断引擎的权威副本（各版本目录里的 core/ 都是它的同步副本）",
        "src": "加载器适配实现，按加载器分开放",
        "tools": "构建、同步、测试、实机验证用的脚本",
        "versions": "每个版本一个自包含目录，可以直接构建",
        "engine/main/java": "引擎源码：core / loader 接口 / mod 共享层",
        "engine/main/resources": "描述文件与图标（三份描述文件，各加载器各取所需）",
        "engine/tools": "引擎自测与运行时验证台",
        "src/loader/neoforge": "NeoForge 的加载器实现（1.20.2 起，含 1.21.x）",
        "src/loader/forge": "Forge 的加载器实现（1.20.1）",
    },
    "en": {
        "README.md": "Simplified Chinese README",
        "README.en.md": "This file (English)",
        "LICENSE": "Full MIT license text",
        "dist": "All four version jars land here (produced by tools/build-all.ps1)",
        "docs": "Title banner used by the READMEs",
        "examples": "Deliberately broken samples: fake mods, several crash reports",
        "engine": "The authoritative copy of the diagnostic engine (each version's core/ is a synced copy)",
        "src": "Loader adapter implementations, one directory per loader",
        "tools": "Scripts for building, syncing, testing and real-client verification",
        "versions": "One self-contained, directly buildable directory per version",
        "engine/main/java": "Engine sources: core / loader interfaces / shared mod layer",
        "engine/main/resources": "Descriptors and icons (three descriptors, each loader takes its own)",
        "engine/tools": "Engine self test and runtime harness",
        "src/loader/neoforge": "NeoForge loader implementation (1.20.2 and up, including 1.21.x)",
        "src/loader/forge": "Forge loader implementation (1.20.1)",
    },
}

INTRO = {
    "zh": """引擎只维护一份，放在 `engine/`。各版本目录由 `tools/sync-versions.ps1`
把引擎和对应加载器实现拼出来，所以同一个 jar 里不会混进别的加载器的代码。""",
    "en": """The engine is maintained in exactly one place, under `engine/`. Each version directory is
assembled from it plus the matching loader implementation by `tools/sync-versions.ps1`, so no jar
ever contains another loader's code.""",
}

TITLE = {"zh": "## 项目结构", "en": "## Project structure"}


def tree(lang):
    d = DESC[lang]
    out = ["oba/", "│",
           f"├── {'README.md':<32}{d['README.md']}",
           f"├── {'README.en.md':<32}{d['README.en.md']}",
           f"├── {'LICENSE':<32}{d['LICENSE']}",
           f"├── {'dist/':<32}{d['dist']}",
           f"├── {'docs/':<32}{d['docs']}",
           f"├── {'examples/':<32}{d['examples']}",
           "│",
           f"├── {'engine/':<32}{d['engine']}",
           f"│   ├── {'main/java/dev/oba/*':<28}{d['engine/main/java']}",
           f"│   ├── {'main/resources/':<28}{d['engine/main/resources']}",
           f"│   └── {'tools/':<28}{d['engine/tools']}",
           "│",
           f"├── {'src/loader/':<32}{d['src']}",
           f"│   ├── {'neoforge/':<28}{d['src/loader/neoforge']}",
           f"│   └── {'forge/':<28}{d['src/loader/forge']}",
           "│",
           f"├── {'tools/':<32}{d['tools']}",
           "│   ├── build-all.ps1                    构建全部四个版本并收集到 dist/",
           "│   ├── sync-versions.ps1                把引擎同步进各版本目录",
           "│   ├── run-engine-tests.ps1             跑引擎自测与运行时验证台",
           "│   ├── run-version.ps1                  从实例目录启动客户端做实机验证",
           "│   ├── mirror-neoforged.init.gradle     改写失效的 maven 主机",
           "│   └── crasher/                         故意崩溃的测试模组（两个加载器各一份）",
           "│",
           f"└── {'versions/':<32}{d['versions']}",
           "    ├── 1.21.1-neoforge/",
           "    ├── 1.20.2-1.20.4-neoforge/",
           "    ├── 1.20.5-1.20.6-neoforge/",
           "    └── 1.20.1-forge/"]

    if lang == "en":
        out[out.index("│   ├── build-all.ps1                    构建全部四个版本并收集到 dist/")] = \
            "│   ├── build-all.ps1                    Builds all four versions into dist/"
        out[out.index("│   ├── sync-versions.ps1                把引擎同步进各版本目录")] = \
            "│   ├── sync-versions.ps1                Syncs the engine into each version"
        out[out.index("│   ├── run-engine-tests.ps1             跑引擎自测与运行时验证台")] = \
            "│   ├── run-engine-tests.ps1             Runs the engine self test and harness"
        out[out.index("│   ├── run-version.ps1                  从实例目录启动客户端做实机验证")] = \
            "│   ├── run-version.ps1                  Launches a client for real-client checks"
        out[out.index("│   ├── mirror-neoforged.init.gradle     改写失效的 maven 主机")] = \
            "│   ├── mirror-neoforged.init.gradle     Rewrites the dead maven host"
        out[out.index("│   └── crasher/                         故意崩溃的测试模组（两个加载器各一份）")] = \
            "│   └── crasher/                         Test mod that crashes (one per loader)"
    return "\n".join(out)


for lang, fname in (("zh", "README.md"), ("en", "README.en.md")):
    p = R / fname
    t = p.read_text(encoding="utf-8")
    title = TITLE[lang]
    start = t.index(title)
    rest = t[start + len(title):]
    m = rest.find("\n## ")
    if m < 0:
        m = len(rest)
    block = f"""

{INTRO[lang]}

```
{tree(lang)}
```

> 每个版本目录内部的文件清单，见该目录自己的 README。
""" if lang == "zh" else f"""

{INTRO[lang]}

```
{tree(lang)}
```

> For the file listing inside each version directory, see that directory's own README.
"""
    t = t[:start + len(title)] + block + rest[m:]
    p.write_text(t, encoding="utf-8", newline="")
    print("重写根", fname, "的结构一节")
