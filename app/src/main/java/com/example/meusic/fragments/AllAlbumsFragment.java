package com.example.meusic.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meusic.R;
import com.example.meusic.activities.DetailSongsActivity;
import com.example.meusic.adapters.AlbumAdapter;
import com.example.meusic.managers.MusicLibrary;
import com.example.meusic.models.Album;
import com.example.meusic.models.Song;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AllAlbumsFragment extends Fragment implements MusicLibrary.OnSongsSortedListener {

    private RecyclerView recyclerView;
    private AlbumAdapter adapter;

    public AllAlbumsFragment() {
        super(R.layout.fragment_all_albums);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MusicLibrary.getInstance().addSortListener(this);
        recyclerView = view.findViewById(R.id.albums_recycler_view);

        // 1. Setup 3-column Grid
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        loadAlbums();
    }

    private void loadAlbums() {
        List<Song> songs = MusicLibrary.getInstance().getSongs();

        if (songs == null || songs.isEmpty()) {
            // Trigger load if empty
            MusicLibrary.getInstance().loadSongs(requireContext(), new MusicLibrary.OnSongsLoadedListener() {
                @Override public void onSongsLoaded(List<Song> loadedSongs) { setupGrid(loadedSongs); }
                @Override public void onPermissionRequired(String permission) {}
                @Override public void onError(String message) {
                    Toast.makeText(requireContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            setupGrid(songs);
        }
    }

    private void setupGrid(List<Song> songs) {
        // 2. Group songs by Album using LinkedHashMap to preserve order
        LinkedHashMap<String, List<Song>> groupedByAlbum = new LinkedHashMap<>();

        for (Song song : songs) {
            String albumName = song.getAlbum() != null ? song.getAlbum() : "Unknown Album";
            if (!groupedByAlbum.containsKey(albumName)) {
                groupedByAlbum.put(albumName, new ArrayList<>());
            }
            groupedByAlbum.get(albumName).add(song);
        }

        // 3. Convert Map to List of Album objects
        List<Album> albumList = new ArrayList<>();
        for (Map.Entry<String, List<Song>> entry : groupedByAlbum.entrySet()) {
            String albumName = entry.getKey();
            List<Song> albumSongs = entry.getValue();
            String artistName = albumSongs.get(0).getArtist(); // Get artist from first song

            albumList.add(new Album(albumName, artistName, albumSongs));
        }

        // 4. Set Adapter
        adapter = new AlbumAdapter(albumList, album -> {
            Intent intent = new Intent(requireContext(), DetailSongsActivity.class);
            intent.putExtra(DetailSongsActivity.EXTRA_TYPE, "ALBUM");
            intent.putExtra(DetailSongsActivity.EXTRA_NAME, album.getName());
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onSongsSorted() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            loadAlbums();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent memory leaks
        MusicLibrary.getInstance().removeSortListener(this);
    }
}