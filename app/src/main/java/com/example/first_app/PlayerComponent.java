package com.example.first_app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.constraintlayout.motion.widget.MotionLayout;
import jp.wasabeef.blurry.Blurry;

public class PlayerComponent implements PlayerManager.PlayerStateListener {
    private MotionLayout motionLayout;
    private PlayerManager playerManager;

    private View mainContainer;
    private TextView titleText, artistText, currentTime, totalTime;
    private ImageView artView, bgArtView, playBtn, prevBtn, nextBtn, downBtn, favBtn;
    private SeekBar seekBar;
    private float startProgress = 0f;
    private long currentSongId = -1;

    public PlayerComponent(MotionLayout motionLayout, PlayerManager playerManager) {
        this.motionLayout = motionLayout;
        this.playerManager = playerManager;

        initViews();
        setupListeners();
        this.playerManager.addListener(this);

        onSongChanged(this.playerManager.getCurrentSong());
        onPlaybackStateChanged(this.playerManager.isPlaying());
    }

    private void initViews() {
        mainContainer = motionLayout.findViewById(R.id.main_container);
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
        favBtn = motionLayout.findViewById(R.id.favourite_icon);

    }

    private void setupListeners() {
        if (playBtn != null) playBtn.setOnClickListener(v -> playerManager.playPause());
        if (prevBtn != null) prevBtn.setOnClickListener(v -> playerManager.prev());
        if (nextBtn != null) nextBtn.setOnClickListener(v -> playerManager.next());

        if (downBtn != null) {
            downBtn.setOnClickListener(v -> motionLayout.transitionToStart());
        }

        if (favBtn != null) {
            favBtn.setOnClickListener(v -> {
                if (currentSongId != -1) {
                    // Toggle the favorite status in the database
                    FavoriteManager.getInstance(v.getContext()).toggleFavorite(currentSongId);

                    // Update the icon immediately (we'll refresh the actual state from DB below)
                    updateFavoriteIcon(currentSongId);
                }
            });
        }

        if (mainContainer != null) {
            final float screenHeight = motionLayout.getResources().getDisplayMetrics().heightPixels;
            final float touchSlop = ViewConfiguration.get(motionLayout.getContext()).getScaledTouchSlop();

            mainContainer.setOnTouchListener(new View.OnTouchListener() {
                private float startY;
                private float startProgress;
                private boolean isDragging = false;
                private VelocityTracker velocityTracker;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            startY = event.getRawY();
                            startProgress = motionLayout.getProgress();
                            isDragging = false;

                            if (velocityTracker == null) {
                                velocityTracker = VelocityTracker.obtain();
                            } else {
                                velocityTracker.clear();
                            }
                            velocityTracker.addMovement(event);

                            return true; // Consume event to receive MOVE and UP

                        case MotionEvent.ACTION_MOVE:
                            if (velocityTracker != null) velocityTracker.addMovement(event);

                            float dy = event.getRawY() - startY;

                            // Only start dragging if the finger moved enough to not be considered a tap
                            if (!isDragging && Math.abs(dy) > touchSlop) {
                                isDragging = true;
                            }

                            if (isDragging) {
                                float newProgress = startProgress - (dy / screenHeight);
                                motionLayout.setProgress(clamp(newProgress, 0f, 1f));
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            if (velocityTracker != null) {
                                velocityTracker.addMovement(event);
                                velocityTracker.computeCurrentVelocity(1000); // pixels per second
                                float yVelocity = velocityTracker.getYVelocity();
                                velocityTracker.recycle();
                                velocityTracker = null;

                                if (isDragging) {
                                    // ==========================================
                                    // 1. VELOCITY / FLING LOGIC
                                    // ==========================================
                                    float flingThreshold = 1000f; // Adjust sensitivity here

                                    if (yVelocity > flingThreshold) {
                                        motionLayout.transitionToStart(); // Fast swipe DOWN
                                    } else if (yVelocity < -flingThreshold) {
                                        motionLayout.transitionToEnd();   // Fast swipe UP
                                    } else {
                                        // ==========================================
                                        // 2. SLOW RELEASE: 50% THRESHOLD LOGIC
                                        // ==========================================
                                        if (motionLayout.getProgress() >= 0.5f) {
                                            motionLayout.transitionToEnd();
                                        } else {
                                            motionLayout.transitionToStart();
                                        }
                                    }
                                } else {
                                    // ==========================================
                                    // 3. SINGLE TAP LOGIC
                                    // ==========================================
                                    // This triggers if the user tapped without dragging

                                    // Example: Tap to expand if currently collapsed
                                    if (motionLayout.getProgress() < 0.5f) {
                                        motionLayout.transitionToEnd();
                                    }

                                    // Optional: Tap to collapse if currently expanded
                                    // else {
                                    //     motionLayout.transitionToStart();
                                    // }
                                }
                                return true;
                            }
                            return false;
                    }
                    return false;
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
        if (artView != null) artView.setImageResource(R.drawable.ic_music);

        updateFavoriteState(song);
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


    // 3. Add this method to update the icon based on the current song
    public void updateFavoriteState(Song song) {
        if (song != null) {
            currentSongId = song.getId();
            updateFavoriteIcon(currentSongId);
        }
    }

    // 4. Helper method to check DB and set the correct icon
    private void updateFavoriteIcon(long songId) {
        // We use the async method to check the database safely
        // Note: You might need to add an 'isFavoriteAsync' method to FavoriteManager similar to getFavouritesCountAsync
        // OR, simpler: use the LiveData observer since motionLayout.getContext() is usually the Activity.

        FavoriteManager.getInstance(motionLayout.getContext())
                .getFavoriteStatus(songId)
                .observe((androidx.lifecycle.LifecycleOwner) motionLayout.getContext(), status -> {
                    if (status != null && status == 1) {
                        favBtn.setImageResource(R.drawable.ic_favorite); // Filled heart/star
                        favBtn.setTag("fav");
                    } else {
                        favBtn.setImageResource(R.drawable.ic_favorite_border); // Empty heart/star
                        favBtn.setTag("not_fav");
                    }
                });
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
    public void detach() {
        playerManager.removeListener(this);
    }
}