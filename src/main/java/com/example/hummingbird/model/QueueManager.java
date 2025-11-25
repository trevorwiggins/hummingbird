package com.example.hummingbird.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the in-memory playback queue.
 *
 * Responsibilities:
 * <ul>
 *     <li>Store an ordered list of songs to be played.</li>
 *     <li>Track which song in the list is currently "active".</li>
 *     <li>Support adding entire playlists to the queue (without duplicates).</li>
 *     <li>Allow navigation (next/previous), clearing, and reordering.</li>
 * </ul>
 *
 * The queue is purely in memory and not persisted to disk.
 */
public class QueueManager {

    // ===================== QUEUE STORAGE =====================

    /** Ordered list of songs currently in the playback queue. */
    private final List<Song> queue = new ArrayList<>();

    /**
     * Index of the current song in the queue.
     * <ul>
     *     <li>-1 means "no current song" (queue empty or not initialized).</li>
     *     <li>0..(queue.size() - 1) points at the active song.</li>
     * </ul>
     */
    private int index = -1;

    // ===================== ADDING SONGS =====================

    /**
     * Adds a song to the end of the queue, using the canonical instance
     * from the given {@link MediaLibrary}.
     * <ul>
     *     <li>If the song (by file) is already in the queue, it is ignored.</li>
     *     <li>If this is the first song added, the current index becomes 0.</li>
     * </ul>
     *
     * @param song         song to add (ignored if {@code null})
     * @param mediaLibrary library used to obtain canonical Song based on file
     */
    public void addSong(Song song, MediaLibrary mediaLibrary) {
        if (song == null || mediaLibrary == null) return;

        // Resolve canonical Song instance based on underlying file
        Song canonical = mediaLibrary.getSongByFile(song.getSongFile());
        if (canonical == null) canonical = song;

        Song canonicalSong = canonical;

        // Prevent duplicates in the queue
        if (queue.stream().anyMatch(s -> s.equals(canonicalSong))) return;

        queue.add(canonicalSong);

        // If this is the first song added, start the index at 0
        if (index == -1) index = 0;
    }

    /**
     * Appends all songs from a playlist to the queue.
     * <ul>
     *     <li>Each song is canonicalized via {@link #addSong(Song, MediaLibrary)}.</li>
     *     <li>Duplicates (by Song equality) are automatically skipped.</li>
     * </ul>
     *
     * @param songs        playlist songs to add
     * @param mediaLibrary library used to canonicalize songs
     */
    public void addPlaylistToQueue(List<Song> songs, MediaLibrary mediaLibrary) {
        if (songs == null || mediaLibrary == null) return;

        for (Song s : songs) {
            // Delegates duplicate prevention and canonicalization to addSong
            addSong(s, mediaLibrary);
        }
    }

    // ===================== CURRENT SONG & NAVIGATION =====================

    /**
     * Returns the current song in the queue.
     *
     * @return current {@link Song}, or {@code null} if the queue is empty
     *         or the index is not set
     */
    public Song getCurrentSong() {
        if (index < 0 || index >= queue.size()) return null;
        return queue.get(index);
    }

    /**
     * Advances the current index to the next song in the queue.
     * <ul>
     *     <li>If at the end, wraps around to the first song.</li>
     *     <li>Does nothing if the queue is empty.</li>
     * </ul>
     *
     * @return the new current {@link Song}, or {@code null} if the queue is empty
     */
    public Song nextSong() {
        if (queue.isEmpty()) return null;

        index = (index + 1) % queue.size();
        return getCurrentSong();
    }

    /**
     * Moves the current index to the previous song in the queue.
     * <ul>
     *     <li>If at the beginning, wraps around to the last song.</li>
     *     <li>Does nothing if the queue is empty.</li>
     * </ul>
     *
     * @return the new current {@link Song}, or {@code null} if the queue is empty
     */
    public Song prevSong() {
        if (queue.isEmpty()) return null;

        index = (index - 1 + queue.size()) % queue.size();
        return getCurrentSong();
    }

    // ===================== CLEAR & STATUS =====================

    /**
     * Clears all songs from the queue and resets the current index.
     */
    public void clearQueue() {
        queue.clear();
        index = -1;
    }

    /**
     * Checks whether the queue currently contains any songs.
     *
     * @return {@code true} if there are no songs in the queue
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the total number of songs in the queue.
     *
     * @return queue size
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Returns the zero-based index of the current song.
     *
     * @return current index, or -1 if no song is selected
     */
    public int getIndex() {
        return index;
    }

    /**
     * Exposes the underlying list used as the queue.
     * <p>
     * Note: modifications to the returned list directly affect the queue.
     * Use with care.
     *
     * @return the internal queue list
     */
    public List<Song> getQueue() {
        return queue;
    }

    // ===================== REORDERING =====================

    /**
     * Moves a song from one position to another inside the queue.
     * Also adjusts the current index if needed so that the "current song"
     * remains consistent.
     *
     * @param fromIndex index of the song to move
     * @param toIndex   target position for the song
     */
    public void moveSong(int fromIndex, int toIndex) {
        // Validate indices
        if (fromIndex < 0 || fromIndex >= queue.size()
                || toIndex < 0 || toIndex >= queue.size()) return;
        if (fromIndex == toIndex) return;

        Song song = queue.remove(fromIndex);
        queue.add(toIndex, song);

        // Adjust current index depending on how the list shifted
        if (index == fromIndex) {
            // Moved the currently playing song
            index = toIndex;
        } else if (fromIndex < index && toIndex >= index) {
            // Removed an item before current and inserted after/at current
            index--;
        } else if (fromIndex > index && toIndex <= index) {
            // Removed an item after current and inserted before/at current
            index++;
        }
    }

    // ===================== REMOVAL =====================

    /**
     * Removes the given song from the queue and adjusts the current index
     * if necessary.
     *
     * @param song song to remove (ignored if not found)
     */
    public void removeSong(Song song) {
        if (song == null || queue.isEmpty()) return;

        int songIndex = queue.indexOf(song);
        if (songIndex == -1) return;

        queue.remove(songIndex);

        if (queue.isEmpty()) {
            // Nothing left in the queue
            index = -1;
        } else if (songIndex < index) {
            // Removed a song before the current one, shift index left
            index--;
        } else if (songIndex == index) {
            // Removed the current song:
            // either stay on the same index (now pointing to the next song),
            // or clamp to last index if we removed the last element.
            if (index >= queue.size()) {
                index = queue.size() - 1;
            }
        }
    }
}
