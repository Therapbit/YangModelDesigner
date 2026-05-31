package com.yangdesigner.yangmodeldesigner.ui.state;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class YangEditorSessionManagerTest {
    @Test
    void createsSequentialUntitledSessions() {
        YangEditorSessionManager manager = new YangEditorSessionManager();

        YangEditorSession first = manager.createUntitled("module first {}");
        YangEditorSession second = manager.createUntitled("module second {}");

        assertEquals("Новая модель 1", first.displayName());
        assertEquals("Новая модель 2", second.displayName());
        assertEquals(2, manager.size());
    }

    @Test
    void findsOpenedSessionByNormalizedPath() {
        YangEditorSessionManager manager = new YangEditorSessionManager();
        Path file = Path.of("models", "..", "model.yang");

        YangEditorSession session = manager.open(file, "module model {}");

        assertSame(session, manager.find(Path.of("model.yang")));
    }

    @Test
    void syncsCurrentSessionState() {
        YangEditorSessionManager manager = new YangEditorSessionManager();
        YangEditorSession session = manager.createUntitled("");
        manager.select(session);
        YangTreeState treeState = new YangTreeState(Set.of("/module/a"), "/module/a");

        manager.syncCurrent(Path.of("saved.yang"), "module saved {}", true, treeState);

        assertEquals(Path.of("saved.yang"), session.file());
        assertEquals("module saved {}", session.text());
        assertEquals(true, session.isDirty());
        assertSame(treeState, session.treeState());
    }
}
