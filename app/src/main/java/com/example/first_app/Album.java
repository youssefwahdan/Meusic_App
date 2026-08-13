package com.example.first_app;

import java.util.List;

public class Album {
    private String name;
    private String artist;
    private List<Song> songs;

    public Album(String name, String artist, List<Song> songs) {
        this.name = name;
        this.artist = artist;
        this.songs = songs;
    }

    public String getName() { return name; }
    public String getArtist() { return artist; }
    public List<Song> getSongs() { return songs; }
}