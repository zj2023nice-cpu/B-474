package com.example.lab.service;

import com.example.lab.entity.Lab;
import com.example.lab.repository.LabRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabServiceTest {

    @Mock
    private LabRepository labRepository;

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
}
