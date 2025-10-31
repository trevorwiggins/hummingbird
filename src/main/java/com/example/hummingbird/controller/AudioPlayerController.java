package com.example.hummingbird.controller;

import com.example.hummingbird.model.*;
import com.example.hummingbird.ui.AudioPlayerUI;
import java.util.List;

public class AudioPlayerController {
    private AudioPlayer audioPlayer;
    private PlaylistManager playlistManager;
    private QueueManager queueManager;
    private MediaLibrary mediaLibrary;
    private AudioPlayerUI view;

    public void playSong(Song song) {}
    public void pauseSong() {}
    public void nextSong() {}
    public void previousSong() {}
    public void addSongToQueue(Song song) {}
    public void addSongToPlaylist(Song song, String playlistName) {}
    public void playPlaylist(String playlistName) {}
    public void loopSong(Song song) {}
    public void updateUI() {}
}
