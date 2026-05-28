package com.legacycraft.mappings;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * In-memory representation of an SRG mapping set.
 * <p>
 * Keys follow ASM's internal naming:
 * <ul>
 *   <li>Classes: internal name with {@code /} separators (e.g. {@code aa}).</li>
 *   <li>Fields:  {@code owner/name} (descriptor is intentionally ignored to
 *       keep the {@link SimpleRemapperKey} contract simple — names are
 *       unique per owner in practice).</li>
 *   <li>Methods: {@code owner/name descriptor}.</li>
 * </ul>
 * The merged shape that ASM's {@code SimpleRemapper} consumes is produced
 * by {@link #toAsmMap()}.
 */
public final class SrgMappings {

    private final Map<String, String> classes;
    private final Map<String, String> fields;
    private final Map<String, String> methods;

    public SrgMappings() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public SrgMappings(Map<String, String> classes,
                       Map<String, String> fields,
                       Map<String, String> methods) {
        this.classes = classes;
        this.fields = fields;
        this.methods = methods;
    }

    public boolean isEmpty() {
        return classes.isEmpty() && fields.isEmpty() && methods.isEmpty();
    }

    public int totalEntries() {
        return classes.size() + fields.size() + methods.size();
    }

    public void addClass(String obfuscated, String deobfuscated) {
        classes.put(obfuscated, deobfuscated);
    }

    public void addField(String ownerSlashName, String deobfuscated) {
        fields.put(ownerSlashName, deobfuscated);
    }

    public void addMethod(String ownerSlashNameSpaceDesc, String deobfuscated) {
        methods.put(ownerSlashNameSpaceDesc, deobfuscated);
    }

    public Map<String, String> classes() {
        return Collections.unmodifiableMap(classes);
    }

    /**
     * Produces the merged map ASM's {@code SimpleRemapper} expects.
     * Fields and methods are encoded the same way SimpleRemapper documents.
     */
    public Map<String, String> toAsmMap() {
        Map<String, String> merged = new HashMap<>(classes.size() + fields.size() + methods.size());
        merged.putAll(classes);
        merged.putAll(fields);
        merged.putAll(methods);
        return merged;
    }
}
