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
    protected Toolbar toolbar;
    MotionLayout motionLayout;
    private MenuAdapter adapter;
    private List<MenuItem> menuList;

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


//         1. Check and request permissions
        checkAndRequestPermissions();
    }

    // --- PERMISSION HANDLING ---
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean isGranted = result.get(
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ? Manifest.permission.READ_MEDIA_AUDIO
                                : Manifest.permission.READ_EXTERNAL_STORAGE
                );

                if (Boolean.TRUE.equals(isGranted)) {
                    fetchLocalSongs(); // Permission granted, fetch songs!
                } else {
                    Toast.makeText(this, "Permission denied. Cannot load songs.", Toast.LENGTH_SHORT).show();
                }
            });

    private void checkAndRequestPermissions() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            fetchLocalSongs(); // Already granted
        } else {
            requestPermissionLauncher.launch(new String[]{permission}); // Ask user
        }
    }

    // --- FETCHING SONGS ---
    private void fetchLocalSongs() {
        // Run in background so the UI doesn't freeze
        executor.execute(() -> {
            int songCount = getActualSongCount();

            // Switch back to Main Thread to update UI
            runOnUiThread(() -> {
                adapter.updateSongCount(songCount);
            });
        });
    }

    private int getActualSongCount() {
        int count = 0;
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // Only select actual music (filter out ringtones, notifications, etc.)
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        // We only need to query one column to get the count
        String[] projection = { MediaStore.Audio.Media._ID };

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, null, null)) {
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return count;
    }
    private void handleMenuClick(MenuItem item, int position) {
        // You can check which item was clicked by its position or its title

        switch (position) {
            case 0:
                // "All songs" clicked
                Toast.makeText(this, "Opening All Songs...", Toast.LENGTH_SHORT).show();
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