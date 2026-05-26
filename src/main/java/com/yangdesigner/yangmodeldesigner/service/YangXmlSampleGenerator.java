package com.yangdesigner.yangmodeldesigner.service;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class YangXmlSampleGenerator {
    private static final Set<YangNodeType> STRUCTURAL_NODES = Set.of(
            YangNodeType.CONTAINER,
            YangNodeType.LIST,
            YangNodeType.LEAF,
            YangNodeType.LEAF_LIST,
            YangNodeType.ANYDATA,
            YangNodeType.ANYXML
    );

    public String generate(YangDocument document) {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(System.lineSeparator());
        String namespace = firstConstraint(document.root(), "namespace");
        if (namespace.isBlank()) {
            builder.append("<data>").append(System.lineSeparator());
        } else {
            builder.append("<data xmlns=\"").append(escapeAttribute(namespace)).append("\">").append(System.lineSeparator());
        }
        for (YangNode child : document.root().children()) {
            writeNode(builder, child, 1);
        }
        builder.append("</data>").append(System.lineSeparator());
        return builder.toString();
    }

    private void writeNode(StringBuilder builder, YangNode node, int indent) {
        if (!isRenderable(node)) {
            for (YangNode child : node.children()) {
                writeNode(builder, child, indent);
            }
            return;
        }
        switch (node.type()) {
            case CONTAINER, LIST -> writeElementWithChildren(builder, node, indent);
            case LEAF, LEAF_LIST -> writeLeaf(builder, node, indent);
            case ANYDATA, ANYXML -> line(builder, indent, "<" + xmlName(node.name()) + "/>");
            default -> {
                for (YangNode child : node.children()) {
                    writeNode(builder, child, indent);
                }
            }
        }
    }

    private void writeElementWithChildren(StringBuilder builder, YangNode node, int indent) {
        String name = xmlName(node.name());
        line(builder, indent, "<" + name + ">");
        if (node.children().isEmpty()) {
            line(builder, indent + 1, "<!-- empty " + node.type().keyword() + " -->");
        } else {
            for (YangNode child : node.children()) {
                writeNode(builder, child, indent + 1);
            }
        }
        line(builder, indent, "</" + name + ">");
    }

    private void writeLeaf(StringBuilder builder, YangNode node, int indent) {
        String name = xmlName(node.name());
        String value = sampleValue(node.dataType(), node.constraints());
        line(builder, indent, "<" + name + ">" + escapeText(value) + "</" + name + ">");
    }

    private boolean isRenderable(YangNode node) {
        return STRUCTURAL_NODES.contains(node.type());
    }

    private String sampleValue(String dataType, Map<String, List<String>> constraints) {
        String defaultValue = firstValue(constraints.get("default"));
        if (!defaultValue.isBlank()) {
            return defaultValue;
        }
        String cleanType = dataType == null ? "" : dataType.strip();
        int separator = cleanType.indexOf(':');
        if (separator >= 0) {
            cleanType = cleanType.substring(separator + 1);
        }
        return switch (cleanType) {
            case "boolean" -> "true";
            case "int8", "int16", "int32", "int64", "uint8", "uint16", "uint32", "uint64" -> "1";
            case "decimal64" -> "1.0";
            case "empty" -> "";
            case "identityref" -> "identity-value";
            case "leafref", "instance-identifier" -> "/sample/path";
            case "binary" -> "c2FtcGxl";
            default -> "sample";
        };
    }

    private String firstConstraint(YangNode node, String keyword) {
        return firstValue(node.constraints().get(keyword));
    }

    private String firstValue(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.getFirst();
    }

    private String xmlName(String name) {
        String clean = name == null ? "" : name.strip();
        if (clean.isBlank()) {
            return "unnamed";
        }
        if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() > 1) {
            clean = clean.substring(1, clean.length() - 1);
        }
        clean = clean.replaceAll("[^A-Za-z0-9_.:-]", "-");
        if (!clean.matches("[A-Za-z_].*")) {
            clean = "_" + clean;
        }
        return clean;
    }

    private String escapeText(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String escapeAttribute(String value) {
        return escapeText(value).replace("\"", "&quot;");
    }

    private void line(StringBuilder builder, int indent, String text) {
        builder.append("  ".repeat(indent)).append(text).append(System.lineSeparator());
    }
}
