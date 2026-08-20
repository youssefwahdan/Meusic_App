package com.example.first_app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.ItemTouchHelper;
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

                // DRAG AND DROP LOGIC
                setupDragAndDrop(recyclerView);
            } else {
                adapter.notifyDataSetChanged();
            }
        });
    }
    private void setupDragAndDrop(RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        int fromPosition = viewHolder.getAdapterPosition();
                        int toPosition = target.getAdapterPosition();

                        // Tell the adapter to shift the items and animate
                        adapter.onItemMove(fromPosition, toPosition);
                        return true; // Return true to indicate the move was successful
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        // We don't want swipe-to-delete, so leave this empty
                    }

                    @Override
                    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerView, viewHolder);

                        // This is called when the user drops the item.
                        // Save the new order to the database!
                        List<Long> newOrder = new ArrayList<>();
                        for (Song song : adapter.getCurrentSongs()) {
                            newOrder.add(song.getId());
                        }

                        // Save to database in the background
                        PlaylistManager.getInstance(PlaylistDetailActivity.this)
                                .reorderPlaylist(playlistId, newOrder);
                    }
                };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
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