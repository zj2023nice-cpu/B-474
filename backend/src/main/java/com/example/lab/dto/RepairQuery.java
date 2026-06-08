package com.example.lab.dto;

import java.time.LocalDateTime;

public class RepairQuery {
    private Integer page = 1;
    private Integer size = 10;
    private Long equipmentId;
    private String status;
    private LocalDateTime reportDateStart;
    private LocalDateTime reportDateEnd;
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

    public LocalDateTime getReportDateStart() {
        return reportDateStart;
    }

    public void setReportDateStart(LocalDateTime reportDateStart) {
        this.reportDateStart = reportDateStart;
    }

    public LocalDateTime getReportDateEnd() {
        return reportDateEnd;
    }

    public void setReportDateEnd(LocalDateTime reportDateEnd) {
        this.reportDateEnd = reportDateEnd;
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
