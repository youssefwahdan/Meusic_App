package com.example.first_app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchItem {
    public static final int TYPE_SONG = 0;
    public static final int TYPE_ARTIST = 1;
    public static final int TYPE_ALBUM = 2;

    public int type;
    public String title;
    public String subtitle;
    public Object data; // Holds the actual Song, Artist, or Album object
    private List<Song> songs;
    public SearchItem(int type, String title, String subtitle, Object data, List<Song> songs) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.data = data;
        this.songs = songs != null ? songs : Collections.emptyList();
    }

    public List<Song> getSongs() {
        return songs;
    }
}