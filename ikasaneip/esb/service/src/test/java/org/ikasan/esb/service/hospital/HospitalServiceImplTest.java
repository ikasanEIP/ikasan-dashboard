package org.ikasan.esb.service.hospital;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
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
 * Unit tests for HospitalServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class HospitalServiceImplTest {

    @Mock
    private EsbEntityDao<ExclusionEventAction> mockDao;

    @Mock
    private ExclusionEventAction mockExclusionEventAction;

    private HospitalServiceImpl service;

    @Before
    public void setUp() {
        service = new HospitalServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new HospitalServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        HospitalServiceImpl testService = new HospitalServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testSaveSingleEntity() {
        // When
        service.save(mockExclusionEventAction);

        // Then
        verify(mockDao).save(mockExclusionEventAction);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityMultipleTimes() {
        // Given
        ExclusionEventAction action2 = mock(ExclusionEventAction.class);
        ExclusionEventAction action3 = mock(ExclusionEventAction.class);

        // When
        service.save(mockExclusionEventAction);
        service.save(action2);
        service.save(action3);

        // Then
        verify(mockDao).save(mockExclusionEventAction);
        verify(mockDao).save(action2);
        verify(mockDao).save(action3);
        verify(mockDao, times(3)).save(any(ExclusionEventAction.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveMultipleEntities() {
        // Given
        List<ExclusionEventAction> actions = Arrays.asList(
            mockExclusionEventAction,
            mock(ExclusionEventAction.class),
            mock(ExclusionEventAction.class)
        );

        // When
        service.save(actions);

        // Then
        verify(mockDao).save(actions);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveEmptyList() {
        // Given
        List<ExclusionEventAction> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleItemList() {
        // Given
        List<ExclusionEventAction> singleItem = Arrays.asList(mockExclusionEventAction);

        // When
        service.save(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListMultipleTimes() {
        // Given
        List<ExclusionEventAction> list1 = Arrays.asList(mockExclusionEventAction);
        List<ExclusionEventAction> list2 = Arrays.asList(
            mock(ExclusionEventAction.class),
            mock(ExclusionEventAction.class)
        );
        List<ExclusionEventAction> list3 = Arrays.asList(
            mock(ExclusionEventAction.class),
            mock(ExclusionEventAction.class),
            mock(ExclusionEventAction.class)
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
    public void testSaveWithLargeList() {
        // Given
        List<ExclusionEventAction> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ExclusionEventAction.class));
        }

        // When
        service.save(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityDoesNotInteractWithListSave() {
        // When
        service.save(mockExclusionEventAction);

        // Then
        verify(mockDao).save(mockExclusionEventAction);
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListDoesNotInteractWithSingleSave() {
        // Given
        List<ExclusionEventAction> actions = Arrays.asList(mockExclusionEventAction);

        // When
        service.save(actions);

        // Then
        verify(mockDao).save(actions);
        verify(mockDao, never()).save(any(ExclusionEventAction.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testMixedSaveCalls() {
        // Given
        List<ExclusionEventAction> list1 = Arrays.asList(mockExclusionEventAction);
        ExclusionEventAction singleAction = mock(ExclusionEventAction.class);
        List<ExclusionEventAction> list2 = Arrays.asList(
            mock(ExclusionEventAction.class),
            mock(ExclusionEventAction.class)
        );

        // When
        service.save(list1);
        service.save(singleAction);
        service.save(list2);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(singleAction);
        verify(mockDao).save(list2);
        verify(mockDao, times(1)).save(any(ExclusionEventAction.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleActionWithDifferentInstances() {
        // Given
        ExclusionEventAction action1 = mock(ExclusionEventAction.class);
        ExclusionEventAction action2 = mock(ExclusionEventAction.class);
        ExclusionEventAction action3 = mock(ExclusionEventAction.class);

        // When
        service.save(action1);
        service.save(action2);
        service.save(action3);

        // Then
        verify(mockDao).save(action1);
        verify(mockDao).save(action2);
        verify(mockDao).save(action3);
        verify(mockDao, times(3)).save(any(ExclusionEventAction.class));
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListWithTenItems() {
        // Given
        List<ExclusionEventAction> tenItems = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenItems.add(mock(ExclusionEventAction.class));
        }

        // When
        service.save(tenItems);

        // Then
        verify(mockDao).save(tenItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(ExclusionEventAction.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListWithFiftyItems() {
        // Given
        List<ExclusionEventAction> fiftyItems = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            fiftyItems.add(mock(ExclusionEventAction.class));
        }

        // When
        service.save(fiftyItems);

        // Then
        verify(mockDao).save(fiftyItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(ExclusionEventAction.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListWithTwoItems() {
        // Given
        List<ExclusionEventAction> twoItems = Arrays.asList(
            mock(ExclusionEventAction.class),
            mock(ExclusionEventAction.class)
        );

        // When
        service.save(twoItems);

        // Then
        verify(mockDao).save(twoItems);
        verify(mockDao, times(1)).save(anyList());
        verify(mockDao, never()).save(any(ExclusionEventAction.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityTwice() {
        // When
        service.save(mockExclusionEventAction);
        service.save(mockExclusionEventAction);

        // Then
        verify(mockDao, times(2)).save(mockExclusionEventAction);
        verify(mockDao, times(2)).save(any(ExclusionEventAction.class));
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListTwice() {
        // Given
        List<ExclusionEventAction> actions = Arrays.asList(
            mockExclusionEventAction,
            mock(ExclusionEventAction.class)
        );

        // When
        service.save(actions);
        service.save(actions);

        // Then
        verify(mockDao, times(2)).save(actions);
        verify(mockDao, times(2)).save(anyList());
        verify(mockDao, never()).save(any(ExclusionEventAction.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveAlternatingBetweenSingleAndList() {
        // Given
        ExclusionEventAction single1 = mock(ExclusionEventAction.class);
        List<ExclusionEventAction> list1 = Arrays.asList(mock(ExclusionEventAction.class));
        ExclusionEventAction single2 = mock(ExclusionEventAction.class);
        List<ExclusionEventAction> list2 = Arrays.asList(
            mock(ExclusionEventAction.class),
            mock(ExclusionEventAction.class)
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
        verify(mockDao, times(2)).save(any(ExclusionEventAction.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }
}
