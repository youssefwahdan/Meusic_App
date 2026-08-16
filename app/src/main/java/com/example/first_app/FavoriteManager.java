package com.example.first_app;

import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteManager {
    private static FavoriteManager instance;
    private final FavoriteDao favoriteDao;
    private final ExecutorService executor;

    private FavoriteManager(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        favoriteDao = db.favoriteDao();
        // Create a single background thread for database operations
        executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized FavoriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoriteManager(context);
        }
        return instance;
    }

    // Toggle favorite (Add if not exists, remove if exists)
    public void toggleFavorite(long songId) {
        executor.execute(() -> {
            // Check current status synchronously inside the background thread
            // Note: We use a raw query here to avoid LiveData overhead in the background
            // For simplicity, we'll just try to delete, and if it doesn't exist, we insert.
            // Actually, let's just use a simple logic:

            // To check if it exists in a background thread, we can query directly:
            // (Room allows synchronous queries off the main thread)
            boolean exists = favoriteDao.isFavoriteSync(songId) == 1;

            if (exists) {
                favoriteDao.removeFavorite(songId);
            } else {
                favoriteDao.addFavorite(new FavoriteEntity(songId, System.currentTimeMillis()));
            }
        });
    }

    // Get LiveData for a specific song's favorite status (for the heart icon)
    public LiveData<Integer> getFavoriteStatus(long songId) {
        return favoriteDao.isFavorite(songId);
    }

    // Get LiveData for all favorites (for the Favorites Tab)
    public LiveData<List<FavoriteEntity>> getAllFavorites() {
        return favoriteDao.getAllFavorites();
    }

    public LiveData<Integer> getFavoritesCount() {
        return favoriteDao.favouritesCount();
    }
}