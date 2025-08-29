package com.moosamax.myapplication;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationSettingsActivity extends AppCompatActivity {

    private Switch notificationSwitch;
    private Spinner intervalSpinner;
    private TextView startTimeDisplay;
    private TextView endTimeDisplay;
    private TextView selectedPeriodText;
    private TextView notificationsPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        initViews();
        setupSpinner();
        setupButtons();
        loadSettings();
        updateDisplays();
    }

    private void initViews() {
        notificationSwitch = findViewById(R.id.notification_switch);
        intervalSpinner = findViewById(R.id.interval_spinner);
        startTimeDisplay = findViewById(R.id.start_time_display);
        endTimeDisplay = findViewById(R.id.end_time_display);
        selectedPeriodText = findViewById(R.id.selected_period_text);
        notificationsPreview = findViewById(R.id.notifications_preview);

        LinearLayout backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void setupSpinner() {
        String[] intervalNames = QuranNotificationManager.getIntervalNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, intervalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSpinner.setAdapter(adapter);
    }

    private void setupButtons() {
        // Preset buttons
        Button presetLight = findViewById(R.id.preset_light);
        Button presetModerate = findViewById(R.id.preset_moderate);
        Button presetFrequent = findViewById(R.id.preset_frequent);

        presetLight.setOnClickListener(v -> applyPreset(240, "Light")); // 4 hours
        presetModerate.setOnClickListener(v -> applyPreset(120, "Moderate")); // 2 hours
        presetFrequent.setOnClickListener(v -> applyPreset(60, "Frequent")); // 1 hour

        // Time picker buttons
        findViewById(R.id.start_time_button).setOnClickListener(v -> showTimePicker(true));
        findViewById(R.id.end_time_button).setOnClickListener(v -> showTimePicker(false));

        // Notification switch
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableNotifications();
            } else {
                disableNotifications();
            }
        });
    }

    private void enableNotifications() {
        int intervalMinutes = getCurrentInterval();
        int[] timePeriod = getCurrentTimePeriod();

        QuranNotificationManager.startNotifications(this,
                timePeriod[0], timePeriod[1], timePeriod[2], timePeriod[3], intervalMinutes);

        String message = "✅ Notifications enabled!\n" + QuranNotificationManager.getFormattedSchedule(this);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        updateDisplays();
    }

    private void disableNotifications() {
        QuranNotificationManager.stopNotifications(this);
        Toast.makeText(this, "❌ Notifications disabled", Toast.LENGTH_SHORT).show();
        updateDisplays();
    }

    private void showTimePicker(boolean isStartTime) {
        int[] currentTimes = getCurrentTimePeriod();
        int hour = isStartTime ? currentTimes[0] : currentTimes[2];
        int minute = isStartTime ? currentTimes[1] : currentTimes[3];

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    if (isStartTime) {
                        updateStartTime(selectedHour, selectedMinute);
                    } else {
                        updateEndTime(selectedHour, selectedMinute);
                    }
                    updateDisplays();
                    if (notificationSwitch.isChecked()) {
                        enableNotifications();
                    }
                }, hour, minute, true);

        timePickerDialog.setTitle(isStartTime ? "Select Start Time" : "Select End Time");
        timePickerDialog.show();
    }

    private void applyPreset(int intervalMinutes, String presetName) {
        setSpinnerToInterval(intervalMinutes);
        updateStartTime(9, 0);
        updateEndTime(21, 0);
        notificationSwitch.setChecked(true);
        updateDisplays();

        QuranNotificationManager.startNotifications(this, 9, 0, 21, 0, intervalMinutes);
        Toast.makeText(this, presetName + " preset applied!", Toast.LENGTH_LONG).show();
    }

    private void loadSettings() {
        boolean enabled = QuranNotificationManager.isEnabled(this);
        int[] settings = QuranNotificationManager.getSettings(this);

        notificationSwitch.setChecked(enabled);
        setSpinnerToInterval(settings[4]);
        updateStartTime(settings[0], settings[1]);
        updateEndTime(settings[2], settings[3]);
    }

    private void updateDisplays() {
        boolean enabled = notificationSwitch.isChecked();
        if (enabled) {
            String schedule = QuranNotificationManager.getFormattedSchedule(this);
            selectedPeriodText.setText(schedule);

            int[] settings = QuranNotificationManager.getSettings(this);
            int notificationsPerDay = calculateNotificationsPerDay(settings);
            notificationsPreview.setText("Approximately " + notificationsPerDay + " notifications per day");
        } else {
            selectedPeriodText.setText("Notifications disabled");
            notificationsPreview.setText("Enable notifications to see preview");
        }
    }

    private int calculateNotificationsPerDay(int[] settings) {
        int startMinutes = settings[0] * 60 + settings[1];
        int endMinutes = settings[2] * 60 + settings[3];
        int interval = settings[4];

        int duration = endMinutes > startMinutes ?
                endMinutes - startMinutes :
                (24 * 60 - startMinutes) + endMinutes;

        return Math.max(1, duration / interval);
    }

    // Helper methods
    private int getCurrentInterval() {
        int selectedIndex = intervalSpinner.getSelectedItemPosition();
        return selectedIndex >= 0 && selectedIndex < QuranNotificationManager.INTERVALS.length ?
                QuranNotificationManager.INTERVALS[selectedIndex] : 60;
    }

    private int[] getCurrentTimePeriod() {
        try {
            String[] startParts = startTimeDisplay.getText().toString().split(":");
            String[] endParts = endTimeDisplay.getText().toString().split(":");
            return new int[]{
                    Integer.parseInt(startParts[0]), Integer.parseInt(startParts[1]),
                    Integer.parseInt(endParts[0]), Integer.parseInt(endParts[1])
            };
        } catch (Exception e) {
            return new int[]{9, 0, 21, 0}; // Default
        }
    }

    private void setSpinnerToInterval(int intervalMinutes) {
        for (int i = 0; i < QuranNotificationManager.INTERVALS.length; i++) {
            if (QuranNotificationManager.INTERVALS[i] == intervalMinutes) {
                intervalSpinner.setSelection(i);
                break;
            }
        }
    }

    private void updateStartTime(int hour, int minute) {
        startTimeDisplay.setText(String.format("%02d:%02d", hour, minute));
    }

    private void updateEndTime(int hour, int minute) {
        endTimeDisplay.setText(String.format("%02d:%02d", hour, minute));
    }
}
