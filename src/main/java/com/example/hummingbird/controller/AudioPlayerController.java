package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.ui.AudioPlayerUI;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

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

    private Media media; //song currently set to play
    private MediaPlayer mediaPlayer; //player for song
    private ArrayList<File> songs; //current loaded queue
    private int songNumber = 0; //song index for playback
    private boolean userIsSeeking;

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

    @FXML
    private Slider songProgressSlider;

    // ADD YOUR DRAG DROP BUTTON HERE
    @FXML
    private Button OnSongDragDrop;

    //plays song
    @FXML
    void startPlayback(ActionEvent event) {
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
        mediaPlayer.pause();
    }

    /*
    The next three functions focus on making sure that the program properly loops
    through the songs even when reaching the start or end of the queue.

    Will implement later full interaction with queue. For now, it uses a preset
    playlist in users/test_user/playlists/test_playlist composed of three songs.
     */
    @FXML
    void queueNextSong(ActionEvent event) {
        queueNextSongNonEvent();
    }

    private void queueNextSongNonEvent() {
        if (songNumber < songs.size() - 1) //makes sure songNumber is inside the bounds of the songs array
            songNumber++;
        else
            songNumber = 0; //set songNumber to the start of the songs array
        updateUI();
    }

    @FXML
    void queuePrevSong(ActionEvent event) {
        if (songNumber > 0) //makes sure songNumber is inside the bounds of the songs array
            songNumber--;
        else
            songNumber = songs.size() - 1; //set songNumber to the end of the songs array
        updateUI();
    }

    //Handles resetting the scene with the new song
    public void updateUI() {
        mediaPlayer.stop();

        //passes the new song from the songs array by converting the filepath (toURI) into a String
        media = new Media(songs.get(songNumber).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        songLabel.setText(songs.get(songNumber).getName());

        mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
        attachMediaPlayListeners();
        mediaPlayer.play();
    }

    private void attachMediaPlayListeners() {
        //smooth, safe progress updating (disabled while user drags slider)
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {

            if (userIsSeeking)
                return; // don't overwrite user's dragging

            if (mediaPlayer.getTotalDuration().toMillis() > 0) {
                double progress = newTime.toMillis() / mediaPlayer.getTotalDuration().toMillis();
                songProgressBar.setProgress(progress);
                songProgressSlider.setValue(progress);
            }
        });

        // automatically switch to next song at end of current song
        mediaPlayer.setOnEndOfMedia(() -> queueNextSongNonEvent());
    }

    @FXML
    void seekTo(MouseEvent event) {
        double sliderPercent = songProgressSlider.getValue();
        sliderPercent = truncateDouble(sliderPercent);

        double endTime = media.getDuration().toMillis();
        double seekTimeMillis = sliderPercent * endTime;
        Duration seekTimeDuration = new Duration(seekTimeMillis);

        mediaPlayer.seek(seekTimeDuration);
    }

    //truncates any double into 2 decimal places and returns it
    public double truncateDouble (double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(2, RoundingMode.DOWN);
        value = bd.doubleValue();
        return value;
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
        File directory = new File("users/test_user/playlists/test_playlist");
        File[] files = directory.listFiles();

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

        attachMediaPlayListeners();

        // detect when user starts and stops dragging the slider
        // drag and drop seek
        songProgressSlider.valueChangingProperty().addListener((obs, was, isChanging) -> {
            userIsSeeking = isChanging;

            if (!isChanging) {
                double pct = songProgressSlider.getValue();
                double totalMillis = media.getDuration().toMillis();
                mediaPlayer.seek(Duration.millis(pct * totalMillis));
            }
        });

        // single click seek
        songProgressSlider.setOnMousePressed(event -> userIsSeeking = true);

        songProgressSlider.setOnMouseReleased(event -> {
            userIsSeeking = false;

            double pct = songProgressSlider.getValue();
            mediaPlayer.seek(Duration.millis(pct * media.getDuration().toMillis()));
        });

        mediaPlayer.pause();

        // ============ DEBUG CODE FOR DRAG DROP BUTTON ============
        System.out.println("=== DEBUG: AudioPlayerController.initialize() completed ===");

        if (OnSongDragDrop == null) {
            System.out.println("ERROR: OnSongDragDrop button is null - check FXML fx:id!");
        } else {
            System.out.println("SUCCESS: OnSongDragDrop button found with text: " + OnSongDragDrop.getText());

            // Set up the file import functionality
            setupFileImportButton();
        }
        System.out.println("=== DEBUG: Controller setup complete ===");
    }

    // ============ FILE IMPORT FUNCTIONALITY ============
    private void setupFileImportButton() {
        OnSongDragDrop.setPrefSize(120, 50);
        OnSongDragDrop.setText("Import MP3");

        OnSongDragDrop.setOnAction(event -> {
            System.out.println("DEBUG: Import button clicked!");
            openFileChooser();
        });

        System.out.println("DEBUG: File import button setup complete");
    }

    private void openFileChooser() {
        System.out.println("DEBUG: openFileChooser() called");

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select MP3 Files");

        javafx.stage.FileChooser.ExtensionFilter mp3Filter =
                new javafx.stage.FileChooser.ExtensionFilter("MP3 Files (*.mp3)", "*.mp3");
        fileChooser.getExtensionFilters().add(mp3Filter);

        // Get stage from the button
        javafx.stage.Stage stage = (javafx.stage.Stage) OnSongDragDrop.getScene().getWindow();

        System.out.println("DEBUG: Opening file chooser...");
        java.util.List<java.io.File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            System.out.println("DEBUG: " + selectedFiles.size() + " files selected");
            processSelectedFiles(selectedFiles);
        } else {
            System.out.println("DEBUG: No files selected or dialog cancelled");
        }
    }

    private void processSelectedFiles(java.util.List<java.io.File> selectedFiles) {
        File targetFolder = new File("users/test_user/playlists/test_playlist");

        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }

        int copiedCount = 0;
        for (File file : selectedFiles) {
            if (file.getName().toLowerCase().endsWith(".mp3")) {
                try {
                    String fileName = file.getName();
                    java.nio.file.Path targetPath = getUniqueFilePath(targetFolder.toPath(), fileName);

                    java.nio.file.Files.copy(file.toPath(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    copiedCount++;
                    System.out.println("SUCCESS: Copied " + file.getName() + " to " + targetPath);
                } catch (java.io.IOException e) {
                    System.err.println("ERROR copying file " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        // Show result alert
        int finalCopiedCount = copiedCount;
        javafx.application.Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Import Complete");
            alert.setHeaderText(null);

            if (finalCopiedCount > 0) {
                alert.setContentText("Successfully imported " + finalCopiedCount + " MP3 file(s) to test_playlist");
                // Refresh the song list if files were added
                refreshSongList();
            } else {
                alert.setContentText("No MP3 files were imported");
            }

            alert.showAndWait();
        });
    }

    private java.nio.file.Path getUniqueFilePath(java.nio.file.Path directory, String fileName) {
        java.nio.file.Path target = directory.resolve(fileName);

        if (!java.nio.file.Files.exists(target)) {
            return target;
        }

        String baseName = fileName.replaceFirst("[.][^.]+$", "");
        String extension = ".mp3";

        int counter = 1;
        while (java.nio.file.Files.exists(target)) {
            String newFileName = baseName + " (" + counter + ")" + extension;
            target = directory.resolve(newFileName);
            counter++;
        }

        return target;
    }

    private void refreshSongList() {
        // Refresh the songs list to include newly added files
        songs.clear();
        File directory = new File("users/test_user/playlists/test_playlist");
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                songs.add(file);
            }
            System.out.println("DEBUG: Refreshed song list, now contains " + songs.size() + " songs");
        }
    }
}