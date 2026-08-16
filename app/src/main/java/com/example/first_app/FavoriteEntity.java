package com.example.first_app;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteEntity {
    @PrimaryKey
    public long songId;

    public long dateAdded; // Timestamp of when it was favorited

    public FavoriteEntity(long songId, long dateAdded) {
        this.songId = songId;
        this.dateAdded = dateAdded;
    }
}