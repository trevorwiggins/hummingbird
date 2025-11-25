package com.example.hummingbird.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;

/**
 * Controller for the small popup window shown during a deletion process.
 *
 * Responsibilities:
 * <ul>
 *     <li>Display a status message describing what is being deleted.</li>
 *     <li>Show a progress indicator while work is ongoing.</li>
 *     <li>Allow the caller to close the popup once deletion is complete.</li>
 * </ul>
 */
public class DeletionPopupController {

    /** Text label used to show the current status (e.g., "Deleting playlist..."). */
    @FXML public Label statusLabel;

    /** Spinner-style indicator to show that work is in progress. */
    @FXML public ProgressIndicator progressIndicator;

    /** Reference to the Stage that owns this popup so it can be closed programmatically. */
    private Stage stage;

    /**
     * Injects the Stage that hosts this popup.
     * Typically called right after loading the FXML.
     *
     * @param stage the Stage containing this popup window
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Updates the status text shown in the popup.
     * Can be called as the deletion process advances through different steps.
     *
     * @param text message to display to the user
     */
    public void updateStatus(String text) {
        statusLabel.setText(text);
    }

    /**
     * Closes the popup window if its Stage reference has been set.
     * Safe to call multiple times; nothing happens if stage is null.
     */
    public void close() {
        if (stage != null) {
            stage.close();
        }
    }
}
