package com.yangdesigner.yangmodeldesigner.app;

import com.yangdesigner.yangmodeldesigner.ui.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class YangModelDesignerApplication extends Application {
    private static final String APP_ICON = "/com/yangdesigner/yangmodeldesigner/app-icon.png";

    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView(stage);
        Scene scene = new Scene(mainView.root(), 1200, 760);
        stage.setMinWidth(980);
        stage.setMinHeight(620);
        stage.setTitle("YANG Model Designer");
        setAppIcon(stage);
        stage.setScene(scene);
        stage.show();
        Platform.runLater(mainView::refreshEditorHighlighting);
    }

    private void setAppIcon(Stage stage) {
        try (var stream = YangModelDesignerApplication.class.getResourceAsStream(APP_ICON)) {
            if (stream != null) {
                stage.getIcons().add(new Image(stream));
            }
        } catch (Exception ignored) {
            // The application can run without a window icon if the resource is unavailable.
        }
    }
}
