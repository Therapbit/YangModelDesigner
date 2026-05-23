package com.yangdesigner.yangmodeldesigner.validation;

public record ValidationIssue(Severity severity, String message, int line, String path) {
    public ValidationIssue(Severity severity, String message) {
        this(severity, message, 0, "");
    }

    public enum Severity {
        ERROR,
        WARNING
    }
}
