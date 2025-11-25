package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.ui.AudioPlayerUI;

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

public class PlaylistManagerController implements Initializable {

    @FXML private Label infoLabel1;
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

    @FXML private HBox mainContent; // Main container to blur

    private PlaylistManager playlistManager;
    private MP3FileImporter mp3Importer;   // importer that writes to library

    // "playlists" or "songs"
    private StringProperty listViewMode = new SimpleStringProperty("playlists");
    // Name of currently open playlist in "songs" mode, or null if showing library/all songs
    private String currentPlaylist = null;

    // ============================================
    // Wiring in PlaylistManager
    // ============================================

    public void setPlaylistManager(PlaylistManager pm) {
        this.playlistManager = pm;
        this.mp3Importer = new MP3FileImporter(playlistManager);
        displayPlaylists();
    }

    // ============================================
    // Display helpers
    // ============================================

    private void displayPlaylists() {
        infoLabel1.setText("Currently displaying Playlist Library");
        mediaListView.getItems().clear();
        currentPlaylist = null;

        for (String name : playlistManager.getAllPlaylistNames()) {
            mediaListView.getItems().add(name);
        }

        // In playlist list view, add/remove song do not make sense
        addSongButton.setDisable(true);
        removeSongButton.setDisable(true);
    }

    private void displaySongs(String name) {
        currentPlaylist = name; // track which playlist is open
        infoLabel1.setText("Current playlist: " + name);
        mediaListView.getItems().clear();

        List<Song> songs = playlistManager.getPlaylist(name);
        if (songs != null) {
            for (Song song : songs) {
                mediaListView.getItems().add(song.toString());
            }
        }

        // We are inside a playlist; enable add/remove
        addSongButton.setDisable(false);
        removeSongButton.setDisable(false);
    }

    // ============================================
    // Mode switching
    // ============================================

    @FXML
    void playlistMode(ActionEvent event) {
        listViewMode.set("playlists");
    }

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

    // ============================================
    // Add / Remove song from CURRENT PLAYLIST
    // ============================================

    /**
     * Add an EXISTING library song to the currently open playlist.
     * - Only works in "songs" mode with a non-null currentPlaylist.
     * - Uses short labels (song titles) in the dialog so the layout doesn't explode.
     */
    @FXML
    void addSongToPlaylist(ActionEvent event) {
        // Must be inside a playlist view
        if (!"songs".equals(listViewMode.get()) || currentPlaylist == null) {
            showWarning("Add Song Disabled",
                    "Please open a playlist before trying to add songs.");
            return;
        }

        // Get all songs in library and in this playlist
        List<Song> librarySongs = playlistManager.getMediaLibrary().getAllSongs();
        List<Song> playlistSongs = playlistManager.getPlaylist(currentPlaylist);

        // Build list of candidates NOT already in this playlist
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

        // === Build a custom dialog with Label + ComboBox ===
        Dialog<Song> dialog = new Dialog<>();
        dialog.setTitle("Add Song to Playlist");
        dialog.setHeaderText("Select a song from your library to add to this playlist:");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // ComboBox with Song objects
        javafx.scene.control.ComboBox<Song> comboBox =
                new javafx.scene.control.ComboBox<>(javafx.collections.FXCollections.observableArrayList(candidates));

        // Show short labels (title if available, else toString)
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

        // Layout: "Song:" label on the left, ComboBox on the right
        javafx.scene.control.Label label = new javafx.scene.control.Label("Song:");
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(10, 10, 10, 10));

        grid.add(label, 0, 0);
        grid.add(comboBox, 1, 0);

        // Give everything room so nothing wraps vertically
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

        // Add song to this playlist (no deletion/import)
        playlistManager.addSongToPlaylist(chosenSong, currentPlaylist);
        playlistManager.savePlaylistsToDisk();

        // Refresh view
        displaySongs(currentPlaylist);
    }



    /**
     * Remove the selected song ONLY from the currently open playlist.
     * Does NOT delete from library or other playlists.
     */
    @FXML
    void removeSongFromPlaylist(ActionEvent event) {
        // Must be inside a playlist view
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

        // Find matching Song in current playlist
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

        // Confirm removal
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Song From Playlist");
        confirm.setHeaderText(null);
        confirm.setContentText("Remove this song from playlist \"" +
                currentPlaylist + "\"?\n\n" + selectedDisplay);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // Remove from this playlist only
        playlistManager.removeSongFromPlaylist(songToRemove, currentPlaylist);
        playlistManager.savePlaylistsToDisk();

        // Refresh view
        displaySongs(currentPlaylist);
    }

    // ============================================
    // Import MP3 (unchanged behavior)
    // ============================================

    /**
     * Import MP3s ONLY if:
     * - we're in "playlists" mode and a playlist is selected in the ListView, OR
     * - we're in "songs" mode and currently viewing a specific playlist (currentPlaylist != null).
     */
    @FXML
    void importMP3(ActionEvent event) {
        if (playlistManager == null || mp3Importer == null) return;

        String mode = listViewMode.get();
        String targetPlaylistName = null;

        if ("playlists".equals(mode)) {
            targetPlaylistName = mediaListView.getSelectionModel().getSelectedItem();
        } else if ("songs".equals(mode)) {
            // Only allowed if we're looking at a specific playlist, not the entire library
            targetPlaylistName = currentPlaylist;
        } else {
            // Any other mode: disallow import
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

        // Open dialog, import into library, then attach imported songs to that playlist
        mp3Importer.importWithDialog(importMP3Button.getScene().getWindow(), importedSongs -> {
            if (importedSongs == null || importedSongs.isEmpty()) return;

            // Ensure playlist exists without overwriting its contents
            if (!playlistManager.playlistExists(playlistName)) {
                playlistManager.addPlaylist(playlistName, new ArrayList<>());
            }

            // Append imported songs to the existing playlist
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

    // ============================================
    // Delete Media (Playlist or Song) with pop-up
    // ============================================

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
            // Load pop-up FXML
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

            // Apply blur to main content
            mainContent.setEffect(new GaussianBlur(10));
            mainContent.setDisable(true);
            popupStage.show();

            // Background deletion task
            Task<Void> deleteTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(750); // simulate deletion delay

                    if (listViewMode.get().equals("playlists")) {
                        playlistManager.deletePlaylist(selection);
                    } else {
                        // Find the Song object corresponding to the selected string
                        Song songToDelete = null;

                        // Try in the current playlist first
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

                        // If not found in current playlist (viewing media library), search the library
                        if (songToDelete == null && playlistManager.getMediaLibrary() != null) {
                            for (Song s : playlistManager.getMediaLibrary().getAllSongs()) {
                                if (s.toString().equals(selection)) {
                                    songToDelete = s;
                                    break;
                                }
                            }
                        }

                        if (songToDelete != null) {
                            // This is the "delete everywhere" behavior
                            playlistManager.removeSongEverywhere(songToDelete);
                        }
                    }

                    // Ensure disk state matches in-memory playlists
                    playlistManager.savePlaylistsToDisk();

                    return null;
                }
            };

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

    // ============================================
    // View all songs (MediaLibrary)
    // ============================================

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

        // Library view: add/remove disabled
        addSongButton.setDisable(true);
        removeSongButton.setDisable(true);
    }

    // ============================================
    // Navigation helpers
    // ============================================

    @FXML
    void loadCreatePlaylistView(ActionEvent event) {
        try {
            Stage thisCurrentStage = (Stage) switchToPlayerButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    AudioPlayerUI.class.getResource("/com/example/hummingbird/create_playlist_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 800);

            // Pass the PlaylistManager
            CreatePlaylistController cpController = fxmlLoader.getController();
            cpController.setPlaylistManager(this.playlistManager);

            thisCurrentStage.setTitle("Hummingbird - Create a Playlist");
            thisCurrentStage.setScene(scene);
            thisCurrentStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void loadPlayerView(ActionEvent event) {
        try {
            Stage thisCurrentStage = (Stage) switchToPlayerButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    AudioPlayerUI.class.getResource("/com/example/hummingbird/player_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 900, 800);
            thisCurrentStage.setTitle("Hummingbird - Player");
            thisCurrentStage.setScene(scene);
            thisCurrentStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ============================================
    // Utility
    // ============================================

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        deleteMediaButton.setText("Delete Playlist");
        addSongButton.setDisable(true);
        removeSongButton.setDisable(true);

        // Bind delete button text and some state to current mode.
        // NOTE: add/remove enablement is handled in displayPlaylists, displaySongs, and loadSongsFromMediaLibrary.
        listViewMode.addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "playlists" -> {
                    openPlaylistButton.setDisable(false);
                    deleteMediaButton.setText("Delete Playlist");
                    displayPlaylists();
                }
                case "songs" -> {
                    openPlaylistButton.setDisable(true);
                    // For library vs playlist, the label text isn't critical,
                    // but we keep "Delete Song" here.
                    deleteMediaButton.setText("Delete Song");
                }
            }
        });
    }
}
