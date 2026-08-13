package com.example.first_app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class AllSongsActivity extends AppCompatActivity {

//    private static final int AUDIO_PERMISSION_REQUEST_CODE = 1001;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;
//    private List<Song> songsList;
//    private SongAdapter adapter;
//    private int currentSortIndex = 0;

//
//    private final MusicLibrary.OnSongsLoadedListener songsListener = new MusicLibrary.OnSongsLoadedListener() {
//        @Override
//        public void onSongsLoaded(List<Song> songs) {
//            songsList = songs;
//            loadSongs();
//        }
//
//        @Override
//        public void onPermissionRequired(String permission) {
//            requestPermissions(new String[]{permission}, AUDIO_PERMISSION_REQUEST_CODE);
//        }
//
//        @Override
//        public void onError(String message) {
//            Toast.makeText(AllSongsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
//        }
//    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_songs);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        setupTabs();
        setupToolbar();

        // 1. Initialize PlayerManager globally
        PlayerManager.getInstance().init(this);


        // 2. Initialize the reusable Player Component
        motionLayout = findViewById(R.id.music_player_motionLayout);
        playerComponent = new PlayerComponent(motionLayout, PlayerManager.getInstance());

//        MusicLibrary.getInstance().loadSongs(this, songsListener);

//        PlayerManager.getInstance().setQueue(MusicLibrary.getInstance().getSongs());
    }

//    private void loadSongs() {
//        new Thread(() -> {
//            if (!isFinishing()) {
//                runOnUiThread(() -> {
//                    RecyclerView recyclerView = findViewById(R.id.songs_recycler_view);
//
//                    adapter = new SongAdapter(songsList, (song, position) -> {
//                        PlayerManager.getInstance().playSong(song, songsList);
//                        // The PlayerComponent will automatically detect the change and expand the UI!
//                    });
//
//                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
//                    recyclerView.setAdapter(adapter);
//                });
//            }
//        }).start();
//    }

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

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                loadSongs();
//            } else {
//                Toast.makeText(this, "Permission needed to load songs", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_search) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_sort) {
            MusicLibrary.getInstance().showSortDialog(this);
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_all_songs, menu);
        for (int i = 0; i < menu.size(); i++) {
            Drawable icon = menu.getItem(i).getIcon();
            if (icon != null) {
                icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
            }
        }
        return true;
    }
//    private void showSortDialog() {
//        String[] sortOptions = {
//                "Title (A - Z)",
//                "Title (Z - A)",
//                "Artist (A - Z)",
//                "Artist (Z - A)",
//                "Album (A - Z)",
//                "Album (Z - A)",
//                "Duration (Shortest first)",
//                "Duration (Longest first)",
//                "Date Added ASC",
//                "Date Added DESC"
//        };
//
//        // 1. Create the Builder
//        AlertDialog.Builder builder = new AlertDialog.Builder(this)
//                .setTitle("Sort Songs By")
//                .setSingleChoiceItems(sortOptions, currentSortIndex, (dialog, which) -> {
//                    currentSortIndex = which;
//                    MusicLibrary.getInstance().sortSongs(which);
//                    dialog.dismiss();
//                });
////                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//
//        // 2. Create the Dialog object
//        AlertDialog dialog = builder.create();
//        // 3. Apply the rounded background
//        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_bg);
//        // 4. Show the dialog
//        dialog.show();
//        // 1. Get the screen width
//        int screenWidth = getResources().getDisplayMetrics().widthPixels;
//
//        // 2. Calculate the dialog width (e.g., 0.90 = 90% of the screen)
//        int dialogWidth = (int) (screenWidth * 0.80);
//
//        // 3. Apply the width and set height to wrap content
//        dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
//    }
//    private void sortSongs(int option) {
//        switch (option) {
//            case 0: // Title (A - Z)
//                songsList.sort((s1, s2) -> s1.getTitle().compareToIgnoreCase(s2.getTitle()));
//                break;
//            case 1: // Title (Z - A)
//                songsList.sort((s1, s2) -> s2.getTitle().compareToIgnoreCase(s1.getTitle()));
//                break;
//
//            case 2: // Artist (A - Z)
//                songsList.sort((s1, s2) -> s1.getArtist().compareToIgnoreCase(s2.getArtist()));
//                break;
//            case 3: // Artist (Z - A)
//                songsList.sort((s1, s2) -> s2.getArtist().compareToIgnoreCase(s1.getArtist()));
//                break;
//            case 4: // Album (A - Z)
//                songsList.sort((s1, s2) -> s1.getAlbum().compareToIgnoreCase(s2.getAlbum()));
//                break;
//            case 5: // Album (Z - A)
//                songsList.sort((s1, s2) -> s2.getAlbum().compareToIgnoreCase(s1.getAlbum()));
//                break;
//
//            case 6: // Duration (Short to Long)
//                songsList.sort((s1, s2) -> Long.compare(s1.getDuration(), s2.getDuration()));
//                break;
//
//            case 7: // Duration (Long to Short)
//                songsList.sort((s1, s2) -> Long.compare(s2.getDuration(), s1.getDuration()));
//                break;
//
//            case 8:
//                songsList.sort((s1, s2) -> s1.getDate().compareToIgnoreCase(s2.getDate()));
//                break;
//            case 9:
//                songsList.sort((s1, s2) -> s2.getDate().compareToIgnoreCase(s1.getDate()));
//                break;
//        }
//
//        // Refresh the RecyclerView to show the new order
//        if (adapter != null) {
//            adapter.notifyDataSetChanged();
//        }
//    }

    private void setupTabs() {
        // Attach the adapter
        viewPager.setAdapter(new AllSongsPagerAdapter(this));

        // Link TabLayout with ViewPager2 so swiping updates tabs and clicking tabs swipes
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Songs"); break;
                case 1: tab.setText("Artists"); break;
                case 2: tab.setText("Albums"); break;
            }
        }).attach();
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