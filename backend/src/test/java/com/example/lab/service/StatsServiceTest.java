package com.example.lab.service;

import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.RepairRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private BorrowRepository borrowRepository;

    @Mock
    private RepairRepository repairRepository;

    @InjectMocks
    private StatsService statsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGetDashboardStats_ShouldReturnAllStats() {
        long equipmentCount = 100L;
        long borrowCount = 15L;
        long overdueCount = 3L;
        long repairCount = 5L;

        when(equipmentRepository.count()).thenReturn(equipmentCount);
        when(borrowRepository.countByStatus("APPROVED")).thenReturn(borrowCount);
        when(borrowRepository.countOverdue(eq("APPROVED"), any(LocalDateTime.class))).thenReturn(overdueCount);
        when(repairRepository.countByStatusNot("FINISHED")).thenReturn(repairCount);

        Map<String, Long> stats = statsService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(4, stats.size());
        assertEquals(equipmentCount, stats.get("equipmentCount"));
        assertEquals(borrowCount, stats.get("borrowCount"));
        assertEquals(overdueCount, stats.get("overdue"));
        assertEquals(repairCount, stats.get("repairCount"));

        verify(equipmentRepository).count();
        verify(borrowRepository).countByStatus("APPROVED");
        verify(borrowRepository).countOverdue(eq("APPROVED"), any(LocalDateTime.class));
        verify(repairRepository).countByStatusNot("FINISHED");
    }

    @Test
    void testGetDashboardStats_WithZeroCounts_ShouldReturnZeros() {
        when(equipmentRepository.count()).thenReturn(0L);
        when(borrowRepository.countByStatus("APPROVED")).thenReturn(0L);
        when(borrowRepository.countOverdue(eq("APPROVED"), any(LocalDateTime.class))).thenReturn(0L);
        when(repairRepository.countByStatusNot("FINISHED")).thenReturn(0L);

        Map<String, Long> stats = statsService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(0L, stats.get("equipmentCount"));
        assertEquals(0L, stats.get("borrowCount"));
        assertEquals(0L, stats.get("overdue"));
        assertEquals(0L, stats.get("repairCount"));
    }

    @Test
    void testGetDashboardStats_WithLargeNumbers_ShouldReturnCorrectCounts() {
        long largeEquipmentCount = 10000L;
        long largeBorrowCount = 5000L;
        long largeOverdueCount = 100L;
        long largeRepairCount = 200L;

        when(equipmentRepository.count()).thenReturn(largeEquipmentCount);
        when(borrowRepository.countByStatus("APPROVED")).thenReturn(largeBorrowCount);
        when(borrowRepository.countOverdue(eq("APPROVED"), any(LocalDateTime.class))).thenReturn(largeOverdueCount);
        when(repairRepository.countByStatusNot("FINISHED")).thenReturn(largeRepairCount);

        Map<String, Long> stats = statsService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(largeEquipmentCount, stats.get("equipmentCount"));
        assertEquals(largeBorrowCount, stats.get("borrowCount"));
        assertEquals(largeOverdueCount, stats.get("overdue"));
        assertEquals(largeRepairCount, stats.get("repairCount"));
    }
}
