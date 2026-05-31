package com.yangdesigner.yangmodeldesigner.ui.controller;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FindReplaceController {
    public Optional<TextRange> findNext(String text, String query, boolean caseSensitive, int caret, int selectionEnd) {
        String cleanQuery = cleanQuery(query);
        if (cleanQuery.isEmpty()) {
            return Optional.empty();
        }
        String haystack = caseSensitive ? text : text.toLowerCase();
        String needle = caseSensitive ? cleanQuery : cleanQuery.toLowerCase();
        int start = Math.max(caret, selectionEnd);
        int index = haystack.indexOf(needle, start);
        if (index < 0 && start > 0) {
            index = haystack.indexOf(needle);
        }
        return range(index, cleanQuery.length());
    }

    public Optional<TextRange> findPrevious(String text, String query, boolean caseSensitive, int selectionStart) {
        String cleanQuery = cleanQuery(query);
        if (cleanQuery.isEmpty()) {
            return Optional.empty();
        }
        String haystack = caseSensitive ? text : text.toLowerCase();
        String needle = caseSensitive ? cleanQuery : cleanQuery.toLowerCase();
        int start = Math.max(0, selectionStart - 1);
        int index = haystack.lastIndexOf(needle, start);
        if (index < 0 && start < haystack.length()) {
            index = haystack.lastIndexOf(needle);
        }
        return range(index, cleanQuery.length());
    }

    public boolean matches(String selectedText, String query, boolean caseSensitive) {
        String cleanQuery = cleanQuery(query);
        if (selectedText == null || cleanQuery.isEmpty()) {
            return false;
        }
        return caseSensitive ? selectedText.equals(cleanQuery) : selectedText.equalsIgnoreCase(cleanQuery);
    }

    public String replaceAll(String text, String query, String replacement, boolean caseSensitive) {
        String cleanQuery = cleanQuery(query);
        if (cleanQuery.isEmpty()) {
            return text;
        }
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        Pattern pattern = Pattern.compile(Pattern.quote(cleanQuery), flags);
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll(Matcher.quoteReplacement(replacement == null ? "" : replacement));
    }

    private Optional<TextRange> range(int start, int length) {
        if (start < 0) {
            return Optional.empty();
        }
        return Optional.of(new TextRange(start, start + length));
    }

    private String cleanQuery(String query) {
        return query == null ? "" : query;
    }

    public record TextRange(int start, int end) {
    }
}
