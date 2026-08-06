package com.example.first_app;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * PlayerManager
 *
 * Singleton that controls audio playback using MediaPlayer.
 * - Holds an application Context and a queue of Song objects.
 * - Provides play, pause, next, previous, and seek controls.
 * - Manages Audio Focus (pauses on calls/other apps, stops other apps when playing).
 * - Manages MediaSession (enables system notification & Huawei Quick Settings controls).
 * - Manages WakeLock (prevents the device from sleeping during playback).
 * - Notifies registered PlayerStateListener instances about song changes, playback state, and progress.
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

    // --- NEW: Audio & System Components ---
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private MediaSessionCompat mediaSession;
    private PowerManager.WakeLock wakeLock;

    private final List<PlayerStateListener> listeners = new ArrayList<>();

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

    public void init(Context context) {
        if (appContext == null) {
            this.appContext = context.getApplicationContext();

            // --- NEW: Initialize System Components ---
            this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
            setupMediaSession();
            setupWakeLock();
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

    // --- NEW: Required for MediaStyle Notification ---
    public MediaSessionCompat.Token getSessionToken() {
        return mediaSession != null ? mediaSession.getSessionToken() : null;
    }

    public void playSong(Song song, List<Song> newQueue) {
        this.queue = newQueue;
        this.currentIndex = newQueue.indexOf(song);
        prepareAndPlay(song);
    }

    public void playPause() {
        if (mediaPlayer == null) {
            if (this.queue == null || this.queue.isEmpty()) {
                Toast.makeText(appContext, "There are no songs", Toast.LENGTH_LONG).show();
                return;
            } else {
//                int startIndex = (currentIndex >= 0 && currentIndex < queue.size()) ? currentIndex : 0;
currentIndex = 0;
                prepareAndPlay(this.queue.get(currentIndex));
                return; // prepareAndPlay handles isPlaying, progress, and notifications
            }
        }

        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            stopProgressUpdate();
            releaseWakeLock();
        } else {
            if (requestAudioFocus()) {
                mediaPlayer.start();
                isPlaying = true;
                acquireWakeLock();
                startProgressUpdate();
            }
        }
        updateMediaSessionState();
        notifyPlaybackState();
        updateServiceNotification();
    }

    public void next() {
        if (queue == null || currentIndex >= queue.size() - 1) {
            releasePlayer();
            return;
        }
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
            updateMediaSessionState();
            notifyProgress();
        }
    }

    public boolean isPlaying() { return isPlaying; }
    public Song getCurrentSong() { return queue != null && currentIndex != -1 ? queue.get(currentIndex) : null; }

    private void prepareAndPlay(Song song) {
        releasePlayer();

        // --- NEW: Request Audio Focus before playing ---
        if (!requestAudioFocus()) {
            Toast.makeText(appContext, "Audio focus not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            // --- NEW: Fallback WakeLock for older devices ---
            mediaPlayer.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK);

            Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
            mediaPlayer.setDataSource(appContext, trackUri);

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                acquireWakeLock();

                // --- NEW: Update MediaSession for Notifications/Quick Settings ---
                updateMediaSessionMetadata(song);
                updateMediaSessionState();

                notifySongChanged();
                notifyPlaybackState();
                startProgressUpdate();
                updateServiceNotification();
            });

            mediaPlayer.setOnCompletionListener(mp -> next()); // Auto-play next
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            releasePlayer();
        }
    }

    public void releasePlayer() {
        stopProgressUpdate();
        if (mediaPlayer != null) {
            try { if (mediaPlayer.isPlaying()) mediaPlayer.stop(); } catch (Exception ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;

        // --- NEW: Cleanup System Components ---
        abandonAudioFocus();
        releaseWakeLock();
        stopService();
        notifyPlaybackState();
    }

    // ==========================================
    // 1. AUDIO FOCUS MANAGEMENT
    // ==========================================
    private boolean requestAudioFocus() {
        if (audioManager == null) return true;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build();
            return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            return audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer != null) mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Another app wants to play permanently (e.g., Spotify). WE MUST STOP.
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    notifyPlaybackState();
                    updateMediaSessionState();
                    updateServiceNotification();
                }
                abandonAudioFocus();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Temporary loss (e.g., Phone call). PAUSE.
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    notifyPlaybackState();
                    updateMediaSessionState();
                    updateServiceNotification();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Temporary loss where we can just lower volume (e.g., Navigation).
                if (mediaPlayer != null) mediaPlayer.setVolume(0.2f, 0.2f);
                break;
        }
    };

    // ==========================================
    // 2. MEDIA SESSION (For Notification & Huawei OS)
    // ==========================================
    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(appContext, "MusicPlayerSession");
        mediaSession.setActive(true);
    }

    private void updateMediaSessionMetadata(Song song) {
        if (mediaSession == null || song == null) return;
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getAlbum())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration())
                .build();
        mediaSession.setMetadata(metadata);
    }

    private void updateMediaSessionState() {
        if (mediaSession == null) return;
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long position = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setState(state, position, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_STOP)
                .build();
        mediaSession.setPlaybackState(playbackState);
    }

    // ==========================================
    // 3. WAKE LOCK & SERVICE MANAGEMENT
    // ==========================================
    private void setupWakeLock() {
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MusicWakeLock");
        }
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire(10 * 60 * 1000L /*10 mins*/);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    private void updateServiceNotification() {
        Intent intent = new Intent(appContext, PlaybackService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    private void stopService() {
        appContext.stopService(new Intent(appContext, PlaybackService.class));
    }

    // ==========================================
    // 4. PROGRESS & LISTENERS
    // ==========================================
    private void startProgressUpdate() {
        updateProgressRunnable = () -> {
            if (mediaPlayer != null && isPlaying) notifyProgress();
            handler.postDelayed(updateProgressRunnable, 1000);
        };
        handler.post(updateProgressRunnable);
    }

    private void stopProgressUpdate() {
        if (updateProgressRunnable != null) handler.removeCallbacks(updateProgressRunnable);
    }

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