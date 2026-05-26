package com.legacycraft.action;

import com.legacycraft.core.VersionTarget;
import com.legacycraft.i18n.Lang;
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
            console.log(Lang.get("log.decompile.noVersion"));
            return;
        }
        console.log(Lang.format("log.decompile.requested", Lang.get(target.getTranslationKey())));
        console.log(Lang.get("log.decompile.notImplemented"));
    }
}
