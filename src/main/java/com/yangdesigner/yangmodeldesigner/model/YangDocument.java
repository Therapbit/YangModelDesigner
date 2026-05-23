package com.yangdesigner.yangmodeldesigner.model;

import java.nio.file.Path;
import java.util.Optional;

public final class YangDocument {
    private final YangNode root;
    private final YangStatement statementRoot;
    private final String source;
    private final Path file;

    public YangDocument(YangNode root, String source, Path file) {
        this(root, root.statement().orElse(null), source, file);
    }

    public YangDocument(YangNode root, YangStatement statementRoot, String source, Path file) {
        this.root = root;
        this.statementRoot = statementRoot;
        this.source = source == null ? "" : source;
        this.file = file;
    }

    public YangNode root() {
        return root;
    }

    public Optional<YangStatement> statementRoot() {
        return Optional.ofNullable(statementRoot);
    }

    public String source() {
        return source;
    }

    public Optional<Path> file() {
        return Optional.ofNullable(file);
    }
}
