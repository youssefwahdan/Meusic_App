package com.example.first_app;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AllArtistsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AllArtistsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AllArtistsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AllArtistsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AllArtistsFragment newInstance(String param1, String param2) {
        AllArtistsFragment fragment = new AllArtistsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all_artists, container, false);
    }
    public static LinkedHashMap<String, List<Song>> groupSongsByArtist(List<Song> songs) {
        LinkedHashMap<String, List<Song>> grouped = new LinkedHashMap<>();

        for (Song song : songs) {
            String artist = song.getArtist() != null ? song.getArtist() : "Unknown Artist";

            // If we haven't seen this artist yet, create a new list for them
            if (!grouped.containsKey(artist)) {
                grouped.put(artist, new ArrayList<>());
            }
            // Add the song to that artist's list
            grouped.get(artist).add(song);
        }
        return grouped;
    }

}