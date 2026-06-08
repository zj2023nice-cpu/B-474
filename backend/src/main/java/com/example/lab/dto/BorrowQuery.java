package com.example.lab.dto;

import java.time.LocalDateTime;

public class BorrowQuery {
    private Integer page = 1;
    private Integer size = 10;
    private Long userId;
    private Long equipmentId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String sortBy = "id";
    private String sortOrder = "desc";

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        if (page != null && page > 0) {
            this.page = page;
        }
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        if (size != null && size > 0 && size <= 100) {
            this.size = size;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        if (sortBy != null && !sortBy.isEmpty()) {
            this.sortBy = sortBy;
        }
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        if ("asc".equalsIgnoreCase(sortOrder) || "desc".equalsIgnoreCase(sortOrder)) {
            this.sortOrder = sortOrder;
        }
    }
}
