package com.example.first_app;

import android.content.ContentUris;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
/**
 * PlayerManager
 *
 * Simple singleton that controls audio playback using MediaPlayer.
 * - Holds an application Context and a queue of Song objects.
 * - Provides play, pause, next, previous, and seek controls.
 * - Notifies registered PlayerStateListener instances about song changes,
 *   playback state, and progress updates.
 *
 * Public methods (what each does)
 * - getInstance()
 *     Returns the singleton PlayerManager instance.
 *
 * - init(Context context)
 *     Store the application context for safe MediaStore/URI access.
 *
 * - setQueue(List<Song> currentQueue)
 *     Replace the internal playback queue with the provided list.
 *
 * - addListener(PlayerStateListener listener)
 *     Register a UI or component to receive playback callbacks.
 *
 * - removeListener(PlayerStateListener listener)
 *     Unregister a previously added listener.
 *
 * - playSong(Song song, List<Song> newQueue)
 *     Set the queue, set current index to the given song, prepare and start playback.
 *
 * - playPause()
 *     Toggle playback: if no MediaPlayer exists it prepares the first song in the queue;
 *     otherwise it pauses or resumes playback and updates progress updates.
 *
 * - next()
 *     Advance to the next song in the queue and start it (no-op at end of queue).
 *
 * - prev()
 *     Go to the previous song if available; if at first song, seek to start.
 *
 * - seekTo(int positionMs)
 *     Seek the current track to the specified millisecond position and notify listeners.
 *
 * - isPlaying()
 *     Return whether playback is currently active.
 *
 * - getCurrentSong()
 *     Return the currently selected Song or null if none.
 *
 * Internal helpers (brief)
 * - prepareAndPlay(Song song)
 *     Release any existing player, create and configure MediaPlayer, set data source
 *     using a MediaStore content Uri, prepare asynchronously and start on prepared.
 *
 * - releasePlayer()
 *     Stop progress updates, stop and release MediaPlayer, clear playing state.
 *
 * - startProgressUpdate() / stopProgressUpdate()
 *     Manage a Runnable on the main Handler that calls listeners with current progress.
 *
 * Listener callbacks (PlayerStateListener)
 * - onSongChanged(Song song)
 * - onPlaybackStateChanged(boolean isPlaying)
 * - onProgressChanged(int currentMs, int totalMs)
 *
 * Notes
 * - Call init(...) early (e.g., Application.onCreate).
 * - For background playback integrate with a Service / MediaSession.
 */



public class PlayerManager {
    private static PlayerManager instance;
    private Context appContext;
    private MediaPlayer mediaPlayer;
    private List<Song> queue;
    private int currentIndex = -1;
    private boolean isPlaying = false;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;

    // Listeners for UI updates
    private List<PlayerStateListener> listeners = new ArrayList<>();

    public interface PlayerStateListener {
        void onSongChanged(Song song);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressChanged(int currentMs, int totalMs);
    }

    private PlayerManager() {}

    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    // Initialize with Application Context to safely access MediaStore URIs
    public void init(Context context) {
        if (appContext == null) {
            this.appContext = context.getApplicationContext();
        }
    }

    public void setQueue(List<Song> currentQueue) {
        this.queue = currentQueue;
    }



    public void addListener(PlayerStateListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(PlayerStateListener listener) {
        listeners.remove(listener);
    }

    public void playSong(Song song, List<Song> newQueue) {
        this.queue = newQueue;
        this.currentIndex = newQueue.indexOf(song);
        prepareAndPlay(song);
    }

    public void playPause() {
        if (mediaPlayer == null) {
            if (this.queue == null) {
                Toast.makeText(appContext, "There is no songs", Toast.LENGTH_LONG ).show();
                return;
            } else {
                prepareAndPlay(this.queue.get(0));
                isPlaying = true;
                startProgressUpdate();
            }
        };
        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            stopProgressUpdate();
        } else {
            mediaPlayer.start();
            isPlaying = true;
            startProgressUpdate();
        }
        notifyPlaybackState();
    }

    public void next() {
        if (queue == null || currentIndex >= queue.size() - 1) return;
        currentIndex++;
        prepareAndPlay(queue.get(currentIndex));
    }

    public void prev() {
        if (queue == null) return;
        if (currentIndex > 0) {
            currentIndex--;
            prepareAndPlay(queue.get(currentIndex));
        } else {
            seekTo(0); // Restart current song if it's the first one
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(positionMs);
            notifyProgress();
        }
    }

    public boolean isPlaying() { return isPlaying; }
    public Song getCurrentSong() { return queue != null && currentIndex != -1 ? queue.get(currentIndex) : null; }

    private void prepareAndPlay(Song song) {
        releasePlayer();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );

            // Use Context and Uri to safely handle Android 10+ Scoped Storage
            Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
            mediaPlayer.setDataSource(appContext, trackUri);

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                notifySongChanged();
                notifyPlaybackState();
                startProgressUpdate();
            });

            mediaPlayer.setOnCompletionListener(mp -> next()); // Auto-play next
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releasePlayer() {
        stopProgressUpdate();
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
    }

    private void startProgressUpdate() {
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) notifyProgress();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateProgressRunnable);
    }

    private void stopProgressUpdate() {
        if (updateProgressRunnable != null) handler.removeCallbacks(updateProgressRunnable);
    }

    // --- Notification Helpers ---
    private void notifySongChanged() {
        Song song = getCurrentSong();
        if (song != null) for (PlayerStateListener l : listeners) l.onSongChanged(song);
    }
    private void notifyPlaybackState() {
        for (PlayerStateListener l : listeners) l.onPlaybackStateChanged(isPlaying);
    }
    private void notifyProgress() {
        if (mediaPlayer != null) {
            int current = mediaPlayer.getCurrentPosition();
            int total = mediaPlayer.getDuration();
            for (PlayerStateListener l : listeners) l.onProgressChanged(current, total);
        }
    }
}