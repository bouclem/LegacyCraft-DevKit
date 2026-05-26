package com.legacycraft.download;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hardcoded compile-time libraries needed to recompile decompiled
 * Minecraft sources.
 * <p>
 * Targeting LWJGL 2.9.0 (Mojang's libraries.minecraft.net is still live
 * for these artifacts). API-compatible with the 2.4.x that shipped with
 * b1.7.3 for everything Minecraft references.
 */
public final class Libraries {

    private static final String BASE = "https://libraries.minecraft.net/";

    private final String fileName;
    private final String url;

    private Libraries(String fileName, String url) {
        this.fileName = fileName;
        this.url = url;
    }

    public String fileName() {
        return fileName;
    }

    public String url() {
        return url;
    }

    public static List<Libraries> compileTime() {
        List<Libraries> list = new ArrayList<>();
        list.add(new Libraries("lwjgl-2.9.0.jar",
                BASE + "org/lwjgl/lwjgl/lwjgl/2.9.0/lwjgl-2.9.0.jar"));
        list.add(new Libraries("lwjgl_util-2.9.0.jar",
                BASE + "org/lwjgl/lwjgl/lwjgl_util/2.9.0/lwjgl_util-2.9.0.jar"));
        list.add(new Libraries("jinput-2.0.5.jar",
                BASE + "net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar"));
        return Collections.unmodifiableList(list);
    }
}
