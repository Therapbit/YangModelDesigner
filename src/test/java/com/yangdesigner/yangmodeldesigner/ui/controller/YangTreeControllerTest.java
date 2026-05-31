package com.yangdesigner.yangmodeldesigner.ui.controller;

import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YangTreeControllerTest {
    private final YangTreeController controller = new YangTreeController();

    @Test
    void buildsTreeItemsAndFindsNodeByPath() {
        YangNode root = new YangNode(YangNodeType.MODULE, "example");
        YangNode container = new YangNode(YangNodeType.CONTAINER, "system");
        YangNode leaf = new YangNode(YangNodeType.LEAF, "enabled");
        root.addChild(container);
        container.addChild(leaf);

        TreeItem<YangNode> rootItem = controller.toTreeItem(root);

        assertSame(leaf, controller.findByPath(rootItem, "/example/system/enabled").getValue());
    }

    @Test
    void detectsReadOnlyInheritedFromParentConfigFalse() {
        YangNode container = new YangNode(YangNodeType.CONTAINER, "state");
        container.addConstraint("config", "false");
        YangNode leaf = new YangNode(YangNodeType.LEAF, "uptime");
        container.addChild(leaf);

        assertTrue(controller.isReadOnly(leaf));
        assertFalse(controller.isReadOnly(new YangNode(YangNodeType.LEAF, "hostname")));
    }

    @Test
    void capturesExpandedPathsAndSelectedPath() {
        YangNode root = new YangNode(YangNodeType.MODULE, "example");
        YangNode container = new YangNode(YangNodeType.CONTAINER, "system");
        root.addChild(container);
        TreeItem<YangNode> rootItem = controller.toTreeItem(root);
        TreeItem<YangNode> childItem = rootItem.getChildren().getFirst();
        rootItem.setExpanded(true);
        childItem.setExpanded(true);

        var state = controller.captureTreeState(rootItem, container);

        assertTrue(state.expandedPaths().contains("/example"));
        assertTrue(state.expandedPaths().contains("/example/system"));
        assertEquals("/example/system", state.selectedPath());
    }

    @Test
    void computesOffsetForOneBasedLineNumber() {
        assertEquals(0, controller.offsetForLine("a\nbb\nccc", 1));
        assertEquals(2, controller.offsetForLine("a\nbb\nccc", 2));
        assertEquals(5, controller.offsetForLine("a\nbb\nccc", 3));
    }
}
