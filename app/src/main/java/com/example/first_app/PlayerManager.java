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
 * A singleton audio playback manager for simple music player apps.
 * Designed to manage a queue of Song objects, control playback via MediaPlayer,
 * and notify UI components about state changes through PlayerStateListener callbacks.
 *
 * Key responsibilities
 * - Maintain a single shared instance (thread-safe lazy initialization).
 * - Hold application Context (application-level) for safe MediaStore access.
 * - Create, prepare, start, pause, stop and release MediaPlayer instances.
 * - Maintain a playback queue and current index.
 * - Provide play / pause / next / previous / seek controls.
 * - Periodically publish playback progress to registered listeners on the main thread.
 *
 * Threading and lifecycle notes
 * - All UI notifications and progress updates are posted on the main Looper via a Handler.
 * - Call init(Context) early (for example in Application.onCreate or first Activity) to set an application Context.
 * - Always call releasePlayer() (or let the manager handle it) when the app or playback component is destroyed to free MediaPlayer resources.
 * - This class is not a full foreground service; if you need persistent playback while the app is backgrounded, integrate with a Service and MediaSession.
 *
 * Permissions and storage
 * - Uses MediaStore URIs built with ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).
 * - Ensure READ_EXTERNAL_STORAGE (or scoped storage access) is granted where required on older Android versions.
 *
 * Usage example
 *   // initialize once (e.g., in Application)
 *   PlayerManager.getInstance().init(applicationContext);
 *
 *   // set queue and play a song
 *   List<Song> queue = ...;
 *   PlayerManager.getInstance().setQueue(queue);
 *   PlayerManager.getInstance().playSong(queue.get(0), queue);
 *
 *   // listen for updates (UI component)
 *   PlayerManager.PlayerStateListener listener = new PlayerManager.PlayerStateListener() {
 *       @Override public void onSongChanged(Song song) //{ update UI}
        *       @Override public void onPlaybackStateChanged(boolean isPlaying) //{ update play/pause button }
 *       @Override public void onProgressChanged(int currentMs, int totalMs) //{ update seekbar  }
 *   };
         *   PlayerManager.getInstance().addListener(listener);
 *
         * Public API summary
 * - getInstance(): PlayerManager
 * - init(Context context): void
 * - setQueue(List<Song> currentQueue): void
 * - playSong(Song song, List<Song> newQueue): void
 * - playPause(): void
 * - next(): void
 * - prev(): void
 * - seekTo(int positionMs): void
 * - isPlaying(): boolean
 * - getCurrentSong(): Song
 * - addListener(PlayerStateListener listener): void
 * - removeListener(PlayerStateListener listener): void
 *
         * Listener callbacks
 * - onSongChanged(Song song): called when the current song changes (after prepare/start).
        * - onPlaybackStateChanged(boolean isPlaying): called when playback starts or pauses/stops.
 * - onProgressChanged(int currentMs, int totalMs): called periodically (approx. every second) while playing.
 *
         * Implementation details and important behaviors
 * - prepareAndPlay(Song): releases any existing MediaPlayer, creates a new MediaPlayer,
        *   sets AudioAttributes, sets data source using a MediaStore content Uri, prepares asynchronously,
 *   and starts playback in onPrepared.
 * - playPause(): if mediaPlayer is null and a queue exists, it prepares and plays the first item.
        *   It toggles between pause and start and updates isPlaying and progress updates accordingly.
 * - next(): advances currentIndex and plays the next song if available.
 * - prev(): goes to previous song if available; otherwise seeks to start of current song.
 * - startProgressUpdate()/stopProgressUpdate(): manage a Runnable posted to the main Handler to call
 *   notifyProgress() every second while playing.
 * - notify* methods iterate registered listeners and call the appropriate callback on the main thread.
 *
         * Error handling
 * - Exceptions during prepare/setDataSource are caught and printed to log (e.printStackTrace()).
        * - The class does not surface detailed error callbacks; consider adding an error listener if needed.
 *
         * Extensibility suggestions
 * - Add an error callback to PlayerStateListener for reporting MediaPlayer errors.
 * - Add shuffle/repeat modes and queue manipulation methods (add/remove/reorder).
        * - Integrate with a foreground Service + MediaSession for robust background playback and lockscreen controls.
        * - Persist queue and playback position if you need resume-after-restart behavior.
        *
        * Example Song contract (expected)
 * - Song should expose at least: long getId() (MediaStore id), String getTitle(), String getArtist(), int getDurationMs() (optional).
        *
        * @author
 *   Generated documentation
 * @since
 *   1.0
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