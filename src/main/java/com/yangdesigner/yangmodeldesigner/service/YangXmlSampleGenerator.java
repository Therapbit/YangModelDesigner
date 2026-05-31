package com.yangdesigner.yangmodeldesigner.service;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class YangXmlSampleGenerator {
    private static final String INDENT = "    ";
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
        Map<String, YangNode> groupings = collectGroupings(document.root());
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(System.lineSeparator());
        String namespace = firstConstraint(document.root(), "namespace");
        if (namespace.isBlank()) {
            builder.append("<data>").append(System.lineSeparator());
        } else {
            builder.append("<data xmlns=\"").append(escapeAttribute(namespace)).append("\">").append(System.lineSeparator());
        }
        for (YangNode child : document.root().children()) {
            writeNode(builder, child, 1, groupings, new HashSet<>());
        }
        builder.append("</data>").append(System.lineSeparator());
        return builder.toString();
    }

    private void writeNode(StringBuilder builder, YangNode node, int indent, Map<String, YangNode> groupings, Set<String> activeUses) {
        if (node.type() == YangNodeType.GROUPING) {
            return;
        }
        if (node.type() == YangNodeType.USES) {
            writeUses(builder, node, indent, groupings, activeUses);
            return;
        }
        if (!isRenderable(node)) {
            for (YangNode child : node.children()) {
                writeNode(builder, child, indent, groupings, activeUses);
            }
            return;
        }
        switch (node.type()) {
            case CONTAINER, LIST -> writeElementWithChildren(builder, node, indent, groupings, activeUses);
            case LEAF, LEAF_LIST -> writeLeaf(builder, node, indent);
            case ANYDATA, ANYXML -> line(builder, indent, "<" + xmlName(node.name()) + "/>");
            default -> {
                for (YangNode child : node.children()) {
                    writeNode(builder, child, indent, groupings, activeUses);
                }
            }
        }
    }

    private void writeElementWithChildren(StringBuilder builder, YangNode node, int indent, Map<String, YangNode> groupings, Set<String> activeUses) {
        String name = xmlName(node.name());
        line(builder, indent, "<" + name + ">");
        if (!hasRenderableChildren(node, groupings)) {
            line(builder, indent + 1, "<!-- empty " + node.type().keyword() + " -->");
        } else {
            for (YangNode child : node.children()) {
                writeNode(builder, child, indent + 1, groupings, activeUses);
            }
        }
        line(builder, indent, "</" + name + ">");
    }

    private void writeUses(StringBuilder builder, YangNode uses, int indent, Map<String, YangNode> groupings, Set<String> activeUses) {
        YangNode grouping = groupings.get(uses.name());
        if (grouping == null) {
            grouping = groupings.get(unprefixedName(uses.name()));
        }
        if (grouping == null || !activeUses.add(grouping.name())) {
            return;
        }
        try {
            for (YangNode child : grouping.children()) {
                writeNode(builder, child, indent, groupings, activeUses);
            }
        } finally {
            activeUses.remove(grouping.name());
        }
    }

    private void writeLeaf(StringBuilder builder, YangNode node, int indent) {
        String name = xmlName(node.name());
        String value = sampleValue(node.dataType(), node.constraints());
        line(builder, indent, "<" + name + ">" + escapeText(value) + "</" + name + ">");
    }

    private boolean isRenderable(YangNode node) {
        return STRUCTURAL_NODES.contains(node.type());
    }

    private boolean hasRenderableChildren(YangNode node, Map<String, YangNode> groupings) {
        for (YangNode child : node.children()) {
            if (child.type() == YangNodeType.GROUPING) {
                continue;
            }
            if (child.type() == YangNodeType.USES) {
                if (groupings.containsKey(child.name()) || groupings.containsKey(unprefixedName(child.name()))) {
                    return true;
                }
                continue;
            }
            if (isRenderable(child) || hasRenderableChildren(child, groupings)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, YangNode> collectGroupings(YangNode root) {
        Map<String, YangNode> groupings = new LinkedHashMap<>();
        collectGroupings(root, groupings);
        return groupings;
    }

    private void collectGroupings(YangNode node, Map<String, YangNode> groupings) {
        if (node.type() == YangNodeType.GROUPING && !node.name().isBlank()) {
            groupings.putIfAbsent(node.name(), node);
            groupings.putIfAbsent(unprefixedName(node.name()), node);
        }
        for (YangNode child : node.children()) {
            collectGroupings(child, groupings);
        }
    }

    private String unprefixedName(String name) {
        String clean = name == null ? "" : name.strip();
        int separator = clean.indexOf(':');
        return separator >= 0 ? clean.substring(separator + 1) : clean;
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
        builder.append(INDENT.repeat(indent)).append(text).append(System.lineSeparator());
    }
}
