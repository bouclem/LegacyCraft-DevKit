package com.legacycraft.download;

import com.legacycraft.core.VersionTarget;

/**
 * Static lookup of download URLs and hashes per {@link VersionTarget}.
 * <p>
 * Hashes are taken from Mojang's own {@code piston-meta} manifests so a
 * mismatched download is rejected before being decompiled.
 */
public final class VersionResources {

    private VersionResources() {
        // utility
    }

    public static String clientUrl(VersionTarget target) {
        switch (target) {
            case BETA_1_7_3:
                return "https://piston-data.mojang.com/v1/objects/"
                        + "43db9b498cb67058d2e12d394e6507722e71bb45/client.jar";
            default:
                throw unsupported(target);
        }
    }

    public static String clientSha1(VersionTarget target) {
        switch (target) {
            case BETA_1_7_3:
                return "43db9b498cb67058d2e12d394e6507722e71bb45";
            default:
                throw unsupported(target);
        }
    }

    private static IllegalArgumentException unsupported(VersionTarget target) {
        return new IllegalArgumentException("No resources registered for " + target);
    }
}
