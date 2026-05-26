package com.legacycraft.recompile;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Wraps {@link JavaCompiler} to compile every {@code .java} file under a
 * source root against a list of classpath jars.
 */
public final class SourceCompiler {

    private SourceCompiler() {
        // utility
    }

    public static boolean isAvailable() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    public static List<File> collectSources(File srcRoot) {
        List<File> sources = new ArrayList<>();
        Deque<File> stack = new ArrayDeque<>();
        if (srcRoot.isDirectory()) {
            stack.push(srcRoot);
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
                } else if (child.isFile() && child.getName().endsWith(".java")) {
                    sources.add(child);
                }
            }
        }
        return sources;
    }

    /**
     * Compiles {@code sources} to {@code classOutput}, using {@code classpath}
     * for symbol resolution.
     *
     * @return the result, including diagnostics on failure
     */
    public static Result compile(List<File> sources, List<File> classpath, File classOutput) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("no system Java compiler");
        }
        if (!classOutput.isDirectory() && !classOutput.mkdirs()) {
            throw new IOException("Could not create directory: " + classOutput);
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(
                diagnostics, null, Charset.forName("UTF-8"))) {
            files.setLocation(javax.tools.StandardLocation.CLASS_OUTPUT,
                    Collections.singletonList(classOutput));
            files.setLocation(javax.tools.StandardLocation.CLASS_PATH, classpath);

            Iterable<? extends JavaFileObject> units = files.getJavaFileObjectsFromFiles(sources);
            List<String> options = Arrays.asList(
                    "-source", "1.8",
                    "-target", "1.8",
                    "-encoding", "UTF-8",
                    "-nowarn",
                    "-proc:none",
                    "-Xmaxerrs", "2000"
            );
            boolean success = compiler.getTask(null, files, diagnostics, options, null, units).call();
            return new Result(success, diagnostics);
        }
    }

    /** Compilation outcome with diagnostic accessors. */
    public static final class Result {
        private final boolean success;
        private final DiagnosticCollector<JavaFileObject> diagnostics;

        Result(boolean success, DiagnosticCollector<JavaFileObject> diagnostics) {
            this.success = success;
            this.diagnostics = diagnostics;
        }

        public boolean success() {
            return success;
        }

        public int errorCount() {
            int count = 0;
            for (javax.tools.Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
                    count++;
                }
            }
            return count;
        }

        public List<String> firstErrors(int limit) {
            List<String> result = new ArrayList<>();
            for (javax.tools.Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() != javax.tools.Diagnostic.Kind.ERROR) {
                    continue;
                }
                if (result.size() >= limit) {
                    break;
                }
                result.add(formatError(d));
            }
            return result;
        }

        private static String formatError(javax.tools.Diagnostic<? extends JavaFileObject> d) {
            String source = d.getSource() != null ? d.getSource().getName() : "<unknown>";
            return source + ":" + d.getLineNumber() + ": " + d.getMessage(null);
        }
    }
}
