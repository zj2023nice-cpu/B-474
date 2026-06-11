package com.example.lab.repository;

import com.example.lab.entity.Repair;
import com.example.lab.enums.RepairStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

public interface RepairRepository extends JpaRepository<Repair, Long>, JpaSpecificationExecutor<Repair> {
    List<Repair> findByEquipment_IdAndStatusNot(Long equipmentId, RepairStatus status);
    List<Repair> findByStatusNotOrderByReportDateDesc(RepairStatus status);
    
    long countByStatusNot(RepairStatus status);
    
    long countByReportDateAfter(LocalDateTime date);
    
    Repair findTopByEquipment_IdOrderByReportDateDesc(Long equipmentId);

    List<Repair> findByEquipment_Lab_IdAndStatusNot(Long labId, RepairStatus status);

    Page<Repair> findAll(Specification<Repair> spec, Pageable pageable);
    
    default boolean hasActiveRepairsByEquipment(Long equipmentId) {
        List<Repair> activeRepairs = findByEquipment_IdAndStatusNot(equipmentId, RepairStatus.FINISHED);
        return !activeRepairs.isEmpty();
    }
}
