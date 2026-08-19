package com.example.first_app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "playlist_songs")
public class PlaylistSongEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int playlistId;
    public long songId;
    public int orderIndex; // To keep the songs in the order the user added them

    public PlaylistSongEntity(int playlistId, long songId, int orderIndex) {
        this.playlistId = playlistId;
        this.songId = songId;
        this.orderIndex = orderIndex;
    }
}