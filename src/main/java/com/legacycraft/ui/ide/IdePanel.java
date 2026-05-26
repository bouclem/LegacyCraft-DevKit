package com.legacycraft.ui.ide;

import com.legacycraft.core.Workspace;
import com.legacycraft.i18n.Lang;
import com.legacycraft.ui.ConsolePanel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.io.File;

/**
 * IDE-mode layout: file tree on the left, code editor in the centre,
 * console docked at the bottom (so RECOMPILE / RUN logs are visible
 * while editing).
 */
public final class IdePanel extends BorderPane {

    private static final double LEFT_DIVIDER = 0.22;
    private static final double BOTTOM_DIVIDER = 0.78;

    private final CodeEditor editor;
    private final Workspace workspace;
    private final TextArea placeholder;
    private final BorderPane editorHost;
    private final FileTreePanel tree;

    public IdePanel(Workspace workspace, ConsolePanel console) {
        this.workspace = workspace;
        this.editor = new CodeEditor();
        this.placeholder = buildPlaceholder();
        this.editorHost = new BorderPane(placeholder);
        this.tree = new FileTreePanel(workspace, this::openFile);

        SplitPane horizontal = new SplitPane(tree, editorHost);
        horizontal.setDividerPositions(LEFT_DIVIDER);
        SplitPane.setResizableWithParent(tree, false);

        SplitPane vertical = new SplitPane(horizontal, console);
        vertical.setOrientation(javafx.geometry.Orientation.VERTICAL);
        vertical.setDividerPositions(BOTTOM_DIVIDER);

        setCenter(vertical);
    }

    /** Reloads the file tree from disk. Called when entering IDE mode. */
    public void refresh() {
        tree.refresh();
    }

    private TextArea buildPlaceholder() {
        TextArea ta = new TextArea(Lang.get("ide.placeholder"));
        ta.setEditable(false);
        ta.setWrapText(true);
        return ta;
    }

    private void openFile(File file) {
        if (file == null || !file.getName().endsWith(".java")) {
            editorHost.setCenter(placeholder);
            editor.closeFile();
            return;
        }
        File originalCounterpart = mapToOriginal(file);
        editor.open(file, originalCounterpart);
        editorHost.setCenter(editor);
    }

    /** Maps {@code decompile/.../src/foo/Bar.java} to {@code .../original/src/foo/Bar.java}. */
    private File mapToOriginal(File file) {
        String currentRoot = workspace.decompileSrc().getAbsolutePath().replace('\\', '/');
        String assetsRoot = workspace.decompileAssets().getAbsolutePath().replace('\\', '/');
        String filePath = file.getAbsolutePath().replace('\\', '/');
        if (filePath.startsWith(currentRoot + "/")) {
            return new File(workspace.originalSrc(), filePath.substring(currentRoot.length() + 1));
        }
        if (filePath.startsWith(assetsRoot + "/")) {
            return new File(workspace.originalAssets(), filePath.substring(assetsRoot.length() + 1));
        }
        return null;
    }
}
