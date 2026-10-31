# One BUG Among Us · 1.20.2-1.20.4-forge

> 这个目录是**1.20.2 / 1.20.3 / 1.20.4**（Forge 49 线，构建时对 49.2.13）的一条独立轨道：源码、资源、构建脚本都在这里，单独拷出去也能构建。
> 仓库总览和完整的规则说明在[根 README](../../README.md)。

## 这一条轨道

| 项 | 值 |
|---|---|
| Minecraft | 1.20.2 / 1.20.3 / 1.20.4 |
| 加载器 | **Forge** 49 线，构建时对 49.2.13 |
| Java | 17 |
| 编译目标 | 1.20.4 / 49.2.13 |
| 描述文件 | `META-INF/mods.toml` |
| 产物 | `oba-mc1.20.2-1.20.4-forge-1.2.0.jar` |
| 状态 | 已出包，客户端与服务端都验证过 |

## 适配了哪些差异

- **描述文件**：`META-INF/mods.toml`。它是占位符模板，构建时把版本号展开再打进 jar；换成别的名字加载器就认不出来。
- **构建链**：net.minecraftforge.gradle [6.0.24,6.2)；需要 reobf：1.20.4 及更早的 Forge 用 SRG 成员名，构建最后一步要把类名换回 SRG。
- **tick 适配**：适配层沿用 Forge 的 `net.minecraftforge.event.TickEvent$ServerTickEvent`：编译期直接挂，只在 `Phase.END` 记一笔。这条轨道在专用服务端和 1.20.4 客户端上都跑过，这一段有实机日志。
- **适配层代码**：`src/main/java/dev/oba/loader/forge/`：`ForgeEnv` 读模组列表和游戏目录，`ForgeTickHook` 挂服务器 tick。

## 引擎怎么工作（所有轨道共用同一份代码）

1. 采集：log4j2 appender 接住 ERROR / FATAL；`crash-reports/` 里的新文件同样进来。
2. 建索引：扫 `mods/*.jar`，得到「类名 → 模组」与「包前缀 → 模组」两张表。
3. 匹配规则：49 条正则逐行跑，一行日志只出一条最具体的结论。
4. 认栈帧：每帧的类名先查索引，查不到用包前缀缩小范围。
5. 打分：离根因的距离、规则严重度、模组 id 是否出现在正文、多帧是否互相印证。
6. 出报告：一句话结论、元凶卡片（模组 / 文件 / 首帧 / 可信度）、问题清单、建议。

索引不能只信加载器给的路径（NeoForge 的联合文件系统路径不是普通文件，只按它扫 16 个模组只认出
3 个），所以还会直接扫 `mods/*.jar` 合并去重；`libraries/` 下的平台 jar 一律排除，否则结论会指到
加载器头上；建索引在后台线程池里跑，模组目录（文件名、大小、修改时间）没变就不重扫。

归因只走两条路：整类名在索引里，或包前缀在索引里。两条都不通时只给建议、不硬指模组。可信度按
信号加权（首帧落在该模组、异常链多帧指向同一处、规则是致命级、模组 id 出现在正文 → 加分；只有
包片段匹配、栈帧落在库代码 → 减分），低于阈值不下结论。

## 处理得了的情况

| 类别 | 规则数 | 例子 |
|---|---|---|
| 加载器与依赖 | 6 | `neoforge.missing_dependency`、`neoforge.missing_dependency_generic` |
| 模组文件本身 | 3 | `loader.invalid_mod_file`、`jar.corrupt` |
| Mixin | 6 | `mixin.apply_failed`、`mixin.apply_failed_alt` |
| 类加载与链接 | 4 | `class.noclassdef`、`class.classnotfound` |
| 注册表与资源 | 4 | `registry.missing`、`registry.duplicate` |
| 存档与地图 | 6 | `world.ticking_entity`、`world.ticking_block_entity` |
| 渲染与图形 | 10 | `opengl.unsupported`、`graphics.unsupported_driver` |
| 内存与卡死 | 2 | `memory.outofmemory`、`watchdog.timeout` |
| 服务端与客户端 | 2 | `server.client_class`、`server.client_class_notfound` |
| 模组生命周期 | 3 | `mod.loading_failed`、`mod.construct_error` |
| Sinytra Connector | 3 | `connector.intermediary`、`connector.failure` |

看门狗那两条（`watchdog.timeout` 与卡死阶段的判定）靠 tick 间隔推断，能不能用取决于上面写的
tick 适配。库命名空间里的缺类（`org.jetbrains`、`kotlin`、`com.google` 等）不算症状，除非类名以
`net.minecraft.client.` 开头。

## 构建

```bash
./gradlew jar
```

产物在 `build/libs/`，文件名是 `oba-mc1.20.2-1.20.4-forge-1.2.0.jar`。

- 首次构建要从 `maven.neoforged.net` / `maven.minecraftforge.net` 拉依赖；走代理时在 `gradle.properties` 里加 `systemProp.https.proxyHost` 和 `systemProp.https.proxyPort`。

## 已知限制

**1.20.3 客户端与服务端都没跑过**：客户端只验了 1.20.4，服务端只验了 1.20.2。
- Forge 只有 1.20.2 与 1.20.4 两条线，没有 1.20.3；为了让 1.20.2 能装，`loaderVersion` 放宽到 `[48,)`，而不是按 1.20.4 的 49 线写。
- 首次构建要下几 MB 的 userdev。Java 不读系统代理，直连只有 ~4 KB/s；卡住时放开 `gradle.properties` 里注释掉的代理行。

## 变更记录

这条轨道每一版改了什么、修了什么，见同目录的 [CHANGELOG.md](CHANGELOG.md)。
