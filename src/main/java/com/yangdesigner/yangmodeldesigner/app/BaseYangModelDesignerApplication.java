package com.yangdesigner.yangmodeldesigner.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public abstract class BaseYangModelDesignerApplication extends Application {
    private static final String APP_ICON = "/com/yangdesigner/yangmodeldesigner/app-icon.png";
    private static final String PREF_WIDTH = "window.width";
    private static final String PREF_HEIGHT = "window.height";
    private static final String PREF_X = "window.x";
    private static final String PREF_Y = "window.y";
    private static final String PREF_MAXIMIZED = "window.maximized";
    private final Preferences preferences = Preferences.userNodeForPackage(BaseYangModelDesignerApplication.class);

    @Override
    public final void start(Stage stage) {
        ViewHandle view = createView(stage);
        Scene scene = new Scene(
                view.root(),
                preferences.getDouble(PREF_WIDTH, initialWidth()),
                preferences.getDouble(PREF_HEIGHT, initialHeight())
        );
        stage.setMinWidth(minWidth());
        stage.setMinHeight(minHeight());
        stage.setTitle(windowTitle());
        setAppIcon(stage);
        stage.setScene(scene);
        restoreWindow(stage);
        stage.setOnCloseRequest(event -> {
            view.saveState();
            saveWindow(stage);
        });
        stage.show();
        Platform.runLater(() -> {
            view.restoreState();
            view.refreshEditorHighlighting();
        });
    }

    protected abstract ViewHandle createView(Stage stage);

    protected String windowTitle() {
        return "YANG Model Designer";
    }

    protected int initialWidth() {
        return 1200;
    }

    protected int initialHeight() {
        return 760;
    }

    protected int minWidth() {
        return 980;
    }

    protected int minHeight() {
        return 620;
    }

    private void setAppIcon(Stage stage) {
        try (var stream = BaseYangModelDesignerApplication.class.getResourceAsStream(APP_ICON)) {
            if (stream != null) {
                stage.getIcons().add(new Image(stream));
            }
        } catch (Exception ignored) {
            // The application can run without a window icon if the resource is unavailable.
        }
    }

    private void restoreWindow(Stage stage) {
        if (preferences.getBoolean(PREF_MAXIMIZED, false)) {
            stage.setMaximized(true);
            return;
        }
        double x = preferences.getDouble(PREF_X, Double.NaN);
        double y = preferences.getDouble(PREF_Y, Double.NaN);
        if (!Double.isNaN(x)) {
            stage.setX(x);
        }
        if (!Double.isNaN(y)) {
            stage.setY(y);
        }
    }

    private void saveWindow(Stage stage) {
        preferences.putBoolean(PREF_MAXIMIZED, stage.isMaximized());
        if (!stage.isMaximized()) {
            preferences.putDouble(PREF_WIDTH, stage.getWidth());
            preferences.putDouble(PREF_HEIGHT, stage.getHeight());
            preferences.putDouble(PREF_X, stage.getX());
            preferences.putDouble(PREF_Y, stage.getY());
        }
    }

    public interface ViewHandle {
        Parent root();

        void refreshEditorHighlighting();

        default void restoreState() {
        }

        default void saveState() {
        }
    }
}
