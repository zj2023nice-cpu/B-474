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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateConstraintServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private RepairRepository repairRepository;

    @InjectMocks
    private StateConstraintService stateConstraintService;

    private Equipment normalEquipment;
    private Equipment borrowedEquipment;
    private Equipment repairingEquipment;
    private Equipment scrappedEquipment;
    private Repair activeRepair;
    private Repair finishedRepair;
    private Borrow pendingBorrow;
    private Borrow approvedBorrow;

    @BeforeEach
    void setUp() {
        normalEquipment = new Equipment();
        normalEquipment.setId(1L);
        normalEquipment.setName("正常设备");
        normalEquipment.setStatus(EquipmentStatus.NORMAL);

        borrowedEquipment = new Equipment();
        borrowedEquipment.setId(2L);
        borrowedEquipment.setName("借用中设备");
        borrowedEquipment.setStatus(EquipmentStatus.BORROWED);

        repairingEquipment = new Equipment();
        repairingEquipment.setId(3L);
        repairingEquipment.setName("维修中设备");
        repairingEquipment.setStatus(EquipmentStatus.REPAIRING);

        scrappedEquipment = new Equipment();
        scrappedEquipment.setId(4L);
        scrappedEquipment.setName("已报废设备");
        scrappedEquipment.setStatus(EquipmentStatus.SCRAPPED);

        activeRepair = new Repair();
        activeRepair.setId(1L);
        activeRepair.setStatus(RepairStatus.REPORTED);
        activeRepair.setEquipment(repairingEquipment);

        finishedRepair = new Repair();
        finishedRepair.setId(2L);
        finishedRepair.setStatus(RepairStatus.FINISHED);
        finishedRepair.setEquipment(normalEquipment);

        pendingBorrow = new Borrow();
        pendingBorrow.setId(1L);
        pendingBorrow.setStatus(BorrowStatus.PENDING);
        pendingBorrow.setEquipment(normalEquipment);

        approvedBorrow = new Borrow();
        approvedBorrow.setId(2L);
        approvedBorrow.setStatus(BorrowStatus.APPROVED);
        approvedBorrow.setEquipment(borrowedEquipment);
    }

    @Test
    void testCheckOperation_ApplyBorrow_NormalEquipment_ShouldAllow() {
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.APPLY_BORROW, normalEquipment);

        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals(EquipmentStatus.NORMAL, result.getCurrentEquipmentStatus());
        assertEquals("正常", result.getCurrentEquipmentStatusText());
        assertTrue(result.getAllowedEquipmentStatusTexts().contains("正常"));
    }

    @Test
    void testCheckOperation_ApplyBorrow_RepairingEquipment_ShouldNotAllow() {
        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.APPLY_BORROW, repairingEquipment);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("EQUIPMENT_STATUS_NOT_ALLOWED", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("维修中"));
        assertTrue(result.getErrorMessage().contains("申请借用"));
    }

    @Test
    void testCheckOperation_ApplyBorrow_ScrappedEquipment_ShouldNotAllow() {
        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.APPLY_BORROW, scrappedEquipment);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("EQUIPMENT_STATUS_NOT_ALLOWED", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("报废"));
        assertTrue(result.getErrorMessage().contains("申请借用"));
    }

    @Test
    void testCheckOperation_ApplyBorrow_BorrowedEquipment_ShouldNotAllow() {
        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.APPLY_BORROW, borrowedEquipment);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("EQUIPMENT_STATUS_NOT_ALLOWED", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("借用中"));
        assertTrue(result.getErrorMessage().contains("申请借用"));
    }

    @Test
    void testCheckOperation_ApplyBorrow_WithActiveRepair_ShouldNotAllow() {
        when(repairRepository.findByEquipment_IdAndStatusNot(eq(1L), any(RepairStatus.class)))
                .thenReturn(Arrays.asList(activeRepair));

        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.APPLY_BORROW, normalEquipment);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("HAS_ACTIVE_REPAIR", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("未完成的维修记录"));
        assertTrue(result.getErrorMessage().contains("不允许借用"));
    }

    @Test
    void testCheckOperation_ReportRepair_NormalEquipment_ShouldAllow() {
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.REPORT_REPAIR, normalEquipment);

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testCheckOperation_ReportRepair_BorrowedEquipment_ShouldAllow() {
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.REPORT_REPAIR, borrowedEquipment);

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testCheckOperation_ReportRepair_ScrappedEquipment_ShouldNotAllow() {
        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.REPORT_REPAIR, scrappedEquipment);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("EQUIPMENT_STATUS_NOT_ALLOWED", result.getErrorCode());
    }

    @Test
    void testCheckOperation_ReportRepair_WithActiveRepair_ShouldNotAllow() {
        when(repairRepository.findByEquipment_IdAndStatusNot(eq(1L), any(RepairStatus.class)))
                .thenReturn(Arrays.asList(activeRepair));

        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.REPORT_REPAIR, normalEquipment);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("HAS_ACTIVE_REPAIR", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("不允许重复报修"));
        assertTrue(result.getDetails().size() > 0);
    }

    @Test
    void testCheckOperation_ApproveBorrow_NormalEquipment_ShouldAllow() {
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.APPROVE_BORROW, normalEquipment);

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testCheckOperation_ApproveBorrow_RepairingEquipment_ShouldNotAllow() {
        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.APPROVE_BORROW, repairingEquipment);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertTrue(result.getErrorMessage().contains("维修中"));
        assertTrue(result.getErrorMessage().contains("批准借用"));
    }

    @Test
    void testCheckBorrowOperation_ApproveBorrow_PendingStatus_ShouldAllow() {
        when(borrowRepository.findById(anyLong())).thenReturn(Optional.of(pendingBorrow));
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        StateConstraintCheckResult result = stateConstraintService.checkBorrowOperation(
                1L, BusinessOperation.APPROVE_BORROW);

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testCheckBorrowOperation_ApproveBorrow_ApprovedStatus_ShouldNotAllow() {
        when(borrowRepository.findById(anyLong())).thenReturn(Optional.of(approvedBorrow));

        StateConstraintCheckResult result = stateConstraintService.checkBorrowOperation(
                2L, BusinessOperation.APPROVE_BORROW);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("BORROW_STATUS_NOT_ALLOWED", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("已批准"));
        assertTrue(result.getErrorMessage().contains("批准借用"));
    }

    @Test
    void testCheckBorrowOperation_ReturnEquipment_ApprovedStatus_ShouldAllow() {
        when(borrowRepository.findById(anyLong())).thenReturn(Optional.of(approvedBorrow));

        StateConstraintCheckResult result = stateConstraintService.checkBorrowOperation(
                2L, BusinessOperation.RETURN_EQUIPMENT);

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testCheckBorrowOperation_ReturnEquipment_PendingStatus_ShouldNotAllow() {
        when(borrowRepository.findById(anyLong())).thenReturn(Optional.of(pendingBorrow));

        StateConstraintCheckResult result = stateConstraintService.checkBorrowOperation(
                1L, BusinessOperation.RETURN_EQUIPMENT);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("BORROW_STATUS_NOT_ALLOWED", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("待审批"));
        assertTrue(result.getErrorMessage().contains("归还设备"));
    }

    @Test
    void testCheckBorrowOperation_CancelBorrow_PendingStatus_ShouldAllow() {
        when(borrowRepository.findById(anyLong())).thenReturn(Optional.of(pendingBorrow));

        StateConstraintCheckResult result = stateConstraintService.checkBorrowOperation(
                1L, BusinessOperation.CANCEL_BORROW);

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testCheckBorrowOperation_CancelBorrow_ApprovedStatus_ShouldNotAllow() {
        when(borrowRepository.findById(anyLong())).thenReturn(Optional.of(approvedBorrow));

        StateConstraintCheckResult result = stateConstraintService.checkBorrowOperation(
                2L, BusinessOperation.CANCEL_BORROW);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("BORROW_STATUS_NOT_ALLOWED", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("已批准"));
        assertTrue(result.getErrorMessage().contains("取消借用"));
    }

    @Test
    void testCheckBorrowOperation_NotFound_ShouldThrowException() {
        when(borrowRepository.findById(anyLong())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            stateConstraintService.checkBorrowOperation(999L, BusinessOperation.APPROVE_BORROW);
        });

        assertTrue(exception.getMessage().contains("借用记录不存在"));
    }

    @Test
    void testCheckRepairOperation_FinishRepair_ReportedStatus_ShouldAllow() {
        when(repairRepository.findById(anyLong())).thenReturn(Optional.of(activeRepair));

        StateConstraintCheckResult result = stateConstraintService.checkRepairOperation(
                1L, BusinessOperation.FINISH_REPAIR);

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testCheckRepairOperation_FinishRepair_FinishedStatus_ShouldNotAllow() {
        when(repairRepository.findById(anyLong())).thenReturn(Optional.of(finishedRepair));

        StateConstraintCheckResult result = stateConstraintService.checkRepairOperation(
                2L, BusinessOperation.FINISH_REPAIR);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("REPAIR_STATUS_NOT_ALLOWED", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("已完成"));
        assertTrue(result.getErrorMessage().contains("完成维修"));
    }

    @Test
    void testCheckRepairOperation_CancelRepair_ReportedStatus_ShouldAllow() {
        when(repairRepository.findById(anyLong())).thenReturn(Optional.of(activeRepair));

        StateConstraintCheckResult result = stateConstraintService.checkRepairOperation(
                1L, BusinessOperation.CANCEL_REPAIR);

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testCheckRepairOperation_CancelRepair_FinishedStatus_ShouldNotAllow() {
        when(repairRepository.findById(anyLong())).thenReturn(Optional.of(finishedRepair));

        StateConstraintCheckResult result = stateConstraintService.checkRepairOperation(
                2L, BusinessOperation.CANCEL_REPAIR);

        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("REPAIR_STATUS_NOT_ALLOWED", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("已完成"));
        assertTrue(result.getErrorMessage().contains("取消报修"));
    }

    @Test
    void testCheckRepairOperation_NotFound_ShouldThrowException() {
        when(repairRepository.findById(anyLong())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            stateConstraintService.checkRepairOperation(999L, BusinessOperation.FINISH_REPAIR);
        });

        assertTrue(exception.getMessage().contains("维修记录不存在"));
    }

    @Test
    void testAssertOperationAllowed_AllowedOperation_ShouldNotThrow() {
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> {
            stateConstraintService.assertOperationAllowed(BusinessOperation.APPLY_BORROW, normalEquipment);
        });
    }

    @Test
    void testAssertOperationAllowed_NotAllowedOperation_ShouldThrowException() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            stateConstraintService.assertOperationAllowed(BusinessOperation.APPLY_BORROW, repairingEquipment);
        });

        assertNotNull(exception.getMessage());
    }

    @Test
    void testCheckWithLock_ShouldUsePessimisticLock() {
        when(equipmentRepository.findByIdWithLock(anyLong())).thenReturn(Optional.of(normalEquipment));
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        StateConstraintCheckResult result = stateConstraintService.checkWithLock(
                BusinessOperation.APPLY_BORROW, 1L);

        assertNotNull(result);
        assertTrue(result.isAllowed());
        verify(equipmentRepository).findByIdWithLock(1L);
    }

    @Test
    void testCheckWithLock_EquipmentNotFound_ShouldThrowException() {
        when(equipmentRepository.findByIdWithLock(anyLong())).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            stateConstraintService.checkWithLock(BusinessOperation.APPLY_BORROW, 999L);
        });

        assertTrue(exception.getMessage().contains("设备不存在"));
    }

    @Test
    void testGetAvailableOperations_AllAllowed_ShouldReturnAllOperations() {
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        List<BusinessOperation> operations = stateConstraintService.getAvailableOperations(normalEquipment);

        assertNotNull(operations);
        assertTrue(operations.contains(BusinessOperation.APPLY_BORROW));
        assertTrue(operations.contains(BusinessOperation.APPROVE_BORROW));
        assertTrue(operations.contains(BusinessOperation.REPORT_REPAIR));
    }

    @Test
    void testGetAvailableOperations_ScrappedEquipment_ShouldReturnEmptyOrLimited() {
        List<BusinessOperation> operations = stateConstraintService.getAvailableOperations(scrappedEquipment);

        assertNotNull(operations);
        assertFalse(operations.contains(BusinessOperation.APPLY_BORROW));
        assertFalse(operations.contains(BusinessOperation.REPORT_REPAIR));
    }

    @Test
    void testCheckOperation_ResultContainsProperMetadata() {
        when(repairRepository.findByEquipment_IdAndStatusNot(anyLong(), any(RepairStatus.class)))
                .thenReturn(Collections.emptyList());

        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.APPLY_BORROW, normalEquipment);

        assertNotNull(result.getOperation());
        assertEquals(BusinessOperation.APPLY_BORROW.getCode(), result.getOperation());
        assertEquals("申请借用", result.getOperationText());
        assertNotNull(result.getAllowedEquipmentStatuses());
        assertNotNull(result.getAllowedEquipmentStatusTexts());
        assertTrue(result.getAllowedEquipmentStatusTexts().size() > 0);
    }

    @Test
    void testCheckOperation_ActiveRepair_DetailsShouldContainRepairInfo() {
        Repair repair = new Repair();
        repair.setId(100L);
        repair.setStatus(RepairStatus.IN_PROGRESS);
        repair.setEquipment(normalEquipment);

        when(repairRepository.findByEquipment_IdAndStatusNot(eq(1L), any(RepairStatus.class)))
                .thenReturn(Arrays.asList(repair));

        StateConstraintCheckResult result = stateConstraintService.checkOperation(
                BusinessOperation.REPORT_REPAIR, normalEquipment);

        assertNotNull(result.getDetails());
        assertTrue(result.getDetails().size() > 0);
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("#100")));
        assertTrue(result.getDetails().stream().anyMatch(d -> d.contains("维修中")));
    }
}
