package com.example.notewidget;

import atlantafx.base.theme.PrimerLight;
import com.example.notewidget.controller.MainController;
import com.example.notewidget.controller.WidgetController;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Objects;
import java.util.prefs.Preferences;

public class NoteWidgetApp extends Application {

    private static final double DEFAULT_MAIN_WIDTH = 1300;
    private static final double DEFAULT_MAIN_HEIGHT = 780;
    // Keep around one-fifth of the original window area.
    private static final double MAIN_WINDOW_SCALE = Math.sqrt(0.2);
    private static final double MAIN_MIN_WIDTH = 420;
    private static final double MAIN_MIN_HEIGHT = 280;
    private static final String KEY_MAIN_X = "main.x";
    private static final String KEY_MAIN_Y = "main.y";

    private Stage mainStage;
    private Stage widgetStage;

    private boolean keepMainWindowOpen;
    private boolean widgetHovered;
    private boolean mainWindowHovered;

    private final Preferences preferences = Preferences.userNodeForPackage(NoteWidgetApp.class);
    private final PauseTransition autoHideDelay = new PauseTransition(Duration.millis(500));

    @Override
    public void start(Stage primaryStage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader mainLoader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/fxml/main-view.fxml"))
        );
        Parent mainRoot = mainLoader.load();
        MainController mainController = mainLoader.getController();

        double initialWidth = DEFAULT_MAIN_WIDTH * MAIN_WINDOW_SCALE;
        double initialHeight = DEFAULT_MAIN_HEIGHT * MAIN_WINDOW_SCALE;

        mainStage = new Stage(StageStyle.DECORATED);
        Scene mainScene = new Scene(mainRoot, initialWidth, initialHeight);
        mainScene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm()
        );

        mainStage.setTitle("桌面笔记挂件");
        mainStage.setMinWidth(MAIN_MIN_WIDTH);
        mainStage.setMinHeight(MAIN_MIN_HEIGHT);
        mainStage.setScene(mainScene);

        mainController.setMainStage(mainStage);
        mainController.setUserInteractionListener(this::pinMainWindow);

        mainScene.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            mainWindowHovered = true;
            cancelAutoHide();
        });
        mainScene.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            mainWindowHovered = false;
            scheduleAutoHide();
        });

        mainStage.setOnCloseRequest(event -> {
            event.consume();
            keepMainWindowOpen = false;
            saveMainWindowPosition();
            mainStage.hide();
        });

        autoHideDelay.setOnFinished(event -> maybeHideMainWindow());

        FXMLLoader widgetLoader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/fxml/widget-view.fxml"))
        );
        Parent widgetRoot = widgetLoader.load();
        WidgetController widgetController = widgetLoader.getController();

        widgetStage = new Stage(StageStyle.TRANSPARENT);
        Scene widgetScene = new Scene(widgetRoot);
        widgetScene.setFill(Color.TRANSPARENT);
        widgetScene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/widget.css")).toExternalForm()
        );

        widgetStage.setAlwaysOnTop(true);
        widgetStage.setResizable(false);
        widgetStage.setScene(widgetScene);

        widgetController.setWidgetStage(widgetStage);
        widgetController.setOpenMainWindowAction(() -> showMainWindow(false));
        widgetController.setHoverEnteredAction(this::onWidgetHoverEntered);
        widgetController.setHoverExitedAction(this::onWidgetHoverExited);

        widgetStage.show();
        if (!widgetController.applySavedPosition()) {
            positionWidgetBottomRight(widgetStage);
        }
    }

    private void onWidgetHoverEntered() {
        widgetHovered = true;
        keepMainWindowOpen = false;
        cancelAutoHide();
        showMainWindow(false);
    }

    private void onWidgetHoverExited() {
        widgetHovered = false;
        scheduleAutoHide();
    }

    private void pinMainWindow() {
        keepMainWindowOpen = true;
        showMainWindow(true);
        cancelAutoHide();
    }

    private void showMainWindow(boolean requestFocus) {
        if (!mainStage.isShowing()) {
            if (!applySavedMainWindowPosition()) {
                positionMainWindowNearWidget();
            }
            mainStage.show();
        }

        if (mainStage.isIconified()) {
            mainStage.setIconified(false);
        }

        mainStage.toFront();
        if (requestFocus) {
            mainStage.requestFocus();
        }
    }

    private void maybeHideMainWindow() {
        if (!mainStage.isShowing()) {
            return;
        }

        if (keepMainWindowOpen) {
            return;
        }

        if (widgetHovered || mainWindowHovered) {
            return;
        }

        saveMainWindowPosition();
        mainStage.hide();
    }

    private void scheduleAutoHide() {
        autoHideDelay.playFromStart();
    }

    private void cancelAutoHide() {
        autoHideDelay.stop();
    }

    private void positionWidgetBottomRight(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double margin = 16;
        stage.setX(bounds.getMaxX() - stage.getWidth() - margin);
        stage.setY(bounds.getMaxY() - stage.getHeight() - margin);
    }

    private void positionMainWindowNearWidget() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double margin = 12;

        double x = widgetStage.getX() + widgetStage.getWidth() - mainStage.getWidth();
        double y = widgetStage.getY() - mainStage.getHeight() - margin;

        if (y < bounds.getMinY() + margin) {
            y = widgetStage.getY() + widgetStage.getHeight() + margin;
        }

        x = clamp(x, bounds.getMinX() + margin, bounds.getMaxX() - mainStage.getWidth() - margin);
        y = clamp(y, bounds.getMinY() + margin, bounds.getMaxY() - mainStage.getHeight() - margin);

        mainStage.setX(x);
        mainStage.setY(y);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void saveMainWindowPosition() {
        if (mainStage == null || !mainStage.isShowing()) {
            return;
        }
        preferences.putDouble(KEY_MAIN_X, mainStage.getX());
        preferences.putDouble(KEY_MAIN_Y, mainStage.getY());
    }

    private boolean applySavedMainWindowPosition() {
        double savedX = preferences.getDouble(KEY_MAIN_X, Double.NaN);
        double savedY = preferences.getDouble(KEY_MAIN_Y, Double.NaN);
        if (Double.isNaN(savedX) || Double.isNaN(savedY)) {
            return false;
        }

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double margin = 8;
        double x = clamp(savedX, bounds.getMinX() + margin, bounds.getMaxX() - mainStage.getWidth() - margin);
        double y = clamp(savedY, bounds.getMinY() + margin, bounds.getMaxY() - mainStage.getHeight() - margin);
        mainStage.setX(x);
        mainStage.setY(y);
        return true;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
