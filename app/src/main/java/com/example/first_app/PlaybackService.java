package com.example.first_app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class PlaybackService extends Service {

    public static final String CHANNEL_ID = "MusicPlaybackChannel";
    public static final int NOTIFICATION_ID = 1;

    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";
    public static final String ACTION_STOP = "ACTION_STOP";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            // Handle clicks from the notification buttons
            switch (intent.getAction()) {
                case ACTION_PLAY_PAUSE:
                    PlayerManager.getInstance().playPause();
                    break;
                case ACTION_NEXT:
                    PlayerManager.getInstance().next();
                    break;
                case ACTION_PREV:
                    PlayerManager.getInstance().prev();
                    break;
                case ACTION_STOP:
                    PlayerManager.getInstance().releasePlayer();
                    stopSelf();
                    return START_NOT_STICKY;
            }
        }

        // Build and show the notification
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    private Notification buildNotification() {
        Song currentSong = PlayerManager.getInstance().getCurrentSong();
        boolean isPlaying = PlayerManager.getInstance().isPlaying();

        // Intent to open the app when the notification body is clicked
        Intent mainIntent = new Intent(this, AllSongsActivity.class); // Change to your main activity
        PendingIntent pendingMainIntent = PendingIntent.getActivity(this, 0, mainIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // Intents for the control buttons
        PendingIntent playPausePendingIntent = createPendingIntent(ACTION_PLAY_PAUSE);
        PendingIntent nextPendingIntent = createPendingIntent(ACTION_NEXT);
        PendingIntent prevPendingIntent = createPendingIntent(ACTION_PREV);
        PendingIntent stopPendingIntent = createPendingIntent(ACTION_STOP);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music) // Replace with your app icon
                .setContentTitle(currentSong != null ? currentSong.getTitle() : "Not Playing")
                .setContentText(currentSong != null ? currentSong.getArtist() : "")
                .setContentIntent(pendingMainIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                // Add Actions (Buttons)
                .addAction(new NotificationCompat.Action(R.drawable.ic_skip_previous, "Previous", prevPendingIntent))
                .addAction(new NotificationCompat.Action(
                        isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow,
                        isPlaying ? "Pause" : "Play",
                        playPausePendingIntent))
                .addAction(new NotificationCompat.Action(R.drawable.ic_skip_next, "Next", nextPendingIntent))
                // MediaStyle links this notification to the MediaSession (Crucial for Huawei/Quick Settings)
                .setStyle(new MediaStyle()
                        .setMediaSession(PlayerManager.getInstance().getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2) // Show all 3 buttons in the compact view
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(stopPendingIntent));

        return builder.build();
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
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW // LOW prevents sound/vibration when notification appears
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}