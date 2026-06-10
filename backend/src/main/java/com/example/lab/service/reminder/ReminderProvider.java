package com.example.lab.service.reminder;

import com.example.lab.dto.reminder.ReminderModule;

public interface ReminderProvider {
    String getKey();

    ReminderModule buildModule();
}
