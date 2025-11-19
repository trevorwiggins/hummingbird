package com.example.hummingbird.model;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Manages all playlists for the application.
 * Each playlist is a mapping from a playlist name to a list of songs.
 */
public class PlaylistManager {

    // === STORE PLAYLISTS ===
    // Key: playlist name, Value: list of songs in that playlist
    private Map<String, List<Song>> playlists;

    /**
     * Initializes the PlaylistManager by scanning a directory for playlists.
     * Each subdirectory in 'directory' is treated as a playlist,
     * and each .mp3 file inside a subdirectory is added to that playlist.
     * @param directory root directory containing playlist subdirectories
     */
    public PlaylistManager(File directory) {
        playlists = new HashMap<>();

        // Defensive check for invalid directory
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid playlist directory: " + directory);
            return;
        }

        // Get all subdirectories (each represents a playlist)
        File[] playlistFolders = directory.listFiles(File::isDirectory);
        if (playlistFolders == null) return;

        for (File folder : playlistFolders) {
            String playlistName = folder.getName();
            List<Song> songs = new ArrayList<>();

            // Add all .mp3 files from this folder to the playlist
            File[] songFiles = folder.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".mp3"));
            if (songFiles != null) {
                for (File songFile : songFiles) {
                    songs.add(new Song(songFile));
                }
            }

            playlists.put(playlistName, songs);
        }
    }

    /**
     * Adds a new empty playlist with the given name.
     * If a playlist with that name already exists, nothing happens.
     * @param name name of the new playlist
     */
    public void addPlaylist(String name) {
        playlists.putIfAbsent(name, new ArrayList<>());
    }

    /**
     * Deletes a playlist with the given name.
     * If the playlist does not exist, nothing happens.
     * @param name name of the playlist to delete
     */
    public void deletePlaylist(String name) {
        playlists.remove(name);
    }

    /**
     * Adds a song to the specified playlist.
     * If the playlist does not exist, it is created.
     * @param song         Song to add
     * @param playlistName Name of the playlist
     */
    public void addSongToPlaylist(Song song, String playlistName) {
        playlists.computeIfAbsent(playlistName, k -> new ArrayList<>()).add(song);
    }

    /**
     * Removes a song from the specified playlist.
     * If the playlist does not exist, nothing happens.
     * @param song         Song to remove
     * @param playlistName Name of the playlist
     */
    public void removeSongFromPlaylist(Song song, String playlistName) {
        List<Song> list = playlists.get(playlistName);
        if (list != null) list.remove(song);
    }

    /**
     * Returns a song from a specific playlist by its index.
     * @param playlistName Playlist name
     * @param index        Index of song (0-based)
     * @return Song at given index, or null if playlist does not exist or index is invalid
     */
    public Song getSongFromPlaylist(String playlistName, int index) {
        List<Song> list = playlists.get(playlistName);
        if (list == null || index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    /**
     * Returns the full list of songs for a given playlist.
     * @param playlistName Name of the playlist
     * @return List of songs, or empty list if playlist does not exist
     */
    public List<Song> getPlaylist(String playlistName) {
        return playlists.getOrDefault(playlistName, new ArrayList<>());
    }

    /**
     * Returns an array of all playlist names.
     * @return playlist names
     */
    public String[] getAllPlaylistNames() {
        if (playlists == null || playlists.isEmpty()) return new String[0];
        return playlists.keySet().toArray(new String[0]);
    }

    /**
     * Generates a readable string representation of all playlists and their songs.
     * Useful for console logging or debugging.
     * @return formatted string of playlists
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (playlists.isEmpty()) {
            sb.append("No playlists available.\n");
            return sb.toString();
        }

        for (Map.Entry<String, List<Song>> entry : playlists.entrySet()) {
            String playlistName = entry.getKey();
            List<Song> songs = entry.getValue();

            sb.append("Playlist: ").append(playlistName).append("\n");
            if (songs.isEmpty()) sb.append("  (No songs in this playlist)\n");
            else {
                for (int i = 0; i < songs.size(); i++) {
                    sb.append("  ").append(i + 1).append(". ").append(songs.get(i)).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}