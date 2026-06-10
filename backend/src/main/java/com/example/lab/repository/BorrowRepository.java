package com.example.lab.repository;

import com.example.lab.entity.Borrow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface BorrowRepository extends JpaRepository<Borrow, Long>, JpaSpecificationExecutor<Borrow> {
    List<Borrow> findByApplicant_Id(Long applicantId);
    List<Borrow> findByStatus(String status);
    List<Borrow> findByStatusOrderByApplyDateDesc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Borrow b WHERE b.id = :id")
    Optional<Borrow> findByIdWithLock(@Param("id") Long id);
    
    long countByStatus(String status);
    
    @Query("SELECT COUNT(b) FROM Borrow b WHERE b.status = :status AND b.endTime < :now")
    long countOverdue(@Param("status") String status, @Param("now") LocalDateTime now);
    
    Borrow findTopByEquipment_IdOrderByApplyDateDesc(Long equipmentId);

    List<Borrow> findByEquipment_Lab_IdAndStatusIn(Long labId, List<String> statuses);

    Page<Borrow> findAll(Specification<Borrow> spec, Pageable pageable);
    
    @Query("SELECT b FROM Borrow b WHERE b.equipment.id = :equipmentId " +
           "AND b.status IN ('PENDING', 'APPROVED') " +
           "AND b.startTime < :end AND b.endTime > :start " +
           "AND (:excludeId IS NULL OR b.id <> :excludeId)")
    List<Borrow> findConflicts(@Param("equipmentId") Long equipmentId, 
                               @Param("start") LocalDateTime start, 
                               @Param("end") LocalDateTime end,
                               @Param("excludeId") Long excludeId);

    @Query("SELECT b FROM Borrow b WHERE b.status = :status AND b.endTime < :now ORDER BY b.endTime ASC")
    List<Borrow> findOverdue(@Param("status") String status, @Param("now") LocalDateTime now);
}
