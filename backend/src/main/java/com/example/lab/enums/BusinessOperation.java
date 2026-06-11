package com.example.lab.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum BusinessOperation {
    APPLY_BORROW("APPLY_BORROW", "申请借用", "仅正常状态的设备可借用",
            Arrays.asList(EquipmentStatus.NORMAL),
            Arrays.asList("检查设备是否有未完成维修单")),
    APPROVE_BORROW("APPROVE_BORROW", "批准借用", "审批时需重新确认设备状态",
            Arrays.asList(EquipmentStatus.NORMAL),
            Arrays.asList("检查设备是否有未完成维修单", "检查借用单状态是否为待审批")),
    REPORT_REPAIR("REPORT_REPAIR", "申请报修", "正常或借用中的设备可报修",
            Arrays.asList(EquipmentStatus.NORMAL, EquipmentStatus.BORROWED),
            Arrays.asList("检查设备是否已有未完成维修单")),
    FINISH_REPAIR("FINISH_REPAIR", "完成维修", "维修中的设备可完成",
            Arrays.asList(EquipmentStatus.REPAIRING),
            Arrays.asList("检查维修单状态是否可完成")),
    RETURN_EQUIPMENT("RETURN_EQUIPMENT", "归还设备", "借用中的设备可归还",
            Arrays.asList(EquipmentStatus.BORROWED),
            Arrays.asList("检查借用单状态是否为已批准")),
    CANCEL_BORROW("CANCEL_BORROW", "取消借用", "待审批的借用可取消",
            null,
            Arrays.asList("检查借用单状态是否为待审批")),
    CANCEL_REPAIR("CANCEL_REPAIR", "取消报修", "已上报的维修可取消",
            null,
            Arrays.asList("检查维修单状态是否为已上报"));

    private final String code;
    private final String description;
    private final String generalTip;
    private final List<EquipmentStatus> allowedEquipmentStatuses;
    private final List<String> additionalChecks;

    BusinessOperation(String code, String description, String generalTip,
                      List<EquipmentStatus> allowedEquipmentStatuses,
                      List<String> additionalChecks) {
        this.code = code;
        this.description = description;
        this.generalTip = generalTip;
        this.allowedEquipmentStatuses = allowedEquipmentStatuses;
        this.additionalChecks = additionalChecks;
    }

    public static BusinessOperation fromCode(String code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(op -> op.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }

    public boolean isEquipmentStatusRequired() {
        return allowedEquipmentStatuses != null && !allowedEquipmentStatuses.isEmpty();
    }

    public boolean isEquipmentStatusAllowed(EquipmentStatus status) {
        if (!isEquipmentStatusRequired()) {
            return true;
        }
        return allowedEquipmentStatuses.contains(status);
    }
}
