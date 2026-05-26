package com.legacycraft.ui;

import com.legacycraft.action.DecompileAction;
import com.legacycraft.action.RecompileAction;
import com.legacycraft.action.RunAction;
import com.legacycraft.core.VersionTarget;
import com.legacycraft.core.Workspace;
import com.legacycraft.i18n.Lang;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Main JavaFX window for LegacyCraft DevKit.
 * <p>
 * Layout:
 * <ul>
 *   <li>Top: RUN / DECOMPILE / RECOMPILE on the left, VERSION on the right.</li>
 *   <li>Center: read-only console.</li>
 * </ul>
 * Long-running actions are dispatched on a single-thread executor so the
 * JavaFX thread stays responsive. While an action is running, the action
 * buttons are disabled.
 */
public final class MainWindow extends Application {

    private static final int DEFAULT_WIDTH = 720;
    private static final int DEFAULT_HEIGHT = 480;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "devkit-actions");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private ConsolePanel console;
    private Workspace workspace;
    private MenuButton versionButton;
    private VersionTarget selectedVersion;

    private Button runButton;
    private Button decompileButton;
    private Button recompileButton;

    private RunAction runAction;
    private DecompileAction decompileAction;
    private RecompileAction recompileAction;

    @Override
    public void start(Stage stage) {
        this.console = new ConsolePanel();
        this.workspace = new Workspace();
        this.runAction = new RunAction(console);
        this.decompileAction = new DecompileAction(console, workspace);
        this.recompileAction = new RecompileAction(console, workspace);

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(console);

        String title = Lang.get("app.title");
        stage.setScene(new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT));
        stage.setTitle(title);
        stage.setOnCloseRequest(event -> worker.shutdownNow());
        stage.show();

        console.log(Lang.format("log.app.started", title));
        console.log(Lang.format("log.version.prompt", Lang.get("button.version")));
    }

    private HBox buildToolbar() {
        this.runButton = new Button(Lang.get("button.run"));
        this.runButton.setOnAction(e -> dispatch(runAction::execute));

        this.decompileButton = new Button(Lang.get("button.decompile"));
        this.decompileButton.setOnAction(e -> dispatch(decompileAction::execute));

        this.recompileButton = new Button(Lang.get("button.recompile"));
        this.recompileButton.setOnAction(e -> dispatch(recompileAction::execute));

        this.versionButton = buildVersionMenu();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, runButton, decompileButton, recompileButton, spacer, versionButton);
        toolbar.setPadding(new Insets(8));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private MenuButton buildVersionMenu() {
        MenuButton menu = new MenuButton(Lang.get("button.version"));
        for (VersionTarget target : VersionTarget.values()) {
            MenuItem item = new MenuItem(Lang.get(target.getTranslationKey()));
            item.setOnAction(event -> selectVersion(target));
            menu.getItems().add(item);
        }
        return menu;
    }

    private void selectVersion(VersionTarget target) {
        this.selectedVersion = target;
        String label = Lang.get(target.getTranslationKey());
        this.versionButton.setText(label);
        console.log(Lang.format("log.version.selected", label));
    }

    private void dispatch(Consumer<VersionTarget> action) {
        if (!busy.compareAndSet(false, true)) {
            console.log(Lang.get("log.action.busy"));
            return;
        }
        VersionTarget target = selectedVersion;
        setButtonsDisabled(true);
        worker.submit(() -> {
            try {
                action.accept(target);
            } finally {
                javafx.application.Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    busy.set(false);
                });
            }
        });
    }

    private void setButtonsDisabled(boolean disabled) {
        runButton.setDisable(disabled);
        decompileButton.setDisable(disabled);
        recompileButton.setDisable(disabled);
    }
}
