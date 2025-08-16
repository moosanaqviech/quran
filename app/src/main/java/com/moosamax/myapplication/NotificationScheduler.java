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
    private static final int BASE_ALARM_REQUEST_CODE = 2000;

    /**
     * Schedule hourly verse notifications within specified time period
     * @param context Application context
     * @param startHour Start hour (0-23)
     * @param startMinute Start minute (0-59)
     * @param endHour End hour (0-23)
     * @param endMinute End minute (0-59)
     */
    public static boolean scheduleHourlyVerseNotifications(Context context, int startHour, int startMinute,
                                                           int endHour, int endMinute) {

        // First cancel any existing notifications
        cancelAllVerseNotifications(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Check if we can schedule exact alarms
        if (!canScheduleExactAlarms(context, alarmManager)) {
            // Use inexact alarms as fallback
            return scheduleInexactHourlyNotifications(context, alarmManager, startHour, startMinute, endHour, endMinute);
        }

        // Calculate start and end times in minutes from midnight
        int startTimeMinutes = startHour * 60 + startMinute;
        int endTimeMinutes = endHour * 60 + endMinute;

        // Handle case where end time is next day (e.g., 22:00 to 08:00)
        boolean crossesMidnight = endTimeMinutes <= startTimeMinutes;

        // Schedule notifications for each hour in the range
        int currentHour = startHour;
        int currentMinute = startMinute;
        int alarmId = 0;

        while (true) {
            int currentTimeMinutes = currentHour * 60 + currentMinute;

            // Check if we've reached the end time
            if (!crossesMidnight && currentTimeMinutes > endTimeMinutes) {
                break;
            } else if (crossesMidnight && currentTimeMinutes > endTimeMinutes && currentTimeMinutes >= startTimeMinutes) {
                // For overnight periods, stop when we pass end time and are still after start time
                break;
            }

            // Schedule notification for this time
            scheduleNotificationAtTime(context, alarmManager, currentHour, currentMinute, alarmId, true);
            alarmId++;

            // Move to next hour
            currentHour++;
            if (currentHour >= 24) {
                currentHour = 0;
                // If we crossed midnight and this is an overnight period, check if we should continue
                if (crossesMidnight && (currentHour * 60 + currentMinute) > endTimeMinutes) {
                    break;
                }
            }

            // Safety check to avoid infinite loop
            if (alarmId > 24) break;
        }

        // Save preferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(PREF_NOTIFICATIONS_ENABLED, true)
                .putInt(PREF_START_HOUR, startHour)
                .putInt(PREF_START_MINUTE, startMinute)
                .putInt(PREF_END_HOUR, endHour)
                .putInt(PREF_END_MINUTE, endMinute)
                .apply();

        return true;
    }

    /**
     * Fallback method for devices that don't allow exact alarms
     * Uses inexact repeating alarms with shorter intervals
     */
    private static boolean scheduleInexactHourlyNotifications(Context context, AlarmManager alarmManager,
                                                              int startHour, int startMinute, int endHour, int endMinute) {

        // Schedule a repeating alarm every 15 minutes, the receiver will check if it's the right time
        Intent intent = new Intent(context, VerseNotificationReceiver.class);
        intent.putExtra("INEXACT_MODE", true);
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
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If current time has passed, start from next hour
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }

        // Use inexact repeating alarm that checks every 15 minutes
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                15 * 60 * 1000, // 15 minutes in milliseconds
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
                .apply();

        return false; // Return false to indicate inexact scheduling was used
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

        // Cancel up to 24 possible alarms (maximum hourly notifications in a day)
        for (int i = 0; i < 24; i++) {
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
     * Get formatted time period string for display
     */
    public static String getFormattedTimePeriod(Context context) {
        int[] times = getNotificationTimePeriod(context);
        return String.format("Hourly from %02d:%02d to %02d:%02d",
                times[0], times[1], times[2], times[3]);
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

    // Legacy method for backward compatibility
    @Deprecated
    public static void scheduleVerseNotifications(Context context, int hour, int minute) {
        // Convert to hourly notifications from this time to this time + 12 hours
        int endHour = (hour + 12) % 24;
        scheduleHourlyVerseNotifications(context, hour, minute, endHour, minute);
    }

    // Legacy method for backward compatibility
    @Deprecated
    public static void cancelVerseNotifications(Context context) {
        cancelAllVerseNotifications(context);
    }

    // Legacy method for backward compatibility
    @Deprecated
    public static int[] getNotificationTime(Context context) {
        int[] times = getNotificationTimePeriod(context);
        return new int[]{times[0], times[1]}; // Return start time
    }
}