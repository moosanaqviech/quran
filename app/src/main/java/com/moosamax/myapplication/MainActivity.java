package com.moosamax.myapplication;

// MainActivity.java - Updated with category browsing functionality
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Bottom navigation views
    private LinearLayout homeTab;
    private LinearLayout versesTab;
    private LinearLayout favoritesTab;
    private LinearLayout settingsTab;

    // Content views
    private TextView verseOfDayArabic;
    private TextView verseOfDayEnglish;
    private TextView verseOfDayReference;
    private TextView verseDateIndicator;
    private TextView totalVersesCount;
    private TextView favoritesCount;

    // Category section views
    private LinearLayout categoriesContainer;
    private LinearLayout refreshVerseButton;

    // Current selected tab index (0=Home, 1=Verses, 2=Favorites, 3=Settings)
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        // Initialize the verse repository
        VerseRepository.getInstance(this).initialize();

        // Initialize views
        initViews();
        initBottomNavigation();

        // Load verse content first
        loadVerseContent();
        updateStatistics();

        // Populate categories dynamically (after repository is initialized)
        populateCategories();

        // Load verse content
        loadVerseContent();
        updateStatistics();

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Add debug information
        debugRepositoryState();

        // Initialize default notification schedule if not set
        if (!NotificationScheduler.areNotificationsEnabled(this)) {
            // Ask user if they want to enable notifications
            showNotificationSetupDialog();
        }

        // Set home as selected by default
        updateTabSelection(0);
    }

    /**
     * Debug method to check VerseRepository state
     */
    private void debugRepositoryState() {
        Log.d("MainActivity", "=== DEBUGGING REPOSITORY STATE ===");
        Log.d("MainActivity", "Repository initialized: " + VerseRepository.isInitialized());

        List<VerseData> allVerses = VerseRepository.getAllVerses();
        Log.d("MainActivity", "Total verses in repository: " + allVerses.size());

        if (allVerses.size() > 0) {
            Log.d("MainActivity", "Sample verse: " + allVerses.get(0).getReference());
            Log.d("MainActivity", "Sample category: " + allVerses.get(0).getCategory());
        }

        List<String> categories = VerseRepository.getAllCategories();
        Log.d("MainActivity", "Categories found: " + categories.size());
        for (String cat : categories) {
            int count = VerseRepository.getVerseCountByCategory(cat);
            Log.d("MainActivity", "- " + cat + ": " + count + " verses");
        }
        Log.d("MainActivity", "=== END DEBUG ===");
    }

    /**
     * Initialize content views
     */
    private void initViews() {
        // Find content views - these IDs should match your layout
        verseOfDayArabic = findViewById(R.id.verse_of_day_arabic);
        verseOfDayEnglish = findViewById(R.id.verse_of_day_english);
        verseOfDayReference = findViewById(R.id.verse_of_day_reference);
        verseDateIndicator = findViewById(R.id.verse_date_indicator);
        totalVersesCount = findViewById(R.id.total_verses_count);
        favoritesCount = findViewById(R.id.favorites_count);
        categoriesContainer = findViewById(R.id.categories_container);
        refreshVerseButton = findViewById(R.id.refresh_verse_button);

        // Set up Browse All click listener
        TextView browseAllButton = findViewById(R.id.browse_all_categories);
        if (browseAllButton != null) {
            browseAllButton.setOnClickListener(v -> navigateToCategoryBrowse());
        }

        // Set up refresh verse button
        if (refreshVerseButton != null) {
            refreshVerseButton.setOnClickListener(v -> showRefreshVerseOptions());
        }

        // If these TextViews don't exist in your current layout, we'll find them by content
        if (verseOfDayArabic == null) {
            findTextViewsByContent();
        }
    }

    /**
     * Dynamically populate categories from VerseRepository
     */
    private void populateCategories() {
        Log.d("MainActivity", "populateCategories() called");

        if (categoriesContainer == null) {
            Log.w("MainActivity", "Categories container not found, trying to find it manually");
            categoriesContainer = findViewById(R.id.categories_container);
            if (categoriesContainer == null) {
                Log.e("MainActivity", "ERROR: categories_container not found in layout!");
                return;
            }
        }

        Log.d("MainActivity", "Categories container found: " + categoriesContainer);

        // Clear existing categories
        categoriesContainer.removeAllViews();
        Log.d("MainActivity", "Cleared existing category views");

        // Check if repository is initialized
        if (!VerseRepository.isInitialized()) {
            Log.w("MainActivity", "VerseRepository not initialized, calling initialize()");
            VerseRepository.initialize();
        }

        // Get actual categories from repository
        List<String> actualCategories = VerseRepository.getAllCategories();

        Log.d("MainActivity", "Found " + actualCategories.size() + " categories from repository");
        for (String category : actualCategories) {
            Log.d("MainActivity", "Category: " + category);
        }

        if (actualCategories.isEmpty()) {
            Log.w("MainActivity", "No categories found! Check VerseRepository data");
            // Add a test category to see if the UI works
            createTestCategory();
            return;
        }

        // Show only first 5 categories on home page for cleaner look
        int categoriesToShow = Math.min(5, actualCategories.size());
        for (int i = 0; i < categoriesToShow; i++) {
            String category = actualCategories.get(i);
            int count = VerseRepository.getVerseCountByCategory(category);
            Log.d("MainActivity", "Creating view for category: " + category + " (" + count + " verses)");
            createCategoryView(category, count);
        }

        // Add "Browse All" option if there are more categories
        if (actualCategories.size() > 5) {
            createBrowseAllCategoryView(actualCategories.size() - 5);
        }

        Log.d("MainActivity", "Finished populating categories. Container now has " +
                categoriesContainer.getChildCount() + " child views");
    }

    /**
     * Create a "Browse All Categories" view
     */
    private void createBrowseAllCategoryView(int remainingCount) {
        try {
            // Create CardView for "Browse All"
            androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, dpToPx(8));
            cardView.setLayoutParams(cardParams);
            cardView.setRadius(dpToPx(12));
            cardView.setCardElevation(dpToPx(2));
            cardView.setUseCompatPadding(true);

            // Gradient background for "Browse All"
            android.graphics.drawable.GradientDrawable gradientDrawable = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF3498DB, 0xFF2980B9}
            );
            gradientDrawable.setCornerRadius(dpToPx(12));
            cardView.setBackground(gradientDrawable);

            cardView.setOnClickListener(v -> navigateToCategoryBrowse());

            // Create content layout
            LinearLayout contentLayout = new LinearLayout(this);
            contentLayout.setLayoutParams(new androidx.cardview.widget.CardView.LayoutParams(
                    androidx.cardview.widget.CardView.LayoutParams.MATCH_PARENT,
                    androidx.cardview.widget.CardView.LayoutParams.WRAP_CONTENT
            ));
            contentLayout.setOrientation(LinearLayout.HORIZONTAL);
            contentLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
            contentLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

            // Icon
            TextView iconView = new TextView(this);
            iconView.setText("📚");
            iconView.setTextSize(20);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                    dpToPx(48), dpToPx(48)
            );
            iconParams.setMarginEnd(dpToPx(16));
            iconView.setLayoutParams(iconParams);
            iconView.setGravity(android.view.Gravity.CENTER);

            // Text container
            LinearLayout textContainer = new LinearLayout(this);
            textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            ));
            textContainer.setOrientation(LinearLayout.VERTICAL);

            // Title
            TextView titleView = new TextView(this);
            titleView.setText("Browse All Categories");
            titleView.setTextSize(17);
            titleView.setTextColor(0xFFFFFFFF);
            titleView.setTypeface(null, android.graphics.Typeface.BOLD);

            // Subtitle
            TextView subtitleView = new TextView(this);
            subtitleView.setText("+" + remainingCount + " more categories available");
            subtitleView.setTextSize(13);
            subtitleView.setTextColor(0xCCFFFFFF);
            LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            subtitleParams.setMargins(0, dpToPx(2), 0, 0);
            subtitleView.setLayoutParams(subtitleParams);

            textContainer.addView(titleView);
            textContainer.addView(subtitleView);

            // Arrow
            TextView arrowView = new TextView(this);
            arrowView.setText("›");
            arrowView.setTextSize(24);
            arrowView.setTextColor(0xFFFFFFFF);
            arrowView.setTypeface(null, android.graphics.Typeface.BOLD);

            contentLayout.addView(iconView);
            contentLayout.addView(textContainer);
            contentLayout.addView(arrowView);

            cardView.addView(contentLayout);
            categoriesContainer.addView(cardView);

        } catch (Exception e) {
            Log.e("MainActivity", "Error creating browse all category view", e);
        }
    }

    /**
     * Create a test category to verify the UI works
     */
    private void createTestCategory() {
        Log.d("MainActivity", "Creating test category");
        createCategoryView("Test Category", 1);
    }

    /**
     * Create a category view dynamically
     */
    private void createCategoryView(String categoryName, int verseCount) {
        Log.d("MainActivity", "Creating category view for: " + categoryName);

        try {
            // Create CardView for better visual appeal
            androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, dpToPx(8)); // Bottom margin between cards
            cardView.setLayoutParams(cardParams);
            cardView.setRadius(dpToPx(12));
            cardView.setCardElevation(dpToPx(3));
            cardView.setUseCompatPadding(true);

            // Set card background with subtle gradient effect
            cardView.setCardBackgroundColor(0xFFFFFFFF); // White background

            // Make card clickable with ripple effect
            cardView.setForeground(getSelectableItemBackground());
            cardView.setClickable(true);
            cardView.setOnClickListener(v -> {
                Log.d("MainActivity", "Category clicked: " + categoryName);
                openCategoryVerses(categoryName);
            });

            // Create main content LinearLayout
            LinearLayout categoryLayout = new LinearLayout(this);
            androidx.cardview.widget.CardView.LayoutParams layoutParams =
                    new androidx.cardview.widget.CardView.LayoutParams(
                            androidx.cardview.widget.CardView.LayoutParams.MATCH_PARENT,
                            androidx.cardview.widget.CardView.LayoutParams.WRAP_CONTENT
                    );
            categoryLayout.setLayoutParams(layoutParams);
            categoryLayout.setOrientation(LinearLayout.HORIZONTAL);
            categoryLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            int paddingPx = dpToPx(16);
            categoryLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

            // Create emoji container with background circle
            LinearLayout emojiContainer = new LinearLayout(this);
            LinearLayout.LayoutParams emojiContainerParams = new LinearLayout.LayoutParams(
                    dpToPx(48), dpToPx(48)
            );
            emojiContainerParams.setMarginEnd(dpToPx(16));
            emojiContainer.setLayoutParams(emojiContainerParams);
            emojiContainer.setGravity(android.view.Gravity.CENTER);

            // Set circular background for emoji
            android.graphics.drawable.GradientDrawable emojiBackground = new android.graphics.drawable.GradientDrawable();
            emojiBackground.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            emojiBackground.setColor(getCategoryColor(categoryName));
            emojiContainer.setBackground(emojiBackground);

            // Create emoji TextView
            TextView emojiView = new TextView(this);
            emojiView.setText(getEmojiForCategory(categoryName));
            emojiView.setTextSize(22);
            emojiView.setGravity(android.view.Gravity.CENTER);
            emojiContainer.addView(emojiView);

            // Create text content container
            LinearLayout textContainer = new LinearLayout(this);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
            );
            textContainer.setLayoutParams(textParams);
            textContainer.setOrientation(LinearLayout.VERTICAL);

            // Create category name TextView
            TextView nameView = new TextView(this);
            nameView.setText(categoryName);
            nameView.setTextSize(17);
            nameView.setTextColor(0xFF2C3E50); // Dark blue-gray
            nameView.setTypeface(null, android.graphics.Typeface.BOLD);
            nameView.setMaxLines(1);
            nameView.setEllipsize(android.text.TextUtils.TruncateAt.END);

            // Create count TextView
            TextView countView = new TextView(this);
            String countText = verseCount == 1 ? "1 verse" : verseCount + " verses";
            countView.setText(countText);
            countView.setTextSize(13);
            countView.setTextColor(0xFF7F8C8D); // Medium gray
            LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            countParams.setMargins(0, dpToPx(2), 0, 0);
            countView.setLayoutParams(countParams);

            textContainer.addView(nameView);
            textContainer.addView(countView);

            // Create arrow icon
            TextView arrowView = new TextView(this);
            arrowView.setText("›");
            arrowView.setTextSize(24);
            arrowView.setTextColor(0xFFBDC3C7); // Light gray
            arrowView.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            arrowView.setLayoutParams(arrowParams);

            // Add all views to category layout
            categoryLayout.addView(emojiContainer);
            categoryLayout.addView(textContainer);
            categoryLayout.addView(arrowView);

            // Add layout to card
            cardView.addView(categoryLayout);

            // Add card to container
            categoriesContainer.addView(cardView);

            Log.d("MainActivity", "Successfully added category card: " + categoryName + " (" + countText + ")");

        } catch (Exception e) {
            Log.e("MainActivity", "Error creating category view for " + categoryName, e);
        }
    }

    /**
     * Get selectable item background drawable safely
     */
    private android.graphics.drawable.Drawable getSelectableItemBackground() {
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
            return getResources().getDrawable(typedValue.resourceId);
        } catch (Exception e) {
            Log.w("MainActivity", "Could not get selectableItemBackground, using default");
            // Return a simple transparent drawable as fallback
            android.graphics.drawable.ColorDrawable fallback = new android.graphics.drawable.ColorDrawable(0x10000000);
            return fallback;
        }
    }

    /**
     * Get color for category background
     */
    private int getCategoryColor(String category) {
        Map<String, Integer> categoryColors = new HashMap<>();
        categoryColors.put("Trust in Allah", 0xFF4CAF50);      // Green
        categoryColors.put("Hope & Patience", 0xFF2196F3);     // Blue
        categoryColors.put("Mercy & Forgiveness", 0xFFE91E63); // Pink
        categoryColors.put("Dua & Supplication", 0xFF9C27B0);  // Purple
        categoryColors.put("Patience & Perseverance", 0xFFFF9800); // Orange
        categoryColors.put("Guidance", 0xFF3F51B5);            // Indigo
        categoryColors.put("Prayer", 0xFF00BCD4);              // Cyan
        categoryColors.put("Gratitude", 0xFF8BC34A);           // Light Green
        categoryColors.put("Protection", 0xFF795548);          // Brown
        categoryColors.put("Wisdom", 0xFF607D8B);              // Blue Gray
        categoryColors.put("Good Deeds", 0xFFFFEB3B);          // Yellow

        // Return color with transparency for softer look
        int baseColor = categoryColors.getOrDefault(category, 0xFF9E9E9E);
        return (baseColor & 0x00FFFFFF) | 0x20000000; // Add transparency
    }

    /**
     * Convert dp to pixels
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Open category verses activity for a specific category
     */
    private void openCategoryVerses(String categoryName) {
        Intent intent = new Intent(this, CategoryVersesActivity.class);
        intent.putExtra("CATEGORY_NAME", categoryName);
        startActivity(intent);
    }

    /**
     * Fallback method to find TextViews by their current content
     * This is used if the IDs don't match the layout
     */
    private void findTextViewsByContent() {
        // Find all TextViews in the layout
        LinearLayout mainLayout = findViewById(android.R.id.content);
        findTextViewsRecursively(mainLayout);
    }

    /**
     * Recursively search for TextViews with specific content patterns
     */
    private void findTextViewsRecursively(View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            String text = textView.getText().toString();

            // Identify TextViews by their current content
            if (text.contains("رَبَّنَا آتِنَا فِي الدُّنْيَا")) {
                verseOfDayArabic = textView;
            } else if (text.contains("Our Lord, give us good")) {
                verseOfDayEnglish = textView;
            } else if (text.contains("Al-Baqarah 2:201")) {
                verseOfDayReference = textView;
            } else if (text.equals("6")) {
                totalVersesCount = textView;
            } else if (text.equals("0") && favoritesCount == null) {
                favoritesCount = textView;
            }
        } else if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;
            for (int i = 0; i < layout.getChildCount(); i++) {
                findTextViewsRecursively(layout.getChildAt(i));
            }
        }
    }

    /**
     * Load verse content from repository
     */
    private void loadVerseContent() {
        // Get the verse of the day (changes only once per day)
        VerseData todayVerse = getVerseOfTheDay();

        if (verseOfDayArabic != null) {
            verseOfDayArabic.setText(todayVerse.getArabicText());
        }

        if (verseOfDayEnglish != null) {
            verseOfDayEnglish.setText(todayVerse.getEnglishTranslation());
        }

        if (verseOfDayReference != null) {
            verseOfDayReference.setText(todayVerse.getReference());
        }

        // Update date indicator
        updateDateIndicator();
    }

    /**
     * Update the date indicator to show when verse was last updated
     */
    private void updateDateIndicator() {
        if (verseDateIndicator != null) {
            SharedPreferences prefs = getSharedPreferences("verse_of_day", MODE_PRIVATE);
            String storedDate = prefs.getString("last_date", "");
            String currentDate = getCurrentDateString();

            if (currentDate.equals(storedDate)) {
                // Format today's date nicely
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault());

                try {
                    java.util.Date date = inputFormat.parse(currentDate);
                    String formattedDate = outputFormat.format(date);
                    verseDateIndicator.setText("Today • " + formattedDate);
                } catch (Exception e) {
                    verseDateIndicator.setText("Today");
                }
            } else {
                verseDateIndicator.setText("Loading...");
            }
        }
    }

    /**
     * Show refresh verse options
     */
    private void showRefreshVerseOptions() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("🔄 Refresh Verse")
                .setMessage("Choose how you'd like to refresh today's verse:")
                .setPositiveButton("✨ Get New Verse", (dialog, which) -> {
                    forceRefreshVerseOfDay();
                    Toast.makeText(this, "✨ New verse selected for today!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("🔄 Reload Current", (dialog, which) -> {
                    loadVerseContent();
                    Toast.makeText(this, "🔄 Today's verse reloaded", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    /**
     * Get verse of the day - changes only once per day
     */
    private VerseData getVerseOfTheDay() {
        SharedPreferences prefs = getSharedPreferences("verse_of_day", MODE_PRIVATE);

        // Get current date as string (YYYY-MM-DD format)
        String currentDate = getCurrentDateString();

        // Get stored date and verse reference
        String storedDate = prefs.getString("last_date", "");
        String storedVerseReference = prefs.getString("verse_reference", "");

        Log.d("MainActivity", "Current date: " + currentDate);
        Log.d("MainActivity", "Stored date: " + storedDate);
        Log.d("MainActivity", "Stored verse: " + storedVerseReference);

        // Check if we need to get a new verse (new day or no stored verse)
        if (!currentDate.equals(storedDate) || storedVerseReference.isEmpty()) {
            Log.d("MainActivity", "Getting new verse of the day");

            // Get a new random verse for today
            VerseData newVerse = getRandomVerseForDate(currentDate);

            // Store the new verse and date
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("last_date", currentDate);
            editor.putString("verse_reference", newVerse.getReference());
            editor.putString("verse_arabic", newVerse.getArabicText());
            editor.putString("verse_english", newVerse.getEnglishTranslation());
            editor.putString("verse_category", newVerse.getCategory());
            editor.putString("verse_origin", newVerse.getOrigin());
            editor.apply();

            Log.d("MainActivity", "Stored new verse of the day: " + newVerse.getReference());
            return newVerse;
        } else {
            Log.d("MainActivity", "Using cached verse of the day: " + storedVerseReference);

            // Return the cached verse for today
            String storedArabic = prefs.getString("verse_arabic", "");
            String storedEnglish = prefs.getString("verse_english", "");
            String storedCategory = prefs.getString("verse_category", "");
            String storedOrigin = prefs.getString("verse_origin", "");

            return new VerseData(storedArabic, storedEnglish, storedVerseReference, storedCategory, storedOrigin);
        }
    }

    /**
     * Public method to get today's verse (can be used by other activities)
     */
    public static VerseData getTodaysVerse(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("verse_of_day", Context.MODE_PRIVATE);
        String currentDate = getCurrentDateString();
        String storedDate = prefs.getString("last_date", "");

        if (currentDate.equals(storedDate)) {
            // Return cached verse for today
            String storedReference = prefs.getString("verse_reference", "");
            String storedArabic = prefs.getString("verse_arabic", "");
            String storedEnglish = prefs.getString("verse_english", "");
            String storedCategory = prefs.getString("verse_category", "");
            String storedOrigin = prefs.getString("verse_origin", "");

            if (!storedReference.isEmpty()) {
                return new VerseData(storedArabic, storedEnglish, storedReference, storedCategory, storedOrigin);
            }
        }

        // Fallback to random verse if no verse of day is stored
        return VerseRepository.getRandomVerse();
    }

    /**
     * Static method to get current date string
     */
    private static String getCurrentDateString() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    /**
     * Get a random verse for a specific date (ensures same verse for same date)
     */
    private VerseData getRandomVerseForDate(String date) {
        List<VerseData> allVerses = VerseRepository.getAllVerses();

        if (allVerses.isEmpty()) {
            // Fallback if no verses available
            return new VerseData(
                    "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                    "Al-Fatihah 1:1",
                    "Opening",
                    "Fallback"
            );
        }

        // Use date as seed for consistent randomness (same date = same verse)
        int seed = date.hashCode();
        java.util.Random random = new java.util.Random(seed);

        // Get random verse based on date seed
        int index = random.nextInt(allVerses.size());
        VerseData selectedVerse = allVerses.get(index);

        Log.d("MainActivity", "Selected verse for date " + date + ": " + selectedVerse.getReference() + " (index: " + index + " of " + allVerses.size() + ")");

        return selectedVerse;
    }

    /**
     * Force refresh verse of the day (for manual refresh)
     */
    private void forceRefreshVerseOfDay() {
        // Clear stored verse to force new selection
        SharedPreferences prefs = getSharedPreferences("verse_of_day", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        Log.d("MainActivity", "Forced refresh of verse of the day");

        // Reload verse content
        loadVerseContent();
    }

    /**
     * Update statistics display
     */
    private void updateStatistics() {
        int totalVerseCount = VerseRepository.getTotalVerseCount();

        if (totalVersesCount != null) {
            totalVersesCount.setText(String.valueOf(totalVerseCount));
        }

        // Update favorites count from FavoritesManager
        FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
        int favoritesCountValue = favoritesManager.getFavoriteCount();
        if (favoritesCount != null) {
            favoritesCount.setText(String.valueOf(favoritesCountValue));
        }

        // Update category counts
        updateCategoryStatistics();
    }

    /**
     * Update category statistics in the UI
     */
    private void updateCategoryStatistics() {
        // Since we're now dynamically creating categories, just repopulate them
        populateCategories();
    }

    /**
     * Update count for a specific category
     */
    private void updateCategoryCount(String categoryName, int count) {
        // This method searches for TextViews containing category counts
        LinearLayout mainLayout = findViewById(R.id.content);
        if (mainLayout != null) {
            updateCategoryCountRecursively(mainLayout, categoryName, count);
        }
    }

    /**
     * Recursively find and update category count TextViews
     */
    private void updateCategoryCountRecursively(View view, String categoryName, int count) {
        if (view instanceof LinearLayout) {
            LinearLayout layout = (LinearLayout) view;

            // Check if this layout contains the category name
            boolean foundCategory = false;
            TextView countTextView = null;

            for (int i = 0; i < layout.getChildCount(); i++) {
                View child = layout.getChildAt(i);
                if (child instanceof TextView) {
                    TextView textView = (TextView) child;
                    String text = textView.getText().toString();

                    if (text.equals(categoryName)) {
                        foundCategory = true;
                    } else if (foundCategory && text.contains("verse")) {
                        countTextView = textView;
                        break;
                    }
                }
            }

            // Update the count if we found the category
            if (foundCategory && countTextView != null) {
                String countText = count == 1 ? "1 verse" : count + " verses";
                countTextView.setText(countText);
                Log.d("MainActivity", "Updated category '" + categoryName + "' to " + countText);
            }

            // Continue searching in child layouts
            for (int i = 0; i < layout.getChildCount(); i++) {
                updateCategoryCountRecursively(layout.getChildAt(i), categoryName, count);
            }
        }
    }

    /**
     * Initialize bottom navigation views and set click listeners
     */
    private void initBottomNavigation() {
        // Find bottom navigation tab views
        homeTab = findViewById(R.id.home_tab);
        versesTab = findViewById(R.id.verses_tab);
        favoritesTab = findViewById(R.id.favorites_tab);
        settingsTab = findViewById(R.id.settings_tab);

        // Set click listeners for each tab
        homeTab.setOnClickListener(v -> onTabSelected(0));
        versesTab.setOnClickListener(v -> onTabSelected(1));
        favoritesTab.setOnClickListener(v -> onTabSelected(2));
        settingsTab.setOnClickListener(v -> onTabSelected(3));
    }

    /**
     * Handle tab selection
     * @param tabIndex Index of selected tab (0-3)
     */
    private void onTabSelected(int tabIndex) {
        if (currentTabIndex == tabIndex) {
            return; // Same tab already selected
        }

        switch (tabIndex) {
            case 0: // Home
                // Already on home - refresh content
                loadVerseContent();
                updateStatistics();
                updateTabSelection(0);
                break;

            case 1: // Browse Categories (formerly Verses)
                navigateToCategoryBrowse();
                break;

            case 2: // Favorites
                navigateToFavorites();
                break;

            case 3: // Settings
                navigateToSettings();
                break;
        }
    }

    /**
     * Update visual selection state of tabs
     * @param selectedIndex Index of selected tab
     */
    private void updateTabSelection(int selectedIndex) {
        currentTabIndex = selectedIndex;

        // Reset all tabs to unselected state
        resetTabStyles();

        // Highlight selected tab
        switch (selectedIndex) {
            case 0:
                setTabSelected(homeTab);
                break;
            case 1:
                setTabSelected(versesTab);
                break;
            case 2:
                setTabSelected(favoritesTab);
                break;
            case 3:
                setTabSelected(settingsTab);
                break;
        }
    }

    /**
     * Reset all tabs to unselected visual state
     */
    private void resetTabStyles() {
        setTabUnselected(homeTab);
        setTabUnselected(versesTab);
        setTabUnselected(favoritesTab);
        setTabUnselected(settingsTab);
    }

    /**
     * Set tab visual state to selected
     * @param tab Tab layout to style as selected
     */
    private void setTabSelected(LinearLayout tab) {
        // Find ImageView and TextView in the tab
        android.widget.ImageView icon = (android.widget.ImageView) tab.getChildAt(0);
        android.widget.TextView text = (android.widget.TextView) tab.getChildAt(1);

        // Apply selected styling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            icon.setImageTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
        }
        text.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
    }

    /**
     * Set tab visual state to unselected
     * @param tab Tab layout to style as unselected
     */
    private void setTabUnselected(LinearLayout tab) {
        // Find ImageView and TextView in the tab
        android.widget.ImageView icon = (android.widget.ImageView) tab.getChildAt(0);
        android.widget.TextView text = (android.widget.TextView) tab.getChildAt(1);

        // Apply unselected styling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            icon.setImageTintList(ContextCompat.getColorStateList(this, android.R.color.darker_gray));
        }
        text.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
    }

    /**
     * Navigate to Category Browse activity
     */
    private void navigateToCategoryBrowse() {
        Intent intent = new Intent(this, CategoryBrowseActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to Verses activity/fragment (kept for compatibility)
     */
    private void navigateToVerses() {
        // Redirect to category browse
        navigateToCategoryBrowse();
    }

    /**
     * Show all verses in a dialog or toast (placeholder implementation)
     */
    private void showAllVerses() {
        List<VerseData> allVerses = VerseRepository.getAllVerses();
        StringBuilder versesText = new StringBuilder();

        for (VerseData verse : allVerses) {
            versesText.append(verse.getReference())
                    .append("\n")
                    .append(verse.getEnglishTranslation())
                    .append("\n")
                    .append("Category: ")
                    .append(verse.getCategory())
                    .append("\n\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("All Verses (" + allVerses.size() + ")")
                .setMessage(versesText.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    /**
     * Navigate to Favorites activity/fragment
     */
    private void navigateToFavorites() {
        Intent intent = new Intent(this, FavoritesActivity.class);
        startActivity(intent);
    }

    /**
     * Navigate to Settings (NotificationSettingsActivity)
     */
    private void navigateToSettings() {
        Intent intent = new Intent(this, NotificationSettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Show dialog to setup hourly notifications when app first launches
     */
    private void showNotificationSetupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hourly Verse Notifications")
                .setMessage("Would you like to receive hourly Quran verse notifications during the day?\n\nDefault schedule: 9:00 AM to 9:00 PM")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Enable hourly notifications with default time period (9 AM to 9 PM)
                    NotificationScheduler.scheduleHourlyVerseNotifications(this, 9, 0, 21, 0);
                    Toast.makeText(this, "Hourly notifications enabled from 9:00 AM to 9:00 PM", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Not now", null)
                .setNeutralButton("Customize", (dialog, which) -> {
                    navigateToSettings();
                })
                .show();
    }

    /**
     * Request notification permission for Android 13+
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }
    }

    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, can send notifications
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied
                Toast.makeText(this, "Notification permission is required for hourly verses",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Handle back button press - update tab selection if needed
     */
    @Override
    public void onBackPressed() {
        if (currentTabIndex != 0) {
            // If not on home tab, go back to home
            updateTabSelection(0);
        } else {
            // If on home tab, exit app
            super.onBackPressed();
        }
    }

    /**
     * Method to manually send test notification (for debugging)
     * This will only send if within the current notification period
     */
    public void sendTestNotification() {
        if (NotificationScheduler.isWithinNotificationPeriod(this)) {
            Intent serviceIntent = new Intent(this, VerseNotificationService.class);
            serviceIntent.setAction("SEND_VERSE_NOTIFICATION");
            startService(serviceIntent);
            Toast.makeText(this, "Test notification sent", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Current time is outside notification period", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Public method to refresh categories manually (for testing)
     */
    public void refreshCategories(View view) {
        Log.d("MainActivity", "Manual refresh categories called");
        populateCategories();
    }

    /**
     * Public method to test save functionality (for debugging)
     */
    public void testSaveFunctionality(View view) {
        Log.d("MainActivity", "Testing save functionality");
        try {
            // Get a random verse
            VerseData testVerse = VerseRepository.getRandomVerse();
            if (testVerse != null) {
                // Test favorites manager
                FavoritesManager favoritesManager = FavoritesManager.getInstance(this);
                boolean added = favoritesManager.addToFavorites(testVerse);

                String message = added ?
                        "✅ Test successful: Added " + testVerse.getReference() :
                        "ℹ️ Verse already in favorites: " + testVerse.getReference();

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                // Update favorites count
                updateStatistics();

                Log.d("MainActivity", "Save test result: " + added + " for verse: " + testVerse.getReference());
            } else {
                Toast.makeText(this, "❌ No verses available for testing", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error testing save functionality", e);
            Toast.makeText(this, "❌ Error testing save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Public method to test notification save (for debugging)
     */
    public void testNotificationSave(View view) {
        Log.d("MainActivity", "Testing notification save functionality");
        try {
            // Get a random verse
            VerseData testVerse = VerseRepository.getRandomVerse();
            if (testVerse != null) {
                // Simulate the broadcast receiver call
                Intent testIntent = new Intent();
                testIntent.setAction("ADD_TO_FAVORITES");
                testIntent.putExtra("verse_reference", testVerse.getReference());
                testIntent.putExtra("verse_category", testVerse.getCategory());
                testIntent.putExtra("verse_arabic", testVerse.getArabicText());
                testIntent.putExtra("verse_english", testVerse.getEnglishTranslation());

                // Create and call the receiver
                FavoriteActionReceiver receiver = new FavoriteActionReceiver();
                receiver.onReceive(this, testIntent);

                Log.d("MainActivity", "Triggered notification save test for: " + testVerse.getReference());
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error testing notification save", e);
            Toast.makeText(this, "❌ Error testing notification save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Public method to open notification settings (can be called from menu items, etc.)
     */
    public void openNotificationSettings(View view) {
        navigateToSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reset to home tab when returning from other activities
        updateTabSelection(0);
        // Refresh content when returning to the app
        loadVerseContent();
        updateStatistics();

        // Check if we need to dynamically update categories
        checkAndUpdateCategories();
    }

    /**
     * Check if the hardcoded categories in layout match actual categories
     * If not, show a debug message with actual categories
     */
    private void checkAndUpdateCategories() {
        List<String> actualCategories = VerseRepository.getAllCategories();

        Log.d("MainActivity", "Actual categories from repository:");
        for (String category : actualCategories) {
            int count = VerseRepository.getVerseCountByCategory(category);
            Log.d("MainActivity", "- " + category + ": " + count + " verses");
        }

        // Check if hardcoded categories exist in actual data
        String[] hardcodedCategories = {
                "Trust in Allah",
                "Hope & Patience",
                "Mercy & Forgiveness",
                "Dua & Supplication",
                "Patience & Perseverance"
        };

        boolean allCategoriesMatch = true;
        for (String hardcoded : hardcodedCategories) {
            if (!actualCategories.contains(hardcoded)) {
                allCategoriesMatch = false;
                Log.w("MainActivity", "Hardcoded category '" + hardcoded + "' not found in actual data");
            }
        }

        if (!allCategoriesMatch) {
            Log.w("MainActivity", "Some hardcoded categories don't match actual data. Consider updating the layout.");

            // Show toast with actual categories for debugging
            StringBuilder actualCats = new StringBuilder("Actual categories: ");
            for (int i = 0; i < actualCategories.size(); i++) {
                actualCats.append(actualCategories.get(i));
                if (i < actualCategories.size() - 1) actualCats.append(", ");
            }

            Log.d("MainActivity", actualCats.toString());
        }
    }

    /**
     * Get emoji for category (can be extended with more mappings)
     */
    private String getEmojiForCategory(String category) {
        Map<String, String> categoryEmojis = new HashMap<>();
        categoryEmojis.put("Trust in Allah", "🤲");
        categoryEmojis.put("Hope & Patience", "🌅");
        categoryEmojis.put("Mercy & Forgiveness", "💚");
        categoryEmojis.put("Dua & Supplication", "🤲");
        categoryEmojis.put("Patience & Perseverance", "💪");
        categoryEmojis.put("Guidance", "🌟");
        categoryEmojis.put("Prayer", "🕌");
        categoryEmojis.put("Gratitude", "🙏");
        categoryEmojis.put("Protection", "🛡️");
        categoryEmojis.put("Wisdom", "📚");
        categoryEmojis.put("Good Deeds", "✨");

        return categoryEmojis.getOrDefault(category, "📖");
    }
}