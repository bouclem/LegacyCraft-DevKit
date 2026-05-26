package com.legacycraft.decompile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Copies the freshly decompiled output ({@code src/}, {@code assets/}) into
 * a sibling {@code original/} directory so that future recompiles and the
 * IDE diff overlay can compare against an immutable baseline.
 */
public final class OriginalSync {

    private OriginalSync() {
        // utility
    }

    /** Returns the number of files copied. */
    public static int populate(File source, File destination) throws IOException {
        if (!source.isDirectory()) {
            return 0;
        }
        if (!destination.isDirectory() && !destination.mkdirs()) {
            throw new IOException("Could not create directory: " + destination);
        }
        int count = 0;
        Deque<File> stack = new ArrayDeque<>();
        stack.push(source);
        while (!stack.isEmpty()) {
            File current = stack.pop();
            File[] children = current.listFiles();
            if (children == null) {
                continue;
            }
            for (File child : children) {
                if (child.isDirectory()) {
                    stack.push(child);
                    continue;
                }
                copyFile(child, mirror(source, destination, child));
                count++;
            }
        }
        return count;
    }

    private static File mirror(File sourceRoot, File destinationRoot, File file) {
        String rel = file.getAbsolutePath().substring(sourceRoot.getAbsolutePath().length());
        if (rel.startsWith(File.separator)) {
            rel = rel.substring(File.separator.length());
        }
        return new File(destinationRoot, rel);
    }

    private static void copyFile(File source, File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
    }
}
