package com.legacycraft.recompile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Packs a class output directory together with an assets directory
 * into a single jar.
 */
public final class JarBuilder {

    private JarBuilder() {
        // utility
    }

    public static void build(File classOutput, File assetsDir, File outputJar) throws IOException {
        File parent = outputJar.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        Set<String> writtenEntries = new HashSet<>();
        try (JarOutputStream out = new JarOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputJar)))) {
            addTree(out, classOutput, classOutput, writtenEntries);
            addTree(out, assetsDir, assetsDir, writtenEntries);
        }
    }

    private static void addTree(JarOutputStream out, File root, File current,
                                Set<String> written) throws IOException {
        if (root == null || !root.isDirectory() || current == null || !current.exists()) {
            return;
        }
        Deque<File> stack = new ArrayDeque<>();
        stack.push(current);
        while (!stack.isEmpty()) {
            File node = stack.pop();
            if (node.isDirectory()) {
                File[] children = node.listFiles();
                if (children != null) {
                    for (File child : children) {
                        stack.push(child);
                    }
                }
                continue;
            }
            String entryName = relativize(root, node);
            if (!written.add(entryName)) {
                continue;
            }
            writeEntry(out, entryName, node);
        }
    }

    private static void writeEntry(JarOutputStream out, String name, File file) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(file.lastModified());
        out.putNextEntry(entry);
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
        out.closeEntry();
    }

    private static String relativize(File root, File file) {
        String rootPath = root.getAbsolutePath().replace('\\', '/');
        String filePath = file.getAbsolutePath().replace('\\', '/');
        if (filePath.startsWith(rootPath + "/")) {
            return filePath.substring(rootPath.length() + 1);
        }
        return file.getName();
    }
}
