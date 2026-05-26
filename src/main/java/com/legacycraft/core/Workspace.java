package com.legacycraft.core;

import java.io.File;

/**
 * Resolves all working paths relative to the directory in which the
 * DevKit was launched ({@code user.dir}).
 * <p>
 * Layout:
 * <pre>
 *   &lt;cwd&gt;/
 *     versions/&lt;id&gt;/client.jar
 *     deps/*.jar
 *     decompile/minecraft_decompile/
 *       src/
 *       assets/
 *       .snapshot
 *     libs/minecraft.jar
 *     mod-&lt;timestamp&gt;.zip
 * </pre>
 */
public final class Workspace {

    private static final String DECOMPILE_FOLDER = "minecraft_decompile";
    private static final String SNAPSHOT_FILE = ".snapshot";
    private static final String RECOMPILED_JAR = "minecraft.jar";

    private final File root;

    public Workspace() {
        this(new File(System.getProperty("user.dir")));
    }

    public Workspace(File root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.root = root;
    }

    public File root() {
        return root;
    }

    public File versionDir(String versionId) {
        return new File(new File(root, "versions"), versionId);
    }

    public File clientJar(String versionId) {
        return new File(versionDir(versionId), "client.jar");
    }

    public File depsDir() {
        return new File(root, "deps");
    }

    public File decompileRoot() {
        return new File(new File(root, "decompile"), DECOMPILE_FOLDER);
    }

    public File decompileSrc() {
        return new File(decompileRoot(), "src");
    }

    public File decompileAssets() {
        return new File(decompileRoot(), "assets");
    }

    public File snapshotFile() {
        return new File(decompileRoot(), SNAPSHOT_FILE);
    }

    public File libsDir() {
        return new File(root, "libs");
    }

    public File recompiledJar() {
        return new File(libsDir(), RECOMPILED_JAR);
    }

    public File modZip(String timestamp) {
        return new File(root, "mod-" + timestamp + ".zip");
    }
}
