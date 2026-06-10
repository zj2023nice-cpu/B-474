package com.example.lab.service;

import com.example.lab.dto.LabDetailDTO;
import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Lab;
import com.example.lab.entity.Repair;
import com.example.lab.entity.User;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.LabRepository;
import com.example.lab.repository.RepairRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabServiceTest {

    @Mock
    private LabRepository labRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private RepairRepository repairRepository;

    @InjectMocks
    private LabService labService;

    private Lab testLab;

    @BeforeEach
    void setUp() {
        testLab = new Lab();
        testLab.setId(1L);
        testLab.setName("物理实验室");
        testLab.setBuilding("实验楼A");
        testLab.setRoom("A101");
        testLab.setPicName("张老师");
        testLab.setPicPhone("13800138000");
        testLab.setCapacity(50);
    }

    @Test
    void testSave_ShouldSaveLabSuccessfully() {
        when(labRepository.save(any(Lab.class))).thenReturn(testLab);

        Lab result = labService.save(testLab);

        assertNotNull(result);
        assertEquals(testLab.getId(), result.getId());
        assertEquals(testLab.getName(), result.getName());
        verify(labRepository).save(testLab);
    }

    @Test
    void testSave_WithNewLab_ShouldGenerateId() {
        Lab newLab = new Lab();
        newLab.setName("化学实验室");
        newLab.setBuilding("实验楼B");

        Lab savedLab = new Lab();
        savedLab.setId(2L);
        savedLab.setName("化学实验室");
        savedLab.setBuilding("实验楼B");

        when(labRepository.save(newLab)).thenReturn(savedLab);

        Lab result = labService.save(newLab);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("化学实验室", result.getName());
        verify(labRepository).save(newLab);
    }

    @Test
    void testFindAll_ShouldReturnAllLabs() {
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("实验室1");

        Lab lab2 = new Lab();
        lab2.setId(2L);
        lab2.setName("实验室2");

        List<Lab> labs = Arrays.asList(lab1, lab2);

        when(labRepository.findAll()).thenReturn(labs);

        List<Lab> result = labService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("实验室1", result.get(0).getName());
        assertEquals("实验室2", result.get(1).getName());
        verify(labRepository).findAll();
    }

    @Test
    void testFindAll_WithEmptyList_ShouldReturnEmptyList() {
        when(labRepository.findAll()).thenReturn(Arrays.asList());

        List<Lab> result = labService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(labRepository).findAll();
    }

    @Test
    void testDelete_ShouldCallRepositoryDelete() {
        Long labId = 1L;

        doNothing().when(labRepository).deleteById(labId);

        labService.delete(labId);

        verify(labRepository).deleteById(labId);
    }

    @Test
    void testDelete_WithNonExistentId_ShouldNotThrowException() {
        Long nonExistentId = 999L;

        doNothing().when(labRepository).deleteById(nonExistentId);

        assertDoesNotThrow(() -> {
            labService.delete(nonExistentId);
        });

        verify(labRepository).deleteById(nonExistentId);
    }

    @Test
    void testSave_WithNullLab_ShouldThrowException() {
        when(labRepository.save(null)).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> {
            labService.save(null);
        });
    }

    // ==================== getDetail 聚合测试 ====================

    private Equipment createEquipment(Long id, String code, String name, String status, LocalDate purchaseDate, Integer lifeSpan) {
        Equipment eq = new Equipment();
        eq.setId(id);
        eq.setCode(code);
        eq.setName(name);
        eq.setStatus(status);
        eq.setPurchaseDate(purchaseDate);
        eq.setLifeSpan(lifeSpan);
        eq.setLab(testLab);
        eq.setPrice(BigDecimal.ZERO);
        return eq;
    }

    private User createUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setUsername("user" + id);
        user.setPassword("pwd");
        user.setRole("STUDENT");
        return user;
    }

    private Borrow createBorrow(Long id, Equipment eq, User applicant, LocalDateTime startTime, LocalDateTime endTime) {
        Borrow b = new Borrow();
        b.setId(id);
        b.setEquipment(eq);
        b.setApplicant(applicant);
        b.setStatus("APPROVED");
        b.setApplyDate(LocalDateTime.now().minusDays(1));
        b.setStartTime(startTime);
        b.setEndTime(endTime);
        b.setPurpose("实验用途");
        return b;
    }

    private Repair createRepair(Long id, Equipment eq, String description, String status) {
        Repair r = new Repair();
        r.setId(id);
        r.setEquipment(eq);
        r.setDescription(description);
        r.setStatus(status);
        r.setReportDate(LocalDateTime.now().minusHours(2));
        r.setReporter(createUser(10L, "报修人"));
        r.setCost(BigDecimal.ZERO);
        return r;
    }

    @Test
    void testGetDetail_ExpiringEquipments_ShouldExcludeScrappedAndMapCorrectly() {
        LocalDate today = LocalDate.now();
        Equipment eq1 = createEquipment(1L, "LAB01-001", "显微镜", "NORMAL", today.minusYears(2).plusDays(10), 2);
        Equipment eq2 = createEquipment(2L, "LAB01-002", "离心机", "NORMAL", today.minusYears(5), 5);
        Equipment eq3 = createEquipment(3L, "LAB01-003", "老设备", "SCRAPPED", today.minusYears(10).plusDays(5), 10);
        Equipment eq4 = createEquipment(4L, "LAB01-004", "烧杯", "NORMAL", null, null);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(equipmentRepository.countByLab_Id(1L)).thenReturn(4L);
        when(equipmentRepository.countByLab_IdAndStatus(anyLong(), anyString())).thenReturn(0L);
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(eq1, eq2, eq3, eq4));
        when(borrowRepository.findByEquipment_Lab_IdAndStatusIn(eq(1L), anyList())).thenReturn(Collections.emptyList());
        when(repairRepository.findByEquipment_Lab_IdAndStatusNot(eq(1L), eq("FINISHED"))).thenReturn(Collections.emptyList());

        LabDetailDTO result = labService.getDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("物理实验室", result.getName());
        assertEquals(4L, result.getTotalEquipment());

        assertNotNull(result.getExpiringEquipments());
        assertEquals(2, result.getExpiringCount());
        assertEquals(2, result.getExpiringEquipments().size());

        assertEquals("LAB01-002", result.getExpiringEquipments().get(0).getCode());
        assertEquals("LAB01-001", result.getExpiringEquipments().get(1).getCode());

        boolean hasScrapped = result.getExpiringEquipments().stream()
                .anyMatch(e -> "LAB01-003".equals(e.getCode()));
        assertFalse(hasScrapped, "报废设备不应出现在即将到期列表中");

        boolean hasNoDate = result.getExpiringEquipments().stream()
                .anyMatch(e -> "LAB01-004".equals(e.getCode()));
        assertFalse(hasNoDate, "无采购日期或寿命的设备不应计入");
    }

    @Test
    void testGetDetail_ActiveBorrows_ShouldMapCorrectly() {
        Equipment eq1 = createEquipment(1L, "LAB01-001", "显微镜", "BORROWED", LocalDate.now().minusYears(1), 5);
        Equipment eq2 = createEquipment(2L, "LAB01-002", "离心机", "BORROWED", LocalDate.now().minusYears(2), 5);
        User applicant = createUser(100L, "张同学");

        Borrow borrow1 = createBorrow(1L, eq1, applicant, LocalDateTime.now(), LocalDateTime.now().plusDays(3));
        Borrow borrow2 = createBorrow(2L, eq2, applicant, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2));
        borrow2.setApplyDate(LocalDateTime.now().minusDays(2));

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(equipmentRepository.countByLab_Id(1L)).thenReturn(2L);
        when(equipmentRepository.countByLab_IdAndStatus(anyLong(), anyString())).thenReturn(0L);
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(eq1, eq2));
        when(borrowRepository.findByEquipment_Lab_IdAndStatusIn(eq(1L), anyList()))
                .thenReturn(Arrays.asList(borrow1, borrow2));
        when(repairRepository.findByEquipment_Lab_IdAndStatusNot(eq(1L), eq("FINISHED"))).thenReturn(Collections.emptyList());

        LabDetailDTO result = labService.getDetail(1L);

        assertEquals(2L, result.getActiveBorrowCount());
        assertNotNull(result.getActiveBorrows());
        assertEquals(2, result.getActiveBorrows().size());

        LabDetailDTO.BorrowSummary first = result.getActiveBorrows().get(0);
        assertEquals(1L, first.getId());
        assertEquals("显微镜", first.getEquipmentName());
        assertEquals("LAB01-001", first.getEquipmentCode());
        assertEquals("张同学", first.getApplicantName());
        assertNotNull(first.getStartTime());
        assertNotNull(first.getEndTime());
        assertEquals("实验用途", first.getPurpose());
    }

    @Test
    void testGetDetail_ActiveRepairs_ShouldMapCorrectly() {
        Equipment eq1 = createEquipment(1L, "LAB01-001", "显微镜", "REPAIRING", LocalDate.now().minusYears(1), 5);
        Equipment eq2 = createEquipment(2L, "LAB01-002", "离心机", "REPAIRING", LocalDate.now().minusYears(2), 5);

        Repair r1 = createRepair(1L, eq1, "镜头模糊", "IN_PROGRESS");
        Repair r2 = createRepair(2L, eq2, "不转了", "REPORTED");

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(equipmentRepository.countByLab_Id(1L)).thenReturn(2L);
        when(equipmentRepository.countByLab_IdAndStatus(anyLong(), anyString())).thenReturn(0L);
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(eq1, eq2));
        when(borrowRepository.findByEquipment_Lab_IdAndStatusIn(eq(1L), anyList())).thenReturn(Collections.emptyList());
        when(repairRepository.findByEquipment_Lab_IdAndStatusNot(eq(1L), eq("FINISHED")))
                .thenReturn(Arrays.asList(r1, r2));

        LabDetailDTO result = labService.getDetail(1L);

        assertEquals(2L, result.getActiveRepairCount());
        assertNotNull(result.getActiveRepairs());
        assertEquals(2, result.getActiveRepairs().size());

        LabDetailDTO.RepairSummary first = result.getActiveRepairs().get(0);
        assertEquals(1L, first.getId());
        assertEquals("显微镜", first.getEquipmentName());
        assertEquals("LAB01-001", first.getEquipmentCode());
        assertEquals("镜头模糊", first.getDescription());
        assertEquals("IN_PROGRESS", first.getStatus());
        assertNotNull(first.getReportDate());
    }

    @Test
    void testGetDetail_EmptyEquipments_ShouldReturnEmptyAggregations() {
        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(equipmentRepository.countByLab_Id(1L)).thenReturn(0L);
        when(equipmentRepository.countByLab_IdAndStatus(anyLong(), anyString())).thenReturn(0L);
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Collections.emptyList());
        when(borrowRepository.findByEquipment_Lab_IdAndStatusIn(eq(1L), anyList())).thenReturn(Collections.emptyList());
        when(repairRepository.findByEquipment_Lab_IdAndStatusNot(eq(1L), eq("FINISHED"))).thenReturn(Collections.emptyList());

        LabDetailDTO result = labService.getDetail(1L);

        assertEquals(0L, result.getTotalEquipment());
        assertEquals(0L, result.getExpiringCount());
        assertTrue(result.getExpiringEquipments().isEmpty());
        assertEquals(0L, result.getActiveBorrowCount());
        assertTrue(result.getActiveBorrows().isEmpty());
        assertEquals(0L, result.getActiveRepairCount());
        assertTrue(result.getActiveRepairs().isEmpty());
    }

    @Test
    void testGetDetail_LabNotFound_ShouldThrowException() {
        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            labService.getDetail(999L);
        });
    }
}
