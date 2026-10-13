package com.telefonica.yang.normalization;

import com.telefonica.yang.normalization.encoding.DeterministicAstEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Renders an abstract tree using the indented notation of the normalization
 * draft, which is convenient for documentation and for diagnosing differences.
 */
public final class YangTreeFormatter {

    private static final Comparator<YangNode> DISPLAY_ORDER = new Comparator<YangNode>() {
        @Override
        public int compare(YangNode left, YangNode right) {
            int byIdentifier = left.identifier().compareTo(right.identifier());
            if (byIdentifier != 0) {
                return byIdentifier;
            }
            try {
                return compareUnsigned(
                        DeterministicAstEncoder.encode(left),
                        DeterministicAstEncoder.encode(right));
            } catch (NormalizationException e) {
                throw new IllegalStateException("Unable to order a valid abstract tree", e);
            }
        }
    };

    private YangTreeFormatter() {
    }

    /**
     * @param root tree to render
     * @return a human-readable rendering of the normalized tree
     */
    public static String format(YangNode root) {
        if (root == null) {
            throw new IllegalArgumentException("The tree root cannot be null");
        }

        StringBuilder output = new StringBuilder();
        if (root.isDocument()) {
            appendChildren(output, root, "", true);
        } else {
            appendNode(output, root, "", true, true);
        }
        return output.toString();
    }

    private static void appendChildren(
            StringBuilder output, YangNode node, String prefix, boolean topLevel) {
        List<YangNode> children = sorted(node.children());
        for (int i = 0; i < children.size(); i++) {
            appendNode(output, children.get(i), prefix, i == children.size() - 1, topLevel);
        }
    }

    private static void appendNode(
            StringBuilder output, YangNode node, String prefix, boolean last, boolean root) {
        output.append(prefix);
        if (!root) {
            output.append(last ? "\u2514\u2500 " : "\u251c\u2500 ");
        }
        output.append(describe(node)).append('\n');

        if (!node.isLeaf()) {
            String childPrefix = root ? prefix : prefix + (last ? "   " : "\u2502  ");
            appendChildren(output, node, childPrefix, false);
        }
    }

    private static String describe(YangNode node) {
        return node.isLeaf() ? node.identifier() + " = " + node.value() : node.identifier();
    }

    private static List<YangNode> sorted(List<YangNode> children) {
        List<YangNode> copy = new ArrayList<YangNode>(children);
        Collections.sort(copy, DISPLAY_ORDER);
        return copy;
    }

    private static int compareUnsigned(byte[] left, byte[] right) {
        int shared = Math.min(left.length, right.length);
        for (int i = 0; i < shared; i++) {
            int comparison = (left[i] & 0xff) - (right[i] & 0xff);
            if (comparison != 0) {
                return comparison;
            }
        }
        return left.length - right.length;
    }
}
