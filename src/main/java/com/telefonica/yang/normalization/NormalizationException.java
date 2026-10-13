package com.telefonica.yang.normalization;

/**
 * Raised when an input document cannot be normalized, either because it is
 * malformed or because it relies on constructs that require schema knowledge.
 */
public class NormalizationException extends Exception {

    private static final long serialVersionUID = 1L;

    public NormalizationException(String message) {
        super(message);
    }

    public NormalizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
