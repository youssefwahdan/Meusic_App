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

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private int currentSortIndex = 0;


    public interface OnSongsLoadedListener {
        void onSongsLoaded(List<Song> songs);
        void onPermissionRequired(String permission);
        void onError(String message);
    }
    public interface OnSongsSortedListener {
        void onSongsSorted();
    }

    public interface OnFavouritesLoadedListener {
        void onFavouritesCountLoaded(int count);
    }

    public interface OnPlaylistsLoadedListener {
        void onPlaylistsCountLoaded(int count);
    }

    private final List<OnSongsSortedListener> sortListeners = new ArrayList<>();

    public void addSortListener(OnSongsSortedListener listener) {
        if (!sortListeners.contains(listener)) sortListeners.add(listener);
    }

    public void removeSortListener(OnSongsSortedListener listener) {
        sortListeners.remove(listener);
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
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.TRACK
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
                int trackNumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK);

                while (cursor.moveToNext()) {
                    songList.add(new Song(
                            cursor.getLong(idCol),
                            cursor.getString(titleCol),
                            cursor.getString(artistCol),
                            cursor.getString(albumCol),
                            cursor.getLong(durationCol),
                            cursor.getString(dataCol),
                            cursor.getString(dateAddedCol),
                            cursor.getInt(trackNumCol)
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
            if (song.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(song);
            }
        }
        return filteredList;
    }


    // Add this method inside your MusicLibrary class
    public List<SearchItem> search(String query) {
        List<SearchItem> results = new ArrayList<>();
        if (songs == null || query == null || query.trim().isEmpty()) {
            return results;
        }

        String lowerQuery = query.toLowerCase().trim();
        Set<String> addedArtists = new HashSet<>();
        Set<String> addedAlbums = new HashSet<>();

        // 1. Find Matching Songs
        for (Song song : songs) {
            if (song.getTitle() != null && song.getTitle().toLowerCase().contains(lowerQuery)) {
                results.add(new SearchItem(SearchItem.TYPE_SONG, song.getTitle(), song.getArtist(), song, new ArrayList<>()));
            }
        }

        // 2. Find Matching Artists (Unique)
        for (Song song : songs) {
            String artist = song.getArtist();
            if (artist != null && artist.toLowerCase().contains(lowerQuery) && !addedArtists.contains(artist)) {
                long count = songs.stream().filter(s -> s.getArtist() != null && s.getArtist().equals(artist)).count();
                results.add(new SearchItem(SearchItem.TYPE_ARTIST, artist, count + " Songs", artist, new ArrayList<>()));
                addedArtists.add(artist);
            }
        }

        // 3. Find Matching Albums (Unique)
        for (Song song : songs) {
            String album = song.getAlbum();
            if (album != null && album.toLowerCase().contains(lowerQuery) && !addedAlbums.contains(album)) {
                ArrayList<Song> albumSongs = new ArrayList<>();

                for (Song s : songs) {
                    if (s.getAlbum() != null && s.getAlbum().equals(album)) {
                        albumSongs.add(s);
                    }
                }
                String artist = song.getArtist();
                results.add(new SearchItem(SearchItem.TYPE_ALBUM, album, artist != null ? artist : "Unknown Artist", album, albumSongs));
                addedAlbums.add(album);
            }
        }

        return results;
    }

    public void sortSongs(int option) {
        switch (option) {
            case 0: // Title (A - Z)
                songs.sort((s1, s2) -> s1.getTitle().compareToIgnoreCase(s2.getTitle()));
                break;
            case 1: // Title (Z - A)
                songs.sort((s1, s2) -> s2.getTitle().compareToIgnoreCase(s1.getTitle()));
                break;
            case 2: // Artist (A - Z)
                songs.sort((s1, s2) -> s1.getArtist().compareToIgnoreCase(s2.getArtist()));
                break;
            case 3: // Artist (Z - A)
                songs.sort((s1, s2) -> s2.getArtist().compareToIgnoreCase(s1.getArtist()));
                break;
            case 4: // Album (A - Z)
                songs.sort((s1, s2) -> s1.getAlbum().compareToIgnoreCase(s2.getAlbum()));
                break;
            case 5: // Album (Z - A)
                songs.sort((s1, s2) -> s2.getAlbum().compareToIgnoreCase(s1.getAlbum()));
                break;
            case 6: // Duration (Shortest first)
                songs.sort((s1, s2) -> Long.compare(s1.getDuration(), s2.getDuration()));
                break;
            case 7: // Duration (Longest first)
                songs.sort((s1, s2) -> Long.compare(s2.getDuration(), s1.getDuration()));
                break;
            case 8: // Date Added ASC
                songs.sort((s1, s2) -> s1.getDate().compareToIgnoreCase(s2.getDate()));
                break;
            case 9: // Date Added DESC
                songs.sort((s1, s2) -> s2.getDate().compareToIgnoreCase(s1.getDate()));
                break;
        }

        // Notify all fragments that the list has been sorted
        for (OnSongsSortedListener listener : sortListeners) {
            listener.onSongsSorted();
        }
    }
    public void showSortDialog(Context context) {
        String[] sortOptions = {
                "Title (A - Z)",
                "Title (Z - A)",
                "Artist (A - Z)",
                "Artist (Z - A)",
                "Album (A - Z)",
                "Album (Z - A)",
                "Duration (Shortest first)",
                "Duration (Longest first)",
                "Date Added ASC",
                "Date Added DESC"
        };

        // 1. Create the Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Sort Songs By")
                .setSingleChoiceItems(sortOptions, currentSortIndex, (dialog, which) -> {
                    currentSortIndex = which;
                    sortSongs(which);
                    dialog.dismiss();
                });
//                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // 2. Create the Dialog object
        AlertDialog dialog = builder.create();
        // 3. Apply the rounded background
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_bg);
        // 4. Show the dialog
        dialog.show();
        // 1. Get the screen width
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

        // 2. Calculate the dialog width (e.g., 0.90 = 90% of the screen)
        int dialogWidth = (int) (screenWidth * 0.90);

        // 3. Apply the width and set height to wrap content
        dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void getFavouritesCount(LifecycleOwner lifecycleOwner, Context context, OnFavouritesLoadedListener listener) {
        FavoriteManager.getInstance(context).getFavoritesCount().observe(lifecycleOwner, count -> {
            if (count != null) {
                if (listener != null) listener.onFavouritesCountLoaded(count);
            }
        });
    }

    public void getPlaylistsCount(LifecycleOwner lifecycleOwner, Context context, OnPlaylistsLoadedListener listener) {
        PlaylistManager.getInstance(context).getPlaylistsCount().observe(lifecycleOwner, count -> {
            if (count != null) {
                if (listener != null) listener.onPlaylistsCountLoaded(count);
            }
        });
    }

}