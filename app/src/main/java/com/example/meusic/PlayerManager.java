package com.example.meusic;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;

import java.util.ArrayList;
import java.util.List;

public class PlayerManager {
    private static PlayerManager instance;
    private Context appContext;
    private ExoPlayer player;
    private List<Song> queue;
    private int currentIndex = -1;
    private boolean isPlaying = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateProgressRunnable;

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private MediaSession mediaSession;
    private PowerManager.WakeLock wakeLock;

    private Bitmap currentAlbumArt;

    public enum PlaybackMode {
        SHUFFLE, REPEAT_NONE, REPEAT_ALL, REPEAT_ONE
    }

    private PlaybackMode playbackMode = PlaybackMode.REPEAT_ALL;

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
            appContext = context.getApplicationContext();
            audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
            setupWakeLock();
            buildPlayer();
        }
    }

    private void buildPlayer() {
        if (player != null) return;

        androidx.media3.common.AudioAttributes attributes =
                new androidx.media3.common.AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build();

        player = new ExoPlayer.Builder(appContext)
                .setAudioAttributes(attributes, true)
                .build();

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying = playing;
                if (playing) {
                    acquireWakeLock();
                    startProgressUpdate();
                } else {
                    releaseWakeLock();
                    stopProgressUpdate();
                }
                notifyPlaybackState();
                updateServiceNotification();
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                int index = player.getCurrentMediaItemIndex();
                if (index >= 0 && queue != null && index < queue.size()) {
                    currentIndex = index;
                    updateCurrentSongMetadata();
                    notifySongChanged();
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED && playbackMode == PlaybackMode.REPEAT_NONE) {
                    isPlaying = false;
                    notifyPlaybackState();
                }
            }
        });

        applyPlaybackMode();
    }

    public ExoPlayer getPlayer() {
        if (player == null && appContext != null) buildPlayer();
        return player;
    }

    public MediaSession getMediaSession() {
        return mediaSession;
    }

    public void setMediaSession(MediaSession session) {
        if (mediaSession != null && mediaSession != session) mediaSession.release();
        mediaSession = session;
    }

    public void setQueue(List<Song> currentQueue) {
        queue = currentQueue;
    }

    public void addListener(PlayerStateListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(PlayerStateListener listener) {
        listeners.remove(listener);
    }

    public Bitmap getCurrentAlbumArt() {
        return currentAlbumArt;
    }

    public void playSong(Song song, List<Song> newQueue) {
        if (appContext == null) return;
        queue = newQueue;
        currentIndex = newQueue.indexOf(song);
        if (currentIndex < 0) return;

        buildPlayer();
        List<MediaItem> items = new ArrayList<>();
        for (Song s : newQueue) items.add(createMediaItem(s));

        player.setMediaItems(items, currentIndex, 0);
        applyPlaybackMode();
        player.prepare();
        player.play();

        updateCurrentSongMetadata();
        notifySongChanged();
        updateServiceNotification();
    }

    public void playPause() {
        buildPlayer();
        if (player == null) return;

        if (queue == null || queue.isEmpty()) {
            Toast.makeText(appContext, "There are no songs", Toast.LENGTH_LONG).show();
            return;
        }

        if (player.getPlaybackState() == Player.STATE_IDLE) {
            playSong(queue.get(Math.max(0, currentIndex)), queue);
            return;
        }

        if (player.isPlaying()) player.pause();
        else player.play();

        updateServiceNotification();
    }

    public void stopPlayback() {
        releasePlayer();
        stopService();
    }

    public void next() {
        if (queue == null || queue.isEmpty()) return;
        buildPlayer();
        if (player == null) return;

        if (playbackMode == PlaybackMode.REPEAT_ONE) {
            player.seekTo(0);
            player.play();
            return;
        }

        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem();
            player.play();
        } else if (playbackMode == PlaybackMode.REPEAT_ALL) {
            player.seekToDefaultPosition(0);
            player.play();
        } else {
            stopPlayback();
        }
    }

    public void prev() {
        if (queue == null || queue.isEmpty()) return;
        buildPlayer();
        if (player == null) return;

        if (playbackMode == PlaybackMode.REPEAT_ONE) {
            player.seekTo(0);
            player.play();
            return;
        }

        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem();
            player.play();
        } else if (playbackMode == PlaybackMode.REPEAT_ALL) {
            player.seekToDefaultPosition(queue.size() - 1);
            player.play();
        } else {
            player.seekTo(0);
        }
    }

    public void seekTo(int positionMs) {
        if (player != null) player.seekTo(positionMs);
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public Song getCurrentSong() {
        return queue != null && currentIndex >= 0 && currentIndex < queue.size()
                ? queue.get(currentIndex) : null;
    }

    private MediaItem createMediaItem(Song song) {
        Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());

        return new MediaItem.Builder()
                .setMediaId(String.valueOf(song.getId()))
                .setUri(trackUri)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(song.getTitle())
                        .setArtist(song.getArtist())
                        .setAlbumTitle(song.getAlbum())
                        .build())
                .build();
    }

    private void updateCurrentSongMetadata() {
        Song song = getCurrentSong();
        if (song != null) currentAlbumArt = getAlbumArtBitmap(song);
    }

    public void releasePlayer() {
        stopProgressUpdate();
        if (player != null) {
            player.stop();
            player.clearMediaItems();
            player.release();
            player = null;
        }
        isPlaying = false;
        abandonAudioFocus();
        releaseWakeLock();
        notifyPlaybackState();
    }

    private boolean requestAudioFocus() {
        if (audioManager == null) return true;

        android.media.AudioAttributes attributes = new android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build();
            return audioManager.requestAudioFocus(audioFocusRequest)
                    == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }

        return audioManager.requestAudioFocus(
                audioFocusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focus -> {
        if (player == null) return;
        if (focus == AudioManager.AUDIOFOCUS_LOSS ||
                focus == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            player.pause();
        } else if (focus == AudioManager.AUDIOFOCUS_GAIN) {
            player.setVolume(1f);
        } else if (focus == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            player.setVolume(0.2f);
        }
    };

    private Bitmap getAlbumArtBitmap(Song song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            Uri uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
            retriever.setDataSource(appContext, uri);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                return Bitmap.createScaledBitmap(bitmap, 256, 256, true);
            }
        } catch (Exception ignored) {
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
        return null;
    }

    private void applyPlaybackMode() {
        if (player == null) return;

        switch (playbackMode) {
            case REPEAT_ONE:
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                player.setShuffleModeEnabled(false);
                break;
            // But REPEAT_NONE here until i Fix it
            case REPEAT_NONE:
            case REPEAT_ALL:
                player.setRepeatMode(Player.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(false);
                break;
            case SHUFFLE:
                player.setRepeatMode(Player.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(true);
                break;
//            case REPEAT_NONE:
//                player.setRepeatMode(Player.REPEAT_MODE_OFF);
//                player.setShuffleModeEnabled(false);
//                break;
        }
    }

    private void setupWakeLock() {
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MusicWakeLock");
        }
    }

    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(10 * 60 * 1000L);
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    private void updateServiceNotification() {
        if (appContext == null) return;
        android.content.Intent intent = new android.content.Intent(
                appContext, PlaybackService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
    }

    private void stopService() {
        if (appContext != null) {
            appContext.stopService(new android.content.Intent(
                    appContext, PlaybackService.class));
        }
    }

    private void startProgressUpdate() {
        if (updateProgressRunnable != null) return;
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && player.isPlaying()) notifyProgress();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateProgressRunnable);
    }

    private void stopProgressUpdate() {
        if (updateProgressRunnable != null) {
            handler.removeCallbacks(updateProgressRunnable);
            updateProgressRunnable = null;
        }
    }

    private void notifySongChanged() {
        Song song = getCurrentSong();
        if (song != null) {
            for (PlayerStateListener l : listeners) l.onSongChanged(song);
        }
    }

    private void notifyPlaybackState() {
        for (PlayerStateListener l : listeners) l.onPlaybackStateChanged(isPlaying());
    }

    private void notifyProgress() {
        if (player != null && player.getDuration() != C.TIME_UNSET) {
            int current = (int) player.getCurrentPosition();
            int total = (int) player.getDuration();
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
        for (OnPlaybackModeChangedListener l : modeListeners) {
            l.onPlaybackModeChanged(playbackMode);
        }
    }

    public void cyclePlaybackMode() {
        switch (playbackMode) {
            case REPEAT_ALL: playbackMode = PlaybackMode.REPEAT_ONE; break;
            case REPEAT_ONE: playbackMode = PlaybackMode.SHUFFLE; break;
            case SHUFFLE: playbackMode = PlaybackMode.REPEAT_NONE; break;
            case REPEAT_NONE: playbackMode = PlaybackMode.REPEAT_ALL; break;
        }
        applyPlaybackMode();
        notifyPlaybackModeChanged();
    }

    public void setPlaybackMode(PlaybackMode mode) {
        playbackMode = mode;
        applyPlaybackMode();
        notifyPlaybackModeChanged();
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void destroyEverything() {
        releasePlayer();
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        stopService();
        queue = null;
        currentIndex = -1;
    }

    public void setCurrentSong(Song song, List<Song> queue) {
        setQueue(queue);
        currentIndex = queue.indexOf(song);
        if (currentIndex < 0) return;

        buildPlayer();
        List<MediaItem> items = new ArrayList<>();
        for (Song s : queue) items.add(createMediaItem(s));
        player.setMediaItems(items, currentIndex, 0);
        applyPlaybackMode();
        player.prepare();
        updateCurrentSongMetadata();
        notifySongChanged();
        updateServiceNotification();
    }

    public int getCurrentIndex() { return currentIndex; }
    public List<Song> getQueue() { return queue; }
}