package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.application.AudioPlayerApplication;

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
 * Main controller for the audio player screen.
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Manage the playback queue (next/previous/play/pause/seek)</li>
 *     <li>Display and switch between playlist library, songs in a playlist, and the active queue</li>
 *     <li>Handle drag-and-drop reordering and context menu actions in the queue</li>
 *     <li>Coordinate with PlaylistManager and QueueManager for data operations</li>
 * </ul>
 */
public class AudioPlayerController implements Initializable {

    // ===================== MANAGERS & MODEL STATE =====================

    /** Provides access to all playlists and songs for the current user. */
    private PlaylistManager playlistManager;

    /** Keeps track of the current playback queue and current song. */
    private QueueManager queueManager;

    /** Currently loaded media file. */
    private Media media;

    /** JavaFX media player instance used to play the current song. */
    private MediaPlayer mediaPlayer;

    /**
     * True while the user is actively dragging the progress slider.
     * Prevents auto-updates from fighting user input.
     */
    private boolean userIsSeeking;

    /** Base directory where this user's playlists are stored on disk. */
    private File directory;

    /**
     * Current mode of the ListView:
     * "playlists" = showing playlist names,
     * "songs" = showing songs from a specific playlist,
     * "queue" = showing the active playback queue.
     */
    private StringProperty listViewMode = new SimpleStringProperty("playlists");

    /** True whenever there is at least one song in the queue. */
    private BooleanProperty queueCreated = new SimpleBooleanProperty(false);

    // ===================== FXML-INJECTED UI CONTROLS =====================

    @FXML private Label songLabel;
    @FXML private Label infoLabel1;
    @FXML private Label infoLabel2;

    @FXML private ProgressBar songProgressBar;
    @FXML private Slider songProgressSlider;

    @FXML private Button nextButton;
    @FXML private Button playbackButton;
    @FXML private Button prevButton;
    @FXML private Button viewSwitchToPlaylistsButton;

    @FXML private Button playlistsDisplayButton;
    @FXML private Button queueDisplayButton;
    @FXML private Button queueSelectedPlaylistButton;
    @FXML private Button openSelectedPlaylistButton;
    @FXML private Button clearQueueButton;

    @FXML private Slider volumeSlider;

    /**
     * Displays one of:
     * <ul>
     *     <li>Playlist titles (playlist mode)</li>
     *     <li>Songs inside a single playlist (songs mode)</li>
     *     <li>Song titles from the active queue (queue mode)</li>
     * </ul>
     */
    @FXML private ListView<String> mediaListView;

    // ===================== BASIC PLAYBACK CONTROLS =====================

    /**
     * Play/Pause button handler.
     * Toggles playback based on the button's current text.
     */
    @FXML
    void playback(ActionEvent event) {
        if ("PLAY".equals(playbackButton.getText())) {
            startPlayback();
            playbackButton.setText("PAUSE");
        } else {
            stopPlayback();
            playbackButton.setText("PLAY");
        }
    }

    /**
     * Starts playback of the currently loaded song.
     * Volume is taken from the volume slider (0 – 100%).
     */
    private void startPlayback() {
        if (mediaPlayer == null) return;
        mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
        mediaPlayer.play();
    }

    /**
     * Pauses the current song if a MediaPlayer exists.
     */
    private void stopPlayback() {
        if (mediaPlayer != null) mediaPlayer.pause();
    }

    /**
     * Button handler to skip to the next song in the queue.
     */
    @FXML
    void queueNextSong(ActionEvent event) {
        nextSong();
    }

    /**
     * Moves the queue forward one song and refreshes the UI.
     * Does nothing if the queue is empty or not initialized.
     */
    private void nextSong() {
        if (queueManager == null || queueManager.isEmpty()) return;
        queueManager.nextSong();
        updateUI();
    }

    /**
     * Button handler to go back to the previous song in the queue.
     */
    @FXML
    void queuePrevSong(ActionEvent event) {
        previousSong();
    }

    /**
     * Moves the queue backward one song and refreshes the UI.
     * Does nothing if the queue is empty or not initialized.
     */
    public void previousSong() {
        if (queueManager == null || queueManager.isEmpty()) return;
        queueManager.prevSong();
        updateUI();
    }

    /**
     * Updates the MediaPlayer and UI labels to reflect the currently
     * selected song in the queue. Also starts playback of that song.
     * <p>
     * If there is no current song, queue state is reset.
     */
    public void updateUI() {
        Song current = (queueManager != null) ? queueManager.getCurrentSong() : null;

        if (current == null) {
            queueCreated.set(false);
            return;
        }

        // Clean up existing player to avoid resource leaks
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) {}
        }

        // Create a new player for the new current song
        media = new Media(current.getSongFile().toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        songLabel.setText(current.getTitle());
        mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);

        attachMediaPlayListeners();
        mediaPlayer.play();

        playbackButton.setText("PAUSE");
    }

    // ===================== LISTVIEW MODE MANAGEMENT =====================

    /**
     * Button handler to show playlist library names in the ListView.
     */
    @FXML
    void playlistMode(ActionEvent event) {
        listViewMode.set("playlists");
    }

    /**
     * Populates the ListView with all playlist names from the PlaylistManager.
     */
    private void displayPlaylists() {
        mediaListView.getItems().clear();
        infoLabel1.setText("Current mode:");
        infoLabel2.setText("Playlist Library view");

        for (String name : playlistManager.getAllPlaylistNames()) {
            mediaListView.getItems().add(name);
        }
    }

    /**
     * Button handler to show the active queue in the ListView.
     */
    @FXML
    void queueMode(ActionEvent event) {
        listViewMode.set("queue");
    }

    /**
     * Populates the ListView with song titles from the current queue and
     * enables drag-and-drop reordering for queue items.
     */
    private void displayQueue() {
        mediaListView.getItems().clear();
        infoLabel1.setText("Current mode:");
        infoLabel2.setText("Queue view");

        if (queueManager != null) {
            for (Song song : queueManager.getQueue()) {
                mediaListView.getItems().add(song.getTitle());
            }
            enableQueueDragAndDrop();
        }
    }

    // ===================== VIEW SWITCHING TO PLAYLIST MANAGER =====================

    /**
     * Switches from the player view back to the playlist manager view.
     * Reuses the existing Stage and passes the current PlaylistManager instance
     * to the PlaylistManagerController.
     */
    @FXML
    void loadPlaylistManagerView(ActionEvent event) {
        try {
            Stage thisCurrentStage = (Stage) viewSwitchToPlaylistsButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    AudioPlayerApplication.class.getResource("/com/example/hummingbird/playlist_manager_view.fxml")
            );
            Scene scene = new Scene(fxmlLoader.load(), 900, 800);

            PlaylistManagerController pmController = fxmlLoader.getController();
            pmController.setPlaylistManager(this.playlistManager);

            thisCurrentStage.setTitle("Hummingbird - Playlist Manager");
            thisCurrentStage.setScene(scene);
            thisCurrentStage.show();

            // Stop and dispose any currently playing media when leaving this view
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ===================== QUEUE MANAGEMENT =====================

    /**
     * Adds all songs from the selected playlist into the queue.
     * <ul>
     *     <li>Shows a warning if no playlist is selected.</li>
     *     <li>Creates a MediaPlayer for the first song if none exists.</li>
     *     <li>Switches the ListView into queue mode afterward.</li>
     * </ul>
     */
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

        // If this is the first time a queue has been created, set up the first song.
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
                // Start in paused state so user explicitly chooses to play
                mediaPlayer.pause();
            }
        }

        displayQueue();
    }

    /**
     * Button handler to open a selected playlist and show its songs.
     * If nothing is selected, a warning dialog is shown.
     */
    @FXML
    void songsMode(ActionEvent event) {
        var selection = mediaListView.getSelectionModel().getSelectedItem();
        if (selection != null) {
            listViewMode.set("songs");
            displaySongs(selection);
        } else {
            Platform.runLater(() ->
                    showWarning("No Playlist Selected", "Please select a playlist to open.")
            );
        }
    }

    /**
     * Displays all songs inside the given playlist in the ListView.
     * <ul>
     *     <li>Each item uses Song.toString() for full info.</li>
     *     <li>Right-click context menu allows adding individual songs to the queue.</li>
     *     <li>Duplicates in the queue are prevented.</li>
     * </ul>
     */
    private void displaySongs(String name) {
        infoLabel1.setText("Current playlist:");
        infoLabel2.setText(name);

        mediaListView.getItems().clear();
        List<Song> songs = playlistManager.getPlaylist(name);
        if (songs == null) return;

        for (Song song : songs) {
            mediaListView.getItems().add(song.toString());
        }

        // Each cell gets a context menu with "Add to Queue"
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

                    // Prevent the same Song instance from being added twice to the queue
                    if (queueManager.getQueue().contains(selectedSong)) {
                        showWarning("Cannot add song to queue",
                                "Song already in queue: " + selectedSong.getTitle());
                        return;
                    }

                    queueManager.addSong(selectedSong, playlistManager.getMediaLibrary());
                    queueCreated.set(true);

                    // If this is the very first song in the queue, set up a MediaPlayer for it
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

    /**
     * Clears the current queue and resets the player UI to the initial state.
     * <ul>
     *     <li>Stops and disposes the MediaPlayer.</li>
     *     <li>Returns the ListView to playlist mode.</li>
     *     <li>Resets labels, buttons, and progress indicators.</li>
     * </ul>
     */
    @FXML
    void clearQueue(ActionEvent event) {
        if (queueManager != null) queueManager.clearQueue();
        mediaListView.getItems().clear();
        queueCreated.set(false);

        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        songLabel.setText("Please select a playlist from the Playlist library below");
        listViewMode.set("playlists");
        displayPlaylists();
        songProgressBar.setProgress(0);
        songProgressSlider.setValue(0);
        playbackButton.setText("PLAY");
    }

    // ===================== MEDIA PLAYER EVENT WIRES =====================

    /**
     * Hooks up listeners to the MediaPlayer:
     * <ul>
     *     <li>Updates progress bar and slider while the song plays.</li>
     *     <li>Automatically advances to the next song when the current one ends.</li>
     * </ul>
     */
    private void attachMediaPlayListeners() {
        if (mediaPlayer == null) return;

        // Update UI as playback time changes
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (userIsSeeking) return;
            if (mediaPlayer == null) return;

            if (mediaPlayer.getTotalDuration() != null &&
                    mediaPlayer.getTotalDuration().toMillis() > 0) {

                double progress = newTime.toMillis() / mediaPlayer.getTotalDuration().toMillis();
                songProgressBar.setProgress(progress);
                songProgressSlider.setValue(progress);
            }
        });

        // When a song finishes, move to the next song in the queue
        mediaPlayer.setOnEndOfMedia(() -> {
            if (queueManager != null && !queueManager.isEmpty()) {
                queueManager.nextSong();
                updateUI();
            }
        });
    }

    /**
     * Called when the user clicks on the progress slider.
     * Jumps playback to a new position in the song based on slider value.
     */
    @FXML
    void seekTo(MouseEvent event) {
        if (media == null || mediaPlayer == null) return;
        mediaPlayer.seek(Duration.millis(
                truncateDouble(songProgressSlider.getValue()) * media.getDuration().toMillis()
        ));
    }

    /**
     * Utility used to limit slider value precision.
     *
     * @param value original double
     * @return value truncated down to 2 decimal places
     */
    public double truncateDouble(double value) {
        return new BigDecimal(value)
                .setScale(2, RoundingMode.DOWN)
                .doubleValue();
    }

    // ===================== DRAG & DROP QUEUE REORDERING =====================

    /**
     * Configures the ListView cells for queue mode to support:
     * <ul>
     *     <li>Right-click context menu (Move to Top/Bottom, Remove)</li>
     *     <li>Drag-and-drop reordering of songs inside the queue</li>
     * </ul>
     * Does nothing if the ListView is not in "queue" mode.
     */
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

            // Context menu options
            ContextMenu contextMenu = new ContextMenu();
            MenuItem moveTop = new MenuItem("Move to Top");
            MenuItem moveBottom = new MenuItem("Move to Bottom");
            MenuItem remove = new MenuItem("Remove");

            moveTop.setOnAction(ev -> moveQueueItem(cell.getIndex(), 0));
            moveBottom.setOnAction(ev -> moveQueueItem(cell.getIndex(), queueManager.getQueueSize() - 1));
            remove.setOnAction(ev -> removeQueueItem(cell.getIndex()));

            contextMenu.getItems().addAll(moveTop, moveBottom, remove);
            cell.setContextMenu(contextMenu);

            // Drag start: store index being dragged
            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(String.valueOf(cell.getIndex()));
                    db.setContent(content);
                    event.consume();
                }
            });

            // Drag over: allow moving items over other cells
            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            // Drop: actually move the item in the queue and refresh UI
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

    /**
     * Moves a song inside the queue from one index to another
     * and refreshes the ListView display.
     */
    private void moveQueueItem(int fromIndex, int toIndex) {
        queueManager.moveSong(fromIndex, toIndex);
        displayQueue();
    }

    /**
     * Removes a song at the given index from the queue.
     * If the queue becomes empty, queueCreated is reset.
     */
    private void removeQueueItem(int index) {
        Song song = queueManager.getQueue().get(index);
        queueManager.removeSong(song);

        if (queueManager.isEmpty()) queueCreated.set(false);
        displayQueue();
    }

    /**
     * Convenience helper to show a simple warning dialog.
     *
     * @param title   dialog title
     * @param message dialog message body
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===================== INITIALIZATION / SETUP =====================

    /**
     * Called automatically after FXML loading is complete.
     * Sets up:
     * <ul>
     *     <li>User-specific playlist directory</li>
     *     <li>PlaylistManager and QueueManager</li>
     *     <li>Initial button states and bindings</li>
     *     <li>Mode listeners (playlists / queue / songs)</li>
     *     <li>Volume and seek slider behavior</li>
     *     <li>Initial ListView content (playlist library)</li>
     * </ul>
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        queueCreated.set(false);

        // Retrieve logged-in user's directory from the shared session
        UserSession session = UserSession.getInstance();
        File userDir = session.getUserDirectory();

        // Fallback if initialize is somehow hit without a valid login
        if (userDir == null) {
            System.err.println("WARNING: No user logged in in UserSession. Falling back to test_user1.");
            userDir = new File("users/test_user1");
        }

        // This user's playlists live under "<userDir>/playlists"
        directory = new File(userDir, "playlists");

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                System.err.println("ERROR: Could not create playlist directory: " + directory.getAbsolutePath());
            }
        }

        // Core model management objects for this view
        playlistManager = new PlaylistManager(directory);
        queueManager = new QueueManager();

        // Initially, playback controls are disabled until a queue exists
        prevButton.setDisable(true);
        playbackButton.setDisable(true);
        nextButton.setDisable(true);
        playbackButton.setText("PLAY");

        // Clear Queue button only works while a queue exists
        clearQueueButton.disableProperty().bind(queueCreated.not());

        // Friendly greeting depending on whether we know the username
        String username = session.getUsername();
        if (username != null) {
            songLabel.setText("Welcome " + username + ", create a queue from the Playlist library below");
        } else {
            songLabel.setText("Please create a queue from the Playlist library below");
        }

        // Enable/disable playback controls whenever queue existence changes
        queueCreated.addListener((obs, oldVal, newVal) -> {
            prevButton.setDisable(!newVal);
            playbackButton.setDisable(!newVal);
            nextButton.setDisable(!newVal);

            if (!newVal) {
                songLabel.setText("Please select a playlist from the Playlist library below");
            }
        });

        // Start in playlist library mode
        listViewMode.set("playlists");

        // Respond to mode changes by updating cell factories and content
        listViewMode.addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "playlists" -> {
                    openSelectedPlaylistButton.setDisable(false);
                    queueSelectedPlaylistButton.setDisable(false);
                    mediaListView.setCellFactory(lv -> new ListCell<>() {
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

        // Keep the MediaPlayer's volume in sync with the volume slider
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue() * 0.01);
            }
        });

        // When the user drags the progress slider, we mark that as "seeking"
        songProgressSlider.valueChangingProperty().addListener((obs, was, isChanging) -> {
            userIsSeeking = isChanging;
            if (!isChanging && media != null && mediaPlayer != null) {
                mediaPlayer.seek(Duration.millis(
                        songProgressSlider.getValue() * media.getDuration().toMillis()
                ));
            }
        });

        // Mouse press/release on the slider also control the seeking flag
        songProgressSlider.setOnMousePressed(event -> userIsSeeking = true);
        songProgressSlider.setOnMouseReleased(event -> {
            userIsSeeking = false;
            if (media != null && mediaPlayer != null) {
                mediaPlayer.seek(Duration.millis(
                        songProgressSlider.getValue() * media.getDuration().toMillis()
                ));
            }
        });

        // Initial view: show all playlists for this user
        displayPlaylists();

        // No song or MediaPlayer is set up until the user creates a queue
        media = null;
        mediaPlayer = null;
    }

}