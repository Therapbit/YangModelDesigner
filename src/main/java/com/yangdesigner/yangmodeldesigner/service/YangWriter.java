package com.yangdesigner.yangmodeldesigner.service;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.model.YangStatement;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class YangWriter {
    private static final Set<String> NODE_STATEMENTS = Set.of(
            "module", "submodule", "container", "list", "leaf", "leaf-list",
            "choice", "case", "grouping", "uses", "augment", "rpc", "action",
            "input", "output", "notification", "deviation", "deviate",
            "anydata", "anyxml", "typedef", "identity", "feature", "extension",
            "enum", "bit"
    );
    private static final Set<String> QUOTED_STATEMENTS = Set.of(
            "namespace", "description", "reference", "organization", "contact",
            "when", "must", "pattern", "presence", "error-message", "units",
            "revision-date", "augment", "deviation"
    );

    public String write(YangDocument document) {
        if (document.statementRoot().isPresent()) {
            StringBuilder builder = new StringBuilder();
            writeStatement(builder, document.statementRoot().get(), 0);
            return builder.toString();
        }
        StringBuilder builder = new StringBuilder();
        writeNode(builder, document.root(), 0);
        return builder.toString();
    }

    private void writeStatement(StringBuilder builder, YangStatement statement, int indent) {
        if (statement.children().isEmpty()) {
            line(builder, indent, statement.keyword() + statementArgument(statement) + ";");
            return;
        }
        line(builder, indent, statement.keyword() + statementArgument(statement) + " {");
        for (int index = 0; index < statement.children().size(); index++) {
            YangStatement child = statement.children().get(index);
            if (index > 0 && shouldSeparateStatements(child)) {
                blankLine(builder);
            }
            writeStatement(builder, child, indent + 1);
        }
        line(builder, indent, "}");
    }

    private boolean shouldSeparateStatements(YangStatement current) {
        return isNodeStatement(current.keyword());
    }

    private boolean isNodeStatement(String keyword) {
        return NODE_STATEMENTS.contains(keyword);
    }

    private String statementArgument(YangStatement statement) {
        if (statement.argument().isBlank()) {
            return "";
        }
        return " " + formatValue(statement.keyword(), statement.argument());
    }

    private void writeNode(StringBuilder builder, YangNode node, int indent) {
        String statementStart = node.name().isBlank()
                ? keyword(node.type()) + " {"
                : keyword(node.type()) + " " + node.name() + " {";
        line(builder, indent, statementStart);
        if (node.type() == YangNodeType.MODULE || node.type() == YangNodeType.SUBMODULE) {
            writeConstraints(builder, node, indent + 1, List.of("yang-version", "namespace", "prefix"));
            blankLine(builder);
        }
        writeConstraints(builder, node, indent + 1, List.of("config"));
        if (!node.dataType().isBlank()) {
            statement(builder, indent + 1, "type", node.dataType());
        }
        if (!node.description().isBlank()) {
            statement(builder, indent + 1, "description", node.description());
        }
        writeConstraints(builder, node, indent + 1, List.of(
                "when", "must", "key", "unique", "mandatory", "default",
                "presence", "min-elements", "max-elements", "pattern", "range",
                "length", "units", "base", "if-feature", "status", "reference",
                "organization", "contact", "belongs-to", "revision-date",
                "prefix", "value", "position"
        ));
        if (!node.children().isEmpty()) {
            if (!node.dataType().isBlank() || hasNonHeaderConstraints(node)) {
                blankLine(builder);
            }
            for (int index = 0; index < node.children().size(); index++) {
                writeNode(builder, node.children().get(index), indent + 1);
                if (index + 1 < node.children().size()) {
                    blankLine(builder);
                }
            }
        }
        line(builder, indent, "}");
    }

    private void writeConstraints(StringBuilder builder, YangNode node, int indent, List<String> keywords) {
        for (String keyword : keywords) {
            List<String> values = node.constraints().get(keyword);
            if (values == null) {
                continue;
            }
            for (String value : values) {
                statement(builder, indent, keyword, value);
            }
        }
    }

    private boolean hasNonHeaderConstraints(YangNode node) {
        return node.constraints().entrySet().stream()
                .filter(entry -> !"yang-version".equals(entry.getKey()))
                .filter(entry -> !"namespace".equals(entry.getKey()))
                .filter(entry -> !"prefix".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .anyMatch(values -> !values.isEmpty());
    }

    private void statement(StringBuilder builder, int indent, String keyword, String value) {
        line(builder, indent, keyword + " " + formatValue(keyword, value) + ";");
    }

    private String formatValue(String keyword, String value) {
        String clean = value == null ? "" : value.strip();
        if (clean.isEmpty()) {
            return "\"\"";
        }
        if (QUOTED_STATEMENTS.contains(keyword) || clean.contains(" ")) {
            return "\"" + clean.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return clean;
    }

    private String keyword(YangNodeType type) {
        return type.name().toLowerCase().replace('_', '-');
    }

    private void line(StringBuilder builder, int indent, String text) {
        builder.append("  ".repeat(indent)).append(text).append(System.lineSeparator());
    }

    private void blankLine(StringBuilder builder) {
        builder.append(System.lineSeparator());
    }
}
