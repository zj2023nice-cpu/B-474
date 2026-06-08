package com.example.lab.service;

import com.example.lab.dto.BorrowQuery;
import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class BorrowService {
    @Autowired
    private BorrowRepository borrowRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;

    @Transactional
    public Borrow apply(Borrow borrow) {
        List<Borrow> conflicts = borrowRepository.findConflicts(
                borrow.getEquipment().getId(), 
                borrow.getStartTime(), 
                borrow.getEndTime());
        
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("该时间段设备已被预约");
        }
        
        borrow.setStatus("PENDING");
        return borrowRepository.save(borrow);
    }

    @Transactional
    public Borrow approve(Long borrowId, Long approverId) {
        Borrow borrow = borrowRepository.findById(borrowId).orElseThrow();
        borrow.setStatus("APPROVED");
        Equipment eq = borrow.getEquipment();
        eq.setStatus("BORROWED");
        equipmentRepository.save(eq);
        
        return borrowRepository.save(borrow);
    }
    
    @Transactional
    public Borrow reject(Long borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId).orElseThrow();
        borrow.setStatus("REJECTED");
        return borrowRepository.save(borrow);
    }

    @Transactional
    public Borrow returnEquipment(Long borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId).orElseThrow();
        borrow.setStatus("RETURNED");
        
        Equipment eq = borrow.getEquipment();
        eq.setStatus("NORMAL");
        equipmentRepository.save(eq);
        
        return borrowRepository.save(borrow);
    }

    public List<Borrow> findAll() {
        return borrowRepository.findAll();
    }
    
    public List<Borrow> findByApplicant(Long applicantId) {
        return borrowRepository.findByApplicant_Id(applicantId);
    }
    
    public Page<Borrow> findAll(BorrowQuery query) {
        Specification<Borrow> spec = createSpecification(query);
        Pageable pageable = createPageable(query);
        return borrowRepository.findAll(spec, pageable);
    }
    
    private Specification<Borrow> createSpecification(BorrowQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Specification<Borrow> spec = Specification.where(null);
            
            if (query.getUserId() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("applicant").get("id"), query.getUserId()));
            }
            
            if (query.getEquipmentId() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("equipment").get("id"), query.getEquipmentId()));
            }
            
            if (StringUtils.hasText(query.getStatus())) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("status"), query.getStatus()));
            }
            
            if (query.getStartTime() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.greaterThanOrEqualTo(r.get("startTime"), query.getStartTime()));
            }
            
            if (query.getEndTime() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.lessThanOrEqualTo(r.get("endTime"), query.getEndTime()));
            }
            
            return spec.toPredicate(root, criteriaQuery, criteriaBuilder);
        };
    }
    
    private Pageable createPageable(BorrowQuery query) {
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortOrder()) 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, query.getSortBy());
        return PageRequest.of(query.getPage() - 1, query.getSize(), sort);
    }

    public void delete(Long id) {
        borrowRepository.deleteById(id);
    }
}
