package com.example.lab.repository;

import com.example.lab.entity.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {
    List<Equipment> findByLab_Id(Long labId);
    List<Equipment> findByStatus(String status);
    
    Page<Equipment> findAll(Specification<Equipment> spec, Pageable pageable);
    
    List<Equipment> findByStatusNot(String status);

    long countByLab_Id(Long labId);
    long countByLab_IdAndStatus(Long labId, String status);
}
