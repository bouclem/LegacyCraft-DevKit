# LegacyCraft DevKit

A JavaFX-based development kit for old Minecraft versions, inspired by classic MCP-style tooling.

## Status

`0.1-pre3` — IDE mode with in-editor diff overlay, real RUN / DECOMPILE /
RECOMPILE pipelines, session logging. Mappings still placeholders.

## Supported versions

- Minecraft Beta 1.7.3 (`b1.7.3`)

More versions will be added; mappings are shared across versions.

## Requirements

- JDK 8 with JavaFX (Oracle JDK 8 or Zulu 8 FX)
- Gradle (wrapper not included yet; a local Gradle install works)

The build uses a Gradle Java toolchain pinned to Java 8, so Gradle itself
can run on a newer JVM. `JAVA_HOME` does not need to point at Java 8.

## Build & run

```
gradle run
```

To produce the distributable fat jar:

```
gradle build
```

The output is `build/libs/devkit_gui.jar`. Drop it into any folder and
launch with `java -jar devkit_gui.jar` — all working directories
(`versions/`, `deps/`, `decompile/`, `libs/`, `logs/`) are created
relative to the launch directory.

## Workflow

1. Click VERSION and pick a target.
2. Click DECOMPILE — downloads `client.jar`, runs CFR, extracts assets,
   and writes an immutable `original/` baseline.
3. Click MODE to switch to IDE. Edit `.java` files in
   `decompile/minecraft_decompile/src/`. Added lines glow green, modified
   lines glow orange, deleted regions are reachable via a `▶` arrow that
   expands a red ghost block showing the original lines.
4. Click RECOMPILE — fetches compile-time libraries, compiles your edits,
   produces `libs/minecraft.jar`, and zips every modified file (in `src/`
   or `assets/`) into a `mod-<timestamp>.zip`.
5. Click RUN — forks a JVM with the LWJGL natives on `java.library.path`
   and launches `libs/minecraft.jar` as `DEV`.

## Project layout

```
build.gradle              Gradle build script
settings.gradle           Gradle settings
mappings/                 Shared SRG mappings (classes/fields/methods)
src/main/java/com/legacycraft/
  Main.java               Entry point
  action/                 RUN / DECOMPILE / RECOMPILE actions
  core/                   Workspace, version target, hashing
  decompile/              CFR driver, asset extractor, original sync
  diff/                   Line-level diff against the original baseline
  download/               HTTP, version resources, libraries, natives
  i18n/                   Lang loader (flat JSON)
  logging/                Session log file writer
  recompile/              javac wrapper, jar builder, mod zipper
  ui/                     JavaFX window, console, IDE panels
src/main/resources/com/legacycraft/assets/
  lang/en_us.json         Translation strings
  css/ide.css             IDE theme
```

## License

MIT — see [LICENSE](../LICENSE).
