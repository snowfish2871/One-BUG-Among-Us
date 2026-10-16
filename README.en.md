# One BUG Among Us

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

**Crash diagnostics for Minecraft 1.20.2 / 1.20.3 / 1.20.4 on the NeoForge 20.2 / 20.3 / 20.4 lines** · abbreviated **OBA**

It names the mod file that broke the game, the class and line where it broke, and what every
mod was doing in the moments before and during the failure.

> This is the **1.20.2 - 1.20.4 track**, running on NeoForge. Verified in game on 1.20.2 + NeoForge 20.2.93.
> This directory produces `oba-mc1.20.2-1.20.4-1.0.1.jar`.

English | [简体中文](README.md)

> **Diagnosis only. This project never modifies a single file.**
> It does not disable, move, rename or delete any mod. Everything it concludes is printed
> and written to a report; acting on it is your decision.

---

## Compatibility

Releases are organised into **version tracks**: one track covers every Minecraft minor that
shares the same API, and within a track the mod code needs no changes at all. The split exists
because NeoForge changed APIs at specific points, and cramming everything into one jar would
make the versions hold each other back.

| | |
|---|---|
| Minecraft | 1.20.2 / 1.20.3 / 1.20.4 |
| Loader | the NeoForge 20.2 / 20.3 / 20.4 lines |
| Java | 17 |
| Built against | `1.20.2` / `20.2.93` |
| Status | **verified on a live client** |

> **1.20.1 runs on Forge, not NeoForge.** NeoForge had not split off yet — the loader was
> still MinecraftForge with `net.minecraftforge.*` packages, an entirely different codebase
> from today's `net.neoforged.*`. That is why 1.20.1 gets its own Forge track.
> **1.20 is not supported at all**: it predates both.

### Building

This directory is self-contained; build it right here:

```bash
./gradlew jar
```

The jar lands in `build/libs/`. The build uses the sources under this directory's `src/main`.

### Other tracks

The same mod is released per **version track**, one directory each:

| Directory | Minecraft | Loader |
|---|---|---|
| `versions/1.21.1-neoforge/` | 1.21 - 1.21.x | NeoForge 21.1+ |
| `versions/1.20.5-1.20.6-neoforge/` | 1.20.5 / 1.20.6 | NeoForge 20.5 / 20.6 |
| `versions/1.20.2-1.20.4-neoforge/` | 1.20.2 / 1.20.3 / 1.20.4 | NeoForge 20.2 / 20.3 / 20.4 |
| `versions/1.20.1-forge/` | 1.20.1 | Forge 47 |

There is one copy of the engine source, under `engine/`. The sources inside each version
directory are per-loader copies assembled from it by `tools/sync-versions.ps1` — edit the engine
there and re-run that script.

Each track also has a branch of the same name (`1.21.1-neoforge`, `1.20.1-forge`, and so on)
holding that version's self-contained project; if you only care about one version, cloning that
branch is the shorter route.

### The loader adapter layer

This section is about **running the same diagnostic logic on both loaders**.

The shared code references no loader classes at all; it only knows three interfaces in
`dev.oba.loader`:

| Interface | Purpose |
|---|---|
| `LoaderEnv` | list mods, get the game directory, loader name |
| `TickHook` | hook the server tick (three different event classes across versions) |
| `Loader` | holds whichever implementation the current track installed |

Each track compiles one implementation on top:

| Directory | Used by |
|---|---|
| `src/loader/neoforge/` | every NeoForge track from 1.20.2 up, including 1.21.x |
| `src/loader/forge/` | the 1.20.1 Forge track |

A track's `build.gradle` adds only its own copy to the source set, so no jar ever contains both.
The parts that cannot be abstracted — the `@Mod` entry point, event registration, the event
classes themselves — live in each implementation, because the two loaders share no common
superclass. All the actual work sits in the shared `ObaLifecycle`.

### The 1.20.1 (Forge) track

That track builds with ForgeGradle on the official toolchain, and reobfuscation is part of the
build, so it takes longer than the other three and needs JDK 17. The jar lands in `dist/` like
the rest.

> One build snag worth writing down, in case another machine hits the same stall: the first
> ForgeGradle run downloads and deobfuscates all of 1.20.1, and `maven.neoforged.net` is not
> reachable here. The other tracks get around that by rewriting repository URLs; ForgeGradle's
> own `MavenArtifactDownloader` cannot be redirected the same way, so it sits in
> `DownloadUtils.downloadFile` with accumulated CPU time flat and the cache no longer growing.
> Pointing it at the official CDN mirror fixed it.

**Why 1.20.1 cannot simply reuse the 1.20.2 recipe:** the NeoForge tracks build uniformly
because from 1.20.2 on the loader packages are all `net.neoforged.*`. Forge on 1.20.1 runs SRG
names in production (`net.minecraft.commands.Commands.m_82127_`) while the client jar itself is
obfuscated (`dt.a`), so a full deobfuscation pipeline is unavoidable — hence ForgeGradle.

### Why the tracks are cut where they are

The cut between 1.20.4 and 1.20.6 exists because **1.20.5 reworked the tick events**:

| Version | What the tick event looks like |
|---|---|
| 1.20.2 – 1.20.4 | `net.neoforged.neoforge.event.TickEvent$ServerTickEvent`, carrying a `Phase` (START / END) — **fires twice per tick** |
| 1.20.6 and later | `net.neoforged.neoforge.event.tick.ServerTickEvent$Post`, split into Pre / Post subclasses — **fires once per tick** |

Not only the fully qualified name differs but the constructors and the class hierarchy too, so
at compile time you can only pick one. The part that crosses that line is resolved at **runtime**
by `TickHook`, which probes for whichever shape is present — so both tracks share one copy of
the source.

### Verified and not verified

| | Status |
|---|---|
| 1.21.1 + NeoForge 21.1.256 | **verified on a real client** |
| 1.20.2 + NeoForge 20.2.93 | **verified on a real client** |
| The 1.20.5 - 1.20.6 track | **verified on a live client** |
| 1.20.1 + Forge 47.4.26 | **verified on a real client** |
| Other minors inside a track | expected to work on the "same API within a track" argument, not verified one by one |

If OBA itself errors on some version, the log will contain
`[OBA] could not start up on this version`, `[OBA] could not register ...` or
`[OBA] could not hook the server tick event`. Report that line together with your version
numbers and it can be looked at.

### Verified in game

All four tracks were run on a real client: a test mod that **deliberately throws in its
constructor** was dropped into `mods`, the client was started, and the question was whether OBA
would name the culprit on its own and write the report into `logs`.

| Track | Client | Result |
|---|---|---|
| 1.21.x | 1.21.1 + NeoForge 21.1.256 | **named CrasherMod.\<init\> (CrasherMod.java:15)**, report written |
| 1.20.5 - 1.20.6 | 1.20.5 + NeoForge 20.5.21-beta | same, report written |
| 1.20.2 - 1.20.4 | 1.20.2 + NeoForge 20.2.93 | same, report written |
| 1.20.1 | 1.20.1 + Forge 47.4.26 | same, on an exact class name match |

Two problems only showed up by actually running it, and both are fixed now:

**1. The descriptor has a different name in each version range**

NeoForge 20.2 - 20.4 looks for `META-INF/mods.toml`; the rename to
`META-INF/neoforge.mods.toml` only happened in 20.5. With the wrong name the loader does not even
read the file, which shows up as "two files in the mods folder but nothing loaded" and a single
`found 2 invalid mod files` line. Both descriptors now ship, and each track excludes the other.

**2. The old dependency block has an extra required field**

In 20.2 - 20.4 a `[[dependencies]]` block requires `mandatory`, and without it you get
`Missing required field mandatory in dependency`. The field also has to come *before* `type` —
the loader reads those keys as a stream, in order.

### About 1.20

**1.20 — meaning the original 1.20, not 1.20.1 — is not supported.** NeoForge only started
publishing on its own with **1.20.2**, so that version range has no usable loader combination.
Drop the NeoForge build of OBA into a 1.20.1 Forge instance and the loader rejects it outright:

```
Mod ID: 'neoforge', Requested by: 'oba', Expected range: '[20.2.0,20.5)', Actual version: '[MISSING]'
net.minecraftforge.fml.ModLoadingException: Mod 'oba' requires 'neoforge' '20.2.0 or above'
```

That instance carries `net.minecraftforge:eventbus:6.0.5`, `net.minecraftforge:forgespi:7.0.1`
and `net.neoforged.fancymodloader:loader:47.2.2` — that is **MinecraftForge 47.2.2**, with
`net.minecraftforge.*` packages. 1.20.1 therefore has its own Forge track producing its own jar,
and the two lines never meet.

### Why it works across minor versions

The mod depends only on the parts of the API that are **stable across minor versions**: the
logging backend (log4j2), mod lifecycle events, the event bus, command registration and chat
text. It never touches rendering, world data or registry contents, so it does not break when
those change. The only place it did run into a version difference is the tick event, now
isolated behind the adapter.

Initialisation is also isolated by design: the log collector, the class index, event
registration, the command tree and the tick adapter each run in their own guarded block. If one
of them throws on a version where an API shifted, only **that one feature** is disabled, and a
line is left in the log to say so. A diagnostic tool has no business being the reason a game
fails to start.

### Adding a new track

When NeoForge moves to a new Minecraft version, three steps are enough:

1. Copy `versions/1.20.5-1.20.6-neoforge/` to `versions/<new version>-neoforge/`;
2. Change `minecraft_version`, `minecraft_version_range`, `neo_version`, `neo_version_range`,
   `archive_suffix` and the `parchment_*` keys in its `gradle.properties`;
3. Add the new directory to the lists in `tools/sync-versions.ps1` and `tools/build-all.ps1`,
   then sync and build.

Shared code is copied out of `engine/` by `tools/sync-versions.ps1`, so **nothing is
hand-duplicated** and a new track picks up every engine change automatically. If the new version
moves an API again, add another runtime adapter shaped like the `TickHook` implementations under
`src/loader/`.


---

## Quick start

### In game

1. Put `oba-1.0.1.jar` into `.minecraft/mods/`
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
java -jar oba-1.0.1.jar analyze

# point at a game directory (quoted or unquoted, paths with spaces are fine)
java -jar oba-1.0.1.jar analyze --game-dir "D:\my games\.minecraft"

# feed it one crash report
java -jar oba-1.0.1.jar analyze crash-reports/crash-2026-10-09_18.03.20-client.txt

# list mods, find duplicates, spot MCreator, mark Fabric mods
java -jar oba-1.0.1.jar mods --game-dir "D:\MC\.minecraft"

# follow the log while you play
java -jar oba-1.0.1.jar watch "D:\MC\.minecraft\logs\latest.log"
```

### Try the bundled examples

`examples/` is a deliberately broken mini game directory: six fake mods, three crash reports
and a missing-dependency log.

```bash
# a NeoForge mod blows up
java -jar oba-1.0.1.jar analyze examples/crash-reports/crash-2026-10-09_18.03.20-client.txt

# a Fabric mod blows up under Connector
java -jar oba-1.0.1.jar analyze examples/crash-reports/crash-2026-10-10_11.35.02-client.txt

# an F3+C debug crash
java -jar oba-1.0.1.jar analyze examples/crash-reports/crash-2026-10-10_16.47.12-client.txt

java -jar oba-1.0.1.jar mods     --game-dir examples
java -jar oba-1.0.1.jar mcreator --game-dir examples
```

Expected: the first names `awesome-machines-2.1.0.jar` at `MachineRegistry.java:143`; the
second names `sodium-extra-0.6.0.jar` at `MixinOptionsScreen.java:47`; the third says the crash
was deliberate; `mods` reports the duplicate `sodium` modId; `mcreator` flags
`coolstuff-1.0.1.jar`.

---

## The interface

There are two: the command line, and the in-game chat. Both share one look - a double-line
title box with the version top right, the `◈` mark, section rules, and a highlight bar on the
current item - and **menu entries are clickable in both.**

### Command-line menu

Run it with no arguments:

```bash
java -jar oba-1.0.1.jar
```

```
╔════════════════════════════════════════════════════════════════╗
║  ◈ One BUG Among Us CLI                             OBA v1.0.1 ║
╚════════════════════════════════════════════════════════════════╝

  Workspace   D:\hmcl\.minecraft
  Status      ● Ready
  Build       One BUG Among Us 1.0.1

  • Choose an action ──────────────────────────────────────────────

   ▶  [1]  Diagnose the newest crash or log            run
  ╭──────────────────────────────────────────────────────────╮
  │ [2]  List loaded mods                            run │
  ╰──────────────────────────────────────────────────────────╯
  │ [3]  Find MCreator generated mods                run │
  │ [4]  Follow latest.log live                      follow │
  │ [5]  Capture and attribute a thread dump         capture │
  │ [6]  Show process and index status               run │
  │ [7]  Switch language                             switch │
  │ [8]  Open the newest report file                 open │
  │ [9]  Quit                                        quit │

  ↑ ↓ move    ENTER confirm    number keys jump    Q quit
```

- `↑` `↓` move, `ENTER` confirms, `Q` quits. Number keys `1`-`9` jump straight to an entry.
- **Coloured names are hyperlinks.** In a terminal that supports them (Windows Terminal,
  iTerm2, most modern emulators), clicking one runs that single command in a **new window**,
  so the menu stays where it is. Under the hood each entry gets a one-shot launcher script and
  the script path is wrapped in an OSC 8 hyperlink; a terminal that does not support them
  ignores the control codes and shows plain text, so nothing breaks. The scripts live in a temp
  directory and are deleted on exit.
- To turn that off: `-Doba.clickable=false`, or the environment variable
  `OBA_CLICKABLE=false`.
- **Piped, redirected, or run in CI (not an interactive terminal) it never shows the menu** -
  it prints the usage instead, so control codes never leak into a log.

Colour is decided in the order `-Doba.color` → `OBA_COLOR` → "is this an interactive
terminal". A non-empty `NO_COLOR`, `TERM=dumb`, or redirected output turns it off.

### In game

`/oba` with no arguments is the clickable menu:

```
╔════════════════════════════════════════════════════════════╗
║  ◈ One BUG Among Us                              OBA v1.0.1 ║
╚════════════════════════════════════════════════════════════╝

  • Choose an action ──────────────────────────────────────

  █ ▶ [1] Show the newest full report                      █
    [2] Process and index status                   run
    [3] List loaded mods                           run
    [4] Find MCreator generated mods               run
    [5] Capture and attribute a thread dump        run
    [6] Diagnose the newest crash report           run
    [7] Rebuild the mod index                      run

  ────────────────────────────────────────────────────────
  • Commands that need an argument ────────────────────────
    /oba info <id>      a mod's description and metadata
    /oba analyze <file> diagnose one file

  Anything with a coloured marker can be clicked.
  https://github.com/OneBUGAmongUs/oba
```

The first row (the highlighted bar) is the recommended action. It *is* item 1 and it is clickable
itself, so the list continues at `[2]`.

Every line is clickable and runs its subcommand. Beyond the menu:

| Where | Click what | What happens |
|---|---|---|
| the mod list from `/oba mods` | a mod name | runs `/oba info <id>` for its description |
| a path in a report | the path | fills it into the chat box, ready to run |
| `/oba info <id>` on the help page | that command | fills it in for you to complete |
| the project URL on the help page | the URL | opens it in a browser |

The chat box does not render ANSI, so the in-game view is built directly from the same visual
elements as a component tree: the same double-line title box, the same section rules and highlight
bar, and the same semantic colours (frame, brand, emphasis, body, hint).

Widths work nothing like a terminal: **the vanilla chat font is not monospaced** — a space is
4 pixels, `i` is 2, a lowercase letter is 6, a CJK glyph is 9. Every in-game line is therefore
measured in pixels before it is padded, with no trailing spaces and over-long lines wrapped by
pixel width. `GameUi` carries a width table measured from the vanilla bitmap font and re-calibrates
it against the client font metric once in game, so the three lines of the title box come out exactly
equally wide and mixed Chinese/English never drifts out of alignment.

Report bodies are assembled by the engine for a **100-column terminal**, so dropping them into the
320-pixel-wide chat would make vanilla wrap them on its own — and the leftovers look like garbage
(a severity rule such as `── fatal ─────…` breaks apart into several rows of dashes). Before anything
reaches chat it is therefore reflowed to the chat width: lines that are mostly rule glyphs collapse
into one full-width separator, everything else wraps by pixel width, and colours are carried per
segment. A bold glyph takes one extra pixel, which is counted as well — otherwise a bold section
header or highlight bar ends up exactly one pixel too wide and vanilla folds it onto a second line.

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
OBA v1.0.1
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
  License       GPL-3.0
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
| `--color` | force colours, even inside a pipe |
| `--no-color` | disable ANSI colours; wins over everything else |
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
java -jar oba-1.0.1.jar analyze --lang en --game-dir examples
OBA_LANG=en java -jar oba-1.0.1.jar mods --game-dir examples
```

All strings live in one table (`engine/main/java/dev/oba/core/Lang.java`) keyed in `namespace.key` form.
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

This directory is **self-contained**: everything listed below is what this version
compiles, and it can be copied out and built on its own. The authoritative copy of the engine
lives in `engine/` at the repository root; the `core/` here is a copy synced by
`tools/sync-versions.ps1` — edit the engine there, then re-run that script.

```
1.20.2-1.20.4-neoforge/
│
├── README.md                        Simplified Chinese README
├── README.en.md                     This file (English)
├── build.gradle                     Build script
├── settings.gradle                  Plugin repositories and root project name
├── gradle.properties                Version, mod metadata, supported version ranges
├── gradlew                          Gradle wrapper (8.14.3)
├── gradlew.bat                      Same, for Windows
├── mirror.init.gradle               Rewrites the dead maven host to the official CDN mirror
│
├── gradle/wrapper/                 Gradle wrapper bootstrap files
│
├── src/main/java/dev/oba/
│   ├── Launcher.java                Single entry point: picks in-game or CLI, then dispatches
│   │
│   ├── cli/
│   │   └── Cli.java                 The CLI: argument parsing, subcommands, output
│   │
│   ├── core/                          The diagnostic engine (compiled into this build)
│   │   ├── Analyzer.java            Orchestration: index, attribution, scoring, timeline, report
│   │   ├── ClassIndex.java          Class/package to mod index and resolution
│   │   ├── Diagnosis.java           Data model: Finding / KnownIssue / Report
│   │   ├── GraphicsMods.java        Graphics families: Sodium/Iris forks and conflict detection
│   │   ├── Lang.java                Chinese/English message table
│   │   ├── LogAnalyzer.java         Log and crash report parsing plus the rule table
│   │   ├── LogWatcher.java          tail -f style log watching (rotation and encoding)
│   │   ├── McreatorDetector.java    Multi-signal MCreator detection
│   │   ├── ModSource.java           Read-only view of one mod file (jar or exploded directory)
│   │   ├── Oba.java                 Constants: name, version, modId, log prefix
│   │   ├── ProcessProbe.java        Thread dump parsing and live JVM capture
│   │   ├── Reports.java             Text / Markdown / JSON rendering
│   │   ├── StackTraceParser.java    Stack parsing (cause chains, module prefix stripping)
│   │   ├── Theme.java               
│   │   ├── Timeline.java            Incident timeline: event extraction, timestamps, levels
│   │   └── Toml.java                Minimal TOML parsing (reads the mod descriptor)
│   │
│   ├── loader/                        Loader adapter layer
│   │   ├── LoadedMod.java           One mod as reported by the loader
│   │   ├── Loader.java              Holds whichever loader implementation this build installed
│   │   ├── LoaderEnv.java           Loader adapter interface: list mods, resolve the game directory
│   │   ├── TickHook.java            Server tick hook interface
│   │   └── neoforge/
│   │       ├── NeoForgeEnv.java       NeoForge loader adapter
│   │       └── NeoForgeTickHook.java  NeoForge tick hook, with cross-version probing
│   │
│   └── mod/                           The in-game half
│       ├── ClientFont.java          
│       ├── ClientLanguage.java      Feeds the game language to the engine (client only)
│       ├── CrashReporter.java       Writes reports into logs/, prunes old ones, never overwrites a strong verdict
│       ├── DiagnosticAppender.java  log4j2 appender capturing stack traces live
│       ├── Diagnostics.java         Index, log window, report cache, debounced scheduling
│       ├── EarlyLanguage.java       Pins the report language early, from options.txt
│       ├── GameUi.java              
│       ├── ObaCommands.java         The /oba command tree
│       ├── ObaLifecycle.java        Shared lifecycle: init, index, watchdog, exit hook
│       ├── ObaMod.java              Mod entry point: event registration, index, watchdog
│       ├── UiShot.java              
│       └── Watchdog.java            Process watchdog: tick gaps, deadlocks, memory
│
├── src/main/resources/
│   ├── oba-icon.png                 Mod icon (256x256, the one shown in the mod list)
│   ├── oba-icon-128.png             Same icon, small size
│   └── META-INF/
│       └── mods.toml                Mod descriptor (placeholders expanded at build time)
│
└── docs/banner.svg               Title banner used by the READMEs
```

> The jar from this directory is also executable: `java -jar build/libs/*.jar help`.

## Building

Build all four versions at once; the jars land in `dist/`:

```bash
pwsh tools/build-all.ps1
# -> dist/oba-mc1.21.1-1.0.1.jar
#    dist/oba-mc1.20.2-1.20.4-1.0.1.jar
#    dist/oba-mc1.20.5-1.20.6-1.0.1.jar
#    dist/oba-mc1.20.1-forge-1.0.1.jar
```

Or build just one — every version directory is self-contained:

```bash
cd versions/1.21.1-neoforge && ./gradlew jar
cd versions/1.20.1-forge    && ./gradlew jar
```

If you cloned a **version branch** (say `1.21.1-neoforge`), that branch holds only that one
version and its root is the Gradle project, so `./gradlew jar` right there is all it takes.

| Version | JDK needed | Notes |
|---|---|---|
| 1.21.x (NeoForge) | **21** | |
| 1.20.5 - 1.20.6 (NeoForge) | **21** | |
| 1.20.2 - 1.20.4 (NeoForge) | **17** | |
| 1.20.1 (Forge) | **17** | uses ForgeGradle, the build includes a reobf step |

The first build needs network access (it downloads and decompiles that Minecraft version).
After that you can pass `--offline` to reuse the Gradle cache.

> On this machine `maven.neoforged.net` is unreachable; the two NeoForge 1.20 tracks work around
> that with `mirror.init.gradle`, which rewrites repository URLs to the official CDN mirror.
> `build-all.ps1` passes it automatically.

```bash
./gradlew build
# -> build/libs/oba-1.0.1.jar

./gradlew dist             # build and print the jar path
./gradlew runClient        # launch a dev client
./gradlew runServer        # launch a dev server
./gradlew runCli --args="analyze --game-dir examples"   # CLI without starting the game
```

Add `--offline` for an offline build, provided the Gradle cache already holds NeoForge and the
decompiled artefacts. Note that `build` also runs the asset download step, which needs network;
`./gradlew jar` is the offline-safe target when you only need the jar.

### Building with IntelliJ IDEA

No plugin needed; IDEA's built-in Gradle support is enough.

**Open a version directory, not the repository root.** The default branch is the overview plus the
engine and scripts; the actual Gradle projects live in `versions/<version>-<loader>/` (on a
version branch, the branch root is the project).

**1. Open.** `File -> Open` and pick the **version directory** you want to work on, for example
`versions/1.21.1-neoforge` (it contains `build.gradle` and `settings.gradle`). The first sync
downloads and decompiles that Minecraft version, so **it takes a few minutes**.

Then check:

- `File -> Project Structure -> Project`: SDK set to the JDK that version needs
- `Settings -> Build Tools -> Gradle`: Gradle JVM matching
  (a mismatch shows up as `invalid source release` at compile time)

| Version | JDK |
|---|---|
| 1.21.x (NeoForge) | **21** |
| 1.20.5 - 1.20.6 (NeoForge) | **21** |
| 1.20.2 - 1.20.4 (NeoForge) | **17** |
| 1.20.1 (Forge) | **17** |

**2. Build.** Gradle tool window -> `Tasks` -> `build`, double click. The jar lands in
`build/libs/`.

**3. Run the game.** The three NeoForge versions generate `runClient` / `runServer` run
configurations; so does the Forge one. To watch a report being written, run a client and then
look for `oba-report-*.txt` under `run/client/logs/`.

**4. Run the CLI.** The NeoForge versions have a `runCli` task:

```bash
cd versions/1.21.1-neoforge
./gradlew runCli --args="analyze --game-dir ../../examples"
./gradlew runCli --args="mods --game-dir ../../examples --lang en"
```

Or use the jar directly:

```bash
java -jar build/libs/oba-mc1.21.1-1.0.1.jar analyze --game-dir ../../examples
```

Do **not** use `dev.oba.mod.ObaMod` as the main class — that is the mod entry point and running
it standalone fails on missing FML. The main class is `dev.oba.Launcher`.

**When editing the engine:** the `core/` inside a version directory is a synced copy of
`engine/`. After editing `engine/`, run `pwsh tools/sync-versions.ps1`, or the version
directories keep the old code.

**Troubleshooting**

| Symptom | Cause and fix |
|---|---|
| Sync stuck on `neoFormDecompile` | the first run decompiles Minecraft; wait, and check the network if it stalls |
| `invalid source release` | Gradle JVM does not match the JDK that version needs |
| No `runClient` in the run configurations | Gradle has not finished syncing; `Reload Gradle Project` |
| `build.gradle` edits have no effect | `Reload Gradle Project` manually |
| `net.neoforged` / `net.minecraftforge` not found | dependencies not fully downloaded; delete `.gradle` and resync |
| Edited `engine/` but the version did not change | you forgot `tools/sync-versions.ps1` |

---

## Verification

| What | How | Result |
|---|---|---|
| Engine logic | `pwsh tools/run-engine-tests.ps1` | **171 / 171 assertions pass** |
| Engine coverage | indexing, attribution, cause chains, mixin failure, missing dependency, duplicate mods, thread dumps, MCreator detection, three report formats, exploded mods, languages, Connector mixed packs, real NeoForge frame format, log encodings, F3+C, paths with spaces, incident timeline, **graphics families and conflicts** | pass |
| **Runtime behaviour** | `engine/tools/RuntimeHarness.java` (real log4j2 + real mod classes) | **52 / 52 assertions pass** |
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
./gradlew -I engine/tools/cp-init.gradle printClasspaths --offline -q
#    keep the COMPILE_CP= line

# 2) compile and run
javac -encoding UTF-8 -proc:none -cp "$COMPILE_CP" -d build-rt \
  engine/main/java/dev/oba/core/*.java \
  src/main/java/dev/oba/*.java \
  src/main/java/dev/oba/mod/*.java \
  engine/tools/RuntimeHarness.java
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

### Verifying the in-game UI without typing

The coloured in-game menu cannot be verified by injecting keystrokes: on this machine
`PostMessage` with `WM_KEYDOWN`, real keys from `keybd_event`, and focus stealing through
`AttachThreadInput` all fail to reach the Minecraft window — `GetForegroundWindow()` reports the
game window, yet even Esc does not open the pause menu. So the mod runs the command itself:

```powershell
# the full /oba help menu
pwsh tools/ui-shot.ps1 -VersionDir "D:\hmcl\.minecraft\versions\test" -World "新的世界" `
  -Out build-v\ui-ingame.png -NoKeys -JavaExe "C:\Program Files\Zulu\zulu-21\bin\java.exe"

# to get the title box inside the frame too, shoot something really short: the chat box
# only shows the last dozen lines or so, and the menu is ~20 lines
pwsh tools/ui-shot.ps1 -VersionDir "D:\hmcl\.minecraft\versions\test" -World "新的世界" `
  -Command "/oba mcreator" -Out build-v\ui-ingame.png -NoKeys -JavaExe "C:\Program Files\Zulu\zulu-21\bin\java.exe"
```

- `-NoKeys` passes `-Doba.ui.shot=<command>` (whatever `-Command` says, `oba help` by default) and
  the mod runs it once the world is up;
- the screenshot comes from the **game's own framebuffer** (`Screenshot.grab`, the same path as
  F2) — a GPU window occluded by other windows is not recomposited, so capturing its rectangle
  only ever yields a stale frame with no chat in it;
- before launching, the instance's `pauseOnLostFocus` is set to `false` and `chatHeightUnfocused`
  to `1.0` (restored afterwards);
- how many chat lines are visible has nothing to do with `guiScale` (3 / 2 / 1 all showed about
  nineteen), so leave the player's GUI scale alone;
- it also cross-checks the layout: `UiShot` logs a character-by-character comparison of the
  width table against the real client font and whether the three header lines share one pixel
  width (`[OBA] 宽度核对：…` / `[OBA] 标题框三行像素宽：…`); the `╭╮╯╰◈` entries in that
  table were corrected this way.

Click-to-run is guarded by the self-test: every menu row is walked as a component tree, and
**every** node must carry a `RUN_COMMAND` click event (links use `OPEN_URL`, argument-taking
rows use `SUGGEST_COMMAND`).

### Still unverified

**The remaining 1.21.x minors** (1.21.2 / 1.21.3 / 1.21.4 / 1.21.5 …) have not been verified on
a live client.

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

Released under the **GNU General Public License v3.0 (GPL-3.0)**, full text in [LICENSE](LICENSE).

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

In short: use it, modify it, redistribute it, but **a modified version you distribute must
stay open under GPL-3.0 with the copyright notice kept**. That is the main difference from MIT:
derived versions cannot be closed off.
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

[GPL-3.0 License](LICENSE) · [简体中文](README.md)

</div>
