package com.example.lab.service.reminder;

import com.example.lab.dto.reminder.ReminderItem;
import com.example.lab.dto.reminder.ReminderModule;
import com.example.lab.dto.reminder.ReminderPriority;
import com.example.lab.entity.Borrow;
import com.example.lab.repository.BorrowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OverdueBorrowReminderProvider implements ReminderProvider {

    @Autowired
    private BorrowRepository borrowRepository;

    @Override
    public String getKey() {
        return "overdue_borrows";
    }

    @Override
    public ReminderModule buildModule() {
        LocalDateTime now = LocalDateTime.now();
        List<Borrow> overdues = borrowRepository.findOverdue("APPROVED", now);

        ReminderModule module = new ReminderModule();
        module.setKey(getKey());
        module.setTitle("超期未归还");
        module.setDescription("已超过应归还时间但尚未归还的借用记录");
        module.setActionRoute("/borrows");
        module.setEmptyStateTitle("没有超期未归还的借用");
        module.setEmptyStateDescription("所有借用都按时归还，做得好！");
        module.setTotalCount(overdues.size());

        List<ReminderItem> items = new ArrayList<>();
        ReminderPriority overallPriority = ReminderPriority.LOW;

        for (Borrow b : overdues) {
            ReminderItem item = new ReminderItem();
            item.setId(b.getId());

            long overdueHours = Duration.between(b.getEndTime(), now).toHours();
            long overdueDays = overdueHours / 24;

            String title = b.getEquipment() != null ? b.getEquipment().getName() : "未知设备";
            if (b.getEquipment() != null && b.getEquipment().getCode() != null) {
                title = "[" + b.getEquipment().getCode() + "] " + title;
            }
            item.setTitle(title);

            String applicantName = b.getApplicant() != null ? b.getApplicant().getName() : "未知用户";
            String overdueText;
            if (overdueDays > 0) {
                overdueText = String.format("超期 %d 天", overdueDays);
            } else {
                overdueText = String.format("超期 %d 小时", Math.max(1, overdueHours));
            }
            item.setSubtitle(String.format("%s · %s", applicantName, overdueText));
            item.setTime(b.getEndTime());

            ReminderPriority priority;
            if (overdueDays >= 7) {
                priority = ReminderPriority.HIGH;
            } else if (overdueDays >= 3 || overdueHours >= 24) {
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
            extra.put("equipmentId", b.getEquipment() != null ? b.getEquipment().getId() : null);
            extra.put("applicantId", b.getApplicant() != null ? b.getApplicant().getId() : null);
            extra.put("applicantName", applicantName);
            extra.put("endTime", b.getEndTime());
            extra.put("purpose", b.getPurpose());
            extra.put("overdueDays", overdueDays);
            extra.put("overdueHours", overdueHours);
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
        if (!items.isEmpty() && overallPriority == ReminderPriority.LOW) {
            overallPriority = ReminderPriority.MEDIUM;
        }
        module.setOverallPriority(overallPriority);

        return module;
    }
}
