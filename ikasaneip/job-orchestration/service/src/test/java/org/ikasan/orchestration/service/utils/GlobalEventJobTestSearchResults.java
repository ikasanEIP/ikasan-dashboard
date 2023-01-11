package org.ikasan.orchestration.service.utils;

import org.ikasan.job.orchestration.model.instance.GlobalEventJobInstanceImpl;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.search.SearchResults;

import java.util.ArrayList;
import java.util.List;

import static org.ikasan.orchestration.service.utils.ScheduledContextRecordTestSearchResults.CONTEXT_NAME;

public class GlobalEventJobTestSearchResults implements SearchResults<SchedulerJobInstanceRecord> {

    public static final String AGENT_NAME = "AgentName";
    
    private int number;
    private boolean useContextName;
    private boolean skipJobs = false;
    private String identifier = null;

    public GlobalEventJobTestSearchResults(int number) {
        this.number = number;
    }

    public GlobalEventJobTestSearchResults(int number, boolean skipJobs, boolean useContextName, String identifier) {
        this.number = number;
        this.skipJobs = skipJobs;
        this.useContextName = useContextName;
        this.identifier = identifier;
    }

    public GlobalEventJobTestSearchResults(int number, boolean useContextName) {
        this.number = number;
        this.useContextName = useContextName;
    }
    
    
    @Override
    public List<SchedulerJobInstanceRecord> getResultList() {
        List<SchedulerJobInstanceRecord> results = new ArrayList<>();
        for (int i = 1; i < number + 1; i++) {
            results.add(new GlobalEventJobTestSearchResults.TestGlobalEventJobRecordImpl(this.identifier != null ? this.identifier : String.valueOf(i), useContextName, "", this.skipJobs));
        }
        return results;
    }

    @Override
    public long getTotalNumberOfResults() {
        return 0;
    }

    @Override
    public long getQueryResponseTime() {
        return 0;
    }

    public static class TestGlobalEventJobRecordImpl implements SchedulerJobInstanceRecord {

        private final String id;
        private final boolean useContextName;
        private final String type;
        private boolean skipJobs;

        public TestGlobalEventJobRecordImpl(String id, boolean useContextName, String type, boolean skipJobs) {
            this.id = id;
            this.useContextName = useContextName;
            this.type = type;
            this.skipJobs = skipJobs;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getJobName() {
            return null;
        }

        @Override
        public void setJobName(String jobName) {

        }

        @Override
        public String getContextName() {
            return null;
        }

        @Override
        public void setContextName(String contextName) {

        }

        @Override
        public String getChildContextName() {
            return null;
        }

        @Override
        public void setChildContextName(String childContextName) {

        }

        @Override
        public String getContextInstanceId() {
            return null;
        }

        @Override
        public void setContextInstanceId(String contextInstanceId) {

        }

        @Override
        public SchedulerJobInstance getSchedulerJobInstance() {
            GlobalEventJobInstance globalEventJob = new GlobalEventJobInstanceImpl();
            globalEventJob.setIdentifier(id);
            globalEventJob.setAgentName(useContextName ? AGENT_NAME + id + "-" + CONTEXT_NAME + id : AGENT_NAME + id);
            globalEventJob.setSkip(skipJobs);
            if(this.skipJobs) globalEventJob.setStatus(InstanceStatus.SKIPPED);
            else globalEventJob.setStatus(InstanceStatus.WAITING);
            globalEventJob.setChildContextName("CONTEXT-1616645609");
            return globalEventJob;
        }

        @Override
        public void setSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance) {

        }

        @Override
        public String getStatus() {
            if(this.skipJobs) return InstanceStatus.SKIPPED.toString();
            else return InstanceStatus.WAITING.toString();
        }

        @Override
        public void setStatus(String status) {

        }

        @Override
        public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {

        }

        @Override
        public boolean isTargetResidingContextOnly() {
            return false;
        }

        @Override
        public void setParticipatesInLock(boolean participatesInLock) {

        }

        @Override
        public boolean isParticipatesInLock() {
            return false;
        }

        @Override
        public long getTimestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public void setTimestamp(long timestamp) {
        }

        @Override
        public long getModifiedTimestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public void setModifiedTimestamp(long timestamp) {
        }

        @Override
        public String getModifiedBy() {
            return "TEST";
        }

        @Override
        public void setModifiedBy(String modifiedBy) {
        }

        @Override
        public String getManuallySubmittedBy() {
            return null;
        }

        @Override
        public void setManuallySubmittedBy(String manuallySubmittedBy) {

        }

        @Override
        public long getStartTime() {
            return 0;
        }

        @Override
        public void setStartTime(long endTime) {

        }

        @Override
        public long getEndTime() {
            return 0;
        }

        @Override
        public void setEndTime(long endTime) {

        }
    }
    
}
