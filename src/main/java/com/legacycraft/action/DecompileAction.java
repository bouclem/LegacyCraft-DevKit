package com.legacycraft.action;

import com.legacycraft.core.VersionTarget;
import com.legacycraft.ui.ConsolePanel;

/**
 * Placeholder action triggered by the DECOMPILE button.
 * <p>
 * v0.1: logs intent only. Real decompile pipeline lands in a later version.
 */
public final class DecompileAction {

    private final ConsolePanel console;

    public DecompileAction(ConsolePanel console) {
        if (console == null) {
            throw new IllegalArgumentException("console must not be null");
        }
        this.console = console;
    }

    public void execute(VersionTarget target) {
        if (target == null) {
            console.log("DECOMPILE failed: no version selected.");
            return;
        }
        console.log("DECOMPILE requested for " + target.getDisplayName() + ".");
        console.log("Decompile pipeline is not implemented yet.");
    }
}
