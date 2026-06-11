package com.example.lab.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum EquipmentStatus {
    NORMAL("NORMAL", "正常"),
    BORROWED("BORROWED", "借用中"),
    REPAIRING("REPAIRING", "维修中"),
    SCRAPPED("SCRAPPED", "报废");

    private final String code;
    private final String description;

    EquipmentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static EquipmentStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    public static List<EquipmentStatus> getBorrowableStatuses() {
        return Arrays.asList(NORMAL);
    }

    public static List<EquipmentStatus> getRepairableStatuses() {
        return Arrays.asList(NORMAL, BORROWED);
    }

    public boolean isBorrowable() {
        return getBorrowableStatuses().contains(this);
    }

    public boolean isRepairable() {
        return getRepairableStatuses().contains(this);
    }
}
