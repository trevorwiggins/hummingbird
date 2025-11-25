package com.example.hummingbird.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

public class DeletionPopupController {

    @FXML public Label statusLabel;
    @FXML public ProgressIndicator progressIndicator;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void updateStatus(String text) {
        statusLabel.setText(text);
    }

    public void close() {
        if (stage != null) {
            stage.close();
        }
    }
}
