package org.ikasan.job.orchestration.context.util;

import static org.ikasan.job.orchestration.context.util.ScheduledContextRecordTestSearchResults.CONTEXT_NAME;

import java.util.ArrayList;
import java.util.List;

import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;

public class InternalEventDrivenJobTestSearchResults implements SearchResults<InternalEventDrivenJobRecord> {
    public static final String AGENT_NAME = "AgentName";

    private final int number;
    private boolean useContextName;

    public InternalEventDrivenJobTestSearchResults(int number) {
        this.number = number;
    }

    public InternalEventDrivenJobTestSearchResults(int number, boolean useContextName) {
        this.number = number;
        this.useContextName = useContextName;
    }

    @Override
    public List<InternalEventDrivenJobRecord> getResultList() {
        List<InternalEventDrivenJobRecord> results = new ArrayList<>();
        for (int i = 1; i < number + 1; i++) {
            results.add(new TestInternalEventDrivenJobRecordImpl(i, useContextName));
        }
        return results;
    }

    @Override
    public long getTotalNumberOfResults() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getQueryResponseTime() {
        throw new UnsupportedOperationException();
    }

    public static class TestInternalEventDrivenJobRecordImpl implements InternalEventDrivenJobRecord {

        private final String id;
        private final boolean useContextName;

        public TestInternalEventDrivenJobRecordImpl(int id, boolean useContextName) {
            this.id = String.valueOf(id);
            this.useContextName = useContextName;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getAgentName() {
            return useContextName ? AGENT_NAME + "-" + CONTEXT_NAME + id : AGENT_NAME + id;
        }

        @Override
        public void setAgentName(String agentName) {
        }

        @Override
        public String getJobName() {
            return "JobName" + id;
        }

        @Override
        public void setJobName(String jobName) {
        }

        @Override
        public String getContextId() {
            return "ContextId" + id;
        }

        @Override
        public void setContextId(String contextId) {
        }

        @Override
        public InternalEventDrivenJob getInternalEventDrivenJob() {
            InternalEventDrivenJobImpl internalEventDrivenJob = new InternalEventDrivenJobImpl();
            internalEventDrivenJob.setIdentifier(id);
            internalEventDrivenJob.setAgentName(useContextName ? AGENT_NAME + id + "-" + CONTEXT_NAME + id : AGENT_NAME + id);
            return internalEventDrivenJob;
        }

        @Override
        public void setInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {
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
    }
}
