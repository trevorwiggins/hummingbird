package com.example.hummingbird.model;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility for importing MP3 files into the application's shared library.
 *
 * Responsibilities:
 * <ul>
 *     <li>Show a {@link FileChooser} for selecting one or more .mp3 files.</li>
 *     <li>Copy the chosen files into the user's library directory.</li>
 *     <li>Create or reuse canonical {@link Song} instances in the {@link MediaLibrary}.</li>
 *     <li>Return the imported Song objects to the caller via a callback.</li>
 * </ul>
 *
 * This class does not directly modify playlists; it only populates the library.
 * Callers (e.g. controllers) decide which playlist(s) to attach the imported songs to.
 */
public class MP3FileImporter {

    /** PlaylistManager used to locate the library directory and MediaLibrary. */
    private final PlaylistManager playlistManager;

    /**
     * Creates a new importer bound to a specific {@link PlaylistManager}.
     *
     * @param playlistManager manager that owns the media library and library folder
     */
    public MP3FileImporter(PlaylistManager playlistManager) {
        this.playlistManager = playlistManager;
    }

    /**
     * Opens a file chooser attached to the given window and imports the selected MP3 files.
     * <p>
     * For each chosen file:
     * <ol>
     *     <li>Copies it into the library directory (if it's not already there).</li>
     *     <li>Finds or creates the canonical {@link Song} in the {@link MediaLibrary}.</li>
     *     <li>Collects each imported Song into a list.</li>
     * </ol>
     * Once all imports have finished, the provided callback is invoked with the list of
     * imported songs. If the user cancels the dialog or nothing is imported, the callback
     * is not invoked.
     *
     * @param owner      window that will own the file chooser dialog
     * @param onImported callback that receives all imported {@link Song} instances;
     *                   may be {@code null} if the caller does not need results
     */
    public void importWithDialog(Window owner, Consumer<List<Song>> onImported) {
        if (owner == null) return;

        // Configure the file chooser to only show MP3 files
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import MP3 Files");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("MP3 Files", "*.mp3")
        );

        // Let the user select one or more MP3 files
        List<File> selectedFiles = chooser.showOpenMultipleDialog(owner);
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return; // user cancelled or picked nothing
        }

        // Ensure library directory exists
        File libraryDir = playlistManager.getLibraryDirectory();
        if (libraryDir == null) {
            System.out.println("Library directory is null. Check PlaylistManager initialization.");
            return;
        }
        if (!libraryDir.exists() && !libraryDir.mkdirs()) {
            System.out.println("Failed to create library directory: " + libraryDir.getAbsolutePath());
            return;
        }

        MediaLibrary mediaLibrary = playlistManager.getMediaLibrary();
        List<Song> importedSongs = new ArrayList<>();

        // Process each selected source file
        for (File sourceFile : selectedFiles) {
            if (sourceFile == null || !sourceFile.exists()) continue;

            File libraryFile = new File(libraryDir, sourceFile.getName());

            // Copy the file into the library directory if it's not already there
            if (!libraryFile.equals(sourceFile)) {
                try {
                    Files.copy(
                            sourceFile.toPath(),
                            libraryFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    System.out.println("Imported into library: " + libraryFile.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("Failed to copy into library: " + sourceFile.getAbsolutePath());
                    continue;
                }
            }

            // Get or create the canonical Song associated with this file
            Song canonical = mediaLibrary.getSongByFile(libraryFile);
            if (canonical == null) {
                canonical = new Song(libraryFile);
                mediaLibrary.addSong(canonical);
                System.out.println("Added new Song to MediaLibrary: " + canonical.getTitle());
            } else {
                System.out.println("Reusing existing Song in MediaLibrary: " + canonical.getTitle());
            }

            importedSongs.add(canonical);
        }

        // Inform caller about the imported songs
        if (!importedSongs.isEmpty() && onImported != null) {
            onImported.accept(importedSongs);
        }
    }
}