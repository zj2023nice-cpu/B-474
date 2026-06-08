package com.example.lab.repository;

import com.example.lab.entity.Lab;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface LabRepository extends JpaRepository<Lab, Long>, JpaSpecificationExecutor<Lab> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Lab> findById(Long id);
    
    Page<Lab> findAll(Specification<Lab> spec, Pageable pageable);
}
