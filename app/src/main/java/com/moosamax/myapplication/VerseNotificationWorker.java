package com.moosamax.myapplication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.Calendar;
import java.util.Random;

public class VerseNotificationWorker extends Worker {

    private static final String CHANNEL_ID = "verse_notifications";
    private static final int NOTIFICATION_ID = 1001;

    // Sample verses - replace with your actual verse data
    private static final String[][] VERSES = {
            {"وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا", "And whoever fears Allah - He will make for him a way out.", "At-Talaq 65:2"},
            {"فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", "For indeed, with hardship [will be] ease.", "Ash-Sharh 94:5"},
            {"رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً", "Our Lord, give us good in this world and good in the hereafter.", "Al-Baqarah 2:201"}
    };

    public VerseNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Check if notifications are enabled
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", false);

        if (!notificationsEnabled) {
            return Result.success();
        }

        // Check if current time is within notification period
        if (isWithinNotificationPeriod()) {
            showVerseNotification();
        }

        return Result.success();
    }

    private boolean isWithinNotificationPeriod() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        String startTime = prefs.getString("start_time", "09:00");
        String endTime = prefs.getString("end_time", "21:00");

        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        int startHour = Integer.parseInt(startTime.split(":")[0]);
        int endHour = Integer.parseInt(endTime.split(":")[0]);

        return currentHour >= startHour && currentHour <= endHour;
    }

    private void showVerseNotification() {
        createNotificationChannel();

        // Get random verse
        Random random = new Random();
        String[] verse = VERSES[random.nextInt(VERSES.length)];

        // Create intent to open app
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
                .setContentTitle("Quran Verse")
                .setContentText(verse[1]) // English translation
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(verse[0] + "\n\n" + verse[1] + "\n\n" + verse[2]))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Verse Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Daily Quran verse notifications");

            NotificationManager notificationManager =
                    getApplicationContext().getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}