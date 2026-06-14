package com.yangdesigner.yangmodeldesigner.ui.state;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void keepsUndoHistoryPerSession() {
        YangEditorSession first = new YangEditorSession("first", null, "module first {}", false);
        YangEditorSession second = new YangEditorSession("second", null, "module second {}", false);

        first.recordEdit("module first {}");
        first.setText("module first { leaf name { type string; } }");
        second.recordEdit("module second {}");
        second.setText("module second { leaf enabled { type boolean; } }");

        assertEquals("module first {}", first.undo(first.text()).orElseThrow());
        assertEquals("module second {}", second.undo(second.text()).orElseThrow());
        assertEquals("module first { leaf name { type string; } }", first.redo("module first {}").orElseThrow());
        assertTrue(second.redo("module second {}").isPresent());
    }

    @Test
    void movesSessionBetweenEditorManagersWithoutCopyingState() {
        YangEditorSessionManager primary = new YangEditorSessionManager();
        YangEditorSessionManager secondary = new YangEditorSessionManager();
        YangEditorSession session = primary.createUntitled("module moved {}");
        primary.select(session);
        session.setDirty(true);

        assertTrue(primary.moveTo(session, secondary));

        assertEquals(0, primary.size());
        assertEquals(1, secondary.size());
        assertSame(session, secondary.sessions().get(0));
        assertTrue(secondary.sessions().get(0).isDirty());
        assertEquals("module moved {}", secondary.sessions().get(0).text());
    }

    @Test
    void displaysFullNormalizedFilePathForTabContextMenu() {
        YangEditorSession saved = new YangEditorSession(
                "saved.yang",
                Path.of("models", "..", "saved.yang"),
                "module saved {}",
                false
        );
        YangEditorSession untitled = new YangEditorSession("Новая модель 1", null, "", false);

        assertEquals(Path.of("saved.yang").toAbsolutePath().normalize().toString(), saved.displayPath());
        assertEquals("Файл еще не сохранен", untitled.displayPath());
    }
}
