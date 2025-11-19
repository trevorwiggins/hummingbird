package com.example.hummingbird.model;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

/**
 * Represents a single audio track with metadata such as title, artist, duration,
 * and the underlying audio file.
 */
public class Song {
    private String title;      // Song title (from metadata or fallback to filename)
    private String artist;     // Song artist (from metadata or "unknown")
    private File songFile;     // Original file reference
    private double duration;   // Duration of the song in seconds

    /**
     * Constructs a Song object from a File and reads its metadata using jaudiotagger.
     * If metadata is unavailable, defaults are used.
     *
     * @param songFile the audio file
     */
    public Song(File songFile) {
        this.songFile = songFile;

        // Default values in case metadata is not available
        this.title = songFile.getName(); // fallback to filename
        this.artist = "unknown";         // fallback artist
        this.duration = 0;               // fallback duration

        try {
            // Read the audio file with jaudiotagger
            AudioFile audioFile = AudioFileIO.read(songFile);
            Tag tag = audioFile.getTag();

            // If metadata tags exist, extract title and artist
            if (tag != null) {
                String tagTitle = tag.getFirst(FieldKey.TITLE);
                String tagArtist = tag.getFirst(FieldKey.ARTIST);

                if (tagTitle != null && !tagTitle.isEmpty()) this.title = tagTitle;
                if (tagArtist != null && !tagArtist.isEmpty()) this.artist = tagArtist;
            }

            // Get the duration from the audio header (in seconds)
            this.duration = audioFile.getAudioHeader().getTrackLength();

        } catch (CannotReadException e) {
            // File cannot be read as an audio file
            System.out.println("Cannot read audio file: " + songFile.getName());
        } catch (Exception e) {
            // General fallback for any other metadata reading issues
            System.out.println("Error reading metadata: " + e.getMessage());
        }
    }

    // === Getters ===
    public String getTitle() { return title; }       // Returns song title
    public String getArtist() { return artist; }     // Returns artist name
    public File getSongFile() { return songFile; }   // Returns original file
    public double getDuration() { return duration; } // Returns duration in seconds

    // === Setters ===
    public void setArtist(String artist) { this.artist = artist; }

    @Override
    public String toString() {
        // Formats song information for display: Title | Artist | Duration
        return String.format("%s | Artist: %s | Duration: %.0f secs", title, artist, duration);
    }
}
