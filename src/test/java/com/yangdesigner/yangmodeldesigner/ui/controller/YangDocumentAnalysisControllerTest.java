package com.yangdesigner.yangmodeldesigner.ui.controller;

import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.service.YangDocumentService;
import com.yangdesigner.yangmodeldesigner.validation.PyangValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangDocumentAnalysisControllerTest {
    private final YangDocumentAnalysisController controller = new YangDocumentAnalysisController(
            new YangDocumentService(),
            new PyangValidator()
    );

    @Test
    void parseReturnsDocumentAndNoMessagesForValidSource() {
        YangDocumentAnalysisController.AnalysisResult result = controller.parse("""
                module example {
                  yang-version 1.1;
                  namespace "urn:example";
                  prefix ex;
                }
                """, null);

        assertEquals(YangNodeType.MODULE, result.document().root().type());
        assertTrue(result.messages().isEmpty());
    }

    @Test
    void parseReturnsMessagesForParseErrors() {
        YangDocumentAnalysisController.AnalysisResult result = controller.parse("""
                module example {
                  container system {
                """, null);

        assertFalse(result.messages().isEmpty());
    }
}
