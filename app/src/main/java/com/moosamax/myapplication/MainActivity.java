package com.moosamax.myapplication;

// MainActivity.java - Complete implementation with bottom navigation and dynamic verse content
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
    private TextView totalVersesCount;
    private TextView favoritesCount;

    // Current selected tab index (0=Home, 1=Verses, 2=Favorites, 3=Settings)
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initViews();
        initBottomNavigation();

        // Load verse content
        loadVerseContent();
        updateStatistics();

        // Request notification permission for Android 13+
        requestNotificationPermission();

        // Initialize default notification schedule if not set
        if (!NotificationScheduler.areNotificationsEnabled(this)) {
            // Ask user if they want to enable notifications
            showNotificationSetupDialog();
        }

        // Set home as selected by default
        updateTabSelection(0);
    }

    /**
     * Initialize content views
     */
    private void initViews() {
        // Find content views - these IDs should match your layout
        verseOfDayArabic = findViewById(R.id.verse_of_day_arabic);
        verseOfDayEnglish = findViewById(R.id.verse_of_day_english);
        verseOfDayReference = findViewById(R.id.verse_of_day_reference);
        totalVersesCount = findViewById(R.id.total_verses_count);
        favoritesCount = findViewById(R.id.favorites_count);

        // If these TextViews don't exist in your current layout, we'll find them by content
        if (verseOfDayArabic == null) {
            findTextViewsByContent();
        }
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
        // Get a random verse for "Verse of the Day"
        VerseData todayVerse = VerseRepository.getRandomVerse();

        if (verseOfDayArabic != null) {
            verseOfDayArabic.setText(todayVerse.getArabicText());
        }

        if (verseOfDayEnglish != null) {
            verseOfDayEnglish.setText(todayVerse.getEnglishTranslation());
        }

        if (verseOfDayReference != null) {
            verseOfDayReference.setText(todayVerse.getReference());
        }
    }

    /**
     * Update statistics display
     */
    private void updateStatistics() {
        List<VerseData> allVerses = VerseRepository.getAllVerses();

        if (totalVersesCount != null) {
            totalVersesCount.setText(String.valueOf(allVerses.size()));
        }

        // For now, favorites count remains 0
        // In a future update, you could implement a favorites system
        if (favoritesCount != null) {
            favoritesCount.setText("0");
        }

        // Update category counts
        updateCategoryStatistics(allVerses);
    }

    /**
     * Update category statistics in the UI
     */
    private void updateCategoryStatistics(List<VerseData> verses) {
        // Count verses by category
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (VerseData verse : verses) {
            String category = verse.getCategory();
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }

        // Update category counts in the UI
        updateCategoryCount("Trust in Allah", categoryCounts.getOrDefault("Trust in Allah", 0));
        updateCategoryCount("Hope & Patience", categoryCounts.getOrDefault("Hope & Patience", 0));
        updateCategoryCount("Mercy & Forgiveness", categoryCounts.getOrDefault("Mercy & Forgiveness", 0));
        updateCategoryCount("Dua & Supplication", categoryCounts.getOrDefault("Dua & Supplication", 0));
        updateCategoryCount("Patience & Perseverance", categoryCounts.getOrDefault("Patience & Perseverance", 0));
    }

    /**
     * Update count for a specific category
     */
    private void updateCategoryCount(String categoryName, int count) {
        // This method searches for TextViews containing category counts
        // You might need to adjust this based on your exact layout structure
        LinearLayout mainLayout = findViewById(R.id.content);
        updateCategoryCountRecursively(mainLayout, categoryName, count);
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
                    } else if (foundCategory && text.contains("verses")) {
                        countTextView = textView;
                        break;
                    }
                }
            }

            // Update the count if we found the category
            if (foundCategory && countTextView != null) {
                String countText = count == 1 ? "1 verse" : count + " verses";
                countTextView.setText(countText);
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

            case 1: // Verses
                navigateToVerses();
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
     * Navigate to Verses activity/fragment
     */
    private void navigateToVerses() {
        // Show all verses with their categories
        showAllVerses();
        updateTabSelection(0); // Keep home selected for now
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
        // TODO: Implement favorites functionality
        Toast.makeText(this, "Favorites feature coming soon!\nCurrently: 0 favorites", Toast.LENGTH_SHORT).show();
        updateTabSelection(0);
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
     * Public method to refresh verse content (can be called from menu items, etc.)
     */
    public void refreshVerseOfDay(View view) {
        loadVerseContent();
        Toast.makeText(this, "Verse of the day refreshed", Toast.LENGTH_SHORT).show();
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
    }
}