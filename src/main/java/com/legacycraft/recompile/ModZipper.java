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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compares a current directory against an immutable {@code original/}
 * baseline and zips every file whose contents differ (or which is brand new).
 * Files present in the original but missing now are not represented.
 */
public final class ModZipper {

    private ModZipper() {
        // utility
    }

    /**
     * @param current   the editable tree (e.g. {@code decompile/.../src})
     * @param baseline  the matching {@code original/} tree
     * @param outputZip target zip file (parent directories are created)
     * @param entryPrefix prefix prepended to each zip entry's path
     * @return the number of zip entries written
     */
    public static int zipModified(File current, File baseline, File outputZip, String entryPrefix) throws IOException {
        List<File> modified = collectModified(current, baseline);
        if (modified.isEmpty()) {
            return 0;
        }
        File parent = outputZip.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        try (ZipOutputStream zip = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(outputZip, true)))) {
            for (File file : modified) {
                String rel = relativize(current, file);
                writeEntry(zip, file, entryPrefix + rel);
            }
        }
        return modified.size();
    }

    private static List<File> collectModified(File current, File baseline) throws IOException {
        List<File> modified = new ArrayList<>();
        for (File file : walk(current)) {
            String rel = relativize(current, file);
            File baselineFile = new File(baseline, rel);
            if (!baselineFile.isFile()) {
                modified.add(file);
                continue;
            }
            String currentHash = Hashing.sha1(file);
            String baselineHash = Hashing.sha1(baselineFile);
            if (!currentHash.equalsIgnoreCase(baselineHash)) {
                modified.add(file);
            }
        }
        return modified;
    }

    private static void writeEntry(ZipOutputStream zip, File file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
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
                } else if (child.isFile()) {
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
