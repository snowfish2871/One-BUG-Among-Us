# One BUG Among Us

<img src="docs/banner.svg" alt="One BUG Among Us" width="420">

**Disclaimer: Because this MOD has a very high priority, please do not apply it to your important or active servers or saves. OBA contributors will not take any responsibility for any issues caused! Using it means you agree to and accept the potential instability risks.**

**Crash diagnostics for Minecraft 1.20.5 / 1.20.6** (NeoForge).

The game dies, the log is a few thousand lines of stack trace, and none of it says who did it.
OBA boils that down to one sentence you can act on: **which jar, which class, which line, and
why that one is suspected.**

English | [简体中文](README.md)

> **Diagnosis only. This project never modifies a single file.**
> It does not disable, move, rename or delete any mod. Everything it concludes is printed
> and written to a report; acting on it is your decision.

> **This is the README for the 1.20.5 – 1.20.6 (NeoForge) track only.** It covers this track's Minecraft
> versions, loader adapter, build and verification results. The repository overview, the branch
> list and the version matrix live in the root README.

<img src="src/main/resources/oba-icon.png" alt="icon" width="128" align="right">

---

## This track

| Item | Value |
|---|---|
| Minecraft | 1.20.5 / 1.20.6 |
| Loader | **NeoForge**, 20.5 line, built against 20.5.21-beta |
| Java | 21 |
| Built against | 1.20.5 / 20.5.21-beta |
| Descriptor | `META-INF/neoforge.mods.toml` |
| Artifact | `oba-mc1.20.5-1.20.6-1.1.0.jar` |
| Status | built and verified in game |

One jar covers both 1.20.5 and 1.20.6: the mod-facing API did not change between them, only the descriptor file name and the dependency versions did.

### The loader adapter on this track

This track has exactly one loader adapter, under
`src/main/java/dev/oba/loader/neoforge/`. The engine references no loader class of its own; it only
knows the three interfaces in `dev.oba.loader`:

| Interface | Role |
|---|---|
| `LoaderEnv` | list mods, resolve the game directory, name the loader |
| `TickHook` | hook the server tick |
| `Loader` | holds whichever implementation this build installed |

`src/main/java/dev/oba/loader/neoforge/`: `NeoForgeEnv` reads the mod list and the game directory, `NeoForgeTickHook` hooks the server tick (both class layouts are probed).

From 20.6 the tick event is `event.tick.ServerTickEvent$Post`; `NeoForgeTickHook` probes that first and falls back to the older `event.TickEvent$ServerTickEvent`.

### In-game results on this track

- 1.20.5 + NeoForge 20.5.21-beta with 13 real mods: `[OBA] enabled: 1.1.0`, indexed 13 mod files / 3063 classes / 612 packages in 81 ms, main menu in about 25 s.
- 1.20.6 + NeoForge 20.6.x with 15 real mods: indexed 16 mod files / 4124 classes / 821 packages in 98 ms, main menu in about 24 s.

### Still open on this track

- From 20.5 the descriptor is `META-INF/neoforge.mods.toml` and the build ships it under that name; renaming it back to the old name makes the loader ignore the jar.
- A mod built for the 20.6 line crashes a 1.20.5 instance outright: the crash report says `Mod supermartijn642configlib requires neoforge 20.6.119 or above`. Pick mods per line.

---

## Contents

- [This track](#this-track)
- [Contents](#contents)
- [What it solves](#what-it-solves)
- [What it does](#what-it-does)
- [Quick start](#quick-start)
- [The interface](#the-interface)
- [Incident timeline](#incident-timeline)
- [Diagnostic reports](#diagnostic-reports)
- [Using it](#using-it)
- [How it works](#how-it-works)
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
- [Changelog](#changelog)
- [Contributing](#contributing)
- [Credits](#credits)
- [License](#license)
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

## What it does

The short version: it turns "the game crashed and I have no idea whose fault it is" into
"it is this jar, on this line".

When a crash happens you get the culprit mod file, the class, the method, the source file and
line number, and the full evidence chain underneath — which log line, which rule matched, how
many points it scored. If the verdict looks wrong you can walk that chain back and find the step
that went astray. What each mod was doing in the minutes around the failure is laid out as a
timeline, and for a freeze with no crash report at all it takes a thread dump and names the mod
that was executing when things stopped.

Two copies of the same mod, a missing dependency, a Mixin conflict, a mod built by MCreator,
a Fabric mod that died under Connector on NeoForge — each of those has its own evidence rather
than a guess. For graphics mods it also knows the dozens of renamed Sodium and Iris forks, and
catches both "two rival branches of the same family installed" and "shader loader with no
renderer to drive".

There are only two ways to run it, and it is the same jar: drop it in `mods/` and it is a mod,
run `java -jar` and it is a command line tool. Reports, CLI output and in-game messages all
switch between English and Chinese. It is read-only throughout — nothing written, changed or
deleted, and a report is only ever a new text file under `logs`.

---

## Quick start

### In game

1. Put `oba-mc1.20.5-1.20.6-1.1.0.jar` into `.minecraft/mods/`
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
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar analyze

# point at a game directory (quoted or unquoted, paths with spaces are fine)
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar analyze --game-dir "D:\my games\.minecraft"

# feed it one crash report
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar analyze crash-reports/crash-2026-10-09_18.03.20-client.txt

# list mods, find duplicates, spot MCreator, mark Fabric mods
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar mods --game-dir "D:\MC\.minecraft"

# follow the log while you play
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar watch "D:\MC\.minecraft\logs\latest.log"
```

### Try the bundled examples

`examples/` is a deliberately broken mini game directory: six fake mods, three crash reports
and a missing-dependency log.

```bash
# a NeoForge mod blows up
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar analyze examples/crash-reports/crash-2026-10-09_18.03.20-client.txt

# a Fabric mod blows up under Connector
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar analyze examples/crash-reports/crash-2026-10-10_11.35.02-client.txt

# an F3+C debug crash
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar analyze examples/crash-reports/crash-2026-10-10_16.47.12-client.txt

java -jar oba-mc1.20.5-1.20.6-1.1.0.jar mods     --game-dir examples
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar mcreator --game-dir examples
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
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar
```

```
╔════════════════════════════════════════════════════════════════╗
║  ◈ One BUG Among Us CLI                             OBA v1.1.0 ║
╚════════════════════════════════════════════════════════════════╝

  Workspace   D:\hmcl\.minecraft
  Status      ● Ready
  Build       One BUG Among Us 1.1.0

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
║  ◈ One BUG Among Us                              OBA v1.1.0 ║
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
  https://github.com/snowfish2871/One-BUG-Among-Us
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
OBA v1.1.0
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
java -jar oba-mc1.20.5-1.20.6-1.1.0.jar analyze --lang en --game-dir examples
OBA_LANG=en java -jar oba-mc1.20.5-1.20.6-1.1.0.jar mods --game-dir examples
```

All strings live in one table (`src/main/java/dev/oba/core/Lang.java`) keyed in `namespace.key` form.
Report text is stored as **key plus arguments** rather than a finished sentence and resolved
when rendered, so a cached report still renders correctly after a language change.

> The engine does not use Minecraft's own `assets/<modid>/lang/*.json`, because the command
> line tool loads no game at all; one shared table is more reliable than maintaining two sets.
> Adding a third language means adding a constant, extending the branches in `detect()`, and
> adding a column to every `put(...)`. The self test fails if any key is missing.

---

## How it works

In one line: line up three things -- what the game said, what the process was doing
at the time, and which jar each class actually lives in -- then let rules and scoring
pick the most likely culprit.

### Where the three inputs meet

- **The log.** On startup it finds `logs/latest.log`, follows it from the first line,
  and also registers an in-process log callback. Lines from both paths go through the
  same entry point and the same regex, which pulls out time, thread, level, logger and
  message. After that, "is this an error" depends only on the normalised text -- not on
  whether the message happened to come from `[Render thread/WARN]`.
- **The process.** A daemon thread samples every thread's name, state and top frames
  every 250 ms and keeps only the snapshots that changed. Freeze detection reads it,
  and so does the incident timeline.
- **The mods.** It walks `mods/`, reads each jar's metadata and class list, and builds a
  class-to-jar index. The index is tied to a fingerprint of that directory (name, size and
  mtime of every jar), so an unchanged directory means no rebuild.

### From one error line to one finding

1. **Normalise.** Strip timestamps, object addresses and thread ids, then use the rest as
   the dedupe key. One line repeating a hundred times still counts once.
2. **Match rules.** Try the regexes in order and rank hits by how specific the wording is:
   `connector.intermediary` names a concrete loader and goes first, a vague
   `class.noclassdef` goes later. Each log line yields at most one finding, otherwise a
   single crash would show up seven times in the list.
   A missing class in a library namespace is not reported: `org.jetbrains`, `kotlin`,
   `com.google`, `org.slf4j` and friends are usually a mod treating an integration as an
   optional soft dependency, and reporting them is pure noise. A mod's own package still
   reports.
3. **Blame.** Every stack frame is looked up in the index; a hit gives that jar a vote, and
   frames higher up or more specific count for more. Classes that resolve to nothing are
   listed separately so the missing family is obvious.
4. **Score and merge.** Hits from the same rule against the same mod merge into one entry,
   the votes add up, and the result is multiplied by corroborating signals (was this mod
   already known bad, is it MCreator-generated, did it arrive through Connector).
5. **Render.** Findings are handed to the renderer once they are final. The terminal lays
   them out in character columns; in game they are re-flowed to the chat HUD's pixel width.
   Same data, two layouts.

### Why it works across versions

The diagnostics live in `src/main/java/dev/oba/core/` and touch no loader API at all: they read log text,
thread dumps, jar files and stack strings. Only a thin layer is version-bound -- the loader
entry point (`@Mod`, event bus, tick callback), the crash hook and the "am I the client"
check, and the client-only font metrics and chat width.

Move to another major version and only that layer changes; the engine stays as it is.
In this directory `src/main/java/dev/oba/core/` is the engine and
`src/main/java/dev/oba/loader/neoforge/` is this track's adapter: a new version touches only the
latter.

### Why startup is fast

The expensive part of startup is the index: open jars, read class names, tens of thousands
of strings per pass. Four rules keep it off the critical path:

1. **Never block startup.** The index runs in the background and the main thread keeps
   going. If something crashes before it finishes, the finding waits and is computed the
   moment the index is ready.
2. **Never run twice.** An unchanged `mods/` fingerprint returns immediately.
3. **Scan in parallel.** Jars are independent, so the scan runs on 2-8 threads picked from
   the CPU count and the results are reassembled in the original order.
4. **Never analyse on the log thread.** A finding is queued onto OBA's own single thread; the
   log4j appender that handed over the line simply moves on. The game taught us this one: the
   analysis used to run inside the log callback, and a single oversized line sent one regex
   backtracking for tens of seconds, stalling every log write so the client looked frozen.
   Each line is now matched on at most its first 4096 characters, and the whole rule set gets
   at most 2 seconds per line before the rest is skipped.

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

34 rules over real log text, plus 3 Connector-specific ones and 1 for deliberate crashes, each
carrying concrete advice. Nine are new in 1.1.0: six cover worlds and maps (entity tick crash,
block entity tick crash, chunk generation failure, DataVersion mismatch, corrupt entity data,
world load aborted), two cover client-only classes on a server, and one covers the loader
refusing a jar outright — a single bad descriptor keeps the whole mods folder from loading,
which looks exactly like "mods installed, none active".

| Category | Rules |
|---|---|
| Dependencies | missing prerequisite, missing mandatory dependency, version mismatch, incompatible mods |
| Duplicate loading | several files for one modId, `Found duplicate mod` |
| Mixin | injection failed, config failed to load, transformation threw, MixinExtras too old |
| Class loading | `NoClassDefFoundError`, `ClassNotFoundException`, `NoSuchMethodError`, `NoSuchFieldError` |
| Registries | missing registry entries, registry conflict |
| Data and resource packs | data pack failed to load, resource pack failed to load |
| Worlds and maps | entity tick crash, block entity tick crash, chunk generation failure, DataVersion mismatch, corrupt entity data, world load aborted |
| Client and server | client-only class on a server, missing client-only class, hard reference to GUI classes |
| Loader / descriptors | descriptor refused, `mandatory` missing from `[[dependencies]]`, whole mods folder not loading |
| File integrity | bundled library missing, corrupt jar |
| Runtime | out of memory, server frozen, graphics driver problem, mod failed to load or construct |
| Connector | transform failure, missing Fabric prerequisite, intermediary names leaked |
| Deliberate | F3+C manual debug crash (reported as intentional) |

Rules only fire on unambiguous text. The structural lines of a crash report itself
(`Description:`, `Time:`, `-- Head --`, `Mod List:`, …) are explicitly excluded, so "the report
describes a crash" is never mistaken for "a crash happened".

---

## Project structure

This directory is **self-contained**: everything below is what this build compiles, and it can be copied out and built on its own.

```
1.20.5-1.20.6-neoforge/
│
├── README.md                        Simplified Chinese README
├── README.en.md                     This file (English)
├── CHANGELOG.md                     What each release on this track changed, fixed and left open
├── LICENSE                          Full GPL-3.0 license text
├── build.gradle                     Build script
├── settings.gradle                  Plugin repositories and root project name
├── gradle.properties                Version, mod metadata, supported version ranges
├── gradlew                          Gradle wrapper (8.14.3)
├── gradlew.bat                      Same, for Windows
├── mirror.init.gradle               Rewrites the dead maven host to the official CDN mirror
├── examples/                        Deliberately broken samples: fake mods, several crash reports
│
├── gradle/wrapper/                 Gradle wrapper bootstrap files
│
├── docs/banner.svg                 Title banner used by the READMEs
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
│   │   ├── Theme.java               Colours and layout for the terminal and the in-game chat
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
│       ├── ClientFont.java          Lays text out by real glyph widths inside the game
│       ├── ClientLanguage.java      Feeds the game language to the engine (client only)
│       ├── CrashReporter.java       Writes reports into logs/, prunes old ones, never overwrites a strong verdict
│       ├── DiagnosticAppender.java  log4j2 appender capturing stack traces live
│       ├── Diagnostics.java         Index, log window, report cache, debounced scheduling
│       ├── EarlyLanguage.java       Pins the report language early, from options.txt
│       ├── GameUi.java              In-game interface: the /oba menu, report paging, chat rendering
│       ├── ObaCommands.java         The /oba command tree
│       ├── ObaLifecycle.java        Shared lifecycle: init, index, watchdog, exit hook
│       ├── ObaMod.java              Mod entry point: event registration, index, watchdog
│       ├── UiShot.java              Developer aid: renders the interface to an image for offline layout checks
│       └── Watchdog.java            Process watchdog: tick gaps, deadlocks, memory
│
├── src/main/resources/
│   ├── oba-icon.png                 Mod icon (256x256, the one shown in the mod list)
│   ├── oba-icon-128.png             Same icon, small size
│   └── META-INF/
│       └── neoforge.mods.toml       Mod descriptor (placeholders expanded at build time)
```

> The jar from this directory is also executable: `java -jar build/libs/*.jar help`.

## Building

This directory is self-contained; nothing else in the repository is needed:

```bash
./gradlew jar -I mirror.init.gradle
```

The artifact lands in `build/libs/oba-mc1.20.5-1.20.6-1.1.0.jar`.

- JDK: 21
- Build plugin: NeoForgeGradle (`net.neoforged.gradle.userdev` 7.1.38)
- `maven.neoforged.net` times out from some networks; `mirror.init.gradle` redirects the requests to the official CDN. Drop `-I` when the network is fine.
- The descriptor is `META-INF/neoforge.mods.toml`, expanded from `gradle.properties` the same way.

---

## Verification

| What | Result |
|---|---|
| Engine self-test (`199` assertions) | pass |
| Runtime harness (`52` assertions) | pass |
| This directory compiles and produces a jar | pass, `build/libs/oba-mc1.20.5-1.20.6-1.1.0.jar` |
| In-game on this track | see "In-game results on this track" above |

Both suites are engine-level: they run the same `core/` code and know nothing about the loader.
The assertions cover the rules, the attribution scoring and the report formats described above.

### What this verification has caught

It is not a formality. It found defects that compile cleanly and still leave the tool useless:

| Defect | Symptom | Cause |
|---|---|---|
| Collector never fired | `install()` reported success and the appender was in the root logger's table, but no event ever arrived | `Configuration.addAppender` only registers; log4j2 dispatches through appender *references*, and `getAppenderRefs()` is a stale snapshot |
| No error ever triggered a diagnosis | Lines reached the window but no verdict was produced | log4j2's `Level.isInRange(min, max)` is a *minimum severity* check with inverted ints (FATAL=100 < ERROR=200), so `isInRange(ERROR, FATAL)` is an empty range |
| Chinese text inside English reports | Body stayed Chinese after switching language | The evidence chain stored an already-rendered rule title as an argument |
| Fabric mods could never be blamed under Connector | Their classes were indexed, but with no modId | A Fabric mod's identity is in `fabric.mod.json`, and only `neoforge.mods.toml` was read |

---

## Known limits

**Does**

- attribute stack frames to a mod file with class, method and line
- build an **incident timeline** showing what each mod was doing around the failure
- recognise 34 unambiguous error patterns, plus Connector and deliberate-crash rules
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

## Changelog

What each release changed on this track, what it fixed and what is still open lives in
[CHANGELOG.md](CHANGELOG.md), next to this file.

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

---

<div align="center">

**One BUG Among Us** · read-only diagnosis · never touches your files

[GPL-3.0 License](LICENSE) · [简体中文](README.md)

</div>
