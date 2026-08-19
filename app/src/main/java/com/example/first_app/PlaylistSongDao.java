package com.example.first_app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PlaylistSongDao {
    @Insert
    void addSongToPlaylist(PlaylistSongEntity playlistSong);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    void removeSongFromPlaylist(int playlistId, long songId);

    // Get all song IDs in a specific playlist, ordered correctly
    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    LiveData<List<Long>> getSongIdsInPlaylist(int playlistId);
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId)")
    int isSongInPlaylistSync(int playlistId, long songId);

    @Query("SELECT songId From (SELECT * FROM playlist_songs ORDER BY orderIndex ASC) WHERE playlistId = :playlistId LIMIT 1")
    LiveData<Long> getFirstSongOnPlaylist(int playlistId);
}