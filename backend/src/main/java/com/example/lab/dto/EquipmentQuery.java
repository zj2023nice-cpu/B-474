package com.example.lab.dto;

public class EquipmentQuery {
    private Integer page = 1;
    private Integer size = 10;
    private String name;
    private String code;
    private String status;
    private Long labId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getLabId() {
        return labId;
    }

    public void setLabId(Long labId) {
        this.labId = labId;
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
