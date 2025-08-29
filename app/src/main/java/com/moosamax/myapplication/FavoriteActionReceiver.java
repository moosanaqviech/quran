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
        Log.d(TAG, "FavoriteActionReceiver triggered");

        if (intent == null) {
            Log.e(TAG, "Intent is null");
            return;
        }

        Log.d(TAG, "Intent action: " + intent.getAction());

        if ("ADD_TO_FAVORITES".equals(intent.getAction())) {
            Log.d(TAG, "Processing ADD_TO_FAVORITES action");

            // Use a background thread for database operations
            new Thread(() -> {
                try {
                    // Extract verse data from intent
                    String verseReference = intent.getStringExtra("verse_reference");
                    String verseCategory = intent.getStringExtra("verse_category");
                    String verseArabic = intent.getStringExtra("verse_arabic");
                    String verseEnglish = intent.getStringExtra("verse_english");
                    String verseOrigin = intent.getStringExtra("verse_origin");

                    Log.d(TAG, "Extracted data:");
                    Log.d(TAG, "- Reference: " + verseReference);
                    Log.d(TAG, "- Category: " + verseCategory);
                    Log.d(TAG, "- Arabic: " + (verseArabic != null ? "Present (" + verseArabic.length() + " chars)" : "NULL"));
                    Log.d(TAG, "- English: " + (verseEnglish != null ? "Present (" + verseEnglish.length() + " chars)" : "NULL"));
                    Log.d(TAG, "- Origin: " + verseOrigin);

                    // Validate required data
                    if (verseReference == null || verseReference.trim().isEmpty()) {
                        Log.e(TAG, "Verse reference is missing or empty");
                        showToastOnMainThread(context, "❌ Error: Missing verse reference");
                        return;
                    }

                    if (verseCategory == null || verseCategory.trim().isEmpty()) {
                        Log.e(TAG, "Verse category is missing or empty");
                        showToastOnMainThread(context, "❌ Error: Missing verse category");
                        return;
                    }

                    if (verseArabic == null || verseArabic.trim().isEmpty()) {
                        Log.e(TAG, "Arabic text is missing or empty");
                        showToastOnMainThread(context, "❌ Error: Missing Arabic text");
                        return;
                    }

                    if (verseEnglish == null || verseEnglish.trim().isEmpty()) {
                        Log.e(TAG, "English text is missing or empty");
                        showToastOnMainThread(context, "❌ Error: Missing English text");
                        return;
                    }

                    // Create VerseData object
                    VerseData verse = new VerseData(
                            verseArabic.trim(),
                            verseEnglish.trim(),
                            verseReference.trim(),
                            verseCategory.trim(),
                            verseOrigin != null ? verseOrigin.trim() : "Notification"
                    );

                    Log.d(TAG, "Created VerseData object: " + verse.getReference());

                    // Initialize FavoritesManager
                    FavoritesManager favoritesManager = FavoritesManager.getInstance(context);

                    if (favoritesManager == null) {
                        Log.e(TAG, "FavoritesManager is null");
                        showToastOnMainThread(context, "❌ Error: Failed to initialize favorites");
                        return;
                    }

                    // Check if already favorited
                    boolean isAlreadyFavorite = favoritesManager.isFavorite(verse);
                    Log.d(TAG, "Is already favorite: " + isAlreadyFavorite);

                    if (isAlreadyFavorite) {
                        Log.d(TAG, "Verse already in favorites: " + verseReference);
                        showToastOnMainThread(context, "⭐ Already in favorites!");

                        // Update notification to show it's already favorited
                        updateNotificationToAlreadyFavorited(context, verse);

                    } else {
                        // Add to favorites
                        boolean added = favoritesManager.addToFavorites(verse);
                        Log.d(TAG, "Add to favorites result: " + added);

                        if (added) {
                            Log.d(TAG, "Successfully added verse to favorites: " + verseReference);
                            showToastOnMainThread(context, "❤️ Added to favorites: " + verseReference);

                            // Update the notification to show it's been favorited
                            updateNotificationToFavorited(context, verse);

                        } else {
                            Log.w(TAG, "Failed to add verse to favorites: " + verseReference);
                            showToastOnMainThread(context, "❌ Failed to add to favorites");
                        }
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing favorite action from notification", e);
                    e.printStackTrace();
                    showToastOnMainThread(context, "❌ Error adding to favorites");
                }
            }).start();

        } else {
            Log.w(TAG, "Unknown action: " + intent.getAction());
        }
    }

    private void showToastOnMainThread(Context context, String message) {
        // Ensure toast is shown on main thread
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Showed toast: " + message);
            } catch (Exception e) {
                Log.e(TAG, "Error showing toast", e);
            }
        });
    }

    private void updateNotificationToFavorited(Context context, VerseData verse) {
        try {
            Log.d(TAG, "Updating notification to show favorited status");

            // Create a new notification to replace the original one
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }

            // Create intent to open the specific verse
            Intent notificationIntent = new Intent(context, CategoryVersesActivity.class);
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
                    requestCode + 1000,
                    Intent.createChooser(shareIntent, "Share Verse"),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            // Create BigText content
            String bigTextContent = "🕌 " + verse.getArabicText() + "\n\n" +
                    "📖 " + verse.getEnglishTranslation() + "\n\n" +
                    "📍 " + verse.getReference();

            // Build updated notification
            androidx.core.app.NotificationCompat.Builder builder =
                    new androidx.core.app.NotificationCompat.Builder(context, "QuranVersesChannel")
                            .setSmallIcon(R.drawable.ic_book)
                            .setContentTitle("📖 " + verse.getCategory() + " ⭐")
                            .setContentText("Added to favorites!")
                            .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                                    .bigText(bigTextContent)
                                    .setBigContentTitle("📖 Ayah of the Hour • Favorited ⭐")
                                    .setSummaryText(verse.getReference()))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_REMINDER)
                            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                            .addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent)
                            .addAction(android.R.drawable.btn_star_big_on, "Favorited ✓", null); // Disabled button showing success

            notificationManager.notify(1001, builder.build());
            Log.d(TAG, "Successfully updated notification to show favorited status");

        } catch (Exception e) {
            Log.e(TAG, "Error updating notification after favoriting", e);
        }
    }

    private void updateNotificationToAlreadyFavorited(Context context, VerseData verse) {
        try {
            Log.d(TAG, "Updating notification to show already favorited status");

            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null");
                return;
            }

            // Create intent to open the specific verse
            Intent notificationIntent = new Intent(context, CategoryVersesActivity.class);
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
                    requestCode + 1000,
                    Intent.createChooser(shareIntent, "Share Verse"),
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            // Create BigText content
            String bigTextContent = "🕌 " + verse.getArabicText() + "\n\n" +
                    "📖 " + verse.getEnglishTranslation() + "\n\n" +
                    "📍 " + verse.getReference();

            // Build updated notification
            androidx.core.app.NotificationCompat.Builder builder =
                    new androidx.core.app.NotificationCompat.Builder(context, "QuranVersesChannel")
                            .setSmallIcon(R.drawable.ic_book)
                            .setContentTitle("📖 " + verse.getCategory() + " ⭐")
                            .setContentText("Already in favorites")
                            .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                                    .bigText(bigTextContent)
                                    .setBigContentTitle("📖 Ayah of the Hour • Already Favorited ⭐")
                                    .setSummaryText(verse.getReference()))
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_REMINDER)
                            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
                            .addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent)
                            .addAction(android.R.drawable.btn_star_big_on, "In Favorites ✓", null);

            notificationManager.notify(1001, builder.build());
            Log.d(TAG, "Successfully updated notification to show already favorited status");

        } catch (Exception e) {
            Log.e(TAG, "Error updating notification for already favorited verse", e);
        }
    }
}