/*
// REPLACE NotificationScheduler.java with this WorkManager-only version

package com.moosamax.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {
    private static final String TAG = "NotificationScheduler";
    private static final String PREFS_NAME = "QuranNotificationPrefs";
    private static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String PREF_START_HOUR = "start_hour";
    private static final String PREF_START_MINUTE = "start_minute";
    private static final String PREF_END_HOUR = "end_hour";
    private static final String PREF_END_MINUTE = "end_minute";
    private static final String PREF_INTERVAL_MINUTES = "interval_minutes";

    // Notification interval options (in minutes) - WorkManager optimized
    public static final int INTERVAL_15_MINUTES = 15;
    public static final int INTERVAL_30_MINUTES = 30;
    public static final int INTERVAL_1_HOUR = 60;
    public static final int INTERVAL_2_HOURS = 120;
    public static final int INTERVAL_3_HOURS = 180;
    public static final int INTERVAL_4_HOURS = 240;
    public static final int INTERVAL_6_HOURS = 360;
    public static final int INTERVAL_8_HOURS = 480;
    public static final int INTERVAL_12_HOURS = 720;

    */
/**
     * Schedule periodic verse notifications using WorkManager ONLY
     * This is 100% compatible with Android 15 and all restrictions
     *//*

    public static boolean scheduleCustomIntervalNotifications(Context context, int startHour, int startMinute,
                                                              int endHour, int endMinute, int intervalMinutes) {

        Log.d(TAG, String.format("Scheduling WorkManager notifications: %02d:%02d to %02d:%02d every %d minutes",
                startHour, startMinute, endHour, endMinute, intervalMinutes));

        // Cancel any existing work first
        cancelAllVerseNotifications(context);

        try {
            // Convert interval to hours for WorkManager (minimum 15 minutes)
            long intervalMinutesForWork = Math.max(15, intervalMinutes);

            // Create work data
            Data inputData = new Data.Builder()
                    .putInt("start_hour", startHour)
                    .putInt("start_minute", startMinute)
                    .putInt("end_hour", endHour)
                    .putInt("end_minute", endMinute)
                    .putInt("interval_minutes", intervalMinutes)
                    .build();

            // Create constraints
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network needed
                    .setRequiresBatteryNotLow(false) // Can run on low battery
                    .setRequiresCharging(false) // Can run on battery
                    .setRequiresDeviceIdle(false) // Can run while device is active
                    .setRequiresStorageNotLow(false) // Can run with low storage
                    .build();

            // For intervals less than 1 hour, use multiple periodic workers
            if (intervalMinutes < 60) {
                scheduleFrequentNotifications(context, inputData, constraints, intervalMinutes);
            } else {
                // For hourly or longer intervals, use single periodic worker
                scheduleHourlyNotifications(context, inputData, constraints, intervalMinutes);
            }

            // Save preferences
            saveNotificationSettings(context, startHour, startMinute, endHour, endMinute, intervalMinutes, true);

            Log.d(TAG, "WorkManager notifications scheduled successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling WorkManager notifications", e);
            return false;
        }
    }

    */
/**
     * Schedule frequent notifications (less than 1 hour intervals)
     * Uses multiple workers to achieve more frequent notifications
     *//*

    private static void scheduleFrequentNotifications(Context context, Data inputData,
                                                      Constraints constraints, int intervalMinutes) {

        // For frequent notifications, create multiple workers with staggered start times
        int workersNeeded = Math.max(1, 60 / intervalMinutes); // How many workers to cover 1 hour

        for (int i = 0; i < workersNeeded; i++) {
            long initialDelay = i * intervalMinutes; // Stagger the workers

            Data workerData = new Data.Builder()
                    .putAll(inputData)
                    .putInt("worker_id", i)
                    .putLong("initial_delay_minutes", initialDelay)
                    .build();

            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    VerseNotificationWorker.class,
                    1, TimeUnit.HOURS // Minimum interval for periodic work
            )
                    .setInputData(workerData)
                    .setConstraints(constraints)
                    .setInitialDelay(initialDelay, TimeUnit.MINUTES)
                    .addTag("verse_notifications_frequent")
                    .addTag("worker_" + i)
                    .build();

            WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(
                            "verse_notifications_worker_" + i,
                            ExistingPeriodicWorkPolicy.REPLACE,
                            workRequest
                    );
        }

        Log.d(TAG, "Scheduled " + workersNeeded + " frequent notification workers");
    }

    */
/**
     * Schedule hourly or longer interval notifications
     *//*

    private static void scheduleHourlyNotifications(Context context, Data inputData,
                                                    Constraints constraints, int intervalMinutes) {

        long intervalHours = Math.max(1, intervalMinutes / 60);

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                VerseNotificationWorker.class,
                intervalHours, TimeUnit.HOURS
        )
                .setInputData(inputData)
                .setConstraints(constraints)
                .addTag("verse_notifications_hourly")
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        "verse_notifications_hourly",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        workRequest
                );

        Log.d(TAG, "Scheduled hourly notification worker (every " + intervalHours + " hours)");
    }

    */
/**
     * WorkManager Worker class that handles sending notifications
     * This replaces the AlarmManager + Service approach completely
     *//*

    public static class VerseNotificationWorker extends Worker {

        public VerseNotificationWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public Result doWork() {
            Context context = getApplicationContext();

            try {
                Log.d(TAG, "VerseNotificationWorker executing");

                // Get work parameters
                Data inputData = getInputData();
                int startHour = inputData.getInt("start_hour", 9);
                int startMinute = inputData.getInt("start_minute", 0);
                int endHour = inputData.getInt("end_hour", 21);
                int endMinute = inputData.getInt("end_minute", 0);
                int intervalMinutes = inputData.getInt("interval_minutes", 60);
                int workerId = inputData.getInt("worker_id", 0);

                // Check if notifications are still enabled
                if (!areNotificationsEnabled(context)) {
                    Log.d(TAG, "Notifications disabled, worker stopping");
                    return Result.success();
                }

                // Check if current time is within notification period
                if (!isWithinTimePeriod(context, startHour, startMinute, endHour, endMinute)) {
                    Log.d(TAG, "Outside notification period, skipping");
                    return Result.success();
                }

                // For frequent notifications, check if it's time for this specific worker
                if (intervalMinutes < 60) {
                    if (!isTimeForWorker(workerId, intervalMinutes)) {
                        Log.d(TAG, "Not time for worker " + workerId + ", skipping");
                        return Result.success();
                    }
                }

                // Send the notification
                sendNotificationDirectly(context);

                Log.d(TAG, "VerseNotificationWorker completed successfully");
                return Result.success();

            } catch (Exception e) {
                Log.e(TAG, "Error in VerseNotificationWorker", e);
                return Result.retry(); // Retry on failure
            }
        }

        */
/**
         * Check if current time is within the specified notification period
         *//*

        private boolean isWithinTimePeriod(Context context, int startHour, int startMinute,
                                           int endHour, int endMinute) {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);

            int currentTimeMinutes = currentHour * 60 + currentMinute;
            int startTimeMinutes = startHour * 60 + startMinute;
            int endTimeMinutes = endHour * 60 + endMinute;

            if (endTimeMinutes <= startTimeMinutes) {
                // Crosses midnight (e.g., 22:00 to 08:00)
                return currentTimeMinutes >= startTimeMinutes || currentTimeMinutes <= endTimeMinutes;
            } else {
                // Same day (e.g., 09:00 to 21:00)
                return currentTimeMinutes >= startTimeMinutes && currentTimeMinutes <= endTimeMinutes;
            }
        }

        */
/**
         * For frequent notifications, check if it's time for this specific worker to run
         *//*

        private boolean isTimeForWorker(int workerId, int intervalMinutes) {
            Calendar now = Calendar.getInstance();
            int currentMinute = now.get(Calendar.MINUTE);

            // Each worker handles specific minutes within the hour
            int minutesPerWorker = intervalMinutes;
            int targetMinute = (workerId * minutesPerWorker) % 60;

            // Allow some tolerance (±2 minutes)
            int tolerance = 2;
            return Math.abs(currentMinute - targetMinute) <= tolerance ||
                    Math.abs((currentMinute + 60) - targetMinute) <= tolerance ||
                    Math.abs(currentMinute - (targetMinute + 60)) <= tolerance;
        }

        */
/**
         * Send notification directly from WorkManager (no service needed)
         *//*

        private void sendNotificationDirectly(Context context) {
            try {
                Log.d(TAG, "Sending notification directly from WorkManager");

                // Initialize repository
                VerseRepository.getInstance(context).initialize();

                // Get a random verse
                VerseData verse = VerseRepository.getRandomVerse();
                if (verse == null) {
                    Log.e(TAG, "No verse available for notification");
                    return;
                }

                // Create notification
                createAndSendNotification(context, verse);

            } catch (Exception e) {
                Log.e(TAG, "Error sending notification from WorkManager", e);
            }
        }

        */
/**
         * Create and send the notification
         *//*

        private void createAndSendNotification(Context context, VerseData verse) {
            try {
                android.app.NotificationManager notificationManager =
                        (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager == null) return;

                // Create notification channel if needed (Android 8+)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    android.app.NotificationChannel channel = new android.app.NotificationChannel(
                            "QuranVersesChannel",
                            "Quran Verses Notifications",
                            android.app.NotificationManager.IMPORTANCE_DEFAULT
                    );
                    channel.setDescription("Daily Quran verses and Islamic reminders");
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                    notificationManager.createNotificationChannel(channel);
                }

                // Create intent to open the app
                android.content.Intent notificationIntent = new android.content.Intent(context, MainActivity.class);
                notificationIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                notificationIntent.putExtra("from_notification", true);

                android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                        context,
                        verse.getReference().hashCode(),
                        notificationIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                                android.app.PendingIntent.FLAG_IMMUTABLE
                );

                // Create the notification
                androidx.core.app.NotificationCompat.Builder builder =
                        new androidx.core.app.NotificationCompat.Builder(context, "QuranVersesChannel")
                                .setSmallIcon(R.drawable.ic_book)
                                .setContentTitle("📖 Ayah of the Hour")
                                .setContentText(verse.getEnglishTranslation())
                                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                                        .bigText(verse.getEnglishTranslation() + "\n\n— " + verse.getReference())
                                        .setBigContentTitle("📖 " + verse.getCategory()))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true)
                                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_REMINDER)
                                .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC);

                // Send the notification
                notificationManager.notify(1001, builder.build());
                Log.d(TAG, "Notification sent successfully: " + verse.getReference());

            } catch (Exception e) {
                Log.e(TAG, "Error creating notification", e);
            }
        }
    }

    */
/**
     * Cancel all notifications using WorkManager only
     *//*

    public static void cancelAllVerseNotifications(Context context) {
        Log.d(TAG, "Cancelling all WorkManager notifications");

        try {
            WorkManager workManager = WorkManager.getInstance(context);

            // Cancel all notification-related work
            workManager.cancelAllWorkByTag("verse_notifications_frequent");
            workManager.cancelAllWorkByTag("verse_notifications_hourly");
            workManager.cancelAllWorkByTag("notification_backup_check");

            // Cancel individual workers
            for (int i = 0; i < 4; i++) { // Cancel up to 4 workers (covers 15-minute intervals)
                workManager.cancelUniqueWork("verse_notifications_worker_" + i);
            }
            workManager.cancelUniqueWork("verse_notifications_hourly");

            Log.d(TAG, "All WorkManager notifications cancelled");

        } catch (Exception e) {
            Log.e(TAG, "Error cancelling WorkManager notifications", e);
        }

        // Update preferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, false).apply();
    }

    */
/**
     * Enhanced scheduling method with WorkManager backup
     *//*

    public static boolean scheduleCustomIntervalNotificationsWithBackup(Context context,
                                                                        int startHour, int startMinute, int endHour, int endMinute, int intervalMinutes) {

        // Just use the WorkManager-only approach (no backup needed)
        return scheduleCustomIntervalNotifications(context, startHour, startMinute,
                endHour, endMinute, intervalMinutes);
    }

    // Helper methods remain the same...

    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false);
    }

    public static int[] getNotificationTimePeriod(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new int[]{
                prefs.getInt(PREF_START_HOUR, 9),
                prefs.getInt(PREF_START_MINUTE, 0),
                prefs.getInt(PREF_END_HOUR, 21),
                prefs.getInt(PREF_END_MINUTE, 0)
        };
    }

    public static int getNotificationInterval(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_INTERVAL_MINUTES, INTERVAL_1_HOUR);
    }

    public static boolean isWithinNotificationPeriod(Context context) {
        if (!areNotificationsEnabled(context)) {
            return false;
        }

        int[] times = getNotificationTimePeriod(context);
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);

        int currentTimeMinutes = currentHour * 60 + currentMinute;
        int startTimeMinutes = times[0] * 60 + times[1];
        int endTimeMinutes = times[2] * 60 + times[3];

        if (endTimeMinutes <= startTimeMinutes) {
            return currentTimeMinutes >= startTimeMinutes || currentTimeMinutes <= endTimeMinutes;
        } else {
            return currentTimeMinutes >= startTimeMinutes && currentTimeMinutes <= endTimeMinutes;
        }
    }

    public static String getFormattedTimePeriod(Context context) {
        int[] times = getNotificationTimePeriod(context);
        int interval = getNotificationInterval(context);
        String intervalName = getIntervalDisplayName(interval);

        return String.format("%s from %02d:%02d to %02d:%02d",
                intervalName, times[0], times[1], times[2], times[3]);
    }

    public static int calculateNotificationsPerDay(Context context) {
        if (!areNotificationsEnabled(context)) {
            return 0;
        }

        int[] times = getNotificationTimePeriod(context);
        int intervalMinutes = getNotificationInterval(context);

        int startTimeMinutes = times[0] * 60 + times[1];
        int endTimeMinutes = times[2] * 60 + times[3];

        int durationMinutes;
        if (endTimeMinutes <= startTimeMinutes) {
            durationMinutes = (24 * 60 - startTimeMinutes) + endTimeMinutes;
        } else {
            durationMinutes = endTimeMinutes - startTimeMinutes;
        }

        int notificationCount = (durationMinutes / intervalMinutes) + 1;
        return Math.max(1, Math.min(notificationCount, 50)); // Cap at reasonable number
    }

    // Interval helper methods
    public static int[] getIntervalValues() {
        return new int[]{
                INTERVAL_15_MINUTES,
                INTERVAL_30_MINUTES,
                INTERVAL_1_HOUR,
                INTERVAL_2_HOURS,
                INTERVAL_3_HOURS,
                INTERVAL_4_HOURS,
                INTERVAL_6_HOURS,
                INTERVAL_8_HOURS,
                INTERVAL_12_HOURS
        };
    }

    public static String[] getIntervalDisplayNames() {
        return new String[]{
                "Every 15 minutes",
                "Every 30 minutes",
                "Every hour",
                "Every 2 hours",
                "Every 3 hours",
                "Every 4 hours",
                "Every 6 hours",
                "Every 8 hours",
                "Every 12 hours"
        };
    }

    public static String getIntervalDisplayName(int intervalMinutes) {
        switch (intervalMinutes) {
            case INTERVAL_15_MINUTES: return "Every 15 minutes";
            case INTERVAL_30_MINUTES: return "Every 30 minutes";
            case INTERVAL_1_HOUR: return "Every hour";
            case INTERVAL_2_HOURS: return "Every 2 hours";
            case INTERVAL_3_HOURS: return "Every 3 hours";
            case INTERVAL_4_HOURS: return "Every 4 hours";
            case INTERVAL_6_HOURS: return "Every 6 hours";
            case INTERVAL_8_HOURS: return "Every 8 hours";
            case INTERVAL_12_HOURS: return "Every 12 hours";
            default: return "Every " + intervalMinutes + " minutes";
        }
    }

    private static void saveNotificationSettings(Context context, int startHour, int startMinute,
                                                 int endHour, int endMinute, int intervalMinutes, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled)
                .putInt(PREF_START_HOUR, startHour)
                .putInt(PREF_START_MINUTE, startMinute)
                .putInt(PREF_END_HOUR, endHour)
                .putInt(PREF_END_MINUTE, endMinute)
                .putInt(PREF_INTERVAL_MINUTES, intervalMinutes)
                .apply();
    }
}*/
