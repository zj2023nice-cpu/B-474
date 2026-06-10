package com.example.lab.service;

import com.example.lab.dto.ExpiringEquipmentDTO;
import com.example.lab.dto.ExpiringQuery;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Lab;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.LabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private LabRepository labRepository;

    @InjectMocks
    private EquipmentService equipmentService;

    private Lab testLab;
    private Equipment testEquipment;

    @BeforeEach
    void setUp() {
        testLab = new Lab();
        testLab.setId(1L);
        testLab.setName("物理实验室");

        testEquipment = new Equipment();
        testEquipment.setId(1L);
        testEquipment.setCode("LAB01-001");
        testEquipment.setName("显微镜");
        testEquipment.setModel("XSP-100");
        testEquipment.setManufacturer("某厂商");
        testEquipment.setPurchaseDate(LocalDate.of(2023, 1, 1));
        testEquipment.setPrice(new BigDecimal("5000.00"));
        testEquipment.setStatus("NORMAL");
        testEquipment.setLifeSpan(5);
        testEquipment.setLab(testLab);
    }

    @Test
    void testAddEquipment_WithValidLab_ShouldCreateEquipmentWithGeneratedCode() {
        Lab lab = new Lab();
        lab.setId(1L);

        Equipment newEquipment = new Equipment();
        newEquipment.setName("新设备");
        newEquipment.setLab(lab);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Collections.emptyList());
        
        Equipment savedEquipment = new Equipment();
        savedEquipment.setId(2L);
        savedEquipment.setCode("LAB01-001");
        savedEquipment.setName("新设备");
        savedEquipment.setStatus("NORMAL");
        savedEquipment.setLab(testLab);

        when(equipmentRepository.save(any(Equipment.class))).thenReturn(savedEquipment);

        Equipment result = equipmentService.addEquipment(newEquipment);

        assertNotNull(result);
        assertEquals("LAB01-001", result.getCode());
        assertEquals("NORMAL", result.getStatus());
        assertEquals(testLab, result.getLab());

        verify(labRepository).findById(1L);
        verify(equipmentRepository).findByLab_Id(1L);
        verify(equipmentRepository).save(any(Equipment.class));
    }

    @Test
    void testAddEquipment_WithExistingEquipments_ShouldGenerateNextCode() {
        Lab lab = new Lab();
        lab.setId(1L);

        Equipment newEquipment = new Equipment();
        newEquipment.setName("新设备");
        newEquipment.setLab(lab);

        when(labRepository.findById(1L)).thenReturn(Optional.of(testLab));
        
        Equipment existing1 = new Equipment();
        Equipment existing2 = new Equipment();
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(existing1, existing2));
        
        Equipment savedEquipment = new Equipment();
        savedEquipment.setId(3L);
        savedEquipment.setCode("LAB01-003");
        savedEquipment.setName("新设备");
        savedEquipment.setStatus("NORMAL");
        savedEquipment.setLab(testLab);

        when(equipmentRepository.save(any(Equipment.class))).thenReturn(savedEquipment);

        Equipment result = equipmentService.addEquipment(newEquipment);

        assertNotNull(result);
        assertEquals("LAB01-003", result.getCode());

        verify(equipmentRepository).findByLab_Id(1L);
    }

    @Test
    void testAddEquipment_WithNonExistentLab_ShouldThrowException() {
        Lab lab = new Lab();
        lab.setId(999L);

        Equipment newEquipment = new Equipment();
        newEquipment.setName("新设备");
        newEquipment.setLab(lab);

        when(labRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            equipmentService.addEquipment(newEquipment);
        });

        verify(labRepository).findById(999L);
        verify(equipmentRepository, never()).save(any(Equipment.class));
    }

    @Test
    void testUpdateEquipment_ShouldSaveEquipment() {
        Equipment updatedEquipment = new Equipment();
        updatedEquipment.setId(1L);
        updatedEquipment.setName("更新后的设备");
        updatedEquipment.setStatus("BORROWED");

        when(equipmentRepository.save(updatedEquipment)).thenReturn(updatedEquipment);

        Equipment result = equipmentService.updateEquipment(updatedEquipment);

        assertNotNull(result);
        assertEquals("更新后的设备", result.getName());
        assertEquals("BORROWED", result.getStatus());
        verify(equipmentRepository).save(updatedEquipment);
    }

    @Test
    void testDeleteEquipment_ShouldCallRepositoryDelete() {
        Long equipmentId = 1L;

        doNothing().when(equipmentRepository).deleteById(equipmentId);

        equipmentService.deleteEquipment(equipmentId);

        verify(equipmentRepository).deleteById(equipmentId);
    }

    @Test
    void testFindAll_ShouldReturnAllEquipments() {
        Equipment eq1 = new Equipment();
        eq1.setId(1L);
        eq1.setName("设备1");

        Equipment eq2 = new Equipment();
        eq2.setId(2L);
        eq2.setName("设备2");

        List<Equipment> equipments = Arrays.asList(eq1, eq2);

        when(equipmentRepository.findAll()).thenReturn(equipments);

        List<Equipment> result = equipmentService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("设备1", result.get(0).getName());
        assertEquals("设备2", result.get(1).getName());
        verify(equipmentRepository).findAll();
    }

    @Test
    void testFindById_WithExistingId_ShouldReturnEquipment() {
        Long equipmentId = 1L;

        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.of(testEquipment));

        Equipment result = equipmentService.findById(equipmentId);

        assertNotNull(result);
        assertEquals(testEquipment.getId(), result.getId());
        assertEquals(testEquipment.getName(), result.getName());
        verify(equipmentRepository).findById(equipmentId);
    }

    @Test
    void testFindById_WithNonExistentId_ShouldReturnNull() {
        Long nonExistentId = 999L;

        when(equipmentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        Equipment result = equipmentService.findById(nonExistentId);

        assertNull(result);
        verify(equipmentRepository).findById(nonExistentId);
    }

    @Test
    void testFindExpiringIn30Days_ShouldReturnExpiringEquipments() {
        LocalDate today = LocalDate.now();
        
        Equipment expiringEq = new Equipment();
        expiringEq.setId(1L);
        expiringEq.setName("即将过期设备");
        expiringEq.setStatus("NORMAL");
        expiringEq.setPurchaseDate(today.minusYears(4).minusMonths(11));
        expiringEq.setLifeSpan(5);

        Equipment notExpiringEq = new Equipment();
        notExpiringEq.setId(2L);
        notExpiringEq.setName("不会过期设备");
        notExpiringEq.setStatus("NORMAL");
        notExpiringEq.setPurchaseDate(today.minusYears(1));
        notExpiringEq.setLifeSpan(5);

        Equipment scrappedEq = new Equipment();
        scrappedEq.setId(3L);
        scrappedEq.setName("已报废设备");
        scrappedEq.setStatus("SCRAPPED");
        scrappedEq.setPurchaseDate(today.minusYears(6));
        scrappedEq.setLifeSpan(5);

        Equipment noDateEq = new Equipment();
        noDateEq.setId(4L);
        noDateEq.setName("无日期设备");
        noDateEq.setStatus("NORMAL");

        List<Equipment> activeEquipments = Arrays.asList(expiringEq, notExpiringEq, scrappedEq, noDateEq);
        when(equipmentRepository.findByStatusNot("SCRAPPED")).thenReturn(Arrays.asList(expiringEq, notExpiringEq, noDateEq));

        List<Equipment> result = equipmentService.findExpiringIn30Days();

        assertNotNull(result);
        verify(equipmentRepository).findByStatusNot("SCRAPPED");
    }

    @Test
    void testFindExpiring_DefaultQuery_ShouldReturnExpiringAndOverdue() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        Equipment expiringEq = new Equipment();
        expiringEq.setId(1L);
        expiringEq.setCode("LAB01-001");
        expiringEq.setName("即将过期设备");
        expiringEq.setStatus("NORMAL");
        expiringEq.setPurchaseDate(today.minusYears(4).minusMonths(11));
        expiringEq.setLifeSpan(5);
        expiringEq.setLab(lab1);

        Equipment overdueEq = new Equipment();
        overdueEq.setId(2L);
        overdueEq.setCode("LAB01-002");
        overdueEq.setName("已过期设备");
        overdueEq.setStatus("NORMAL");
        overdueEq.setPurchaseDate(today.minusYears(6));
        overdueEq.setLifeSpan(5);
        overdueEq.setLab(lab1);

        Equipment notExpiringEq = new Equipment();
        notExpiringEq.setId(3L);
        notExpiringEq.setCode("LAB01-003");
        notExpiringEq.setName("不会过期设备");
        notExpiringEq.setStatus("NORMAL");
        notExpiringEq.setPurchaseDate(today.minusYears(1));
        notExpiringEq.setLifeSpan(5);
        notExpiringEq.setLab(lab1);

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Arrays.asList(expiringEq, overdueEq, notExpiringEq));

        ExpiringQuery query = new ExpiringQuery();
        query.setExpiredOnly(false);
        query.setIncludeIncomplete(true);
        query.setSortBy("remainingDays");
        query.setSortOrder("asc");

        Page<ExpiringEquipmentDTO> result = equipmentService.findExpiring(query);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(dto -> dto.isDataComplete()));
        assertTrue(result.getContent().stream().allMatch(dto -> dto.getRemainingDays() <= 30));
    }

    @Test
    void testFindExpiring_ExpiredOnly_ShouldReturnOnlyOverdue() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        Equipment overdueEq = new Equipment();
        overdueEq.setId(1L);
        overdueEq.setCode("LAB01-001");
        overdueEq.setName("已过期设备");
        overdueEq.setStatus("NORMAL");
        overdueEq.setPurchaseDate(today.minusYears(6));
        overdueEq.setLifeSpan(5);
        overdueEq.setLab(lab1);

        Equipment expiringEq = new Equipment();
        expiringEq.setId(2L);
        expiringEq.setCode("LAB01-002");
        expiringEq.setName("即将过期设备");
        expiringEq.setStatus("NORMAL");
        expiringEq.setPurchaseDate(today.minusYears(4).minusMonths(11));
        expiringEq.setLifeSpan(5);
        expiringEq.setLab(lab1);

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Arrays.asList(overdueEq, expiringEq));

        ExpiringQuery query = new ExpiringQuery();
        query.setExpiredOnly(true);
        query.setIncludeIncomplete(false);
        query.setSortBy("remainingDays");
        query.setSortOrder("asc");

        Page<ExpiringEquipmentDTO> result = equipmentService.findExpiring(query);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getRemainingDays() < 0);
    }

    @Test
    void testFindExpiring_IncompleteData_ShouldMarkAndFilter() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        Equipment noPurchaseDate = new Equipment();
        noPurchaseDate.setId(1L);
        noPurchaseDate.setCode("LAB01-001");
        noPurchaseDate.setName("无采购日期设备");
        noPurchaseDate.setStatus("NORMAL");
        noPurchaseDate.setLifeSpan(5);
        noPurchaseDate.setLab(lab1);

        Equipment noLifeSpan = new Equipment();
        noLifeSpan.setId(2L);
        noLifeSpan.setCode("LAB01-002");
        noLifeSpan.setName("无使用年限设备");
        noLifeSpan.setStatus("NORMAL");
        noLifeSpan.setPurchaseDate(today.minusYears(3));
        noLifeSpan.setLab(lab1);

        Equipment bothMissing = new Equipment();
        bothMissing.setId(3L);
        bothMissing.setCode("LAB01-003");
        bothMissing.setName("都缺设备");
        bothMissing.setStatus("NORMAL");
        bothMissing.setLab(lab1);

        Equipment complete = new Equipment();
        complete.setId(4L);
        complete.setCode("LAB01-004");
        complete.setName("完整设备");
        complete.setStatus("NORMAL");
        complete.setPurchaseDate(today.minusYears(4).minusMonths(11));
        complete.setLifeSpan(5);
        complete.setLab(lab1);

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Arrays.asList(noPurchaseDate, noLifeSpan, bothMissing, complete));

        ExpiringQuery query = new ExpiringQuery();
        query.setExpiredOnly(false);
        query.setIncludeIncomplete(true);
        query.setSortBy("remainingDays");
        query.setSortOrder("asc");

        Page<ExpiringEquipmentDTO> result = equipmentService.findExpiring(query);

        assertEquals(4, result.getContent().size());
        ExpiringEquipmentDTO dto1 = result.getContent().stream().filter(d -> d.getId().equals(1L)).findFirst().orElse(null);
        assertNotNull(dto1);
        assertFalse(dto1.isDataComplete());
        assertTrue(dto1.getDataIncompleteReason().contains("采购日期"));

        ExpiringEquipmentDTO dto2 = result.getContent().stream().filter(d -> d.getId().equals(2L)).findFirst().orElse(null);
        assertNotNull(dto2);
        assertFalse(dto2.isDataComplete());
        assertTrue(dto2.getDataIncompleteReason().contains("使用年限"));

        ExpiringEquipmentDTO dto3 = result.getContent().stream().filter(d -> d.getId().equals(3L)).findFirst().orElse(null);
        assertNotNull(dto3);
        assertFalse(dto3.isDataComplete());
        assertTrue(dto3.getDataIncompleteReason().contains("采购日期"));
        assertTrue(dto3.getDataIncompleteReason().contains("使用年限"));

        ExpiringEquipmentDTO dto4 = result.getContent().stream().filter(d -> d.getId().equals(4L)).findFirst().orElse(null);
        assertNotNull(dto4);
        assertTrue(dto4.isDataComplete());
    }

    @Test
    void testFindExpiring_ExcludeIncomplete_ShouldFilterOutIncompleteData() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        Equipment noPurchaseDate = new Equipment();
        noPurchaseDate.setId(1L);
        noPurchaseDate.setCode("LAB01-001");
        noPurchaseDate.setName("无采购日期设备");
        noPurchaseDate.setStatus("NORMAL");
        noPurchaseDate.setLifeSpan(5);
        noPurchaseDate.setLab(lab1);

        Equipment complete = new Equipment();
        complete.setId(2L);
        complete.setCode("LAB01-002");
        complete.setName("完整设备");
        complete.setStatus("NORMAL");
        complete.setPurchaseDate(today.minusYears(4).minusMonths(11));
        complete.setLifeSpan(5);
        complete.setLab(lab1);

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Arrays.asList(noPurchaseDate, complete));

        ExpiringQuery query = new ExpiringQuery();
        query.setExpiredOnly(false);
        query.setIncludeIncomplete(false);
        query.setSortBy("remainingDays");
        query.setSortOrder("asc");

        Page<ExpiringEquipmentDTO> result = equipmentService.findExpiring(query);

        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).isDataComplete());
    }

    @Test
    void testFindExpiring_SortByExpiryDate_ShouldSortCorrectly() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        Equipment eq1 = new Equipment();
        eq1.setId(1L);
        eq1.setCode("LAB01-001");
        eq1.setName("设备A");
        eq1.setStatus("NORMAL");
        eq1.setPurchaseDate(today.minusYears(5));
        eq1.setLifeSpan(5);
        eq1.setLab(lab1);

        Equipment eq2 = new Equipment();
        eq2.setId(2L);
        eq2.setCode("LAB01-002");
        eq2.setName("设备B");
        eq2.setStatus("NORMAL");
        eq2.setPurchaseDate(today.minusYears(3));
        eq2.setLifeSpan(3);
        eq2.setLab(lab1);

        Equipment eq3 = new Equipment();
        eq3.setId(3L);
        eq3.setCode("LAB01-003");
        eq3.setName("设备C");
        eq3.setStatus("NORMAL");
        eq3.setPurchaseDate(today.minusYears(4).minusMonths(11));
        eq3.setLifeSpan(5);
        eq3.setLab(lab1);

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Arrays.asList(eq1, eq2, eq3));

        ExpiringQuery query = new ExpiringQuery();
        query.setExpiredOnly(false);
        query.setIncludeIncomplete(true);
        query.setSortBy("expiryDate");
        query.setSortOrder("asc");

        Page<ExpiringEquipmentDTO> result = equipmentService.findExpiring(query);

        assertTrue(result.getContent().size() >= 2);
        for (int i = 0; i < result.getContent().size() - 1; i++) {
            LocalDate current = result.getContent().get(i).getExpiryDate();
            LocalDate next = result.getContent().get(i + 1).getExpiryDate();
            if (current != null && next != null) {
                assertTrue(current.isBefore(next) || current.isEqual(next));
            }
        }
    }

    @Test
    void testFindExpiring_Pagination_ShouldReturnCorrectPage() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        List<Equipment> equipments = new java.util.ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Equipment eq = new Equipment();
            eq.setId((long) i);
            eq.setCode(String.format("LAB01-%03d", i));
            eq.setName("设备" + i);
            eq.setStatus("NORMAL");
            eq.setPurchaseDate(today.minusYears(5).plusDays(i));
            eq.setLifeSpan(5);
            eq.setLab(lab1);
            equipments.add(eq);
        }

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(equipments);

        ExpiringQuery query = new ExpiringQuery();
        query.setExpiredOnly(false);
        query.setIncludeIncomplete(true);
        query.setSortBy("remainingDays");
        query.setSortOrder("asc");
        query.setPage(1);
        query.setSize(10);

        Page<ExpiringEquipmentDTO> result = equipmentService.findExpiring(query);

        assertEquals(10, result.getContent().size());
        assertEquals(15, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
    }

    @Test
    void testFindExpiring_ExpiredOnlyWithIncomplete_ShouldExcludeIncompletes() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        Equipment overdueEq = new Equipment();
        overdueEq.setId(1L);
        overdueEq.setCode("LAB01-001");
        overdueEq.setName("已过期设备");
        overdueEq.setStatus("NORMAL");
        overdueEq.setPurchaseDate(today.minusYears(6));
        overdueEq.setLifeSpan(5);
        overdueEq.setLab(lab1);

        Equipment noPurchaseDate = new Equipment();
        noPurchaseDate.setId(2L);
        noPurchaseDate.setCode("LAB01-002");
        noPurchaseDate.setName("无采购日期");
        noPurchaseDate.setStatus("NORMAL");
        noPurchaseDate.setLifeSpan(5);
        noPurchaseDate.setLab(lab1);

        Equipment noLifeSpan = new Equipment();
        noLifeSpan.setId(3L);
        noLifeSpan.setCode("LAB01-003");
        noLifeSpan.setName("无使用年限");
        noLifeSpan.setStatus("NORMAL");
        noLifeSpan.setPurchaseDate(today.minusYears(10));
        noLifeSpan.setLab(lab1);

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Arrays.asList(overdueEq, noPurchaseDate, noLifeSpan));

        ExpiringQuery query = new ExpiringQuery();
        query.setExpiredOnly(true);
        query.setIncludeIncomplete(true);
        query.setSortBy("remainingDays");
        query.setSortOrder("asc");

        Page<ExpiringEquipmentDTO> result = equipmentService.findExpiring(query);

        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).isDataComplete());
        assertTrue(result.getContent().get(0).getRemainingDays() < 0);
        assertEquals("LAB01-001", result.getContent().get(0).getCode());
    }

    @Test
    void testFindExpiring_OverdueDegreeVsRemainingDays_ShouldSortDifferently() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        Equipment eqOverdue30 = new Equipment();
        eqOverdue30.setId(1L);
        eqOverdue30.setCode("LAB01-001");
        eqOverdue30.setName("超期30天");
        eqOverdue30.setStatus("NORMAL");
        eqOverdue30.setPurchaseDate(today.minusYears(5).minusDays(30));
        eqOverdue30.setLifeSpan(5);
        eqOverdue30.setLab(lab1);

        Equipment eqOverdue10 = new Equipment();
        eqOverdue10.setId(2L);
        eqOverdue10.setCode("LAB01-002");
        eqOverdue10.setName("超期10天");
        eqOverdue10.setStatus("NORMAL");
        eqOverdue10.setPurchaseDate(today.minusYears(5).minusDays(10));
        eqOverdue10.setLifeSpan(5);
        eqOverdue10.setLab(lab1);

        Equipment eqSoon10 = new Equipment();
        eqSoon10.setId(3L);
        eqSoon10.setCode("LAB01-003");
        eqSoon10.setName("还有10天到期");
        eqSoon10.setStatus("NORMAL");
        eqSoon10.setPurchaseDate(today.minusYears(5).plusDays(10));
        eqSoon10.setLifeSpan(5);
        eqSoon10.setLab(lab1);

        Equipment eqSoon20 = new Equipment();
        eqSoon20.setId(4L);
        eqSoon20.setCode("LAB01-004");
        eqSoon20.setName("还有20天到期");
        eqSoon20.setStatus("NORMAL");
        eqSoon20.setPurchaseDate(today.minusYears(5).plusDays(20));
        eqSoon20.setLifeSpan(5);
        eqSoon20.setLab(lab1);

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Arrays.asList(eqOverdue30, eqOverdue10, eqSoon10, eqSoon20));

        ExpiringQuery queryByDegree = new ExpiringQuery();
        queryByDegree.setExpiredOnly(false);
        queryByDegree.setIncludeIncomplete(true);
        queryByDegree.setSortBy("overdueDegree");
        queryByDegree.setSortOrder("desc");

        Page<ExpiringEquipmentDTO> resultByDegree = equipmentService.findExpiring(queryByDegree);

        ExpiringQuery queryByRemaining = new ExpiringQuery();
        queryByRemaining.setExpiredOnly(false);
        queryByRemaining.setIncludeIncomplete(true);
        queryByRemaining.setSortBy("remainingDays");
        queryByRemaining.setSortOrder("asc");

        Page<ExpiringEquipmentDTO> resultByRemaining = equipmentService.findExpiring(queryByRemaining);

        assertEquals(4, resultByDegree.getContent().size());
        assertEquals(4, resultByRemaining.getContent().size());

        assertEquals("LAB01-001", resultByDegree.getContent().get(0).getCode());
        assertEquals("LAB01-002", resultByDegree.getContent().get(1).getCode());
        long degree0 = Math.max(0, -resultByDegree.getContent().get(0).getRemainingDays());
        long degree1 = Math.max(0, -resultByDegree.getContent().get(1).getRemainingDays());
        assertTrue(degree0 >= degree1);

        long remaining0 = resultByRemaining.getContent().get(0).getRemainingDays();
        long remaining1 = resultByRemaining.getContent().get(1).getRemainingDays();
        assertTrue(remaining0 <= remaining1);
    }

    @Test
    void testFindExpiring_DescSort_IncompletesStillLast() {
        LocalDate today = LocalDate.now();
        Lab lab1 = new Lab();
        lab1.setId(1L);
        lab1.setName("物理实验室");

        Equipment eqComplete1 = new Equipment();
        eqComplete1.setId(1L);
        eqComplete1.setCode("LAB01-001");
        eqComplete1.setName("设备A");
        eqComplete1.setStatus("NORMAL");
        eqComplete1.setPurchaseDate(today.minusYears(5).minusDays(30));
        eqComplete1.setLifeSpan(5);
        eqComplete1.setLab(lab1);

        Equipment eqComplete2 = new Equipment();
        eqComplete2.setId(2L);
        eqComplete2.setCode("LAB01-002");
        eqComplete2.setName("设备B");
        eqComplete2.setStatus("NORMAL");
        eqComplete2.setPurchaseDate(today.minusYears(4).minusMonths(11));
        eqComplete2.setLifeSpan(5);
        eqComplete2.setLab(lab1);

        Equipment eqIncomplete = new Equipment();
        eqIncomplete.setId(3L);
        eqIncomplete.setCode("LAB01-003");
        eqIncomplete.setName("信息不全");
        eqIncomplete.setStatus("NORMAL");
        eqIncomplete.setLab(lab1);

        when(equipmentRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(Arrays.asList(eqIncomplete, eqComplete1, eqComplete2));

        ExpiringQuery query = new ExpiringQuery();
        query.setExpiredOnly(false);
        query.setIncludeIncomplete(true);
        query.setSortBy("remainingDays");
        query.setSortOrder("desc");

        Page<ExpiringEquipmentDTO> result = equipmentService.findExpiring(query);

        assertEquals(3, result.getContent().size());
        assertTrue(result.getContent().get(0).isDataComplete());
        assertTrue(result.getContent().get(1).isDataComplete());
        assertFalse(result.getContent().get(2).isDataComplete());
        assertEquals("LAB01-003", result.getContent().get(2).getCode());
    }
}
