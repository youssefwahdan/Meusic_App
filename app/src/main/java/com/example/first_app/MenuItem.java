package com.example.first_app;

import android.view.View;

public class MenuItem {
    private int iconResId;
    private String title;
    private String subtitle;

    public MenuItem(int iconResId, String title, String subtitle) {
        this.iconResId = iconResId;
        this.title = title;
        this.subtitle = subtitle;
    }

    public int getIconResId() { return iconResId; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
}