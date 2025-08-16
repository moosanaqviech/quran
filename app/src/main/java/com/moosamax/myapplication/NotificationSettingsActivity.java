package com.moosamax.myapplication;

import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationSettingsActivity extends AppCompatActivity {

    private Switch notificationSwitch;
    private Button startTimeButton;
    private Button endTimeButton;
    private Spinner intervalSpinner;
    private TextView selectedPeriodText;
    private TextView startTimeDisplay;
    private TextView endTimeDisplay;
    private TextView notificationsPreview;

    private int startHour = 9;
    private int startMinute = 0;
    private int endHour = 21;
    private int endMinute = 0;
    private int currentIntervalMinutes = NotificationScheduler.INTERVAL_1_HOUR;

    private static final int EXACT_ALARM_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        initViews();
        loadCurrentSettings();
        setupSpinner();
        setupListeners();
        updateDisplay();
    }

    private void initViews() {
        notificationSwitch = findViewById(R.id.notification_switch);
        startTimeButton = findViewById(R.id.start_time_button);
        endTimeButton = findViewById(R.id.end_time_button);
        intervalSpinner = findViewById(R.id.interval_spinner);
        selectedPeriodText = findViewById(R.id.selected_period_text);
        startTimeDisplay = findViewById(R.id.start_time_display);
        endTimeDisplay = findViewById(R.id.end_time_display);
        notificationsPreview = findViewById(R.id.notifications_preview);
    }

    private void setupSpinner() {
        String[] intervalNames = NotificationScheduler.getIntervalDisplayNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                intervalNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSpinner.setAdapter(adapter);

        // Set selection based on current interval
        int[] intervalValues = NotificationScheduler.getIntervalValues();
        for (int i = 0; i < intervalValues.length; i++) {
            if (intervalValues[i] == currentIntervalMinutes) {
                intervalSpinner.setSelection(i);
                break;
            }
        }

        intervalSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                int[] values = NotificationScheduler.getIntervalValues();
                currentIntervalMinutes = values[position];
                updateDisplay();
                updateNotifications();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadCurrentSettings() {
        boolean enabled = NotificationScheduler.areNotificationsEnabled(this);
        int[] timePeriod = NotificationScheduler.getNotificationTimePeriod(this);
        currentIntervalMinutes = NotificationScheduler.getNotificationInterval(this);

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

        // Setup preset buttons
        setupPresetButtons();
    }

    private void setupPresetButtons() {
        Button presetLight = findViewById(R.id.preset_light);
        Button presetModerate = findViewById(R.id.preset_moderate);
        Button presetFrequent = findViewById(R.id.preset_frequent);

        if (presetLight != null) {
            presetLight.setOnClickListener(v -> applyPreset("light"));
        }

        if (presetModerate != null) {
            presetModerate.setOnClickListener(v -> applyPreset("moderate"));
        }

        if (presetFrequent != null) {
            presetFrequent.setOnClickListener(v -> applyPreset("frequent"));
        }
    }

    private void applyPreset(String presetType) {
        switch (presetType) {
            case "light":
                // Every 4 hours, 8 AM to 8 PM
                startHour = 8;
                startMinute = 0;
                endHour = 20;
                endMinute = 0;
                currentIntervalMinutes = NotificationScheduler.INTERVAL_4_HOURS;
                break;

            case "moderate":
                // Every 2 hours, 9 AM to 9 PM
                startHour = 9;
                startMinute = 0;
                endHour = 21;
                endMinute = 0;
                currentIntervalMinutes = NotificationScheduler.INTERVAL_2_HOURS;
                break;

            case "frequent":
                // Every hour, 8 AM to 10 PM
                startHour = 8;
                startMinute = 0;
                endHour = 22;
                endMinute = 0;
                currentIntervalMinutes = NotificationScheduler.INTERVAL_1_HOUR;
                break;
        }

        // Update UI to reflect preset
        updateSpinnerSelection();
        updateDisplay();

        // Show confirmation
        String presetName = presetType.substring(0, 1).toUpperCase() + presetType.substring(1);
        String intervalName = NotificationScheduler.getIntervalDisplayName(currentIntervalMinutes);
        Toast.makeText(this, presetName + " preset applied: " + intervalName.toLowerCase() +
                " from " + String.format("%02d:%02d", startHour, startMinute) +
                " to " + String.format("%02d:%02d", endHour, endMinute), Toast.LENGTH_LONG).show();

        // Update notifications if enabled
        if (notificationSwitch.isChecked()) {
            updateNotifications();
        }
    }

    private void updateSpinnerSelection() {
        int[] intervalValues = NotificationScheduler.getIntervalValues();
        for (int i = 0; i < intervalValues.length; i++) {
            if (intervalValues[i] == currentIntervalMinutes) {
                intervalSpinner.setSelection(i);
                break;
            }
        }
    }

    private void enableNotifications() {
        // Validate interval for the time period
        if (!isIntervalValid()) {
            showIntervalWarningDialog();
            notificationSwitch.setChecked(false);
            return;
        }

        // Check if we need exact alarm permission for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmPermissionDialog();
                notificationSwitch.setChecked(false); // Reset switch until permission is granted
                return;
            }
        }

        boolean exactScheduling = NotificationScheduler.scheduleCustomIntervalNotifications(
                this, startHour, startMinute, endHour, endMinute, currentIntervalMinutes);

        if (exactScheduling) {
            Toast.makeText(this, "Verse notifications enabled with " +
                            NotificationScheduler.getIntervalDisplayName(currentIntervalMinutes).toLowerCase(),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Notifications enabled (approximate timing)", Toast.LENGTH_LONG).show();
        }

        updateButtonStates(true);
    }

    private boolean isIntervalValid() {
        int durationMinutes = calculateDurationMinutes();

        // Check if interval is reasonable for the time period
        if (currentIntervalMinutes > durationMinutes) {
            return false; // Interval longer than the entire period
        }

        // For very short intervals, warn if it will result in too many notifications
        int notificationCount = (durationMinutes / currentIntervalMinutes) + 1;
        return notificationCount <= 100; // Reasonable limit
    }

    private void showIntervalWarningDialog() {
        int durationMinutes = calculateDurationMinutes();
        String message;

        if (currentIntervalMinutes > durationMinutes) {
            message = "The selected interval (" + NotificationScheduler.getIntervalDisplayName(currentIntervalMinutes) +
                    ") is longer than your notification period (" + formatDuration(durationMinutes) +
                    ").\n\nPlease choose a shorter interval or extend your notification period.";
        } else {
            int notificationCount = (durationMinutes / currentIntervalMinutes) + 1;
            message = "The selected interval will result in " + notificationCount +
                    " notifications per day, which may be too many.\n\nConsider choosing a longer interval.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Interval Warning")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + " minutes";
        } else if (minutes == 60) {
            return "1 hour";
        } else if (minutes % 60 == 0) {
            return (minutes / 60) + " hours";
        } else {
            return (minutes / 60) + "h " + (minutes % 60) + "m";
        }
    }

    private int calculateDurationMinutes() {
        int startTimeMinutes = startHour * 60 + startMinute;
        int endTimeMinutes = endHour * 60 + endMinute;

        if (endTimeMinutes <= startTimeMinutes) {
            // Crosses midnight
            return (24 * 60 - startTimeMinutes) + endTimeMinutes;
        } else {
            // Same day
            return endTimeMinutes - startTimeMinutes;
        }
    }

    private void disableNotifications() {
        NotificationScheduler.cancelAllVerseNotifications(this);
        Toast.makeText(this, "Verse notifications disabled", Toast.LENGTH_SHORT).show();
        updateButtonStates(false);
    }

    private void showExactAlarmPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Precise Timing Permission")
                .setMessage("For accurate notifications at your chosen interval, this app needs permission to schedule exact alarms. " +
                        "This ensures you receive verses at the exact times you specify.\n\n" +
                        "Without this permission, notifications may be delayed by several minutes.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    Intent intent = NotificationScheduler.getExactAlarmPermissionIntent(this);
                    if (intent != null) {
                        startActivityForResult(intent, EXACT_ALARM_PERMISSION_REQUEST);
                    }
                })
                .setNegativeButton("Use Approximate Timing", (dialog, which) -> {
                    // Enable with inexact scheduling
                    notificationSwitch.setChecked(true);
                    boolean exactScheduling = NotificationScheduler.scheduleCustomIntervalNotifications(
                            this, startHour, startMinute, endHour, endMinute, currentIntervalMinutes);
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
        intervalSpinner.setEnabled(enabled);
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
        String intervalName = NotificationScheduler.getIntervalDisplayName(currentIntervalMinutes);
        selectedPeriodText.setText(String.format("%s from %02d:%02d to %02d:%02d",
                intervalName, startHour, startMinute, endHour, endMinute));

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

            // Add warning for very frequent notifications
            if (notificationCount > 50) {
                notificationsPreview.setText(previewText + " - This may be too frequent!");
                notificationsPreview.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else if (notificationCount > 20) {
                notificationsPreview.setText(previewText + " - Consider a longer interval");
                notificationsPreview.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            } else {
                notificationsPreview.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            }
        } else {
            notificationsPreview.setText("Notifications are disabled");
            notificationsPreview.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private int calculateNotificationCount() {
        int durationMinutes = calculateDurationMinutes();

        // Add 1 because we include both start and end times
        int notificationCount = (durationMinutes / currentIntervalMinutes) + 1;

        // Ensure minimum of 1 and maximum of 200
        return Math.max(1, Math.min(notificationCount, 200));
    }

    private void updateNotifications() {
        if (notificationSwitch.isChecked()) {
            if (isIntervalValid()) {
                NotificationScheduler.scheduleCustomIntervalNotifications(
                        this, startHour, startMinute, endHour, endMinute, currentIntervalMinutes);
                Toast.makeText(this, "Notification schedule updated", Toast.LENGTH_SHORT).show();
            } else {
                // Disable notifications if interval becomes invalid
                notificationSwitch.setChecked(false);
                disableNotifications();
                showIntervalWarningDialog();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the display when returning to this activity
        updateDisplay();
    }
}