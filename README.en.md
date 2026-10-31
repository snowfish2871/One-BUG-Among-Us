# One BUG Among Us · 26.2-forge

> One self-contained track: **26.2**, loader Forge, 26.2 line, built against 26.2-65.1.5, Java 25. Sources, resources and the build
> script all live in this directory, so it can be copied out and built on its own. The
> repository overview and the full rule set are in the [root README](../../README.en.md).

## This track

| Item | Value |
|---|---|
| Minecraft | 26.2 |
| Loader | **Forge**, 26.2 line, built against 26.2-65.1.5 |
| Java | 25 |
| Target | 26.2 / 26.2-65.1.5 |
| Descriptor | `META-INF/mods-forge.toml` |
| Artifact | `oba-mc26.2-forge-1.2.0.jar` |
| Status | built, verified in game |

## What differs here

- **Descriptor**: `META-INF/mods-forge.toml`. It is a placeholder template, expanded at build time; renaming it makes the loader ignore the jar.
- **Build chain**: net.minecraftforge.gradle [7.0.17,8); No reobf: Forge dropped it from 1.20.5 on.
- **Tick adapter**: The 26.2 server tick is still `net.minecraftforge.event.TickEvent$ServerTickEvent`; `ForgeTickHook` binds it at compile time and only counts `Phase.END`. Neither the client nor the dedicated server logged a 「could not hook the tick event」 warning.
- **Adapter code**: `src/main/java/dev/oba/loader/forge262/`: `ForgeEnv` reads the mod list and the game directory, `ForgeTickHook` hooks the server tick. Neither changed on 26.2.

## How the engine works (one shared code base)

1. Collect: a log4j2 appender catches ERROR / FATAL; new files in `crash-reports/` come in too.
2. Index: scan `mods/*.jar` into two maps, class name to mod and package prefix to mod.
3. Match: 49 regex rules run over each line; one line yields one most-specific verdict.
4. Frames: each `at` class name is looked up in the index, falling back to the package prefix.
5. Score: root-cause distance, rule severity, mod id in the text, agreement between frames.
6. Report: headline, culprit card (mod / file / first frame / confidence), issue list, advice.

The index cannot trust only the loader's own list (NeoForge hands out union-file-system paths that are
not regular files, and trusting them alone found 3 of 16 mods), so `mods/*.jar` is scanned as well and
de-duplicated; platform jars under `libraries/` are excluded or the verdict points at the loader; and
indexing runs on a background pool, skipped when the mods folder is unchanged.

Attribution takes two routes only: the full class name is in the index, or the package prefix is. When
neither works the verdict names nobody. Confidence is weighted (first frame inside the mod, several
frames agreeing, a fatal rule, the mod id in the text add; a bare package match or library frames
subtract), and below the threshold there is no verdict.

## Situations this track handles

| Category | Rules | Examples |
|---|---|---|
| Loader and dependencies | 6 | `neoforge.missing_dependency`, `neoforge.missing_dependency_generic` |
| The mod file itself | 3 | `loader.invalid_mod_file`, `jar.corrupt` |
| Mixin | 6 | `mixin.apply_failed`, `mixin.apply_failed_alt` |
| Class loading and linking | 4 | `class.noclassdef`, `class.classnotfound` |
| Registries and resources | 4 | `registry.missing`, `registry.duplicate` |
| Saves and maps | 6 | `world.ticking_entity`, `world.ticking_block_entity` |
| Rendering and graphics | 10 | `opengl.unsupported`, `graphics.unsupported_driver` |
| Memory and stalls | 2 | `memory.outofmemory`, `watchdog.timeout` |
| Server and client | 2 | `server.client_class`, `server.client_class_notfound` |
| Mod lifecycle | 3 | `mod.loading_failed`, `mod.construct_error` |
| Sinytra Connector | 3 | `connector.intermediary`, `connector.failure` |

The watchdog rules infer a stall from tick intervals, so they only work when the tick adapter above
does. Missing classes in library namespaces (`org.jetbrains`, `kotlin`, `com.google` and friends) are
not treated as symptoms unless the class starts with `net.minecraft.client.`.

## Building

```bash
./gradlew jar
```

The jar lands in `build/libs/` as `oba-mc26.2-forge-1.2.0.jar`.

- This track builds with Gradle: `./gradlew jar`. `build.gradle` follows the official Forge 26.2 MDK; FG7 swapped the DSL (`minecraft.runs.configureEach` with `workingDir`, `minecraft.dependency(...)` as a plain dependency, `minecraft.mavenizer(it)` as a repository).

## Known limits

- 26.1 and 26.3 have no jar and have never been launched.
- On Windows, Forge 26.2 itself throws `AppenderLoggingException`s (`io.netty.channel.epoll.Native` / `kqueue.Native` exist only on Linux and OSX). The server still finished and OBA did not treat it as a crash.
- The in-game `/oba` menu is verified only as far as “the command runs and the chat answers”; the pages were not screenshotted one by one.

## Changelog

What each release of this track changed and fixed is in [CHANGELOG.md](CHANGELOG.md).
