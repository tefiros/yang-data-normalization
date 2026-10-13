package com.telefonica.yang.normalization.codec;

import com.telefonica.yang.normalization.NormalizationException;
import com.telefonica.yang.normalization.NormalizationOptions;
import com.telefonica.yang.normalization.YangNode;
import com.upokecenter.cbor.CBORException;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Builds the abstract tree from a CBOR representation, as defined by RFC 9254.
 *
 * <p>Only the schema-independent subset is supported: identifiers must be text
 * strings, because resolving YANG SIDs requires schema knowledge.
 */
public final class CborYangParser implements YangParser {

    private final NormalizationOptions options;

    public CborYangParser(NormalizationOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }
        this.options = options;
    }

    @Override
    public YangNode parse(byte[] input) throws NormalizationException {
        if (input == null) {
            throw new NormalizationException("CBOR input cannot be null");
        }

        CBORObject root;
        try {
            root = CBORObject.DecodeFromBytes(input);
        } catch (CBORException e) {
            throw new NormalizationException("Malformed CBOR document: " + e.getMessage(), e);
        }

        rejectUnsupportedTags(root);
        if (root.getType() != CBORType.Map) {
            throw new NormalizationException("A YANG CBOR document must have a map at its root");
        }

        List<YangNode> roots = new ArrayList<YangNode>();
        appendMap(root, roots);
        return YangNode.document(roots);
    }

    private void appendMap(CBORObject map, List<YangNode> output) throws NormalizationException {
        for (Map.Entry<CBORObject, CBORObject> entry : map.getEntries()) {
            CBORObject key = entry.getKey();
            rejectUnsupportedTags(key);

            if (key.getType() != CBORType.TextString) {
                throw new NormalizationException(
                        "Only text-string member names are supported; YANG SIDs require schema context");
            }

            String identifier = Identifiers.normalize(key.AsString(), options.stripModulePrefixes());
            appendValue(identifier, entry.getValue(), output);
        }
    }

    private void appendValue(String identifier, CBORObject value, List<YangNode> output)
            throws NormalizationException {
        rejectUnsupportedTags(value);

        // Numbers are checked first because decimal values arrive tagged.
        if (value.isNumber()) {
            output.add(YangNode.leaf(identifier, scalarOf(value)));
            return;
        }

        if (value.getType() == CBORType.Array) {
            if (isEmptyType(value)) {
                output.add(YangNode.leaf(identifier, ""));
                return;
            }
            // List and leaf-list entries become siblings sharing the identifier.
            for (CBORObject item : value.getValues()) {
                appendValue(identifier, item, output);
            }
            return;
        }

        if (value.getType() == CBORType.Map) {
            List<YangNode> children = new ArrayList<YangNode>();
            appendMap(value, children);
            output.add(YangNode.inner(identifier, children));
            return;
        }

        output.add(YangNode.leaf(identifier, scalarOf(value)));
    }

    /** RFC 9254 encodes the YANG {@code empty} type as a single-element null array. */
    private boolean isEmptyType(CBORObject array) {
        return array.size() == 1 && array.get(0).isNull();
    }

    private String scalarOf(CBORObject value) throws NormalizationException {
        if (value.isNull()) {
            return "";
        }
        if (value.isNumber()) {
            if (!value.AsNumber().IsFinite()) {
                throw new NormalizationException(
                        "Non-finite CBOR numbers have no XML or JSON equivalent");
            }
            return value.ToJSONString();
        }

        CBORType type = value.getType();
        if (type == CBORType.TextString) {
            return value.AsString();
        }
        if (type == CBORType.Boolean) {
            return Boolean.toString(value.AsBoolean());
        }
        if (type == CBORType.ByteString) {
            // Matches the base64 form that RFC 7951 uses for the YANG binary type.
            return Base64.getEncoder().encodeToString(value.GetByteString());
        }
        throw new NormalizationException("Unsupported schema-independent CBOR value type: " + type);
    }

    private void rejectUnsupportedTags(CBORObject value) throws NormalizationException {
        if (value.isTagged() && !value.isNumber()) {
            throw new NormalizationException(
                    "Non-numeric CBOR tags require schema-aware normalization and are not supported");
        }
    }
}
