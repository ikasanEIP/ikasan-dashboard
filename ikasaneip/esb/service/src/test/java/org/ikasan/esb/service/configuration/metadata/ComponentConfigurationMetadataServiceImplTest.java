package org.ikasan.esb.service.configuration.metadata;

import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.model.ConfigurationMetaData;
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
 * Unit tests for ConfigurationMetadataServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComponentConfigurationMetadataServiceImplTest {

    @Mock
    private ComponentConfigurationMetadataDao mockDao;

    @Mock
    private ConfigurationMetaData mockConfigurationMetaData;

    private ConfigurationMetadataServiceImpl service;

    @Before
    public void setUp() {
        service = new ConfigurationMetadataServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new ConfigurationMetadataServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        ConfigurationMetadataServiceImpl testService =
            new ConfigurationMetadataServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("config-1")).thenReturn(mockConfigurationMetaData);

        // When
        ConfigurationMetaData result = service.findById("config-1");

        // Then
        assertNotNull(result);
        assertEquals(mockConfigurationMetaData, result);
        verify(mockDao).findById("config-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockDao.findById("non-existent")).thenReturn(null);

        // When
        ConfigurationMetaData result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockDao).findById("non-existent");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdWithDifferentIds() {
        // Given
        ConfigurationMetaData config1 = mock(ConfigurationMetaData.class);
        ConfigurationMetaData config2 = mock(ConfigurationMetaData.class);
        ConfigurationMetaData config3 = mock(ConfigurationMetaData.class);

        when(mockDao.findById("config-1")).thenReturn(config1);
        when(mockDao.findById("config-2")).thenReturn(config2);
        when(mockDao.findById("config-3")).thenReturn(config3);

        // When
        ConfigurationMetaData result1 = service.findById("config-1");
        ConfigurationMetaData result2 = service.findById("config-2");
        ConfigurationMetaData result3 = service.findById("config-3");

        // Then
        assertEquals(config1, result1);
        assertEquals(config2, result2);
        assertEquals(config3, result3);
        verify(mockDao).findById("config-1");
        verify(mockDao).findById("config-2");
        verify(mockDao).findById("config-3");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAll() {
        // Given
        List<ConfigurationMetaData> expectedList = Arrays.asList(
            mockConfigurationMetaData,
            mock(ConfigurationMetaData.class),
            mock(ConfigurationMetaData.class)
        );
        when(mockDao.findAll()).thenReturn(expectedList);

        // When
        List<ConfigurationMetaData> results = service.findAll();

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        verify(mockDao).findAll();
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllEmptyResults() {
        // Given
        when(mockDao.findAll()).thenReturn(new ArrayList<>());

        // When
        List<ConfigurationMetaData> results = service.findAll();

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).findAll();
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllSingleResult() {
        // Given
        List<ConfigurationMetaData> singleResult = Arrays.asList(mockConfigurationMetaData);
        when(mockDao.findAll()).thenReturn(singleResult);

        // When
        List<ConfigurationMetaData> results = service.findAll();

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(mockConfigurationMetaData, results.get(0));
        verify(mockDao).findAll();
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdList() {
        // Given
        List<String> configIds = Arrays.asList("config-1", "config-2", "config-3");
        List<ConfigurationMetaData> expectedResults = Arrays.asList(
            mockConfigurationMetaData,
            mock(ConfigurationMetaData.class),
            mock(ConfigurationMetaData.class)
        );
        when(mockDao.findInIdList(configIds)).thenReturn(expectedResults);

        // When
        List<ConfigurationMetaData> results = service.findByIdList(configIds);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        verify(mockDao).findInIdList(configIds);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdListWithEmptyList() {
        // Given
        List<String> emptyList = new ArrayList<>();
        when(mockDao.findInIdList(emptyList)).thenReturn(new ArrayList<>());

        // When
        List<ConfigurationMetaData> results = service.findByIdList(emptyList);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).findInIdList(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdListWithSingleId() {
        // Given
        List<String> singleId = Arrays.asList("config-1");
        List<ConfigurationMetaData> singleResult = Arrays.asList(mockConfigurationMetaData);
        when(mockDao.findInIdList(singleId)).thenReturn(singleResult);

        // When
        List<ConfigurationMetaData> results = service.findByIdList(singleId);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(mockConfigurationMetaData, results.get(0));
        verify(mockDao).findInIdList(singleId);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdListWithNoMatchingResults() {
        // Given
        List<String> configIds = Arrays.asList("non-existent-1", "non-existent-2");
        when(mockDao.findInIdList(configIds)).thenReturn(new ArrayList<>());

        // When
        List<ConfigurationMetaData> results = service.findByIdList(configIds);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).findInIdList(configIds);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsert() {
        // Given
        List<ConfigurationMetaData> configList = Arrays.asList(
            mockConfigurationMetaData,
            mock(ConfigurationMetaData.class)
        );

        // When
        service.insert(configList);

        // Then
        verify(mockDao).save(configList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertWithEmptyList() {
        // Given
        List<ConfigurationMetaData> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertWithSingleItem() {
        // Given
        List<ConfigurationMetaData> singleItem = Arrays.asList(mockConfigurationMetaData);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<ConfigurationMetaData> list1 = Arrays.asList(mockConfigurationMetaData);
        List<ConfigurationMetaData> list2 = Arrays.asList(
            mock(ConfigurationMetaData.class),
            mock(ConfigurationMetaData.class)
        );
        List<ConfigurationMetaData> list3 = Arrays.asList(
            mock(ConfigurationMetaData.class),
            mock(ConfigurationMetaData.class),
            mock(ConfigurationMetaData.class)
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
        List<ConfigurationMetaData> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ConfigurationMetaData.class));
        }

        // When
        service.insert(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdListWithLargeList() {
        // Given
        List<String> largeIdList = new ArrayList<>();
        List<ConfigurationMetaData> largeResultList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeIdList.add("config-" + i);
            largeResultList.add(mock(ConfigurationMetaData.class));
        }
        when(mockDao.findInIdList(largeIdList)).thenReturn(largeResultList);

        // When
        List<ConfigurationMetaData> results = service.findByIdList(largeIdList);

        // Then
        assertNotNull(results);
        assertEquals(50, results.size());
        verify(mockDao).findInIdList(largeIdList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllWithLargeResults() {
        // Given
        List<ConfigurationMetaData> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ConfigurationMetaData.class));
        }
        when(mockDao.findAll()).thenReturn(largeList);

        // When
        List<ConfigurationMetaData> results = service.findAll();

        // Then
        assertNotNull(results);
        assertEquals(100, results.size());
        verify(mockDao).findAll();
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdMultipleCalls() {
        // Given
        when(mockDao.findById("config-1")).thenReturn(mockConfigurationMetaData);

        // When
        service.findById("config-1");
        service.findById("config-1");
        service.findById("config-1");

        // Then
        verify(mockDao, times(3)).findById("config-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllMultipleCalls() {
        // Given
        List<ConfigurationMetaData> expectedList = Arrays.asList(mockConfigurationMetaData);
        when(mockDao.findAll()).thenReturn(expectedList);

        // When
        service.findAll();
        service.findAll();

        // Then
        verify(mockDao, times(2)).findAll();
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdListPartialResults() {
        // Given - Requesting 5 IDs but only 3 exist
        List<String> requestedIds = Arrays.asList(
            "config-1", "config-2", "non-existent-1", "config-3", "non-existent-2"
        );
        List<ConfigurationMetaData> partialResults = Arrays.asList(
            mock(ConfigurationMetaData.class),
            mock(ConfigurationMetaData.class),
            mock(ConfigurationMetaData.class)
        );
        when(mockDao.findInIdList(requestedIds)).thenReturn(partialResults);

        // When
        List<ConfigurationMetaData> results = service.findByIdList(requestedIds);

        // Then
        assertNotNull(results);
        assertEquals(3, results.size());
        verify(mockDao).findInIdList(requestedIds);
        verifyNoMoreInteractions(mockDao);
    }
}
