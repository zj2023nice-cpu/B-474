package com.example.lab.dto.reminder;

import lombok.Data;
import java.util.List;

@Data
public class ReminderModule {
    private String key;
    private String title;
    private String description;
    private int totalCount;
    private ReminderPriority overallPriority;
    private List<ReminderItem> items;
    private String emptyStateTitle;
    private String emptyStateDescription;
    private String actionRoute;
}
