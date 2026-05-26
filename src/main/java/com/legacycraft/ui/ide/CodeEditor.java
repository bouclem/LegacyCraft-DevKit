package com.legacycraft.ui.ide;

import com.legacycraft.diff.LineDiff;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A minimal in-house code editor.
 * <p>
 * Lines are stored in an {@link ObservableList} and rendered as
 * {@code TextField}-backed list cells. Per-cell CSS classes paint the
 * diff overlay (green = added, orange = modified). Deletions surface a
 * collapsible ghost block (red + strikethrough) anchored to the line
 * before the original deletion site.
 * <p>
 * Per-line undo/redo is provided by the platform {@code TextField}
 * (Ctrl+Z / Ctrl+Y while a line is focused).
 */
public final class CodeEditor extends BorderPane {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final ObservableList<EditorLine> lines = FXCollections.observableArrayList();
    private final ListView<EditorLine> list = new ListView<>(lines);
    private final Map<Integer, List<String>> deletionByAnchor = new HashMap<>();
    private final Set<Integer> expandedDeletions = new HashSet<>();

    private File currentFile;
    private File originalFile;

    public CodeEditor() {
        list.setCellFactory(new LineCellFactory(
                this::onLineEdited,
                this::toggleDeletion,
                idx -> isDeletionAnchorRealLine(idx),
                expandedDeletions::contains));
        list.setFocusTraversable(false);
        setCenter(list);
    }

    public void open(File file, File originalCounterpart) {
        this.currentFile = file;
        this.originalFile = originalCounterpart;
        this.expandedDeletions.clear();
        this.lines.setAll(loadLines(file));
        refreshDiff();
    }

    public void closeFile() {
        this.currentFile = null;
        this.originalFile = null;
        this.expandedDeletions.clear();
        this.deletionByAnchor.clear();
        this.lines.clear();
    }

    private void onLineEdited(int index, String newText) {
        if (index < 0 || index >= lines.size()) {
            return;
        }
        EditorLine line = lines.get(index);
        if (line.isGhost()) {
            return;
        }
        line.setText(newText);
        saveCurrent();
        refreshDiff();
    }

    private void toggleDeletion(int realLineIndex) {
        if (!deletionByAnchor.containsKey(realLineIndex)) {
            return;
        }
        if (expandedDeletions.contains(realLineIndex)) {
            collapseDeletion(realLineIndex);
        } else {
            expandDeletion(realLineIndex);
        }
        // Force visible rows to repaint their gutter arrows.
        list.refresh();
    }

    private void expandDeletion(int realLineIndex) {
        List<String> deleted = deletionByAnchor.get(realLineIndex);
        if (deleted == null || deleted.isEmpty()) {
            return;
        }
        int absoluteAnchor = absoluteIndexFor(realLineIndex);
        int insertAt = absoluteAnchor + 1;
        for (int i = 0; i < deleted.size(); i++) {
            lines.add(insertAt + i, EditorLine.ghost(deleted.get(i), realLineIndex));
        }
        expandedDeletions.add(realLineIndex);
    }

    private void collapseDeletion(int realLineIndex) {
        // Remove every ghost line whose anchor matches.
        lines.removeIf(line -> line.isGhost() && line.getGhostAnchor() == realLineIndex);
        expandedDeletions.remove(realLineIndex);
    }

    /**
     * Translates a "real-line index" (file coordinate) into the matching
     * row in the current observable list, accounting for any expanded
     * ghost blocks above it.
     */
    private int absoluteIndexFor(int realLineIndex) {
        int seenReal = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).isGhost()) {
                seenReal++;
                if (seenReal == realLineIndex) {
                    return i;
                }
            }
        }
        return Math.max(0, lines.size() - 1);
    }

    private boolean isDeletionAnchorRealLine(int absoluteRow) {
        if (absoluteRow < 0 || absoluteRow >= lines.size()) {
            return false;
        }
        if (lines.get(absoluteRow).isGhost()) {
            return false;
        }
        int realIndex = realIndexFor(absoluteRow);
        return deletionByAnchor.containsKey(realIndex);
    }

    private int realIndexFor(int absoluteRow) {
        int seenReal = -1;
        for (int i = 0; i <= absoluteRow && i < lines.size(); i++) {
            if (!lines.get(i).isGhost()) {
                seenReal++;
            }
        }
        return seenReal;
    }

    private void saveCurrent() {
        if (currentFile == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (EditorLine line : lines) {
            if (line.isGhost()) {
                continue;
            }
            if (!first) {
                sb.append('\n');
            }
            sb.append(line.getText());
            first = false;
        }
        try {
            Files.write(currentFile.toPath(), sb.toString().getBytes(UTF_8));
        } catch (Exception ignored) {
            // best-effort; never crash the UI on a save failure
        }
    }

    private void refreshDiff() {
        deletionByAnchor.clear();
        if (currentFile == null || originalFile == null || !originalFile.isFile()) {
            // No diff context: clear visual styling to NORMAL.
            for (int i = 0; i < lines.size(); i++) {
                EditorLine current = lines.get(i);
                if (!current.isGhost()) {
                    lines.set(i, current.withKind(EditorLine.Kind.NORMAL));
                }
            }
            list.refresh();
            return;
        }
        List<LineDiff.DiffMark> marks = LineDiff.compute(currentFile, originalFile);
        applyMarks(marks);
        rebuildDeletionMap(marks);
        list.refresh();
    }

    private void applyMarks(List<LineDiff.DiffMark> marks) {
        // Reset every non-ghost line to NORMAL first.
        for (int i = 0; i < lines.size(); i++) {
            EditorLine line = lines.get(i);
            if (!line.isGhost() && line.getKind() != EditorLine.Kind.NORMAL) {
                lines.set(i, line.withKind(EditorLine.Kind.NORMAL));
            }
        }
        for (LineDiff.DiffMark mark : marks) {
            if (mark.type == LineDiff.MarkType.DELETED) {
                continue;
            }
            EditorLine.Kind kind = (mark.type == LineDiff.MarkType.ADDED)
                    ? EditorLine.Kind.ADDED
                    : EditorLine.Kind.MODIFIED;
            for (int realIdx = mark.firstLine; realIdx <= mark.lastLineInclusive; realIdx++) {
                int abs = absoluteIndexFor(realIdx);
                if (abs < 0 || abs >= lines.size()) {
                    continue;
                }
                EditorLine line = lines.get(abs);
                if (line.isGhost()) {
                    continue;
                }
                lines.set(abs, line.withKind(kind));
            }
        }
    }

    private void rebuildDeletionMap(List<LineDiff.DiffMark> marks) {
        for (LineDiff.DiffMark mark : marks) {
            if (mark.type == LineDiff.MarkType.DELETED) {
                deletionByAnchor.put(mark.firstLine, mark.deletedText);
            }
        }
    }

    private static List<EditorLine> loadLines(File file) {
        if (file == null || !file.isFile()) {
            return java.util.Collections.singletonList(EditorLine.normal(""));
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            String text = new String(bytes, UTF_8);
            String[] split = text.split("\\r\\n|\\n|\\r", -1);
            java.util.List<EditorLine> result = new java.util.ArrayList<>(split.length);
            for (String s : Arrays.asList(split)) {
                result.add(EditorLine.normal(s));
            }
            return result;
        } catch (Exception e) {
            return java.util.Collections.singletonList(
                    EditorLine.normal("// failed to open " + file + ": " + e.getMessage()));
        }
    }
}
