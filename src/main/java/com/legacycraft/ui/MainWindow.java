package com.legacycraft.ui;

import com.legacycraft.action.DecompileAction;
import com.legacycraft.action.RecompileAction;
import com.legacycraft.action.RunAction;
import com.legacycraft.core.VersionTarget;
import com.legacycraft.core.Workspace;
import com.legacycraft.i18n.Lang;
import com.legacycraft.logging.FileLogger;
import com.legacycraft.ui.ide.IdePanel;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Main JavaFX window for LegacyCraft DevKit.
 * <p>
 * Toggles between two layouts:
 * <ul>
 *   <li>CONSOLE: console fills the centre.</li>
 *   <li>IDE: file tree | editor | console strip.</li>
 * </ul>
 * Long-running actions run on a single-thread executor; the action
 * buttons are disabled while one is in flight.
 */
public final class MainWindow extends Application {

    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 720;

    private enum Mode { CONSOLE, IDE }

    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "devkit-actions");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean busy = new AtomicBoolean(false);

    private ConsolePanel console;
    private Workspace workspace;
    private FileLogger fileLogger;
    private IdePanel idePanel;
    private BorderPane root;

    private MenuButton versionButton;
    private VersionTarget selectedVersion;

    private Button runButton;
    private Button decompileButton;
    private Button recompileButton;
    private Button modeButton;

    private RunAction runAction;
    private DecompileAction decompileAction;
    private RecompileAction recompileAction;

    private Mode mode = Mode.CONSOLE;

    @Override
    public void start(Stage stage) {
        this.workspace = new Workspace();
        this.console = new ConsolePanel();
        this.fileLogger = openFileLogger();
        if (fileLogger != null) {
            this.console.attachFileLogger(fileLogger);
        }
        this.runAction = new RunAction(console, workspace);
        this.decompileAction = new DecompileAction(console, workspace);
        this.recompileAction = new RecompileAction(console, workspace);
        this.idePanel = new IdePanel(workspace, console);

        this.root = new BorderPane();
        this.root.setTop(buildToolbar());
        applyMode();

        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add(getClass().getResource(
                "/com/legacycraft/assets/css/ide.css").toExternalForm());
        String title = Lang.get("app.title");
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setOnCloseRequest(event -> {
            worker.shutdownNow();
            if (fileLogger != null) {
                fileLogger.close();
            }
        });
        stage.show();

        console.log(Lang.format("log.app.started", title));
        console.log(Lang.format("log.version.prompt", Lang.get("button.version")));
    }

    private FileLogger openFileLogger() {
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        return FileLogger.open(workspace.logFile(stamp));
    }

    private HBox buildToolbar() {
        this.runButton = new Button(Lang.get("button.run"));
        this.runButton.setOnAction(e -> dispatch(runAction::execute));

        this.decompileButton = new Button(Lang.get("button.decompile"));
        this.decompileButton.setOnAction(e -> dispatch(decompileAction::execute));

        this.recompileButton = new Button(Lang.get("button.recompile"));
        this.recompileButton.setOnAction(e -> dispatch(recompileAction::execute));

        this.modeButton = new Button(modeLabel());
        this.modeButton.setOnAction(e -> toggleMode());

        this.versionButton = buildVersionMenu();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, runButton, decompileButton, recompileButton,
                spacer, modeButton, versionButton);
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

    private void toggleMode() {
        this.mode = (mode == Mode.CONSOLE) ? Mode.IDE : Mode.CONSOLE;
        applyMode();
        modeButton.setText(modeLabel());
        console.log(Lang.format("log.mode.changed", Lang.get(modeKey())));
    }

    private void applyMode() {
        if (mode == Mode.IDE) {
            // The console is owned by the IDE panel while in IDE mode.
            root.setCenter(idePanel);
        } else {
            root.setCenter(console);
        }
    }

    private String modeLabel() {
        return Lang.format("button.mode", Lang.get(modeKey()));
    }

    private String modeKey() {
        return mode == Mode.CONSOLE ? "mode.console" : "mode.ide";
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
