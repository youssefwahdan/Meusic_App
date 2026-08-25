package com.example.meusic.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.meusic.R;
import com.example.meusic.activities.AllSongsActivity;
import com.example.meusic.managers.AppStateManager;
import com.example.meusic.managers.MusicLibrary;
import com.example.meusic.managers.PlayerManager;
import com.example.meusic.models.Song;

public class PlaybackService extends MediaSessionService {

    public static final String CHANNEL_ID = "MusicPlaybackChannel";
    public static final int NOTIFICATION_ID = 101;

    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_UPDATE_UI = "UPDATE_UI";

    private PlayerManager playerManager;
    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();

        playerManager = PlayerManager.getInstance();
        playerManager.init(getApplicationContext());

        mediaSession = new MediaSession.Builder(this, playerManager.getPlayer())
                .setCallback(new MediaSession.Callback() {

                    @Override
                    public boolean onMediaButtonEvent(
                            MediaSession session,
                            MediaSession.ControllerInfo controllerInfo,
                            Intent intent) {

                        KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                        if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                            switch (keyEvent.getKeyCode()) {
                                case KeyEvent.KEYCODE_MEDIA_PLAY:
                                    if (!playerManager.isPlaying()) playerManager.playPause();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                                    if (playerManager.isPlaying()) playerManager.playPause();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                                case KeyEvent.KEYCODE_HEADSETHOOK:
                                    playerManager.playPause();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_NEXT:
                                    playerManager.next();
                                    return true;
                                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                                    playerManager.prev();
                                    return true;
                            }
                        }
                        return false;
                    }
                })
                .build();

        playerManager.setMediaSession(mediaSession);
        createNotificationChannel();
    }

    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) handleIntent(intent);

        Notification notification = buildCustomNotification();
        startForeground(NOTIFICATION_ID, notification);

        return Service.START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        new AppStateManager(this).saveState(
                playerManager.getPlaybackMode(),
                playerManager.getCurrentIndex(),
                playerManager.getQueue(),
                MusicLibrary.getInstance().getCurrentSortIndex()
        );

        playerManager.releasePlayer();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTION_PLAY_PAUSE:
                playerManager.playPause();
                break;
            case ACTION_NEXT:
                playerManager.next();
                break;
            case ACTION_PREV:
                playerManager.prev();
                break;
            case ACTION_UPDATE_UI:
                break;
        }

        updateNotification();
    }

    public void updateNotification() {
        Notification notification = buildCustomNotification();
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, notification);
    }

    private Notification buildCustomNotification() {
        Song currentSong = playerManager.getCurrentSong();
        boolean isPlaying = playerManager.isPlaying();
        Bitmap albumArt = playerManager.getCurrentAlbumArt();

        RemoteViews remoteViews =
                new RemoteViews(getPackageName(), R.layout.notification_player);
        RemoteViews smallRemoteViews =
                new RemoteViews(getPackageName(), R.layout.small_notification_player);

        if (currentSong != null) {
            remoteViews.setTextViewText(
                    R.id.notif_song_title, currentSong.getTitle());
            remoteViews.setTextViewText(
                    R.id.notif_song_artist, currentSong.getArtist());
            smallRemoteViews.setTextViewText(
                    R.id.notif_song_title, currentSong.getTitle());
        }

        if (albumArt != null) {
            remoteViews.setImageViewBitmap(R.id.notif_album_art, albumArt);
        } else {
            remoteViews.setImageViewResource(
                    R.id.notif_album_art, R.drawable.ic_music);
        }

        int playPauseIcon =
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow;

        remoteViews.setImageViewResource(
                R.id.notif_play_pause_btn, playPauseIcon);
        smallRemoteViews.setImageViewResource(
                R.id.notif_play_pause_btn, playPauseIcon);

        Intent openAppIntent = new Intent(this, AllSongsActivity.class);
        PendingIntent openAppPending = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent playPausePending = createPendingIntent(ACTION_PLAY_PAUSE);
        PendingIntent nextPending = createPendingIntent(ACTION_NEXT);
        PendingIntent prevPending = createPendingIntent(ACTION_PREV);

        remoteViews.setOnClickPendingIntent(
                R.id.notif_play_pause_btn, playPausePending);
        remoteViews.setOnClickPendingIntent(R.id.notif_next_btn, nextPending);
        remoteViews.setOnClickPendingIntent(R.id.notif_prev_btn, prevPending);

        smallRemoteViews.setOnClickPendingIntent(
                R.id.notif_play_pause_btn, playPausePending);
        smallRemoteViews.setOnClickPendingIntent(
                R.id.notif_next_btn, nextPending);
        smallRemoteViews.setOnClickPendingIntent(
                R.id.notif_prev_btn, prevPending);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music)
                .setContentIntent(openAppPending)
                .setOngoing(isPlaying)
                .setCustomContentView(smallRemoteViews)
                .setCustomBigContentView(remoteViews)
                .build();
    }

    private PendingIntent createPendingIntent(String action) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);

        return PendingIntent.getService(
                this,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onDestroy() {
        if (playerManager != null) {
            playerManager.destroyEverything();
        }
        super.onDestroy();
    }
}