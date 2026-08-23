package com.example.meusic;

import android.widget.ImageView;
import android.widget.TextView;

public class SongOptionItem {
    private int iconResId;
    private String title;

    SongOptionItem(int iconResId, String title) {
        this.iconResId = iconResId;
        this.title = title;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }
}
