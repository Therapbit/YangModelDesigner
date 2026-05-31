package com.yangdesigner.yangmodeldesigner.ui.state;

import java.nio.file.Path;

public final class YangEditorSession {
    private final String untitledName;
    private Path file;
    private String text;
    private boolean dirty;
    private YangTreeState treeState = YangTreeState.empty();

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

    public String displayName() {
        return file == null ? untitledName : file.getFileName().toString();
    }
}
