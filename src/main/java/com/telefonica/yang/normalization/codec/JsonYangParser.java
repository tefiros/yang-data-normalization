package com.telefonica.yang.normalization.codec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.telefonica.yang.normalization.NormalizationException;
import com.telefonica.yang.normalization.NormalizationOptions;
import com.telefonica.yang.normalization.YangNode;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the abstract tree from a JSON representation, as defined by RFC 7951.
 */
public final class JsonYangParser implements YangParser {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final NormalizationOptions options;

    public JsonYangParser(NormalizationOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }
        this.options = options;
    }

    @Override
    public YangNode parse(byte[] input) throws NormalizationException {
        if (input == null) {
            throw new NormalizationException("JSON input cannot be null");
        }

        JsonElement root;
        try {
            root = JsonParser.parseString(new String(input, UTF_8));
        } catch (JsonParseException e) {
            throw new NormalizationException("Malformed JSON document: " + e.getMessage(), e);
        }

        if (!root.isJsonObject()) {
            throw new NormalizationException("A YANG JSON document must have an object at its root");
        }

        List<YangNode> roots = new ArrayList<YangNode>();
        appendObject(root.getAsJsonObject(), roots);
        return YangNode.document(roots);
    }

    private void appendObject(JsonObject object, List<YangNode> output) throws NormalizationException {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String identifier = Identifiers.normalize(entry.getKey(), options.stripModulePrefixes());
            appendValue(identifier, entry.getValue(), output);
        }
    }

    private void appendValue(String identifier, JsonElement value, List<YangNode> output)
            throws NormalizationException {
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            if (isEmptyType(array)) {
                output.add(YangNode.leaf(identifier, ""));
                return;
            }
            // List and leaf-list entries become siblings sharing the identifier.
            for (JsonElement item : array) {
                appendValue(identifier, item, output);
            }
            return;
        }

        if (value.isJsonObject()) {
            List<YangNode> children = new ArrayList<YangNode>();
            appendObject(value.getAsJsonObject(), children);
            output.add(YangNode.inner(identifier, children));
            return;
        }

        output.add(YangNode.leaf(identifier, scalarOf(value)));
    }

    /** RFC 7951 encodes the YANG {@code empty} type as {@code [null]}. */
    private boolean isEmptyType(JsonArray array) {
        return array.size() == 1 && array.get(0).isJsonNull();
    }

    private String scalarOf(JsonElement value) {
        if (value.isJsonNull()) {
            return "";
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return Boolean.toString(primitive.getAsBoolean());
        }
        return primitive.getAsString();
    }
}
