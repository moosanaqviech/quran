package com.moosamax.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import java.util.Calendar;

public class NotificationScheduler {

    private static final String PREFS_NAME = "QuranNotificationPrefs";
    private static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String PREF_START_HOUR = "start_hour";
    private static final String PREF_START_MINUTE = "start_minute";
    private static final String PREF_END_HOUR = "end_hour";
    private static final String PREF_END_MINUTE = "end_minute";
    private static final String PREF_INTERVAL_MINUTES = "interval_minutes";
    private static final int BASE_ALARM_REQUEST_CODE = 2000;

    // Notification interval options (in minutes)
    public static final int INTERVAL_15_MINUTES = 15;
    public static final int INTERVAL_30_MINUTES = 30;
    public static final int INTERVAL_45_MINUTES = 45;
    public static final int INTERVAL_1_HOUR = 60;
    public static final int INTERVAL_2_HOURS = 120;
    public static final int INTERVAL_3_HOURS = 180;
    public static final int INTERVAL_4_HOURS = 240;
    public static final int INTERVAL_6_HOURS = 360;
    public static final int INTERVAL_8_HOURS = 480;
    public static final int INTERVAL_12_HOURS = 720;

    /**
     * Get available notification intervals with display names
     */
    public static String[] getIntervalDisplayNames() {
        return new String[]{
                "Every 15 minutes",
                "Every 30 minutes",
                "Every 45 minutes",
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
     * Get available notification interval values (in minutes)
     */
    public static int[] getIntervalValues() {
        return new int[]{
                INTERVAL_15_MINUTES,
                INTERVAL_30_MINUTES,
                INTERVAL_45_MINUTES,
                INTERVAL_1_HOUR,
                INTERVAL_2_HOURS,
                INTERVAL_3_HOURS,
                INTERVAL_4_HOURS,
                INTERVAL_6_HOURS,
                INTERVAL_8_HOURS,
                INTERVAL_12_HOURS
        };
    }

    /**
     * Schedule verse notifications with custom interval within specified time period
     * @param context Application context
     * @param startHour Start hour (0-23)
     * @param startMinute Start minute (0-59)
     * @param endHour End hour (0-23)
     * @param endMinute End minute (0-59)
     * @param intervalMinutes Notification interval in minutes
     */
    public static boolean scheduleCustomIntervalNotifications(Context context, int startHour, int startMinute,
                                                              int endHour, int endMinute, int intervalMinutes) {

        // First cancel any existing notifications
        cancelAllVerseNotifications(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Check if we can schedule exact alarms
        if (!canScheduleExactAlarms(context, alarmManager)) {
            // Use inexact alarms as fallback
            return scheduleInexactCustomNotifications(context, alarmManager, startHour, startMinute,
                    endHour, endMinute, intervalMinutes);
        }

        // Calculate start and end times in minutes from midnight
        int startTimeMinutes = startHour * 60 + startMinute;
        int endTimeMinutes = endHour * 60 + endMinute;

        // Handle case where end time is next day (e.g., 22:00 to 08:00)
        boolean crossesMidnight = endTimeMinutes <= startTimeMinutes;

        // Schedule notifications at custom intervals
        int currentTimeMinutes = startTimeMinutes;
        int alarmId = 0;

        while (true) {
            int currentHour = (currentTimeMinutes / 60) % 24;
            int currentMinute = currentTimeMinutes % 60;

            // Check if we've reached the end time
            if (!crossesMidnight && currentTimeMinutes > endTimeMinutes) {
                break;
            } else if (crossesMidnight) {
                // For overnight periods, handle the wrap-around logic
                if (currentTimeMinutes >= (24 * 60)) {
                    currentTimeMinutes = currentTimeMinutes - (24 * 60);
                    currentHour = currentTimeMinutes / 60;
                    currentMinute = currentTimeMinutes % 60;

                    if (currentTimeMinutes > endTimeMinutes) {
                        break;
                    }
                }
            }

            // Schedule notification for this time
            scheduleNotificationAtTime(context, alarmManager, currentHour, currentMinute, alarmId, true);
            alarmId++;

            // Move to next interval
            currentTimeMinutes += intervalMinutes;

            // Safety check to avoid infinite loop (max 200 notifications per day)
            if (alarmId > 200) break;
        }

        // Save preferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_NOTIFICATIONS_ENABLED, true)
                .putInt(PREF_START_HOUR, startHour)
                .putInt(PREF_START_MINUTE, startMinute)
                .putInt(PREF_END_HOUR, endHour)
                .putInt(PREF_END_MINUTE, endMinute)
                .putInt(PREF_INTERVAL_MINUTES, intervalMinutes)
                .apply();

        return true;
    }

    /**
     * Fallback method for devices that don't allow exact alarms
     */
    private static boolean scheduleInexactCustomNotifications(Context context, AlarmManager alarmManager,
                                                              int startHour, int startMinute, int endHour,
                                                              int endMinute, int intervalMinutes) {

        // Use a shorter check interval for better accuracy with custom intervals
        int checkIntervalMinutes = Math.min(intervalMinutes / 4, 15);
        if (checkIntervalMinutes < 5) checkIntervalMinutes = 5; // Minimum 5 minutes

        Intent intent = new Intent(context, VerseNotificationReceiver.class);
        intent.putExtra("INEXACT_MODE", true);
        intent.putExtra("CUSTOM_INTERVAL", intervalMinutes);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_ALARM_REQUEST_CODE,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                        PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Set up the calendar for the first alarm
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, startMinute);
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If current time has passed, start from next occurrence
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.MINUTE, intervalMinutes);
        }

        // Use inexact repeating alarm with check interval
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                checkIntervalMinutes * 60 * 1000, // Check interval in milliseconds
                pendingIntent
        );

        // Save preferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_NOTIFICATIONS_ENABLED, true)
                .putInt(PREF_START_HOUR, startHour)
                .putInt(PREF_START_MINUTE, startMinute)
                .putInt(PREF_END_HOUR, endHour)
                .putInt(PREF_END_MINUTE, endMinute)
                .putInt(PREF_INTERVAL_MINUTES, intervalMinutes)
                .apply();

        return false; // Return false to indicate inexact scheduling was used
    }

    /**
     * Legacy method updated to use default hourly interval
     */
    public static boolean scheduleHourlyVerseNotifications(Context context, int startHour, int startMinute,
                                                           int endHour, int endMinute) {
        return scheduleCustomIntervalNotifications(context, startHour, startMinute, endHour, endMinute, INTERVAL_1_HOUR);
    }

    /**
     * Check if the app can schedule exact alarms
     */
    private static boolean canScheduleExactAlarms(Context context, AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            return alarmManager.canScheduleExactAlarms();
        }
        return true; // Pre-Android 12 devices can schedule exact alarms
    }

    /**
     * Request exact alarm permission (for Android 12+)
     */
    public static Intent getExactAlarmPermissionIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            return intent;
        }
        return null;
    }

    /**
     * Schedule a single notification at specific time
     */
    private static void scheduleNotificationAtTime(Context context, AlarmManager alarmManager,
                                                   int hour, int minute, int alarmId, boolean useExact) {
        Intent intent = new Intent(context, VerseNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_ALARM_REQUEST_CODE + alarmId,
                intent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                        PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Set up the calendar for the alarm
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the time has already passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Schedule the alarm based on Android version and permission
        if (useExact) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setExactAndAllowWhileIdle for better reliability
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                // Use setRepeating for older versions
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
            }
        } else {
            // Use inexact alarm as fallback
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    /**
     * Cancel all verse notifications
     */
    public static void cancelAllVerseNotifications(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Cancel up to 200 possible alarms (maximum for custom intervals)
        for (int i = 0; i < 200; i++) {
            Intent intent = new Intent(context, VerseNotificationReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    BASE_ALARM_REQUEST_CODE + i,
                    intent,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_NO_CREATE :
                            PendingIntent.FLAG_NO_CREATE
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }

        // Update preferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, false).apply();
    }

    /**
     * Check if notifications are currently enabled
     */
    public static boolean areNotificationsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, false);
    }

    /**
     * Get the current notification time period
     * @return int array: [startHour, startMinute, endHour, endMinute]
     */
    public static int[] getNotificationTimePeriod(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new int[]{
                prefs.getInt(PREF_START_HOUR, 9), // Default start: 9:00 AM
                prefs.getInt(PREF_START_MINUTE, 0),
                prefs.getInt(PREF_END_HOUR, 21), // Default end: 9:00 PM
                prefs.getInt(PREF_END_MINUTE, 0)
        };
    }

    /**
     * Get the current notification interval in minutes
     */
    public static int getNotificationInterval(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(PREF_INTERVAL_MINUTES, INTERVAL_1_HOUR); // Default: 1 hour
    }

    /**
     * Get display name for interval
     */
    public static String getIntervalDisplayName(int intervalMinutes) {
        switch (intervalMinutes) {
            case INTERVAL_15_MINUTES: return "Every 15 minutes";
            case INTERVAL_30_MINUTES: return "Every 30 minutes";
            case INTERVAL_45_MINUTES: return "Every 45 minutes";
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

    /**
     * Get formatted time period string for display
     */
    public static String getFormattedTimePeriod(Context context) {
        int[] times = getNotificationTimePeriod(context);
        int interval = getNotificationInterval(context);
        String intervalName = getIntervalDisplayName(interval);

        return String.format("%s from %02d:%02d to %02d:%02d",
                intervalName, times[0], times[1], times[2], times[3]);
    }

    /**
     * Check if current time is within notification period
     */
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
            // Crosses midnight
            return currentTimeMinutes >= startTimeMinutes || currentTimeMinutes <= endTimeMinutes;
        } else {
            // Same day
            return currentTimeMinutes >= startTimeMinutes && currentTimeMinutes <= endTimeMinutes;
        }
    }

    /**
     * Calculate number of notifications per day based on current settings
     */
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
            // Crosses midnight
            durationMinutes = (24 * 60 - startTimeMinutes) + endTimeMinutes;
        } else {
            // Same day
            durationMinutes = endTimeMinutes - startTimeMinutes;
        }

        int notificationCount = (durationMinutes / intervalMinutes) + 1;
        return Math.max(1, Math.min(notificationCount, 200)); // Cap at 200
    }

    // Legacy methods for backward compatibility
    @Deprecated
    public static void scheduleVerseNotifications(Context context, int hour, int minute) {
        int endHour = (hour + 12) % 24;
        scheduleHourlyVerseNotifications(context, hour, minute, endHour, minute);
    }

    @Deprecated
    public static void cancelVerseNotifications(Context context) {
        cancelAllVerseNotifications(context);
    }

    @Deprecated
    public static int[] getNotificationTime(Context context) {
        int[] times = getNotificationTimePeriod(context);
        return new int[]{times[0], times[1]}; // Return start time
    }
}