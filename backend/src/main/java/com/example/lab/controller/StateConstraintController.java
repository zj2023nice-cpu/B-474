package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.dto.StateConstraintCheckResult;
import com.example.lab.enums.BusinessOperation;
import com.example.lab.service.StateConstraintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/state-constraints")
public class StateConstraintController {

    @Autowired
    private StateConstraintService stateConstraintService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/equipment/{equipmentId}/check")
    public ApiResponse<StateConstraintCheckResult> checkEquipmentOperation(
            @PathVariable Long equipmentId,
            @RequestParam String operation) {
        BusinessOperation op = BusinessOperation.fromCode(operation);
        if (op == null) {
            return ApiResponse.error("无效的操作类型");
        }
        StateConstraintCheckResult result = stateConstraintService.checkOperation(op, equipmentId);
        return ApiResponse.success(result);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/equipment/{equipmentId}/available-operations")
    public ApiResponse<List<StateConstraintCheckResult>> checkAllEquipmentOperations(
            @PathVariable Long equipmentId) {
        List<StateConstraintCheckResult> results = new ArrayList<>();
        BusinessOperation[] operations = {
                BusinessOperation.APPLY_BORROW,
                BusinessOperation.REPORT_REPAIR
        };
        for (BusinessOperation op : operations) {
            StateConstraintCheckResult result = stateConstraintService.checkOperation(op, equipmentId);
            results.add(result);
        }
        return ApiResponse.success(results);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/borrow/{borrowId}/check")
    public ApiResponse<StateConstraintCheckResult> checkBorrowOperation(
            @PathVariable Long borrowId,
            @RequestParam String operation) {
        BusinessOperation op = BusinessOperation.fromCode(operation);
        if (op == null) {
            return ApiResponse.error("无效的操作类型");
        }
        StateConstraintCheckResult result = stateConstraintService.checkBorrowOperation(op, borrowId);
        return ApiResponse.success(result);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/repair/{repairId}/check")
    public ApiResponse<StateConstraintCheckResult> checkRepairOperation(
            @PathVariable Long repairId,
            @RequestParam String operation) {
        BusinessOperation op = BusinessOperation.fromCode(operation);
        if (op == null) {
            return ApiResponse.error("无效的操作类型");
        }
        StateConstraintCheckResult result = stateConstraintService.checkRepairOperation(op, repairId);
        return ApiResponse.success(result);
    }
}
