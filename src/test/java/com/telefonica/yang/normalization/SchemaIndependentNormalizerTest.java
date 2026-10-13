package com.telefonica.yang.normalization;

import com.telefonica.yang.normalization.encoding.Fingerprint;
import com.upokecenter.cbor.CBORObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaIndependentNormalizerTest {

    private final YangNormalizer normalizer = SchemaIndependentNormalizer.create();

    @Test
    void equivalentXmlJsonAndCborProduceTheSameRepresentation() throws Exception {
        String xml =
                "<top><list>"
                        + "<item><val>42.5</val><foo>true</foo><bar>Chocolate</bar></item>"
                        + "<item><val>44.5</val><foo>true</foo><bar>Banana</bar></item>"
                        + "</list></top>";
        String json =
                "{\"top\":{\"list\":{\"item\":["
                        + "{\"bar\":\"Banana\",\"foo\":true,\"val\":44.5},"
                        + "{\"bar\":\"Chocolate\",\"foo\":true,\"val\":42.5}"
                        + "]}}}";

        byte[] xmlNormalized = normalizer.normalizeXml(xml);
        byte[] jsonNormalized = normalizer.normalizeJson(json);
        byte[] cborNormalized =
                normalizer.normalizeCbor(CBORObject.FromJSONString(json).EncodeToBytes());

        assertArrayEquals(xmlNormalized, jsonNormalized);
        assertArrayEquals(xmlNormalized, cborNormalized);
        assertArrayEquals(
                Fingerprint.sha256(xmlNormalized), Fingerprint.sha256(cborNormalized));
    }

    @Test
    void siblingAndListEntryOrderIsNotSignificant() throws Exception {
        String first =
                "{\"top\":{\"item\":[{\"name\":\"eth1\",\"up\":false},"
                        + "{\"name\":\"eth0\",\"up\":true}],\"count\":2}}";
        String second =
                "{\"top\":{\"count\":2,\"item\":[{\"up\":true,\"name\":\"eth0\"},"
                        + "{\"up\":false,\"name\":\"eth1\"}]}}";

        assertTrue(normalizer.areEquivalent(
                bytes(first), SerializationFormat.JSON,
                bytes(second), SerializationFormat.JSON));
    }

    @Test
    void repeatedEntriesArePreserved() throws Exception {
        byte[] one = normalizer.normalizeJson("{\"top\":{\"tag\":[\"a\"]}}");
        byte[] two = normalizer.normalizeJson("{\"top\":{\"tag\":[\"a\",\"a\"]}}");

        assertFalse(Arrays.equals(one, two));
    }

    @Test
    void distinctValuesAndStructuresRemainDistinct() throws Exception {
        byte[] valueOne = normalizer.normalizeJson("{\"top\":{\"value\":\"1\"}}");
        byte[] valueTwo = normalizer.normalizeJson("{\"top\":{\"value\":\"2\"}}");
        byte[] nested = normalizer.normalizeJson("{\"top\":{\"value\":{\"child\":\"1\"}}}");

        assertFalse(Arrays.equals(valueOne, valueTwo));
        assertFalse(Arrays.equals(valueOne, nested));
    }

    @Test
    void defaultsPreserveSignificantXmlWhitespace() throws Exception {
        byte[] xml = normalizer.normalizeXml("<name> eth0 </name>");
        byte[] json = normalizer.normalizeJson("{\"name\":\" eth0 \"}");

        assertArrayEquals(json, xml);
        assertFalse(Arrays.equals(xml, normalizer.normalizeJson("{\"name\":\"eth0\"}")));
    }

    @Test
    void xmlWhitespaceCanBeTrimmedExplicitly() throws Exception {
        YangNormalizer trimming = SchemaIndependentNormalizer.create(
                NormalizationOptions.defaults().withTrimXmlLeafWhitespace(true));

        assertArrayEquals(
                trimming.normalizeXml("<name> eth0 </name>"),
                trimming.normalizeJson("{\"name\":\"eth0\"}"));
    }

    @Test
    void modulePrefixesAreRemovedByDefaultAndCanBePreserved() throws Exception {
        assertArrayEquals(
                normalizer.normalizeXml("<if:interface xmlns:if=\"urn:ietf:params:xml:ns:yang:ietf-interfaces\"/>"),
                normalizer.normalizeJson("{\"ietf-interfaces:interface\":\"\"}"));

        YangNormalizer preserving = SchemaIndependentNormalizer.create(
                NormalizationOptions.defaults().withStripModulePrefixes(false));
        assertFalse(Arrays.equals(
                preserving.normalizeXml("<if:interface xmlns:if=\"urn:test\"/>"),
                preserving.normalizeJson("{\"ietf-interfaces:interface\":\"\"}")));
    }

    @Test
    void cborByteStringMatchesBase64TextRepresentations() throws Exception {
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add("binary", new byte[] {0, 1, 2, (byte) 0xff});

        assertArrayEquals(
                normalizer.normalizeJson("{\"binary\":\"AAEC/w==\"}"),
                normalizer.normalizeCbor(cbor.EncodeToBytes()));
    }

    @Test
    void publicApiRejectsNullArguments() {
        assertThrows(NormalizationException.class,
                () -> normalizer.parse(null, SerializationFormat.JSON));
        assertThrows(NormalizationException.class,
                () -> normalizer.parse(new byte[0], null));
        assertThrows(NormalizationException.class, () -> normalizer.encode(null));
        assertThrows(NormalizationException.class, () -> normalizer.normalizeJson(null));
        assertThrows(IllegalArgumentException.class,
                () -> SchemaIndependentNormalizer.create(null));
    }

    @Test
    void formatNamesAreCaseInsensitive() throws Exception {
        assertEquals(SerializationFormat.XML, SerializationFormat.fromName("xMl"));
        assertThrows(NormalizationException.class,
                () -> SerializationFormat.fromName("yaml"));
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
