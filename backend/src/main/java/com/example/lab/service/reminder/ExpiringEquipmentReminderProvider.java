package com.example.lab.service.reminder;

import com.example.lab.dto.reminder.ReminderItem;
import com.example.lab.dto.reminder.ReminderModule;
import com.example.lab.dto.reminder.ReminderPriority;
import com.example.lab.entity.Equipment;
import com.example.lab.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExpiringEquipmentReminderProvider implements ReminderProvider {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Override
    public String getKey() {
        return "expiring_equipments";
    }

    @Override
    public ReminderModule buildModule() {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(30);

        List<Equipment> activeEquipments = equipmentRepository.findByStatusNot("SCRAPPED");

        List<Equipment> expiringList = activeEquipments.stream()
                .filter(e -> {
                    if (e.getPurchaseDate() == null || e.getLifeSpan() == null) {
                        return false;
                    }
                    LocalDate expiryDate = e.getPurchaseDate().plusYears(e.getLifeSpan());
                    return !expiryDate.isAfter(futureDate);
                })
                .sorted(Comparator.comparing(e -> e.getPurchaseDate().plusYears(e.getLifeSpan())))
                .collect(Collectors.toList());

        ReminderModule module = new ReminderModule();
        module.setKey(getKey());
        module.setTitle("设备即将到期或已到期");
        module.setDescription("30 天内使用年限到期或已超期的设备（不含报废）");
        module.setActionRoute("/expiring");
        module.setEmptyStateTitle("30 天内没有到期的设备");
        module.setEmptyStateDescription("所有设备都在正常使用年限内");
        module.setTotalCount(expiringList.size());

        List<ReminderItem> items = new ArrayList<>();
        ReminderPriority overallPriority = ReminderPriority.LOW;

        for (Equipment e : expiringList) {
            ReminderItem item = new ReminderItem();
            item.setId(e.getId());

            LocalDate expiryDate = e.getPurchaseDate().plusYears(e.getLifeSpan());
            long remainingDays = ChronoUnit.DAYS.between(today, expiryDate);

            String title = e.getName() != null ? e.getName() : "未知设备";
            if (e.getCode() != null) {
                title = "[" + e.getCode() + "] " + title;
            }
            item.setTitle(title);

            String labName = e.getLab() != null ? e.getLab().getName() : "未分配实验室";
            String remainingText;
            if (remainingDays < 0) {
                remainingText = String.format("已超期 %d 天", Math.abs(remainingDays));
            } else if (remainingDays == 0) {
                remainingText = "今天到期";
            } else {
                remainingText = String.format("剩余 %d 天", remainingDays);
            }
            item.setSubtitle(String.format("%s · %s", labName, remainingText));
            item.setTime(expiryDate.atStartOfDay());

            ReminderPriority priority;
            if (remainingDays < 0) {
                priority = ReminderPriority.HIGH;
            } else if (remainingDays <= 7) {
                priority = ReminderPriority.HIGH;
            } else if (remainingDays <= 15) {
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
            extra.put("code", e.getCode());
            extra.put("model", e.getModel());
            extra.put("manufacturer", e.getManufacturer());
            extra.put("status", e.getStatus());
            extra.put("labId", e.getLab() != null ? e.getLab().getId() : null);
            extra.put("labName", labName);
            extra.put("expiryDate", expiryDate.toString());
            extra.put("remainingDays", remainingDays);
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
