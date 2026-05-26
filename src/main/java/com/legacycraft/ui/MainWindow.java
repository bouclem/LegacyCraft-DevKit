package com.legacycraft.ui;

import com.legacycraft.action.DecompileAction;
import com.legacycraft.action.RunAction;
import com.legacycraft.core.VersionTarget;
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

/**
 * Main JavaFX window for LegacyCraft DevKit.
 * <p>
 * Layout:
 * <ul>
 *   <li>Top: RUN / DECOMPILE on the left, VERSION menu on the right.</li>
 *   <li>Center: read-only console.</li>
 * </ul>
 * All visible labels and log lines are loaded from the i18n layer.
 */
public final class MainWindow extends Application {

    private static final int DEFAULT_WIDTH = 720;
    private static final int DEFAULT_HEIGHT = 480;

    private ConsolePanel console;
    private MenuButton versionButton;
    private VersionTarget selectedVersion;
    private RunAction runAction;
    private DecompileAction decompileAction;

    @Override
    public void start(Stage stage) {
        this.console = new ConsolePanel();
        this.runAction = new RunAction(console);
        this.decompileAction = new DecompileAction(console);

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(console);

        String title = Lang.get("app.title");
        stage.setScene(new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT));
        stage.setTitle(title);
        stage.show();

        console.log(Lang.format("log.app.started", title));
        console.log(Lang.format("log.version.prompt", Lang.get("button.version")));
    }

    private HBox buildToolbar() {
        Button runButton = new Button(Lang.get("button.run"));
        runButton.setOnAction(event -> runAction.execute(selectedVersion));

        Button decompileButton = new Button(Lang.get("button.decompile"));
        decompileButton.setOnAction(event -> decompileAction.execute(selectedVersion));

        this.versionButton = buildVersionMenu();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, runButton, decompileButton, spacer, versionButton);
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
}
