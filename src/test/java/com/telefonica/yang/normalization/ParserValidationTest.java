package com.telefonica.yang.normalization;

import com.upokecenter.cbor.CBORObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserValidationTest {

    private final YangNormalizer normalizer = SchemaIndependentNormalizer.create();

    @Test
    void rejectsMalformedOrNonObjectJson() {
        NormalizationException malformed = assertThrows(
                NormalizationException.class, () -> normalizer.normalizeJson("{"));
        assertTrue(malformed.getMessage().startsWith("Malformed JSON document:"));

        assertThrows(NormalizationException.class,
                () -> normalizer.normalizeJson("[{\"name\":\"eth0\"}]"));
    }

    @Test
    void rejectsMalformedMixedAndExternalEntityXml() {
        assertThrows(NormalizationException.class,
                () -> normalizer.normalizeXml("<top>"));
        assertThrows(NormalizationException.class,
                () -> normalizer.normalizeXml("<top>text<child/></top>"));
        assertThrows(NormalizationException.class,
                () -> normalizer.normalizeXml(
                        "<!DOCTYPE top [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                                + "<top>&xxe;</top>"));
    }

    @Test
    void rejectsUnsupportedCborRootsKeysAndValues() {
        assertThrows(NormalizationException.class,
                () -> normalizer.normalizeCbor(CBORObject.FromObject("text").EncodeToBytes()));

        CBORObject integerKey = CBORObject.NewMap();
        integerKey.Add(123, "value");
        assertThrows(NormalizationException.class,
                () -> normalizer.normalizeCbor(integerKey.EncodeToBytes()));

        CBORObject unsupported = CBORObject.NewMap();
        unsupported.Add("undefined", CBORObject.Undefined);
        assertThrows(NormalizationException.class,
                () -> normalizer.normalizeCbor(unsupported.EncodeToBytes()));
    }

    @Test
    void rejectsMixedXmlContentEvenWhenWhitespaceTrimmingIsDisabled() {
        byte[] input = "<top>\n text <child>value</child>\n</top>"
                .getBytes(StandardCharsets.UTF_8);
        assertThrows(NormalizationException.class,
                () -> normalizer.normalize(input, SerializationFormat.XML));
    }
}
