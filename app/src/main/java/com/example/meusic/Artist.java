package com.example.meusic;

import java.util.List;

public class Artist {
    private String name;
    private int songCount;
    private List<Song> songs;

    public Artist(String name, List<Song> songs) {
        this.name = name;
        this.songs = songs;
        this.songCount = songs.size();
    }

    public String getName() { return name; }
    public int getSongCount() { return songCount; }
    public List<Song> getSongs() { return songs; }
}