package com.yangdesigner.yangmodeldesigner.ui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangCompletionSupportTest {
    private final YangCompletionSupport completionSupport = new YangCompletionSupport();

    @Test
    void suggestsYangKeywordsByPrefix() {
        assertTrue(completionSupport.suggestions("cont", 4, 10).contains("container"));
    }

    @Test
    void suggestsYangTypesByPrefix() {
        assertTrue(completionSupport.suggestions("type str", 8, 10).contains("string"));
    }

    @Test
    void findsPrefixStart() {
        assertEquals(5, completionSupport.prefixStart("type str", 8));
    }

    @Test
    void addsSpaceAfterKeywordCompletion() {
        assertEquals("container ", completionSupport.insertionText("container"));
        assertEquals("string", completionSupport.insertionText("string"));
    }
}
