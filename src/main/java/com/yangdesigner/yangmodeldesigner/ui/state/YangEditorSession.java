package com.yangdesigner.yangmodeldesigner.ui.state;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class YangEditorSession {
    private static final int MAX_EDIT_HISTORY = 200;

    private final String untitledName;
    private Path file;
    private String text;
    private boolean dirty;
    private YangTreeState treeState = YangTreeState.empty();
    private final Deque<String> undoHistory = new ArrayDeque<>();
    private final Deque<String> redoHistory = new ArrayDeque<>();

    public YangEditorSession(String untitledName, Path file, String text, boolean dirty) {
        this.untitledName = untitledName;
        this.file = file;
        this.text = text;
        this.dirty = dirty;
    }

    public Path file() {
        return file;
    }

    public void setFile(Path file) {
        this.file = file;
    }

    public String text() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public YangTreeState treeState() {
        return treeState;
    }

    public void setTreeState(YangTreeState treeState) {
        this.treeState = treeState == null ? YangTreeState.empty() : treeState;
    }

    public void recordEdit(String previousText) {
        if (previousText == null || previousText.equals(undoHistory.peek())) {
            return;
        }
        undoHistory.push(previousText);
        trimHistory(undoHistory);
        redoHistory.clear();
    }

    public Optional<String> undo(String currentText) {
        if (undoHistory.isEmpty()) {
            return Optional.empty();
        }
        redoHistory.push(currentText == null ? "" : currentText);
        trimHistory(redoHistory);
        return Optional.of(undoHistory.pop());
    }

    public Optional<String> redo(String currentText) {
        if (redoHistory.isEmpty()) {
            return Optional.empty();
        }
        undoHistory.push(currentText == null ? "" : currentText);
        trimHistory(undoHistory);
        return Optional.of(redoHistory.pop());
    }

    public void clearEditHistory() {
        undoHistory.clear();
        redoHistory.clear();
    }

    public String displayName() {
        return file == null ? untitledName : file.getFileName().toString();
    }

    private void trimHistory(Deque<String> history) {
        while (history.size() > MAX_EDIT_HISTORY) {
            history.removeLast();
        }
    }
}
