# One BUG Among Us

**Turns a Minecraft crash into one sentence a person can act on.**

It reads three things: the game log, the crash reports, and the jars in `mods/`. Then it gives one
verdict — which mod, how sure, what to do next. It installs no drivers, edits no configs, touches no
mod file.

- Runs on the client and on a dedicated server; with no chat window the verdict goes to the console
- Nine version tracks, 1.20.1 through 26.2, with separate Forge and NeoForge jars
- One jar carries the in-game commands, the command line, and the post-crash window

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

> **Read only**: OBA writes its own report and log, and never modifies, moves or deletes a mod file.

## Contents

- [What this is](#what-this-is)
- [Supported versions](#supported-versions)
- [How one error becomes a verdict](#how-one-error-becomes-a-verdict)
- [Collection: logs and crash reports](#collection-logs-and-crash-reports)
- [The mod index](#the-mod-index)
- [Attribution and confidence](#attribution-and-confidence)
- [Rule set](#rule-set)
- [How false positives are kept out](#how-false-positives-are-kept-out)
- [Cross-version support](#cross-version-support)
- [Situations it handles](#situations-it-handles)
- [In-game interface](#in-game-interface)
- [The companion process](#the-companion-process)
- [Command line](#command-line)
- [Building](#building)
- [Layout](#layout)
- [Known limits](#known-limits)
- [License](#license)

## What this is

A crashed modpack log is long, and the useful part is usually one `Caused by:` line plus a stack.
A human scrolls up, finds the `at` frames that mention a mod's package, and works out which mod that
package belongs to. OBA turns those steps into a fixed pipeline: a rule recognises the kind of error,
the stack frames land on a specific mod, and the two become one sentence plus an issue list.

It does not guess. Every verdict carries its evidence: which rule matched, which log line, which
frame, which jar.

## Supported versions

| Minecraft | Loader | Java | Artifact |
|---|---|---|---|
| 1.20.1 | Forge, 47 line, built against 47.4.26 | 17 | `oba-mc1.20.1-forge-1.2.0.jar` |
| 1.20.2 / 1.20.3 / 1.20.4 | Forge, 49 line, built against 49.2.13 | 17 | `oba-mc1.20.2-1.20.4-forge-1.2.0.jar` |
| 1.20.2 / 1.20.3 / 1.20.4 | NeoForge, 20.2 line, built against 20.2.93 | 17 | `oba-mc1.20.2-1.20.4-1.2.0.jar` |
| 1.20.6 (Forge never shipped 1.20.5) | Forge, 50 line, built against 50.2.14 | 21 | `oba-mc1.20.5-1.20.6-forge-1.2.0.jar` |
| 1.20.5 / 1.20.6 | NeoForge, 20.5 line, built against 20.5.21-beta | 21 | `oba-mc1.20.5-1.20.6-1.2.0.jar` |
| 1.21.1 | Forge, 52 line, built against 52.1.20 | 21 | `oba-mc1.21.1-forge-1.2.0.jar` |
| 1.21 – 1.21.x | NeoForge, 21.1 line | 21 | `oba-mc1.21.1-1.2.0.jar` |
| 26.2 | Forge, 26.2 line, built against 26.2-65.1.5 | 25 | `oba-mc26.2-forge-1.2.0.jar` |
| 26.2 | NeoForge, 26.2 line, built against 26.2.0.93 | 25 | `oba-mc26.2-1.2.0.jar` |

1.20.1 is Forge only. From 1.20.2 to 1.21.x every range ships both a Forge and a NeoForge track, and
26.2 ships two. Forge and NeoForge tracks in the same range share the engine code and differ only in
the adapter layer.

## How one error becomes a verdict

1. **Collect**: an appender on log4j2 catches ERROR/FATAL lines; new files in `crash-reports/` come in too.
2. **Index**: scan `mods/*.jar` and build two maps — class name to mod, package prefix to mod.
3. **Match rules**: 49 regex rules run over each line; one log line yields the most specific verdict.
4. **Resolve frames**: every `at` class name is looked up in the index; if that fails, the package prefix gives a shortlist.
5. **Score**: distance from the root cause, rule severity, whether the mod id appears in the log text, whether several frames agree.
6. **Report**: a one-line headline, a culprit card (mod, file, first frame, confidence), an issue list, advice. Console, chat, report file and companion window all read the same data.

## Collection: logs and crash reports

**Why an appender**: reading stdout would need a second process, would miss lines written before a
server's output redirect is set up, and would lose the log level. The appender sees the `Level` and
the logger name before the line is even written.

A few details were learned the hard way:

- Level tests cannot use `level.isInRange(Level.ERROR, Level.ALL)`. In log4j2 the severity integers run
  backwards (FATAL=100, ERROR=200, INFO=400), so that expression means "ERROR through most verbose"
  and counts INFO as an error. The code uses `isMoreSpecificThan(Level.ERROR)`, and only
  `isMoreSpecificThan(Level.FATAL)` counts as fatal.
- Analysis must not run on the appender thread. One very long line combined with a backtracking regex
  can stall all log writing; lines are cut at 4096 characters, the whole rule pass has a two second
  budget, and analysis runs on a single-worker queue.
- It does not read itself: lines starting with `[OBA] ` are skipped, otherwise the report would be
  analysed as a new error.
- Repeats are merged: the same line is analysed once per five seconds, the same verdict is announced
  once per five minutes, and the repeat count is reported with the next announcement.
- Crash reports: a file in `crash-reports/` newer than the session start joins the timeline; if the
  session ends before analysis, the shutdown hook writes a report from it.

## The mod index

The index is what attribution stands on. Each jar is opened once and yields the `modId`, display name,
version, file name, and every `.class` name and package prefix inside it.

Three things are not optional:

- **Do not trust only the loader's file list.** NeoForge hands out paths in a union file system, and
  `Files.isRegularFile` is false for them; relying on it alone indexed 3 of 16 mods. So after the
  loader list the scanner also walks `<game dir>/mods/*.jar` and de-duplicates by path.
- **Keep platform jars out.** Anything under `libraries/`, anything named `neoforge-`, `forge-`,
  `client-extra`, `securejarhandler`, is not a mod. Left in, the verdict points at the loader itself.
- **Do not block startup.** Indexing runs on a background pool (`min(8, cores)`); if the mods folder is
  unchanged (name, size, modification time) it is not scanned again.
- **Read mods that live inside other jars.** A jar-in-jar mod usually sits on a zip file system opened by
  `FileSystems.newFileSystem`, where `Path.toFile()` throws `UnsupportedOperationException`. Everything is
  therefore read as a stream, and a path is used only when it really is a local file; the entry points live
  in `ModFiles` (`isPlainFile`, `contains`, `readAllBytes`), and OBA never opens a zip file system itself.
  Nested mods then count as their own source and can be blamed on their own.

## Attribution and confidence

A frame lands on a mod by one of two routes: the full class name is in the index, or the package prefix
is. When neither works the verdict says "no frame belonged to a mod" and keeps only the rule's advice,
which is better than pinning it on an innocent mod.

Confidence is a weighted sum, not a probability. A frame starts at `0.30 + 0.10 × depth`, is multiplied
by the matched rule's own confidence, and gets +0.85 when it is the root-cause frame (+0.35 for other
frames in the exception chain); frames in library code, or frames matched only by a package fragment,
are discounted by 0.45. Seeing the mod id or file name in the log text adds one more evidence entry.

A score of 2.0 or more reads as "certain", 0.9 as "high", 0.45 as "medium", anything else "low". The
percentage shown in the window is `score / 2.5`, capped at 99% — the score is accumulated weight, not a
probability, so it never claims 100%. Below the lowest band no culprit is named and the verdict says that
no frame belonged to a mod.

## Rule set

### Loader and dependencies (6)

| Rule id | Recognises | Points at |
|---|---|---|
| `neoforge.missing_dependency` | the `Missing or unsupported mandatory dependencies` lines | names the missing mod |
| `neoforge.missing_dependency_generic` | the same dependency mismatch without the listing | same, guessed from context |
| `neoforge.incompatible_mod` | `Mod ... requires ... which is missing` | version mismatch |
| `neoforge.dependency_version` | the range in `requires <mod> <range>` is not satisfied | installed, but too old or too new |
| `fml.duplicate_mod` | `Found duplicate mods` | one modId twice; lists both files |
| `fml.duplicate_mod_alt` | the older FML wording | same |

### The mod file itself (3)

| Rule id | Recognises | Points at |
|---|---|---|
| `loader.invalid_mod_file` | `InvalidModFileException` / `Missing required field ... in dependency` / `found an invalid mod file` | one bad jar stops the whole mods folder |
| `jar.corrupt` | `zip END header not found` / `invalid CEN header` | half-downloaded jar |
| `jarjar.missing` | `Unable to resolve ... inside jar` | a dependency missing inside a jar-in-jar |

### Mixin (6)

| Rule id | Recognises | Points at |
|---|---|---|
| `mixin.apply_failed` | `Mixin apply failed` / `MixinTransformerError` | which mixin, which target, who broke it |
| `mixin.apply_failed_alt` | the other wording | same |
| `mixin.transformer_error` | `MixinTransformerError` | same, through the transformer path |
| `mixin.config_failed` | `Failed to load mixin config` | the mixin config itself is broken |
| `mixin.crash` | `org.spongepowered.asm.mixin.throwables.MixinException` | thrown from inside a mixin |
| `mixinextras.version` | `MixinExtras` version not satisfied | MixinExtras too old |

### Class loading and linking (4)

| Rule id | Recognises | Points at |
|---|---|---|
| `class.noclassdef` | `NoClassDefFoundError: <class>` | which class is missing; attributed by package |
| `class.classnotfound` | `ClassNotFoundException: <class>` | same, the other route |
| `class.nosuchmethod` | `NoSuchMethodError` | a method that does not exist, usually a version mismatch |
| `class.nosuchfield` | `NoSuchFieldError` | same for a field |

### Registries and resources (4)

| Rule id | Recognises | Points at |
|---|---|---|
| `registry.missing` | `Unknown registry key` / `Missing registry` | a missing entry, usually from a data pack or mod |
| `registry.duplicate` | `Duplicate registration` | two mods claiming one id |
| `datapack.load_failed` | `Failed to load datapack` | a data pack that will not parse |
| `resourcepack.load_failed` | `Failed to load resource pack` | a resource pack that will not parse |

### Saves and maps (6)

| Rule id | Recognises | Points at |
|---|---|---|
| `world.ticking_entity` | `Ticking entity` / `Exception while ticking entity` | a broken entity, with type and coordinates |
| `world.ticking_block_entity` | `Ticking block entity` | a broken chest, machine or spawner |
| `world.chunk` | `Region file ... is corrupt/missing` / `has truncated header` / `has an invalid sector` | a broken region file |
| `world.dataversion` | `Data version ... is newer` / `Unknown data version` | a save from a newer game version |
| `world.entity_data` | `Failed to load entity data` / `Invalid entity NBT` | invalid NBT in an entity or container |
| `world.level_load` | `Failed to load level data or datapacks` | the world never started; points at the packs or mods it references |

### Rendering and graphics (10)

| Rule id | Recognises | Points at |
|---|---|---|
| `opengl.unsupported` | `Unsupported OpenGL version` or a version below 3.2 | an old driver |
| `graphics.unsupported_driver` | `GLFW error 65542` (Microsoft Basic Render) | no GPU driver; software rendering |
| `graphics.glfw_init` | `Failed to initialize GLFW` | no window or GL context |
| `graphics.context_lost` | `GL context lost` / `Context lost` | the driver dropped the context |
| `graphics.shader_compile` | `Shader compile error` / `Could not compile shader` | which shader failed |
| `graphics.shader_pack` | `Failed to load shader pack` | a shader pack that will not load |
| `graphics.sampler_missing` | `could not find sampler named` | a missing sampler; points at graphics mods or the pack |
| `graphics.framebuffer` | `Framebuffer is not complete` | an incomplete framebuffer, mostly with shaders |
| `graphics.mixin_conflict` | `Mixin conflict` / `already transformed` | two optimisation mods patching one method |
| `graphics.family_conflict` | the other wording for the same family clash | same |

### Memory and stalls (2)

| Rule id | Recognises | Points at |
|---|---|---|
| `memory.outofmemory` | `java.lang.OutOfMemoryError` | not enough heap; shows the current limit |
| `watchdog.timeout` | a server tick over the threshold (timed by OBA, not from the log) | which phase stalled, plus the last action |

### Server and client (2)

| Rule id | Recognises | Points at |
|---|---|---|
| `server.client_class` | `Attempted to load class net/minecraft/client/... for invalid dist DEDICATED_SERVER` | a client-only mod on a server; names the class owner |
| `server.client_class_notfound` | `ClassNotFoundException: net.minecraft.client.*` | same, the other route |

### Mod lifecycle (3)

| Rule id | Recognises | Points at |
|---|---|---|
| `mod.loading_failed` | `Mod Loading has failed` / `Failed to create mod instance` | the loading phase failed; culprit found from context |
| `mod.construct_error` | `Failed to complete lifecycle event CONSTRUCT` | a mod constructor threw |
| `manual.debug_crash` | `Manually triggered debug crash` | not a bug; triggered with F3+C |

### Sinytra Connector (3)

| Rule id | Recognises | Points at |
|---|---|---|
| `connector.intermediary` | intermediary names such as `class_1234` / `method_5678` | a Fabric mod not remapped under Connector |
| `connector.failure` | Connector's own errors | Connector version or compatibility |
| `connector.missing_mod` | `Requires ... Fabric API` and similar | a missing Fabric-side dependency |


## How false positives are kept out

- **Library namespaces**: a missing class in `org.jetbrains.annotations`, `kotlin`, `com.google`,
  `org.apache`, `io.netty` or `org.slf4j` is not a symptom (plenty of mods forget a `provided`
  dependency). Those stay quiet unless the class starts with `net.minecraft.client.`.
- **Platform jars**: not in the index, so the loader is never the culprit.
- **A sampler name is not a mod**: when a shader reports `could not find sampler named Sampler2`,
  `Sampler2` is a name the shader chose. The rule keeps only the fact that a sampler is missing.
- **Warnings do not write files**: a hit that is warning-level and names no mod is announced once on
  the console and produces no report file, otherwise a per-frame graphics warning leaves one useless
  file per launch.
- **One line, one verdict**: before comparing, leading `Caused by:`, `Exception message:` and
  `Suppressed:` labels are stripped and digits are replaced with placeholders, so the same line with a
  different coordinate or millisecond is not counted twice.

## Cross-version support

**How the tracks are cut**: by "one engine build compiles and behaves the same", not by patch version.
1.20.1 and 1.20.2 differ in both mappings and reobf; NeoForge reworked tick events and moved its
descriptor around 1.20.5; 1.21.1 and 26.2 differ in Java version and build plugin.

**At compile time**:

| Track | Target | Build plugin | Note |
|---|---|---|---|
| 1.20.1 Forge | Forge 47 line | ForgeGradle 6 | reobf required |
| 1.20.2~1.20.4 | Forge 48/49, NeoForge 20.2~20.4 | ForgeGradle 6 / NeoGradle userdev | reobf on the Forge side only |
| 1.20.5~1.20.6 | Forge 50, NeoForge 20.5~20.6 | same | Forge drops reobf; descriptor moves |
| 1.21.1 | Forge 52, NeoForge 21.1 | same | |
| 26.2 | Forge 65, NeoForge 26.2 | ForgeGradle 7 / ModDevGradle 2 | Java 25; ForgeGradle 7 rewrote the DSL |

**Probed at run time** (reflection instead of compile-time constants):

- Tick events: `event.tick.ServerTickEvent$Post` then `event.TickEvent$ServerTickEvent` are tried with
  `Class.forName`; registration goes through `IEventBus.addListener(Class, Consumer)` so nothing is
  registered against the abstract `Event` class, which throws `IllegalArgumentException`.
- Descriptor: three templates (`mods-forge.toml` for Forge, which becomes `mods.toml`, and `mods.toml` or
  `neoforge.mods.toml` on the NeoForge side); the build picks one.
- Client or server: read `FMLEnvironment.dist` or the loader marker in `META-INF`. Never
  `Class.forName("net.minecraft.client.Minecraft")` — on a server that trips `RuntimeDistCleaner`, and
  the resulting error looks exactly like "a client-only mod is installed on a server".

## Situations it handles

| Situation | What OBA does | How |
|---|---|---|
| A missing dependency | Names what is missing and which version range it wants | `neoforge.missing_dependency*` |
| A mod built for another version | Says whether it is too old or wants something newer | `neoforge.dependency_version` / `incompatible_mod` |
| Duplicate modId | Lists both files so the old one can be removed | `fml.duplicate_mod*` |
| One bad jar stops the whole mods folder | Names the jar and explains the `mandatory` field on 1.20.2~1.20.4 | `loader.invalid_mod_file` |
| A half-downloaded jar | Suggests downloading it again, with the file name | `jar.corrupt` |
| A mixin that failed to apply | Points at the mixin class and the target, and at whoever broke it | `mixin.*` |
| Class, method or field not found | Attributed by package; library namespaces stay quiet | `class.*` plus the noise filter |
| Registry entries missing or duplicated | Names the registry key and the mods involved | `registry.*` |
| Data pack or resource pack failed to parse | Names the pack and the first parse error | `datapack.*` / `resourcepack.*` |
| A broken entity in the save | Entity type, coordinates, first line of the exception | `world.ticking_entity` |
| A broken block entity (chest, machine, spawner) | Block coordinates and the exception | `world.ticking_block_entity` |
| A corrupt region file (truncated, bad sector) | Says which `.mca` and offers restore-from-backup or delete | `world.chunk` |
| A save from a newer game version | Explains the DataVersion mismatch; downgrading is not something a mod can fix | `world.dataversion` |
| Invalid NBT in an entity or container | Points at that entry and notes it usually follows a removed mod | `world.entity_data` |
| The world never loaded at all | Points at the data packs or mods the save references | `world.level_load` |
| Old GPU driver, software rendering | Says it is the driver, not a mod | `opengl.*` / `graphics.unsupported_driver` |
| Shader compile errors, missing samplers, incomplete framebuffer | Points at graphics mods or the shader pack | `graphics.shader*` / `sampler_missing` / `framebuffer` |
| Two optimisation mods patching the same method | Names both sides | `graphics.mixin_conflict` / `family_conflict` |
| Out of memory | Shows the current limit and suggests a larger `-Xmx` | `memory.outofmemory` |
| A server that stopped ticking | OBA times ticks itself and reports the stalled phase | `watchdog.timeout` |
| A client-only mod installed on a server | Names the class and which mod owns it | `server.client_class*` |
| Fabric mods under Sinytra Connector | Resolves intermediary names back to the Fabric mod | `connector.*` |
| An MCreator workspace | Recognised by its directory and naming patterns, with its own advice | `McreatorDetector` |
| A crash before OBA itself is constructed | No window and no report; the next start reads `crash-reports/` | Shutdown hook plus startup read |
| A normal exit that did capture errors | The companion window still opens, caption "errors seen" | `capturedErrors` in the handoff |
| A crash inside a native library or a kill from Task Manager | Falls back to the crash report written this session | `CrashReporter` |
| A mod that carries other mods inside its jar (jar-in-jar) | The inner mod counts as its own source and can be blamed like any other | `ModSource` streaming plus `ModFiles` |

## In-game interface

`/oba` is a command tree that mirrors the CLI: `help`, `status`, `report`, `mods`, `mcreator`,
`analyze`, `crash`, `info`, plus two troubleshooting entries: `reindex` drops the index and rescans
`mods/`, and `threads` writes the current thread dump into the report. Every clickable entry carries a `ClickEvent`, so one click runs it.

The chat font is not monospaced: a space is 4 px, a lowercase letter about 6 px, a CJK glyph 9 px and
bold adds 1 px per glyph. A 100-column terminal layout pushed into chat wraps into confetti, so the text
is reflowed by pixel width before it goes to the chat (`GameUi.lines`), and rule lines are collapsed to a
single glyph row.

## The companion process

Some crashes leave no time to speak: a fault inside a native library, a kill from Task Manager, a
failure during mod loading. No line of the game's code runs again, and the verdict printed to chat goes
with it.

OBA starts a second JVM at client startup with `javaw -cp <the same jar>`, main class
`dev.oba.companion.CompanionMain` — no separate executable, no unpacking the UI into a temp directory.
It does one thing: wait for the game process to disappear, read the handoff file, open a window.

| What happened this session | Caption | Window |
|---|---|---|
| No error, normal exit | clean exit | none |
| Errors captured, no crash | errors seen | opens |
| Crash (new crash report, or the handoff says so) | crash | opens and raises itself |
| Crash before OBA is constructed | — | nothing at all |

The handoff goes to `logs/oba-companion/handoff-*.json`: one-line verdict, culprit, issue list with rule
names, timeline, environment, full report. The window shows verdict, culprit and issues on the left;
report, timeline, environment and raw JSON on the right.

The window is frosted glass: a screenshot of what sits behind it, downscaled to a quarter, blurred, tinted, clipped to rounded corners and framed by a soft outer shadow plus a one-pixel highlight edge. Windows has no public backdrop-blur API, so the backdrop is a captured frame, re-taken whenever the window moves.

Switch: `-Doba.companion=off|auto|always` (or `OBA_COMPANION`), default `auto`. To see it without a
crash: `java -cp <jar> dev.oba.companion.CompanionMain --demo`.

## Command line

```text
java -jar oba-mc1.21.1-1.2.0.jar analyze <log or directory>
java -jar oba-mc1.21.1-1.2.0.jar mods <game directory>
java -jar oba-mc1.21.1-1.2.0.jar mcreator <project directory>
java -jar oba-mc1.21.1-1.2.0.jar watch <game directory>
java -jar oba-mc1.21.1-1.2.0.jar version
```

`analyze` with no argument reads `logs/latest.log` and `crash-reports/` from the working directory.
Useful switches: `--json` for machine-readable output, `--md` for Markdown, `--lang=zh|en`, `--game-dir`
and `--mods-dir`, `--pick` to narrow the evidence to one mod, `--menu` for the interactive menu, and
`--quiet` to keep only the verdict. Output is coloured; `--color=always|never|auto` controls it, and
redirection turns colour off by itself.

## Building

```bash
./gradlew jar            # jar into dist/
pwsh tools/build-all.ps1 # every track
```

JDK 17 for 1.20.1 and the Forge 1.20.2~1.20.4 tracks, 21 for NeoForge 1.20.5+ and Forge 1.20.5+, 25 for
26.2. The first build downloads from `maven.neoforged.net` and `maven.minecraftforge.net`; behind a proxy
add `systemProp.https.proxyHost/Port` to `gradle.properties`.

## Layout

```text
engine/main/java/dev/oba/
├── core/      rules, attribution, reports, strings
├── mod/       in-game commands, chat layout, collector, companion launcher
├── cli/       command line entry point
├── companion/ the post-crash window (second JVM, same jar)
└── loader/    loader-agnostic interfaces
src/loader/{forge,neoforge,forge262}/  per-track adapters
versions/<track>/                       self-contained build directory per track
```

## Acknowledgements

- Special thanks to [XinKo_TS](https://space.bilibili.com/1555260755?) for supporting and maintaining this mod
- [McreatorDetector-1192](https://github.com/yiyuyan/McreatorDetector-1192) — Reference implementation of MCreator detection ideas
- [Sinytra Connector](https://github.com/Sinytra/Connector) — Bridge solution for Fabric mods on NeoForge
- [NeoForged](https://neoforged.net/) and [MinecraftForge](https://github.com/MinecraftForge) — Loader and event system
- Testers who provided real modpacks and crash log feedback
- Deepseek for multi-language translation

## Known limits

- A crash before OBA is constructed (mod discovery or dependency checks) produces neither a window nor
  a report; the next start reads `crash-reports/` and catches up.
- Attribution needs frames or rules. An NPE whose four frames all sit in Minecraft's own code gets
  advice, not a mod name.
- The watchdog depends on tick events. When neither `ServerTickEvent$Post` nor `TickEvent$ServerTickEvent` can be found it logs one warning and disables hang detection; everything else keeps working. All nine tracks currently find one.
- 26.1 and 26.3 are not packaged.
- A NeoForge 26.2 server logs its own appender errors (netty cannot find the kqueue/epoll native
  library). OBA does not treat that as a crash, but the lines are in the log.

## License

GPL-3.0-only
