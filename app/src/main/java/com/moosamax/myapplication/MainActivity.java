package com.moosamax.myapplication;

// MainActivity.java - Complete implementation with bottom navigation
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    // Bottom navigation views
    private LinearLayout homeTab;
    private LinearLayout versesTab;
    private LinearLayout favoritesTab;
    private LinearLayout settingsTab;

    // Current selected tab index (0=Home, 1=Verses, 2=Favorites, 3=Settings)
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        initBottomNavigation();

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
                // Already on home - no navigation needed
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
        // TODO: Implement navigation to verses list
        // For now, show a toast message
        Toast.makeText(this, "Verses feature coming soon!", Toast.LENGTH_SHORT).show();

        // Keep home selected for now since we don't have verses activity yet
        updateTabSelection(0);

        // Example of how to navigate to a new activity:
        // Intent intent = new Intent(this, VersesActivity.class);
        // startActivity(intent);
    }

    /**
     * Navigate to Favorites activity/fragment
     */
    private void navigateToFavorites() {
        // TODO: Implement navigation to favorites
        // For now, show a toast message
        Toast.makeText(this, "Favorites feature coming soon!", Toast.LENGTH_SHORT).show();

        // Keep home selected for now since we don't have favorites activity yet
        updateTabSelection(0);

        // Example of how to navigate to a new activity:
        // Intent intent = new Intent(this, FavoritesActivity.class);
        // startActivity(intent);
    }

    /**
     * Navigate to Settings (NotificationSettingsActivity)
     */
    private void navigateToSettings() {
        Intent intent = new Intent(this, NotificationSettingsActivity.class);
        startActivity(intent);

        // Note: We don't update tab selection here because we're navigating to a new activity
        // When user returns, home will still be selected
    }

    /**
     * Show dialog to setup notifications when app first launches
     */
    private void showNotificationSetupDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Daily Verse Notifications")
                .setMessage("Would you like to receive a daily Quran verse notification?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Enable notifications with default time (9:00 AM)
                    NotificationScheduler.scheduleVerseNotifications(this, 9, 0);
                    Toast.makeText(this, "Daily notifications enabled at 9:00 AM", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Not now", null)
                .setNeutralButton("Settings", (dialog, which) -> {
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
                Toast.makeText(this, "Notification permission is required for daily verses",
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
     */
    public void sendTestNotification() {
        Intent serviceIntent = new Intent(this, VerseNotificationService.class);
        serviceIntent.setAction("SEND_VERSE_NOTIFICATION");
        startService(serviceIntent);
        Toast.makeText(this, "Test notification sent", Toast.LENGTH_SHORT).show();
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
    }
}