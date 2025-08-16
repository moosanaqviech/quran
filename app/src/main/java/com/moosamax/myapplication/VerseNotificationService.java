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
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class VerseNotificationService extends Service {

    private static final String CHANNEL_ID = "QuranVersesChannel";
    private static final String CHANNEL_NAME = "Quran Verses Notifications";
    private static final String FOREGROUND_CHANNEL_ID = "QuranServiceChannel";
    private static final String FOREGROUND_CHANNEL_NAME = "Quran Service";
    private static final int NOTIFICATION_ID = 1001;
    private static final int FOREGROUND_NOTIFICATION_ID = 1002;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start foreground service immediately to avoid the exception
        startForegroundService();

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "SEND_VERSE_NOTIFICATION":
                    sendVerseNotification();
                    break;
            }
        }

        // Stop the service after sending notification
        stopForeground(true);
        stopSelf();

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundService() {
        // Create a simple foreground notification
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, mainIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        Notification foregroundNotification = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book)
                .setContentTitle("Quran Verses")
                .setContentText("Preparing daily verse...")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(FOREGROUND_NOTIFICATION_ID, foregroundNotification);
    }

    private void createNotificationChannels() {
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

            // Channel for foreground service
            NotificationChannel serviceChannel = new NotificationChannel(
                    FOREGROUND_CHANNEL_ID,
                    FOREGROUND_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Background service for sending verse notifications");
            serviceChannel.setShowBadge(false);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void sendVerseNotification() {
        VerseData verse = VerseRepository.getRandomVerse();

        // Intent to open the main activity when notification is tapped
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, mainIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_book) // You need to add this icon
                .setContentTitle("Ayah of the Hour")
                .setContentText(verse.getEnglishTranslation())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(verse.getArabicText() + "\n\n" +
                                verse.getEnglishTranslation() + "\n\n" +
                                "- " + verse.getReference()))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}