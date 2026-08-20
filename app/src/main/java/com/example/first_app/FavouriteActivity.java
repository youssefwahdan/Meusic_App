package com.example.first_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class FavouriteActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private TextView emptyText;
    private List<Song> favoriteSongsList = new ArrayList<>();
    private Toolbar toolbar;
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        setContentView(R.layout.activity_favourite);

        setupToolbar();

        // 1. Initialize PlayerManager globally
        PlayerManager.getInstance().init(this);


        // 2. Initialize the reusable Player Component
        motionLayout = findViewById(R.id.music_player_motionLayout);
        playerComponent = new PlayerComponent(motionLayout, PlayerManager.getInstance());

        recyclerView = findViewById(R.id.favourite_songs_recycler);
        emptyText = findViewById(R.id.empty_txt);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadFavoritesFromDatabase();
    }

    private void loadFavoritesFromDatabase() {
        // 1. Get the LiveData from the database
        LiveData<List<FavoriteEntity>> favoritesLiveData =
                FavoriteManager.getInstance(this).getAllFavorites();

        // 2. Observe it. Whenever the DB changes, this block runs automatically!
        favoritesLiveData.observe(this, favoriteEntities -> {

            if (favoriteEntities == null || favoriteEntities.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
                return;
            }

            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);

            // 3. Map the IDs from the database to full Song objects from MusicLibrary
            favoriteSongsList.clear();
            List<Song> allSongs = MusicLibrary.getInstance().getSongs();

            if (allSongs != null) {
                for (FavoriteEntity entity : favoriteEntities) {
                    for (Song song : allSongs) {
                        if (song.getId() == entity.songId) {
                            favoriteSongsList.add(song);
                            break;
                        }
                    }
                }
            }

            // 4. Setup Adapter
            adapter = new SongAdapter(favoriteSongsList, (song, position) -> {
                PlayerManager.getInstance().playSong(song, favoriteSongsList);
            });
            recyclerView.setAdapter(adapter);
        });
    }
    @Override
    public void onBackPressed() {
        if (motionLayout.getProgress() > 0.0) {
//            Toast.makeText(this, "player opened", Toast.LENGTH_SHORT).show();
            motionLayout.transitionToStart();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 4. Prevent memory leaks by detaching the component
        if (playerComponent != null) {
            playerComponent.detach();
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
    protected void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Favourites");
            }
        }
    }
}
