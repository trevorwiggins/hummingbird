package com.example.hummingbird.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the playback queue for songs.
 * Handles adding, removing, navigating, and reordering songs in the queue.
 */
public class QueueManager {

    // === QUEUE STORAGE ===
    private final List<Song> queue = new ArrayList<>(); // List of songs in the queue
    private int index = -1; // Current song index. -1 means "no song selected"

    // === QUEUE MANAGEMENT ===

    /**
     * Adds a song to the end of the queue.
     * If this is the first song added, sets it as the current song.
     * @param song Song to add (ignored if null)
     */
    public void addSong(Song song) {
        if (song == null) return;
        queue.add(song);

        // If this is the first song added, set index to 0
        if (index == -1) index = 0;
    }

    /**
     * Returns the current song in the queue.
     * @return current Song or null if queue is empty / no song selected
     */
    public Song getCurrentSong() {
        if (index < 0 || index >= queue.size()) return null;
        return queue.get(index);
    }

    /**
     * Advances to the next song in the queue.
     * Loops back to the start if at the end.
     * @return new current Song, or null if queue is empty
     */
    public Song nextSong() {
        if (queue.isEmpty()) return null;

        // Move index forward with wrap-around
        index = (index + 1) % queue.size();
        return getCurrentSong();
    }

    /**
     * Moves to the previous song in the queue.
     * Loops to the end if currently at the first song.
     * @return new current Song, or null if queue is empty
     */
    public Song prevSong() {
        if (queue.isEmpty()) return null;

        // Move index backward with wrap-around
        index = (index - 1 + queue.size()) % queue.size();
        return getCurrentSong();
    }

    /**
     * Clears all songs from the queue.
     * Resets the current song index.
     */
    public void clearQueue() {
        queue.clear();
        index = -1;
    }

    /**
     * Checks if the queue is empty.
     * @return true if no songs in queue
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Returns the number of songs in the queue.
     * @return queue size
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * Returns the index of the current song.
     * @return current index, -1 if no song selected
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the list representing the queue.
     * Changes to the list directly affect the queue.
     * @return queue list
     */
    public List<Song> getQueue() {
        return queue;
    }

    // === QUEUE REORDERING ===

    /**
     * Moves a song from one position to another within the queue.
     * Adjusts the current song index if needed.
     * @param fromIndex index of song to move
     * @param toIndex   index to move song to
     */
    public void moveSong(int fromIndex, int toIndex) {
        // Validate indices
        if (fromIndex < 0 || fromIndex >= queue.size() || toIndex < 0 || toIndex >= queue.size()) return;
        if (fromIndex == toIndex) return; // no change needed

        // Remove the song and insert at new position
        Song song = queue.remove(fromIndex);
        queue.add(toIndex, song);

        // Adjust current index if needed
        if (index == fromIndex) index = toIndex; // if moving current song
        else if (fromIndex < index && toIndex >= index) index--; // current song shifts left
        else if (fromIndex > index && toIndex <= index) index++; // current song shifts right
    }

    // === QUEUE REMOVAL ===

    /**
     * Removes a specific song from the queue.
     * Updates current index appropriately.
     * @param song Song to remove
     */
    public void removeSong(Song song) {
        if (song == null || queue.isEmpty()) return;

        int songIndex = queue.indexOf(song);
        if (songIndex == -1) return; // song not in queue

        queue.remove(songIndex);

        if (queue.isEmpty()) {
            // Queue is now empty
            index = -1;
        } else if (songIndex < index) {
            // Removed a song before current song, shift current index left
            index--;
        } else if (songIndex == index) {
            // Removed the currently playing song
            if (index >= queue.size()) index = queue.size() - 1; // move to last song if needed
        }
    }

    /**
     * Removes a song from the queue by its index.
     * @param indexToRemove index of song to remove
     */
    public void removeSong(int indexToRemove) {
        if (indexToRemove < 0 || indexToRemove >= queue.size()) return;
        Song song = queue.get(indexToRemove);
        removeSong(song); // reuse removal logic
    }
}
