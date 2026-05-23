package com.yangdesigner.yangmodeldesigner.ui;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.parser.YangParseResult;
import com.yangdesigner.yangmodeldesigner.service.YangDocumentService;
import com.yangdesigner.yangmodeldesigner.validation.ValidationIssue;
import com.yangdesigner.yangmodeldesigner.validation.YangValidator;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
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
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

public final class MainView {
    private static final String INDENT = "  ";
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
    private final YangValidator validator = new YangValidator();
    private final BorderPane root = new BorderPane();
    private final TreeView<YangNode> nodeTree = new TreeView<>();
    private final CodeArea editor = new CodeArea();
    private final TextField nodeName = new TextField();
    private final Label nodeType = new Label("-");
    private final Label nodePath = new Label("-");
    private final ComboBox<String> nodeDataType = new ComboBox<>();
    private final TextArea nodeDescription = new TextArea();
    private final TextArea nodeConstraints = new TextArea();
    private final ComboBox<YangNodeType> childType = new ComboBox<>();
    private final TextField newNodeName = new TextField();
    private final ComboBox<String> newNodeDataType = new ComboBox<>();
    private final TextArea newNodeDescription = new TextArea();
    private final TextArea newNodeConstraints = new TextArea();
    private final ListView<UiMessage> messages = new ListView<>();
    private final PauseTransition parseDelay = new PauseTransition(Duration.millis(650));
    private URL editorCss;
    private Path currentFile;
    private YangDocument currentDocument;
    private YangNode selectedNode;
    private boolean restoringTree;
    private boolean dirty;
    private boolean updatingEditor;

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

        SplitPane workArea = new SplitPane(nodeTree, editor, propertiesPane());
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
        MenuItem validate = item("Проверить", "Shortcut+R", this::validateDocument);
        MenuItem instruction = item("Инструкция", "F1", this::showInstruction);

        Menu file = new Menu("Файл");
        file.getItems().addAll(newFile, open, reload, new SeparatorMenuItem(), save, saveAs, export);

        Menu tools = new Menu("Инструменты");
        tools.getItems().add(validate);

        Menu help = new Menu("Помощь");
        help.getItems().add(instruction);

        return new MenuBar(file, tools, help);
    }

    private MenuItem item(String text, String shortcut, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setAccelerator(KeyCombination.keyCombination(shortcut));
        item.setOnAction(event -> action.run());
        return item;
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

        newNodeConstraints.setWrapText(true);
        newNodeConstraints.setPrefRowCount(8);
        newNodeConstraints.setPromptText("config: true\nmandatory: false");

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
                newNodeConstraints,
                addChild);
        pane.setPadding(new Insets(12));
        pane.setMinWidth(340);
        VBox.setVgrow(newNodeConstraints, Priority.ALWAYS);
        return pane;
    }

    private Label section(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private void configureActions() {
        parseDelay.setOnFinished(event -> parseAndRefresh());
        editor.addEventFilter(KeyEvent.KEY_PRESSED, this::handleEditorKeyPressed);
        editor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingEditor) {
                return;
            }
            dirty = true;
            highlightEditor();
            updateTitle();
            parseDelay.playFromStart();
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
                }
            }
        });
    }

    private void handleEditorKeyPressed(KeyEvent event) {
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

    private void createNewDocument() {
        currentFile = null;
        editor.replaceText(documentService.newModuleTemplate());
        dirty = false;
        parseAndRefresh();
        highlightEditor();
        updateTitle();
    }

    private void openDocument() {
        FileChooser chooser = yangChooser("Открыть YANG модель");
        Path file = selectedPath(chooser.showOpenDialog(stage));
        if (file == null) {
            return;
        }
        try {
            editor.replaceText(documentService.read(file));
            currentFile = file;
            dirty = false;
            parseAndRefresh();
            highlightEditor();
            updateTitle();
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
            editor.replaceText(documentService.read(currentFile));
            dirty = false;
            parseAndRefresh();
            highlightEditor();
            updateTitle();
        } catch (IOException ex) {
            showError("Не удалось перезагрузить файл", ex.getMessage());
        }
    }

    private void saveDocument() {
        if (currentFile == null) {
            saveDocumentAs();
            return;
        }
        try {
            documentService.write(currentFile, editor.getText());
            dirty = false;
            updateTitle();
        } catch (IOException ex) {
            showError("Не удалось сохранить файл", ex.getMessage());
        }
    }

    private void saveDocumentAs() {
        FileChooser chooser = yangChooser("Сохранить YANG модель");
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        currentFile = file;
        saveDocument();
    }

    private void exportDocument() {
        FileChooser chooser = yangChooser("Экспортировать YANG модель");
        Path file = selectedPath(chooser.showSaveDialog(stage));
        if (file == null) {
            return;
        }
        try {
            documentService.write(file, editor.getText());
        } catch (IOException ex) {
            showError("Не удалось экспортировать файл", ex.getMessage());
        }
    }

    private void validateDocument() {
        TreeState treeState = captureTreeState();
        YangParseResult result = documentService.parse(editor.getText(), currentFile);
        List<UiMessage> validationMessages = validator.validate(result.document()).stream()
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
    }

    private void applySelectedNodeChanges() {
        if (selectedNode == null || currentDocument == null) {
            return;
        }
        selectedNode.setName(valueOrEmpty(nodeName.getText()));
        selectedNode.setDataType(valueOrEmpty(nodeDataType.getValue()));
        selectedNode.setDescription("-".equals(nodeDescription.getText()) ? "" : valueOrEmpty(nodeDescription.getText()));
        selectedNode.clearConstraints();
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

        TextArea content = new TextArea(readInstructionText());
        content.setEditable(false);
        content.setWrapText(true);
        content.setPrefColumnCount(100);
        content.setPrefRowCount(34);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
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

    private Path selectedPath(java.io.File file) {
        return file == null ? null : file.toPath();
    }

    private void updateTitle() {
        String fileName = currentFile == null ? "Новая модель" : currentFile.getFileName().toString();
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
        int position = offsetForLine(editor.getText(), node.line());
        editor.moveTo(position);
        editor.requestFollowCaret();
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

    private record TreeState(Set<String> expandedPaths, String selectedPath) {
        private static TreeState empty() {
            return new TreeState(Set.of(), "");
        }
    }
}
