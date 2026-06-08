package com.example.lab.service;

import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
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
        
        return stats;
    }
}
