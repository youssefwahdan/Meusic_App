package com.example.first_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private TextView emptyText;
    private View createPlaylistBtn;
    private PlaylistAdapter adapter;
    private List<PlaylistEntity> playlistList = new ArrayList<>();
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        setupToolbar();

        // 1. Initialize PlayerManager globally
        PlayerManager.getInstance().init(this);


        // 2. Initialize the reusable Player Component
        motionLayout = findViewById(R.id.music_player_motionLayout);
        playerComponent = new PlayerComponent(motionLayout, PlayerManager.getInstance());

        recyclerView = findViewById(R.id.playlists_recycler_view);
        emptyText = findViewById(R.id.empty_playlists_text);
        createPlaylistBtn = findViewById(R.id.create_playlist);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        createPlaylistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreatePlaylistDialog();
            }
        });

        // Observe the database
        PlaylistManager.getInstance(this).getAllPlaylists().observe(this, playlists -> {
            if (playlists == null || playlists.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyText.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyText.setVisibility(View.GONE);

                playlistList.clear();
                playlistList.addAll(playlists);

                if (adapter == null) {
                    adapter = new PlaylistAdapter(playlistList, playlist -> {
                        // Open the playlist detail activity
                        Intent intent = new Intent(this, PlaylistDetailActivity.class);
                        intent.putExtra("PLAYLIST_ID", playlist.playlistId);
                        intent.putExtra("PLAYLIST_NAME", playlist.name);
                        startActivity(intent);
                    }, this,this);
                    recyclerView.setAdapter(adapter);
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }
    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_playlist,null);

        EditText editTxt = dialogView.findViewById(R.id.create_playlist_edit_txt);
        Button createBtn = dialogView.findViewById(R.id.create_playlist_create_action);
        Button cancelBtn = dialogView.findViewById(R.id.create_playlist_cancel_action);
        final String[] playlistName = new String[1];

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setView(dialogView);

        AlertDialog dialog = builder.create();

        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().getAttributes().y = 50;
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_dialog_bg);
        dialog.show();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;

        // 2. Calculate the dialog width (e.g., 0.90 = 90% of the screen)
        int dialogWidth = (int) (screenWidth * 0.90);

        // 3. Apply the width and set height to wrap content
        dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        editTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                playlistName[0] = s.toString();
            }
        });
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!playlistName[0].isEmpty()) {
                    PlaylistManager.getInstance(PlaylistActivity.this).createPlaylist(playlistName[0]);
                    dialog.dismiss();
                }
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    dialog.dismiss();
            }
        });
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
    protected void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Playlists");
            }
        }
    }
}
