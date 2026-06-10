package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.common.PageResponse;
import com.example.lab.dto.FinishRepairRequest;
import com.example.lab.dto.RepairQuery;
import com.example.lab.entity.Repair;
import com.example.lab.service.RepairService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repairs")
public class RepairController {
    @Autowired
    private RepairService repairService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<PageResponse<Repair>> getAll(RepairQuery query) {
        Page<Repair> page = repairService.findAll(query);
        PageResponse<Repair> response = new PageResponse<>(page);
        return ApiResponse.success(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ApiResponse<Repair> getById(@PathVariable Long id) {
        Repair repair = repairService.findById(id);
        return ApiResponse.success(repair);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ApiResponse<Repair> report(@Valid @RequestBody Repair repair) {
        Repair savedRepair = repairService.report(repair);
        return ApiResponse.success("报修成功", savedRepair);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping("/{id}/finish")
    public ApiResponse<Repair> finish(@PathVariable Long id, @Valid @RequestBody FinishRepairRequest request) {
        Repair finishedRepair = repairService.finish(id, request);
        return ApiResponse.success("维修完成", finishedRepair);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        repairService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
