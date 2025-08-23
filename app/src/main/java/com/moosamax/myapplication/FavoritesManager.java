package com.moosamax.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoritesManager {
    private static final String TAG = "FavoritesManager";
    private static final String PREFS_NAME = "QuranAppFavorites";
    private static final String KEY_FAVORITE_REFERENCES = "favorite_references";

    private static FavoritesManager instance;
    private SharedPreferences prefs;
    private Set<String> favoriteReferences;
    private List<OnFavoritesChangedListener> listeners;

    public interface OnFavoritesChangedListener {
        void onFavoritesChanged();
    }

    private FavoritesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        favoriteReferences = new HashSet<>();
        listeners = new ArrayList<>();
        loadFavorites();
    }

    public static synchronized FavoritesManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoritesManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Load favorite references from SharedPreferences
     */
    private void loadFavorites() {
        Set<String> savedFavorites = prefs.getStringSet(KEY_FAVORITE_REFERENCES, new HashSet<>());
        favoriteReferences.clear();
        favoriteReferences.addAll(savedFavorites);
        Log.d(TAG, "Loaded " + favoriteReferences.size() + " favorites");
    }

    /**
     * Save favorite references to SharedPreferences
     */
    private void saveFavorites() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(KEY_FAVORITE_REFERENCES, favoriteReferences);
        editor.apply();
        Log.d(TAG, "Saved " + favoriteReferences.size() + " favorites");

        // Notify listeners
        for (OnFavoritesChangedListener listener : listeners) {
            listener.onFavoritesChanged();
        }
    }

    /**
     * Add a verse to favorites
     */
    public boolean addToFavorites(VerseData verse) {
        if (verse == null || verse.getReference() == null) {
            return false;
        }

        boolean added = favoriteReferences.add(verse.getReference());
        if (added) {
            saveFavorites();
            Log.d(TAG, "Added to favorites: " + verse.getReference());
        }
        return added;
    }

    /**
     * Remove a verse from favorites
     */
    public boolean removeFromFavorites(VerseData verse) {
        if (verse == null || verse.getReference() == null) {
            return false;
        }

        boolean removed = favoriteReferences.remove(verse.getReference());
        if (removed) {
            saveFavorites();
            Log.d(TAG, "Removed from favorites: " + verse.getReference());
        }
        return removed;
    }

    /**
     * Toggle favorite status of a verse
     */
    public boolean toggleFavorite(VerseData verse) {
        if (isFavorite(verse)) {
            return removeFromFavorites(verse);
        } else {
            return addToFavorites(verse);
        }
    }

    /**
     * Check if a verse is in favorites
     */
    public boolean isFavorite(VerseData verse) {
        if (verse == null || verse.getReference() == null) {
            return false;
        }
        return favoriteReferences.contains(verse.getReference());
    }

    /**
     * Get all favorite verses
     */
    public List<VerseData> getFavoriteVerses() {
        List<VerseData> favorites = new ArrayList<>();
        List<VerseData> allVerses = VerseRepository.getAllVerses();

        for (VerseData verse : allVerses) {
            if (isFavorite(verse)) {
                favorites.add(verse);
            }
        }

        Log.d(TAG, "Retrieved " + favorites.size() + " favorite verses");
        return favorites;
    }

    /**
     * Get count of favorite verses
     */
    public int getFavoriteCount() {
        return favoriteReferences.size();
    }

    /**
     * Clear all favorites
     */
    public void clearAllFavorites() {
        favoriteReferences.clear();
        saveFavorites();
        Log.d(TAG, "Cleared all favorites");
    }

    /**
     * Add listener for favorites changes
     */
    public void addListener(OnFavoritesChangedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove listener for favorites changes
     */
    public void removeListener(OnFavoritesChangedListener listener) {
        listeners.remove(listener);
    }

    /**
     * Get favorite verses by category
     */
    public List<VerseData> getFavoritesByCategory(String category) {
        List<VerseData> categoryFavorites = new ArrayList<>();
        List<VerseData> allFavorites = getFavoriteVerses();

        for (VerseData verse : allFavorites) {
            if (verse.getCategory().equals(category)) {
                categoryFavorites.add(verse);
            }
        }

        return categoryFavorites;
    }
}