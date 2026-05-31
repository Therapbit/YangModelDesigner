package com.yangdesigner.yangmodeldesigner.ui.view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownViewFactory {
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+)`");

    public VBox create(String markdown) {
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        boolean codeBlock = false;
        StringBuilder code = new StringBuilder();
        for (String line : markdown.split("\\R")) {
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
            content.getChildren().add(markdownTextFlow("* " + text.substring(2)));
            return;
        }
        content.getChildren().add(markdownTextFlow(text));
    }

    private TextFlow markdownTextFlow(String line) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(2);
        Matcher matcher = INLINE_CODE.matcher(line);
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
}
