package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.common.PageResponse;
import com.example.lab.dto.EquipmentDetailDTO;
import com.example.lab.dto.EquipmentQuery;
import com.example.lab.dto.ExpiringEquipmentDTO;
import com.example.lab.dto.ExpiringQuery;
import com.example.lab.entity.Equipment;
import com.example.lab.service.EquipmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {
    @Autowired
    private EquipmentService equipmentService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<PageResponse<Equipment>> getAll(EquipmentQuery query) {
        Page<Equipment> page = equipmentService.findAll(query);
        PageResponse<Equipment> response = new PageResponse<>(page);
        return ApiResponse.success(response);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping
    public ApiResponse<Equipment> create(@Valid @RequestBody Equipment equipment) {
        Equipment savedEquipment = equipmentService.addEquipment(equipment);
        return ApiResponse.success("创建成功", savedEquipment);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PutMapping
    public ApiResponse<Equipment> update(@Valid @RequestBody Equipment equipment) {
        Equipment updatedEquipment = equipmentService.updateEquipment(equipment);
        return ApiResponse.success("更新成功", updatedEquipment);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        equipmentService.deleteEquipment(id);
        return ApiResponse.success("删除成功", null);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}/detail")
    public ApiResponse<EquipmentDetailDTO> getDetail(@PathVariable Long id) {
        EquipmentDetailDTO detail = equipmentService.getDetail(id);
        return ApiResponse.success(detail);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/expiring")
    public ApiResponse<PageResponse<ExpiringEquipmentDTO>> getExpiring(ExpiringQuery query) {
        Page<ExpiringEquipmentDTO> page = equipmentService.findExpiring(query);
        PageResponse<ExpiringEquipmentDTO> response = new PageResponse<>(page);
        return ApiResponse.success(response);
    }
}
