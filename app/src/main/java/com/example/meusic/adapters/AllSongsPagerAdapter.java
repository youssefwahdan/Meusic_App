package com.example.meusic.adapters;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.meusic.fragments.AllAlbumsFragment;
import com.example.meusic.fragments.AllArtistsFragment;
import com.example.meusic.fragments.AllSongsFragment;

public class AllSongsPagerAdapter extends FragmentStateAdapter {

    public AllSongsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new AllSongsFragment();
            case 1: return new AllArtistsFragment();
            case 2: return new AllAlbumsFragment();
            default: return new AllSongsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Total number of tabs
    }
}