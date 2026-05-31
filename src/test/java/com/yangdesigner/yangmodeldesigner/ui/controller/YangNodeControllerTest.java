package com.yangdesigner.yangmodeldesigner.ui.controller;

import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangNodeControllerTest {
    private final YangNodeController controller = new YangNodeController();

    @Test
    void appliesNodeChangesAndBooleanConstraints() {
        YangNode leaf = new YangNode(YangNodeType.LEAF, "old");

        var messages = controller.applyNodeChanges(leaf, new YangNodeController.NodeEditData(
                "enabled",
                "boolean",
                "Admin state",
                "default: true",
                "1..10",
                true,
                false
        ));

        assertTrue(messages.isEmpty());
        assertEquals("enabled", leaf.name());
        assertEquals("boolean", leaf.dataType());
        assertEquals("Admin state", leaf.description());
        assertEquals("true", leaf.constraints().get("config").getFirst());
        assertEquals("false", leaf.constraints().get("mandatory").getFirst());
        assertEquals("true", leaf.constraints().get("default").getFirst());
        assertEquals("1..10", leaf.constraints().get("range").getFirst());
    }

    @Test
    void skipsBooleanConstraintsFromTextFieldAndReportsMessage() {
        YangNode leaf = new YangNode(YangNodeType.LEAF, "enabled");

        var messages = controller.applyNodeChanges(leaf, new YangNodeController.NodeEditData(
                "enabled",
                "boolean",
                "",
                "config: false\nmandatory: true\nwhen: ../mode",
                "",
                null,
                null
        ));

        assertEquals(2, messages.size());
        assertFalse(leaf.constraints().containsKey("config"));
        assertFalse(leaf.constraints().containsKey("mandatory"));
        assertEquals("../mode", leaf.constraints().get("when").getFirst());
    }

    @Test
    void addsChildWithDefaultLeafTypeAndName() {
        YangNode parent = new YangNode(YangNodeType.CONTAINER, "system");

        YangNodeController.AddChildResult result = controller.addChild(parent, YangNodeType.LEAF, new YangNodeController.NodeEditData(
                "",
                "",
                "",
                "",
                "",
                null,
                null
        ));

        assertTrue(result.messages().isEmpty());
        assertSame(result.child(), parent.children().getFirst());
        assertEquals("leaf-1", result.child().name());
        assertEquals("string", result.child().dataType());
    }

    @Test
    void deletesChildAndReturnsParent() {
        YangNode parent = new YangNode(YangNodeType.CONTAINER, "system");
        YangNode child = new YangNode(YangNodeType.LEAF, "enabled");
        parent.addChild(child);

        assertSame(parent, controller.delete(child).orElseThrow());
        assertTrue(parent.children().isEmpty());
    }
}
