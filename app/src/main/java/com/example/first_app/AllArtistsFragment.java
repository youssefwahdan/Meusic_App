package com.example.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AllArtistsFragment extends Fragment implements MusicLibrary.OnSongsSortedListener {

    private RecyclerView recyclerView;
    private ArtistAdapter adapter;

    public AllArtistsFragment() {
        super(R.layout.fragment_all_artists);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MusicLibrary.getInstance().addSortListener(this);

        recyclerView = view.findViewById(R.id.artists_recycler_view);

        // 1. Setup normal vertical list (LinearLayoutManager)
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadArtists();
    }

    private void loadArtists() {
        List<Song> songs = MusicLibrary.getInstance().getSongs();

        if (songs == null || songs.isEmpty()) {
            MusicLibrary.getInstance().loadSongs(requireContext(), new MusicLibrary.OnSongsLoadedListener() {
                @Override public void onSongsLoaded(List<Song> loadedSongs) { setupList(loadedSongs); }
                @Override public void onPermissionRequired(String permission) {}
                @Override public void onError(String message) {
                    Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            setupList(songs);
        }
    }

    private void setupList(List<Song> songs) {
        // 2. Group songs by Artist using LinkedHashMap to preserve order
        LinkedHashMap<String, List<Song>> groupedByArtist = new LinkedHashMap<>();

        for (Song song : songs) {
            String artistName = song.getArtist() != null ? song.getArtist() : "Unknown Artist";
            if (!groupedByArtist.containsKey(artistName)) {
                groupedByArtist.put(artistName, new ArrayList<>());
            }
            groupedByArtist.get(artistName).add(song);
        }

        // 3. Convert Map to List of Artist objects
        List<Artist> artistList = new ArrayList<>();
        for (Map.Entry<String, List<Song>> entry : groupedByArtist.entrySet()) {
            artistList.add(new Artist(entry.getKey(), entry.getValue()));
        }

        // 4. Set Adapter
        adapter = new ArtistAdapter(artistList, artist -> {
            Intent intent = new Intent(requireContext(), DetailSongsActivity.class);
            intent.putExtra(DetailSongsActivity.EXTRA_TYPE, "ARTIST");
            intent.putExtra(DetailSongsActivity.EXTRA_NAME, artist.getName());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }
    @Override
    public void onSongsSorted() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            loadArtists();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent memory leaks
        MusicLibrary.getInstance().removeSortListener(this);
    }
}