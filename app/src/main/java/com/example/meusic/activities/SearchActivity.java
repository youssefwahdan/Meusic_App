package com.example.meusic.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meusic.R;
import com.example.meusic.adapters.SearchAdapter;
import com.example.meusic.components.PlayerComponent;
import com.example.meusic.managers.MusicLibrary;
import com.example.meusic.managers.PlayerManager;
import com.example.meusic.models.SearchItem;
import com.example.meusic.models.Song;

import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView recyclerView;
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;
    private ImageView backBtn;

    private final MusicLibrary.OnSongsLoadedListener songsListener = new MusicLibrary.OnSongsLoadedListener() {
        @Override
        public void onSongsLoaded(List<Song> songs) {
        }

        @Override
        public void onPermissionRequired(String permission) {
        }

        @Override
        public void onError(String message) {
            Toast.makeText(SearchActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_search); // Make sure you have this layout

        searchEditText = findViewById(R.id.search_songs_input);
        recyclerView = findViewById(R.id.search_songs_recycler);
        backBtn = findViewById(R.id.back_btn);
        View included = (View) findViewById(R.id.included);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // Check if the user started dragging the list
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    InputMethodManager imm = (InputMethodManager) recyclerView.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);

                    // Hide keyboard using the RecyclerView's window token
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(recyclerView.getWindowToken(), 0);
                    }

                    // Optional: Clear focus from any child EditText
                    recyclerView.clearFocus();
                }
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        // 1. Initialize PlayerManager globally
        PlayerManager.getInstance().init(this);


        // 2. Initialize the reusable Player Component
        motionLayout = findViewById(R.id.music_player_motionLayout);
        playerComponent = new PlayerComponent(motionLayout, PlayerManager.getInstance());

        MusicLibrary.getInstance().loadSongs(this, songsListener);

        // Initialize adapter with an empty lis

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        recyclerView.setAdapter(adapter);

        // Listen to text changes
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                performSearch(s.toString());
            }
        });

    }

    private void performSearch(String query) {
        // 1. Get results directly from MusicLibrary
        List<SearchItem> results = MusicLibrary.getInstance().search(query);

        // 2. Setup Adapter with Click Listener
        SearchAdapter searchAdapter = new SearchAdapter(results, item -> {

            if (item.type == SearchItem.TYPE_SONG) {
                // Play the song
                Song song = (Song) item.data;
                PlayerManager.getInstance().playSong(song, MusicLibrary.getInstance().getSongs());

            } else if (item.type == SearchItem.TYPE_ARTIST) {
                // Open Artist Details Activity
                String artistName = (String) item.data;
                Intent intent = new Intent(SearchActivity.this, DetailSongsActivity.class);
                intent.putExtra(DetailSongsActivity.EXTRA_TYPE, "ARTIST");
                intent.putExtra(DetailSongsActivity.EXTRA_NAME, artistName);
                startActivity(intent);

            } else if (item.type == SearchItem.TYPE_ALBUM) {
                // Open Album Details Activity
                String albumName = (String) item.data;
                Intent intent = new Intent(SearchActivity.this, DetailSongsActivity.class);
                intent.putExtra(DetailSongsActivity.EXTRA_TYPE, "ALBUM");
                intent.putExtra(DetailSongsActivity.EXTRA_NAME, albumName);
                startActivity(intent);
            }
        });

        // 3. Update RecyclerView
        recyclerView.setAdapter(searchAdapter);
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
}