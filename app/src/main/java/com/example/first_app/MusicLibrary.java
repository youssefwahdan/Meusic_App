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
 * A singleton helper that discovers, caches, and serves audio files (Song objects)
 * from the device MediaStore. Designed to centralize permission handling, background
 * loading, and simple search functionality for music apps.
 *
 * Responsibilities
 * - Provide a single shared instance via getInstance().
 * - Query MediaStore for audio files and build a cached list of Song objects.
 * - Handle runtime permission checks for audio/media access and notify callers when
 *   permission is required.
 * - Load songs on a background thread and deliver results on the main thread via
 *   OnSongsLoadedListener callbacks.
 * - Offer a simple in-memory search across title, artist, and album fields.
 *
 * Threading and lifecycle notes
 * - loadSongs(...) performs the heavy MediaStore query on a background thread.
 *   Results and error callbacks are posted to the main thread using a Handler.
 * - The class caches results in memory (songs list). Subsequent calls return cached
 *   data immediately if already loaded.
 * - If a permission is required, the listener is notified via onPermissionRequired(...)
 *   and the caller should request the permission from the user. After permission is
 *   granted, call onPermissionGranted(...) to continue loading.
 *
 * Permissions and Android versions
 * - Uses READ_MEDIA_AUDIO on Android 33+ and READ_EXTERNAL_STORAGE on older versions.
 * - Caller must request and obtain the appropriate permission before calling loadSongs,
 *   or handle the onPermissionRequired callback to request it.
 *
 * Public API summary
 * - getInstance(): MusicLibrary
 * - getSongs(): List<Song>
 * - loadSongs(Context context, OnSongsLoadedListener listener): void
 *     - Checks permission, starts background fetch if needed, and returns results via listener.
 * - onPermissionGranted(Context context, OnSongsLoadedListener listener): void
 *     - Call after the user grants permission to resume loading.
 * - searchSongs(String query): List<Song>
 *     - Case-insensitive search across title, artist, and album. Returns an empty list
 *       for null/empty queries by default.
 *
 * Listener callbacks (OnSongsLoadedListener)
 * - onSongsLoaded(List<Song> songs): invoked on the main thread when loading completes.
 * - onPermissionRequired(String permission): invoked when the required permission is not granted.
 * - onError(String message): invoked on the main thread if an exception occurs during loading.
 *
 * Implementation details
 * - fetchAllSongsFromDevice(Context): queries MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
 *   with projection fields: _ID, TITLE, ARTIST, ALBUM, DURATION, DATA.
 * - Only rows where IS_MUSIC != 0 are included.
 * - Results are sorted by TITLE ascending.
 * - The method maps cursor rows into Song instances and returns the list.
 * - The cursor is closed using try-with-resources to avoid leaks.
 *
 * Caching and state flags
 * - isLoading prevents concurrent fetches.
 * - isLoaded indicates whether the cache is populated; when true and songs is non-empty,
 *   loadSongs returns cached data immediately.
 * - onPermissionGranted resets isLoaded to false to force a fresh fetch after permission.
 *
 * Error handling
 * - Exceptions during fetching are caught and posted to the listener via onError.
 * - Query exceptions are printed to log (e.printStackTrace()) and do not crash the app.
 *
 * Usage example
 *   MusicLibrary.getInstance().loadSongs(context, new MusicLibrary.OnSongsLoadedListener() {
 *       @Override public void onSongsLoaded(List<Song> songs) // {update UI}
        *       @Override public void onPermissionRequired(String permission) {
 *           // Request the permission from the user, then call onPermissionGranted(...)
 *       }
 *       @Override public void onError(String message) //{ show error }
 *   });
         *
         * After permission is granted (Activity/Fragment):
        *   MusicLibrary.getInstance().onPermissionGranted(context, listener);
 *
         * Extensibility suggestions
 * - Add pagination or incremental loading for very large libraries.
 * - Provide more advanced search (fuzzy matching, tokenized search).
        * - Expose methods to refresh the cache, remove items, or observe changes via ContentObserver.
 * - Return immutable copies of the cached list to prevent external modification.
 * - Add sorting options (by artist, album, duration) and filtering (genre, year).
        *
        * Expected Song contract (required fields)
 * - long getId()  -> MediaStore _ID
 * - String getTitle()
 * - String getArtist()
 * - String getAlbum()
 * - long getDuration()
 * - String getData() (file path) — optional depending on how you play tracks (MediaStore URIs are preferred)
 *
         * Notes
 * - For Android 10+ consider using MediaStore URIs (ContentUris.withAppendedId(...)) and scoped storage
 *   best practices rather than relying on file paths.
        * - Keep UI updates lightweight when receiving onSongsLoaded because the list may be large.
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
                MediaStore.Audio.Media.DATA
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                while (cursor.moveToNext()) {
                    songList.add(new Song(
                            cursor.getLong(idCol),
                            cursor.getString(titleCol),
                            cursor.getString(artistCol),
                            cursor.getString(albumCol),
                            cursor.getLong(durationCol),
                            cursor.getString(dataCol)
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