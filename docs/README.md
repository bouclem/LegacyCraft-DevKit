# LegacyCraft DevKit

A JavaFX-based development kit for old Minecraft versions, inspired by classic MCP-style tooling.

## Status

`0.1-pre1` — initial scaffold. GUI window with placeholder actions.

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

## Project layout

```
build.gradle              Gradle build script
settings.gradle           Gradle settings
mappings/                 Shared SRG mappings (classes/fields/methods)
src/main/java/com/legacycraft/
  Main.java               Entry point
  ui/                     JavaFX window + console
  core/                   Core types (VersionTarget)
  action/                 RUN / DECOMPILE actions
```

## License

MIT — see [LICENSE](../LICENSE).
