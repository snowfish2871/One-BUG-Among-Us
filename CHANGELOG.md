# 变更记录

这份文件只记 `versions/26.2-forge/` 这一条轨道自己的历史：这一版改了什么、修了什么、还剩什么。
仓库总体的版本说明在根目录的 `CHANGELOG.md`。

## 这条轨道是什么

| 项目 | 值 |
|---|---|
| Minecraft | 26.2 |
| 加载器 | **Forge** 26.2 线，构建时对 26.2-65.1.5 |
| Java | 25 |
| 编译目标 | 26.2 / 26.2-65.1.5 |
| 描述文件 | META-INF/mods-forge.toml（源文件 `mods-forge.toml`） |
| 产物 | `oba-mc26.2-forge-1.2.0.jar` |
| 状态 | 已出包，已实机验证 |

Forge 把 26.2 的版本写成 `26.2-65.x`（加载器主版本 65），要 Java 25。这条轨道只对 26.2，26.1 和 26.3 没有单独出包。

## 1.2.0 — 2026-10-31

### 这一版这条轨道改了什么

- 版本号抬到 1.2.0，产物换成 `oba-mc26.2-forge-1.2.0.jar`；引擎改动与 1.1.0 的规则、归因没有回退。

### 这一版进到这个 jar 里的引擎改动

- **崩溃后的附属进程做进同一个 jar**：客户端启动时用 `javaw -cp <本 jar>` 另起一个 JVM，入口
  `dev.oba.companion.CompanionMain`；游戏退出时关闭钩子往 `logs/oba-companion/` 写交接 JSON，
  它读到就开窗。分发物仍然只有一个文件，没有 exe，也不往临时目录解东西。
- **窗口三态**：崩了顶栏写「崩溃」并抢前台；只是捕获过错误写「出过错误」；一条错误都没有的正常
  退出不开窗。判据分别是本场新出现的崩溃报告、交接 JSON 里的 `capturedErrors`、以及两者都没有。
- **问题清单带规则名**：每条下面一行 `规则: <规则名>`，不用再去翻报告正文。
- **`--tab=report|timeline|environment|json`**：打开时直接翻到那一页。
- **毛玻璃外观**：背后一块屏幕截图缩到四分之一、方框模糊，铺半透明着色，外层圆角裁剪，
  加软阴影和 1px 高光边。Windows 没有公开的背景模糊接口，底图靠截屏；挪窗口会重抓，
  抓之前得先把窗口藏起来（逐像素透明与整窗不透明度互斥，早先那版淡入直接把窗口弄没了）。
- **套在别的 jar 里的模组（jar-in-jar）也能读**：这类模组的路径在 zip 文件系统上，
  `Path.toFile()` 会抛 `UnsupportedOperationException`，原来会把 OBA 自己带崩（栈顶在
  `McreatorDetector.probeForBanner`）。现在读 jar 一律走流，路径只在真的是本地文件时才转成
  `File`（`ModFiles.isPlainFile`），外面再补一层兜底；内嵌模组因此能作为独立来源参与索引。
- **附属进程只读**：只读 `logs/oba-companion/` 与 `crash-reports/`，不动 `mods/`、不动存档。

### 这一版验证到哪一步

- 客户端 26.2 + Forge 26.2-65.1.5（Java 25），装 Mouse Tweaks / Carry On / Better Advancements / Fast IP Ping / No Chat Reports / Iceberg / Collective 七个真实模组：`[OBA] 已启用：1.2.0`，15.4 秒进主菜单，索引 8 个模组文件 / 1100 个类 / 204 个包（29 ms），附属进程也起来了；日志里只有两条拿不到账号的联网错误，OBA 没有误报。
- 同版本的专用服务端装同样七个模组：索引 8 个模组文件 / 1100 个类 / 204 个包（26 ms），进到 `Done (0.190s)`，停服正常，会话小结「本次会话未捕获到错误」，`oba status`、`oba mods` 都有输出。
- 引擎自测 **203 / 203** 通过，运行时验证台 **52 / 52** 通过。

### 这一版还剩的问题

- 26.1 / 26.3 没有单独出包，也没进过游戏。
- Forge 26.2 在 Windows 上会自己抛 `AppenderLoggingException`（`io.netty.channel.epoll.Native` / `kqueue.Native` 只有 Linux、OSX 才有），服务端照常跑完；OBA 没有把这串算成崩溃。
- 游戏内 `/oba` 菜单只验到「命令能输、聊天栏有输出」，没有逐项截图核对。

## 1.1.0 — 2026-10-16

这条轨道在 1.1.0 才出现，所以没有更早的版本记录。

### 这一版这条轨道改了什么

- 新开这条轨道：目标 `26.2 / 26.2-65.1.5`，Java 25，产物名带 `mc26.2-forge`，描述文件 `META-INF/mods-forge.toml`（构建时展开成 `mods.toml`）。
- Forge 26.2 的加载器 API 和 1.21.1 那条一致，`src/loader/forge262` 直接从 `src/loader/forge` 抄过来就能编；跟不上的只有构建脚本里的 ForgeGradle 版本。

### 这一版进到这个 jar 里的引擎改动

- **六条存档 / 地图规则 + 两条服务端规则**：实体 tick 崩溃、方块实体 tick 崩溃、区块生成失败、
  DataVersion 不匹配、实体数据损坏、世界加载被中止共六条；另两条认「服务端加载到只在客户端
  存在的类」，`NoClassDefFoundError` 与 `ClassNotFoundException` 两种写法各一条。文案中英各一份。
- **规则顺序调整**：点名到加载器的规则排到泛泛的类加载错误前面，同一个崩溃不会被更宽的那条先吃掉。
- **去重键换成「一行日志」**：不再是「规则 + 摘录」，同一条崩溃不会在问题清单里刷出多条同源记录。
- **建索引挪出启动路径**：改成后台线程池并行扫 jar，启动不等它；扫完再补算之前挂起的结论。
- **模组目录指纹**：每个 jar 的名字、大小、修改时间都没变就复用上次的索引，不重扫。
- **索引范围兜底**：除了问加载器要模组列表，还会无条件扫一遍 `<游戏目录>/mods/*.jar` 合并去重——
  加载器给的路径不一定都能当普通文件读，只靠它会漏掉大部分模组。
- **分析离开日志线程**：分析只往自己的单线程队列里排，不再占着 log4j 的 appender 线程；
  超过 4096 字符的行截断后再匹配，单次扫描的规则匹配总预算 2 秒。
- **噪声过滤**：库命名空间（`org.jetbrains`、`kotlin`、`com.google` 等）里的类缺失不再报
  「依赖缺失」，模组自己的类缺失照报。
- **刷屏节流**：同一行错误 5 秒内只算一次；同一条结论 5 分钟内只播一次，攒下的次数合并成
  「又出现了 N 次」；报告文件按结论指纹加 3 秒最小间隔。
- **加载器拒收 jar 的单条规则**：`InvalidModFileException` / `Missing required field ... in dependency`
  现在有自己的规则，文案直接说明「这个 jar 比拼的加载器新，一个坏包会让整个 mods 目录都不加载」。
- **平台 jar 不进索引**：加载器本体、Minecraft 自己、启动器和 `libraries/` 下的库不再被当成模组，
  归因不会再把崩溃算到 neoforge 头上（实机上真出现过 99% 可信度指着加载器自己）。
- **报告门槛**：只有警告级、又没点到任何模组的命中不再单独写报告文件，只在对话里播一次。
- **采样器名不再当模组名**：`graphics.sampler_missing` 不再把 `Sampler2` 这类名字当 modId 去查。
- **区块规则认可真实 region 日志**：1.20.1 服务端把区块文件截断后打的是
  `Region file ... has truncated header: 300` 和上百条 `has an invalid sector at index: N`，
  原来的规则一条都不认，现在两句都认。
- **按形状去重 + 时间线折叠**：同一句只差数字的命中（`index: 0`、`index: 1`…）合并成一条结论，
  时间线同一形状只留两行、后面跟一句「重复了 N 次，已折叠」。
- **服务端拦客户端类的那句话也认**：Forge 1.20.1 专用服务端报的是
  `Attempted to load class net/minecraft/client/... for invalid dist DEDICATED_SERVER`，
  不是 `NoClassDefFoundError`，规则两种都认。
- **tick 钩子改反射挂载**：NeoForge 20.2 上 `bus.register` 会拒绝方法签名为 `handle(Event)` 的监听器
  （抽象事件类不能注册），改成 `IEventBus.addListener(Class, Consumer)`；停服监听器拆出同一个
  `try`，tick 挂不上不再连带停服通知一起失效。
- **采集器不再把每条日志都当错误**：`Level.isInRange(ERROR, ALL)` 在 log4j2 里读作「从 ERROR
  一直到 ALL」，而严重度是反着排的，INFO / DEBUG 全被算成错误；一次一百多行、一条 ERROR 都没有
  的启动被报成「捕获到 2284 行错误」。改成 `isMoreSpecificThan(ERROR)`，致命只看 `isMoreSpecificThan(FATAL)`。
- **播报节流记住多条键**：键从「整句异常文本」改成「异常类型（冒号之前）+ 元凶文件 + 首个可定位
  栈帧」，并且记住最近 16 条，交替出现的两种异常不会再把对方的抑制顶掉。


### 这一版验证到哪一步

- 客户端 26.2 + Forge 26.2-65.1.5（Java 25），装 Mouse Tweaks / Carry On / Better Advancements / Fast IP Ping / No Chat Reports / Iceberg / Collective 七个真实模组：`[OBA] 已启用：1.2.0`，15.4 秒进主菜单，索引 8 个模组文件 / 1100 个类 / 204 个包（29 ms），附属进程也起来了；日志里只有两条拿不到账号的联网错误，OBA 没有误报。
- 同版本的专用服务端装同样七个模组：索引 8 个模组文件 / 1100 个类 / 204 个包（26 ms），进到 `Done (0.190s)`，停服正常，会话小结「本次会话未捕获到错误」，`oba status`、`oba mods` 都有输出。
- 引擎自测 **203 / 203** 通过，运行时验证台 **52 / 52** 通过。

### 这一版还剩的问题

- 26.1 / 26.3 没有单独出包，也没进过游戏。
- Forge 26.2 在 Windows 上会自己抛 `AppenderLoggingException`（`io.netty.channel.epoll.Native` / `kqueue.Native` 只有 Linux、OSX 才有），服务端照常跑完；OBA 没有把这串算成崩溃。
- 游戏内 `/oba` 菜单只验到「命令能输、聊天栏有输出」，没有逐项截图核对。
