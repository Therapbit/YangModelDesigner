package com.yangdesigner.yangmodeldesigner.ui;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.parser.YangParseResult;
import com.yangdesigner.yangmodeldesigner.service.YangDocumentService;
import com.yangdesigner.yangmodeldesigner.service.YangXmlSampleGenerator;
import com.yangdesigner.yangmodeldesigner.ui.controller.FindReplaceController;
import com.yangdesigner.yangmodeldesigner.ui.controller.InstructionController;
import com.yangdesigner.yangmodeldesigner.ui.controller.YangDocumentAnalysisController;
import com.yangdesigner.yangmodeldesigner.ui.controller.YangDocumentController;
import com.yangdesigner.yangmodeldesigner.ui.controller.YangNodeController;
import com.yangdesigner.yangmodeldesigner.ui.controller.YangTreeController;
import com.yangdesigner.yangmodeldesigner.ui.editor.YangCompletionSupport;
import com.yangdesigner.yangmodeldesigner.ui.editor.EditorTextSupport;
import com.yangdesigner.yangmodeldesigner.ui.editor.YangSyntaxHighlighter;
import com.yangdesigner.yangmodeldesigner.ui.state.YangEditorSession;
import com.yangdesigner.yangmodeldesigner.ui.state.YangEditorSessionManager;
import com.yangdesigner.yangmodeldesigner.ui.state.YangTreeState;
import com.yangdesigner.yangmodeldesigner.ui.state.UiMessage;
import com.yangdesigner.yangmodeldesigner.ui.view.MarkdownViewFactory;
import com.yangdesigner.yangmodeldesigner.validation.PyangValidator;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class ClassicMainView {
    private static final int DEFAULT_EDITOR_FONT_SIZE = 13;
    private static final int MIN_EDITOR_FONT_SIZE = 9;
    private static final int MAX_EDITOR_FONT_SIZE = 32;
    private static final String PREF_PYANG_IETF = "pyang.ietf";
    private final Stage stage;
    private final Preferences preferences = Preferences.userNodeForPackage(ClassicMainView.class);
    private final YangDocumentService documentService = new YangDocumentService();
    private final YangXmlSampleGenerator xmlSampleGenerator = new YangXmlSampleGenerator();
    private final PyangValidator validator = new PyangValidator();
    private final YangDocumentController documentController = new YangDocumentController(documentService, xmlSampleGenerator);
    private final YangDocumentAnalysisController analysisController = new YangDocumentAnalysisController(documentService, validator);
    private final YangNodeController nodeController = new YangNodeController();
    private final YangTreeController treeController = new YangTreeController();
    private final FindReplaceController findReplaceController = new FindReplaceController();
    private final InstructionController instructionController = new InstructionController();
    private final MarkdownViewFactory markdownViewFactory = new MarkdownViewFactory();
    private final YangSyntaxHighlighter syntaxHighlighter = new YangSyntaxHighlighter();
    private final YangCompletionSupport completionSupport = new YangCompletionSupport();
    private final BorderPane root = new BorderPane();
    private final TreeView<YangNode> nodeTree = new TreeView<>();
    private final BorderPane editorPane = new BorderPane();
    private final BorderPane primaryEditorPane = new BorderPane();
    private final TabPane fileTabs = new TabPane();
    private final TabPane secondaryFileTabs = new TabPane();
    private final TabPane propertiesTabs = new TabPane();
    private final CodeArea editor = new CodeArea();
    private final CodeArea splitEditor = new CodeArea();
    private final VirtualizedScrollPane<CodeArea> primaryEditorScroll = new VirtualizedScrollPane<>(editor);
    private final VirtualizedScrollPane<CodeArea> splitEditorScroll = new VirtualizedScrollPane<>(splitEditor);
    private final BorderPane splitEditorPane = new BorderPane();
    private final SplitPane editorSplitPane = new SplitPane();
    private final Popup completionPopup = new Popup();
    private final ListView<String> completionList = new ListView<>();
    private final HBox searchBar = new HBox(8);
    private final TextField searchText = new TextField();
    private final CheckBox searchCaseSensitive = new CheckBox("Aa");
    private final Label searchStatus = new Label("");
    private final TextField nodeName = new TextField();
    private final Label nodeType = new Label("-");
    private final Label nodePath = new Label("-");
    private final ComboBox<String> nodeDataType = new ComboBox<>();
    private final TextField nodeRange = new TextField();
    private final TextArea nodeDescription = new TextArea();
    private final TextArea nodeConstraints = new TextArea();
    private final TriStateCheckBox nodeConfig = new TriStateCheckBox("config");
    private final TriStateCheckBox nodeMandatory = new TriStateCheckBox("mandatory");
    private final ComboBox<YangNodeType> childType = new ComboBox<>();
    private final TextField newNodeName = new TextField();
    private final ComboBox<String> newNodeDataType = new ComboBox<>();
    private final TextField newNodeRange = new TextField();
    private final TextArea newNodeDescription = new TextArea();
    private final TextArea newNodeConstraints = new TextArea();
    private final TriStateCheckBox newNodeConfig = new TriStateCheckBox("config");
    private final TriStateCheckBox newNodeMandatory = new TriStateCheckBox("mandatory");
    private final ListView<UiMessage> messages = new ListView<>();
    private final PauseTransition parseDelay = new PauseTransition(Duration.millis(650));
    private final PauseTransition autoSaveDelay = new PauseTransition(Duration.seconds(2));
    private final PauseTransition highlightDelay = new PauseTransition(Duration.millis(55));
    private URL editorCss;
    private final YangEditorSessionManager sessionManager = new YangEditorSessionManager();
    private final YangEditorSessionManager secondarySessionManager = new YangEditorSessionManager();
    private final Map<YangEditorSession, Tab> sessionTabs = new HashMap<>();
    private final Map<YangEditorSession, Tab> secondarySessionTabs = new HashMap<>();
    private SplitPane workArea;
    private Path currentFile;
    private YangDocument currentDocument;
    private YangNode selectedNode;
    private boolean switchingSessions;
    private boolean restoringTree;
    private boolean dirty;
    private boolean updatingEditor;
    private boolean applyingSessionHistory;
    private boolean acceptingCompletion;
    private boolean selectingTextWithMouse;
    private boolean switchingSecondarySessions;
    private boolean editorSplitVisible;
    private YangEditorSession draggedSession;
    private boolean draggedFromSecondary;
    private boolean treePaneVisible = true;
    private boolean propertiesPaneVisible = true;
    private boolean pyangIetfMode;
    private double treeDividerPosition = 0.24;
    private double propertiesDividerPosition = 0.70;
    private int editorFontSize = DEFAULT_EDITOR_FONT_SIZE;
    private CodeArea activeEditor = editor;
    private String lastPrimaryHighlightText = null;
    private String lastSplitHighlightText = null;
    private int lastPrimaryHighlightCaret = Integer.MIN_VALUE;
    private int lastSplitHighlightCaret = Integer.MIN_VALUE;

    public ClassicMainView(Stage stage) {
        this.stage = stage;
        pyangIetfMode = preferences.getBoolean(PREF_PYANG_IETF, false);
        configureLayout();
        configureActions();
        createNewDocument();
    }

    public Parent root() {
        return root;
    }

    public void refreshEditorHighlighting() {
        resetHighlightCache();
        highlightEditorNow();
    }

    public Path currentFile() {
        syncCurrentSession();
        return currentFile;
    }

    public boolean openInitialDocument(Path file) {
        if (file == null || !java.nio.file.Files.isRegularFile(file)) {
            return false;
        }
        YangEditorSession initialSession = sessionManager.currentSession();
        try {
            YangEditorSession opened = sessionManager.open(file, documentController.read(file));
            addSession(opened);
            removeCleanUntitledSession(initialSession);
            return true;
        } catch (IOException ex) {
            showError("Не удалось восстановить файл", ex.getMessage());
            return false;
        }
    }

    protected void refreshTreeView() {
        nodeTree.refresh();
    }

    private void configureLayout() {
        root.setTop(menuBar());

        configureCodeEditor(editor);
        configureCodeEditor(splitEditor);
        editorCss = ClassicMainView.class.getResource("/com/yangdesigner/yangmodeldesigner/yang-editor.css");
        if (editorCss != null) {
            editor.getStylesheets().add(editorCss.toExternalForm());
            splitEditor.getStylesheets().add(editorCss.toExternalForm());
            root.getStylesheets().add(editorCss.toExternalForm());
        }
        configureSearchBar();
        configureFileTabs();
        configureSecondaryFileTabs();
        configureTabDropTarget(primaryEditorPane, false);
        configureTabDropTarget(splitEditorPane, true);
        configureCompletionPopup();
        primaryEditorPane.setTop(fileTabs);
        primaryEditorPane.setCenter(primaryEditorScroll);
        splitEditorPane.setTop(secondaryFileTabs);
        splitEditorPane.setCenter(splitEditorScroll);
        editorSplitPane.setDividerPositions(0.5);
        refreshEditorSplitLayout();

        nodeTree.setShowRoot(true);
        nodeTree.setMinWidth(240);
        nodeTree.setCellFactory(ignored -> new TreeCell<>() {
            @Override
            protected void updateItem(YangNode node, boolean empty) {
                super.updateItem(node, empty);
                if (empty || node == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(treeCellText(node));
                setGraphic(treeCellGraphic(node));
            }

            {
                setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.MIDDLE && getTreeItem() == nodeTree.getRoot()) {
                        treeController.expandAll(nodeTree.getRoot());
                        syncCurrentSession();
                        event.consume();
                    }
                });
            }
        });

        configurePropertiesPane();
        workArea = new SplitPane();
        refreshWorkAreaLayout();

        messages.setCellFactory(ignored -> new MessageCell());
        messages.setMinHeight(70);
        messages.setPrefHeight(130);
        SplitPane verticalArea = new SplitPane(workArea, messages);
        verticalArea.setOrientation(Orientation.VERTICAL);
        verticalArea.setDividerPositions(0.82);
        root.setCenter(verticalArea);
    }

    private void configureCodeEditor(CodeArea codeEditor) {
        codeEditor.setWrapText(false);
        codeEditor.getStyleClass().add("yang-code-area");
        codeEditor.setParagraphGraphicFactory(LineNumberFactory.get(codeEditor));
    }

    private MenuBar menuBar() {
        MenuItem newFile = item("Создать", "Shortcut+N", this::createNewDocument);
        MenuItem open = item("Открыть...", "Shortcut+O", this::openDocument);
        MenuItem reload = item("Перезагрузить", "Shortcut+Shift+R", this::reloadDocument);
        MenuItem save = item("Сохранить", "Shortcut+S", this::saveDocument);
        MenuItem saveAs = item("Сохранить как...", "Shortcut+Shift+S", this::saveDocumentAs);
        MenuItem export = item("Экспортировать...", "Shortcut+E", this::exportDocument);
        MenuItem exportXml = item("Экспортировать XML...", "Shortcut+Shift+E", this::exportXmlSample);
        MenuItem find = item("Найти...", "Shortcut+F", this::showFindDialog);
        MenuItem replace = item("Заменить...", "Shortcut+H", this::showReplaceDialog);
        MenuItem format = item("Форматировать", "Shortcut+Shift+L", this::formatDocument);
        CheckMenuItem showTree = checkItem("Показать дерево", "Shortcut+Alt+DIGIT1", true, this::toggleTreePane);
        CheckMenuItem showProperties = checkItem("Показать правую панель", "Shortcut+Alt+DIGIT2", true, this::togglePropertiesPane);
        CheckMenuItem splitEditorItem = checkItem("Разделить редактор", "Shortcut+Alt+S", false, this::toggleEditorSplit);
        MenuItem zoomIn = item("Увеличить масштаб", "Shortcut+PLUS", this::zoomEditorIn);
        MenuItem zoomOut = item("Уменьшить масштаб", "Shortcut+MINUS", this::zoomEditorOut);
        MenuItem resetZoom = item("Сбросить масштаб", "Shortcut+0", this::resetEditorZoom);
        MenuItem validate = item("Проверить", "Shortcut+R", this::validateDocument);
        CheckMenuItem pyangIetf = checkItem("pyang --ietf", "Shortcut+Alt+I", pyangIetfMode, this::togglePyangIetfMode);
        MenuItem instruction = item("Инструкция", "F1", this::showInstruction);

        Menu file = new Menu("Файл");
        file.getItems().addAll(newFile, open, new SeparatorMenuItem(), save, saveAs, exportXml);

        Menu edit = new Menu("Правка");
        edit.getItems().addAll(find, replace, new SeparatorMenuItem(), format);

        Menu view = new Menu("Вид");
        view.getItems().addAll(showTree, showProperties, splitEditorItem, new SeparatorMenuItem(), zoomIn, zoomOut, resetZoom);

        Menu tools = new Menu("Инструменты");
        tools.getItems().addAll(validate, new SeparatorMenuItem(), pyangIetf);

        Menu help = new Menu("Помощь");
        help.getItems().add(instruction);

        return new MenuBar(file, edit, view, tools, help);
    }

    private MenuItem item(String text, String shortcut, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setAccelerator(KeyCombination.keyCombination(shortcut));
        item.setOnAction(event -> action.run());
        return item;
    }

    private CheckMenuItem checkItem(String text, String shortcut, boolean selected, Runnable action) {
        CheckMenuItem item = new CheckMenuItem(text);
        item.setSelected(selected);
        item.setAccelerator(KeyCombination.keyCombination(shortcut));
        item.setOnAction(event -> action.run());
        return item;
    }

    private void toggleTreePane() {
        treePaneVisible = !treePaneVisible;
        refreshWorkAreaLayout();
    }

    private void togglePropertiesPane() {
        propertiesPaneVisible = !propertiesPaneVisible;
        refreshWorkAreaLayout();
    }

    private void toggleEditorSplit() {
        editorSplitVisible = !editorSplitVisible;
        refreshEditorSplitLayout();
    }

    private void refreshEditorSplitLayout() {
        if (editorSplitVisible) {
            ensureSecondarySessionSelected();
            editorPane.setCenter(null);
            editorSplitPane.getItems().setAll(primaryEditorPane, splitEditorPane);
            editorPane.setCenter(editorSplitPane);
            editorSplitPane.setDividerPositions(0.5);
        } else {
            syncEditorSession(splitEditor, secondarySessionManager.currentSession());
            if (activeEditor == splitEditor) {
                activeEditor = editor;
            }
            editorSplitPane.getItems().clear();
            editorPane.setCenter(primaryEditorPane);
        }
        highlightEditor();
    }

    private void ensureSecondarySessionSelected() {
        YangEditorSession current = secondarySessionManager.currentSession();
        if (current != null) {
            switchSecondaryToSession(current);
            selectSecondaryTab(current);
            return;
        }
        addSecondarySession(secondarySessionManager.createUntitled(documentService.newModuleTemplate()));
    }

    private void selectSecondaryTab(YangEditorSession session) {
        Tab tab = secondarySessionTabs.get(session);
        if (tab == null) {
            return;
        }
        switchingSecondarySessions = true;
        secondaryFileTabs.getSelectionModel().select(tab);
        switchingSecondarySessions = false;
    }

    private void togglePyangIetfMode() {
        pyangIetfMode = !pyangIetfMode;
        preferences.putBoolean(PREF_PYANG_IETF, pyangIetfMode);
    }

    private void refreshWorkAreaLayout() {
        if (workArea == null) {
            return;
        }
        captureWorkAreaDividers();
        workArea.getItems().clear();
        if (treePaneVisible) {
            workArea.getItems().add(nodeTree);
        }
        workArea.getItems().add(editorPane);
        if (propertiesPaneVisible) {
            workArea.getItems().add(propertiesTabs);
        }
        restoreWorkAreaDividers();
    }

    private void captureWorkAreaDividers() {
        double[] positions = workArea.getDividerPositions();
        if (treePaneVisible && propertiesPaneVisible && positions.length >= 2) {
            treeDividerPosition = positions[0];
            propertiesDividerPosition = positions[1];
        } else if (treePaneVisible && positions.length >= 1) {
            treeDividerPosition = positions[0];
        } else if (propertiesPaneVisible && positions.length >= 1) {
            propertiesDividerPosition = positions[0];
        }
    }

    private void restoreWorkAreaDividers() {
        if (treePaneVisible && propertiesPaneVisible) {
            workArea.setDividerPositions(treeDividerPosition, propertiesDividerPosition);
        } else if (treePaneVisible) {
            workArea.setDividerPositions(treeDividerPosition);
        } else if (propertiesPaneVisible) {
            workArea.setDividerPositions(propertiesDividerPosition);
        }
    }

    private void configureSearchBar() {
        searchText.setPromptText("Найти");
        searchText.setMinWidth(260);
        searchText.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchText, Priority.ALWAYS);

        Button previous = new Button("Назад");
        Button next = new Button("Далее");
        Button close = new Button("Закрыть");
        previous.setOnAction(event -> findPreviousFromSearchBar());
        next.setOnAction(event -> findNextFromSearchBar());
        close.setOnAction(event -> hideSearchBar());
        searchText.setOnAction(event -> findNextFromSearchBar());

        searchBar.getChildren().setAll(
                new Label("Найти"),
                searchText,
                searchCaseSensitive,
                previous,
                next,
                searchStatus,
                close
        );
        searchBar.setPadding(new Insets(6, 8, 6, 8));
        searchBar.getStyleClass().add("search-bar");
        if (!isModernInterface()) {
            searchBar.setStyle("-fx-background-color: #eef2f7; -fx-border-color: #cbd5e1; -fx-border-width: 1 0 0 0;");
        }
        searchBar.setVisible(false);
        searchBar.setManaged(false);
        editorPane.setBottom(searchBar);
    }

    private void configureFileTabs() {
        fileTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        fileTabs.setMinHeight(30);
        fileTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (switchingSessions || newTab == null) {
                return;
            }
            switchToSession(sessionFor(newTab));
        });
    }

    private void configureSecondaryFileTabs() {
        secondaryFileTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        secondaryFileTabs.setMinHeight(30);
        secondaryFileTabs.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (switchingSecondarySessions || newTab == null) {
                return;
            }
            switchSecondaryToSession(sessionFor(newTab));
        });
    }

    private void configureTabDropTarget(Node target, boolean secondary) {
        target.setOnDragOver(event -> {
            if (editorSplitVisible && draggedSession != null && draggedFromSecondary != secondary) {
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });
        target.setOnDragDropped(event -> {
            boolean moved = editorSplitVisible
                    && draggedSession != null
                    && draggedFromSecondary != secondary
                    && moveSessionBetweenEditors(draggedSession, draggedFromSecondary, secondary);
            event.setDropCompleted(moved);
            event.consume();
        });
    }

    private void configureCompletionPopup() {
        completionList.setPrefWidth(260);
        completionList.setPrefHeight(180);
        completionList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (handleCompletionNavigation(event)) {
                activeEditor.requestFocus();
            }
        });
        completionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                acceptCompletion();
            }
        });
        completionPopup.setAutoHide(true);
        completionPopup.getContent().add(completionList);
    }

    private void addSession(YangEditorSession session) {
        sessionManager.add(session);
        Tab tab = createSessionTab(session, false);
        sessionTabs.put(session, tab);
        fileTabs.getTabs().add(tab);
        selectSession(session);
    }

    private void addSecondarySession(YangEditorSession session) {
        secondarySessionManager.add(session);
        Tab tab = createSessionTab(session, true);
        secondarySessionTabs.put(session, tab);
        secondaryFileTabs.getTabs().add(tab);
        selectSecondarySession(session);
    }

    private Tab createSessionTab(YangEditorSession session, boolean secondary) {
        Tab tab = new Tab();
        Label label = new Label();
        tab.setGraphic(label);
        tab.setUserData(session);
        setTabTitle(tab, session);
        ContextMenu pathMenu = new ContextMenu();
        MenuItem pathItem = new MenuItem();
        pathItem.setDisable(true);
        pathMenu.getItems().add(pathItem);
        pathMenu.setOnShowing(event -> pathItem.setText(session.displayPath()));
        label.setContextMenu(pathMenu);
        label.setOnDragDetected(event -> {
            draggedSession = session;
            draggedFromSecondary = secondary;
            Dragboard dragboard = label.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(session.displayName());
            dragboard.setContent(content);
            event.consume();
        });
        label.setOnDragDone(event -> {
            draggedSession = null;
            event.consume();
        });
        YangEditorSessionManager manager = secondary ? secondarySessionManager : sessionManager;
        Map<YangEditorSession, Tab> tabs = secondary ? secondarySessionTabs : sessionTabs;
        tab.setOnCloseRequest(event -> {
            if (manager.size() == 1) {
                event.consume();
            } else if (session == manager.currentSession()) {
                if (secondary) {
                    syncEditorSession(splitEditor, session);
                } else {
                    syncCurrentSession();
                }
            }
        });
        tab.setOnClosed(event -> {
            manager.remove(session);
            tabs.remove(session);
        });
        return tab;
    }

    private boolean moveSessionBetweenEditors(YangEditorSession session, boolean sourceSecondary, boolean targetSecondary) {
        YangEditorSessionManager sourceManager = sourceSecondary ? secondarySessionManager : sessionManager;
        YangEditorSessionManager targetManager = targetSecondary ? secondarySessionManager : sessionManager;
        Map<YangEditorSession, Tab> sourceTabs = sourceSecondary ? secondarySessionTabs : sessionTabs;
        Map<YangEditorSession, Tab> targetTabs = targetSecondary ? secondarySessionTabs : sessionTabs;
        TabPane sourcePane = sourceSecondary ? secondaryFileTabs : fileTabs;
        TabPane targetPane = targetSecondary ? secondaryFileTabs : fileTabs;

        syncSessionBeforeMove(session, sourceSecondary);
        Tab sourceTab = sourceTabs.get(session);
        if (sourceTab == null || !sourceManager.moveTo(session, targetManager)) {
            return false;
        }

        setSessionSwitching(sourceSecondary, true);
        sourcePane.getTabs().remove(sourceTab);
        sourceTabs.remove(session);
        setSessionSwitching(sourceSecondary, false);

        Tab targetTab = createSessionTab(session, targetSecondary);
        targetTabs.put(session, targetTab);
        targetPane.getTabs().add(targetTab);
        selectRemainingSession(sourceSecondary);
        if (targetSecondary) {
            selectSecondarySession(session);
        } else {
            selectSession(session);
        }
        return true;
    }

    private void syncSessionBeforeMove(YangEditorSession session, boolean secondary) {
        if (secondary) {
            if (session == secondarySessionManager.currentSession()) {
                syncEditorSession(splitEditor, session);
                session.setTreeState(captureTreeState());
            }
        } else if (session == sessionManager.currentSession()) {
            syncCurrentSession();
        }
    }

    private void selectRemainingSession(boolean secondary) {
        YangEditorSessionManager manager = secondary ? secondarySessionManager : sessionManager;
        if (manager.size() == 0) {
            clearEditorAfterMove(secondary ? splitEditor : editor, secondary);
            return;
        }
        YangEditorSession remaining = manager.currentSession() == null
                ? manager.sessions().get(0)
                : manager.currentSession();
        if (secondary) {
            selectSecondarySession(remaining);
        } else {
            selectSession(remaining);
        }
    }

    private void clearEditorAfterMove(CodeArea codeEditor, boolean secondary) {
        updatingEditor = true;
        codeEditor.clear();
        clearEditorUndoHistory(codeEditor);
        updatingEditor = false;
        if (!secondary) {
            currentFile = null;
            dirty = false;
        }
    }

    private void setSessionSwitching(boolean secondary, boolean switching) {
        if (secondary) {
            switchingSecondarySessions = switching;
        } else {
            switchingSessions = switching;
        }
    }

    private void selectSession(YangEditorSession session) {
        switchingSessions = true;
        fileTabs.getSelectionModel().select(sessionTabs.get(session));
        switchingSessions = false;
        switchToSession(session);
    }

    private void selectSecondarySession(YangEditorSession session) {
        switchingSecondarySessions = true;
        secondaryFileTabs.getSelectionModel().select(secondarySessionTabs.get(session));
        switchingSecondarySessions = false;
        switchSecondaryToSession(session);
    }

    private void removeCleanUntitledSession(YangEditorSession session) {
        if (session == null || session.file() != null || session.isDirty() || sessionManager.size() <= 1) {
            return;
        }
        Tab tab = sessionTabs.remove(session);
        if (tab != null) {
            fileTabs.getTabs().remove(tab);
        }
        Tab secondaryTab = secondarySessionTabs.remove(session);
        if (secondaryTab != null) {
            secondaryFileTabs.getTabs().remove(secondaryTab);
        }
        sessionManager.remove(session);
    }

    private void switchToSession(YangEditorSession session) {
        if (session == null || session == sessionManager.currentSession()) {
            return;
        }
        syncCurrentSession();
        sessionManager.select(session);
        currentFile = session.file();
        dirty = session.isDirty();
        activeEditor = editor;
        updatingEditor = true;
        editor.replaceText(session.text());
        resetHighlightCache();
        clearEditorUndoHistory(editor);
        updatingEditor = false;
        YangTreeState treeState = session.treeState() == null ? YangTreeState.empty() : session.treeState();
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(editor.getText(), currentFile);
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.messages());
        highlightEditor();
        updateTitle();
    }

    private void switchSecondaryToSession(YangEditorSession session) {
        if (session == null || session == secondarySessionManager.currentSession()) {
            return;
        }
        syncEditorSession(splitEditor, secondarySessionManager.currentSession());
        secondarySessionManager.select(session);
        activeEditor = splitEditor;
        updatingEditor = true;
        splitEditor.replaceText(session.text());
        resetHighlightCache();
        clearEditorUndoHistory(splitEditor);
        updatingEditor = false;
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(splitEditor.getText(), session.file());
        setDocument(result.document(), session.treeState());
        messages.getItems().setAll(result.messages());
        highlightEditor();
    }

    private void syncCurrentSession() {
        YangEditorSession currentSession = sessionManager.currentSession();
        if (currentSession == null) {
            return;
        }
        sessionManager.syncCurrent(currentFile, editor.getText(), dirty, captureTreeState());
        updateTabTitle(currentSession);
    }

    private void clearEditorUndoHistory() {
        clearEditorUndoHistory(editor);
        clearEditorUndoHistory(splitEditor);
    }

    private void clearEditorUndoHistory(CodeArea codeEditor) {
        codeEditor.getUndoManager().forgetHistory();
        codeEditor.getUndoManager().mark();
    }

    private void undoEditorChange() {
        YangEditorSession currentSession = sessionForEditor(activeEditor);
        if (currentSession == null) {
            return;
        }
        currentSession.undo(activeEditor.getText()).ifPresent(this::applySessionHistoryText);
    }

    private void redoEditorChange() {
        YangEditorSession currentSession = sessionForEditor(activeEditor);
        if (currentSession == null) {
            return;
        }
        currentSession.redo(activeEditor.getText()).ifPresent(this::applySessionHistoryText);
    }

    private void applySessionHistoryText(String text) {
        applyingSessionHistory = true;
        try {
            activeEditor.replaceText(text);
            clearEditorUndoHistory();
        } finally {
            applyingSessionHistory = false;
        }
    }

    private YangEditorSession sessionFor(Tab tab) {
        return (YangEditorSession) tab.getUserData();
    }

    private YangEditorSession sessionForEditor(CodeArea codeEditor) {
        return codeEditor == splitEditor ? secondarySessionManager.currentSession() : sessionManager.currentSession();
    }

    private Path fileForEditor(CodeArea codeEditor) {
        YangEditorSession session = sessionForEditor(codeEditor);
        return session == null ? null : session.file();
    }

    private YangEditorSession findSession(Path file) {
        return activeEditor == splitEditor && editorSplitVisible
                ? secondarySessionManager.find(file)
                : sessionManager.find(file);
    }

    private void updateTabTitle(YangEditorSession session) {
        if (session == null) {
            return;
        }
        Tab tab = session == null ? null : sessionTabs.get(session);
        if (tab != null) {
            setTabTitle(tab, session);
        }
        Tab secondaryTab = secondarySessionTabs.get(session);
        if (secondaryTab != null) {
            setTabTitle(secondaryTab, session);
        }
    }

    private void setTabTitle(Tab tab, YangEditorSession session) {
        String title = (session.isDirty() ? "* " : "") + session.displayName();
        if (tab.getGraphic() instanceof Label label) {
            label.setText(title);
        } else {
            tab.setText(title);
        }
    }

    private void configurePropertiesPane() {
        propertiesTabs.setMinWidth(360);
        propertiesTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        propertiesTabs.getTabs().setAll(
                new Tab("Текущий узел", scrollablePropertiesPane(selectedNodePane())),
                new Tab("Новый узел", scrollablePropertiesPane(newNodePane()))
        );
    }

    private ScrollPane scrollablePropertiesPane(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("properties-scroll-pane");
        return scrollPane;
    }

    private VBox selectedNodePane() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.addRow(0, new Label("Имя"), nodeName);
        grid.addRow(1, new Label("Тип узла"), nodeType);
        grid.addRow(2, new Label("Путь"), nodePath);
        grid.addRow(3, new Label("YANG type"), nodeDataType);
        GridPane.setHgrow(nodeName, Priority.ALWAYS);
        GridPane.setHgrow(nodeDataType, Priority.ALWAYS);

        nodeDataType.setEditable(true);
        nodeDataType.getItems().setAll(yangTypes());
        nodeDataType.setMaxWidth(Double.MAX_VALUE);
        nodeRange.setPromptText("Например: 1..65535 | 0 | 10..max");
        nodeRange.setMaxWidth(Double.MAX_VALUE);

        nodeDescription.setWrapText(true);
        nodeDescription.setPrefRowCount(5);

        configureBooleanConstraintCheck(nodeConfig);
        configureBooleanConstraintCheck(nodeMandatory);

        nodeConstraints.setWrapText(true);
        nodeConstraints.setPrefRowCount(8);

        Button apply = new Button("Применить");
        apply.setMaxWidth(Double.MAX_VALUE);
        apply.setOnAction(event -> applySelectedNodeChanges());

        Button delete = new Button("Удалить");
        delete.setMaxWidth(Double.MAX_VALUE);
        delete.setOnAction(event -> deleteSelectedNode());

        VBox pane = new VBox(10,
                section("Просмотр и редактирование выбранного узла"),
                grid,
                section("Описание"),
                nodeDescription,
                section("Ограничения"),
                new Label("Range"),
                nodeRange,
                booleanConstraintsPane(nodeConfig, nodeMandatory),
                nodeConstraints,
                apply,
                delete);
        pane.setPadding(new Insets(12));
        pane.setMinWidth(340);
        VBox.setVgrow(nodeConstraints, Priority.ALWAYS);
        return pane;
    }

    private VBox newNodePane() {
        childType.getItems().setAll(nodeController.addableTypes());
        childType.getSelectionModel().select(YangNodeType.LEAF);
        childType.setMaxWidth(Double.MAX_VALUE);

        newNodeName.setPromptText("Имя нового узла");
        newNodeDataType.setEditable(true);
        newNodeDataType.getItems().setAll(yangTypes());
        newNodeDataType.setValue("string");
        newNodeDataType.setMaxWidth(Double.MAX_VALUE);
        newNodeRange.setPromptText("Например: 1..65535 | 0 | 10..max");
        newNodeRange.setMaxWidth(Double.MAX_VALUE);

        newNodeDescription.setWrapText(true);
        newNodeDescription.setPrefRowCount(5);
        newNodeDescription.setPromptText("Описание нового узла");

        configureBooleanConstraintCheck(newNodeConfig);
        configureBooleanConstraintCheck(newNodeMandatory);
        resetBooleanConstraintCheck(newNodeConfig);
        resetBooleanConstraintCheck(newNodeMandatory);

        newNodeConstraints.setWrapText(true);
        newNodeConstraints.setPrefRowCount(8);
        newNodeConstraints.setPromptText("when: ../enabled\nmust: ../name");

        Button addChild = new Button("Добавить к выбранному узлу");
        addChild.setMaxWidth(Double.MAX_VALUE);
        addChild.setOnAction(event -> addChild());

        VBox pane = new VBox(10,
                section("Создание дочернего узла"),
                new Label("Родитель: выбранный узел в дереве"),
                new Label("Тип нового узла"),
                childType,
                new Label("Имя нового узла"),
                newNodeName,
                new Label("YANG type"),
                newNodeDataType,
                new Label("Range"),
                newNodeRange,
                section("Описание нового узла"),
                newNodeDescription,
                section("Ограничения нового узла"),
                booleanConstraintsPane(newNodeConfig, newNodeMandatory),
                newNodeConstraints,
                addChild);
        pane.setPadding(new Insets(12));
        pane.setMinWidth(340);
        VBox.setVgrow(newNodeConstraints, Priority.ALWAYS);
        return pane;
    }

    private HBox booleanConstraintsPane(TriStateCheckBox config, TriStateCheckBox mandatory) {
        HBox pane = new HBox(16, config, mandatory);
        pane.setPadding(new Insets(0, 0, 2, 0));
        return pane;
    }

    private Label section(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private void configureActions() {
        parseDelay.setOnFinished(event -> parseAndRefresh());
        autoSaveDelay.setOnFinished(event -> autoSaveDocument());
        highlightDelay.setOnFinished(event -> highlightEditorNow());
        configureEditorActions(editor);
        configureEditorActions(splitEditor);
        nodeTree.getSelectionModel().selectedItemProperty().addListener((observable, oldItem, newItem) -> {
            if (newItem != null) {
                showNode(newItem.getValue());
                if (!restoringTree) {
                    navigateToNodeDefinition(newItem.getValue());
                }
            }
        });
        messages.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                UiMessage message = messages.getSelectionModel().getSelectedItem();
                if (message != null && !message.path().isBlank()) {
                    selectNodeByPath(message.path());
                } else if (message != null && message.line() > 0) {
                    navigateToLine(message.line());
                }
            }
        });
    }

    private void configureEditorActions(CodeArea codeEditor) {
        codeEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> handleEditorKeyPressed(codeEditor, event));
        codeEditor.addEventFilter(KeyEvent.KEY_TYPED, event -> handleEditorKeyTyped(codeEditor, event));
        codeEditor.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            activeEditor = codeEditor;
            if (event.getButton() == MouseButton.PRIMARY) {
                selectingTextWithMouse = true;
            }
        });
        codeEditor.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            activeEditor = codeEditor;
            if (event.getButton() == MouseButton.PRIMARY) {
                selectingTextWithMouse = false;
                highlightEditor();
            }
        });
        codeEditor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingEditor) {
                return;
            }
            activeEditor = codeEditor;
            YangEditorSession editedSession = sessionForEditor(codeEditor);
            if (editedSession != null) {
                if (!applyingSessionHistory) {
                    editedSession.recordEdit(oldValue);
                }
                editedSession.setText(newValue);
                editedSession.setDirty(true);
                updateTabTitle(editedSession);
            }
            if (codeEditor == editor) {
                dirty = true;
            }
            highlightEditor();
            updateTitle();
            parseDelay.playFromStart();
            scheduleAutoSave();
            updateCompletionPopup();
        });
        codeEditor.caretPositionProperty().addListener((observable, oldValue, newValue) -> {
            activeEditor = codeEditor;
            highlightEditor();
        });
        codeEditor.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused) {
                activeEditor = codeEditor;
            }
            if (!focused) {
                completionPopup.hide();
            }
        });
    }

    private void handleEditorKeyTyped(CodeArea codeEditor, KeyEvent event) {
        activeEditor = codeEditor;
        if (!"{".equals(event.getCharacter())) {
            return;
        }
        event.consume();
        insertBracePair(codeEditor);
    }

    private void syncEditorSession(CodeArea codeEditor, YangEditorSession session) {
        if (session == null) {
            return;
        }
        session.setText(codeEditor.getText());
        if (codeEditor == editor) {
            session.setFile(currentFile);
            session.setDirty(dirty);
        }
        updateTabTitle(session);
    }

    private void handleEditorKeyPressed(CodeArea codeEditor, KeyEvent event) {
        activeEditor = codeEditor;
        if (completionPopup.isShowing() && handleCompletionNavigation(event)) {
            return;
        }
        if (event.isShortcutDown() && event.getCode() == KeyCode.Z) {
            event.consume();
            if (event.isShiftDown()) {
                redoEditorChange();
            } else {
                undoEditorChange();
            }
            return;
        }
        if (event.isShortcutDown() && event.getCode() == KeyCode.Y) {
            event.consume();
            redoEditorChange();
            return;
        }
        if (event.isShortcutDown() && event.isShiftDown() && event.getCode() == KeyCode.L) {
            event.consume();
            formatDocument();
            return;
        }
        if (event.isShortcutDown() && (event.getCode() == KeyCode.SLASH || event.getCode() == KeyCode.DIVIDE)) {
            event.consume();
            toggleLineComment();
            return;
        }
        if (event.isShortcutDown() && event.getCode() == KeyCode.SPACE) {
            event.consume();
            showCompletionPopup(true);
            return;
        }
        if (event.isShortcutDown() && event.getCode() == KeyCode.EQUALS) {
            event.consume();
            zoomEditorIn();
            return;
        }
        if (event.getCode() == KeyCode.ESCAPE && searchBar.isVisible()) {
            event.consume();
            hideSearchBar();
            return;
        }
        if (event.getCode() == KeyCode.ESCAPE && completionPopup.isShowing()) {
            event.consume();
            completionPopup.hide();
            return;
        }
        if (event.getCode() == KeyCode.TAB) {
            event.consume();
            if (event.isShiftDown()) {
                unindentSelection();
            } else {
                indentSelection();
            }
            return;
        }
        if (event.getCode() == KeyCode.ENTER) {
            event.consume();
            insertIndentedNewLine();
        }
    }

    private boolean handleCompletionNavigation(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN) {
            event.consume();
            int index = completionList.getSelectionModel().getSelectedIndex();
            completionList.getSelectionModel().select(Math.min(index + 1, completionList.getItems().size() - 1));
            completionList.scrollTo(completionList.getSelectionModel().getSelectedIndex());
            return true;
        }
        if (event.getCode() == KeyCode.UP) {
            event.consume();
            int index = completionList.getSelectionModel().getSelectedIndex();
            completionList.getSelectionModel().select(Math.max(index - 1, 0));
            completionList.scrollTo(completionList.getSelectionModel().getSelectedIndex());
            return true;
        }
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
            event.consume();
            acceptCompletion();
            return true;
        }
        if (event.getCode() == KeyCode.ESCAPE) {
            event.consume();
            completionPopup.hide();
            return true;
        }
        return false;
    }

    private void indentSelection() {
        CodeArea codeEditor = activeEditor;
        IndexRange selection = codeEditor.getSelection();
        if (selection.getLength() == 0) {
            codeEditor.replaceSelection(EditorTextSupport.INDENT);
            return;
        }
        String text = codeEditor.getText();
        int start = EditorTextSupport.lineStart(text, selection.getStart());
        int end = EditorTextSupport.lineEnd(text, selection.getEnd());
        String block = text.substring(start, end);
        String indented = EditorTextSupport.addIndent(block);
        codeEditor.replaceText(start, end, indented);
        codeEditor.selectRange(selection.getStart() + EditorTextSupport.INDENT.length(), selection.getEnd() + indented.length() - block.length());
    }

    private void insertBracePair(CodeArea codeEditor) {
        IndexRange selection = codeEditor.getSelection();
        EditorTextSupport.BracePair bracePair = EditorTextSupport.bracePair(
                codeEditor.getText(),
                selection.getStart(),
                selection.getEnd()
        );
        codeEditor.replaceText(bracePair.start(), bracePair.end(), bracePair.replacement());
        if (selection.getLength() > 0) {
            codeEditor.selectRange(bracePair.caretPosition(), bracePair.selectionEnd());
            return;
        }
        codeEditor.moveTo(bracePair.caretPosition());
    }

    private void unindentSelection() {
        CodeArea codeEditor = activeEditor;
        IndexRange selection = codeEditor.getSelection();
        String text = codeEditor.getText();
        int originalCaret = codeEditor.getCaretPosition();
        int start = EditorTextSupport.lineStart(text, selection.getStart());
        int end = selection.getLength() == 0 ? EditorTextSupport.lineEnd(text, selection.getStart()) : EditorTextSupport.lineEnd(text, selection.getEnd());
        String block = text.substring(start, end);
        String unindented = EditorTextSupport.removeIndent(block);
        int removed = block.length() - unindented.length();
        int removedBeforeSelectionStart = removedIndentBefore(block, selection.getStart() - start);
        int removedBeforeSelectionEnd = removedIndentBefore(block, selection.getEnd() - start);
        codeEditor.replaceText(start, end, unindented);
        if (selection.getLength() == 0) {
            codeEditor.moveTo(Math.max(start, originalCaret - removed));
        } else {
            codeEditor.selectRange(
                    Math.max(start, selection.getStart() - removedBeforeSelectionStart),
                    Math.max(start, selection.getEnd() - removedBeforeSelectionEnd)
            );
        }
    }

    private int removedIndentBefore(String block, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, block.length()));
        String prefix = block.substring(0, safeOffset);
        return prefix.length() - EditorTextSupport.removeIndent(prefix).length();
    }

    private void insertIndentedNewLine() {
        CodeArea codeEditor = activeEditor;
        EditorTextSupport.IndentedNewLine newLine = EditorTextSupport.indentedNewLine(codeEditor.getText(), codeEditor.getCaretPosition());
        codeEditor.replaceSelection(newLine.insertion());
        if (newLine.caretPosition() >= 0) {
            codeEditor.moveTo(newLine.caretPosition());
        }
    }

    private void toggleLineComment() {
        CodeArea codeEditor = activeEditor;
        IndexRange selection = codeEditor.getSelection();
        int caret = codeEditor.getCaretPosition();
        EditorTextSupport.CommentToggle toggle = EditorTextSupport.toggleLineComment(
                codeEditor.getText(),
                selection.getStart(),
                selection.getEnd()
        );
        codeEditor.replaceText(toggle.start(), toggle.end(), toggle.replacement());
        if (selection.getLength() == 0) {
            int delta = toggle.replacement().length() - (toggle.end() - toggle.start());
            codeEditor.moveTo(Math.max(toggle.start(), Math.min(codeEditor.getLength(), caret + delta)));
        } else {
            codeEditor.selectRange(toggle.start(), toggle.start() + toggle.replacement().length());
        }
    }

    private void updateCompletionPopup() {
        if (acceptingCompletion || updatingEditor || !activeEditor.isFocused()) {
            return;
        }
        showCompletionPopup(false);
    }

    private void showCompletionPopup(boolean force) {
        CodeArea codeEditor = activeEditor;
        List<String> suggestions = completionSupport.suggestions(codeEditor.getText(), codeEditor.getCaretPosition(), 12);
        if (suggestions.isEmpty()) {
            completionPopup.hide();
            return;
        }
        String prefix = completionSupport.prefix(codeEditor.getText(), codeEditor.getCaretPosition());
        if (!force && prefix.length() < 2) {
            completionPopup.hide();
            return;
        }
        completionList.getItems().setAll(suggestions);
        completionList.getSelectionModel().selectFirst();
        Bounds caretBounds = codeEditor.getCaretBounds().orElse(null);
        if (caretBounds == null) {
            completionPopup.show(codeEditor, stage.getX() + 120, stage.getY() + 120);
            return;
        }
        if (!completionPopup.isShowing()) {
            completionPopup.show(codeEditor, caretBounds.getMinX(), caretBounds.getMaxY() + 4);
        }
    }

    private void acceptCompletion() {
        String completion = completionList.getSelectionModel().getSelectedItem();
        if (completion == null && !completionList.getItems().isEmpty()) {
            completionList.getSelectionModel().selectFirst();
            completion = completionList.getSelectionModel().getSelectedItem();
        }
        if (completion == null || completion.isBlank()) {
            completionPopup.hide();
            activeEditor.requestFocus();
            return;
        }
        CodeArea codeEditor = activeEditor;
        int caret = codeEditor.getCaretPosition();
        int start = completionSupport.prefixStart(codeEditor.getText(), caret);
        acceptingCompletion = true;
        try {
            codeEditor.replaceText(start, caret, completionSupport.insertionText(completion));
        } finally {
            acceptingCompletion = false;
        }
        completionPopup.hide();
        codeEditor.requestFocus();
    }

    private void zoomEditorIn() {
        setEditorFontSize(editorFontSize + 1);
    }

    private void zoomEditorOut() {
        setEditorFontSize(editorFontSize - 1);
    }

    private void resetEditorZoom() {
        setEditorFontSize(DEFAULT_EDITOR_FONT_SIZE);
    }

    private void setEditorFontSize(int size) {
        editorFontSize = Math.max(MIN_EDITOR_FONT_SIZE, Math.min(MAX_EDITOR_FONT_SIZE, size));
        editor.setStyle("-fx-font-size: " + editorFontSize + "px;");
        splitEditor.setStyle("-fx-font-size: " + editorFontSize + "px;");
    }

    private void showFindDialog() {
        String selectedText = activeEditor.getSelectedText();
        if (selectedText != null && !selectedText.isBlank() && !selectedText.contains("\n") && !selectedText.contains("\r")) {
            searchText.setText(selectedText);
        }
        searchStatus.setText("");
        searchBar.setVisible(true);
        searchBar.setManaged(true);
        searchText.requestFocus();
        searchText.selectAll();
    }

    private void showReplaceDialog() {
        showFindReplaceDialog(true);
    }

    private void hideSearchBar() {
        searchBar.setVisible(false);
        searchBar.setManaged(false);
        activeEditor.requestFocus();
    }

    private void findNextFromSearchBar() {
        boolean found = findNext(searchText.getText(), searchCaseSensitive.isSelected());
        searchStatus.setText(found ? "" : "Не найдено");
    }

    private void findPreviousFromSearchBar() {
        boolean found = findPrevious(searchText.getText(), searchCaseSensitive.isSelected());
        searchStatus.setText(found ? "" : "Не найдено");
    }

    private void showFindReplaceDialog(boolean replaceMode) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(replaceMode ? "Заменить" : "Найти");
        dialog.setHeaderText(replaceMode ? "Замена в YANG-тексте" : "Поиск в YANG-тексте");
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField findText = new TextField();
        findText.setPromptText("Найти");
        TextField replaceText = new TextField();
        replaceText.setPromptText("Заменить на");
        CheckBox caseSensitive = new CheckBox("Учитывать регистр");
        Button findNext = new Button("Найти далее");
        Button replace = new Button("Заменить");
        Button replaceAll = new Button("Заменить все");

        GridPane fields = new GridPane();
        fields.setHgap(8);
        fields.setVgap(8);
        fields.addRow(0, new Label("Найти"), findText);
        if (replaceMode) {
            fields.addRow(1, new Label("Заменить"), replaceText);
        }
        GridPane.setHgrow(findText, Priority.ALWAYS);
        GridPane.setHgrow(replaceText, Priority.ALWAYS);

        HBox actions = replaceMode
                ? new HBox(8, findNext, replace, replaceAll)
                : new HBox(8, findNext);
        VBox content = new VBox(10, fields, caseSensitive, actions);
        content.setPadding(new Insets(10));
        content.setMinWidth(480);
        dialog.getDialogPane().setContent(content);

        findNext.setOnAction(event -> findNext(findText.getText(), caseSensitive.isSelected()));
        replace.setOnAction(event -> replaceCurrent(findText.getText(), replaceText.getText(), caseSensitive.isSelected()));
        replaceAll.setOnAction(event -> replaceAll(findText.getText(), replaceText.getText(), caseSensitive.isSelected()));

        dialog.setOnShown(event -> {
            String selectedText = activeEditor.getSelectedText();
            if (selectedText != null && !selectedText.isBlank() && !selectedText.contains("\n") && !selectedText.contains("\r")) {
                findText.setText(selectedText);
            }
            findText.requestFocus();
        });
        dialog.showAndWait();
    }

    private boolean findNext(String query, boolean caseSensitive) {
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return false;
        }
        Optional<FindReplaceController.TextRange> range = findReplaceController.findNext(
                activeEditor.getText(),
                cleanQuery,
                caseSensitive,
                activeEditor.getCaretPosition(),
                activeEditor.getSelection().getEnd()
        );
        if (range.isEmpty()) {
            messages.getItems().add(new UiMessage("Текст не найден: " + cleanQuery, "", 0));
            return false;
        }
        activeEditor.selectRange(range.get().start(), range.get().end());
        activeEditor.requestFollowCaret();
        return true;
    }

    private boolean findPrevious(String query, boolean caseSensitive) {
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return false;
        }
        Optional<FindReplaceController.TextRange> range = findReplaceController.findPrevious(
                activeEditor.getText(),
                cleanQuery,
                caseSensitive,
                activeEditor.getSelection().getStart()
        );
        if (range.isEmpty()) {
            messages.getItems().add(new UiMessage("Текст не найден: " + cleanQuery, "", 0));
            return false;
        }
        activeEditor.selectRange(range.get().start(), range.get().end());
        activeEditor.requestFollowCaret();
        return true;
    }

    private void replaceCurrent(String query, String replacement, boolean caseSensitive) {
        String selectedText = activeEditor.getSelectedText();
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return;
        }
        if (!findReplaceController.matches(selectedText, cleanQuery, caseSensitive) && !findNext(cleanQuery, caseSensitive)) {
            return;
        }
        activeEditor.replaceSelection(replacement == null ? "" : replacement);
        findNext(cleanQuery, caseSensitive);
    }

    private void replaceAll(String query, String replacement, boolean caseSensitive) {
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return;
        }
        activeEditor.replaceText(findReplaceController.replaceAll(activeEditor.getText(), cleanQuery, replacement, caseSensitive));
    }

    private void createNewDocument() {
        if (activeEditor == splitEditor && editorSplitVisible) {
            addSecondarySession(secondarySessionManager.createUntitled(documentService.newModuleTemplate()));
        } else {
            addSession(sessionManager.createUntitled(documentService.newModuleTemplate()));
        }
    }

    private void openDocument() {
        FileChooser chooser = yangChooser("Открыть YANG модель");
        Path file = selectedPath(chooser.showOpenDialog(stage));
        if (file == null) {
            return;
        }
        YangEditorSession existing = findSession(file);
        if (existing != null) {
            if (activeEditor == splitEditor && editorSplitVisible) {
                selectSecondarySession(existing);
            } else {
                selectSession(existing);
            }
            return;
        }
        try {
            if (activeEditor == splitEditor && editorSplitVisible) {
                addSecondarySession(secondarySessionManager.open(file, documentController.read(file)));
            } else {
                addSession(sessionManager.open(file, documentController.read(file)));
            }
        } catch (IOException ex) {
            showError("Не удалось открыть файл", ex.getMessage());
        }
    }

    private void reloadDocument() {
        CodeArea codeEditor = activeEditor;
        YangEditorSession session = sessionForEditor(codeEditor);
        Path file = session == null ? null : session.file();
        if (file == null) {
            showError("Файл не выбран", "Новая модель еще не связана с файлом.");
            return;
        }
        try {
            String reloadedText = documentController.read(file);
            updatingEditor = true;
            codeEditor.replaceText(reloadedText);
            clearEditorUndoHistory(codeEditor);
            updatingEditor = false;
            if (codeEditor == editor) {
                dirty = false;
            }
            session.setText(reloadedText);
            session.setDirty(false);
            session.clearEditHistory();
            parseAndRefresh();
            highlightEditor();
            syncEditorSession(codeEditor, session);
            updateTitle();
        } catch (IOException ex) {
            updatingEditor = false;
            showError("Не удалось перезагрузить файл", ex.getMessage());
        }
    }

    private void saveDocument() {
        CodeArea codeEditor = activeEditor;
        YangEditorSession session = sessionForEditor(codeEditor);
        Path file = session == null ? null : session.file();
        if (file == null) {
            saveDocumentAs();
            return;
        }
        try {
            Path savedFile = documentController.save(file, codeEditor.getText());
            session.setFile(savedFile);
            session.setText(codeEditor.getText());
            session.setDirty(false);
            if (codeEditor == editor) {
                currentFile = savedFile;
                dirty = false;
            }
            updateTabTitle(session);
            updateTitle();
        } catch (IOException ex) {
            showError("Не удалось сохранить файл", ex.getMessage());
        }
    }

    private void saveDocumentAs() {
        YangEditorSession session = sessionForEditor(activeEditor);
        FileChooser chooser = yangChooser("Сохранить YANG модель");
        chooser.setInitialFileName(documentController.defaultYangFileName(session == null ? null : session.file()));
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        Path target = documentController.ensureYangExtension(file);
        if (session != null) {
            session.setFile(target);
        }
        if (activeEditor == editor) {
            currentFile = target;
        }
        saveDocument();
    }

    private void exportDocument() {
        YangEditorSession session = sessionForEditor(activeEditor);
        FileChooser chooser = yangChooser("Экспортировать YANG модель");
        chooser.setInitialFileName(documentController.defaultYangFileName(session == null ? null : session.file()));
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        try {
            documentController.exportYang(file, activeEditor.getText());
        } catch (IOException ex) {
            showError("Не удалось экспортировать файл", ex.getMessage());
        }
    }

    private void exportXmlSample() {
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(activeEditor.getText(), fileForEditor(activeEditor));
        setDocument(result.document(), captureTreeState());
        if (!result.messages().isEmpty()) {
            messages.getItems().setAll(result.messages());
            showError("XML не экспортирован", "Сначала исправьте ошибки разбора YANG-модели.");
            return;
        }
        FileChooser chooser = xmlChooser("Экспортировать XML-пример");
        chooser.setInitialFileName(documentController.defaultXmlFileName(fileForEditor(activeEditor)));
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        try {
            documentController.exportXml(file, result.document(), fileForEditor(activeEditor));
        } catch (IOException ex) {
            showError("Не удалось экспортировать XML", ex.getMessage());
        }
    }

    private void validateDocument() {
        YangTreeState treeState = captureTreeState();
        YangDocumentAnalysisController.AnalysisResult result = analysisController.validate(activeEditor.getText(), fileForEditor(activeEditor), pyangIetfMode);
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.messages());
    }

    private void formatDocument() {
        CodeArea codeEditor = activeEditor;
        YangParseResult parseResult = documentService.parse(codeEditor.getText(), fileForEditor(codeEditor));
        if (parseResult.hasErrors()) {
            messages.getItems().setAll(parseResult.errors().stream()
                    .map(error -> new UiMessage(error, "", 0))
                    .toList());
            return;
        }
        YangTreeState treeState = captureTreeState();
        int caret = codeEditor.getCaretPosition();
        String formatted = documentService.writeToText(parseResult.document());
        updatingEditor = true;
        codeEditor.replaceText(formatted);
        codeEditor.moveTo(Math.min(caret, formatted.length()));
        updatingEditor = false;
        if (codeEditor == editor) {
            dirty = true;
        }
        YangEditorSession session = sessionForEditor(codeEditor);
        if (session != null) {
            session.setText(formatted);
            session.setDirty(true);
        }
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(formatted, fileForEditor(codeEditor));
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.messages());
        syncCurrentSession();
        highlightEditor();
        updateTitle();
        scheduleAutoSave();
    }

    private void parseAndRefresh() {
        YangTreeState treeState = captureTreeState();
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(activeEditor.getText(), fileForEditor(activeEditor));
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.messages());
        syncCurrentSession();
    }

    private void setDocument(YangDocument document) {
        setDocument(document, YangTreeState.empty());
    }

    private void setDocument(YangDocument document, YangTreeState treeState) {
        currentDocument = document;
        TreeItem<YangNode> rootItem = treeController.toTreeItem(document.root());
        nodeTree.setRoot(rootItem);
        restoreTreeState(treeState);
    }

    private void showNode(YangNode node) {
        if (restoringTree) {
            selectedNode = node;
            return;
        }
        selectedNode = node;
        nodeName.setText(blank(node.name()));
        nodeType.setText(node.type().name());
        nodePath.setText(node.path());
        nodeDataType.setValue(node.dataType().isBlank() ? "" : node.dataType());
        nodeRange.setText(firstConstraintValue(node.constraints().get("range")));
        nodeDescription.setText(blank(node.description()));
        nodeConstraints.setText(nodeController.formatConstraints(node.constraints()));
        setBooleanConstraintCheck(nodeConfig, node.constraints().get("config"));
        setBooleanConstraintCheck(nodeMandatory, node.constraints().get("mandatory"));
    }

    private void applySelectedNodeChanges() {
        if (selectedNode == null || currentDocument == null) {
            return;
        }
        messages.getItems().addAll(nodeController.applyNodeChanges(selectedNode, nodeEditData(
                nodeName.getText(),
                nodeDataType.getValue(),
                nodeDescription.getText(),
                nodeConstraints.getText(),
                nodeRange.getText(),
                nodeConfig,
                nodeMandatory
        )));
        regenerateTextFromModel();
    }

    private void addChild() {
        YangNodeType type = childType.getValue();
        if (selectedNode == null || currentDocument == null || type == null) {
            return;
        }
        YangNodeController.AddChildResult result = nodeController.addChild(selectedNode, type, nodeEditData(
                newNodeName.getText(),
                newNodeDataType.getValue(),
                newNodeDescription.getText(),
                newNodeConstraints.getText(),
                newNodeRange.getText(),
                newNodeConfig,
                newNodeMandatory
        ));
        messages.getItems().addAll(result.messages());
        clearNewNodeForm();
        regenerateTextFromModel();
    }

    private void clearNewNodeForm() {
        newNodeName.clear();
        newNodeDescription.clear();
        newNodeConstraints.clear();
        newNodeRange.clear();
        newNodeDataType.setValue("string");
        resetBooleanConstraintCheck(newNodeConfig);
        resetBooleanConstraintCheck(newNodeMandatory);
        childType.getSelectionModel().select(YangNodeType.LEAF);
    }

    private void deleteSelectedNode() {
        Optional<YangNode> parent = nodeController.delete(selectedNode);
        if (parent.isEmpty()) {
            return;
        }
        selectedNode = parent.get();
        regenerateTextFromModel();
    }

    private void regenerateTextFromModel() {
        CodeArea codeEditor = activeEditor;
        updatingEditor = true;
        String generatedText = documentService.writeToText(currentDocument);
        codeEditor.replaceText(generatedText);
        highlightEditor();
        updatingEditor = false;
        if (codeEditor == editor) {
            dirty = true;
        }
        YangEditorSession session = sessionForEditor(codeEditor);
        if (session != null) {
            session.setText(generatedText);
            session.setDirty(true);
        }
        parseAndRefresh();
        updateTitle();
        scheduleAutoSave();
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String firstConstraintValue(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.getFirst();
    }

    private void configureBooleanConstraintCheck(TriStateCheckBox checkBox) {
        resetBooleanConstraintCheck(checkBox);
    }

    private void resetBooleanConstraintCheck(TriStateCheckBox checkBox) {
        checkBox.setValue(null);
    }

    private void setBooleanConstraintCheck(TriStateCheckBox checkBox, List<String> values) {
        if (values == null || values.isEmpty()) {
            resetBooleanConstraintCheck(checkBox);
            return;
        }
        String value = values.get(values.size() - 1);
        if ("true".equalsIgnoreCase(value)) {
            checkBox.setValue(true);
            return;
        }
        if ("false".equalsIgnoreCase(value)) {
            checkBox.setValue(false);
            return;
        }
        resetBooleanConstraintCheck(checkBox);
    }

    private YangNodeController.NodeEditData nodeEditData(
            String name,
            String dataType,
            String description,
            String constraintsText,
            String range,
            TriStateCheckBox config,
            TriStateCheckBox mandatory
    ) {
        return new YangNodeController.NodeEditData(
                name,
                dataType,
                description,
                constraintsText,
                range,
                booleanConstraintValue(config),
                booleanConstraintValue(mandatory)
        );
    }

    private Boolean booleanConstraintValue(TriStateCheckBox checkBox) {
        return checkBox.value();
    }

    private static final class TriStateCheckBox extends HBox {
        private final Label indicator = new Label();
        private final Label caption = new Label();
        private Boolean value;

        private TriStateCheckBox(String text) {
            super(6);
            caption.setText(text);
            getStyleClass().add("tri-state-check");
            indicator.getStyleClass().add("tri-state-indicator");
            caption.getStyleClass().add("tri-state-caption");
            setAlignment(Pos.CENTER_LEFT);
            setFocusTraversable(true);
            getChildren().addAll(indicator, caption);
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    advance();
                }
            });
            addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    advance();
                }
            });
            setValue(null);
        }

        private void advance() {
            if (value == null) {
                setValue(true);
            } else if (value) {
                setValue(false);
            } else {
                setValue(null);
            }
        }

        private void setValue(Boolean value) {
            this.value = value;
            getStyleClass().removeAll("tri-state-unset", "tri-state-true", "tri-state-false");
            if (value == null) {
                indicator.setText("-");
                getStyleClass().add("tri-state-unset");
            } else if (value) {
                indicator.setText("✓");
                getStyleClass().add("tri-state-true");
            } else {
                indicator.setText("");
                getStyleClass().add("tri-state-false");
            }
        }

        private Boolean value() {
            return value;
        }
    }

    private void scheduleAutoSave() {
        YangEditorSession session = sessionForEditor(activeEditor);
        if (session != null && session.file() != null) {
            autoSaveDelay.playFromStart();
        }
    }

    private void autoSaveDocument() {
        CodeArea codeEditor = activeEditor;
        YangEditorSession session = sessionForEditor(codeEditor);
        if (session == null || session.file() == null || !session.isDirty()) {
            return;
        }
        try {
            Path savedFile = documentController.save(session.file(), codeEditor.getText());
            session.setFile(savedFile);
            session.setText(codeEditor.getText());
            session.setDirty(false);
            if (codeEditor == editor) {
                currentFile = savedFile;
                dirty = false;
            }
            updateTabTitle(session);
            updateTitle();
        } catch (IOException ex) {
            messages.getItems().add(new UiMessage("Автосохранение не выполнено: " + ex.getMessage(), "", 0));
        }
    }

    private List<String> yangTypes() {
        return List.of(
                "",
                "string",
                "boolean",
                "int8",
                "int16",
                "int32",
                "int64",
                "uint8",
                "uint16",
                "uint32",
                "uint64",
                "decimal64",
                "empty",
                "enumeration",
                "bits",
                "binary",
                "leafref",
                "identityref",
                "instance-identifier"
        );
    }

    protected String treeCellText(YangNode node) {
        return null;
    }

    protected Node treeCellGraphic(YangNode node) {
        if (isModernInterface()) {
            return modernTreeCellGraphic(node);
        }
        return classicTreeCellGraphic(node);
    }

    protected boolean isModernInterface() {
        return false;
    }

    private HBox modernTreeCellGraphic(YangNode node) {
        Label typeIcon = new Label(typeIcon(node.type()));
        typeIcon.getStyleClass().addAll("yang-tree-type-icon", "yang-tree-type-" + node.type().keyword());

        Label type = new Label(node.type().keyword());
        type.getStyleClass().add("yang-tree-type-label");

        Label name = new Label(treeNodeName(node));
        name.getStyleClass().add("yang-tree-title");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label access = accessModeLabel(node);
        HBox graphic = new HBox(8, typeIcon, type, name, access);
        graphic.getStyleClass().add("yang-tree-node");
        return graphic;
    }

    private HBox classicTreeCellGraphic(YangNode node) {
        Label typeIcon = new Label(typeIcon(node.type()));
        typeIcon.setMinWidth(22);
        typeIcon.setStyle("-fx-text-fill: #5b6f91; -fx-font-weight: bold;");

        Label type = new Label(node.type().keyword());
        type.setStyle("-fx-text-fill: #1f6fb2; -fx-font-weight: bold;");

        Label name = new Label(treeNodeName(node));
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        HBox graphic = new HBox(7, typeIcon, type, name, accessModeLabel(node));
        graphic.setMinHeight(24);
        return graphic;
    }

    private String treeNodeName(YangNode node) {
        return node.name().isBlank() ? "-" : node.name();
    }

    private String typeIcon(YangNodeType type) {
        return switch (type) {
            case MODULE -> "⬡";
            case SUBMODULE -> "⬢";
            case IMPORT -> "⇲";
            case INCLUDE -> "⊂";
            case REVISION -> "◷";
            case EXTENSION -> "✦";
            case FEATURE -> "◈";
            case IDENTITY -> "◆";
            case TYPEDEF -> "T";
            case CONTAINER -> "▣";
            case LIST -> "☷";
            case LEAF -> "•";
            case LEAF_LIST -> "⋮";
            case CHOICE -> "◇";
            case CASE -> "◌";
            case GROUPING -> "▦";
            case USES -> "↪";
            case AUGMENT -> "＋";
            case RPC -> "λ";
            case ACTION -> "▶";
            case INPUT -> "→";
            case OUTPUT -> "←";
            case NOTIFICATION -> "!";
            case DEVIATION -> "△";
            case DEVIATE -> "▵";
            case ANYDATA -> "{ }";
            case ANYXML -> "<>";
            case ENUM -> "≡";
            case BIT -> "◻";
            case UNKNOWN -> "?";
        };
    }

    private Label accessModeLabel(YangNode node) {
        boolean readOnly = treeController.isReadOnly(node);
        Label label = new Label(readOnly ? "RO" : "RW");
        label.setMinWidth(26);
        if (isModernInterface()) {
            label.getStyleClass().addAll("yang-tree-access", readOnly ? "yang-tree-access-ro" : "yang-tree-access-rw");
            return label;
        }
        label.setStyle(readOnly
                ? "-fx-background-color: #eef1f5; -fx-text-fill: #46515f; -fx-font-size: 10px; -fx-padding: 1 4 1 4;"
                : "-fx-background-color: #e7f5ec; -fx-text-fill: #1f6b3a; -fx-font-size: 10px; -fx-padding: 1 4 1 4;");
        return label;
    }

    private void showInstruction() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Инструкция");
        dialog.setHeaderText("YANG Model Designer");
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ScrollPane scrollPane = new ScrollPane(markdownViewFactory.create(
                instructionController.readInstructionText(ClassicMainView.class)
        ));
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(900);
        scrollPane.setPrefViewportHeight(680);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }


    private FileChooser yangChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("YANG files", "*.yang"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        if (currentFile != null && currentFile.getParent() != null) {
            chooser.setInitialDirectory(currentFile.getParent().toFile());
        }
        return chooser;
    }

    private FileChooser xmlChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML files", "*.xml"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        if (currentFile != null && currentFile.getParent() != null) {
            chooser.setInitialDirectory(currentFile.getParent().toFile());
        }
        return chooser;
    }

    private Path selectedPath(java.io.File file) {
        return file == null ? null : file.toPath();
    }

    private void updateTitle() {
        YangEditorSession activeSession = sessionForEditor(activeEditor);
        String fileName = activeSession == null
                ? "Новая модель"
                : activeSession.displayName();
        if (activeSession != null) {
            if (activeEditor == editor) {
                activeSession.setFile(currentFile);
                activeSession.setDirty(dirty);
            }
            updateTabTitle(activeSession);
        }
        boolean activeDirty = activeSession != null && activeSession.isDirty();
        stage.setTitle((activeDirty ? "* " : "") + fileName + " - YANG Model Designer");
    }

    private void highlightEditor() {
        if (selectingTextWithMouse) {
            return;
        }
        highlightDelay.playFromStart();
    }

    private void highlightEditorNow() {
        if (selectingTextWithMouse) {
            return;
        }
        highlightCodeEditor(editor);
        if (editorSplitVisible) {
            highlightCodeEditor(splitEditor);
        }
    }

    private void highlightCodeEditor(CodeArea codeEditor) {
        int caretPosition = codeEditor.getSelection().getLength() == 0 ? codeEditor.getCaretPosition() : -1;
        String text = codeEditor.getText();
        if (!highlightChanged(codeEditor, text, caretPosition)) {
            return;
        }
        codeEditor.setStyleSpans(0, syntaxHighlighter.compute(text, caretPosition));
        rememberHighlight(codeEditor, text, caretPosition);
    }

    private boolean highlightChanged(CodeArea codeEditor, String text, int caretPosition) {
        if (codeEditor == editor) {
            return caretPosition != lastPrimaryHighlightCaret || !text.equals(lastPrimaryHighlightText);
        }
        return caretPosition != lastSplitHighlightCaret || !text.equals(lastSplitHighlightText);
    }

    private void rememberHighlight(CodeArea codeEditor, String text, int caretPosition) {
        if (codeEditor == editor) {
            lastPrimaryHighlightText = text;
            lastPrimaryHighlightCaret = caretPosition;
        } else {
            lastSplitHighlightText = text;
            lastSplitHighlightCaret = caretPosition;
        }
    }

    private void resetHighlightCache() {
        lastPrimaryHighlightText = null;
        lastSplitHighlightText = null;
        lastPrimaryHighlightCaret = Integer.MIN_VALUE;
        lastSplitHighlightCaret = Integer.MIN_VALUE;
    }

    private void selectNodeByPath(String path) {
        TreeItem<YangNode> item = treeController.findByPath(nodeTree.getRoot(), path);
        if (item == null) {
            return;
        }
        treeController.expandParents(item);
        nodeTree.getSelectionModel().select(item);
        nodeTree.scrollTo(nodeTree.getRow(item));
    }

    private YangTreeState captureTreeState() {
        return treeController.captureTreeState(nodeTree.getRoot(), selectedNode);
    }

    private void restoreTreeState(YangTreeState treeState) {
        TreeItem<YangNode> rootItem = nodeTree.getRoot();
        if (rootItem == null) {
            return;
        }
        restoringTree = true;
        treeController.restoreExpandedPaths(rootItem, treeState.expandedPaths());
        TreeItem<YangNode> selectedItem = treeController.findByPath(rootItem, treeState.selectedPath());
        if (selectedItem == null) {
            selectedItem = rootItem;
        }
        treeController.expandParents(selectedItem);
        nodeTree.getSelectionModel().select(selectedItem);
        showNode(selectedItem.getValue());
        restoringTree = false;
        showNode(selectedItem.getValue());
    }

    private void navigateToNodeDefinition(YangNode node) {
        if (node.line() <= 0 || activeEditor.getLength() == 0) {
            return;
        }
        navigateToLine(node.line());
    }

    private void navigateToLine(int line) {
        if (line <= 0 || activeEditor.getLength() == 0) {
            return;
        }
        int position = treeController.offsetForLine(activeEditor.getText(), line);
        activeEditor.moveTo(position);
        activeEditor.showParagraphAtCenter(Math.max(0, line - 1));
    }

    private static final class MessageCell extends ListCell<UiMessage> {
        @Override
        protected void updateItem(UiMessage message, boolean empty) {
            super.updateItem(message, empty);
            if (empty || message == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(null);
            setGraphic(messageGraphic(message.text()));
        }

        private TextFlow messageGraphic(String message) {
            if (message.startsWith("ERROR:")) {
                return severityGraphic("ERROR", message.substring("ERROR".length()), Color.web("#ff5f57"));
            }
            if (message.startsWith("WARNING:")) {
                return severityGraphic("WARNING", message.substring("WARNING".length()), Color.web("#f4c430"));
            }
            return new TextFlow(new Text(message));
        }

        private TextFlow severityGraphic(String severity, String rest, Color color) {
            Text severityText = new Text(severity);
            severityText.setFill(color);
            severityText.setStyle("-fx-font-weight: bold;");
            Text restText = new Text(rest);
            return new TextFlow(severityText, restText);
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.setHeaderText(header);
        alert.initOwner(stage);
        alert.showAndWait();
    }

}
