package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.common.PageResponse;
import com.example.lab.dto.ApprovalRequest;
import com.example.lab.dto.BorrowQuery;
import com.example.lab.entity.Borrow;
import com.example.lab.service.BorrowService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ApiResponse<Borrow> apply(@Valid @RequestBody Borrow borrow) {
        Borrow savedBorrow = borrowService.apply(borrow);
        return ApiResponse.success("申请成功", savedBorrow);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}/approve")
    public ApiResponse<Borrow> approve(@PathVariable Long id) {
        Long approverId = getCurrentUserId();
        Borrow approvedBorrow = borrowService.approve(id, approverId);
        return ApiResponse.success("审批通过", approvedBorrow);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}/reject")
    public ApiResponse<Borrow> reject(@PathVariable Long id, @Valid @RequestBody ApprovalRequest request) {
        Long approverId = getCurrentUserId();
        Borrow rejectedBorrow = borrowService.reject(id, approverId, request.getRejectReason());
        return ApiResponse.success("已拒绝", rejectedBorrow);
    }

    @PreAuthorize("isAuthenticated()")
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

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (Long) authentication.getPrincipal();
    }
}
