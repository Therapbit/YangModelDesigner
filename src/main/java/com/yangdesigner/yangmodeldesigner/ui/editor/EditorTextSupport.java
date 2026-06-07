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

    public static CommentToggle toggleLineComment(String text, int selectionStart, int selectionEnd) {
        String source = text == null ? "" : text;
        int safeStart = Math.max(0, Math.min(selectionStart, source.length()));
        int safeEnd = Math.max(0, Math.min(selectionEnd, source.length()));
        if (safeStart > safeEnd) {
            int swap = safeStart;
            safeStart = safeEnd;
            safeEnd = swap;
        }
        int start = lineStart(source, safeStart);
        int endPosition = safeEnd;
        if (safeEnd > safeStart && isLineStart(source, safeEnd)) {
            endPosition = safeEnd - 1;
        }
        int end = lineEnd(source, endPosition);
        String block = source.substring(start, end);
        String replacement = allNonBlankLinesCommented(block) ? uncommentLines(block) : commentLines(block);
        return new CommentToggle(start, end, replacement);
    }

    public static BracePair bracePair(String text, int selectionStart, int selectionEnd) {
        String source = text == null ? "" : text;
        int safeStart = Math.max(0, Math.min(selectionStart, source.length()));
        int safeEnd = Math.max(0, Math.min(selectionEnd, source.length()));
        if (safeStart > safeEnd) {
            int swap = safeStart;
            safeStart = safeEnd;
            safeEnd = swap;
        }
        String selectedText = source.substring(safeStart, safeEnd);
        String replacement = "{" + selectedText + "}";
        int caret = safeStart + 1;
        int selectionEndAfterInsert = safeStart + replacement.length() - 1;
        return new BracePair(safeStart, safeEnd, replacement, caret, selectionEndAfterInsert);
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

    private static boolean isLineStart(String text, int position) {
        return position <= 0 || position <= text.length()
                && (text.charAt(position - 1) == '\n' || text.charAt(position - 1) == '\r');
    }

    private static boolean allNonBlankLinesCommented(String block) {
        boolean hasNonBlankLine = false;
        int lineStart = 0;
        while (lineStart <= block.length()) {
            int lineEnd = lineEnd(block, lineStart);
            String line = block.substring(lineStart, lineEnd);
            if (!line.isBlank()) {
                hasNonBlankLine = true;
                if (!line.stripLeading().startsWith("//")) {
                    return false;
                }
            }
            if (lineEnd >= block.length()) {
                break;
            }
            lineStart = nextLineStart(block, lineEnd);
        }
        return hasNonBlankLine;
    }

    private static String commentLines(String block) {
        return transformLines(block, false);
    }

    private static String uncommentLines(String block) {
        return transformLines(block, true);
    }

    private static String transformLines(String block, boolean uncomment) {
        StringBuilder result = new StringBuilder(block.length() + INDENT.length());
        int lineStart = 0;
        while (lineStart <= block.length()) {
            int lineEnd = lineEnd(block, lineStart);
            String line = block.substring(lineStart, lineEnd);
            result.append(uncomment ? uncommentLine(line) : commentLine(line));
            if (lineEnd >= block.length()) {
                break;
            }
            int nextLineStart = nextLineStart(block, lineEnd);
            result.append(block, lineEnd, nextLineStart);
            lineStart = nextLineStart;
        }
        return result.toString();
    }

    private static String commentLine(String line) {
        int indentEnd = leadingWhitespaceLength(line);
        return line.substring(0, indentEnd) + "//" + line.substring(indentEnd);
    }

    private static String uncommentLine(String line) {
        int indentEnd = leadingWhitespaceLength(line);
        if (!line.startsWith("//", indentEnd)) {
            return line;
        }
        int removeEnd = indentEnd + 2;
        if (removeEnd < line.length() && line.charAt(removeEnd) == ' ') {
            removeEnd++;
        }
        return line.substring(0, indentEnd) + line.substring(removeEnd);
    }

    private static int nextLineStart(String text, int lineEnd) {
        if (lineEnd < text.length() && text.charAt(lineEnd) == '\r' && lineEnd + 1 < text.length() && text.charAt(lineEnd + 1) == '\n') {
            return lineEnd + 2;
        }
        return Math.min(lineEnd + 1, text.length());
    }

    private static int leadingWhitespaceLength(String text) {
        int index = 0;
        while (index < text.length()) {
            char symbol = text.charAt(index);
            if (symbol != ' ' && symbol != '\t') {
                break;
            }
            index++;
        }
        return index;
    }

    public record IndentedNewLine(String insertion, int caretPosition) {
    }

    public record CommentToggle(int start, int end, String replacement) {
    }

    public record BracePair(int start, int end, String replacement, int caretPosition, int selectionEnd) {
    }
}
