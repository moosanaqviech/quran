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

        if (inexactMode) {
            // For inexact mode, check if current time matches any scheduled hour
            if (!isTimeForNotification(context)) {
                return; // Not the right time yet
            }

            // Check if we already sent a notification in this hour
            if (wasNotificationSentThisHour(context)) {
                return; // Already sent this hour
            }
        }

        // Check if within notification period
        if (!NotificationScheduler.isWithinNotificationPeriod(context)) {
            // If outside notification period, reschedule for next valid time
            rescheduleNextNotification(context);
            return;
        }

        // Mark that we sent a notification this hour (for inexact mode)
        if (inexactMode) {
            markNotificationSentThisHour(context);
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
            rescheduleNextNotification(context);
        }
    }

    /**
     * Check if current time is at the start of an hour within notification period
     */
    private boolean isTimeForNotification(Context context) {
        Calendar now = Calendar.getInstance();
        int currentMinute = now.get(Calendar.MINUTE);

        // Only trigger notifications at the top of each hour (0-4 minutes past)
        // This gives a window for inexact alarms
        if (currentMinute > 4) {
            return false;
        }

        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(context);
        int startHour = timePeriod[0];
        int endHour = timePeriod[2];

        // Check if current hour is within notification period
        if (endHour <= startHour) {
            // Crosses midnight
            return currentHour >= startHour || currentHour <= endHour;
        } else {
            // Same day
            return currentHour >= startHour && currentHour <= endHour;
        }
    }

    /**
     * Check if we already sent a notification this hour (prevents duplicates)
     */
    private boolean wasNotificationSentThisHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationTracker", Context.MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        String hourKey = now.get(Calendar.YEAR) + "_" + now.get(Calendar.DAY_OF_YEAR) + "_" + now.get(Calendar.HOUR_OF_DAY);
        return prefs.getBoolean(hourKey, false);
    }

    /**
     * Mark that we sent a notification this hour
     */
    private void markNotificationSentThisHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("NotificationTracker", Context.MODE_PRIVATE);
        Calendar now = Calendar.getInstance();
        String hourKey = now.get(Calendar.YEAR) + "_" + now.get(Calendar.DAY_OF_YEAR) + "_" + now.get(Calendar.HOUR_OF_DAY);

        prefs.edit().putBoolean(hourKey, true).apply();

        // Clean up old entries (older than 2 days)
        cleanupOldNotificationRecords(prefs, now);
    }

    /**
     * Clean up old notification tracking records to prevent SharedPreferences from growing too large
     */
    private void cleanupOldNotificationRecords(SharedPreferences prefs, Calendar now) {
        SharedPreferences.Editor editor = prefs.edit();

        // Remove entries older than 2 days
        Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.DAY_OF_YEAR, -2);
        String cutoffPrefix = cutoff.get(Calendar.YEAR) + "_" + cutoff.get(Calendar.DAY_OF_YEAR);

        // This is a simple cleanup - in a production app you might want a more sophisticated approach
        for (String key : prefs.getAll().keySet()) {
            if (key.compareTo(cutoffPrefix) < 0) {
                editor.remove(key);
            }
        }

        editor.apply();
    }

    /**
     * Reschedule the next notification for devices that don't support repeating exact alarms
     */
    private void rescheduleNextNotification(Context context) {
        // Only reschedule for exact alarm mode
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        boolean canScheduleExact = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExact = alarmManager.canScheduleExactAlarms();
        }

        if (!canScheduleExact) {
            return; // Let inexact repeating alarm handle it
        }

        Calendar now = Calendar.getInstance();
        Calendar nextNotification = Calendar.getInstance();
        nextNotification.add(Calendar.HOUR_OF_DAY, 1);
        nextNotification.set(Calendar.MINUTE, 0);
        nextNotification.set(Calendar.SECOND, 0);
        nextNotification.set(Calendar.MILLISECOND, 0);

        // Check if next hour is still within notification period
        int nextHour = nextNotification.get(Calendar.HOUR_OF_DAY);
        int nextMinute = nextNotification.get(Calendar.MINUTE);

        int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(context);
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
}