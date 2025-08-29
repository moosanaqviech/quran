package com.moosamax.myapplication;

import android.app.Application;
import android.util.Log;

public class QuranVersesApplication extends Application {
    private static final String TAG = "QuranVersesApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application onCreate - Pure WorkManager system");

        // Initialize VerseRepository
        VerseRepository.getInstance(this).initialize();

        // Restart notifications if they were enabled (handles boot/app update)
        if (QuranNotificationManager.isEnabled(this)) {
            Log.d(TAG, "Restarting notifications after app launch");
            int[] settings = QuranNotificationManager.getSettings(this);
            QuranNotificationManager.startNotifications(this,
                    settings[0], settings[1], settings[2], settings[3], settings[4]);
        }

        Log.d(TAG, "Pure WorkManager application initialization complete");
    }
}