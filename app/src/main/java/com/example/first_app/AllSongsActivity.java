package com.example.first_app;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


public class AllSongsActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;

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