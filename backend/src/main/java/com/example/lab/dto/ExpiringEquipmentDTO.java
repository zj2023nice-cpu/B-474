package com.example.lab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ExpiringEquipmentDTO {
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
    private boolean dataComplete;
    private String dataIncompleteReason;

    public static class LabInfo {
        private Long id;
        private String name;

        public LabInfo() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public ExpiringEquipmentDTO() {}

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
    public boolean isDataComplete() { return dataComplete; }
    public void setDataComplete(boolean dataComplete) { this.dataComplete = dataComplete; }
    public String getDataIncompleteReason() { return dataIncompleteReason; }
    public void setDataIncompleteReason(String dataIncompleteReason) { this.dataIncompleteReason = dataIncompleteReason; }
}
