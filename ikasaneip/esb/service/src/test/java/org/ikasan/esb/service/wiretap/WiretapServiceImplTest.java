package org.ikasan.esb.service.wiretap;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.module.ModuleService;
import org.ikasan.spec.wiretap.WiretapEvent;
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
 * Unit tests for WiretapServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class WiretapServiceImplTest {

    @Mock
    private EsbEntityDao<WiretapEvent> mockDao;

    @Mock
    private ModuleService mockModuleService;

    @Mock
    private WiretapEvent mockWiretapEvent;

    private WiretapServiceImpl service;

    @Before
    public void setUp() {
        service = new WiretapServiceImpl(mockDao, mockModuleService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new WiretapServiceImpl(null, mockModuleService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullModuleService() {
        new WiretapServiceImpl(mockDao, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithBothNull() {
        new WiretapServiceImpl(null, null);
    }

    @Test
    public void testConstructorWithValidDaoAndModuleService() {
        // When
        WiretapServiceImpl testService = new WiretapServiceImpl(mockDao, mockModuleService);

        // Then
        assertNotNull(testService);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleParameterConstructorWithNullDao() {
        new WiretapServiceImpl(null);
    }

    @Test
    public void testSingleParameterConstructorWithValidDao() {
        // When
        WiretapServiceImpl testService = new WiretapServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testSaveSingleEntity() {
        // When
        service.save(mockWiretapEvent);

        // Then
        verify(mockDao).save(mockWiretapEvent);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveSingleEntityMultipleTimes() {
        // Given
        WiretapEvent event2 = mock(WiretapEvent.class);
        WiretapEvent event3 = mock(WiretapEvent.class);

        // When
        service.save(mockWiretapEvent);
        service.save(event2);
        service.save(event3);

        // Then
        verify(mockDao).save(mockWiretapEvent);
        verify(mockDao).save(event2);
        verify(mockDao).save(event3);
        verify(mockDao, times(3)).save(any(WiretapEvent.class));
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveMultipleEntities() {
        // Given
        List<WiretapEvent> events = Arrays.asList(
            mockWiretapEvent,
            mock(WiretapEvent.class),
            mock(WiretapEvent.class)
        );

        // When
        service.save(events);

        // Then
        verify(mockDao).save(events);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveEmptyList() {
        // Given
        List<WiretapEvent> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveSingleItemList() {
        // Given
        List<WiretapEvent> singleItem = Arrays.asList(mockWiretapEvent);

        // When
        service.save(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveListMultipleTimes() {
        // Given
        List<WiretapEvent> list1 = Arrays.asList(mockWiretapEvent);
        List<WiretapEvent> list2 = Arrays.asList(
            mock(WiretapEvent.class),
            mock(WiretapEvent.class)
        );
        List<WiretapEvent> list3 = Arrays.asList(
            mock(WiretapEvent.class),
            mock(WiretapEvent.class),
            mock(WiretapEvent.class)
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
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testInsert() {
        // Given
        List<WiretapEvent> events = Arrays.asList(
            mockWiretapEvent,
            mock(WiretapEvent.class)
        );

        // When
        service.insert(events);

        // Then
        verify(mockDao).save(events);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testInsertEmptyList() {
        // Given
        List<WiretapEvent> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testInsertSingleItem() {
        // Given
        List<WiretapEvent> singleItem = Arrays.asList(mockWiretapEvent);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<WiretapEvent> list1 = Arrays.asList(mockWiretapEvent);
        List<WiretapEvent> list2 = Arrays.asList(
            mock(WiretapEvent.class),
            mock(WiretapEvent.class)
        );
        List<WiretapEvent> list3 = Arrays.asList(
            mock(WiretapEvent.class),
            mock(WiretapEvent.class),
            mock(WiretapEvent.class)
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
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testInsertWithLargeList() {
        // Given
        List<WiretapEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(WiretapEvent.class));
        }

        // When
        service.insert(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveWithLargeList() {
        // Given
        List<WiretapEvent> largeList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeList.add(mock(WiretapEvent.class));
        }

        // When
        service.save(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testInsertCallsSaveMethod() {
        // Given
        List<WiretapEvent> events = Arrays.asList(
            mockWiretapEvent,
            mock(WiretapEvent.class)
        );

        // When
        service.insert(events);

        // Then
        // Verify that insert actually delegates to save
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(WiretapEvent.class));
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testMixedSaveAndInsertCalls() {
        // Given
        List<WiretapEvent> list1 = Arrays.asList(mockWiretapEvent);
        WiretapEvent singleEvent = mock(WiretapEvent.class);
        List<WiretapEvent> list2 = Arrays.asList(
            mock(WiretapEvent.class),
            mock(WiretapEvent.class)
        );

        // When
        service.save(list1);
        service.save(singleEvent);
        service.insert(list2);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(singleEvent);
        verify(mockDao).save(list2);
        verify(mockDao, times(1)).save(any(WiretapEvent.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveSingleEntityDoesNotInteractWithListSave() {
        // When
        service.save(mockWiretapEvent);

        // Then
        verify(mockDao).save(mockWiretapEvent);
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveListDoesNotInteractWithSingleSave() {
        // Given
        List<WiretapEvent> events = Arrays.asList(mockWiretapEvent);

        // When
        service.save(events);

        // Then
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(WiretapEvent.class));
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testInsertDoesNotInteractWithSingleSave() {
        // Given
        List<WiretapEvent> events = Arrays.asList(mockWiretapEvent);

        // When
        service.insert(events);

        // Then
        verify(mockDao).save(events);
        verify(mockDao, never()).save(any(WiretapEvent.class));
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveSingleEventWithDifferentInstances() {
        // Given
        WiretapEvent event1 = mock(WiretapEvent.class);
        WiretapEvent event2 = mock(WiretapEvent.class);
        WiretapEvent event3 = mock(WiretapEvent.class);

        // When
        service.save(event1);
        service.save(event2);
        service.save(event3);

        // Then
        verify(mockDao).save(event1);
        verify(mockDao).save(event2);
        verify(mockDao).save(event3);
        verify(mockDao, times(3)).save(any(WiretapEvent.class));
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSaveListWithTenItems() {
        // Given
        List<WiretapEvent> tenItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenItems.add(mock(WiretapEvent.class));
        }

        // When
        service.save(tenItems);

        // Then
        verify(mockDao).save(tenItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(WiretapEvent.class));
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testInsertListWithTwentyItems() {
        // Given
        List<WiretapEvent> twentyItems = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            twentyItems.add(mock(WiretapEvent.class));
        }

        // When
        service.insert(twentyItems);

        // Then
        verify(mockDao).save(twentyItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(WiretapEvent.class));
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }

    @Test
    public void testSingleParameterConstructorSaveOperations() {
        // Given
        WiretapServiceImpl serviceWithoutModuleService = new WiretapServiceImpl(mockDao);
        List<WiretapEvent> events = Arrays.asList(mockWiretapEvent);

        // When
        serviceWithoutModuleService.save(mockWiretapEvent);
        serviceWithoutModuleService.save(events);

        // Then
        verify(mockDao).save(mockWiretapEvent);
        verify(mockDao).save(events);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testAlternatingBetweenSingleAndList() {
        // Given
        WiretapEvent single1 = mock(WiretapEvent.class);
        List<WiretapEvent> list1 = Arrays.asList(mock(WiretapEvent.class));
        WiretapEvent single2 = mock(WiretapEvent.class);
        List<WiretapEvent> list2 = Arrays.asList(
            mock(WiretapEvent.class),
            mock(WiretapEvent.class)
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
        verify(mockDao, times(2)).save(any(WiretapEvent.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
        verifyNoInteractions(mockModuleService);
    }
}
