/*
 * $Id$
 * $URL$
 *
 * ====================================================================
 * Ikasan Enterprise Integration Platform
 *
 * Distributed under the Modified BSD License.
 * Copyright notice: The copyright for this software and a full listing
 * of individual contributors are as shown in the packaged copyright.txt
 * file.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  - Neither the name of the ORGANIZATION nor the names of its contributors may
 *    be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 */
package org.ikasan.orchestration.service.context.recovery;

import com.esotericsoftware.minlog.Log;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.orchestration.service.context.ContextInstanceServiceBase;
import org.ikasan.orchestration.service.context.util.JobServiceThreadFactory;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
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
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class ContextInstanceRecoveryServiceImpl extends ContextInstanceServiceBase implements ContextInstanceRecoveryService {
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceRecoveryServiceImpl.class);

    private final ExecutorService executor = Executors.newCachedThreadPool(new JobServiceThreadFactory("ContextInstanceRecoveryServiceImpl"));

    private final ContextInstanceRegistrationService contextInstanceRegistrationService;

    private final ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService;

    private boolean isIkasanEnterpriseSchedulerInstance;


    /**
     * Constructor
     *
     * @param queueDirectory
     * @param scheduledContextInstanceService
     * @param jobInitiationService
     * @param moduleMetadataService
     * @param internalEventDrivenJobService
     * @param contextParametersInstanceService
     * @param contextInstancePublicationService
     * @param jobLockCacheService
     * @param scheduledContextService
     * @param schedulerJobInstanceService
     * @param contextInstanceStateChangeEventBroadcaster
     * @param schedulerJobStateChangeEventBroadcaster
     * @param jobLockCacheInitialisationService
     * @param contextInstanceSchedulerService
     * @param timeService
     * @param contextInstanceRegistrationService
     */
    public ContextInstanceRecoveryServiceImpl(String queueDirectory,
                                              ScheduledContextInstanceService scheduledContextInstanceService,
                                              JobInitiationService jobInitiationService,
                                              ModuleMetaDataService moduleMetadataService,
                                              InternalEventDrivenJobService internalEventDrivenJobService,
                                              ContextParametersInstanceService contextParametersInstanceService,
                                              ContextInstancePublicationService contextInstancePublicationService,
                                              JobLockCacheService jobLockCacheService,
                                              ScheduledContextService scheduledContextService,
                                              SchedulerJobInstanceService schedulerJobInstanceService,
                                              ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
                                              SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
                                              JobLockCacheInitialisationService jobLockCacheInitialisationService,
                                              ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService,
                                              TimeService timeService,
                                              ContextInstanceRegistrationService contextInstanceRegistrationService,
                                              JobUtilsService jobUtilsService,
                                              JobProvisionService jobProvisionService,
                                              SchedulerJobService schedulerJobService,
                                              boolean isIkasanEnterpriseSchedulerInstance) {
        super(queueDirectory,
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
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

        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }

        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if (this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }

        this.isIkasanEnterpriseSchedulerInstance = isIkasanEnterpriseSchedulerInstance;
    }

    /**
     * Re-create an instances that should be running as at now.
     * Currently, if the end window for a job has passed, or the start window is future to now
     * (which is common of 1 * * ... i.e. every minute) then instances are not brought back to life, which means
     * events gathered on the agents for old instances will cause issues on the scheduler.
     * Likewise, if the dashboard has been down until after the plan has ended, all the status information will be lost.
     */
    public void recoverInstances() {
        if(!isIkasanEnterpriseSchedulerInstance) {
            LOG.warn("This instance of the dashboard is not configured to run as a scheduler, therefore no job plan instance recovery will run!");
            return;
        }
        SearchResults<ScheduledContextInstanceRecord> contextInstanceRecords = scheduledContextInstanceService
            .getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING, InstanceStatus.ERROR, InstanceStatus.COMPLETE));

        Map<String, List<ScheduledContextInstanceRecord>> contextNameToInstances = new HashMap<>();

        for (ScheduledContextInstanceRecord scheduledContextInstanceRecord : contextInstanceRecords.getResultList()) {
            List<ScheduledContextInstanceRecord> scheduledContextInstanceRecords = contextNameToInstances.get(scheduledContextInstanceRecord.getContextName());
            if (scheduledContextInstanceRecords == null) {
                contextNameToInstances.put(scheduledContextInstanceRecord.getContextName(), new ArrayList<>(List.of(scheduledContextInstanceRecord)));
            } else {
                scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);
            }
        }

        SearchResults<ScheduledContextRecord> scheduledContextRecords = (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

        Date now = timeService.getDateNow();
        super.removeAllContextInstancesFromAgent();
        for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {
            ContextTemplate context = scheduledContextRecord.getContext();

            // We do not recover disabled contexts!
            if(scheduledContextRecord.isDisabled()) {
                // Remove prepared context instances if they exist.
                super.removeAllPrepared(context.getName());
                Log.info("Not Recovering context " + scheduledContextRecord.getContextName() + " instance ID " + scheduledContextRecord.getId() + " because the context is disabled");
            }
            else {
                try {
                    List<ContextInstance> prepared = this.findPrepared(context.getName());
                    List<ContextInstance> future = new ArrayList<>();

                    for (ContextInstance contextInstance : prepared) {
                        if(contextInstance.isCustomWeekDayOfMonth() &&
                            contextInstance.getStartTime() < System.currentTimeMillis()) {
                            executor.execute(() -> {
                                try {
                                    initialiseContextMachine(scheduledContextRecord.getContext(), contextInstance, true, true, null);

                                    contextInstanceSchedulerService.registerEndJobAndTrigger(contextInstance.getName()
                                        , CronUtils.buildCronFromOriginal(contextInstance.getProjectedEndTime(), ZoneId.systemDefault().getId())
                                        , contextInstance.getTimezone(), contextInstance.getId());

                                    super.prepareFutureContextInstance(context.getName());
                                } catch (Exception e) {
                                    LOG.error(String.format("Got error back filling context [%s] with custom cron schedule [%s]. Error: %s"
                                        , scheduledContextRecord.getContextName(), scheduledContextRecord.getContext().getTimeWindowStart(), e));
                                }
                            });
                        }
                        else if (contextInstance.getStartTime() < System.currentTimeMillis()) {
                            super.removeContextInstance(contextInstance.getId());
                            if(contextNameToInstances.containsKey(contextInstance.getName())) {
                                contextNameToInstances.get(contextInstance.getName()).remove(contextInstance);
                            }
                        } else {
                            future.add(contextInstance);
                            super.prepareContextInstance(context, contextInstance, false);
                        }
                    }

                    if (future.isEmpty()) {
                        super.prepareFutureContextInstance(context.getName());
                    }
                }
                catch (Exception e) {
                    Log.warn(String.format("Could not recover prepared instance for context[%s]", context.getName()), e);
                }

                // if outside the operating window instances will be created when ContextInstanceRegistrationServiceImpl.register is triggered
                List<ScheduledContextInstanceRecord> scheduledContextInstanceRecords = contextNameToInstances.get(context.getName());

                if (scheduledContextInstanceRecords != null) {
                    for (ScheduledContextInstanceRecord scheduledContextInstanceRecord : scheduledContextInstanceRecords) {
                        try {
                            if((scheduledContextInstanceRecord.getContextInstance().getProjectedEndTime() == 0 || scheduledContextInstanceRecord.getContextInstance().getProjectedEndTime() < System.currentTimeMillis())
                                && !scheduledContextInstanceRecord.getContextInstance().isRunContextUntilManuallyEnded()) {
                                LOG.info("Removing context instance[{}], with name[{}] as the projected end time has been passed and the context is not marked to run until manually ended.",
                                    scheduledContextInstanceRecord.getContextInstanceId(), scheduledContextInstanceRecord.getContextName());
                                removeAgentInstances(scheduledContextInstanceRecord.getContextInstance());
                                ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                                contextInstance.setEndTime(System.currentTimeMillis());
                                saveContextInstance(contextInstance, InstanceStatus.ENDED);
                            }
                            else if (QuartzTimeWindowChecker.withinOperatingWindowOnRecovery(scheduledContextInstanceRecord.getContextInstance().getStartTime(), scheduledContextInstanceRecord.getContextInstance().getProjectedEndTime(), System.currentTimeMillis())
                                || (scheduledContextInstanceRecord.getContextInstance().isRunContextUntilManuallyEnded())) {
                                if (scheduledContextInstanceRecord != null) {
                                    try {
                                        ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                                        // @todo check with mick where the cron expressions are entered
                                        if (!QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(contextInstance.getBlackoutWindowCronExpressions(), contextInstance.getTimezone(), now)
                                            && !QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(contextInstance.getBlackoutWindowDateTimeRanges(), now)) {
                                            initialiseContextMachine(context, contextInstance, false, false, null);
                                            if (!contextInstance.isRunContextUntilManuallyEnded()) {
                                                contextInstanceSchedulerService.registerEndJobAndTrigger(contextInstance.getName()
                                                    , CronUtils.buildCronFromOriginal(contextInstance.getProjectedEndTime(), contextInstance.getTimezone())
                                                    , contextInstance.getTimezone(), contextInstance.getId());
                                                LOG.info(String.format("Recovering context [%s] instance id [%s]", contextInstance.getName(), contextInstance.getId()));
                                            }
                                        } else {
                                            LOG.info(String.format("Not Recovering. Job Plan [%s] instance ID [%s] falls within a blackout time window and will not be registered!", contextInstance.getName(), contextInstance.getId()));
                                            removeAgentInstances(contextInstance);
                                            contextInstance.setEndTime(System.currentTimeMillis());
                                            saveContextInstance(contextInstance, InstanceStatus.ENDED);
                                        }

                                    } catch (Exception e) {
                                        // todo probably want to send a notification here.
                                        LOG.error(String.format("Removing Job Plan [%s] instance ID [%s] due to an issue that makes it unrecoverable: ", scheduledContextInstanceRecord.getContextName(), scheduledContextInstanceRecord.getContextInstanceId()), e);
                                        removeAgentInstances(scheduledContextInstanceRecord.getContextInstance());
                                        ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                                        contextInstance.setEndTime(System.currentTimeMillis());
                                        saveContextInstance(contextInstance, InstanceStatus.ENDED);
                                    }
                                }
                            } else {
                                Log.info("Not Recovering context " + scheduledContextRecord.getContextName() + " instance ID " + scheduledContextRecord.getId() + " because we are now outside it time window. Ending it");
                                removeAgentInstances(scheduledContextInstanceRecord.getContextInstance());
                                ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                                contextInstance.setEndTime(System.currentTimeMillis());
                                saveContextInstance(contextInstance, InstanceStatus.ENDED);
                            }
                        } catch (Exception e) {
                            // todo probably want to send a notification here.
                            e.printStackTrace();
                            if(scheduledContextInstanceRecord != null) {
                                LOG.error(String.format("Not Recovering context [%s] instance ID [%s] due an issue with the definition of the cron expression for the time windows. Ending it"
                                    , scheduledContextInstanceRecord.getContextName(), scheduledContextInstanceRecord.getContextInstanceId()), e);
                                removeAgentInstances(scheduledContextInstanceRecord.getContextInstance());
                                ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                                contextInstance.setEndTime(System.currentTimeMillis());
                                saveContextInstance(contextInstance, InstanceStatus.ENDED);
                            }
                        }
                    }
                }
                else if (QuartzTimeWindowChecker.withinOperatingWindow(context.getTimezone(), context.getTimeWindowStart(), context.getContextTtlMilliseconds(), now)) {
                    // we have a context record without an instance which should not be the case
                    String message = String.format("Recovering context [%s] does not have an instance. Creating instance now!", scheduledContextRecord.getContextName());
                    LOG.info(message);
                    MissingContextInstanceRecoveryRunnable missingContextInstanceRecoveryRunnable = new MissingContextInstanceRecoveryRunnable(
                        this.queueDirectory, this.scheduledContextInstanceService, this.jobInitiationService, this.moduleMetadataService, this.internalEventDrivenJobService,
                        this.contextParametersInstanceService, this.contextInstancePublicationService, this.jobLockCacheService, this.scheduledContextService,
                        scheduledContextRecord, this.schedulerJobInstanceService, this.contextInstanceStateChangeEventBroadcaster, this.schedulerJobStateChangeEventBroadcaster,
                        this.jobLockCacheInitialisationService, this.contextInstanceSchedulerService, this.timeService, this.jobUtilsService, this.jobProvisionService,
                        this.schedulerJobService);
                    missingContextInstanceRecoveryRunnable.setContextMachineExecutorWaitTimeoutSeconds(super.contextMachineExecutorWaitTimeoutSeconds);
                    executor.execute(missingContextInstanceRecoveryRunnable);
                }
            }
        }
    }
}
