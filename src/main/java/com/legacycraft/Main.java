package com.legacycraft;

import com.legacycraft.i18n.Lang;
import com.legacycraft.ui.MainWindow;
import javafx.application.Application;

/**
 * Entry point for LegacyCraft DevKit.
 * Loads the default locale and delegates startup to the JavaFX
 * {@link MainWindow}.
 */
public final class Main {

    private Main() {
        // utility entry point
    }

    public static void main(String[] args) {
        Lang.loadDefault();
        Application.launch(MainWindow.class, args);
    }
}
