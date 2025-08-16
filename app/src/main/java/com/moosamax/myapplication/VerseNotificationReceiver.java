package com.moosamax.myapplication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;

public class VerseNotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if notifications are enabled
        if (!NotificationScheduler.areNotificationsEnabled(context)) {
            return;
        }

        // Check if this is inexact mode (for devices without exact alarm permission)
        boolean inexactMode = intent.getBooleanExtra("INEXACT_MODE", false);
        int customInterval = intent.getIntExtra("CUSTOM_INTERVAL", 60); // Default 60 minutes

        if (inexactMode) {
            // For inexact mode, check if current time matches any scheduled interval
            if (!isTimeForCustomNotification(context, customInterval)) {
                return; // Not the right time yet
            }

            // Check if we already sent a notification in this interval period
            if (wasNotificationSentInInterval(context, customInterval)) {
                return; // Already sent in this interval
            }
        }

        // Check if within notification period
        if (!NotificationScheduler.isWithinNotificationPeriod(context)) {
            // If outside notification period, reschedule for next valid time
            rescheduleNextCustomNotification(context, customInterval);
            return;
        }

        // Mark that we sent a notification this interval (for inexact mode)
        if (inexactMode) {
            markNotificationSentInInterval(context, customInterval);
        }

        // Send the notification
        Intent serviceIntent = new Intent(context, VerseNotificationService.class);
        serviceIntent.setAction("SEND_VERSE_NOTIFICATION");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // For Android M and above with exact alarms, reschedule the next notification
        // because setExactAndAllowWhileIdle doesn't repeat automatically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !inexactMode) {
            rescheduleNextCustomNotification(context, customInterval);
        }
    }

    /**
     * Check if current time is appropriate for a custom interval notification
     */
    private boolean isTimeForCustomNotification(Context context, int intervalMinutes) {
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int currentTimeMinutes = currentHour * 60 + currentMinute;

        // Get notification settings
        int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(context);
        int startTimeMinutes = timePeriod[0] * 60 + timePeriod[1];

        // Calculate how many intervals have passed since start time
        int minutesSinceStart;
        if (currentTimeMinutes >= startTimeMinutes) {
            minutesSinceStart = currentTimeMinutes - startTimeMinutes;
        } else {
            // Handle overnight periods
            minutesSinceStart = (24 * 60 - startTimeMinutes) + currentTimeMinutes;
        }

        // Check if we're at an interval boundary (with tolerance for inexact alarms)
        int intervalRemainder = minutesSinceStart % intervalMinutes;

        // Allow a window for inexact timing based on interval length
        int toleranceMinutes = Math.min(intervalMinutes / 4, 10); // Max 10 minutes tolerance
        if (toleranceMinutes < 2) toleranceMinutes = 2; // Minimum 2 minutes tolerance

        return intervalRemainder <= toleranceMinutes || intervalRemainder >= (intervalMinutes - toleranceMinutes);
    }

    /**
     * Check if we already sent a notification in this interval period
     */
    private boolean wasNotificationSentInInterval(Context context, int intervalMinutes) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationTracker", Context.MODE_PRIVATE);
        Calendar now = Calendar.getInstance();

        // Create a unique key for this interval period
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int currentTimeMinutes = currentHour * 60 + currentMinute;

        // Calculate which interval period we're in
        int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(context);
        int startTimeMinutes = timePeriod[0] * 60 + timePeriod[1];

        int minutesSinceStart;
        if (currentTimeMinutes >= startTimeMinutes) {
            minutesSinceStart = currentTimeMinutes - startTimeMinutes;
        } else {
            // Handle overnight periods
            minutesSinceStart = (24 * 60 - startTimeMinutes) + currentTimeMinutes;
        }

        int intervalPeriod = minutesSinceStart / intervalMinutes;

        String intervalKey = now.get(Calendar.YEAR) + "_" + now.get(Calendar.DAY_OF_YEAR) +
                "_" + intervalMinutes + "_" + intervalPeriod;

        return prefs.getBoolean(intervalKey, false);
    }

    /**
     * Mark that we sent a notification in this interval period
     */
    private void markNotificationSentInInterval(Context context, int intervalMinutes) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationTracker", Context.MODE_PRIVATE);
        Calendar now = Calendar.getInstance();

        // Create the same key as in wasNotificationSentInInterval
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int currentTimeMinutes = currentHour * 60 + currentMinute;

        int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(context);
        int startTimeMinutes = timePeriod[0] * 60 + timePeriod[1];

        int minutesSinceStart;
        if (currentTimeMinutes >= startTimeMinutes) {
            minutesSinceStart = currentTimeMinutes - startTimeMinutes;
        } else {
            minutesSinceStart = (24 * 60 - startTimeMinutes) + currentTimeMinutes;
        }

        int intervalPeriod = minutesSinceStart / intervalMinutes;

        String intervalKey = now.get(Calendar.YEAR) + "_" + now.get(Calendar.DAY_OF_YEAR) +
                "_" + intervalMinutes + "_" + intervalPeriod;

        prefs.edit().putBoolean(intervalKey, true).apply();

        // Clean up old entries (older than 3 days)
        cleanupOldNotificationRecords(prefs, now);
    }

    /**
     * Clean up old notification tracking records
     */
    private void cleanupOldNotificationRecords(SharedPreferences prefs, Calendar now) {
        SharedPreferences.Editor editor = prefs.edit();

        // Remove entries older than 3 days
        Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.DAY_OF_YEAR, -3);
        String cutoffPrefix = cutoff.get(Calendar.YEAR) + "_" + cutoff.get(Calendar.DAY_OF_YEAR);

        for (String key : prefs.getAll().keySet()) {
            if (key.length() > 10 && key.compareTo(cutoffPrefix) < 0) {
                editor.remove(key);
            }
        }

        editor.apply();
    }

    /**
     * Reschedule the next notification for custom intervals
     */
    private void rescheduleNextCustomNotification(Context context, int intervalMinutes) {
        // Only reschedule for exact alarm mode
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        boolean canScheduleExact = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExact = alarmManager.canScheduleExactAlarms();
        }

        if (!canScheduleExact) {
            return; // Let inexact repeating alarm handle it
        }

        // Get current interval from settings
        int currentInterval = NotificationScheduler.getNotificationInterval(context);

        Calendar now = Calendar.getInstance();
        Calendar nextNotification = Calendar.getInstance();
        nextNotification.add(Calendar.MINUTE, currentInterval);

        // Check if next notification time is still within notification period
        int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(context);
        int nextHour = nextNotification.get(Calendar.HOUR_OF_DAY);
        int nextMinute = nextNotification.get(Calendar.MINUTE);

        int startTimeMinutes = timePeriod[0] * 60 + timePeriod[1];
        int endTimeMinutes = timePeriod[2] * 60 + timePeriod[3];
        int nextTimeMinutes = nextHour * 60 + nextMinute;

        boolean isWithinPeriod;
        if (endTimeMinutes <= startTimeMinutes) {
            // Crosses midnight
            isWithinPeriod = nextTimeMinutes >= startTimeMinutes || nextTimeMinutes <= endTimeMinutes;
        } else {
            // Same day
            isWithinPeriod = nextTimeMinutes >= startTimeMinutes && nextTimeMinutes <= endTimeMinutes;
        }

        if (!isWithinPeriod) {
            // Schedule for start of next notification period (tomorrow)
            nextNotification.add(Calendar.DAY_OF_YEAR, 1);
            nextNotification.set(Calendar.HOUR_OF_DAY, timePeriod[0]);
            nextNotification.set(Calendar.MINUTE, timePeriod[1]);
            nextNotification.set(Calendar.SECOND, 0);
            nextNotification.set(Calendar.MILLISECOND, 0);
        }

        // Create and schedule the alarm
        Intent alarmIntent = new Intent(context, VerseNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                2000, // Use a consistent request code for rescheduling
                alarmIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT :
                        PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextNotification.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextNotification.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    /**
     * Legacy method for backward compatibility with hourly notifications
     */
    @Deprecated
    private boolean isTimeForNotification(Context context) {
        return isTimeForCustomNotification(context, 60); // Default to hourly
    }

    /**
     * Legacy method for backward compatibility
     */
    @Deprecated
    private boolean wasNotificationSentThisHour(Context context) {
        return wasNotificationSentInInterval(context, 60);
    }

    /**
     * Legacy method for backward compatibility
     */
    @Deprecated
    private void markNotificationSentThisHour(Context context) {
        markNotificationSentInInterval(context, 60);
    }

    /**
     * Legacy method for backward compatibility
     */
    @Deprecated
    private void rescheduleNextNotification(Context context) {
        rescheduleNextCustomNotification(context, 60);
    }
}