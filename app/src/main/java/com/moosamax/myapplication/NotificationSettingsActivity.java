package com.moosamax.myapplication;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationSettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        prefs = getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);

        setupSpinner();
        setupButtons();
        loadSettings();
    }

    private void setupSpinner() {
        Spinner spinner = findViewById(R.id.interval_spinner);
        String[] intervals = {"Every hour", "Every 2 hours", "Every 4 hours"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, intervals);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupButtons() {
        // Preset buttons
        Button lightButton = findViewById(R.id.preset_light);
        lightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPreset("Every 4 hours", "09:00", "21:00");
            }
        });

        Button moderateButton = findViewById(R.id.preset_moderate);
        moderateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPreset("Every 2 hours", "09:00", "21:00");
            }
        });

        Button frequentButton = findViewById(R.id.preset_frequent);
        frequentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyPreset("Every hour", "09:00", "21:00");
            }
        });

        // Time picker buttons
        Button startTimeButton = findViewById(R.id.start_time_button);
        startTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(true);
            }
        });

        Button endTimeButton = findViewById(R.id.end_time_button);
        endTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(false);
            }
        });

        // Notification switch
        Switch notificationSwitch = findViewById(R.id.notification_switch);
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                String interval = getCurrentInterval();
                String startTime = getStartTime();
                String endTime = getEndTime();

                NotificationHelper.scheduleNotifications(this, interval, startTime, endTime);
            } else {
                NotificationHelper.cancelNotifications(this);
            }
        });
    }

    private void showTimePicker(boolean isStartTime) {
        String currentTime;
        if (isStartTime) {
            currentTime = getStartTime();
        } else {
            currentTime = getEndTime();
        }

        String[] timeParts = currentTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
                        String formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute);

                        if (isStartTime) {
                            setStartTime(formattedTime);
                        } else {
                            setEndTime(formattedTime);
                        }

                        updateAllDisplays();

                        if (isNotificationEnabled()) {
                            String interval = getCurrentInterval();
                            String start = getStartTime();
                            String end = getEndTime();
                            NotificationHelper.scheduleNotifications(
                                    NotificationSettingsActivity.this, interval, start, end);
                        }
                    }
                },
                hour,
                minute,
                true
        );

        if (isStartTime) {
            timePickerDialog.setTitle("Select Start Time");
        } else {
            timePickerDialog.setTitle("Select End Time");
        }

        timePickerDialog.show();
    }

    private void applyPreset(String interval, String startTime, String endTime) {
        // Update spinner
        setSpinnerSelection(interval);

        // Update time displays
        setStartTime(startTime);
        setEndTime(endTime);

        // Turn on notifications
        setNotificationEnabled(true);

        // Update all displays
        updateAllDisplays();

        // Save settings
        saveSettings(interval, startTime, endTime, true);

        // Schedule notifications
        NotificationHelper.scheduleNotifications(this, interval, startTime, endTime);
    }

    private void updateAllDisplays() {
        String interval = getCurrentInterval();
        String startTime = getStartTime();
        String endTime = getEndTime();

        // Update period text
        TextView periodText = findViewById(R.id.selected_period_text);
        periodText.setText(interval + " from " + startTime + " to " + endTime);

        // Update preview
        updatePreview(interval, startTime, endTime);

        // Save current settings
        boolean enabled = isNotificationEnabled();
        saveSettings(interval, startTime, endTime, enabled);
    }

    private void updatePreview(String interval, String startTime, String endTime) {
        TextView preview = findViewById(R.id.notifications_preview);

        int hours = 1;
        if (interval.contains("2 hours")) {
            hours = 2;
        } else if (interval.contains("4 hours")) {
            hours = 4;
        }

        int startHour = Integer.parseInt(startTime.split(":")[0]);
        int endHour = Integer.parseInt(endTime.split(":")[0]);
        int totalHours = endHour - startHour;
        int notificationsPerDay = Math.max(1, totalHours / hours);

        preview.setText("You will receive " + notificationsPerDay + " notifications per day");
    }

    private void setSpinnerSelection(String interval) {
        Spinner spinner = findViewById(R.id.interval_spinner);
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        int position = adapter.getPosition(interval);
        if (position >= 0) {
            spinner.setSelection(position);
        }
    }

    private String getCurrentInterval() {
        Spinner spinner = findViewById(R.id.interval_spinner);
        if (spinner.getSelectedItem() != null) {
            return spinner.getSelectedItem().toString();
        }
        return "Every hour";
    }

    private String getStartTime() {
        TextView startDisplay = findViewById(R.id.start_time_display);
        return startDisplay.getText().toString();
    }

    private String getEndTime() {
        TextView endDisplay = findViewById(R.id.end_time_display);
        return endDisplay.getText().toString();
    }

    private void setStartTime(String time) {
        TextView startDisplay = findViewById(R.id.start_time_display);
        startDisplay.setText(time);
    }

    private void setEndTime(String time) {
        TextView endDisplay = findViewById(R.id.end_time_display);
        endDisplay.setText(time);
    }

    private boolean isNotificationEnabled() {
        Switch notificationSwitch = findViewById(R.id.notification_switch);
        return notificationSwitch.isChecked();
    }

    private void setNotificationEnabled(boolean enabled) {
        Switch notificationSwitch = findViewById(R.id.notification_switch);
        notificationSwitch.setChecked(enabled);
    }

    private void saveSettings(String interval, String startTime, String endTime, boolean enabled) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("interval", interval);
        editor.putString("start_time", startTime);
        editor.putString("end_time", endTime);
        editor.putBoolean("notifications_enabled", enabled);
        editor.apply();
    }

    private void loadSettings() {
        String interval = prefs.getString("interval", "Every hour");
        String startTime = prefs.getString("start_time", "09:00");
        String endTime = prefs.getString("end_time", "21:00");
        boolean enabled = prefs.getBoolean("notifications_enabled", false);

        // Set all UI elements
        setSpinnerSelection(interval);
        setStartTime(startTime);
        setEndTime(endTime);
        setNotificationEnabled(enabled);

        // Update displays
        updateAllDisplays();
    }
}