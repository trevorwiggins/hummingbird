package com.example.hummingbird.model;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * In-memory catalog of all songs known to the application.
 *
 * Responsibilities:
 * <ul>
 *     <li>Keep a single canonical {@link Song} instance per audio file.</li>
 *     <li>Provide fast lookup by underlying file.</li>
 *     <li>Expose an ordered list of songs for UI display.</li>
 * </ul>
 *
 * Note: This class only manages in-memory structures. Deleting a song from
 * all playlists or from disk is handled elsewhere (e.g. in PlaylistManager).
 */
public class MediaLibrary {

    /**
     * Backing set of songs used to:
     * <ul>
     *     <li>Guarantee uniqueness (no duplicate Song objects with same file).</li>
     *     <li>Provide efficient membership checks.</li>
     * </ul>
     */
    private Set<Song> librarySet = new HashSet<>();

    /**
     * Ordered list of songs used for:
     * <ul>
     *     <li>Stable iteration order (e.g. as songs were added).</li>
     *     <li>Friendly consumption by UI components such as ListView.</li>
     * </ul>
     */
    private List<Song> libraryList = new ArrayList<>();

    // ===================== MUTATION =====================

    /**
     * Adds a song to the media library, unless an equivalent song
     * is already present.
     *
     * Equivalence is defined by {@link Song#equals(Object)} and
     * {@link Song#hashCode()}, typically based on the underlying file path.
     *
     * @param song the song to add; ignored if {@code null} or already present
     */
    public void addSong(Song song) {
        if (song == null) return;

        // HashSet enforces uniqueness using Song.equals/hashCode
        boolean added = librarySet.add(song);

        // Only maintain insertion order list when the song is truly new
        if (added) {
            libraryList.add(song);
        }
    }

    /**
     * Removes a song from the library's internal collections (set + list).
     * This does not touch any files on disk and does not remove the song
     * from playlists; those operations are handled by higher-level managers.
     *
     * @param song the song to remove
     * @return {@code true} if the song existed in the library and was removed
     */
    public boolean removeSongFromLibrary(Song song) {
        if (song == null) return false;

        boolean removed = librarySet.remove(song);
        if (removed) {
            libraryList.remove(song);
        }
        return removed;
    }

    // ===================== READ ACCESS =====================

    /**
     * Returns all songs currently registered in the media library.
     *
     * @return a new {@link List} containing all songs,
     *         so callers cannot mutate the internal list directly
     */
    public List<Song> getAllSongs() {
        return new ArrayList<>(libraryList);
    }

    /**
     * Searches for a song by its title using a case-insensitive exact match.
     *
     * @param title song title to search for
     * @return the first {@link Song} whose title matches, or {@code null} if none found
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
     * Returns the canonical {@link Song} instance associated with the given file.
     * <p>
     * This ensures callers always operate on the shared Song object already
     * known to the library (rather than creating ad-hoc duplicates).
     *
     * @param file audio file to look up
     * @return the existing Song that wraps this file, or {@code null} if not found
     */
    public Song getSongByFile(File file) {
        if (file == null) return null;

        // Temporary Song object used purely for equality comparison
        Song temp = new Song(file);

        // Use the set for efficient lookup via equals/hashCode semantics
        for (Song s : librarySet) {
            if (s.equals(temp)) return s;
        }
        return null;
    }
}
