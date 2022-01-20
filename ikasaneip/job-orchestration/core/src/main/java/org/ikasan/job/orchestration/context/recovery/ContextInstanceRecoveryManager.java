package org.ikasan.job.orchestration.context.recovery;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContextInstanceRecoveryManager {
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceRecoveryManager.class);

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ScheduledContextService scheduledContextService;
    private InternalEventDrivenJobService internalEventDrivenJobRecordService;
    private String queueDirectory;

    public ContextInstanceRecoveryManager(ScheduledContextInstanceService scheduledContextInstanceService, ScheduledContextService scheduledContextService,
                                          InternalEventDrivenJobService internalEventDrivenJobRecordService, String queueDirectory) {
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.internalEventDrivenJobRecordService = internalEventDrivenJobRecordService;
        if(this.internalEventDrivenJobRecordService == null) {
            throw new IllegalArgumentException("internalEventDrivenJobRecordService cannot be null!");
        }
        this.queueDirectory = queueDirectory;
        if(this.queueDirectory == null) {
            throw new IllegalArgumentException("queueDirectory cannot be null!");
        }
    }

    @PostConstruct
    public void recoverContextInstances() {
        logger.info("Recovering context instances!");

        SearchResults<ScheduledContextInstanceRecord> contextInstanceRecords = scheduledContextInstanceService
            .getScheduledContextInstancesByStatus(List.of(InstanceStatus.RUNNING));

        for(ScheduledContextInstanceRecord contextInstanceRecord: contextInstanceRecords.getResultList()) {
            try {
                ScheduledContextRecord contextRecord = this.scheduledContextService.findByName(contextInstanceRecord.getContextName());
                SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults
                    = this.internalEventDrivenJobRecordService.findByContext(contextInstanceRecord.getContextName(), -1, -1);

                Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
                    .map(internalEventDrivenJobRecord -> internalEventDrivenJobRecord.getInternalEventDrivenJob())
                    .collect(Collectors.toMap(InternalEventDrivenJob::getIdentifier, Function.identity()));

                ContextMachine contextMachine = new ContextMachine(contextRecord.getContext(), contextInstanceRecord.getContextInstance(),
                    this.scheduledContextInstanceService, internalEventDrivenJobMap, this.queueDirectory);

                ContextMachineCache.instance().put(contextMachine);
            }
            catch (Exception e) {
                // todo probably want to send a notification here.
                logger.error(String.format("An error has occurred recovering context instance[%s]!", contextInstanceRecord.getId()), e);
            }
        };
    }
}
