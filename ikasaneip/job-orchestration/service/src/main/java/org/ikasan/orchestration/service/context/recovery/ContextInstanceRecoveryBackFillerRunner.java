package org.ikasan.orchestration.service.context.recovery;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.orchestration.service.context.ContextInstanceHelperService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceRecoveryBackFillerRunner extends ContextInstanceHelperService implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceRecoveryBackFillerRunner.class);

    private final ScheduledContextRecord scheduledContextRecord;

    public ContextInstanceRecoveryBackFillerRunner(String queueDirectory,
                                                   ScheduledContextInstanceService scheduledContextInstanceService,
                                                   SchedulerService schedulerService,
                                                   ModuleMetaDataService moduleMetadataService,
                                                   InternalEventDrivenJobService internalEventDrivenJobService,
                                                   ContextParametersInstanceService contextParametersInstanceService,
                                                   ContextParametersUpdateService contextParametersUpdateService,
                                                   JobLockCacheService jobLockCacheService,
                                                   ScheduledContextService scheduledContextService,
                                                   ScheduledContextRecord scheduledContextRecord,
                                                   SchedulerJobInstanceService schedulerJobInstanceService,
                                                   ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
                                                   SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster) {
        super(queueDirectory,
            scheduledContextInstanceService,
            schedulerService, moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster);

        this.scheduledContextRecord = scheduledContextRecord;
    }

    @Override
    public void run() {
        try {
            LOG.info(String.format("Back filling instance for context [%s]", scheduledContextRecord.getContextName()));

            ContextInstanceImpl contextInstance = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(this.scheduledContextRecord.getContext()), ContextInstanceImpl.class);

            initialiseContextMachine(scheduledContextRecord.getContext(), contextInstance);

        } catch (Exception e) {
            // TODO hook in notification here
            LOG.error(String.format("Got error back filling context [%s]. Error: %s", this.scheduledContextRecord.getContextName(), e));
        }
    }
}
