package com.example.hummingbird.model;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;

/**
 * Represents a single audio track in the application.
 *
 * Responsibilities:
 * <ul>
 *     <li>Wrap the underlying audio {@link File}.</li>
 *     <li>Expose metadata such as title, artist, and duration.</li>
 *     <li>Provide a stable identity based on the canonical file path
 *         (used by collections like {@link java.util.Set}).</li>
 * </ul>
 *
 * Metadata is loaded using the jaudiotagger library. If metadata
 * cannot be read, sensible fallback values are used.
 */
public class Song {

    /** Song title (metadata TITLE tag, or file name when missing). */
    private String title;

    /** Song artist (metadata ARTIST tag, or "unknown" when missing). */
    private String artist;

    /** Underlying audio file on disk. */
    private File songFile;

    /** Duration of the track in seconds (from audio header). */
    private double duration;

    /**
     * Constructs a Song from an audio file and attempts to read its metadata.
     * <ul>
     *     <li>If metadata is available, the title and artist are populated from tags.</li>
     *     <li>If metadata is missing or unreadable, defaults are used:
     *         <ul>
     *             <li>title = file name</li>
     *             <li>artist = "unknown"</li>
     *             <li>duration = 0</li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * @param songFile the audio file backing this {@code Song}
     */
    public Song(File songFile) {
        this.songFile = songFile;

        // Default fallbacks in case tag reading fails
        this.title = songFile.getName();
        this.artist = "unknown";
        this.duration = 0;

        try {
            // Use jaudiotagger to read metadata and audio header
            AudioFile audioFile = AudioFileIO.read(songFile);
            Tag tag = audioFile.getTag();

            // Extract basic tags if they exist
            if (tag != null) {
                String tagTitle = tag.getFirst(FieldKey.TITLE);
                String tagArtist = tag.getFirst(FieldKey.ARTIST);

                if (tagTitle != null && !tagTitle.isEmpty()) {
                    this.title = tagTitle;
                }
                if (tagArtist != null && !tagArtist.isEmpty()) {
                    this.artist = tagArtist;
                }
            }

            // Duration in seconds from the audio header
            this.duration = audioFile.getAudioHeader().getTrackLength();

        } catch (CannotReadException e) {
            // File exists but cannot be interpreted as an audio file
            System.out.println("Cannot read audio file: " + songFile.getName());
        } catch (Exception e) {
            // Any other metadata-related errors
            System.out.println("Error reading metadata: " + e.getMessage());
        }
    }

    // ===================== GETTERS =====================

    /** @return display title of this song. */
    public String getTitle() { return title; }

    /** @return artist name associated with this song. */
    public String getArtist() { return artist; }

    /** @return underlying audio file on disk. */
    public File getSongFile() { return songFile; }

    /** @return length of the track in seconds. */
    public double getDuration() { return duration; }

    // ===================== SETTERS =====================

    /**
     * Updates the artist name. This only affects the in-memory representation
     * and does not write anything back to the file metadata.
     *
     * @param artist new artist name
     */
    public void setArtist(String artist) { this.artist = artist; }

    // ===================== OBJECT CONTRACT =====================

    /**
     * Returns a human-readable representation of this Song,
     * used by UI components such as ListView.
     *
     * Format: {@code Title | Artist: X | Duration: N secs}
     */
    @Override
    public String toString() {
        return String.format("%s | Artist: %s | Duration: %.0f secs", title, artist, duration);
    }

    /**
     * Two Song instances are considered equal if they refer to the same
     * audio file on disk (based on canonical path, or absolute path as
     * a fallback).
     *
     * This equality definition allows:
     * <ul>
     *     <li>Hash-based collections ({@link java.util.HashSet}) to treat
     *         duplicate file references as the same logical song.</li>
     *     <li>Managers like {@link MediaLibrary} and {@link PlaylistManager}
     *         to de-duplicate songs by file path.</li>
     * </ul>
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Song)) return false;

        Song other = (Song) o;

        try {
            return this.songFile.getCanonicalPath()
                    .equals(other.songFile.getCanonicalPath());
        } catch (Exception e) {
            // Fallback if canonical path cannot be resolved
            return this.songFile.getAbsolutePath()
                    .equals(other.songFile.getAbsolutePath());
        }
    }

    /**
     * Computes a hash code based on the file path used in {@link #equals(Object)}.
     *
     * @return hash code derived from canonical path, or absolute path on failure
     */
    @Override
    public int hashCode() {
        try {
            return this.songFile.getCanonicalPath().hashCode();
        } catch (Exception e) {
            return this.songFile.getAbsolutePath().hashCode();
        }
    }
}
