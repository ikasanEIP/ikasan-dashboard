package org.ikasan.notification.monitor.mock;

import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.search.SearchResults;

import java.util.ArrayList;
import java.util.List;

public class InternalEventDrivenJobServiceTestImpl implements InternalEventDrivenJobService {
    @Override
    public SearchResults<InternalEventDrivenJobRecord> findAll(int limit, int offset) {
        return null;
    }

    @Override
    public SearchResults<InternalEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {

        List<InternalEventDrivenJobRecord> list = new ArrayList<>();
        list.add(new InternalEventDrivenJobRecordTestImpl());
        return new SearchResultsImpl(list,1,100);
    }

    @Override
    public InternalEventDrivenJobRecord findById(String id) {
        return null;
    }

    @Override
    public void save(InternalEventDrivenJobRecord record) {

    }
}
