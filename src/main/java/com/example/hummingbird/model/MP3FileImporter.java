package com.example.hummingbird.controller;

import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MP3FileImporter {

    public static void setupFileImport(Button button) {
        System.out.println("MP3FileImporter.setupFileImport() called");

        button.setPrefSize(120, 50);
        button.setText("Import MP3");

        button.setOnAction(event -> {
            System.out.println("Import button clicked!");

            // Simple test - just open file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select MP3 Files");

            FileChooser.ExtensionFilter mp3Filter = new FileChooser.ExtensionFilter(
                    "MP3 Files (*.mp3)", "*.mp3"
            );
            fileChooser.getExtensionFilters().add(mp3Filter);

            Stage stage = (Stage) button.getScene().getWindow();
            System.out.println("Stage: " + stage);

            // Try to open the dialog
            try {
                fileChooser.showOpenMultipleDialog(stage);
                System.out.println("File chooser opened successfully");
            } catch (Exception e) {
                System.err.println("Error opening file chooser: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}