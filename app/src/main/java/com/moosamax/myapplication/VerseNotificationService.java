/*
package com.moosamax.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class VerseNotificationService extends Service {
    private static final String TAG = "VerseNotificationService";
    private static final String CHANNEL_ID = "QuranVersesChannel";
    private static final String CHANNEL_NAME = "Quran Verses Notifications";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle notification in background thread to avoid ANR
        new Thread(() -> {
            if (intent != null && intent.getAction() != null) {
                switch (intent.getAction()) {
                    case "SEND_VERSE_NOTIFICATION":
                        sendVerseNotification();
                        break;
                }
            }
            // Stop the service after completing work
            stopSelf();
        }).start();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);

            // Channel for verse notifications
            NotificationChannel verseChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            verseChannel.setDescription("Daily Quran verses and Islamic reminders");
            verseChannel.enableVibration(true);
            verseChannel.setVibrationPattern(new long[]{0, 250, 250, 250});
            manager.createNotificationChannel(verseChannel);
        }
    }

    private void sendVerseNotification() {
        try {
            Log.d(TAG, "Preparing to send verse notification");

            // Initialize repository if needed
            VerseRepository.getInstance(this).initialize();

            // Get a random verse
            VerseData verse = VerseRepository.getRandomVerse();
            if (verse == null) {
                Log.e(TAG, "No verse available for notification");
                return;
            }

            Log.d(TAG, "Selected verse for notification: " + verse.getReference());

            // Create intent to open specific verse (DEEP LINK)
            Intent notificationIntent = new Intent(this, CategoryVersesActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationIntent.putExtra("CATEGORY_NAME", verse.getCategory());
            notificationIntent.putExtra("verse_reference", verse.getReference());
            notificationIntent.putExtra("from_notification", true);

            // Create unique request code based on verse reference to avoid intent caching
            int requestCode = verse.getReference().hashCode();

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    requestCode,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Create action buttons for the notification
            PendingIntent shareIntent = createShareIntent(verse, requestCode + 1);
            PendingIntent favoriteIntent = createFavoriteIntent(verse, requestCode + 2);

            // Build the enhanced notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_book)
                    .setContentTitle("📖 Ayah of the Hour")
                    .setContentText(verse.getEnglishTranslation())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(verse.getEnglishTranslation() + "\n\n— " + verse.getReference())
                            .setBigContentTitle("📖 " + verse.getCategory()))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            // Add action buttons if they were created successfully
            if (shareIntent != null) {
                builder.addAction(android.R.drawable.ic_menu_share, "Share", shareIntent);
                Log.d(TAG, "Added share action to notification");
            }

            if (favoriteIntent != null) {
                builder.addAction(android.R.drawable.btn_star_big_off, "Save", favoriteIntent);
                Log.d(TAG, "Added favorite action to notification");
            }

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
                Log.d(TAG, "Notification sent successfully for verse: " + verse.getReference());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sending verse notification", e);
        }
    }

    private PendingIntent createShareIntent(VerseData verse, int requestCode) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String shareText = verse.getArabicText() + "\n\n" +
                verse.getEnglishTranslation() + "\n\n" +
                "— " + verse.getReference() + "\n" +
                "Category: " + verse.getCategory() + "\n\n" +
                "Shared from Quran Verses App";

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Quran Verse - " + verse.getReference());

        return PendingIntent.getActivity(
                this,
                requestCode,
                Intent.createChooser(shareIntent, "Share Verse"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private PendingIntent createFavoriteIntent(VerseData verse, int requestCode) {
        try {
            Intent favoriteIntent = new Intent(this, FavoriteActionReceiver.class);
            favoriteIntent.setAction("ADD_TO_FAVORITES");
            favoriteIntent.putExtra("verse_reference", verse.getReference());
            favoriteIntent.putExtra("verse_category", verse.getCategory());
            favoriteIntent.putExtra("verse_arabic", verse.getArabicText());
            favoriteIntent.putExtra("verse_english", verse.getEnglishTranslation());
            favoriteIntent.putExtra("verse_origin", verse.getOrigin());

            Log.d(TAG, "Creating favorite intent for verse: " + verse.getReference());

            return PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    favoriteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating favorite intent", e);
            return null;
        }
    }
}*/
