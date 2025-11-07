package com.example.hummingbird.model;

import javafx.scene.media.MediaPlayer;
import javafx.scene.media.Media;

public class AudioPlayer {
    private MediaPlayer mediaPlayer;
    private Song currentSong;

    public void play(Song song) {}
    public void pause() {}
    public void stop() {}
    public void next() {}
    public void previous() {}
    public void setVolume(double volume) {}
    public Song getCurrentSong() { return currentSong; }
}
