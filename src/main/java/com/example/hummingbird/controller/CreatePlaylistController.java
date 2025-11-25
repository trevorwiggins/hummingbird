package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.application.AudioPlayerApplication;

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

/**
 * Controller for the "Create Playlist" view.
 *
 * Responsibilities:
 * <ul>
 *     <li>Show the global media library so the user can pick songs.</li>
 *     <li>Maintain a temporary "preview" playlist before it is saved.</li>
 *     <li>Filter the media library as the user types in a search box.</li>
 *     <li>Create a new playlist in PlaylistManager after confirmation.</li>
 *     <li>Navigate back to the Playlist Manager screen.</li>
 * </ul>
 */
public class CreatePlaylistController implements Initializable {

    // ===================== FXML-INJECTED CONTROLS =====================

    /** Instruction / status text displayed above the list. */
    @FXML private Text infoLabel;

    /** Text field where the user enters the name of the new playlist. */
    @FXML private TextField playlistNameTextField;

    /** Text field for live-searching the media library. */
    @FXML private TextField songSearchTextField;

    @FXML private Button switchToPlaylistManagerViewButton;
    @FXML private Button mediaLibraryButton;
    @FXML private Button addSelectedSongButton;
    @FXML private Button createPlaylistButton;
    @FXML private Button previewPlaylistButton;

    /**
     * Displays either media library songs or the preview playlist,
     * depending on the current mode.
     */
    @FXML private ListView<String> mediaListView;

    // ===================== MODEL REFERENCES & STATE =====================

    /** Shared manager that owns all playlists and the media library. */
    private PlaylistManager playlistManager;

    /**
     * The canonical Song objects currently shown in the ListView when in
     * media library mode. This provides a mapping from ListView index
     * → Song instance.
     */
    private final List<Song> displayedMediaSongs = new ArrayList<>();

    /**
     * Temporary list of songs that will become the new playlist
     * once the user confirms creation.
     */
    private List<Song> previewPlaylist = new ArrayList<>();

    /**
     * Tracks what the ListView is currently showing:
     * <ul>
     *     <li>"mediaLibrary" – all songs in the global media library</li>
     *     <li>"playlistPreview" – songs added to the new playlist</li>
     * </ul>
     */
    private final StringProperty listViewMode = new SimpleStringProperty("mediaLibrary");

    /**
     * Injects the PlaylistManager created elsewhere and immediately
     * populates the media library view.
     */
    public void setPlaylistManager(PlaylistManager pm) {
        this.playlistManager = pm;
        displayMediaLibrary();
    }

    // ===================== NAVIGATION =====================

    /**
     * Returns to the Playlist Manager view.
     * Reuses the current Stage and passes the same PlaylistManager instance
     * to the PlaylistManagerController so playlists stay in sync.
     */
    @FXML
    void loadPlaylistManagerView(ActionEvent event) {
        try {
            Stage stage = (Stage) switchToPlaylistManagerViewButton.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(
                    AudioPlayerApplication.class.getResource("/com/example/hummingbird/playlist_manager_view.fxml")
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

    // ===================== ADD SONGS TO PREVIEW PLAYLIST =====================

    /**
     * Adds the currently selected song (from media library view) into
     * the preview playlist.
     * <ul>
     *     <li>Uses displayedMediaSongs to convert ListView index → Song.</li>
     *     <li>Replaces with the canonical Song from MediaLibrary.</li>
     *     <li>Prevents duplicates in the preview playlist.</li>
     *     <li>Switches the view to "playlistPreview" mode afterward.</li>
     * </ul>
     */
    @FXML
    void addSelectedSongToPreviewPlaylist(ActionEvent event) {
        int selectedIndex = mediaListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex == -1) {
            showWarning("No Song Selected", "Please select a song to add to your playlist.");
            return;
        }

        // Map ListView index → actual Song using displayedMediaSongs
        Song selectedSong = displayedMediaSongs.get(selectedIndex);

        // Use the canonical instance from the MediaLibrary
        Song canonical = playlistManager.getMediaLibrary().getSongByFile(selectedSong.getSongFile());
        if (canonical != null && !previewPlaylist.contains(canonical)) {
            previewPlaylist.add(canonical);
        }

        listViewMode.set("playlistPreview");
    }

    /**
     * Button handler that explicitly switches the view back
     * to showing the media library.
     */
    @FXML
    void showMediaLibrary(ActionEvent event) {
        listViewMode.set("mediaLibrary");
    }

    // ===================== MEDIA LIBRARY DISPLAY & FILTERING =====================

    /**
     * Convenience method to show the media library using the current
     * text in the search field as a filter.
     */
    private void displayMediaLibrary() {
        updateMediaLibraryView(songSearchTextField.getText());
    }

    /**
     * Updates the media library ListView according to the given filter string.
     * <ul>
     *     <li>Filters by title, artist, or the Song.toString() representation.</li>
     *     <li>Clears and repopulates both the ListView and displayedMediaSongs.</li>
     *     <li>Shows an error if the MediaLibrary is not available.</li>
     * </ul>
     *
     * @param filterText text typed by the user into the search field
     */
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

            // Simple case-insensitive search across multiple fields
            if (query.isEmpty()
                    || title.contains(query)
                    || artist.contains(query)
                    || repr.contains(query)) {

                displayedMediaSongs.add(song);
                mediaListView.getItems().add(song.toString());
            }
        }
    }

    // ===================== PLAYLIST PREVIEW DISPLAY =====================

    /**
     * Button handler that switches into preview mode, but only if at least
     * one song has been added. Otherwise, shows a warning.
     */
    @FXML
    void showPlaylistPreview(ActionEvent event) {
        if (previewPlaylist.isEmpty()) {
            showWarning("Empty playlist preview", "Please add songs to your playlist.");
            return;
        }
        listViewMode.set("playlistPreview");
    }

    /**
     * Populates the ListView with songs from the preview playlist.
     */
    private void displayPlaylistPreview() {
        mediaListView.getItems().clear();
        for (Song s : previewPlaylist) {
            mediaListView.getItems().add(s.toString());
        }
    }

    // ===================== CREATE PLAYLIST =====================

    /**
     * Creates a new playlist in PlaylistManager based on the preview playlist.
     * Steps:
     * <ol>
     *     <li>Validate that there is at least one song.</li>
     *     <li>Validate the playlist name (non-empty, non-duplicate).</li>
     *     <li>Show a confirmation dialog listing all song titles.</li>
     *     <li>Map songs to their canonical instances in the MediaLibrary.</li>
     *     <li>Call PlaylistManager.addPlaylist(..).</li>
     *     <li>Reset UI and show a success dialog.</li>
     * </ol>
     */
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

        // Check for name collisions (case-insensitive)
        for (String existing : playlistManager.getAllPlaylistNames()) {
            if (existing.equalsIgnoreCase(trimmedName)) {
                showWarning("Duplicate Playlist", "A playlist with this name already exists.");
                return;
            }
        }

        // Build a human-readable confirmation message
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

        // Ensure we store canonical Song instances owned by the MediaLibrary
        List<Song> canonical = new ArrayList<>();
        for (Song s : previewPlaylist) {
            Song c = playlistManager.getMediaLibrary().getSongByFile(s.getSongFile());
            if (c != null && !canonical.contains(c)) {
                canonical.add(c);
            }
        }

        playlistManager.addPlaylist(trimmedName, canonical);

        // Reset UI back to "fresh state" after creation
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

    // ===================== UTILITIES =====================

    /**
     * Convenience method to show a warning alert with a title and message.
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
     * Called automatically after FXML loading.
     * <ul>
     *     <li>Sets up behavior when switching between "mediaLibrary" and "playlistPreview".</li>
     *     <li>Enables live filtering of the media library as the user types.</li>
     *     <li>Note: PlaylistManager is injected later via setPlaylistManager(..).</li>
     * </ul>
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // React whenever the view mode changes
        listViewMode.addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case "mediaLibrary" -> {
                    addSelectedSongButton.setDisable(false);
                    songSearchTextField.setDisable(false);
                    displayMediaLibrary();
                }
                case "playlistPreview" -> {
                    addSelectedSongButton.setDisable(true);
                    // Do not allow filtering while in preview mode
                    songSearchTextField.setDisable(true);
                    displayPlaylistPreview();
                }
            }
        });

        // Live search: refilter the media library only while in mediaLibrary mode
        songSearchTextField.textProperty().addListener((obs, oldText, newText) -> {
            if ("mediaLibrary".equals(listViewMode.get())) {
                updateMediaLibraryView(newText);
            }
        });
    }
}