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
 * Handles MP3 importing:
 * - Lets the user pick MP3 files via FileChooser
 * - Copies them into the shared library directory
 * - Creates/returns canonical Song instances from the MediaLibrary
 */
public class MP3FileImporter {

    private final PlaylistManager playlistManager;

    public MP3FileImporter(PlaylistManager playlistManager) {
        this.playlistManager = playlistManager;
    }

    /**
     * Opens a file chooser attached to the given owner window, imports selected MP3s
     * into the library, and returns canonical Song instances via the callback.
     *
     * @param owner      Window to own the file chooser dialog
     * @param onImported Callback receiving the list of imported Songs
     */
    public void importWithDialog(Window owner, Consumer<List<Song>> onImported) {
        if (owner == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import MP3 Files");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("MP3 Files", "*.mp3")
        );

        List<File> selectedFiles = chooser.showOpenMultipleDialog(owner);
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        File libraryDir = playlistManager.getLibraryDirectory();
        if (libraryDir == null) {
            System.out.println("❌ Library directory is null. Check PlaylistManager initialization.");
            return;
        }
        if (!libraryDir.exists() && !libraryDir.mkdirs()) {
            System.out.println("❌ Failed to create library directory: " + libraryDir.getAbsolutePath());
            return;
        }

        MediaLibrary mediaLibrary = playlistManager.getMediaLibrary();
        List<Song> importedSongs = new ArrayList<>();

        for (File sourceFile : selectedFiles) {
            if (sourceFile == null || !sourceFile.exists()) continue;

            File libraryFile = new File(libraryDir, sourceFile.getName());

            // Copy into library if needed
            if (!libraryFile.equals(sourceFile)) {
                try {
                    Files.copy(
                            sourceFile.toPath(),
                            libraryFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                    System.out.println("📥 Imported into library: " + libraryFile.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println("❌ Failed to copy into library: " + sourceFile.getAbsolutePath());
                    continue;
                }
            }

            // Canonical Song from MediaLibrary
            Song canonical = mediaLibrary.getSongByFile(libraryFile);
            if (canonical == null) {
                canonical = new Song(libraryFile);
                mediaLibrary.addSong(canonical);
                System.out.println("🎵 Added new Song to MediaLibrary: " + canonical.getTitle());
            } else {
                System.out.println("↪ Reusing existing Song in MediaLibrary: " + canonical.getTitle());
            }

            importedSongs.add(canonical);
        }

        if (!importedSongs.isEmpty() && onImported != null) {
            onImported.accept(importedSongs);
        }
    }
}