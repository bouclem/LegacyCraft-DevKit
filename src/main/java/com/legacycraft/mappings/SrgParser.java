package com.legacycraft.mappings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Parses the classic SRG line format used by MCP and Forge tooling:
 * <pre>
 *   CL: &lt;obfClass&gt; &lt;deobfClass&gt;
 *   FD: &lt;obfClass&gt;/&lt;obfField&gt; &lt;deobfClass&gt;/&lt;deobfField&gt;
 *   MD: &lt;obfClass&gt;/&lt;obfMethod&gt; &lt;obfDesc&gt; &lt;deobfClass&gt;/&lt;deobfMethod&gt; &lt;deobfDesc&gt;
 * </pre>
 * Lines starting with {@code #} or empty lines are ignored.
 * <p>
 * The parser tries three sources in order:
 * <ol>
 *   <li>{@code <workspace>/mappings/} on the filesystem (user override).</li>
 *   <li>{@code /com/legacycraft/assets/mappings/} on the classpath
 *       (shipped with the jar).</li>
 * </ol>
 * Filesystem entries with the same key win over bundled ones, which lets
 * a user drop a {@code mappings/} folder next to the jar to extend or
 * override the built-in mappings without rebuilding.
 */
public final class SrgParser {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String CLASSPATH_BASE = "/com/legacycraft/assets/mappings/";
    private static final String[] FILES = { "classes.srg", "fields.srg", "methods.srg" };

    private SrgParser() {
        // utility
    }

    /**
     * Parses every known SRG file from the bundled classpath location and
     * the filesystem directory (filesystem entries take precedence).
     */
    public static SrgMappings parseDirectory(File filesystemDir) throws IOException {
        SrgMappings mappings = new SrgMappings();
        for (String name : FILES) {
            parseFromClasspath(name, mappings);
        }
        if (filesystemDir != null && filesystemDir.isDirectory()) {
            for (String name : FILES) {
                parseFromFile(new File(filesystemDir, name), mappings);
            }
        }
        return mappings;
    }

    private static void parseFromClasspath(String name, SrgMappings target) throws IOException {
        String resource = CLASSPATH_BASE + name;
        try (InputStream in = SrgParser.class.getResourceAsStream(resource)) {
            if (in == null) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, UTF_8))) {
                parseLines(reader, target);
            }
        }
    }

    private static void parseFromFile(File file, SrgMappings target) throws IOException {
        if (!file.isFile()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), UTF_8))) {
            parseLines(reader, target);
        }
    }

    private static void parseLines(BufferedReader reader, SrgMappings target) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                continue;
            }
            parseLine(trimmed, target);
        }
    }

    private static void parseLine(String line, SrgMappings target) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            return;
        }
        String tag = parts[0];
        if (tag.equals("CL:") && parts.length >= 3) {
            target.addClass(parts[1], parts[2]);
            return;
        }
        if (tag.equals("FD:") && parts.length >= 3) {
            String key = parts[1];
            String deobf = parts[2];
            int slash = deobf.lastIndexOf('/');
            if (slash > 0) {
                target.addField(key, deobf.substring(slash + 1));
            }
            return;
        }
        if (tag.equals("MD:") && parts.length >= 5) {
            String obfRef = parts[1];
            String obfDesc = parts[2];
            String deobf = parts[3];
            int slash = deobf.lastIndexOf('/');
            if (slash > 0) {
                target.addMethod(obfRef + " " + obfDesc, deobf.substring(slash + 1));
            }
        }
    }
}
