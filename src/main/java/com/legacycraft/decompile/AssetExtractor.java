package com.legacycraft.decompile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extracts non-class entries from a Minecraft jar into a flat assets directory.
 * Skips {@code .class} files and the {@code META-INF/} folder.
 */
public final class AssetExtractor {

    private AssetExtractor() {
        // utility
    }

    /** Returns the number of extracted entries. */
    public static int extract(File jarFile, File outputDir) throws IOException {
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IOException("Could not create directory: " + outputDir);
        }
        int count = 0;
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (shouldSkip(entry)) {
                    continue;
                }
                writeEntry(jar, entry, outputDir);
                count++;
            }
        }
        return count;
    }

    private static boolean shouldSkip(JarEntry entry) {
        if (entry.isDirectory()) {
            return true;
        }
        String name = entry.getName();
        return name.endsWith(".class") || name.startsWith("META-INF/");
    }

    private static void writeEntry(JarFile jar, JarEntry entry, File outputDir) throws IOException {
        File target = new File(outputDir, entry.getName());
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
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
