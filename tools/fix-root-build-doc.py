"""根 README 的「构建」一节按新结构重写。

原来讲的是单个工程 ./gradlew build，现在是四个版本目录 + 一个收集脚本。
"""
import pathlib

R = pathlib.Path(r"D:\dshgowork\moddoctor")

ZH_OLD = """## 构建

需要 **JDK 21**（Minecraft 1.21 与 NeoForge 21.x 要求 Java 21）。首次构建需要联网。

```bash
./gradlew build
# -> build/libs/oba-1.0.0.jar

./gradlew dist             # 构建并打印 jar 路径
./gradlew runClient        # 启动开发环境客户端
./gradlew runServer        # 启动开发环境服务端
./gradlew runCli --args="analyze --game-dir examples"   # 不启动游戏，直接跑命令行
```

离线构建加 `--offline`，前提是本地 Gradle 缓存里已有 NeoForge 与反编译产物。
注意 `build` 还会执行资源下载任务，需要联网；只想要 jar 时用 `./gradlew jar` 即可。
"""

ZH_NEW = """## 构建

一次构建全部四个版本，产物收在 `dist/`：

```bash
pwsh tools/build-all.ps1
# -> dist/oba-mc1.21.1-1.0.0.jar
#    dist/oba-mc1.20.2-1.20.4-1.0.0.jar
#    dist/oba-mc1.20.5-1.20.6-1.0.0.jar
#    dist/oba-mc1.20.1-forge-1.0.0.jar
```

也可以只构建某一个版本 —— 每个版本目录都是自包含的：

```bash
cd versions/1.21.1-neoforge && ./gradlew jar
cd versions/1.20.1-forge    && ./gradlew jar
```

| 版本 | 需要的 JDK | 备注 |
|---|---|---|
| 1.21.x（NeoForge） | **21** | |
| 1.20.5 ~ 1.20.6（NeoForge） | **21** | |
| 1.20.2 ~ 1.20.4（NeoForge） | **17** | |
| 1.20.1（Forge） | **17** | 用 ForgeGradle，构建含 reobf 步骤 |

首次构建需要联网（要下载并反编译对应版本的 Minecraft）。
之后可以加 `--offline` 复用 Gradle 缓存。

> 这个环境里 `maven.neoforged.net` 连不通，NeoForge 那两条 1.20 轨道靠
> `mirror.init.gradle` 把仓库请求改写到官方 CDN 镜像；`build-all.ps1` 会自动带上它。
"""

EN_OLD = """## Building

Requires **JDK 21** (Minecraft 1.21 and NeoForge 21.x require Java 21). The first build needs
network access.
"""

EN_NEW = """## Building

Build all four versions at once; the jars land in `dist/`:

```bash
pwsh tools/build-all.ps1
# -> dist/oba-mc1.21.1-1.0.0.jar
#    dist/oba-mc1.20.2-1.20.4-1.0.0.jar
#    dist/oba-mc1.20.5-1.20.6-1.0.0.jar
#    dist/oba-mc1.20.1-forge-1.0.0.jar
```

Or build just one — every version directory is self-contained:

```bash
cd versions/1.21.1-neoforge && ./gradlew jar
cd versions/1.20.1-forge    && ./gradlew jar
```

| Version | JDK needed | Notes |
|---|---|---|
| 1.21.x (NeoForge) | **21** | |
| 1.20.5 - 1.20.6 (NeoForge) | **21** | |
| 1.20.2 - 1.20.4 (NeoForge) | **17** | |
| 1.20.1 (Forge) | **17** | uses ForgeGradle, the build includes a reobf step |

The first build needs network access (it downloads and decompiles that Minecraft version).
After that you can pass `--offline` to reuse the Gradle cache.

> On this machine `maven.neoforged.net` is unreachable; the two NeoForge 1.20 tracks work around
> that with `mirror.init.gradle`, which rewrites repository URLs to the official CDN mirror.
> `build-all.ps1` passes it automatically.
"""

for name, old, new in (("README.md", ZH_OLD, ZH_NEW), ("README.en.md", EN_OLD, EN_NEW)):
    p = R / name
    t = p.read_text(encoding="utf-8")
    if old in t:
        t = t.replace(old, new, 1)
        p.write_text(t, encoding="utf-8", newline="")
        print("重写", name, "的构建一节")
    else:
        print(name, ": 没找到原文，跳过")
