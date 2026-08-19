package com.example.first_app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {

    // Add to favorites
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addFavorite(FavoriteEntity favorite);

    // Remove from favorites
    @Query("DELETE FROM favorites WHERE songId = :songId")
    void removeFavorite(long songId);

    // Check if a specific song is a favorite (Returns 1 or 0)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    LiveData<Integer> isFavorite(long songId);

    // Get all favorites, sorted by the date they were added (newest first)
    @Query("SELECT * FROM favorites ORDER BY dateAdded DESC")
    LiveData<List<FavoriteEntity>> getAllFavorites();

    @Query("SELECT COUNT(*) FROM favorites")
    LiveData<Integer> favouritesCount();
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songId = :songId)")
    int isFavoriteSync(long songId); // Add this line to FavoriteDao
}