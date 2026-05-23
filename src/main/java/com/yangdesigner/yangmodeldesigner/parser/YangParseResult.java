package com.yangdesigner.yangmodeldesigner.parser;

import com.yangdesigner.yangmodeldesigner.model.YangDocument;

import java.util.List;

public record YangParseResult(YangDocument document, List<String> errors) {
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
