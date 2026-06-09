package com.example.lab.dto;

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
}
