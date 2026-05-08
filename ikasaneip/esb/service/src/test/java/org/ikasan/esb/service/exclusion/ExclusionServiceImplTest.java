package org.ikasan.esb.service.exclusion;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.exclusion.ExclusionEvent;
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
 * Unit tests for ExclusionServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExclusionServiceImplTest {

    @Mock
    private EsbEntityDao<ExclusionEvent> mockDao;

    @Mock
    private ExclusionEvent mockExclusionEvent;

    private ExclusionServiceImpl service;

    @Before
    public void setUp() {
        service = new ExclusionServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new ExclusionServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        ExclusionServiceImpl testService = new ExclusionServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testSaveSingleEntity() {
        // When
        service.save(mockExclusionEvent);

        // Then
        verify(mockDao).save(mockExclusionEvent);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityMultipleTimes() {
        // Given
        ExclusionEvent event2 = mock(ExclusionEvent.class);
        ExclusionEvent event3 = mock(ExclusionEvent.class);

        // When
        service.save(mockExclusionEvent);
        service.save(event2);
        service.save(event3);

        // Then
        verify(mockDao).save(mockExclusionEvent);
        verify(mockDao).save(event2);
        verify(mockDao).save(event3);
        verify(mockDao, times(3)).save(any(ExclusionEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveMultipleEntities() {
        // Given
        List<ExclusionEvent> events = Arrays.asList(
            mockExclusionEvent,
            mock(ExclusionEvent.class),
            mock(ExclusionEvent.class)
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
        List<ExclusionEvent> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleItemList() {
        // Given
        List<ExclusionEvent> singleItem = Arrays.asList(mockExclusionEvent);

        // When
        service.save(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListMultipleTimes() {
        // Given
        List<ExclusionEvent> list1 = Arrays.asList(mockExclusionEvent);
        List<ExclusionEvent> list2 = Arrays.asList(
            mock(ExclusionEvent.class),
            mock(ExclusionEvent.class)
        );
        List<ExclusionEvent> list3 = Arrays.asList(
            mock(ExclusionEvent.class),
            mock(ExclusionEvent.class),
            mock(ExclusionEvent.class)
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
        List<ExclusionEvent> events = Arrays.asList(
            mockExclusionEvent,
            mock(ExclusionEvent.class)
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
        List<ExclusionEvent> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSingleItem() {
        // Given
        List<ExclusionEvent> singleItem = Arrays.asList(mockExclusionEvent);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<ExclusionEvent> list1 = Arrays.asList(mockExclusionEvent);
        List<ExclusionEvent> list2 = Arrays.asList(
            mock(ExclusionEvent.class),
            mock(ExclusionEvent.class)
        );
        List<ExclusionEvent> list3 = Arrays.asList(
            mock(ExclusionEvent.class),
            mock(ExclusionEvent.class),
            mock(ExclusionEvent.class)
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
        List<ExclusionEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ExclusionEvent.class));
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
        List<ExclusionEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeList.add(mock(ExclusionEvent.class));
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
        List<ExclusionEvent> events = Arrays.asList(
            mockExclusionEvent,
            mock(ExclusionEvent.class)
        );

        // When
        service.insert(events);

        // Then
        // Verify that insert actually delegates to save
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(ExclusionEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testMixedSaveAndInsertCalls() {
        // Given
        List<ExclusionEvent> list1 = Arrays.asList(mockExclusionEvent);
        ExclusionEvent singleEvent = mock(ExclusionEvent.class);
        List<ExclusionEvent> list2 = Arrays.asList(
            mock(ExclusionEvent.class),
            mock(ExclusionEvent.class)
        );

        // When
        service.save(list1);
        service.save(singleEvent);
        service.insert(list2);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(singleEvent);
        verify(mockDao).save(list2);
        verify(mockDao, times(1)).save(any(ExclusionEvent.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityDoesNotInteractWithListSave() {
        // When
        service.save(mockExclusionEvent);

        // Then
        verify(mockDao).save(mockExclusionEvent);
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListDoesNotInteractWithSingleSave() {
        // Given
        List<ExclusionEvent> events = Arrays.asList(mockExclusionEvent);

        // When
        service.save(events);

        // Then
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(ExclusionEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertDoesNotInteractWithSingleSave() {
        // Given
        List<ExclusionEvent> events = Arrays.asList(mockExclusionEvent);

        // When
        service.insert(events);

        // Then
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(ExclusionEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEventWithDifferentInstances() {
        // Given
        ExclusionEvent event1 = mock(ExclusionEvent.class);
        ExclusionEvent event2 = mock(ExclusionEvent.class);
        ExclusionEvent event3 = mock(ExclusionEvent.class);

        // When
        service.save(event1);
        service.save(event2);
        service.save(event3);

        // Then
        verify(mockDao).save(event1);
        verify(mockDao).save(event2);
        verify(mockDao).save(event3);
        verify(mockDao, times(3)).save(any(ExclusionEvent.class));
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListWithTenItems() {
        // Given
        List<ExclusionEvent> tenItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenItems.add(mock(ExclusionEvent.class));
        }

        // When
        service.save(tenItems);

        // Then
        verify(mockDao).save(tenItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(ExclusionEvent.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertListWithTwentyItems() {
        // Given
        List<ExclusionEvent> twentyItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            twentyItems.add(mock(ExclusionEvent.class));
        }

        // When
        service.insert(twentyItems);

        // Then
        verify(mockDao).save(twentyItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(ExclusionEvent.class));
        verifyNoMoreInteractions(mockDao);
    }
}
