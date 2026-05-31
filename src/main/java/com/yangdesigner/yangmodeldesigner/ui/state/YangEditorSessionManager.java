package com.yangdesigner.yangmodeldesigner.ui.state;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class YangEditorSessionManager {
    private final List<YangEditorSession> sessions = new ArrayList<>();
    private int untitledCounter = 1;
    private YangEditorSession currentSession;

    public List<YangEditorSession> sessions() {
        return Collections.unmodifiableList(sessions);
    }

    public int size() {
        return sessions.size();
    }

    public YangEditorSession currentSession() {
        return currentSession;
    }

    public YangEditorSession createUntitled(String text) {
        YangEditorSession session = new YangEditorSession("Новая модель " + untitledCounter++, null, text, false);
        add(session);
        return session;
    }

    public YangEditorSession open(Path file, String text) {
        YangEditorSession session = new YangEditorSession(file.getFileName().toString(), file, text, false);
        add(session);
        return session;
    }

    public void add(YangEditorSession session) {
        if (sessions.contains(session)) {
            return;
        }
        sessions.add(session);
    }

    public void remove(YangEditorSession session) {
        sessions.remove(session);
        if (currentSession == session) {
            currentSession = null;
        }
    }

    public void select(YangEditorSession session) {
        currentSession = session;
    }

    public YangEditorSession find(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        return sessions.stream()
                .filter(session -> session.file() != null)
                .filter(session -> session.file().toAbsolutePath().normalize().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    public void syncCurrent(Path file, String text, boolean dirty, YangTreeState treeState) {
        if (currentSession == null) {
            return;
        }
        currentSession.setFile(file);
        currentSession.setText(text);
        currentSession.setDirty(dirty);
        currentSession.setTreeState(treeState);
    }
}
