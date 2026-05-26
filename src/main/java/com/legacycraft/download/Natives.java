package com.legacycraft.download;

import com.legacycraft.core.Workspace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Fetches and extracts LWJGL 2.9.0 native libraries for the host OS.
 * <p>
 * The natives jar is downloaded into {@code deps/} and its DLL/SO/DYLIB
 * entries are unpacked into {@code deps/natives/&lt;os&gt;/} so the runtime
 * can be launched with {@code -Djava.library.path}.
 */
public final class Natives {

    private static final String LIB_BASE =
            "https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.0/";

    private Natives() {
        // utility
    }

    public static String currentOs() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "osx";
        }
        return "linux";
    }

    public static File ensureExtracted(Workspace workspace) throws IOException {
        String os = currentOs();
        File targetDir = new File(workspace.nativesDir(), os);
        if (alreadyExtracted(targetDir)) {
            return targetDir;
        }
        File jar = new File(workspace.depsDir(), "lwjgl-platform-2.9.0-natives-" + os + ".jar");
        String url = LIB_BASE + "lwjgl-platform-2.9.0-natives-" + os + ".jar";
        HttpDownloader.fetch(url, jar, null);
        extract(jar, targetDir);
        return targetDir;
    }

    private static boolean alreadyExtracted(File dir) {
        if (!dir.isDirectory()) {
            return false;
        }
        File[] children = dir.listFiles();
        return children != null && children.length > 0;
    }

    private static void extract(File jarFile, File outputDir) throws IOException {
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IOException("Could not create directory: " + outputDir);
        }
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) {
                    continue;
                }
                File target = new File(outputDir, new File(entry.getName()).getName());
                try (InputStream in = jar.getInputStream(entry);
                     OutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                }
            }
        }
    }
}
