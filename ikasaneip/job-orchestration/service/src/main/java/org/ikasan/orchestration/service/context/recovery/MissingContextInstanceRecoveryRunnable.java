package org.ikasan.orchestration.service.context.recovery;

import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
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
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class MissingContextInstanceRecoveryRunnable extends ContextInstanceServiceBase implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(MissingContextInstanceRecoveryRunnable.class);

    private final ScheduledContextRecord scheduledContextRecord;

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
                                                  ContextInstanceSchedulerService contextInstanceSchedulerService,
                                                  TimeService timeService) {
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
            contextInstanceSchedulerService,
            timeService);

        this.scheduledContextRecord = scheduledContextRecord;
    }

    @Override
    public void run() {
        try {
            LOG.info(String.format("Back filling instance for context [%s]", scheduledContextRecord.getContextName()));

            ContextInstanceImpl contextInstance = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(this.scheduledContextRecord.getContext()), ContextInstanceImpl.class);
            Date now = timeService.getDateNow();
            // @todo check with mick where the cron expressions are entered
            if(!QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(contextInstance.getBlackoutWindowCronExpressions(), contextInstance.getTimezone(), now)
                && !QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(contextInstance.getBlackoutWindowDateTimeRanges(),now)) {
                initialiseContextMachine(scheduledContextRecord.getContext(), contextInstance, true, null);

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
