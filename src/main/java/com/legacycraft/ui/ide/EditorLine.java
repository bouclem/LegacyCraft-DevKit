package com.legacycraft.ui.ide;

/**
 * A single editable (or ghost) line in the {@link CodeEditor}.
 * <p>
 * Ghost lines render the original deleted text and are filtered out
 * before the file is saved to disk.
 */
public final class EditorLine {

    public enum Kind {
        NORMAL,
        ADDED,
        MODIFIED,
        GHOST
    }

    private String text;
    private final Kind kind;
    /** Index in the source file (0-based) of the deletion anchor that owns this ghost; -1 otherwise. */
    private final int ghostAnchor;

    private EditorLine(String text, Kind kind, int ghostAnchor) {
        this.text = text;
        this.kind = kind;
        this.ghostAnchor = ghostAnchor;
    }

    public static EditorLine normal(String text) {
        return new EditorLine(text, Kind.NORMAL, -1);
    }

    public static EditorLine ghost(String text, int anchor) {
        return new EditorLine(text, Kind.GHOST, anchor);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Kind getKind() {
        return kind;
    }

    public int getGhostAnchor() {
        return ghostAnchor;
    }

    public boolean isGhost() {
        return kind == Kind.GHOST;
    }

    /**
     * Returns a clone of this line with the supplied {@link Kind} applied.
     * Ghost lines are returned unchanged so their styling is never lost.
     */
    public EditorLine withKind(Kind newKind) {
        if (this.kind == newKind || isGhost()) {
            return this;
        }
        return new EditorLine(text, newKind, ghostAnchor);
    }
}
