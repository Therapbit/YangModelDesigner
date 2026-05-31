package com.yangdesigner.yangmodeldesigner.ui.editor;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class YangCompletionSupport {
    private static final List<String> KEYWORDS = List.of(
            "module",
            "submodule",
            "yang-version",
            "namespace",
            "prefix",
            "import",
            "include",
            "revision",
            "organization",
            "contact",
            "description",
            "reference",
            "container",
            "list",
            "leaf",
            "leaf-list",
            "choice",
            "case",
            "grouping",
            "uses",
            "augment",
            "rpc",
            "action",
            "input",
            "output",
            "notification",
            "typedef",
            "type",
            "default",
            "config",
            "mandatory",
            "must",
            "when",
            "key",
            "unique",
            "presence",
            "min-elements",
            "max-elements",
            "ordered-by",
            "status",
            "if-feature",
            "feature",
            "identity",
            "base",
            "extension",
            "argument",
            "yin-element",
            "deviation",
            "deviate",
            "anydata",
            "anyxml",
            "enum",
            "bit",
            "units",
            "range",
            "length",
            "pattern",
            "error-message",
            "error-app-tag",
            "value",
            "position",
            "require-instance"
    );
    private static final List<String> TYPES = List.of(
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
            "instance-identifier",
            "union"
    );
    private static final List<String> STATEMENT_VALUES = List.of(
            "true",
            "false",
            "current",
            "deprecated",
            "obsolete",
            "system",
            "user"
    );
    private static final List<String> ALL = Stream.of(KEYWORDS, TYPES, STATEMENT_VALUES)
            .flatMap(List::stream)
            .distinct()
            .sorted()
            .toList();

    public List<String> suggestions(String text, int caret, int limit) {
        String prefix = prefix(text, caret);
        if (prefix.isBlank()) {
            return List.of();
        }
        String cleanPrefix = prefix.toLowerCase(Locale.ROOT);
        return ALL.stream()
                .filter(item -> item.startsWith(cleanPrefix))
                .limit(Math.max(1, limit))
                .toList();
    }

    public String prefix(String text, int caret) {
        int safeCaret = Math.max(0, Math.min(caret, text == null ? 0 : text.length()));
        if (text == null || safeCaret == 0) {
            return "";
        }
        int start = safeCaret;
        while (start > 0 && isCompletionCharacter(text.charAt(start - 1))) {
            start--;
        }
        return text.substring(start, safeCaret);
    }

    public int prefixStart(String text, int caret) {
        return caret - prefix(text, caret).length();
    }

    public String insertionText(String completion) {
        return KEYWORDS.contains(completion) ? completion + " " : completion;
    }

    private boolean isCompletionCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '-' || value == '_' || value == ':';
    }
}
