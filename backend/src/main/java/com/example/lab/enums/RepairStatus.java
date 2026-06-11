package com.example.lab.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum RepairStatus {
    REPORTED("REPORTED", "已上报"),
    IN_PROGRESS("IN_PROGRESS", "维修中"),
    FINISHED("FINISHED", "已完成");

    private final String code;
    private final String description;

    RepairStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static RepairStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    public static List<RepairStatus> getActiveStatuses() {
        return Arrays.asList(REPORTED, IN_PROGRESS);
    }

    public boolean isActive() {
        return getActiveStatuses().contains(this);
    }

    public boolean canFinish() {
        return this == REPORTED || this == IN_PROGRESS;
    }

    public boolean canCancel() {
        return this == REPORTED;
    }
}
