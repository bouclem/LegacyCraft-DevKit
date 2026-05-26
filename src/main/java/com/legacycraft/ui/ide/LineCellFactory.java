package com.legacycraft.ui.ide;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

/**
 * Cell factory for the {@link CodeEditor}'s line list.
 * <p>
 * Each cell renders:
 * <ul>
 *   <li>a fixed-width line number,</li>
 *   <li>a {@code ▶}/{@code ▼} arrow if the line is a deletion anchor,</li>
 *   <li>a {@link TextField} for the actual line content (read-only on
 *       ghost lines).</li>
 * </ul>
 * Per-line CSS class is reset on every update so cells recycled by
 * JavaFX's virtualization keep the right style.
 */
final class LineCellFactory implements Callback<ListView<EditorLine>, ListCell<EditorLine>> {

    private static final String[] STATE_CLASSES = {
            "diff-added", "diff-modified", "diff-ghost"
    };

    private final BiConsumer<Integer, String> onLineEdited;
    private final IntConsumer onArrowClicked;
    private final java.util.function.IntPredicate isDeletionAnchor;
    private final java.util.function.IntPredicate isExpanded;

    LineCellFactory(BiConsumer<Integer, String> onLineEdited,
                    IntConsumer onArrowClicked,
                    java.util.function.IntPredicate isDeletionAnchor,
                    java.util.function.IntPredicate isExpanded) {
        this.onLineEdited = onLineEdited;
        this.onArrowClicked = onArrowClicked;
        this.isDeletionAnchor = isDeletionAnchor;
        this.isExpanded = isExpanded;
    }

    @Override
    public ListCell<EditorLine> call(ListView<EditorLine> list) {
        return new LineCell();
    }

    private final class LineCell extends ListCell<EditorLine> {

        private final Label lineNumber = new Label();
        private final Label arrow = new Label();
        private final TextField field = new TextField();
        private final HBox box = new HBox(4, lineNumber, arrow, field);

        LineCell() {
            lineNumber.getStyleClass().add("line-number");
            lineNumber.setMinWidth(48);
            lineNumber.setAlignment(Pos.CENTER_RIGHT);
            arrow.getStyleClass().add("deletion-arrow");
            arrow.setMinWidth(16);
            field.getStyleClass().add("code-line");
            HBox.setHgrow(field, Priority.ALWAYS);
            box.setAlignment(Pos.CENTER_LEFT);
            arrow.setOnMouseClicked(event -> {
                int idx = getIndex();
                if (idx >= 0) {
                    onArrowClicked.accept(idx);
                }
            });
            field.focusedProperty().addListener((obs, was, isNow) -> commitIfChanged());
            field.setOnAction(event -> commitIfChanged());
            setText(null);
        }

        private void commitIfChanged() {
            EditorLine line = getItem();
            int idx = getIndex();
            if (line == null || idx < 0 || line.isGhost()) {
                return;
            }
            String typed = field.getText();
            if (typed != null && !typed.equals(line.getText())) {
                onLineEdited.accept(idx, typed);
            }
        }

        @Override
        protected void updateItem(EditorLine item, boolean empty) {
            super.updateItem(item, empty);
            for (String c : STATE_CLASSES) {
                getStyleClass().remove(c);
            }
            if (empty || item == null) {
                setGraphic(null);
                return;
            }
            int idx = getIndex();
            lineNumber.setText(item.isGhost() ? "" : String.valueOf(idx + 1));
            arrow.setText(arrowFor(idx));
            field.setText(item.getText());
            field.setEditable(!item.isGhost());
            applyKindClass(item);
            setGraphic(box);
        }

        private String arrowFor(int idx) {
            if (idx < 0 || !isDeletionAnchor.test(idx)) {
                return "";
            }
            return isExpanded.test(idx) ? "\u25BC" : "\u25B6";
        }

        private void applyKindClass(EditorLine item) {
            switch (item.getKind()) {
                case ADDED:
                    getStyleClass().add("diff-added");
                    break;
                case MODIFIED:
                    getStyleClass().add("diff-modified");
                    break;
                case GHOST:
                    getStyleClass().add("diff-ghost");
                    break;
                default:
                    break;
            }
        }
    }
}
