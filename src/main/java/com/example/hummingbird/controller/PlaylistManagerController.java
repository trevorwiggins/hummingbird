package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.application.AudioPlayerApplication;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Controller for the Playlist Manager screen.
 *
 * Responsibilities:
 * <ul>
 *     <li>Display all playlists and songs owned by the current user.</li>
 *     <li>Support switching between "playlist view" and "songs view".</li>
 *     <li>Allow creating, deleting, and editing playlists.</li>
 *     <li>Allow importing MP3 files into the user's library and playlists.</li>
 *     <li>Coordinate with the Deletion popup and the Create Playlist / Player views.</li>
 * </ul>
 */
public class PlaylistManagerController implements Initializable {

    // ===================== FXML-INJECTED UI CONTROLS =====================

    /** Label that describes what the ListView is currently showing. */
    @FXML private Label infoLabel1;

    /**
     * Main list used to display either:
     * <ul>
     *     <li>Playlist names (playlist mode)</li>
     *     <li>Songs inside a single playlist or from the whole library (songs mode)</li>
     * </ul>
     */
    @FXML private ListView<String> mediaListView;

    @FXML private Button playlistsDisplayButton;
    @FXML private Button openPlaylistButton;
    @FXML private Button importMP3Button;
    @FXML private Button createPlaylistButton;
    @FXML private Button deleteMediaButton;
    @FXML private Button viewAllSongsButton;
    @FXML private Button addSongButton;
    @FXML private Button removeSongButton;
    @FXML private Button switchToPlayerButton;

    /** Root HBox for the screen; blurred while deletion popup is active. */
    @FXML private HBox mainContent;

    // ===================== MODEL REFERENCES & STATE =====================

    /** Manages all playlists and the media library for this user. */
    private PlaylistManager playlistManager;

    /** Helper that handles file-chooser and importing MP3s into the library. */
    private MP3FileImporter mp3Importer;

    /**
     * Current mode of the ListView:
     * <ul>
     *     <li>"playlists" – list of playlist names</li>
     *     <li>"songs" – songs from a specific playlist or the entire library</li>
     * </ul>
     */
    private StringProperty listViewMode = new SimpleStringProperty("playlists");

    /**
     * Name of the playlist currently being viewed in "songs" mode.
     * <p>
     * If this is {@code null} while in songs mode, the view is showing
     * "all songs from Media Library" instead of a single playlist.
     */
    private String currentPlaylist = null;

    // ===================== PLAYLIST MANAGER WIRING =====================

    /**
     * Injects the PlaylistManager used by this screen.
     * Also constructs an MP3FileImporter and renders the initial playlist list.
     *
     * @param pm shared PlaylistManager instance
     */
    public void setPlaylistManager(PlaylistManager pm) {
        this.playlistManager = pm;
        this.mp3Importer = new MP3FileImporter(playlistManager);
        displayPlaylists();
    }

    // ===================== DISPLAY HELPERS =====================

    /**
     * Populates the ListView with all playlist names.
     * Switches into "library of playlists" mode:
     * <ul>
     *     <li>Each row is a playlist name.</li>
     *     <li>Add/Remove song buttons are disabled (they only make sense inside a playlist).</li>
     * </ul>
     */
    private void displayPlaylists() {
        infoLabel1.setText("Currently displaying Playlist Library");
        mediaListView.getItems().clear();
        currentPlaylist = null;

        for (String name : playlistManager.getAllPlaylistNames()) {
            mediaListView.getItems().add(name);
        }

        // In playlist mode we are not editing a specific playlist yet
        addSongButton.setDisable(true);
        removeSongButton.setDisable(true);
    }

    /**
     * Populates the ListView with songs from a specific playlist and enables
     * playlist-specific operations.
     *
     * @param name name of the playlist to display
     */
    private void displaySongs(String name) {
        currentPlaylist = name;
        infoLabel1.setText("Current playlist: " + name);
        mediaListView.getItems().clear();

        List<Song> songs = playlistManager.getPlaylist(name);
        if (songs != null) {
            for (Song song : songs) {
                mediaListView.getItems().add(song.toString());
            }
        }

        // Inside a playlist, songs can be added or removed
        addSongButton.setDisable(false);
        removeSongButton.setDisable(false);
    }

    // ===================== MODE SWITCHING =====================

    /**
     * Handler for switching the ListView to "playlists" mode.
     * Used when clicking a "Playlists" button.
     */
    @FXML
    void playlistMode(ActionEvent event) {
        listViewMode.set("playlists");
    }

    /**
     * Handler for switching the ListView to "songs" mode by opening the
     * currently selected playlist.
     * If nothing is selected, shows a warning.
     */
    @FXML
    void songsMode(ActionEvent event) {
        String selection = mediaListView.getSelectionModel().getSelectedItem();
        if (selection != null) {
            listViewMode.set("songs");
            displaySongs(selection);
        } else {
            showWarning("No Playlist Selected", "Please select a playlist to open.");
        }
    }

    // ===================== ADD / REMOVE SONG IN CURRENT PLAYLIST =====================

    /**
     * Adds an existing library song to the currently open playlist.
     * <ul>
     *     <li>Only works if we are in "songs" mode and a specific playlist is open.</li>
     *     <li>Shows a dialog listing songs that are not already in the playlist.</li>
     *     <li>Uses the song's title as the display label where possible.</li>
     * </ul>
     */
    @FXML
    void addSongToPlaylist(ActionEvent event) {
        // Must be viewing a specific playlist
        if (!"songs".equals(listViewMode.get()) || currentPlaylist == null) {
            showWarning("Add Song Disabled",
                    "Please open a playlist before trying to add songs.");
            return;
        }

        // All songs in the library + songs already in this playlist
        List<Song> librarySongs = playlistManager.getMediaLibrary().getAllSongs();
        List<Song> playlistSongs = playlistManager.getPlaylist(currentPlaylist);

        // Candidate songs are those not already in the current playlist
        List<Song> candidates = new ArrayList<>();
        for (Song s : librarySongs) {
            if (!playlistSongs.contains(s)) {
                candidates.add(s);
            }
        }

        if (candidates.isEmpty()) {
            showWarning("No Songs Available",
                    "All songs in your library are already in this playlist.");
            return;
        }

        // --- Build a compact dialog to avoid layout issues ---

        Dialog<Song> dialog = new Dialog<>();
        dialog.setTitle("Add Song to Playlist");
        dialog.setHeaderText("Select a song from your library to add to this playlist:");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // ComboBox with Song objects as options
        javafx.scene.control.ComboBox<Song> comboBox =
                new javafx.scene.control.ComboBox<>(javafx.collections.FXCollections.observableArrayList(candidates));

        // Cell renderer: show either the title or the full toString()
        comboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Song item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String label = (item.getTitle() != null && !item.getTitle().isBlank())
                            ? item.getTitle()
                            : item.toString();
                    setText(label);
                }
            }
        });

        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Song item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String label = (item.getTitle() != null && !item.getTitle().isBlank())
                            ? item.getTitle()
                            : item.toString();
                    setText(label);
                }
            }
        });

        comboBox.getSelectionModel().selectFirst();

        // Simple "Song: [comboBox]" layout
        javafx.scene.control.Label label = new javafx.scene.control.Label("Song:");
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10, 10, 10, 10));

        grid.add(label, 0, 0);
        grid.add(comboBox, 1, 0);

        // Give the dialog enough width so labels don't wrap vertically
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(450);

        dialog.setResultConverter(button -> {
            if (button == addButtonType) {
                return comboBox.getValue();
            }
            return null;
        });

        Optional<Song> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == null) return;

        Song chosenSong = result.get();

        // Persist change to playlist + disk
        playlistManager.addSongToPlaylist(chosenSong, currentPlaylist);
        playlistManager.savePlaylistsToDisk();

        displaySongs(currentPlaylist);
    }

    /**
     * Removes the selected song from the currently open playlist.
     * <ul>
     *     <li>Does not delete the song from the library or other playlists.</li>
     *     <li>Only operates in "songs" mode while a specific playlist is open.</li>
     * </ul>
     */
    @FXML
    void removeSongFromPlaylist(ActionEvent event) {
        if (!"songs".equals(listViewMode.get()) || currentPlaylist == null) {
            showWarning("Remove Song Disabled",
                    "Please open a playlist before trying to remove songs.");
            return;
        }

        String selectedDisplay = mediaListView.getSelectionModel().getSelectedItem();
        if (selectedDisplay == null) {
            showWarning("No Song Selected",
                    "Please select a song to remove from this playlist.");
            return;
        }

        // Find matching Song object based on toString() representation
        Song songToRemove = null;
        List<Song> songsInPlaylist = playlistManager.getPlaylist(currentPlaylist);
        for (Song s : songsInPlaylist) {
            if (s.toString().equals(selectedDisplay)) {
                songToRemove = s;
                break;
            }
        }

        if (songToRemove == null) {
            showWarning("Error", "Could not find the selected song in the current playlist.");
            return;
        }

        // User confirmation
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Song From Playlist");
        confirm.setHeaderText(null);
        confirm.setContentText("Remove this song from playlist \"" +
                currentPlaylist + "\"?\n\n" + selectedDisplay);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        playlistManager.removeSongFromPlaylist(songToRemove, currentPlaylist);
        playlistManager.savePlaylistsToDisk();

        displaySongs(currentPlaylist);
    }

    // ===================== MP3 IMPORT =====================

    /**
     * Imports MP3 files into the user's library and links them into a specific playlist.
     * <ul>
     *     <li>Allowed when:
     *         <ul>
     *             <li>In "playlists" mode with a playlist selected, OR</li>
     *             <li>In "songs" mode viewing a specific playlist (not library view).</li>
     *         </ul>
     *     </li>
     *     <li>Imported songs are stored in the shared library and also added to the chosen playlist.</li>
     * </ul>
     */
    @FXML
    void importMP3(ActionEvent event) {
        if (playlistManager == null || mp3Importer == null) return;

        String mode = listViewMode.get();
        String targetPlaylistName = null;

        if ("playlists".equals(mode)) {
            targetPlaylistName = mediaListView.getSelectionModel().getSelectedItem();
        } else if ("songs".equals(mode)) {
            // Only allowed if showing a specific playlist, not all-library view
            targetPlaylistName = currentPlaylist;
        } else {
            showWarning("Import Disabled",
                    "Please select a playlist in the Playlist Library,\n" +
                            "or open a specific playlist before importing MP3 files.");
            return;
        }

        if (targetPlaylistName == null || targetPlaylistName.isBlank()) {
            showWarning("No Playlist Selected",
                    "Please select a playlist in the Playlist Library,\n" +
                            "or open a specific playlist before importing MP3 files.");
            return;
        }

        final String playlistName = targetPlaylistName;
        final String finalMode = mode;

        // Show file chooser, then import and attach songs to that playlist
        mp3Importer.importWithDialog(importMP3Button.getScene().getWindow(), importedSongs -> {
            if (importedSongs == null || importedSongs.isEmpty()) return;

            // Ensure playlist exists; do not overwrite existing content
            if (!playlistManager.playlistExists(playlistName)) {
                playlistManager.addPlaylist(playlistName, new ArrayList<>());
            }

            for (Song s : importedSongs) {
                playlistManager.addSongToPlaylist(s, playlistName);
            }

            playlistManager.savePlaylistsToDisk();

            final int count = importedSongs.size();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Import Complete");
                alert.setHeaderText(null);
                alert.setContentText("Successfully imported " + count +
                        " MP3 file(s) into playlist: " + playlistName);
                alert.showAndWait();

                if ("playlists".equals(finalMode)) {
                    displayPlaylists();
                } else if ("songs".equals(finalMode)) {
                    displaySongs(playlistName);
                }
            });
        });
    }

    // ===================== DELETE PLAYLIST / SONG WITH POPUP =====================

    /**
     * Deletes either:
     * <ul>
     *     <li>The selected playlist (in "playlists" mode), OR</li>
     *     <li>The selected song (in "songs" mode).</li>
     * </ul>
     * <p>
     * When deleting a song, it is removed everywhere using
     * {@link PlaylistManager#removeSongEverywhere(Song)}.
     * A small popup with a progress indicator is shown while deletion runs
     * on a background thread. The main content is blurred during this time.
     */
    @FXML
    void deleteMedia(ActionEvent event) {
        String selection = mediaListView.getSelectionModel().getSelectedItem();
        if (selection == null) {
            showWarning("No Selection", "Please select a " +
                    listViewMode.get().substring(0, listViewMode.get().length() - 1) + " to delete.");
            return;
        }

        String itemType = listViewMode.get().equals("playlists") ? "playlist" : "song";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete " + itemType + " Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you'd like to delete this " + itemType + "?\n" + selection);
        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            // Load the deletion popup
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/hummingbird/deletion_popup.fxml"));
            Scene popupScene = new Scene(loader.load());
            Stage popupStage = new Stage();
            popupStage.setScene(popupScene);
            popupStage.setTitle("Deleting " + itemType + "...");
            popupStage.initOwner(switchToPlayerButton.getScene().getWindow());
            popupStage.initModality(Modality.WINDOW_MODAL);
            popupStage.setResizable(false);

            DeletionPopupController popupController = loader.getController();
            popupController.setStage(popupStage);

            // Blur the main content while deletion is in progress
            mainContent.setEffect(new GaussianBlur(10));
            mainContent.setDisable(true);
            popupStage.show();

            // Background task that handles slow deletion / disk I/O
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    // Small delay purely for visual feedback of "work in progress"
                    Thread.sleep(750);

                    if (listViewMode.get().equals("playlists")) {
                        playlistManager.deletePlaylist(selection);
                    } else {
                        // Delete a song everywhere
                        Song songToDelete = null;

                        // Try to find the Song in the current playlist first
                        if (currentPlaylist != null) {
                            List<Song> songs = playlistManager.getPlaylist(currentPlaylist);
                            if (songs != null) {
                                for (Song s : songs) {
                                    if (s.toString().equals(selection)) {
                                        songToDelete = s;
                                        break;
                                    }
                                }
                            }
                        }

                        // If not found and we might be in "all songs" view, search the entire library
                        if (songToDelete == null && playlistManager.getMediaLibrary() != null) {
                            for (Song s : playlistManager.getMediaLibrary().getAllSongs()) {
                                if (s.toString().equals(selection)) {
                                    songToDelete = s;
                                    break;
                                }
                            }
                        }

                        if (songToDelete != null) {
                            playlistManager.removeSongEverywhere(songToDelete);
                        }
                    }

                    playlistManager.savePlaylistsToDisk();
                    return null;
                }
            };

            // When deletion finishes, show a brief success message, then close popup and refresh UI
            deleteTask.setOnSucceeded(e -> {
                popupController.updateStatus(
                        itemType.substring(0, 1).toUpperCase() + itemType.substring(1) + " deleted!");
                PauseTransition wait = new PauseTransition(Duration.seconds(0.8));
                wait.setOnFinished(ev -> {
                    popupController.close();
                    mainContent.setEffect(null);
                    mainContent.setDisable(false);

                    if (listViewMode.get().equals("playlists")) {
                        displayPlaylists();
                    } else {
                        if (currentPlaylist != null) {
                            displaySongs(currentPlaylist);
                        } else {
                            loadSongsFromMediaLibrary(null);
                        }
                    }
                });
                wait.play();
            });

            new Thread(deleteTask).start();

        } catch (IOException ex) {
            ex.printStackTrace();
            showWarning("Error", "Failed to show deletion popup.");
        }
    }

    // ===================== VIEW ALL SONGS FROM MEDIA LIBRARY =====================

    /**
     * Displays every song from the MediaLibrary (not tied to a specific playlist).
     * <ul>
     *     <li>Switches to "songs" mode.</li>
     *     <li>Sets currentPlaylist to null.</li>
     *     <li>Disables Add/Remove song buttons because this is a library view, not a playlist.</li>
     * </ul>
     */
    @FXML
    void loadSongsFromMediaLibrary(ActionEvent event) {
        if (playlistManager == null || playlistManager.getMediaLibrary() == null) {
            showWarning("No Media Library", "Media Library is not loaded.");
            return;
        }

        infoLabel1.setText("Displaying all songs from Media Library");
        mediaListView.getItems().clear();
        currentPlaylist = null;

        List<Song> allSongs = playlistManager.getMediaLibrary().getAllSongs();
        for (Song song : allSongs) {
            mediaListView.getItems().add(song.toString());
        }

        listViewMode.set("songs");

        // Library-wide view: cannot add/remove relative to a specific playlist
        addSongButton.setDisable(true);
        removeSongButton.setDisable(true);
    }

    // ===================== NAVIGATION TO OTHER SCREENS =====================

    /**
     * Loads the "Create Playlist" view, passing the existing PlaylistManager
     * so any new playlists are immediately visible to the rest of the app.
     */
    @FXML
    void loadCreatePlaylistView(ActionEvent event) {
        try {
            Stage thisCurrentStage = (Stage) switchToPlayerButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    AudioPlayerApplication.class.getResource("/com/example/hummingbird/create_playlist_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 800);

            CreatePlaylistController cpController = fxmlLoader.getController();
            cpController.setPlaylistManager(this.playlistManager);

            thisCurrentStage.setTitle("Hummingbird - Create a Playlist");
            thisCurrentStage.setScene(scene);
            thisCurrentStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Switches from the Playlist Manager to the main Player view.
     * Uses the username from UserSession for the window title.
     */
    @FXML
    void loadPlayerView(ActionEvent event) {
        try {
            Stage thisCurrentStage = (Stage) switchToPlayerButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    AudioPlayerApplication.class.getResource("/com/example/hummingbird/player_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 800);
            String username = UserSession.getInstance().getUsername();
            thisCurrentStage.setTitle("Hummingbird - " + username);
            thisCurrentStage.setScene(scene);
            thisCurrentStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ===================== UTILITY =====================

    /**
     * Shows a simple warning dialog with the given title and message.
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ===================== INITIALIZATION =====================

    /**
     * Initializes UI state and sets up reactions to mode changes.
     * <ul>
     *     <li>Defaults delete button text and disables song-editing buttons.</li>
     *     <li>Updates delete button label and playlist-open button availability based on the view mode.</li>
     *     <li>Actual ListView content is loaded later when PlaylistManager is injected.</li>
     * </ul>
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        deleteMediaButton.setText("Delete Playlist");
        addSongButton.setDisable(true);
        removeSongButton.setDisable(true);

        // React when switching between "playlists" and "songs" modes
        // (The actual enabling/disabling of add/remove happens inside display methods.)
        listViewMode.addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "playlists" -> {
                    openPlaylistButton.setDisable(false);
                    deleteMediaButton.setText("Delete Playlist");
                    displayPlaylists();
                }
                case "songs" -> {
                    openPlaylistButton.setDisable(true);
                    deleteMediaButton.setText("Delete Song");
                }
            }
        });
    }
}