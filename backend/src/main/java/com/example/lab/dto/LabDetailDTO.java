package com.example.lab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class LabDetailDTO {
    private Long id;
    private String name;
    private String building;
    private String room;
    private String picName;
    private String picPhone;
    private Integer capacity;
    private long totalEquipment;
    private Map<String, Long> statusCounts;
    private long activeBorrowCount;
    private long activeRepairCount;
    private long expiringCount;
    private List<EquipmentSummary> expiringEquipments;
    private List<BorrowSummary> activeBorrows;
    private List<RepairSummary> activeRepairs;

    public LabDetailDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public String getPicName() { return picName; }
    public void setPicName(String picName) { this.picName = picName; }
    public String getPicPhone() { return picPhone; }
    public void setPicPhone(String picPhone) { this.picPhone = picPhone; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public long getTotalEquipment() { return totalEquipment; }
    public void setTotalEquipment(long totalEquipment) { this.totalEquipment = totalEquipment; }
    public Map<String, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Long> statusCounts) { this.statusCounts = statusCounts; }
    public long getActiveBorrowCount() { return activeBorrowCount; }
    public void setActiveBorrowCount(long activeBorrowCount) { this.activeBorrowCount = activeBorrowCount; }
    public long getActiveRepairCount() { return activeRepairCount; }
    public void setActiveRepairCount(long activeRepairCount) { this.activeRepairCount = activeRepairCount; }
    public long getExpiringCount() { return expiringCount; }
    public void setExpiringCount(long expiringCount) { this.expiringCount = expiringCount; }
    public List<EquipmentSummary> getExpiringEquipments() { return expiringEquipments; }
    public void setExpiringEquipments(List<EquipmentSummary> expiringEquipments) { this.expiringEquipments = expiringEquipments; }
    public List<BorrowSummary> getActiveBorrows() { return activeBorrows; }
    public void setActiveBorrows(List<BorrowSummary> activeBorrows) { this.activeBorrows = activeBorrows; }
    public List<RepairSummary> getActiveRepairs() { return activeRepairs; }
    public void setActiveRepairs(List<RepairSummary> activeRepairs) { this.activeRepairs = activeRepairs; }

    public static class EquipmentSummary {
        private Long id;
        private String code;
        private String name;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate expiryDate;
        private long remainingDays;

        public EquipmentSummary() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
        public long getRemainingDays() { return remainingDays; }
        public void setRemainingDays(long remainingDays) { this.remainingDays = remainingDays; }
    }

    public static class BorrowSummary {
        private Long id;
        private String equipmentName;
        private String equipmentCode;
        private String applicantName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;
        private String purpose;

        public BorrowSummary() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEquipmentName() { return equipmentName; }
        public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
        public String getEquipmentCode() { return equipmentCode; }
        public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
        public String getApplicantName() { return applicantName; }
        public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
    }

    public static class RepairSummary {
        private Long id;
        private String equipmentName;
        private String equipmentCode;
        private String description;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime reportDate;
        private String status;

        public RepairSummary() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEquipmentName() { return equipmentName; }
        public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
        public String getEquipmentCode() { return equipmentCode; }
        public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getReportDate() { return reportDate; }
        public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
