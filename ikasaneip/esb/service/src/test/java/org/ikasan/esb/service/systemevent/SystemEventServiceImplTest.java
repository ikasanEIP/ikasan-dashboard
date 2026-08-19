package org.ikasan.esb.service.systemevent;

import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.systemevent.SystemEvent;
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
 * Unit tests for SystemEventServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class SystemEventServiceImplTest {

    @Mock
    private EntityDao<SystemEvent> mockDao;

    @Mock
    private SystemEvent mockSystemEvent;

    private SystemEventServiceImpl service;

    @Before
    public void setUp() {
        service = new SystemEventServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new SystemEventServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        SystemEventServiceImpl testService = new SystemEventServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testSaveSingleEntity() {
        // When
        service.save(mockSystemEvent);

        // Then
        verify(mockDao).save(mockSystemEvent);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityMultipleTimes() {
        // Given
        SystemEvent event2 = mock(SystemEvent.class);
        SystemEvent event3 = mock(SystemEvent.class);

        // When
        service.save(mockSystemEvent);
        service.save(event2);
        service.save(event3);

        // Then
        verify(mockDao).save(mockSystemEvent);
        verify(mockDao).save(event2);
        verify(mockDao).save(event3);
        verify(mockDao, times(3)).save(any(SystemEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveMultipleEntities() {
        // Given
        List<SystemEvent> events = Arrays.asList(
            mockSystemEvent,
            mock(SystemEvent.class),
            mock(SystemEvent.class)
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
        List<SystemEvent> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleItemList() {
        // Given
        List<SystemEvent> singleItem = Arrays.asList(mockSystemEvent);

        // When
        service.save(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListMultipleTimes() {
        // Given
        List<SystemEvent> list1 = Arrays.asList(mockSystemEvent);
        List<SystemEvent> list2 = Arrays.asList(
            mock(SystemEvent.class),
            mock(SystemEvent.class)
        );
        List<SystemEvent> list3 = Arrays.asList(
            mock(SystemEvent.class),
            mock(SystemEvent.class),
            mock(SystemEvent.class)
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
        List<SystemEvent> events = Arrays.asList(
            mockSystemEvent,
            mock(SystemEvent.class)
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
        List<SystemEvent> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSingleItem() {
        // Given
        List<SystemEvent> singleItem = Arrays.asList(mockSystemEvent);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<SystemEvent> list1 = Arrays.asList(mockSystemEvent);
        List<SystemEvent> list2 = Arrays.asList(
            mock(SystemEvent.class),
            mock(SystemEvent.class)
        );
        List<SystemEvent> list3 = Arrays.asList(
            mock(SystemEvent.class),
            mock(SystemEvent.class),
            mock(SystemEvent.class)
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
        List<SystemEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(SystemEvent.class));
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
        List<SystemEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeList.add(mock(SystemEvent.class));
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
        List<SystemEvent> events = Arrays.asList(
            mockSystemEvent,
            mock(SystemEvent.class)
        );

        // When
        service.insert(events);

        // Then
        // Verify that insert actually delegates to save
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(SystemEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testMixedSaveAndInsertCalls() {
        // Given
        List<SystemEvent> list1 = Arrays.asList(mockSystemEvent);
        SystemEvent singleEvent = mock(SystemEvent.class);
        List<SystemEvent> list2 = Arrays.asList(
            mock(SystemEvent.class),
            mock(SystemEvent.class)
        );

        // When
        service.save(list1);
        service.save(singleEvent);
        service.insert(list2);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(singleEvent);
        verify(mockDao).save(list2);
        verify(mockDao, times(1)).save(any(SystemEvent.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityDoesNotInteractWithListSave() {
        // When
        service.save(mockSystemEvent);

        // Then
        verify(mockDao).save(mockSystemEvent);
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListDoesNotInteractWithSingleSave() {
        // Given
        List<SystemEvent> events = Arrays.asList(mockSystemEvent);

        // When
        service.save(events);

        // Then
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(SystemEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertDoesNotInteractWithSingleSave() {
        // Given
        List<SystemEvent> events = Arrays.asList(mockSystemEvent);

        // When
        service.insert(events);

        // Then
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(SystemEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEventWithDifferentInstances() {
        // Given
        SystemEvent event1 = mock(SystemEvent.class);
        SystemEvent event2 = mock(SystemEvent.class);
        SystemEvent event3 = mock(SystemEvent.class);

        // When
        service.save(event1);
        service.save(event2);
        service.save(event3);

        // Then
        verify(mockDao).save(event1);
        verify(mockDao).save(event2);
        verify(mockDao).save(event3);
        verify(mockDao, times(3)).save(any(SystemEvent.class));
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListWithTenItems() {
        // Given
        List<SystemEvent> tenItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenItems.add(mock(SystemEvent.class));
        }

        // When
        service.save(tenItems);

        // Then
        verify(mockDao).save(tenItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(SystemEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertListWithTwentyItems() {
        // Given
        List<SystemEvent> twentyItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            twentyItems.add(mock(SystemEvent.class));
        }

        // When
        service.insert(twentyItems);

        // Then
        verify(mockDao).save(twentyItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(SystemEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEventTwice() {
        // When
        service.save(mockSystemEvent);
        service.save(mockSystemEvent);

        // Then
        verify(mockDao, times(2)).save(mockSystemEvent);
        verify(mockDao, times(2)).save(any(SystemEvent.class));
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListTwice() {
        // Given
        List<SystemEvent> events = Arrays.asList(
            mockSystemEvent,
            mock(SystemEvent.class)
        );

        // When
        service.save(events);
        service.save(events);

        // Then
        verify(mockDao, times(2)).save(events);
        verify(mockDao, times(2)).save(anyList());
        verify(mockDao, never()).save(any(SystemEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSameListTwice() {
        // Given
        List<SystemEvent> events = Arrays.asList(mockSystemEvent);

        // When
        service.insert(events);
        service.insert(events);

        // Then
        verify(mockDao, times(2)).save(events);
        verify(mockDao, times(2)).save(anyList());
        verify(mockDao, never()).save(any(SystemEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testAlternatingBetweenSingleAndList() {
        // Given
        SystemEvent single1 = mock(SystemEvent.class);
        List<SystemEvent> list1 = Arrays.asList(mock(SystemEvent.class));
        SystemEvent single2 = mock(SystemEvent.class);
        List<SystemEvent> list2 = Arrays.asList(
            mock(SystemEvent.class),
            mock(SystemEvent.class)
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
        verify(mockDao, times(2)).save(any(SystemEvent.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListWithFiveItems() {
        // Given
        List<SystemEvent> fiveItems = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fiveItems.add(mock(SystemEvent.class));
        }

        // When
        service.save(fiveItems);

        // Then
        verify(mockDao).save(fiveItems);
        verifyNoMoreInteractions(mockDao);
    }
}
