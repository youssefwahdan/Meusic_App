package com.example.first_app;

import android.content.Context;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistManager {
    private static PlaylistManager instance;
    private final PlaylistDao playlistDao;
    private final PlaylistSongDao playlistSongDao;
    private final ExecutorService executor;

    private PlaylistManager(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        playlistDao = db.playlistDao();
        playlistSongDao = db.playlistSongDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized PlaylistManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlaylistManager(context);
        }
        return instance;
    }

    // --- Playlist Operations ---
    public void createPlaylist(String name) {
        executor.execute(() -> {
            PlaylistEntity playlist = new PlaylistEntity(name, System.currentTimeMillis());
            playlistDao.insertPlaylist(playlist);
        });
    }

    public void deletePlaylist(int playlistId) {
        executor.execute(() -> {
            playlistDao.deletePlaylist(playlistId);
            // Optional: Also delete all songs associated with this playlist
            // playlistSongDao.deleteAllSongsInPlaylist(playlistId);
        });
    }

    public LiveData<List<PlaylistEntity>> getAllPlaylists() {
        return playlistDao.getAllPlaylists();
    }

    public interface AddSongCallback {
        void onResult(boolean success, String message);
    }
    public void addSongToPlaylistSafe(int playlistId, long songId, AddSongCallback callback) {
        executor.execute(() -> {
            try {
                // Check if it exists
                int exists = playlistSongDao.isSongInPlaylistSync(playlistId, songId);
                boolean alreadyExists = (exists == 1);

                if (alreadyExists) {
                    // Post failure to main thread
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        if (callback != null) {
                            callback.onResult(false, "This song already exists in this playlist");
                        }
                    });
                } else {
                    // Add the song
                    int orderIndex = (int) (System.currentTimeMillis() / 1000);
                    playlistSongDao.addSongToPlaylist(new PlaylistSongEntity(playlistId, songId, orderIndex));

                    // Post success to main thread
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        if (callback != null) {
                            callback.onResult(true, "Added to playlist");
                        }
                    });
                }
            } catch (Exception e) {
                // Log the error properly
                android.util.Log.e("PlaylistManager", "Error adding song to playlist", e);

                // Post error to main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onResult(false, "Failed to add song: " + e.getMessage());
                    }
                });
            }
        });
    }

    public void reorderPlaylist(int playlistId, List<Long> newSongOrder) {
        executor.execute(() -> {
            // 1. Clear the old order
            playlistSongDao.deleteAllSongsInPlaylist(playlistId);

            // 2. Create new entities with the correct orderIndex
            List<PlaylistSongEntity> newEntities = new ArrayList<>();
            int orderIndex = 0;
            for (Long songId : newSongOrder) {
                newEntities.add(new PlaylistSongEntity(playlistId, songId, orderIndex++));
            }

            // 3. Save the new order
            playlistSongDao.insertAllPlaylistSongs(newEntities);
        });
    }

    public LiveData<List<Long>> getSongIdsInPlaylist(int playlistId) {
        return playlistSongDao.getSongIdsInPlaylist(playlistId);
    }
    public Long getFirstSongIdInPlaylist(int playlistId) {
        return playlistSongDao.getFirstSongInPlaylist(playlistId);
    }

    public LiveData<Integer> getPlaylistsCount() {
        return playlistDao.playlistsCount();
    }
}