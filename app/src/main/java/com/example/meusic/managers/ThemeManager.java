package com.example.meusic.managers;

import android.content.Context;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;


public class ThemeManager {
    private static  ThemeManager instance;
    private static final String PREFS_NAME = "app_theme_prefs";
    private static final String KEY_PRIMARY_COLOR = "primary_color";

    private static final int DEFAULT_COLOR = Color.parseColor("#FFFF5C5C");

    private int currentColor;
    private Context appContext;

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(int currentColor) {
        this.currentColor = currentColor;
    }

    public interface PrimaryColorListener {
        void onPrimaryColorChangeListener(int color);
    }
    private List<PrimaryColorListener> primaryColorListeners = new ArrayList<>();

    public void addPrimaryColorListener(PrimaryColorListener listener) {
        if (!primaryColorListeners.contains(listener)) {
            primaryColorListeners.add(listener);
            listener.onPrimaryColorChangeListener(currentColor);
        };
    }

    public void removePrimaryColorListener(PrimaryColorListener listener) {
        primaryColorListeners.remove(listener);
    }
    public ThemeManager(Context context){
        this.appContext = context;
        setCurrentColor(getPrimaryColor());
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context);
        }
        return instance;
    }
    public void savePrimaryColor(int color) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_PRIMARY_COLOR, color)
                .apply();
        setCurrentColor(color);
        for (PrimaryColorListener listener : instance.primaryColorListeners) {
            listener.onPrimaryColorChangeListener(color);
        }
    }

    public int getPrimaryColor() {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_PRIMARY_COLOR, DEFAULT_COLOR);
    }

}