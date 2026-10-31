# One BUG Among Us · 1.20.2-1.20.4-neoforge

> 这个目录是**1.20.2 / 1.20.3 / 1.20.4**（NeoForge 20.2 线，构建时对 20.2.93）的一条独立轨道：源码、资源、构建脚本都在这里，单独拷出去也能构建。
> 仓库总览和完整的规则说明在[根 README](../../README.md)。

## 这一条轨道

| 项 | 值 |
|---|---|
| Minecraft | 1.20.2 / 1.20.3 / 1.20.4 |
| 加载器 | **NeoForge** 20.2 线，构建时对 20.2.93 |
| Java | 17 |
| 编译目标 | 1.20.2 / 20.2.93 |
| 描述文件 | `META-INF/mods.toml` |
| 产物 | `oba-mc1.20.2-1.20.4-1.2.0.jar` |
| 状态 | 已出包，已实机验证 |

## 适配了哪些差异

- **描述文件**：`META-INF/mods.toml`。它是占位符模板，构建时把版本号展开再打进 jar；换成别的名字加载器就认不出来。
- **构建链**：net.neoforged.gradle.userdev 7.1.38
- **tick 适配**：这条轨道上的 tick 事件是 `net.neoforged.neoforge.event.TickEvent$ServerTickEvent`，带 `Phase`（START / END），一次 tick 触发两回，只认 END 才能和「一次 tick 一回」对上。
- **适配层代码**：`src/main/java/dev/oba/loader/neoforge/`：`NeoForgeEnv` 读模组列表和游戏目录，`NeoForgeTickHook` 挂服务器 tick（这条轨道走老的那套事件）。

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

产物在 `build/libs/`，文件名是 `oba-mc1.20.2-1.20.4-1.2.0.jar`。

- 首次构建要从 `maven.neoforged.net` / `maven.minecraftforge.net` 拉依赖；走代理时在 `gradle.properties` 里加 `systemProp.https.proxyHost` 和 `systemProp.https.proxyPort`。

## 已知限制

- 描述文件是 `META-INF/mods.toml`：`[[dependencies]]` 里的 `mandatory` 字段必须写，而且必须排在 `type` 前面，否则加载器直接报解析失败。
- 1.20.4 编译的模组装进 1.20.2 / 1.20.3，会让整个 mods 目录被判 invalid（1.20.4 起 `mandatory` 被删掉，而 20.2~20.3 还要求它）：一个这样的 jar 就够让所有模组都不加载，看起来像「装了模组但一个都没生效」。OBA 认这条日志（`loader.invalid_mod_file`）。
- 本机 1.20.3 的 USB 声卡要 `ALSOFT_DRIVERS=null` 才过得了音频初始化（`jstack` 停在 `alcResetDeviceSOFT`），和 OBA 无关。

## 变更记录

这条轨道每一版改了什么、修了什么，见同目录的 [CHANGELOG.md](CHANGELOG.md)。
