# LegacyCraft Mappings

Original (Lc-prefixed) names for obfuscated Minecraft Beta 1.7.3 client
classes. Every entry is grounded in evidence from the actual decompiled
bytecode and includes a short comment explaining why it is what it is.

## Format

Standard MCP/Forge SRG line format:

```
CL: <obfClass>            <deobfClass>
FD: <obfClass>/<obfField> <deobfClass>/<deobfField>
MD: <obfClass>/<obfMethod> <obfDesc> <deobfClass>/<deobfMethod> <deobfDesc>
```

Lines starting with `#` and blank lines are ignored.

## Files

- `classes.srg` — class renames. Currently 99 verified entries.
- `fields.srg`  — field renames. Currently empty by design (see file).
- `methods.srg` — method renames. Currently empty by design (see file).

## Naming convention

All deobfuscated names live under the `net/legacycraft/...` package and
carry an `Lc` prefix on the simple class name (`LcBlock`, `LcWorld`,
`LcEntity`, ...). This avoids collision with names other Minecraft tooling
ecosystems use, makes the project's symbols visually distinct, and signals
that these are LegacyCraft's own names — not borrowed from another
project.

## Adding entries

When adding a class entry, prefer evidence visible in a single file:

- a constant string array tied to a specific subsystem (e.g.
  `"options.renderDistance.*"` proves a settings class),
- a unique import set (e.g. `paulscode.sound.*` proves a sound system
  binding),
- a tiny abstract API tied to a known stdlib type (e.g. tag id byte
  in NBT subclasses),
- a static registry table that one-to-one maps obfuscated subclasses to
  protocol ids (e.g. the packet table in the network base).

If a class only "looks like" something, leave it out. Empty space is
safer than a wrong name; the decompile output will simply keep the
obfuscated name until somebody can prove what the class does.
