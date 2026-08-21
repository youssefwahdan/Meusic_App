package com.example.first_app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PlaybackService extends Service {

    public static final String CHANNEL_ID = "MusicPlaybackChannel";
    public static final int NOTIFICATION_ID = 101;

    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) handleIntent(intent);

        // Build and show the custom notification
        Notification notification = buildCustomNotification();
        startForeground(NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        // Save state before killing the service
        new AppStateManager(this).saveState(
                PlayerManager.getInstance().getPlaybackMode(),
                PlayerManager.getInstance().getCurrentIndex(),
                PlayerManager.getInstance().getQueue(),
                MusicLibrary.getInstance().getCurrentSortIndex()
        );
        // 1. Stop the music and release resources
        PlayerManager.getInstance().releasePlayer();

        // 2. Remove the notification and stop the service
        stopForeground(true);
        stopSelf();
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        PlayerManager player = PlayerManager.getInstance();
        if (action != null) {
            switch (action) {
                case ACTION_PLAY_PAUSE: player.playPause(); break;
                case ACTION_NEXT: player.next(); break;
                case ACTION_PREV: player.prev(); break;
            }
            // Update notification UI after state changes
            updateNotification();
        }
    }

    // Call this from PlayerManager when song or play state changes
    public void updateNotification() {
        Notification notification = buildCustomNotification();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildCustomNotification() {
        Song currentSong = PlayerManager.getInstance().getCurrentSong();
        boolean isPlaying = PlayerManager.getInstance().isPlaying();
        Bitmap albumArt = PlayerManager.getInstance().getCurrentAlbumArt();

        // 1. Create the RemoteViews object using our custom layout
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_player);
        RemoteViews smallRemoteViews = new RemoteViews(getPackageName(), R.layout.small_notification_player);

        // 2. Set the data (Text and Image)
        if (currentSong != null) {
            remoteViews.setTextViewText(R.id.notif_song_title, currentSong.getTitle());
            remoteViews.setTextViewText(R.id.notif_song_artist, currentSong.getArtist());
            // for the small one
            smallRemoteViews.setTextViewText(R.id.notif_song_title, currentSong.getTitle());
        }
        if (albumArt != null) {
            remoteViews.setImageViewBitmap(R.id.notif_album_art, albumArt);
        } else {
            remoteViews.setImageViewResource(R.id.notif_album_art, R.drawable.ic_music);
        }

        // 3. Set the Play/Pause icon dynamically
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
        remoteViews.setImageViewResource(R.id.notif_play_pause_btn, playPauseIcon);
        // for the small one
        smallRemoteViews.setImageViewResource(R.id.notif_play_pause_btn, playPauseIcon);

        // 4. Create PendingIntents for button clicks
        Intent openAppIntent = new Intent(this, AllSongsActivity.class);
        PendingIntent openAppPending = PendingIntent.getActivity(this, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent playPausePending = createPendingIntent(ACTION_PLAY_PAUSE);
        PendingIntent nextPending = createPendingIntent(ACTION_NEXT);
        PendingIntent prevPending = createPendingIntent(ACTION_PREV);

        // 5. Attach click listeners to the RemoteViews buttons
        remoteViews.setOnClickPendingIntent(R.id.notif_play_pause_btn, playPausePending);
        remoteViews.setOnClickPendingIntent(R.id.notif_next_btn, nextPending);
        remoteViews.setOnClickPendingIntent(R.id.notif_prev_btn, prevPending);

        // for the small one
        smallRemoteViews.setOnClickPendingIntent(R.id.notif_play_pause_btn, playPausePending);
        smallRemoteViews.setOnClickPendingIntent(R.id.notif_next_btn, nextPending);
        smallRemoteViews.setOnClickPendingIntent(R.id.notif_prev_btn, prevPending);

        // 6. Build the Notification using DecoratedCustomViewStyle
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music) // Required: small icon for status bar
                .setContentIntent(openAppPending) // Tap the whole notification to open app
                .setOngoing(true)
                .setCustomContentView(smallRemoteViews) // Apply our custom layout
                .setCustomBigContentView(remoteViews) // Use same layout when expanded
                .build();
    }

    private PendingIntent createPendingIntent(String action) {
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, action.hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Music Playback", android.app.NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Fallback cleanup just in case
        PlayerManager.getInstance().destroyEverything();
        if (PlayerManager.getInstance() != null) {
            PlayerManager.getInstance().releasePlayer();
        }
    }
}