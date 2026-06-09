package com.example.lab.service;

import com.example.lab.dto.FinishRepairRequest;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Repair;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.RepairRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
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

    @InjectMocks
    private RepairService repairService;

    private Equipment testEquipment;
    private Repair testRepair;

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);
        testEquipment.setName("显微镜");
        testEquipment.setStatus("NORMAL");

        testRepair = new Repair();
        testRepair.setId(1L);
        testRepair.setEquipment(testEquipment);
        testRepair.setStatus("REPORTED");
        testRepair.setDescription("设备故障报修");
    }

    @Test
    void testReport_ShouldSetStatusReportedAndUpdateEquipment() {
        Repair newRepair = new Repair();
        newRepair.setEquipment(testEquipment);
        newRepair.setDescription("设备需要维修");

        when(equipmentRepository.findById(testEquipment.getId())).thenReturn(Optional.of(testEquipment));

        Equipment repairingEquipment = new Equipment();
        repairingEquipment.setId(testEquipment.getId());
        repairingEquipment.setStatus("REPAIRING");

        when(equipmentRepository.save(testEquipment)).thenReturn(repairingEquipment);

        Repair savedRepair = new Repair();
        savedRepair.setId(1L);
        savedRepair.setStatus("REPORTED");
        savedRepair.setEquipment(repairingEquipment);

        when(repairRepository.save(any(Repair.class))).thenReturn(savedRepair);

        Repair result = repairService.report(newRepair);

        assertNotNull(result);
        assertEquals("REPORTED", result.getStatus());
        assertNotNull(result.getReportDate());
        assertEquals("REPAIRING", result.getEquipment().getStatus());

        verify(equipmentRepository).findById(testEquipment.getId());
        verify(equipmentRepository).save(testEquipment);
        verify(repairRepository).save(any(Repair.class));
    }

    @Test
    void testReport_WithNonExistentEquipment_ShouldThrowException() {
        Repair newRepair = new Repair();
        newRepair.setEquipment(testEquipment);

        when(equipmentRepository.findById(testEquipment.getId())).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> {
            repairService.report(newRepair);
        });

        verify(repairRepository, never()).save(any(Repair.class));
    }

    @Test
    void testFinish_ShouldSetStatusFinishedAndResetEquipmentIfNoActiveRepairs() {
        Long repairId = 1L;

        Repair reportedRepair = new Repair();
        reportedRepair.setId(repairId);
        reportedRepair.setStatus("REPORTED");
        reportedRepair.setEquipment(testEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(reportedRepair));

        Repair finishedRepair = new Repair();
        finishedRepair.setId(repairId);
        finishedRepair.setStatus("FINISHED");
        finishedRepair.setEquipment(testEquipment);
        finishedRepair.setRepairConclusion("已修复");

        when(repairRepository.save(reportedRepair)).thenReturn(finishedRepair);
        when(repairRepository.hasActiveRepairsByEquipment(testEquipment.getId())).thenReturn(false);

        Equipment normalEquipment = new Equipment();
        normalEquipment.setId(testEquipment.getId());
        normalEquipment.setStatus("NORMAL");

        when(equipmentRepository.save(testEquipment)).thenReturn(normalEquipment);

        FinishRepairRequest request = new FinishRepairRequest();
        request.setRepairConclusion("已修复");
        request.setRepairCompany("维修公司A");
        request.setCost(java.math.BigDecimal.valueOf(100));

        Repair result = repairService.finish(repairId, request);

        assertNotNull(result);
        assertEquals("FINISHED", result.getStatus());
        assertNotNull(result.getFinishDate());
        assertEquals("已修复", result.getRepairConclusion());

        verify(repairRepository).findById(repairId);
        verify(repairRepository).save(reportedRepair);
        verify(repairRepository).hasActiveRepairsByEquipment(testEquipment.getId());
        verify(equipmentRepository).save(testEquipment);
    }

    @Test
    void testFinish_WithOtherActiveRepairs_ShouldNotResetEquipmentStatus() {
        Long repairId = 1L;

        Repair reportedRepair = new Repair();
        reportedRepair.setId(repairId);
        reportedRepair.setStatus("REPORTED");
        reportedRepair.setEquipment(testEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(reportedRepair));

        Repair finishedRepair = new Repair();
        finishedRepair.setId(repairId);
        finishedRepair.setStatus("FINISHED");
        finishedRepair.setEquipment(testEquipment);
        finishedRepair.setRepairConclusion("维修完毕");

        when(repairRepository.save(reportedRepair)).thenReturn(finishedRepair);
        when(repairRepository.hasActiveRepairsByEquipment(testEquipment.getId())).thenReturn(true);

        FinishRepairRequest request = new FinishRepairRequest();
        request.setRepairConclusion("维修完毕");

        Repair result = repairService.finish(repairId, request);

        assertNotNull(result);
        assertEquals("FINISHED", result.getStatus());
        assertEquals("维修完毕", result.getRepairConclusion());

        verify(repairRepository).hasActiveRepairsByEquipment(testEquipment.getId());
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

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

    @Test
    void testDelete_ShouldDeleteRepairAndResetEquipmentIfNoActiveRepairs() {
        Long repairId = 1L;

        Repair repairToDelete = new Repair();
        repairToDelete.setId(repairId);
        repairToDelete.setEquipment(testEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(repairToDelete));
        doNothing().when(repairRepository).deleteById(repairId);
        when(repairRepository.hasActiveRepairsByEquipment(testEquipment.getId())).thenReturn(false);

        Equipment normalEquipment = new Equipment();
        normalEquipment.setId(testEquipment.getId());
        normalEquipment.setStatus("NORMAL");

        when(equipmentRepository.save(testEquipment)).thenReturn(normalEquipment);

        repairService.delete(repairId);

        verify(repairRepository).findById(repairId);
        verify(repairRepository).deleteById(repairId);
        verify(repairRepository).hasActiveRepairsByEquipment(testEquipment.getId());
        verify(equipmentRepository).save(testEquipment);
    }

    @Test
    void testDelete_WithOtherActiveRepairs_ShouldNotResetEquipmentStatus() {
        Long repairId = 1L;

        Repair repairToDelete = new Repair();
        repairToDelete.setId(repairId);
        repairToDelete.setEquipment(testEquipment);

        when(repairRepository.findById(repairId)).thenReturn(Optional.of(repairToDelete));
        doNothing().when(repairRepository).deleteById(repairId);
        when(repairRepository.hasActiveRepairsByEquipment(testEquipment.getId())).thenReturn(true);

        repairService.delete(repairId);

        verify(repairRepository).hasActiveRepairsByEquipment(testEquipment.getId());
        verify(equipmentRepository, never()).save(any(Equipment.class));
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
}
