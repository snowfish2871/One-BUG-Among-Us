# One BUG Among Us   一个BUG隐藏在我们当中

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

**声明：由于该MOD极高的优先级，请不要将其应用于您重要的或者正在运营的服务器或者存档，造成的任何问题OBA contributors不会承担！使用则代表您同意并接受可能的潜在的不稳定风险。**

**面向 Minecraft 1.20.1 ~ 1.21.x 的模组崩溃诊断工具**（Forge 与 NeoForge 都支持）

**他的使命就是解决问题，当问题解决时他的任务就完成了，也就没有任何留着mods文件夹的必要了**

游戏崩了，日志里几千行堆栈看不出是谁干的。OBA 把这件事收敛成一句能照着做的结论：
**哪个 jar、哪个类的哪一行、以及为什么怀疑它。**

[English](README.en.md) | 简体中文

> **本项目只做诊断，绝不改动任何文件。**
> 不会禁用、移动、重命名或删除任何一个模组。所有结论只是打印出来并写成报告，
> 是否采取行动由你决定。

> **这是一份总 README**，讲整个项目怎么组织、怎么用、怎么构建。
> 单个 Minecraft 版本的源码、构建配置和它自己的 README 在**各自的分支**里，
> 分支名与版本目录同名，见 [仓库与分支](#仓库与分支)。

<img src="engine/main/resources/oba-icon.png" alt="图标" width="128" align="right">



## 目录

- [它解决什么问题](#它解决什么问题)
- [能做什么](#能做什么)
- [仓库与分支](#仓库与分支)
- [兼容性](#兼容性)
- [快速开始](#快速开始)
- [界面](#界面)
- [事故时间线](#事故时间线)
- [诊断报告](#诊断报告)
- [使用方式](#使用方式)
- [实现原理](#实现原理)
- [定位原理](#定位原理)
- [MCreator 识别](#mcreator-识别)
- [图形类模组专项](#图形类模组专项)
- [Sinytra Connector 适配](#sinytra-connector-适配)
- [规则库](#规则库)
- [项目结构](#项目结构)
- [构建](#构建)
- [验证](#验证)
- [已知限制](#已知限制)
- [常见问题](#常见问题)
- [变更记录](#变更记录)
- [参与贡献](#参与贡献)
- [致谢](#致谢)
- [许可协议](#许可协议)
- [附录 A：待补充社区内容](#附录-a待补充社区内容)
- [附录 B：术语表](#附录-b术语表)

---

## 它解决什么问题

游戏因为模组报错时，日志里通常只有一大段堆栈，看完仍然不知道是谁的问题。
OBA 把这些信息整理成一句能直接行动的结论：**哪个文件、哪一行、为什么**。

| 你遇到的情况 | OBA 给出的答案 |
|---|---|
| 游戏崩溃，只有一堆堆栈 | 元凶模组文件 + 类 / 方法 / 行号 + 完整的证据权重 |
| 日志写着 `NoClassDefFoundError` | 缺的是哪个类、本该由哪个模组提供、是不是前置没装 |
| 卡死、无响应，但没有崩溃报告 | 采集线程转储，指出卡住时**正在执行哪个模组** |
| 整合包几百个模组，不知道从哪查 | 可疑度排序，每个模组附上它被怀疑的具体理由 |
| `Mixin apply failed` | 定位到具体 mixin 类和行号，提示冲突或版本不匹配 |
| 同一个模组装了两份 | 列出重复的 modId 及其全部文件 |
| 想筛出 MCreator 做的模组 | 多重特征评分，逐条列出判定理由 |
| Fabric 模组经 Connector 跑在 NeoForge 上崩了 | 照样归到那个 Fabric 模组的 jar |
| 按 F3+C 触发了调试崩溃 | 明确告诉你这是人为触发，不用找元凶 |

---

## 能做什么

一句话：把「游戏崩了，不知道是谁的锅」变成「就是这个 jar 的这一行」。

崩溃发生时，OBA 给出元凶模组文件、类名、方法名、源文件行号，以及完整的证据链 ——
哪条日志、命中哪条规则、加了多少分，都列在结论下面。你觉得结论不对，就顺着往回查，
能看出是哪一步推错了。崩溃前后一段时间里各个模组分别做了什么，会按时间排成一条线；
卡死没有崩溃报告时，它采集线程转储，指出卡住的那一刻正在执行哪个模组。

装了两份同一个模组、前置没装、Mixin 冲突、MCreator 生成的模组、Fabric 模组经
Connector 跑在 NeoForge 上崩掉 —— 这些各有各的判断依据，不靠猜。图形类模组还认得出
钠 / iris 那几十个换名分支，能查出「同一族装了两个互斥分支」和「光影加载器缺了渲染主体」。

用起来只有两种形态，还是同一个 jar：丢进 `mods/` 就是模组，`java -jar` 就是命令行工具。
报告、命令行输出、游戏内提示都支持中英文切换。全程只读，不写、不改、不删任何文件，
报告也只是在 `logs` 下新增一个文本文件。

---

## 仓库与分支

这个仓库不是「一份源码 + 一堆构建配置」，而是**一份引擎 + 多条版本轨道**。

- **你正在看的是默认分支**（总览）：项目文档、共享引擎的权威副本 `engine/`、
  加载器适配实现 `src/loader/`、构建与验证脚本 `tools/`、故意做坏的样例 `examples/`。
  在这一份上可以一次把七个版本的 jar 全构建出来。
- **每个 Minecraft 版本区间一条分支**，有它自己的 `src/main`、`build.gradle`
  和 README，切过去就能单独构建、单独改，不用管别的版本。

| 分支 | 覆盖的 Minecraft | 加载器 |
|---|---|---|
| `1.21.1-neoforge` | 1.21 ~ 1.21.x | NeoForge 21.1+ |
| `1.21.1-forge` | 1.21.1 | Forge 52 |
| `1.20.5-1.20.6-neoforge` | 1.20.5 / 1.20.6 | NeoForge 20.5 / 20.6 |
| `1.20.5-1.20.6-forge` | 1.20.5 / 1.20.6 | Forge 50 |
| `1.20.2-1.20.4-neoforge` | 1.20.2 / 1.20.3 / 1.20.4 | NeoForge 20.2 / 20.3 / 20.4 |
| `1.20.2-1.20.4-forge` | 1.20.2 / 1.20.3 / 1.20.4 | Forge 49 |
| `1.20.1-forge` | 1.20.1 | Forge 47 |

**想看某个版本的源码，就切到对应分支**：

```bash
# 只关心一个版本，克隆时就指定分支
git clone -b 1.20.1-forge https://github.com/snowfish2871/One-BUG-Among-Us.git

# 已经克隆过了
git switch 1.21.1-neoforge
```

要改的是**诊断逻辑本身**（判据、打分、报告排版），改默认分支上的 `engine/`，
再跑 `tools/sync-versions.ps1` 把引擎同步进七条版本轨道 —— 版本目录里的
`src/main/java/dev/oba/` 是同步出来的副本，直接手改会在下次同步时被覆盖。
要改的是**加载器相关**的东西（`@Mod` 入口、事件注册、tick 适配），
改 `src/loader/neoforge/` 或 `src/loader/forge/`。


---

## 兼容性

按**版本轨道**发布：一条轨道覆盖「一套 API」下的所有 Minecraft 小版本，
同一轨道内模组代码不需要任何改动就能跑。这么分是因为 NeoForge 在这些地方改过 API，
强行塞进一个 jar 反而会互相拖累。

| 轨道 | Minecraft | 加载器 | Java | 编译目标 | 状态 |
|---|---|---|---|---|---|
| **1.21.x** | 1.21 ~ 1.21.x | NeoForge 21.1+ | 21 | `1.21.1` / `21.1.256` | 1.1.0 已实机验证 |
| **1.21.1** | 1.21.1 | **Forge** 52 线 | 21 | `1.21.1` / `52.1.20` | 已出包，客户端与服务端都验证过 |
| **1.20.5 ~ 1.20.6** | 1.20.5 / 1.20.6 | NeoForge 20.5 / 20.6 线 | 21 | `1.20.5` / `20.5.21-beta` | 1.1.0 已实机验证 |
| **1.20.5 ~ 1.20.6** | 1.20.6 | **Forge** 50 线 | 21 | `1.20.6` / `50.2.14` | 已出包，客户端与服务端都验证过 |
| **1.20.2 ~ 1.20.4** | 1.20.2 / 1.20.3 / 1.20.4 | NeoForge 20.2 / 20.3 / 20.4 线 | 17 | `1.20.2` / `20.2.93` | 1.1.0 已实机验证 |
| **1.20.2 ~ 1.20.4** | 1.20.2 / 1.20.3 / 1.20.4 | **Forge** 49 线 | 17 | `1.20.4` / `49.2.13` | 已出包，客户端与服务端都验证过 |
| **1.20.1** | 1.20.1 | **Forge** 47 线 | 17 | `1.20.1` / `47.4.26` | 1.1.0 已实机验证 |

> **1.20.1 走的是 Forge，不是 NeoForge。** 那会儿 NeoForge 还没独立出来 ——
> 加载器还叫 MinecraftForge，包名是 `net.minecraftforge.*`，和现在的
> `net.neoforged.*` 是两套完全不同的东西。所以 1.20.1 单独有一条 Forge 轨道。
> **1.20 不在支持范围内**：那是最早的 1.20，两边都还没就位。

> 表里的「1.1.0 已实机验证」是拿真实整合包跑出来的：七个客户端各装 10~18 个模组文件
> （按 Modrinth 的版本清单挑，前置依赖递归补齐），都能进主菜单，索引落在
> 1802~4958 个类、383~1150 个包、68~186 ms。加载器和 Minecraft 自己的 jar 不算模组，
> 不进索引：早先把它们算进去会得出「1 万多个类」这种虚高的数字，归因还可能指着 neoforge 自己。

> 1.20.3 上有两个坑值得记一笔：装 1.20.4 编译的模组会让整个 mods 目录被判 invalid
> （20.2~20.3 的 `[[dependencies]]` 还要求 `mandatory` 字段），以及这台机器的 USB 声卡
> 要 `ALSOFT_DRIVERS=null` 才过得了音频初始化。两件事都不影响结论，但会让人以为「模组没加载」。

> 三条 1.20.2 以上的 Forge 轨道**这一版都出了包**，其中两条已经在专用服务端上跑过。
> 它们卡了很久，最后查明跟镜像无关：这台机器的浏览器走 `127.0.0.1:7897` 的系统代理，
> 而 Java 默认直连 —— 直连 `maven.minecraftforge.net` 只有 4 KB/s 左右，同一个 userdev
> 文件走代理 2.7 秒下完。在 `gradle.properties` 里补上 `systemProp.https.proxyHost` /
> `proxyPort` 之后，三条轨道分别用 2m38s、108s、73s 建出了 jar。
>
> 顺手修了两处构建脚本：Forge 1.20.5 起官方取消了 reobf，`reobfJar` 任务不再存在，
> 两条轨道的 `build.gradle` 改成按任务是否存在决定要不要挂 `finalizedBy`；1.20.2 的加载器
> 是 48 线，`loaderVersion` 从 `[49,)` 放宽到 `[48,)`，1.21.1 的 Minecraft 区间从
> `[1.21,1.22)` 收成 `[1.21.1,1.21.2)`。另外 Forge 官方没有 1.20.5 的构建
> （metadata 里 1.20.5 线是 0 个版本），所以 `1.20.5-1.20.6-forge` 实际只覆盖 1.20.6。

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

### 1.20.1（Forge）轨道

1.20.1 那条轨道单独用 ForgeGradle 构建，走官方工具链，jar 里多了 reobf 那一步，
所以它比另外三条慢，也必须用 JDK 17。产物和别的轨道一样落在 `dist/` 下。

> 构建踩过的坑记在这里，换台机器遇到同样的卡顿能对上：ForgeGradle 首次运行要下载并
> 反混淆整个 1.20.1，而 `maven.neoforged.net` 这台在本机网络下不通。另外三条轨道靠
> 改写仓库地址绕过了，ForgeGradle 自己那套 `MavenArtifactDownloader` 绕不过去，
> 表现是长时间停在 `DownloadUtils.downloadFile` 上，CPU 累计时间不涨、缓存也不长。
> 换成官方 CDN 镜像之后正常出 jar。

**为什么 1.20.1 不能直接套 1.20.2 那套**：NeoForge 三条轨道能统一构建，是因为 1.20.2 起
加载器包名统一成了 `net.neoforged.*`；1.20.1 的 Forge 跑的是 SRG 命名
（`net.minecraft.commands.Commands.m_82127_`），而客户端 jar 本身是混淆的（`dt.a`），
中间必须过一整条反混淆流水线，所以只能走 ForgeGradle。

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
| 1.20.1 + Forge 47.4.26 | **真客户端实机验证过** |
| 各轨道内部的其余小版本 | 依据「同一轨道 API 一致」推断可用，未逐一验证 |

如果你在某个版本上跑出了 OBA 自己的报错，日志里会出现
`[OBA] OBA 在这个版本上没能启动`、`[OBA] xxx 注册失败` 或
`[OBA] 在这个版本上没能挂上服务器 tick 事件`，
把那一行连同版本号提个 issue 即可。

### 实机验证结果

四个轨道都在真客户端上跑过：往 `mods` 里放一个**故意在构造阶段抛异常**的测试模组，
启动客户端，看 OBA 能不能自己把元凶点出来、并把报告落到 `logs` 下。

| 轨道 | 客户端版本 | 结果 |
|---|---|---|
| 1.21.x | 1.21.1 + NeoForge 21.1.256 | **定位到 CrasherMod.\<init\> (CrasherMod.java:15)**，报告落盘 |
| 1.20.5 ~ 1.20.6 | 1.20.5 + NeoForge 20.5.21-beta | 同上 |
| 1.20.2 ~ 1.20.4 | 1.20.2 + NeoForge 20.2.93 | 同上 |
| 1.20.1 | 1.20.1 + Forge 47.4.26 | 同上，判定依据是「精确匹配类名」 |

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

### 关于 1.20

**1.20（这里指最早的 1.20，不含 1.20.1）不在支持范围内。** NeoForge 从 **1.20.2**
才独立发布，1.20 那会儿两边都还没就位，没有可用的加载器组合。把 NeoForge 版的 OBA
放进 1.20.1 的 Forge 实例，加载器会直接拒绝：

```
Mod ID: 'neoforge', Requested by: 'oba', Expected range: '[20.2.0,20.5)', Actual version: '[MISSING]'
net.minecraftforge.fml.ModLoadingException: Mod 'oba' requires 'neoforge' '20.2.0 or above'
```

那个实例的库是 `net.minecraftforge:eventbus:6.0.5`、`net.minecraftforge:forgespi:7.0.1`、
`net.neoforged.fancymodloader:loader:47.2.2` —— 也就是说它是 **MinecraftForge 47.2.2**，
包名是 `net.minecraftforge.*`。1.20.1 因此单独有一条 Forge 轨道，产物是另一个 jar，
两条线互不干扰。

### 为什么能跨小版本

模组只依赖**跨版本稳定的那部分 API**：日志后端（log4j2）、模组生命周期事件、
事件总线、命令注册、以及聊天栏文本。它不碰渲染、不碰世界数据、不碰注册表内容，
所以不会随小版本迭代而失效。唯一踩到版本差异的就是 tick 事件，已经用适配层隔离掉。

同时整个初始化都做了故障隔离：采集器安装、类索引、事件注册、命令树、tick 适配
各自独立成一段，任何一段因为版本差异抛异常，都只会让**那一个功能**停用，
并在日志里留下一句说明。OBA 是个诊断工具，它自己绝不该成为游戏起不来的原因。

### 加一条新轨道

NeoForge 出了新版 Minecraft 时，照下面三步就能加一条：

1. 复制 `versions/1.20.5-1.20.6-neoforge/` 成 `versions/<新版本>-neoforge/`；
2. 改它 `gradle.properties` 里的 `minecraft_version`、`minecraft_version_range`、
   `neo_version`、`neo_version_range`、`archive_suffix` 和 `parchment_*`；
3. 把新目录名加进 `tools/sync-versions.ps1` 与 `tools/build-all.ps1` 的列表，
   跑一次同步 + 构建。

共享代码由 `tools/sync-versions.ps1` 从 `engine/` 同步进各版本目录，**不需要手抄**，
所以新轨道自动跟上引擎的所有功能改动。如果新版本又动了 API，
照 `src/loader/` 下那两套 `TickHook` 实现的样子加一层运行时适配即可。


---

## 快速开始

### 游戏内

1. 把 `oba-mc1.21.1-1.1.0.jar` 放进 `.minecraft/mods/`
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
java -jar oba-mc1.21.1-1.1.0.jar analyze

# 指定游戏目录（路径带空格时，加不加引号都能识别）
java -jar oba-mc1.21.1-1.1.0.jar analyze --game-dir "D:\我的 游戏\.minecraft"

# 直接喂一个崩溃报告
java -jar oba-mc1.21.1-1.1.0.jar analyze crash-reports/crash-2026-10-09_18.03.20-client.txt

# 列模组 + 查重复 + 查 MCreator + 标出 Fabric 模组
java -jar oba-mc1.21.1-1.1.0.jar mods --game-dir "D:\MC\.minecraft"

# 实时监听，一边玩一边报
java -jar oba-mc1.21.1-1.1.0.jar watch "D:\MC\.minecraft\logs\latest.log"
```

### 试用自带样例

`examples/` 是一个**故意做坏的**迷你游戏目录：六个假模组、三份崩溃报告、一份缺前置的日志。

```bash
# NeoForge 模组崩溃
java -jar oba-mc1.21.1-1.1.0.jar analyze examples/crash-reports/crash-2026-10-09_18.03.20-client.txt

# Fabric 模组经 Connector 崩溃
java -jar oba-mc1.21.1-1.1.0.jar analyze examples/crash-reports/crash-2026-10-10_11.35.02-client.txt

# F3+C 手动调试崩溃
java -jar oba-mc1.21.1-1.1.0.jar analyze examples/crash-reports/crash-2026-10-10_16.47.12-client.txt

java -jar oba-mc1.21.1-1.1.0.jar mods     --game-dir examples
java -jar oba-mc1.21.1-1.1.0.jar mcreator --game-dir examples
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
java -jar oba-mc1.21.1-1.1.0.jar
```

```
╔════════════════════════════════════════════════════════════════╗
║  ◈ One BUG Among Us CLI                             OBA v1.1.0 ║
╚════════════════════════════════════════════════════════════════╝

  工作目录    D:\hmcl\.minecraft
  状态        ● 就绪
  构建        One BUG Among Us 1.1.0

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
║  ◈ One BUG Among Us                              OBA v1.1.0 ║
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
  https://github.com/snowfish2871/One-BUG-Among-Us
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
OBA v1.1.0
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
java -jar oba-mc1.21.1-1.1.0.jar analyze --lang en --game-dir examples
OBA_LANG=en java -jar oba-mc1.21.1-1.1.0.jar mods --game-dir examples
```

所有文案集中在一张表里（`engine/main/java/dev/oba/core/Lang.java`），以 `namespace.key` 形式索引。
报告文本存的是**文案键加参数**而不是成品句子，渲染时才解析，
因此已缓存的报告在切换语言后依然能正确重渲。

> 引擎没有使用 Minecraft 自带的 `assets/<modid>/lang/*.json`：
> 命令行工具根本不加载游戏，共用一张表比维护两套更可靠。
> 新增语言的方法是加一个常量、扩展 `detect()` 分支，并给每条 `put(...)` 补一列；
> 任何语言缺键，自测台会直接失败。

---

## 实现原理

一句话：把「游戏说了什么」「进程当时在干什么」「这些类分别装在哪个 jar 里」三份材料对齐，
再用规则和打分从里面挑出最可能的那一个。

### 三份材料怎么进到同一个地方

- **日志**。启动时定位 `logs/latest.log`，从第一行开始跟着读，同时注册一个进程内的日志回调。
  两条路的行都先进同一个入口，打一遍同一个正则，拿到时间、线程名、级别、logger、正文。
  之后「是不是错误」只看归一化后的文本，不看这条消息是不是恰好从 `[Render thread/WARN]` 来的。
- **进程**。守护线程每 250 ms 记一次所有线程的名字、状态和栈顶帧，只保留有变化的快照，
  既给死锁检测用，也给「事故时间线」用。
- **模组**。`mods/` 目录扫一遍，读每个 jar 的描述文件和类列表，建「类名 → jar」的倒排索引。
  索引和模组目录的指纹（每个 jar 的名字、大小、修改时间）绑在一起，指纹没变就不重建。

### 从一条错误到一条结论

1. **归一化**。去掉时间戳、内存地址、线程号这些每次都变的部分，剩下的当去重键。
   同一行刷一百遍也只算一次。
2. **认规则**。一条一条正则往下试，命中的按「词的强度」排队：`connector.intermediary`
   这种点到具体加载器的排最前，泛泛的 `class.noclassdef` 排后面。每行日志只产出一条结论，
   否则一次崩溃会在清单里刷出七八条同源条目。
   库命名空间里的类缺失不算结论：`org.jetbrains`、`kotlin`、`com.google`、`org.slf4j`
   这些前缀的类找不到，多半是模组把可选集成当软依赖，报出来只是噪声；模组自己的包名缺失照报。
3. **找元凶**。堆栈里的每一帧拿去索引里反查，命中哪个 jar 就得一票；帧越靠上、越具体，票越重。
   反查不到归属的类名单独列出来，提示是哪一族缺失。
4. **打分合并**。同一条规则、同一个模组的多条命中并成一条，票数加总，再和旁证
   （这个模组是不是已经明确坏过、是不是 MCreator 生成、是不是经 Connector 加载的）相乘。
5. **落地**。结论算完一次性交给渲染层。终端按字符列排，游戏内按聊天栏的像素宽度重排。
   同一份数据，两种排版。

### 为什么能跨版本

诊断逻辑全在 `engine/` 里，不碰任何加载器 API：它读的是日志文本、线程栈、jar 文件、
堆栈字符串。和具体版本绑定的只有薄薄一层：加载器入口（`@Mod`、事件总线、tick 回调）、
崩溃钩子与「当前是不是客户端」的判断、客户端专有的字体度量和聊天栏宽度。

换一个大版本，改的是这一层，引擎照旧。`tools/sync-versions.ps1` 把 `engine/` 原样复制进
每条轨道，就是为了让「改一处、七条轨道一起生效」这件事只有一种做法。

### 启动为什么快

启动时最贵的是建索引：扫 jar、读类名，一遍就是几万个字符串。规矩是四条：

1. **不挡启动**。索引在后台跑，主线程先往下走；跑完之前如果真崩了，结论先挂着，
   索引一就绪立刻补算。
2. **不重复跑**。模组目录的指纹没变就直接返回，不扫第二遍。
3. **并行扫**。jar 之间互不相干，按 CPU 核数开 2~8 个线程并行读，结果按原顺序收回来。
4. **不在日志线程上算**。分析请求只往自己的单线程队列里排，log4j 那条写日志的线程放下这一行就继续走。
   这条是实机教出来的：原来分析直接挂在日志回调里，一行超长日志让某条正则回溯了几十秒，
   整个游戏的日志写盘被堵住，客户端看着就跟卡死一样。现在一批日志最多看每行前 4096 个字符，
   整套规则在同一行上最多花 2 秒，超了就跳过。

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
$ java -jar oba-mc1.21.1-1.1.0.jar mods --game-dir "D:\MC\.minecraft"
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

34 条针对真实日志文本的规则，另加 3 条 Connector 专用规则与 1 条人为触发规则，
命中后直接给建议。1.1.0 新加的九条里，六条专管存档与地图（实体 tick 崩溃、
方块实体 tick 崩溃、区块生成失败、DataVersion 不匹配、实体数据损坏、世界加载被中止），
两条专管「服务端装了只在客户端存在的类」，一条管**加载器直接拒收某个 jar** ——
一个坏包的描述文件能让整个 mods 目录都不被加载，表现是「装了模组但一个都没生效」。

| 类别 | 规则 |
|---|---|
| 依赖 | 缺少前置、缺少必需依赖、前置版本不匹配、模组互斥 |
| 重复加载 | 同一 modId 多份、`Found duplicate mod` |
| Mixin | 注入失败、配置加载失败、变换阶段异常、MixinExtras 版本过低 |
| 类加载 | `NoClassDefFoundError`、`ClassNotFoundException`、`NoSuchMethodError`、`NoSuchFieldError` |
| 注册表 | 缺少注册条目、注册冲突 |
| 数据 / 资源包 | 数据包加载失败、资源包加载失败 |
| 存档 / 地图 | 实体 tick 崩溃、方块实体 tick 崩溃、区块生成失败、DataVersion 不匹配、实体数据损坏、世界加载被中止 |
| 客户端 / 服务端 | 服务端引用客户端专用类、缺客户端专用类、硬引用界面类 |
| 加载器 / 描述文件 | 描述文件被拒收、`[[dependencies]]` 缺 `mandatory`、整个 mods 目录不加载 |
| 文件完整性 | 内嵌库缺失、jar 损坏 |
| 运行时 | 内存不足、服务器卡死、图形驱动问题、模组加载 / 实例化失败 |
| Connector | 变换失败、缺 Fabric 前置、intermediary 名称泄漏 |
| 人为触发 | F3+C 手动调试崩溃（明确告知不是故障） |

规则只在**文本形态明确**时触发。崩溃报告自身的结构性文字（`Description:`、`Time:`、
`-- Head --`、`Mod List:` 等）被显式排除，避免把「报告里描述了崩溃」当成「发生了一次崩溃」。

---

## 项目结构

引擎只维护一份，放在 `engine/`。各版本目录由 `tools/sync-versions.ps1`
把引擎和对应加载器实现拼出来，所以同一个 jar 里不会混进别的加载器的代码。

```
oba/
│
├── README.md                       本文档（简体中文）
├── README.en.md                    English README
├── LICENSE                         GPL-3.0 许可协议全文
├── dist/                           七个版本的 jar 都收在这里（跑 tools/build-all.ps1 生成）
├── docs/                           README 用的标题条
├── examples/                       故意做坏的样例目录：假模组、几种崩溃报告
│
├── engine/                         诊断引擎的权威副本（各版本目录里的 core/ 都是它的同步副本）
│   ├── main/java/dev/oba/*         引擎源码：core / loader 接口 / mod 共享层
│   ├── main/resources/             描述文件与图标（三份描述文件，各加载器各取所需）
│   └── tools/                      引擎自测与运行时验证台
│
├── src/loader/                     加载器适配实现，按加载器分开放
│   ├── neoforge/                   NeoForge 的加载器实现（1.20.2 起，含 1.21.x）
│   └── forge/                      Forge 的加载器实现（1.20.1）
│
├── tools/                          构建、同步、测试、实机验证用的脚本
│   ├── build-all.ps1                    构建全部七条轨道并收集到 dist/
│   ├── sync-versions.ps1                把引擎同步进各版本目录
│   ├── run-engine-tests.ps1             跑引擎自测与运行时验证台
│   ├── run-version.ps1                  从实例目录启动客户端做实机验证
│   ├── mirror-neoforged.init.gradle     改写失效的 maven 主机
│   └── crasher/                         故意崩溃的测试模组（两个加载器各一份）
│
└── versions/                       每个版本一个自包含目录，可以直接构建
    ├── 1.21.1-neoforge/
    ├── 1.20.5-1.20.6-neoforge/
    ├── 1.20.2-1.20.4-neoforge/
    ├── 1.20.1-forge/
    ├── 1.21.1-forge/
    ├── 1.20.5-1.20.6-forge/
    └── 1.20.2-1.20.4-forge/
```

> 每个版本目录内部的文件清单，见该目录自己的 README。
>
> 这份树是**默认分支**上的样子：引擎一份、七条版本轨道都在，方便一次建全部 jar。
> 各版本另有独立分支，分支里就是那个版本自包含的工程（`src/main`、`build.gradle`、
> README 齐全），只想要一个版本的话切过去更清爽 —— 见 [仓库与分支](#仓库与分支)。

## 构建

一次构建全部七条轨道，产物收在 `dist/`：

```bash
pwsh tools/build-all.ps1
# -> dist/oba-mc1.21.1-1.1.0.jar
#    dist/oba-mc1.20.5-1.20.6-1.1.0.jar
#    dist/oba-mc1.20.2-1.20.4-1.1.0.jar
#    dist/oba-mc1.20.1-forge-1.1.0.jar
#    加上另外三条 Forge 轨道的 jar（本机没能出包，见下面说明）
```

也可以只构建某一个版本 —— 每个版本目录都是自包含的：

```bash
cd versions/1.21.1-neoforge && ./gradlew jar
cd versions/1.20.1-forge    && ./gradlew jar
```

如果你是从**版本分支**克隆的（比如 `1.21.1-neoforge`），分支里只有那一个版本，
分支根目录就是它的 Gradle 工程，直接 `./gradlew jar`。

| 版本 | 需要的 JDK | 备注 |
|---|---|---|
| 1.21.x（NeoForge） | **21** | |
| 1.20.5 ~ 1.20.6（NeoForge） | **21** | |
| 1.20.2 ~ 1.20.4（NeoForge） | **17** | |
| 1.20.1（Forge） | **17** | 用 ForgeGradle，构建含 reobf 步骤 |
| 1.21.1（Forge） | **21** | 用 ForgeGradle |
| 1.20.5 ~ 1.20.6（Forge） | **21** | 用 ForgeGradle |
| 1.20.2 ~ 1.20.4（Forge） | **17** | 用 ForgeGradle |

首次构建需要联网（要下载并反编译对应版本的 Minecraft）。
之后可以加 `--offline` 复用 Gradle 缓存。

> 这个环境里 `maven.neoforged.net` 连不通，NeoForge 的 1.20 轨道靠
> `mirror.init.gradle` 把仓库请求改写到官方 CDN 镜像；`build-all.ps1` 会自动带上它。
>
> 三条 1.20.2 起的 Forge 轨道首次构建要下几 MB 的 userdev：这台机器上 Java 默认直连，
> 只有 4 KB/s；在 `gradle.properties` 里配上系统代理（`systemProp.https.proxyHost` /
> `proxyPort`）后 2.7 秒就下完了。三条轨道都已出包，构建时间 2m38s / 108s / 73s。
> 仓库里那几行代理配置默认是注释掉的，需要时放开并改成本机端口就行。

### 用 IntelliJ IDEA 构建

不需要装任何插件，IDEA 自带的 Gradle 支持就够了。

**导入的是某个版本目录，不是仓库根。** 默认分支上是总览 + 引擎 + 脚本，
真正的 Gradle 工程在 `versions/<版本>-<加载器>/` 里（版本分支上则在分支根目录）。

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
java -jar build/libs/oba-mc1.21.1-1.1.0.jar analyze --game-dir ../../examples
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
| 引擎逻辑 | `pwsh tools/run-engine-tests.ps1` | **199 / 199 断言通过** |
| 引擎逻辑覆盖 | 索引、异常归属、异常链、Mixin 失败、依赖缺失、重复模组、线程转储、MCreator 识别、三种报告渲染、目录形态模组、多语言、Connector 混装、NeoForge 真实帧格式、日志编码、F3+C、带空格路径、事故时间线、**图形模组谱系与冲突** | 通过 |
| **运行时行为** | `engine/tools/RuntimeHarness.java`（真实 log4j2 + 真实 Mod 类） | **52 / 52 断言通过** |
| 运行时覆盖 | 日志采集、异常链还原、自动结论输出、报告落盘、F3+C 崩溃报告补捡、双语渲染、线程转储、只读保证 | 通过 |
| 单 jar 两种用法 | `java -jar ... version` / 模拟游戏环境 / `-Doba.mode=cli` | 三种路径都正确分派 |
| **专用服务端** | 六台真服务端：Forge 1.20.1-47.4.28 / 1.20.2-48.1.0 / 1.20.6-50.2.14 / 1.21.1-52.1.20，NeoForge 21.1.262 / 20.2.93 | 都能加载、都能正常停服；控制台 `oba status` / `oba mods` / `oba report` 可用；装仅客户端模组时报告能点名 |
| 服务端坏存档 | 把 1.20.1 服务端的 `world\region\r.0.0.mca` 截到 300 字节 | 认出「存档里的区块读不出来」并点名 `.mca`；同一句只差 index 的上百条合成一条结论，时间线折叠成一行 |
| 服务端装仅客户端模组 | 自建探针模组，在 common 类里引用 `net.minecraft.client.gui.screens.Screen` （Forge 1.20.1 与 NeoForge 20.2.93 各验一次） | 进程退出时兜底写出一份报告，点名探针模组、可信度 高 (55%)，问题清单首条是「服务端装了仅客户端模组」。控制台上一个字都没有——崩在模组加载阶段，OBA 还没来得及说话，只剩 `crash-reports` 里的文件和退出钩子 |
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

### 专用服务端实测（Forge 1.20.1 / 1.20.2 / 1.20.6 / 1.21.1，NeoForge 21.1.262 / 20.2.93）

客户端跑过之后又拿六台真服务端跑了一遍，都是原样启动、`stop` 正常退出：

| 场景 | 结果 |
|---|---|
| 空服务端 | 加载、索引、控制台命令、退出报告都正常 |
| 装上 lithium / modernfix / ferritecore / Clumps（NeoForge 21.1.262） | 索引 5 个模组文件 / 1114 个类 / 364 个包 / 38 ms，无报错 |
| 装上 8 个真实 Forge 模组（Forge 1.21.1-52.1.20） | 索引 11 个模组文件 / 3343 个类 / 611 个包 / 86 ms，14 秒进到 `Done` |
| 装上 6 个真实 Forge 模组（Forge 1.20.2-48.1.0） | 索引 8 个模组文件 / 2013 个类 / 362 个包 / 64 ms，16 秒进到 `Done` |
| 装上 6 个真实 Forge 模组（Forge 1.20.6-50.2.14） | 索引 9 个模组文件 / 2348 个类 / 368 个包 / 83 ms，14 秒进到 `Done`；balm 读配置的 `ClassCastException` 算回 balm（极高 99%） |
| 装上 10 个真实 Forge 模组（Forge 1.20.4-49.2.13 客户端） | 索引 12 个模组文件 / 3095 个类 / 576 个包 / 107 ms，23 秒进主菜单；顺手把 balm 读配置时的 `ClassCastException` 算到 balm 自己（极高 99%） |
| 装上 11 个真实 Forge 模组（Forge 1.20.6-50.2.14 客户端） | 索引 15 个模组文件 / 2702 个类 / 418 个包 / 92 ms，23.5 秒进主菜单；同样把 balm 的配置读取错误算回 balm（极高 99%） |
| 装上 9 个真实 Forge 模组（Forge 1.21.1-52.1.20 客户端） | 索引 13 个模组文件 / 3633 个类 / 660 个包 / 115 ms，23.5 秒进主菜单 |
| 区块文件被截断到 300 字节 | 命中「存档里的区块读不出来」，报告点名出问题的 `.mca`，时间线把重复的一百多条折成一行 |
| 装一个真会摸 `net.minecraft.client.*` 的探针模组 | 进程退出时兜底写出的报告里点名探针模组，问题清单首条是「服务端装了仅客户端模组」（Forge 1.20.1 / 1.20.2 / 1.21.1 与 NeoForge 20.2.93 各验一台） |

这一轮改掉的五个只在服务端才暴露的问题：

- NeoForge 20.2.93 上，`bus.register` 一个方法签名是 `handle(Event)` 的监听器会被拒
  （`Cannot register listeners for abstract class net.neoforged.bus.api.Event`），tick 钩子等于没挂上；
  改成 `IEventBus.addListener(Class, Consumer)` 反射挂载后正常。顺带把「停止事件」从同一个 try 里拆出来，
  tick 挂不上也不影响停服通知。
- 坏存档时同一句 `Region file ... has an invalid sector at index: N` 会打上百条，
  原来会变成几十条一模一样的结论、把时间线占满；现在按「抹掉数字后的形状」去重，
  结论只留一条，时间线只留两条再加一行「重复了 N 次」。
- 同一句 `Attempted to load class ... for invalid dist DEDICATED_SERVER` 在报告里会出两条结论
  （一条原句、一条带着 `Exception message: ` 前缀）。现在比形状之前先剥掉句首的
  `Caused by:` / `Exception message:` / `Suppressed:` 这些标签，只留一条。
- 采集器把**每一条日志都当成错误**：`Level.isInRange(ERROR, ALL)` 在 log4j2 里的意思是
  「从 ERROR 一直到 ALL」，而严重度的 int 值是反着排的，结果 INFO / DEBUG 全被算进错误。
  1.20.6 服务端一次只有 112 行、一条 ERROR 都没有的启动，会话小结写的是「捕获到 2284 行错误」。
  改成 `isMoreSpecificThan(ERROR)` 之后同一轮只数出 2 行——正好是 balm 的两条真实报错。
- 同一条结论在服务端 25 秒里播了六遍：balm 读同一份配置时交替抛两个不同的
  `ClassCastException`，播报键原来只记「最后一条结论」，两个键轮流把对方顶掉，抑制就失效了。
  现在键里只留异常类型（冒号之前）+ 元凶文件 + 首个可定位栈帧，并且记住最近 16 条键，
  同一轮只播一次，中间攒下的次数在下次真播时一起报。

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
- 识别 34 类文本形态明确的报错，另加 Connector 与人为触发规则
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

## 变更记录

每一次发版改了什么、为什么改、哪些还没验，都记在 [`CHANGELOG.md`](CHANGELOG.md) 里。
每条版本轨道自己还有一份更细的开发日志，在 `versions/<轨道名>/CHANGELOG.md`。

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
