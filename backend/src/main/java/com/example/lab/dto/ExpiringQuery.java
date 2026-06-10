package com.example.lab.dto;

public class ExpiringQuery {
    private Integer page = 1;
    private Integer size = 10;
    private Long labId;
    private String status;
    private Boolean expiredOnly = false;
    private Boolean includeIncomplete = true;
    private String sortBy = "remainingDays";
    private String sortOrder = "asc";

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

    public Long getLabId() {
        return labId;
    }

    public void setLabId(Long labId) {
        this.labId = labId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getExpiredOnly() {
        return expiredOnly;
    }

    public void setExpiredOnly(Boolean expiredOnly) {
        if (expiredOnly != null) {
            this.expiredOnly = expiredOnly;
        }
    }

    public Boolean getIncludeIncomplete() {
        return includeIncomplete;
    }

    public void setIncludeIncomplete(Boolean includeIncomplete) {
        if (includeIncomplete != null) {
            this.includeIncomplete = includeIncomplete;
        }
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
