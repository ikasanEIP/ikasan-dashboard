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

import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
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

import static org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker.withinOperatingWindow;

public class ContextInstanceRecoveryServiceImpl extends ContextInstanceServiceBase implements ContextInstanceRecoveryService {
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceRecoveryServiceImpl.class);

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
                                              ContextInstanceSchedulerService contextInstanceSchedulerService) {
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
            contextInstanceSchedulerService);
    }

    /**
     * Re-create an instances that should be running as at now.
     *
     * Currently, if the end window for a job has passed, or the start window is future to now
     * (which is common of 1 * * ... i.e. every minute) then instances are not brought back to life, which means
     * events gathered on the agents for old instances will cause issues on the scheduler.
     *
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

        // outdated statement - TODO this represents an exception case  - there should never be more than one instance WAITING OR RUNNING OR ERROR
        // @Mick @todo - Now, we CAN have more than one instance running that we need to re-hydrate, what strategy do you feel we should we adopt for this
        // could try to bring back all that have end time = 0 and/or all that have end trigger less than last update to Solr
        List<ScheduledContextInstanceRecord> newestInstancePerContextName = new ArrayList<>();
        for (String contextName : contextNameToInstances.keySet()) {
            List<ScheduledContextInstanceRecord> contextInstances = contextNameToInstances.get(contextName);
            // get the most recent based on created timestamp
            ScheduledContextInstanceRecord scheduledContextInstanceRecord = contextInstances.stream()
                .sorted(Comparator.comparing(ScheduledContextInstanceRecord::getTimestamp).reversed())
                .collect(Collectors.toList())
                .get(0);
            newestInstancePerContextName.add(scheduledContextInstanceRecord);
        }

        LOG.info("Recovering instances for: "
            + newestInstancePerContextName.stream().map(ScheduledContextInstanceRecord::getContextName).collect(Collectors.toList()));

        Map<String, ScheduledContextInstanceRecord> contextNameToInstanceMap
            = newestInstancePerContextName.stream().collect(Collectors.toMap(ScheduledContextInstanceRecord::getContextName, record -> record));
        SearchResults<ScheduledContextRecord> scheduledContextRecords = (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

        Date now = new Date();
        for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {
            ContextTemplate context = scheduledContextRecord.getContext();

            // We do not recover disabled contexts!
            if(context.isDisabled()) continue;

            ScheduledContextInstanceRecord scheduledContextInstanceRecord = contextNameToInstanceMap.get(scheduledContextRecord.getContextName());
            // if outside the operating window instances will be created when ContextInstanceRegistrationServiceImpl.register runs
            if (withinOperatingWindow(context.getTimeWindowStart(), context.getTimeWindowEnd(), now)) {
                if (scheduledContextInstanceRecord != null) {
                    try {
                        ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                        LOG.info(String.format("Recovering instance [%s] id [%s]", contextInstance.getName(), contextInstance.getId()));

                        if(!this.fallsWithinCronBlackoutWindows(contextInstance.getBlackoutWindowCronExpressions(), contextInstance.getTimezone())
                            && !this.fallsWithinDateTimeBlackoutRanges(contextInstance.getBlackoutWindowDateTimeRanges(), contextInstance.getTimezone())) {
                            initialiseContextMachine(context, contextInstance, false);
                            contextInstanceSchedulerService.registerEndJobAndTrigger(contextInstance.getName(), contextInstance.getTimeWindowEnd(), contextInstance.getTimezone(), contextInstance.getId());
                        }
                        else {
                            LOG.info(String.format("ContextTemplate [%s] falls withing a blackout time window and will not be registered!", context.getName()));
                        }

                    } catch (Exception e) {
                        // todo probably want to send a notification here.
                        LOG.error(String.format("An error has occurred recovering context instance [%s]!", scheduledContextInstanceRecord.getContextName()), e);
                    }
                } else {
                    // we have a context record without an instance which should not be the case
                    String message = String.format("Context [%s] does not have an instance. Creating instance now!", scheduledContextRecord.getContextName());
                    LOG.info(message);
                    executor.execute(new MissingContextInstanceRecoveryRunnable(
                        this.queueDirectory, this.scheduledContextInstanceService, this.jobInitiationService, this.moduleMetadataService, this.internalEventDrivenJobService,
                        this.contextParametersInstanceService, this.contextInstancePublicationService, this.jobLockCacheService, this.scheduledContextService,
                        scheduledContextRecord, this.schedulerJobInstanceService, this.contextInstanceStateChangeEventBroadcaster, this.schedulerJobStateChangeEventBroadcaster,
                        this.jobLockCacheInitialisationService, this.contextInstanceSchedulerService
                    ));
                }
            }
        }
    }
}
