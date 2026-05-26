package com.yangdesigner.yangmodeldesigner.validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PyangValidator {
    private static final Pattern ISSUE_PATTERN = Pattern.compile(
            "^(?<file>.*?):(?<line>\\d+)(?::(?<column>\\d+))?:\\s*(?<severity>error|warning):\\s*(?<message>.*)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final List<List<String>> COMMANDS = List.of(
            List.of("pyang"),
            List.of("python", "-m", "pyang"),
            List.of("python3", "-m", "pyang"),
            List.of("py", "-m", "pyang")
    );

    public List<ValidationIssue> validate(String source, Path currentFile) {
        try {
            Optional<List<String>> command = availableCommand();
            if (command.isEmpty()) {
                return List.of(new ValidationIssue(
                        ValidationIssue.Severity.ERROR,
                        "pyang не найден. Установите его командой: python -m pip install pyang"
                ));
            }
            return runPyang(command.get(), source, currentFile);
        } catch (IOException ex) {
            return List.of(new ValidationIssue(
                    ValidationIssue.Severity.ERROR,
                    "Не удалось запустить pyang: " + ex.getMessage()
            ));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return List.of(new ValidationIssue(
                    ValidationIssue.Severity.ERROR,
                    "Проверка pyang была прервана."
            ));
        }
    }

    private Optional<List<String>> availableCommand() throws IOException, InterruptedException {
        for (List<String> command : COMMANDS) {
            List<String> probe = new ArrayList<>(command);
            probe.add("--version");
            try {
                ProcessResult result = execute(probe, null);
                if (result.exitCode() == 0) {
                    return Optional.of(command);
                }
            } catch (IOException ignored) {
                // Try the next common launcher name.
            }
        }
        return Optional.empty();
    }

    private List<ValidationIssue> runPyang(List<String> command, String source, Path currentFile)
            throws IOException, InterruptedException {
        Path tempFile = temporaryYangFile(source, currentFile);
        try {
            List<String> args = new ArrayList<>(command);
            Path importDirectory = importDirectory(currentFile, tempFile);
            args.add("-p");
            args.add(importDirectory.toString());
            args.add(tempFile.toString());
            ProcessResult result = execute(args, importDirectory);
            List<ValidationIssue> issues = parseIssues(result.output());
            if (issues.isEmpty() && result.exitCode() != 0) {
                issues.add(new ValidationIssue(
                        ValidationIssue.Severity.ERROR,
                        result.output().isBlank() ? "pyang завершился с кодом " + result.exitCode() : result.output().strip()
                ));
            }
            return issues;
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private Path temporaryYangFile(String source, Path currentFile) throws IOException {
        Path directory = currentFile != null && currentFile.getParent() != null
                ? currentFile.getParent()
                : Path.of(System.getProperty("java.io.tmpdir"));
        String fileName = currentFile == null ? "yang-model-designer-" : currentFile.getFileName().toString() + "-";
        Path tempFile = Files.createTempFile(directory, fileName, ".yang");
        Files.writeString(tempFile, source, StandardCharsets.UTF_8);
        return tempFile;
    }

    private Path importDirectory(Path currentFile, Path tempFile) {
        if (currentFile != null && currentFile.getParent() != null) {
            return currentFile.getParent();
        }
        Path parent = tempFile.getParent();
        return parent == null ? Path.of(".") : parent;
    }

    private ProcessResult execute(List<String> command, Path directory) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (directory != null) {
            builder.directory(directory.toFile());
        }
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, output);
    }

    List<ValidationIssue> parseIssues(String output) {
        List<ValidationIssue> issues = new ArrayList<>();
        for (String line : output.split("\\R")) {
            String clean = line.strip();
            if (clean.isEmpty()) {
                continue;
            }
            Matcher matcher = ISSUE_PATTERN.matcher(clean);
            if (!matcher.matches()) {
                issues.add(new ValidationIssue(ValidationIssue.Severity.ERROR, clean));
                continue;
            }
            ValidationIssue.Severity severity = "warning".equals(matcher.group("severity").toLowerCase(Locale.ROOT))
                    ? ValidationIssue.Severity.WARNING
                    : ValidationIssue.Severity.ERROR;
            int lineNumber = Integer.parseInt(matcher.group("line"));
            issues.add(new ValidationIssue(severity, "Строка " + lineNumber + ": " + matcher.group("message"), lineNumber, ""));
        }
        return issues;
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
