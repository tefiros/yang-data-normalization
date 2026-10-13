package com.telefonica.yang.normalization;

/**
 * Tuning knobs for the schema-independent normalization profile.
 *
 * <p>Every option exists because a serialization format carries information that
 * cannot be reconciled with the other formats without schema knowledge.
 */
public final class NormalizationOptions {

    private static final NormalizationOptions DEFAULTS = new NormalizationOptions(false, true);

    private final boolean trimXmlLeafWhitespace;
    private final boolean stripModulePrefixes;

    private NormalizationOptions(boolean trimXmlLeafWhitespace, boolean stripModulePrefixes) {
        this.trimXmlLeafWhitespace = trimXmlLeafWhitespace;
        this.stripModulePrefixes = stripModulePrefixes;
    }

    public static NormalizationOptions defaults() {
        return DEFAULTS;
    }

    /**
     * When enabled, whitespace surrounding an XML leaf value is discarded.
     *
     * <p>This is disabled by default because a schema-independent implementation
     * cannot determine whether whitespace is significant for the leaf's YANG type.
     */
    public boolean trimXmlLeafWhitespace() {
        return trimXmlLeafWhitespace;
    }

    /**
     * When enabled, module prefixes and namespace qualifiers are removed from node
     * identifiers, so {@code ietf-interfaces:interface} and {@code interface}
     * normalize identically. Disable it to keep identifiers verbatim.
     */
    public boolean stripModulePrefixes() {
        return stripModulePrefixes;
    }

    public NormalizationOptions withTrimXmlLeafWhitespace(boolean enabled) {
        return new NormalizationOptions(enabled, stripModulePrefixes);
    }

    public NormalizationOptions withStripModulePrefixes(boolean enabled) {
        return new NormalizationOptions(trimXmlLeafWhitespace, enabled);
    }

    @Override
    public String toString() {
        return "NormalizationOptions{trimXmlLeafWhitespace=" + trimXmlLeafWhitespace
                + ", stripModulePrefixes=" + stripModulePrefixes + "}";
    }
}
