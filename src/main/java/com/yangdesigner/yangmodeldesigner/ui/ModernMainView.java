package com.yangdesigner.yangmodeldesigner.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public final class ModernMainView extends ClassicMainView {
    private static final String MODERN_CSS = "/com/yangdesigner/yangmodeldesigner/modern-ui.css";
    private static final String MODERN_ROOT = "modern-root";
    private static final String LIGHT_THEME = "modern-theme-light";
    private static final String DARK_THEME = "modern-theme-dark";
    private static final String PREF_THEME = "ui.theme";
    private static final String PREF_CURRENT_FILE = "current.file";
    private final Preferences preferences = Preferences.userNodeForPackage(ModernMainView.class);
    private Theme currentTheme = Theme.MODERN_LIGHT;

    public ModernMainView(Stage stage) {
        super(stage);
        applyModernStylesheet();
        installThemeMenu();
        applyTheme(loadTheme());
    }

    @Override
    protected boolean isModernInterface() {
        return currentTheme == null || currentTheme != Theme.CLASSIC;
    }

    public void restoreState() {
        String file = preferences.get(PREF_CURRENT_FILE, "");
        if (!file.isBlank()) {
            openInitialDocument(Path.of(file));
        }
    }

    public void saveState() {
        preferences.put(PREF_THEME, currentTheme.name());
        Path file = currentFile();
        if (file == null) {
            preferences.remove(PREF_CURRENT_FILE);
        } else {
            preferences.put(PREF_CURRENT_FILE, file.toAbsolutePath().normalize().toString());
        }
    }

    private Theme loadTheme() {
        String value = preferences.get(PREF_THEME, Theme.MODERN_LIGHT.name());
        try {
            return Theme.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return Theme.MODERN_LIGHT;
        }
    }

    private void applyModernStylesheet() {
        URL css = ModernMainView.class.getResource(MODERN_CSS);
        if (css != null && root().getStylesheets().stream().noneMatch(css.toExternalForm()::equals)) {
            root().getStylesheets().add(css.toExternalForm());
        }
    }

    private void installThemeMenu() {
        if (!(root() instanceof BorderPane borderPane) || !(borderPane.getTop() instanceof MenuBar menuBar)) {
            return;
        }

        RadioMenuItem classic = new RadioMenuItem("Классическая");
        RadioMenuItem light = new RadioMenuItem("Светлая");
        RadioMenuItem dark = new RadioMenuItem("Темная");
        ToggleGroup group = new ToggleGroup();
        classic.setToggleGroup(group);
        light.setToggleGroup(group);
        dark.setToggleGroup(group);

        classic.setOnAction(event -> applyTheme(Theme.CLASSIC));
        light.setOnAction(event -> applyTheme(Theme.MODERN_LIGHT));
        dark.setOnAction(event -> applyTheme(Theme.MODERN_DARK));

        Menu theme = new Menu("Тема");
        theme.getItems().addAll(classic, light, dark);
        menuBar.getMenus().add(theme);
    }

    private void selectThemeMenuItem(Theme theme) {
        if (!(root() instanceof BorderPane borderPane) || !(borderPane.getTop() instanceof MenuBar menuBar)) {
            return;
        }
        Menu themeMenu = menuBar.getMenus().stream()
                .filter(menu -> "Тема".equals(menu.getText()))
                .findFirst()
                .orElse(null);
        if (themeMenu == null || themeMenu.getItems().size() < 3) {
            return;
        }
        ((RadioMenuItem) themeMenu.getItems().get(theme.ordinal())).setSelected(true);
    }

    private void decorateTopMenu(boolean modern) {
        if (!(root() instanceof BorderPane borderPane) || !(borderPane.getTop() instanceof MenuBar menuBar)) {
            return;
        }
        String[] icons = {"◆", "✎", "⌕", "⚙", "?", "◐"};
        for (int index = 0; index < menuBar.getMenus().size() && index < icons.length; index++) {
            if (!modern) {
                menuBar.getMenus().get(index).setGraphic(null);
                continue;
            }
            Label icon = new Label(icons[index]);
            icon.getStyleClass().add("modern-menu-icon");
            menuBar.getMenus().get(index).setGraphic(icon);
        }
    }

    private void applyTheme(Theme theme) {
        currentTheme = theme;
        root().getStyleClass().removeAll(MODERN_ROOT, LIGHT_THEME, DARK_THEME);
        if (theme == Theme.MODERN_LIGHT) {
            root().getStyleClass().addAll(MODERN_ROOT, LIGHT_THEME);
            decorateTopMenu(true);
        } else if (theme == Theme.MODERN_DARK) {
            root().getStyleClass().addAll(MODERN_ROOT, DARK_THEME);
            decorateTopMenu(true);
        } else {
            decorateTopMenu(false);
        }
        preferences.put(PREF_THEME, currentTheme.name());
        selectThemeMenuItem(theme);
        refreshTreeView();
        refreshEditorHighlighting();
    }

    private enum Theme {
        CLASSIC,
        MODERN_LIGHT,
        MODERN_DARK
    }
}
