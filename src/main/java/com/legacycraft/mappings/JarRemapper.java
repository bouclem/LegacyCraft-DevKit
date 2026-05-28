package com.legacycraft.mappings;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Reads a jar, applies an SRG mapping set to every {@code .class} entry
 * via ASM's {@link ClassRemapper}, and writes the result to a new jar.
 * Non-class entries are copied through verbatim.
 * <p>
 * If the supplied mappings are empty the source jar is returned unchanged.
 */
public final class JarRemapper {

    private JarRemapper() {
        // utility
    }

    /**
     * @return the destination jar path. Equal to {@code source} when
     *         {@code mappings} is empty.
     */
    public static File apply(File source, File destination, SrgMappings mappings) throws IOException {
        if (mappings.isEmpty()) {
            return source;
        }
        File parent = destination.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
        SimpleRemapper remapper = new SimpleRemapper(mappings.toAsmMap());
        try (JarFile jar = new JarFile(source);
             JarOutputStream out = new JarOutputStream(new BufferedOutputStream(
                     new FileOutputStream(destination)))) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().endsWith(".class")) {
                    rewriteClass(jar, entry, mappings, remapper, out);
                } else {
                    copyEntry(jar, entry, out);
                }
            }
        }
        return destination;
    }

    private static void rewriteClass(JarFile jar, JarEntry entry, SrgMappings mappings,
                                     SimpleRemapper remapper, JarOutputStream out) throws IOException {
        try (InputStream in = new BufferedInputStream(jar.getInputStream(entry))) {
            ClassReader reader = new ClassReader(in);
            ClassWriter writer = new ClassWriter(0);
            ClassRemapper visitor = new ClassRemapper(writer, remapper);
            reader.accept(visitor, 0);
            byte[] data = writer.toByteArray();

            String originalInternal = stripDotClass(entry.getName());
            String mappedInternal = mappings.classes().getOrDefault(originalInternal, originalInternal);
            JarEntry newEntry = new JarEntry(mappedInternal + ".class");
            newEntry.setTime(entry.getTime());
            out.putNextEntry(newEntry);
            out.write(data);
            out.closeEntry();
        }
    }

    private static void copyEntry(JarFile jar, JarEntry entry, JarOutputStream out) throws IOException {
        JarEntry copy = new JarEntry(entry.getName());
        copy.setTime(entry.getTime());
        out.putNextEntry(copy);
        try (InputStream in = jar.getInputStream(entry)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
        out.closeEntry();
    }

    private static String stripDotClass(String entryName) {
        return entryName.endsWith(".class")
                ? entryName.substring(0, entryName.length() - ".class".length())
                : entryName;
    }
}
