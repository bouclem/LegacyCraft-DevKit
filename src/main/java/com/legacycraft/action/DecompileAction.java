package com.legacycraft.action;

import com.legacycraft.core.VersionTarget;
import com.legacycraft.core.Workspace;
import com.legacycraft.decompile.AssetExtractor;
import com.legacycraft.decompile.Decompiler;
import com.legacycraft.decompile.OriginalSync;
import com.legacycraft.download.HttpDownloader;
import com.legacycraft.download.VersionResources;
import com.legacycraft.i18n.Lang;
import com.legacycraft.mappings.JarRemapper;
import com.legacycraft.mappings.SrgMappings;
import com.legacycraft.mappings.SrgParser;
import com.legacycraft.ui.ConsolePanel;

import java.io.File;
import java.io.IOException;

/**
 * Real DECOMPILE pipeline:
 * <ol>
 *   <li>Download the version's client jar.</li>
 *   <li>Run CFR over it into {@code decompile/minecraft_decompile/src/}.</li>
 *   <li>Extract non-class entries into {@code decompile/minecraft_decompile/assets/}.</li>
 *   <li>Mirror src + assets into {@code decompile/minecraft_decompile/original/}
 *       to serve as the diff baseline for later recompiles.</li>
 * </ol>
 */
public final class DecompileAction {

    private final ConsolePanel console;
    private final Workspace workspace;

    public DecompileAction(ConsolePanel console, Workspace workspace) {
        if (console == null || workspace == null) {
            throw new IllegalArgumentException("console/workspace must not be null");
        }
        this.console = console;
        this.workspace = workspace;
    }

    public void execute(VersionTarget target) {
        if (target == null) {
            console.log(Lang.get("log.decompile.noVersion"));
            return;
        }
        String name = Lang.get(target.getTranslationKey());
        console.log(Lang.format("log.decompile.starting", name));
        try {
            File jar = downloadClient(target);
            File jarToDecompile = applyMappings(target, jar);
            runDecompiler(jarToDecompile);
            extractAssets(jarToDecompile);
            mirrorOriginal();
            console.log(Lang.get("log.decompile.done"));
        } catch (IOException e) {
            console.log(Lang.format("log.error.io", String.valueOf(e.getMessage())));
        } catch (RuntimeException e) {
            console.log(Lang.format("log.error.unexpected", String.valueOf(e.getMessage())));
        }
    }

    private File applyMappings(VersionTarget target, File jar) throws IOException {
        SrgMappings mappings = SrgParser.parseDirectory(workspace.mappingsDir());
        if (mappings.isEmpty()) {
            console.log(Lang.get("log.decompile.noMappings"));
            return jar;
        }
        File destination = workspace.remappedClientJar(target.getId());
        console.log(Lang.format("log.decompile.applyingMappings", mappings.totalEntries()));
        File result = JarRemapper.apply(jar, destination, mappings);
        console.log(Lang.format("log.decompile.mappingsApplied", result.getName()));
        return result;
    }

    private File downloadClient(VersionTarget target) throws IOException {
        File destination = workspace.clientJar(target.getId());
        String url = VersionResources.clientUrl(target);
        String sha1 = VersionResources.clientSha1(target);
        if (HttpDownloader.isAlreadyCached(destination, sha1)) {
            console.log(Lang.format("log.download.exists", destination.getName()));
            return destination;
        }
        console.log(Lang.format("log.download.starting", url));
        long size = HttpDownloader.fetch(url, destination, sha1);
        console.log(Lang.format("log.download.done", destination.getName(), size));
        return destination;
    }

    private void runDecompiler(File jar) {
        File srcDir = workspace.decompileSrc();
        console.log(Lang.format("log.decompile.cfrStart", jar.getName()));
        Decompiler.decompile(jar, srcDir);
        console.log(Lang.format("log.decompile.cfrDone", srcDir.getAbsolutePath()));
    }

    private void extractAssets(File jar) throws IOException {
        File assetsDir = workspace.decompileAssets();
        console.log(Lang.format("log.decompile.assetsStart", jar.getName()));
        int extracted = AssetExtractor.extract(jar, assetsDir);
        console.log(Lang.format("log.decompile.assetsDone", extracted, assetsDir.getAbsolutePath()));
    }

    private void mirrorOriginal() throws IOException {
        int srcCount = OriginalSync.populate(workspace.decompileSrc(), workspace.originalSrc());
        int assetCount = OriginalSync.populate(workspace.decompileAssets(), workspace.originalAssets());
        console.log(Lang.format("log.decompile.originalSaved", srcCount + assetCount));
    }
}
