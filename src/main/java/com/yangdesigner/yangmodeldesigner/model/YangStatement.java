package com.yangdesigner.yangmodeldesigner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class YangStatement {
    private static final List<String> PREFERRED_ORDER = List.of(
            "yang-version", "namespace", "prefix", "belongs-to",
            "config", "type", "description", "when", "must",
            "key", "unique", "mandatory", "default", "units",
            "min-elements", "max-elements", "ordered-by", "presence",
            "base", "if-feature", "status", "reference", "organization",
            "contact", "revision-date", "value", "position"
    );
    private static final Comparator<YangStatement> STATEMENT_ORDER = Comparator
            .comparingInt((YangStatement statement) -> orderIndex(statement.keyword()))
            .thenComparing(YangStatement::keyword);

    private final String keyword;
    private String argument;
    private final int line;
    private YangStatement parent;
    private final List<YangStatement> children = new ArrayList<>();

    public YangStatement(String keyword, String argument, int line) {
        this.keyword = Objects.requireNonNullElse(keyword, "");
        this.argument = Objects.requireNonNullElse(argument, "");
        this.line = Math.max(0, line);
    }

    public String keyword() {
        return keyword;
    }

    public String argument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = Objects.requireNonNullElse(argument, "");
    }

    public int line() {
        return line;
    }

    public Optional<YangStatement> parent() {
        return Optional.ofNullable(parent);
    }

    public List<YangStatement> children() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(YangStatement child) {
        if (children.contains(child)) {
            return;
        }
        child.parent = this;
        children.add(child);
    }

    public void addChildOrdered(YangStatement child) {
        addChild(child);
        children.sort(STATEMENT_ORDER);
    }

    public boolean removeChild(YangStatement child) {
        boolean removed = children.remove(child);
        if (removed) {
            child.parent = null;
        }
        return removed;
    }

    public void removeChildren(String keyword) {
        children.removeIf(child -> keyword.equals(child.keyword()));
    }

    private static int orderIndex(String keyword) {
        int index = PREFERRED_ORDER.indexOf(keyword);
        if (index >= 0) {
            return index;
        }
        YangNodeType type = YangNodeType.fromKeyword(keyword);
        if (type != YangNodeType.UNKNOWN) {
            return 10_000;
        }
        return 9_000;
    }
}
