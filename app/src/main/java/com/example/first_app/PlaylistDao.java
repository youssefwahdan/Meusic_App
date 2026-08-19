package com.example.first_app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PlaylistDao {
    @Insert
    void insertPlaylist(PlaylistEntity playlist);

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    void deletePlaylist(int playlistId);

    // Get all playlists, sorted by creation date (newest first)
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    LiveData<List<PlaylistEntity>> getAllPlaylists();

    @Query("SELECT COUNT(*) FROM playlists")
    LiveData<Integer> playlistsCount();
}