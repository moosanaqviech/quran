/*
// UPDATED BootWorkManager.java - Pure WorkManager, no AlarmManager

package com.moosamax.myapplication;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;

*/
/**
 * BootWorkManager - 100% WorkManager solution for Android 15 compatibility
 * Handles rescheduling notifications after device reboot using WorkManager ONLY
 *//*

public class BootWorkManager {
    private static final String TAG = "BootWorkManager";
    private static final String WORK_TAG_BOOT_CHECK = "boot_notification_reschedule";

    */
/**
     * Schedule a one-time work to check and reschedule notifications after boot
     * This should be called from Application.onCreate()
     *//*

    public static void scheduleBootRescheduleWork(Context context) {
        Log.d(TAG, "Scheduling boot reschedule work (WorkManager only)");

        try {
            // Create a one-time work request that runs soon after app launch
            OneTimeWorkRequest bootWork = new OneTimeWorkRequest.Builder(BootRescheduleWorker.class)
                    .addTag(WORK_TAG_BOOT_CHECK)
                    .setInitialDelay(5, TimeUnit.SECONDS) // Small delay to ensure app is ready
                    .build();

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                            WORK_TAG_BOOT_CHECK,
                            ExistingWorkPolicy.REPLACE,
                            bootWork
                    );

            Log.d(TAG, "Boot reschedule work scheduled successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling boot reschedule work", e);
        }
    }

    */
/**
     * Worker class that handles notification rescheduling using WorkManager only
     *//*

    public static class BootRescheduleWorker extends Worker {

        public BootRescheduleWorker(Context context, WorkerParameters params) {
            super(context, params);
        }

        @Override
        public Result doWork() {
            Log.d(TAG, "BootRescheduleWorker: Starting notification reschedule check");

            try {
                Context context = getApplicationContext();

                // Check if notifications were previously enabled
                if (NotificationScheduler.areNotificationsEnabled(context)) {
                    Log.d(TAG, "Notifications were enabled, rescheduling with WorkManager...");

                    // Get the saved settings
                    int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(context);
                    int interval = NotificationScheduler.getNotificationInterval(context);

                    // Reschedule notifications using WorkManager only
                    boolean success = NotificationScheduler.scheduleCustomIntervalNotifications(
                            context,
                            timePeriod[0], timePeriod[1], // start hour, minute
                            timePeriod[2], timePeriod[3], // end hour, minute
                            interval // interval in minutes
                    );

                    if (success) {
                        Log.i(TAG, "Successfully rescheduled WorkManager notifications after boot");
                        Log.d(TAG, String.format("Schedule: %02d:%02d to %02d:%02d, every %d minutes",
                                timePeriod[0], timePeriod[1], timePeriod[2], timePeriod[3], interval));
                    } else {
                        Log.w(TAG, "Failed to reschedule notifications after boot");
                    }
                } else {
                    Log.d(TAG, "Notifications were not enabled, no rescheduling needed");
                }

                return Result.success();

            } catch (Exception e) {
                Log.e(TAG, "Error rescheduling notifications after boot", e);
                return Result.failure();
            }
        }
    }
}       */
