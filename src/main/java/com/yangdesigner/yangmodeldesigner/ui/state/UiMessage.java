package com.yangdesigner.yangmodeldesigner.ui.state;

public record UiMessage(String text, String path, int line) {
    @Override
    public String toString() {
        return text;
    }
}
