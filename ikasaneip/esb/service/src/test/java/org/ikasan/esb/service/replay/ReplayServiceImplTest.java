package org.ikasan.esb.service.replay;

import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.replay.ReplayEvent;
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
 * Unit tests for ReplayServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ReplayServiceImplTest {

    @Mock
    private EntityDao<ReplayEvent> mockDao;

    @Mock
    private ReplayEvent mockReplayEvent;

    private ReplayServiceImpl service;

    @Before
    public void setUp() {
        service = new ReplayServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new ReplayServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        ReplayServiceImpl testService = new ReplayServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testSaveSingleEntity() {
        // When
        service.save(mockReplayEvent);

        // Then
        verify(mockDao).save(mockReplayEvent);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityMultipleTimes() {
        // Given
        ReplayEvent event2 = mock(ReplayEvent.class);
        ReplayEvent event3 = mock(ReplayEvent.class);

        // When
        service.save(mockReplayEvent);
        service.save(event2);
        service.save(event3);

        // Then
        verify(mockDao).save(mockReplayEvent);
        verify(mockDao).save(event2);
        verify(mockDao).save(event3);
        verify(mockDao, times(3)).save(any(ReplayEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveMultipleEntities() {
        // Given
        List<ReplayEvent> events = Arrays.asList(
            mockReplayEvent,
            mock(ReplayEvent.class),
            mock(ReplayEvent.class)
        );

        // When
        service.save(events);

        // Then
        verify(mockDao).save(events);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveEmptyList() {
        // Given
        List<ReplayEvent> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleItemList() {
        // Given
        List<ReplayEvent> singleItem = Arrays.asList(mockReplayEvent);

        // When
        service.save(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListMultipleTimes() {
        // Given
        List<ReplayEvent> list1 = Arrays.asList(mockReplayEvent);
        List<ReplayEvent> list2 = Arrays.asList(
            mock(ReplayEvent.class),
            mock(ReplayEvent.class)
        );
        List<ReplayEvent> list3 = Arrays.asList(
            mock(ReplayEvent.class),
            mock(ReplayEvent.class),
            mock(ReplayEvent.class)
        );

        // When
        service.save(list1);
        service.save(list2);
        service.save(list3);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(list2);
        verify(mockDao).save(list3);
        verify(mockDao, times(3)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsert() {
        // Given
        List<ReplayEvent> events = Arrays.asList(
            mockReplayEvent,
            mock(ReplayEvent.class)
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
        List<ReplayEvent> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSingleItem() {
        // Given
        List<ReplayEvent> singleItem = Arrays.asList(mockReplayEvent);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<ReplayEvent> list1 = Arrays.asList(mockReplayEvent);
        List<ReplayEvent> list2 = Arrays.asList(
            mock(ReplayEvent.class),
            mock(ReplayEvent.class)
        );
        List<ReplayEvent> list3 = Arrays.asList(
            mock(ReplayEvent.class),
            mock(ReplayEvent.class),
            mock(ReplayEvent.class)
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
        List<ReplayEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ReplayEvent.class));
        }

        // When
        service.insert(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveWithLargeList() {
        // Given
        List<ReplayEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeList.add(mock(ReplayEvent.class));
        }

        // When
        service.save(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertCallsSaveMethod() {
        // Given
        List<ReplayEvent> events = Arrays.asList(
            mockReplayEvent,
            mock(ReplayEvent.class)
        );

        // When
        service.insert(events);

        // Then
        // Verify that insert actually delegates to save
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(ReplayEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testMixedSaveAndInsertCalls() {
        // Given
        List<ReplayEvent> list1 = Arrays.asList(mockReplayEvent);
        ReplayEvent singleEvent = mock(ReplayEvent.class);
        List<ReplayEvent> list2 = Arrays.asList(
            mock(ReplayEvent.class),
            mock(ReplayEvent.class)
        );

        // When
        service.save(list1);
        service.save(singleEvent);
        service.insert(list2);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(singleEvent);
        verify(mockDao).save(list2);
        verify(mockDao, times(1)).save(any(ReplayEvent.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityDoesNotInteractWithListSave() {
        // When
        service.save(mockReplayEvent);

        // Then
        verify(mockDao).save(mockReplayEvent);
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListDoesNotInteractWithSingleSave() {
        // Given
        List<ReplayEvent> events = Arrays.asList(mockReplayEvent);

        // When
        service.save(events);

        // Then
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(ReplayEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertDoesNotInteractWithSingleSave() {
        // Given
        List<ReplayEvent> events = Arrays.asList(mockReplayEvent);

        // When
        service.insert(events);

        // Then
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(ReplayEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEventWithDifferentInstances() {
        // Given
        ReplayEvent event1 = mock(ReplayEvent.class);
        ReplayEvent event2 = mock(ReplayEvent.class);
        ReplayEvent event3 = mock(ReplayEvent.class);

        // When
        service.save(event1);
        service.save(event2);
        service.save(event3);

        // Then
        verify(mockDao).save(event1);
        verify(mockDao).save(event2);
        verify(mockDao).save(event3);
        verify(mockDao, times(3)).save(any(ReplayEvent.class));
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListWithTenItems() {
        // Given
        List<ReplayEvent> tenItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenItems.add(mock(ReplayEvent.class));
        }

        // When
        service.save(tenItems);

        // Then
        verify(mockDao).save(tenItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(ReplayEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertListWithTwentyItems() {
        // Given
        List<ReplayEvent> twentyItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            twentyItems.add(mock(ReplayEvent.class));
        }

        // When
        service.insert(twentyItems);

        // Then
        verify(mockDao).save(twentyItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(ReplayEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEventTwice() {
        // When
        service.save(mockReplayEvent);
        service.save(mockReplayEvent);

        // Then
        verify(mockDao, times(2)).save(mockReplayEvent);
        verify(mockDao, times(2)).save(any(ReplayEvent.class));
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListTwice() {
        // Given
        List<ReplayEvent> events = Arrays.asList(
            mockReplayEvent,
            mock(ReplayEvent.class)
        );

        // When
        service.save(events);
        service.save(events);

        // Then
        verify(mockDao, times(2)).save(events);
        verify(mockDao, times(2)).save(anyList());
        verify(mockDao, never()).save(any(ReplayEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSameListTwice() {
        // Given
        List<ReplayEvent> events = Arrays.asList(mockReplayEvent);

        // When
        service.insert(events);
        service.insert(events);

        // Then
        verify(mockDao, times(2)).save(events);
        verify(mockDao, times(2)).save(anyList());
        verify(mockDao, never()).save(any(ReplayEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testAlternatingBetweenSingleAndList() {
        // Given
        ReplayEvent single1 = mock(ReplayEvent.class);
        List<ReplayEvent> list1 = Arrays.asList(mock(ReplayEvent.class));
        ReplayEvent single2 = mock(ReplayEvent.class);
        List<ReplayEvent> list2 = Arrays.asList(
            mock(ReplayEvent.class),
            mock(ReplayEvent.class)
        );

        // When
        service.save(single1);
        service.save(list1);
        service.save(single2);
        service.save(list2);

        // Then
        verify(mockDao).save(single1);
        verify(mockDao).save(list1);
        verify(mockDao).save(single2);
        verify(mockDao).save(list2);
        verify(mockDao, times(2)).save(any(ReplayEvent.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }
}
