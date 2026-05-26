package com.yangdesigner.yangmodeldesigner.ui;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.parser.YangParseResult;
import com.yangdesigner.yangmodeldesigner.service.YangDocumentService;
import com.yangdesigner.yangmodeldesigner.service.YangXmlSampleGenerator;
import com.yangdesigner.yangmodeldesigner.validation.PyangValidator;
import com.yangdesigner.yangmodeldesigner.validation.ValidationIssue;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Parent;
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
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public final class MainView {
    private static final String INDENT = "  ";
    private static final int DEFAULT_EDITOR_FONT_SIZE = 13;
    private static final int MIN_EDITOR_FONT_SIZE = 9;
    private static final int MAX_EDITOR_FONT_SIZE = 32;
    private static final String[] YANG_KEYWORDS = {
            "module", "submodule", "yang-version", "namespace", "prefix",
            "import", "include", "revision", "extension", "feature", "identity",
            "typedef", "container", "list", "leaf", "leaf-list", "choice",
            "case", "grouping", "uses", "augment", "rpc", "action", "input",
            "output", "notification", "deviation", "deviate", "anydata", "anyxml",
            "type", "description", "key", "when", "must", "mandatory", "default",
            "config", "presence", "min-elements", "max-elements", "pattern",
            "range", "length", "units", "base", "if-feature", "status",
            "reference", "organization", "contact", "belongs-to", "revision-date",
            "value", "position", "enum", "bit"
    };
    private static final String[] YANG_TYPES = {
            "string", "boolean", "int8", "int16", "int32", "int64",
            "uint8", "uint16", "uint32", "uint64", "decimal64", "empty",
            "enumeration", "bits", "binary", "leafref", "identityref",
            "instance-identifier"
    };
    private static final Pattern YANG_HIGHLIGHT_PATTERN = Pattern.compile(
            "(?<COMMENT>//[^\\n]*|/\\*(.|\\R)*?\\*/)"
                    + "|(?<STRING>\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*')"
                    + "|(?<KEYWORD>\\b(" + String.join("|", YANG_KEYWORDS) + ")\\b)"
                    + "|(?<TYPE>\\b(" + String.join("|", YANG_TYPES) + ")\\b)"
                    + "|(?<BRACE>[{}])"
                    + "|(?<SEMICOLON>;)"
    );

    private final Stage stage;
    private final YangDocumentService documentService = new YangDocumentService();
    private final YangXmlSampleGenerator xmlSampleGenerator = new YangXmlSampleGenerator();
    private final PyangValidator validator = new PyangValidator();
    private final BorderPane root = new BorderPane();
    private final TreeView<YangNode> nodeTree = new TreeView<>();
    private final BorderPane editorPane = new BorderPane();
    private final TabPane fileTabs = new TabPane();
    private final CodeArea editor = new CodeArea();
    private final HBox searchBar = new HBox(8);
    private final TextField searchText = new TextField();
    private final CheckBox searchCaseSensitive = new CheckBox("Aa");
    private final Label searchStatus = new Label("");
    private final TextField nodeName = new TextField();
    private final Label nodeType = new Label("-");
    private final Label nodePath = new Label("-");
    private final ComboBox<String> nodeDataType = new ComboBox<>();
    private final TextArea nodeDescription = new TextArea();
    private final TextArea nodeConstraints = new TextArea();
    private final CheckBox nodeConfig = new CheckBox("config");
    private final CheckBox nodeMandatory = new CheckBox("mandatory");
    private final ComboBox<YangNodeType> childType = new ComboBox<>();
    private final TextField newNodeName = new TextField();
    private final ComboBox<String> newNodeDataType = new ComboBox<>();
    private final TextArea newNodeDescription = new TextArea();
    private final TextArea newNodeConstraints = new TextArea();
    private final CheckBox newNodeConfig = new CheckBox("config");
    private final CheckBox newNodeMandatory = new CheckBox("mandatory");
    private final ListView<UiMessage> messages = new ListView<>();
    private final PauseTransition parseDelay = new PauseTransition(Duration.millis(650));
    private final PauseTransition autoSaveDelay = new PauseTransition(Duration.seconds(2));
    private URL editorCss;
    private final List<EditorSession> sessions = new ArrayList<>();
    private Path currentFile;
    private EditorSession currentSession;
    private YangDocument currentDocument;
    private YangNode selectedNode;
    private int untitledCounter = 1;
    private boolean switchingSessions;
    private boolean restoringTree;
    private boolean dirty;
    private boolean updatingEditor;
    private int editorFontSize = DEFAULT_EDITOR_FONT_SIZE;

    public MainView(Stage stage) {
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

    private void configureLayout() {
        root.setTop(menuBar());

        editor.setWrapText(false);
        editor.getStyleClass().add("yang-code-area");
        editor.setParagraphGraphicFactory(LineNumberFactory.get(editor));
        editorCss = MainView.class.getResource("/com/yangdesigner/yangmodeldesigner/yang-editor.css");
        if (editorCss != null) {
            editor.getStylesheets().add(editorCss.toExternalForm());
            root.getStylesheets().add(editorCss.toExternalForm());
        }
        configureSearchBar();
        configureFileTabs();
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
                setText(node.toString());
                setGraphic(accessModeLabel(node));
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
        MenuItem zoomIn = item("Увеличить масштаб", "Shortcut+PLUS", this::zoomEditorIn);
        MenuItem zoomOut = item("Уменьшить масштаб", "Shortcut+MINUS", this::zoomEditorOut);
        MenuItem resetZoom = item("Сбросить масштаб", "Shortcut+0", this::resetEditorZoom);
        MenuItem validate = item("Проверить", "Shortcut+R", this::validateDocument);
        MenuItem instruction = item("Инструкция", "F1", this::showInstruction);

        Menu file = new Menu("Файл");
        file.getItems().addAll(newFile, open, reload, new SeparatorMenuItem(), save, saveAs, export, exportXml);

        Menu edit = new Menu("Правка");
        edit.getItems().addAll(find, replace);

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
        searchBar.setStyle("-fx-background-color: #eef2f7; -fx-border-color: #cbd5e1; -fx-border-width: 1 0 0 0;");
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

    private void addSession(EditorSession session) {
        Tab tab = new Tab(session.displayName());
        tab.setUserData(session);
        session.tab = tab;
        sessions.add(session);
        tab.setOnCloseRequest(event -> {
            if (sessions.size() == 1) {
                event.consume();
            } else if (session == currentSession) {
                syncCurrentSession();
            }
        });
        tab.setOnClosed(event -> sessions.remove(session));
        fileTabs.getTabs().add(tab);
        selectSession(session);
    }

    private void selectSession(EditorSession session) {
        switchingSessions = true;
        fileTabs.getSelectionModel().select(session.tab);
        switchingSessions = false;
        switchToSession(session);
    }

    private void switchToSession(EditorSession session) {
        if (session == null || session == currentSession) {
            return;
        }
        syncCurrentSession();
        currentSession = session;
        currentFile = session.file;
        dirty = session.dirty;
        updatingEditor = true;
        editor.replaceText(session.text);
        updatingEditor = false;
        TreeState treeState = session.treeState == null ? TreeState.empty() : session.treeState;
        YangParseResult result = documentService.parse(editor.getText(), currentFile);
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.errors().stream()
                .map(error -> new UiMessage(error, "", 0))
                .toList());
        highlightEditor();
        updateTitle();
    }

    private void syncCurrentSession() {
        if (currentSession == null) {
            return;
        }
        currentSession.file = currentFile;
        currentSession.text = editor.getText();
        currentSession.dirty = dirty;
        currentSession.treeState = captureTreeState();
        updateTabTitle(currentSession);
    }

    private EditorSession sessionFor(Tab tab) {
        return (EditorSession) tab.getUserData();
    }

    private EditorSession findSession(Path file) {
        Path normalized = file.toAbsolutePath().normalize();
        return sessions.stream()
                .filter(session -> session.file != null)
                .filter(session -> session.file.toAbsolutePath().normalize().equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private void updateTabTitle(EditorSession session) {
        if (session == null || session.tab == null) {
            return;
        }
        session.tab.setText((session.dirty ? "* " : "") + session.displayName());
    }

    private TabPane propertiesPane() {
        TabPane tabs = new TabPane();
        tabs.setMinWidth(360);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(new Tab("Текущий узел", selectedNodePane()));
        tabs.getTabs().add(new Tab("Новый узел", newNodePane()));
        return tabs;
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
        childType.getItems().setAll(addableTypes());
        childType.getSelectionModel().select(YangNodeType.LEAF);
        childType.setMaxWidth(Double.MAX_VALUE);

        newNodeName.setPromptText("Имя нового узла");
        newNodeDataType.setEditable(true);
        newNodeDataType.getItems().setAll(yangTypes());
        newNodeDataType.setValue("string");
        newNodeDataType.setMaxWidth(Double.MAX_VALUE);

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

    private HBox booleanConstraintsPane(CheckBox config, CheckBox mandatory) {
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
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingEditor) {
                return;
            }
            dirty = true;
            if (currentSession != null) {
                currentSession.text = newValue;
                currentSession.dirty = true;
            }
            highlightEditor();
            updateTitle();
            parseDelay.playFromStart();
            scheduleAutoSave();
        });
        editor.caretPositionProperty().addListener((observable, oldValue, newValue) -> highlightEditor());
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

    private void indentSelection() {
        IndexRange selection = editor.getSelection();
        if (selection.getLength() == 0) {
            editor.replaceSelection(INDENT);
            return;
        }
        String text = editor.getText();
        int start = lineStart(text, selection.getStart());
        int end = lineEnd(text, selection.getEnd());
        String block = text.substring(start, end);
        String indented = addIndent(block);
        editor.replaceText(start, end, indented);
        editor.selectRange(selection.getStart() + INDENT.length(), selection.getEnd() + indented.length() - block.length());
    }

    private void unindentSelection() {
        IndexRange selection = editor.getSelection();
        String text = editor.getText();
        int start = lineStart(text, selection.getStart());
        int end = selection.getLength() == 0 ? lineEnd(text, selection.getStart()) : lineEnd(text, selection.getEnd());
        String block = text.substring(start, end);
        String unindented = removeIndent(block);
        editor.replaceText(start, end, unindented);
        int removed = block.length() - unindented.length();
        if (selection.getLength() == 0) {
            editor.moveTo(Math.max(start, editor.getCaretPosition() - removed));
        } else {
            editor.selectRange(Math.max(start, selection.getStart() - INDENT.length()), Math.max(start, selection.getEnd() - removed));
        }
    }

    private void insertIndentedNewLine() {
        String text = editor.getText();
        int caret = editor.getCaretPosition();
        int start = lineStart(text, caret);
        int end = lineEnd(text, caret);
        String beforeCaret = text.substring(start, caret);
        String afterCaret = text.substring(caret, end);
        String baseIndent = leadingWhitespace(beforeCaret);
        String nextIndent = beforeCaret.stripTrailing().endsWith("{") ? baseIndent + INDENT : baseIndent;
        String lineSeparator = lineSeparator(text);

        if (nextIndent.length() > baseIndent.length() && afterCaret.stripLeading().startsWith("}")) {
            String insertion = lineSeparator + nextIndent + lineSeparator + baseIndent;
            editor.replaceSelection(insertion);
            editor.moveTo(caret + lineSeparator.length() + nextIndent.length());
            return;
        }
        editor.replaceSelection(lineSeparator + nextIndent);
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
        String source = editor.getText();
        String haystack = caseSensitive ? source : source.toLowerCase();
        String needle = caseSensitive ? cleanQuery : cleanQuery.toLowerCase();
        int start = Math.max(editor.getCaretPosition(), editor.getSelection().getEnd());
        int index = haystack.indexOf(needle, start);
        if (index < 0 && start > 0) {
            index = haystack.indexOf(needle);
        }
        if (index < 0) {
            messages.getItems().add(new UiMessage("Текст не найден: " + cleanQuery, "", 0));
            return false;
        }
        editor.selectRange(index, index + cleanQuery.length());
        editor.requestFollowCaret();
        return true;
    }

    private boolean findPrevious(String query, boolean caseSensitive) {
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return false;
        }
        String source = editor.getText();
        String haystack = caseSensitive ? source : source.toLowerCase();
        String needle = caseSensitive ? cleanQuery : cleanQuery.toLowerCase();
        int start = Math.max(0, editor.getSelection().getStart() - 1);
        int index = haystack.lastIndexOf(needle, start);
        if (index < 0 && start < haystack.length()) {
            index = haystack.lastIndexOf(needle);
        }
        if (index < 0) {
            messages.getItems().add(new UiMessage("Текст не найден: " + cleanQuery, "", 0));
            return false;
        }
        editor.selectRange(index, index + cleanQuery.length());
        editor.requestFollowCaret();
        return true;
    }

    private void replaceCurrent(String query, String replacement, boolean caseSensitive) {
        String selectedText = editor.getSelectedText();
        String cleanQuery = query == null ? "" : query;
        if (cleanQuery.isEmpty()) {
            return;
        }
        boolean matches = caseSensitive
                ? selectedText.equals(cleanQuery)
                : selectedText.equalsIgnoreCase(cleanQuery);
        if (!matches && !findNext(cleanQuery, caseSensitive)) {
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
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern = Pattern.compile(Pattern.quote(cleanQuery), flags);
        Matcher matcher = pattern.matcher(editor.getText());
        String updated = matcher.replaceAll(Matcher.quoteReplacement(replacement == null ? "" : replacement));
        editor.replaceText(updated);
    }

    private void createNewDocument() {
        String name = "Новая модель " + untitledCounter++;
        addSession(new EditorSession(name, null, documentService.newModuleTemplate(), false));
    }

    private void openDocument() {
        FileChooser chooser = yangChooser("Открыть YANG модель");
        Path file = selectedPath(chooser.showOpenDialog(stage));
        if (file == null) {
            return;
        }
        EditorSession existing = findSession(file);
        if (existing != null) {
            selectSession(existing);
            return;
        }
        try {
            addSession(new EditorSession(file.getFileName().toString(), file, documentService.read(file), false));
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
            editor.replaceText(documentService.read(currentFile));
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
            currentFile = ensureYangExtension(currentFile);
            documentService.write(currentFile, editor.getText());
            dirty = false;
            syncCurrentSession();
            updateTitle();
        } catch (IOException ex) {
            showError("Не удалось сохранить файл", ex.getMessage());
        }
    }

    private void saveDocumentAs() {
        FileChooser chooser = yangChooser("Сохранить YANG модель");
        chooser.setInitialFileName(defaultYangFileName());
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        currentFile = ensureYangExtension(file);
        saveDocument();
    }

    private void exportDocument() {
        FileChooser chooser = yangChooser("Экспортировать YANG модель");
        chooser.setInitialFileName(defaultYangFileName());
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        try {
            documentService.write(ensureYangExtension(file), editor.getText());
        } catch (IOException ex) {
            showError("Не удалось экспортировать файл", ex.getMessage());
        }
    }

    private void exportXmlSample() {
        YangParseResult result = documentService.parse(editor.getText(), currentFile);
        setDocument(result.document(), captureTreeState());
        if (!result.errors().isEmpty()) {
            messages.getItems().setAll(result.errors().stream()
                    .map(error -> new UiMessage(error, "", 0))
                    .toList());
            showError("XML не экспортирован", "Сначала исправьте ошибки разбора YANG-модели.");
            return;
        }
        FileChooser chooser = xmlChooser("Экспортировать XML-пример");
        chooser.setInitialFileName(defaultXmlFileName());
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        try {
            Files.writeString(ensureXmlExtension(file), xmlSampleGenerator.generate(result.document()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            showError("Не удалось экспортировать XML", ex.getMessage());
        }
    }

    private void validateDocument() {
        TreeState treeState = captureTreeState();
        YangParseResult result = documentService.parse(editor.getText(), currentFile);
        List<UiMessage> validationMessages = validator.validate(editor.getText(), currentFile).stream()
                .map(this::formatIssue)
                .collect(Collectors.toCollection(java.util.ArrayList::new));
        List<UiMessage> parseErrors = result.errors().stream()
                .map(error -> new UiMessage(error, "", 0))
                .toList();
        validationMessages.addAll(0, parseErrors);
        if (validationMessages.isEmpty()) {
            validationMessages.add(new UiMessage("Ошибок не найдено.", "", 0));
        }
        setDocument(result.document(), treeState);
        messages.getItems().setAll(validationMessages);
    }

    private void parseAndRefresh() {
        TreeState treeState = captureTreeState();
        YangParseResult result = documentService.parse(editor.getText(), currentFile);
        setDocument(result.document(), treeState);
        messages.getItems().setAll(result.errors().stream()
                .map(error -> new UiMessage(error, "", 0))
                .toList());
        syncCurrentSession();
    }

    private void setDocument(YangDocument document) {
        setDocument(document, TreeState.empty());
    }

    private void setDocument(YangDocument document, TreeState treeState) {
        currentDocument = document;
        TreeItem<YangNode> rootItem = toTreeItem(document.root());
        nodeTree.setRoot(rootItem);
        restoreTreeState(treeState);
    }

    private TreeItem<YangNode> toTreeItem(YangNode node) {
        TreeItem<YangNode> item = new TreeItem<>(node);
        for (YangNode child : node.children()) {
            item.getChildren().add(toTreeItem(child));
        }
        return item;
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
        nodeDescription.setText(blank(node.description()));
        nodeConstraints.setText(formatConstraints(node.constraints()));
        setBooleanConstraintCheck(nodeConfig, node.constraints().get("config"));
        setBooleanConstraintCheck(nodeMandatory, node.constraints().get("mandatory"));
    }

    private void applySelectedNodeChanges() {
        if (selectedNode == null || currentDocument == null) {
            return;
        }
        selectedNode.setName(valueOrEmpty(nodeName.getText()));
        selectedNode.setDataType(valueOrEmpty(nodeDataType.getValue()));
        selectedNode.setDescription("-".equals(nodeDescription.getText()) ? "" : valueOrEmpty(nodeDescription.getText()));
        selectedNode.clearConstraints();
        applyBooleanConstraint(selectedNode, "config", nodeConfig);
        applyBooleanConstraint(selectedNode, "mandatory", nodeMandatory);
        applyConstraints(selectedNode, nodeConstraints.getText());
        regenerateTextFromModel();
    }

    private void addChild() {
        YangNodeType type = childType.getValue();
        if (selectedNode == null || currentDocument == null || type == null) {
            return;
        }
        YangNode parent = selectedNode;
        String name = valueOrEmpty(newNodeName.getText());
        YangNode child = new YangNode(type, name.isBlank() ? defaultName(type, parent) : name);
        if (type == YangNodeType.LEAF || type == YangNodeType.LEAF_LIST) {
            child.setDataType(valueOrEmpty(newNodeDataType.getValue()).isBlank()
                    ? "string"
                    : valueOrEmpty(newNodeDataType.getValue()));
        } else if (type == YangNodeType.TYPEDEF) {
            child.setDataType(valueOrEmpty(newNodeDataType.getValue()).isBlank()
                    ? "string"
                    : valueOrEmpty(newNodeDataType.getValue()));
        }
        child.setDescription(valueOrEmpty(newNodeDescription.getText()));
        applyBooleanConstraint(child, "config", newNodeConfig);
        applyBooleanConstraint(child, "mandatory", newNodeMandatory);
        applyConstraints(child, newNodeConstraints.getText());
        parent.addChild(child);
        clearNewNodeForm();
        regenerateTextFromModel();
    }

    private void clearNewNodeForm() {
        newNodeName.clear();
        newNodeDescription.clear();
        newNodeConstraints.clear();
        newNodeDataType.setValue("string");
        resetBooleanConstraintCheck(newNodeConfig);
        resetBooleanConstraintCheck(newNodeMandatory);
        childType.getSelectionModel().select(YangNodeType.LEAF);
    }

    private List<YangNodeType> addableTypes() {
        return List.of(
                YangNodeType.CONTAINER,
                YangNodeType.LIST,
                YangNodeType.LEAF,
                YangNodeType.LEAF_LIST,
                YangNodeType.CHOICE,
                YangNodeType.CASE,
                YangNodeType.GROUPING,
                YangNodeType.USES,
                YangNodeType.TYPEDEF,
                YangNodeType.IDENTITY,
                YangNodeType.FEATURE,
                YangNodeType.RPC,
                YangNodeType.ACTION,
                YangNodeType.INPUT,
                YangNodeType.OUTPUT,
                YangNodeType.NOTIFICATION,
                YangNodeType.AUGMENT,
                YangNodeType.ANYDATA,
                YangNodeType.ANYXML
        );
    }

    private void deleteSelectedNode() {
        if (selectedNode == null || selectedNode.parent().isEmpty()) {
            return;
        }
        YangNode parent = selectedNode.parent().get();
        parent.removeChild(selectedNode);
        selectedNode = parent;
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

    private void applyConstraints(YangNode node, String text) {
        if (text == null || text.isBlank() || "-".equals(text.strip())) {
            return;
        }
        for (String line : text.split("\\R")) {
            String clean = line.strip();
            if (clean.isEmpty()) {
                continue;
            }
            int separator = clean.indexOf(':');
            if (separator < 1) {
                messages.getItems().add(new UiMessage("Ограничение пропущено, нужен формат `keyword: value`: " + clean, "", 0));
                continue;
            }
            String keyword = clean.substring(0, separator).strip();
            String value = clean.substring(separator + 1).strip();
            if ("config".equals(keyword) || "mandatory".equals(keyword)) {
                messages.getItems().add(new UiMessage("Оператор `" + keyword + "` задается галочкой и пропущен из текстового поля.", "", 0));
                continue;
            }
            node.addConstraint(keyword, value);
        }
    }

    private String defaultName(YangNodeType type, YangNode parent) {
        if (type == YangNodeType.INPUT || type == YangNodeType.OUTPUT) {
            return "";
        }
        if (type == YangNodeType.AUGMENT) {
            return "\"/target\"";
        }
        String prefix = type.name().toLowerCase().replace('_', '-');
        int index = parent.children().size() + 1;
        return prefix + "-" + index;
    }

    private String formatConstraints(Map<String, List<String>> constraints) {
        if (constraints.isEmpty()) {
            return "";
        }
        return constraints.entrySet().stream()
                .filter(entry -> !"config".equals(entry.getKey()))
                .filter(entry -> !"mandatory".equals(entry.getKey()))
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private UiMessage formatIssue(ValidationIssue issue) {
        return new UiMessage(issue.severity() + ": " + issue.message(), issue.path(), issue.line());
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.strip();
    }

    private void configureBooleanConstraintCheck(CheckBox checkBox) {
        checkBox.setAllowIndeterminate(true);
        resetBooleanConstraintCheck(checkBox);
    }

    private void resetBooleanConstraintCheck(CheckBox checkBox) {
        checkBox.setIndeterminate(true);
        checkBox.setSelected(false);
    }

    private void setBooleanConstraintCheck(CheckBox checkBox, List<String> values) {
        if (values == null || values.isEmpty()) {
            resetBooleanConstraintCheck(checkBox);
            return;
        }
        String value = values.get(values.size() - 1);
        if ("true".equalsIgnoreCase(value)) {
            checkBox.setIndeterminate(false);
            checkBox.setSelected(true);
            return;
        }
        if ("false".equalsIgnoreCase(value)) {
            checkBox.setIndeterminate(false);
            checkBox.setSelected(false);
            return;
        }
        resetBooleanConstraintCheck(checkBox);
    }

    private void applyBooleanConstraint(YangNode node, String keyword, CheckBox checkBox) {
        if (!checkBox.isIndeterminate()) {
            node.addConstraint(keyword, Boolean.toString(checkBox.isSelected()));
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
            currentFile = ensureYangExtension(currentFile);
            documentService.write(currentFile, editor.getText());
            dirty = false;
            syncCurrentSession();
            updateTitle();
        } catch (IOException ex) {
            messages.getItems().add(new UiMessage("Автосохранение не выполнено: " + ex.getMessage(), "", 0));
        }
    }

    private Path ensureYangExtension(Path file) {
        String fileName = file.getFileName().toString();
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".yang")) {
            return file;
        }
        if (lower.endsWith(".txt")) {
            return file.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".yang");
        }
        return file.resolveSibling(fileName + ".yang");
    }

    private Path ensureXmlExtension(Path file) {
        String fileName = file.getFileName().toString();
        if (fileName.toLowerCase().endsWith(".xml")) {
            return file;
        }
        int extension = fileName.lastIndexOf('.');
        if (extension > 0) {
            return file.resolveSibling(fileName.substring(0, extension) + ".xml");
        }
        return file.resolveSibling(fileName + ".xml");
    }

    private String defaultYangFileName() {
        if (currentFile != null) {
            return ensureYangExtension(currentFile).getFileName().toString();
        }
        return "model.yang";
    }

    private String defaultXmlFileName() {
        if (currentFile == null) {
            return "sample.xml";
        }
        String fileName = currentFile.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        return (extension > 0 ? fileName.substring(0, extension) : fileName) + ".xml";
    }

    private String addIndent(String block) {
        StringBuilder result = new StringBuilder(block.length() + INDENT.length());
        boolean atLineStart = true;
        for (int index = 0; index < block.length(); index++) {
            if (atLineStart) {
                result.append(INDENT);
                atLineStart = false;
            }
            char symbol = block.charAt(index);
            result.append(symbol);
            if (symbol == '\n') {
                atLineStart = true;
            }
        }
        return result.toString();
    }

    private String removeIndent(String block) {
        StringBuilder result = new StringBuilder(block.length());
        int lineStart = 0;
        while (lineStart < block.length()) {
            int lineEnd = lineEnd(block, lineStart);
            String line = block.substring(lineStart, lineEnd);
            if (line.startsWith(INDENT)) {
                result.append(line.substring(INDENT.length()));
            } else if (line.startsWith("\t")) {
                result.append(line.substring(1));
            } else {
                result.append(line);
            }
            if (lineEnd < block.length()) {
                if (block.charAt(lineEnd) == '\r' && lineEnd + 1 < block.length() && block.charAt(lineEnd + 1) == '\n') {
                    result.append("\r\n");
                    lineStart = lineEnd + 2;
                } else {
                    result.append(block.charAt(lineEnd));
                    lineStart = lineEnd + 1;
                }
            } else {
                lineStart = lineEnd;
            }
        }
        return result.toString();
    }

    private int lineStart(String text, int position) {
        int cursor = Math.min(position, text.length());
        while (cursor > 0) {
            char previous = text.charAt(cursor - 1);
            if (previous == '\n' || previous == '\r') {
                break;
            }
            cursor--;
        }
        return cursor;
    }

    private int lineEnd(String text, int position) {
        int cursor = Math.min(position, text.length());
        while (cursor < text.length()) {
            char symbol = text.charAt(cursor);
            if (symbol == '\n' || symbol == '\r') {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private String leadingWhitespace(String text) {
        int index = 0;
        while (index < text.length()) {
            char symbol = text.charAt(index);
            if (symbol != ' ' && symbol != '\t') {
                break;
            }
            index++;
        }
        return text.substring(0, index).replace("\t", INDENT);
    }

    private String lineSeparator(String text) {
        return text.contains("\r\n") ? "\r\n" : "\n";
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

    private Label accessModeLabel(YangNode node) {
        boolean readOnly = isReadOnly(node);
        Label label = new Label(readOnly ? "RO" : "RW");
        label.setMinWidth(26);
        label.setStyle(readOnly
                ? "-fx-background-color: #eef1f5; -fx-text-fill: #46515f; -fx-font-size: 10px; -fx-padding: 1 4 1 4;"
                : "-fx-background-color: #e7f5ec; -fx-text-fill: #1f6b3a; -fx-font-size: 10px; -fx-padding: 1 4 1 4;");
        return label;
    }

    private boolean isReadOnly(YangNode node) {
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

    private void showInstruction() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Инструкция");
        dialog.setHeaderText("YANG Model Designer");
        dialog.initOwner(stage);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        ScrollPane scrollPane = new ScrollPane(markdownView(readInstructionText()));
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportWidth(900);
        scrollPane.setPrefViewportHeight(680);
        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    private VBox markdownView(String markdown) {
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        boolean codeBlock = false;
        StringBuilder code = new StringBuilder();
        for (String line : markdown.split("\\R", -1)) {
            if (line.startsWith("```")) {
                if (codeBlock) {
                    content.getChildren().add(codeBlock(code.toString().stripTrailing()));
                    code.setLength(0);
                }
                codeBlock = !codeBlock;
                continue;
            }
            if (codeBlock) {
                code.append(line).append(System.lineSeparator());
                continue;
            }
            addMarkdownLine(content, line);
        }
        if (codeBlock && !code.isEmpty()) {
            content.getChildren().add(codeBlock(code.toString().stripTrailing()));
        }
        return content;
    }

    private void addMarkdownLine(VBox content, String line) {
        if (line.isBlank()) {
            content.getChildren().add(new Label(""));
            return;
        }
        String text = line.strip();
        if (text.startsWith("## ")) {
            Label label = new Label(text.substring(3));
            label.setFont(Font.font("System", FontWeight.BOLD, 18));
            label.setPadding(new Insets(10, 0, 2, 0));
            content.getChildren().add(label);
            return;
        }
        if (text.startsWith("# ")) {
            Label label = new Label(text.substring(2));
            label.setFont(Font.font("System", FontWeight.BOLD, 22));
            label.setPadding(new Insets(0, 0, 4, 0));
            content.getChildren().add(label);
            return;
        }
        if (text.matches("\\d+\\.\\s+.*")) {
            content.getChildren().add(markdownTextFlow(text));
            return;
        }
        if (text.startsWith("- ")) {
            content.getChildren().add(markdownTextFlow("• " + text.substring(2)));
            return;
        }
        content.getChildren().add(markdownTextFlow(text));
    }

    private TextFlow markdownTextFlow(String line) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(2);
        Matcher matcher = Pattern.compile("`([^`]+)`").matcher(line);
        int cursor = 0;
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                flow.getChildren().add(normalText(line.substring(cursor, matcher.start())));
            }
            Text code = normalText(matcher.group(1));
            code.setFont(Font.font("Consolas", FontWeight.NORMAL, 13));
            code.setStyle("-fx-fill: #0f172a;");
            flow.getChildren().add(code);
            cursor = matcher.end();
        }
        if (cursor < line.length()) {
            flow.getChildren().add(normalText(line.substring(cursor)));
        }
        return flow;
    }

    private Text normalText(String value) {
        Text text = new Text(value);
        text.setFont(Font.font("System", FontPosture.REGULAR, 14));
        return text;
    }

    private TextFlow codeBlock(String value) {
        TextFlow flow = new TextFlow();
        Text text = new Text(value.isBlank() ? " " : value);
        text.setFont(Font.font("Consolas", FontWeight.NORMAL, 13));
        flow.getChildren().add(text);
        flow.setPadding(new Insets(8));
        flow.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-radius: 4; -fx-background-radius: 4;");
        return flow;
    }

    private String readInstructionText() {
        for (Path path : instructionCandidates()) {
            if (Files.isRegularFile(path)) {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    return "Не удалось прочитать файл инструкции: " + path;
                }
            }
        }
        return "Файл инструкции YangDesignerInstruction.md не найден.";
    }

    private List<Path> instructionCandidates() {
        return List.of(
                Path.of("YangDesignerInstruction.md"),
                applicationDirectory().map(path -> path.resolve("YangDesignerInstruction.md")).orElse(Path.of(""))
        );
    }

    private Optional<Path> applicationDirectory() {
        try {
            Path location = Path.of(MainView.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Optional.of(Files.isDirectory(location) ? location : location.getParent());
        } catch (URISyntaxException | RuntimeException ex) {
            return Optional.empty();
        }
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
        String fileName = currentSession == null
                ? "Новая модель"
                : currentSession.displayName();
        if (currentSession != null) {
            currentSession.file = currentFile;
            currentSession.dirty = dirty;
            updateTabTitle(currentSession);
        }
        stage.setTitle((dirty ? "* " : "") + fileName + " - YANG Model Designer");
    }

    private void highlightEditor() {
        editor.setStyleSpans(0, computeHighlighting(editor.getText()));
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Set<Integer> matchingBraces = matchingBracePositions(text, editor.getCaretPosition());
        Matcher matcher = YANG_HIGHLIGHT_PATTERN.matcher(text);
        int lastKeywordEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            Collection<String> styleClasses = styleClasses(matcher, matchingBraces, matcher.start());
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKeywordEnd);
            spansBuilder.add(styleClasses, matcher.end() - matcher.start());
            lastKeywordEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKeywordEnd);
        return spansBuilder.create();
    }

    private Collection<String> styleClasses(Matcher matcher, Set<Integer> matchingBraces, int start) {
        String styleClass = styleClass(matcher);
        if ("brace".equals(styleClass) && matchingBraces.contains(start)) {
            return List.of("brace", "matching-brace");
        }
        return Collections.singleton(styleClass);
    }

    private String styleClass(Matcher matcher) {
        if (matcher.group("COMMENT") != null) {
            return "comment";
        }
        if (matcher.group("STRING") != null) {
            return "string";
        }
        if (matcher.group("KEYWORD") != null) {
            return "keyword";
        }
        if (matcher.group("TYPE") != null) {
            return "type";
        }
        if (matcher.group("BRACE") != null) {
            return "brace";
        }
        return "semicolon";
    }

    private Set<Integer> matchingBracePositions(String text, int caret) {
        int bracePosition = bracePositionNearCaret(text, caret);
        if (bracePosition < 0) {
            return Set.of();
        }
        int matchPosition = matchingBracePosition(text, bracePosition);
        if (matchPosition < 0) {
            return Set.of(bracePosition);
        }
        return Set.of(bracePosition, matchPosition);
    }

    private int bracePositionNearCaret(String text, int caret) {
        if (caret < text.length() && isBrace(text.charAt(caret))) {
            return caret;
        }
        if (caret > 0 && isBrace(text.charAt(caret - 1))) {
            return caret - 1;
        }
        return -1;
    }

    private int matchingBracePosition(String text, int bracePosition) {
        char brace = text.charAt(bracePosition);
        if (brace == '{') {
            int depth = 0;
            for (int index = bracePosition; index < text.length(); index++) {
                char symbol = text.charAt(index);
                if (symbol == '{') {
                    depth++;
                } else if (symbol == '}') {
                    depth--;
                    if (depth == 0) {
                        return index;
                    }
                }
            }
            return -1;
        }
        int depth = 0;
        for (int index = bracePosition; index >= 0; index--) {
            char symbol = text.charAt(index);
            if (symbol == '}') {
                depth++;
            } else if (symbol == '{') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private boolean isBrace(char symbol) {
        return symbol == '{' || symbol == '}';
    }

    private void selectNodeByPath(String path) {
        TreeItem<YangNode> item = findByPath(nodeTree.getRoot(), path);
        if (item == null) {
            return;
        }
        expandParents(item);
        nodeTree.getSelectionModel().select(item);
        nodeTree.scrollTo(nodeTree.getRow(item));
    }

    private TreeState captureTreeState() {
        TreeItem<YangNode> rootItem = nodeTree.getRoot();
        if (rootItem == null) {
            return TreeState.empty();
        }
        Set<String> expandedPaths = new HashSet<>();
        collectExpandedPaths(rootItem, expandedPaths);
        String selectedPath = selectedNode == null ? rootItem.getValue().path() : selectedNode.path();
        return new TreeState(expandedPaths, selectedPath);
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

    private void restoreTreeState(TreeState treeState) {
        TreeItem<YangNode> rootItem = nodeTree.getRoot();
        if (rootItem == null) {
            return;
        }
        restoringTree = true;
        restoreExpandedPaths(rootItem, treeState.expandedPaths());
        TreeItem<YangNode> selectedItem = findByPath(rootItem, treeState.selectedPath());
        if (selectedItem == null) {
            selectedItem = rootItem;
        }
        expandParents(selectedItem);
        nodeTree.getSelectionModel().select(selectedItem);
        showNode(selectedItem.getValue());
        restoringTree = false;
        showNode(selectedItem.getValue());
    }

    private void restoreExpandedPaths(TreeItem<YangNode> item, Set<String> expandedPaths) {
        item.setExpanded(expandedPaths.contains(item.getValue().path()) || item.getParent() == null);
        for (TreeItem<YangNode> child : item.getChildren()) {
            restoreExpandedPaths(child, expandedPaths);
        }
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
        int position = offsetForLine(editor.getText(), line);
        editor.moveTo(position);
        editor.showParagraphAtCenter(Math.max(0, line - 1));
    }

    private int offsetForLine(String text, int line) {
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

    private TreeItem<YangNode> findByPath(TreeItem<YangNode> item, String path) {
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

    private void expandParents(TreeItem<YangNode> item) {
        TreeItem<YangNode> cursor = item;
        while (cursor != null) {
            cursor.setExpanded(true);
            cursor = cursor.getParent();
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR, content, ButtonType.OK);
        alert.setHeaderText(header);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private record UiMessage(String text, String path, int line) {
        @Override
        public String toString() {
            return text;
        }
    }

    private static final class EditorSession {
        private final String untitledName;
        private Path file;
        private String text;
        private boolean dirty;
        private TreeState treeState = TreeState.empty();
        private Tab tab;

        private EditorSession(String untitledName, Path file, String text, boolean dirty) {
            this.untitledName = untitledName;
            this.file = file;
            this.text = text;
            this.dirty = dirty;
        }

        private String displayName() {
            return file == null ? untitledName : file.getFileName().toString();
        }
    }

    private record TreeState(Set<String> expandedPaths, String selectedPath) {
        private static TreeState empty() {
            return new TreeState(Set.of(), "");
        }
    }
}
