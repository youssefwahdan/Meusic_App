package com.example.first_app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private EditText searchEditText;
    private RecyclerView recyclerView;
    private SearchSongAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search); // Make sure you have this layout

        searchEditText = findViewById(R.id.search_songs_input);
        recyclerView = findViewById(R.id.search_songs_recycler);

        // Initialize adapter with an empty list
        adapter = new SearchSongAdapter(new ArrayList<>(), song -> {
            // Handle click: e.g., play the song or open player
            // playSong(song);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Listen to text changes
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // 1. Ask MusicLibrary to search
                List<Song> results = MusicLibrary.getInstance().searchSongs(s.toString());

                // 2. Update the adapter with the new results
                adapter.updateList(results);
            }
        });
    }
}