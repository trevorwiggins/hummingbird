package com.example.hummingbird.ui;

import atlantafx.base.theme.PrimerDark;
import com.example.hummingbird.controller.AudioPlayerController;
import com.example.hummingbird.model.PlaybackListener;
import com.example.hummingbird.model.Song;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class AudioPlayerUI extends Application implements PlaybackListener {
    private Stage primaryStage;
    private Scene mainScene;
    private Button playButton;
    private Button pauseButton;
    private Button nextButton;
    private Button previousButton;
    private AudioPlayerController controller;

    public AudioPlayerUI() {}
    public AudioPlayerUI(Stage stage) { this.primaryStage = stage; }

    public void initializeUI() {}
    public void updateUI() {}
    public void displayNowPlaying(String songTitle) {}
    public void setController(AudioPlayerController controller) {
        this.controller = controller;
    }

    @Override
    public void onPlay(Song song) {

    }

    @Override
    public void onPause(Song song) {

    }

    @Override
    public void onStop(Song song) {

    }

    @Override
    public void onNext(Song song) {

    }

    @Override
    public void onPrevious(Song song) {

    }

    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        FXMLLoader fxmlLoader = new FXMLLoader(AudioPlayerUI.class.getResource("/com/example/hummingbird/mainscene.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 200);

        // Get the controller and verify it's loaded
        AudioPlayerController controller = fxmlLoader.getController();
        if (controller != null) {
            System.out.println("SUCCESS: AudioPlayerController loaded!");
            this.controller = controller;
        } else {
            System.out.println("ERROR: AudioPlayerController is null!");
        }


        stage.setTitle("Hummingbird");
        stage.setScene(scene);
        stage.show();

        //cleanly close the program and end all processes
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                Platform.exit();
                System.exit(0);
            }
        });
    }



    public static void main(String[] args) {
        launch();
    }
}
