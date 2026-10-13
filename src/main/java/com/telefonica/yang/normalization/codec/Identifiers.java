package com.telefonica.yang.normalization.codec;

import com.telefonica.yang.normalization.NormalizationException;

/**
 * Shared identifier handling for every input adapter.
 */
final class Identifiers {

    private Identifiers() {
    }

    /**
     * Removes the module prefix or namespace qualifier when configured to do so,
     * so that equivalent identifiers from different serializations converge.
     */
    static String normalize(String identifier, boolean stripModulePrefixes)
            throws NormalizationException {
        if (identifier == null || identifier.isEmpty()) {
            throw new NormalizationException("Node identifier cannot be null or empty");
        }
        if (!stripModulePrefixes) {
            return identifier;
        }

        int separator = identifier.indexOf(':');
        String normalized = separator < 0 ? identifier : identifier.substring(separator + 1);
        if (normalized.isEmpty()) {
            throw new NormalizationException("Node identifier is empty after removing its prefix");
        }
        return normalized;
    }
}
