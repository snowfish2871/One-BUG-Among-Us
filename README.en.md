# One BUG Among Us

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">


**Crash diagnostics for Minecraft 1.21.x on NeoForge**

It names the mod file that broke the game, the class and line where it broke, and what every
mod was doing in the moments before and during the failure.

English | [简体中文](README.md)

> **Diagnosis only. This project never modifies a single file.**
> It does not disable, move, rename or delete any mod. Everything it concludes is printed
> and written to a report; acting on it is your decision.

<img src="src/main/resources/oba-icon.png" alt="icon" width="128" align="right">


---

## Contents

- [What it solves](#what-it-solves)
- [Highlights](#highlights)
- [Compatibility](#compatibility)
- [Quick start](#quick-start)
- [Incident timeline](#incident-timeline)
- [Diagnostic reports](#diagnostic-reports)
- [Using it](#using-it)
- [How attribution works](#how-attribution-works)
- [MCreator detection](#mcreator-detection)
- [Graphics mods](#graphics-mods)
- [Sinytra Connector support](#sinytra-connector-support)
- [Rule set](#rule-set)
- [Project structure](#project-structure)
- [Building](#building)
- [Verification](#verification)
- [Known limits](#known-limits)
- [FAQ](#faq)
- [Contributing](#contributing)
- [Credits](#credits)
- [License](#license)
- [Appendix A: community content to be written](#appendix-a-community-content-to-be-written)
- [Appendix B: glossary](#appendix-b-glossary)

---

## What it solves

When a mod breaks the game, the log usually offers a wall of stack trace and no answer.
OBA turns that into one actionable sentence: **which file, which line, and why**.

| What you are looking at | What OBA answers |
|---|---|
| A crash and nothing but stack frames | The mod file, the class/method/line, and the full evidence weighting |
| `NoClassDefFoundError` in the log | Which class is missing, which mod should provide it, whether a dependency is absent |
| A freeze with no crash report | Captures a thread dump and names the mod that was executing |
| A few hundred mods and no idea where to start | Ranking by confidence, each suspect with its own reasons |
| `Mixin apply failed` | The mixin class and line, plus a conflict or version hint |
| The same mod installed twice | Every duplicate modId and all files declaring it |
| Wanting to spot MCreator mods | Multi-signal scoring, with every reason listed |
| A Fabric mod crashing under Connector | Still attributed to that Fabric mod's jar |
| An F3+C debug crash | Reported as deliberate, with no culprit to chase |

---

## Highlights

| | Feature | Detail |
|---|---|---|
| 🎯 | **Down to the line** | Class, method, source file and line number, ready to match against the author's source |
| 📜 | **Incident timeline** | What each mod was doing at, and shortly before, the failure |
| 🧾 | **Traceable evidence** | Every point awarded is listed, so a wrong verdict can be traced to the step that caused it |
| 🔒 | **Strictly read only** | No writes, moves or deletes; a report only ever adds a file under `logs` |
| 📦 | **One jar, two jobs** | In `mods/` it is a mod; `java -jar` makes it a command line tool |
| 🌐 | **English and Chinese** | The engine, CLI, in-game messages and reports all switch language |
| 🎨 | **Graphics family awareness** | Knows the dozens of renamed Sodium and Iris forks, and catches "two rival branches installed" and "shader loader with no renderer" |
| 🖱️ | **Click a mod for its description** | Every row of /oba mods is clickable and expands that mod's description and key metadata |
| 🧩 | **Built for cross-version use** | Only APIs stable across minor versions, with every risky step isolated |
| 🛡️ | **Fails quietly** | If it cannot start on your version it goes silent instead of blocking the game |

---

## Compatibility

| | Requirement |
|---|---|
| Minecraft | **1.21 – 1.21.x** (versions without NeoForge are not supported) |
| NeoForge | **21.1 or newer** (each 1.21.x minor has its own 21.x line) |
| Java | **21** (required by Minecraft since 1.21) |
| Client / server | Both |

### Why it works across minor versions

The mod depends only on the parts of the API that are **stable across minor versions**: the
logging backend (log4j2), mod lifecycle events, the event bus, command registration and chat
text. It never touches rendering, world data or registry contents, so it does not break when
those change.

Initialisation is also isolated by design: the log collector, the class index, event
registration and the command tree each run in their own guarded block. If one of them throws
on a version where an API shifted, only **that one feature** is disabled, and a line is left in
the log to say so. A diagnostic tool has no business being the reason a game fails to start.

### On verification scope

Building and real-client verification were done on **1.21.1 + NeoForge 21.1.256**. Other
1.21.x minors are expected to work by the design described above, but have not each been
verified on a live client.

If OBA itself errors on another 1.21.x version, the log will contain
`[OBA] could not start up on this version` or `[OBA] could not register ...`.
Report that line together with your version numbers and it can be looked at.

---

## Quick start

### In game

1. Put `oba-1.0.0.jar` into `.minecraft/mods/`
2. Start the game

From then on **you do not type anything**. When something breaks, the verdict appears in the
log and in chat by itself, and the full report lands in `logs` on its own:

```
[OBA] Culprit: Awesome Machines  [awesome-machines-2.1.0.jar]
[OBA] Failure location: com.example.awesomemachines.registry.MachineRegistry.register (MachineRegistry.java:143)
[OBA] Confidence: very high (99%)
[OBA] Report written to D:\MC\.minecraft\logs\oba-report-2026-10-10_11.31.58.txt
```

Type `/oba report` only when you want the full report on screen.

### Command line

```bash
# find .minecraft, analyse the newest crash report, write the report into logs
java -jar oba-1.0.0.jar analyze

# point at a game directory (quoted or unquoted, paths with spaces are fine)
java -jar oba-1.0.0.jar analyze --game-dir "D:\my games\.minecraft"

# feed it one crash report
java -jar oba-1.0.0.jar analyze crash-reports/crash-2026-10-09_18.03.20-client.txt

# list mods, find duplicates, spot MCreator, mark Fabric mods
java -jar oba-1.0.0.jar mods --game-dir "D:\MC\.minecraft"

# follow the log while you play
java -jar oba-1.0.0.jar watch "D:\MC\.minecraft\logs\latest.log"
```

### Try the bundled examples

`examples/` is a deliberately broken mini game directory: six fake mods, three crash reports
and a missing-dependency log.

```bash
# a NeoForge mod blows up
java -jar oba-1.0.0.jar analyze examples/crash-reports/crash-2026-10-09_18.03.20-client.txt

# a Fabric mod blows up under Connector
java -jar oba-1.0.0.jar analyze examples/crash-reports/crash-2026-10-10_11.35.02-client.txt

# an F3+C debug crash
java -jar oba-1.0.0.jar analyze examples/crash-reports/crash-2026-10-10_16.47.12-client.txt

java -jar oba-1.0.0.jar mods     --game-dir examples
java -jar oba-1.0.0.jar mcreator --game-dir examples
```

Expected: the first names `awesome-machines-2.1.0.jar` at `MachineRegistry.java:143`; the
second names `sodium-extra-0.6.0.jar` at `MixinOptionsScreen.java:47`; the third says the crash
was deliberate; `mods` reports the duplicate `sodium` modId; `mcreator` flags
`coolstuff-1.0.0.jar`.

---

## Incident timeline

Knowing the culprit is sometimes not enough — you also want to know what happened right before
it broke. OBA arranges the log into a timestamped timeline, marking which mod each event
came from:

```
[Incident timeline] (most recent event first)
  time         mod                what happened
      22:09:18 -                   + Failed loading villager cartographer registered at cartographer
      22:09:10 -                   * Error loading class: net/caffeinemc/... (ClassNotFoundException)
      22:08:04 -                   + registered internal shaders
      22:07:54 kubejs              + Key mapping key.kubejs.itemkillkey registered after event
      22:07:49 modernui            + Redirect registered font 'Source Han Sans SC' to 'modernui:...'
      22:07:21 -                   * Failed to find class: java.util.zip.ZipException: zip END header not found
```

### Reading it

- **Read from the top.** The first line is the moment closest to the failure; going down goes
  further back in time.
- **The second column is the mod.** `-` means the line could not be attributed to a specific
  mod, usually because it came from vanilla or the loader.
- **The third column is a marker:** `!!` error, `!` warning, `*` rule hit, `+` lifecycle event,
  `·` plain information.

### A deliberate trade-off

Logs run to tens of thousands of lines, so simply showing the last few hundred is useless —
the tail is usually unrelated routine output. The timeline is therefore **selected by
importance**: fatal and error events are always kept, rule hits come next, events attributed to
a specific mod score higher, and events closer to the failure score higher still. The selection
is then put back into chronological order.

A line repeated more than three times in a row is folded into a single notice, so one burst of
spam cannot drown the timeline.

The timeline appears in all three output formats:

| Format | Presentation |
|---|---|
| Text | Aligned table, up to 40 rows |
| Markdown | Three-column table, up to 30 rows |
| JSON | A `timeline` array with `time` / `modId` / `modName` / `type` / `event` |

---

## Diagnostic reports

### Where they are saved

**Always the game's `logs` folder**, in game and from the command line alike.

| File | When it appears |
|---|---|
| `logs/oba-report-<timestamp>.txt` | On a captured error, a freeze, game exit, or a new crash report |
| `logs/oba-latest.txt` | Same content, fixed name, always the newest |

Timestamps look like `yyyy-MM-dd_HH.mm.ss`. Only the **10** most recent reports are kept; older
ones are deleted so the folder does not fill up.

### When it triggers

| Trigger | Detail |
|---|---|
| **Captured error** | As soon as an attributable error appears in the log. The same error (same culprit and exception) is written once |
| **Freeze** | After the watchdog captures a thread dump and attributes it. Routine vanilla heavy work (saving chunks, stopping the world, autosave) is not reported, to avoid noise |
| **Game exit** | If anything was captured this session, one final report is written. Besides the FML shutdown event there is a JVM shutdown hook |
| **F3+C debug crash** | This kind writes no log at all, only a file under `crash-reports`. On exit that folder is scanned and reports new to this session are diagnosed |
| **Command line** | `analyze` writes into the `logs` folder of `--game-dir` when it finds a problem; `--output` overrides the location |

Every report states up front why it was written:

```
OBA v1.0.0
Triggered by a captured error
Report only. No file was modified, disabled or deleted.
════════════════════════════════════════════════════════════════════════
```

### What is in a report

In order: environment information, the **verdict** (culprit and failure location), the
**incident timeline**, the suspect ranking, a detailed evidence chain per mod, the rule-hit
findings, MCreator results, and the packages that could not be attributed.

### Reading the verdict

```
[Verdict]
  Most likely culprit: Awesome Machines (awesomemachines)
  Mod file     : awesome-machines-2.1.0.jar
  SHA-1        : 0ea0b938b2eb0a092f37a38d95e8bcde8158840e
  Confidence   : very high (85%)
  Failure location: com.example.awesomemachines.registry.MachineRegistry.register (MachineRegistry.java:143)
  Exception    : java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null
```

- **`Failure location`** is the line that matters: `MachineRegistry.java:143` maps straight
  onto the author's source.
- **`SHA-1`** confirms the file has not been altered. Attaching it to a bug report avoids the
  usual "wrong version" round trip.
- **`Confidence`** is a band derived from evidence weight, not a probability. It answers "is
  the evidence solid", nothing more.
- **`Evidence`** lists every point awarded, so a wrong verdict shows which step went wrong
  instead of leaving you to trust or distrust a number.
- **`[exact class match]`** means the class really exists in that jar. When it instead reads
  `[package match xxx (72%)]`, only a package prefix matched and the weight is discounted.

---

## Using it

### In-game commands

You do not need any command to get a diagnosis; these exist for when you want more.

| Command | Effect |
|---|---|
| `/oba report` | Show the most recent full diagnosis, and write it to `logs` |
| `/oba status` | Process state: memory, threads, deadlocks, indexed mods, report folder |
| `/oba mcreator` | List mods that look MCreator generated, with reasons |
| `/oba mods` | List loaded mods, versions, class counts, duplicate ids, graphics family |
| `/oba info <id>` | **Show a mod's description and metadata** |
| `/oba threads` | Capture a thread dump and attribute it |
| `/oba crash` | Analyse the newest crash report on disk |
| `/oba analyze <file>` | Analyse a specific log or crash report |
| `/oba reindex` | Rescan mods and rebuild the class index |
| `/oba help` | Command list |

Works in single player, on a LAN host and from a dedicated server console. No world state is
ever changed.

### Click a mod to read its description

Every row printed by `/oba mods` is **clickable**. Hovering shows a hint, and clicking
expands that mod's description and metadata:

`
Awesome Machines  (awesomemachines)
  A multi-block machine that builds machines by itself. Needs FE to run.
  JEI shows the recipes; the config lives in config/awesomemachines-common.toml.

  File          awesome-machines-2.1.0.jar
  Version       2.1.0
  Author        Example Author
  License       MIT
  Loader        javafml
  Classes       5
  Size          1.6 KB
  SHA-1         0ea0b938b2eb0a092f37a38d95e8bcde8158840e
  Duplicate: 1 other file declares the same mod ID
  Looks MCreator generated (23 points)
  Graphics family: Sodium family (render optimizer)
      the main Forge/NeoForge port of Sodium
`

The description comes from the mod's own metadata (the `description` field in `mods.toml`
or `fabric.mod.json`); a jar that bundles several mods shows each one. Alongside it are the
fields that actually matter when troubleshooting: SHA-1 (needed for bug reports), duplicate
status, MCreator detection, and which graphics family it belongs to. A mod whose metadata has no
description says so rather than showing nothing.

Chat cannot hold everything, so output is capped at 24 lines and then points at the report file.

Typing the command directly does the same thing:

`
/oba info sodium
/oba info awesomemachines
`

### Command line tool

```
oba analyze [log or crash report...]  name the failing mod and the line
oba mods                              list the mods folder and check for problems
oba mcreator                          run only the MCreator detection
oba watch [latest.log]                follow the log and report as it happens
oba threads [dump.txt]                analyse a thread dump
oba version / help
```

| Option | Meaning |
|---|---|
| `--mods-dir <path>` | mods folder (auto-detected by default) |
| `--game-dir <path>` | game root containing `mods/` `logs/` `crash-reports/` |
| `--format text\|md\|json` | output format, default text |
| `--output <file\|dir>` | write the report to a specific path instead of `logs/` |
| `--lang <en\|zh>` | report language (default: system locale) |
| `--no-color` | disable ANSI colours |
| `--no-mcreator` | skip the MCreator check (faster) |
| `--thread <name>` | stalled thread to focus on, e.g. `"Server thread"` |
| `-q` / `-v` | terse output / print stack traces on errors |

**Exit codes**: `0` clean · `2` problems found · `64` bad arguments · `70` internal error.

### Languages

Reports switch between English and Chinese, covering the engine, the CLI, the in-game messages
and the log output.

**Language resolution** (highest priority first):

1. `--lang <en|zh>` on the command line
2. `OBA_LANG` environment variable, or `-Doba.lang=zh`
3. In-game mod: the language the player picked in the game options (client)
4. JVM default locale (`user.language`)
5. Fallback: English

So a Chinese client gets Chinese verdicts and any other client gets English, with no
configuration.

```bash
java -jar oba-1.0.0.jar analyze --lang en --game-dir examples
OBA_LANG=en java -jar oba-1.0.0.jar mods --game-dir examples
```

All strings live in one table (`common/.../core/Lang.java`) keyed in `namespace.key` form.
Report text is stored as **key plus arguments** rather than a finished sentence and resolved
when rendered, so a cached report still renders correctly after a language change.

> The engine does not use Minecraft's own `assets/<modid>/lang/*.json`, because the command
> line tool loads no game at all; one shared table is more reliable than maintaining two sets.
> Adding a third language means adding a constant, extending the branches in `detect()`, and
> adding a column to every `put(...)`. The self test fails if any key is missing.

---

## How attribution works

### 1. Index: which class belongs to which jar

Each mod jar's entry table is read (**directory listing only, nothing extracted or written**),
producing two maps:

- **class name to jar**: exact, confidence 1.0
- **package name to jar**: prefix match, at most 0.9, decreasing with depth

Package matching **never falls back to a single-segment package** such as `com` or `org`,
otherwise any `com.*` mod would claim ownership of everyone else's frames. When two jars share
a package, the one contributing more classes there wins.

Descriptor reading covers `META-INF/neoforge.mods.toml`, `META-INF/mods.toml` and Fabric's
`fabric.mod.json`, so a mod's real name, id and version are known regardless of the loader it
was built for.

### 2. Stack parsing

Real logs are messier than the textbook, so the parser handles:

- `at a.b.C.m(File.java:12)` standard frames
- `a.b.C$$Lambda$12/0x...` lambdas and synthetic frames
- `a.b.C$Inner.run(...)` inner classes
- `Native Method` / `Unknown Source` frames without line numbers
- `... 23 more` elision
- **multi-line exception messages**, where the JVM prints the message raw across lines; the
  parser knows where to stop, otherwise the frames that follow get swallowed as message text
- `Caused by:` / `Suppressed:` chains, linked root-cause-outward to match `Throwable.getCause()`
- **real NeoForge frame format**: a trailing `~[module@version/...] {re:classloading}`, plus
  class-name prefixes like `TRANSFORMER/crasher@1.0.0/`, both stripped correctly
- prefixed log lines: `[18:03:20] [main/ERROR] java.lang.XxxException: ...`

Prefixes that never belong to a user's mod are filtered out (`java.`, `net.minecraft.`,
`net.neoforged.`, `org.spongepowered.asm.`, `com.mojang.`, …), along with the internals of the
Fabric loader and Connector, and OBA itself.

### 3. Scoring

| Evidence | Weight |
|---|---|
| Frame in the root cause chain | `0.30 + 0.10 × chain depth` |
| Frame in an outer chain | the above × 0.45 |
| Exact class match | × 1.25 |
| Package match | × (confidence + 0.4) |
| **First located frame of the root cause** | **+0.85** |
| Rule match, FATAL | +1.6 |
| Rule match, ERROR | +1.0 |
| modId mentioned in the text | +0.15 |

Anything below the noise floor (0.18) is dropped so the report is not drowned by unrelated
mods. The weights are published so a wrong verdict can be traced to the step that produced it,
rather than being a black box.

### How freezes are judged

A freeze throws nothing, so the table above is useless. Instead:

1. the watchdog measures the gap between server ticks;
2. past 12 seconds it captures a **thread dump** (`ThreadMXBean`, pure reads);
3. the stalled thread's frames are attributed to mods;
4. deeper frames and the mod nearest the top of the stack are the most suspicious.

This answers "**whose code was running when it hung**", which is an inference rather than proof,
and the report says so. `/oba threads` captures a dump on demand.

---

## MCreator detection

Following the approach of [McreatorDetector-1192](https://github.com/yiyuyan/McreatorDetector-1192)
(metadata text, package names, the procedures package, file name) and extended with the
structural fingerprints MCreator actually leaves in a compiled jar. Detection is
**multi-signal scoring**, not a single condition.

**Decisive signals** (specific to MCreator; their presence is highly suspicious)

| Signal | Points | Note |
|---|---|---|
| Package root is `net.mcreator.*` | 6 | MCreator's default package, the strongest signal |
| A `procedures` package exists | 4 | Each visual procedure compiles into a class |
| `*ModElement` classes exist | 4 | Mod element registration code |
| A resource carries "Generated by MCreator" | 5 | Bounded content probe over small files |
| Metadata mentions `mcreator` | 4 | Name, description, author or link |
| A `variables` package exists | 2 | MCreator's global variable system |
| File name contains `mcreator` | 3 | |

**Supporting signals** (only counted once a decisive signal is present)

| Signal | Points | Note |
|---|---|---|
| Two or more `*Model` / `*Renderer` classes | 2 | Auto-generated entity or armour models |
| Consolidated `ModItems` / `ModBlocks` style registries | 2 | Characteristic of generated code |
| Resource layout matches a generated export | 2 | `models/` plus `data/` plus paired procedure classes |

Below **6** points it is not considered MCreator. That threshold and the "supporting signals do
not count on their own" rule were calibrated against reality: on a real 236-mod pack the
earlier logic flagged **98** mods, including hand-written ones like Alex's Mobs, JEI and
Xaero's Minimap — they merely use `*Model`, `ModItems` and similar **ordinary Java naming that
every mod has**. After the change the same pack yields **2**, and both genuinely carry a
`net.mcreator` package, a `procedures` package and generator banners.

**Every hit is printed**, rather than a bare yes/no, so a false positive can be traced to the
signal that caused it.

> A hand-written mod whose file name contains `mcreator` and whose description says "Made with
> MCreator" will also be flagged. That is intentional: this detects *generation traits*, not
> authorship, and a hit says nothing about the mod's quality.

---

## Graphics mods

Sodium, Iris and their relatives are where packs most often break, and their problems are
**invisible in a stack trace** — they usually do not throw at all, they just crash the render
thread, go black, or fail to load a shader pack. On top of that the family is full of forks
that were renamed:

| Family | Members (the same thing under different names) |
|---|---|
| **Sodium** | Sodium, Rubidium, Embeddium, Xenon, Sodium Reforged, Sodium Forge |
| **Iris** | Iris Shaders, Oculus, NeOculus |
| **Lithium** | Lithium, Radium, Canary |
| **Krypton** | Krypton, Krypton Reforged |
| **Phosphor** | Phosphor, Phosphor Create |

OBA knows the relationships, so it catches two problems **no amount of log reading would
find**.

### 1. Two mutually exclusive members of one family

```
[Graphics mods] (Sodium, Iris and their many forks)
  ! Two members of the Sodium family are installed at once
      - embeddium-0.3.31.jar
      - rubidium-0.7.1.jar
    -> embeddium / rubidium are the same renderer under different names and are mutually
       exclusive. Keep exactly one and remove the rest; having two is a common cause of a
       black screen or a crash on the render thread.
```

### 2. A shader loader with no renderer to attach to

```
  ! oculus has no renderer to attach to
    oculus-1.7.0.jar
    -> oculus is a shader loader with no renderer of its own; it needs embeddium or rubidium.
```

`iris` pairs with `sodium`; `oculus` and `NeOculus` pair with `embeddium` or `rubidium`.
OBA knows which member needs which.

### Fabric builds loaded through Connector are recognised too

Sodium and Iris both ship Fabric builds, and those can end up on NeoForge through Sinytra
Connector. The modId is still `sodium` or `iris` and the classes still live under
`net.caffeinemc.`, `me.jellysquid.mods.sodium.` and `net.irisshaders.`, so **family detection
works identically for the Fabric and Forge builds** with no extra configuration.

### Rules for graphics failures

These logs look nothing like an ordinary error, so they get their own rules:

| Rule | Triggers on | Note |
|---|---|---|
| `graphics.shader_compile` | a shader that fails to compile | The pack does not match this Iris/Sodium build, or the driver rejected the GLSL |
| `graphics.shader_pack` | a shader pack that fails to load | Incomplete pack, wrong game version, or an option the loader does not provide |
| `graphics.sampler` | a missing shader sampler | The advice names the sampler that is missing |
| `graphics.framebuffer` | an incomplete framebuffer | A black screen and missing world rendering usually follow |
| `graphics.mixin_conflict` | mods patching each other's render classes | A frequent source of visual glitches and render-thread crashes |
| `graphics.unsupported_driver` | an old or unsupported driver | Sodium and Iris want a reasonably modern OpenGL 4.5 driver |
| `graphics.glfw_init` | GLFW failing to start | Happens before mods load, so it is a driver or launcher problem |
| `graphics.context_lost` | a lost GL context | Usually a GPU hang caused the driver to reset the device |

### Add-ons are not mistaken for rivals

Add-ons like Sodium Extra or Sodium Dynamic Lights are *supposed* to sit alongside the main
renderer, so conflict detection only considers the two roles that occupy the main slot (render
optimizer and shader loader). This was calibrated on a real pack shipping `embeddium` plus
`sodiumdynamiclights`: the earlier logic reported the dynamic lights mod as "another Sodium
branch".

Also, the file named `rubidium-extra` actually declares the modId `embeddium_extra`, so each
member carries a list of aliases — matching a single id would miss it.

---

## Sinytra Connector support

[Sinytra Connector](https://github.com/Sinytra/Connector) lets Fabric mods run on NeoForge.
With it installed a pack contains three kinds of thing at once: native NeoForge mods, Fabric
mods, and Connector itself. OBA adapts to that mix in four places.

**1. Fabric mods are identified properly**

A Fabric mod's identity lives in `fabric.mod.json`, not `neoforge.mods.toml`. Both are read, so
a Fabric mod keeps its name, modId and version instead of degrading to a bare file name.

**2. Fabric mod frames are no longer discarded as loader code**

The whole `net.fabricmc.` prefix used to be treated as "not the user's code". Under Connector
that is exactly where a Fabric mod's own classes live, so the blanket rule meant a Fabric mod
could never appear in a report. Now only the loader internals are excluded:

| Excluded | Not excluded (participates in attribution) |
|---|---|
| `net.fabricmc.loader.*` | `net.fabricmc.fabric.api.*` (Fabric API, a mod) |
| `net.fabricmc.mappingio.*` | A mod's own packages, e.g. `me.jellysquid.mods.*` |
| `net.fabricmc.accesswidener.*` | |
| `net.fabricmc.tinyremapper.*` | |
| `org.sinytra.connector.locator/transformer/util.*` | `org.sinytra.connector.ConnectorMod` itself |

**3. Three Connector-specific rules**

| Rule | Triggers on | Advice |
|---|---|---|
| `connector.failure` | Connector reporting a remap or transform failure | Check Connector and Fabric API versions, then whether that Fabric mod supports this game version |
| `connector.missing_mod` | `Fabric mod xxx is not present` | Install the missing Fabric mod, or remove the mod depending on it |
| `connector.intermediary` | `class_1234` / `method_123` in the log | Intermediary names were never remapped; usually the mod bypassed Connector or targets another version |

**4. The environment is stated in the report**

When Connector is found in the index, the report adds a note saying Fabric mods will show up
under their own jars and that class names may have been remapped — so a wall of `net.fabricmc`
does not look like a fault.

> One caveat: after Connector remapping, a Fabric mod's **class names may be the remapped ones**
> and not match the author's source. Line numbers are usually accurate, so searching by line
> number is more reliable when the name looks unfamiliar.

---

## Rule set

25 rules over real log text, plus 3 Connector-specific ones and 1 for deliberate crashes, each
carrying concrete advice.

| Category | Rules |
|---|---|
| Dependencies | missing prerequisite, missing mandatory dependency, version mismatch, incompatible mods |
| Duplicate loading | several files for one modId, `Found duplicate mod` |
| Mixin | injection failed, config failed to load, transformation threw, MixinExtras too old |
| Class loading | `NoClassDefFoundError`, `ClassNotFoundException`, `NoSuchMethodError`, `NoSuchFieldError` |
| Registries | missing registry entries, registry conflict |
| Data and resource packs | data pack failed to load, resource pack failed to load |
| File integrity | bundled library missing, corrupt jar |
| Runtime | out of memory, server frozen, graphics driver problem, mod failed to load or construct |
| Connector | transform failure, missing Fabric prerequisite, intermediary names leaked |
| Deliberate | F3+C manual debug crash (reported as intentional) |

Rules only fire on unambiguous text. The structural lines of a crash report itself
(`Description:`, `Time:`, `-- Head --`, `Mod List:`, …) are explicitly excluded, so "the report
describes a crash" is never mistaken for "a crash happened".

---

## Project structure

```
oba/
│
├── README.md                       Simplified Chinese README
├── README.en.md                    This file (English)
├── LICENSE                         Full MIT license text
├── build.gradle                    One build producing one dual-purpose jar
├── settings.gradle                 Plugin repositories and root project name
├── gradle.properties               Versions, mod metadata, supported version range
├── gradlew / gradlew.bat           Gradle wrapper (8.14.3)
│
├── src/main/java/dev/oba/
│   ├── Launcher.java               The only entry point: game or command line, then dispatch
│   │
│   ├── cli/
│   │   └── Cli.java                Command line tool: arguments, subcommands, output
│   │
│   └── mod/                        The in-game half
│       ├── ObaMod.java       Mod entry point: events, index, watchdog
│       ├── Diagnostics.java        Index, log window, report cache, debounce
│       ├── DiagnosticAppender.java log4j2 collector (captures traces live)
│       ├── CrashReporter.java      Writes reports into logs/, prunes old ones
│       ├── Watchdog.java           Process watchdog: tick gaps / deadlocks / memory
│       ├── ObaCommands.java  The /oba command tree
│       └── ClientLanguage.java     Feeds the game language into the engine (client only)
│
├── src/main/resources/
│   ├── oba-icon.png                Mod icon (256x256, the one shown in the mod list)
│   ├── oba-icon-128.png            Same icon, small size
│   └── META-INF/
│       └── neoforge.mods.toml      Mod descriptor (placeholders expanded at build time)
│
├── common/                                     Diagnostic engine (no Minecraft dependency)
│   ├── src/main/java/dev/oba/core/
│   │   ├── OBA.java          Constants: version, modId, log prefix
│   │   ├── Lang.java               Message catalogue in both languages
│   │   ├── Diagnosis.java          Data model: Finding / KnownIssue / Report
│   │   ├── ModSource.java          Read-only view of one mod file (jar or directory)
│   │   ├── Toml.java               Small TOML reader for neoforge.mods.toml
│   │   ├── ClassIndex.java         Class and package name to mod resolution
│   │   ├── StackTraceParser.java   Stack parsing (cause chains, module prefix stripping)
│   │   ├── LogAnalyzer.java        Log and crash report parsing plus the rule table
│   │   ├── Analyzer.java           Orchestration: index, attribute, score, timeline, report
│   │   ├── Timeline.java           Incident timeline: event extraction, time and level detection
│   │   ├── McreatorDetector.java   Multi-signal MCreator detection
│   │   ├── ProcessProbe.java       Thread dump parsing and live JVM capture
│   │   ├── LogWatcher.java         tail -f style watching (handles rotation and encoding)
│   │   └── Reports.java            Text / Markdown / JSON rendering
│   │
│   └── tools/
│       ├── SelfTest.java           Engine self test, plain JDK
│       ├── RuntimeHarness.java     Runtime verification with real log4j2 and real mod classes
│       └── cp-init.gradle          Prints classpaths for the harness above
│
├── tools/                                      Helpers for real-client verification
│   ├── run-instance.ps1            Builds a launch command from the instance's version json
│   └── crasher/CrasherMod.java     Test mod that throws during construction
│
└── examples/                                   Deliberately broken sample directory
    ├── mods/                       Fake mods (duplicate modId, MCreator style, Fabric, Connector)
    ├── logs/                       Sample latest.log and a missing-dependency log
    └── crash-reports/              NeoForge / Fabric / F3+C crash reports
```

`common/src/main/java` is wired into the same source set, so **one project and one jar** carry
the engine, the mod and the command line tool. There is no second copy to keep in sync.

---

## Building

Requires **JDK 21** (Minecraft 1.21 and NeoForge 21.x require Java 21). The first build needs
network access.

```bash
./gradlew build
# -> build/libs/oba-1.0.0.jar

./gradlew dist             # build and print the jar path
./gradlew runClient        # launch a dev client
./gradlew runServer        # launch a dev server
./gradlew runCli --args="analyze --game-dir examples"   # CLI without starting the game
```

Add `--offline` for an offline build, provided the Gradle cache already holds NeoForge and the
decompiled artefacts. Note that `build` also runs the asset download step, which needs network;
`./gradlew jar` is the offline-safe target when you only need the jar.

### Building in IntelliJ IDEA

No plugin needed; IDEA's built-in Gradle support is enough.

**1. Import.** `File → Open` and pick the **project root** (the folder with `build.gradle` and
`settings.gradle`), not `common/` or `src/`. The first sync downloads NeoForge and decompiles
Minecraft, so **expect several minutes**.

Once it settles, check:

- `File → Project Structure → Project`: SDK **21**, language level **21**
- `Settings → Build Tools → Gradle`: Gradle JVM **21** as well
  (with 17 you get `invalid source release` at compile time)

**2. Build.** In the `Gradle` tool window: `oba → Tasks → build`, double click. The
artifact lands in `build/libs/oba-1.0.0.jar`.

**3. Run the game.** Run configurations are generated on import and appear in the dropdown:
`runClient` / `runServer` / `runData` / `runGameTestServer`, with working directories
`run/client` and `run/server`. To see reports appear, run the client, trigger a problem, then
look in `run/client/logs/` for `oba-report-*.txt`.

**4. Run the CLI.** `Gradle` tool window → `Tasks → oba → runCli`, double click; with
arguments use the terminal:

```bash
./gradlew runCli --args="analyze --game-dir examples"
./gradlew runCli --args="mods --game-dir examples --lang en"
```

Or create your own Application configuration: main class `dev.oba.Launcher`, classpath of
module `oba.main`, working directory the project root. Do **not** use
`dev.oba.mod.ObaMod` as the main class — that is the mod entry point and running it
standalone fails because FML is not there.

**Troubleshooting**

| Symptom | Cause and fix |
|---|---|
| Sync hangs at `neoFormDecompile` | First run decompiles Minecraft; give it time, and check the network if it never finishes |
| `invalid source release: 21` | Gradle JVM is not 21; change `Settings → Build Tools → Gradle → Gradle JVM` |
| No `runClient` in the configuration list | Gradle has not finished syncing; `Reload Gradle Project` |
| `build.gradle` changes have no effect | Run `Reload Gradle Project` manually |
| `net.neoforged` cannot be resolved | NeoForge dependencies did not finish downloading; delete `.gradle` and re-sync |

---

## Verification

| What | How | Result |
|---|---|---|
| Engine logic | `java common/tools/SelfTest.java` | **127 / 127 assertions pass** |
| Engine coverage | indexing, attribution, cause chains, mixin failure, missing dependency, duplicate mods, thread dumps, MCreator detection, three report formats, exploded mods, languages, Connector mixed packs, real NeoForge frame format, log encodings, F3+C, paths with spaces, incident timeline, **graphics families and conflicts** | pass |
| **Runtime behaviour** | `common/tools/RuntimeHarness.java` (real log4j2 + real mod classes) | **52 / 52 assertions pass** |
| Runtime coverage | log capture, cause chain fidelity, automatic verdict, report written into logs, F3+C crash-report pickup, bilingual rendering, thread dumps, read-only guarantee | pass |
| One jar, two jobs | `java -jar ... version` / simulated game environment / `-Doba.mode=cli` | all three dispatch correctly |
| **Real client** | 1.21.1 + NeoForge 21.1.256, with a mod that throws during construction | **correctly names `CrasherMod.java:13`, report lands in `logs`** |
| Real modpack | 236 mods / 52,313 classes / 1 GB | scanned in 1 second; found a genuinely duplicated coroutil and placed 5 graphics mods into their families |
| Real GBK log | 61,380-line Chinese modpack log | read cleanly; 69 of 1,128 frames attributed |
| Paths with spaces | `--game-dir "...my games..."` (quoted and unquoted) | both forms give identical results |

### Running the runtime harness

`RuntimeHarness` drives the **real mod classes** against a **real log4j2 backend**:

1. it writes an ERROR with a `Caused by` chain through log4j2 and checks the collector receives
   the exception type, message, every frame's line number and the cause chain intact;
2. it calls the mod's own `Diagnostics.analyzeWindow()` and checks the debounced verdict names
   the right mod, gives the line, and states its read-only stance;
3. it calls `CrashReporter` and checks the report really lands in `logs`, that
   `oba-latest.txt` exists, and that an F3+C crash report appearing mid-session is picked up;
4. it snapshots the input directory before and after and asserts that **every mod file's SHA-1
   is unchanged** — turning "read only" from a claim into a checked fact.

```bash
# 1) real dependency classpath (works offline)
./gradlew -I common/tools/cp-init.gradle printClasspaths --offline -q
#    keep the COMPILE_CP= line

# 2) compile and run
javac -encoding UTF-8 -proc:none -cp "$COMPILE_CP" -d build-rt \
  common/src/main/java/dev/oba/core/*.java \
  src/main/java/dev/oba/*.java \
  src/main/java/dev/oba/mod/*.java \
  common/tools/RuntimeHarness.java
java -Dstdout.encoding=UTF-8 -cp "build-rt:$COMPILE_CP" dev.oba.tools.RuntimeHarness

# 3) and again in the other language
java -Doba.lang=en -cp "build-rt:$COMPILE_CP" dev.oba.tools.RuntimeHarness
```

### What this verification has caught

It is not a formality. It found defects that compile cleanly and still leave the tool useless:

| Defect | Symptom | Cause |
|---|---|---|
| Collector never fired | `install()` reported success and the appender was in the root logger's table, but no event ever arrived | `Configuration.addAppender` only registers; log4j2 dispatches through appender *references*, and `getAppenderRefs()` is a stale snapshot |
| No error ever triggered a diagnosis | Lines reached the window but no verdict was produced | log4j2's `Level.isInRange(min, max)` is a *minimum severity* check with inverted ints (FATAL=100 < ERROR=200), so `isInRange(ERROR, FATAL)` is an empty range |
| Chinese text inside English reports | Body stayed Chinese after switching language | The evidence chain stored an already-rendered rule title as an argument |
| Fabric mods could never be blamed under Connector | Their classes were indexed, but with no modId | A Fabric mod's identity is in `fabric.mod.json`, and only `neoforge.mods.toml` was read |

### Real-client verification

Running an actual client and real modpacks found six more, all of which only surface live:

| Defect | Symptom | Cause |
|---|---|---|
| A mod crashing during construction was missed entirely | The game log held a full exception; OBA said nothing | The collector was installed in `FMLCommonSetupEvent`, which only fires once **every** mod has been constructed |
| Reports said "no frame could be attributed" | A perfectly good exception was right there | NeoForge appends `~[module@version/...]` and prefixes class names with `TRANSFORMER/crasher@1.0.0/`; the regex and the index both failed to match |
| One report snowballed into a dozen | A new report every 2–3 seconds, each longer | OBA's own verdict was read back by the log watcher and looked like a new stack trace |
| Chinese modpack logs were unreadable | `Input length = 1` | The log was GBK and the code read UTF-8 unconditionally |
| 98 MCreator false positives | 98 of 236 mods flagged | Ordinary Java naming (`*Model`, `ModItems`) was treated as an MCreator trait |
| Blaming itself, and freeze false alarms | Saving chunks on world exit was called a freeze, and the culprit was OBA's own thread | Routine vanilla heavy work tripped the watchdog, and thread selection picked its own watcher |

### Feedback from a second device


| Defect | Symptom | Cause |
|---|---|---|
| A path containing spaces failed | Quoting made the quotes part of the path: `Illegal char <">` | The shell does not strip quotes for the program |
| F3+C crashes were invisible | Nothing in the report, the verdict or the log | A manual debug crash never goes through log4j2; only a file under `crash-reports` |
| Reports snowballing again | `/oba analyze` output was re-analysed repeatedly | Chat-rendered lines carry only `[CHAT]`, no log prefix, so they bypassed the self-output filter |

### Still unverified

The `/oba` command tree and the watchdog tick loop need a human to type commands in game
or at a server console (the development machine's instance only has a client jar, so a dedicated
server cannot be started, and no game window is available for key injection). They remain
covered only at the "real mod classes plus real logging backend" level.

The same applies to F3+C: the pickup logic is covered by the harness, but nobody has actually
pressed the key combination yet.

**The remaining 1.21.x minors** (1.21.2 / 1.21.3 / 1.21.4 / 1.21.5 …) have not been verified on
a live client; with only NeoForge 21.1.256 cached locally, cross-compilation could not be checked
either.

---

## Known limits

**Does**

- attribute stack frames to a mod file with class, method and line
- build an **incident timeline** showing what each mod was doing around the failure
- recognise 25 unambiguous error patterns, plus Connector and deliberate-crash rules
- name who was executing when the game froze, without an exception
- detect MCreator generation traits and show its reasons
- keep Fabric mods attributable when Sinytra Connector is present
- detect log encoding from content, so both UTF-8 and GBK logs are readable
- accept paths containing spaces, quoted or not
- speak English and Chinese, and never write to your files

**Does not**

- **obfuscated mods**: line numbers usually survive, class names may not be attributable
- **Fabric class names may not match the source**: after Connector remapping, search by line number
- **code outside the mods folder**: Java agents, coremods and launcher injections are not
  indexed and show up under "packages that could not be attributed"
- **a client crash on a multiplayer server**: the client log or crash report has to be brought over
- **"plugins"**: NeoForge has no separate plugin system like Bukkit/Spigot, so everything here is
  a mod
- **attribution is not blame**: the most suspicious mod is not necessarily the buggy one; A may
  only fail because B corrupted shared state first. The report gives the evidence chain, the
  final call is yours
- **freeze analysis is inference**: the mod at the top of the stack is the first thing to test,
  not proof of an infinite loop

---

## FAQ

**Will it disable the broken mod for me?**

No, never. That is a hard design constraint: no writes, moves, renames or deletes of any mod
file. When advice says "try moving that jar out", the next sentence says the operation is yours
to perform.

**Will reports fill up my logs folder?**

No. Only the 10 most recent are kept; older ones are deleted. `oba-latest.txt` always
points at the newest, which is convenient for scripts or for just opening it.

**Why was my mod not named?**

Look at the "packages that could not be attributed" section first. If every frame comes from
vanilla and the loader, then there genuinely is no mod to blame — an F3+C debug crash is the
typical case. If a match was by package prefix rather than exact class, the confidence is
discounted and worth keeping in mind.

**Does it slow the game down?**

The index is built once at startup (about 1 second for 236 mods) and costs nothing afterwards.
The log collector does string checks only, and the watchdog wakes every 15 seconds.

**How do I share a report?**

Send `logs/oba-latest.txt` as is; it contains the culprit, the line, the SHA-1 and the
full evidence chain. For a forum or issue, `--format md` produces tidier Markdown.

**Can it analyse logs from another machine?**

Yes. Take the other machine's `logs/` and `mods/` folders and run
`java -jar oba.jar analyze --game-dir <that folder>`. The game does not need to be running.

---

## Contributing

Issues and pull requests are welcome. To make a bug report easy to act on, please include:

1. `logs/oba-latest.txt`, if a report was already produced
2. the original log or crash report
3. game version, NeoForge version, OBA version
4. if OBA itself is erroring, the lines starting with `[OBA]`


---

## Credits

- **Special thanks**[XinKo_TS](https://space.bilibili.com/1555260755?)**Support and maintenance for this mod**
- [McreatorDetector-1192](https://github.com/yiyuyan/McreatorDetector-1192) — reference implementation for MCreator detection
- [Sinytra Connector](https://github.com/Sinytra/Connector) — bridging Fabric mods onto NeoForge
- [NeoForged](https://neoforged.net/) and [MinecraftForge](https://github.com/MinecraftForge) — loader and event system
- The testers who provided real modpacks and crash logs
- Deepseek does multilingual translation
---

## License

Released under the **MIT License**, full text in [LICENSE](LICENSE).

```
MIT License

Copyright (c) 2026 OBA contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

In short: use it, modify it, redistribute it, even commercially, as long as the copyright
notice stays; the authors accept no liability.

---

<!-- ============================================================ -->
<!-- The sections below are reserved placeholders.               -->
<!-- Headings and purpose are fixed; the bodies are to be filled. -->
<!-- ============================================================ -->

## Appendix A: community content to be written

> **📌 This section is a reserved placeholder.**

---

## Appendix B: glossary

| Term | Meaning |
|---|---|
| **Culprit** | The mod file with the highest evidence weight. "Most suspicious", not "convicted" |
| **Attribution** | Mapping a stack frame or log line back to the mod it belongs to |
| **Evidence chain** | Every point awarded in support of a conclusion, listed with its weight |
| **Confidence** | The band derived from evidence weight (high / very high), not a probability |
| **Root cause exception** | The innermost exception in the `Caused by` chain, usually where things actually broke |
| **Loader frames** | Frames belonging to FML, ModLauncher, Mixin and friends; they never participate in mod attribution |
| **Incident timeline** | The events around a failure, arranged in time order, with the responsible mod on each |
| **Noise floor** | Suspects below this weight are discarded so the report is not drowned by unrelated mods |
| **F3+C** | Minecraft's built-in manual debug crash key combination, used to produce a crash report on purpose |

---

<div align="center">

**One BUG Among Us** · read-only diagnosis · never touches your files

[MIT License](LICENSE) · [简体中文](README.md)

</div>
