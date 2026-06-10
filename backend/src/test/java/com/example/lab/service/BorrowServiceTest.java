package com.example.lab.service;

import com.example.lab.constant.RoleConstant;
import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.User;
import com.example.lab.exception.BusinessException;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

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

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

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

        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority(RoleConstant.ROLE_ADMIN)
        );
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(2L);
        lenient().when(authentication.getAuthorities()).thenAnswer(inv -> authorities);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
                any(LocalDateTime.class),
                isNull()
        )).thenReturn(Collections.emptyList());

        when(equipmentRepository.findByIdWithLock(eq(testEquipment.getId())))
                .thenReturn(Optional.of(testEquipment));

        Borrow savedBorrow = new Borrow();
        savedBorrow.setId(1L);
        savedBorrow.setStatus("PENDING");
        savedBorrow.setEquipment(testEquipment);

        when(borrowRepository.save(any(Borrow.class))).thenReturn(savedBorrow);

        Borrow result = borrowService.apply(newBorrow);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(borrowRepository, times(2)).findConflicts(
                eq(testEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull()
        );
        verify(equipmentRepository).findByIdWithLock(eq(testEquipment.getId()));
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
        User applicant = new User();
        applicant.setName("张三");
        conflictingBorrow.setApplicant(applicant);
        conflictingBorrow.setStatus("APPROVED");
        conflictingBorrow.setStartTime(LocalDateTime.now().plusDays(1));
        conflictingBorrow.setEndTime(LocalDateTime.now().plusDays(2));

        when(borrowRepository.findConflicts(
                eq(testEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull()
        )).thenReturn(Arrays.asList(conflictingBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.apply(newBorrow);
        });
        assertTrue(exception.getMessage().contains("冲突记录"));
        assertFalse(exception.getMessage().contains("其他用户同时提交申请"));

        verify(equipmentRepository, never()).findByIdWithLock(any());
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testApply_WithConcurrentConflicts_ShouldThrowException() {
        Borrow newBorrow = new Borrow();
        newBorrow.setEquipment(testEquipment);
        newBorrow.setStartTime(LocalDateTime.now().plusDays(3));
        newBorrow.setEndTime(LocalDateTime.now().plusDays(4));

        Borrow conflictingBorrow = new Borrow();
        conflictingBorrow.setId(2L);
        User applicant = new User();
        applicant.setName("李四");
        conflictingBorrow.setApplicant(applicant);
        conflictingBorrow.setStatus("PENDING");
        conflictingBorrow.setStartTime(LocalDateTime.now().plusDays(3));
        conflictingBorrow.setEndTime(LocalDateTime.now().plusDays(4));

        when(borrowRepository.findConflicts(
                eq(testEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull()
        )).thenReturn(Collections.emptyList())
          .thenReturn(Arrays.asList(conflictingBorrow));

        when(equipmentRepository.findByIdWithLock(eq(testEquipment.getId())))
                .thenReturn(Optional.of(testEquipment));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.apply(newBorrow);
        });
        assertTrue(exception.getMessage().contains("其他用户同时提交申请"));
        assertTrue(exception.getMessage().contains("请调整时间后重新提交"));

        verify(borrowRepository, times(2)).findConflicts(
                eq(testEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull()
        );
        verify(equipmentRepository).findByIdWithLock(eq(testEquipment.getId()));
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testApply_WithNonNormalEquipment_ShouldThrowException() {
        Borrow newBorrow = new Borrow();
        Equipment borrowedEquipment = new Equipment();
        borrowedEquipment.setId(2L);
        borrowedEquipment.setName("已借出设备");
        borrowedEquipment.setStatus("BORROWED");
        newBorrow.setEquipment(borrowedEquipment);
        newBorrow.setStartTime(LocalDateTime.now().plusDays(3));
        newBorrow.setEndTime(LocalDateTime.now().plusDays(4));

        when(borrowRepository.findConflicts(
                eq(borrowedEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull()
        )).thenReturn(Collections.emptyList());

        when(equipmentRepository.findByIdWithLock(eq(borrowedEquipment.getId())))
                .thenReturn(Optional.of(borrowedEquipment));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.apply(newBorrow);
        });
        assertTrue(exception.getMessage().contains("设备当前状态不可借用"));

        verify(equipmentRepository).findByIdWithLock(eq(borrowedEquipment.getId()));
        verify(borrowRepository, times(1)).findConflicts(
                eq(borrowedEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull()
        );
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testApply_WithNonExistentEquipment_ShouldThrowException() {
        Borrow newBorrow = new Borrow();
        Equipment nonExistEquipment = new Equipment();
        nonExistEquipment.setId(999L);
        newBorrow.setEquipment(nonExistEquipment);
        newBorrow.setStartTime(LocalDateTime.now().plusDays(3));
        newBorrow.setEndTime(LocalDateTime.now().plusDays(4));

        when(borrowRepository.findConflicts(
                eq(nonExistEquipment.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                isNull()
        )).thenReturn(Collections.emptyList());

        when(equipmentRepository.findByIdWithLock(eq(nonExistEquipment.getId())))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.apply(newBorrow);
        });
        assertTrue(exception.getMessage().contains("设备不存在"));

        verify(equipmentRepository).findByIdWithLock(eq(nonExistEquipment.getId()));
        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testCheckConflicts_WithNoConflicts_ShouldReturnNoConflict() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(3);
        LocalDateTime endTime = LocalDateTime.now().plusDays(4);

        when(borrowRepository.findConflicts(
                eq(testEquipment.getId()),
                eq(startTime),
                eq(endTime),
                isNull()
        )).thenReturn(Collections.emptyList());

        var result = borrowService.checkConflicts(testEquipment.getId(), startTime, endTime, null);

        assertNotNull(result);
        assertFalse(result.isHasConflict());
        assertTrue(result.getConflicts().isEmpty());
    }

    @Test
    void testCheckConflicts_WithConflicts_ShouldReturnConflicts() {
        LocalDateTime startTime = LocalDateTime.now().plusDays(1);
        LocalDateTime endTime = LocalDateTime.now().plusDays(2);

        Borrow conflictingBorrow = new Borrow();
        conflictingBorrow.setId(2L);
        User applicant = new User();
        applicant.setName("王五");
        conflictingBorrow.setApplicant(applicant);
        conflictingBorrow.setStatus("APPROVED");
        conflictingBorrow.setStartTime(startTime);
        conflictingBorrow.setEndTime(endTime);

        when(borrowRepository.findConflicts(
                eq(testEquipment.getId()),
                eq(startTime),
                eq(endTime),
                isNull()
        )).thenReturn(Arrays.asList(conflictingBorrow));

        var result = borrowService.checkConflicts(testEquipment.getId(), startTime, endTime, null);

        assertNotNull(result);
        assertTrue(result.isHasConflict());
        assertEquals(1, result.getConflicts().size());
        assertEquals("王五", result.getConflicts().get(0).getApplicantName());
        assertEquals("APPROVED", result.getConflicts().get(0).getStatus());
    }

    @Test
    void testApprove_ShouldSetStatusApprovedAndUpdateEquipment() {
        Long borrowId = 1L;

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(pendingBorrow));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(borrowRepository.save(pendingBorrow)).thenReturn(pendingBorrow);

        Borrow result = borrowService.approve(borrowId);

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertEquals(testApprover, result.getApprover());
        assertNotNull(result.getApproveTime());
        assertNull(result.getRejectReason());
        assertNull(result.getRejectTime());

        verify(borrowRepository).findByIdWithLock(borrowId);
        verify(userRepository).findById(2L);
        verify(equipmentRepository).save(testEquipment);
        verify(borrowRepository).save(pendingBorrow);
    }

    @Test
    void testApprove_AlreadyApprovedStatus_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow approvedBorrow = new Borrow();
        approvedBorrow.setId(borrowId);
        approvedBorrow.setStatus("APPROVED");
        approvedBorrow.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(approvedBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.approve(borrowId);
        });
        assertTrue(exception.getMessage().contains("已被批准"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testApprove_AlreadyRejectedStatus_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow rejectedBorrow = new Borrow();
        rejectedBorrow.setId(borrowId);
        rejectedBorrow.setStatus("REJECTED");
        rejectedBorrow.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(rejectedBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.approve(borrowId);
        });
        assertTrue(exception.getMessage().contains("已被拒绝"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testApprove_AlreadyReturnedStatus_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow returnedBorrow = new Borrow();
        returnedBorrow.setId(borrowId);
        returnedBorrow.setStatus("RETURNED");
        returnedBorrow.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(returnedBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.approve(borrowId);
        });
        assertTrue(exception.getMessage().contains("已归还"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testApprove_AlreadyHasApprover_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow alreadyApproved = new Borrow();
        alreadyApproved.setId(borrowId);
        alreadyApproved.setStatus("PENDING");
        alreadyApproved.setApprover(testApprover);
        alreadyApproved.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(alreadyApproved));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.approve(borrowId);
        });
        assertTrue(exception.getMessage().contains("已被审批"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_ShouldSetStatusRejectedAndRejectReason() {
        Long borrowId = 1L;
        String rejectReason = "设备维护中";

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(pendingBorrow));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testApprover));
        when(borrowRepository.save(pendingBorrow)).thenReturn(pendingBorrow);

        Borrow result = borrowService.reject(borrowId, rejectReason);

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        assertEquals(testApprover, result.getApprover());
        assertEquals("设备维护中", result.getRejectReason());
        assertNotNull(result.getRejectTime());
        assertNull(result.getApproveTime());

        verify(borrowRepository).findByIdWithLock(borrowId);
        verify(userRepository).findById(2L);
        verify(borrowRepository).save(pendingBorrow);
    }

    @Test
    void testReject_AlreadyRejectedStatus_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow rejectedBorrow = new Borrow();
        rejectedBorrow.setId(borrowId);
        rejectedBorrow.setStatus("REJECTED");
        rejectedBorrow.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(rejectedBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, "原因");
        });
        assertTrue(exception.getMessage().contains("已被拒绝"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_AlreadyApprovedStatus_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow approvedBorrow = new Borrow();
        approvedBorrow.setId(borrowId);
        approvedBorrow.setStatus("APPROVED");
        approvedBorrow.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(approvedBorrow));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, "原因");
        });
        assertTrue(exception.getMessage().contains("已被批准"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_BlankRejectReason_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, "");
        });
        assertTrue(exception.getMessage().contains("拒绝原因不能为空"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_NullRejectReason_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, null);
        });
        assertTrue(exception.getMessage().contains("拒绝原因不能为空"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_AlreadyHasApprover_ShouldThrowBusinessException() {
        Long borrowId = 1L;

        Borrow alreadyApproved = new Borrow();
        alreadyApproved.setId(borrowId);
        alreadyApproved.setStatus("PENDING");
        alreadyApproved.setApprover(testApprover);
        alreadyApproved.setEquipment(testEquipment);

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(alreadyApproved));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, "原因");
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

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(approvedBorrow));
        when(borrowRepository.save(approvedBorrow)).thenReturn(approvedBorrow);

        Borrow result = borrowService.returnEquipment(borrowId);

        assertNotNull(result);
        assertEquals("RETURNED", result.getStatus());
        assertEquals("NORMAL", result.getEquipment().getStatus());

        verify(borrowRepository).findByIdWithLock(borrowId);
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

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.of(pendingBorrow));

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

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.approve(borrowId);
        });
        assertTrue(exception.getMessage().contains("借用记录不存在"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_BorrowNotFound_ShouldThrowBusinessException() {
        Long borrowId = 999L;

        when(borrowRepository.findByIdWithLock(borrowId)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, "原因");
        });
        assertTrue(exception.getMessage().contains("借用记录不存在"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }

    @Test
    void testReject_ReasonTooLong_ShouldThrowBusinessException() {
        Long borrowId = 1L;
        String longReason = "a".repeat(501);

        Borrow pendingBorrow = new Borrow();
        pendingBorrow.setId(borrowId);
        pendingBorrow.setStatus("PENDING");
        pendingBorrow.setEquipment(testEquipment);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            borrowService.reject(borrowId, longReason);
        });
        assertTrue(exception.getMessage().contains("500"));

        verify(borrowRepository, never()).save(any(Borrow.class));
    }
}
