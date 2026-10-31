# -*- coding: utf-8 -*-
"""根 README 与轨道 README 的中文正文。

只放文字；轨道数据从 tools/track_info.py 取，拼装在 tools/gen-readmes.py 里。
"""

# ---------------------------------------------------------------- 规则库

RULES = [
    ("加载器与依赖", [
        ("neoforge.missing_dependency", "`Missing or unsupported mandatory dependencies` 那几行", "缺前置，并点名缺哪个模组"),
        ("neoforge.missing_dependency_generic", "同样的依赖失配，但日志里没有清单行", "同上，靠上下文猜"),
        ("neoforge.incompatible_mod", "`Mod ... requires ... which is missing`", "版本对不上"),
        ("neoforge.dependency_version", "`requires <mod> <range>` 里的区间不满足", "装了但版本太旧/太新"),
        ("fml.duplicate_mod", "`Found duplicate mods`", "同一个 modId 出现两份，列出两个文件"),
        ("fml.duplicate_mod_alt", "旧版 FML 的另一种措辞", "同上"),
    ]),
    ("模组文件本身", [
        ("loader.invalid_mod_file", "`InvalidModFileException` / `Missing required field ... in dependency` / `found an invalid mod file`", "一个坏 jar 让整个 mods 目录不加载，点名那个文件"),
        ("jar.corrupt", "`zip END header not found` / `invalid CEN header`", "jar 下载不全"),
        ("jarjar.missing", "`Unable to resolve ... inside jar`", "jar-in-jar 里缺依赖"),
    ]),
    ("Mixin", [
        ("mixin.apply_failed", "`Mixin apply failed` / `MixinTransformerError`", "哪个 mixin、目标类、被谁改坏"),
        ("mixin.apply_failed_alt", "另一种写法", "同上"),
        ("mixin.transformer_error", "`MixinTransformerError`", "同上，走转换器报错的路径"),
        ("mixin.config_failed", "`Failed to load mixin config`", "mixin 配置文件本身坏了"),
        ("mixin.crash", "`org.spongepowered.asm.mixin.throwables.MixinException`", "异常从 mixin 里抛出来"),
        ("mixinextras.version", "`MixinExtras` 版本不满足", "MixinExtras 太旧"),
    ]),
    ("类加载与链接", [
        ("class.noclassdef", "`NoClassDefFoundError: <类>`", "哪个类没找到，按包名归属"),
        ("class.classnotfound", "`ClassNotFoundException: <类>`", "同上，另一条路径"),
        ("class.nosuchmethod", "`NoSuchMethodError`", "调到了不存在的方法，通常是版本错配"),
        ("class.nosuchfield", "`NoSuchFieldError`", "同上，字段"),
    ]),
    ("注册表与资源", [
        ("registry.missing", "`Unknown registry key` / `Missing registry`", "缺注册项，通常是那条数据包/模组的锅"),
        ("registry.duplicate", "`Duplicate registration`", "两个模组抢同一个 id"),
        ("datapack.load_failed", "`Failed to load datapack`", "数据包解析失败"),
        ("resourcepack.load_failed", "`Failed to load resource pack`", "资源包解析失败"),
    ]),
    ("存档与地图", [
        ("world.ticking_entity", "`Ticking entity` / `Exception while ticking entity`", "存档里某个实体坏了，给出类型与坐标"),
        ("world.ticking_block_entity", "`Ticking block entity`", "方块实体（箱子、机器、刷怪笼）坏了"),
        ("world.chunk", "`Region file ... is corrupt/missing` / `has truncated header` / `has an invalid sector` / `Failed to load chunk`", "区块文件坏了"),
        ("world.dataversion", "`Data version ... is newer` / `Unknown data version`", "存档来自更新的游戏版本"),
        ("world.entity_data", "`Failed to load entity data` / `Invalid entity NBT`", "实体/容器的 NBT 不合法"),
        ("world.level_load", "`Failed to load level data or datapacks`", "世界压根没起来，指向存档引用的数据包或模组"),
    ]),
    ("渲染与图形", [
        ("opengl.unsupported", "`Unsupported OpenGL version` / 版本低于 3.2", "驱动太旧"),
        ("graphics.unsupported_driver", "`GLFW error 65542`（Microsoft Basic Render）", "没装显卡驱动，走的是软件渲染"),
        ("graphics.glfw_init", "`Failed to initialize GLFW`", "窗口/GL 上下文起不来"),
        ("graphics.context_lost", "`GL context lost` / `Context lost`", "驱动把上下文丢了"),
        ("graphics.shader_compile", "`Shader compile error` / `Could not compile shader`", "哪个着色器编译失败"),
        ("graphics.shader_pack", "`Failed to load shader pack`", "光影包加载失败"),
        ("graphics.sampler_missing", "`could not find sampler named`", "着色器采样器缺失，指向图形模组/光影"),
        ("graphics.framebuffer", "`Framebuffer is not complete`", "帧缓冲不完整，多见于光影"),
        ("graphics.mixin_conflict", "`Mixin conflict` / `already transformed`", "两个优化模组改同一个方法"),
        ("graphics.family_conflict", "另一套图形模组家族的冲突措辞", "同上，换了个说法"),
    ]),
    ("内存与卡死", [
        ("memory.outofmemory", "`java.lang.OutOfMemoryError`", "堆不够，给出建议与当前上限"),
        ("watchdog.timeout", "服务器 tick 超过阈值（OBA 自己计时，不认日志）", "哪个阶段卡住，附最后一次操作"),
    ]),
    ("服务端与客户端", [
        ("server.client_class", "`NoClassDefFoundError: net.minecraft.client.*` / `Attempted to load class net/minecraft/client/... for invalid dist DEDICATED_SERVER`", "服务端装了仅客户端模组，点名那个类属于谁"),
        ("server.client_class_notfound", "`ClassNotFoundException: net.minecraft.client.*`", "同上，另一条路径"),
    ]),
    ("模组生命周期", [
        ("mod.loading_failed", "`Mod Loading has failed` / `Failed to create mod instance`", "加载阶段整体失败，按上下文找元凶"),
        ("mod.construct_error", "`Failed to complete lifecycle event CONSTRUCT`", "某个模组的构造函数抛了"),
        ("manual.debug_crash", "`Manually triggered debug crash`", "不是 bug，按 F3+C 之类主动触发的"),
    ]),
    ("Sinytra Connector", [
        ("connector.intermediary", "`class_1234` / `method_5678` / `field_9012` 这类 intermediary 名", "Fabric 模组在 Connector 下没转好，点名那个 Fabric 模组"),
        ("connector.failure", "Connector 自己的报错", "Connector 版本/兼容性问题"),
        ("connector.missing_mod", "`Requires ... Fabric API` 之类", "缺 Fabric 侧前置"),
    ]),
]

# ---------------------------------------------------------------- 处理得了的情况

CASES = [
    ("缺前置模组", "点名缺的是哪个、要求什么版本区间", "规则 `neoforge.missing_dependency*`"),
    ("模组版本对不上", "指出是「版本太旧」还是「要求更高」，给区间", "规则 `neoforge.dependency_version` / `incompatible_mod`"),
    ("modId 重复", "列出两个文件名，让人删掉旧的", "规则 `fml.duplicate_mod*`"),
    ("一个 jar 让整个 mods 目录都不加载", "点名那个 jar，并说明 1.20.2~1.20.4 要求 `mandatory` 字段", "规则 `loader.invalid_mod_file`"),
    ("jar 下载不全", "提示重新下载，给文件名", "规则 `jar.corrupt`"),
    ("Mixin 应用失败", "定位到 mixin 类与目标类，指出被哪个模组改坏", "规则 `mixin.*`"),
    ("类找不到 / 方法字段不存在", "按包名归属到模组；库命名空间（`org.jetbrains`、`kotlin`、`com.google`）默认不报", "规则 `class.*` + 噪声过滤"),
    ("注册表缺项、重复注册", "指向注册项名与涉及模组", "规则 `registry.*`"),
    ("数据包 / 资源包解析失败", "给包名与第一条解析错误", "规则 `datapack.*` / `resourcepack.*`"),
    ("存档里的实体坏了", "给实体类型、坐标、异常首行", "规则 `world.ticking_entity`"),
    ("方块实体坏了（箱子、机器、刷怪笼）", "给方块坐标与异常", "规则 `world.ticking_block_entity`"),
    ("区块文件损坏（截断、扇区越界）", "指出是哪个 `.mca`，建议从备份恢复或删掉那一块", "规则 `world.chunk`"),
    ("存档来自更新的版本", "说明 DataVersion 不匹配，降级不是模组能解决的", "规则 `world.dataversion`"),
    ("实体/容器的 NBT 不合法", "指向那条实体数据，提示多半是删过模组", "规则 `world.entity_data`"),
    ("世界压根没起来", "指向存档引用的数据包或模组", "规则 `world.level_load`"),
    ("显卡驱动太旧 / 走软件渲染", "说清楚是驱动问题，不是模组", "规则 `opengl.*` / `graphics.unsupported_driver`"),
    ("着色器编译失败、采样器缺失、帧缓冲不完整", "指向图形模组或光影包", "规则 `graphics.shader*` / `sampler_missing` / `framebuffer`"),
    ("两个优化模组改同一个方法", "点名冲突的双方", "规则 `graphics.mixin_conflict` / `family_conflict`"),
    ("内存不够", "给出当前上限与「加 -Xmx」的建议", "规则 `memory.outofmemory`"),
    ("服务器卡死（tick 不推进）", "OBA 自己按 tick 间隔计时，报出卡住的阶段", "看门狗 `watchdog.timeout`"),
    ("服务端装了仅客户端模组", "点名那个类属于哪个模组，建议从服务端 mods 里拿掉", "规则 `server.client_class*`"),
    ("Sinytra Connector 下的 Fabric 模组", "用 intermediary 名反查，点名 Fabric 模组", "规则 `connector.*`"),
    ("MCreator 生成的工作区", "按 MCreator 的目录/命名特征认出来，给专门的建议", "`McreatorDetector`"),
    ("崩在 OBA 自己构造之前", "没有窗口也没有报告，靠下一次启动读 `crash-reports/` 补一份", "退出钩子 + 启动补读"),
    ("正常退出但这一场有错误", "附属进程照样开窗，顶栏写「出过错误」", "交接 JSON 里的 `capturedErrors`"),
    ("崩在原生库、被任务管理器结束", "关闭钩子不跑，退化成读本场新增的崩溃报告", "`CrashReporter`"),
    ("模组把别的模组套在 jar 里（jar-in-jar）", "内嵌的那个模组单算一个来源，照样能索引、能定元凶", "`ModSource` 流式读取 + `ModFiles` 分流"),
]

# ---------------------------------------------------------------- 根 README

def root(tracks):
    support = "\n".join(
        "| {mc} | {loader} | {java} | `{jar}` |".format(
            mc=t["mc_zh"], loader=t["loader_zh"].replace("**", ""), java=t["java"], jar=t["jar"])
        for t in tracks.values())

    rules_total = sum(len(rows) for _, rows in RULES)
    rules_block = []
    for cat, rows in RULES:
        rules_block.append("### %s（%d 条）\n\n| 规则 id | 认什么 | 结论指向 |\n|---|---|---|\n%s\n"
                           % (cat, len(rows),
                              "\n".join("| `%s` | %s | %s |" % (rid, what, where) for rid, what, where in rows)))
    rules_block = "\n".join(rules_block)

    cases = "\n".join("| %s | %s | %s |" % (a, b, c) for a, b, c in CASES)

    return f"""# One BUG Among Us

**把 Minecraft 的崩溃翻译成一句人话。**

它读三种东西：游戏日志、崩溃报告、mods 目录里的 jar。读完给一条结论——哪个模组、可信度多少、下一步做什么。不装驱动、不改配置、不动任何模组文件。

- 客户端和服务端都能跑：服务端没有聊天栏时结论走控制台
- 九条版本轨道，从 1.20.1 到 26.2，Forge 与 NeoForge 各自出包
- 一个 jar 同时带游戏内命令、命令行和崩溃后的独立窗口

> **只读**：OBA 只写自己那份报告和日志，不改动、不移动、不删除任何模组文件。

## 目录

- [这是什么](#这是什么)
- [支持范围](#支持范围)
- [一条错误怎么变成结论](#一条错误怎么变成结论)
- [采集：日志与崩溃报告](#采集日志与崩溃报告)
- [模组索引](#模组索引)
- [归因与置信度](#归因与置信度)
- [规则库](#规则库)
- [误报是怎么排掉的](#误报是怎么排掉的)
- [跨版本适配](#跨版本适配)
- [处理得了的情况](#处理得了的情况)
- [游戏内界面](#游戏内界面)
- [崩溃之后的附属进程](#崩溃之后的附属进程)
- [命令行](#命令行)
- [构建](#构建)
- [目录结构](#目录结构)
- [已知限制](#已知限制)
- [许可](#许可)

## 这是什么

模组崩溃的日志很长，真正有用的往往只有一行 `Caused by:` 加一段栈。人的做法是往上翻、找 `at` 里带模组包名的那几帧、确认那个包属于哪个模组。OBA 把这几步做成固定流程：日志进来，规则先认出「这是哪一类错误」，栈帧再落到具体模组，最后合成一句话结论和一张问题清单。

它不猜。每条结论都带着出处：命中了哪条规则、哪一行日志、哪个栈帧、哪个 jar。

## 支持范围

| Minecraft | 加载器 | Java | 产物 |
|---|---|---|---|
{support}

1.20.1 只有 Forge；1.20.2 到 1.21.x 每个区间 Forge 与 NeoForge 各一条；26.2 两条。同一区间的 Forge 与 NeoForge 版本共用一份引擎代码，只在适配层分开。

## 一条错误怎么变成结论

1. **收**：log4j2 上的 appender 逐行接住 ERROR/FATAL；`crash-reports/` 里出现的新文件也一起收进来。
2. **索引**：扫 `mods/*.jar`，建两张表——类全名到模组、包前缀到模组。
3. **认规则**：{rules_total} 条正则规则逐条匹配。同一条日志只留最专门的那条结论。
4. **认栈帧**：异常里每个 `at` 的类名拿去查索引；查不到就用包前缀做模糊匹配，得出一组候选。
5. **打分**：栈帧离根因多远、规则严重度、模组 id 有没有出现在日志正文里、多个栈帧是不是指向同一个模组——四项加权。
6. **出结论**：一句话 headline、元凶卡（模组、文件、首个栈帧、置信度）、问题清单、建议。控制台、聊天栏、报告文件、附属进程窗口用的是同一份数据。

## 采集：日志与崩溃报告

**为什么挂在 appender 上**：从 stdout 里读要额外起进程，抓不到服务端控制台重定向之前的行，也分不清级别。appender 能拿到 `Level` 与 logger 名，还能在写盘之前就看见这一行。

几个细节是踩出来的：

- 级别判定不能用 `level.isInRange(Level.ERROR, Level.ALL)`。log4j2 里严重度的 int 是反序的（FATAL=100、ERROR=200、INFO=400），这句读作「ERROR 到最啰嗦」，INFO 会被算成错误。现在用 `isMoreSpecificThan(Level.ERROR)`，并且只有 `isMoreSpecificThan(Level.FATAL)` 才算致命。
- 分析不能跑在 appender 线程上。一行超长日志配上回溯型正则，会把整个日志写盘堵死；现在行内容截到 4096 字符、规则总预算 2 秒，分析丢到一个单线程队列里跑。
- 不抓自己：带 `[OBA] ` 前缀的行直接跳过，否则报告本身会被当成新错误再分析一遍。
- 重复行合并：5 秒内同一行只分析一次；同一句结论 5 分钟只播一次，重复次数在下一次播报时一起给。
- 崩溃报告：`crash-reports/` 里的新文件（按修改时间晚于本场开始）会被读进时间线；退出时如果没来得及分析，退出钩子补写一份。

## 模组索引

索引是归因的地基。每个 jar 打开一次，记下：`modId`、显示名、版本、文件名，以及 jar 里所有 `.class` 的全名与包前缀。

三件必须做的事：

- **不只信加载器给的路径**。NeoForge 的 `IModFileInfo` 给的是联合文件系统的虚拟路径，`Files.isRegularFile` 不成立；只按它扫，16 个模组能只索引到 3 个。所以加载器列表之外，无条件再扫一遍 `<游戏目录>/mods/*.jar`，按路径去重。
- **平台自带的 jar 不进索引**。`libraries/` 下的、`neoforge-`/`forge-` 开头的、`client-extra`、`securejarhandler` 这些不是模组，混进来会让归因指着加载器本身。
- **不挡启动**。索引在后台线程池（`min(8, 核数)`）里并行做，主线程只登记任务；模组目录没变（名字+大小+修改时间一致）就跳过重扫。
- **套在别的 jar 里的模组也读**。jar-in-jar 的模组路径常常不在默认文件系统上（`FileSystems.newFileSystem` 打开的 zip 里），`Path.toFile()` 会抛 `UnsupportedOperationException`。所以读 jar 一律走流，路径只在确实是本地文件时才用；读取入口统一在 `ModFiles`（`isPlainFile` / `contains` / `readAllBytes`），OBA 自己也不去开 zip 文件系统。这样内嵌模组算独立来源，能单独索引、单独背锅。

## 归因与置信度

栈帧落到模组只有两条路：类全名命中索引，或者包前缀命中索引里唯一/少数几个模组。两条都走不通时，结论里写「没有栈帧能归属到某个模组」，只保留规则本身的建议——这比硬点一个模组要好。

置信度是加权和，不是概率。每帧的基础分是 `0.30 + 0.10 × 深度`，再乘命中规则自身的可信度：根因帧再加
0.85，异常链上的其它帧加 0.35；帧落在库代码里，或者只靠包片段对得上，整体乘 0.45 折价。日志正文里
出现模组 id 或文件名，另外补一项证据分。

分数 ≥ 2.0 记「极高」、≥ 0.9「高」、≥ 0.45「中」，其余「低」。界面上的百分比是 `分数 / 2.5` 封顶 99%，
所以最高显示 99% 而不是 100%——分数是累加的权重，不是概率。低于最低一档时不给元凶，只写「没有栈帧
落在某个模组上」。

## 规则库

{rules_block}

## 误报是怎么排掉的

- **库命名空间**：`org.jetbrains.annotations`、`kotlin`、`com.google`、`org.apache`、`io.netty`、`org.slf4j` 这些包里的类找不到，本身不代表问题（很多模组忘了打 `provided` 依赖）。这类缺失不报，除非类名以 `net.minecraft.client.` 开头。
- **平台 jar**：见上，索引里没有它们，加载器就不会被当元凶。
- **采样器名不是模组**：着色器报 `could not find sampler named Sampler2` 时，`Sampler2` 是着色器自己起的名字，不能拿它查模组。这条规则只取「采样器缺失」这个事实。
- **纯警告不落盘**：只有警告级别、又没点到模组的命中，只在控制台播一次，不生成报告文件。否则「每帧一条」的图形警告会让人每次启动都多一份没人看的文件。
- **同一句只出一条**：比较之前先剥掉 `Caused by:`、`Exception message:`、`Suppressed:` 这些前缀，再把数字换成占位符。同一句日志换了个坐标或毫秒数，不会被算成两条问题。

## 跨版本适配

**轨道怎么切**：按「同一份引擎代码能编过、运行期行为一致」来切，而不是按小版本。1.20.1 与 1.20.2 之间 Forge 的映射和 reobf 都变了，1.20.5 前后 NeoForge 重做了 tick 事件和描述文件位置，1.21.1 与 26.2 的 Java 版本与构建插件也不一样。

**编译期**：

| 轨道 | 编译目标 | 构建插件 | 备注 |
|---|---|---|---|
| 1.20.1 Forge | Forge 47 线 | ForgeGradle 6 | 需要 reobf |
| 1.20.2~1.20.4 | Forge 48/49、NeoForge 20.2~20.4 | ForgeGradle 6 / NeoGradle userdev | Forge 侧需要 reobf，NeoForge 侧不需要 |
| 1.20.5~1.20.6 | Forge 50、NeoForge 20.5~20.6 | 同上 | Forge 取消 reobf；描述文件换位置 |
| 1.21.1 | Forge 52、NeoForge 21.1 | 同上 | |
| 26.2 | Forge 65、NeoForge 26.2 | ForgeGradle 7 / ModDevGradle 2 | Java 25；ForgeGradle 7 重写了 DSL |

**运行期探测**（不靠编译期常量，靠反射问一遍）：

- tick 事件：`event.tick.ServerTickEvent$Post`（新）与 `event.TickEvent$ServerTickEvent`（旧）两个类名依次 `Class.forName`，挂载走 `IEventBus.addListener(Class, Consumer)`，避免给抽象类 `Event` 注册监听器（那会直接抛 `IllegalArgumentException`）。
- 描述文件：`META-INF/neoforge.mods.toml` 与 `META-INF/mods.toml` 三份模板——Forge 用 `mods-forge.toml`（打出来叫 `mods.toml`），NeoForge 侧按版本选 `mods.toml` 或
  `neoforge.mods.toml`；构建时按轨道挑一份。
- 客户端/服务端判定：只读 `FMLEnvironment.dist`，或者读 `META-INF` 里的加载器标记。绝不 `Class.forName("net.minecraft.client.Minecraft")`——在服务端上这行会触发 `RuntimeDistCleaner`，抛出的错误还会被 OBA 自己当成「服务端装了仅客户端模组」报出来。

## 处理得了的情况

| 现象 | OBA 怎么做 | 靠什么 |
|---|---|---|
{cases}

## 游戏内界面

`/oba` 是一棵命令树，等价于把 CLI 搬进聊天栏：`help`、`status`、`report`、`mods`、`mcreator`、
`analyze`、`crash`、`info`，外加两个排障用的 `reindex`（丢掉索引重扫模组）和 `threads`（把线程栈写进报告）。每个可点的条目挂 `ClickEvent`，点一下就执行。

聊天栏不是等宽字体：空格 4 px、小写字母约 6 px、汉字 9 px、粗体每字多 1 px。终端那份 100 列的排版直接塞进聊天栏会被折得七零八落，所以进游戏前会按像素宽度重排（`GameUi.lines`），分隔线用制表符系列字符压成一条。

## 崩溃之后的附属进程

有些崩溃来不及说话：崩在原生库、被任务管理器结束、加载阶段就断了。这时游戏里的代码一行都跑不到，聊天栏里刚播的结论也跟着消失。

OBA 的处理是：客户端启动时用 `javaw -cp <同一个 jar>` 再起一个 JVM，入口 `dev.oba.companion.CompanionMain`，classpath 就是 OBA 自己这个 jar，没有单独的 exe，也不用把界面解到临时目录。它只做一件事——等游戏进程消失，读交接文件，开窗。

| 这一场的情况 | 顶栏 | 弹窗 |
|---|---|---|
| 没有错误，正常退出 | 正常退出 | 不弹 |
| 捕获过错误但没崩 | 出过错误 | 弹 |
| 崩了（有新崩溃报告，或交接文件标了） | 崩溃 | 弹并抢前台 |
| 崩在 OBA 构造之前 | —— | 什么都没有 |

交接文件写在 `logs/oba-companion/handoff-*.json`：一句话结论、元凶、问题清单（含规则名）、时间线、环境、报告全文。窗口左边是结论/元凶/问题清单，右边是报告、时间线、环境、原始 JSON 四个页签。

窗口本身是毛玻璃效果：背后一块屏幕截图缩到四分之一再模糊，铺一层半透明着色，外层圆角裁剪，外圈加软阴影和一道高光边。Windows 没有公开的「背景模糊」接口，所以底图是截出来的——挪动窗口时会重新抓一块。

开关 `-Doba.companion=off|auto|always`（或 `OBA_COMPANION`），默认 `auto`。不想等崩溃：`java -cp <jar> dev.oba.companion.CompanionMain --demo`。

## 命令行

```text
java -jar oba-mc1.21.1-1.2.0.jar analyze <日志或目录>
java -jar oba-mc1.21.1-1.2.0.jar mods <游戏目录>
java -jar oba-mc1.21.1-1.2.0.jar mcreator <工程目录>
java -jar oba-mc1.21.1-1.2.0.jar watch <游戏目录>
java -jar oba-mc1.21.1-1.2.0.jar version
```

`analyze` 不带路径时读当前目录的 `logs/latest.log` 与 `crash-reports/`。常用开关：`--json` 出机器可读
结果、`--md` 出 Markdown、`--lang=zh|en` 换语言、`--game-dir` / `--mods-dir` 指定目录、`--pick` 只
挑某个模组的证据、`--menu` 直接进交互菜单、`--quiet` 只留结论。输出带颜色，`--color=always|never|auto`
控制，重定向到文件时自动关掉。

## 构建

```bash
./gradlew jar            # 打进 dist/
pwsh tools/build-all.ps1 # 全部轨道
```

需要 JDK 17（1.20.1 及 1.20.2~1.20.4 的 Forge）、21（NeoForge 1.20.5+、Forge 1.20.5+）、25（26.2）。Gradle 首次构建要从 `maven.neoforged.net` / `maven.minecraftforge.net` 拉依赖；本机走代理时在 `gradle.properties` 里补 `systemProp.https.proxyHost/Port`。

## 目录结构

```text
engine/main/java/dev/oba/
├── core/     规则、归因、报告、文案
├── mod/      游戏内命令、聊天栏排版、采集器、附属进程拉起
├── cli/      命令行入口
├── companion/ 崩溃后窗口（同一 jar 的第二 JVM）
└── loader/   加载器无关接口
src/loader/{{forge,neoforge,forge262}}/  各轨道的适配实现
versions/<轨道>/                        每条轨道自包含的构建目录
```

## 已知限制

- 崩在 OBA 构造之前（模组发现、依赖校验就失败）既没有窗口也没有报告。下一次启动才会从 `crash-reports/` 补读。
- 归因靠栈帧和规则，逻辑层抖出来的一句「NPE」如果四个帧全落在 Minecraft 自己的代码里，只能给建议，指不到模组。
- 看门狗依赖 tick 事件：`ServerTickEvent$Post` 与 `TickEvent$ServerTickEvent` 两个类名都探不到时，会在日志里打一行告警并停用卡死检测，其余功能照常。九条轨道目前都探到了。
- 26.1 与 26.3 没有出包。
- 服务端加载的是 NeoForge 26.2 自带的一串 appender 报错（netty 找不到 kqueue/epoll 原生库），OBA 不会把它算成崩溃，但它会出现在日志里。

## 许可

GPL-3.0-only。致谢见 `LICENSE` 与仓库提交历史。
"""
