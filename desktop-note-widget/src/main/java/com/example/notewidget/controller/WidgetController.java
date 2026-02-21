package com.example.notewidget.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class WidgetController {

    private static final String KEY_WIDGET_X = "widget.x";
    private static final String KEY_WIDGET_Y = "widget.y";

    @FXML
    private StackPane widgetRoot;

    private final Preferences preferences = Preferences.userNodeForPackage(WidgetController.class);

    private Stage widgetStage;
    private Runnable openMainWindowAction;
    private Runnable hoverEnteredAction;
    private Runnable hoverExitedAction;

    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        widgetRoot.setOnMousePressed(event -> {
            if (widgetStage == null) {
                return;
            }
            dragOffsetX = event.getScreenX() - widgetStage.getX();
            dragOffsetY = event.getScreenY() - widgetStage.getY();
        });

        widgetRoot.setOnMouseDragged(event -> {
            if (widgetStage == null) {
                return;
            }
            widgetStage.setX(event.getScreenX() - dragOffsetX);
            widgetStage.setY(event.getScreenY() - dragOffsetY);
        });

        widgetRoot.setOnMouseReleased(event -> saveWidgetPosition());

        widgetRoot.setOnMouseEntered(event -> {
            if (hoverEnteredAction != null) {
                hoverEnteredAction.run();
            }
        });

        widgetRoot.setOnMouseExited(event -> {
            if (hoverExitedAction != null) {
                hoverExitedAction.run();
            }
        });
    }

    @FXML
    private void handleWidgetClick() {
        if (openMainWindowAction != null) {
            openMainWindowAction.run();
        }
    }

    public void setWidgetStage(Stage widgetStage) {
        this.widgetStage = widgetStage;
    }

    public void setOpenMainWindowAction(Runnable openMainWindowAction) {
        this.openMainWindowAction = openMainWindowAction;
    }

    public void setHoverEnteredAction(Runnable hoverEnteredAction) {
        this.hoverEnteredAction = hoverEnteredAction;
    }

    public void setHoverExitedAction(Runnable hoverExitedAction) {
        this.hoverExitedAction = hoverExitedAction;
    }

    public boolean applySavedPosition() {
        if (widgetStage == null) {
            return false;
        }

        double savedX = preferences.getDouble(KEY_WIDGET_X, Double.NaN);
        double savedY = preferences.getDouble(KEY_WIDGET_Y, Double.NaN);

        if (Double.isNaN(savedX) || Double.isNaN(savedY)) {
            return false;
        }

        widgetStage.setX(savedX);
        widgetStage.setY(savedY);
        return true;
    }

    private void saveWidgetPosition() {
        if (widgetStage == null) {
            return;
        }

        preferences.putDouble(KEY_WIDGET_X, widgetStage.getX());
        preferences.putDouble(KEY_WIDGET_Y, widgetStage.getY());
    }
}