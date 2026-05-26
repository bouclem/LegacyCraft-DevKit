package com.legacycraft.diff;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Line-level diff between a "current" file and an "original" baseline,
 * exposed in a shape that the IDE editor can render directly.
 * <p>
 * Each delta becomes a {@link DiffMark}:
 * <ul>
 *   <li>{@link MarkType#ADDED}    — lines exist only in current.</li>
 *   <li>{@link MarkType#MODIFIED} — lines exist in both but differ.</li>
 *   <li>{@link MarkType#DELETED}  — lines exist only in original; rendered
 *       as a collapsible ghost block in the gutter.</li>
 * </ul>
 */
public final class LineDiff {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private LineDiff() {
        // utility
    }

    public enum MarkType { ADDED, MODIFIED, DELETED }

    public static final class DiffMark {
        public final MarkType type;
        public final int firstLine;          // 0-based, in current
        public final int lastLineInclusive;  // 0-based, in current
        public final List<String> deletedText; // populated only for DELETED

        DiffMark(MarkType type, int firstLine, int lastLineInclusive, List<String> deletedText) {
            this.type = type;
            this.firstLine = firstLine;
            this.lastLineInclusive = lastLineInclusive;
            this.deletedText = deletedText;
        }
    }

    public static List<DiffMark> compute(File current, File original) {
        List<String> currentLines = readLines(current);
        List<String> originalLines = readLines(original);
        Patch<String> patch = DiffUtils.diff(originalLines, currentLines);
        List<DiffMark> marks = new ArrayList<>();
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int targetStart = delta.getTarget().getPosition();
            int targetSize = delta.getTarget().size();
            DeltaType type = delta.getType();
            if (type == DeltaType.INSERT) {
                marks.add(new DiffMark(MarkType.ADDED,
                        targetStart, targetStart + targetSize - 1, Collections.emptyList()));
            } else if (type == DeltaType.CHANGE) {
                marks.add(new DiffMark(MarkType.MODIFIED,
                        targetStart, targetStart + targetSize - 1, Collections.emptyList()));
                marks.add(new DiffMark(MarkType.DELETED,
                        targetStart, targetStart, new ArrayList<>(delta.getSource().getLines())));
            } else if (type == DeltaType.DELETE) {
                int anchor = Math.max(0, targetStart - 1);
                marks.add(new DiffMark(MarkType.DELETED,
                        anchor, anchor, new ArrayList<>(delta.getSource().getLines())));
            }
        }
        return marks;
    }

    private static List<String> readLines(File file) {
        if (file == null || !file.isFile()) {
            return new ArrayList<>();
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String text = new String(bytes, UTF_8);
            // Use -1 to keep trailing empty strings consistent with editor content.
            return new ArrayList<>(Arrays.asList(text.split("\\r\\n|\\n|\\r", -1)));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
