package com.example.hummingbird.model;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * MediaLibrary stores all unique songs currently available in the application,
 * regardless of which playlist they belong to.
 *
 * Removing a song:
 *  - removes it from the media library
 *  - removes it from EVERY playlist (via PlaylistManager)
 *  - deletes the underlying .mp3 file from disk
 */
public class MediaLibrary {

    // Set prevents duplicates
    private Set<Song> librarySet = new HashSet<>();

    // List keeps insertion order and is UI-friendly
    private List<Song> libraryList = new ArrayList<>();


    /**
     * Adds a song to the media library if it is not already present.
     * Duplicate songs (same file path) are ignored.
     */
    public void addSong(Song song) {
        if (song == null) return;

        // HashSet prevents duplicates based on Song.equals() + hashCode()
        boolean added = librarySet.add(song);

        // Only add to list if it was truly new
        if (added) {
            libraryList.add(song);
        }
    }


    /**
     * Removes a song from the MediaLibrary data structures (list + set)
     * but does NOT delete the file.
     * @return true if the song was present and removed
     */
    public boolean removeSongFromLibrary(Song song) {
        if (song == null) return false;

        boolean removed = librarySet.remove(song);
        if (removed) {
            libraryList.remove(song);
        }
        return removed;
    }

    /**
     * @return all songs in the media library as a safe-copied list.
     */
    public List<Song> getAllSongs() {
        return new ArrayList<>(libraryList);
    }


    /**
     * Finds a song by title (case-insensitive exact match).
     */
    public Song findSong(String title) {
        if (title == null) return null;

        for (Song s : libraryList) {
            if (title.equalsIgnoreCase(s.getTitle())) {
                return s;
            }
        }
        return null;
    }

    /**
     * Returns the canonical Song instance for a given File, or null if not found.
     * Uses equals/hashCode of Song (which checks canonicalPath) for safe lookup.
     */
    public Song getSongByFile(File file) {
        if (file == null) return null;

        // Temporary Song object to leverage equals/hashCode
        Song temp = new Song(file);

        // HashSet lookup is fast
        for (Song s : librarySet) {
            if (s.equals(temp)) return s;
        }
        return null;
    }
}
