package com.example.meusic.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.meusic.database.entities.PlaylistSongEntity;

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

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC LIMIT 1")
    Long getFirstSongInPlaylist(int playlistId);

    // Delete all songs for a specific playlist (used before re-saving the new order)
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void deleteAllSongsInPlaylist(int playlistId);

    // Re-insert songs with a new order (Room handles this efficiently)
    @Insert
    void insertAllPlaylistSongs(List<PlaylistSongEntity> playlistSongs);
}