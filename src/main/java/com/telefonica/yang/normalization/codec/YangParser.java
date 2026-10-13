package com.telefonica.yang.normalization.codec;

import com.telefonica.yang.normalization.NormalizationException;
import com.telefonica.yang.normalization.YangNode;

/**
 * Converts a serialized YANG document into the schema-independent abstract tree.
 */
public interface YangParser {

    /**
     * Parses a serialized document.
     *
     * @param input serialized document bytes
     * @return the document root of the abstract tree
     * @throws NormalizationException if the document is malformed or unsupported
     */
    YangNode parse(byte[] input) throws NormalizationException;
}
