package com.legacycraft.action;

import com.legacycraft.core.VersionTarget;
import com.legacycraft.core.Workspace;
import com.legacycraft.download.Natives;
import com.legacycraft.i18n.Lang;
import com.legacycraft.ui.ConsolePanel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Real RUN pipeline: launch {@code libs/minecraft.jar} in a forked JVM
 * with LWJGL natives on {@code java.library.path}.
 * <p>
 * Stdout and stderr are streamed back to the console panel.
 */
public final class RunAction {

    private static final String USERNAME = "DEV";

    private final ConsolePanel console;
    private final Workspace workspace;

    public RunAction(ConsolePanel console, Workspace workspace) {
        if (console == null || workspace == null) {
            throw new IllegalArgumentException("console/workspace must not be null");
        }
        this.console = console;
        this.workspace = workspace;
    }

    public void execute(VersionTarget target) {
        if (target == null) {
            console.log(Lang.get("log.run.noVersion"));
            return;
        }
        File jar = workspace.recompiledJar();
        if (!jar.isFile()) {
            console.log(Lang.get("log.run.noJar"));
            return;
        }
        try {
            File natives = Natives.ensureExtracted(workspace);
            launch(jar, natives);
        } catch (IOException e) {
            console.log(Lang.format("log.error.io", String.valueOf(e.getMessage())));
        } catch (RuntimeException e) {
            console.log(Lang.format("log.error.unexpected", String.valueOf(e.getMessage())));
        }
    }

    private void launch(File jar, File nativesDir) throws IOException {
        List<String> command = buildCommand(jar, nativesDir);
        console.log(Lang.format("log.run.launching", jar.getAbsolutePath()));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workspace.root());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        streamProcessOutput(process);
    }

    private List<String> buildCommand(File jar, File nativesDir) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        command.add("-cp");
        command.add(jar.getAbsolutePath());
        // b1.7.3 main class. Newer versions need a different entry point.
        command.add("net.minecraft.client.Minecraft");
        command.add(USERNAME);
        return command;
    }

    private static String javaExecutable() {
        String home = System.getProperty("java.home");
        String exe = "java" + (System.getProperty("os.name", "").toLowerCase().contains("win")
                ? ".exe" : "");
        File candidate = new File(new File(home, "bin"), exe);
        return candidate.isFile() ? candidate.getAbsolutePath() : "java";
    }

    private void streamProcessOutput(Process process) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("UTF-8")))) {
                String line;
                while ((line = br.readLine()) != null) {
                    console.log(Lang.format("log.run.stdout", line));
                }
            } catch (IOException e) {
                console.log(Lang.format("log.error.io", String.valueOf(e.getMessage())));
            } finally {
                int code;
                try {
                    code = process.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    code = -1;
                }
                console.log(Lang.format("log.run.exited", code));
            }
        }, "minecraft-stdout");
        reader.setDaemon(true);
        reader.start();
    }
}
