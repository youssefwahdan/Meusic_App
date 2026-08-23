package com.example.meusic;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 1001;
    protected Toolbar toolbar;
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;
    private MenuAdapter adapter;
    private List<MenuItem> menuList;
    private AppStateManager stateManager;

    private final MusicLibrary.OnSongsLoadedListener songsListener = new MusicLibrary.OnSongsLoadedListener() {
        @Override
        public void onSongsLoaded(List<Song> songs) {
            adapter.updateSongCount(songs.size());
            if (songs != null && !songs.isEmpty()) {
                stateManager.restoreState(PlayerManager.getInstance(), songs);
            }
            if (PlayerManager.getInstance().getQueue() == null || PlayerManager.getInstance().getQueue().isEmpty()) {
                PlayerManager.getInstance().setQueue(songs);
            }

        }

        @Override
        public void onPermissionRequired(String permission) {
            requestPermissions(new String[]{permission}, AUDIO_PERMISSION_REQUEST_CODE);
        }

        @Override
        public void onError(String message) {
            Toast.makeText(MainActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
        }
    };

    private final MusicLibrary.OnFavouritesLoadedListener favouritesListener = new MusicLibrary.OnFavouritesLoadedListener() {
        @Override
        public void onFavouritesCountLoaded(int count) {
            adapter.updateFavouritesCount(count);
        }
    };

    private final MusicLibrary.OnPlaylistsLoadedListener playlistslistener = new MusicLibrary.OnPlaylistsLoadedListener() {
        @Override
        public void onPlaylistsCountLoaded(int count) {
            adapter.updatePlaylistsCount(count);
        }
    };


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
        setContentView(R.layout.activity_main);

        setupToolbar();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        menuList = new ArrayList<>();

        // Initialize with a temporary count (e.g., "0 songs")
        menuList.add(new MenuItem(R.drawable.ic_music, "All songs", "0 songs"));
        menuList.add(new MenuItem(R.drawable.ic_playlist, "Playlists", "1"));
        menuList.add(new MenuItem(R.drawable.ic_favorite, "Favourite", "0 songs"));
//        menuList.add(new MenuItem(R.drawable.ic_recent, "Recently played", "644 songs"));
        menuList.add(new MenuItem(R.drawable.ic_settings, "Settings", ""));
        adapter = new MenuAdapter(menuList, new MenuAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MenuItem item, int position) {
                // 2. THIS IS WHERE YOU HANDLE THE CLICK!
                handleMenuClick(item, position);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Initialize PlayerManager and PlayerComponent
        PlayerManager.getInstance().init(this);
        motionLayout = findViewById(R.id.music_player_motionLayout);
        playerComponent = new PlayerComponent(motionLayout, PlayerManager.getInstance());

        stateManager = new AppStateManager(this);

        // Load songs (handles permissions + fetching + caching automatically)
        MusicLibrary.getInstance().loadSongs(this, songsListener);
        MusicLibrary.getInstance().getFavouritesCount(this, this, favouritesListener);
        MusicLibrary.getInstance().getPlaylistsCount(this, this, playlistslistener);

    }

    private void handleMenuClick(MenuItem item, int position) {
        // You can check which item was clicked by its position or its title

        switch (position) {
            case 0:
                // "All songs" clicked
//                Toast.makeText(this, "Opening All Songs...", Toast.LENGTH_SHORT).show();
                 Intent allSongIntent = new Intent(this, AllSongsActivity.class);
                 startActivity(allSongIntent);
                break;

            case 1:
                // "Playlists" clicked
//                Toast.makeText(this, "Opening Playlists...", Toast.LENGTH_SHORT).show();
                Intent playlistIntent = new Intent(this, PlaylistActivity.class);
                startActivity(playlistIntent);
                break;

            case 2:
                // "Favourite" clicked
//                Toast.makeText(this, "Opening Favourites...", Toast.LENGTH_SHORT).show();
                Intent favouriteIntent = new Intent(this, FavouriteActivity.class);
                startActivity(favouriteIntent);
                break;

            case 5:
                // "Settings" clicked
                Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show();
                // Intent intent = new Intent(this, SettingsActivity.class);
                // startActivity(intent);
                break;

            default:
                // Fallback
                Toast.makeText(this, "Clicked: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                break;
        }}
    protected void setupToolbar() {
        // Include the toolbar layout from the activity's XML
        // Assuming the activity layout has <include layout="@layout/toolbar"/>
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            // Optional: Set default title or navigation icon
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Library");
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, tell MusicLibrary to scan again
                MusicLibrary.getInstance().loadSongs(this, songsListener);
            } else {
                Toast.makeText(this, "Permission needed to load songs", Toast.LENGTH_SHORT).show();
            }
        }
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
        savePlayerState();
    }
    private void savePlayerState() {
        PlayerManager manager = PlayerManager.getInstance();
        stateManager.saveState(
                manager.getPlaybackMode(),
                manager.getCurrentIndex(), // Add this getter to PlayerManager
                manager.getQueue(),         // Add this getter to PlayerManager
                MusicLibrary.getInstance().getCurrentSortIndex()
        );
    }
}