package com.telefonica.yang.normalization.tool;

import com.telefonica.yang.normalization.SchemaIndependentNormalizer;
import com.telefonica.yang.normalization.SerializationFormat;
import com.telefonica.yang.normalization.YangNode;
import com.telefonica.yang.normalization.YangNormalizer;
import com.telefonica.yang.normalization.YangTreeFormatter;
import com.telefonica.yang.normalization.encoding.Fingerprint;
import com.upokecenter.cbor.CBORObject;

/**
 * Small screen demonstration of the AST-based normalization process.
 */
public final class SimpleDemo {

    private SimpleDemo() {
    }

    public static void main(String[] args) throws Exception {
        String xml =
                "<interfaces>"
                        + "<interface>"
                        + "<name>eth0</name>"
                        + "<description>Uplink to the core network</description>"
                        + "<type>ethernetCsmacd</type>"
                        + "<enabled>true</enabled>"
                        + "<ipv4><address><ip>192.0.2.10</ip>"
                        + "<prefix-length>24</prefix-length></address></ipv4>"
                        + "</interface>"
                        + "<interface>"
                        + "<name>eth1</name>"
                        + "<description>Link to the lab</description>"
                        + "<type>ethernetCsmacd</type>"
                        + "<enabled>false</enabled>"
                        + "</interface>"
                        + "</interfaces>";
        String json =
                "{\"interfaces\":{\"interface\":["
                        + "{\"name\":\"eth0\","
                        + "\"description\":\"Uplink to the core network\","
                        + "\"type\":\"ethernetCsmacd\",\"enabled\":true,"
                        + "\"ipv4\":{\"address\":{\"ip\":\"192.0.2.10\","
                        + "\"prefix-length\":24}}},"
                        + "{\"name\":\"eth1\","
                        + "\"description\":\"Link to the lab\","
                        + "\"type\":\"ethernetCsmacd\",\"enabled\":false}"
                        + "]}}";
        YangNormalizer normalizer = SchemaIndependentNormalizer.create();

        System.out.println("=== YANG DATA NORMALIZATION ===");
        System.out.println();
        System.out.println("1) INPUT XML");
        System.out.println(xml);
        System.out.println();
        System.out.println("2) INPUT JSON");
        System.out.println(json);
        System.out.println();

        YangNode xmlTree = normalizer.parse(
                xml.getBytes(YangNormalizer.UTF_8), SerializationFormat.XML);
        YangNode jsonTree = normalizer.parse(
                json.getBytes(YangNormalizer.UTF_8), SerializationFormat.JSON);
        CBORObject cbor = CBORObject.FromJSONString(json);
        YangNode cborTree = normalizer.parse(
                cbor.EncodeToBytes(), SerializationFormat.CBOR);

        System.out.println("3) AST (los tres documentos producen este arbol)");
        System.out.println("XML:");
        System.out.println(asciiTree(YangTreeFormatter.format(xmlTree)));
        System.out.println("JSON:");
        System.out.println(asciiTree(YangTreeFormatter.format(jsonTree)));
        System.out.println("CBOR:");
        System.out.println(asciiTree(YangTreeFormatter.format(cborTree)));

        byte[] xmlNormalized = normalizer.encode(xmlTree);
        byte[] jsonNormalized = normalizer.encode(jsonTree);
        byte[] cborNormalized = normalizer.encode(cborTree);
        String xmlFingerprint = Fingerprint.toHex(Fingerprint.sha256(xmlNormalized));
        String jsonFingerprint = Fingerprint.toHex(Fingerprint.sha256(jsonNormalized));
        String cborFingerprint = Fingerprint.toHex(Fingerprint.sha256(cborNormalized));

        System.out.println("4) RESULTADO");
        System.out.println("Bytes normalizados: " + xmlNormalized.length);
        System.out.println("Huella XML : " + xmlFingerprint);
        System.out.println("Huella JSON: " + jsonFingerprint);
        System.out.println("Huella CBOR: " + cborFingerprint);
        System.out.println("Son equivalentes? " + java.util.Arrays.equals(
                xmlNormalized, jsonNormalized));
        System.out.println("XML, JSON y CBOR coinciden? "
                + java.util.Arrays.equals(xmlNormalized, cborNormalized));
    }

    private static String asciiTree(String tree) {
        return tree.replace("\u251c\u2500 ", "+-- ")
                .replace("\u2514\u2500 ", "+-- ")
                .replace("\u2502  ", "|   ");
    }
}
