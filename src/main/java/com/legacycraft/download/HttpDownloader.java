package com.legacycraft.download;

import com.legacycraft.core.Hashing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Minimal HTTPS GET helper with optional SHA-1 verification and on-disk caching.
 * <p>
 * If the destination file already exists and matches the expected hash,
 * the download is skipped. On a hash mismatch the cached file is replaced.
 */
public final class HttpDownloader {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS    = 60_000;

    private HttpDownloader() {
        // utility
    }

    /**
     * Downloads {@code url} to {@code destination}. If {@code expectedSha1}
     * is non-null, the file is verified after download and rejected on mismatch.
     *
     * @return the size of the file in bytes (downloaded or cached)
     */
    public static long fetch(String url, File destination, String expectedSha1) throws IOException {
        if (isAlreadyCached(destination, expectedSha1)) {
            return destination.length();
        }
        ensureParent(destination);
        download(url, destination);
        verifyHash(destination, expectedSha1);
        return destination.length();
    }

    public static boolean isAlreadyCached(File destination, String expectedSha1) throws IOException {
        if (!destination.isFile()) {
            return false;
        }
        if (expectedSha1 == null) {
            return true;
        }
        return expectedSha1.equalsIgnoreCase(Hashing.sha1(destination));
    }

    private static void ensureParent(File destination) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create directory: " + parent);
        }
    }

    private static void download(String url, File destination) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "LegacyCraft-DevKit");
        try (InputStream in = conn.getInputStream();
             OutputStream out = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static void verifyHash(File destination, String expectedSha1) throws IOException {
        if (expectedSha1 == null) {
            return;
        }
        String actual = Hashing.sha1(destination);
        if (!expectedSha1.equalsIgnoreCase(actual)) {
            // surface both hashes so the caller can surface them via i18n
            throw new IOException("hash-mismatch:" + expectedSha1 + ":" + actual);
        }
    }
}
