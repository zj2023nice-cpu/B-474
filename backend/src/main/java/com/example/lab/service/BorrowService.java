package com.example.lab.service;

import com.example.lab.constant.RoleConstant;
import com.example.lab.dto.BorrowQuery;
import com.example.lab.dto.ConflictCheckResult;
import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.User;
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

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_RETURNED = "RETURNED";

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private UserRepository userRepository;

    public ConflictCheckResult checkConflicts(Long equipmentId, LocalDateTime startTime, LocalDateTime endTime, Long excludeBorrowId) {
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

    @Transactional
    public Borrow apply(Borrow borrow) {
        checkCanApplyOrManage();
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

        if (!"NORMAL".equals(equipment.getStatus())) {
            throw new BusinessException("设备当前状态不可借用：" + equipment.getStatus());
        }

        ConflictCheckResult finalCheckResult = checkConflicts(
                borrow.getEquipment().getId(),
                borrow.getStartTime(),
                borrow.getEndTime(),
                null);

        if (finalCheckResult.isHasConflict()) {
            throw buildConflictException(finalCheckResult.getConflicts(), true);
        }

        borrow.setStatus(STATUS_PENDING);
        borrow.setApplyDate(LocalDateTime.now());
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
                    "APPROVED".equals(record.getStatus()) ? "已批准" : "待审批",
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

        validateCanApprove(borrow);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(404, "审批人不存在"));

        borrow.setStatus(STATUS_APPROVED);
        borrow.setApprover(approver);
        borrow.setApproveTime(LocalDateTime.now());
        borrow.setRejectReason(null);
        borrow.setRejectTime(null);

        Equipment eq = borrow.getEquipment();
        eq.setStatus("BORROWED");
        equipmentRepository.save(eq);

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

        validateCanApprove(borrow);

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(404, "审批人不存在"));

        borrow.setStatus(STATUS_REJECTED);
        borrow.setApprover(approver);
        borrow.setRejectReason(rejectReason.trim());
        borrow.setRejectTime(LocalDateTime.now());
        borrow.setApproveTime(null);

        return borrowRepository.save(borrow);
    }

    private void validateCanApprove(Borrow borrow) {
        String status = borrow.getStatus();
        if (STATUS_APPROVED.equals(status)) {
            throw new BusinessException("该申请已被批准，无需重复审批");
        }
        if (STATUS_REJECTED.equals(status)) {
            throw new BusinessException("该申请已被拒绝，无法重复审批");
        }
        if (STATUS_RETURNED.equals(status)) {
            throw new BusinessException("该借用已归还，无法审批");
        }
        if (!STATUS_PENDING.equals(status)) {
            throw new BusinessException("当前状态不允许审批，仅待审批状态的申请可以审批");
        }
        if (borrow.getApprover() != null) {
            throw new BusinessException("该申请已被审批，请勿重复操作");
        }
    }

    @Transactional
    public Borrow returnEquipment(Long borrowId) {
        checkCanApplyOrManage();
        Borrow borrow = borrowRepository.findByIdWithLock(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        if (!STATUS_APPROVED.equals(borrow.getStatus())) {
            throw new BusinessException("当前状态不允许归还，仅已批准状态的借用可以归还");
        }

        borrow.setStatus(STATUS_RETURNED);

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
