package com.example.first_app;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.MenuItem;
import android.widget.ImageView;
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
    private TextView titleText, artistText;
    private ImageView artView, bgArtView, playBtn;
    private Toolbar toolbar;

    private final List<Song> songs = new ArrayList<>();
    private SongAdapter adapter;

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

        motionLayout = findViewById(R.id.music_player_motionLayout);
        titleText = findViewById(R.id.song_title);
        artistText = findViewById(R.id.song_artist);
        artView = findViewById(R.id.album_art);
        bgArtView = findViewById(R.id.background_album_art);
        playBtn = findViewById(R.id.play_btn);

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        RecyclerView recyclerView = findViewById(R.id.songs_recycler_view);

        adapter = new SongAdapter(songs, (song, position) -> playSong(song));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        playBtn.setOnClickListener(v -> togglePlayPause());

        checkPermissionAndLoadSongs();
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
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    private void playSong(Song song) {
        if (song == null) return;

        motionLayout.transitionToEnd();

        if (currentSong != null && currentSong.getId() == song.getId()) {
            if (mediaPlayer == null) {
                prepareAndStart(song);
            } else {
                togglePlayPause();
            }
            return;
        }

        currentSong = song;

        titleText.setText(song.getTitle());
        artistText.setText(song.getArtist());

        artView.setImageResource(R.drawable.ic_music);
        loadAlbumArt(song);

        prepareAndStart(song);
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
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    song.getId()
            );

            mediaPlayer.setDataSource(this, trackUri);

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                updatePlayPauseIcon();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                updatePlayPauseIcon();

                // Optional: play next song here
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show();
                releasePlayer();
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Cannot play song: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            releasePlayer();
        }
    }

    private void togglePlayPause() {
        if (currentSong == null) {
            if (!songs.isEmpty()) {
                playSong(songs.get(0));
            } else {
                Toast.makeText(this, "No songs loaded", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (mediaPlayer == null) {
            prepareAndStart(currentSong);
            return;
        }

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                isPlaying = false;
                shouldResumeOnFocusGain = false;
            } else {
                if (requestAudioFocus()) {
                    mediaPlayer.start();
                    isPlaying = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        if (playBtn == null) return;

        if (isPlaying) {
            playBtn.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playBtn.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private boolean requestAudioFocus() {
        if (audioManager == null) return true;

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build();

            return audioManager.requestAudioFocus(audioFocusRequest)
                    == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            int result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
            );

            return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
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

    private void loadAlbumArt(Song song) {
        if (song == null || artView == null || bgArtView == null) return;

        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();

            try {
                Uri trackUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        song.getId()
                );

                retriever.setDataSource(this, trackUri);

                byte[] art = retriever.getEmbeddedPicture();

                if (art != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);

                    if (bitmap != null && !isFinishing()) {
                        runOnUiThread(() -> artView.setImageBitmap(bitmap));
                        runOnUiThread(() -> Blurry.with(this)
                                .radius(50)
                                .sampling(4)
                                .from(bitmap) // or .capture(view)
                                .into(bgArtView)
                        );
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
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
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };

        try (Cursor cursor = getContentResolver().query(
                uri,
                projection,
                selection,
                null,
                sortOrder
        )) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String title = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    String album = cursor.getString(albumCol);
                    long duration = cursor.getLong(durationCol);
                    String data = cursor.getString(dataCol);

                    songList.add(new Song(id, title, artist, album, duration, data));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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