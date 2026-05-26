package com.legacycraft.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Small SHA-1 helper used for download verification and snapshot diffing.
 */
public final class Hashing {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Hashing() {
        // utility
    }

    public static String sha1(File file) throws IOException {
        MessageDigest digest = newSha1();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private static MessageDigest newSha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            // SHA-1 is required by every JRE
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2]     = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }
}
