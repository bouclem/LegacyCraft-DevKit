package com.legacycraft.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * Tees console output to a session log file. Failures are swallowed so
 * a missing logs directory never crashes the UI.
 */
public final class FileLogger {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Writer writer;

    private FileLogger(Writer writer) {
        this.writer = writer;
    }

    public static FileLogger open(File logFile) {
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                return null;
            }
            return new FileLogger(new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(logFile, true), UTF_8)));
        } catch (IOException e) {
            return null;
        }
    }

    public void writeLine(String line) {
        try {
            writer.write(line);
            writer.write(System.lineSeparator());
            writer.flush();
        } catch (IOException ignored) {
            // never crash the UI on a logging failure
        }
    }

    public void close() {
        try {
            writer.close();
        } catch (IOException ignored) {
            // best-effort
        }
    }
}
