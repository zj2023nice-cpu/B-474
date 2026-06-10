package com.example.lab.service;

import com.example.lab.dto.BorrowQuery;
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

    @Transactional
    public Borrow apply(Borrow borrow) {
        List<Borrow> conflicts = borrowRepository.findConflicts(
                borrow.getEquipment().getId(),
                borrow.getStartTime(),
                borrow.getEndTime());

        if (!conflicts.isEmpty()) {
            throw new BusinessException("该时间段设备已被预约");
        }

        borrow.setStatus("PENDING");
        return borrowRepository.save(borrow);
    }

    @Transactional
    public Borrow approve(Long borrowId, Long approverId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        if (!"PENDING".equals(borrow.getStatus())) {
            throw new BusinessException("当前状态不允许审批，仅待审批状态的申请可以批准");
        }

        if (borrow.getApprover() != null) {
            throw new BusinessException("该申请已被审批，请勿重复操作");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(404, "审批人不存在"));

        borrow.setStatus("APPROVED");
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
    public Borrow reject(Long borrowId, Long approverId, String rejectReason) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        if (!"PENDING".equals(borrow.getStatus())) {
            throw new BusinessException("当前状态不允许审批，仅待审批状态的申请可以拒绝");
        }

        if (borrow.getApprover() != null) {
            throw new BusinessException("该申请已被审批，请勿重复操作");
        }

        if (!StringUtils.hasText(rejectReason)) {
            throw new BusinessException("拒绝原因不能为空");
        }

        if (rejectReason.length() > 500) {
            throw new BusinessException("拒绝原因长度不能超过 500 个字符");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new BusinessException(404, "审批人不存在"));

        borrow.setStatus("REJECTED");
        borrow.setApprover(approver);
        borrow.setRejectReason(rejectReason.trim());
        borrow.setRejectTime(LocalDateTime.now());
        borrow.setApproveTime(null);

        return borrowRepository.save(borrow);
    }

    @Transactional
    public Borrow returnEquipment(Long borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));

        if (!"APPROVED".equals(borrow.getStatus())) {
            throw new BusinessException("当前状态不允许归还，仅已批准状态的借用可以归还");
        }

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
