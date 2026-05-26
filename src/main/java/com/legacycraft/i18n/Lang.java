package com.legacycraft.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal translation registry.
 * <p>
 * Locales are loaded from {@code /com/legacycraft/assets/lang/<locale>.json}
 * on the classpath. The expected file format is a flat JSON object whose
 * values are strings, e.g. {@code {"button.run": "RUN"}}.
 * <p>
 * The parser is intentionally tiny: flat string-to-string maps only.
 * No nesting, no arrays, no numbers, no booleans. Unknown {@code \\uXXXX}
 * escapes are not decoded; write the character directly in UTF-8.
 * <p>
 * If a key is missing the key itself is returned, so the UI never crashes
 * on a missing translation.
 * <p>
 * {@link #format(String, Object...)} substitutes positional placeholders
 * {@code {0}}, {@code {1}}, ... in the translated string.
 */
public final class Lang {

    private static final String BASE_PATH = "/com/legacycraft/assets/lang/";
    private static final String DEFAULT_LOCALE = "en_us";
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static volatile Map<String, String> entries = Collections.emptyMap();

    private Lang() {
        // static-only registry
    }

    /** Loads {@code en_us.json}. Safe to call multiple times. */
    public static void loadDefault() {
        load(DEFAULT_LOCALE);
    }

    /**
     * Loads the given locale. If the resource is missing or malformed,
     * the previously loaded entries are kept.
     */
    public static void load(String locale) {
        if (locale == null || locale.isEmpty()) {
            return;
        }
        String resource = BASE_PATH + locale + ".json";
        try (InputStream in = Lang.class.getResourceAsStream(resource)) {
            if (in == null) {
                return;
            }
            try (Reader reader = new InputStreamReader(in, UTF_8)) {
                entries = parse(readAll(reader));
            }
        } catch (IOException ignored) {
            // keep current entries; callers will fall back to keys
        }
    }

    /** Returns the translation for {@code key}, or {@code key} if absent. */
    public static String get(String key) {
        if (key == null) {
            return "";
        }
        String value = entries.get(key);
        return value != null ? value : key;
    }

    /**
     * Returns {@link #get(String)} with positional placeholders substituted.
     * {@code "{0}"} is replaced by {@code args[0]}, {@code "{1}"} by
     * {@code args[1]}, and so on. Unknown indices are left as-is.
     */
    public static String format(String key, Object... args) {
        String template = get(key);
        if (args == null || args.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder(template.length() + 16);
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int end = template.indexOf('}', i + 1);
                if (end > i + 1) {
                    Integer index = parseIndex(template, i + 1, end);
                    if (index != null && index >= 0 && index < args.length) {
                        sb.append(args[index]);
                        i = end + 1;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static Integer parseIndex(String text, int from, int toExclusive) {
        int value = 0;
        for (int i = from; i < toExclusive; i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return null;
            }
            value = value * 10 + (c - '0');
        }
        return value;
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while ((read = reader.read(buffer)) > 0) {
            sb.append(buffer, 0, read);
        }
        return sb.toString();
    }

    private static Map<String, String> parse(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        int i = skipWhitespace(text, 0);
        if (i >= text.length() || text.charAt(i) != '{') {
            return result;
        }
        i++;
        while (true) {
            i = skipWhitespace(text, i);
            if (i >= text.length()) {
                break;
            }
            char c = text.charAt(i);
            if (c == '}') {
                break;
            }
            if (c == ',') {
                i++;
                continue;
            }
            if (c != '"') {
                break;
            }
            int[] cursor = {i};
            String key = readString(text, cursor);
            i = skipWhitespace(text, cursor[0]);
            if (i >= text.length() || text.charAt(i) != ':') {
                break;
            }
            i = skipWhitespace(text, i + 1);
            if (i >= text.length() || text.charAt(i) != '"') {
                break;
            }
            cursor[0] = i;
            String value = readString(text, cursor);
            i = cursor[0];
            result.put(key, value);
        }
        return result;
    }

    private static int skipWhitespace(String text, int from) {
        int i = from;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    /** Expects {@code text.charAt(cursor[0]) == '"'}. Advances cursor past the closing quote. */
    private static String readString(String text, int[] cursor) {
        StringBuilder sb = new StringBuilder();
        int i = cursor[0] + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '"') {
                cursor[0] = i + 1;
                return sb.toString();
            }
            if (c == '\\' && i + 1 < text.length()) {
                sb.append(unescape(text.charAt(i + 1)));
                i += 2;
                continue;
            }
            sb.append(c);
            i++;
        }
        cursor[0] = i;
        return sb.toString();
    }

    private static char unescape(char escaped) {
        switch (escaped) {
            case '"':  return '"';
            case '\\': return '\\';
            case '/':  return '/';
            case 'n':  return '\n';
            case 't':  return '\t';
            case 'r':  return '\r';
            case 'b':  return '\b';
            case 'f':  return '\f';
            default:   return escaped;
        }
    }
}
