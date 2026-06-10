package com.example.lab.service;

import com.example.lab.dto.reminder.ReminderModule;
import com.example.lab.dto.reminder.ReminderPriority;
import com.example.lab.dto.reminder.SystemRemindersDTO;
import com.example.lab.service.reminder.ReminderProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ReminderService {

    @Autowired
    private List<ReminderProvider> providers;

    public SystemRemindersDTO getSystemReminders() {
        SystemRemindersDTO dto = new SystemRemindersDTO();
        dto.setGeneratedAt(LocalDateTime.now());

        List<ReminderModule> modules = new ArrayList<>();
        int totalCount = 0;
        int highPriorityCount = 0;

        for (ReminderProvider provider : providers) {
            ReminderModule module = provider.buildModule();
            modules.add(module);
            totalCount += module.getTotalCount();
            if (module.getItems() != null) {
                for (var item : module.getItems()) {
                    if (item.getPriority() == ReminderPriority.HIGH) {
                        highPriorityCount++;
                    }
                }
            }
        }

        modules.sort(Comparator.comparing(m -> {
            if (m.getOverallPriority() == null) return 3;
            switch (m.getOverallPriority()) {
                case HIGH: return 0;
                case MEDIUM: return 1;
                case LOW: return 2;
                default: return 3;
            }
        }).thenComparingInt(m -> -m.getTotalCount()));

        dto.setModules(modules);
        dto.setTotalCount(totalCount);
        dto.setHighPriorityCount(highPriorityCount);

        return dto;
    }
}
