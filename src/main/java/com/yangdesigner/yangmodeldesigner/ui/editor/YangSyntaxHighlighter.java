package com.yangdesigner.yangmodeldesigner.ui.editor;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YangSyntaxHighlighter {
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

    public StyleSpans<Collection<String>> compute(String text, int caretPosition) {
        Set<Integer> matchingBraces = matchingBracePositions(text, caretPosition);
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
        if (caret < 0) {
            return -1;
        }
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
}
