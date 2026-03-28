package com.example.notewidget.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class WidgetController {

    private static final String KEY_WIDGET_X = "widget.x";
    private static final String KEY_WIDGET_Y = "widget.y";
    private static final double DRAG_THRESHOLD = 5.0;

    @FXML
    private StackPane widgetRoot;
    @FXML
    private Button openButton;

    private final Preferences preferences = Preferences.userNodeForPackage(WidgetController.class);

    private Stage widgetStage;
    private Runnable openMainWindowAction;
    private Runnable hoverEnteredAction;
    private Runnable hoverExitedAction;

    private double dragOffsetX;
    private double dragOffsetY;
    private double pressScreenX;
    private double pressScreenY;
    private boolean dragging;

    @FXML
    private void initialize() {
        // 在捕获阶段拦截鼠标事件，防止 Button 吞掉拖拽
        widgetRoot.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (widgetStage == null) {
                return;
            }
            dragOffsetX = event.getScreenX() - widgetStage.getX();
            dragOffsetY = event.getScreenY() - widgetStage.getY();
            pressScreenX = event.getScreenX();
            pressScreenY = event.getScreenY();
            dragging = false;
        });

        widgetRoot.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (widgetStage == null) {
                return;
            }
            double dx = event.getScreenX() - pressScreenX;
            double dy = event.getScreenY() - pressScreenY;
            if (!dragging && (dx * dx + dy * dy) >= DRAG_THRESHOLD * DRAG_THRESHOLD) {
                dragging = true;
            }
            if (dragging) {
                widgetStage.setX(event.getScreenX() - dragOffsetX);
                widgetStage.setY(event.getScreenY() - dragOffsetY);
                event.consume(); // 阻止事件传递给 Button
            }
        });

        widgetRoot.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (dragging) {
                saveWidgetPosition();
                event.consume(); // 拖拽结束不触发按钮点击
            }
            dragging = false;
        });

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