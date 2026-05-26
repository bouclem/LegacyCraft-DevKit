package com.legacycraft.ui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Read-only console used to surface action output to the user.
 * <p>
 * Thread-safe: messages from background threads are routed through
 * {@link Platform#runLater(Runnable)} so the JavaFX scene graph is
 * only mutated on the application thread.
 */
public final class ConsolePanel extends BorderPane {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TextArea output;

    public ConsolePanel() {
        this.output = new TextArea();
        this.output.setEditable(false);
        this.output.setWrapText(true);
        this.output.setStyle(
                "-fx-font-family: 'Consolas', 'Monospaced';"
                        + " -fx-font-size: 12px;"
        );
        setCenter(this.output);
    }

    /**
     * Appends a timestamped line to the console.
     */
    public void log(String message) {
        if (message == null) {
            return;
        }
        String line = "[" + LocalTime.now().format(TIME_FORMAT) + "] " + message
                + System.lineSeparator();
        if (Platform.isFxApplicationThread()) {
            output.appendText(line);
        } else {
            Platform.runLater(() -> output.appendText(line));
        }
    }

    /**
     * Clears the console output.
     */
    public void clear() {
        if (Platform.isFxApplicationThread()) {
            output.clear();
        } else {
            Platform.runLater(output::clear);
        }
    }
}
