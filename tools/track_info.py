"""每条版本轨道自己的事实。

只放「这一条轨道」的数据：目标版本、加载器、Java、产物、出没出包、实机跑成什么样、
还剩什么问题。根 README 是全量文档；版本 README 和版本 CHANGELOG 都从这里取事实，
保证七份分支文档互相之间不串版本、不串轨道。

改版本、改状态就改这里，然后重跑：

    python tools/gen-version-readmes.py
    python tools/gen-track-changelogs.py
    python tools/gen-version-trees.py
"""

TRACKS = {
    "1.21.1-neoforge": dict(
        mc_zh="1.21 ~ 1.21.x",
        mc_en="1.21 – 1.21.x",
        loader_zh="**NeoForge** 21.1 线",
        loader_en="**NeoForge**, 21.1 line",
        java="21",
        target="1.21.1 / 21.1.256",
        toml="META-INF/neoforge.mods.toml",
        jar="oba-mc1.21.1-1.1.0.jar",
        built=True,
        verified=True,
        new_in_110=False,
        note_zh="覆盖 1.21.1 和 21.x 的 NeoForge。NeoForge 从 1.20.5 起重做过 tick 事件，"
                "tick 钩子改成走 `IEventBus.addListener(Class, Consumer)` 反射挂载，实机已经能挂上；1.21.2 以后还没进游戏试过。",
        note_en="Covers 1.21.1 and the 21.x line of NeoForge. NeoForge reworked its tick events in "
                "1.20.5; the hook now registers through `IEventBus.addListener(Class, Consumer)` and attaches in game. "
                "NeoForge 1.21.2 and later has not been tried in game.",
        adapter_zh="`src/main/java/dev/oba/loader/neoforge/`：`NeoForgeEnv` 读模组列表和游戏目录，"
                   "`NeoForgeTickHook` 挂服务器 tick。",
        adapter_en="`src/main/java/dev/oba/loader/neoforge/`: `NeoForgeEnv` reads the mod list and "
                   "the game directory, `NeoForgeTickHook` hooks the server tick.",
        tick_zh="NeoForge 在 1.20.5 前后重做过 tick 事件，`NeoForgeTickHook` 用 `Class.forName` "
                "探两套类：`net.neoforged.neoforge.event.tick.ServerTickEvent$Post` 和老的 "
                "`net.neoforged.neoforge.event.TickEvent$ServerTickEvent`。两套类名都能探到（实机 21.1.262 上走的是新类名），"
                "挂载不经过 `bus.register`，避免给抽象类 `Event` 注册监听器。",
        tick_en="NeoForge rebuilt its tick events around 1.20.5, so `NeoForgeTickHook` probes two "
                "class names with `Class.forName`: `event.tick.ServerTickEvent$Post` and the older "
                "`event.TickEvent$ServerTickEvent`. Both resolve on this track; registration goes through "
                "`IEventBus.addListener(Class, Consumer)`, so nothing is registered against the "
                "abstract `Event` class.",
        verify_zh=[
            "专用服务端 NeoForge 21.1.262 上跑通：能加载、能停服；装 lithium / modernfix / ferritecore / "
            "Clumps 四个真实模组后索引 5 个文件 / 1114 个类 / 364 个包 / 38 ms，退出时正常出报告。",
            "装一个 common 类里引用 `net.minecraft.client.gui.screens.Screen` 的探针模组：崩在模组加载阶段，"
            "退出时兜底写出的报告点名探针模组、可信度 高 (55%)，问题清单首条是「服务端装了仅客户端模组」。",
            "1.21.1 + NeoForge 21.1.256，16 个真实模组：`[OBA] enabled: 1.1.0`，"
            "索引 17 个模组文件 / 4899 个类 / 1150 个包 / 100 ms，进主菜单约 20 秒。",
        ],
        verify_en=[
            "Runs on a dedicated NeoForge 21.1.262 server: starts and stops cleanly; with lithium, "
            "modernfix, ferritecore and Clumps installed it indexes 5 files / 1114 classes / 364 "
            "packages in 38 ms and still writes the shutdown report.",
            "A probe mod whose common class touches `net.minecraft.client.gui.screens.Screen` dies "
            "during mod loading; the report written on shutdown names it with confidence high (55%), "
            "first finding \"a client-only mod is installed on a server\".",
            "1.21.1 + NeoForge 21.1.256 with 16 real mods: `[OBA] enabled: 1.1.0`, indexed 17 mod "
            "files / 4899 classes / 1150 packages in 100 ms, main menu in about 20 s.",
        ],
        issues_zh=[
            "1.21.2 及以后的 NeoForge 没有实机验证过。",
        ],
        issues_en=[
            "NeoForge 1.21.2 and later has not been verified in game.",
        ],
    ),
    "1.20.5-1.20.6-neoforge": dict(
        mc_zh="1.20.5 / 1.20.6",
        mc_en="1.20.5 / 1.20.6",
        loader_zh="**NeoForge** 20.5 线，构建时对 20.5.21-beta",
        loader_en="**NeoForge**, 20.5 line, built against 20.5.21-beta",
        java="21",
        target="1.20.5 / 20.5.21-beta",
        toml="META-INF/neoforge.mods.toml",
        jar="oba-mc1.20.5-1.20.6-1.1.0.jar",
        built=True,
        verified=True,
        new_in_110=False,
        note_zh="同一个 jar 同时管 1.20.5 和 1.20.6：这两个小版本的模组接口没变，"
                "变的只是模组描述文件名和依赖版本号。",
        note_en="One jar covers both 1.20.5 and 1.20.6: the mod-facing API did not change between "
                "them, only the descriptor file name and the dependency versions did.",
        adapter_zh="`src/main/java/dev/oba/loader/neoforge/`：`NeoForgeEnv` 读模组列表和游戏目录，"
                   "`NeoForgeTickHook` 挂服务器 tick（两套事件都探）。",
        adapter_en="`src/main/java/dev/oba/loader/neoforge/`: `NeoForgeEnv` reads the mod list and "
                   "the game directory, `NeoForgeTickHook` hooks the server tick (both class "
                   "layouts are probed).",
        tick_zh="20.6 起 tick 事件是 `net.neoforged.neoforge.event.tick.ServerTickEvent$Post`，"
                "`NeoForgeTickHook` 先探这个，探不到再退到老的 `event.TickEvent$ServerTickEvent`。",
        tick_en="From 20.6 the tick event is `event.tick.ServerTickEvent$Post`; "
                "`NeoForgeTickHook` probes that first and falls back to the older "
                "`event.TickEvent$ServerTickEvent`.",
        verify_zh=[
            "1.20.5 + NeoForge 20.5.21-beta，13 个真实模组：`[OBA] enabled: 1.1.0`，"
            "索引 13 个模组文件 / 3060 个类 / 612 个包 / 77 ms，进主菜单约 25 秒。",
            "1.20.6 + NeoForge 20.6.x，15 个真实模组：索引 16 个模组文件 / 4121 个类 / "
            "821 个包 / 90 ms，进主菜单约 24 秒。",
        ],
        verify_en=[
            "1.20.5 + NeoForge 20.5.21-beta with 13 real mods: `[OBA] enabled: 1.1.0`, indexed 13 "
            "mod files / 3063 classes / 612 packages in 81 ms, main menu in about 25 s.",
            "1.20.6 + NeoForge 20.6.x with 15 real mods: indexed 16 mod files / 4124 classes / "
            "821 packages in 98 ms, main menu in about 24 s.",
        ],
        issues_zh=[
            "从 20.5 起模组描述文件名是 `META-INF/neoforge.mods.toml`，构建脚本按这个名字打包；"
            "把它改成老名字加载器就认不出来。",
            "1.20.6 线的模组装进 1.20.5 实例会当场崩：崩溃报告里写 "
            "`Mod supermartijn642configlib requires neoforge 20.6.119 or above`。"
            "两个小版本要按各自的线挑模组。",
        ],
        issues_en=[
            "From 20.5 the descriptor is `META-INF/neoforge.mods.toml` and the build ships it under "
            "that name; renaming it back to the old name makes the loader ignore the jar.",
            "A mod built for the 20.6 line crashes a 1.20.5 instance outright: the crash report says "
            "`Mod supermartijn642configlib requires neoforge 20.6.119 or above`. Pick mods per line.",
        ],
    ),
    "1.20.2-1.20.4-neoforge": dict(
        mc_zh="1.20.2 / 1.20.3 / 1.20.4",
        mc_en="1.20.2 / 1.20.3 / 1.20.4",
        loader_zh="**NeoForge** 20.2 线，构建时对 20.2.93",
        loader_en="**NeoForge**, 20.2 line, built against 20.2.93",
        java="17",
        target="1.20.2 / 20.2.93",
        toml="META-INF/mods.toml",
        jar="oba-mc1.20.2-1.20.4-1.1.0.jar",
        built=True,
        verified=True,
        new_in_110=False,
        note_zh="NeoForge 在 20.2 ~ 20.4 还不叫 NeoForge 自己的描述文件格式，用的是从 Forge 继承下来的 "
                "`mods.toml`。这三个小版本共用一份构建。",
        note_en="In 20.2 – 20.4 NeoForge still used the descriptor format inherited from Forge, "
                "`mods.toml`. One build covers all three minor versions.",
        adapter_zh="`src/main/java/dev/oba/loader/neoforge/`：`NeoForgeEnv` 读模组列表和游戏目录，"
                   "`NeoForgeTickHook` 挂服务器 tick（这条轨道走老的那套事件）。",
        adapter_en="`src/main/java/dev/oba/loader/neoforge/`: `NeoForgeEnv` reads the mod list and "
                   "the game directory, `NeoForgeTickHook` hooks the server tick (the older event "
                   "class on this track).",
        tick_zh="这条轨道上的 tick 事件是 `net.neoforged.neoforge.event.TickEvent$ServerTickEvent`，"
                "带 `Phase`（START / END），一次 tick 触发两回，只认 END 才能和「一次 tick 一回」对上。",
        tick_en="The tick event here is `event.TickEvent$ServerTickEvent` with a `Phase` "
                "(START / END): it fires twice per tick, and only END matches the "
                "one-call-per-tick rhythm the watchdog expects.",
        verify_zh=[
            "1.20.2 + NeoForge 20.2.93，14 个真实模组：`[OBA] enabled: 1.1.0`，"
            "索引 16 个模组文件 / 3117 个类 / 644 个包 / 86 ms，进主菜单约 21 秒。",
            "1.20.3 + NeoForge 20.3.8-beta，10 个真实模组：索引 10 个模组文件 / 1802 个类 / "
            "383 个包 / 68 ms，进主菜单约 19 秒（这台机器要 `ALSOFT_DRIVERS=null`）。",
            "1.20.4 + NeoForge 20.4.x，10 个真实模组：索引 10 个模组文件 / 3056 个类 / "
            "568 个包 / 94 ms，进主菜单约 20 秒。",
            "专用服务端 NeoForge 20.2.93 上装一个 common 类里引用 "
            "`net.minecraft.client.gui.screens.Screen` 的探针模组：崩在模组加载阶段，"
            "退出时兜底写出的报告点名探针模组、可信度 高 (55%)，问题清单首条是「服务端装了仅客户端模组」。",
        ],
        verify_en=[
            "1.20.2 + NeoForge 20.2.93 with 14 real mods: `[OBA] enabled: 1.1.0`, indexed 16 mod "
            "files / 3117 classes / 644 packages in 86 ms, main menu in about 21 s.",
            "1.20.3 + NeoForge 20.3.8-beta with 10 real mods: indexed 10 mod files / 1802 classes / "
            "383 packages in 68 ms, main menu in about 19 s (needs `ALSOFT_DRIVERS=null` on this machine).",
            "1.20.4 + NeoForge 20.4.x with 10 real mods: indexed 10 mod files / 3056 classes / "
            "568 packages in 94 ms, main menu in about 20 s.",
            "On a dedicated NeoForge 20.2.93 server a probe mod whose common class touches "
            "`net.minecraft.client.gui.screens.Screen` dies during mod loading; the report written on "
            "shutdown names it with confidence high (55%), first finding \"a client-only mod is "
            "installed on a server\".",
        ],
        issues_zh=[
            "描述文件是 `META-INF/mods.toml`：`[[dependencies]]` 里的 `mandatory` 字段必须写，"
            "而且必须排在 `type` 前面，否则加载器直接报解析失败。",
            "1.20.4 编译的模组装进 1.20.2 / 1.20.3，会让整个 mods 目录被判 invalid"
            "（1.20.4 起 `mandatory` 被删掉，而 20.2~20.3 还要求它）：一个这样的 jar 就够让所有模组"
            "都不加载，看起来像「装了模组但一个都没生效」。OBA 认这条日志（`loader.invalid_mod_file`）。",
            "本机 1.20.3 的 USB 声卡要 `ALSOFT_DRIVERS=null` 才过得了音频初始化"
            "（`jstack` 停在 `alcResetDeviceSOFT`），和 OBA 无关。",
        ],
        issues_en=[
            "The descriptor is `META-INF/mods.toml`: every `[[dependencies]]` block needs "
            "`mandatory`, and it has to come before `type`, otherwise the loader reports a parse "
            "failure.",
            "A jar built for 1.20.4 makes the loader reject the whole mods folder on 1.20.2 / "
            "1.20.3 (1.20.4 dropped `mandatory`, 20.2 – 20.3 still require it): one such jar "
            "keeps every mod from loading, which looks like 'installed but none active'. OBA "
            "recognises that log line (`loader.invalid_mod_file`).",
            "On this machine 1.20.3 needs `ALSOFT_DRIVERS=null` to get past audio init "
            "(`jstack` stopped in `alcResetDeviceSOFT`) — nothing to do with OBA.",
        ],
    ),
    "1.20.1-forge": dict(
        mc_zh="1.20.1",
        mc_en="1.20.1",
        loader_zh="**Forge** 47 线，构建时对 47.4.26",
        loader_en="**Forge**, 47 line, built against 47.4.26",
        java="17",
        target="1.20.1 / 47.4.26",
        toml="META-INF/mods.toml",
        jar="oba-mc1.20.1-forge-1.1.0.jar",
        built=True,
        verified=True,
        new_in_110=False,
        note_zh="1.20.1 那会儿还没有 NeoForge，只有 Forge 一条。这条轨道用 ForgeGradle 构建："
                "Forge 1.20.1 跑的是 SRG 命名，本机客户端 jar 又是混淆的，"
                "得先过一整条反混淆流水线才有能对着写代码的 jar。",
        note_en="There was no NeoForge back in 1.20.1, so Forge is the only option here. The build "
                "uses ForgeGradle: Forge 1.20.1 runs on SRG names and the client jar is obfuscated, "
                "so a full deobfuscation pass has to run before any code can be compiled against it.",
        adapter_zh="`src/main/java/dev/oba/loader/forge/`：`ForgeEnv` 读模组列表和游戏目录，"
                   "`ForgeTickHook` 挂服务器 tick。",
        adapter_en="`src/main/java/dev/oba/loader/forge/`: `ForgeEnv` reads the mod list and the "
                   "game directory, `ForgeTickHook` hooks the server tick.",
        tick_zh="Forge 1.20.1 只有一套 tick 事件 `net.minecraftforge.event.TickEvent$ServerTickEvent`，"
                "编译期就确定，不用反射，直接 `MinecraftForge.EVENT_BUS.register`；"
                "START 和 END 都会进来，只在 END 记一笔。",
        tick_en="Forge 1.20.1 has a single tick event, "
                "`net.minecraftforge.event.TickEvent$ServerTickEvent`. It is known at compile time, "
                "so no reflection is needed: the hook registers on `MinecraftForge.EVENT_BUS` and "
                "only counts `Phase.END`.",
        verify_zh=[
            "1.20.1 + Forge 47.4.26，16 个真实模组：`[OBA] enabled: 1.1.0`，"
            "索引 18 个模组文件 / 4958 个类 / 924 个包 / 186 ms，进主菜单约 28 秒。",
            "同一轮里显卡驱动每帧报一条 shader 缺采样器的错误，1.1.0 的节流把它压成整场只播一次，"
            "而且这种只有警告、又没点到任何模组的命中不再单独写报告文件。",
            "专用服务端 Forge 1.20.1-47.4.28 上装一个 common 类里引用 "
            "`net.minecraft.client.gui.screens.Screen` 的探针模组：崩在模组加载阶段，控制台上什么都没有，"
            "退出时兜底写出的报告点名探针模组、可信度 高 (55%)，问题清单首条是「服务端装了仅客户端模组」。",
        ],
        verify_en=[
            "1.20.1 + Forge 47.4.26 with 16 real mods: `[OBA] enabled: 1.1.0`, indexed 18 mod "
            "files / 4958 classes / 924 packages in 186 ms, main menu in about 28 s.",
            "In the same run the graphics driver printed a missing-sampler error every frame; the "
            "1.1.0 throttle reduced it to a single announcement, and warning-only findings that "
            "blame no mod no longer get a report file of their own.",
            "On a dedicated Forge 1.20.1-47.4.28 server a probe mod whose common class touches "
            "`net.minecraft.client.gui.screens.Screen` dies during mod loading; the console shows nothing, "
            "but the report written on shutdown names it with confidence high (55%), first finding "
            "\"a client-only mod is installed on a server\".",
        ],
        issues_zh=[
            "必须用 JDK 17 构建；首次构建要下载并反混淆整个 1.20.1，需要能访问 "
            "`maven.minecraftforge.net`，耗时几分钟。",
            "启动阶段的 shader 报错本身是显卡那边的事，OBA 只是不再重复播报它。",
        ],
        issues_en=[
            "Needs JDK 17 to build; the first build downloads and deobfuscates all of 1.20.1, which "
            "takes a few minutes and needs `maven.minecraftforge.net` to be reachable.",
            "The shader error at startup is the driver's business; OBA only stops repeating it.",
        ],
    ),
    "1.21.1-forge": dict(
        mc_zh="1.21.1",
        mc_en="1.21.1",
        loader_zh="**Forge** 52 线，构建时对 52.1.20",
        loader_en="**Forge**, 52 line, built against 52.1.20",
        java="21",
        target="1.21.1 / 52.1.20",
        toml="META-INF/mods.toml",
        jar="oba-mc1.21.1-forge-1.1.0.jar",
        built=True,
        verified=True,
        status_zh="已出包，客户端与服务端都验证过",
        status_en="built, verified on client and server",
        new_in_110=True,
        note_zh="1.1.0 新开的一条轨道：1.21.1 上有人还在用 Forge，而这个版本的 Forge 与 NeoForge 的"
                "事件体系已经分家，得单独一份加载器适配。",
        note_en="Opened in 1.1.0: some people still run Forge on 1.21.1, and by this version Forge "
                "and NeoForge have separate event systems, so this needs its own loader adapter.",
        adapter_zh="`src/main/java/dev/oba/loader/forge/`：`ForgeEnv` 读模组列表和游戏目录，"
                   "`ForgeTickHook` 挂服务器 tick。",
        adapter_en="`src/main/java/dev/oba/loader/forge/`: `ForgeEnv` reads the mod list and the "
                   "game directory, `ForgeTickHook` hooks the server tick.",
        tick_zh="适配层沿用 Forge 的 `net.minecraftforge.event.TickEvent$ServerTickEvent`：编译期直接挂，"
                "只在 `Phase.END` 记一笔。这条轨道在客户端和专用服务端上都跑过，这一段有实机日志。",
        tick_en="The adapter keeps Forge's `net.minecraftforge.event.TickEvent$ServerTickEvent`: it "
                "subscribes at compile time and counts only `Phase.END`. This track ran on both a "
                "client and a dedicated server, so that path has real logs.",
        verify_zh=[
            "Forge 1.21.1 客户端（9 个真实 Forge 模组，10 个 jar）：`索引完成：13 个模组文件 / "
            "3633 个类 / 660 个包（115 ms）`，23.5 秒进主菜单，横幅和 `/oba` 提示正常。",
            "专用服务端（Forge 1.21.1-52.1.20，装 8 个真实 Forge 模组）：`索引完成：11 个模组文件 / "
            "3343 个类 / 611 个包（86 ms）`，14 秒进到 `Done`，`stop` 正常退出。",
            "同一个服务端换成仅客户端探针：崩在构造阶段，退出时兜底写出的报告点名 `Clientonly probe`"
            "（可信度 55%），问题清单只有一条「服务端装了仅客户端模组」。",
        ],
        verify_en=[
            "Forge 1.21.1 client (nine real Forge mods, ten jars): `13 mod files / 3633 classes / "
            "660 packages (115 ms)`, main menu in 23.5 s, banner and `/oba` hint present.",
            "Dedicated server (Forge 1.21.1-52.1.20, eight real Forge mods): `11 mod files / 3343 "
            "classes / 611 packages (86 ms)`, `Done` in 14 s, clean `stop`.",
            "Same server with the client-only probe: it crashed in its constructor, and the report "
            "written on shutdown named `Clientonly probe` (55%) with exactly one finding, "
            "\"a client-only mod is installed on a server\".",
        ],
        issues_zh=[
            "**游戏内界面只验到「能进主菜单」这一步**：`/oba` 菜单、报告分页、聊天栏这几块还没有逐项截图核对。",
            "版本区间收在 `[1.21.1, 1.21.2)`：Forge 的 1.21 是 51 线、1.21.1 才是 52 线，"
            "而 jar 里写的是 `loaderVersion=\"[52,)\"`，装到 1.21 上会被当成加载器版本不够。",
            "1.21.2 及以后没有对应的 Forge 线，这条轨道不覆盖。",
        ],
        issues_en=[
            "**In-game screens are only verified up to \"the main menu loads\"**: the `/oba` menu, "
            "report pagination and chat lines have not been checked one by one on this track.",
            "The range is pinned to `[1.21.1, 1.21.2)`: Forge 1.21 is the 51 line and only 1.21.1 "
            "is 52, while the jar declares `loaderVersion=\"[52,)\"`, so 1.21 would reject it.",
            "There is no Forge line for 1.21.2 and later, so this track does not cover them.",
        ],
    ),
    "1.20.5-1.20.6-forge": dict(
        mc_zh="1.20.6（Forge 没有 1.20.5）",
        mc_en="1.20.6 (Forge never shipped 1.20.5)",
        loader_zh="**Forge** 50 线，构建时对 50.2.14",
        loader_en="**Forge**, 50 line, built against 50.2.14",
        java="21",
        target="1.20.6 / 50.2.14",
        toml="META-INF/mods.toml",
        jar="oba-mc1.20.5-1.20.6-forge-1.1.0.jar",
        built=True,
        verified=True,
        status_zh="已出包，客户端与服务端都验证过",
        status_en="built, verified on client and server",
        new_in_110=True,
        note_zh="1.1.0 新开的一条轨道：1.20.5 / 1.20.6 上还有整合包留在 Forge 50 线，"
                "所以单独编一份。Forge 官方没有 1.20.5 的构建，实际覆盖的是 1.20.6。",
        note_en="Opened in 1.1.0: some packs stayed on the Forge 50 line for 1.20.5 / 1.20.6, so "
                "this is a separate build. Forge never shipped a 1.20.5 build, so in practice it "
                "covers 1.20.6.",
        adapter_zh="`src/main/java/dev/oba/loader/forge/`：`ForgeEnv` 读模组列表和游戏目录，"
                   "`ForgeTickHook` 挂服务器 tick。",
        adapter_en="`src/main/java/dev/oba/loader/forge/`: `ForgeEnv` reads the mod list and the "
                   "game directory, `ForgeTickHook` hooks the server tick.",
        tick_zh="适配层沿用 Forge 的 `net.minecraftforge.event.TickEvent$ServerTickEvent`：编译期直接挂，"
                "只在 `Phase.END` 记一笔。这条轨道在 1.20.6 的客户端和专用服务端上都跑过。",
        tick_en="The adapter keeps Forge's `net.minecraftforge.event.TickEvent$ServerTickEvent`: it "
                "subscribes at compile time and counts only `Phase.END`. This track has run on both "
                "the 1.20.6 client and a dedicated 1.20.6 server.",
        verify_zh=[
            "Forge 1.20.6 客户端（11 个真实 Forge 模组）：`索引完成：15 个模组文件 / 2702 个类 / "
            "418 个包（92 ms）`，23.5 秒进主菜单，`/oba` 提示正常。",
            "同一轮把 balm 读配置时的 `ClassCastException` 定位回 balm（极高 99%）。",
            "Forge 1.20.6 专用服务端（同样 6 个真实 Forge 模组）：`索引完成：9 个模组文件 / "
            "2348 个类 / 368 个包（83 ms）`，14 秒进到 `Done`，`stop` 正常退出。",
            "服务端那轮同一条 balm 配置错误只播一次：两行 `ClassCastException` 消息里的类名不同，"
            "播报键只取异常类型，所以第二条被算成重复。",
        ],
        verify_en=[
            "Forge 1.20.6 client (eleven real Forge mods): `15 mod files / 2702 classes / 418 "
            "packages (92 ms)`, main menu in 23.5 s, `/oba` hint present.",
            "The same run pinned balm's config `ClassCastException` back on balm (99%).",
            "Dedicated Forge 1.20.6 server (the same six real Forge mods): `9 mod files / 2348 "
            "classes / 368 packages (83 ms)`, `Done` in 14 s, clean `stop`.",
            "On the server the same balm config error was announced once: the two "
            "`ClassCastException` lines name different classes, and the announce key keeps only "
            "the exception type, so the second one counted as a repeat.",
        ],
        issues_zh=[
            "**客户端与服务端各跑过一轮，但都只覆盖 1.20.6**：Forge 没有 1.20.5 的构建，1.20.5 这一侧无实测。",
            "**Forge 官方没有 1.20.5 的构建**（metadata 里 1.20.5 线是 0 个版本），"
            "所以这条轨道实际只覆盖 1.20.6；名字里的 1.20.5 只是沿用这个版本区间的写法。",
            "jar 里写的是 `loaderVersion=\"[50,)\"`，而 1.20.5 上本来也没有可用的 Forge 50 构建。",
        ],
        issues_en=[
            "**One client run and one server run, both on 1.20.6 only**: Forge has no 1.20.5 build, "
            "so that half of the range has no in-game evidence.",
            "**Forge never shipped a 1.20.5 build** (its metadata has zero 1.20.5 versions), so this "
            "track really only covers 1.20.6; the 1.20.5 in the name follows the version range.",
            "The jar declares `loaderVersion=\"[50,)\"`, and there is no Forge 50 build for 1.20.5 "
            "to load it anyway.",
        ],
    ),
    "1.20.2-1.20.4-forge": dict(
        mc_zh="1.20.2 / 1.20.3 / 1.20.4",
        mc_en="1.20.2 / 1.20.3 / 1.20.4",
        loader_zh="**Forge** 49 线，构建时对 49.2.13",
        loader_en="**Forge**, 49 line, built against 49.2.13",
        java="17",
        target="1.20.4 / 49.2.13",
        toml="META-INF/mods.toml",
        jar="oba-mc1.20.2-1.20.4-forge-1.1.0.jar",
        built=True,
        verified=True,
        status_zh="已出包，客户端与服务端都验证过",
        status_en="built, verified on client and server",
        new_in_110=True,
        note_zh="1.1.0 新开的一条轨道：这三个小版本当时是 Forge 与 NeoForge 并存的时期，"
                "有人用 Forge 49 线，所以单独编一份。",
        note_en="Opened in 1.1.0: Forge and NeoForge overlapped on these three minor versions, and "
                "some packs stayed on the Forge 49 line, so it gets its own build.",
        adapter_zh="`src/main/java/dev/oba/loader/forge/`：`ForgeEnv` 读模组列表和游戏目录，"
                   "`ForgeTickHook` 挂服务器 tick。",
        adapter_en="`src/main/java/dev/oba/loader/forge/`: `ForgeEnv` reads the mod list and the "
                   "game directory, `ForgeTickHook` hooks the server tick.",
        tick_zh="适配层沿用 Forge 的 `net.minecraftforge.event.TickEvent$ServerTickEvent`：编译期直接挂，"
                "只在 `Phase.END` 记一笔。这条轨道在专用服务端和 1.20.4 客户端上都跑过，这一段有实机日志。",
        tick_en="The adapter keeps Forge's `net.minecraftforge.event.TickEvent$ServerTickEvent`: it "
                "subscribes at compile time and counts only `Phase.END`. This track ran on both a "
                "dedicated server and the 1.20.4 client, so that path has real logs.",
        verify_zh=[
            "专用服务端（Forge 1.20.2-48.1.0，装 6 个真实 Forge 模组）：`索引完成：8 个模组文件 / "
            "2013 个类 / 362 个包（64 ms）`，16 秒进到 `Done`，`stop` 正常退出。",
            "同一个服务端换成仅客户端探针：崩在构造阶段，退出时兜底写出的报告点名 `Clientonly probe`"
            "（可信度 55%），问题清单只有一条「服务端装了仅客户端模组」。",
            "Forge 1.20.4 客户端（10 个真实 Forge 模组）：`索引完成：12 个模组文件 / 3095 个类 / "
            "576 个包（107 ms）`，23 秒进主菜单；balm 读配置时的 `ClassCastException` 被算回 balm（极高 99%）。",
        ],
        verify_en=[
            "Dedicated server (Forge 1.20.2-48.1.0, six real Forge mods): `8 mod files / 2013 "
            "classes / 362 packages (64 ms)`, `Done` in 16 s, clean `stop`.",
            "Same server with the client-only probe: it crashed in its constructor, and the report "
            "written on shutdown named `Clientonly probe` (55%) with exactly one finding, "
            "\"a client-only mod is installed on a server\".",
            "Forge 1.20.4 client (ten real Forge mods): `12 mod files / 3095 classes / 576 packages "
            "(107 ms)`, main menu in 23 s; balm's config `ClassCastException` was pinned on balm (99%).",
        ],
        issues_zh=[
            "**1.20.3 客户端与服务端都没跑过**：客户端只验了 1.20.4，服务端只验了 1.20.2。",
            "Forge 只有 1.20.2 与 1.20.4 两条线，没有 1.20.3；为了让 1.20.2 能装，"
            "`loaderVersion` 放宽到 `[48,)`，而不是按 1.20.4 的 49 线写。",
            "首次构建要下几 MB 的 userdev。Java 不读系统代理，直连只有 ~4 KB/s；"
            "卡住时放开 `gradle.properties` 里注释掉的代理行。",
        ],
        issues_en=[
            "**1.20.3 has not been run anywhere**: the client was only tested on 1.20.4 and the "
            "server only on 1.20.2.",
            "Forge only shipped 1.20.2 and 1.20.4 lines, never 1.20.3; `loaderVersion` is relaxed to "
            "`[48,)` so 1.20.2 accepts the jar instead of pinning it to the 49 line.",
            "The first build downloads several MB of userdev. Java ignores the system proxy and a "
            "direct connection runs at about 4 KB/s; uncomment the proxy lines in `gradle.properties` "
            "if it stalls.",
        ],
    ),
}

# 这一条轨道在 1.1.0 里动过的、和引擎无关的东西（构建、描述文件、源码同步）
TRACK_CHANGE_ZH = {
    "1.21.1-neoforge": [
        "构建脚本换成 `net.neoforged.gradle.userdev` 7.1.38，描述文件 `META-INF/neoforge.mods.toml`。",
        "`repositories` 里除了 `maven.neoforged.net` 还列了官方 CDN 镜像 "
        "`neoforged.forgecdn.net`，主站连不上时由它顶上。",
    ],
    "1.20.5-1.20.6-neoforge": [
        "构建脚本换成 `net.neoforged.gradle.userdev` 7.1.38，描述文件 `META-INF/neoforge.mods.toml`。",
        "这条轨道额外带 `mirror.init.gradle`，本机直连 `maven.neoforged.net` 超时时用它把请求"
        "转到官方 CDN。",
    ],
    "1.20.2-1.20.4-neoforge": [
        "构建脚本换成 `net.neoforged.gradle.userdev` 7.1.38，描述文件仍是 `META-INF/mods.toml`"
        "（NeoForge 20.2 ~ 20.4 用的是从 Forge 继承的格式）。",
        "这条轨道额外带 `mirror.init.gradle`，本机直连 `maven.neoforged.net` 超时时用它把请求"
        "转到官方 CDN。",
    ],
    "1.20.1-forge": [
        "描述文件统一成 `META-INF/mods-forge.toml`（内容全是 `${}` 占位符，构建时按 "
        "`gradle.properties` 展开成 `mods.toml`）。",
        "源码目录改成自包含：`src/main/java` 下就是这条轨道要编的全部文件，不依赖目录外的东西。",
    ],
    "1.21.1-forge": [
        "新开这条轨道：`build.gradle` 用 ForgeGradle 6，Java 21，产物名带 `mc1.21.1-forge`。",
        "描述文件 `META-INF/mods-forge.toml`，构建时展开成 `mods.toml`，"
        "`loader_version_range=[52,)`。",
    ],
    "1.20.5-1.20.6-forge": [
        "新开这条轨道：`build.gradle` 用 ForgeGradle 6，Java 21，产物名带 `mc1.20.5-1.20.6-forge`。",
        "描述文件 `META-INF/mods-forge.toml`，`loader_version_range=[50,)`，"
        "Minecraft 范围 `[1.20.5,1.20.7)`。",
    ],
    "1.20.2-1.20.4-forge": [
        "新开这条轨道：`build.gradle` 用 ForgeGradle 6，Java 17，产物名带 `mc1.20.2-1.20.4-forge`。",
        "描述文件 `META-INF/mods-forge.toml`，`loader_version_range=[49,)`，"
        "Minecraft 范围 `[1.20.2,1.20.5)`。",
    ],
}

TRACK_CHANGE_EN = {
    "1.21.1-neoforge": [
        "Build script moved to `net.neoforged.gradle.userdev` 7.1.38; descriptor is "
        "`META-INF/neoforge.mods.toml`.",
        "`neoforged.forgecdn.net` is listed as a mirror in `repositories`, for networks where "
        "`maven.neoforged.net` does not answer.",
    ],
    "1.20.5-1.20.6-neoforge": [
        "Build script moved to `net.neoforged.gradle.userdev` 7.1.38; descriptor is "
        "`META-INF/neoforge.mods.toml`.",
        "This track carries `mirror.init.gradle`, which redirects requests to the official CDN "
        "when `maven.neoforged.net` times out.",
    ],
    "1.20.2-1.20.4-neoforge": [
        "Build script moved to `net.neoforged.gradle.userdev` 7.1.38; the descriptor is still "
        "`META-INF/mods.toml`, the format NeoForge 20.2 – 20.4 inherited from Forge.",
        "This track carries `mirror.init.gradle`, which redirects requests to the official CDN "
        "when `maven.neoforged.net` times out.",
    ],
    "1.20.1-forge": [
        "Descriptor renamed to `META-INF/mods-forge.toml`: all placeholders, expanded into "
        "`mods.toml` at build time from `gradle.properties`.",
        "The source tree became self-contained: `src/main/java` holds every file this track "
        "compiles, with nothing pulled in from outside the directory.",
    ],
    "1.21.1-forge": [
        "New track: ForgeGradle 6, Java 21, artifact name suffixed `mc1.21.1-forge`.",
        "Descriptor `META-INF/mods-forge.toml` expands into `mods.toml` at build time, with "
        "`loader_version_range=[52,)`.",
    ],
    "1.20.5-1.20.6-forge": [
        "New track: ForgeGradle 6, Java 21, artifact name suffixed `mc1.20.5-1.20.6-forge`.",
        "Descriptor `META-INF/mods-forge.toml`, `loader_version_range=[50,)`, Minecraft range "
        "`[1.20.5,1.20.7)`.",
    ],
    "1.20.2-1.20.4-forge": [
        "New track: ForgeGradle 6, Java 17, artifact name suffixed `mc1.20.2-1.20.4-forge`.",
        "Descriptor `META-INF/mods-forge.toml`, `loader_version_range=[49,)`, Minecraft range "
        "`[1.20.2,1.20.5)`.",
    ],
}
