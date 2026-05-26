# LegacyCraft DevKit

A JavaFX-based development kit for old Minecraft versions, inspired by classic MCP-style tooling.

## Status

`0.1.0` — initial scaffold. GUI window with placeholder actions.

## Supported versions

- Minecraft Beta 1.7.3 (`b1.7.3`)

More versions will be added; mappings are shared across versions.

## Requirements

- JDK 8 (Oracle JDK or Zulu FX — must include JavaFX)
- Gradle (the wrapper is not included yet; use a local Gradle install)

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
