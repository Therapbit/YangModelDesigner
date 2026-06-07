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

    @Test
    void commentsCurrentLineAfterIndent() {
        EditorTextSupport.CommentToggle toggle = EditorTextSupport.toggleLineComment("leaf a;\n    leaf b;", 11, 11);

        assertEquals(8, toggle.start());
        assertEquals(19, toggle.end());
        assertEquals("    //leaf b;", toggle.replacement());
    }

    @Test
    void uncommentsAlreadyCommentedLine() {
        EditorTextSupport.CommentToggle toggle = EditorTextSupport.toggleLineComment("    //leaf b;", 4, 4);

        assertEquals("    leaf b;", toggle.replacement());
    }

    @Test
    void commentsSelectedLines() {
        String text = "leaf a;\nleaf b;\nleaf c;";
        EditorTextSupport.CommentToggle toggle = EditorTextSupport.toggleLineComment(text, 0, 14);

        assertEquals("//leaf a;\n//leaf b;", toggle.replacement());
    }

    @Test
    void uncommentsSelectedLinesWhenAllNonBlankLinesAreCommented() {
        String text = "//leaf a;\n//leaf b;\nleaf c;";
        EditorTextSupport.CommentToggle toggle = EditorTextSupport.toggleLineComment(text, 0, 18);

        assertEquals("leaf a;\nleaf b;", toggle.replacement());
    }

    @Test
    void insertsClosingBraceAndKeepsCaretBetweenBraces() {
        EditorTextSupport.BracePair bracePair = EditorTextSupport.bracePair("container system ", 17, 17);

        assertEquals(17, bracePair.start());
        assertEquals(17, bracePair.end());
        assertEquals("{}", bracePair.replacement());
        assertEquals(18, bracePair.caretPosition());
    }

    @Test
    void wrapsSelectedTextWithBraces() {
        EditorTextSupport.BracePair bracePair = EditorTextSupport.bracePair("leaf name", 0, 4);

        assertEquals(0, bracePair.start());
        assertEquals(4, bracePair.end());
        assertEquals("{leaf}", bracePair.replacement());
        assertEquals(1, bracePair.caretPosition());
        assertEquals(5, bracePair.selectionEnd());
    }
}
