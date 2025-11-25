package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.ui.AudioPlayerUI;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class CreatePlaylistController implements Initializable {

    @FXML private Text infoLabel;
    @FXML private TextField playlistNameTextField;
    @FXML private TextField songSearchTextField;

    @FXML private Button switchToPlaylistManagerViewButton;
    @FXML private Button mediaLibraryButton;
    @FXML private Button addSelectedSongButton;
    @FXML private Button createPlaylistButton;
    @FXML private Button previewPlaylistButton;

    @FXML private ListView<String> mediaListView;

    private PlaylistManager playlistManager;

    // Tracks the canonical Song objects currently shown in the ListView
    private final List<Song> displayedMediaSongs = new ArrayList<>();

    private List<Song> previewPlaylist = new ArrayList<>();
    private final StringProperty listViewMode = new SimpleStringProperty("mediaLibrary");

    public void setPlaylistManager(PlaylistManager pm) {
        this.playlistManager = pm;
        displayMediaLibrary();
    }

    @FXML
    void loadPlaylistManagerView(ActionEvent event) {
        try {
            Stage stage = (Stage) switchToPlaylistManagerViewButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    AudioPlayerUI.class.getResource("/com/example/hummingbird/playlist_manager_view.fxml")
            );
            Scene scene = new Scene(fxmlLoader.load(), 900, 800);

            PlaylistManagerController controller = fxmlLoader.getController();
            controller.setPlaylistManager(this.playlistManager);

            stage.setTitle("Hummingbird - Playlist Manager");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // -----------------------------
    // Add selected song to preview
    // -----------------------------
    @FXML
    void addSelectedSongToPreviewPlaylist(ActionEvent event) {
        int selectedIndex = mediaListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex == -1) {
            showWarning("No Song Selected", "Please select a song to add to your playlist.");
            return;
        }

        // Map ListView index → actual Song using displayedMediaSongs
        Song selectedSong = displayedMediaSongs.get(selectedIndex);

        // Replace with canonical instance from MediaLibrary
        Song canonical = playlistManager.getMediaLibrary().getSongByFile(selectedSong.getSongFile());
        if (canonical != null && !previewPlaylist.contains(canonical)) {
            previewPlaylist.add(canonical);
        }

        listViewMode.set("playlistPreview");
    }

    @FXML
    void showMediaLibrary(ActionEvent event) {
        listViewMode.set("mediaLibrary");
    }

    // -----------------------------
    // MEDIA LIBRARY DISPLAY + FILTER
    // -----------------------------

    private void displayMediaLibrary() {
        updateMediaLibraryView(songSearchTextField.getText());
    }

    private void updateMediaLibraryView(String filterText) {
        if (playlistManager == null || playlistManager.getMediaLibrary() == null) {
            showWarning("No Media Library", "Media Library is not loaded.");
            return;
        }

        infoLabel.setText("Create a playlist from the library below");
        mediaListView.getItems().clear();
        displayedMediaSongs.clear();

        String query = (filterText == null) ? "" : filterText.toLowerCase().trim();

        List<Song> allSongs = playlistManager.getMediaLibrary().getAllSongs();

        for (Song song : allSongs) {

            String title = song.getTitle() == null ? "" : song.getTitle().toLowerCase();
            String artist = song.getArtist() == null ? "" : song.getArtist().toLowerCase();
            String repr = song.toString().toLowerCase();

            if (query.isEmpty()
                    || title.contains(query)
                    || artist.contains(query)
                    || repr.contains(query)) {

                displayedMediaSongs.add(song);
                mediaListView.getItems().add(song.toString());
            }
        }
    }

    // -----------------------------
    // PLAYLIST PREVIEW DISPLAY
    // -----------------------------
    @FXML
    void showPlaylistPreview(ActionEvent event) {
        if (previewPlaylist.isEmpty()) {
            showWarning("Empty playlist preview", "Please add songs to your playlist.");
            return;
        }
        listViewMode.set("playlistPreview");
    }

    private void displayPlaylistPreview() {
        mediaListView.getItems().clear();
        for (Song s : previewPlaylist) {
            mediaListView.getItems().add(s.toString());
        }
    }

    // -----------------------------
    // CREATE PLAYLIST
    // -----------------------------
    @FXML
    void addPlaylistToManager(ActionEvent event) {
        if (previewPlaylist.isEmpty()) {
            showWarning("No Songs Added", "Please add at least one song before creating a playlist.");
            return;
        }

        String playlistName = playlistNameTextField.getText();
        if (playlistName == null || playlistName.isBlank()) {
            showWarning("Invalid Name", "Playlist name cannot be empty.");
            return;
        }

        String trimmedName = playlistName.trim();

        for (String existing : playlistManager.getAllPlaylistNames()) {
            if (existing.equalsIgnoreCase(trimmedName)) {
                showWarning("Duplicate Playlist", "A playlist with this name already exists.");
                return;
            }
        }

        // Build confirmation text
        StringBuilder previewText = new StringBuilder();
        previewText.append("Playlist Name: ").append(trimmedName).append("\n\nSongs to be added:\n");
        for (Song s : previewPlaylist) {
            previewText.append("• ").append(s.getTitle()).append("\n");
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Playlist Creation");
        confirm.setHeaderText("Create playlist \"" + trimmedName + "\"?");
        confirm.setContentText(previewText.toString());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // Convert preview to canonical songs
        List<Song> canonical = new ArrayList<>();
        for (Song s : previewPlaylist) {
            Song c = playlistManager.getMediaLibrary().getSongByFile(s.getSongFile());
            if (c != null && !canonical.contains(c)) {
                canonical.add(c);
            }
        }

        playlistManager.addPlaylist(trimmedName, canonical);

        // Reset UI
        previewPlaylist.clear();
        playlistNameTextField.clear();
        listViewMode.set("mediaLibrary");
        displayMediaLibrary();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Playlist Created");
        alert.setHeaderText(null);
        alert.setContentText("Playlist \"" + trimmedName + "\" was created successfully.");
        alert.showAndWait();
    }

    // -----------------------------
    // UTILS
    // -----------------------------
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // -----------------------------
    // INITIALIZER
    // -----------------------------
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Change behavior based on mode
        listViewMode.addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "mediaLibrary" -> {
                    addSelectedSongButton.setDisable(false);
                    songSearchTextField.setDisable(false);
                    displayMediaLibrary();
                }
                case "playlistPreview" -> {
                    addSelectedSongButton.setDisable(true);
                    songSearchTextField.setDisable(true); // 🔒 disable search box
                    displayPlaylistPreview();
                }
            }
        });

        // Live filtering
        songSearchTextField.textProperty().addListener((obs, oldText, newText) -> {
            if ("mediaLibrary".equals(listViewMode.get())) {
                updateMediaLibraryView(newText);
            }
        });
    }
}
