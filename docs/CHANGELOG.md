# Changelog

## 0.1-pre1

- Initial Gradle (Java 8 via toolchain) project scaffold
- JavaFX main window: RUN / DECOMPILE on the left, VERSION menu on the right
- VERSION button starts unset; selecting a version renames the button
- Console panel in the center for action output
- `VersionTarget` enum with Minecraft Beta 1.7.3 as the first entry
- Placeholder `RunAction` and `DecompileAction` (log only, no real pipeline)
- Shared SRG mapping files: `classes.srg`, `fields.srg`, `methods.srg`
- i18n: `en_us.json` bundled at `assets/lang/`, loaded via tiny flat-JSON
  parser in `Lang`; every UI label and log message is translated, with
  positional placeholders ({0}, {1}, ...) via `Lang.format`
- MIT license
