package com.example.hummingbird.model;

public interface PlaybackListener {
    void onPlay(Song song);
    void onPause(Song song);
    void onStop(Song song);
    void onNext(Song song);
    void onPrevious(Song song);
}
