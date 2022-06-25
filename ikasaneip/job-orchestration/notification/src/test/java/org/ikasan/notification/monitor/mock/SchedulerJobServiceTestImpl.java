package org.ikasan.notification.monitor.mock;

import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;

import java.util.List;

public class SchedulerJobServiceTestImpl implements SchedulerJobService {

    @Override
    public SearchResults findByAgent(String agent, int limit, int offset) {
        return null;
    }

    @Override
    public SchedulerJobRecord findById(String id) {
        return null;
    }

    @Override
    public SchedulerJobRecord findByContextIdAndJobName(String contextId, String jobName) {
        return new SchedulerJobRecordTestImpl();
    }

    @Override
    public SearchResults findByContext(String contextId, int limit, int offset) {
        return null;
    }

    @Override
    public SearchResults findByFilter(SchedulerJobSearchFilter filter, int limit, int offset, String sortColumn, String sortDirection) {
        return null;
    }

    @Override
    public void delete(SchedulerJobRecord record) {

    }

    @Override
    public void deleteByAgentName(String agentName) {

    }

    @Override
    public void deleteByContextName(String contextName) {

    }

    @Override
    public void save(List records) {

    }

    @Override
    public void saveFileEventDrivenJobRecord(FileEventDrivenJobRecord fileEventDrivenJobRecord) {

    }

    @Override
    public void saveInternalEventDrivenJobRecord(InternalEventDrivenJobRecord internalEventDrivenJobRecord) {

    }

    @Override
    public void saveQuartzScheduledJobRecord(QuartzScheduleDrivenJobRecord quartzScheduleDrivenJobRecord) {

    }

    @Override
    public void saveFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob) {

    }

    @Override
    public void saveInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {

    }

    @Override
    public void saveQuartzScheduledJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) {

    }

    @Override
    public void saveFileEventDrivenJobs(List quartzScheduleDrivenJobs) {

    }

    @Override
    public void saveQuartzScheduledJobs(List list) {

    }

    @Override
    public void saveInternalEventDrivenJobs(List quartzScheduleDrivenJobs) {

    }

    @Override
    public void saveQuartzScheduledJobRecords(List quartzScheduleDrivenJobRecord) {

    }

    @Override
    public void saveInternalEventDrivenJobRecords(List internalEventDrivenJobRecord) {

    }

    @Override
    public void saveFileEventDrivenJobRecords(List list) {

    }
}
