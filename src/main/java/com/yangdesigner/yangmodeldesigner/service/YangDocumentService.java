package com.yangdesigner.yangmodeldesigner.service;

import com.yangdesigner.yangmodeldesigner.parser.YangParseResult;
import com.yangdesigner.yangmodeldesigner.parser.YangParser;
import com.yangdesigner.yangmodeldesigner.model.YangDocument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
