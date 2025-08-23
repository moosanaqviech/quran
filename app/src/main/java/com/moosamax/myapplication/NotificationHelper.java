package com.moosamax.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class NotificationHelper {

    private static final String WORK_TAG = "verse_notifications";

    public static void scheduleNotifications(Context context, String interval,
                                             String startTime, String endTime) {
        Log.d("NotificationHelper", "Scheduling: " + interval);

        // Save preferences
        SharedPreferences prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("notifications_enabled", true);
        editor.putString("interval", interval);
        editor.putString("start_time", startTime);
        editor.putString("end_time", endTime);
        editor.apply();

        // Cancel existing work
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);

        // Get interval in hours
        int hours = 1;
        if (interval.contains("2 hours")) {
            hours = 2;
        } else if (interval.contains("4 hours")) {
            hours = 4;
        }

        // Create work request
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                VerseNotificationWorker.class,
                hours, TimeUnit.HOURS
        )
                .addTag(WORK_TAG)
                .build();

        // Schedule work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
        );

        Log.d("NotificationHelper", "Notifications scheduled successfully");
    }

    public static void cancelNotifications(Context context) {
        Log.d("NotificationHelper", "Cancelling notifications");

        // Update preferences
        SharedPreferences prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("notifications_enabled", false).apply();

        // Cancel work
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);
    }
}