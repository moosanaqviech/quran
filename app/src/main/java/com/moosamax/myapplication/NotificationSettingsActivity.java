package com.moosamax.myapplication;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationSettingsActivity extends AppCompatActivity {

    private Switch notificationSwitch;
    private Button timePickerButton;
    private TextView selectedTimeText;
    private int selectedHour = 10;
    private int selectedMinute = 35;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        initViews();
        loadCurrentSettings();
        setupListeners();
    }

    private void initViews() {
        notificationSwitch = findViewById(R.id.notification_switch);
        timePickerButton = findViewById(R.id.time_picker_button);
        selectedTimeText = findViewById(R.id.selected_time_text);
    }

    private void loadCurrentSettings() {
        boolean enabled = NotificationScheduler.areNotificationsEnabled(this);
        int[] time = NotificationScheduler.getNotificationTime(this);

        notificationSwitch.setChecked(enabled);
        selectedHour = time[0];
        selectedMinute = time[1];
        updateTimeDisplay();

        timePickerButton.setEnabled(enabled);
    }

    private void setupListeners() {
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timePickerButton.setEnabled(isChecked);

            if (isChecked) {
                NotificationScheduler.scheduleVerseNotifications(this, selectedHour, selectedMinute);
                Toast.makeText(this, "Daily verse notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                NotificationScheduler.cancelVerseNotifications(this);
                Toast.makeText(this, "Daily verse notifications disabled", Toast.LENGTH_SHORT).show();
            }
        });

        timePickerButton.setOnClickListener(v -> showTimePicker());
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;
                    updateTimeDisplay();

                    if (notificationSwitch.isChecked()) {
                        NotificationScheduler.scheduleVerseNotifications(this, selectedHour, selectedMinute);
                        Toast.makeText(this, "Notification time updated", Toast.LENGTH_SHORT).show();
                    }
                },
                selectedHour,
                selectedMinute,
                false
        );

        timePickerDialog.show();
    }

    private void updateTimeDisplay() {
        String timeString = String.format("%02d:%02d", selectedHour, selectedMinute);
        selectedTimeText.setText("Daily verse at " + timeString);
    }
}