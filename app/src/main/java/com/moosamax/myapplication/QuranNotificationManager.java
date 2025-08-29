// PURE WORKMANAGER SOLUTION - Single worker that reschedules itself

// 1. NEW QuranNotificationManager.java - Pure WorkManager approach
package com.moosamax.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class QuranNotificationManager {
    private static final String TAG = "QuranNotificationMgr";
    private static final String PREFS_NAME = "QuranNotificationPrefs";
    private static final String WORK_NAME = "QuranNotifications";

    // Preference keys
    private static final String PREF_ENABLED = "notifications_enabled";
    private static final String PREF_START_HOUR = "start_hour";
    private static final String PREF_START_MINUTE = "start_minute";
    private static final String PREF_END_HOUR = "end_hour";
    private static final String PREF_END_MINUTE = "end_minute";
    private static final String PREF_INTERVAL_MINUTES = "interval_minutes";

    // Available intervals
    public static final int[] INTERVALS = {2, 5, 10, 15, 30, 60, 120, 180, 240, 360, 480, 720};

    public static String[] getIntervalNames() {
        return new String[]{
                "Every 2 minutes",
                "Every 5 minutes",
                "Every 10 minutes",
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

    /**
     * Start notifications using pure WorkManager
     */
    public static void startNotifications(Context context, int startHour, int startMinute,
                                          int endHour, int endMinute, int intervalMinutes) {

        Log.d(TAG, String.format("Starting notifications: %02d:%02d to %02d:%02d every %d minutes",
                startHour, startMinute, endHour, endMinute, intervalMinutes));

        // Save settings
        saveSettings(context, startHour, startMinute, endHour, endMinute, intervalMinutes, true);

        // Cancel any existing work
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);

        // Create input data for worker
        Data inputData = new Data.Builder()
                .putInt("start_hour", startHour)
                .putInt("start_minute", startMinute)
                .putInt("end_hour", endHour)
                .putInt("end_minute", endMinute)
                .putInt("interval_minutes", intervalMinutes)
                .putBoolean("is_first_run", true)
                .build();

        // Create constraints (minimal for better reliability)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiresStorageNotLow(false)
                .build();

        // Schedule first worker immediately
        OneTimeWorkRequest initialWork = new OneTimeWorkRequest.Builder(QuranNotificationWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .addTag("quran_notifications")
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                initialWork
        );

        Log.d(TAG, "Initial notification work scheduled");
    }

    /**
     * Stop all notifications
     */
    public static void stopNotifications(Context context) {
        Log.d(TAG, "Stopping notifications");

        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        WorkManager.getInstance(context).cancelAllWorkByTag("quran_notifications");

        // Update settings
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_ENABLED, false).apply();

        Log.d(TAG, "All notification work cancelled");
    }

    // Helper methods
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_ENABLED, false);
    }

    public static int[] getSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new int[]{
                prefs.getInt(PREF_START_HOUR, 9),
                prefs.getInt(PREF_START_MINUTE, 0),
                prefs.getInt(PREF_END_HOUR, 21),
                prefs.getInt(PREF_END_MINUTE, 0),
                prefs.getInt(PREF_INTERVAL_MINUTES, 60)
        };
    }

    public static String getFormattedSchedule(Context context) {
        int[] settings = getSettings(context);
        String intervalName = getIntervalName(settings[4]);
        return String.format("%s from %02d:%02d to %02d:%02d",
                intervalName, settings[0], settings[1], settings[2], settings[3]);
    }

    public static String getIntervalName(int minutes) {
        for (int i = 0; i < INTERVALS.length; i++) {
            if (INTERVALS[i] == minutes) {
                return getIntervalNames()[i];
            }
        }
        return "Every " + minutes + " minutes";
    }

    private static void saveSettings(Context context, int startHour, int startMinute,
                                     int endHour, int endMinute, int intervalMinutes, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_ENABLED, enabled)
                .putInt(PREF_START_HOUR, startHour)
                .putInt(PREF_START_MINUTE, startMinute)
                .putInt(PREF_END_HOUR, endHour)
                .putInt(PREF_END_MINUTE, endMinute)
                .putInt(PREF_INTERVAL_MINUTES, intervalMinutes)
                .apply();
    }

    /**
     * Self-scheduling WorkManager Worker
     * This is the key: each worker schedules the next one
     */
    public static class QuranNotificationWorker extends Worker {
        private static final String TRACKING_PREFS = "notification_tracking";
        private static final String LAST_NOTIFICATION_KEY = "last_notification_time";

        public QuranNotificationWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public Result doWork() {
            Context context = getApplicationContext();

            try {
                Log.d(TAG, "QuranNotificationWorker executing");

                // Get work parameters
                Data inputData = getInputData();
                int startHour = inputData.getInt("start_hour", 9);
                int startMinute = inputData.getInt("start_minute", 0);
                int endHour = inputData.getInt("end_hour", 21);
                int endMinute = inputData.getInt("end_minute", 0);
                int intervalMinutes = inputData.getInt("interval_minutes", 60);
                boolean isFirstRun = inputData.getBoolean("is_first_run", false);

                Log.d(TAG, String.format("Worker params: %02d:%02d to %02d:%02d, interval=%d min, firstRun=%s",
                        startHour, startMinute, endHour, endMinute, intervalMinutes, isFirstRun));

                // Check if notifications are still enabled
                if (!isEnabled(context)) {
                    Log.d(TAG, "Notifications disabled, stopping worker chain");
                    return Result.success();
                }

                // Check if we're within the notification period
                if (!isWithinPeriod(context, startHour, startMinute, endHour, endMinute)) {
                    Log.d(TAG, "Outside notification period");

                    // Schedule next check for start of next period
                    scheduleNextCheck(context, startHour, startMinute, endHour, endMinute, intervalMinutes, true);
                    return Result.success();
                }

                // For first run, just schedule next check without sending notification
                if (isFirstRun) {
                    Log.d(TAG, "First run, scheduling next check without sending notification");
                    scheduleNextCheck(context, startHour, startMinute, endHour, endMinute, intervalMinutes, false);
                    return Result.success();
                }

                // Check if enough time has passed since last notification
                if (!shouldSendNotification(context, intervalMinutes)) {
                    Log.d(TAG, "Not enough time passed since last notification");
                    scheduleNextCheck(context, startHour, startMinute, endHour, endMinute, intervalMinutes, false);
                    return Result.success();
                }

                // Send notification
                boolean notificationSent = sendNotification(context);

                if (notificationSent) {
                    // Update last notification time
                    SharedPreferences prefs = context.getSharedPreferences(TRACKING_PREFS, Context.MODE_PRIVATE);
                    prefs.edit().putLong(LAST_NOTIFICATION_KEY, System.currentTimeMillis()).apply();
                    Log.d(TAG, "Notification sent and timestamp updated");
                }

                // Schedule next check
                scheduleNextCheck(context, startHour, startMinute, endHour, endMinute, intervalMinutes, false);

                return Result.success();

            } catch (Exception e) {
                Log.e(TAG, "Error in QuranNotificationWorker", e);

                // On error, try to reschedule to keep the chain going
                try {
                    int[] settings = getSettings(context);
                    scheduleNextCheck(context, settings[0], settings[1], settings[2], settings[3], settings[4], false);
                } catch (Exception e2) {
                    Log.e(TAG, "Failed to reschedule after error", e2);
                }

                return Result.failure();
            }
        }

        private boolean isWithinPeriod(Context context, int startHour, int startMinute, int endHour, int endMinute) {
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);

            int currentMinutes = currentHour * 60 + currentMinute;
            int startMinutes = startHour * 60 + startMinute;
            int endMinutes = endHour * 60 + endMinute;

            Log.d(TAG, String.format("Time check: current=%02d:%02d (%d), start=%02d:%02d (%d), end=%02d:%02d (%d)",
                    currentHour, currentMinute, currentMinutes, startHour, startMinute, startMinutes, endHour, endMinute, endMinutes));

            boolean withinPeriod;
            if (endMinutes <= startMinutes) {
                // Overnight period (e.g., 22:00 to 08:00)
                withinPeriod = currentMinutes >= startMinutes || currentMinutes <= endMinutes;
            } else {
                // Same day period (e.g., 09:00 to 21:00)
                withinPeriod = currentMinutes >= startMinutes && currentMinutes <= endMinutes;
            }

            Log.d(TAG, "Within period: " + withinPeriod);
            return withinPeriod;
        }

        private boolean shouldSendNotification(Context context, int intervalMinutes) {
            SharedPreferences prefs = context.getSharedPreferences(TRACKING_PREFS, Context.MODE_PRIVATE);
            long lastNotificationTime = prefs.getLong(LAST_NOTIFICATION_KEY, 0);
            long currentTime = System.currentTimeMillis();

            long intervalMillis = intervalMinutes * 60 * 1000L;
            long timeSinceLastNotification = currentTime - lastNotificationTime;
            long minutesSince = timeSinceLastNotification / (60 * 1000);

            Log.d(TAG, String.format("Interval check: last=%d, current=%d, interval=%d min, since=%d min, should_send=%s",
                    lastNotificationTime, currentTime, intervalMinutes, minutesSince, (timeSinceLastNotification >= intervalMillis)));

            return timeSinceLastNotification >= intervalMillis;
        }

        private boolean sendNotification(Context context) {
            try {
                Log.d(TAG, "Sending notification");

                // Initialize repository
                VerseRepository.getInstance(context).initialize();
                VerseData verse = VerseRepository.getRandomVerse();

                if (verse == null) {
                    Log.e(TAG, "No verse available for notification");
                    return false;
                }

                // Create and send notification
                createNotification(context, verse);
                Log.d(TAG, "Notification sent successfully: " + verse.getReference());
                return true;

            } catch (Exception e) {
                Log.e(TAG, "Error sending notification", e);
                return false;
            }
        }

        private void createNotification(Context context, VerseData verse) {
            android.app.NotificationManager notificationManager =
                    (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Create channel if needed
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                android.app.NotificationChannel channel = new android.app.NotificationChannel(
                        "QuranVersesChannel",
                        "Quran Verses",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setDescription("Daily Quran verses and reminders");
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            // Create intent to open app
            android.content.Intent notificationIntent = new android.content.Intent(context, MainActivity.class);
            notificationIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);

            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    verse.getReference().hashCode(),
                    notificationIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            // Build notification
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
                            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_REMINDER);

            notificationManager.notify(1001, builder.build());
        }

        private void scheduleNextCheck(Context context, int startHour, int startMinute, int endHour, int endMinute,
                                       int intervalMinutes, boolean isForNextPeriod) {

            try {
                // Calculate delay until next check
                long delayMinutes;

                if (isForNextPeriod) {
                    // Calculate time until next period starts
                    delayMinutes = calculateMinutesUntilNextPeriod(startHour, startMinute);
                    Log.d(TAG, "Scheduling for next period in " + delayMinutes + " minutes");
                } else {
                    // For frequent intervals (< 15 minutes), check more often for precision
                    if (intervalMinutes < 15) {
                        delayMinutes = Math.min(intervalMinutes, 5); // Check every 5 minutes max
                    } else {
                        delayMinutes = Math.min(intervalMinutes / 2, 15); // Check at half interval, max 15 min
                    }
                    Log.d(TAG, "Scheduling next check in " + delayMinutes + " minutes");
                }

                // Create data for next worker
                Data nextData = new Data.Builder()
                        .putInt("start_hour", startHour)
                        .putInt("start_minute", startMinute)
                        .putInt("end_hour", endHour)
                        .putInt("end_minute", endMinute)
                        .putInt("interval_minutes", intervalMinutes)
                        .putBoolean("is_first_run", false)
                        .build();

                // Create constraints
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .setRequiresStorageNotLow(false)
                        .build();

                // Schedule next worker
                OneTimeWorkRequest nextWork = new OneTimeWorkRequest.Builder(QuranNotificationWorker.class)
                        .setInputData(nextData)
                        .setConstraints(constraints)
                        .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                        .addTag("quran_notifications")
                        .build();

                WorkManager.getInstance(context).enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        nextWork
                );

                Log.d(TAG, "Next worker scheduled for " + delayMinutes + " minutes from now");

            } catch (Exception e) {
                Log.e(TAG, "Error scheduling next check", e);
            }
        }

        private long calculateMinutesUntilNextPeriod(int startHour, int startMinute) {
            Calendar now = Calendar.getInstance();
            Calendar nextStart = Calendar.getInstance();

            nextStart.set(Calendar.HOUR_OF_DAY, startHour);
            nextStart.set(Calendar.MINUTE, startMinute);
            nextStart.set(Calendar.SECOND, 0);
            nextStart.set(Calendar.MILLISECOND, 0);

            // If start time has passed today, move to tomorrow
            if (nextStart.getTimeInMillis() <= now.getTimeInMillis()) {
                nextStart.add(Calendar.DAY_OF_YEAR, 1);
            }

            long diffMillis = nextStart.getTimeInMillis() - now.getTimeInMillis();
            return diffMillis / (60 * 1000);
        }
    }
}