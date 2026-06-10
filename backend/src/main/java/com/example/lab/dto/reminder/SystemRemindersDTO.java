package com.example.lab.dto.reminder;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SystemRemindersDTO {
    private int totalCount;
    private int highPriorityCount;
    private LocalDateTime generatedAt;
    private List<ReminderModule> modules;
}
