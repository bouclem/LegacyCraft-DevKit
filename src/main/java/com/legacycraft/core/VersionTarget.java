package com.legacycraft.core;

/**
 * Minecraft versions targeted by the DevKit.
 * <p>
 * b1.7.3 is the first supported version. New entries can be added here
 * without changing call sites; mappings are shared across versions.
 */
public enum VersionTarget {

    BETA_1_7_3("b1.7.3", "Minecraft Beta 1.7.3");

    private final String id;
    private final String displayName;

    VersionTarget(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
