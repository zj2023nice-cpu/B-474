package com.example.lab.util;

import com.example.lab.entity.Equipment;
import com.example.lab.entity.Lab;
import com.example.lab.repository.EquipmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentCodeGeneratorTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private EquipmentCodeGenerator codeGenerator;

    private Lab testLab;

    @BeforeEach
    void setUp() {
        testLab = new Lab();
        testLab.setId(1L);
    }

    private Equipment eq(Long id, String code) {
        Equipment e = new Equipment();
        e.setId(id);
        e.setCode(code);
        e.setLab(testLab);
        return e;
    }

    @Test
    void generateNextCode_NoEquipments_ShouldStartFrom001() {
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Collections.emptyList());
        String code = codeGenerator.generateNextCode(1L);
        assertEquals("LAB01-001", code);
    }

    @Test
    void generateNextCode_WithExistingSerials_ShouldPickMaxPlus1() {
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(
                eq(1L, "LAB01-001"),
                eq(2L, "LAB01-003"),
                eq(3L, "LAB01-002")
        ));
        String code = codeGenerator.generateNextCode(1L);
        assertEquals("LAB01-004", code);
    }

    @Test
    void generateNextCode_AfterDeletion_ShouldNotReuseDeleted() {
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(
                eq(2L, "LAB01-005")
        ));
        String code = codeGenerator.generateNextCode(1L);
        assertEquals("LAB01-006", code,
                "删除了 001~004 后，应从现有最大 005 继续向后，而不是回填 001");
    }

    @Test
    void generateNextCode_SerialOver999_ShouldExpandWidth() {
        Equipment e = eq(1L, "LAB01-999");
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Collections.singletonList(e));
        String code = codeGenerator.generateNextCode(1L);
        assertEquals("LAB01-1000", code);
    }

    @Test
    void generateNextCode_LabIdOver99_ShouldExpandWidth() {
        Lab bigLab = new Lab();
        bigLab.setId(123L);
        Equipment e = new Equipment();
        e.setId(1L);
        e.setCode("LAB123-042");
        e.setLab(bigLab);
        when(equipmentRepository.findByLab_Id(123L)).thenReturn(Collections.singletonList(e));

        String code = codeGenerator.generateNextCode(123L);
        assertEquals("LAB123-043", code);
    }

    @Test
    void findMaxSerial_WithMalformedCodes_ShouldSkipGracefully() {
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(
                eq(1L, "LAB01-005"),
                eq(2L, null),
                eq(3L, ""),
                eq(4L, "CUSTOM-001"),
                eq(5L, "LAB01-abc"),
                eq(6L, "LAB-01-007"),
                eq(7L, "LAB01007")
        ));
        long max = codeGenerator.findMaxSerial(1L);
        assertEquals(5L, max, "只有第一个 LAB01-005 是合法的，其余异常编号都应被跳过");
    }

    @Test
    void findMaxSerial_WithCrossLabCodes_ShouldIgnoreOtherLabs() {
        Equipment wrongPrefix = eq(2L, "LAB99-999");
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(
                eq(1L, "LAB01-003"),
                wrongPrefix
        ));
        long max = codeGenerator.findMaxSerial(1L);
        assertEquals(3L, max,
                "LAB99-999 的前缀不属于 labId=1，即便编号更大也必须忽略");
    }

    @Test
    void findMaxSerial_AllMalformed_ShouldReturnZero() {
        when(equipmentRepository.findByLab_Id(1L)).thenReturn(Arrays.asList(
                eq(1L, "GARBAGE"),
                eq(2L, "")
        ));
        assertEquals(0L, codeGenerator.findMaxSerial(1L));
    }

    @Test
    void formatCode_DynamicPadding() {
        assertEquals("LAB01-001", codeGenerator.formatCode(1L, 1));
        assertEquals("LAB09-099", codeGenerator.formatCode(9L, 99));
        assertEquals("LAB10-100", codeGenerator.formatCode(10L, 100));
        assertEquals("LAB100-0099", codeGenerator.formatCode(100L, 99));
        assertEquals("LAB01-10000", codeGenerator.formatCode(1L, 10000));
    }

    @Test
    void generateNextCode_InvalidLabId_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> codeGenerator.generateNextCode(null));
        assertThrows(IllegalArgumentException.class, () -> codeGenerator.generateNextCode(0L));
        assertThrows(IllegalArgumentException.class, () -> codeGenerator.generateNextCode(-1L));
    }

    @Test
    void parseSerial_ValidCases() {
        assertEquals(7L, codeGenerator.parseSerial("LAB01-007", 1L));
        assertEquals(1234L, codeGenerator.parseSerial("LAB01-1234", 1L));
        assertNull(codeGenerator.parseSerial("LAB99-001", 1L), "前缀不匹配");
        assertNull(codeGenerator.parseSerial("BAD", 1L), "格式不匹配");
        assertNull(codeGenerator.parseSerial(null, 1L));
        assertEquals(7L, codeGenerator.parseSerial("LAB01-007", null), "不校验前缀时任意 labId 都行");
    }
}
