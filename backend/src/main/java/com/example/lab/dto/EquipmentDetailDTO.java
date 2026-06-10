package com.example.lab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EquipmentDetailDTO {
    private Long id;
    private String code;
    private String name;
    private String model;
    private String manufacturer;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    private BigDecimal price;
    private Integer lifeSpan;
    private String status;

    private LabInfo lab;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private Long remainingDays;

    private LatestBorrow latestBorrow;
    private LatestRepair latestRepair;

    public static class LabInfo {
        private Long id;
        private String name;
        private String building;
        private String room;
        private String picName;
        private String picPhone;
        private Integer capacity;

        public LabInfo() {}

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
    }

    public static class LatestBorrow {
        private Long id;
        private String applicantName;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime applyDate;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime endTime;

        private String purpose;
        private String status;

        public LatestBorrow() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getApplicantName() { return applicantName; }
        public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
        public LocalDateTime getApplyDate() { return applyDate; }
        public void setApplyDate(LocalDateTime applyDate) { this.applyDate = applyDate; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class LatestRepair {
        private Long id;
        private String description;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime reportDate;

        private String repairCompany;
        private BigDecimal cost;
        private String repairConclusion;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime finishDate;

        private String status;
        private String reporterName;

        public LatestRepair() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getReportDate() { return reportDate; }
        public void setReportDate(LocalDateTime reportDate) { this.reportDate = reportDate; }
        public String getRepairCompany() { return repairCompany; }
        public void setRepairCompany(String repairCompany) { this.repairCompany = repairCompany; }
        public BigDecimal getCost() { return cost; }
        public void setCost(BigDecimal cost) { this.cost = cost; }
        public String getRepairConclusion() { return repairConclusion; }
        public void setRepairConclusion(String repairConclusion) { this.repairConclusion = repairConclusion; }
        public LocalDateTime getFinishDate() { return finishDate; }
        public void setFinishDate(LocalDateTime finishDate) { this.finishDate = finishDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReporterName() { return reporterName; }
        public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    }

    public EquipmentDetailDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getLifeSpan() { return lifeSpan; }
    public void setLifeSpan(Integer lifeSpan) { this.lifeSpan = lifeSpan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LabInfo getLab() { return lab; }
    public void setLab(LabInfo lab) { this.lab = lab; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Long getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Long remainingDays) { this.remainingDays = remainingDays; }
    public LatestBorrow getLatestBorrow() { return latestBorrow; }
    public void setLatestBorrow(LatestBorrow latestBorrow) { this.latestBorrow = latestBorrow; }
    public LatestRepair getLatestRepair() { return latestRepair; }
    public void setLatestRepair(LatestRepair latestRepair) { this.latestRepair = latestRepair; }
}
