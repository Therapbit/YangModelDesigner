package com.yangdesigner.yangmodeldesigner.ui.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class InstructionController {
    public String readInstructionText(Class<?> applicationClass) {
        for (Path path : instructionCandidates(applicationClass)) {
            if (Files.isRegularFile(path)) {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    return "Не удалось прочитать файл инструкции: " + path;
                }
            }
        }
        return "Файл инструкции YangDesignerInstruction.md не найден.";
    }

    private List<Path> instructionCandidates(Class<?> applicationClass) {
        return List.of(
                Path.of("YangDesignerInstruction.md"),
                applicationDirectory(applicationClass).map(path -> path.resolve("YangDesignerInstruction.md")).orElse(Path.of(""))
        );
    }

    private Optional<Path> applicationDirectory(Class<?> applicationClass) {
        try {
            Path location = Path.of(applicationClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Optional.of(Files.isDirectory(location) ? location : location.getParent());
        } catch (URISyntaxException | RuntimeException ex) {
            return Optional.empty();
        }
    }
}
