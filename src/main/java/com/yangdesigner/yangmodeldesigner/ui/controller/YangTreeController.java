package com.yangdesigner.yangmodeldesigner.ui.controller;

import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.ui.state.YangTreeState;
import javafx.scene.control.TreeItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class YangTreeController {
    public TreeItem<YangNode> toTreeItem(YangNode node) {
        TreeItem<YangNode> item = new TreeItem<>(node);
        for (YangNode child : node.children()) {
            item.getChildren().add(toTreeItem(child));
        }
        return item;
    }

    public YangTreeState captureTreeState(TreeItem<YangNode> rootItem, YangNode selectedNode) {
        if (rootItem == null) {
            return YangTreeState.empty();
        }
        Set<String> expandedPaths = new HashSet<>();
        collectExpandedPaths(rootItem, expandedPaths);
        String selectedPath = selectedNode == null ? rootItem.getValue().path() : selectedNode.path();
        return new YangTreeState(expandedPaths, selectedPath);
    }

    public void restoreExpandedPaths(TreeItem<YangNode> item, Set<String> expandedPaths) {
        item.setExpanded(expandedPaths.contains(item.getValue().path()) || item.getParent() == null);
        for (TreeItem<YangNode> child : item.getChildren()) {
            restoreExpandedPaths(child, expandedPaths);
        }
    }

    public TreeItem<YangNode> findByPath(TreeItem<YangNode> item, String path) {
        if (item == null) {
            return null;
        }
        if (path.equals(item.getValue().path())) {
            return item;
        }
        for (TreeItem<YangNode> child : item.getChildren()) {
            TreeItem<YangNode> found = findByPath(child, path);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public void expandParents(TreeItem<YangNode> item) {
        TreeItem<YangNode> cursor = item;
        while (cursor != null) {
            cursor.setExpanded(true);
            cursor = cursor.getParent();
        }
    }

    public boolean isReadOnly(YangNode node) {
        YangNode cursor = node;
        while (cursor != null) {
            List<String> configValues = cursor.constraints().get("config");
            if (configValues != null && configValues.stream().anyMatch("false"::equalsIgnoreCase)) {
                return true;
            }
            cursor = cursor.parent().orElse(null);
        }
        return false;
    }

    public int offsetForLine(String text, int line) {
        int targetLine = Math.max(1, line);
        int currentLine = 1;
        for (int index = 0; index < text.length(); index++) {
            if (currentLine == targetLine) {
                return index;
            }
            if (text.charAt(index) == '\n') {
                currentLine++;
            }
        }
        return text.length();
    }

    private void collectExpandedPaths(TreeItem<YangNode> item, Set<String> expandedPaths) {
        if (item == null) {
            return;
        }
        if (item.isExpanded()) {
            expandedPaths.add(item.getValue().path());
        }
        for (TreeItem<YangNode> child : item.getChildren()) {
            collectExpandedPaths(child, expandedPaths);
        }
    }
}
