package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.ui.AudioPlayerUI;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.URL;
import java.util.*;

public class AudioPlayerController implements Initializable {
    //lines 23-27 is old code we had from when we initially set up the skeleton code, need to implement later
    private AudioPlayer audioPlayer;
    private PlaylistManager playlistManager;
    private QueueManager queueManager;
    private MediaLibrary mediaLibrary;
    private AudioPlayerUI view;

    private Media media;
    private MediaPlayer mediaPlayer;
    private File directory;
    private File[] files;
    private ArrayList<File> songs;
    private int songNumber = 0;
    private Timer timer;
    private boolean playback;

    @FXML
    private Label songLabel;

    @FXML
    private ProgressBar songProgressBar;

    @FXML
    private Button nextButton;

    @FXML
    private Button pauseButton;

    @FXML
    private Button playButton;

    @FXML
    private Button prevButton;

    @FXML
    private Slider volumeSlider;

    //plays song
    @FXML
    void startPlayback(ActionEvent event) {
        startTimer();
        /*
        For the next line, we multiply by .01 because the setVolume
        method, belonging to the mediaPlayer, can only take values
        between 0-1. That's why we multiply it by .01 to convert
        the percentage into a value that stays in the bounds of the
        setVolume method.
         */
        mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
        mediaPlayer.play();
    }

    //pauses song
    @FXML
    void stopPlayback(ActionEvent event) {
        stopTimer();
        mediaPlayer.pause();
    }


    /*
    The next two functions focus on making sure that the program properly loops
    through the songs even when reaching the start or end of the queue.

    Will implement later full interaction with queue. For now, it uses a preset
    playlist in users/test_user/playlists/test_playlist composed of three songs.
     */
    @FXML
    void queueNextSong(ActionEvent event) {
        if (songNumber < songs.size() - 1) { //makes sure songNumber is inside the bounds of the songs array
            songNumber++;
            updateUI();
        }
        else {
            //set songNumber to the start of the songs array
            songNumber = 0;
            updateUI();
        }
    }

    @FXML
    void queuePrevSong(ActionEvent event) {
        if (songNumber > 0) { //makes sure songNumber is inside the bounds of the songs array
            songNumber--;
            updateUI();
        }
        else {
            //set songNumber to the end of the songs array
            songNumber = songs.size() - 1;
            updateUI();
        }
    }

    //Handles resetting the scene and timer with the new song
    public void updateUI() {
        mediaPlayer.stop();

        if (playback) //if currently playing a song
            stopTimer();

        //passes the new song from the songs array by converting the filepath (toURI) into a String
        media = new Media(songs.get(songNumber).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        songLabel.setText(songs.get(songNumber).getName());

        mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
        startTimer();
        mediaPlayer.play();
    }

    //Start of the timer used for updating songProgressBar
    public void startTimer() {
        timer = new Timer();

        TimerTask task = new TimerTask() { //start timer
            @Override
            public void run() {
                playback = true;
                double current = mediaPlayer.getCurrentTime().toSeconds();
                double end = media.getDuration().toSeconds();
                songProgressBar.setProgress(current / end);

                if (current / end == 1) { //if song is finished
                    stopTimer();
                }
            }
        };

        //sets timer to execute the TimerTask task with a delay of 0ms, at a rate of 1000ms (1s)
        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    public void stopTimer() {
        playback = false;
        timer.cancel();
    }

    //lines 159-167 is old code we had from when we initially set up the skeleton code, need to implement later
    public void playSong(Song song) {}
    public void pauseSong() {}
    public void nextSong() {}
    public void previousSong() {}
    public void addSongToQueue(Song song) {}
    public void addSongToPlaylist(Song song, String playlistName) {}
    public void playPlaylist(String playlistName) {}
    public void loopSong(Song song) {}

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //fetches songs from temporary playlist and puts into songs array for playback
        songs = new ArrayList<File>();
        directory = new File("users/test_user/playlists/test_playlist");
        files = directory.listFiles();

        if (files != null) { //makes sure user playlist isn't empty
            for (File file : files) {
                songs.add(file); //adds songs to queue
            }
        }
        else {
            System.out.println("<<<NOBODY PANIC, SOMETHING WENT WRONG>>>");
        }

        media = new Media(songs.get(songNumber).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        songLabel.setText(songs.get(songNumber).getName());

        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
            }
        });
    }
}
