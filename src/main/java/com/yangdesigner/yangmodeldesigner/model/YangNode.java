package com.yangdesigner.yangmodeldesigner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class YangNode {
    private final YangNodeType type;
    private String name;
    private int line;
    private YangNode parent;
    private String dataType = "";
    private String description = "";
    private YangStatement statement;
    private final Map<String, List<String>> constraints = new LinkedHashMap<>();
    private final List<YangNode> children = new ArrayList<>();

    public YangNode(YangNodeType type, String name) {
        this.type = Objects.requireNonNull(type);
        this.name = Objects.requireNonNullElse(name, "");
    }

    public YangNodeType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public int line() {
        return line;
    }

    public void setLine(int line) {
        this.line = Math.max(0, line);
    }

    public void setName(String name) {
        this.name = Objects.requireNonNullElse(name, "");
        statement().ifPresent(statement -> statement.setArgument(this.name));
    }

    public Optional<YangStatement> statement() {
        return Optional.ofNullable(statement);
    }

    public void setStatement(YangStatement statement) {
        this.statement = statement;
    }

    public Optional<YangNode> parent() {
        return Optional.ofNullable(parent);
    }

    public String dataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = Objects.requireNonNullElse(dataType, "");
        upsertStatementChild("type", this.dataType);
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNullElse(description, "");
        upsertStatementChild("description", this.description);
    }

    public Map<String, List<String>> constraints() {
        return Collections.unmodifiableMap(constraints);
    }

    public List<YangNode> children() {
        return Collections.unmodifiableList(children);
    }

    public void clearConstraints() {
        for (String keyword : constraints.keySet()) {
            statement().ifPresent(statement -> statement.removeChildren(keyword));
        }
        constraints.clear();
    }

    public void addConstraint(String keyword, String value) {
        constraints.computeIfAbsent(keyword, ignored -> new ArrayList<>())
                .add(Objects.requireNonNullElse(value, ""));
        statement().ifPresent(statement -> statement.addChildOrdered(new YangStatement(keyword, value, 0)));
    }

    public void addChild(YangNode child) {
        child.parent = this;
        children.add(child);
        if (statement != null) {
            if (child.statement == null) {
                child.statement = new YangStatement(child.type.keyword(), child.name, 0);
            }
            syncStatementFromState(child);
            statement.addChildOrdered(child.statement);
        }
    }

    public boolean removeChild(YangNode child) {
        boolean removed = children.remove(child);
        if (removed && statement != null && child.statement != null) {
            statement.removeChild(child.statement);
        }
        return removed;
    }

    public void addParsedChild(YangNode child) {
        child.parent = this;
        children.add(child);
    }

    public void addParsedConstraint(String keyword, String value) {
        constraints.computeIfAbsent(keyword, ignored -> new ArrayList<>())
                .add(Objects.requireNonNullElse(value, ""));
    }

    private void upsertStatementChild(String keyword, String value) {
        if (statement == null) {
            return;
        }
        statement.removeChildren(keyword);
        if (!value.isBlank()) {
            statement.addChildOrdered(new YangStatement(keyword, value, 0));
        }
    }

    private void syncStatementFromState(YangNode node) {
        if (node.statement == null) {
            return;
        }
        if (!node.dataType.isBlank()) {
            node.statement.addChildOrdered(new YangStatement("type", node.dataType, 0));
        }
        if (!node.description.isBlank()) {
            node.statement.addChildOrdered(new YangStatement("description", node.description, 0));
        }
        for (Map.Entry<String, List<String>> entry : node.constraints.entrySet()) {
            for (String value : entry.getValue()) {
                node.statement.addChildOrdered(new YangStatement(entry.getKey(), value, 0));
            }
        }
    }

    public String path() {
        List<String> names = new ArrayList<>();
        YangNode cursor = this;
        while (cursor != null) {
            names.add(cursor.name);
            cursor = cursor.parent;
        }
        Collections.reverse(names);
        return "/" + String.join("/", names);
    }

    @Override
    public String toString() {
        if (name.isBlank()) {
            return type.name().toLowerCase();
        }
        return type.name().toLowerCase().replace('_', '-') + " " + name;
    }
}
