package com.example.first_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.content.ContextCompat;
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

    private List<Song> songsList;
    private final MusicLibrary.OnSongsLoadedListener songsListener = new MusicLibrary.OnSongsLoadedListener() {
        @Override
        public void onSongsLoaded(List<Song> songs) {
            songsList = songs;
            adapter.updateSongCount(songs.size());
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

    // Background thread executor to prevent UI freezing
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        menuList = new ArrayList<>();

        // Initialize with a temporary count (e.g., "0 songs")
        menuList.add(new MenuItem(R.drawable.ic_music, "All songs", "0 songs"));
        menuList.add(new MenuItem(R.drawable.ic_playlist, "Playlists", "1"));
        menuList.add(new MenuItem(R.drawable.ic_favorite, "Favourite", "25 songs"));
        menuList.add(new MenuItem(R.drawable.ic_recent, "Recently played", "644 songs"));
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

        // Load songs (handles permissions + fetching + caching automatically)
        MusicLibrary.getInstance().loadSongs(this, songsListener);
        PlayerManager.getInstance().setQueue(MusicLibrary.getInstance().getSongs());
    }

    private void handleMenuClick(MenuItem item, int position) {
        // You can check which item was clicked by its position or its title

        switch (position) {
            case 0:
                // "All songs" clicked
//                Toast.makeText(this, "Opening All Songs...", Toast.LENGTH_SHORT).show();
                 Intent intent = new Intent(this, AllSongsActivity.class);
                 startActivity(intent);
                break;

            case 1:
                // "Playlists" clicked
                Toast.makeText(this, "Opening Playlists...", Toast.LENGTH_SHORT).show();
                break;

            case 2:
                // "Favourite" clicked
                Toast.makeText(this, "Opening Favourites...", Toast.LENGTH_SHORT).show();
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
}