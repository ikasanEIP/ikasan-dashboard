package org.ikasan.notification.monitor.mock;

import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.instance.model.SolrFileEventDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceRecordImpl;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.search.SearchResults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SchedulerJobInstanceServiceTestImpl implements SchedulerJobInstanceService {
    @Override
    public SchedulerJobInstanceRecord findById(String s) {
        return null;
    }

    @Override
    public SchedulerJobInstanceRecord findByContextIdJobNameChildContextName(String s, String s1, String s2) {
        return null;
    }

    @Override
    public void save(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {

    }

    @Override
    public void update(SchedulerJobInstance schedulerJobInstance) {

    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String s, int i, int i1, String s1, String s2) {
        return null;
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String s, int i, int i1, String s1, String s2) {
        List<SchedulerJobInstanceRecord> list = new ArrayList<>();

        SchedulerJobInstanceRecord record = new SolrSchedulerJobInstanceRecordImpl();

        FileEventDrivenJobInstance fileEventDrivenJobInstance = new SolrFileEventDrivenJobInstanceImpl();
        fileEventDrivenJobInstance.setCronExpression("0 0/1 05-23 ? * MON-SUN *");
        fileEventDrivenJobInstance.setJobName("job-1");
        fileEventDrivenJobInstance.setChildContextIds(Arrays.asList("context-instance-1"));
        fileEventDrivenJobInstance.setStatus(InstanceStatus.COMPLETE);

        record.setSchedulerJobInstance(fileEventDrivenJobInstance);

        list.add(record);

        SearchResults<SchedulerJobInstanceRecord> results = new SearchResultsImpl(list,1,100);
        return results;
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(SchedulerJobInstanceSearchFilter schedulerJobInstanceSearchFilter, int i, int i1, String s, String s1) {
        return null;
    }

    @Override
    public List<SchedulerJobInstance> initialiseSchedulerJobInstancesForContext(ContextInstance contextInstance) throws SchedulerJobInstanceInitialisationException {
        return null;
    }
}
