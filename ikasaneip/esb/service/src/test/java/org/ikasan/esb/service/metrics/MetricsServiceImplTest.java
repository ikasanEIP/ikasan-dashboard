package org.ikasan.esb.service.metrics;

import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.metrics.MetricsDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetricsServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class MetricsServiceImplTest {

    @Mock
    private MetricsDao mockDao;

    @Mock
    private FlowInvocationMetric mockMetric;

    private MetricsServiceImpl service;

    @Before
    public void setUp() {
        service = new MetricsServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new MetricsServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        MetricsServiceImpl testService = new MetricsServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testInsert() {
        // Given
        List<FlowInvocationMetric> metrics = Arrays.asList(
            mockMetric,
            mock(FlowInvocationMetric.class),
            mock(FlowInvocationMetric.class)
        );

        // When
        service.insert(metrics);

        // Then
        verify(mockDao).save(metrics);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertEmptyList() {
        // Given
        List<FlowInvocationMetric> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSingleItem() {
        // Given
        List<FlowInvocationMetric> singleItem = Arrays.asList(mockMetric);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<FlowInvocationMetric> list1 = Arrays.asList(mockMetric);
        List<FlowInvocationMetric> list2 = Arrays.asList(
            mock(FlowInvocationMetric.class),
            mock(FlowInvocationMetric.class)
        );

        // When
        service.insert(list1);
        service.insert(list2);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(list2);
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertWithLargeList() {
        // Given
        List<FlowInvocationMetric> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(FlowInvocationMetric.class));
        }

        // When
        service.insert(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithTimeRange() {
        // Given
        long startTime = 1000L;
        long endTime = 2000L;
        List<FlowInvocationMetric> expectedMetrics = Arrays.asList(
            mockMetric,
            mock(FlowInvocationMetric.class)
        );
        when(mockDao.getMetrics(startTime, endTime)).thenReturn(expectedMetrics);

        // When
        List<FlowInvocationMetric> results = service.getMetrics(startTime, endTime);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(mockDao).getMetrics(startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithTimeRangeReturnsEmptyList() {
        // Given
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.getMetrics(startTime, endTime)).thenReturn(new ArrayList<>());

        // When
        List<FlowInvocationMetric> results = service.getMetrics(startTime, endTime);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).getMetrics(startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithModuleName() {
        // Given
        String moduleName = "test-module";
        long startTime = 1000L;
        long endTime = 2000L;
        List<FlowInvocationMetric> expectedMetrics = Arrays.asList(mockMetric);
        when(mockDao.getMetrics(moduleName, startTime, endTime)).thenReturn(expectedMetrics);

        // When
        List<FlowInvocationMetric> results = service.getMetrics(moduleName, startTime, endTime);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mockDao).getMetrics(moduleName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithModuleNameReturnsEmptyList() {
        // Given
        String moduleName = "non-existent-module";
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.getMetrics(moduleName, startTime, endTime)).thenReturn(new ArrayList<>());

        // When
        List<FlowInvocationMetric> results = service.getMetrics(moduleName, startTime, endTime);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).getMetrics(moduleName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithModuleAndFlowName() {
        // Given
        String moduleName = "test-module";
        String flowName = "test-flow";
        long startTime = 1000L;
        long endTime = 2000L;
        List<FlowInvocationMetric> expectedMetrics = Arrays.asList(
            mockMetric,
            mock(FlowInvocationMetric.class),
            mock(FlowInvocationMetric.class)
        );
        when(mockDao.getMetrics(moduleName, flowName, startTime, endTime))
            .thenReturn(expectedMetrics);

        // When
        List<FlowInvocationMetric> results = service.getMetrics(moduleName, flowName, startTime, endTime);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        verify(mockDao).getMetrics(moduleName, flowName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithModuleAndFlowNameReturnsEmptyList() {
        // Given
        String moduleName = "test-module";
        String flowName = "non-existent-flow";
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.getMetrics(moduleName, flowName, startTime, endTime))
            .thenReturn(new ArrayList<>());

        // When
        List<FlowInvocationMetric> results = service.getMetrics(moduleName, flowName, startTime, endTime);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).getMetrics(moduleName, flowName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithTimeRangeAndPagination() {
        // Given
        long startTime = 1000L;
        long endTime = 2000L;
        int offset = 10;
        int limit = 20;
        List<FlowInvocationMetric> expectedMetrics = Arrays.asList(mockMetric);
        when(mockDao.getMetrics(startTime, endTime, offset, limit)).thenReturn(expectedMetrics);

        // When
        List<FlowInvocationMetric> results = service.getMetrics(startTime, endTime, offset, limit);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mockDao).getMetrics(startTime, endTime, offset, limit);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithTimeRangeAndPaginationReturnsEmptyList() {
        // Given
        long startTime = 1000L;
        long endTime = 2000L;
        int offset = 100;
        int limit = 20;
        when(mockDao.getMetrics(startTime, endTime, offset, limit)).thenReturn(new ArrayList<>());

        // When
        List<FlowInvocationMetric> results = service.getMetrics(startTime, endTime, offset, limit);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).getMetrics(startTime, endTime, offset, limit);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testCountWithTimeRange() {
        // Given
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.count(startTime, endTime)).thenReturn(42L);

        // When
        long count = service.count(startTime, endTime);

        // Then
        assertEquals(42L, count);
        verify(mockDao).count(startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testCountWithTimeRangeReturnsZero() {
        // Given
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.count(startTime, endTime)).thenReturn(0L);

        // When
        long count = service.count(startTime, endTime);

        // Then
        assertEquals(0L, count);
        verify(mockDao).count(startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithModuleNameAndPagination() {
        // Given
        String moduleName = "test-module";
        long startTime = 1000L;
        long endTime = 2000L;
        int offset = 5;
        int limit = 10;
        List<FlowInvocationMetric> expectedMetrics = Arrays.asList(
            mockMetric,
            mock(FlowInvocationMetric.class)
        );
        when(mockDao.getMetrics(moduleName, startTime, endTime, offset, limit))
            .thenReturn(expectedMetrics);

        // When
        List<FlowInvocationMetric> results = service.getMetrics(moduleName, startTime, endTime, offset, limit);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(mockDao).getMetrics(moduleName, startTime, endTime, offset, limit);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithModuleNameAndPaginationReturnsEmptyList() {
        // Given
        String moduleName = "test-module";
        long startTime = 1000L;
        long endTime = 2000L;
        int offset = 1000;
        int limit = 10;
        when(mockDao.getMetrics(moduleName, startTime, endTime, offset, limit))
            .thenReturn(new ArrayList<>());

        // When
        List<FlowInvocationMetric> results = service.getMetrics(moduleName, startTime, endTime, offset, limit);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).getMetrics(moduleName, startTime, endTime, offset, limit);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testCountWithModuleName() {
        // Given
        String moduleName = "test-module";
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.count(moduleName, startTime, endTime)).thenReturn(15L);

        // When
        long count = service.count(moduleName, startTime, endTime);

        // Then
        assertEquals(15L, count);
        verify(mockDao).count(moduleName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testCountWithModuleNameReturnsZero() {
        // Given
        String moduleName = "non-existent-module";
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.count(moduleName, startTime, endTime)).thenReturn(0L);

        // When
        long count = service.count(moduleName, startTime, endTime);

        // Then
        assertEquals(0L, count);
        verify(mockDao).count(moduleName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithModuleFlowNameAndPagination() {
        // Given
        String moduleName = "test-module";
        String flowName = "test-flow";
        long startTime = 1000L;
        long endTime = 2000L;
        int offset = 0;
        int limit = 50;
        List<FlowInvocationMetric> expectedMetrics = Arrays.asList(mockMetric);
        when(mockDao.getMetrics(moduleName, flowName, startTime, endTime, offset, limit))
            .thenReturn(expectedMetrics);

        // When
        List<FlowInvocationMetric> results = service.getMetrics(
            moduleName, flowName, startTime, endTime, offset, limit
        );

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mockDao).getMetrics(moduleName, flowName, startTime, endTime, offset, limit);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithModuleFlowNameAndPaginationReturnsEmptyList() {
        // Given
        String moduleName = "test-module";
        String flowName = "test-flow";
        long startTime = 1000L;
        long endTime = 2000L;
        int offset = 500;
        int limit = 50;
        when(mockDao.getMetrics(moduleName, flowName, startTime, endTime, offset, limit))
            .thenReturn(new ArrayList<>());

        // When
        List<FlowInvocationMetric> results = service.getMetrics(
            moduleName, flowName, startTime, endTime, offset, limit
        );

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).getMetrics(moduleName, flowName, startTime, endTime, offset, limit);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testCountWithModuleAndFlowName() {
        // Given
        String moduleName = "test-module";
        String flowName = "test-flow";
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.count(moduleName, flowName, startTime, endTime)).thenReturn(100L);

        // When
        long count = service.count(moduleName, flowName, startTime, endTime);

        // Then
        assertEquals(100L, count);
        verify(mockDao).count(moduleName, flowName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testCountWithModuleAndFlowNameReturnsZero() {
        // Given
        String moduleName = "test-module";
        String flowName = "non-existent-flow";
        long startTime = 1000L;
        long endTime = 2000L;
        when(mockDao.count(moduleName, flowName, startTime, endTime)).thenReturn(0L);

        // When
        long count = service.count(moduleName, flowName, startTime, endTime);

        // Then
        assertEquals(0L, count);
        verify(mockDao).count(moduleName, flowName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithDifferentTimeRanges() {
        // Given
        long startTime1 = 1000L;
        long endTime1 = 2000L;
        long startTime2 = 5000L;
        long endTime2 = 10000L;

        List<FlowInvocationMetric> metrics1 = Arrays.asList(mockMetric);
        List<FlowInvocationMetric> metrics2 = Arrays.asList(
            mock(FlowInvocationMetric.class),
            mock(FlowInvocationMetric.class)
        );

        when(mockDao.getMetrics(startTime1, endTime1)).thenReturn(metrics1);
        when(mockDao.getMetrics(startTime2, endTime2)).thenReturn(metrics2);

        // When
        List<FlowInvocationMetric> results1 = service.getMetrics(startTime1, endTime1);
        List<FlowInvocationMetric> results2 = service.getMetrics(startTime2, endTime2);

        // Then
        assertEquals(1, results1.size());
        assertEquals(2, results2.size());
        verify(mockDao).getMetrics(startTime1, endTime1);
        verify(mockDao).getMetrics(startTime2, endTime2);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testCountWithDifferentParameters() {
        // Given
        long startTime = 1000L;
        long endTime = 2000L;
        String moduleName = "test-module";
        String flowName = "test-flow";

        when(mockDao.count(startTime, endTime)).thenReturn(100L);
        when(mockDao.count(moduleName, startTime, endTime)).thenReturn(50L);
        when(mockDao.count(moduleName, flowName, startTime, endTime)).thenReturn(25L);

        // When
        long count1 = service.count(startTime, endTime);
        long count2 = service.count(moduleName, startTime, endTime);
        long count3 = service.count(moduleName, flowName, startTime, endTime);

        // Then
        assertEquals(100L, count1);
        assertEquals(50L, count2);
        assertEquals(25L, count3);
        verify(mockDao).count(startTime, endTime);
        verify(mockDao).count(moduleName, startTime, endTime);
        verify(mockDao).count(moduleName, flowName, startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testGetMetricsWithLargeResults() {
        // Given
        long startTime = 1000L;
        long endTime = 2000L;
        List<FlowInvocationMetric> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(FlowInvocationMetric.class));
        }
        when(mockDao.getMetrics(startTime, endTime)).thenReturn(largeList);

        // When
        List<FlowInvocationMetric> results = service.getMetrics(startTime, endTime);

        // Then
        assertNotNull(results);
        assertEquals(100, results.size());
        verify(mockDao).getMetrics(startTime, endTime);
        verifyNoMoreInteractions(mockDao);
    }
}
