package org.ikasan.rest.dashboard.util;

import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Test implementation of ESBSearchService for unit testing.
 */
public class TestESBSearchService implements ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults> {

    private long totalResults = 0;
    private List<IkasanESBDocument> results = new ArrayList<>();

    // Track last search parameters for verification
    private Set<String> lastModuleNames;
    private Set<String> lastFlowNames;
    private Set<String> lastComponentNames;
    private Set<String> lastIdentifiers;
    private List<String> lastEntityTypes;
    private long lastStartTime;
    private long lastEndTime;
    private int lastOffset;
    private int lastLimit;

    public void setTotalResults(long totalResults) {
        this.totalResults = totalResults;
    }

    public void setResults(List<IkasanESBDocument> results) {
        this.results = results;
    }

    // Getters for verification in tests
    public Set<String> getLastModuleNames() {
        return lastModuleNames;
    }

    public Set<String> getLastFlowNames() {
        return lastFlowNames;
    }

    public Set<String> getLastComponentNames() {
        return lastComponentNames;
    }

    public List<String> getLastEntityTypes() {
        return lastEntityTypes;
    }

    public long getLastStartTime() {
        return lastStartTime;
    }

    public long getLastEndTime() {
        return lastEndTime;
    }

    public int getLastOffset() {
        return lastOffset;
    }

    public int getLastLimit() {
        return lastLimit;
    }

    public Set<String> getLastIdentifiers() {
        return lastIdentifiers;
    }

    public void reset() {
        this.totalResults = 0;
        this.results = new ArrayList<>();
        this.lastModuleNames = null;
        this.lastFlowNames = null;
        this.lastComponentNames = null;
        this.lastIdentifiers = null;
        this.lastEntityTypes = null;
        this.lastStartTime = 0;
        this.lastEndTime = 0;
        this.lastOffset = 0;
        this.lastLimit = 0;
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> identifiers, int offset, int resultSize, String sortField, String sortOrder) {
        this.lastIdentifiers = identifiers;
        this.lastOffset = offset;
        this.lastLimit = resultSize;
        return new TestIkasanDocumentSearchResults(results, totalResults);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleName, Set<String> flowNames, String searchString,
                                              long startTime, long endTime, int resultSize, boolean negateQuery,
                                              String sortField, String sortOrder) {
        this.lastModuleNames = moduleName;
        this.lastFlowNames = flowNames;
        this.lastStartTime = startTime;
        this.lastEndTime = endTime;
        this.lastLimit = resultSize;
        return new TestIkasanDocumentSearchResults(results, totalResults);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleName, Set<String> flowNames, String searchString,
                                              long startTime, long endTime, int resultSize, List<String> entityTypes,
                                              boolean negateQuery, String sortField, String sortOrder) {
        this.lastModuleNames = moduleName;
        this.lastFlowNames = flowNames;
        this.lastEntityTypes = entityTypes;
        this.lastStartTime = startTime;
        this.lastEndTime = endTime;
        this.lastLimit = resultSize;
        return new TestIkasanDocumentSearchResults(results, totalResults);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleName, Set<String> flowNames, Set<String> componentNames,
                                              String eventId, String searchString, long startTime, long endTime,
                                              int offset, int resultSize, List<String> entityTypes, boolean negateQuery,
                                              String sortField, String sortOrder) {
        this.lastModuleNames = moduleName;
        this.lastFlowNames = flowNames;
        this.lastComponentNames = componentNames;
        this.lastEntityTypes = entityTypes;
        this.lastStartTime = startTime;
        this.lastEndTime = endTime;
        this.lastOffset = offset;
        this.lastLimit = resultSize;
        return new TestIkasanDocumentSearchResults(results, totalResults);
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int resultSize,
                                              List<String> entityTypes, boolean negateQuery, String sortField,
                                              String sortOrder) {
        return new TestIkasanDocumentSearchResults(results, totalResults);
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int offset,
                                              int resultSize, List<String> entityTypes, boolean negateQuery,
                                              String sortField, String sortOrder) {
        return new TestIkasanDocumentSearchResults(results, totalResults);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, String searchString, long startTime,
                                              long endTime, int offset, int resultSize, List<String> entityTypes,
                                              boolean negateQuery, String sortField, String sortOrder) {
        return new TestIkasanDocumentSearchResults(results, totalResults);
    }

    @Override
    public IkasanDocumentSearchResults searchByHarvestReceivedTime(Set<String> moduleName, Set<String> flowNames
        , Set<String> componentNames, String eventId, String searchString, long harvestReceivedStartTime
        , long harvestReceivedEndTime, int offset, int resultSize, List<String> entityTypes
        , boolean negateQuery, String sortField, String sortOrder) {
        this.lastModuleNames = moduleName;
        this.lastFlowNames = flowNames;
        this.lastComponentNames = componentNames;
        this.lastEntityTypes = entityTypes;
        this.lastStartTime = harvestReceivedStartTime;
        this.lastEndTime = harvestReceivedEndTime;
        this.lastOffset = offset;
        this.lastLimit = resultSize;
        return new TestIkasanDocumentSearchResults(results, totalResults);
    }

    @Override
    public IkasanESBDocument findById(String type, String id) {
        return null;
    }

    @Override
    public IkasanESBDocument findByErrorUri(String type, String uri) {
        return null;
    }

    /**
     * Test implementation of IkasanDocumentSearchResults
     */
    private static class TestIkasanDocumentSearchResults implements IkasanDocumentSearchResults {
        private final List<IkasanESBDocument> resultList;
        private final long totalResults;

        public TestIkasanDocumentSearchResults(List<IkasanESBDocument> resultList, long totalResults) {
            this.resultList = resultList;
            this.totalResults = totalResults;
        }

        @Override
        public List<IkasanESBDocument> getResultList() {
            return resultList;
        }

        @Override
        public long getTotalNumberOfResults() {
            return totalResults;
        }

        @Override
        public long getQueryResponseTime() {
            return 0;
        }
    }
}
