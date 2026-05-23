package com.yangdesigner.yangmodeldesigner.validation;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class YangValidator {
    public List<ValidationIssue> validate(YangDocument document) {
        List<ValidationIssue> issues = new ArrayList<>();
        YangNode root = document.root();
        if (root.type() != YangNodeType.MODULE && root.type() != YangNodeType.SUBMODULE) {
            issues.add(error(root, "Документ должен начинаться с module или submodule согласно YANG 1.1 RFC 7950."));
        }
        if (root.type() == YangNodeType.MODULE) {
            requireConstraint(root, "yang-version", issues, "Для YANG 1.1 укажите `yang-version 1.1;`.");
            requireConstraintValue(root, "yang-version", "1.1", issues, "Поддерживается только YANG 1.1: `yang-version 1.1;`.");
            requireConstraint(root, "namespace", issues, "Для module обязательно поле namespace.");
            requireConstraint(root, "prefix", issues, "Для module обязательно поле prefix.");
        }
        validateNode(root, issues);
        return issues;
    }

    private void validateNode(YangNode node, List<ValidationIssue> issues) {
        Set<String> childNames = new HashSet<>();
        for (YangNode child : node.children()) {
            String key = child.name();
            if (!childNames.add(key)) {
                issues.add(error(child, "Повторяющийся дочерний узел `" + child.name() + "` в " + node.path() + "."));
            }
            if ((child.type() == YangNodeType.LEAF || child.type() == YangNodeType.LEAF_LIST)
                    && child.dataType().isBlank()) {
                issues.add(error(child, "Для " + child.path() + " не указан type."));
            }
            if (child.type() == YangNodeType.LIST) {
                validateList(child, issues);
            }
            if (child.type() == YangNodeType.LEAF_LIST) {
                validateLeafList(child, issues);
            }
            validateNode(child, issues);
        }
    }

    private void validateList(YangNode list, List<ValidationIssue> issues) {
        List<String> keys = list.constraints().getOrDefault("key", List.of());
        if (keys.isEmpty()) {
            issues.add(error(list, "Для list " + list.path() + " нужно указать key."));
            return;
        }
        Set<String> leafNames = list.children().stream()
                .filter(child -> child.type() == YangNodeType.LEAF)
                .map(YangNode::name)
                .collect(Collectors.toSet());
        for (String keyExpression : keys) {
            for (String keyName : keyExpression.split("\\s+")) {
                if (!keyName.isBlank() && !leafNames.contains(keyName)) {
                    issues.add(error(list, "Ключ `" + keyName + "` в " + list.path() + " должен ссылаться на дочерний leaf."));
                }
            }
        }
    }

    private void validateLeafList(YangNode leafList, List<ValidationIssue> issues) {
        if (leafList.constraints().containsKey("default") && leafList.constraints().containsKey("min-elements")) {
            issues.add(error(leafList, "leaf-list не должен одновременно иметь default и min-elements."));
        }
    }

    private void requireConstraint(YangNode node, String keyword, List<ValidationIssue> issues, String message) {
        if (!node.constraints().containsKey(keyword)) {
            issues.add(error(node, message));
        }
    }

    private void requireConstraintValue(YangNode node, String keyword, String expected, List<ValidationIssue> issues, String message) {
        List<String> values = node.constraints().get(keyword);
        if (values == null) {
            return;
        }
        boolean hasExpected = values.stream().anyMatch(expected::equals);
        if (!hasExpected) {
            issues.add(error(node, message + " Сейчас: " + formatValues(values) + "."));
        }
    }

    private String formatValues(List<String> values) {
        return values.stream().map(value -> "`" + value + "`").collect(Collectors.joining(", "));
    }

    private ValidationIssue error(YangNode node, String message) {
        String prefix = node.line() > 0 ? "Строка " + node.line() + ": " : "";
        return new ValidationIssue(ValidationIssue.Severity.ERROR, prefix + message, node.line(), node.path());
    }
}
