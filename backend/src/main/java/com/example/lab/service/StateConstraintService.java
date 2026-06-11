package com.example.lab.service;

import com.example.lab.dto.StateConstraintCheckResult;
import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Repair;
import com.example.lab.enums.BorrowStatus;
import com.example.lab.enums.BusinessOperation;
import com.example.lab.enums.EquipmentStatus;
import com.example.lab.enums.RepairStatus;
import com.example.lab.exception.BusinessException;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StateConstraintService {

    public static final String ERROR_CODE_EQUIPMENT_STATUS = "EQUIPMENT_STATUS_NOT_ALLOWED";
    public static final String ERROR_CODE_HAS_ACTIVE_REPAIR = "HAS_ACTIVE_REPAIR";
    public static final String ERROR_CODE_HAS_ACTIVE_BORROW = "HAS_ACTIVE_BORROW";
    public static final String ERROR_CODE_BORROW_STATUS = "BORROW_STATUS_NOT_ALLOWED";
    public static final String ERROR_CODE_REPAIR_STATUS = "REPAIR_STATUS_NOT_ALLOWED";

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private RepairRepository repairRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    public StateConstraintCheckResult checkOperation(BusinessOperation operation, Long equipmentId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new BusinessException(404, "设备不存在"));
        return checkOperation(operation, equipment);
    }

    @Transactional(readOnly = true)
    public StateConstraintCheckResult checkOperation(BusinessOperation operation, Equipment equipment) {
        EquipmentStatus equipmentStatus = equipment.getStatus();

        StateConstraintCheckResult result = StateConstraintCheckResult.success(operation);
        result.setCurrentEquipmentStatus(equipmentStatus != null ? equipmentStatus.getCode() : null);
        if (equipmentStatus != null) {
            result.setCurrentEquipmentStatusText(equipmentStatus.getDescription());
        }

        if (operation.isEquipmentStatusRequired()) {
            if (equipmentStatus == null || !operation.isEquipmentStatusAllowed(equipmentStatus)) {
                String errorMsg = String.format("设备当前状态为「%s」，不允许%s。当前仅%s状态的设备可%s。",
                        result.getCurrentEquipmentStatusText(),
                        operation.getDescription(),
                        String.join("、", result.getAllowedEquipmentStatusTexts()),
                        operation.getDescription());
                result = StateConstraintCheckResult.failure(
                        operation,
                        ERROR_CODE_EQUIPMENT_STATUS,
                        errorMsg,
                        equipmentStatus
                );
            }
        }

        switch (operation) {
            case APPLY_BORROW, APPROVE_BORROW -> {
                if (result.isAllowed()) {
                    checkActiveRepair(equipment.getId(), result, operation);
                }
            }
            case REPORT_REPAIR -> {
                if (result.isAllowed()) {
                    checkActiveRepair(equipment.getId(), result, operation);
                }
            }
            default -> {
            }
        }

        return result;
    }

    public StateConstraintCheckResult checkBorrowOperation(BusinessOperation operation, Long borrowId) {
        Borrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new BusinessException(404, "借用记录不存在"));
        return checkBorrowOperation(operation, borrow);
    }

    @Transactional(readOnly = true)
    public StateConstraintCheckResult checkBorrowOperation(BusinessOperation operation, Borrow borrow) {
        Equipment equipment = borrow.getEquipment();
        StateConstraintCheckResult result = checkOperation(operation, equipment);

        if (!result.isAllowed()) {
            return result;
        }

        BorrowStatus borrowStatus = borrow.getStatus();

        switch (operation) {
            case APPROVE_BORROW -> {
                if (borrowStatus == null || !borrowStatus.canApprove()) {
                    String currentStatusText = borrowStatus != null ? borrowStatus.getDescription() : (borrow.getStatus() != null ? borrow.getStatus().getCode() : null);
                    String errorMsg = String.format("借用单当前状态为「%s」，不允许审批。仅「待审批」状态的借用单可审批。",
                            currentStatusText);
                    result = StateConstraintCheckResult.failure(
                            operation,
                            ERROR_CODE_BORROW_STATUS,
                            errorMsg,
                            equipment.getStatus()
                    );
                    result.addDetail("借用单状态校验失败");
                }
            }
            case RETURN_EQUIPMENT -> {
                if (borrowStatus == null || !borrowStatus.canReturn()) {
                    String currentStatusText = borrowStatus != null ? borrowStatus.getDescription() : (borrow.getStatus() != null ? borrow.getStatus().getCode() : null);
                    String errorMsg = String.format("借用单当前状态为「%s」，不允许归还。仅「已批准」状态的借用单可归还。",
                            currentStatusText);
                    result = StateConstraintCheckResult.failure(
                            operation,
                            ERROR_CODE_BORROW_STATUS,
                            errorMsg,
                            equipment.getStatus()
                    );
                    result.addDetail("借用单状态校验失败");
                }
            }
            case CANCEL_BORROW -> {
                if (borrowStatus == null || !borrowStatus.canCancel()) {
                    String currentStatusText = borrowStatus != null ? borrowStatus.getDescription() : (borrow.getStatus() != null ? borrow.getStatus().getCode() : null);
                    String errorMsg = String.format("借用单当前状态为「%s」，不允许取消。仅「待审批」状态的借用单可取消。",
                            currentStatusText);
                    result = StateConstraintCheckResult.failure(
                            operation,
                            ERROR_CODE_BORROW_STATUS,
                            errorMsg,
                            null
                    );
                    result.addDetail("借用单状态校验失败");
                }
            }
            default -> {
            }
        }

        return result;
    }

    public StateConstraintCheckResult checkRepairOperation(BusinessOperation operation, Long repairId) {
        Repair repair = repairRepository.findById(repairId)
                .orElseThrow(() -> new BusinessException(404, "维修记录不存在"));
        return checkRepairOperation(operation, repair);
    }

    @Transactional(readOnly = true)
    public StateConstraintCheckResult checkRepairOperation(BusinessOperation operation, Repair repair) {
        Equipment equipment = repair.getEquipment();
        StateConstraintCheckResult result = checkOperation(operation, equipment);

        if (!result.isAllowed()) {
            return result;
        }

        RepairStatus repairStatus = repair.getStatus();

        switch (operation) {
            case FINISH_REPAIR -> {
                if (repairStatus == null || !repairStatus.canFinish()) {
                    String currentStatusText = repairStatus != null ? repairStatus.getDescription() : (repair.getStatus() != null ? repair.getStatus().getCode() : null);
                    String errorMsg = String.format("维修单当前状态为「%s」，不允许完成。仅「已上报」或「维修中」状态的维修单可完成。",
                            currentStatusText);
                    result = StateConstraintCheckResult.failure(
                            operation,
                            ERROR_CODE_REPAIR_STATUS,
                            errorMsg,
                            equipment.getStatus()
                    );
                    result.addDetail("维修单状态校验失败");
                }
            }
            case CANCEL_REPAIR -> {
                if (repairStatus == null || !repairStatus.canCancel()) {
                    String currentStatusText = repairStatus != null ? repairStatus.getDescription() : (repair.getStatus() != null ? repair.getStatus().getCode() : null);
                    String errorMsg = String.format("维修单当前状态为「%s」，不允许取消。仅「已上报」状态的维修单可取消。",
                            currentStatusText);
                    result = StateConstraintCheckResult.failure(
                            operation,
                            ERROR_CODE_REPAIR_STATUS,
                            errorMsg,
                            null
                    );
                    result.addDetail("维修单状态校验失败");
                }
            }
            default -> {
            }
        }

        return result;
    }

    public void assertOperationAllowed(BusinessOperation operation, Long equipmentId) {
        StateConstraintCheckResult result = checkOperation(operation, equipmentId);
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorMessage());
        }
    }

    public void assertOperationAllowed(BusinessOperation operation, Equipment equipment) {
        StateConstraintCheckResult result = checkOperation(operation, equipment);
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorMessage());
        }
    }

    public void assertBorrowOperationAllowed(BusinessOperation operation, Long borrowId) {
        StateConstraintCheckResult result = checkBorrowOperation(operation, borrowId);
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorMessage());
        }
    }

    public void assertBorrowOperationAllowed(BusinessOperation operation, Borrow borrow) {
        StateConstraintCheckResult result = checkBorrowOperation(operation, borrow);
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorMessage());
        }
    }

    public void assertRepairOperationAllowed(BusinessOperation operation, Long repairId) {
        StateConstraintCheckResult result = checkRepairOperation(operation, repairId);
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorMessage());
        }
    }

    public void assertRepairOperationAllowed(BusinessOperation operation, Repair repair) {
        StateConstraintCheckResult result = checkRepairOperation(operation, repair);
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorMessage());
        }
    }

    private void checkActiveRepair(Long equipmentId, StateConstraintCheckResult result, BusinessOperation operation) {
        List<Repair> activeRepairs = repairRepository.findByEquipment_IdAndStatusNot(equipmentId, RepairStatus.FINISHED);
        if (!activeRepairs.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("该设备存在 %d 条未完成的维修记录，", activeRepairs.size()));
            if (operation == BusinessOperation.APPLY_BORROW || operation == BusinessOperation.APPROVE_BORROW) {
                sb.append("不允许借用。");
            } else if (operation == BusinessOperation.REPORT_REPAIR) {
                sb.append("不允许重复报修。");
            }
            sb.append("请先完成或取消现有维修单。");
            result.setAllowed(false);
            result.setErrorCode(ERROR_CODE_HAS_ACTIVE_REPAIR);
            result.setErrorMessage(sb.toString());
            result.addDetail(String.format("未完成维修单数：%d", activeRepairs.size()));
            for (Repair repair : activeRepairs) {
                RepairStatus status = repair.getStatus();
                String statusText = status != null ? status.getDescription() : (repair.getStatus() != null ? repair.getStatus().getCode() : null);
                result.addDetail(String.format("- 维修单 #%d，状态：%s", repair.getId(), statusText));
            }
        }
    }

    public StateConstraintCheckResult checkWithLock(BusinessOperation operation, Long equipmentId) {
        Equipment equipment = equipmentRepository.findByIdWithLock(equipmentId)
                .orElseThrow(() -> new BusinessException(404, "设备不存在"));
        return checkOperation(operation, equipment);
    }

    public void assertWithLock(BusinessOperation operation, Long equipmentId) {
        StateConstraintCheckResult result = checkWithLock(operation, equipmentId);
        if (!result.isAllowed()) {
            throw new BusinessException(result.getErrorMessage());
        }
    }
}
