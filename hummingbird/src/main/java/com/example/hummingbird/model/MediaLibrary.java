package com.example.hummingbird.model;

import java.util.List;
import java.util.ArrayList;

public class MediaLibrary {
    private List<Song> library = new ArrayList<>();

    public void addSong(Song song) {}
    public void removeSong(Song song) {}
    public List<Song> getAllSongs() { return library; }
    public Song findSong(String title) { return null; }
}
