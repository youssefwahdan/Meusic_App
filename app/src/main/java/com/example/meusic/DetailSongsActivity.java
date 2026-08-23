package com.example.meusic;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DetailSongsActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type"; // "ARTIST" or "ALBUM"
    public static final String EXTRA_NAME = "name";

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private Toolbar toolbar;
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;
    private List<Song> filteredSongs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Allow the app to draw behind the system bars
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2. Set the bars to transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        // 3. Make the status bar icons WHITE (since your app has a dark background)
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
            insetsController.setAppearanceLightNavigationBars(false);
        }
        setContentView(R.layout.activity_detail_songs);

        // Get data from Intent and filter
        String type = getIntent().getStringExtra(EXTRA_TYPE);
        String name = getIntent().getStringExtra(EXTRA_NAME);

        setupToolbar(name);
        setupPlayer();
        setupRecyclerView();

        filterAndLoadSongs(type, name);
    }

    private void setupToolbar(String titleName) {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle(titleName);
            }
        }
    }

    private void setupPlayer() {
        PlayerManager.getInstance().init(this);
        motionLayout = findViewById(R.id.music_player_motionLayout);
        playerComponent = new PlayerComponent(motionLayout, PlayerManager.getInstance());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.detail_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void filterAndLoadSongs(String type, String name) {
        List<Song> allSongs = MusicLibrary.getInstance().getSongs();
        if (allSongs == null) return;



        filteredSongs.clear();
        for (Song song : allSongs) {
            if (type != null && name != null) {
                if (type.equals("ARTIST") && song.getArtist() != null && song.getArtist().equalsIgnoreCase(name)) {
                    filteredSongs.add(song);
                } else if (type.equals("ALBUM") && song.getAlbum() != null && song.getAlbum().equalsIgnoreCase(name)) {
                    filteredSongs.add(song);
                }
            }
        }

        if ("ALBUM".equals(type)) {
            filteredSongs.sort((s1, s2) -> {
                int track1 = s1.getTrackNumber();
                int track2 = s2.getTrackNumber();

                // Handle songs with missing/unknown track numbers (usually 0)
                if (track1 == 0 && track2 == 0) return 0;
                if (track1 == 0) return 1;  // Put missing tracks at the end
                if (track2 == 0) return -1; // Put missing tracks at the end

                return Integer.compare(track1, track2);
            });
        }

        // Setup Adapter
        adapter = new SongAdapter(filteredSongs, (song, position) -> {
            // Play the song using the filtered list as the queue
            PlayerManager.getInstance().playSong(song, filteredSongs);
//            motionLayout.transitionToEnd(); // Expand player
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        if (motionLayout.getProgress() > 0.0) {
            motionLayout.transitionToStart();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playerComponent != null) playerComponent.detach();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}