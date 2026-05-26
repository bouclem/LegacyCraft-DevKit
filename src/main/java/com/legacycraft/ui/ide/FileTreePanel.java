package com.legacycraft.ui.ide;

import com.legacycraft.core.Workspace;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * Lazily-loaded directory tree rooted at the decompile folder.
 * <p>
 * Selecting a leaf invokes the supplied callback only for {@code .java}
 * files. Non-Java leaves are accepted but the callback decides what to do.
 */
public final class FileTreePanel extends BorderPane {

    private final TreeView<File> tree;

    public FileTreePanel(Workspace workspace, Consumer<File> onFileChosen) {
        this.tree = new TreeView<>(rootItem(workspace));
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
    }

    private static TreeItem<File> rootItem(Workspace workspace) {
        File root = workspace.decompileRoot();
        TreeItem<File> item = new LazyTreeItem(root);
        item.setExpanded(true);
        return item;
    }

    /** Tree item that loads its children only when first expanded. */
    private static final class LazyTreeItem extends TreeItem<File> {

        private boolean loaded;

        LazyTreeItem(File file) {
            super(file);
        }

        @Override
        public boolean isLeaf() {
            return getValue() == null || getValue().isFile();
        }

        @Override
        public javafx.collections.ObservableList<TreeItem<File>> getChildren() {
            if (!loaded) {
                loaded = true;
                File file = getValue();
                if (file != null && file.isDirectory()) {
                    File[] children = file.listFiles();
                    if (children != null) {
                        File[] sorted = sort(children);
                        for (File child : sorted) {
                            super.getChildren().add(new LazyTreeItem(child));
                        }
                    }
                }
            }
            return super.getChildren();
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
