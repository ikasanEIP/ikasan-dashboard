package org.ikasan.notification.monitor.mock;

import org.apache.commons.lang3.time.DateUtils;
import org.ikasan.scheduled.event.model.SolrScheduledProcessEvent;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.instance.model.SolrFileEventDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrInternalEventDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceRecordImpl;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.search.SearchResults;

import java.util.*;

public class SchedulerJobInstanceServiceTestImpl implements SchedulerJobInstanceService {

    private String type;

    public void setType(String type) {
        this.type = type;
    }

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
    public void save(List<SchedulerJobInstanceRecord> scheduledContextInstanceRecords) {

    }

    @Override
    public void update(SchedulerJobInstance schedulerJobInstance) {

    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String s, int i, int i1, String s1, String s2) {
        return getSchedulerJobInstancesByContextName(s, i, i1, s1, s2);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String s, int i, int i1, String s1, String s2) {

        List<SchedulerJobInstanceRecord> list = new ArrayList<>();

        SchedulerJobInstanceRecord record = new SolrSchedulerJobInstanceRecordImpl();

        if (type.equalsIgnoreCase("file-notify")) {

            FileEventDrivenJobInstance fileEventDrivenJobInstance = new SolrFileEventDrivenJobInstanceImpl();
            fileEventDrivenJobInstance.setCronExpression("0 0/1 05-23 ? * MON-SUN *");
            fileEventDrivenJobInstance.setSlaCronExpression("0 0/1 05-23 ? * MON-SUN *");
            fileEventDrivenJobInstance.setJobName("job-1");
            fileEventDrivenJobInstance.setChildContextNames(Arrays.asList("context-instance-1"));
            fileEventDrivenJobInstance.setStatus(InstanceStatus.COMPLETE);

            record.setSchedulerJobInstance(fileEventDrivenJobInstance);

        } else if (type.equalsIgnoreCase("file-no-notify")) {

            FileEventDrivenJobInstance fileEventDrivenJobInstance = new SolrFileEventDrivenJobInstanceImpl();
            fileEventDrivenJobInstance.setSlaCronExpression("0 0/1 05-23 ? * MON-SUN *");
            fileEventDrivenJobInstance.setJobName("job-1");
            fileEventDrivenJobInstance.setChildContextNames(Arrays.asList("context-instance-1"));
            fileEventDrivenJobInstance.setStatus(InstanceStatus.COMPLETE);

            record.setSchedulerJobInstance(fileEventDrivenJobInstance);

        }
        else if (type.equalsIgnoreCase("internal")) {

            InternalEventDrivenJobInstance internalEventDrivenJobInstance = new SolrInternalEventDrivenJobInstanceImpl();
            internalEventDrivenJobInstance.setStatus(InstanceStatus.COMPLETE);

            ScheduledProcessEvent scheduledProcessEvent = new SolrScheduledProcessEvent();
            scheduledProcessEvent.setFireTime(DateUtils.addMinutes(new Date(), -15).getTime());
            scheduledProcessEvent.setCompletionTime(new Date().getTime());

            internalEventDrivenJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
            internalEventDrivenJobInstance.setChildContextNames(Arrays.asList("context-instance-1"));
            internalEventDrivenJobInstance.setJobName("job-1");

            record.setSchedulerJobInstance(internalEventDrivenJobInstance);
        }

        list.add(record);

        return new SearchResultsImpl(list, 1, 100);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(SchedulerJobInstanceSearchFilter schedulerJobInstanceSearchFilter, int i, int i1, String s, String s1) {
        return null;
    }

    @Override
    public List<SchedulerJobInstance> initialiseSchedulerJobInstancesForContext(ContextInstance contextInstance, SchedulerJobInstancesInitialisationParameters parameters) throws SchedulerJobInstanceInitialisationException {
        return null;
    }

    @Override
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstances(List<String> contextInstanceIds) {
        return null;
    }

    @Override
    public Map<String, InternalEventDrivenJobInstance> getCommandExecutionJobsForContextInstance(String contextInstanceId) {
        return null;
    }

    @Override
    public Map<String, InternalEventDrivenJobInstance> getCommandExecutionJobsForContextInstanceChildContext(String contextInstanceId) {
        return null;
    }

    @Override
    public List<SchedulerJobInstanceRecord> holdJobsWithinContext(ContextInstance contextInstance, String childContextName) {
        return null;
    }

    @Override
    public List<SchedulerJobInstanceRecord> getJobsToReleaseWithinContext(ContextInstance contextInstance, String childContextName) {
        return null;
    }

    @Override
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(List<String> contextInstanceIds) {
        return null;
    }

    @Override
    public void deleteSchedulerJobInstances(String contextInstanceId) {

    }
}
