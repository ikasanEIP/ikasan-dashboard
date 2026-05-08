package org.ikasan.esb.service.business.stream.metadata;

import org.ikasan.spec.metadata.BusinessStreamMetadataSearchResults;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.metadata.model.ModuleMetaData;
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
 * Unit tests for BusinessStreamMetaDataServiceImpl using Mockito.
 */
@RunWith(MockitoJUnitRunner.class)
public class BusinessStreamMetaDataServiceImplTest {

    @Mock
    private BusinessStreamMetadataDao mockDao;

    @Mock
    private BusinessStreamMetaData mockBusinessStreamMetaData;

    @Mock
    private BusinessStreamMetadataSearchResults mockSearchResults;

    @Mock
    private ModuleMetaData mockModuleMetaData;

    private BusinessStreamMetaDataServiceImpl service;

    @Before
    public void setUp() {
        service = new BusinessStreamMetaDataServiceImpl(mockDao);
    }

    @Test
    public void testConstructorWithValidDao() {
        // When
        BusinessStreamMetaDataServiceImpl testService = new BusinessStreamMetaDataServiceImpl(mockDao);

        // Then
        assertNotNull(testService);
    }

    @Test
    public void testFindById() {
        // Given
        when(mockDao.findById("bs-1")).thenReturn(mockBusinessStreamMetaData);

        // When
        BusinessStreamMetaData result = service.findById("bs-1");

        // Then
        assertNotNull(result);
        assertEquals(mockBusinessStreamMetaData, result);
        verify(mockDao).findById("bs-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdReturnsNull() {
        // Given
        when(mockDao.findById("non-existent")).thenReturn(null);

        // When
        BusinessStreamMetaData result = service.findById("non-existent");

        // Then
        assertNull(result);
        verify(mockDao).findById("non-existent");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindByIdWithDifferentIds() {
        // Given
        BusinessStreamMetaData metaData1 = mock(BusinessStreamMetaData.class);
        BusinessStreamMetaData metaData2 = mock(BusinessStreamMetaData.class);
        BusinessStreamMetaData metaData3 = mock(BusinessStreamMetaData.class);

        when(mockDao.findById("bs-1")).thenReturn(metaData1);
        when(mockDao.findById("bs-2")).thenReturn(metaData2);
        when(mockDao.findById("bs-3")).thenReturn(metaData3);

        // When
        BusinessStreamMetaData result1 = service.findById("bs-1");
        BusinessStreamMetaData result2 = service.findById("bs-2");
        BusinessStreamMetaData result3 = service.findById("bs-3");

        // Then
        assertEquals(metaData1, result1);
        assertEquals(metaData2, result2);
        assertEquals(metaData3, result3);
        verify(mockDao).findById("bs-1");
        verify(mockDao).findById("bs-2");
        verify(mockDao).findById("bs-3");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAll() {
        // Given
        List<BusinessStreamMetaData> expectedList = Arrays.asList(
            mockBusinessStreamMetaData,
            mock(BusinessStreamMetaData.class)
        );
        when(mockDao.findAll(10, 0)).thenReturn(expectedList);

        // When
        List<BusinessStreamMetaData> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(mockDao).findAll(10, 0);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllWithDifferentParameters() {
        // Given
        List<BusinessStreamMetaData> list1 = Arrays.asList(mockBusinessStreamMetaData);
        List<BusinessStreamMetaData> list2 = Arrays.asList(
            mock(BusinessStreamMetaData.class),
            mock(BusinessStreamMetaData.class),
            mock(BusinessStreamMetaData.class)
        );

        when(mockDao.findAll(5, 0)).thenReturn(list1);
        when(mockDao.findAll(20, 10)).thenReturn(list2);

        // When
        List<BusinessStreamMetaData> results1 = service.findAll(5, 0);
        List<BusinessStreamMetaData> results2 = service.findAll(20, 10);

        // Then
        assertEquals(1, results1.size());
        assertEquals(3, results2.size());
        verify(mockDao).findAll(5, 0);
        verify(mockDao).findAll(20, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllEmptyResults() {
        // Given
        when(mockDao.findAll(10, 0)).thenReturn(new ArrayList<>());

        // When
        List<BusinessStreamMetaData> results = service.findAll(10, 0);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).findAll(10, 0);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFind() {
        // Given
        List<String> businessStreamNames = Arrays.asList("stream-1", "stream-2", "stream-3");
        when(mockDao.find(businessStreamNames, 10, 0)).thenReturn(mockSearchResults);

        // When
        BusinessStreamMetadataSearchResults results = service.find(businessStreamNames, 10, 0);

        // Then
        assertNotNull(results);
        assertEquals(mockSearchResults, results);
        verify(mockDao).find(businessStreamNames, 10, 0);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithEmptyList() {
        // Given
        List<String> emptyList = new ArrayList<>();
        when(mockDao.find(emptyList, 10, 0)).thenReturn(mockSearchResults);

        // When
        BusinessStreamMetadataSearchResults results = service.find(emptyList, 10, 0);

        // Then
        assertNotNull(results);
        verify(mockDao).find(emptyList, 10, 0);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithSingleName() {
        // Given
        List<String> singleName = Arrays.asList("stream-1");
        when(mockDao.find(singleName, 5, 0)).thenReturn(mockSearchResults);

        // When
        BusinessStreamMetadataSearchResults results = service.find(singleName, 5, 0);

        // Then
        assertNotNull(results);
        verify(mockDao).find(singleName, 5, 0);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithDifferentOffsetAndLimit() {
        // Given
        List<String> names = Arrays.asList("stream-1");
        BusinessStreamMetadataSearchResults searchResults1 = mock(BusinessStreamMetadataSearchResults.class);
        BusinessStreamMetadataSearchResults searchResults2 = mock(BusinessStreamMetadataSearchResults.class);

        when(mockDao.find(names, 10, 0)).thenReturn(searchResults1);
        when(mockDao.find(names, 20, 50)).thenReturn(searchResults2);

        // When
        BusinessStreamMetadataSearchResults results1 = service.find(names, 10, 0);
        BusinessStreamMetadataSearchResults results2 = service.find(names, 20, 50);

        // Then
        assertEquals(searchResults1, results1);
        assertEquals(searchResults2, results2);
        verify(mockDao).find(names, 10, 0);
        verify(mockDao).find(names, 20, 50);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSave() {
        // When
        service.save(mockBusinessStreamMetaData);

        // Then
        verify(mockDao).save(mockBusinessStreamMetaData);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testSaveMultipleTimes() {
        // Given
        BusinessStreamMetaData metaData2 = mock(BusinessStreamMetaData.class);
        BusinessStreamMetaData metaData3 = mock(BusinessStreamMetaData.class);

        // When
        service.save(mockBusinessStreamMetaData);
        service.save(metaData2);
        service.save(metaData3);

        // Then
        verify(mockDao).save(mockBusinessStreamMetaData);
        verify(mockDao).save(metaData2);
        verify(mockDao).save(metaData3);
        verify(mockDao, times(3)).save(any(BusinessStreamMetaData.class));
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testDelete() {
        // When
        service.delete("bs-1");

        // Then
        verify(mockDao).delete("bs-1");
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testDeleteMultipleTimes() {
        // When
        service.delete("bs-1");
        service.delete("bs-2");
        service.delete("bs-3");

        // Then
        verify(mockDao).delete("bs-1");
        verify(mockDao).delete("bs-2");
        verify(mockDao).delete("bs-3");
        verify(mockDao, times(3)).delete(anyString());
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindBusinessStreamsContainingFlow() {
        // Given
        List<BusinessStreamMetaData> expectedList = Arrays.asList(
            mockBusinessStreamMetaData,
            mock(BusinessStreamMetaData.class)
        );
        when(mockDao.findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 10))
            .thenReturn(expectedList);

        // When
        List<BusinessStreamMetaData> results =
            service.findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(mockDao).findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindBusinessStreamsContainingFlowEmptyResults() {
        // Given
        when(mockDao.findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 10))
            .thenReturn(new ArrayList<>());

        // When
        List<BusinessStreamMetaData> results =
            service.findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 10);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(mockDao).findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindBusinessStreamsContainingFlowWithDifferentParameters() {
        // Given
        List<BusinessStreamMetaData> list1 = Arrays.asList(mockBusinessStreamMetaData);
        List<BusinessStreamMetaData> list2 = Arrays.asList(
            mock(BusinessStreamMetaData.class),
            mock(BusinessStreamMetaData.class)
        );

        when(mockDao.findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 5))
            .thenReturn(list1);
        when(mockDao.findBusinessStreamsContainingFlow("module-2", "flow-2", 10, 20))
            .thenReturn(list2);

        // When
        List<BusinessStreamMetaData> results1 =
            service.findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 5);
        List<BusinessStreamMetaData> results2 =
            service.findBusinessStreamsContainingFlow("module-2", "flow-2", 10, 20);

        // Then
        assertEquals(1, results1.size());
        assertEquals(2, results2.size());
        verify(mockDao).findBusinessStreamsContainingFlow("module-1", "flow-1", 0, 5);
        verify(mockDao).findBusinessStreamsContainingFlow("module-2", "flow-2", 10, 20);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindBusinessStreamsForModules() {
        // Given
        List<ModuleMetaData> modules = Arrays.asList(
            mockModuleMetaData,
            mock(ModuleMetaData.class)
        );
        when(mockDao.findBusinessStreamsForModules("filter", modules, 0, 10))
            .thenReturn(mockSearchResults);

        // When
        BusinessStreamMetadataSearchResults results =
            service.findBusinessStreamsForModules("filter", modules, 0, 10);

        // Then
        assertNotNull(results);
        assertEquals(mockSearchResults, results);
        verify(mockDao).findBusinessStreamsForModules("filter", modules, 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindBusinessStreamsForModulesWithEmptyModulesList() {
        // Given
        List<ModuleMetaData> emptyModules = new ArrayList<>();
        when(mockDao.findBusinessStreamsForModules("filter", emptyModules, 0, 10))
            .thenReturn(mockSearchResults);

        // When
        BusinessStreamMetadataSearchResults results =
            service.findBusinessStreamsForModules("filter", emptyModules, 0, 10);

        // Then
        assertNotNull(results);
        verify(mockDao).findBusinessStreamsForModules("filter", emptyModules, 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindBusinessStreamsForModulesWithNullFilter() {
        // Given
        List<ModuleMetaData> modules = Arrays.asList(mockModuleMetaData);
        when(mockDao.findBusinessStreamsForModules(null, modules, 0, 10))
            .thenReturn(mockSearchResults);

        // When
        BusinessStreamMetadataSearchResults results =
            service.findBusinessStreamsForModules(null, modules, 0, 10);

        // Then
        assertNotNull(results);
        verify(mockDao).findBusinessStreamsForModules(null, modules, 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindBusinessStreamsForModulesWithDifferentParameters() {
        // Given
        List<ModuleMetaData> modules1 = Arrays.asList(mockModuleMetaData);
        List<ModuleMetaData> modules2 = Arrays.asList(
            mock(ModuleMetaData.class),
            mock(ModuleMetaData.class),
            mock(ModuleMetaData.class)
        );

        BusinessStreamMetadataSearchResults searchResults1 = mock(BusinessStreamMetadataSearchResults.class);
        BusinessStreamMetadataSearchResults searchResults2 = mock(BusinessStreamMetadataSearchResults.class);

        when(mockDao.findBusinessStreamsForModules("filter1", modules1, 0, 10))
            .thenReturn(searchResults1);
        when(mockDao.findBusinessStreamsForModules("filter2", modules2, 20, 50))
            .thenReturn(searchResults2);

        // When
        BusinessStreamMetadataSearchResults results1 =
            service.findBusinessStreamsForModules("filter1", modules1, 0, 10);
        BusinessStreamMetadataSearchResults results2 =
            service.findBusinessStreamsForModules("filter2", modules2, 20, 50);

        // Then
        assertEquals(searchResults1, results1);
        assertEquals(searchResults2, results2);
        verify(mockDao).findBusinessStreamsForModules("filter1", modules1, 0, 10);
        verify(mockDao).findBusinessStreamsForModules("filter2", modules2, 20, 50);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindBusinessStreamsForModulesWithEmptyFilter() {
        // Given
        List<ModuleMetaData> modules = Arrays.asList(mockModuleMetaData);
        when(mockDao.findBusinessStreamsForModules("", modules, 0, 10))
            .thenReturn(mockSearchResults);

        // When
        BusinessStreamMetadataSearchResults results =
            service.findBusinessStreamsForModules("", modules, 0, 10);

        // Then
        assertNotNull(results);
        verify(mockDao).findBusinessStreamsForModules("", modules, 0, 10);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllWithLargeLimit() {
        // Given
        List<BusinessStreamMetaData> largeList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(mock(BusinessStreamMetaData.class));
        }
        when(mockDao.findAll(100, 0)).thenReturn(largeList);

        // When
        List<BusinessStreamMetaData> results = service.findAll(100, 0);

        // Then
        assertNotNull(results);
        assertEquals(100, results.size());
        verify(mockDao).findAll(100, 0);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindAllWithLargeOffset() {
        // Given
        List<BusinessStreamMetaData> expectedList = Arrays.asList(mockBusinessStreamMetaData);
        when(mockDao.findAll(10, 1000)).thenReturn(expectedList);

        // When
        List<BusinessStreamMetaData> results = service.findAll(10, 1000);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
        verify(mockDao).findAll(10, 1000);
        verifyNoMoreInteractions(mockDao);
    }

    @Test
    public void testFindWithLargeListOfNames() {
        // Given
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            largeList.add("stream-" + i);
        }
        when(mockDao.find(largeList, 100, 0)).thenReturn(mockSearchResults);

        // When
        BusinessStreamMetadataSearchResults results = service.find(largeList, 100, 0);

        // Then
        assertNotNull(results);
        verify(mockDao).find(largeList, 100, 0);
        verifyNoMoreInteractions(mockDao);
    }
}
