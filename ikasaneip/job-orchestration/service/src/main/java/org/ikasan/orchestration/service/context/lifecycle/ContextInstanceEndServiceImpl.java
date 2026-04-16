package org.ikasan.orchestration.service.context.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.orchestration.service.context.register.ContextInstanceRegistrationServiceImpl;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceSchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
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
import org.ikasan.spec.systemevent.SystemEventService;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextInstanceEndServiceImpl extends ContextInstanceRegistrationServiceImpl implements ContextInstanceStateChangeEventBroadcastListener {
    private static final Log LOG = LogFactory.getLog(ContextInstanceEndServiceImpl.class);
    private final ContextInstanceSchedulerService contextInstanceSchedulerService;


    /**
     * Constructor for ContextInstanceEndServiceImpl class.
     *
     * @param queueDirectory the directory where the job queue is located
     * @param scheduledContextInstanceService service for interacting with scheduled context instances
     * @param jobInitiationService service for initiating jobs
     * @param moduleMetadataService service for module metadata
     * @param internalEventDrivenJobService service for internal event-driven jobs
     * @param contextParametersInstanceService service for context parameters instances
     * @param contextInstancePublicationService service for publishing context instances
     * @param jobLockCacheService service for caching job locks
     * @param scheduledContextService service for scheduled contexts
     * @param schedulerJobInstanceService service for scheduler job instances
     * @param jobLockCacheInitialisationService service for initializing job lock cache
     * @param timeService service for handling time-related operations
     * @param systemEventService service for system events
     * @param jobUtilsService service for job utility methods
     * @param jobProvisionService service for job provisioning
     * @param schedulerJobService service for scheduler jobs
     * @param contextInstanceSchedulerService service for scheduling context instances
     * @param isIkasanEnterpriseSchedulerInstance flag indicating if it's an Ikasan Enterprise Scheduler instance
     */
    public ContextInstanceEndServiceImpl(String queueDirectory, ScheduledContextInstanceService scheduledContextInstanceService
        , JobInitiationService jobInitiationService, ModuleMetaDataService moduleMetadataService
        , InternalEventDrivenJobService internalEventDrivenJobService, ContextParametersInstanceService contextParametersInstanceService
        , ContextInstancePublicationService contextInstancePublicationService, JobLockCacheService jobLockCacheService
        , ScheduledContextService scheduledContextService, SchedulerJobInstanceService schedulerJobInstanceService
        , JobLockCacheInitialisationService jobLockCacheInitialisationService, TimeService timeService
        , SystemEventService systemEventService
        , JobUtilsService jobUtilsService, JobProvisionService jobProvisionService, SchedulerJobService schedulerJobService
        , ContextInstanceSchedulerService contextInstanceSchedulerService, boolean isIkasanEnterpriseSchedulerInstance) {
        super(queueDirectory, scheduledContextInstanceService, jobInitiationService, moduleMetadataService, internalEventDrivenJobService
            , contextParametersInstanceService, contextInstancePublicationService, jobLockCacheService, scheduledContextService
            , schedulerJobInstanceService, jobLockCacheInitialisationService, timeService, systemEventService, jobUtilsService
            , jobProvisionService, schedulerJobService, isIkasanEnterpriseSchedulerInstance);

        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if(this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }

        ContextInstanceStateChangeEventBroadcaster.register(this);
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if(event.getContextInstance().isEndJobPlanUponCompletion()
            && !event.getContextInstance().isRunContextUntilManuallyEnded()
            && event.getNewStatus().equals(InstanceStatus.COMPLETE)) {
            try {
                LOG.info(String.format("Job Plan Instance[%s], with ID[%s] has been configured to END when the Job Plan Instance is COMPLETE. " +
                    "The Job Plan Instance is now COMPLETE and will be ended.", event.getContextInstance().getName(), event.getContextInstanceId()));
                super.deRegisterById(event.getContextInstanceId());
                LOG.info(String.format("Job Plan Instance[%s], with ID[%s] has ENDED successfully!", event.getContextInstance().getName(), event.getContextInstanceId()));

                List<ContextInstance> preparedInstances
                    = super.findPrepared(event.getContextInstance().getName());

                AtomicBoolean preparedInPast = new AtomicBoolean(true);
                preparedInstances.forEach(instance -> {
                    if (instance.getStartTime() > System.currentTimeMillis()) {
                        preparedInPast.set(false);
                    }
                });

                if (preparedInPast.get() == true) {
                    LOG.info(String.format("Current PREPARED Job Plan Instance[%s] should already have been started. Starting this " +
                        "PREPARED job plan instance.", event.getContextInstance().getName()));
                    super.register(event.getContextInstance().getName(), this.contextInstanceSchedulerService);
                    LOG.info(String.format("Current PREPARED Job Plan Instance[%s] has been STARTED.", event.getContextInstance().getName()));

                }
            }
            catch (Exception e) {
                LOG.error(String.format("An exception has occurred ending Job Plan Instance[%s], with ID[%s] with the following error message[%s]!"
                    , event.getContextInstance().getName(), event.getContextInstanceId(), e.getMessage()), e);
            }
        }
    }
}
