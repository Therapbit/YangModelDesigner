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
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
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
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

public class ClassicMainView {
    private static final int DEFAULT_EDITOR_FONT_SIZE = 13;
    private static final int MIN_EDITOR_FONT_SIZE = 9;
    private static final int MAX_EDITOR_FONT_SIZE = 32;
    private final Stage stage;
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
    private final TabPane fileTabs = new TabPane();
    private final CodeArea editor = new CodeArea();
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
    private URL editorCss;
    private final YangEditorSessionManager sessionManager = new YangEditorSessionManager();
    private final Map<YangEditorSession, Tab> sessionTabs = new HashMap<>();
    private Path currentFile;
    private YangDocument currentDocument;
    private YangNode selectedNode;
    private boolean switchingSessions;
    private boolean restoringTree;
    private boolean dirty;
    private boolean updatingEditor;
    private boolean acceptingCompletion;
    private boolean selectingTextWithMouse;
    private int editorFontSize = DEFAULT_EDITOR_FONT_SIZE;

    public ClassicMainView(Stage stage) {
        this.stage = stage;
        configureLayout();
        configureActions();
        createNewDocument();
    }

    public Parent root() {
        return root;
    }

    public void refreshEditorHighlighting() {
        highlightEditor();
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

        editor.setWrapText(false);
        editor.getStyleClass().add("yang-code-area");
        editor.setParagraphGraphicFactory(LineNumberFactory.get(editor));
        editorCss = ClassicMainView.class.getResource("/com/yangdesigner/yangmodeldesigner/yang-editor.css");
        if (editorCss != null) {
            editor.getStylesheets().add(editorCss.toExternalForm());
            root.getStylesheets().add(editorCss.toExternalForm());
        }
        configureSearchBar();
        configureFileTabs();
        configureCompletionPopup();
        editorPane.setTop(fileTabs);
        editorPane.setCenter(new VirtualizedScrollPane<>(editor));

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
        });

        SplitPane workArea = new SplitPane(nodeTree, editorPane, propertiesPane());
        workArea.setDividerPositions(0.24, 0.70);
        root.setCenter(workArea);

        messages.setPrefHeight(130);
        root.setBottom(messages);
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
        MenuItem zoomIn = item("Увеличить масштаб", "Shortcut+PLUS", this::zoomEditorIn);
        MenuItem zoomOut = item("Уменьшить масштаб", "Shortcut+MINUS", this::zoomEditorOut);
        MenuItem resetZoom = item("Сбросить масштаб", "Shortcut+0", this::resetEditorZoom);
        MenuItem validate = item("Проверить", "Shortcut+R", this::validateDocument);
        MenuItem instruction = item("Инструкция", "F1", this::showInstruction);

        Menu file = new Menu("Файл");
        file.getItems().addAll(newFile, open, new SeparatorMenuItem(), save, saveAs, exportXml);

        Menu edit = new Menu("Правка");
        edit.getItems().addAll(find, replace, new SeparatorMenuItem(), format);

        Menu view = new Menu("Вид");
        view.getItems().addAll(zoomIn, zoomOut, resetZoom);

        Menu tools = new Menu("Инструменты");
        tools.getItems().add(validate);

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

    private void configureCompletionPopup() {
        completionList.setPrefWidth(260);
        completionList.setPrefHeight(180);
        completionList.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (handleCompletionNavigation(event)) {
                editor.requestFocus();
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
        Tab tab = new Tab(session.displayName());
        tab.setUserData(session);
        sessionTabs.put(session, tab);
        sessionManager.add(session);
        tab.setOnCloseRequest(event -> {
            if (sessionManager.size() == 1) {
                event.consume();
            } else if (session == sessionManager.currentSession()) {
                syncCurrentSession();
            }
        });
        tab.setOnClosed(event -> {
            sessionManager.remove(session);
            sessionTabs.remove(session);
        });
        fileTabs.getTabs().add(tab);
        selectSession(session);
    }

    private void selectSession(YangEditorSession session) {
        switchingSessions = true;
        fileTabs.getSelectionModel().select(sessionTabs.get(session));
        switchingSessions = false;
        switchToSession(session);
    }

    private void removeCleanUntitledSession(YangEditorSession session) {
        if (session == null || session.file() != null || session.isDirty() || sessionManager.size() <= 1) {
            return;
        }
        Tab tab = sessionTabs.remove(session);
        if (tab != null) {
            fileTabs.getTabs().remove(tab);
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
        updatingEditor = true;
        editor.replaceText(session.text());
        updatingEditor = false;
        YangTreeState treeState = session.treeState() == null ? YangTreeState.empty() : session.treeState();
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(editor.getText(), currentFile);
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.messages());
        highlightEditor();
        updateTitle();
    }

    private void syncCurrentSession() {
        YangEditorSession currentSession = sessionManager.currentSession();
        if (currentSession == null) {
            return;
        }
        sessionManager.syncCurrent(currentFile, editor.getText(), dirty, captureTreeState());
        updateTabTitle(currentSession);
    }

    private YangEditorSession sessionFor(Tab tab) {
        return (YangEditorSession) tab.getUserData();
    }

    private YangEditorSession findSession(Path file) {
        return sessionManager.find(file);
    }

    private void updateTabTitle(YangEditorSession session) {
        Tab tab = session == null ? null : sessionTabs.get(session);
        if (tab == null) {
            return;
        }
        tab.setText((session.isDirty() ? "* " : "") + session.displayName());
    }

    private TabPane propertiesPane() {
        TabPane tabs = new TabPane();
        tabs.setMinWidth(360);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(new Tab("Текущий узел", scrollablePropertiesPane(selectedNodePane())));
        tabs.getTabs().add(new Tab("Новый узел", scrollablePropertiesPane(newNodePane())));
        return tabs;
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
        editor.addEventFilter(KeyEvent.KEY_PRESSED, this::handleEditorKeyPressed);
        editor.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selectingTextWithMouse = true;
            }
        });
        editor.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selectingTextWithMouse = false;
                highlightEditor();
            }
        });
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingEditor) {
                return;
            }
            dirty = true;
            YangEditorSession currentSession = sessionManager.currentSession();
            if (currentSession != null) {
                currentSession.setText(newValue);
                currentSession.setDirty(true);
            }
            highlightEditor();
            updateTitle();
            parseDelay.playFromStart();
            scheduleAutoSave();
            updateCompletionPopup();
        });
        editor.caretPositionProperty().addListener((observable, oldValue, newValue) -> highlightEditor());
        editor.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                completionPopup.hide();
            }
        });
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

    private void handleEditorKeyPressed(KeyEvent event) {
        if (completionPopup.isShowing() && handleCompletionNavigation(event)) {
            return;
        }
        if (event.isShortcutDown() && event.isShiftDown() && event.getCode() == KeyCode.L) {
            event.consume();
            formatDocument();
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
        IndexRange selection = editor.getSelection();
        if (selection.getLength() == 0) {
            editor.replaceSelection(EditorTextSupport.INDENT);
            return;
        }
        String text = editor.getText();
        int start = EditorTextSupport.lineStart(text, selection.getStart());
        int end = EditorTextSupport.lineEnd(text, selection.getEnd());
        String block = text.substring(start, end);
        String indented = EditorTextSupport.addIndent(block);
        editor.replaceText(start, end, indented);
        editor.selectRange(selection.getStart() + EditorTextSupport.INDENT.length(), selection.getEnd() + indented.length() - block.length());
    }

    private void unindentSelection() {
        IndexRange selection = editor.getSelection();
        String text = editor.getText();
        int originalCaret = editor.getCaretPosition();
        int start = EditorTextSupport.lineStart(text, selection.getStart());
        int end = selection.getLength() == 0 ? EditorTextSupport.lineEnd(text, selection.getStart()) : EditorTextSupport.lineEnd(text, selection.getEnd());
        String block = text.substring(start, end);
        String unindented = EditorTextSupport.removeIndent(block);
        int removed = block.length() - unindented.length();
        int removedBeforeSelectionStart = removedIndentBefore(block, selection.getStart() - start);
        int removedBeforeSelectionEnd = removedIndentBefore(block, selection.getEnd() - start);
        editor.replaceText(start, end, unindented);
        if (selection.getLength() == 0) {
            editor.moveTo(Math.max(start, originalCaret - removed));
        } else {
            editor.selectRange(
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
        EditorTextSupport.IndentedNewLine newLine = EditorTextSupport.indentedNewLine(editor.getText(), editor.getCaretPosition());
        editor.replaceSelection(newLine.insertion());
        if (newLine.caretPosition() >= 0) {
            editor.moveTo(newLine.caretPosition());
        }
    }

    private void updateCompletionPopup() {
        if (acceptingCompletion || updatingEditor || !editor.isFocused()) {
            return;
        }
        showCompletionPopup(false);
    }

    private void showCompletionPopup(boolean force) {
        List<String> suggestions = completionSupport.suggestions(editor.getText(), editor.getCaretPosition(), 12);
        if (suggestions.isEmpty()) {
            completionPopup.hide();
            return;
        }
        String prefix = completionSupport.prefix(editor.getText(), editor.getCaretPosition());
        if (!force && prefix.length() < 2) {
            completionPopup.hide();
            return;
        }
        completionList.getItems().setAll(suggestions);
        completionList.getSelectionModel().selectFirst();
        Bounds caretBounds = editor.getCaretBounds().orElse(null);
        if (caretBounds == null) {
            completionPopup.show(editor, stage.getX() + 120, stage.getY() + 120);
            return;
        }
        if (!completionPopup.isShowing()) {
            completionPopup.show(editor, caretBounds.getMinX(), caretBounds.getMaxY() + 4);
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
            editor.requestFocus();
            return;
        }
        int caret = editor.getCaretPosition();
        int start = completionSupport.prefixStart(editor.getText(), caret);
        acceptingCompletion = true;
        try {
            editor.replaceText(start, caret, completionSupport.insertionText(completion));
        } finally {
            acceptingCompletion = false;
        }
        completionPopup.hide();
        editor.requestFocus();
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
    }

    private void showFindDialog() {
        String selectedText = editor.getSelectedText();
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
        editor.requestFocus();
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
            String selectedText = editor.getSelectedText();
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
                editor.getText(),
                cleanQuery,
                caseSensitive,
                editor.getCaretPosition(),
                editor.getSelection().getEnd()
        );
        if (range.isEmpty()) {
            messages.getItems().add(new UiMessage("Текст не найден: " + cleanQuery, "", 0));
            return false;
        }
        editor.selectRange(range.get().start(), range.get().end());
        editor.requestFollowCaret();
        return true;
    }

    private boolean findPrevious(String query, boolean caseSensitive) {
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return false;
        }
        Optional<FindReplaceController.TextRange> range = findReplaceController.findPrevious(
                editor.getText(),
                cleanQuery,
                caseSensitive,
                editor.getSelection().getStart()
        );
        if (range.isEmpty()) {
            messages.getItems().add(new UiMessage("Текст не найден: " + cleanQuery, "", 0));
            return false;
        }
        editor.selectRange(range.get().start(), range.get().end());
        editor.requestFollowCaret();
        return true;
    }

    private void replaceCurrent(String query, String replacement, boolean caseSensitive) {
        String selectedText = editor.getSelectedText();
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return;
        }
        if (!findReplaceController.matches(selectedText, cleanQuery, caseSensitive) && !findNext(cleanQuery, caseSensitive)) {
            return;
        }
        editor.replaceSelection(replacement == null ? "" : replacement);
        findNext(cleanQuery, caseSensitive);
    }

    private void replaceAll(String query, String replacement, boolean caseSensitive) {
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return;
        }
        editor.replaceText(findReplaceController.replaceAll(editor.getText(), cleanQuery, replacement, caseSensitive));
    }

    private void createNewDocument() {
        addSession(sessionManager.createUntitled(documentService.newModuleTemplate()));
    }

    private void openDocument() {
        FileChooser chooser = yangChooser("Открыть YANG модель");
        Path file = selectedPath(chooser.showOpenDialog(stage));
        if (file == null) {
            return;
        }
        YangEditorSession existing = findSession(file);
        if (existing != null) {
            selectSession(existing);
            return;
        }
        try {
            addSession(sessionManager.open(file, documentController.read(file)));
        } catch (IOException ex) {
            showError("Не удалось открыть файл", ex.getMessage());
        }
    }

    private void reloadDocument() {
        if (currentFile == null) {
            showError("Файл не выбран", "Новая модель еще не связана с файлом.");
            return;
        }
        try {
            updatingEditor = true;
            editor.replaceText(documentController.read(currentFile));
            updatingEditor = false;
            dirty = false;
            parseAndRefresh();
            highlightEditor();
            syncCurrentSession();
            updateTitle();
        } catch (IOException ex) {
            updatingEditor = false;
            showError("Не удалось перезагрузить файл", ex.getMessage());
        }
    }

    private void saveDocument() {
        if (currentFile == null) {
            saveDocumentAs();
            return;
        }
        try {
            currentFile = documentController.save(currentFile, editor.getText());
            dirty = false;
            syncCurrentSession();
            updateTitle();
        } catch (IOException ex) {
            showError("Не удалось сохранить файл", ex.getMessage());
        }
    }

    private void saveDocumentAs() {
        FileChooser chooser = yangChooser("Сохранить YANG модель");
        chooser.setInitialFileName(documentController.defaultYangFileName(currentFile));
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        currentFile = documentController.ensureYangExtension(file);
        saveDocument();
    }

    private void exportDocument() {
        FileChooser chooser = yangChooser("Экспортировать YANG модель");
        chooser.setInitialFileName(documentController.defaultYangFileName(currentFile));
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        try {
            documentController.exportYang(file, editor.getText());
        } catch (IOException ex) {
            showError("Не удалось экспортировать файл", ex.getMessage());
        }
    }

    private void exportXmlSample() {
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(editor.getText(), currentFile);
        setDocument(result.document(), captureTreeState());
        if (!result.messages().isEmpty()) {
            messages.getItems().setAll(result.messages());
            showError("XML не экспортирован", "Сначала исправьте ошибки разбора YANG-модели.");
            return;
        }
        FileChooser chooser = xmlChooser("Экспортировать XML-пример");
        chooser.setInitialFileName(documentController.defaultXmlFileName(currentFile));
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        try {
            documentController.exportXml(file, result.document());
        } catch (IOException ex) {
            showError("Не удалось экспортировать XML", ex.getMessage());
        }
    }

    private void validateDocument() {
        YangTreeState treeState = captureTreeState();
        YangDocumentAnalysisController.AnalysisResult result = analysisController.validate(editor.getText(), currentFile);
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.messages());
    }

    private void formatDocument() {
        YangParseResult parseResult = documentService.parse(editor.getText(), currentFile);
        if (parseResult.hasErrors()) {
            messages.getItems().setAll(parseResult.errors().stream()
                    .map(error -> new UiMessage(error, "", 0))
                    .toList());
            return;
        }
        YangTreeState treeState = captureTreeState();
        int caret = editor.getCaretPosition();
        String formatted = documentService.writeToText(parseResult.document());
        updatingEditor = true;
        editor.replaceText(formatted);
        editor.moveTo(Math.min(caret, formatted.length()));
        updatingEditor = false;
        dirty = true;
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(formatted, currentFile);
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.messages());
        syncCurrentSession();
        highlightEditor();
        updateTitle();
        scheduleAutoSave();
    }

    private void parseAndRefresh() {
        YangTreeState treeState = captureTreeState();
        YangDocumentAnalysisController.AnalysisResult result = analysisController.parse(editor.getText(), currentFile);
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
        updatingEditor = true;
        editor.replaceText(documentService.writeToText(currentDocument));
        highlightEditor();
        updatingEditor = false;
        dirty = true;
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
        if (currentFile != null) {
            autoSaveDelay.playFromStart();
        }
    }

    private void autoSaveDocument() {
        if (currentFile == null || !dirty) {
            return;
        }
        try {
            currentFile = documentController.save(currentFile, editor.getText());
            dirty = false;
            syncCurrentSession();
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
            case MODULE, SUBMODULE -> "M";
            case CONTAINER -> "C";
            case LIST -> "L";
            case LEAF -> "f";
            case LEAF_LIST -> "[]";
            case RPC, ACTION -> "λ";
            case INPUT -> "in";
            case OUTPUT -> "out";
            case NOTIFICATION -> "!";
            case TYPEDEF -> "T";
            case IDENTITY -> "I";
            case CHOICE, CASE -> "?";
            default -> "?";
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
        YangEditorSession currentSession = sessionManager.currentSession();
        String fileName = currentSession == null
                ? "Новая модель"
                : currentSession.displayName();
        if (currentSession != null) {
            currentSession.setFile(currentFile);
            currentSession.setDirty(dirty);
            updateTabTitle(currentSession);
        }
        stage.setTitle((dirty ? "* " : "") + fileName + " - YANG Model Designer");
    }

    private void highlightEditor() {
        if (selectingTextWithMouse) {
            return;
        }
        int caretPosition = editor.getSelection().getLength() == 0 ? editor.getCaretPosition() : -1;
        editor.setStyleSpans(0, syntaxHighlighter.compute(editor.getText(), caretPosition));
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
        if (node.line() <= 0 || editor.getLength() == 0) {
            return;
        }
        navigateToLine(node.line());
    }

    private void navigateToLine(int line) {
        if (line <= 0 || editor.getLength() == 0) {
            return;
        }
        int position = treeController.offsetForLine(editor.getText(), line);
        editor.moveTo(position);
        editor.showParagraphAtCenter(Math.max(0, line - 1));
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.setHeaderText(header);
        alert.initOwner(stage);
        alert.showAndWait();
    }

}
