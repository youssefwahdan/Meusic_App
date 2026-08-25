package com.example.meusic.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.meusic.R;
import com.example.meusic.managers.ThemeManager;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;


public class SettingsActivity extends AppCompatActivity {

    private View themeColorOption;
    private View themeDivider;

    private Toolbar toolbar;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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

        setContentView(R.layout.activity_settings);

        setupToolbar();

        themeDivider = findViewById(R.id.theme_title_divider);
        themeColorOption = findViewById(R.id.theme_color_option);

        themeColorOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog();
            }
        });
        applyThemeColor();
    }

    private void showColorPickerDialog() {
        // 1. Get the currently saved color (defaults to Purple if none is saved)
        int currentColor = ThemeManager.getInstance(this).getPrimaryColor();

        // 2. Build and show the dialog
        ColorPickerDialog.Builder builder = ColorPickerDialog.newBuilder()
                .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                .setAllowPresets(true) // Shows a row of preset colors at the bottom
                .setAllowCustom(true)  // Allows the user to use the color wheel
                .setColor(currentColor)
                .setDialogId(1001); // ID to identify this specific dialog
        ColorPickerDialog dialog = builder.create();


        dialog.setColorPickerDialogListener(new ColorPickerDialogListener() {
                    @Override
                    public void onColorSelected(int dialogId, int color) {
                        if (dialogId == 1001) {
                            ThemeManager.getInstance(SettingsActivity.this).savePrimaryColor(color);
                            applyThemeColor();
                        }
                    }

                    @Override
                    public void onDialogDismissed(int dialogId) {
                        // Do nothing when closed without selecting
                    }
                });

        dialog.show(getSupportFragmentManager(), "color-picker-dialog");
    }

    private void applyThemeColor() {
        int primaryColor = ThemeManager.getInstance(this).getPrimaryColor();
        themeDivider.setBackgroundColor(primaryColor);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Settings");
            }
        }
    }
}
