package org.ikasan.orchestration.service.utils;

import org.apache.commons.lang3.StringUtils;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stub SchedulerJobInstanceService as the context instance Id is unknown when test are running, therefore when we
 * apply filters to find out what type of record to return (internal of global), it is possible during a test that 
 * the contextId is not known yet which means a Mock is not possible, so this class is used to Stub it.
 */
public class StubSchedulerJobInstanceServiceTestImpl implements SchedulerJobInstanceService {

    List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();
    
    @Override
    public SchedulerJobInstanceRecord findById(String id) {
        return null;
    }

    @Override
    public SchedulerJobInstanceRecord findByContextIdJobNameChildContextName(String uuid, String jobName, String childContextName) {
        return null;
    }

    @Override
    public void save(SchedulerJobInstanceRecord scheduledContextInstanceRecord) {
        schedulerJobInstanceRecords.add(scheduledContextInstanceRecord);
    }

    @Override
    public void save(List<SchedulerJobInstanceRecord> scheduledContextInstanceRecords) {
        schedulerJobInstanceRecords.addAll(scheduledContextInstanceRecords);
    }

    @Override
    public void update(SchedulerJobInstance schedulerJobInstance) {

    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String contextInstanceId, int limit, int offset, String sortField, String sortDirection) {
        return null;
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String contextName, int limit, int offset, String sortField, String sortDirection) {
        return null;
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(SchedulerJobInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        List<SchedulerJobInstanceRecord> returnRecords = new ArrayList<>();
        for(SchedulerJobInstanceRecord record :schedulerJobInstanceRecords) {
            // Search for filters provided - > ignore instanceId as for test it is unknown
            if (StringUtils.isNotBlank(filter.getJobType())) {
                if (StringUtils.equals(filter.getJobType(), JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE) && 
                    record.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
                    returnRecords.add(record);
                } else if (StringUtils.equals(filter.getJobType(), JobConstants.GLOBAL_EVENT_JOB_INSTANCE) &&
                    record.getSchedulerJobInstance() instanceof GlobalEventJobInstance) {
                    returnRecords.add(record);
                }
            }
        }
        return new SearchResultsStubImpl(returnRecords, returnRecords.size(), 100);
    }

    @Override
    public List<SchedulerJobInstance> initialiseSchedulerJobInstancesForContext(ContextTemplate contextTemplate, ContextInstance contextInstance
        , SchedulerJobInstancesInitialisationParameters parameters) throws SchedulerJobInstanceInitialisationException {
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

    public class SearchResultsStubImpl<T> implements SearchResults<T> {
        private List<T> results;
        private long totalNumberOfResults;
        private long queryResponseTime;

        public SearchResultsStubImpl(List<T> results, long totalNumberOfResults, long queryResponseTime) {
            this.results = results;
            this.totalNumberOfResults = totalNumberOfResults;
            this.queryResponseTime = queryResponseTime;
        }

        @Override
        public List<T> getResultList() {
            return this.results;
        }

        @Override
        public long getTotalNumberOfResults() {
            return this.totalNumberOfResults;
        }

        @Override
        public long getQueryResponseTime() {
            return this.queryResponseTime;
        }
    }
}
