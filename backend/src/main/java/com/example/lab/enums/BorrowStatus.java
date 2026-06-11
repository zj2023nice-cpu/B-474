package com.example.lab.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BorrowStatus {
    PENDING("PENDING", "待审批"),
    APPROVED("APPROVED", "已批准"),
    REJECTED("REJECTED", "已拒绝"),
    RETURNED("RETURNED", "已归还"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String description;

    BorrowStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static BorrowStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    public boolean canApprove() {
        return this == PENDING;
    }

    public boolean canReturn() {
        return this == APPROVED;
    }

    public boolean canCancel() {
        return this == PENDING;
    }
}
