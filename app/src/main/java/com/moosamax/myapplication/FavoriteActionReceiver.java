package com.moosamax.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

public class FavoriteActionReceiver extends BroadcastReceiver {
    private static final String TAG = "FavoriteActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "ADD_TO_FAVORITES".equals(intent.getAction())) {
            Log.d(TAG, "Received favorite action from notification");

            // Use a background thread for database operations
            new Thread(() -> {
                try {
                    // Extract verse data from intent
                    String verseReference = intent.getStringExtra("verse_reference");
                    String verseCategory = intent.getStringExtra("verse_category");
                    String verseArabic = intent.getStringExtra("verse_arabic");
                    String verseEnglish = intent.getStringExtra("verse_english");

                    Log.d(TAG, "Processing favorite for verse: " + verseReference);

                    if (verseReference != null && verseCategory != null &&
                            verseArabic != null && verseEnglish != null) {

                        // Create VerseData object
                        VerseData verse = new VerseData(verseArabic, verseEnglish, verseReference, verseCategory, "Notification");

                        // Initialize FavoritesManager
                        FavoritesManager favoritesManager = FavoritesManager.getInstance(context);

                        // Check if already favorited
                        boolean isAlreadyFavorite = favoritesManager.isFavorite(verse);

                        if (isAlreadyFavorite) {
                            Log.d(TAG, "Verse already in favorites: " + verseReference);
                            showToastOnMainThread(context, "Already in favorites ⭐");
                        } else {
                            // Add to favorites
                            boolean added = favoritesManager.addToFavorites(verse);

                            if (added) {
                                Log.d(TAG, "Successfully added verse to favorites: " + verseReference);
                                showToastOnMainThread(context, "❤️ Added to favorites: " + verseReference);

                                // Optional: Update the notification to show it's been favorited
                                updateNotificationToFavorited(context, verse);
                            } else {
                                Log.w(TAG, "Failed to add verse to favorites: " + verseReference);
                                showToastOnMainThread(context, "Failed to add to favorites");
                            }
                        }

                    } else {
                        Log.e(TAG, "Missing verse data in favorite action intent");
                        Log.e(TAG, "Reference: " + verseReference);
                        Log.e(TAG, "Category: " + verseCategory);
                        Log.e(TAG, "Arabic: " + (verseArabic != null ? "Present" : "NULL"));
                        Log.e(TAG, "English: " + (verseEnglish != null ? "Present" : "NULL"));
                        showToastOnMainThread(context, "Error: Missing verse data");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error adding verse to favorites from notification", e);
                    showToastOnMainThread(context, "Error adding to favorites");
                }
            }).start();
        }
    }

    private void showToastOnMainThread(Context context, String message) {
        // Ensure toast is shown on main thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast", e);
            }
        });
    }

    private void updateNotificationToFavorited(Context context, VerseData verse) {
        try {
            // Create a new notification to replace the original one, showing it's been favorited
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                // Create intent to open the specific verse
                Intent notificationIntent = new Intent(context, MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                notificationIntent.putExtra("verse_reference", verse.getReference());
                notificationIntent.putExtra("verse_category", verse.getCategory());
                notificationIntent.putExtra("from_notification", true);

                int requestCode = verse.getReference().hashCode();
                android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                        context,
                        requestCode,
                        notificationIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                );

                // Create share intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareText = verse.getArabicText() + "\n\n" +
                        verse.getEnglishTranslation() + "\n\n" +
                        "— " + verse.getReference() + "\n" +
                        "Category: " + verse.getCategory() + "\n\n" +
                        "Shared from Quran Verses App";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                android.app.PendingIntent sharePendingIntent = android.app.PendingIntent.getActivity(
                        context,
                        requestCode + 1,
                        Intent.createChooser(shareIntent, "Share Verse"),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
                );

                // Build updated notification
                androidx.core.app.NotificationCompat.Builder builder =
                        new androidx.core.app.NotificationCompat.Builder(context, "QuranVersesChannel")
                                .setSmallIcon(R.drawable.ic_book)
                                .setContentTitle("📖 Ayah of the Hour")
                                .setContentText(verse.getEnglishTranslation())
                                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                                        .bigText(verse.getEnglishTranslation() + "\n\n— " + verse.getReference())
                                        .setBigContentTitle("📖 " + verse.getCategory() + " ⭐ Favorited"))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true)
                                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_REMINDER)
                                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                                .addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent)
                                .addAction(android.R.drawable.btn_star_big_on, "Favorited ✓", null); // Disabled button

                notificationManager.notify(1001, builder.build());
                Log.d(TAG, "Updated notification to show favorited status");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating notification after favoriting", e);
        }
    }
}