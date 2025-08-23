package com.moosamax.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity implements FavoritesManager.OnFavoritesChangedListener {

    private RecyclerView favoritesRecyclerView;
    private FavoriteVersesAdapter adapter;
    private LinearLayout backButton;
    private LinearLayout shareAllButton;
    private LinearLayout clearAllButton;
    private LinearLayout emptyStateLayout;  // Changed from TextView to LinearLayout
    private TextView favoritesCount;

    private FavoritesManager favoritesManager;
    private List<VerseData> favoriteVerses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        initViews();
        setupRecyclerView();
        loadFavorites();
    }

    private void initViews() {
        favoritesRecyclerView = findViewById(R.id.favorites_recycler_view);
        backButton = findViewById(R.id.back_button);
        shareAllButton = findViewById(R.id.share_all_button);
        clearAllButton = findViewById(R.id.clear_all_button);
        emptyStateLayout = findViewById(R.id.empty_state_text);  // Changed to LinearLayout
        favoritesCount = findViewById(R.id.favorites_count);

        // Initialize favorites manager
        favoritesManager = FavoritesManager.getInstance(this);
        favoritesManager.addListener(this);

        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        shareAllButton.setOnClickListener(v -> shareAllFavorites());
        clearAllButton.setOnClickListener(v -> clearAllFavorites());
    }

    private void setupRecyclerView() {
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadFavorites() {
        favoriteVerses = favoritesManager.getFavoriteVerses();
        updateUI();
    }

    private void updateUI() {
        int count = favoriteVerses.size();

        // Update count
        favoritesCount.setText(count + " favorite" + (count == 1 ? "" : "s"));

        if (count == 0) {
            // Show empty state
            favoritesRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);  // Changed to LinearLayout
            shareAllButton.setVisibility(View.GONE);
            clearAllButton.setVisibility(View.GONE);
        } else {
            // Show favorites list
            favoritesRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);  // Changed to LinearLayout
            shareAllButton.setVisibility(View.VISIBLE);
            clearAllButton.setVisibility(View.VISIBLE);

            // Setup adapter
            if (adapter == null) {
                adapter = new FavoriteVersesAdapter(favoriteVerses, new FavoriteVersesAdapter.OnVerseActionListener() {
                    @Override
                    public void onVerseClick(VerseData verse) {
                        openVerseDetail(verse);
                    }

                    @Override
                    public void onShareClick(VerseData verse) {
                        ShareUtils.shareVerseAsText(FavoritesActivity.this, verse);
                    }

                    @Override
                    public void onFavoriteClick(VerseData verse) {
                        favoritesManager.removeFromFavorites(verse);
                    }
                });
                favoritesRecyclerView.setAdapter(adapter);
            } else {
                adapter.updateVerses(favoriteVerses);
            }
        }
    }

    private void openVerseDetail(VerseData verse) {
        // Open verse detail or category verses activity
        Intent intent = new Intent(this, CategoryVersesActivity.class);
        intent.putExtra("CATEGORY_NAME", verse.getCategory());
        intent.putExtra("INITIAL_VERSE_REFERENCE", verse.getReference());
        startActivity(intent);
    }

    private void shareAllFavorites() {
        if (!favoriteVerses.isEmpty()) {
            ShareUtils.shareMultipleVerses(this, favoriteVerses, "My Favorite Quran Verses");
        }
    }

    private void clearAllFavorites() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Clear All Favorites")
                .setMessage("Are you sure you want to remove all verses from your favorites? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    favoritesManager.clearAllFavorites();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onFavoritesChanged() {
        runOnUiThread(() -> {
            loadFavorites();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (favoritesManager != null) {
            favoritesManager.removeListener(this);
        }
    }
}