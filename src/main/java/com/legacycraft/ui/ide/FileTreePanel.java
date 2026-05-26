package com.legacycraft.ui.ide;

import com.legacycraft.core.Workspace;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * File tree rooted at the decompile folder.
 * <p>
 * Folder enumeration runs on a single background thread; results are
 * pushed back to the JavaFX thread in chunks so a directory with many
 * entries never freezes the UI. Internal directories ({@code original/},
 * {@code build-classes/}) are hidden from the tree because the user
 * never edits them.
 */
public final class FileTreePanel extends BorderPane {

    private static final Set<String> HIDDEN_DIR_NAMES;
    static {
        Set<String> hidden = new HashSet<>();
        hidden.add("original");
        hidden.add("build-classes");
        HIDDEN_DIR_NAMES = Collections.unmodifiableSet(hidden);
    }

    private final TreeView<File> tree;
    private final Workspace workspace;
    private final ExecutorService scanExecutor;

    public FileTreePanel(Workspace workspace, Consumer<File> onFileChosen) {
        this.workspace = workspace;
        this.scanExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ide-tree-scan");
            t.setDaemon(true);
            return t;
        });
        this.tree = new TreeView<>();
        this.tree.setShowRoot(true);
        this.tree.setCellFactory(view -> new javafx.scene.control.TreeCell<File>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        this.tree.getSelectionModel().selectedItemProperty().addListener((obs, prev, current) -> {
            if (current == null) {
                return;
            }
            File file = current.getValue();
            if (file != null && file.isFile()) {
                onFileChosen.accept(file);
            }
        });
        setCenter(tree);
        refresh();
    }

    /** Rebuilds the tree from the current state of the decompile folder. */
    public void refresh() {
        File root = workspace.decompileRoot();
        AsyncTreeItem rootItem = new AsyncTreeItem(root, scanExecutor);
        tree.setRoot(rootItem);
        rootItem.scheduleLoad();
        rootItem.setExpanded(true);
    }

    /**
     * Tree item that loads its children on a background thread the first
     * time it's expanded. Hidden directories are filtered out so the tree
     * only shows files the user is meant to edit.
     */
    private static final class AsyncTreeItem extends TreeItem<File> {

        private final ExecutorService executor;
        private boolean scheduled;

        AsyncTreeItem(File file, ExecutorService executor) {
            super(file);
            this.executor = executor;
            expandedProperty().addListener((obs, was, isNow) -> {
                if (Boolean.TRUE.equals(isNow)) {
                    scheduleLoad();
                }
            });
        }

        @Override
        public boolean isLeaf() {
            File f = getValue();
            return f == null || f.isFile();
        }

        void scheduleLoad() {
            File file = getValue();
            if (scheduled || file == null || !file.isDirectory()) {
                return;
            }
            scheduled = true;
            executor.submit(() -> loadChildrenSafely(file));
        }

        private void loadChildrenSafely(File file) {
            File[] raw;
            try {
                raw = file.listFiles();
            } catch (RuntimeException e) {
                raw = null;
            }
            File[] children = (raw == null) ? new File[0] : sort(filter(raw));
            Platform.runLater(() -> {
                List<TreeItem<File>> items = new java.util.ArrayList<>(children.length);
                for (File child : children) {
                    items.add(new AsyncTreeItem(child, executor));
                }
                super.getChildren().setAll(items);
            });
        }

        private static File[] filter(File[] children) {
            int kept = 0;
            File[] tmp = new File[children.length];
            for (File child : children) {
                if (child.isDirectory() && HIDDEN_DIR_NAMES.contains(child.getName())) {
                    continue;
                }
                tmp[kept++] = child;
            }
            return Arrays.copyOf(tmp, kept);
        }

        private static File[] sort(File[] children) {
            File[] sorted = Arrays.copyOf(children, children.length);
            Arrays.sort(sorted, Comparator
                    .comparing((File f) -> !f.isDirectory())
                    .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            return sorted;
        }
    }
}
