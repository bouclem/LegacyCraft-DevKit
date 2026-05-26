package com.legacycraft.core;

/**
 * Minecraft versions targeted by the DevKit.
 * <p>
 * b1.7.3 is the first supported version. New entries can be added here
 * without changing call sites; mappings are shared across versions.
 * <p>
 * Display names are not stored on the enum: callers resolve
 * {@link #getTranslationKey()} through the i18n layer.
 */
public enum VersionTarget {

    BETA_1_7_3("b1.7.3", "version.b1_7_3");

    private final String id;
    private final String translationKey;

    VersionTarget(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String getId() {
        return id;
    }

    public String getTranslationKey() {
        return translationKey;
    }
}
