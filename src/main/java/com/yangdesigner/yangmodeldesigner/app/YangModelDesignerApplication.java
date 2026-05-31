package com.yangdesigner.yangmodeldesigner.app;

import com.yangdesigner.yangmodeldesigner.ui.ModernMainView;
import javafx.stage.Stage;

public final class YangModelDesignerApplication extends BaseYangModelDesignerApplication {
    @Override
    protected ViewHandle createView(Stage stage) {
        ModernMainView view = new ModernMainView(stage);
        return new ViewHandle() {
            @Override
            public javafx.scene.Parent root() {
                return view.root();
            }

            @Override
            public void refreshEditorHighlighting() {
                view.refreshEditorHighlighting();
            }

            @Override
            public void restoreState() {
                view.restoreState();
            }

            @Override
            public void saveState() {
                view.saveState();
            }
        };
    }
}
