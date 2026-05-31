package com.yangdesigner.yangmodeldesigner.ui.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindReplaceControllerTest {
    private final FindReplaceController controller = new FindReplaceController();

    @Test
    void findsNextFromCaret() {
        var range = controller.findNext("leaf one\nleaf two", "leaf", true, 2, 4).orElseThrow();

        assertEquals(new FindReplaceController.TextRange(9, 13), range);
    }

    @Test
    void wrapsFindNextToBeginning() {
        var range = controller.findNext("leaf one\nleaf two", "leaf", true, 13, 13).orElseThrow();

        assertEquals(new FindReplaceController.TextRange(0, 4), range);
    }

    @Test
    void findsPreviousCaseInsensitive() {
        var range = controller.findPrevious("Leaf one\nleaf two", "leaf", false, 8).orElseThrow();

        assertEquals(new FindReplaceController.TextRange(0, 4), range);
    }

    @Test
    void replacesAllWithQuotedReplacement() {
        String updated = controller.replaceAll("a.b a.b", "a.b", "$x", true);

        assertEquals("$x $x", updated);
    }

    @Test
    void matchesSelectedTextCaseInsensitive() {
        assertTrue(controller.matches("Leaf", "leaf", false));
    }
}
