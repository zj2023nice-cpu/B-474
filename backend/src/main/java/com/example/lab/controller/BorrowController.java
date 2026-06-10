package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.common.PageResponse;
import com.example.lab.dto.ApprovalRequest;
import com.example.lab.dto.BorrowQuery;
import com.example.lab.dto.ConflictCheckResult;
import com.example.lab.entity.Borrow;
import com.example.lab.service.BorrowService;

import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/borrows")
public class BorrowController {
    @Autowired
    private BorrowService borrowService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<PageResponse<Borrow>> getAll(BorrowQuery query) {
        Page<Borrow> page = borrowService.findAll(query);
        PageResponse<Borrow> response = new PageResponse<>(page);
        return ApiResponse.success(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/check-conflicts")
    public ApiResponse<ConflictCheckResult> checkConflicts(
            @RequestParam Long equipmentId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Long excludeBorrowId) {
        ConflictCheckResult result = borrowService.checkConflicts(equipmentId, startTime, endTime, excludeBorrowId);
        return ApiResponse.success(result);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping
    public ApiResponse<Borrow> apply(@Valid @RequestBody Borrow borrow) {
        Borrow savedBorrow = borrowService.apply(borrow);
        return ApiResponse.success("申请成功", savedBorrow);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/approve")
    public ApiResponse<Borrow> approve(@PathVariable Long id) {
        Borrow approvedBorrow = borrowService.approve(id);
        return ApiResponse.success("审批通过", approvedBorrow);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reject")
    public ApiResponse<Borrow> reject(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        Borrow rejectedBorrow = borrowService.reject(id, request.getRejectReason());
        return ApiResponse.success("已拒绝", rejectedBorrow);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}/return")
    public ApiResponse<Borrow> returnEquipment(@PathVariable Long id) {
        Borrow returnedBorrow = borrowService.returnEquipment(id);
        return ApiResponse.success("归还成功", returnedBorrow);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        borrowService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
