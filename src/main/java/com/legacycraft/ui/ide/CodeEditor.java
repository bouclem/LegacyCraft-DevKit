package com.legacycraft.ui.ide;

import com.legacycraft.diff.LineDiff;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * RichTextFX-based editor with a diff overlay against an immutable original.
 * <p>
 * Real lines render with green/orange backgrounds for additions and
 * modifications. Ghost paragraphs (red, strikethrough) are inserted for
 * deletions when the user clicks the {@code ▶} arrow in the gutter.
 * Ghost lines are filtered out by {@link #saveTo(File)} so the on-disk
 * file never contains them.
 */
public final class CodeEditor extends VirtualizedScrollPane<CodeArea> {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final CodeArea area;
    private final Set<Integer> ghostParagraphs = new HashSet<>();
    private final Map<Integer, List<String>> deletionByLine = new HashMap<>();
    private final Set<Integer> expandedDeletions = new HashSet<>();

    private File currentFile;
    private File originalFile;
    private List<LineDiff.DiffMark> marks = Collections.emptyList();
    private boolean suppressChangeListener;

    public CodeEditor() {
        super(new CodeArea());
        this.area = getContent();
        this.area.getStyleClass().add("code-area");
        this.area.setParagraphGraphicFactory(buildGraphicFactory());
        this.area.textProperty().addListener(autosaveListener());
    }

    public void open(File file, File originalCounterpart) {
        try {
            String text = file.isFile()
                    ? new String(Files.readAllBytes(file.toPath()), UTF_8)
                    : "";
            this.currentFile = file;
            this.originalFile = originalCounterpart;
            this.ghostParagraphs.clear();
            this.expandedDeletions.clear();
            this.suppressChangeListener = true;
            this.area.replaceText(text);
            this.suppressChangeListener = false;
            refreshDiff();
        } catch (Exception e) {
            this.area.replaceText("// failed to open " + file + ": " + e.getMessage());
        }
    }

    public void closeFile() {
        this.currentFile = null;
        this.originalFile = null;
        this.marks = Collections.emptyList();
        this.ghostParagraphs.clear();
        this.expandedDeletions.clear();
        this.area.replaceText("");
    }

    /** Saves the current file, stripping ghost paragraphs from the output. */
    public void saveTo(File file) {
        if (file == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        int total = area.getParagraphs().size();
        for (int i = 0; i < total; i++) {
            if (ghostParagraphs.contains(i)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(area.getParagraph(i).getText());
        }
        try {
            Files.write(file.toPath(), sb.toString().getBytes(UTF_8));
        } catch (Exception ignored) {
            // surface via a logger one day; for now never crash the UI
        }
    }

    private ChangeListener<String> autosaveListener() {
        return (obs, oldText, newText) -> {
            if (suppressChangeListener || currentFile == null) {
                return;
            }
            saveTo(currentFile);
            refreshDiff();
        };
    }

    private IntFunction<Node> buildGraphicFactory() {
        IntFunction<Node> numbers = LineNumberFactory.get(area);
        return paragraphIndex -> {
            HBox box = new HBox(numbers.apply(paragraphIndex));
            box.setSpacing(4);
            if (deletionByLine.containsKey(paragraphIndex)) {
                Label arrow = new Label(expandedDeletions.contains(paragraphIndex) ? "▼" : "▶");
                arrow.getStyleClass().add("deletion-arrow");
                arrow.setOnMouseClicked(e -> toggleDeletion(paragraphIndex));
                box.getChildren().add(arrow);
            }
            return box;
        };
    }

    private void toggleDeletion(int paragraphIndex) {
        if (expandedDeletions.contains(paragraphIndex)) {
            collapseDeletion(paragraphIndex);
        } else {
            expandDeletion(paragraphIndex);
        }
        // Force the gutter to re-render so the arrow flips.
        area.setParagraphGraphicFactory(buildGraphicFactory());
    }

    private void expandDeletion(int paragraphIndex) {
        List<String> lines = deletionByLine.get(paragraphIndex);
        if (lines == null || lines.isEmpty()) {
            return;
        }
        StringBuilder insertion = new StringBuilder();
        for (String line : lines) {
            insertion.append('\n').append(line);
        }
        suppressChangeListener = true;
        int endOfLine = computeEndOfParagraph(paragraphIndex);
        area.insertText(endOfLine, insertion.toString());
        suppressChangeListener = false;
        for (int i = 1; i <= lines.size(); i++) {
            int ghostIdx = paragraphIndex + i;
            ghostParagraphs.add(ghostIdx);
            area.setParagraphStyle(ghostIdx, Collections.singleton("diff-ghost"));
        }
        expandedDeletions.add(paragraphIndex);
    }

    private void collapseDeletion(int paragraphIndex) {
        List<String> lines = deletionByLine.get(paragraphIndex);
        if (lines == null || lines.isEmpty()) {
            return;
        }
        int firstGhost = paragraphIndex + 1;
        int lastGhost = paragraphIndex + lines.size();
        int start = computeEndOfParagraph(paragraphIndex);
        int end = computeEndOfParagraph(lastGhost);
        suppressChangeListener = true;
        area.deleteText(start, end);
        suppressChangeListener = false;
        for (int i = firstGhost; i <= lastGhost; i++) {
            ghostParagraphs.remove(i);
        }
        expandedDeletions.remove(paragraphIndex);
    }

    private int computeEndOfParagraph(int paragraphIndex) {
        int absolute = 0;
        for (int i = 0; i <= paragraphIndex; i++) {
            absolute += area.getParagraph(i).length();
            if (i < paragraphIndex) {
                absolute += 1; // newline
            }
        }
        return absolute;
    }

    private void refreshDiff() {
        if (currentFile == null || originalFile == null) {
            this.marks = Collections.emptyList();
            this.deletionByLine.clear();
            return;
        }
        this.marks = LineDiff.compute(currentFile, originalFile);
        applyParagraphStyles();
        rebuildDeletionMap();
        area.setParagraphGraphicFactory(buildGraphicFactory());
    }

    private void applyParagraphStyles() {
        int total = area.getParagraphs().size();
        for (int i = 0; i < total; i++) {
            if (ghostParagraphs.contains(i)) {
                continue;
            }
            area.setParagraphStyle(i, Collections.<String>emptyList());
        }
        for (LineDiff.DiffMark mark : marks) {
            if (mark.type == LineDiff.MarkType.DELETED) {
                continue;
            }
            String style = mark.type == LineDiff.MarkType.ADDED ? "diff-added" : "diff-modified";
            int from = Math.max(0, mark.firstLine);
            int to = Math.min(total - 1, mark.lastLineInclusive);
            for (int i = from; i <= to; i++) {
                if (ghostParagraphs.contains(i)) {
                    continue;
                }
                Collection<String> single = Collections.singleton(style);
                area.setParagraphStyle(i, single);
            }
        }
    }

    private void rebuildDeletionMap() {
        this.deletionByLine.clear();
        for (LineDiff.DiffMark mark : marks) {
            if (mark.type != LineDiff.MarkType.DELETED) {
                continue;
            }
            this.deletionByLine.put(mark.firstLine, new ArrayList<>(mark.deletedText));
        }
    }
}
