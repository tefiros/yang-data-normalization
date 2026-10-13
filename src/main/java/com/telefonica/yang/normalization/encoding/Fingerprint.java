package com.telefonica.yang.normalization.encoding;

import com.telefonica.yang.normalization.NormalizationException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stable content identifiers derived from a normalized representation.
 *
 * <p>These digests are what a signing layer such as COSE is expected to protect.
 */
public final class Fingerprint {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private Fingerprint() {
    }

    /**
     * @param normalizedData output of the deterministic encoder
     * @return the SHA-256 digest of the normalized representation
     * @throws NormalizationException if SHA-256 is unavailable
     */
    public static byte[] sha256(byte[] normalizedData) throws NormalizationException {
        if (normalizedData == null) {
            throw new NormalizationException("Normalized data cannot be null");
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(normalizedData);
        } catch (NoSuchAlgorithmException e) {
            throw new NormalizationException("SHA-256 is not available in this runtime", e);
        }
    }

    public static String toHex(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        char[] hex = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int value = data[i] & 0xff;
            hex[i * 2] = HEX[value >>> 4];
            hex[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(hex);
    }
}
