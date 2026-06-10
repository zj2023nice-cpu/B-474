package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.dto.reminder.SystemRemindersDTO;
import com.example.lab.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    @Autowired
    private ReminderService reminderService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<SystemRemindersDTO> getSystemReminders() {
        SystemRemindersDTO reminders = reminderService.getSystemReminders();
        return ApiResponse.success(reminders);
    }
}
