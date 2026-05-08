package org.ikasan.esb.service.error.reporting;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
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
 * Unit tests for ErrorReportingServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ErrorReportingServiceImplTest {

    @Mock
    private EsbEntityDao<ErrorOccurrence> mockDao;

    @Mock
    private ErrorOccurrence mockErrorOccurrence;

    private ErrorReportingServiceImpl service;

    @Before
    public void setUp() {
        service = new ErrorReportingServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new ErrorReportingServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        ErrorReportingServiceImpl testService = new ErrorReportingServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testSaveSingleEntity() {
        // When
        service.save(mockErrorOccurrence);

        // Then
        verify(mockDao).save(mockErrorOccurrence);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityMultipleTimes() {
        // Given
        ErrorOccurrence error2 = mock(ErrorOccurrence.class);
        ErrorOccurrence error3 = mock(ErrorOccurrence.class);

        // When
        service.save(mockErrorOccurrence);
        service.save(error2);
        service.save(error3);

        // Then
        verify(mockDao).save(mockErrorOccurrence);
        verify(mockDao).save(error2);
        verify(mockDao).save(error3);
        verify(mockDao, times(3)).save(any(ErrorOccurrence.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveMultipleEntities() {
        // Given
        List<ErrorOccurrence> errors = Arrays.asList(
            mockErrorOccurrence,
            mock(ErrorOccurrence.class),
            mock(ErrorOccurrence.class)
        );

        // When
        service.save(errors);

        // Then
        verify(mockDao).save(errors);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveEmptyList() {
        // Given
        List<ErrorOccurrence> emptyList = new ArrayList<>();

        // When
        service.save(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleItemList() {
        // Given
        List<ErrorOccurrence> singleItem = Arrays.asList(mockErrorOccurrence);

        // When
        service.save(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListMultipleTimes() {
        // Given
        List<ErrorOccurrence> list1 = Arrays.asList(mockErrorOccurrence);
        List<ErrorOccurrence> list2 = Arrays.asList(
            mock(ErrorOccurrence.class),
            mock(ErrorOccurrence.class)
        );
        List<ErrorOccurrence> list3 = Arrays.asList(
            mock(ErrorOccurrence.class),
            mock(ErrorOccurrence.class),
            mock(ErrorOccurrence.class)
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
        List<ErrorOccurrence> errors = Arrays.asList(
            mockErrorOccurrence,
            mock(ErrorOccurrence.class)
        );

        // When
        service.insert(errors);

        // Then
        verify(mockDao).save(errors);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertEmptyList() {
        // Given
        List<ErrorOccurrence> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSingleItem() {
        // Given
        List<ErrorOccurrence> singleItem = Arrays.asList(mockErrorOccurrence);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<ErrorOccurrence> list1 = Arrays.asList(mockErrorOccurrence);
        List<ErrorOccurrence> list2 = Arrays.asList(
            mock(ErrorOccurrence.class),
            mock(ErrorOccurrence.class)
        );
        List<ErrorOccurrence> list3 = Arrays.asList(
            mock(ErrorOccurrence.class),
            mock(ErrorOccurrence.class),
            mock(ErrorOccurrence.class)
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
        List<ErrorOccurrence> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ErrorOccurrence.class));
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
        List<ErrorOccurrence> largeList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeList.add(mock(ErrorOccurrence.class));
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
        List<ErrorOccurrence> errors = Arrays.asList(
            mockErrorOccurrence,
            mock(ErrorOccurrence.class)
        );

        // When
        service.insert(errors);

        // Then
        // Verify that insert actually delegates to save
        verify(mockDao).save(errors);
        verify(mockDao, never()).save(any(ErrorOccurrence.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testMixedSaveAndInsertCalls() {
        // Given
        List<ErrorOccurrence> list1 = Arrays.asList(mockErrorOccurrence);
        ErrorOccurrence singleError = mock(ErrorOccurrence.class);
        List<ErrorOccurrence> list2 = Arrays.asList(
            mock(ErrorOccurrence.class),
            mock(ErrorOccurrence.class)
        );

        // When
        service.save(list1);
        service.save(singleError);
        service.insert(list2);

        // Then
        verify(mockDao).save(list1);
        verify(mockDao).save(singleError);
        verify(mockDao).save(list2);
        verify(mockDao, times(1)).save(any(ErrorOccurrence.class));
        verify(mockDao, times(2)).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveSingleEntityDoesNotInteractWithListSave() {
        // When
        service.save(mockErrorOccurrence);

        // Then
        verify(mockDao).save(mockErrorOccurrence);
        verify(mockDao, never()).save(anyList());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveListDoesNotInteractWithSingleSave() {
        // Given
        List<ErrorOccurrence> errors = Arrays.asList(mockErrorOccurrence);

        // When
        service.save(errors);

        // Then
        verify(mockDao).save(errors);
        verify(mockDao, never()).save(any(ErrorOccurrence.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertDoesNotInteractWithSingleSave() {
        // Given
        List<ErrorOccurrence> errors = Arrays.asList(mockErrorOccurrence);

        // When
        service.insert(errors);

        // Then
        verify(mockDao).save(errors);
        verify(mockDao, never()).save(any(ErrorOccurrence.class));
        verifyNoMoreInteractions(mockDao);
    }
}
