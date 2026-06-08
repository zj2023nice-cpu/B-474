package com.example.lab.repository;

import com.example.lab.entity.Repair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RepairRepository extends JpaRepository<Repair, Long>, JpaSpecificationExecutor<Repair> {
    List<Repair> findByEquipment_IdAndStatusNot(Long equipmentId, String status);
    
    long countByStatusNot(String status);
    
    Page<Repair> findAll(Specification<Repair> spec, Pageable pageable);
    
    default boolean hasActiveRepairsByEquipment(Long equipmentId) {
        List<Repair> activeRepairs = findByEquipment_IdAndStatusNot(equipmentId, "FINISHED");
        return !activeRepairs.isEmpty();
    }
}
