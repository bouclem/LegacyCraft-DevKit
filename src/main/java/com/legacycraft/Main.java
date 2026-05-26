package com.legacycraft;

import com.legacycraft.ui.MainWindow;
import javafx.application.Application;

/**
 * Entry point for LegacyCraft DevKit.
 * Delegates startup to the JavaFX {@link MainWindow}.
 */
public final class Main {

    private Main() {
        // utility entry point
    }

    public static void main(String[] args) {
        Application.launch(MainWindow.class, args);
    }
}
