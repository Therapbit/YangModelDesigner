package com.yangdesigner.yangmodeldesigner.service;

import com.yangdesigner.yangmodeldesigner.parser.YangParseResult;
import com.yangdesigner.yangmodeldesigner.parser.YangParser;
import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class YangDocumentService {
    private final YangParser parser = new YangParser();
    private final YangWriter writer = new YangWriter();

    public String newModuleTemplate() {
        return """
                module example-module {
                    yang-version 1.1;
                    namespace "urn:example:module";
                    prefix ex;

                    container system {
                        description "System settings.";

                        leaf hostname {
                            type string;
                            description "Device host name.";
                        }
                    }
                }
                """;
    }

    public YangParseResult parse(String source, Path file) {
        return parser.parse(source, file);
    }

    public String writeToText(YangDocument document) {
        return writer.write(document);
    }

    public String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public void write(Path file, String source) throws IOException {
        Files.writeString(file, source, StandardCharsets.UTF_8);
    }

    public List<YangDocument> readRelatedDocuments(YangDocument document, Path currentFile) throws IOException {
        Path sourceFile = currentFile != null ? currentFile : document.file().orElse(null);
        if (sourceFile == null || sourceFile.getParent() == null) {
            return List.of();
        }
        Path directory = sourceFile.getParent();
        Set<Path> visited = new HashSet<>();
        visited.add(sourceFile.toAbsolutePath().normalize());
        List<YangDocument> relatedDocuments = new ArrayList<>();
        readRelatedDocuments(document, directory, visited, relatedDocuments);
        return relatedDocuments;
    }

    private void readRelatedDocuments(YangDocument document, Path directory, Set<Path> visited, List<YangDocument> relatedDocuments) throws IOException {
        for (YangNode dependency : dependencies(document.root())) {
            Optional<Path> dependencyFile = dependencyFile(directory, dependency.name());
            if (dependencyFile.isEmpty()) {
                continue;
            }
            Path normalized = dependencyFile.get().toAbsolutePath().normalize();
            if (!visited.add(normalized)) {
                continue;
            }
            YangDocument related = parse(read(normalized), normalized).document();
            relatedDocuments.add(related);
            readRelatedDocuments(related, directory, visited, relatedDocuments);
        }
    }

    private List<YangNode> dependencies(YangNode root) {
        return root.children().stream()
                .filter(child -> child.type() == YangNodeType.IMPORT || child.type() == YangNodeType.INCLUDE)
                .filter(child -> !child.name().isBlank())
                .toList();
    }

    private Optional<Path> dependencyFile(Path directory, String moduleName) throws IOException {
        String cleanName = cleanIdentifier(moduleName);
        Path exact = directory.resolve(cleanName + ".yang");
        if (Files.isRegularFile(exact)) {
            return Optional.of(exact);
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.equals(cleanName + ".yang")
                                || fileName.startsWith(cleanName + "@") && fileName.endsWith(".yang");
                    })
                    .findFirst();
        }
    }

    private String cleanIdentifier(String value) {
        String clean = value == null ? "" : value.strip();
        if ((clean.startsWith("\"") && clean.endsWith("\""))
                || (clean.startsWith("'") && clean.endsWith("'"))) {
            return clean.substring(1, clean.length() - 1);
        }
        return clean;
    }
}
