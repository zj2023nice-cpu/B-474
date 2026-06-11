package com.example.lab.service;

import com.example.lab.constant.RoleConstant;
import com.example.lab.dto.BorrowQuery;
import com.example.lab.dto.ConflictCheckResult;
import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.User;
import com.example.lab.enums.BorrowStatus;
import com.example.lab.enums.BusinessOperation;
import com.example.lab.enums.EquipmentStatus;
import com.example.lab.exception.BusinessException;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BorrowService {

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StateConstraintService stateConstraintService;

    @Autowired
    private com.example.lab.repository.RepairRepository repairRepository;

    public ConflictCheckResult checkConflicts(Long equipmentId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBorrowId) {
        if (equipmentId == null) {
            throw new BusinessException("请选择要检查的设备");
        }
        if (startTime == null) {
            throw new BusinessException("请选择开始时间");
        }
        if (endTime == null) {
            throw new BusinessException("请选择结束时间");
        }
        if (endTime.isBefore(startTime)) {
            throw new BusinessException("结束时间不能早于开始时间");
        }
        if (endTime.isEqual(startTime)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        List<Borrow> conflicts = findConflictsInternal(equipmentId, startTime, endTime, excludeBorrowId);
        ConflictCheckResult result = new ConflictCheckResult();
        result.setHasConflict(!conflicts.isEmpty());
        result.setConflicts(conflicts.stream()
                .map(ConflictCheckResult.ConflictRecord::new)
                .collect(java.util.stream.Collectors.toList()));
        return result;
    }

    private void checkCanApplyOrManage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("请先登录");
        }
        boolean hasPermission = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(RoleConstant.ROLE_ADMIN) || role.equals(RoleConstant.ROLE_TEACHER));
        if (!hasPermission) {
            throw new BusinessException("权限不足：仅教师和管理员可发起借用申请");
        }
    }

    private void checkCanApprove() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("请先登录");
        }
        boolean hasPermission = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(RoleConstant.ROLE_ADMIN));
        if (!hasPermission) {
            throw new BusinessException("权限不足：仅管理员可审批借用申请");
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("请先登录");
        }
        return (Long) authentication.getPrincipal();
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals(RoleConstant.ROLE_ADMIN));
    }

    private void checkCanCancel(Borrow borrow) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new BusinessException("请先登录");
        }
        Long currentUserId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();
        boolean isApplicant = borrow.getApplicant() != null && borrow.getApplicant().getId().equals(currentUserId);

        if (!isAdmin && !isApplicant) {
            throw new BusinessException("权限不足：仅申请人本人或管理员可取消申请");
        }
    }

    private void validateBorrowApplication(Borrow borrow) {
        LocalDateTime now = LocalDateTime.now();

        if (borrow.getEquipment() == null || borrow.getEquipment().getId() == null) {
            throw new BusinessException("请选择要借用的设备");
        }

        if (borrow.getStartTime() == null) {
            throw new BusinessException("请选择借用开始时间");
        }

        if (borrow.getEndTime() == null) {
            throw new BusinessException("请选择借用结束时间");
        }

        if (borrow.getEndTime().isBefore(borrow.getStartTime())) {
            throw new BusinessException("结束时间不能早于开始时间");
        }

        if (borrow.getEndTime().isEqual(borrow.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        if (borrow.getStartTime().isBefore(now)) {
            throw new BusinessException("开始时间不能早于当前时间");
        }

        if (borrow.getPurpose() != null && borrow.getPurpose().length() > 500) {
            throw new BusinessException("用途说明长度不能超过 500 个字符");
        }
    }

    @Transactional
    public Borrow apply(Borrow borrow) {
        checkCanApplyOrManage();

        validateBorrowApplication(borrow);

        Long currentUserId = getCurrentUserId();
        User applicant = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(404, "当前用户不存在"));

        ConflictCheckResult preCheckResult = checkConflicts(
                borrow.getEquipment().getId(),
                borrow.getStartTime(),
                borrow.getEndTime(),
                null);

        if (preCheckResult.isHasConflict()) {
            throw buildConflictException(preCheckResult.getConflicts(), false);
        }

        Equipment equipment = equipmentRepository.findByIdWithLock(borrow.getEquipment().getId())
                .orElseThrow(() -> new BusinessException(404, "设备不存在"));

        stateConstraintService.assertOperationAllowed(BusinessOperation.APPLY_BORROW, equipment);

        ConflictCheckResult finalCheckResult = checkConflicts(
                borrow.getEquipment().getId(),
                borrow.getStartTime(),
                borrow.getEndTime(),
                null);

        if (finalCheckResult.isHasConflict()) {
            throw buildConflictException(finalCheckResult.getConflicts(), true);
        }

        borrow.setStatus(BorrowStatus.PENDING);
        borrow.setApplyDate(LocalDateTime.now());
        borrow.setApplicant(applicant);
        borrow.setApprover(null);
        borrow.setApproveTime(null);
        borrow.setRejectReason(null);
        borrow.setRejectTime(null);
        borrow.setCancelTime(null);
        borrow.setCancelOperator(null);
        return borrowRepository.save(borrow);
    }

    private BusinessException buildConflictException(List<ConflictCheckResult.ConflictRecord> conflicts, boolean isConcurrent) {
        StringBuilder sb = new StringBuilder();
        if (isConcurrent) {
            sb.append("由于其他用户同时提交申请，");
        }
        sb.append("该时间段设备已被预约，冲突记录：\n");
        for (ConflictCheckResult.ConflictRecord record : conflicts) {
            sb.append(String.format("- %s (%s): %s ~ %s\n",
                    record.getApplicantName(),
                    record.getStatus() == BorrowStatus.APPROVED ? "已批准" : "待审批",
                    record.getStartTime(),
                    record.getEndTime()));
        }
        if (isConcurrent) {
            sb.append("\n请调整时间后重新提交");
        }
        return new BusinessException(sb.toString().trim());
    }

    private List<Borrow> findConflictsInternal(Long equipmentId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBorrowId) {
        if (equipmentId == null || startTime == null || endTime == null) {
            return java.util.Collections.emptyList();
        }
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            return java.util.Collections.emptyList();
        }
        return borrowRepository.findConflicts(equipmentId, startTime, endTime, excludeBorrowId);
    }

    @Transactional
    public Borrow approve(Long borrowId) {
        checkCanApprove();
        Long approverId = getCurrentUserId();

        Borrow borrow = borrowRepository.findByIdWithLock(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        stateConstraintService.assertBorrowOperationAllowed(BusinessOperation.APPROVE_BORROW, borrow);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(404, "审批人不存在"));

        Equipment equipment = equipmentRepository.findByIdWithLock(borrow.getEquipment().getId())
                .orElseThrow(() -> new BusinessException(404, "设备不存在"));

        stateConstraintService.assertWithLock(BusinessOperation.APPROVE_BORROW, equipment.getId());

        ConflictCheckResult conflictCheckResult = checkConflicts(
                equipment.getId(),
                borrow.getStartTime(),
                borrow.getEndTime(),
                borrow.getId());

        if (conflictCheckResult.isHasConflict()) {
            throw buildConflictException(conflictCheckResult.getConflicts(), true);
        }

        borrow.setStatus(BorrowStatus.APPROVED);
        borrow.setApprover(approver);
        borrow.setApproveTime(LocalDateTime.now());
        borrow.setRejectReason(null);
        borrow.setRejectTime(null);

        equipment.setStatus(EquipmentStatus.BORROWED);
        equipmentRepository.save(equipment);

        return borrowRepository.save(borrow);
    }

    @Transactional
    public Borrow reject(Long borrowId, String rejectReason) {
        checkCanApprove();
        Long approverId = getCurrentUserId();

        if (!StringUtils.hasText(rejectReason)) {
            throw new BusinessException("拒绝原因不能为空");
        }

        if (rejectReason.length() > 500) {
            throw new BusinessException("拒绝原因长度不能超过 500 个字符");
        }

        Borrow borrow = borrowRepository.findByIdWithLock(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        stateConstraintService.assertBorrowOperationAllowed(BusinessOperation.APPROVE_BORROW, borrow);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(404, "审批人不存在"));

        borrow.setStatus(BorrowStatus.REJECTED);
        borrow.setApprover(approver);
        borrow.setRejectReason(rejectReason.trim());
        borrow.setRejectTime(LocalDateTime.now());
        borrow.setApproveTime(null);

        return borrowRepository.save(borrow);
    }

    @Transactional
    public Borrow returnEquipment(Long borrowId) {
        checkCanApplyOrManage();
        Borrow borrow = borrowRepository.findByIdWithLock(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        stateConstraintService.assertBorrowOperationAllowed(BusinessOperation.RETURN_EQUIPMENT, borrow);

        borrow.setStatus(BorrowStatus.RETURNED);

        Equipment equipment = borrow.getEquipment();
        if (equipment != null) {
            equipment = equipmentRepository.findByIdWithLock(equipment.getId())
                    .orElseThrow(() -> new BusinessException(404, "设备不存在"));
            if (!repairRepository.hasActiveRepairsByEquipment(equipment.getId())) {
                equipment.setStatus(EquipmentStatus.NORMAL);
            }
            equipmentRepository.save(equipment);
        }

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
                    cb.equal(r.get("status"), BorrowStatus.valueOf(query.getStatus())));
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

    @Transactional
    public Borrow cancel(Long borrowId) {
        Long currentUserId = getCurrentUserId();

        Borrow borrow = borrowRepository.findByIdWithLock(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        checkCanCancel(borrow);

        stateConstraintService.assertBorrowOperationAllowed(BusinessOperation.CANCEL_BORROW, borrow);

        User operator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(404, "操作人不存在"));

        borrow.setStatus(BorrowStatus.CANCELLED);
        borrow.setCancelTime(LocalDateTime.now());
        borrow.setCancelOperator(operator.getName());

        Equipment equipment = borrow.getEquipment();
        if (equipment != null && equipment.getStatus() == EquipmentStatus.BORROWED) {
            equipment = equipmentRepository.findByIdWithLock(equipment.getId())
                    .orElseThrow(() -> new BusinessException(404, "设备不存在"));
            if (!repairRepository.hasActiveRepairsByEquipment(equipment.getId())) {
                equipment.setStatus(EquipmentStatus.NORMAL);
            }
            equipmentRepository.save(equipment);
        }

        return borrowRepository.save(borrow);
    }

    @Transactional
    public void delete(Long id) {
        checkCanApprove();

        Borrow borrow = borrowRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        Equipment equipment = borrow.getEquipment();
        if (equipment != null && equipment.getStatus() == EquipmentStatus.BORROWED) {
            equipment = equipmentRepository.findByIdWithLock(equipment.getId())
                    .orElseThrow(() -> new BusinessException(404, "设备不存在"));
            if (!repairRepository.hasActiveRepairsByEquipment(equipment.getId())) {
                equipment.setStatus(EquipmentStatus.NORMAL);
            }
            equipmentRepository.save(equipment);
        }

        borrowRepository.deleteById(id);
    }

}
