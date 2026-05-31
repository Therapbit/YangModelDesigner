package com.yangdesigner.yangmodeldesigner.ui.editor;

import org.fxmisc.richtext.model.StyleSpans;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangSyntaxHighlighterTest {
    private final YangSyntaxHighlighter highlighter = new YangSyntaxHighlighter();

    @Test
    void marksKeywordsTypesAndMatchingBraces() {
        StyleSpans<Collection<String>> spans = highlighter.compute("leaf enabled { type boolean; }", 13);

        assertTrue(containsStyle(spans, "keyword"));
        assertTrue(containsStyle(spans, "type"));
        assertTrue(containsStyle(spans, "matching-brace"));
    }

    @Test
    void skipsMatchingBraceHighlightWhenCaretIsDisabled() {
        StyleSpans<Collection<String>> spans = highlighter.compute("leaf enabled { type boolean; }", -1);

        assertTrue(containsStyle(spans, "brace"));
        assertFalse(containsStyle(spans, "matching-brace"));
    }

    private boolean containsStyle(StyleSpans<Collection<String>> spans, String styleClass) {
        return spans.stream()
                .map(span -> span.getStyle())
                .anyMatch(styles -> styles.contains(styleClass));
    }
}
