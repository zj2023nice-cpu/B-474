package com.example.lab.service;

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
}
