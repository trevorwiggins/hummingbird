package com.example.hummingbird.model;

import java.util.LinkedList;
import java.util.Queue;

public class QueueManager {
    private Queue<Song> queue = new LinkedList<>();

    public void addSong(Song song) {}
    public Song getNextSong() { return queue.peek(); }
    public void clearQueue() { queue.clear(); }
    public boolean isEmpty() { return queue.isEmpty(); }
}
