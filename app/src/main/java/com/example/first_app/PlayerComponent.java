package com.example.first_app;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.motion.widget.MotionLayout;

import java.util.List;

import jp.wasabeef.blurry.Blurry;
/**
 * PlayerComponent
 *
 * A UI controller that binds a MotionLayout-based player view to the global PlayerManager.
 * Implements PlayerManager.PlayerStateListener to receive playback updates and reflect them
 * in the UI (title, artist, album art, play/pause state, and seek progress).
 *
 * Responsibilities
 * - Initialize and cache view references from a MotionLayout container.
 * - Wire UI controls (play, next, prev, down, seekbar) to PlayerManager actions.
 * - Listen for PlayerManager callbacks and update UI accordingly:
 *     - onSongChanged: update title, artist, and album art.
 *     - onPlaybackStateChanged: update play/pause icon.
 *     - onProgressChanged: update seekbar and time labels.
 * - Load embedded album art off the main thread using MediaMetadataRetriever and post results
 *   back to the MotionLayout context. Apply a blurred background using the Blurry library.
 * - Register and unregister itself as a PlayerStateListener to avoid leaks.
 *
 * Threading and lifecycle notes
 * - Album art extraction runs on a background thread; UI updates are posted to the MotionLayout
 *   via motionLayout.post(...) to ensure they run on the main thread.
 * - Call detach() from the hosting Activity or Fragment onDestroy (or equivalent) to remove
 *   the listener and avoid memory leaks.
 * - This component does not manage runtime permissions itself; ensure audio/media permissions
 *   are granted before attempting playback or metadata retrieval on older Android versions.
 *
 * UI behavior and interactions
 * - Play button toggles playback via playerManager.playPause().
 * - Prev/Next buttons call playerManager.prev() and playerManager.next().
 * - SeekBar:
 *     - Shows current and total duration (milliseconds).
 *     - User scrubbing updates the current time label while dragging and calls playerManager.seekTo()
 *       when the user stops dragging.
 * - Main container click expands the MotionLayout if it is collapsed (progress == 0f).
 * - Down button transitions the MotionLayout back to its start state.
 *
 * Error handling
 * - Exceptions during metadata retrieval are caught and printed to log (e.printStackTrace()).
 * - If album art is not embedded or cannot be decoded, the component leaves the default
 *   placeholder (R.drawable.ic_music) in place.
 *
 * Dependencies and permissions
 * - Requires the Blurry library (jp.wasabeef:blurry) for background blur operations.
 * - Uses MediaMetadataRetriever to read embedded artwork from MediaStore URIs.
 * - Ensure READ_EXTERNAL_STORAGE or appropriate scoped storage access is available on Android
 *   versions that require it.
 *
 * Expected view IDs inside the provided MotionLayout
 * - R.id.song_title
 * - R.id.song_artist
 * - R.id.album_art
 * - R.id.background_album_art
 * - R.id.play_btn
 * - R.id.prev_btn
 * - R.id.next_btn
 * - R.id.down_btn
 * - R.id.seek_bar
 * - R.id.current_time
 * - R.id.total_time
 * - R.id.main_container
 *
 * Example usage
 *   // In Activity/Fragment after inflating layout with MotionLayout:
 *   MotionLayout motionLayout = findViewById(R.id.player_motion_layout);
 *   PlayerManager playerManager = PlayerManager.getInstance();
 *   PlayerComponent playerComponent = new PlayerComponent(motionLayout, playerManager);
 *
 *   // When Activity/Fragment is destroyed:
 *   @Override
 *   protected void onDestroy() {
 *       super.onDestroy();
 *       playerComponent.detach();
 *   }
 *
 * Extensibility suggestions
 * - Add an explicit permission check and request flow for audio/media access.
 * - Provide a callback interface for the host Activity/Fragment to react to expand/collapse events.
 * - Add placeholder handling and crossfade when switching album art bitmaps.
 * - Expose methods to programmatically expand/collapse the MotionLayout or update the queue UI.
 *
 * Notes
 * - This component assumes PlayerManager has been initialized and may already have a current song.
 * - Keep UI updates minimal and efficient to avoid jank during frequent progress updates.
 *
 * @see PlayerManager
 * @see MediaMetadataRetriever
 * @see jp.wasabeef.blurry.Blurry
 */

public class PlayerComponent implements PlayerManager.PlayerStateListener {
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 1001;

    private MotionLayout motionLayout;
    private PlayerManager playerManager;

    private TextView titleText, artistText, currentTime, totalTime;
    private ImageView artView, bgArtView, playBtn, prevBtn, nextBtn, downBtn;
    private SeekBar seekBar;

    private List<Song> songsList;
    public PlayerComponent(MotionLayout motionLayout, PlayerManager playerManager) {
        this.motionLayout = motionLayout;
        this.playerManager = playerManager;

        initViews();
        setupListeners();
        songsList = MusicLibrary.getInstance().getSongs();
        // Start listening to global player state
        this.playerManager.addListener(this);

        // Initialize UI with current state (in case a song is already playing)
        onSongChanged(this.playerManager.getCurrentSong());
        onPlaybackStateChanged(this.playerManager.isPlaying());
    }

    private void initViews() {
        titleText = motionLayout.findViewById(R.id.song_title);
        artistText = motionLayout.findViewById(R.id.song_artist);
        artView = motionLayout.findViewById(R.id.album_art);
        bgArtView = motionLayout.findViewById(R.id.background_album_art);
        playBtn = motionLayout.findViewById(R.id.play_btn);
        prevBtn = motionLayout.findViewById(R.id.prev_btn);
        nextBtn = motionLayout.findViewById(R.id.next_btn);
        downBtn = motionLayout.findViewById(R.id.down_btn);
        seekBar = motionLayout.findViewById(R.id.seek_bar);
        currentTime = motionLayout.findViewById(R.id.current_time);
        totalTime = motionLayout.findViewById(R.id.total_time);
    }

    private void setupListeners() {
        if (playBtn != null) playBtn.setOnClickListener(v -> playerManager.playPause());
        if (prevBtn != null) prevBtn.setOnClickListener(v -> playerManager.prev());
        if (nextBtn != null) nextBtn.setOnClickListener(v -> playerManager.next());

        if (downBtn != null) {
            downBtn.setOnClickListener(v -> motionLayout.transitionToStart());
        }

        // Allow clicking the main container to expand the player if it's collapsed
        View mainContainer = motionLayout.findViewById(R.id.main_container);
        if (mainContainer != null) {
            mainContainer.setOnClickListener(v -> {
                if (motionLayout.getProgress() == 0f) {
                    motionLayout.transitionToEnd();
                }
            });
        }

        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && currentTime != null) currentTime.setText(formatTime(progress));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    playerManager.seekTo(seekBar.getProgress());
                }
            });
        }
    }

    // --- PlayerStateListener Implementations ---
    @Override
    public void onSongChanged(Song song) {
        if (song == null) return;
        if (titleText != null) titleText.setText(song.getTitle());
        if (artistText != null) artistText.setText(song.getArtist());
        if (artView != null) artView.setImageResource(R.drawable.ic_music); // Reset before loading

        // Automatically expand the player when a new song is played
//        motionLayout.transitionToEnd();

        loadAlbumArt(song);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (playBtn != null) {
            playBtn.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
        }
    }

    @Override
    public void onProgressChanged(int currentMs, int totalMs) {
        if (seekBar != null) {
            seekBar.setMax(totalMs);
            seekBar.setProgress(currentMs);
            if (currentTime != null) currentTime.setText(formatTime(currentMs));
            if (totalTime != null) totalTime.setText(formatTime(totalMs));
        }
    }

    private String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void loadAlbumArt(Song song) {
        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                Uri trackUri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
                retriever.setDataSource(motionLayout.getContext(), trackUri);
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    if (bitmap != null) {
                        motionLayout.post(() -> {
                            if (artView != null) artView.setImageBitmap(bitmap);
                            if (bgArtView != null) {
                                Blurry.with(motionLayout.getContext())
                                        .radius(20)
                                        .sampling(4)
                                        .from(bitmap)
                                        .into(bgArtView);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { retriever.release(); } catch (Exception ignored) {}
            }
        }).start();
    }

    // Call this in the Activity's onDestroy to prevent memory leaks
    public void detach() {
        playerManager.removeListener(this);
    }
}