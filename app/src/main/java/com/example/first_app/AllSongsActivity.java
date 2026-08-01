package com.example.first_app;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.blurry.Blurry;

public class AllSongsActivity extends AppCompatActivity {

    private static final int AUDIO_PERMISSION_REQUEST_CODE = 1001;

    private MotionLayout motionLayout;
    private TextView titleText, artistText, currentTime, totalTime;
    private ImageView artView, bgArtView, playBtn, prevBtn, nextBtn;
    private SeekBar seekBar;
    // SeekBar Handler
    private Handler handler;
    private Runnable updateSeekBarRunnable;

    private Toolbar toolbar;

    private final List<Song> songs = new ArrayList<>();
    private SongAdapter adapter;
    private int currentIndex = -1;

    private MediaPlayer mediaPlayer;
    private Song currentSong;

    private boolean isPlaying = false;
    private boolean shouldResumeOnFocusGain = false;

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_songs);

        setupToolbar();
        initViews();
        setupListeners();

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        handler = new Handler(Looper.getMainLooper());

        checkPermissionAndLoadSongs();
    }
    private void initViews() {
        motionLayout = findViewById(R.id.music_player_motionLayout);
        titleText = findViewById(R.id.song_title);
        artistText = findViewById(R.id.song_artist);
        artView = findViewById(R.id.album_art);
        bgArtView = findViewById(R.id.background_album_art);
        playBtn = findViewById(R.id.play_btn);
        prevBtn = findViewById(R.id.prev_btn);
        nextBtn = findViewById(R.id.next_btn);
        seekBar = findViewById(R.id.seek_bar);
        currentTime = findViewById(R.id.current_time);
        totalTime = findViewById(R.id.total_time);
    }

    private void setupListeners() {
        playBtn.setOnClickListener(v -> togglePlayPause());
        prevBtn.setOnClickListener(v -> playPrev());
        nextBtn.setOnClickListener(v -> playNext());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopSeekBarUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
                startSeekBarUpdate();
            }
        });
    }
    private void checkPermissionAndLoadSongs() {
        String permission;

        if (Build.VERSION.SDK_INT >= 33) {
            permission = "android.permission.READ_MEDIA_AUDIO";
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED) {
            loadSongs();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{permission},
                    AUDIO_PERMISSION_REQUEST_CODE
            );
        }
    }

    private void loadSongs() {
        new Thread(() -> {
            final List<Song> fetchedSongs = fetchAllSongs();
            if (!isFinishing()) {
                runOnUiThread(() -> {
                    songs.clear();
                    songs.addAll(fetchedSongs);

                    RecyclerView recyclerView = findViewById(R.id.songs_recycler_view);
                    adapter = new SongAdapter(songs, (song, position) -> playSong(position));
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(adapter);
                });
            }
        }).start();
    }

    private void playSong(int position) {
        if (position < 0 || position >= songs.size()) return;

        currentIndex = position;
        Song song = songs.get(currentIndex);

//        motionLayout.transitionToEnd();

        titleText.setText(song.getTitle());
        artistText.setText(song.getArtist());
        artView.setImageResource(R.drawable.ic_music);
        loadAlbumArt(song);

        prepareAndStart(song);
    }

    private void playNext() {
        if (currentIndex < songs.size() - 1) {
            playSong(currentIndex + 1);
        } else {
            releasePlayer();
            motionLayout.transitionToStart();
        }
    }

    private void playPrev() {
        if (currentIndex > 0) {
            playSong(currentIndex - 1);
        } else {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(0);
                seekBar.setProgress(0);
                currentTime.setText("00:00");
            }
        }
    }
    private void prepareAndStart(Song song) {
        releasePlayer();

        if (!requestAudioFocus()) {
            Toast.makeText(this, "Audio focus not granted", Toast.LENGTH_SHORT).show();
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

            Uri trackUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
            mediaPlayer.setDataSource(this, trackUri);

            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                totalTime.setText(formatTime(mp.getDuration()));
                currentTime.setText("00:00");
                seekBar.setProgress(0);

                mp.start();
                isPlaying = true;
                updatePlayPauseIcon();
                startSeekBarUpdate();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                stopSeekBarUpdate();
                playNext(); // Auto-play next song
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show();
                releasePlayer();
                return true;
            });

            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Cannot play song", Toast.LENGTH_SHORT).show();
            releasePlayer();
        }
    }
    private void togglePlayPause() {
        if (currentIndex == -1 && !songs.isEmpty()) {
            playSong(0);
            return;
        }

        if (mediaPlayer == null) {
            if (currentIndex != -1) prepareAndStart(songs.get(currentIndex));
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                stopSeekBarUpdate();
            } else {
                if (requestAudioFocus()) {
                    mediaPlayer.start();
                    isPlaying = true;
                    startSeekBarUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        if (playBtn == null) return;
        playBtn.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
    }
    private void startSeekBarUpdate() {
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    currentTime.setText(formatTime(currentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateSeekBarRunnable);
    }

    private void stopSeekBarUpdate() {
        if (updateSeekBarRunnable != null) {
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    private String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    // --- Audio Focus Logic ---
    private boolean requestAudioFocus() {
        if (audioManager == null) return true;
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener).build();
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
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
            focusChange -> {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if (mediaPlayer != null && !mediaPlayer.isPlaying() && shouldResumeOnFocusGain) {
                            mediaPlayer.start();
                            isPlaying = true;
                            updatePlayPauseIcon();
                        }

                        if (mediaPlayer != null) {
                            try {
                                mediaPlayer.setVolume(1.0f, 1.0f);
                            } catch (Exception ignored) {
                            }
                        }

                        shouldResumeOnFocusGain = false;
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS:
                        if (mediaPlayer != null) {
                            try {
                                mediaPlayer.pause();
                            } catch (Exception ignored) {
                            }
                            isPlaying = false;
                            updatePlayPauseIcon();
                        }
                        abandonAudioFocus();
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            try {
                                mediaPlayer.pause();
                            } catch (Exception ignored) {
                            }
                            isPlaying = false;
                            shouldResumeOnFocusGain = true;
                            updatePlayPauseIcon();
                        }
                        break;

                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            try {
                                mediaPlayer.setVolume(0.3f, 0.3f);
                            } catch (Exception ignored) {
                            }
                        }
                        break;
                }
            };

    // --- Album Art & Cleanup ---
    private void loadAlbumArt(Song song) {
        if (song == null || artView == null) return;
        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.getId());
                retriever.setDataSource(this, trackUri);
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null && !isFinishing()) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    if (bitmap != null) {
                        runOnUiThread(() -> artView.setImageBitmap(bitmap));
                        runOnUiThread(() -> Blurry.with(this)
                                .radius(20)
                                .sampling(4)
                                .from(bitmap) // or .capture(view)
                                .into(bgArtView));
                    };
                }
            } catch (Exception e) { e.printStackTrace(); }
            finally { try { retriever.release(); } catch (Exception ignored) {} }
        }).start();
    }
    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {
            }

            mediaPlayer.release();
            mediaPlayer = null;
        }

        isPlaying = false;
        shouldResumeOnFocusGain = false;

        abandonAudioFocus();
        updatePlayPauseIcon();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(this, "Permission needed to load songs", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private List<Song> fetchAllSongs() {
        List<Song> songList = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        String[] projection = {
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA
        };
        try (Cursor cursor = getContentResolver().query(uri, projection, selection, null, sortOrder)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                while (cursor.moveToNext()) {
                    songList.add(new Song(
                            cursor.getLong(idCol), cursor.getString(titleCol),
                            cursor.getString(artistCol), cursor.getString(albumCol),
                            cursor.getLong(durationCol), cursor.getString(dataCol)
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return songList;
    }
    protected void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);

        if (toolbar != null) {
            setSupportActionBar(toolbar);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("All Songs");
            }
        }
    }
}