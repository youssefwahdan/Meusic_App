package com.example.meusic;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
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

public class PlayerManager {
    private static PlayerManager instance;
    private Context appContext;
    private MediaPlayer mediaPlayer;
    private List<Song> queue;
    private int currentIndex = -1;
    private boolean isPlaying = false;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private MediaSessionCompat mediaSession;
    private PowerManager.WakeLock wakeLock;

    // Cache the current album art for the notification
    private Bitmap currentAlbumArt;

    public enum PlaybackMode {
        SHUFFLE,          // Shuffle
        REPEAT_NONE,      // Straight and stop
        REPEAT_ALL,       // Straight and loop
        REPEAT_ONE        // Loop on current song
    }

    private PlaybackMode playbackMode = PlaybackMode.REPEAT_ALL; // Default to straight and loop

    // Listeners to notify the UI when the mode changes
    public interface OnPlaybackModeChangedListener {
        void onPlaybackModeChanged(PlaybackMode mode);
    }
    private final List<OnPlaybackModeChangedListener> modeListeners = new ArrayList<>();

    private final List<PlayerStateListener> listeners = new ArrayList<>();

    public interface PlayerStateListener {
        void onSongChanged(Song song);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressChanged(int currentMs, int totalMs);
    }

    private PlayerManager() {}

    public static synchronized PlayerManager getInstance() {
        if (instance == null) instance = new PlayerManager();
        return instance;
    }

    public void init(Context context) {
        if (appContext == null) {
            this.appContext = context.getApplicationContext();
            this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
            setupMediaSession();
            setupWakeLock();
        }
    }

    public void setQueue(List<Song> currentQueue) {
        this.queue = currentQueue;
    //
    }
    public void addListener(PlayerStateListener listener) { if (!listeners.contains(listener)) listeners.add(listener); }
    public void removeListener(PlayerStateListener listener) { listeners.remove(listener); }
    public MediaSessionCompat.Token getSessionToken() { return mediaSession != null ? mediaSession.getSessionToken() : null; }

    // NEW: Getter for the notification to use
    public Bitmap getCurrentAlbumArt() { return currentAlbumArt; }

    public void playSong(Song song, List<Song> newQueue) {
        this.queue = newQueue;
        this.currentIndex = newQueue.indexOf(song);
        prepareAndPlay(song, true);
    }

    public void playPause() {
        if (mediaPlayer == null) {
            if (this.queue == null || this.queue.isEmpty()) {
                Toast.makeText(appContext, "There are no songs", Toast.LENGTH_LONG).show();
                return;
            } else {
                currentIndex = 0;
                prepareAndPlay(this.queue.get(currentIndex), true);
                return;
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
        refreshNotificationUI();
    }

    public void stopPlayback() {
        releasePlayer();
        stopService();
    }

    public void next() {
        if (queue == null || queue.isEmpty()) {
            stopPlayback(); // Queue ended! NOW we stop the service and notification.
            return;
        }
        if (playbackMode == PlaybackMode.REPEAT_ONE) {
            // Loop current song: just seek to 0 and play
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
                isPlaying = true;
                notifyPlaybackState();
            }
            return;
        }

        if (playbackMode == PlaybackMode.SHUFFLE) {
            // Pick a random song that isn't the current one
            if (queue.size() > 1) {
                int nextIndex;
                do {
                    nextIndex = new java.util.Random().nextInt(queue.size());
                } while (nextIndex == currentIndex);
                currentIndex = nextIndex;
            }
        } else {
            // REPEAT_ALL or REPEAT_NONE (Straight)
            if (currentIndex >= queue.size() - 1) {
                if (playbackMode == PlaybackMode.REPEAT_NONE) {
                    stopPlayback(); // Straight and stop
                    return;
                } else {
                    currentIndex = 0; // Straight and loop back to start
                }
            } else {
                currentIndex++; // Normal next
            }
        }
        prepareAndPlay(queue.get(currentIndex), true);
    }

    public void prev() {
        if (queue == null || queue.isEmpty()){
            stopPlayback();
            return;
        }
            if (playbackMode == PlaybackMode.REPEAT_ONE) {
                // Loop current song: just seek to 0 and play
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();
                    isPlaying = true;
                    notifyPlaybackState();
                }
                return;
            }

            if (playbackMode == PlaybackMode.SHUFFLE) {
                // Pick a random song that isn't the current one
                if (queue.size() > 1) {
                    int prevIndex;
                    do {
                        prevIndex = new java.util.Random().nextInt(queue.size());
                    } while (prevIndex == currentIndex);
                    currentIndex = prevIndex;
                }
            } else {
                // REPEAT_ALL or REPEAT_NONE (Straight)
                if (currentIndex <= 0) {
                    if (playbackMode == PlaybackMode.REPEAT_NONE) {
                        stopPlayback(); // Straight and stop
                        return;
                    } else {
                        currentIndex = queue.size() - 1; // Straight and loop to end
                    }
                } else {
                    currentIndex--; // Normal prev
                }
            }
            prepareAndPlay(queue.get(currentIndex), true);
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(positionMs);
            updateMediaSessionState();
            notifyProgress();
        }
    }

    public boolean isPlaying() { return isPlaying; }
    public Song getCurrentSong() {
        return queue != null && currentIndex != -1 ? queue.get(currentIndex) : null;
    }

    private void prepareAndPlay(Song song, Boolean autoPlay) {
        releasePlayer();
        if (!requestAudioFocus()) {
            Toast.makeText(appContext, "Audio focus not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mediaPlayer.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK);

            Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
            mediaPlayer.setDataSource(appContext, trackUri);

            mediaPlayer.setOnPreparedListener(mp -> {
                if (autoPlay) {
                    mp.start();
                    isPlaying = true;
                } else {
                    isPlaying = false;
                }
                acquireWakeLock();

                updateMediaSessionMetadata(song);
                updateMediaSessionState();

                notifySongChanged();
                notifyPlaybackState();
                startProgressUpdate();
                updateServiceNotification();
                refreshNotificationUI();
            });

            mediaPlayer.setOnCompletionListener(mp -> next());
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
        abandonAudioFocus();
        releaseWakeLock();
//        stopService();
        notifyPlaybackState();
    }

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
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    isPlaying = false;
                    notifyPlaybackState();
                    updateMediaSessionState();
                    updateServiceNotification();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null) mediaPlayer.setVolume(0.2f, 0.2f);
                break;
        }
    };

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(appContext, "MusicPlayerSession");
        mediaSession.setActive(true);
    }

    private void updateMediaSessionMetadata(Song song) {
        if (mediaSession == null || song == null) return;

        // Fetch and SCALE DOWN the album art (Critical for notifications)
        currentAlbumArt = getAlbumArtBitmap(song);

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getAlbum())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration());

        if (currentAlbumArt != null) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentAlbumArt);
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, currentAlbumArt);
        }
        mediaSession.setMetadata(builder.build());
    }

    private Bitmap getAlbumArtBitmap(Song song) {
        try {
            Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(appContext, trackUri);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                // SCALE DOWN to 256x256. Notifications will silently reject huge bitmaps!
                return Bitmap.createScaledBitmap(bitmap, 256, 256, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    private void setupWakeLock() {
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        if (pm != null) wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MusicWakeLock");
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) wakeLock.acquire(10 * 60 * 1000L);
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
    public void refreshNotificationUI() {
        // This tells the service to rebuild the RemoteViews with the latest data
        Intent intent = new Intent(appContext, PlaybackService.class);
        intent.setAction("UPDATE_UI");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    private void stopService() {
        appContext.stopService(new Intent(appContext, PlaybackService.class));
    }

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

    public void addModeListener(OnPlaybackModeChangedListener listener) {
        if (!modeListeners.contains(listener)) modeListeners.add(listener);
    }

    public void removeModeListener(OnPlaybackModeChangedListener listener) {
        modeListeners.remove(listener);
    }

    private void notifyPlaybackModeChanged() {
        for (OnPlaybackModeChangedListener listener : modeListeners) {
            listener.onPlaybackModeChanged(playbackMode);
        }
    }

    public void cyclePlaybackMode() {
        switch (playbackMode) {
            case REPEAT_ALL:
                playbackMode = PlaybackMode.REPEAT_ONE;
                break;
            case REPEAT_ONE:
                playbackMode = PlaybackMode.SHUFFLE;
                break;
            case SHUFFLE:
                playbackMode = PlaybackMode.REPEAT_NONE;
                break;
            case REPEAT_NONE:
                playbackMode = PlaybackMode.REPEAT_ALL;
                break;
        }
        notifyPlaybackModeChanged();
    }
    public void setPlaybackMode(PlaybackMode mode) {
        this.playbackMode = mode;
        notifyPlaybackModeChanged();
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void destroyEverything() {
        // 1. Stop and release the MediaPlayer
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;

        // 2. Abandon audio focus
        abandonAudioFocus();

        // 3. Release wake lock
        releaseWakeLock();

        // 4. Stop the service (This removes the notification)
        stopService();

        // 5. Clear the queue and state
        queue = null;
        currentIndex = -1;
    }
    public void setCurrentSong(Song song, List<Song> queue) {
        setQueue(queue);
        this.currentIndex = queue.indexOf(song);

        // Pass 'false' to prepare the song and update UI, but DO NOT play it
        prepareAndPlay(song, false);
    }
    public int getCurrentIndex() {
        return currentIndex;
    }

    public List<Song> getQueue() {
        return queue;
    }
}