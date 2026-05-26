package com.legacycraft.recompile;

import com.legacycraft.core.Hashing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compares a directory against a snapshot and zips every file whose
 * SHA-1 differs from the recorded value (or which is brand new).
 * Deletions are not represented.
 */
public final class ModZipper {

    private ModZipper() {
        // utility
    }

    /**
     * @return the number of entries written, or 0 if no differences were found
     */
    public static int zipModified(File root, Map<String, String> snapshot, File outputZip) throws IOException {
        List<String> modified = collectModified(root, snapshot);
        if (modified.isEmpty()) {
            return 0;
        }
        File parent = outputZip.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        try (ZipOutputStream zip = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputZip)))) {
            for (String relPath : modified) {
                writeEntry(zip, root, relPath);
            }
        }
        return modified.size();
    }

    private static List<String> collectModified(File root, Map<String, String> snapshot) throws IOException {
        List<String> modified = new ArrayList<>();
        for (File file : walk(root)) {
            String rel = relativize(root, file);
            String currentHash = Hashing.sha1(file);
            String previous = snapshot.get(rel);
            if (previous == null || !previous.equalsIgnoreCase(currentHash)) {
                modified.add(rel);
            }
        }
        return modified;
    }

    private static void writeEntry(ZipOutputStream zip, File root, String relPath) throws IOException {
        File file = new File(root, relPath);
        ZipEntry entry = new ZipEntry(relPath);
        entry.setTime(file.lastModified());
        zip.putNextEntry(entry);
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                zip.write(buffer, 0, read);
            }
        }
        zip.closeEntry();
    }

    private static List<File> walk(File root) {
        List<File> files = new ArrayList<>();
        Deque<File> stack = new ArrayDeque<>();
        if (root.isDirectory()) {
            stack.push(root);
        }
        while (!stack.isEmpty()) {
            File current = stack.pop();
            File[] children = current.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child.isDirectory()) {
                    stack.push(child);
                } else if (child.isFile() && !child.getName().equals(".snapshot")) {
                    files.add(child);
                }
            }
        }
        return files;
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
