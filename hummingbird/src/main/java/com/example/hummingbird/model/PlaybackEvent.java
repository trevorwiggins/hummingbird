package com.example.hummingbird.model;

public class PlaybackEvent {
    private PlaybackEventType eventType;
    private Song song;

    public PlaybackEvent(PlaybackEventType eventType, Song song) {
        this.eventType = eventType;
        this.song = song;
    }

    public PlaybackEventType getEventType() { return eventType; }
    public Song getSong() { return song; }
}
