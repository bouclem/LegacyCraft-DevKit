package com.legacycraft.action;

import com.legacycraft.core.VersionTarget;
import com.legacycraft.core.Workspace;
import com.legacycraft.decompile.Snapshot;
import com.legacycraft.download.HttpDownloader;
import com.legacycraft.download.Libraries;
import com.legacycraft.i18n.Lang;
import com.legacycraft.recompile.JarBuilder;
import com.legacycraft.recompile.ModZipper;
import com.legacycraft.recompile.SourceCompiler;
import com.legacycraft.ui.ConsolePanel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Real RECOMPILE pipeline:
 * <ol>
 *   <li>Validate that a previous decompile exists.</li>
 *   <li>Fetch compile-time libraries into {@code deps/}.</li>
 *   <li>Compile {@code decompile/minecraft_decompile/src/} to a temp dir.</li>
 *   <li>Pack classes + assets into {@code libs/minecraft.jar}.</li>
 *   <li>Diff against the snapshot and emit {@code mod-&lt;timestamp&gt;.zip}.</li>
 * </ol>
 */
public final class RecompileAction {

    private static final int MAX_REPORTED_ERRORS = 20;

    private final ConsolePanel console;
    private final Workspace workspace;

    public RecompileAction(ConsolePanel console, Workspace workspace) {
        if (console == null || workspace == null) {
            throw new IllegalArgumentException("console/workspace must not be null");
        }
        this.console = console;
        this.workspace = workspace;
    }

    public void execute(VersionTarget target) {
        if (target == null) {
            console.log(Lang.get("log.recompile.noVersion"));
            return;
        }
        String name = Lang.get(target.getTranslationKey());
        console.log(Lang.format("log.recompile.starting", name));
        try {
            if (!isReady()) {
                return;
            }
            List<File> sources = collectSources();
            if (sources.isEmpty()) {
                return;
            }
            List<File> classpath = fetchLibraries();
            File classOutput = compile(sources, classpath);
            if (classOutput == null) {
                return;
            }
            packageJar(classOutput);
            zipModifiedFiles();
            console.log(Lang.get("log.recompile.done"));
        } catch (IOException e) {
            console.log(Lang.format("log.error.io", String.valueOf(e.getMessage())));
        } catch (RuntimeException e) {
            console.log(Lang.format("log.error.unexpected", String.valueOf(e.getMessage())));
        }
    }

    private boolean isReady() {
        if (!workspace.decompileSrc().isDirectory() || !workspace.snapshotFile().isFile()) {
            console.log(Lang.get("log.recompile.noDecompile"));
            return false;
        }
        if (!SourceCompiler.isAvailable()) {
            console.log(Lang.get("log.recompile.noJavac"));
            return false;
        }
        return true;
    }

    private List<File> collectSources() {
        console.log(Lang.get("log.recompile.collecting"));
        List<File> sources = SourceCompiler.collectSources(workspace.decompileSrc());
        console.log(Lang.format("log.recompile.collected", sources.size()));
        return sources;
    }

    private List<File> fetchLibraries() throws IOException {
        console.log(Lang.get("log.recompile.fetchingLibs"));
        File depsDir = workspace.depsDir();
        List<File> classpath = new ArrayList<>();
        for (Libraries lib : Libraries.compileTime()) {
            File destination = new File(depsDir, lib.fileName());
            if (HttpDownloader.isAlreadyCached(destination, null)) {
                console.log(Lang.format("log.download.exists", destination.getName()));
            } else {
                console.log(Lang.format("log.download.starting", lib.url()));
                long size = HttpDownloader.fetch(lib.url(), destination, null);
                console.log(Lang.format("log.download.done", destination.getName(), size));
            }
            classpath.add(destination);
        }
        return classpath;
    }

    /** Returns the class output dir on success, null on compile failure. */
    private File compile(List<File> sources, List<File> classpath) throws IOException {
        console.log(Lang.get("log.recompile.compiling"));
        File classOutput = new File(workspace.decompileRoot(), "build-classes");
        SourceCompiler.Result result = SourceCompiler.compile(sources, classpath, classOutput);
        if (result.success()) {
            return classOutput;
        }
        int errors = result.errorCount();
        console.log(Lang.format("log.recompile.compileFailed", errors));
        List<String> shown = result.firstErrors(MAX_REPORTED_ERRORS);
        for (String line : shown) {
            console.log(Lang.format("log.recompile.compileError", line));
        }
        if (errors > shown.size()) {
            console.log(Lang.format("log.recompile.compileMore", errors - shown.size()));
        }
        return null;
    }

    private void packageJar(File classOutput) throws IOException {
        File outputJar = workspace.recompiledJar();
        console.log(Lang.format("log.recompile.packaging", outputJar.getAbsolutePath()));
        JarBuilder.build(classOutput, workspace.decompileAssets(), outputJar);
    }

    private void zipModifiedFiles() throws IOException {
        console.log(Lang.get("log.recompile.zipping"));
        Map<String, String> snapshot = Snapshot.read(workspace.snapshotFile());
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File zip = workspace.modZip(timestamp);
        int written = ModZipper.zipModified(workspace.decompileRoot(), snapshot, zip);
        if (written == 0) {
            console.log(Lang.get("log.recompile.zippedNone"));
            return;
        }
        console.log(Lang.format("log.recompile.zipped", zip.getName(), written));
    }
}
