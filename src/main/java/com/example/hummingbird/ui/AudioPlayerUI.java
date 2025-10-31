package com.example.hummingbird.ui;

import atlantafx.base.theme.PrimerDark;
import com.example.hummingbird.controller.AudioPlayerController;
import com.example.hummingbird.model.PlaybackListener;
•
import com.example.hummingbird.model.Song;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;

import java.io.IOException;

public class AudioPlayerUI extends Application implements PlaybackListener {
    private Stage primaryStage;
    private Scene mainScene;
    private Button playButton;
    private Button pauseButton;
    private Button nextButton;
    private Button previousButton;

    public AudioPlayerUI() {}
    public AudioPlayerUI(Stage stage) { this.primaryStage = stage; }

    public void initializeUI() {}
    public void updateUI() {}
    public void displayNowPlaying(String songTitle) {}
    public void setController(AudioPlayerController controller) {}

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
        FXMLLoader fxmlLoader = new FXMLLoader(AudioPlayerUI.class.getResource("/com/example/hummingbird/hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hummingbird");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
