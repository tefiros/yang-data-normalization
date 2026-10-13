package com.telefonica.yang.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable node of the schema-independent abstract tree that every input
 * adapter produces.
 *
 * <p>A node either carries a value (leaf or leaf-list entry) or carries
 * descendants (container, list entry or document root), never both.
 */
public final class YangNode {

    /** Identifier reserved for the synthetic node wrapping a whole document. */
    public static final String DOCUMENT_IDENTIFIER = "";

    private final String identifier;
    private final String value;
    private final List<YangNode> children;

    private YangNode(String identifier, String value, List<YangNode> children) {
        this.identifier = identifier;
        this.value = value;
        this.children = children;
    }

    /**
     * Creates a leaf or leaf-list entry.
     *
     * @param identifier node identifier, never {@code null}
     * @param value textual value, never {@code null}; use {@code ""} for the YANG
     *     {@code empty} type
     * @return the created leaf node
     */
    public static YangNode leaf(String identifier, String value) {
        requireIdentifier(identifier);
        if (value == null) {
            throw new IllegalArgumentException("Leaf value cannot be null");
        }
        return new YangNode(identifier, value, Collections.<YangNode>emptyList());
    }

    /**
     * Creates an inner node such as a container or a list entry.
     *
     * @param identifier node identifier, never {@code null}
     * @param children descendants, possibly empty
     * @return the created inner node
     */
    public static YangNode inner(String identifier, List<YangNode> children) {
        requireIdentifier(identifier);
        return new YangNode(identifier, null, copyOf(children));
    }

    /**
     * Creates the synthetic root that wraps the top-level nodes of a document.
     *
     * @param children top-level nodes of the document
     * @return the created document node
     */
    public static YangNode document(List<YangNode> children) {
        return new YangNode(DOCUMENT_IDENTIFIER, null, copyOf(children));
    }

    public String identifier() {
        return identifier;
    }

    /**
     * @return the textual value, or {@code null} when this node is not a leaf
     */
    public String value() {
        return value;
    }

    public List<YangNode> children() {
        return children;
    }

    public boolean isLeaf() {
        return value != null;
    }

    public boolean isDocument() {
        return value == null && DOCUMENT_IDENTIFIER.equals(identifier);
    }

    private static List<YangNode> copyOf(List<YangNode> children) {
        if (children == null) {
            throw new IllegalArgumentException("Children cannot be null");
        }
        List<YangNode> copy = new ArrayList<YangNode>(children.size());
        for (YangNode child : children) {
            if (child == null) {
                throw new IllegalArgumentException("Children cannot contain null entries");
            }
            copy.add(child);
        }
        return Collections.unmodifiableList(copy);
    }

    private static void requireIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Node identifier cannot be null or empty");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof YangNode)) {
            return false;
        }
        YangNode that = (YangNode) other;
        return identifier.equals(that.identifier)
                && (value == null ? that.value == null : value.equals(that.value))
                && children.equals(that.children);
    }

    @Override
    public int hashCode() {
        int result = identifier.hashCode();
        result = 31 * result + (value == null ? 0 : value.hashCode());
        result = 31 * result + children.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (isLeaf()) {
            return identifier + " = " + value;
        }
        return identifier + " (" + children.size() + " children)";
    }
}
