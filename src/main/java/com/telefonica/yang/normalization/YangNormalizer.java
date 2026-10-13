package com.telefonica.yang.normalization;

import java.nio.charset.Charset;

/**
 * Produces a representation of YANG state data that does not depend on the
 * serialization format used to convey it.
 *
 * <p>Equivalent data encoded as XML, JSON or CBOR normalizes to the same bytes,
 * so those bytes can be compared, hashed or handed to a signing layer such as
 * COSE.
 */
public interface YangNormalizer {

    Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Parses a serialized document into the schema-independent abstract tree.
     *
     * @param input serialized document bytes
     * @param format serialization format of {@code input}
     * @return the document root of the abstract tree
     * @throws NormalizationException if the document is malformed or unsupported
     */
    YangNode parse(byte[] input, SerializationFormat format) throws NormalizationException;

    /**
     * Encodes an abstract tree into its normalized representation.
     *
     * @param node tree to encode
     * @return the normalized bytes
     * @throws NormalizationException if the tree cannot be encoded
     */
    byte[] encode(YangNode node) throws NormalizationException;

    /**
     * Parses and encodes a serialized document in a single step.
     *
     * @param input serialized document bytes
     * @param format serialization format of {@code input}
     * @return the normalized bytes
     * @throws NormalizationException if the document cannot be normalized
     */
    byte[] normalize(byte[] input, SerializationFormat format) throws NormalizationException;

    /**
     * @param xml XML document
     * @return the normalized bytes
     * @throws NormalizationException if the document cannot be normalized
     */
    default byte[] normalizeXml(String xml) throws NormalizationException {
        return normalize(toBytes(xml, "XML"), SerializationFormat.XML);
    }

    /**
     * @param json JSON document
     * @return the normalized bytes
     * @throws NormalizationException if the document cannot be normalized
     */
    default byte[] normalizeJson(String json) throws NormalizationException {
        return normalize(toBytes(json, "JSON"), SerializationFormat.JSON);
    }

    /**
     * @param cbor CBOR document
     * @return the normalized bytes
     * @throws NormalizationException if the document cannot be normalized
     */
    default byte[] normalizeCbor(byte[] cbor) throws NormalizationException {
        return normalize(cbor, SerializationFormat.CBOR);
    }

    /**
     * Checks whether two documents convey the same YANG state data.
     *
     * @param left first document
     * @param leftFormat serialization format of {@code left}
     * @param right second document
     * @param rightFormat serialization format of {@code right}
     * @return {@code true} when both normalize to the same bytes
     * @throws NormalizationException if either document cannot be normalized
     */
    default boolean areEquivalent(
            byte[] left,
            SerializationFormat leftFormat,
            byte[] right,
            SerializationFormat rightFormat)
            throws NormalizationException {
        return java.util.Arrays.equals(
                normalize(left, leftFormat), normalize(right, rightFormat));
    }

    static byte[] toBytes(String document, String format) throws NormalizationException {
        if (document == null) {
            throw new NormalizationException(format + " input cannot be null");
        }
        return document.getBytes(UTF_8);
    }
}
