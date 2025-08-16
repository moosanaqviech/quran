package com.moosamax.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BootReceiver - Reschedules notifications after device reboot
 * This ensures that hourly notifications continue working even after the device is restarted
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                        intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON"))) {

            // Check if notifications were previously enabled
            if (NotificationScheduler.areNotificationsEnabled(context)) {
                // Get the saved time period
                int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(context);

                // Reschedule hourly notifications
                NotificationScheduler.scheduleHourlyVerseNotifications(
                        context,
                        timePeriod[0], timePeriod[1],
                        timePeriod[2], timePeriod[3]
                );
            }
        }
    }
}