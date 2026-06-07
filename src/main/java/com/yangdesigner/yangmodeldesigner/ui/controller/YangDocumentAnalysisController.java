package com.yangdesigner.yangmodeldesigner.ui.controller;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.parser.YangParseResult;
import com.yangdesigner.yangmodeldesigner.service.YangDocumentService;
import com.yangdesigner.yangmodeldesigner.ui.state.UiMessage;
import com.yangdesigner.yangmodeldesigner.validation.PyangValidator;
import com.yangdesigner.yangmodeldesigner.validation.ValidationIssue;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class YangDocumentAnalysisController {
    private final YangDocumentService documentService;
    private final PyangValidator validator;

    public YangDocumentAnalysisController(YangDocumentService documentService, PyangValidator validator) {
        this.documentService = documentService;
        this.validator = validator;
    }

    public AnalysisResult parse(String text, Path file) {
        YangParseResult result = documentService.parse(text, file);
        return new AnalysisResult(result.document(), parseMessages(result));
    }

    public AnalysisResult validate(String text, Path file) {
        return validate(text, file, false);
    }

    public AnalysisResult validate(String text, Path file, boolean ietfMode) {
        YangParseResult result = documentService.parse(text, file);
        List<UiMessage> messages = validator.validate(text, file, ietfMode).stream()
                .map(this::formatIssue)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        messages.addAll(0, parseMessages(result));
        if (messages.isEmpty()) {
            messages.add(new UiMessage("Ошибок не найдено.", "", 0));
        }
        return new AnalysisResult(result.document(), messages);
    }

    private List<UiMessage> parseMessages(YangParseResult result) {
        return result.errors().stream()
                .map(error -> new UiMessage(error, "", 0))
                .toList();
    }

    private UiMessage formatIssue(ValidationIssue issue) {
        return new UiMessage(issue.severity() + ": " + issue.message(), issue.path(), issue.line());
    }

    public record AnalysisResult(YangDocument document, List<UiMessage> messages) {
    }
}
