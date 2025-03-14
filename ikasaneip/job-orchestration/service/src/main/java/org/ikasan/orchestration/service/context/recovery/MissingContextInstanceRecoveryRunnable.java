package org.ikasan.orchestration.service.context.recovery;

import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.orchestration.service.context.ContextInstanceServiceBase;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class MissingContextInstanceRecoveryRunnable extends ContextInstanceServiceBase implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MissingContextInstanceRecoveryRunnable.class);

    private final ScheduledContextRecord scheduledContextRecord;

    private final ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;

    /**
     * Runnable implementation for recovering missing context instances.
     *
     * @param queueDirectory the directory where queue is located
     * @param scheduledContextInstanceService service for scheduled context instances
     * @param jobInitiationService service for job initiation
     * @param moduleMetadataService service for module metadata
     * @param internalEventDrivenJobService service for internal event-driven jobs
     * @param contextParametersInstanceService service for context parameters instances
     * @param contextInstancePublicationService service for context instance publication
     * @param jobLockCacheService service for job lock cache
     * @param scheduledContextService service for scheduled context
     * @param scheduledContextRecord the record of the scheduled context
     * @param schedulerJobInstanceService service for scheduler job instances
     * @param contextInstanceStateChangeEventBroadcaster broadcaster for context instance state change events
     * @param schedulerJobStateChangeEventBroadcaster broadcaster for scheduler job state change events
     * @param jobLockCacheInitialisationService service for job lock cache initialization
     * @param contextInstanceSchedulerService service for context instance scheduling
     * @param timeService service for handling time-related operations
     * @param jobUtilsService service for job utilities
     */
    public MissingContextInstanceRecoveryRunnable(String queueDirectory,
                                                  ScheduledContextInstanceService scheduledContextInstanceService,
                                                  JobInitiationService jobInitiationService,
                                                  ModuleMetaDataService moduleMetadataService,
                                                  InternalEventDrivenJobService internalEventDrivenJobService,
                                                  ContextParametersInstanceService contextParametersInstanceService,
                                                  ContextInstancePublicationService contextInstancePublicationService,
                                                  JobLockCacheService jobLockCacheService,
                                                  ScheduledContextService scheduledContextService,
                                                  ScheduledContextRecord scheduledContextRecord,
                                                  SchedulerJobInstanceService schedulerJobInstanceService,
                                                  ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
                                                  SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
                                                  JobLockCacheInitialisationService jobLockCacheInitialisationService,
                                                  ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService,
                                                  TimeService timeService,
                                                  JobUtilsService jobUtilsService,
                                                  JobProvisionService jobProvisionService,
                                                  SchedulerJobService schedulerJobService) {
        super(queueDirectory,
            scheduledContextInstanceService,
            jobInitiationService, moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            jobLockCacheInitialisationService,
            timeService,
            jobUtilsService,
            jobProvisionService,
            schedulerJobService);

        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if (this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }

        this.scheduledContextRecord = scheduledContextRecord;
    }

    @Override
    public void run() {
        try {
            LOG.info(String.format("Back filling instance for context [%s]", scheduledContextRecord.getContextName()));

            ContextInstanceImpl contextInstance = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(this.scheduledContextRecord.getContext()), ContextInstanceImpl.class);
            Date now = timeService.getDateNow();
            if(!QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(contextInstance.getBlackoutWindowCronExpressions(), contextInstance.getTimezone(), now)
                && !QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(contextInstance.getBlackoutWindowDateTimeRanges(),now)) {
                initialiseContextMachine(scheduledContextRecord.getContext(), contextInstance, true, true,null);

                contextInstanceSchedulerService.registerEndJobAndTrigger(contextInstance.getName(), CronUtils.buildCronFromOriginal(contextInstance.getProjectedEndTime(), contextInstance.getTimezone())
                    , contextInstance.getTimezone(), contextInstance.getId());
            }
            else {
                LOG.info(String.format("ContextTemplate [%s] falls withing a blackout time window and will not be registered!"
                    , scheduledContextRecord.getContext().getName()));
            }

        } catch (Exception e) {
            // TODO hook in notification here
            LOG.error(String.format("Got error back filling context [%s]. Error: %s", this.scheduledContextRecord.getContextName(), e));
        }
    }
}
