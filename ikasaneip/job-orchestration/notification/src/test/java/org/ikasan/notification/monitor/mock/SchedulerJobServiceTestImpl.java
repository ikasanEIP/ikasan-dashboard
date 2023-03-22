package org.ikasan.notification.monitor.mock;

import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SchedulerJobServiceTestImpl implements SchedulerJobService {

    private String type;

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public SearchResults findByAgent(String agent, int limit, int offset) {
        return null;
    }

    @Override
    public SchedulerJobRecord findById(String id) {
        return null;
    }

    @Override
    public SchedulerJobRecord findByContextNameAndJobName(String contextId, String jobName) {
        return new SchedulerJobRecordTestImpl();
    }

    @Override
    public SearchResults findByContext(String contextId, int limit, int offset) {

        if (type.equalsIgnoreCase("file")) {
            List<SchedulerJobRecord> list = new ArrayList<>();
            list.add(new SchedulerJobRecordTestImpl());
            return new SearchResultsImpl(list,1,100);
        }
        else if (type.equalsIgnoreCase("internal")) {
            List<InternalEventDrivenJobRecord> list = new ArrayList<>();
            list.add(new InternalEventDrivenJobRecordTestImpl(0, 3));
            return new SearchResultsImpl(list,1,100);
        }

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
    public void save(List records, String actor) {

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
    public void saveGlobalEventJobRecord(GlobalEventJobRecord globalEventJobRecord) {

    }

    @Override
    public void saveFileEventDrivenJob(FileEventDrivenJob fileEventDrivenJob, String modifiedBy) {

    }

    @Override
    public void saveInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob, String modifiedBy) {

    }

    @Override
    public void saveQuartzScheduledJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob, String modifiedBy) {

    }

    @Override
    public void saveGlobalEventJob(GlobalEventJob globalEventJob, String modifiedBy) {

    }

    @Override
    public void saveFileEventDrivenJobs(List quartzScheduleDrivenJobs, String actor) {

    }

    @Override
    public void saveQuartzScheduledJobs(List list, String actor) {

    }

    @Override
    public void saveInternalEventDrivenJobs(List quartzScheduleDrivenJobs, String actor) {

    }

    @Override
    public void saveGlobalEventJobs(List list, String actor) {

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

    @Override
    public void saveGlobalEventJobRecords(List list) {

    }

    @Override
    public void skip(SchedulerJobRecord jobRecord, List childContextNames, String actor) {

    }

    @Override
    public void enable(SchedulerJobRecord jobRecord, ContextTemplate contextTemplate, String actor) {

    }

    @Override
    public void hold(SchedulerJobRecord jobRecord, List childContextNames, String actor) {

    }

    @Override
    public void release(SchedulerJobRecord jobRecord, String actor) {

    }

    @Override
    public void releaseAll(String contextName, String actor) {

    }

    @Override
    public void holdAll(String contextName, String actor) {

    }

    @Override
    public void enableAll(String contextName, String actor) {

    }

    @Override
    public void renameContextForJobs(String oldName, String newName, String actor) {

    }

    @Override
    public Map<String, InternalEventDrivenJob> getCommandExecutionJobsForContext(String contextName) {
        return null;
    }
}
