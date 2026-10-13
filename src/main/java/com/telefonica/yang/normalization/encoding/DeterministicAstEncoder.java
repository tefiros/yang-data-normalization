package com.telefonica.yang.normalization.encoding;

import com.telefonica.yang.normalization.NormalizationException;
import com.telefonica.yang.normalization.YangNode;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Serializes the abstract tree into an unambiguous byte sequence.
 *
 * <p>The encoding is length-prefixed, so no value can be confused with a
 * structural separator, and sibling order is removed by sorting each child by
 * its own complete encoding. Repeated list and leaf-list entries are preserved.
 *
 * <pre>
 * header     := "YANG-NORM-01"
 * node       := identifier leaf-flag [value] child-count child*
 * identifier := uint32 length, UTF-8 bytes
 * leaf-flag  := 1 byte, 0x01 for leaves and 0x00 for inner nodes
 * value      := uint32 length, UTF-8 bytes (leaves only)
 * child-count:= uint32, always 0 for leaves
 * child*     := child encodings in ascending unsigned lexicographic order
 * </pre>
 */
public final class DeterministicAstEncoder {

    /** Identifier of the normalized representation produced by this encoder. */
    public static final String FORMAT_VERSION = "YANG-NORM-01";

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final byte[] HEADER = FORMAT_VERSION.getBytes(US_ASCII);

    private static final Comparator<byte[]> UNSIGNED_LEXICOGRAPHIC = new Comparator<byte[]>() {
        @Override
        public int compare(byte[] left, byte[] right) {
            int shared = Math.min(left.length, right.length);
            for (int i = 0; i < shared; i++) {
                int comparison = (left[i] & 0xff) - (right[i] & 0xff);
                if (comparison != 0) {
                    return comparison;
                }
            }
            return left.length - right.length;
        }
    };

    private DeterministicAstEncoder() {
    }

    /**
     * Encodes a tree into its normalized representation.
     *
     * @param root tree root, typically a document node
     * @return the normalized bytes, ready to be hashed, compared or signed
     * @throws NormalizationException if the tree cannot be encoded
     */
    public static byte[] encode(YangNode root) throws NormalizationException {
        if (root == null) {
            throw new NormalizationException("The abstract tree root cannot be null");
        }

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(buffer);
            output.write(HEADER);
            writeNode(output, root);
            output.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new NormalizationException("Unable to encode the normalized representation", e);
        }
    }

    private static void writeNode(DataOutputStream output, YangNode node) throws IOException {
        writeString(output, node.identifier());
        output.writeBoolean(node.isLeaf());

        if (node.isLeaf()) {
            writeString(output, node.value());
            output.writeInt(0);
            return;
        }

        List<byte[]> encodedChildren = new ArrayList<byte[]>(node.children().size());
        for (YangNode child : node.children()) {
            ByteArrayOutputStream childBuffer = new ByteArrayOutputStream();
            DataOutputStream childOutput = new DataOutputStream(childBuffer);
            writeNode(childOutput, child);
            childOutput.flush();
            encodedChildren.add(childBuffer.toByteArray());
        }
        Collections.sort(encodedChildren, UNSIGNED_LEXICOGRAPHIC);

        output.writeInt(encodedChildren.size());
        for (byte[] encodedChild : encodedChildren) {
            output.write(encodedChild);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] utf8 = value.getBytes(UTF_8);
        output.writeInt(utf8.length);
        output.write(utf8);
    }
}
