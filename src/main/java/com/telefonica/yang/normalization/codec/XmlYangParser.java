package com.telefonica.yang.normalization.codec;

import com.telefonica.yang.normalization.NormalizationException;
import com.telefonica.yang.normalization.NormalizationOptions;
import com.telefonica.yang.normalization.YangNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the abstract tree from an XML representation, as produced by NETCONF
 * or by XML-encoded RESTCONF responses.
 */
public final class XmlYangParser implements YangParser {

    private final NormalizationOptions options;

    public XmlYangParser(NormalizationOptions options) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }
        this.options = options;
    }

    @Override
    public YangNode parse(byte[] input) throws NormalizationException {
        if (input == null) {
            throw new NormalizationException("XML input cannot be null");
        }

        Document document;
        try {
            document = newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(input)));
        } catch (ParserConfigurationException e) {
            throw new NormalizationException("Unable to configure a secure XML parser", e);
        } catch (SAXException e) {
            throw new NormalizationException("Malformed XML document: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new NormalizationException("Unable to read the XML document", e);
        }

        Element root = document.getDocumentElement();
        if (root == null) {
            throw new NormalizationException("The XML document has no root element");
        }
        return YangNode.document(Collections.singletonList(toNode(root)));
    }

    private DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder();
    }

    private YangNode toNode(Element element) throws NormalizationException {
        List<YangNode> children = new ArrayList<YangNode>();
        StringBuilder text = new StringBuilder();

        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            short nodeType = node.getNodeType();

            if (nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                children.add(toNode((Element) node));
            } else if (nodeType == org.w3c.dom.Node.TEXT_NODE
                    || nodeType == org.w3c.dom.Node.CDATA_SECTION_NODE) {
                text.append(node.getNodeValue());
            }
        }

        String identifier = identifierOf(element);
        if (children.isEmpty()) {
            return YangNode.leaf(identifier, valueOf(text.toString()));
        }
        if (text.toString().trim().length() > 0) {
            throw new NormalizationException(
                    "Mixed XML content is not valid YANG data at node '" + identifier + "'");
        }
        return YangNode.inner(identifier, children);
    }

    private String valueOf(String text) {
        return options.trimXmlLeafWhitespace() ? text.trim() : text;
    }

    private String identifierOf(Element element) throws NormalizationException {
        if (options.stripModulePrefixes()) {
            String localName = element.getLocalName();
            String name = localName != null ? localName : element.getTagName();
            return Identifiers.normalize(name, true);
        }
        return Identifiers.normalize(element.getTagName(), false);
    }
}
