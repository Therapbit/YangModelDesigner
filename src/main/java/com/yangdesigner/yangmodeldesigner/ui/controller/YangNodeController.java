package com.yangdesigner.yangmodeldesigner.ui.controller;

import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.ui.state.UiMessage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class YangNodeController {
    public List<UiMessage> applyNodeChanges(YangNode node, NodeEditData data) {
        node.setName(valueOrEmpty(data.name()));
        node.setDataType(valueOrEmpty(data.dataType()));
        node.setDescription("-".equals(data.description()) ? "" : valueOrEmpty(data.description()));
        node.clearConstraints();
        applyBooleanConstraint(node, "config", data.config());
        applyBooleanConstraint(node, "mandatory", data.mandatory());
        applyTextConstraint(node, "range", data.range());
        return applyConstraints(node, data.constraintsText());
    }

    public AddChildResult addChild(YangNode parent, YangNodeType type, NodeEditData data) {
        String name = valueOrEmpty(data.name());
        YangNode child = new YangNode(type, name.isBlank() ? defaultName(type, parent) : name);
        if (needsDefaultDataType(type)) {
            String dataType = valueOrEmpty(data.dataType());
            child.setDataType(dataType.isBlank() ? "string" : dataType);
        }
        child.setDescription(valueOrEmpty(data.description()));
        applyBooleanConstraint(child, "config", data.config());
        applyBooleanConstraint(child, "mandatory", data.mandatory());
        applyTextConstraint(child, "range", data.range());
        List<UiMessage> messages = applyConstraints(child, data.constraintsText());
        parent.addChild(child);
        return new AddChildResult(child, messages);
    }

    public Optional<YangNode> delete(YangNode node) {
        if (node == null || node.parent().isEmpty()) {
            return Optional.empty();
        }
        YangNode parent = node.parent().get();
        parent.removeChild(node);
        return Optional.of(parent);
    }

    public List<YangNodeType> addableTypes() {
        return List.of(
                YangNodeType.CONTAINER,
                YangNodeType.LIST,
                YangNodeType.LEAF,
                YangNodeType.LEAF_LIST,
                YangNodeType.CHOICE,
                YangNodeType.CASE,
                YangNodeType.GROUPING,
                YangNodeType.USES,
                YangNodeType.TYPEDEF,
                YangNodeType.IDENTITY,
                YangNodeType.FEATURE,
                YangNodeType.RPC,
                YangNodeType.ACTION,
                YangNodeType.INPUT,
                YangNodeType.OUTPUT,
                YangNodeType.NOTIFICATION,
                YangNodeType.AUGMENT,
                YangNodeType.ANYDATA,
                YangNodeType.ANYXML
        );
    }

    public String formatConstraints(Map<String, List<String>> constraints) {
        if (constraints.isEmpty()) {
            return "";
        }
        return constraints.entrySet().stream()
                .filter(entry -> !"config".equals(entry.getKey()))
                .filter(entry -> !"mandatory".equals(entry.getKey()))
                .filter(entry -> !"range".equals(entry.getKey()))
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private List<UiMessage> applyConstraints(YangNode node, String text) {
        if (text == null || text.isBlank() || "-".equals(text.strip())) {
            return List.of();
        }
        java.util.ArrayList<UiMessage> messages = new java.util.ArrayList<>();
        for (String line : text.split("\\R")) {
            String clean = line.strip();
            if (clean.isEmpty()) {
                continue;
            }
            int separator = clean.indexOf(':');
            if (separator < 1) {
                messages.add(new UiMessage("Ограничение пропущено, нужен формат `keyword: value`: " + clean, "", 0));
                continue;
            }
            String keyword = clean.substring(0, separator).strip();
            String value = clean.substring(separator + 1).strip();
            if ("config".equals(keyword) || "mandatory".equals(keyword)) {
                messages.add(new UiMessage("Оператор `" + keyword + "` задается галочкой и пропущен из текстового поля.", "", 0));
                continue;
            }
            if ("range".equals(keyword)) {
                messages.add(new UiMessage("Operator `range` is set by the separate Range field and skipped from the text constraints field.", "", 0));
                continue;
            }
            node.addConstraint(keyword, value);
        }
        return messages;
    }

    private void applyBooleanConstraint(YangNode node, String keyword, Boolean value) {
        if (value != null) {
            node.addConstraint(keyword, Boolean.toString(value));
        }
    }

    private void applyTextConstraint(YangNode node, String keyword, String value) {
        String clean = valueOrEmpty(value);
        if (!clean.isBlank()) {
            node.addConstraint(keyword, clean);
        }
    }

    private boolean needsDefaultDataType(YangNodeType type) {
        return type == YangNodeType.LEAF || type == YangNodeType.LEAF_LIST || type == YangNodeType.TYPEDEF;
    }

    private String defaultName(YangNodeType type, YangNode parent) {
        if (type == YangNodeType.INPUT || type == YangNodeType.OUTPUT) {
            return "";
        }
        if (type == YangNodeType.AUGMENT) {
            return "\"/target\"";
        }
        String prefix = type.name().toLowerCase().replace('_', '-');
        int index = parent.children().size() + 1;
        return prefix + "-" + index;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.strip();
    }

    public record NodeEditData(
            String name,
            String dataType,
            String description,
            String constraintsText,
            String range,
            Boolean config,
            Boolean mandatory
    ) {
    }

    public record AddChildResult(YangNode child, List<UiMessage> messages) {
    }
}
