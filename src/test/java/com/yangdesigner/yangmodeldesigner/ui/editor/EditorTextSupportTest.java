package com.yangdesigner.yangmodeldesigner.ui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorTextSupportTest {
    @Test
    void addsIndentToEachLine() {
        assertEquals("    leaf a;\n    leaf b;", EditorTextSupport.addIndent("leaf a;\nleaf b;"));
    }

    @Test
    void removesSpacesOrTabIndentFromEachLine() {
        assertEquals("leaf a;\nleaf b;", EditorTextSupport.removeIndent("    leaf a;\n\tleaf b;"));
    }

    @Test
    void enterAfterOpeningBraceAddsNestedIndent() {
        EditorTextSupport.IndentedNewLine newLine = EditorTextSupport.indentedNewLine("container system {", 18);

        assertEquals("\n    ", newLine.insertion());
        assertEquals(-1, newLine.caretPosition());
    }

    @Test
    void enterBetweenBracesCreatesMiddleLineAndKeepsClosingBraceAligned() {
        EditorTextSupport.IndentedNewLine newLine = EditorTextSupport.indentedNewLine("container system {}", 18);

        assertEquals("\n    \n", newLine.insertion());
        assertEquals(23, newLine.caretPosition());
    }
}
