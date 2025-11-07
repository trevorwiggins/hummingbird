package com.example.hummingbird.model;

import java.util.List;
import java.util.Map;

public class PlaylistManager {
    private Map<String, List<Song>> playlists;

    public void addPlaylist(String name) {}
    public void deletePlaylist(String name) {}
    public void addSongToPlaylist(Song song, String playlistName) {}
    public void removeSongFromPlaylist(Song song, String playlistName) {}
    public List<Song> getPlaylist(String playlistName) { return playlists.get(playlistName); }
}
