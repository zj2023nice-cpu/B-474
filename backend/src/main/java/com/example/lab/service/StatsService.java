package com.example.lab.service;

import com.example.lab.dto.PendingItemDTO;
import com.example.lab.entity.Borrow;
import com.example.lab.entity.Repair;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private BorrowRepository borrowRepository;
    
    @Autowired
    private RepairRepository repairRepository;
    
    public Map<String, Long> getDashboardStats() {
        Map<String, Long> stats = new HashMap<>();
        
        stats.put("equipmentCount", equipmentRepository.count());
        stats.put("borrowCount", borrowRepository.countByStatus("APPROVED"));
        stats.put("overdue", borrowRepository.countOverdue("APPROVED", LocalDateTime.now()));
        stats.put("repairCount", repairRepository.countByStatusNot("FINISHED"));
        stats.put("pendingBorrowCount", borrowRepository.countByStatus("PENDING"));
        stats.put("recentRepairCount", repairRepository.countByReportDateAfter(LocalDateTime.now().minusDays(7)));
        
        return stats;
    }

    public List<PendingItemDTO> getPendingItems() {
        List<PendingItemDTO> items = new ArrayList<>();

        List<Borrow> pendingBorrows = borrowRepository.findByStatusOrderByApplyDateDesc("PENDING");
        for (Borrow b : pendingBorrows) {
            PendingItemDTO dto = new PendingItemDTO();
            dto.setId(b.getId());
            dto.setType("BORROW");
            dto.setEquipmentName(b.getEquipment() != null ? b.getEquipment().getName() : "");
            dto.setStatus(b.getStatus());
            dto.setTime(b.getApplyDate());
            dto.setUserName(b.getApplicant() != null ? b.getApplicant().getName() : "");
            dto.setDescription(b.getPurpose());
            items.add(dto);
        }

        List<Repair> unfinishedRepairs = repairRepository.findByStatusNotOrderByReportDateDesc("FINISHED");
        for (Repair r : unfinishedRepairs) {
            PendingItemDTO dto = new PendingItemDTO();
            dto.setId(r.getId());
            dto.setType("REPAIR");
            dto.setEquipmentName(r.getEquipment() != null ? r.getEquipment().getName() : "");
            dto.setStatus(r.getStatus());
            dto.setTime(r.getReportDate());
            dto.setUserName(r.getReporter() != null ? r.getReporter().getName() : "");
            dto.setDescription(r.getDescription());
            items.add(dto);
        }

        items.sort((a, b) -> {
            if (a.getTime() == null && b.getTime() == null) return 0;
            if (a.getTime() == null) return 1;
            if (b.getTime() == null) return -1;
            return b.getTime().compareTo(a.getTime());
        });

        return items;
    }
}
