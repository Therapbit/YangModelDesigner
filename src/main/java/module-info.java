module com.yangdesigner.yangmodeldesigner {
    requires javafx.controls;
    requires java.prefs;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires org.fxmisc.undo;

    exports com.yangdesigner.yangmodeldesigner;
    exports com.yangdesigner.yangmodeldesigner.app;
    exports com.yangdesigner.yangmodeldesigner.model;
    exports com.yangdesigner.yangmodeldesigner.parser;
    exports com.yangdesigner.yangmodeldesigner.service;
    exports com.yangdesigner.yangmodeldesigner.validation;
}
