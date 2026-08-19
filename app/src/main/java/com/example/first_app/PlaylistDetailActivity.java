package com.example.first_app;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private Toolbar toolbar;
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;
    private SongAdapter adapter;
    private List<Song> playlistSongs = new ArrayList<>();
    private int playlistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getIntExtra("PLAYLIST_ID", -1);
        String playlistName = getIntent().getStringExtra("PLAYLIST_NAME");

        setupToolbar(playlistName);
        setupPlayer();
        setupRecyclerView();
        loadPlaylistSongs();
    }

    private void setupToolbar(String name) {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(name != null ? name : "Playlist");
        }
    }

    private void setupPlayer() {
        PlayerManager.getInstance().init(this);
        motionLayout = findViewById(R.id.music_player_motionLayout);
        playerComponent = new PlayerComponent(motionLayout, PlayerManager.getInstance());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.playlist_recycler_view);
        emptyText = findViewById(R.id.empty_playlist_text);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadPlaylistSongs() {
        // 1. Get LiveData of Song IDs from the database
        LiveData<List<Long>> songIdsLiveData = PlaylistManager.getInstance(this).getSongIdsInPlaylist(playlistId);

        // 2. Observe the IDs
        songIdsLiveData.observe(this, songIds -> {
            if (songIds == null || songIds.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
                return;
            }

            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);

            // 3. Map IDs to full Song objects
            playlistSongs.clear();
            List<Song> allSongs = MusicLibrary.getInstance().getSongs();
            if (allSongs != null) {
                for (Long id : songIds) {
                    for (Song song : allSongs) {
                        if (song.getId() == id) {
                            playlistSongs.add(song);
                            break;
                        }
                    }
                }
            }

            // 4. Setup Adapter
            if (adapter == null) {
                adapter = new SongAdapter(playlistSongs, (song, position) -> {
                    PlayerManager.getInstance().playSong(song, playlistSongs);
                    motionLayout.transitionToEnd();
                });
                recyclerView.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }
        });
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