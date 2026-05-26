package com.legacycraft.decompile;

import org.benf.cfr.reader.api.CfrDriver;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps CFR's programmatic API to decompile a whole jar to a directory.
 * <p>
 * CFR's {@code outputdir} option streams each decompiled class to disk
 * under the package path, which is exactly what we want.
 */
public final class Decompiler {

    private Decompiler() {
        // utility
    }

    public static void decompile(File jarFile, File outputDir) {
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IllegalStateException("Could not create directory: " + outputDir);
        }
        Map<String, String> options = new HashMap<>();
        options.put("outputdir", outputDir.getAbsolutePath());
        // Slightly nicer output for inspection. None of these affect correctness.
        options.put("comments", "false");
        options.put("hideutf", "false");
        options.put("removeinnerclasssynthetics", "true");

        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .build();
        driver.analyse(Collections.singletonList(jarFile.getAbsolutePath()));
    }
}
