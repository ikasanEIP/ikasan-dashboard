package org.ikasan.esb.service.replay;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.replay.ReplayAuditEvent;
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
 * Unit tests for ReplayAuditServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReplayAuditServiceImplTest {

    @Mock
    private EsbEntityDao<ReplayAuditEvent> mockDao;

    @Mock
    private ReplayAuditEvent mockReplayAuditEvent;

    private ReplayAuditServiceImpl service;

    @Before
    public void setUp() {
        service = new ReplayAuditServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new ReplayAuditServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        ReplayAuditServiceImpl testService = new ReplayAuditServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testInsert() {
        // Given
        List<ReplayAuditEvent> events = Arrays.asList(
            mockReplayAuditEvent,
            mock(ReplayAuditEvent.class),
            mock(ReplayAuditEvent.class)
        );

        // When
        service.insert(events);

        // Then
        verify(mockDao).save(events);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertEmptyList() {
        // Given
        List<ReplayAuditEvent> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSingleItem() {
        // Given
        List<ReplayAuditEvent> singleItem = Arrays.asList(mockReplayAuditEvent);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<ReplayAuditEvent> list1 = Arrays.asList(mockReplayAuditEvent);
        List<ReplayAuditEvent> list2 = Arrays.asList(
            mock(ReplayAuditEvent.class),
            mock(ReplayAuditEvent.class)
        );
        List<ReplayAuditEvent> list3 = Arrays.asList(
            mock(ReplayAuditEvent.class),
            mock(ReplayAuditEvent.class),
            mock(ReplayAuditEvent.class)
        );

        // When
        service.insert(list1);
        service.insert(list2);
        service.insert(list3);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(list2);
        verify(mockDao).save(list3);
        verify(mockDao, times(3)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertWithLargeList() {
        // Given
        List<ReplayAuditEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ReplayAuditEvent.class));
        }

        // When
        service.insert(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertWithTwoItems() {
        // Given
        List<ReplayAuditEvent> twoItems = Arrays.asList(
            mock(ReplayAuditEvent.class),
            mock(ReplayAuditEvent.class)
        );

        // When
        service.insert(twoItems);

        // Then
        verify(mockDao).save(twoItems);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertWithTenItems() {
        // Given
        List<ReplayAuditEvent> tenItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenItems.add(mock(ReplayAuditEvent.class));
        }

        // When
        service.insert(tenItems);

        // Then
        verify(mockDao).save(tenItems);
        verify(mockDao, times(1)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertWithFiftyItems() {
        // Given
        List<ReplayAuditEvent> fiftyItems = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            fiftyItems.add(mock(ReplayAuditEvent.class));
        }

        // When
        service.insert(fiftyItems);

        // Then
        verify(mockDao).save(fiftyItems);
        verify(mockDao, times(1)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSameListTwice() {
        // Given
        List<ReplayAuditEvent> events = Arrays.asList(
            mockReplayAuditEvent,
            mock(ReplayAuditEvent.class)
        );

        // When
        service.insert(events);
        service.insert(events);

        // Then
        verify(mockDao, times(2)).save(events);
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertDifferentLists() {
        // Given
        List<ReplayAuditEvent> list1 = Arrays.asList(mock(ReplayAuditEvent.class));
        List<ReplayAuditEvent> list2 = Arrays.asList(
            mock(ReplayAuditEvent.class),
            mock(ReplayAuditEvent.class)
        );
        List<ReplayAuditEvent> list3 = Arrays.asList(
            mock(ReplayAuditEvent.class),
            mock(ReplayAuditEvent.class),
            mock(ReplayAuditEvent.class)
        );

        // When
        service.insert(list1);
        service.insert(list2);
        service.insert(list3);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(list2);
        verify(mockDao).save(list3);
        verify(mockDao, times(3)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertWithFiveItems() {
        // Given
        List<ReplayAuditEvent> fiveItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fiveItems.add(mock(ReplayAuditEvent.class));
        }

        // When
        service.insert(fiveItems);

        // Then
        verify(mockDao).save(fiveItems);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testMultipleInsertsWithVariousSizes() {
        // Given
        List<ReplayAuditEvent> list1 = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list1.add(mock(ReplayAuditEvent.class));
        }

        List<ReplayAuditEvent> list2 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list2.add(mock(ReplayAuditEvent.class));
        }

        List<ReplayAuditEvent> list3 = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            list3.add(mock(ReplayAuditEvent.class));
        }

        // When
        service.insert(list1);
        service.insert(list2);
        service.insert(list3);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(list2);
        verify(mockDao).save(list3);
        verify(mockDao, times(3)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }
}
