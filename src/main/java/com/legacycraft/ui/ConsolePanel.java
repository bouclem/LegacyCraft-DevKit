package com.legacycraft.ui;

import com.legacycraft.logging.FileLogger;
import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Read-only console used to surface action output to the user.
 * <p>
 * Optionally tees every line to a {@link FileLogger}, which lets the
 * application keep a session log on disk without each caller knowing
 * about the file.
 */
public final class ConsolePanel extends BorderPane {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TextArea output;
    private FileLogger fileLogger;

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

    public void attachFileLogger(FileLogger logger) {
        this.fileLogger = logger;
    }

    /** Appends a timestamped line to the console (and to the log file, if any). */
    public void log(String message) {
        if (message == null) {
            return;
        }
        String stamped = "[" + LocalTime.now().format(TIME_FORMAT) + "] " + message;
        if (fileLogger != null) {
            fileLogger.writeLine(stamped);
        }
        String line = stamped + System.lineSeparator();
        if (Platform.isFxApplicationThread()) {
            output.appendText(line);
        } else {
            Platform.runLater(() -> output.appendText(line));
        }
    }

    /** Clears the console output. Does not touch the log file. */
    public void clear() {
        if (Platform.isFxApplicationThread()) {
            output.clear();
        } else {
            Platform.runLater(output::clear);
        }
    }
}
