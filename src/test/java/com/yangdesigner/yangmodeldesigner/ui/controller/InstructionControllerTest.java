package com.yangdesigner.yangmodeldesigner.ui.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionControllerTest {
    @Test
    void readsInstructionFromProjectRoot() {
        InstructionController controller = new InstructionController();

        String text = controller.readInstructionText(InstructionControllerTest.class);

        assertTrue(text.contains("YANG"));
    }
}
