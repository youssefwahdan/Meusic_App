package com.example.first_app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;

public class AppStateManager {
    private static final String PREFS_NAME = "app_state_prefs";
    private static final String KEY_PLAY_MODE = "playback_mode";
    private static final String KEY_INDEX = "current_index";
    private static final String KEY_QUEUE = "queue_ids";
    private static final String KEY_SORT_MODE = "sort_index";

    private final SharedPreferences prefs;

    public AppStateManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // --- SAVE STATE ---
    public void saveState(PlayerManager.PlaybackMode mode, int currentIndex, List<Song> queue, int sortIndex) {
        SharedPreferences.Editor editor = prefs.edit();

        // 1. Save Mode
        editor.putString(KEY_PLAY_MODE, mode.name());

        // 2. Save Index
        editor.putInt(KEY_INDEX, currentIndex);

        editor.putInt(KEY_SORT_MODE, sortIndex);

        // 3. Save Queue as a comma-separated String to preserve order
        StringBuilder sb = new StringBuilder();
        if (queue != null) {
            for (int i = 0; i < queue.size(); i++) {
                sb.append(queue.get(i).getId());
                if (i < queue.size() - 1) sb.append(",");
            }
        }
        editor.putString(KEY_QUEUE, sb.toString());

        editor.apply();
    }

    public void restoreSortState( MusicLibrary musicLibrary) {
        int sortIndex = prefs.getInt(KEY_SORT_MODE, -1);
        if (sortIndex != -1) {
            musicLibrary.setCurrentSortIndex(sortIndex);
        }
    }

    // --- LOAD STATE ---
    public void restoreState(PlayerManager manager, List<Song> allSongs) {
        String modeStr = prefs.getString(KEY_PLAY_MODE, null);
        int currentIndex = prefs.getInt(KEY_INDEX, -1);
        String queueStr = prefs.getString(KEY_QUEUE, "");

        if (modeStr == null || queueStr.isEmpty()) {
            return; // No state saved
        }

        // 1. Restore Mode
        try {
            PlayerManager.PlaybackMode mode = PlayerManager.PlaybackMode.valueOf(modeStr);
            manager.setPlaybackMode(mode); // You will need to add this setter to PlayerManager
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Restore Queue (Map IDs back to full Song objects)
        List<Song> restoredQueue = new ArrayList<>();
        if (!queueStr.isEmpty()) {
            String[] ids = queueStr.split(",");
            for (String idStr : ids) {
                long id = Long.parseLong(idStr);
                // Find the full song object from the global MusicLibrary
                for (Song song : allSongs) {
                    if (song.getId() == id) {
                        restoredQueue.add(song);
                        break;
                    }
                }
            }
        }

        // 3. Restore Index and Queue
        if (!restoredQueue.isEmpty() && currentIndex >= 0 && currentIndex < restoredQueue.size()) {
            manager.setQueue(restoredQueue);
            manager.setCurrentSong(restoredQueue.get(currentIndex), restoredQueue);
        }
    }
}