package com.telefonica.yang.normalization;

import java.util.Locale;

/**
 * Serialization formats that can convey YANG state data.
 */
public enum SerializationFormat {

    XML,
    JSON,
    CBOR;

    /**
     * Resolves a format from a case-insensitive name.
     *
     * @param name format name, for example {@code xml}
     * @return the matching format
     * @throws NormalizationException if the name does not match a known format
     */
    public static SerializationFormat fromName(String name) throws NormalizationException {
        if (name == null) {
            throw new NormalizationException("Serialization format cannot be null");
        }
        for (SerializationFormat format : values()) {
            if (format.name().equals(name.toUpperCase(Locale.ROOT))) {
                return format;
            }
        }
        throw new NormalizationException("Unknown serialization format: " + name);
    }
}
