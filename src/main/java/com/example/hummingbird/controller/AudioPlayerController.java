package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.ui.AudioPlayerUI;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Controller for the Audio Player UI.
 * Handles playback, queue management, playlist display, and drag-and-drop functionality.
 */
public class AudioPlayerController implements Initializable {

    // === MANAGERS ===
    private PlaylistManager playlistManager;  // Manages playlists and songs
    private QueueManager queueManager;       // Manages current playback queue

    // === MEDIA PLAYER ===
    private Media media;                     // Currently loaded media
    private MediaPlayer mediaPlayer;         // JavaFX MediaPlayer for playback
    private boolean userIsSeeking;           // Flag to prevent slider conflicts during seeking

    // === FILE DIRECTORY ===
    private File directory;                  // Base directory for user playlists

    // === UI STATE PROPERTIES ===
    private StringProperty listViewMode = new SimpleStringProperty("playlists"); // Tracks current ListView mode: playlists, songs, or queue
    private BooleanProperty queueCreated = new SimpleBooleanProperty(false);     // True if there is at least one song in the queue

    // === FXML UI COMPONENTS ===
    @FXML private Label songLabel;           // Displays currently playing song
    @FXML private Label infoLabel1;          // Context info label (e.g., "Current mode:")
    @FXML private Label infoLabel2;          // Context info label (e.g., playlist/queue name)

    @FXML private ProgressBar songProgressBar; // Visual playback progress
    @FXML private Slider songProgressSlider;   // User-controlled slider for seeking

    @FXML private Button nextButton;
    @FXML private Button playbackButton;
    @FXML private Button prevButton;
    @FXML private Button viewSwitchToPlaylistsButton;

    @FXML private Button playlistsDisplayButton;
    @FXML private Button queueDisplayButton;
    @FXML private Button queueSelectedPlaylistButton;
    @FXML private Button openSelectedPlaylistButton;
    @FXML private Button clearQueueButton;    // Enabled only if queue exists

    @FXML private Slider volumeSlider;         // Volume control

    @FXML private ListView<String> mediaListView; // Displays playlists, songs, or queue depending on mode

    // === PLAYBACK CONTROL METHODS ===

    @FXML
    void playback(ActionEvent event) {
        // Check current text of the button to determine state
        if ("PLAY".equals(playbackButton.getText())) {
            // If button says "Play", start playback
            startPlayback();                        // Call existing startPlayback method
            playbackButton.setText("PAUSE");        // Update button text to indicate next action
        } else {
            // Otherwise, button says "Pause", so stop playback
            stopPlayback();                         // Call existing stopPlayback method
            playbackButton.setText("PLAY");         // Update button text back to Play
        }
    }

    /** Starts playback of current media, setting volume from slider */
    private void startPlayback() {
        if (mediaPlayer == null) return;
        mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
        mediaPlayer.play();
    }

    /** Pauses current playback */
    private void stopPlayback() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }

    /** Skips to next song in queue */
    @FXML
    void queueNextSong(ActionEvent event) { nextSong(); }

    /** Internal method to advance to the next song and update UI */
    private void nextSong() {
        if (queueManager == null || queueManager.isEmpty()) return;
        queueManager.nextSong();
        updateUI();
    }

    /** Skips to previous song in queue */
    @FXML
    void queuePrevSong(ActionEvent event) { previousSong(); }

    /** Internal method to go to previous song and update UI */
    public void previousSong() {
        if (queueManager == null || queueManager.isEmpty()) return;
        queueManager.prevSong();
        updateUI();
    }

    /** Updates the UI and MediaPlayer to reflect the current song in queue */
    public void updateUI() {
        Song current = (queueManager != null) ? queueManager.getCurrentSong() : null;

        if (current == null) {
            // If no song, disable queue-related features
            queueCreated.set(false);
            return;
        }

        // Dispose of previous media player to avoid resource leaks
        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.dispose(); } catch (Exception ignored) {}
        }

        // Load the current song into MediaPlayer
        media = new Media(current.getSongFile().toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        // Only display the song's title, not full file info
        songLabel.setText(current.getTitle());
        mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);

        attachMediaPlayListeners();
        mediaPlayer.play();
    }

    // === LISTVIEW MODES ===

    /** Switch UI to playlist mode */
    @FXML
    void playlistMode(ActionEvent event) { listViewMode.set("playlists"); }

    /** Populate ListView with playlists */
    private void displayPlaylists() {
        mediaListView.getItems().clear();
        infoLabel1.setText("Current mode:");
        infoLabel2.setText("Playlist Library view");

        for (String name : playlistManager.getAllPlaylistNames()) {
            mediaListView.getItems().add(name);
        }
    }

    /** Switch UI to queue mode */
    @FXML
    void queueMode(ActionEvent event) { listViewMode.set("queue"); }

    /** Populate ListView with queue items and enable drag-and-drop reordering */
    private void displayQueue() {
        mediaListView.getItems().clear();
        infoLabel1.setText("Current mode:");
        infoLabel2.setText("Queue view");

        if (queueManager != null) {
            for (Song song : queueManager.getQueue()) {
                // Only show song title in queue list
                mediaListView.getItems().add(song.getTitle());
            }
            enableQueueDragAndDrop();
        }
    }

    @FXML
    void loadPlaylistManagerView(ActionEvent event) {
        try {
            Stage thisCurrentStage = (Stage) viewSwitchToPlaylistsButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(AudioPlayerUI.class.getResource("/com/example/hummingbird/playlist_manager_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 800);

            PlaylistManagerController pmController = fxmlLoader.getController();
            // Pass the PlaylistManager
            pmController.setPlaylistManager(this.playlistManager);

            thisCurrentStage.setTitle("Hummingbird - Playlist Manager");
            thisCurrentStage.setScene(scene);
            thisCurrentStage.show();
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** Queue all songs from the selected playlist (metadata-aware duplicate prevention) */
    @FXML
    void queueSelectedPlaylist(ActionEvent event) {
        SelectionModel<String> selectionModel = mediaListView.getSelectionModel();
        if (selectionModel.getSelectedItem() == null) {
            Platform.runLater(() ->
                    showWarning("No Playlist Selected", "Please select a playlist to queue it.")
            );
            return;
        }

        String selectedPlaylistName = selectionModel.getSelectedItem();
        listViewMode.set("queue");

        List<Song> selectedPlaylist = playlistManager.getPlaylist(selectedPlaylistName);
        if (selectedPlaylist == null || selectedPlaylist.isEmpty()) return;

        MediaLibrary library = playlistManager.getMediaLibrary();

        queueManager.addPlaylistToQueue(selectedPlaylist, library);

        queueCreated.set(!queueManager.isEmpty());

        // ✅ fixed block here
        if (mediaPlayer == null && !queueManager.isEmpty()) {
            Song first = queueManager.getCurrentSong();
            if (first == null && !queueManager.getQueue().isEmpty()) {
                first = queueManager.getQueue().get(0);
            }

            if (first != null) {
                media = new Media(first.getSongFile().toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                songLabel.setText(first.getTitle());
                attachMediaPlayListeners();
                mediaPlayer.pause();  // or .play();
            }
        }

        displayQueue();
    }

    /** Switch to songs mode and display songs from selected playlist */
    @FXML
    void songsMode(ActionEvent event) {
        var selection = mediaListView.getSelectionModel().getSelectedItem();
        if (selection != null) {
            listViewMode.set("songs");
            displaySongs(selection);
        }
        else {
            Platform.runLater(() -> {
                showWarning("No Playlist Selected", "Please select a playlist to open.");
            });
        }
    }

    /** Display songs in a playlist, add context menu for adding individual songs to queue */
    private void displaySongs(String name) {
        infoLabel1.setText("Current playlist:");
        infoLabel2.setText(name);

        mediaListView.getItems().clear();
        List<Song> songs = playlistManager.getPlaylist(name);
        if (songs == null) return;

        for (Song song : songs) {
            // Show full file information
            mediaListView.getItems().add(song.toString());
        }

        // === CONTEXT MENU: Add to Queue ===
        mediaListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };

            ContextMenu contextMenu = new ContextMenu();
            MenuItem addToQueue = new MenuItem("Add to Queue");

            addToQueue.setOnAction(ev -> {
                int idx = cell.getIndex();
                if (idx >= 0 && idx < songs.size()) {
                    Song selectedSong = playlistManager.getSongFromPlaylist(name, idx);

                    // Prevent duplicates in queue
                    if (queueManager.getQueue().contains(selectedSong)) {
                        showWarning("Cannot add song to queue", "Song already in queue: " + selectedSong.getTitle());
                        return;
                    }

                    queueManager.addSong(selectedSong, playlistManager.getMediaLibrary());
                    queueCreated.set(true);

                    // Initialize player for first song if needed
                    if (queueManager.getQueueSize() == 1) {
                        media = new Media(selectedSong.getSongFile().toURI().toString());
                        mediaPlayer = new MediaPlayer(media);
                        songLabel.setText(selectedSong.getTitle());
                        attachMediaPlayListeners();
                        mediaPlayer.pause();
                    }

                    listViewMode.set("queue");
                    displayQueue();
                }
            });

            contextMenu.getItems().add(addToQueue);
            cell.setContextMenu(contextMenu);
            return cell;
        });
    }

    /** Clears the current queue, disables playback buttons, and resets UI */
    @FXML
    void clearQueue(ActionEvent event) {
        if (queueManager != null) queueManager.clearQueue();
        mediaListView.getItems().clear();
        queueCreated.set(false);

        if (mediaPlayer != null) {
            try { mediaPlayer.stop(); mediaPlayer.dispose(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        songLabel.setText("Please select a playlist from the Playlist library below");
        listViewMode.set("playlists");
        displayPlaylists();
        songProgressBar.setProgress(0);
        songProgressSlider.setValue(0);
        playbackButton.setText("PLAY");
    }

    // === MEDIA PLAYER EVENT HANDLERS ===

    /** Attaches listeners for progress updates and song end events */
    private void attachMediaPlayListeners() {
        if (mediaPlayer == null) return;

        // Update progress bar/slider as song plays
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (userIsSeeking) return; // Skip if user is actively moving slider
            if (mediaPlayer == null) return;

            if (mediaPlayer.getTotalDuration() != null &&
                    mediaPlayer.getTotalDuration().toMillis() > 0) {

                double progress = newTime.toMillis() / mediaPlayer.getTotalDuration().toMillis();
                songProgressBar.setProgress(progress);
                songProgressSlider.setValue(progress);
            }
        });

        // Advance to next song automatically
        mediaPlayer.setOnEndOfMedia(() -> {
            if (queueManager != null && !queueManager.isEmpty()) {
                queueManager.nextSong();
                updateUI();
            }
        });
    }

    /** Seek to a specific position in the current song */
    @FXML
    void seekTo(MouseEvent event) {
        if (media == null || mediaPlayer == null) return;
        mediaPlayer.seek(Duration.millis(truncateDouble(songProgressSlider.getValue()) * media.getDuration().toMillis()));
    }

    /** Truncate a double to 2 decimal places (used for slider precision) */
    public double truncateDouble(double value) {
        return new BigDecimal(value).setScale(2, RoundingMode.DOWN).doubleValue();
    }

    // === DRAG AND DROP QUEUE MANAGEMENT ===

    /** Enables drag-and-drop reordering and context menu operations in the queue view */
    private void enableQueueDragAndDrop() {
        if (!"queue".equals(listViewMode.get())) return;

        mediaListView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };

            // Context menu for queue management
            ContextMenu contextMenu = new ContextMenu();
            MenuItem moveTop = new MenuItem("Move to Top");
            MenuItem moveBottom = new MenuItem("Move to Bottom");
            MenuItem remove = new MenuItem("Remove");

            moveTop.setOnAction(ev -> moveQueueItem(cell.getIndex(), 0));
            moveBottom.setOnAction(ev -> moveQueueItem(cell.getIndex(), queueManager.getQueueSize() - 1));
            remove.setOnAction(ev -> removeQueueItem(cell.getIndex()));

            contextMenu.getItems().addAll(moveTop, moveBottom, remove);
            cell.setContextMenu(contextMenu);

            // Drag events
            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(String.valueOf(cell.getIndex()));
                    db.setContent(content);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    int dropIndex = cell.getIndex();
                    moveQueueItem(draggedIndex, dropIndex);
                    event.setDropCompleted(true);
                }
                event.consume();
            });

            return cell;
        });
    }

    /** Moves a song in the queue from one index to another */
    private void moveQueueItem(int fromIndex, int toIndex) {
        queueManager.moveSong(fromIndex, toIndex);
        displayQueue(); // Refresh UI after move
    }

    /** Removes a song from the queue */
    private void removeQueueItem(int index) {
        Song song = queueManager.getQueue().get(index);
        queueManager.removeSong(song);

        if (queueManager.isEmpty()) queueCreated.set(false); // Disable Clear Queue if empty
        displayQueue();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // === INITIALIZATION ===
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // No queue exists initially
        queueCreated.set(false);

        // Load playlist directory and initialize managers
        directory = new File("users/test_user1/playlists");
        playlistManager = new PlaylistManager(directory);
        queueManager = new QueueManager();

        // Disable playback buttons until a queue exists
        prevButton.setDisable(true);
        playbackButton.setDisable(true);
        nextButton.setDisable(true);
        playbackButton.setText("PLAY");

        // Clear queue button only enabled when queue exists
        clearQueueButton.disableProperty().bind(queueCreated.not());

        songLabel.setText("Please create a queue from the Playlist library below");

        // Update playback button states whenever queueCreated changes
        queueCreated.addListener((obs, oldVal, newVal) -> {
            prevButton.setDisable(!newVal);
            playbackButton.setDisable(!newVal);
            nextButton.setDisable(!newVal);

            if (!newVal) songLabel.setText("Please select a playlist from the Playlist library below");
        });

        // Initialize ListView mode to playlists
        listViewMode.set("playlists");

        // Listen for mode changes to update UI
        listViewMode.addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "playlists" -> {
                    openSelectedPlaylistButton.setDisable(false);
                    queueSelectedPlaylistButton.setDisable(false);
                    mediaListView.setCellFactory(lv -> new ListCell<>() { // reset factory
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty ? null : item);
                        }
                    });
                    displayPlaylists();
                }
                case "queue" -> {
                    openSelectedPlaylistButton.setDisable(true);
                    queueSelectedPlaylistButton.setDisable(true);
                    displayQueue();
                }
                case "songs" -> {
                    openSelectedPlaylistButton.setDisable(true);
                    queueSelectedPlaylistButton.setDisable(true);
                    mediaListView.setCellFactory(lv -> new ListCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty ? null : item);
                        }
                    });
                }
            }
        });

        // Volume slider listener
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) mediaPlayer.setVolume(newVal.doubleValue() * 0.01);
        });

        // Slider seeking logic
        songProgressSlider.valueChangingProperty().addListener((obs, was, isChanging) -> {
            userIsSeeking = isChanging;
            if (!isChanging && media != null && mediaPlayer != null) {
                mediaPlayer.seek(Duration.millis(songProgressSlider.getValue() * media.getDuration().toMillis()));
            }
        });

        songProgressSlider.setOnMousePressed(event -> userIsSeeking = true);
        songProgressSlider.setOnMouseReleased(event -> {
            userIsSeeking = false;
            if (media != null && mediaPlayer != null) {
                mediaPlayer.seek(Duration.millis(songProgressSlider.getValue() * media.getDuration().toMillis()));
            }
        });

        // Display playlists initially
        displayPlaylists();

        // No MediaPlayer is created initially
        media = null;
        mediaPlayer = null;
    }
}