package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.common.PageResponse;
import com.example.lab.dto.LabDetailDTO;
import com.example.lab.dto.LabQuery;
import com.example.lab.entity.Lab;
import com.example.lab.service.LabService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/labs")
public class LabController {
    @Autowired
    private LabService labService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<PageResponse<Lab>> getAll(LabQuery query) {
        Page<Lab> page = labService.findAll(query);
        PageResponse<Lab> response = new PageResponse<>(page);
        return ApiResponse.success(response);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/detail")
    public ApiResponse<LabDetailDTO> getDetail(@PathVariable Long id) {
        LabDetailDTO detail = labService.getDetail(id);
        return ApiResponse.success(detail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<Lab> create(@Valid @RequestBody Lab lab) {
        Lab savedLab = labService.save(lab);
        return ApiResponse.success("创建成功", savedLab);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<Lab> update(@PathVariable Long id, @Valid @RequestBody Lab lab) {
        lab.setId(id);
        Lab updatedLab = labService.save(lab);
        return ApiResponse.success("更新成功", updatedLab);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return ApiResponse.success("删除成功", null);
    }
}
