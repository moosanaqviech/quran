package com.moosamax.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.List;

public class CategoryVersesActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private VersesPagerAdapter pagerAdapter;
    private TextView categoryTitle;
    private TextView verseCounter;
    private LinearLayout backButton;
    private LinearLayout shareButton;
    private LinearLayout favoriteButton;

    private String categoryName;
    private List<VerseData> verses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_verses);

        // Get category name from intent
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        if (categoryName == null) {
            finish();
            return;
        }

        initViews();
        loadVerses();
        setupViewPager();
    }

    private void initViews() {
        viewPager = findViewById(R.id.verses_view_pager);
        categoryTitle = findViewById(R.id.category_title);
        verseCounter = findViewById(R.id.verse_counter);
        backButton = findViewById(R.id.back_button);
        shareButton = findViewById(R.id.share_button);
        favoriteButton = findViewById(R.id.favorite_button);

        // Set category title
        categoryTitle.setText(categoryName);

        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        shareButton.setOnClickListener(v -> shareCurrentVerse());
        favoriteButton.setOnClickListener(v -> toggleFavoriteCurrentVerse());
    }

    private void loadVerses() {
        verses = VerseRepository.getVersesByCategory(categoryName);

        if (verses.isEmpty()) {
            // If no verses found, show error and finish
            finish();
            return;
        }
    }

    private void setupViewPager() {
        pagerAdapter = new VersesPagerAdapter(verses);

        // Set action listener for share and favorite buttons
        pagerAdapter.setActionListener(new VersesPagerAdapter.OnVerseActionListener() {
            @Override
            public void onShareClick(VerseData verse) {
                showShareOptions(verse);
            }

            @Override
            public void onFavoriteClick(VerseData verse) {
                toggleFavorite(verse);
            }
        });

        viewPager.setAdapter(pagerAdapter);

        // Update counter when page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateVerseCounter(position);
                updateHeaderFavoriteButton(verses.get(position));
            }
        });

        // Initialize counter and favorite button
        updateVerseCounter(0);
        if (!verses.isEmpty()) {
            updateHeaderFavoriteButton(verses.get(0));
        }
    }

    private void showShareOptions(VerseData verse) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Share Verse")
                .setMessage("How would you like to share this verse?")
                .setPositiveButton("As Text", (dialog, which) ->
                        ShareUtils.shareVerseAsText(this, verse))
                .setNegativeButton("As Image", (dialog, which) ->
                        ShareUtils.shareVerseAsImage(this, verse))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void toggleFavorite(VerseData verse) {
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        boolean wasToggled = favoritesManager.toggleFavorite(verse);

        if (wasToggled) {
            boolean isFavorite = favoritesManager.isFavorite(verse);
            String message = isFavorite ? "Added to favorites ❤️" : "Removed from favorites";
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();

            // Update both header and page favorite buttons
            updateHeaderFavoriteButton(verse);

            // Refresh the current page to update the favorite button
            int currentPosition = viewPager.getCurrentItem();
            pagerAdapter.notifyItemChanged(currentPosition);
        }
    }

    private void updateHeaderFavoriteButton(VerseData verse) {
        // Update the header favorite button appearance
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        boolean isFavorite = favoritesManager.isFavorite(verse);

        // Find ImageView in favoriteButton and update it
        for (int i = 0; i < favoriteButton.getChildCount(); i++) {
            View child = favoriteButton.getChildAt(i);
            if (child instanceof android.widget.ImageView) {
                android.widget.ImageView icon = (android.widget.ImageView) child;
                icon.setImageResource(isFavorite ?
                        android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
                break;
            }
        }
    }

    private void updateVerseCounter(int position) {
        String counterText = (position + 1) + " of " + verses.size();
        verseCounter.setText(counterText);
    }

    private void shareCurrentVerse() {
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < verses.size()) {
            VerseData currentVerse = verses.get(currentPosition);
            showShareOptions(currentVerse);
        }
    }

    private void toggleFavoriteCurrentVerse() {
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < verses.size()) {
            VerseData currentVerse = verses.get(currentPosition);
            toggleFavorite(currentVerse);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}