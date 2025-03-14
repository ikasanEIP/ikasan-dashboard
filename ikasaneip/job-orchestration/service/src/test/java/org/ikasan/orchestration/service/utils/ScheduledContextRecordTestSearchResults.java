package org.ikasan.orchestration.service.utils;

import java.util.ArrayList;
import java.util.List;

import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.search.SearchResults;

public class ScheduledContextRecordTestSearchResults<ANY> implements SearchResults<ScheduledContextRecord> {

    public static final String CONTEXT_NAME = "ContextName";
    private final int number;
    private final boolean outsideOfOperatingWindow;
    private boolean disabled = false;
    private boolean requiresSynchronisation = false;

    public ScheduledContextRecordTestSearchResults(int number, boolean outsideOfOperatingWindow) {
        this.number = number;
        this.outsideOfOperatingWindow = outsideOfOperatingWindow;
    }

    public ScheduledContextRecordTestSearchResults(int number, boolean outsideOfOperatingWindow, boolean disabled) {
        this.number = number;
        this.outsideOfOperatingWindow = outsideOfOperatingWindow;
        this.disabled = disabled;
    }

    public ScheduledContextRecordTestSearchResults(int number, boolean outsideOfOperatingWindow, boolean disabled,
                                                   boolean requiresSynchronisation) {
        this.number = number;
        this.outsideOfOperatingWindow = outsideOfOperatingWindow;
        this.disabled = disabled;
        this.requiresSynchronisation = requiresSynchronisation;
    }

    @Override
    public List<ScheduledContextRecord> getResultList() {
        List<ScheduledContextRecord> results = new ArrayList<>();
        for (int i = 1; i < number + 1; i++) {
            ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
            record.setContextName(CONTEXT_NAME + i);
            record.setDisabled(this.disabled);
            ContextTemplateImpl context = new ContextTemplateImpl();
            context.setDisabled(this.disabled);
            context.setName(CONTEXT_NAME + i);
            context.setDelayAgentSynchronisationUntilNextInstance(this.requiresSynchronisation);
            context.setRequiresAgentSynchronisation(this.requiresSynchronisation);
            if (outsideOfOperatingWindow) {
                context.setTimeWindowStart("59 59 23 ? * * *");
                context.setContextTtlMilliseconds(1L);
            } else {
                context.setTimeWindowStart("* * 0 ? * * *");
                context.setContextTtlMilliseconds(82800000L);
            }
            record.setContext(context);
            results.add(record);
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
}
