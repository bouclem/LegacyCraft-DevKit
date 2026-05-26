# Changelog

## 0.1-pre3

- New IDE mode: file tree on the left, in-house code editor in the
  centre (a `ListView` of editable `TextField`-backed lines, no
  third-party editor library), console docked at the bottom. Toggle
  with the new MODE button on the toolbar
- In-editor diff overlay against an immutable `original/` baseline:
  added lines render with a green background, modified lines with
  orange. Deleted regions surface a `▶` arrow on the line; clicking
  it expands a read-only ghost block (red, strikethrough) showing the
  original lines, click again to collapse
- Autosave on line commit (focus loss / Enter); ghost lines are
  stripped before the file hits disk so they never affect compilation
- Per-line native Ctrl+Z / Ctrl+Y while a line is focused
- New `original/` baseline folder created at decompile time. The mod
  zip now diffs `src/` and `assets/` against `original/` and the old
  `.snapshot` file is gone
- Real RUN pipeline: launches `libs/minecraft.jar` in a forked JVM
  with `-Djava.library.path=deps/natives`, username `DEV`. LWJGL 2.9.0
  natives are downloaded from `libraries.minecraft.net` and unpacked
  on first run. Process stdout streams back into the console
- Session log on disk at `logs/devkit-<timestamp>.log`, written in
  parallel with the GUI console
- Default window enlarged to 1280x720
- New i18n keys for IDE placeholder, mode toggle, and run pipeline

## 0.1-pre2

- Real DECOMPILE pipeline: download `client.jar` from Mojang (with SHA-1
  verification), decompile with CFR (in-process), extract non-class entries
  to `decompile/minecraft_decompile/assets/`, and snapshot every file
  into `.snapshot` for later diffing
- New RECOMPILE button: validates the previous decompile, fetches LWJGL
  2.9.0 + jinput into `deps/`, compiles `src/` with the system Java
  compiler, packages classes + assets into `libs/minecraft.jar`, and
  zips every modified file into `mod-<timestamp>.zip`
- All long-running actions run on a background thread; action buttons
  disable while an operation is in flight
- `Workspace` resolves all paths relative to the launch directory so the
  jar can be dropped into any folder
- Output jar renamed to `devkit_gui.jar`
- New i18n keys for download, decompile, and recompile pipelines
- `.gitignore` updated to cover `versions/`, `deps/`, `decompile/`,
  `libs/`, and `mod-*.zip`

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
