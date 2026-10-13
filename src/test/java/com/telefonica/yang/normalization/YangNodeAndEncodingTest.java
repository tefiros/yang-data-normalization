package com.telefonica.yang.normalization;

import com.telefonica.yang.normalization.encoding.DeterministicAstEncoder;
import com.telefonica.yang.normalization.encoding.Fingerprint;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangNodeAndEncodingTest {

    @Test
    void nodesAreImmutableValueObjects() {
        List<YangNode> source = new ArrayList<YangNode>();
        source.add(YangNode.leaf("name", "eth0"));
        YangNode node = YangNode.inner("interface", source);
        source.clear();

        assertEquals(1, node.children().size());
        assertThrows(UnsupportedOperationException.class,
                () -> node.children().add(YangNode.leaf("enabled", "true")));
        assertEquals(
                YangNode.inner("interface",
                        Collections.singletonList(YangNode.leaf("name", "eth0"))),
                node);
        assertEquals(node.hashCode(),
                YangNode.inner("interface",
                        Collections.singletonList(YangNode.leaf("name", "eth0"))).hashCode());
    }

    @Test
    void factoriesEnforceAstInvariants() {
        assertThrows(IllegalArgumentException.class, () -> YangNode.leaf(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> YangNode.leaf("", "value"));
        assertThrows(IllegalArgumentException.class, () -> YangNode.leaf("leaf", null));
        assertThrows(IllegalArgumentException.class, () -> YangNode.inner("node", null));
        assertThrows(IllegalArgumentException.class,
                () -> YangNode.inner("node", Arrays.asList(YangNode.leaf("ok", "yes"), null)));
    }

    @Test
    void encodingIsVersionedUnambiguousAndOrderIndependent() throws Exception {
        YangNode first = YangNode.document(Arrays.asList(
                YangNode.leaf("b", "two"),
                YangNode.inner("a", Collections.singletonList(YangNode.leaf("x", "one")))));
        YangNode reordered = YangNode.document(Arrays.asList(
                YangNode.inner("a", Collections.singletonList(YangNode.leaf("x", "one"))),
                YangNode.leaf("b", "two")));
        YangNode structurallyDifferent = YangNode.document(Arrays.asList(
                YangNode.leaf("a", "x"),
                YangNode.leaf("b", "two")));

        byte[] encoded = DeterministicAstEncoder.encode(first);
        assertTrue(new String(encoded, 0, DeterministicAstEncoder.FORMAT_VERSION.length(),
                StandardCharsets.US_ASCII).startsWith("YANG-NORM-"));
        assertArrayEquals(encoded, DeterministicAstEncoder.encode(reordered));
        assertNotEquals(
                Fingerprint.toHex(encoded),
                Fingerprint.toHex(DeterministicAstEncoder.encode(structurallyDifferent)));
    }

    @Test
    void formatterShowsHierarchyAndCanonicalListOrder() {
        YangNode eth1 = YangNode.inner("interface", Arrays.asList(
                YangNode.leaf("name", "eth1"), YangNode.leaf("enabled", "false")));
        YangNode eth0 = YangNode.inner("interface", Arrays.asList(
                YangNode.leaf("name", "eth0"), YangNode.leaf("enabled", "true")));
        YangNode root = YangNode.document(Collections.singletonList(
                YangNode.inner("interfaces", Arrays.asList(eth1, eth0))));

        String formatted = YangTreeFormatter.format(root);
        assertEquals(
                "interfaces\n"
                        + "\u251c\u2500 interface\n"
                        + "\u2502  \u251c\u2500 enabled = true\n"
                        + "\u2502  \u2514\u2500 name = eth0\n"
                        + "\u2514\u2500 interface\n"
                        + "   \u251c\u2500 enabled = false\n"
                        + "   \u2514\u2500 name = eth1\n",
                formatted);

        YangNode reversed = YangNode.document(Collections.singletonList(
                YangNode.inner("interfaces", Arrays.asList(eth0, eth1))));
        assertEquals(formatted, YangTreeFormatter.format(reversed));
    }

    @Test
    void fingerprintUsesSha256AndLowercaseHex() throws Exception {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223"
                        + "b00361a396177a9cb410ff61f20015ad",
                Fingerprint.toHex(Fingerprint.sha256(
                        "abc".getBytes(StandardCharsets.US_ASCII))));
        assertThrows(NormalizationException.class, () -> Fingerprint.sha256(null));
        assertThrows(IllegalArgumentException.class, () -> Fingerprint.toHex(null));
    }
}
