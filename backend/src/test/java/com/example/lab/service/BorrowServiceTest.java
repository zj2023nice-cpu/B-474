package com.example.lab.service;

import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
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

    @InjectMocks
    private BorrowService borrowService;

    private Equipment testEquipment;
    private Borrow testBorrow;

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

        assertThrows(RuntimeException.class, () -> {
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

        Equipment borrowedEquipment = new Equipment();
        borrowedEquipment.setId(testEquipment.getId());
        borrowedEquipment.setStatus("BORROWED");

        when(equipmentRepository.save(testEquipment)).thenReturn(borrowedEquipment);

        Borrow approvedBorrow = new Borrow();
        approvedBorrow.setId(borrowId);
        approvedBorrow.setStatus("APPROVED");
        approvedBorrow.setEquipment(borrowedEquipment);

        when(borrowRepository.save(pendingBorrow)).thenReturn(approvedBorrow);

        Borrow result = borrowService.approve(borrowId, approverId);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertEquals("BORROWED", result.getEquipment().getStatus());

        verify(borrowRepository).findById(borrowId);
        verify(equipmentRepository).save(testEquipment);
        verify(borrowRepository).save(pendingBorrow);
    }

    @Test
    void testReject_ShouldSetStatusRejected() {
        Long borrowId = 1L;
        Long approverId = 2L;
        String rejectReason = "设备维护中";

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");

        when(borrowRepository.findById(borrowId)).thenReturn(Optional.of(pendingBorrow));

        Borrow rejectedBorrow = new Borrow();
        rejectedBorrow.setId(borrowId);
        rejectedBorrow.setStatus("REJECTED");

        when(borrowRepository.save(pendingBorrow)).thenReturn(rejectedBorrow);

        Borrow result = borrowService.reject(borrowId, approverId, rejectReason);

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        verify(borrowRepository).findById(borrowId);
        verify(borrowRepository).save(pendingBorrow);
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

        Equipment returnedEquipment = new Equipment();
        returnedEquipment.setId(1L);
        returnedEquipment.setStatus("NORMAL");

        when(equipmentRepository.save(borrowedEquipment)).thenReturn(returnedEquipment);

        Borrow returnedBorrow = new Borrow();
        returnedBorrow.setId(borrowId);
        returnedBorrow.setStatus("RETURNED");
        returnedBorrow.setEquipment(returnedEquipment);

        when(borrowRepository.save(approvedBorrow)).thenReturn(returnedBorrow);

        Borrow result = borrowService.returnEquipment(borrowId);

        assertNotNull(result);
        assertEquals("RETURNED", result.getStatus());
        assertEquals("NORMAL", result.getEquipment().getStatus());

        verify(borrowRepository).findById(borrowId);
        verify(equipmentRepository).save(borrowedEquipment);
        verify(borrowRepository).save(approvedBorrow);
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
}
