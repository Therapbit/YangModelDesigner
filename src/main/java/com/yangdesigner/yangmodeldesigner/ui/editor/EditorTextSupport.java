package com.yangdesigner.yangmodeldesigner.ui.editor;

public final class EditorTextSupport {
    public static final String INDENT = "    ";

    private EditorTextSupport() {
    }

    public static String addIndent(String block) {
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

    public static String removeIndent(String block) {
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

    public static IndentedNewLine indentedNewLine(String text, int caret) {
        int start = lineStart(text, caret);
        int end = lineEnd(text, caret);
        String beforeCaret = text.substring(start, caret);
        String afterCaret = text.substring(caret, end);
        String baseIndent = leadingWhitespace(beforeCaret);
        String nextIndent = beforeCaret.stripTrailing().endsWith("{") ? baseIndent + INDENT : baseIndent;
        String lineSeparator = lineSeparator(text);

        if (nextIndent.length() > baseIndent.length() && afterCaret.stripLeading().startsWith("}")) {
            String insertion = lineSeparator + nextIndent + lineSeparator + baseIndent;
            return new IndentedNewLine(insertion, caret + lineSeparator.length() + nextIndent.length());
        }
        return new IndentedNewLine(lineSeparator + nextIndent, -1);
    }

    public static int lineStart(String text, int position) {
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

    public static int lineEnd(String text, int position) {
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

    public static String leadingWhitespace(String text) {
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

    public static String lineSeparator(String text) {
        return text.contains("\r\n") ? "\r\n" : "\n";
    }

    public record IndentedNewLine(String insertion, int caretPosition) {
    }
}
