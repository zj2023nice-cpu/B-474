package com.example.lab.service;

import com.example.lab.constant.RoleConstant;
import com.example.lab.dto.FinishRepairRequest;
import com.example.lab.dto.RepairQuery;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Repair;
import com.example.lab.entity.User;
import com.example.lab.enums.BusinessOperation;
import com.example.lab.enums.EquipmentStatus;
import com.example.lab.enums.RepairStatus;
import com.example.lab.exception.BusinessException;
import com.example.lab.exception.ResourceNotFoundException;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.RepairRepository;
import com.example.lab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RepairService {

    @Autowired
    private RepairRepository repairRepository;
    
    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StateConstraintService stateConstraintService;

    private void checkCanReportOrManage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("请先登录");
        }
        boolean hasPermission = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(RoleConstant.ROLE_ADMIN) || role.equals(RoleConstant.ROLE_TEACHER));
        if (!hasPermission) {
            throw new BusinessException("权限不足：仅教师和管理员可发起报修");
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("请先登录");
        }
        return (Long) authentication.getPrincipal();
    }

    @Transactional
    public Repair report(Repair repair) {
        checkCanReportOrManage();

        if (repair.getEquipment() == null || repair.getEquipment().getId() == null) {
            throw new BusinessException("请选择要报修的设备");
        }

        Long currentUserId = getCurrentUserId();
        User reporter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(404, "当前用户不存在"));

        Equipment equipment = equipmentRepository.findByIdWithLock(repair.getEquipment().getId())
                .orElseThrow(() -> new ResourceNotFoundException("设备不存在"));

        stateConstraintService.assertOperationAllowed(BusinessOperation.REPORT_REPAIR, equipment);

        repair.setStatus(RepairStatus.REPORTED);
        repair.setReportDate(LocalDateTime.now());
        repair.setRepairConclusion(null);
        repair.setRepairCompany(null);
        repair.setCost(null);
        repair.setFinishDate(null);
        repair.setReporter(reporter);
        
        equipment.setStatus(EquipmentStatus.REPAIRING);
        equipmentRepository.save(equipment);
        
        return repairRepository.save(repair);
    }

    @Transactional
    public Repair finish(Long repairId, FinishRepairRequest request) {
        Repair repair = repairRepository.findById(repairId)
            .orElseThrow(() -> new ResourceNotFoundException("维修记录不存在"));

        stateConstraintService.assertRepairOperationAllowed(BusinessOperation.FINISH_REPAIR, repair);

        String conclusion = request.getRepairConclusion();
        if (!StringUtils.hasText(conclusion)) {
            throw new BusinessException("维修结论不能为空");
        }
        
        repair.setStatus(RepairStatus.FINISHED);
        repair.setFinishDate(LocalDateTime.now());
        repair.setRepairConclusion(conclusion.trim());
        
        String company = request.getRepairCompany();
        repair.setRepairCompany(StringUtils.hasText(company) ? company.trim() : null);
        
        BigDecimal cost = request.getCost();
        repair.setCost(cost != null && cost.compareTo(BigDecimal.ZERO) >= 0 ? cost : null);
        
        repair = repairRepository.save(repair);
        
        Equipment equipment = repair.getEquipment();
        equipment = equipmentRepository.findByIdWithLock(equipment.getId())
                .orElseThrow(() -> new BusinessException(404, "设备不存在"));
        if (!repairRepository.hasActiveRepairsByEquipment(equipment.getId())) {
            equipment.setStatus(EquipmentStatus.NORMAL);
        }
        equipmentRepository.save(equipment);
        
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
            
            if (query.getUserId() != null) {
                spec = spec.and((r, q, cb) ->
                    cb.equal(r.get("reporter").get("id"), query.getUserId()));
            }
            
            if (query.getEquipmentId() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("equipment").get("id"), query.getEquipmentId()));
            }
            
            if (StringUtils.hasText(query.getStatus())) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("status"), RepairStatus.valueOf(query.getStatus())));
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

        stateConstraintService.assertRepairOperationAllowed(BusinessOperation.CANCEL_REPAIR, repair);

        Equipment equipment = repair.getEquipment();
        
        repairRepository.deleteById(id);
        
        equipment = equipmentRepository.findByIdWithLock(equipment.getId())
                .orElseThrow(() -> new BusinessException(404, "设备不存在"));
        if (!repairRepository.hasActiveRepairsByEquipment(equipment.getId())) {
            equipment.setStatus(EquipmentStatus.NORMAL);
        }
        equipmentRepository.save(equipment);
    }
}
