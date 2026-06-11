package com.example.lab.dto;

import com.example.lab.enums.BusinessOperation;
import com.example.lab.enums.EquipmentStatus;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StateConstraintCheckResult {

    private boolean allowed;
    private String operation;
    private String operationDescription;
    private String currentEquipmentStatus;
    private String currentEquipmentStatusText;
    private List<String> allowedEquipmentStatuses;
    private List<String> allowedEquipmentStatusTexts;
    private String errorCode;
    private String errorMessage;
    private List<String> details;
    private String generalTip;

    public static StateConstraintCheckResult success(BusinessOperation operation) {
        StateConstraintCheckResult result = new StateConstraintCheckResult();
        result.setAllowed(true);
        result.setOperation(operation.getCode());
        result.setOperationDescription(operation.getDescription());
        result.setGeneralTip(operation.getGeneralTip());
        result.setDetails(new ArrayList<>());
        if (operation.isEquipmentStatusRequired()) {
            result.setAllowedEquipmentStatuses(
                    operation.getAllowedEquipmentStatuses().stream()
                            .map(EquipmentStatus::getCode)
                            .toList()
            );
            result.setAllowedEquipmentStatusTexts(
                    operation.getAllowedEquipmentStatuses().stream()
                            .map(EquipmentStatus::getDescription)
                            .toList()
            );
        }
        return result;
    }

    public static StateConstraintCheckResult failure(BusinessOperation operation,
                                                      String errorCode,
                                                      String errorMessage,
                                                      EquipmentStatus currentStatus) {
        StateConstraintCheckResult result = success(operation);
        result.setAllowed(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        if (currentStatus != null) {
            result.setCurrentEquipmentStatus(currentStatus.getCode());
            result.setCurrentEquipmentStatusText(currentStatus.getDescription());
        }
        return result;
    }

    public void addDetail(String detail) {
        if (this.details == null) {
            this.details = new ArrayList<>();
        }
        this.details.add(detail);
    }
}
