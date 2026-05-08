package org.ikasan.esb.service.module.metadata;

import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.module.ModuleType;
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
 * Unit tests for ModuleMetaDataServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class ModuleMetadataServiceImplTest {

    @Mock
    private ModuleMetadataDao mockDao;

    @Mock
    private ModuleMetaData mockModuleMetaData;

    @Mock
    private ModuleMetadataSearchResults mockSearchResults;

    private ModuleMetaDataServiceImpl service;

    @Before
    public void setUp() {
        service = new ModuleMetaDataServiceImpl(mockDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDao() {
        new ModuleMetaDataServiceImpl(null);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        ModuleMetaDataServiceImpl testService = new ModuleMetaDataServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testInsert() {
        // Given
        List<ModuleMetaData> modules = Arrays.asList(
            mockModuleMetaData,
            mock(ModuleMetaData.class),
            mock(ModuleMetaData.class)
        );

        // When
        service.insert(modules);

        // Then
        verify(mockDao).save(modules);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertEmptyList() {
        // Given
        List<ModuleMetaData> emptyList = new ArrayList<>();

        // When
        service.insert(emptyList);

        // Then
        verify(mockDao).save(emptyList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertSingleItem() {
        // Given
        List<ModuleMetaData> singleItem = Arrays.asList(mockModuleMetaData);

        // When
        service.insert(singleItem);

        // Then
        verify(mockDao).save(singleItem);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testInsertMultipleTimes() {
        // Given
        List<ModuleMetaData> list1 = Arrays.asList(mockModuleMetaData);
        List<ModuleMetaData> list2 = Arrays.asList(
            mock(ModuleMetaData.class),
            mock(ModuleMetaData.class)
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
        List<ModuleMetaData> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(ModuleMetaData.class));
        }

        // When
        service.insert(largeList);

        // Then
        verify(mockDao).save(largeList);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("module-1")).thenReturn(mockModuleMetaData);

        // When
        ModuleMetaData result = service.findById("module-1");

        // Then
        assertNotNull(result);
        assertEquals(mockModuleMetaData, result);
        verify(mockDao).findById("module-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockDao.findById("non-existent")).thenReturn(null);

        // When
        ModuleMetaData result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockDao).findById("non-existent");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdWithDifferentIds() {
        // Given
        ModuleMetaData module1 = mock(ModuleMetaData.class);
        ModuleMetaData module2 = mock(ModuleMetaData.class);
        ModuleMetaData module3 = mock(ModuleMetaData.class);

        when(mockDao.findById("module-1")).thenReturn(module1);
        when(mockDao.findById("module-2")).thenReturn(module2);
        when(mockDao.findById("module-3")).thenReturn(module3);

        // When
        ModuleMetaData result1 = service.findById("module-1");
        ModuleMetaData result2 = service.findById("module-2");
        ModuleMetaData result3 = service.findById("module-3");

        // Then
        assertEquals(module1, result1);
        assertEquals(module2, result2);
        assertEquals(module3, result3);
        verify(mockDao).findById("module-1");
        verify(mockDao).findById("module-2");
        verify(mockDao).findById("module-3");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAll() {
        // Given
        ModuleMetadataSearchResults searchResults = new ModuleMetadataSearchResults(
            Arrays.asList(mockModuleMetaData), 5, 100
        );
        List<ModuleMetaData> allModules = Arrays.asList(
            mockModuleMetaData,
            mock(ModuleMetaData.class),
            mock(ModuleMetaData.class),
            mock(ModuleMetaData.class),
            mock(ModuleMetaData.class)
        );

        when(mockDao.find(null, 0, 0)).thenReturn(searchResults);
        when(mockDao.findAll(0, 5)).thenReturn(allModules);

        // When
        List<ModuleMetaData> results = service.findAll();

        // Then
        assertNotNull(results);
        assertEquals(5, results.size());
        verify(mockDao).find(null, 0, 0);
        verify(mockDao).findAll(0, 5);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllWithLargeTotalResults() {
        // Given
        long largeTotalResults = (long) Integer.MAX_VALUE + 100L;
        ModuleMetadataSearchResults searchResults = new ModuleMetadataSearchResults(
            Arrays.asList(mockModuleMetaData), largeTotalResults, 100
        );
        List<ModuleMetaData> allModules = Arrays.asList(mockModuleMetaData);

        when(mockDao.find(null, 0, 0)).thenReturn(searchResults);
        when(mockDao.findAll(0, Integer.MAX_VALUE)).thenReturn(allModules);

        // When
        List<ModuleMetaData> results = service.findAll();

        // Then
        assertNotNull(results);
        verify(mockDao).find(null, 0, 0);
        verify(mockDao).findAll(0, Integer.MAX_VALUE);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllWithEmptyResults() {
        // Given
        ModuleMetadataSearchResults searchResults = new ModuleMetadataSearchResults(
            new ArrayList<>(), 0, 50
        );

        when(mockDao.find(null, 0, 0)).thenReturn(searchResults);
        when(mockDao.findAll(0, 0)).thenReturn(new ArrayList<>());

        // When
        List<ModuleMetaData> results = service.findAll();

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).find(null, 0, 0);
        verify(mockDao).findAll(0, 0);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithModuleNames() {
        // Given
        List<String> moduleNames = Arrays.asList("module-1", "module-2", "module-3");
        ModuleMetadataSearchResults initialSearchResults = new ModuleMetadataSearchResults(
            Arrays.asList(mockModuleMetaData), 3, 100
        );
        ModuleMetadataSearchResults finalSearchResults = new ModuleMetadataSearchResults(
            Arrays.asList(mockModuleMetaData, mock(ModuleMetaData.class), mock(ModuleMetaData.class)), 3, 100
        );

        when(mockDao.find(moduleNames, 0, 0)).thenReturn(initialSearchResults);
        when(mockDao.find(moduleNames, 0, 3)).thenReturn(finalSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(moduleNames);

        // Then
        assertNotNull(results);
        verify(mockDao).find(moduleNames, 0, 0);
        verify(mockDao).find(moduleNames, 0, 3);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithNullModuleNames() {
        // When
        ModuleMetadataSearchResults results = service.find(null);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getTotalNumberOfResults());
        assertEquals(0, results.getResultList().size());
        verifyNoInteractions(mockDao);
    }

    @Test
    public void testFindWithEmptyModuleNames() {
        // Given
        List<String> emptyList = new ArrayList<>();

        // When
        ModuleMetadataSearchResults results = service.find(emptyList);

        // Then
        assertNotNull(results);
        assertEquals(0, results.getTotalNumberOfResults());
        assertEquals(0, results.getResultList().size());
        verifyNoInteractions(mockDao);
    }

    @Test
    public void testFindWithModuleNamesAndLargeTotalResults() {
        // Given
        List<String> moduleNames = Arrays.asList("module-1");
        long largeTotalResults = (long) Integer.MAX_VALUE + 200L;
        ModuleMetadataSearchResults initialSearchResults = new ModuleMetadataSearchResults(
            Arrays.asList(mockModuleMetaData), largeTotalResults, 100
        );
        ModuleMetadataSearchResults finalSearchResults = new ModuleMetadataSearchResults(
            Arrays.asList(mockModuleMetaData), largeTotalResults, 100
        );

        when(mockDao.find(moduleNames, 0, 0)).thenReturn(initialSearchResults);
        when(mockDao.find(moduleNames, 0, Integer.MAX_VALUE)).thenReturn(finalSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(moduleNames);

        // Then
        assertNotNull(results);
        verify(mockDao).find(moduleNames, 0, 0);
        verify(mockDao).find(moduleNames, 0, Integer.MAX_VALUE);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithModuleNamesOffsetAndLimit() {
        // Given
        List<String> moduleNames = Arrays.asList("module-1", "module-2");
        when(mockDao.find(moduleNames, 10, 20)).thenReturn(mockSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(moduleNames, 10, 20);

        // Then
        assertNotNull(results);
        assertEquals(mockSearchResults, results);
        verify(mockDao).find(moduleNames, 10, 20);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithModuleNamesOffsetAndLimitEmptyNames() {
        // Given
        List<String> emptyList = new ArrayList<>();
        when(mockDao.find(emptyList, 0, 10)).thenReturn(mockSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(emptyList, 0, 10);

        // Then
        assertNotNull(results);
        verify(mockDao).find(emptyList, 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithModuleNamesOffsetAndLimitNullNames() {
        // Given
        when(mockDao.find(null, 5, 15)).thenReturn(mockSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(null, 5, 15);

        // Then
        assertNotNull(results);
        verify(mockDao).find(null, 5, 15);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithModuleNamesTypeOffsetAndLimit() {
        // Given
        List<String> moduleNames = Arrays.asList("module-1", "module-2");
        ModuleType moduleType = ModuleType.INTEGRATION_MODULE;
        when(mockDao.find(moduleNames, moduleType, 10, 20)).thenReturn(mockSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(moduleNames, moduleType, 10, 20);

        // Then
        assertNotNull(results);
        assertEquals(mockSearchResults, results);
        verify(mockDao).find(moduleNames, moduleType, 10, 20);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithModuleNamesTypeOffsetAndLimitNullNames() {
        // Given
        ModuleType moduleType = ModuleType.INTEGRATION_MODULE;
        when(mockDao.find(null, moduleType, 0, 10)).thenReturn(mockSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(null, moduleType, 0, 10);

        // Then
        assertNotNull(results);
        verify(mockDao).find(null, moduleType, 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithModuleNamesTypeOffsetAndLimitNullType() {
        // Given
        List<String> moduleNames = Arrays.asList("module-1");
        when(mockDao.find(moduleNames, null, 5, 15)).thenReturn(mockSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(moduleNames, null, 5, 15);

        // Then
        assertNotNull(results);
        verify(mockDao).find(moduleNames, null, 5, 15);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithDifferentModuleTypes() {
        // Given
        List<String> moduleNames = Arrays.asList("module-1");
        ModuleType type1 = ModuleType.INTEGRATION_MODULE;
        ModuleType type2 = ModuleType.SCHEDULER_AGENT;
        ModuleMetadataSearchResults results1 = mock(ModuleMetadataSearchResults.class);
        ModuleMetadataSearchResults results2 = mock(ModuleMetadataSearchResults.class);

        when(mockDao.find(moduleNames, type1, 0, 10)).thenReturn(results1);
        when(mockDao.find(moduleNames, type2, 0, 10)).thenReturn(results2);

        // When
        ModuleMetadataSearchResults result1 = service.find(moduleNames, type1, 0, 10);
        ModuleMetadataSearchResults result2 = service.find(moduleNames, type2, 0, 10);

        // Then
        assertEquals(results1, result1);
        assertEquals(results2, result2);
        verify(mockDao).find(moduleNames, type1, 0, 10);
        verify(mockDao).find(moduleNames, type2, 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testDeleteById() {
        // When
        service.deleteById("module-1");

        // Then
        verify(mockDao).deleteById("module-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testDeleteByIdMultipleTimes() {
        // When
        service.deleteById("module-1");
        service.deleteById("module-2");
        service.deleteById("module-3");

        // Then
        verify(mockDao).deleteById("module-1");
        verify(mockDao).deleteById("module-2");
        verify(mockDao).deleteById("module-3");
        verify(mockDao, times(3)).deleteById(anyString());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testDeleteByIdSameModuleMultipleTimes() {
        // When
        service.deleteById("module-1");
        service.deleteById("module-1");

        // Then
        verify(mockDao, times(2)).deleteById("module-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithLargeListOfModuleNames() {
        // Given
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeList.add("module-" + i);
        }
        ModuleMetadataSearchResults initialSearchResults = new ModuleMetadataSearchResults(
            Arrays.asList(mockModuleMetaData), 50, 100
        );
        ModuleMetadataSearchResults finalSearchResults = new ModuleMetadataSearchResults(
            Arrays.asList(mockModuleMetaData), 50, 100
        );

        when(mockDao.find(largeList, 0, 0)).thenReturn(initialSearchResults);
        when(mockDao.find(largeList, 0, 50)).thenReturn(finalSearchResults);

        // When
        ModuleMetadataSearchResults results = service.find(largeList);

        // Then
        assertNotNull(results);
        verify(mockDao).find(largeList, 0, 0);
        verify(mockDao).find(largeList, 0, 50);
        verifyNoMoreInteractions(mockDao);
    }
}
