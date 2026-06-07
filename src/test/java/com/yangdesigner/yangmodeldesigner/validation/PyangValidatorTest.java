package com.yangdesigner.yangmodeldesigner.validation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PyangValidatorTest {
    private final PyangValidator validator = new PyangValidator();

    @Test
    void parsesPyangErrorOutput() {
        List<ValidationIssue> issues = validator.parseIssues("module.yang:7: error: unexpected keyword \"foo\"");

        assertEquals(1, issues.size());
        assertEquals(ValidationIssue.Severity.ERROR, issues.getFirst().severity());
        assertEquals(7, issues.getFirst().line());
        assertTrue(issues.getFirst().message().contains("module.yang"));
        assertTrue(issues.getFirst().message().contains("unexpected keyword"));
    }

    @Test
    void parsesPyangWarningOutput() {
        List<ValidationIssue> issues = validator.parseIssues("module.yang:12: warning: imported module not used");

        assertEquals(1, issues.size());
        assertEquals(ValidationIssue.Severity.WARNING, issues.getFirst().severity());
        assertEquals(12, issues.getFirst().line());
        assertTrue(issues.getFirst().message().contains("module.yang"));
    }

    @Test
    void addsIetfFlagWhenEnabled() {
        List<String> args = validator.pyangArguments(List.of("pyang"), Path.of("models"), Path.of("models", "module.yang"), true);

        assertEquals(List.of("pyang", "--ietf", "-p", "models", Path.of("models", "module.yang").toString()), args);
    }
}
