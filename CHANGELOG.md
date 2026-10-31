# 版本日志

几条轨道同步发版，所以按版本记，不按轨道记。想看某条轨道自己的流水账，
去 `versions/<轨道名>/CHANGELOG.md`。

## 1.2.0 — 2026-10-31

**附属进程**

- 崩溃后弹窗的附属进程，做进了同一个 jar：客户端启动时 OBA 用 `javaw -cp <oba.jar>` 另起一个
  JVM（入口 `dev.oba.companion.CompanionMain`），classpath 就是 OBA 自己这个 jar。分发物仍然
  只有一个文件，没有 exe，也不用把界面代码解到临时目录。
- 它只等游戏 PID 消失。游戏退出后 OBA 的关闭钩子往 `logs/oba-companion/` 写一份
  `handoff-*.json`（一句话结论、元凶、问题清单、时间线、环境、报告全文），它读到就开窗：
  左边结论、元凶、问题清单，右边报告、时间线、环境、交接 JSON 四个页签。
  崩在原生库、被任务管理器结束这些「钩子也跑不掉」的情况，改用 `crash-reports/` 里这一场
  新出现的文件判定，窗口只显示崩溃报告原文。
- 界面从 WinUI 3 换成 JDK 自带的 Swing：WinUI 要多一个 .NET 可执行文件，或者把运行时塞进
  jar 再解到临时目录，跟「只发一个 jar」冲突。按 Fluent 的观感手画了圆角卡片、系统强调色、
  分段标题和等宽报告正文；动效与 Mica 没有。窗口出现时置顶 1.2 秒抢前台，然后放开。
- 触发条件从「只认崩溃」放宽到「这一场捕获过错误」：一条错误都没捕获的正常退出不打扰，
  有错误但不崩的时候也开窗，顶栏写「出过错误」。三种说法是崩溃 / 出过错误 / 正常退出。
- 问题清单每条下面加一行 `规则: <规则名>`；新加 `--tab=timeline|environment|json`，
  打开时直接翻到那一页。
- 两条新实测：1.20.5 + NeoForge 20.5（Jade 14.2.2 引用已删的 `TickEvent$ClientTickEvent`，
  捕获 88 行错误、6 条问题）关窗后窗口自己弹出，问题清单第一条带着 `规则: world.ticking_entity`；
  1.21.1 + Forge 52.1.20 一条错误都没捕获，交接文件照写、窗口不弹。
- 开关 `-Doba.companion=off|auto|always`（或 `OBA_COMPANION`），默认 `auto` 只在真崩了之后弹；
  专用服务端不启动（判据是类加载器里没有 `net.minecraft.client.Minecraft`）。
- 承诺没变：附属进程只读 `logs/oba-companion/` 和 `crash-reports/`，不碰任何模组文件。
- 实机跑过（1.20.1 + Forge 47.4.26 客户端）：12 秒进主菜单时 `logs/oba-companion/` 里已经有
  `companion.log`，进程列表里多一个 `javaw -cp …CompanionMain`；正常关窗退出后写出
  `handoff-<时刻>-<pid>.json` 与 `latest.json`，`reason` 是 `exit`，环境里带 Java 17.0.19、
  Windows 11 amd64、加载器 Forge、模组数。任务管理器强杀时不跑关闭钩子，没有交接文件，
  用 `-Doba.companion=always` 能确认窗口仍然自己到前台。
- 窗口是手写的 Swing，调试中修掉三处：中文与长类名会顶出卡片边界（改成按真实宽度折行）、
  在缩放屏幕上居中会跑到屏幕外（改成落在可用桌面内）、零宽空格在 Swing 里会画成方块
  （长串改为按字符断行）。
- 限制照实记：崩在 OBA 自己构造之前——模组发现、依赖校验阶段就失败——附属进程根本不会启动，
  这类崩溃既没有窗口也没有报告。用 `loaderVersion="[99,)"` 的探针模组验过这一条。

**窗口外观**

- 附属进程窗口改成毛玻璃外观：半透明底、圆角裁剪、外阴影、高光边、斜向高光；底图靠屏幕截图 + 方框模糊拼出来（Windows 没有公开的背景模糊接口），挪窗口会重抓。抓图前要把窗口藏起来，为此放弃了会跟逐像素透明打架的 `setOpacity` 淡入——早先那版淡入曾经让窗口彻底不显示。

**修复**

- 修了一处会让 OBA 自己崩掉的错误：模组套在别的 jar 里（jar-in-jar）时，加载器给出的路径在 zip 文件系统上，`Path.toFile()` 抛 `UnsupportedOperationException: Path not associated with default file system`，栈顶落在 `McreatorDetector.probeForBanner`。这类异常是 `RuntimeException`，只兜 `IOException` 兜不住，会一路冒到启动流程里。现在读 jar 一律走流（`ModSource` / `McreatorDetector`），路径只在真的是本地文件时才转成 `File`（`ModFiles.isPlainFile`），外面再补一层兜底；内嵌模组因此能作为独立来源参与索引。用「外层 zip 里放一个内嵌模组」的探针在 1.21.1 + NeoForge 21.1.262 服务端上验过：修复前同一条路径直接抛异常，修复后索引到 3 个模组文件、内嵌模组也认得出 MCreator 特征。

**验证**

- 九条轨道重新出包，产物都在 `dist/`，文件名带 `1.2.0`。
- 客户端：10 / 10 个实例都进到主菜单，横幅都是 `1.2.0`，索引规模 1826~5023 个类（每个实例 10~18 个模组文件），没有出现 `at dev.oba.` 开头的异常栈。
- 专用服务端：8 / 8 台全部加载并正常停服（Forge 1.20.1-47.4.28、NeoForge 21.1.262 (1.21.1)、Forge 1.21.1-52.1.20、NeoForge 20.2.93 (1.20.2)、Forge 1.20.2-48.1.0、Forge 1.20.6-50.2.14、NeoForge 26.2.0.93 (MC 26.2)、Forge 26.2-65.1.5 (MC 26.2)），服务端上 OBA 只在控制台给结论、不启动附属进程。
- 引擎自测 203 / 203 通过。

## 1.1.0 — 2026-10-16

**轨道**

- 新增三条 Forge 轨道：`1.21.1-forge`、`1.20.5-1.20.6-forge`、`1.20.2-1.20.4-forge`。
  1.20.1 仍然只有 Forge 一条，那会儿还没有 NeoForge。
- 新增 `26.2-forge` 轨道：Minecraft 26.2、Forge 26.2-65.1.5、Java 25。Forge 26.2 的加载器
  API 和 1.21.1 那条一致，`src/loader/forge262` 从 `src/loader/forge` 抄过来就能编；
  产物 `dist/oba-mc26.2-forge-1.1.0.jar` 现在也是 `./gradlew jar` 出的（首跑 48 秒，431,986 B）：
  `build.gradle` 照着 Forge 26.2 官方 MDK 重写过，FG7 的三处 DSL 变化——
  `minecraft.runs.configureEach` + `workingDir`、`minecraft.dependency(...)` 当普通依赖、
  `minecraft.mavenizer(it)` 当仓库——都按 MDK 走，wrapper 是 Gradle 9.8.2。
  之前几版是 `javac` 直编的临时产物。客户端与服务端都用这个新 jar 复测过。
- 新增 `26.2-neoforge` 轨道：Minecraft 26.2、NeoForge 26.2.0.93、Java 25。加载器 API 在 26.x 上
  没挪窝 —— `IEventBus.addListener`、`ModContainer`、`ModList` 一行没改就编过也跑通了；没跟上的是
  构建插件：`net.neoforged.gradle.userdev` 7.1.38 到 1.21.x 为止，这条轨道换成官方新给的
  `net.neoforged.moddev`（ModDevGradle）。之前因为 `maven.neoforged.net` 连不上，jar 是拿 HCML
  已装好的客户端 jar 加 NeoForge universal jar 直编的；这一轮网络恢复后 `./gradlew jar` 已经能
  出包（首跑 3 分 50 秒，要下 Minecraft 资源，之后走缓存），产物 433,912 B，就是 `dist/` 里那份。
- 八条轨道的描述文件统一：Forge 那四条共用 `META-INF/mods-forge.toml`，内容全是占位符，
  构建时按 `gradle.properties` 填；`tools/sync-versions.ps1` 会给每条轨道只留自己那份。
- `tools/build-all.ps1` 一次构建八个 jar，产物名带版本区间。

**规则**

- 新加九条规则。六条针对存档和地图：实体 tick 崩溃、方块实体 tick 崩溃、
  区块生成失败、DataVersion 不匹配、实体数据损坏、世界加载被中止；
  两条针对「服务端加载到只在客户端存在的类」，一条认 `NoClassDefFoundError`，
  一条认 `ClassNotFoundException` 式的报错；还有一条认加载器直接拒收某个 jar
  （`InvalidModFileException` / `Missing required field ... in dependency`）——
  一个坏包能让整个 mods 目录都不加载，表现是「装了模组但一个都没生效」。
- 规则匹配顺序调整：点名到具体加载器的规则（Connector 的 intermediary 泄漏）排到
  泛泛的类加载错误前面，避免同一个崩溃被更宽的那条先吃掉。
- 去重键改成「一行日志」，不再是「规则 + 摘录」。同一条崩溃不会在问题清单里刷出多条同源记录。

**启动**

- 建索引不再卡启动：改成后台线程池并行扫 jar，扫完再补算之前挂起的结论。
- 模组目录指纹（每个 jar 的名字、大小、修改时间）没变就直接复用上次的索引，不重扫。
- 索引本身不慢：只放 OBA 自己时是 3 个模组文件、8704~9862 个类、57~81 ms；装上 10~18 个真实模组后是 1802~4958 个类、68~186 ms（加载器自己的 jar 和 Minecraft 本体不算模组，早先算进去得出的五位数是虚高的）。慢的是它原来挡在启动路径上：现在改到后台线程池并行扫，扫完再补算挂起的结论。

**实机验证**

- 九条轨道的产物整跑一遍：十个客户端实例 + 八台专用服务端，全绿。这一轮又修掉两个
  只在真实服务端上出现的问题：命令里的悬停提示在专用服务端上构造 `HoverEvent` 会抛异常，
  让 `/oba status`、`/oba mods` 直接报错；以及探客户端环境用
  `Class.forName("net.minecraft.client.Minecraft")`，被 NeoForge 的 `RuntimeDistCleaner`
  记成 ERROR，于是干净服务端也报「装了仅客户端模组」——等于自己造证据。现在前者失败就
  不挂提示，后者改成问加载器要 `FMLEnvironment.dist`（资源清单兜底），不再加载客户端类。

- 三条新 Forge 轨道（`1.21.1-forge`、`1.20.5-1.20.6-forge`、`1.20.2-1.20.4-forge`）**都出了包**。卡了很久的原因跟镜像无关：这台机器的浏览器走 `127.0.0.1:7897` 的系统代理，而 Java 与 `curl` 默认直连 —— 直连 `maven.minecraftforge.net` 只有 4 KB/s 左右，同一个 userdev 文件走代理 2.7 秒下完。在 `gradle.properties` 里补上 `systemProp.https.proxyHost` / `proxyPort` 之后，三条轨道分别用 2m38s、108s、73s 建出 jar。
- 后来把代理切成 TUN 模式，直连也能 2 秒下完 3 MB 的 userdev，三条轨道重新 clean 构建分别用 17.8s、12.4s、12.1s；仓库里那几行代理配置默认注释掉，需要时再放开。
- 顺手修了两处构建脚本：Forge 1.20.5 起官方取消了 reobf，`reobfJar` 任务不再存在，两条较新的轨道改成按任务是否存在决定要不要挂 `finalizedBy`；版本区间也对不上过 —— 1.20.2 的加载器是 48 线，`loaderVersion` 从 `[49,)` 放宽到 `[48,)`，1.21.1 的 Minecraft 区间从 `[1.21,1.22)` 收成 `[1.21.1,1.21.2)`，否则服务端会以「加载器版本不够」拒收。另外 Forge 官方没有 1.20.5 的构建（metadata 里 1.20.5 线是 0 个版本），`1.20.5-1.20.6-forge` 实际只覆盖 1.20.6。
- 外加 26.2 的两个：NeoForge 26.2.0.93 客户端装了一整包 48 个模组文件，索引 48 / 23898 / 2533（317 ms），附属进程也起来了；同一个版本的专用服务端装 5 个模组，索引 8 / 5424 / 524（38 ms），`Done (0.290s)`，停服正常。以下是旧一轮的记录：NeoForge 26.2.0.93 客户端装 5 个模组，索引 7 个模组文件 / 5401 个类 /
  523 个包（68 ms），17.5 秒进主菜单；同一组模组的专用服务端 进到 `Done`，索引 44~49 ms，
  停服正常。日志里那三条 ERROR 是拿不到账号的联网错误，OBA 没有跟着误报。
- 内嵌 jar 这个修复同步到九条轨道后，又整跑了一遍：**十个客户端实例 + 八台专用服务端全绿**。顺带发现 `test-1.20.5-NeoForge` 的模组目录里有两个真不兼容的包——`Jade-1.20.5-NeoForge-14.2.2.jar` 和 `modernfix-neoforge-5.17.0+mc1.20.5.jar`，两者都引用了 NeoForge 20.5 已经删掉的 `net/neoforged/neoforge/event/TickEvent$ClientTickEvent`，一装就崩在模组构造阶段。OBA 两次都把责任算回它们自己（70%），把这两个包移走之后同一实例正常进主菜单（索引 11 / 2564 / 468 / 98 ms）。
- 七个客户端都进到了主菜单，日志里有 `[OBA] enabled: 1.1.0`、报告目录提示和 `/oba` 提示：1.20.1-Forge、1.20.2/1.20.3/1.20.4-NeoForge、1.20.5/1.20.6-NeoForge、1.21.1-NeoForge。
- 之后又补了两台 Forge 客户端（此前这三条轨道只有专用服务端结果）：1.20.4 装 10 个真实 Forge 模组，索引 12 个模组文件 / 3095 个类 / 576 个包 / 107 ms；1.20.6 装 11 个，15 / 2702 / 418 / 92 ms；两台都在 23 秒左右进主菜单，并且都把 balm 读配置时的 `ClassCastException` 算回 balm 自己（极高 99%）。
- 再加一台 1.21.1 Forge 客户端：装 9 个真实 Forge 模组（10 个 jar），索引 13 个模组文件 /
  3633 个类 / 660 个包 / 115 ms，23.5 秒进主菜单。
- 之后从 Modrinth 按版本给每个实例装了 10~18 个真实模组（按 `dependencies` 递归补前置，加载器对不上的构建会当场剔掉）重跑：七个实例都能进主菜单，索引落在 10~18 个模组文件、1802~4958 个类、383~1150 个包、68~186 ms。
- 真实模组一上来就暴露了一处会让归因失效的缺陷：NeoForge 的 `IModInfo.getOwningFile().getFile().getFilePath()` 给的是联合文件系统里的虚拟路径，`Files.isRegularFile` 判不过，**十六个模组只索引到三个**。现在除了问加载器要列表，还无条件扫一遍 `<游戏目录>/mods/*.jar` 合并去重。
- 还暴露了一处会卡死客户端的问题：分析原本跑在 log4j 的 appender 线程上，某一行超长日志让正则回溯几十秒，整个日志写盘被堵住，客户端看着就像卡死（`jstack` 里 `modloading-sync-worker` 占满一个核 90 秒，栈顶停在 `LogAnalyzer.find`）。现在分析只往自己的单线程队列里排，超过 4096 字符的行截断后再匹配，规则匹配总预算 2 秒。
- 噪声收了一条：混入框架给可选集成报的 `Error loading class: org/jetbrains/annotations/...` 原来会被归成「类找不到（依赖缺失）」，现在库命名空间（`org.jetbrains`、`kotlin`、`com.google` 等）里的类缺失不再报，模组自己的类缺失照报。
- 归因还修了一处：`neoforge-*-universal.jar` 这类平台 jar 原来会被当成模组收进索引，1.20.5 那次崩溃被 99% 可信度算到 neoforge 头上。现在加载器本体、Minecraft、启动器和 `libraries/` 下的库都不进索引，同一个崩溃重新算回 jade。
- 1.20.3 一开始以为只是卡在 OpenAL（`jstack` 停在 `alcResetDeviceSOFT`），真正的第一原因是 mods 目录被加载器整目录拒收：装进去的 1.20.4 构建缺 `mandatory` 字段（20.2~20.3 还要求，1.20.4 起删掉），一个这样的 jar 就够让所有模组都不加载，看起来正好像「模组装了但没生效」。换掉那几个包、再给这块 USB 声卡 `ALSOFT_DRIVERS=null`，1.20.3 也跑通了（10 个模组文件 / 1802 个类 / 70 ms）。
- 实机在 1.20.1-Forge 的启动阶段抓到一处会把日志刷爆的缺陷：显卡驱动每帧打一条 shader 报错，原来是每一条都触发一次分析、每次分析都把结论播一遍，两秒内刷出二十多行、四份报告文件。现在同一行错误 5 秒内只算一次，同一条结论 5 分钟内只播一次，攒下的重复次数合并成一句「又出现了 N 次」；报告文件也按结论指纹加 3 秒最小间隔去重。只有警告级、又没点到任何模组的命中不再单独写报告文件，只在对话里播一次；采样器名字（`Sampler2` 这种）也不再被当成模组 id 去查。

**专用服务端**

- 六台真服务端跑通：Forge 1.20.1-47.4.28、Forge 1.20.2-48.1.0、Forge 1.20.6-50.2.14、
  Forge 1.21.1-52.1.20、NeoForge 21.1.262、NeoForge 20.2.93。都能加载、都能正常停服，
  控制台里 `oba status` / `oba mods` / `oba report` 都有输出。
- 真实模组也装过：Forge 1.20.6 装 6 个，索引 9 个模组文件 / 2348 个类 / 368 个包（83 ms），
  14 秒进到 `Done`，balm 读配置的 `ClassCastException` 算回 balm（极高 99%）；
  Forge 1.21.1 装 8 个，索引 11 个模组文件 / 3343 个类 / 611 个包（86 ms）；
  Forge 1.20.2 装 6 个，索引 8 个模组文件 / 2013 个类 / 362 个包（64 ms）；
  NeoForge 21.1.262 装 4 个，索引 5 个模组文件 / 1114 个类 / 364 个包（38 ms）。
- tick 钩子在 NeoForge 20.2.93 上其实一直没挂上：`bus.register` 拒绝方法签名为 `handle(Event)`
  的监听器（`Cannot register listeners for abstract class net.neoforged.bus.api.Event`）。
  改成 `IEventBus.addListener(Class, Consumer)` 反射挂载后正常；顺带把停服监听器拆出同一个
  `try`，tick 挂不上也不再连带停服通知一起失效。
- 装了 lithium / modernfix / ferritecore / Clumps 的服务端索引 5 个模组文件 / 1114 个类 /
  364 个包 / 38 ms，退出报告里只有两条无害 WARN。
- 把 1.20.1 服务端的 `world\region\r.0.0.mca` 截到 300 字节，真实日志是
  `Region file ... has truncated header: 300` 加一百多条 `has an invalid sector at index: N`，
  原来的 `world.chunk` 一条都不认。现在这两句都认；同一句只差数字的命中改成按「抹掉数字后的
  形状」去重，问题清单从十二条压到两条，时间线折叠成一行「重复了 112 次，已折叠」。
- 服务端装仅客户端模组：造了个探针模组，在 common 类里引用
  `net.minecraft.client.gui.screens.Screen`，Forge 1.20.1 与 NeoForge 20.2.93 各装了一次。
  两个加载器报的都是 `Attempted to load class net/minecraft/client/gui/screens/Screen for invalid dist
  DEDICATED_SERVER`（NeoForge 包成 `RuntimeException`），不是 `NoClassDefFoundError`，规则两种都认。
  这种崩发生在模组加载阶段，控制台上 OBA 一个字都播不出来；这次运行退出时兜底写的那份报告里
  点名了探针模组、可信度「高 (55%)」，问题清单首条就是「服务端装了仅客户端模组」。
- 同一句 `Attempted to load class ... for invalid dist DEDICATED_SERVER` 在报告里出过两条结论
  （一条原句、一条带着 `Exception message: ` 前缀）。现在比形状之前先剥掉句首的
  `Caused by:` / `Exception message:` / `Suppressed:` 标签，只剩一条。
- 采集器把每一条日志都当成错误：`Level.isInRange(ERROR, ALL)` 在 log4j2 里读作「从 ERROR 到 ALL」，
  而严重度的 int 值是反着排的，INFO / DEBUG 全被算成错误。1.20.6 服务端一次只有 112 行、
  一条 ERROR 都没有的启动，会话小结写成「捕获到 2284 行错误（致命 2284 行）」。改成
  `isMoreSpecificThan(ERROR)`、并且只有 FATAL 才计入致命之后，同一轮只数出 2 行，
  正好是 balm 的两条真实报错。
- 同一条结论在服务端 25 秒里播了六遍：balm 读同一份配置时交替抛两个不同的
  `ClassCastException`，播报键原来只记最后一条，两个键互相顶掉，抑制等于没有。键现在只留
  异常类型（冒号之前）+ 元凶文件 + 首个可定位栈帧，并且记住最近 16 条键：同一轮只播一次，
  攒下的重复次数在下一次真播时一起报。
- 还有一条照旧没被真机触发：把 `level.dat` 的 `DataVersion` 改成 9999，1.20.1 与 1.21.1
  服务端都照常启动，`world.dataversion` 仍然只有构造文本的验证。

## 1.0.1 — 2026-10-15

- 游戏内 `/oba` 命令树重新注册（此前被重复注册顶掉，整棵树没挂上）。
- 补 `pack.mcmeta` / `pack.png`，Forge 不会再把它当成坏掉的资源包报警。
- 日志跟踪不再自己吃自己：自己的结论行和聊天栏回显被显式排除，报告不会层层嵌套。
- 管道里也能上色：`-Doba.color` / `OBA_COLOR` / `--color` 三个开关真正生效，
  以前只看有没有终端。
- 游戏内界面按像素排版，终端按字符列排版，两边共用同一份视觉元素。

## 1.0.0 — 2026-10-14

- 第一版：日志归一化、规则匹配、堆栈反查、模组打分、事故时间线、报告落盘。
- 四条轨道：1.21.1、1.20.5 ~ 1.20.6、1.20.2 ~ 1.20.4（NeoForge）和 1.20.1（Forge）。
- CLI 子命令 `analyze` / `mods` / `mcreator` / `watch` / `version`，以及游戏内 `/oba` 菜单。
