package com.yangdesigner.yangmodeldesigner.model;

public enum YangNodeType {
    MODULE,
    SUBMODULE,
    IMPORT,
    INCLUDE,
    REVISION,
    EXTENSION,
    FEATURE,
    IDENTITY,
    TYPEDEF,
    CONTAINER,
    LIST,
    LEAF,
    LEAF_LIST,
    CHOICE,
    CASE,
    GROUPING,
    USES,
    AUGMENT,
    RPC,
    ACTION,
    INPUT,
    OUTPUT,
    NOTIFICATION,
    DEVIATION,
    DEVIATE,
    ANYDATA,
    ANYXML,
    ENUM,
    BIT,
    UNKNOWN;

    public String keyword() {
        return name().toLowerCase().replace('_', '-');
    }

    public static YangNodeType fromKeyword(String keyword) {
        return switch (keyword) {
            case "module" -> MODULE;
            case "submodule" -> SUBMODULE;
            case "import" -> IMPORT;
            case "include" -> INCLUDE;
            case "revision" -> REVISION;
            case "extension" -> EXTENSION;
            case "feature" -> FEATURE;
            case "identity" -> IDENTITY;
            case "typedef" -> TYPEDEF;
            case "container" -> CONTAINER;
            case "list" -> LIST;
            case "leaf" -> LEAF;
            case "leaf-list" -> LEAF_LIST;
            case "choice" -> CHOICE;
            case "case" -> CASE;
            case "grouping" -> GROUPING;
            case "uses" -> USES;
            case "augment" -> AUGMENT;
            case "rpc" -> RPC;
            case "action" -> ACTION;
            case "input" -> INPUT;
            case "output" -> OUTPUT;
            case "notification" -> NOTIFICATION;
            case "deviation" -> DEVIATION;
            case "deviate" -> DEVIATE;
            case "anydata" -> ANYDATA;
            case "anyxml" -> ANYXML;
            case "enum" -> ENUM;
            case "bit" -> BIT;
            default -> UNKNOWN;
        };
    }
}
