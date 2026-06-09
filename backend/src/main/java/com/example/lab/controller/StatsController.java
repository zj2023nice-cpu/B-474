package com.example.lab.controller;

import com.example.lab.common.ApiResponse;
import com.example.lab.dto.PendingItemDTO;
import com.example.lab.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {
    @Autowired
    private StatsService statsService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ApiResponse<Map<String, Long>> getDashboardStats() {
        Map<String, Long> stats = statsService.getDashboardStats();
        return ApiResponse.success(stats);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/pending-items")
    public ApiResponse<List<PendingItemDTO>> getPendingItems() {
        List<PendingItemDTO> items = statsService.getPendingItems();
        return ApiResponse.success(items);
    }
}
