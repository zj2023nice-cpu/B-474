package com.example.lab.service;

import com.example.lab.constant.RoleConstant;
import com.example.lab.dto.FinishRepairRequest;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Repair;
import com.example.lab.entity.User;
import com.example.lab.enums.EquipmentStatus;
import com.example.lab.enums.RepairStatus;
import com.example.lab.exception.BusinessException;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.RepairRepository;
import com.example.lab.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepairServiceTest {

    @Mock
    private RepairRepository repairRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StateConstraintService stateConstraintService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private RepairService repairService;

    private Equipment testEquipment;
    private Repair testRepair;
    private User testReporter;

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);
        testEquipment.setName("显微镜");
        testEquipment.setStatus(EquipmentStatus.NORMAL);

        testRepair = new Repair();
        testRepair.setId(1L);
        testRepair.setEquipment(testEquipment);
        testRepair.setStatus(RepairStatus.REPORTED);
        testRepair.setDescription("设备故障报修");

        testReporter = new User();
        testReporter.setId(1L);
        testReporter.setName("报修教师");
        testReporter.setRole("TEACHER");

        Collection<GrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority(RoleConstant.ROLE_TEACHER)
        );
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(1L);
        lenient().when(authentication.getAuthorities()).thenAnswer(inv -> authorities);
        SecurityContextHolder.setContext(securityContext);

        lenient().doNothing().when(stateConstraintService).assertOperationAllowed(any(), any(Equipment.class));
        lenient().doNothing().when(stateConstraintService).assertOperationAllowed(any(), anyLong());
        lenient().doNothing().when(stateConstraintService).assertRepairOperationAllowed(any(), any(Repair.class));
        lenient().doNothing().when(stateConstraintService).assertRepairOperationAllowed(any(), anyLong());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== 报修阶段：状态流转 ====================

    @Test
    void testReport_ShouldSetStatusREPORTEDAndMarkEquipmentREPAIRING_WithCaptor() {
        Repair newRepair = new Repair();
        newRepair.setEquipment(testEquipment);
        newRepair.setDescription("设备需要维修");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testReporter));
        when(equipmentRepository.findByIdWithLock(testEquipment.getId())).thenReturn(Optional.of(testEquipment));

        Repair savedRepair = new Repair();
        savedRepair.setId(1L);
        savedRepair.setStatus(RepairStatus.REPORTED);
        savedRepair.setEquipment(testEquipment);

        when(repairRepository.save(any(Repair.class))).thenAnswer(inv -> {
            Repair r = inv.getArgument(0);
            assertEquals(RepairStatus.REPORTED, r.getStatus(), "维修单初始状态必须为 REPORTED");
            assertNotNull(r.getReportDate(), "reportDate 应由服务端设置");
            assertEquals(testReporter, r.getReporter(), "报修人应为当前登录用户");
            assertNull(r.getRepairConclusion(), "报修时不应有维修结论");
            assertNull(r.getFinishDate(), "报修时不应有完成时间");
            savedRepair.setReportDate(r.getReportDate());
            savedRepair.setReporter(r.getReporter());
            return savedRepair;
        });

        Repair result = repairService.report(newRepair);

        assertNotNull(result);
        assertEquals(RepairStatus.REPORTED, result.getStatus());

        ArgumentCaptor<Equipment> equipmentCaptor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentRepository).save(equipmentCaptor.capture());
        Equipment savedEquipment = equipmentCaptor.getValue();
        assertEquals(EquipmentStatus.REPAIRING, savedEquipment.getStatus(),
                "报修后设备状态必须精确变为 REPAIRING，防回归：有人漏写 equipment.setStatus(REPAIRING)");
        assertSame(testEquipment, savedEquipment,
                "必须是 findByIdWithLock 返回的被锁定设备对象被保存");
    }

    @Test
    void testReport_WithNonExistentEquipment_ShouldThrowException() {
        Repair newRepair = new Repair();
        newRepair.setEquipment(testEquipment);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testReporter));
        when(equipmentRepository.findByIdWithLock(testEquipment.getId())).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> {
            repairService.report(newRepair);
        });

        verify(repairRepository, never()).save(any(Repair.class));
    }

    @Test
    void testReport_DuplicateRepairWithActiveOne_ShouldBeRejected_ByStateConstraint() {
        Repair newRepair = new Repair();
        newRepair.setEquipment(testEquipment);
        newRepair.setDescription("重复报修");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testReporter));
        when(equipmentRepository.findByIdWithLock(testEquipment.getId())).thenReturn(Optional.of(testEquipment));
        doThrow(new BusinessException("该设备存在 1 条未完成的维修记录，不允许重复报修。请先完成或取消现有维修单。"))
                .when(stateConstraintService).assertOperationAllowed(any(), eq(testEquipment));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            repairService.report(newRepair);
        });
        assertTrue(exception.getMessage().contains("未完成的维修记录") || exception.getMessage().contains("重复报修"),
                "已有活动维修时重复报修必须被拒绝，防回归：StateConstraint 校验被跳过");

        verify(repairRepository, never()).save(any(Repair.class));
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    // ==================== 完成维修阶段：状态联动 ====================

    @Test
    void testFinish_ShouldSetStatusFINISHEDAndResetToNORMALWhenNoActiveRepairs_WithCaptor() {
        Long repairId = 1L;

        Equipment repairingEquipment = new Equipment();
        repairingEquipment.setId(testEquipment.getId());
        repairingEquipment.setStatus(EquipmentStatus.REPAIRING);

        Repair reportedRepair = new Repair();
        reportedRepair.setId(repairId);
        reportedRepair.setStatus(RepairStatus.REPORTED);
        reportedRepair.setEquipment(repairingEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(reportedRepair));
        when(equipmentRepository.findByIdWithLock(repairingEquipment.getId())).thenReturn(Optional.of(repairingEquipment));
        when(repairRepository.save(reportedRepair)).thenReturn(reportedRepair);
        when(repairRepository.hasActiveRepairsByEquipment(repairingEquipment.getId())).thenReturn(false);

        FinishRepairRequest request = new FinishRepairRequest();
        request.setRepairConclusion("已修复，更换损坏零件");
        request.setRepairCompany("XX维修公司");
        request.setCost(BigDecimal.valueOf(250));

        Repair result = repairService.finish(repairId, request);

        assertNotNull(result);
        assertEquals(RepairStatus.FINISHED, result.getStatus(), "维修完成后状态应为 FINISHED");
        assertNotNull(result.getFinishDate(), "完成时间必须被设置");
        assertEquals("已修复，更换损坏零件", result.getRepairConclusion());
        assertEquals("XX维修公司", result.getRepairCompany());
        assertEquals(0, BigDecimal.valueOf(250).compareTo(result.getCost()));

        ArgumentCaptor<Equipment> equipmentCaptor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentRepository).save(equipmentCaptor.capture());
        assertEquals(EquipmentStatus.NORMAL, equipmentCaptor.getValue().getStatus(),
                "维修完成且无其他活动维修时，设备必须精确恢复为 NORMAL，防回归：漏写 hasActiveRepairs 判断后的 setStatus");
    }

    @Test
    void testFinish_WithMultipleActiveRepairs_ShouldKeepREPAIRINGAfterFinishingOne() {
        Long repairId = 1L;

        Equipment repairingEquipment = new Equipment();
        repairingEquipment.setId(testEquipment.getId());
        repairingEquipment.setStatus(EquipmentStatus.REPAIRING);

        Repair reportedRepair = new Repair();
        reportedRepair.setId(repairId);
        reportedRepair.setStatus(RepairStatus.REPORTED);
        reportedRepair.setEquipment(repairingEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(reportedRepair));
        when(repairRepository.save(reportedRepair)).thenReturn(reportedRepair);
        when(repairRepository.hasActiveRepairsByEquipment(repairingEquipment.getId())).thenReturn(true);

        FinishRepairRequest request = new FinishRepairRequest();
        request.setRepairConclusion("第一条修好了，但还有另一条维修中");

        Repair result = repairService.finish(repairId, request);

        assertEquals(RepairStatus.FINISHED, result.getStatus(), "当前维修单确实要 FINISHED");

        verify(repairRepository).hasActiveRepairsByEquipment(repairingEquipment.getId());
        verify(equipmentRepository, never()).save(any(Equipment.class));
        assertEquals(EquipmentStatus.REPAIRING, repairingEquipment.getStatus(),
                "完成一条但还有其他活动维修时，设备状态不得被改动，防回归：无条件重置 NORMAL");
    }

    @Test
    void testFinish_AlreadyFINISHED_ShouldBeRejected_ProtectsAgainstDoubleFinish() {
        Long repairId = 1L;

        Repair finishedRepair = new Repair();
        finishedRepair.setId(repairId);
        finishedRepair.setStatus(RepairStatus.FINISHED);
        finishedRepair.setEquipment(testEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(finishedRepair));
        doThrow(new BusinessException("维修单当前状态为「已完成」，不允许完成。仅「已上报」或「维修中」状态的维修单可完成。"))
                .when(stateConstraintService).assertRepairOperationAllowed(any(), eq(finishedRepair));

        FinishRepairRequest request = new FinishRepairRequest();
        request.setRepairConclusion("重复完成");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            repairService.finish(repairId, request);
        });
        assertTrue(exception.getMessage().contains("不允许完成") || exception.getMessage().contains("已完成"),
                "FINISHED 状态的维修单不允许再次 finish，防回归：状态机被绕过");

        verify(repairRepository, never()).save(any(Repair.class));
    }

    @Test
    void testFinish_BlankConclusion_ShouldBeRejected() {
        Long repairId = 1L;

        Repair reportedRepair = new Repair();
        reportedRepair.setId(repairId);
        reportedRepair.setStatus(RepairStatus.REPORTED);
        reportedRepair.setEquipment(testEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(reportedRepair));

        FinishRepairRequest request = new FinishRepairRequest();
        request.setRepairConclusion("   ");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            repairService.finish(repairId, request);
        });
        assertTrue(exception.getMessage().contains("维修结论不能为空"),
                "空白维修结论必须被拒绝，防回归：StringUtils.hasText 判空被删除");
    }

    // ==================== 删除维修阶段：状态联动 ====================

    @Test
    void testDelete_REPORTEDStatus_NoOtherActive_ShouldResetEquipmentToNORMAL() {
        Long repairId = 1L;

        Equipment repairingEquipment = new Equipment();
        repairingEquipment.setId(testEquipment.getId());
        repairingEquipment.setStatus(EquipmentStatus.REPAIRING);

        Repair repairToDelete = new Repair();
        repairToDelete.setId(repairId);
        repairToDelete.setStatus(RepairStatus.REPORTED);
        repairToDelete.setEquipment(repairingEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(repairToDelete));
        doNothing().when(repairRepository).deleteById(repairId);
        when(equipmentRepository.findByIdWithLock(repairingEquipment.getId())).thenReturn(Optional.of(repairingEquipment));
        when(repairRepository.hasActiveRepairsByEquipment(repairingEquipment.getId())).thenReturn(false);

        repairService.delete(repairId);

        ArgumentCaptor<Equipment> equipmentCaptor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentRepository).save(equipmentCaptor.capture());
        assertEquals(EquipmentStatus.NORMAL, equipmentCaptor.getValue().getStatus(),
                "删除最后一条活动维修后，设备必须恢复 NORMAL，防回归：删除后忘记恢复状态");
        verify(repairRepository).deleteById(repairId);
    }

    @Test
    void testDelete_WithOtherActiveRepairs_ShouldKeepREPAIRING() {
        Long repairIdToDelete = 1L;
        Long otherRepairId = 2L;

        Equipment repairingEquipment = new Equipment();
        repairingEquipment.setId(testEquipment.getId());
        repairingEquipment.setStatus(EquipmentStatus.REPAIRING);

        Repair repairToDelete = new Repair();
        repairToDelete.setId(repairIdToDelete);
        repairToDelete.setStatus(RepairStatus.REPORTED);
        repairToDelete.setEquipment(repairingEquipment);

        Repair otherActiveRepair = new Repair();
        otherActiveRepair.setId(otherRepairId);
        otherActiveRepair.setStatus(RepairStatus.IN_PROGRESS);
        otherActiveRepair.setEquipment(repairingEquipment);

        when(repairRepository.findById(repairIdToDelete)).thenReturn(Optional.of(repairToDelete));
        doNothing().when(repairRepository).deleteById(repairIdToDelete);
        when(equipmentRepository.findByIdWithLock(repairingEquipment.getId())).thenReturn(Optional.of(repairingEquipment));
        when(repairRepository.hasActiveRepairsByEquipment(repairingEquipment.getId())).thenReturn(true);

        repairService.delete(repairIdToDelete);

        verify(repairRepository).hasActiveRepairsByEquipment(repairingEquipment.getId());
        verify(equipmentRepository, never()).save(any(Equipment.class));
        assertEquals(EquipmentStatus.REPAIRING, repairingEquipment.getStatus(),
                "存在其他活动维修时，删除一条后设备必须继续保持 REPAIRING，防回归：忽略其他维修单直接 NORMAL");
    }

    @Test
    void testDelete_AlreadyFINISHED_ShouldBeRejected_CancelOnlyAllowsREPORTED() {
        Long repairId = 1L;

        Repair finishedRepair = new Repair();
        finishedRepair.setId(repairId);
        finishedRepair.setStatus(RepairStatus.FINISHED);
        finishedRepair.setEquipment(testEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(finishedRepair));
        doThrow(new BusinessException("维修单当前状态为「已完成」，不允许取消。仅「已上报」状态的维修单可取消。"))
                .when(stateConstraintService).assertRepairOperationAllowed(any(), eq(finishedRepair));

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            repairService.delete(repairId);
        });
        assertTrue(exception.getMessage().contains("不允许取消") || exception.getMessage().contains("已完成"),
                "FINISHED 状态的维修单不允许删除（delete 走 CANCEL_REPAIR 校验，canCancel 仅 REPORTED），防回归：状态机校验被删除");

        verify(repairRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDelete_WithNonExistentRepair_ShouldThrowException() {
        Long nonExistentId = 999L;

        when(repairRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> {
            repairService.delete(nonExistentId);
        });

        verify(repairRepository, never()).deleteById(anyLong());
    }

    // ==================== 工具查询 ====================

    @Test
    void testFindAll_ShouldReturnAllRepairs() {
        Repair r1 = new Repair();
        r1.setId(1L);

        Repair r2 = new Repair();
        r2.setId(2L);

        List<Repair> repairs = Arrays.asList(r1, r2);

        when(repairRepository.findAll()).thenReturn(repairs);

        List<Repair> result = repairService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repairRepository).findAll();
    }

    // ==================== 三条活动维修场景：逐步 finish 观察状态 ====================

    @Test
    void testFinish_TwoOfThreeActiveRepairs_EquipmentStillREPAIRING_UntilLastOneFinished() {
        Equipment repairingEquipment = new Equipment();
        repairingEquipment.setId(testEquipment.getId());
        repairingEquipment.setStatus(EquipmentStatus.REPAIRING);

        Repair repair1 = new Repair();
        repair1.setId(1L);
        repair1.setStatus(RepairStatus.REPORTED);
        repair1.setEquipment(repairingEquipment);

        Repair repair2 = new Repair();
        repair2.setId(2L);
        repair2.setStatus(RepairStatus.IN_PROGRESS);
        repair2.setEquipment(repairingEquipment);

        Repair repair3 = new Repair();
        repair3.setId(3L);
        repair3.setStatus(RepairStatus.REPORTED);
        repair3.setEquipment(repairingEquipment);

        when(repairRepository.findById(1L)).thenReturn(Optional.of(repair1));
        when(repairRepository.findById(2L)).thenReturn(Optional.of(repair2));
        when(repairRepository.findById(3L)).thenReturn(Optional.of(repair3));
        when(repairRepository.save(any(Repair.class))).thenAnswer(inv -> inv.getArgument(0));
        when(equipmentRepository.findByIdWithLock(repairingEquipment.getId())).thenReturn(Optional.of(repairingEquipment));

        when(repairRepository.hasActiveRepairsByEquipment(repairingEquipment.getId()))
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        FinishRepairRequest request = new FinishRepairRequest();
        request.setRepairConclusion("完成");

        repairService.finish(1L, request);
        verify(equipmentRepository, never()).save(any(Equipment.class));
        assertEquals(EquipmentStatus.REPAIRING, repairingEquipment.getStatus(),
                "3 条活动维修 finish 第 1 条后，仍有 2 条，设备保持 REPAIRING");

        repairService.finish(2L, request);
        verify(equipmentRepository, never()).save(any(Equipment.class));
        assertEquals(EquipmentStatus.REPAIRING, repairingEquipment.getStatus(),
                "3 条活动维修 finish 第 2 条后，仍有 1 条，设备保持 REPAIRING");

        repairService.finish(3L, request);
        ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
        verify(equipmentRepository).save(captor.capture());
        assertEquals(EquipmentStatus.NORMAL, captor.getValue().getStatus(),
                "最后一条活动维修 finish 后，设备必须恢复 NORMAL");
    }
}
