package com.yangdesigner.yangmodeldesigner.ui.state;

import java.util.Set;

public record YangTreeState(Set<String> expandedPaths, String selectedPath) {
    public static YangTreeState empty() {
        return new YangTreeState(Set.of(), "");
    }
}
