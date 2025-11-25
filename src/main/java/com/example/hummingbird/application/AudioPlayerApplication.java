package com.example.hummingbird.application;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.WindowEvent;

import java.io.IOException;

/**
 * Entry point for the Hummingbird application.
 *
 * Responsibilities:
 *  - Initialize JavaFX
 *  - Load the initial Login view from FXML
 *  - Apply the global UI theme
 *  - Ensure the application shuts down cleanly when the window closes
 */
public class AudioPlayerApplication extends Application {

    /**
     * Called automatically by the JavaFX runtime when the application starts.
     * Sets up the main window (Stage), loads the first FXML layout, and
     * configures application-wide UI and shutdown behavior.
     */
    @Override
    public void start(Stage stage) throws IOException {

        // Apply the global dark theme from the AtlantisFX library
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Load the login screen from its FXML layout file
        FXMLLoader fxmlLoader = new FXMLLoader(
                AudioPlayerApplication.class.getResource("/com/example/hummingbird/login_view.fxml")
        );

        // Create the window's scene and set its initial size
        Scene scene = new Scene(fxmlLoader.load(), 900, 800);

        // Configure the primary window (Stage)
        stage.setTitle("Hummingbird - Login");
        stage.setScene(scene);
        stage.show();

        // Ensure all background threads and media players shut down when the window closes
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                Platform.exit();  // Gracefully stop JavaFX runtime
                System.exit(0);   // Hard exit to guarantee all non-FX threads die
            }
        });
    }

    /**
     * Standard Java entry point.
     * Delegates to JavaFX's Application.launch() to start the UI framework.
     */
    public static void main(String[] args) {
        launch();
    }
}
