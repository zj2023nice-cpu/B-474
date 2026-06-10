package com.example.lab.service;

import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.User;
import com.example.lab.exception.BusinessException;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BorrowService borrowService;

    private Equipment testEquipment;
    private Borrow testBorrow;
    private User testApprover;

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);
        testEquipment.setName("显微镜");
        testEquipment.setStatus("NORMAL");

        testBorrow = new Borrow();
        testBorrow.setId(1L);
        testBorrow.setEquipment(testEquipment);
        testBorrow.setStartTime(LocalDateTime.now().plusDays(1));
        testBorrow.setEndTime(LocalDateTime.now().plusDays(2));
        testBorrow.setStatus("PENDING");

        testApprover = new User();
        testApprover.setId(2L);
        testApprover.setName("管理员");
        testApprover.setRole("ADMIN");
    }

    @Test
    void testApply_WithNoConflicts_ShouldCreatePendingBorrow() {
        Borrow newBorrow = new Borrow();
        newBorrow.setEquipment(testEquipment);
        newBorrow.setStartTime(LocalDateTime.now().plusDays(3));
        newBorrow.setEndTime(LocalDateTime.now().plusDays(4));

        when(borrowRepository.findConflicts(
                eq(testEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Collections.emptyList());

        Borrow savedBorrow = new Borrow();
        savedBorrow.setId(1L);
        savedBorrow.setStatus("PENDING");
        savedBorrow.setEquipment(testEquipment);

        when(borrowRepository.save(any(Borrow.class))).thenReturn(savedBorrow);

        Borrow result = borrowService.apply(newBorrow);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(borrowRepository).findConflicts(
                eq(testEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(borrowRepository).save(any(Borrow.class));
    }

    @Test
    void testApply_WithConflicts_ShouldThrowException() {
        Borrow newBorrow = new Borrow();
        newBorrow.setEquipment(testEquipment);
        newBorrow.setStartTime(LocalDateTime.now().plusDays(1));
        newBorrow.setEndTime(LocalDateTime.now().plusDays(2));

        Borrow conflictingBorrow = new Borrow();
        conflictingBorrow.setId(2L);

        when(borrowRepository.findConflicts(
                eq(testEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(Arrays.asList(conflictingBorrow));

        assertThrows(BusinessException.class, () -> {
            borrowService.apply(newBorrow);
        });

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testApprove_ShouldSetStatusApprovedAndUpdateEquipment() {
        Long borrowId = 1L;
        Long approverId = 2L;

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(pendingBorrow));
        when(userRepository.findById(approverId)).thenReturn(Optional.of(testApprover));
        when(borrowRepository.save(pendingBorrow)).thenReturn(pendingBorrow);

        Borrow result = borrowService.approve(borrowId, approverId);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertEquals(testApprover, result.getApprover());
        assertNotNull(result.getApproveTime());
        assertNull(result.getRejectReason());
        assertNull(result.getRejectTime());

        verify(borrowRepository).findById(borrowId);
        verify(userRepository).findById(approverId);
        verify(equipmentRepository).save(testEquipment);
        verify(borrowRepository).save(pendingBorrow);
    }

    @Test
    void testApprove_NonPendingStatus_ShouldThrowBusinessException() {
        Long borrowId = 1L;
        Long approverId = 2L;

        Borrow approvedBorrow = new Borrow();
        approvedBorrow.setId(borrowId);
        approvedBorrow.setStatus("APPROVED");
        approvedBorrow.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(approvedBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.approve(borrowId, approverId);
        });
        assertTrue(exception.getMessage().contains("当前状态不允许审批"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testApprove_AlreadyApproved_ShouldThrowBusinessException() {
        Long borrowId = 1L;
        Long approverId = 2L;

        Borrow alreadyApproved = new Borrow();
        alreadyApproved.setId(borrowId);
        alreadyApproved.setStatus("PENDING");
        alreadyApproved.setApprover(testApprover);
        alreadyApproved.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(alreadyApproved));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.approve(borrowId, approverId);
        });
        assertTrue(exception.getMessage().contains("已被审批"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_ShouldSetStatusRejectedAndRejectReason() {
        Long borrowId = 1L;
        Long approverId = 2L;
        String rejectReason = "设备维护中";

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(pendingBorrow));
        when(userRepository.findById(approverId)).thenReturn(Optional.of(testApprover));
        when(borrowRepository.save(pendingBorrow)).thenReturn(pendingBorrow);

        Borrow result = borrowService.reject(borrowId, approverId, rejectReason);

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        assertEquals(testApprover, result.getApprover());
        assertEquals("设备维护中", result.getRejectReason());
        assertNotNull(result.getRejectTime());
        assertNull(result.getApproveTime());

        verify(borrowRepository).findById(borrowId);
        verify(userRepository).findById(approverId);
        verify(borrowRepository).save(pendingBorrow);
    }

    @Test
    void testReject_NonPendingStatus_ShouldThrowBusinessException() {
        Long borrowId = 1L;
        Long approverId = 2L;

        Borrow rejectedBorrow = new Borrow();
        rejectedBorrow.setId(borrowId);
        rejectedBorrow.setStatus("REJECTED");
        rejectedBorrow.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(rejectedBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, approverId, "原因");
        });
        assertTrue(exception.getMessage().contains("当前状态不允许审批"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_BlankRejectReason_ShouldThrowBusinessException() {
        Long borrowId = 1L;
        Long approverId = 2L;

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(pendingBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, approverId, "");
        });
        assertTrue(exception.getMessage().contains("拒绝原因不能为空"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_NullRejectReason_ShouldThrowBusinessException() {
        Long borrowId = 1L;
        Long approverId = 2L;

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(pendingBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, approverId, null);
        });
        assertTrue(exception.getMessage().contains("拒绝原因不能为空"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_AlreadyApproved_ShouldThrowBusinessException() {
        Long borrowId = 1L;
        Long approverId = 2L;

        Borrow alreadyApproved = new Borrow();
        alreadyApproved.setId(borrowId);
        alreadyApproved.setStatus("PENDING");
        alreadyApproved.setApprover(testApprover);
        alreadyApproved.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(alreadyApproved));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, approverId, "原因");
        });
        assertTrue(exception.getMessage().contains("已被审批"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReturnEquipment_ShouldSetStatusReturnedAndResetEquipment() {
        Long borrowId = 1L;

        Equipment borrowedEquipment = new Equipment();
        borrowedEquipment.setId(1L);
        borrowedEquipment.setStatus("BORROWED");

        Borrow approvedBorrow = new Borrow();
        approvedBorrow.setId(borrowId);
        approvedBorrow.setStatus("APPROVED");
        approvedBorrow.setEquipment(borrowedEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(approvedBorrow));
        when(borrowRepository.save(approvedBorrow)).thenReturn(approvedBorrow);

        Borrow result = borrowService.returnEquipment(borrowId);

        assertNotNull(result);
        assertEquals("RETURNED", result.getStatus());
        assertEquals("NORMAL", result.getEquipment().getStatus());

        verify(borrowRepository).findById(borrowId);
        verify(equipmentRepository).save(borrowedEquipment);
        verify(borrowRepository).save(approvedBorrow);
    }

    @Test
    void testReturnEquipment_NonApprovedStatus_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(pendingBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.returnEquipment(borrowId);
        });
        assertTrue(exception.getMessage().contains("当前状态不允许归还"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testFindAll_ShouldReturnAllBorrows() {
        Borrow b1 = new Borrow();
        b1.setId(1L);

        Borrow b2 = new Borrow();
        b2.setId(2L);

        List<Borrow> borrows = Arrays.asList(b1, b2);

        when(borrowRepository.findAll()).thenReturn(borrows);

        List<Borrow> result = borrowService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(borrowRepository).findAll();
    }

    @Test
    void testFindByApplicant_ShouldReturnApplicantBorrows() {
        Long applicantId = 1L;

        Borrow b1 = new Borrow();
        b1.setId(1L);

        List<Borrow> borrows = Arrays.asList(b1);

        when(borrowRepository.findByApplicant_Id(applicantId)).thenReturn(borrows);

        List<Borrow> result = borrowService.findByApplicant(applicantId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(borrowRepository).findByApplicant_Id(applicantId);
    }

    @Test
    void testDelete_ShouldCallRepositoryDelete() {
        Long borrowId = 1L;

        doNothing().when(borrowRepository).deleteById(borrowId);

        borrowService.delete(borrowId);

        verify(borrowRepository).deleteById(borrowId);
    }

    @Test
    void testApprove_BorrowNotFound_ShouldThrowBusinessException() {
        Long borrowId = 999L;
        Long approverId = 2L;

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.approve(borrowId, approverId);
        });
        assertTrue(exception.getMessage().contains("借用记录不存在"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_BorrowNotFound_ShouldThrowBusinessException() {
        Long borrowId = 999L;
        Long approverId = 2L;

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, approverId, "原因");
        });
        assertTrue(exception.getMessage().contains("借用记录不存在"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_ReasonTooLong_ShouldThrowBusinessException() {
        Long borrowId = 1L;
        Long approverId = 2L;
        String longReason = "a".repeat(501);

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(pendingBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, approverId, longReason);
        });
        assertTrue(exception.getMessage().contains("500"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }
}
