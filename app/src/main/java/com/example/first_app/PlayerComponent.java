package com.example.first_app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.constraintlayout.motion.widget.MotionLayout;


import jp.wasabeef.blurry.Blurry;
/**
 * PlayerComponent
 *
 * UI binder that connects a MotionLayout player view to PlayerManager.
 * - Updates title, artist, album art, play/pause icon, seekbar and time labels.
 * - Handles user actions: play/pause, next, prev, seek, expand/collapse.
 * - Loads embedded album art on a background thread and applies a blurred background.
 *
 * Public / lifecycle methods (what each does)
 * - PlayerComponent(MotionLayout motionLayout, PlayerManager playerManager)
 *     Constructor: finds views inside the MotionLayout, wires UI listeners,
 *     registers as a PlayerStateListener, and initializes UI from current player state.
 *
 * - detach()
 *     Unregister this component from PlayerManager to avoid memory leaks (call in onDestroy).
 *
 * PlayerStateListener implementations (called by PlayerManager)
 * - onSongChanged(Song song)
 *     Update title and artist text, reset placeholder art, and start loading album art.
 *
 * - onPlaybackStateChanged(boolean isPlaying)
 *     Update the play/pause button icon to reflect current playback state.
 *
 * - onProgressChanged(int currentMs, int totalMs)
 *     Update seekBar max/progress and the current/total time labels.
 *
 * UI wiring and helpers
 * - initViews()
 *     Cache references to required views (title, artist, art, buttons, seekbar, times).
 *
 * - setupListeners()
 *     Attach click handlers for play/prev/next/down and a SeekBar listener that
 *     updates the displayed time while dragging and calls playerManager.seekTo() on release.
 *
 * - loadAlbumArt(Song song)
 *     Background thread: use MediaMetadataRetriever with a MediaStore Uri to extract
 *     embedded artwork; post bitmap updates to the main thread and blur the background.
 *
 * Notes
 * - Call detach() when the host Activity/Fragment is destroyed.
 * - Ensure required permissions are granted before metadata retrieval on older Android versions.
 */


public class PlayerComponent implements PlayerManager.PlayerStateListener {
    private MotionLayout motionLayout;
    private PlayerManager playerManager;

    private TextView titleText, artistText, currentTime, totalTime;
    private ImageView artView, bgArtView, playBtn, prevBtn, nextBtn, downBtn;
    private SeekBar seekBar;

    public PlayerComponent(MotionLayout motionLayout, PlayerManager playerManager) {
        this.motionLayout = motionLayout;
        this.playerManager = playerManager;

        initViews();
        setupListeners();
        // Start listening to global player state
        this.playerManager.addListener(this);

        // Initialize UI with current state (in case a song is already playing)
        onSongChanged(this.playerManager.getCurrentSong());
        onPlaybackStateChanged(this.playerManager.isPlaying());
    }

    public void disappear() {
        titleText.setVisibility(View.GONE);
        artistText.setVisibility(View.GONE);
        artView.setVisibility(View.GONE);
        bgArtView.setVisibility(View.GONE);
        playBtn.setVisibility(View.GONE);
        prevBtn.setVisibility(View.GONE);
        nextBtn.setVisibility(View.GONE);
        downBtn.setVisibility(View.GONE);
        seekBar.setVisibility(View.GONE);
        currentTime.setVisibility(View.GONE);
        totalTime.setVisibility(View.GONE);
    }
    public void appear() {
        titleText.setVisibility(View.VISIBLE);
        artistText.setVisibility(View.VISIBLE);
        artView.setVisibility(View.VISIBLE);
        bgArtView.setVisibility(View.VISIBLE);
        playBtn.setVisibility(View.VISIBLE);
        prevBtn.setVisibility(View.VISIBLE);
        nextBtn.setVisibility(View.VISIBLE);
        downBtn.setVisibility(View.VISIBLE);
        seekBar.setVisibility(View.VISIBLE);
        currentTime.setVisibility(View.VISIBLE);
        totalTime.setVisibility(View.VISIBLE);
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