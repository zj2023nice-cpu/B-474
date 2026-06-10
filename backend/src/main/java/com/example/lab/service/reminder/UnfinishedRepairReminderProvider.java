package com.example.lab.service.reminder;

import com.example.lab.dto.reminder.ReminderItem;
import com.example.lab.dto.reminder.ReminderModule;
import com.example.lab.dto.reminder.ReminderPriority;
import com.example.lab.entity.Repair;
import com.example.lab.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UnfinishedRepairReminderProvider implements ReminderProvider {

    @Autowired
    private RepairRepository repairRepository;

    @Override
    public String getKey() {
        return "unfinished_repairs";
    }

    @Override
    public ReminderModule buildModule() {
        LocalDateTime now = LocalDateTime.now();
        List<Repair> unfinished = repairRepository.findByStatusNotOrderByReportDateDesc("FINISHED");

        ReminderModule module = new ReminderModule();
        module.setKey(getKey());
        module.setTitle("未完成维修单");
        module.setDescription("尚未完成的维修工单，包括待处理和维修中");
        module.setActionRoute("/repairs");
        module.setEmptyStateTitle("没有未完成的维修单");
        module.setEmptyStateDescription("所有维修都已处理完成");
        module.setTotalCount(unfinished.size());

        List<ReminderItem> items = new ArrayList<>();
        ReminderPriority overallPriority = ReminderPriority.LOW;

        for (Repair r : unfinished) {
            ReminderItem item = new ReminderItem();
            item.setId(r.getId());

            long pendingHours = Duration.between(r.getReportDate(), now).toHours();
            long pendingDays = pendingHours / 24;

            String equipmentName = r.getEquipment() != null ? r.getEquipment().getName() : "未知设备";
            if (r.getEquipment() != null && r.getEquipment().getCode() != null) {
                equipmentName = "[" + r.getEquipment().getCode() + "] " + equipmentName;
            }
            item.setTitle(equipmentName);

            String reporterName = r.getReporter() != null ? r.getReporter().getName() : "未知";
            String statusText = "REPORTED".equals(r.getStatus()) ? "待处理" : "维修中";
            String pendingText;
            if (pendingDays > 0) {
                pendingText = String.format("已等待 %d 天", pendingDays);
            } else {
                pendingText = String.format("已等待 %d 小时", Math.max(1, pendingHours));
            }
            item.setSubtitle(String.format("%s · %s · %s", reporterName, statusText, pendingText));
            item.setTime(r.getReportDate());

            ReminderPriority priority;
            if ("REPORTED".equals(r.getStatus()) && (pendingDays >= 3 || pendingHours >= 48)) {
                priority = ReminderPriority.HIGH;
            } else if ("REPORTED".equals(r.getStatus()) || pendingDays >= 7) {
                priority = ReminderPriority.MEDIUM;
            } else {
                priority = ReminderPriority.LOW;
            }
            item.setPriority(priority);

            if (priority == ReminderPriority.HIGH) {
                overallPriority = ReminderPriority.HIGH;
            } else if (priority == ReminderPriority.MEDIUM && overallPriority != ReminderPriority.HIGH) {
                overallPriority = ReminderPriority.MEDIUM;
            }

            Map<String, Object> extra = new HashMap<>();
            extra.put("equipmentId", r.getEquipment() != null ? r.getEquipment().getId() : null);
            extra.put("reporterId", r.getReporter() != null ? r.getReporter().getId() : null);
            extra.put("reporterName", reporterName);
            extra.put("status", r.getStatus());
            extra.put("statusText", statusText);
            extra.put("description", r.getDescription());
            extra.put("repairCompany", r.getRepairCompany());
            extra.put("pendingDays", pendingDays);
            extra.put("pendingHours", pendingHours);
            item.setExtra(extra);

            items.add(item);
        }

        items.sort((a, b) -> {
            int p = a.getPriority().compareTo(b.getPriority());
            if (p != 0) return p;
            if (a.getTime() == null && b.getTime() == null) return 0;
            if (a.getTime() == null) return 1;
            if (b.getTime() == null) return -1;
            return a.getTime().compareTo(b.getTime());
        });

        module.setItems(items);
        module.setOverallPriority(overallPriority);

        return module;
    }
}
