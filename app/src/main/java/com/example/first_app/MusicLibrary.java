package com.example.first_app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * MusicLibrary
 *
 * Singleton that discovers, caches, and serves Song objects from MediaStore.
 * - Handles permission checks, background loading, caching, and simple search.
 *
 * Public methods (what each does)
 * - getInstance()
 *     Return the singleton MusicLibrary instance.
 *
 * - getSongs()
 *     Return the cached list of songs (modifiable list reference).
 *
 * - loadSongs(Context context, OnSongsLoadedListener listener)
 *     Main entry: if cached data exists it returns it immediately via listener.
 *     Otherwise it checks the required permission (READ_MEDIA_AUDIO on Android 33+,
 *     READ_EXTERNAL_STORAGE on older versions). If permission is missing it calls
 *     listener.onPermissionRequired(permission). If permission is granted it starts
 *     a background thread to query MediaStore and posts results to listener.onSongsLoaded.
 *
 * - onPermissionGranted(Context context, OnSongsLoadedListener listener)
 *     Call after the user grants permission; resets loaded state and calls loadSongs(...)
 *     to continue loading.
 *
 * - searchSongs(String query)
 *     Simple case-insensitive search across title, artist, and album. Returns an empty
 *     list for null/empty queries by default.
 *
 * Listener interface (callbacks)
 * - OnSongsLoadedListener.onSongsLoaded(List<Song> songs)
 * - OnSongsLoadedListener.onPermissionRequired(String permission)
 * - OnSongsLoadedListener.onError(String message)
 *
 * Internal helpers
 * - fetchAllSongsFromDevice(Context context)
 *     Query MediaStore.Audio.Media.EXTERNAL_CONTENT_URI for rows where IS_MUSIC != 0,
 *     map cursor rows to Song objects, and return the list. Uses try-with-resources to
 *     close the cursor.
 *
 * Notes
 * - Results are loaded on a background thread and delivered on the main thread.
 * - Caller must request permissions when onPermissionRequired is invoked.
 * - Consider returning immutable copies of the cached list if external modification is a concern.
 */



public class MusicLibrary {
    private static MusicLibrary instance;
    private final List<Song> songs = new ArrayList<>();
    private boolean isLoading = false;
    private boolean isLoaded = false;

    public interface OnSongsLoadedListener {
        void onSongsLoaded(List<Song> songs);
        void onPermissionRequired(String permission);
        void onError(String message);
    }

    private MusicLibrary() {}

    public static synchronized MusicLibrary getInstance() {
        if (instance == null) {
            instance = new MusicLibrary();
        }
        return instance;
    }

    public List<Song> getSongs() {
        return songs;
    }

    /**
     * Main entry point for any Activity that needs songs.
     * Handles permissions, fetching, and caching automatically.
     */
    public void loadSongs(Context context, OnSongsLoadedListener listener) {
        // If already loaded, return cached data immediately
        if (isLoaded && !songs.isEmpty()) {
            if (listener != null) listener.onSongsLoaded(songs);
            return;
        }

        // If currently loading, just wait (don't start another fetch)
        if (isLoading) return;

        // Check permission
        String permission = Build.VERSION.SDK_INT >= 33
                ? "android.permission.READ_MEDIA_AUDIO"
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) listener.onPermissionRequired(permission);
            return;
        }

        // Start fetching on background thread
        isLoading = true;

        new Thread(() -> {
            try {
                List<Song> fetchedSongs = fetchAllSongsFromDevice(context);

                songs.clear();
                songs.addAll(fetchedSongs);
                isLoaded = true;
                isLoading = false;

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onSongsLoaded(songs);
                });
            } catch (Exception e) {
                isLoading = false;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Call this after permission is granted to continue loading
     */
    public void onPermissionGranted(Context context, OnSongsLoadedListener listener) {
        isLoaded = false; // Reset so it fetches again
        loadSongs(context, listener);
    }

    private List<Song> fetchAllSongsFromDevice(Context context) {
        List<Song> songList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    songList.add(new Song(
                            cursor.getLong(idCol),
                            cursor.getString(titleCol),
                            cursor.getString(artistCol),
                            cursor.getString(albumCol),
                            cursor.getLong(durationCol),
                            cursor.getString(dataCol),
                            cursor.getString(dateAddedCol)
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songList;
    }

    public List<Song> searchSongs(String query) {
        // If the search box is empty, return the whole list (or an empty list, depending on your preference)
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String lowerCaseQuery = query.toLowerCase().trim();
        List<Song> filteredList = new ArrayList<>();

        for (Song song : songs) {
            // Search across Title, Artist, and Album
            if (song.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                    song.getArtist().toLowerCase().contains(lowerCaseQuery) ||
                    song.getAlbum().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(song);
            }
        }
        return filteredList;
    }
}