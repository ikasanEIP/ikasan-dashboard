package org.ikasan.rest.dashboard.util;

import org.ikasan.rest.dashboard.model.systemevent.SystemEventRecordImpl;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventRecord;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Test implementation of SystemEventSearchService for unit testing.
 */
public class TestSystemEventSearchService implements SystemEventSearchService {

    private Map<String, SystemEventRecord> systemEvents = new HashMap<>();
    private List<SystemEvent> searchResults = new ArrayList<>();
    private RuntimeException exceptionToThrow;

    JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public SystemEvent findById(String id) {
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
        return systemEvents.get(id);
    }

    @Override
    public SearchResults<SystemEvent> findByFilter(SystemEventSearchFilter searchFilter, int limit, int offset, String sortColumn, String sortOrder) {
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }

        return new SearchResults<>() {
            @Override
            public List<SystemEvent> getResultList() {
                return searchResults;
            }

            @Override
            public long getTotalNumberOfResults() {
                return searchResults.size();
            }

            @Override
            public long getQueryResponseTime() {
                return 0;
            }
        };
    }

    // Test helper methods

    public void addSystemEvent(SystemEvent event) {
        systemEvents.put(event.getId().toString(), this.convert(event));
        searchResults.add(event);
    }

    public void setSearchResults(List<SystemEvent> results) {
        this.searchResults = results.stream()
            .map(e -> (SystemEvent)this.convert(e))
            .collect(Collectors.toList());
    }

    private SystemEventRecord convert(SystemEvent event) {
        SystemEventRecord record = new SystemEventRecordImpl();
        record.setAction(event.getAction());
        record.setActor(event.getActor());
        record.setSubject(event.getSubject());
        record.setPayload(mapper.writeValueAsString(event));

        return record;
    }

    public void setExceptionToThrow(RuntimeException exception) {
        this.exceptionToThrow = exception;
    }

    public void clear() {
        systemEvents.clear();
        searchResults.clear();
        exceptionToThrow = null;
    }
}
