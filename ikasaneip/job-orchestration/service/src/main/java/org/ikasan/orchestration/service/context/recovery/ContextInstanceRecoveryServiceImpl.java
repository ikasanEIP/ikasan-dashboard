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
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.orchestration.service.context.ContextInstanceServiceBase;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
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
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
public class ContextInstanceRecoveryServiceImpl extends ContextInstanceServiceBase implements ContextInstanceRecoveryService {
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceRecoveryServiceImpl.class);
    private static final long FORTY_EIGHT_HOURS_IN_MILLIS = 172800000;

    private final ExecutorService executor = Executors.newCachedThreadPool();


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
                                              ContextInstanceSchedulerService contextInstanceSchedulerService,
                                              TimeService timeService) {
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
            contextInstanceSchedulerService,
            timeService);

    }

    /**
     * Re-create an instances that should be running as at now.
     * Currently, if the end window for a job has passed, or the start window is future to now
     * (which is common of 1 * * ... i.e. every minute) then instances are not brought back to life, which means
     * events gathered on the agents for old instances will cause issues on the scheduler.
     * Likewise, if the dashboard has been down until after the plan has ended, all the status information will be lost.
     */
    public void recoverInstances() {
        SearchResults<ScheduledContextInstanceRecord> contextInstanceRecords = scheduledContextInstanceService
            .getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING, InstanceStatus.ERROR));

        Map<String, List<ScheduledContextInstanceRecord>> contextNameToInstances = new HashMap<>();

        for (ScheduledContextInstanceRecord scheduledContextInstanceRecord : contextInstanceRecords.getResultList()) {
            List<ScheduledContextInstanceRecord> scheduledContextInstanceRecords = contextNameToInstances.get(scheduledContextInstanceRecord.getContextName());
            if (scheduledContextInstanceRecords == null) {
                contextNameToInstances.put(scheduledContextInstanceRecord.getContextName(), new ArrayList<>(List.of(scheduledContextInstanceRecord)));
            } else {
                scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);
            }
        }

        long cutoffTime = System.currentTimeMillis() - FORTY_EIGHT_HOURS_IN_MILLIS;

        // @todo This is a temporary measure to reduce the 'noise' when ressurecting instances upon a dashboard recovery
        // The real fix here is to set the end time for the instance, and use that when the deashboard recovers to see if the
        // end time is not breached, agreed with Mick this will be done in the next Jira.
        List<ScheduledContextInstanceRecord> newestInstancePerContextName = new ArrayList<>();
        for (String contextName : contextNameToInstances.keySet()) {
            List<ScheduledContextInstanceRecord> contextInstances = contextNameToInstances.get(contextName);
            // get the most recent based on created timestamp
            ScheduledContextInstanceRecord recentContextInstances = contextInstances.stream()
                .filter(x -> x.getTimestamp() > cutoffTime)
                .sorted(Comparator.comparing(ScheduledContextInstanceRecord::getTimestamp).reversed())
                .collect(Collectors.toList())
                .get(0);
            newestInstancePerContextName.add(recentContextInstances);
        }

        Map<String, ScheduledContextInstanceRecord> contextNameToInstanceMap
            = newestInstancePerContextName.stream().collect(Collectors.toMap(ScheduledContextInstanceRecord::getContextName, record -> record));
        SearchResults<ScheduledContextRecord> scheduledContextRecords = (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

        Date now = timeService.getDateNow();
        for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {
            ContextTemplate context = scheduledContextRecord.getContext();

            // We do not recover disabled contexts!
            if(context.isDisabled()) {
                Log.info("Not Recovering context " + scheduledContextRecord.getContextName() + " instance ID " + scheduledContextRecord.getId() + " because the context is disabled");
            } else {
                ScheduledContextInstanceRecord scheduledContextInstanceRecord = contextNameToInstanceMap.get(scheduledContextRecord.getContextName());
                // if outside the operating window instances will be created when ContextInstanceRegistrationServiceImpl.register is triggered
                if (QuartzTimeWindowChecker.withinOperatingWindow(context.getTimezone(), context.getTimeWindowStart(), context.getTimeWindowEnd(), now)) {
                    if (scheduledContextInstanceRecord != null) {
                        try {
                            ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                            // @todo check with mick where the cron expressions are entered
                            if (!QuartzTimeWindowChecker.fallsWithinCronBlackoutWindows(contextInstance.getBlackoutWindowCronExpressions(), contextInstance.getTimezone(), now)
                                && !QuartzTimeWindowChecker.fallsWithinDateTimeBlackoutRanges(contextInstance.getBlackoutWindowDateTimeRanges(), now)) {
                                initialiseContextMachine(context, contextInstance, false);
                                contextInstanceSchedulerService.registerEndJobAndTrigger(contextInstance.getName(), contextInstance.getTimeWindowEnd(), contextInstance.getTimezone(), contextInstance.getId());
                                LOG.info(String.format("Recovering context [%s] instance id [%s]", contextInstance.getName(), contextInstance.getId()));
                            } else {
                                LOG.info(String.format("Not Recovering context [%s] instance ID [%s] falls withing a blackout time window and will not be registered!", contextInstance.getName(), contextInstance.getId()));
                            }

                        } catch (Exception e) {
                            // todo probably want to send a notification here.
                            LOG.error(String.format("Not Recovering context [%s] instance ID [%s] due to an ", scheduledContextInstanceRecord.getContextName(), scheduledContextInstanceRecord.getContextInstanceId()), e);
                        }
                    } else {
                        // we have a context record without an instance which should not be the case
                        String message = String.format("Recovering context [%s] does not have an instance. Creating instance now!", scheduledContextRecord.getContextName());
                        LOG.info(message);
                        executor.execute(new MissingContextInstanceRecoveryRunnable(
                            this.queueDirectory, this.scheduledContextInstanceService, this.jobInitiationService, this.moduleMetadataService, this.internalEventDrivenJobService,
                            this.contextParametersInstanceService, this.contextInstancePublicationService, this.jobLockCacheService, this.scheduledContextService,
                            scheduledContextRecord, this.schedulerJobInstanceService, this.contextInstanceStateChangeEventBroadcaster, this.schedulerJobStateChangeEventBroadcaster,
                            this.jobLockCacheInitialisationService, this.contextInstanceSchedulerService, this.timeService
                        ));
                    }
                } else {
                    Log.info("Not Recovering context " + scheduledContextRecord.getContextName() + " instance ID " + scheduledContextRecord.getId() + " because we are now outside it time window");
                }
            }
        }
    }
}
