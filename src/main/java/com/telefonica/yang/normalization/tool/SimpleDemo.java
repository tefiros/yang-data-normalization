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
                "<interfaces xmlns=\"urn:ietf:params:xml:ns:yang:ietf-interfaces\"\n"
                        + "    xmlns:iana-if-type=\"urn:ietf:params:xml:ns:yang:iana-if-type\"\n"
                        + "    xmlns:vlan=\"http://example.com/vlan\">\n"
                        + "  <interface>\n"
                        + "    <name>eth0</name>\n"
                        + "    <type>iana-if-type:ethernetCsmacd</type>\n"
                        + "    <enabled>false</enabled>\n"
                        + "  </interface>\n"
                        + "  <interface>\n"
                        + "    <name>eth1</name>\n"
                        + "    <type>iana-if-type:ethernetCsmacd</type>\n"
                        + "    <enabled>true</enabled>\n"
                        + "    <vlan:vlan-tagging>true</vlan:vlan-tagging>\n"
                        + "  </interface>\n"
                        + "  <interface>\n"
                        + "    <name>eth1.10</name>\n"
                        + "    <type>iana-if-type:l2vlan</type>\n"
                        + "    <enabled>true</enabled>\n"
                        + "    <vlan:base-interface>eth1</vlan:base-interface>\n"
                        + "    <vlan:vlan-id>10</vlan:vlan-id>\n"
                        + "  </interface>\n"
                        + "  <interface>\n"
                        + "    <name>lo1</name>\n"
                        + "    <type>iana-if-type:softwareLoopback</type>\n"
                        + "    <enabled>true</enabled>\n"
                        + "  </interface>\n"
                        + "</interfaces>";
        String json =
                "{\n"
                        + "  \"ietf-interfaces:interfaces\": {\n"
                        + "    \"interface\": [\n"
                        + "      {\n"
                        + "        \"enabled\": true,\n"
                        + "        \"name\": \"lo1\",\n"
                        + "        \"type\": \"iana-if-type:softwareLoopback\"\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"vlan:vlan-id\": 10,\n"
                        + "        \"enabled\": true,\n"
                        + "        \"type\": \"iana-if-type:l2vlan\",\n"
                        + "        \"vlan:base-interface\": \"eth1\",\n"
                        + "        \"name\": \"eth1.10\"\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"enabled\": false,\n"
                        + "        \"type\": \"iana-if-type:ethernetCsmacd\",\n"
                        + "        \"name\": \"eth0\"\n"
                        + "      },\n"
                        + "      {\n"
                        + "        \"vlan:vlan-tagging\": true,\n"
                        + "        \"name\": \"eth1\",\n"
                        + "        \"enabled\": true,\n"
                        + "        \"type\": \"iana-if-type:ethernetCsmacd\"\n"
                        + "      }\n"
                        + "    ]\n"
                        + "  }\n"
                        + "}";
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
        System.out.println(YangTreeFormatter.format(xmlTree));
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
