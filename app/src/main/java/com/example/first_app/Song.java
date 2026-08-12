package com.example.first_app;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private long duration; // in milliseconds
    private String data;   // file path
    private String dateAdded;
    public Song(long id, String title, String artist, String album, long duration, String data, String dateAdded) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.data = data;
        this.dateAdded = dateAdded;
    }

    // Getters
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public long getDuration() { return duration; }
    public String getData() { return data; }
    public String getDate() { return dateAdded; }
}