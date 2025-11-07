package com.example.hummingbird.model;

public class Song {
    private String title;
    private String artist;
    private String filePath;
    private double duration;

    public Song(String title, String artist, String filePath, double duration) {
        this.title = title;
        this.artist = artist;
        this.filePath = filePath;
        this.duration = duration;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getFilePath() { return filePath; }
    public double getDuration() { return duration; }
}
