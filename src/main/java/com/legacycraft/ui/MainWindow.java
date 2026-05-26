package com.legacycraft.ui;

import com.legacycraft.action.DecompileAction;
import com.legacycraft.action.RunAction;
import com.legacycraft.core.VersionTarget;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Main JavaFX window for LegacyCraft DevKit.
 * <p>
 * Layout:
 * <ul>
 *   <li>Top: toolbar with version selector + RUN / DECOMPILE buttons.</li>
 *   <li>Center: read-only console.</li>
 * </ul>
 */
public final class MainWindow extends Application {

    private static final String TITLE = "LegacyCraft DevKit";
    private static final int DEFAULT_WIDTH = 720;
    private static final int DEFAULT_HEIGHT = 480;

    private ConsolePanel console;
    private ChoiceBox<VersionTarget> versionSelector;
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

        stage.setScene(new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT));
        stage.setTitle(TITLE);
        stage.show();

        console.log(TITLE + " started.");
        console.log("Default target: " + versionSelector.getValue().getDisplayName() + ".");
    }

    private HBox buildToolbar() {
        Label versionLabel = new Label("Version:");

        this.versionSelector = new ChoiceBox<>();
        this.versionSelector.getItems().addAll(VersionTarget.values());
        this.versionSelector.setValue(VersionTarget.BETA_1_7_3);

        Button runButton = new Button("RUN");
        runButton.setOnAction(event -> runAction.execute(currentTarget()));

        Button decompileButton = new Button("DECOMPILE");
        decompileButton.setOnAction(event -> decompileAction.execute(currentTarget()));

        HBox toolbar = new HBox(8, versionLabel, versionSelector, runButton, decompileButton);
        toolbar.setPadding(new Insets(8));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private VersionTarget currentTarget() {
        return versionSelector.getValue();
    }
}
