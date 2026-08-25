package com.example.meusic.activities;

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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.meusic.R;
import com.example.meusic.adapters.AllSongsPagerAdapter;
import com.example.meusic.components.PlayerComponent;
import com.example.meusic.managers.MusicLibrary;
import com.example.meusic.managers.PlayerManager;
import com.example.meusic.managers.ThemeManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


public class AllSongsActivity extends AppCompatActivity implements ThemeManager.PrimaryColorListener{

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private Toolbar toolbar;
    private MotionLayout motionLayout;
    private PlayerComponent playerComponent;
    private TabLayoutMediator tabLayoutMediator;

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

        ThemeManager.getInstance(this).addPrimaryColorListener(this);
    }

    private void updateTabLayoutPrimaryColor(int color) {
        tabLayout.setSelectedTabIndicatorColor(color);
        tabLayout.setTabTextColors(Color.argb(180, 255, 255, 255), color);
        tabLayout.setBackgroundColor(Color.TRANSPARENT);
    }
    @Override
    public void onPrimaryColorChangeListener(int color) {
        updateTabLayoutPrimaryColor(color);
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
        ThemeManager.getInstance(this).removePrimaryColorListener(this);
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
        tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Songs"); break;
                case 1: tab.setText("Artists"); break;
                case 2: tab.setText("Albums"); break;
            }
        });
        tabLayoutMediator.attach();
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