package com.legacycraft.action;

import com.legacycraft.core.VersionTarget;
import com.legacycraft.i18n.Lang;
import com.legacycraft.ui.ConsolePanel;

/**
 * Placeholder action triggered by the RUN button.
 * <p>
 * v0.1: logs intent only. Real launch flow lands in a later version.
 */
public final class RunAction {

    private final ConsolePanel console;

    public RunAction(ConsolePanel console) {
        if (console == null) {
            throw new IllegalArgumentException("console must not be null");
        }
        this.console = console;
    }

    public void execute(VersionTarget target) {
        if (target == null) {
            console.log(Lang.get("log.run.noVersion"));
            return;
        }
        console.log(Lang.format("log.run.requested", Lang.get(target.getTranslationKey())));
        console.log(Lang.get("log.run.notImplemented"));
    }
}
