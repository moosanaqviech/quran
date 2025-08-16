package com.moosamax.myapplication;

import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationSettingsActivity extends AppCompatActivity {

    private Switch notificationSwitch;
    private Button startTimeButton;
    private Button endTimeButton;
    private TextView selectedPeriodText;
    private TextView startTimeDisplay;
    private TextView endTimeDisplay;
    private TextView notificationsPreview;

    private int startHour = 9;
    private int startMinute = 0;
    private int endHour = 21;
    private int endMinute = 0;

    private static final int EXACT_ALARM_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        initViews();
        loadCurrentSettings();
        setupListeners();
        updateDisplay();
    }

    private void initViews() {
        notificationSwitch = findViewById(R.id.notification_switch);
        startTimeButton = findViewById(R.id.start_time_button);
        endTimeButton = findViewById(R.id.end_time_button);
        selectedPeriodText = findViewById(R.id.selected_period_text);
        startTimeDisplay = findViewById(R.id.start_time_display);
        endTimeDisplay = findViewById(R.id.end_time_display);
        notificationsPreview = findViewById(R.id.notifications_preview);
    }

    private void loadCurrentSettings() {
        boolean enabled = NotificationScheduler.areNotificationsEnabled(this);
        int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(this);

        notificationSwitch.setChecked(enabled);
        startHour = timePeriod[0];
        startMinute = timePeriod[1];
        endHour = timePeriod[2];
        endMinute = timePeriod[3];

        updateButtonStates(enabled);
    }

    private void setupListeners() {
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableNotifications();
            } else {
                disableNotifications();
            }
        });

        startTimeButton.setOnClickListener(v -> showStartTimePicker());
        endTimeButton.setOnClickListener(v -> showEndTimePicker());
    }

    private void enableNotifications() {
        // Check if we need exact alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmPermissionDialog();
                notificationSwitch.setChecked(false); // Reset switch until permission is granted
                return;
            }
        }

        boolean exactScheduling = NotificationScheduler.scheduleHourlyVerseNotifications(
                this, startHour, startMinute, endHour, endMinute);

        if (exactScheduling) {
            Toast.makeText(this, "Hourly verse notifications enabled", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Notifications enabled (inexact timing)", Toast.LENGTH_LONG).show();
        }

        updateButtonStates(true);
    }

    private void disableNotifications() {
        NotificationScheduler.cancelAllVerseNotifications(this);
        Toast.makeText(this, "Hourly verse notifications disabled", Toast.LENGTH_SHORT).show();
        updateButtonStates(false);
    }

    private void showExactAlarmPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Precise Timing Permission")
                .setMessage("For accurate hourly notifications, this app needs permission to schedule exact alarms. " +
                        "This ensures you receive verses at the exact times you specify.\n\n" +
                        "Without this permission, notifications may be delayed by up to 15 minutes.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    Intent intent = NotificationScheduler.getExactAlarmPermissionIntent(this);
                    if (intent != null) {
                        startActivityForResult(intent, EXACT_ALARM_PERMISSION_REQUEST);
                    }
                })
                .setNegativeButton("Use Inexact Timing", (dialog, which) -> {
                    // Enable with inexact scheduling
                    notificationSwitch.setChecked(true);
                    boolean exactScheduling = NotificationScheduler.scheduleHourlyVerseNotifications(
                            this, startHour, startMinute, endHour, endMinute);
                    Toast.makeText(this, "Notifications enabled with approximate timing", Toast.LENGTH_LONG).show();
                    updateButtonStates(true);
                })
                .setNeutralButton("Cancel", (dialog, which) -> {
                    notificationSwitch.setChecked(false);
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EXACT_ALARM_PERMISSION_REQUEST) {
            // Check if permission was granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (alarmManager.canScheduleExactAlarms()) {
                    // Permission granted, enable notifications
                    notificationSwitch.setChecked(true);
                    enableNotifications();
                } else {
                    // Permission denied, offer inexact option
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Denied")
                            .setMessage("Exact alarm permission was not granted. Would you like to enable notifications with approximate timing instead?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                notificationSwitch.setChecked(true);
                                enableNotifications();
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                notificationSwitch.setChecked(false);
                            })
                            .show();
                }
            }
        }
    }

    private void updateButtonStates(boolean enabled) {
        startTimeButton.setEnabled(enabled);
        endTimeButton.setEnabled(enabled);
        updateDisplay();
    }

    private void showStartTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    startHour = hourOfDay;
                    startMinute = minute;
                    updateDisplay();
                    updateNotifications();
                },
                startHour,
                startMinute,
                true // 24 hour format
        );

        timePickerDialog.setTitle("Select Start Time");
        timePickerDialog.show();
    }

    private void showEndTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    endHour = hourOfDay;
                    endMinute = minute;
                    updateDisplay();
                    updateNotifications();
                },
                endHour,
                endMinute,
                true // 24 hour format
        );

        timePickerDialog.setTitle("Select End Time");
        timePickerDialog.show();
    }

    private void updateDisplay() {
        // Update time displays
        startTimeDisplay.setText(String.format("%02d:%02d", startHour, startMinute));
        endTimeDisplay.setText(String.format("%02d:%02d", endHour, endMinute));

        // Update period text
        selectedPeriodText.setText(String.format("Hourly from %02d:%02d to %02d:%02d",
                startHour, startMinute, endHour, endMinute));

        // Calculate and display number of notifications
        int notificationCount = calculateNotificationCount();
        String previewText;

        if (notificationCount == 1) {
            previewText = "You will receive 1 notification per day";
        } else {
            previewText = String.format("You will receive %d notifications per day", notificationCount);
        }

        if (notificationSwitch.isChecked()) {
            // Check if exact alarms are available
            boolean hasExactAlarms = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                hasExactAlarms = alarmManager.canScheduleExactAlarms();
            }

            if (hasExactAlarms) {
                notificationsPreview.setText(previewText);
            } else {
                notificationsPreview.setText(previewText + " (approximate timing)");
            }
        } else {
            notificationsPreview.setText("Notifications are disabled");
        }
    }

    private int calculateNotificationCount() {
        int startTimeMinutes = startHour * 60 + startMinute;
        int endTimeMinutes = endHour * 60 + endMinute;

        int durationMinutes;
        if (endTimeMinutes <= startTimeMinutes) {
            // Crosses midnight (e.g., 22:00 to 08:00)
            durationMinutes = (24 * 60 - startTimeMinutes) + endTimeMinutes;
        } else {
            // Same day (e.g., 09:00 to 21:00)
            durationMinutes = endTimeMinutes - startTimeMinutes;
        }

        // Add 1 because we include both start and end times
        int hourCount = (durationMinutes / 60) + 1;

        // Ensure minimum of 1 and maximum of 24
        return Math.max(1, Math.min(hourCount, 24));
    }

    private void updateNotifications() {
        if (notificationSwitch.isChecked()) {
            NotificationScheduler.scheduleHourlyVerseNotifications(
                    this, startHour, startMinute, endHour, endMinute);
            Toast.makeText(this, "Notification schedule updated", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the display when returning to this activity
        updateDisplay();
    }
}