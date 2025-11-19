package com.example.hummingbird.model;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;

/**
 * Represents a single audio track.
 * Stores metadata (title, artist), file information, and duration.
 */
public class Song {

    // === SONG METADATA ===
    private String title;       // Song title (defaults to file name)
    private String artist;      // Song artist (defaults to "unknown")

    // === FILE INFORMATION ===
    private File songFile;      // File object representing the audio file
    private String filePath;    // File path as URI string (used by JavaFX Media)

    private double duration;    // Duration in seconds (populated asynchronously)

    /**
     * Constructs a Song from a given audio file.
     * Initializes title, artist, file info, and retrieves duration asynchronously using JavaFX MediaPlayer.
     * @param songFile the audio file (.mp3) for this song
     */
    public Song(File songFile) {
        this.title = songFile.getName(); // Use file name as default title
        this.artist = "unknown";         // Default artist
        this.songFile = songFile;
        this.filePath = songFile.toURI().toString();

        // Create a JavaFX Media object to load the audio file
        Media media = new Media(filePath);

        // MediaPlayer is used to asynchronously get duration
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnReady(() -> {
            duration = media.getDuration().toSeconds(); // Store duration in seconds
            // Uncomment for debugging
            // System.out.println("Duration loaded for " + title + ": " + duration);
        });
    }

    // === GETTERS AND SETTERS ===

    /**
     * @return song title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return artist name
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Sets the artist name
     * @param artist artist name
     */
    public void setArtist(String artist) {
        this.artist = artist;
    }

    /**
     * @return file path as a URI string (used by JavaFX MediaPlayer)
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @return song duration in seconds
     */
    public double getDuration() {
        return duration;
    }

    /**
     * @return underlying File object for this song
     */
    public File getSongFile() {
        return songFile;
    }

    /**
     * Returns a readable string representation of the song.
     * Includes title, artist, and duration.
     * @return formatted string
     */
    @Override
    public String toString() {
        // Format duration as an integer number of seconds
        String formattedDuration = String.format("%.0f", duration);
        return title + " | Artist: " + artist + " | Duration: " + formattedDuration + " secs";
    }
}
