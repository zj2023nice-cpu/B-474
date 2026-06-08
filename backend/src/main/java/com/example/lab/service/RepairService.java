package com.example.lab.service;

import com.example.lab.dto.RepairQuery;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Repair;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepairService {
    @Autowired
    private RepairRepository repairRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;

    @Transactional
    public Repair report(Repair repair) {
        repair.setStatus("REPORTED");
        repair.setReportDate(LocalDateTime.now());
        
        Equipment eq = equipmentRepository.findById(repair.getEquipment().getId()).orElseThrow();
        eq.setStatus("REPAIRING");
        equipmentRepository.save(eq);
        
        return repairRepository.save(repair);
    }

    @Transactional
    public Repair finish(Long repairId) {
        Repair repair = repairRepository.findById(repairId).orElseThrow();
        repair.setStatus("FINISHED");
        repair.setFinishDate(LocalDateTime.now());
        
        repair = repairRepository.save(repair);
        
        Equipment eq = repair.getEquipment();
        if (!repairRepository.hasActiveRepairsByEquipment(eq.getId())) {
            eq.setStatus("NORMAL");
            equipmentRepository.save(eq);
        }
        
        return repair;
    }
    
    public List<Repair> findAll() {
        return repairRepository.findAll();
    }
    
    public Page<Repair> findAll(RepairQuery query) {
        Specification<Repair> spec = createSpecification(query);
        Pageable pageable = createPageable(query);
        return repairRepository.findAll(spec, pageable);
    }
    
    private Specification<Repair> createSpecification(RepairQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Specification<Repair> spec = Specification.where(null);
            
            if (query.getEquipmentId() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("equipment").get("id"), query.getEquipmentId()));
            }
            
            if (StringUtils.hasText(query.getStatus())) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("status"), query.getStatus()));
            }
            
            if (query.getReportDateStart() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.greaterThanOrEqualTo(r.get("reportDate"), query.getReportDateStart()));
            }
            
            if (query.getReportDateEnd() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.lessThanOrEqualTo(r.get("reportDate"), query.getReportDateEnd()));
            }
            
            return spec.toPredicate(root, criteriaQuery, criteriaBuilder);
        };
    }
    
    private Pageable createPageable(RepairQuery query) {
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortOrder()) 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, query.getSortBy());
        return PageRequest.of(query.getPage() - 1, query.getSize(), sort);
    }

    @Transactional
    public void delete(Long id) {
        Repair repair = repairRepository.findById(id).orElseThrow();
        Equipment eq = repair.getEquipment();
        
        repairRepository.deleteById(id);
        
        if (!repairRepository.hasActiveRepairsByEquipment(eq.getId())) {
            eq.setStatus("NORMAL");
            equipmentRepository.save(eq);
        }
    }
}
