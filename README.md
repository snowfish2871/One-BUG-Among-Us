# One BUG Among Us

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

**面向 Minecraft 1.20.1 + **Forge** 47 线 的崩溃诊断工具** · 缩写 **OBA**

指出是哪个模组文件把游戏搞崩的，错在哪个类的哪一行，以及崩溃前后各个模组分别做了什么。

> 这是 **1.20.1 轨道**，加载器是 **Forge**（不是 NeoForge —— 1.20.1 那会儿加载器还叫 MinecraftForge）。实机验证是在 1.20.1 + Forge 47.4.26 上做的。
> 本目录的产物是 `oba-mc1.20.1-forge-1.0.1.jar`。

[English](README.en.md) | 简体中文

> **本项目只做诊断，绝不改动任何文件。**
> 不会禁用、移动、重命名或删除任何一个模组。所有结论只是打印出来并写成报告，
> 是否采取行动由你决定。

---

## 兼容性

按**版本轨道**发布：一条轨道覆盖「一套 API」下的所有 Minecraft 小版本，
同一轨道内模组代码不需要任何改动就能跑。这么分是因为 NeoForge 在这些地方改过 API，
强行塞进一个 jar 反而会互相拖累。

| 项目 | 值 |
|---|---|
| Minecraft | 1.20.1 |
| 加载器 | **Forge** 47 线 |
| Java | 17 |
| 编译目标 | `1.20.1` / `47.4.26` |
| 状态 | **已实机验证** |

> **1.20.1 走的是 Forge，不是 NeoForge。** 那会儿 NeoForge 还没独立出来 ——
> 加载器还叫 MinecraftForge，包名是 `net.minecraftforge.*`，和现在的
> `net.neoforged.*` 是两套完全不同的东西。所以 1.20.1 单独有一条 Forge 轨道。
> **1.20 不在支持范围内**：那是最早的 1.20，两边都还没就位。

### 构建

本目录是自包含的，直接在这里构建：

```bash
./gradlew jar
```

产物在 `build/libs/` 下。构建脚本用的是本目录 `src/main` 里的源码。

### 其他轨道

同一个模组按**版本轨道**分开发布，每条轨道一个独立目录：

| 目录 | Minecraft | 加载器 |
|---|---|---|
| `versions/1.21.1-neoforge/` | 1.21 ~ 1.21.x | NeoForge 21.1+ |
| `versions/1.20.5-1.20.6-neoforge/` | 1.20.5 / 1.20.6 | NeoForge 20.5 / 20.6 |
| `versions/1.20.2-1.20.4-neoforge/` | 1.20.2 / 1.20.3 / 1.20.4 | NeoForge 20.2 / 20.3 / 20.4 |
| `versions/1.20.1-forge/` | 1.20.1 | Forge 47 |

引擎源码只有一份，放在 `engine/`。各版本目录里的源码是按加载器拼出来的副本，
用 `tools/sync-versions.ps1` 同步 —— 改引擎请改 `engine/`，然后重跑那个脚本。

### 加载器适配层

1.20.1 上用不了 NeoForge —— 那会儿加载器还叫 MinecraftForge，包名是
`net.minecraftforge.*`，和现在的 `net.neoforged.*` 是两套完全不同的东西。
所以这一节说的是**同一套诊断逻辑怎么跑在两套加载器上**。

共享代码不引用任何加载器的类，只认 `dev.oba.loader` 里的三个接口：

| 接口 | 作用 |
|---|---|
| `LoaderEnv` | 列模组、取游戏目录、加载器名字 |
| `TickHook` | 挂服务器 tick（三套事件类各不相同） |
| `Loader` | 存放当前轨道装进来的实现 |

每个轨道额外编译一份自己的实现：

| 目录 | 给谁用 |
|---|---|
| `src/loader/neoforge/` | 1.20.2 起的所有 NeoForge 轨道，含 1.21.x |
| `src/loader/forge/` | 1.20.1 的 Forge 轨道 |

各轨道的 `build.gradle` 只把对应那一份加进源目录，所以同一个 jar 里
不会同时出现两套实现。`@Mod` 入口、事件注册、事件类这些没法抽象的部分
也放在各自的实现里 —— 两边没有共同的父类可用，只能各写一份，
但真正干活的逻辑全在共享的 `ObaLifecycle` 里。

### 1.20.1（Forge）轨道的现状

**代码是完整的，但 jar 还没构建出来。** 该做的都做完了：

- 加载器适配层和 `src/loader/forge/` 下的实现
- Forge 版的 `@Mod` 入口，事件注册、命令注册、tick 挂载都接好了
- `tracks/1.20.1-forge/` 的构建配置（用 ForgeGradle 6，走官方工具链）

卡住的地方是 **ForgeGradle 首次运行要下载并反混淆整个 1.20.1**。
实测：`maven.neoforged.net` 那台是不通的（本仓库其余轨道靠改写仓库地址绕过了），
但 ForgeGradle 自己那套 `MavenArtifactDownloader` 会在
`DownloadUtils.downloadFile` 里长时间阻塞在同一批下载上，
12 分钟里 CPU 累计时间一点没涨、缓存也不再增长，等于停住了。

也就是说这不是代码问题，是这台机器的网络到 ForgeGradle 下游那几个主机不稳。
换一台网络正常的机器，`./gradlew -p tracks/1.20.1-forge jar` 就能出 jar。

**顺带说明为什么 1.20.1 不能用 1.20.2 那套办法**：NeoForge 那几条轨道能用
NeoGradle 构建，是因为 1.20.2 起加载器包名统一成了 `net.neoforged.*`；
1.20.1 的 Forge 生产环境跑的是 SRG 命名（`net.minecraft.commands.Commands.m_82127_`），
而本机那个客户端 jar 是混淆的（`dt.a`），中间必须过一整条反混淆流水线。
我试过自己用本机的映射文件离线重映射，但 MCP 那份 TSRG2 里的名字是
`net/minecraft/src/C_xxxxx_` 这种中间占位名，不是最终类名，接不上，
所以还是得走 ForgeGradle。

### 为什么轨道要这么切

切在 1.20.4 和 1.20.6 之间，是因为 **1.20.5 把 tick 事件重做了一遍**：

| 版本 | tick 事件长什么样 |
|---|---|
| 1.20.2 ~ 1.20.4 | `net.neoforged.neoforge.event.TickEvent$ServerTickEvent`，带 `Phase`（START / END），**一次 tick 触发两回** |
| 1.20.6 及以后 | `net.neoforged.neoforge.event.tick.ServerTickEvent$Post`，拆成 Pre / Post 子类，**一次 tick 触发一回** |

同一个东西在两边的全限定名、构造器、继承关系都不一样，编译期只能选一套。
跨过这条线的部分由 `TickHook` 的实现在**运行时**探测该用哪套，
所以两个轨道的源码是同一份，不需要各维护一份。

### 已验证和未验证的

| | 状态 |
|---|---|
| 1.21.1 + NeoForge 21.1.256 | **真客户端实机验证过** |
| 1.20.2 + NeoForge 20.2.93 | **真客户端实机验证过** |
| 1.20.5 + NeoForge 20.5.21-beta | **真客户端实机验证过** |
| 各轨道内部的其余小版本 | 依据「同一轨道 API 一致」推断可用，未逐一验证 |

如果你在某个版本上跑出了 OBA 自己的报错，日志里会出现
`[OBA] OBA 在这个版本上没能启动`、`[OBA] xxx 注册失败` 或
`[OBA] 在这个版本上没能挂上服务器 tick 事件`，
把那一行连同版本号提个 issue 即可。

### 实机验证结果

两条 1.20 轨道都在真客户端上跑过了：往 `mods` 里放一个**故意在构造阶段抛异常**的
测试模组，启动客户端，看 OBA 能不能自己把元凶点出来、并把报告落到 `logs` 下。

| 轨道 | 客户端版本 | 结果 |
|---|---|---|
| 1.20.2 ~ 1.20.4 | 1.20.2 + NeoForge 20.2.93 | **定位到 CrasherMod.\<init\> (CrasherMod.java:15)**，报告落盘 |
| 1.20.5 ~ 1.20.6 | 1.20.5 + NeoForge 20.5.21-beta | 同上，报告落盘 |

顺带在这次实测里修掉了两个只有真跑才暴露出来的问题：

**1. 描述文件名在两个版本段里不一样**

NeoForge 20.2~20.4 找的是 `META-INF/mods.toml`，20.5 起才改名
`META-INF/neoforge.mods.toml`。名字不对的话加载器连文件都不看，
表现是"模组文件夹里有两个文件，但一个都没加载"，日志里只有
`found 2 invalid mod files`。现在两份描述文件都在，各自的轨道排掉另一份。

**2. 旧格式的依赖块多一个必填字段**

20.2~20.4 的 `[[dependencies]]` 要求 `mandatory`，少了会直接报
`Missing required field mandatory in dependency`。这个字段还必须写在
`type` 前面 —— 加载器是按顺序流式读这几个键的。

### 关于 1.20 和 1.20.1

**这两个版本用不了 NeoForge，因此不在支持范围内。** 实测确认：把 OBA 放进
一个 1.20.1 实例后，加载器直接拒绝：

```
Mod ID: 'neoforge', Requested by: 'oba', Expected range: '[20.2.0,20.5)', Actual version: '[MISSING]'
net.minecraftforge.fml.ModLoadingException: Mod 'oba' requires 'neoforge' '20.2.0 or above'
```

那个实例的库是 `net.minecraftforge:eventbus:6.0.5`、`net.minecraftforge:forgespi:7.0.1`、
`net.neoforged.fancymodloader:loader:47.2.2` —— 这是 **MinecraftForge 47.2.2**，
包名是 `net.minecraftforge.*`。NeoForge 从 **1.20.2** 才开始独立发布，
所以「1.20 全小版本 + NeoForge」这个组合实际从 1.20.2 起。
想支持 1.20.1 得另起一套 Forge 代码，那是另一个项目了。

### 为什么能跨小版本

模组只依赖**跨版本稳定的那部分 API**：日志后端（log4j2）、模组生命周期事件、
事件总线、命令注册、以及聊天栏文本。它不碰渲染、不碰世界数据、不碰注册表内容，
所以不会随小版本迭代而失效。唯一踩到版本差异的就是 tick 事件，已经用适配层隔离掉。

同时整个初始化都做了故障隔离：采集器安装、类索引、事件注册、命令树、tick 适配
各自独立成一段，任何一段因为版本差异抛异常，都只会让**那一个功能**停用，
并在日志里留下一句说明。OBA 是个诊断工具，它自己绝不该成为游戏起不来的原因。

### 加一条新轨道

NeoForge 出了新版 Minecraft 时，照下面三步就能加一条：

1. 复制 `tracks/1.20.5-1.20.6/` 成 `tracks/<新版本>/`；
2. 改它 `gradle.properties` 里的 `minecraft_version`、`minecraft_version_range`、
   `neo_version`、`neo_version_range`、`archive_suffix` 和 `parchment_*`；
3. `./gradlew -p tracks/<新版本> jar`。

共享代码直接从仓库根目录取（见轨道里的 `build.gradle`），**不需要复制源码**，
所以新轨道自动跟上所有功能改动。如果新版本又动了 API，
照 `src/loader/` 下那两套 `TickHook` 实现的样子加一层运行时适配即可。


---

## 快速开始

### 游戏内

1. 把 `oba-1.0.1.jar` 放进 `.minecraft/mods/`
2. 启动游戏

此后**不需要输入任何命令**。一旦出错，结论会自己出现在日志和聊天栏，
完整报告会自己落到 `logs` 目录：

```
[OBA] 定位到出错模组: Awesome Machines  [awesome-machines-2.1.0.jar]
[OBA] 出错位置: com.example.awesomemachines.registry.MachineRegistry.register (MachineRegistry.java:143)
[OBA] 可信度: 极高 (99%)
[OBA] 报告已写入 D:\MC\.minecraft\logs\oba-report-2026-10-10_11.31.58.txt
```

想主动看完整报告时再敲 `/oba report`。

### 命令行

```bash
# 自动找到 .minecraft，分析最新崩溃报告，并把报告写进 logs
java -jar oba-1.0.1.jar analyze

# 指定游戏目录（路径带空格时，加不加引号都能识别）
java -jar oba-1.0.1.jar analyze --game-dir "D:\我的 游戏\.minecraft"

# 直接喂一个崩溃报告
java -jar oba-1.0.1.jar analyze crash-reports/crash-2026-10-09_18.03.20-client.txt

# 列模组 + 查重复 + 查 MCreator + 标出 Fabric 模组
java -jar oba-1.0.1.jar mods --game-dir "D:\MC\.minecraft"

# 实时监听，一边玩一边报
java -jar oba-1.0.1.jar watch "D:\MC\.minecraft\logs\latest.log"
```

### 试用自带样例

`examples/` 是一个**故意做坏的**迷你游戏目录：六个假模组、三份崩溃报告、一份缺前置的日志。

```bash
# NeoForge 模组崩溃
java -jar oba-1.0.1.jar analyze examples/crash-reports/crash-2026-10-09_18.03.20-client.txt

# Fabric 模组经 Connector 崩溃
java -jar oba-1.0.1.jar analyze examples/crash-reports/crash-2026-10-10_11.35.02-client.txt

# F3+C 手动调试崩溃
java -jar oba-1.0.1.jar analyze examples/crash-reports/crash-2026-10-10_16.47.12-client.txt

java -jar oba-1.0.1.jar mods     --game-dir examples
java -jar oba-1.0.1.jar mcreator --game-dir examples
```

预期结果：第一份指向 `awesome-machines-2.1.0.jar` 的 `MachineRegistry.java:143`；
第二份指向 `sodium-extra-0.6.0.jar` 的 `MixinOptionsScreen.java:47`；
第三份说明这是人为触发；`mods` 报出 `sodium` 的重复 modId；`mcreator` 命中 `coolstuff-1.0.1.jar`。

---

## 界面

界面分两处：命令行和游戏内聊天栏。两边是同一套视觉——双线标题框、右上角版本号、
`◈` 品牌标记、分段横线、当前项高亮条——**而且两边的菜单项都能直接点。**

### 命令行菜单

不带参数直接跑就是菜单：

```bash
java -jar oba-1.0.1.jar
```

```
╔════════════════════════════════════════════════════════════════╗
║  ◈ One BUG Among Us CLI                             OBA v1.0.1 ║
╚════════════════════════════════════════════════════════════════╝

  工作目录    D:\hmcl\.minecraft
  状态        ● 就绪
  构建        One BUG Among Us 1.0.1

  • 选择操作 ──────────────────────────────────────────────────────

   ▶  [1]  诊断最新的崩溃或日志                           执行
  ╭──────────────────────────────────────────────────────────╮
  │ [2]  列出已加载模组                                 执行 │
  ╰──────────────────────────────────────────────────────────╯
  │ [3]  识别 MCreator 生成的模组                       执行 │
  │ [4]  实时跟踪 latest.log                            跟踪 │
  │ [5]  采集并归属线程转储                             采集 │
  │ [6]  查看进程与索引状态                             执行 │
  │ [7]  切换语言                                       切换 │
  │ [8]  打开最新的报告文件                             打开 │
  │ [9]  退出                                           退出 │

  ↑ ↓ 切换    ENTER 确认    数字键直达    Q 退出
```

- `↑` `↓` 移动，`ENTER` 确认，`Q` 退出。也可以直接按数字键 `1`–`9` 跳到那一项。
- **带颜色的名字是超链接。** 在支持超链接的终端（Windows Terminal、iTerm2、
  绝大多数现代终端）里点一下，会在**新窗口**里单独跑那一条命令，主菜单不用退出来。
  实现方式是给每一项写一个一次性启动脚本，再把脚本路径包成 OSC 8 超链接发给终端；
  终端不支持的话控制码会被直接忽略，只剩纯文字，界面照旧。脚本写在临时目录里，退出时删掉。
- 不想生成这些脚本：加 `-Doba.clickable=false`，或设环境变量 `OBA_CLICKABLE=false`。
- **管道、重定向、CI 里跑（不是交互式终端）时不会进菜单**，只打印用法——
  免得控制码混进日志。

颜色按 `-Doba.color` → `OBA_COLOR` → 「是不是交互式终端」的顺序判断。
`NO_COLOR` 非空、`TERM=dumb`、输出被重定向，任何一个成立就不上色。

### 游戏内

`/oba` 不带参数就是可点菜单：

```
╔════════════════════════════════════════════════════════════╗
║  ◈ One BUG Among Us                              OBA v1.0.1 ║
╚════════════════════════════════════════════════════════════╝

  • 选择操作 ──────────────────────────────────────────────

  █ ▶ [1] 显示最新一份完整报告                            █
    [2] 进程与索引状态                             执行
    [3] 列出已加载模组                             执行
    [4] 识别 MCreator 生成的模组                   执行
    [5] 采集并归属线程转储                         执行
    [6] 诊断最新的崩溃报告                         执行
    [7] 重建模组索引                               执行

  ────────────────────────────────────────────────────────
  • 需要带参数的命令 ─────────────────────────────────────
    /oba info <id>      模组简介与元数据
    /oba analyze <file> 诊断指定的某个文件

  带颜色标记的都能点。
  https://github.com/OneBUGAmongUs/oba
```

第一项（反白条那条）是推荐动作，它本身就是第 1 项，也点得动；所以下面接着从 `[2]` 编号。

每行都能点，点一下跑对应子命令。除此之外：

| 哪里 | 点什么 | 会发生什么 |
|---|---|---|
| `/oba mods` 的模组列表 | 模组名 | 跑 `/oba info <id>`，看简介 |
| 报告里的路径 | 路径 | 填进输入框，补个名字就能跑 |
| 帮助页的 `/oba info <id>` | 那条命令 | 填进输入框等你补名字 |
| 帮助页的项目地址 | 网址 | 用浏览器打开 |

游戏内不用 ANSI 控制码（聊天栏不认），而是用同一套视觉元素直接拼组件：同样的双线标题框、
分段横线、反白条，同样的语义色（框线、品牌、强调、正文、提示各一色）。

宽度这块和终端完全不是一回事：**原版聊天栏不是等宽字体**——空格 4 像素、`i` 2 像素、
小写字母 6 像素、汉字 9 像素。所以游戏内每一行都按像素量宽再补空格，行尾不留多余空格、
超长行按像素折行。`GameUi` 里存着一份从原版位图字体量出来的宽度表，进游戏后再用客户端
字体度量校准，因此标题框三行是严格等像素宽的，中英混排也不会错位。

报告正文是引擎那边照**终端 100 列**拼好的，直接塞进 320 像素宽的聊天栏会被原版自动折行 ——
折出来的残段看上去就是一串乱码（`── 致命 ─────…` 这种分级线会散成好几行横杠）。
所以送进聊天栏之前会先按聊天栏宽度重排：整行几乎只有横线的收成一条满宽分隔线，
其余按像素折行，颜色跟着每一段单独带上。加粗的字每个多占 1 像素，量宽度时也算进去，
不然加粗的分段标题、反白条会正好被挤下去折成两行。


---

## 事故时间线

只给出元凶有时并不够——你还想知道「崩之前到底发生了什么」。
OBA 会把日志整理成一条带时间戳的时间线，逐条标明是哪个模组在说话：

```
[事故时间线] (最近发生的排在最前面)
  时间         模组               发生了什么
      22:09:18 -                   + Failed loading villager cartographer registered at cartographer
      22:09:10 -                   * Error loading class: net/caffeinemc/... (ClassNotFoundException)
      22:08:04 -                   + registered internal shaders
      22:07:54 kubejs              + Key mapping key.kubejs.itemkillkey registered after event
      22:07:49 modernui            + Redirect registered font 'Source Han Sans SC' to 'modernui:...'
      22:07:21 -                   * Failed to find class: java.util.zip.ZipException: zip END header not found
```

### 怎么读

- **从上往下读**，第一行就是最接近崩溃的那一刻，越往下越早。
- **第二列是模组**，`-` 表示这一行没能归属到具体模组（多为原版或加载器输出）。
- **第三列是标记**：`!!` 错误、`!` 警告、`*` 命中规则、`+` 结构性事件、`·` 普通信息。

### 设计上的取舍

日志动辄几万行，直接把最后几百行贴出来没有意义——末尾多半是无关的例行输出。
所以时间线是**按重要程度挑选**的：致命和错误一律保留，命中规则的优先，
归属到具体模组的加分，越接近出事时刻的分越高；挑完再按原始时间顺序放回去。

同一行连续重复超过三次会被折叠成一条提示，防止一段刷屏把时间线冲垮。

时间线同时出现在三种输出里：

| 格式 | 呈现方式 |
|---|---|
| 纯文本 | 对齐的表格，最多 40 行 |
| Markdown | 三列表格，最多 30 行 |
| JSON | `timeline` 数组，字段为 `time` / `modId` / `modName` / `type` / `event` |

---

## 诊断报告

### 保存在哪里

**统一放在游戏的 `logs` 目录下**，游戏内和命令行一致。

| 文件 | 什么时候产生 |
|---|---|
| `logs/oba-report-<时间戳>.txt` | 捕获到错误、卡死、游戏退出、或发现新崩溃报告时 |
| `logs/oba-latest.txt` | 同上，固定名字那份，永远是最新的 |

时间戳格式为 `yyyy-MM-dd_HH.mm.ss`。报告只保留最近 **10** 份，
超出的按时间从旧到新删除，不会把日志目录塞满。

### 触发时机

| 时机 | 说明 |
|---|---|
| **捕获到错误** | 日志里出现可归属的报错时立刻写一份。同一类错误（元凶 + 异常相同）只写一次 |
| **卡死** | 看门狗抓到线程转储并定位后写一份。服务器线程在做原版重活（存区块、退世界、自动保存）时不报，避免噪音 |
| **游戏退出** | 只要这次会话抓到过东西，退出时兜底再写一份。除 FML 关闭事件外还挂了 JVM shutdown hook |
| **F3+C 调试崩溃** | 这类崩溃不写任何日志，只在 `crash-reports` 下落一个文件。退出时会扫描该目录，把本次运行新出现的报告补一份诊断 |
| **命令行** | `analyze` 找到问题时，除了打印也会往 `--game-dir` 的 `logs` 下写一份；显式指定 `--output` 则按指定位置写 |

每份报告开头都会说明是什么原因触发的：

```
OBA v1.0.1
由捕获到的错误触发
报告内容为只读诊断结果，未对任何文件做出改动。
════════════════════════════════════════════════════════════════════════
```

### 报告内容

一份完整报告依次包含：环境信息、**结论**（元凶与出错位置）、**事故时间线**、
可疑模组排序、每个模组的详细证据链、规则命中的问题清单、MCreator 识别结果、
以及无法归属的包。

### 怎么读结论

```
[结论]
  最可能的元凶 : Awesome Machines (awesomemachines)
  模组文件     : awesome-machines-2.1.0.jar
  SHA-1        : 0ea0b938b2eb0a092f37a38d95e8bcde8158840e
  可信度       : 极高 (85%)
  出错位置     : com.example.awesomemachines.registry.MachineRegistry.register (MachineRegistry.java:143)
  异常         : java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null
```

- **`出错位置`** 是最有价值的一行，`MachineRegistry.java:143` 直接对应作者源码。
- **`SHA-1`** 用来确认文件没被改过。给作者报 bug 时附上能省掉「你版本不对」的扯皮。
- **`可信度`** 是证据权重折算的区间，不是概率。它回答的是「证据够不够硬」。
- **`证据链`** 列出每一条加分理由。结论看着不对时，看这里就知道是哪一步推错了。
- **`[精确匹配类名]`** 表示这个类完整存在于该 jar；若显示 `[包名匹配 xxx (72%)]`，
  说明只匹配到包名前缀，可信度会打折。

---

## 使用方式

### 游戏内命令

不用敲任何命令也能拿到诊断；下面这些是想要更多信息时用的。

| 命令 | 作用 |
|---|---|
| `/oba` | **打开可点菜单**（每一项点一下就执行） |
| `/oba report` | 显示最近一次完整诊断，并写入 `logs` |
| `/oba status` | 进程状态：内存、线程、死锁、已索引模组数、报告目录 |
| `/oba mcreator` | 列出疑似 MCreator 生成的模组及判定理由 |
| `/oba mods` | 列出已加载模组、版本、类数量、重复 modId、所属图形族 |
| `/oba info <id>` | **查看某个模组的简介与元数据** |
| `/oba threads` | 采集线程转储并做归属分析 |
| `/oba crash` | 分析磁盘上最新的崩溃报告 |
| `/oba analyze <file>` | 分析指定的日志或崩溃报告 |
| `/oba reindex` | 重新扫描模组并重建类索引 |
| `/oba help` | 命令列表 |

单人、局域网主机、专用服务器控制台都可以使用，不会改变任何世界状态。

### 点开一个模组看简介

`/oba mods` 列出的每一行都**可以点**。把鼠标移到那一行上会提示
「在 /oba mods 里点模组名就能看到这些」，点下去就展开该模组的详情：

```
Awesome Machines  （awesomemachines）
  一台会自己造机器的多方块机器，需要 FE 供能。
  支持 JEI 查看配方，配置文件在 config/awesomemachines-common.toml。

  文件          awesome-machines-2.1.0.jar
  版本          2.1.0
  作者          Example Author
  许可          GPL-3.0
  加载器        javafml
  类数量        5
  文件大小      1.6 KB
  SHA-1         0ea0b938b2eb0a092f37a38d95e8bcde8158840e
  重复：另有 1 个文件声明了同一个 mod ID
  疑似 MCreator 生成（23 分）
  图形类模组：钠系（Sodium）（渲染优化主体）
      钠在 Forge/NeoForge 上的主力移植
```

简介来自模组自己的元数据（`mods.toml` 或 `fabric.mod.json` 里的 `description`），
一个 jar 里打包了多个模组时会把各自的简介都列出来。
除了简介，这一段还刻意带上了几个**判断问题时最常用**的字段：
SHA-1（报 bug 时要用）、重复情况、是不是 MCreator、以及属于哪个图形谱系。
元数据里没写简介的会直说「没有写简介」，不会留一片空白。

聊天栏一次放不下太多内容，超过 24 行会截断并指向报告文件。

也可以直接敲命令，效果一样：

```
/oba info sodium
/oba info awesomemachines
```

### 命令行工具

```
oba                               交互式主菜单（方向键选择，菜单项可点）
oba menu [--pick <序号|名字>]     交互式主菜单，或直接运行菜单里的某一项
oba analyze [日志或崩溃报告...]   指出出错的模组和行号
oba mods                          列出模组目录并检查问题
oba mcreator                      只跑 MCreator 识别
oba watch [latest.log]            实时监听并在出问题时报告
oba threads [dump.txt]            分析线程转储
oba version / help
```

| 选项 | 含义 |
|---|---|
| `--mods-dir <路径>` | 模组目录（默认自动探测） |
| `--game-dir <路径>` | 游戏根目录，含 `mods/` `logs/` `crash-reports/` |
| `--format text\|md\|json` | 输出格式，默认纯文本 |
| `--output <文件或目录>` | 把报告写到指定位置，而不是 `logs/` |
| `--lang <en\|zh>` | 报告语言（默认跟随系统） |
| `--pick <序号\|名字>` | 只给菜单用：不进菜单，直接跑某一项 |
| `--color` | 强制上色，管道里也上 |
| `--no-color` | 关闭 ANSI 颜色，优先级最高 |
| `--no-mcreator` | 跳过 MCreator 检查，更快 |
| `--thread <名称>` | 指定要重点分析的卡死线程，如 `"Server thread"` |
| `-q` / `-v` | 精简输出 / 出错时打印堆栈 |

菜单项做成可点脚本这件事，可以用 JVM 参数 `-Doba.clickable=false`（或环境变量
`OBA_CLICKABLE=false`）关掉。颜色的判断顺序见[界面](#界面)。

**退出码**：`0` 无问题 · `2` 发现问题 · `64` 参数错误 · `70` 内部错误。

### 多语言

报告可以在中文和英文之间切换，覆盖引擎、命令行、游戏内提示和日志输出的全部文案。

**语言判定顺序**：

1. `--lang <en|zh>` 命令行参数
2. 环境变量 `OBA_LANG`，或 JVM 参数 `-Doba.lang=zh`
3. 游戏内模组：玩家在游戏设置里选的语言（客户端）
4. JVM 默认区域设置（`user.language`）
5. 兜底：英文

也就是说，**中文客户端自动出中文报告，其他语言的客户端自动出英文报告**，不用手动配。

第 3 条在客户端上分两层读，这是实机验证时发现必须要的：

| 时机 | 怎么读 | 为什么 |
|---|---|---|
| 模组构造函数 | 直接读 `options.txt` 里的 `lang:` | 模组如果在构造阶段就崩，整套加载流程当场结束，`FMLCommonSetupEvent` 根本不会触发。挂在那边的话，这种崩溃的报告语言就退回 JVM 区域了，和玩家选的不是一回事 |
| 客户端就绪 / 进世界 | `LanguageManager.getSelected()` | 这才是权威值。玩家中途改了语言也能跟上 |

`options.txt` 是 UTF-8 纯文本，很小，构造函数阶段读它没有任何负担。


```bash
java -jar oba-1.0.1.jar analyze --lang en --game-dir examples
OBA_LANG=en java -jar oba-1.0.1.jar mods --game-dir examples
```

所有文案集中在一张表里（`engine/main/java/dev/oba/core/Lang.java`），以 `namespace.key` 形式索引。
报告文本存的是**文案键加参数**而不是成品句子，渲染时才解析，
因此已缓存的报告在切换语言后依然能正确重渲。

> 引擎没有使用 Minecraft 自带的 `assets/<modid>/lang/*.json`：
> 命令行工具根本不加载游戏，共用一张表比维护两套更可靠。
> 新增语言的方法是加一个常量、扩展 `detect()` 分支，并给每条 `put(...)` 补一列；
> 任何语言缺键，自测台会直接失败。

---

## 定位原理

### 1. 建索引：哪个类属于哪个 jar

读取每个模组 jar 的条目表（**只读目录，不解压、不落盘**），建立两张表：

- **类名 → jar**：精确匹配，置信度 1.0
- **包名 → jar**：前缀匹配，最高 0.9，随包层级递减

包名匹配**绝不退化到单段包名**（`com`、`org` 之类），否则任何 `com.*` 的模组
都会认领别人的栈帧。两个 jar 共享同一包名时，在其中贡献类更多的那个胜出。

描述文件同时支持 `META-INF/neoforge.mods.toml`、`META-INF/mods.toml`
和 Fabric 的 `fabric.mod.json`，因此无论模组为哪个加载器构建，都能拿到真实的名称、id 和版本。

### 2. 解析堆栈

真实日志比教科书复杂得多，解析器覆盖了这些情况：

- `at a.b.C.m(File.java:12)` 标准栈帧
- `a.b.C$$Lambda$12/0x...` lambda 与合成帧
- `a.b.C$Inner.run(...)` 内部类
- `Native Method` / `Unknown Source` 这类没有行号的帧
- `... 23 more` 省略标记
- **多行异常消息**：JVM 会把消息原样跨行打印，解析器知道在哪里收住，
  否则紧跟其后的栈帧会被当成消息一起吞掉
- `Caused by:` / `Suppressed:` 异常链，按根因向外的顺序连接，与
  `Throwable.getCause()` 的语义一致
- **NeoForge 的真实帧格式**：末尾带 `~[模块@版本/...] {re:classloading}`，
  类名前还可能有 `TRANSFORMER/crasher@1.0.0/` 这类两层模块前缀，都会被正确剥离
- 带前缀的日志行：`[18:03:20] [main/ERROR] java.lang.XxxException: ...`

永远不属于用户模组的前缀会被过滤掉（`java.`、`net.minecraft.`、`net.neoforged.`、
`org.spongepowered.asm.`、`com.mojang.` 等），外加 Fabric 加载器与 Connector 的内部实现，
以及 OBA 自己。

### 3. 打分

| 证据 | 权重 |
|---|---|
| 根因异常链中的栈帧 | `0.30 + 0.10 × 链深度` |
| 外层异常链中的栈帧 | 上式 × 0.45 |
| 精确匹配类名 | × 1.25 |
| 包名匹配 | × (置信度 + 0.4) |
| **根因链中第一个定位到的帧** | **+0.85** |
| 命中规则，FATAL | +1.6 |
| 命中规则，ERROR | +1.0 |
| 文本中出现该模组的 modId | +0.15 |

低于噪音下限（0.18）的会被丢弃，避免报告被无关模组淹没。

公开权重的意义在于：**结论错了能追溯到是哪一步推错的**，而不是一个黑箱。

### 卡死是怎么判断的

卡死不会抛异常，上面的表用不上。流程是：

1. 看门狗测量服务器 tick 之间的间隔；
2. 超过 12 秒就采集一份**线程转储**（`ThreadMXBean`，纯读取）；
3. 把卡住线程的栈帧归属到模组；
4. 越靠栈顶的帧、越接近顶部的模组越可疑。

这回答的是「**卡住的时候是谁的代码在跑**」，属于推断而非证明，报告里也这么写。
`/oba threads` 可以随时手动采集一份。

---

## MCreator 识别

参考社区实现 [McreatorDetector-1192](https://github.com/yiyuyan/McreatorDetector-1192)
的思路（元数据文本、包名、procedures 包、文件名），并补充了编译产物里真实存在的结构特征。
判定采用**多信号打分**，而非单一条件。

**判定性特征**（MCreator 特有，出现即高度可疑）

| 信号 | 分值 | 说明 |
|---|---|---|
| 包根为 `net.mcreator.*` | 6 | MCreator 默认代码包名，最强信号 |
| 存在 `procedures` 包 | 4 | 每个可视化「过程」编译成一个类 |
| 存在 `*ModElement` 类 | 4 | 「模组元素」注册代码 |
| 资源里带 "Generated by MCreator" | 5 | 内容探测，有上限地读少量小文件 |
| 元数据出现 `mcreator` 字样 | 4 | 名称 / 描述 / 作者 / 链接 |
| 存在 `variables` 包 | 2 | MCreator 的全局变量系统 |
| 文件名含 `mcreator` | 3 | |

**佐证性特征**（只有已出现上面那些痕迹时才加分）

| 信号 | 分值 | 说明 |
|---|---|---|
| `*Model` / `*Renderer` 类 ≥ 2 个 | 2 | 自动生成的实体 / 盔甲模型 |
| `ModItems` / `ModBlocks` 等集中注册类 | 2 | 生成风格 |
| 资源结构呈生成布局 | 2 | `models/` + `data/` + `procedures` 类成对出现 |

分低于 **6** 不算 MCreator。这个阈值和「佐证特征单独不加分」的规则是**实机校准**出来的：
拿一个真实的 236 模组整合包跑，原来的写法报了 **98 个**，其中
`Alex 的生物`、`JEI`、`Xaero 的小地图` 这些手写模组全被误判——它们只是用了
`*Model`、`ModItems` 这类**任何模组都会有的普通 Java 命名**。
改完之后同样的包只报 **2 个**，两个都确实带着 `net.mcreator` 包、`procedures` 包和生成器标记。

**每条命中都会打印出来**，不是只给一个是 / 否，所以误报时你能指出是哪个信号导致的。

> 一个「文件名含 mcreator、描述写 Made with MCreator」的**手写模组**也会被命中——
> 这是有意的：识别的是「生成特征」，不是「作者身份」。命中不代表该模组质量差。

---

## 图形类模组专项

钠（Sodium）、iris 这一系是整合包崩溃和画面异常的高发区，而且**光看堆栈查不出来**——
它们的冲突往往不抛异常，只是崩在渲染线程、黑屏、或者光影加载失败。
更重要的是，这一系有大量**换名字的分支**：

| 族 | 分支（同一个东西的不同名字） |
|---|---|
| **钠系** | Sodium、Rubidium、Embeddium、Xenon、Sodium Reforged、Sodium Forge |
| **iris 系** | Iris Shaders、Oculus、NeOculus |
| **锂系** | Lithium、Radium、Canary |
| **氪系** | Krypton、Krypton Reforged |
| **磷系** | Phosphor、Phosphor Create |

OBA 认这些分支的亲戚关系，所以能查出两种**只看日志永远发现不了**的问题。

### ① 同一族装了两个互斥的分支

```
[图形类模组] (钠、iris 及其各分支)
  ! 钠系（Sodium） 同时装了多个分支
      - embeddium-0.3.31.jar
      - rubidium-0.7.1.jar
    建议：embeddium / rubidium 换名字但干的是同一件事，彼此互斥。
          只保留其中一个、其余移除。同时装两个是黑屏或者崩在渲染线程的常见原因。
```

### ② 光影加载器缺了配套的渲染主体

```
  ! oculus 没有可以挂靠的渲染主体
    oculus-1.7.0.jar
    建议：oculus 是光影加载器，自己不带渲染实现，必须配合 embeddium 或 rubidium。
          缺了它通常在主菜单之前就抛 NoClassDefFoundError。
```

`iris` 要配 `sodium`，`oculus` / `NeOculus` 要配 `embeddium` 或 `rubidium`——
OBA 知道哪一支该配哪一支。

### 认得出经 Connector 加载的 Fabric 版

钠和 iris 都有 Fabric 版，也可能被 Sinytra Connector 加载到 NeoForge 上。
这种情况下 modId 仍然是 `sodium` / `iris`，类名也仍然在
`net.caffeinemc.`、`me.jellysquid.mods.sodium.`、`net.irisshaders.` 这些包下，
所以**谱系识别对 Fabric 版和 Forge 版一样有效**，不需要额外配置。

### 图形类崩溃的专项规则

这类问题的日志长得和普通报错很不一样，所以单独加了一组规则：

| 规则 | 触发 | 说明 |
|---|---|---|
| `graphics.shader_compile` | 着色器编译失败 | 光影包与当前 iris / 钠版本不匹配，或驱动拒绝了这段 GLSL |
| `graphics.shader_pack` | 光影包加载失败 | 包不完整、版本不对，或者用到了加载器没提供的选项 |
| `graphics.sampler` | 着色器缺少采样器 | 提示里会写出缺的是哪个 sampler |
| `graphics.framebuffer` | 帧缓冲不完整 | 接着往往就是黑屏、世界渲染不出来 |
| `graphics.mixin_conflict` | 模组之间在改对方的渲染类 | 画面异常和渲染线程崩溃的常见来源 |
| `graphics.unsupported_driver` | 驱动过旧 / 不支持 | 钠和 iris 需要比较新的 OpenGL 4.5 驱动 |
| `graphics.glfw_init` | GLFW 起不来 | 发生在模组加载之前，是驱动或启动器的问题 |
| `graphics.context_lost` | 图形上下文丢失 | 通常是 GPU 挂住导致驱动重置设备 |

### 附加组件不会被误判

`Sodium Extra`、动态光源这类附加组件和主体共存是正常的，
所以冲突判定只看**占主体位置的那两种角色**（渲染优化主体、光影加载器），
附加组件之间不算冲突。这条是实机校准出来的：拿一个装了
`embeddium` + `sodiumdynamiclights` 的真实整合包跑，
早期写法会把动态光源误报成「钠的另一个分支」。

另外 `rubidium-extra` 这个文件名下面声明的 modId 其实是 `embeddium_extra`，
所以每条分支还带一组别名，只认一个 id 会漏掉它。

---

## Sinytra Connector 适配

[Sinytra Connector](https://github.com/Sinytra/Connector) 让 Fabric 模组能跑在 NeoForge 上。
装了它之后，整合包里会同时存在三类东西：原生 NeoForge 模组、Fabric 模组、以及 Connector 自己。
OBA 针对这种混装做了四处适配。

**1. Fabric 模组会被正常识别**

Fabric 模组的身份写在 `fabric.mod.json` 而不是 `neoforge.mods.toml`。
OBA 两种都读，所以 Fabric 模组的名字、modId、版本都能拿到，
不会退化成一个光秃秃的文件名。

**2. Fabric 模组的栈帧不再被当成加载器代码丢掉**

`net.fabricmc.` 整个前缀原本都被划进「不是用户的代码」。但 Connector 场景下，
Fabric 模组自己的类（以及 Fabric API）就在这个前缀下，一棍子打死会让
Fabric 模组永远不出现在报告里。现在只排除加载器内部实现：

| 被排除 | 不排除（会参与归属） |
|---|---|
| `net.fabricmc.loader.*` | `net.fabricmc.fabric.api.*`（Fabric API，是模组） |
| `net.fabricmc.mappingio.*` | 模组自己的包，如 `me.jellysquid.mods.*` |
| `net.fabricmc.accesswidener.*` | |
| `net.fabricmc.tinyremapper.*` | |
| `org.sinytra.connector.locator/transformer/util.*` | `org.sinytra.connector.ConnectorMod` 自身 |

**3. 三条 Connector 专用规则**

| 规则 | 触发 | 建议 |
|---|---|---|
| `connector.failure` | Connector 报 remap / transform 失败 | 核对 Connector 与 Fabric API 版本，再看那个 Fabric 模组是否支持当前游戏版本 |
| `connector.missing_mod` | `Fabric mod xxx is not present` | 补装缺失的 Fabric 模组，或移除依赖它的那个 |
| `connector.intermediary` | 日志里出现 `class_1234` / `method_123` | intermediary 名称没被重映射，通常是模组没经过 Connector 处理或版本不对 |

**4. 报告里会注明环境**

索引里发现 Connector 时，报告会加一条说明，提示 Fabric 模组会以各自的 jar 出现、
类名可能经过重映射——免得看到一堆 `net.fabricmc` 以为是异常。

```bash
$ java -jar oba-1.0.1.jar mods --game-dir "D:\MC\.minecraft"
OBA 模组清单 — 6 个文件，26 个类
检测到 Sinytra Connector，报告里的 Fabric 模组会以各自的 jar 出现。……

文件                        modId            版本   类数  备注
──────────────────────────────────────────────────────────────────────
sodium-extra-0.6.0.jar      sodiumextra      0.6.0  3     Fabric模组（Connector）
connectormod-2.0.0.jar      connectormod     2.0.0  3
```

> 一点提醒：Fabric 模组经 Connector 重映射后，**类名可能是重映射后的名字**，
> 和作者源码里的名字对不上。行号一般是准的，类名对不上时按行号去搜更靠谱。

---

## 规则库

25 条针对真实日志文本的规则，另加 3 条 Connector 专用规则与 1 条人为触发规则，
命中后直接给建议。

| 类别 | 规则 |
|---|---|
| 依赖 | 缺少前置、缺少必需依赖、前置版本不匹配、模组互斥 |
| 重复加载 | 同一 modId 多份、`Found duplicate mod` |
| Mixin | 注入失败、配置加载失败、变换阶段异常、MixinExtras 版本过低 |
| 类加载 | `NoClassDefFoundError`、`ClassNotFoundException`、`NoSuchMethodError`、`NoSuchFieldError` |
| 注册表 | 缺少注册条目、注册冲突 |
| 数据 / 资源包 | 数据包加载失败、资源包加载失败 |
| 文件完整性 | 内嵌库缺失、jar 损坏 |
| 运行时 | 内存不足、服务器卡死、图形驱动问题、模组加载 / 实例化失败 |
| Connector | 变换失败、缺 Fabric 前置、intermediary 名称泄漏 |
| 人为触发 | F3+C 手动调试崩溃（明确告知不是故障） |

规则只在**文本形态明确**时触发。崩溃报告自身的结构性文字（`Description:`、`Time:`、
`-- Head --`、`Mod List:` 等）被显式排除，避免把「报告里描述了崩溃」当成「发生了一次崩溃」。

---

## 项目结构

本目录是**自包含的**：下面列的就是这个版本要编的全部源码，拷走单独构建也行。
引擎源码的权威副本在仓库根的 `engine/`，这里的 `core/` 是
`tools/sync-versions.ps1` 同步过来的副本 —— 改逻辑请改 `engine/`，然后重跑那个脚本。

```
1.20.1-forge/
│
├── README.md                        本文档（简体中文）
├── README.en.md                     English README
├── build.gradle                     构建脚本
├── settings.gradle                  插件仓库与根项目名
├── gradle.properties                版本号、模组元数据、兼容版本范围
├── gradlew                          Gradle 包装器（8.14.3）
├── gradlew.bat                      同上，Windows 用
│
├── gradle/wrapper/                 Gradle 包装器的引导文件
│
├── src/main/java/dev/oba/
│   ├── Launcher.java                唯一入口：判断是游戏内还是命令行，然后分派
│   │
│   ├── cli/
│   │   └── Cli.java                 命令行工具：参数解析、子命令、输出
│   │
│   ├── core/                          诊断引擎（与本版本共编的一份）
│   │   ├── Analyzer.java            编排：建索引、归属、打分、时间线、出报告
│   │   ├── ClassIndex.java          类名 / 包名 → 模组 的索引与解析
│   │   ├── Diagnosis.java           数据模型：Finding / KnownIssue / Report
│   │   ├── GraphicsMods.java        图形类模组谱系：钠 / iris 各分支与冲突判定
│   │   ├── Lang.java                中英文文案表
│   │   ├── LogAnalyzer.java         日志 / 崩溃报告解析 + 规则表
│   │   ├── LogWatcher.java          类 tail -f 的日志监听（处理轮转与编码）
│   │   ├── McreatorDetector.java    MCreator 多信号识别
│   │   ├── ModSource.java           单个模组文件的只读视图（jar 与目录两种形态）
│   │   ├── Oba.java                 常量：名字、版本号、modId、日志前缀
│   │   ├── ProcessProbe.java        线程转储解析 + 活体 JVM 采集
│   │   ├── Reports.java             文本 / Markdown / JSON 渲染
│   │   ├── StackTraceParser.java    堆栈解析（含 Caused by 链、模块前缀剥离）
│   │   ├── Theme.java               
│   │   ├── Timeline.java            事故时间线：事件抽取、时间戳与级别识别
│   │   └── Toml.java                极简 TOML 解析（读模组描述文件）
│   │
│   ├── loader/                        加载器适配层
│   │   ├── LoadedMod.java           加载器报出来的一条模组信息
│   │   ├── Loader.java              存放当前版本装进来的加载器实现
│   │   ├── LoaderEnv.java           加载器适配接口：列模组、取游戏目录
│   │   ├── TickHook.java            服务器 tick 的挂载点接口
│   │   └── forge/
│   │       ├── ForgeEnv.java          Forge 的加载器适配实现
│   │       └── ForgeTickHook.java     Forge 的 tick 挂载
│   │
│   └── mod/                           游戏内那一半
│       ├── ClientFont.java          
│       ├── ClientLanguage.java      把游戏语言同步给引擎（仅客户端）
│       ├── CrashReporter.java       把报告写进 logs 目录，自动清理旧报告，不覆盖强结论
│       ├── DiagnosticAppender.java  log4j2 采集器（实时抓 stack trace）
│       ├── Diagnostics.java         索引、日志窗口、报告缓存、去抖动调度
│       ├── EarlyLanguage.java       尽早从 options.txt 定下报告语言
│       ├── GameUi.java              
│       ├── ObaCommands.java         /oba 命令树
│       ├── ObaLifecycle.java        共享的生命周期：初始化、索引、看门狗、退出钩子
│       ├── ObaMod.java              模组入口：注册事件、启动索引与看门狗
│       ├── UiShot.java              
│       └── Watchdog.java            进程看门狗：tick 间隔 / 死锁 / 内存
│
├── src/main/resources/
│   ├── oba-icon.png                 模组图标（256×256，模组列表里显示的那张）
│   ├── oba-icon-128.png             同上，小尺寸一份
│   └── META-INF/
│       └── mods-1.20.1-forge.toml   模组描述文件（构建时展开占位符）
│           （源文件叫这个名，打包时落成 mods.toml）
│
└── docs/banner.svg               README 用的标题条
```

> 游戏外的命令行用法：本目录的 jar 同时是可执行的，`java -jar build/libs/*.jar help`。

## 构建

一次构建全部四个版本，产物收在 `dist/`：

```bash
pwsh tools/build-all.ps1
# -> dist/oba-mc1.21.1-1.0.1.jar
#    dist/oba-mc1.20.2-1.20.4-1.0.1.jar
#    dist/oba-mc1.20.5-1.20.6-1.0.1.jar
#    dist/oba-mc1.20.1-forge-1.0.1.jar
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

### 用 IntelliJ IDEA 构建

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
java -jar build/libs/oba-mc1.21.1-1.0.1.jar analyze --game-dir ../../examples
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

## 验证

| 验证项 | 方式 | 结果 |
|---|---|---|
| 引擎逻辑 | `pwsh tools/run-engine-tests.ps1` | **171 / 171 断言通过** |
| 引擎逻辑覆盖 | 索引、异常归属、异常链、Mixin 失败、依赖缺失、重复模组、线程转储、MCreator 识别、三种报告渲染、目录形态模组、多语言、Connector 混装、NeoForge 真实帧格式、日志编码、F3+C、带空格路径、事故时间线、**图形模组谱系与冲突** | 通过 |
| **运行时行为** | `engine/tools/RuntimeHarness.java`（真实 log4j2 + 真实 Mod 类） | **52 / 52 断言通过** |
| 运行时覆盖 | 日志采集、异常链还原、自动结论输出、报告落盘、F3+C 崩溃报告补捡、双语渲染、线程转储、只读保证 | 通过 |
| 单 jar 两种用法 | `java -jar ... version` / 模拟游戏环境 / `-Doba.mode=cli` | 三种路径都正确分派 |
| **真客户端实机** | 1.21.1 + NeoForge 21.1.256，故意放一个构造阶段就抛异常的模组 | 正确定位到 `CrasherMod.java:13`，报告自动落盘 |
| 真实整合包 | 236 个模组 / 52313 个类 / 1 GB | 1 秒扫完；查出重复安装的 coroutil，认出 5 个图形类模组所属的族 |
| 真实 GBK 日志 | 61380 行的中文整合包日志 | 正常读出，1128 个栈帧归属出 69 个 |
| 带空格路径 | `--game-dir "...我的 游戏..."`（加引号 / 不加引号） | 两种写法结果一致 |

### 运行时验证怎么跑

`RuntimeHarness` 用**真实 log4j2 后端**驱动**真实 Mod 类**，覆盖游戏内那一半的核心链路：

1. 通过 log4j2 写一条带 `Caused by` 链的 ERROR，确认采集器收到时异常类型、消息、
   每个栈帧的行号、cause 链都完好；
2. 调用 Mod 自己的 `Diagnostics.analyzeWindow()`，确认去抖动后**自动输出**的结论里
   点名了正确的模组、给出了行号、并声明了只读立场；
3. 调 `CrashReporter`，确认报告真的写进了 `logs` 目录、额外留了一份 `oba-latest.txt`，
   并能捡起本次运行新产生的 F3+C 崩溃报告；
4. 对输入目录做全流程前后快照，断言**每个模组文件 SHA-1 不变**——
   把「只读」从口号变成可验证的事实。

```bash
# 1) 取真实依赖的 classpath（离线可跑）
./gradlew -I engine/tools/cp-init.gradle printClasspaths --offline -q
#    把 COMPILE_CP= 那一行存下来

# 2) 编译并运行
javac -encoding UTF-8 -proc:none -cp "$COMPILE_CP" -d build-rt \
  engine/main/java/dev/oba/core/*.java \
  src/main/java/dev/oba/*.java \
  src/main/java/dev/oba/mod/*.java \
  engine/tools/RuntimeHarness.java
java -Dstdout.encoding=UTF-8 -cp "build-rt:$COMPILE_CP" dev.oba.tools.RuntimeHarness

# 3) 换个语言再跑一遍
java -Doba.lang=en -cp "build-rt:$COMPILE_CP" dev.oba.tools.RuntimeHarness
```

### 这套验证揪出过什么

不是走形式——它找出的缺陷**编译期完全看不出来，但会让功能整体失效**：

| 缺陷 | 症状 | 根因 |
|---|---|---|
| 日志采集器从不触发 | `install()` 返回成功，appender 也在 root logger 的表里，但事件永远不到达 | 只调 `Configuration.addAppender` 只是登记，log4j2 按 appender **引用**派发；而 `getAppenderRefs()` 是过期快照 |
| 任何错误都不触发诊断 | 日志进了窗口，但永不产出结论 | log4j2 的 `Level.isInRange(min, max)` 是**最小严重度**语义且 int 值反序（FATAL=100 < ERROR=200），`isInRange(ERROR, FATAL)` 是空区间 |
| 英文报告里混中文 | 换语言后正文仍是中文 | 证据链把「已渲染的结论标题」当成参数存了下来，渲染时机早于语言切换 |
| Connector 场景下 Fabric 模组永不被点名 | 索引里有类，但没有 modId，规则命中时关联不上 | Fabric 模组的身份在 `fabric.mod.json`，当时只读了 `neoforge.mods.toml` |

### 实机验证（1.21.1 + NeoForge 21.1.256 真客户端）

真客户端和真实整合包跑过之后又揪出六个，都只在真实环境里才暴露：

| 缺陷 | 症状 | 根因 |
|---|---|---|
| 模组在构造阶段崩就什么都采集不到 | 游戏日志里有完整异常，OBA 却一声不吭 | 采集器挂在 `FMLCommonSetupEvent` 里，而该事件要等**所有**模组构造完才触发 |
| 报告写「没有栈帧能归属到某个模组」 | 明明异常就摆在那儿，一个模组都点不出来 | NeoForge 给栈帧加了 `~[模块@版本/...]` 尾巴，类名前还带 `TRANSFORMER/crasher@1.0.0/` 这种两层前缀，正则和索引都对不上 |
| 一份报告滚成十几份 | 每 2~3 秒生成一份，内容越来越长 | 自己的结论被文件监听读回来，正文里的 `java.lang.XxxException` 看着就是异常开头，形成正反馈 |
| 中文整合包的日志读不了 | `读取失败: Input length = 1` | 日志是 GBK，代码写死按 UTF-8 读 |
| MCreator 误报 98 个 | 236 个模组的包里报了 98 个 | `*Model`、`ModItems` 这类普通 Java 命名被当成 MCreator 特征 |
| 自己指控自己 + 卡死误报 | 退出世界存区块被当成卡死，元凶报的是 OBA 自己 | 看门狗把原版重活当卡死，线程选择又选中了自己的监听线程 |

### 另一台设备的反馈

从另一台机器导出了崩溃信息包，又暴露三个问题：

| 缺陷 | 症状 | 根因 |
|---|---|---|
| 路径里有空格就失败 | 加引号会被当成路径的一部分，报 `Illegal char <">` | 命令行不会替程序剥引号 |
| F3+C 崩溃完全看不到 | 报告、结论、日志里都没有任何痕迹 | 手动调试崩溃不经过 log4j2，只有 `crash-reports` 下落一个文件 |
| 报告刷屏 | `/oba analyze` 的输出被反复分析 | 聊天栏渲染的行只有 `[CHAT]`，没有日志前缀，绕过了「忽略自己输出」的过滤 |

### 游戏内界面怎么验（不用手敲键盘）

游戏内那套彩色菜单没法靠模拟按键验：这台机器上 `PostMessage` 塞 `WM_KEYDOWN`、
`keybd_event` 造真按键、`AttachThreadInput` 抢焦点三条路都进不了 Minecraft 窗口 ——
`GetForegroundWindow()` 回报的就是游戏窗口，但连 Esc 都弹不出暂停菜单。于是改成让模组自己跑：

```powershell
# 截 /oba help 的完整菜单
pwsh tools/ui-shot.ps1 -VersionDir "D:\hmcl\.minecraft\versions\test" -World "新的世界" `
  -Out build-v\ui-ingame.png -NoKeys -JavaExe "C:\Program Files\Zulu\zulu-21\bin\java.exe"

# 想连标题框一起看见，就跑条输出特别短的：聊天栏只露得出最后十来行，
# 菜单二十来行，顶上的标题框会被切掉（/oba mcreator 没有结果时只有五行，刚好全露）
pwsh tools/ui-shot.ps1 -VersionDir "D:\hmcl\.minecraft\versions\test" -World "新的世界" `
  -Command "/oba mcreator" -Out build-v\ui-ingame.png -NoKeys -JavaExe "C:\Program Files\Zulu\zulu-21\bin\java.exe"
```

- `-NoKeys` 会带上 `-Doba.ui.shot=<命令>`（`-Command` 给什么就跑什么，默认 `oba help`），
  模组进世界后自己执行；
- 截图取的是**游戏自己的帧缓冲**（`Screenshot.grab`，等同按 F2）—— 被别的窗口盖住的 GPU 窗口
  抓屏只能拿到过期画面，聊天栏在窗口截图里永远看不见；
- 跑之前临时把实例的 `pauseOnLostFocus` 改成 `false`、`chatHeightUnfocused` 改成 `1.0`（跑完还原）；
- 聊天栏能露多少行跟 `guiScale` 没关系（3 / 2 / 1 都试过，一直都是十九行上下），别去动玩家的界面缩放；
- 顺手核对排版：`UiShot` 会把「宽度表 vs 客户端真字体」逐字比对、以及标题框三行是否等像素宽
  写进日志（`[OBA] 宽度核对：…` / `[OBA] 标题框三行像素宽：…`），宽度表里 `╭╮╯╰◈`
  几个值就是这么被纠正的。

「点一下就能执行」由自测护着：菜单行按整棵组件树展开，断言**每一个**节点都挂着
`RUN_COMMAND` 点击事件（链接行是 `OPEN_URL`，带参数行是 `SUGGEST_COMMAND`）。

### 还没验证的部分


**1.21.x 的其余小版本**（1.21.2 / 1.21.3 / 1.21.4 / 1.21.5 …）没有实机验证过。

---

## 已知限制

**能做到**

- 把堆栈帧精确归属到模组文件，给出类名 + 方法名 + 行号
- 整理出崩溃前后的**事故时间线**，标出每个模组当时在做什么
- 识别 25 类文本形态明确的报错，另加 Connector 与人为触发规则
- 无异常时通过线程转储指出「卡住时谁在跑」
- 识别 MCreator 生成特征并给出理由
- 装了 Sinytra Connector 时，Fabric 模组照样能参与归属
- 日志按内容猜编码，UTF-8 和 GBK 都能读
- 路径带空格也能用，加不加引号都行
- 中英双语，全程只读

**做不到 / 不保证**

- **混淆过的模组**：行号通常还在，但类名可能无法归属
- **Fabric 模组的类名可能对不上源码**：重映射后按行号去找更可靠
- **不在 mods 目录里的代码**：Java 代理、coremod、启动器注入的代码不在索引范围内，
  会出现在报告的「无法归属的包」一节
- **多人服务器上的客户端崩溃**：需要把客户端日志或崩溃报告拿过来分析
- **「插件」**：NeoForge 没有 Bukkit / Spigot 那种独立插件体系，本项目按**模组**处理
- **归属不等于定罪**：最可疑的模组不一定是真正的 bug 源头，
  A 可能只是因为 B 先破坏了共享状态才失败。报告给出的是证据链，最终判断在你
- **卡死分析是推断**：栈顶的模组是最该先测的，不是死循环的证明

---

## 常见问题

**Q：它会自动禁用出问题的模组吗？**

不会，永远不会。这是设计上的硬约束：不写入、不移动、不重命名、不删除任何模组文件。
建议里提到「把某个 jar 移出去试试」时，紧接着就会说明这个操作需要你自己执行。

**Q：报告会不会占满我的日志目录？**

不会，只保留最近 10 份，超出的按时间从旧到新删除。另外 `oba-latest.txt`
固定指向最新一份，方便脚本或直接打开。

**Q：为什么我的模组没被点名？**

先看报告里的「无法归属的包」一节。如果崩溃栈帧全部来自原版和加载器，那就是真的没有模组可归——
比如 F3+C 手动触发的调试崩溃。如果是包名匹配而非类名匹配，可信度会打折，也要留意。

**Q：装了它会不会拖慢游戏？**

索引只在启动时建一次（236 个模组实测约 1 秒），之后是常数开销。
日志采集器只做字符串判断，看门狗每 15 秒才醒一次。

**Q：怎么把报告发给别人？**

`logs/oba-latest.txt` 直接发就行，里面包含元凶、行号、SHA-1 和完整证据链。
要贴到论坛或 issue 里，用 `--format md` 生成 Markdown 会更整齐。

**Q：能分析别的机器上的日志吗？**

能。把对方的 `logs/` 和 `mods/` 目录拿过来，用
`java -jar oba.jar analyze --game-dir <对方目录>` 即可，不需要启动游戏。

---

## 参与贡献

欢迎提 issue 和 PR。为了让问题好定位，报 bug 时请附上：

1. `logs/oba-latest.txt`（如果已经生成了报告）
2. 出问题的原始日志或崩溃报告
3. 游戏版本、NeoForge 版本、OBA 版本
4. 如果是「OBA 自己报错」，附上日志里 `[OBA]` 开头的那几行


---

## 致谢

- **特别鸣谢**[XinKo_TS](https://space.bilibili.com/1555260755?)**对本模组的支持与维护**
- [McreatorDetector-1192](https://github.com/yiyuyan/McreatorDetector-1192) —— MCreator 识别思路的参考实现
- [Sinytra Connector](https://github.com/Sinytra/Connector) —— Fabric 模组在 NeoForge 上的桥接方案
- [NeoForged](https://neoforged.net/) 与 [MinecraftForge](https://github.com/MinecraftForge) —— 加载器与事件体系
- 提供真实整合包与崩溃日志反馈的测试者
- Deepseek进行多语言翻译
---

## 许可协议

本项目以 **GNU 通用公共许可证第 3 版（GPL-3.0）** 发布，全文见 [LICENSE](LICENSE)。

```
One BUG Among Us (OBA)
Copyright (C) 2026 ModDoctor contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 3 of the License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
```

简单说：你可以自由使用、修改、分发，但**分发修改后的版本时必须同样以 GPL-3.0 开源并保留版权声明**；
作者不对使用后果负责。和 MIT 最大的区别就在这里——改过的版本不能再闭源。

---

<!-- ============================================================ -->
<!-- 以下为预留板块。结构与标题已固定，正文待补充。               -->
<!-- ============================================================ -->

## 附录 A：待补充社区内容

> **📌 本板块为预留位，内容待补充。**

---

## 附录 B：术语表

| 术语 | 含义 |
|---|---|
| **元凶 / 元凶模组** | 证据权重最高的那个模组文件。是「最可疑」而非「已定罪」 |
| **归属** | 把一条栈帧或一行日志对应回它所属的模组文件 |
| **证据链** | 支撑某个结论的全部加分理由，逐条列出权重 |
| **可信度** | 证据权重折算出的区间（高 / 极高），不是概率 |
| **根因异常** | `Caused by` 链最里层那个异常，通常才是真正出问题的地方 |
| **加载器帧** | 属于 FML、ModLauncher、Mixin 等基础设施的栈帧，不参与模组归属 |
| **事故时间线** | 崩溃发生时及之前一段时间内，各模组行为按时间排列的视图 |
| **噪音下限** | 低于此权重的怀疑对象会被丢弃，避免报告被无关模组淹没 |
| **F3+C** | Minecraft 内置的手动调试崩溃快捷键，用于主动生成一份崩溃报告 |

---

<div align="center">

**One BUG Among Us** · 只读诊断 · 不碰你的任何文件

[GPL-3.0 License](LICENSE) · [English README](README.en.md)

</div>
