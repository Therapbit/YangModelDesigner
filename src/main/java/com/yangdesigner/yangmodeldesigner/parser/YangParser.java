package com.yangdesigner.yangmodeldesigner.parser;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;
import com.yangdesigner.yangmodeldesigner.model.YangNode;
import com.yangdesigner.yangmodeldesigner.model.YangNodeType;
import com.yangdesigner.yangmodeldesigner.model.YangStatement;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

public final class YangParser {
    private static final Set<String> NODE_KEYWORDS = Set.of(
            "module", "submodule", "import", "include", "revision", "extension",
            "feature", "identity", "typedef", "container", "list", "leaf",
            "leaf-list", "choice", "case", "grouping", "uses", "augment",
            "rpc", "action", "input", "output", "notification", "deviation",
            "deviate", "anydata", "anyxml", "enum", "bit"
    );
    private static final Set<String> CONSTRAINT_KEYWORDS = Set.of(
            "key", "when", "must", "mandatory", "default", "config", "presence",
            "min-elements", "max-elements", "pattern", "range", "length", "units",
            "base", "if-feature", "status", "reference", "organization", "contact",
            "belongs-to", "revision-date", "prefix", "value", "position"
    );

    public YangParseResult parse(String source, Path file) {
        String text = source == null ? "" : source;
        List<Token> tokens = tokenize(text);
        List<String> errors = new ArrayList<>();
        YangStatement statementRoot = buildAst(tokens, errors);
        YangStatement firstRootStatement = statementRoot.children().isEmpty() ? statementRoot : statementRoot.children().getFirst();
        YangNode root = toNode(firstRootStatement);

        return new YangParseResult(new YangDocument(root, firstRootStatement, text, file), errors);
    }

    private YangStatement buildAst(List<Token> tokens, List<String> errors) {
        YangStatement syntheticRoot = new YangStatement("document", "", 0);
        Deque<YangStatement> statementStack = new ArrayDeque<>();
        statementStack.push(syntheticRoot);

        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if ("}".equals(token.value())) {
                if (statementStack.size() > 1) {
                    statementStack.pop();
                } else {
                    errors.add("Строка " + token.line() + ": лишняя закрывающая скобка.");
                }
                continue;
            }
            if ("{".equals(token.value()) || ";".equals(token.value())) {
                continue;
            }

            String keyword = token.value();
            Statement statement = readStatement(tokens, index);
            index = statement.nextIndex();

            YangStatement child = new YangStatement(keyword, statement.argument(), token.line());
            statementStack.peek().addChild(child);
            if (statement.opensBlock()) {
                statementStack.push(child);
            }
        }

        if (statementStack.size() > 1) {
            errors.add("Не все блоки закрыты фигурными скобками.");
        }
        return syntheticRoot;
    }

    private YangNode toNode(YangStatement statement) {
        YangNodeType type = YangNodeType.fromKeyword(statement.keyword());
        YangNode node = new YangNode(type, statement.argument());
        node.setLine(statement.line());
        for (YangStatement childStatement : statement.children()) {
            if (NODE_KEYWORDS.contains(childStatement.keyword())) {
                node.addParsedChild(toNode(childStatement));
            } else if ("description".equals(childStatement.keyword())) {
                node.setDescription(childStatement.argument());
            } else if ("type".equals(childStatement.keyword())) {
                node.setDataType(childStatement.argument());
            } else if (isConstraint(childStatement.keyword(), type)) {
                node.addParsedConstraint(childStatement.keyword(), childStatement.argument());
            }
        }
        node.setStatement(statement);
        return node;
    }

    private boolean isConstraint(String keyword, YangNodeType currentType) {
        return CONSTRAINT_KEYWORDS.contains(keyword)
                || (("namespace".equals(keyword) || "prefix".equals(keyword) || "yang-version".equals(keyword))
                && currentType == YangNodeType.MODULE);
    }

    private Statement readStatement(List<Token> tokens, int keywordIndex) {
        StringBuilder argument = new StringBuilder();
        boolean opensBlock = false;
        int index = keywordIndex + 1;
        while (index < tokens.size()) {
            String value = tokens.get(index).value();
            if ("{".equals(value)) {
                opensBlock = true;
                break;
            }
            if (";".equals(value) || "}".equals(value)) {
                break;
            }
            if (!argument.isEmpty()) {
                argument.append(' ');
            }
            argument.append(value);
            index++;
        }
        return new Statement(argument.toString(), opensBlock, index);
    }

    private List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int line = 1;
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '\n') {
                line++;
                index++;
                continue;
            }
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '/') {
                index += 2;
                while (index < source.length() && source.charAt(index) != '\n') {
                    index++;
                }
                continue;
            }
            if (current == '/' && index + 1 < source.length() && source.charAt(index + 1) == '*') {
                index += 2;
                while (index + 1 < source.length()
                        && !(source.charAt(index) == '*' && source.charAt(index + 1) == '/')) {
                    if (source.charAt(index) == '\n') {
                        line++;
                    }
                    index++;
                }
                index = Math.min(index + 2, source.length());
                continue;
            }
            if (current == '{' || current == '}' || current == ';') {
                tokens.add(new Token(String.valueOf(current), line));
                index++;
                continue;
            }
            if (current == '"' || current == '\'') {
                char quote = current;
                int startLine = line;
                index++;
                StringBuilder value = new StringBuilder();
                while (index < source.length() && source.charAt(index) != quote) {
                    if (source.charAt(index) == '\n') {
                        line++;
                    }
                    value.append(source.charAt(index));
                    index++;
                }
                index = Math.min(index + 1, source.length());
                tokens.add(new Token(value.toString(), startLine));
                continue;
            }
            int start = index;
            while (index < source.length()
                    && !Character.isWhitespace(source.charAt(index))
                    && "{};\"'".indexOf(source.charAt(index)) < 0) {
                index++;
            }
            tokens.add(new Token(source.substring(start, index), line));
        }
        return tokens;
    }

    private record Token(String value, int line) {
    }

    private record Statement(String argument, boolean opensBlock, int nextIndex) {
    }
}
