package com.example.lab.service;

import com.example.lab.dto.FinishRepairRequest;
import com.example.lab.dto.RepairQuery;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Repair;
import com.example.lab.exception.BusinessException;
import com.example.lab.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class RepairService {

    private static final List<String> ACTIVE_STATUSES = Arrays.asList("REPORTED", "IN_PROGRESS");
    private static final String STATUS_FINISHED = "FINISHED";
    private static final String STATUS_REPORTED = "REPORTED";
    private static final String EQUIPMENT_STATUS_REPAIRING = "REPAIRING";
    private static final String EQUIPMENT_STATUS_NORMAL = "NORMAL";

    @Autowired
    private RepairRepository repairRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;

    @Transactional
    public Repair report(Repair repair) {
        repair.setStatus(STATUS_REPORTED);
        repair.setReportDate(LocalDateTime.now());
        repair.setRepairConclusion(null);
        repair.setRepairCompany(null);
        repair.setCost(null);
        repair.setFinishDate(null);
        
        Equipment eq = equipmentRepository.findById(repair.getEquipment().getId())
            .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));
        
        eq.setStatus(EQUIPMENT_STATUS_REPAIRING);
        equipmentRepository.save(eq);
        
        return repairRepository.save(repair);
    }

    @Transactional
    public Repair finish(Long repairId, FinishRepairRequest request) {
        Repair repair = repairRepository.findById(repairId)
            .orElseThrow(() -> new ResourceNotFoundException("维修记录不存在"));
        
        if (STATUS_FINISHED.equals(repair.getStatus())) {
            throw new BusinessException("该维修记录已完成，请勿重复操作");
        }
        
        if (!ACTIVE_STATUSES.contains(repair.getStatus())) {
            throw new BusinessException("当前状态不允许完成维修");
        }
        
        String conclusion = request.getRepairConclusion();
        if (!StringUtils.hasText(conclusion)) {
            throw new BusinessException("维修结论不能为空");
        }
        
        repair.setStatus(STATUS_FINISHED);
        repair.setFinishDate(LocalDateTime.now());
        repair.setRepairConclusion(conclusion.trim());
        
        String company = request.getRepairCompany();
        repair.setRepairCompany(StringUtils.hasText(company) ? company.trim() : null);
        
        BigDecimal cost = request.getCost();
        repair.setCost(cost != null && cost.compareTo(BigDecimal.ZERO) >= 0 ? cost : null);
        
        repair = repairRepository.save(repair);
        
        Equipment eq = repair.getEquipment();
        if (!repairRepository.hasActiveRepairsByEquipment(eq.getId())) {
            eq.setStatus(EQUIPMENT_STATUS_NORMAL);
            equipmentRepository.save(eq);
        }
        
        return repair;
    }
    
    public Repair findById(Long id) {
        return repairRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("维修记录不存在"));
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
        Repair repair = repairRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("维修记录不存在"));
        Equipment eq = repair.getEquipment();
        
        repairRepository.deleteById(id);
        
        if (!repairRepository.hasActiveRepairsByEquipment(eq.getId())) {
            eq.setStatus(EQUIPMENT_STATUS_NORMAL);
            equipmentRepository.save(eq);
        }
    }
}
