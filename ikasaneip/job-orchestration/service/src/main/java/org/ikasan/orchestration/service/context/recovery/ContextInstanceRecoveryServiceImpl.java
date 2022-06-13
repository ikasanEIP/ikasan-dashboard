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
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker.outsideOfOperatingWindow;

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
                                              SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster) {
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
            schedulerJobStateChangeEventBroadcaster);
    }

    public void recoverInstances() {
        SearchResults<ScheduledContextInstanceRecord> contextInstanceRecords = scheduledContextInstanceService
            .getScheduledContextInstancesByStatus(List.of(InstanceStatus.WAITING, InstanceStatus.RUNNING));

        Map<String, List<ScheduledContextInstanceRecord>> records = new HashMap<>();
        for (ScheduledContextInstanceRecord scheduledContextInstanceRecord : contextInstanceRecords.getResultList()) {
            List<ScheduledContextInstanceRecord> scheduledContextInstanceRecords = records.get(scheduledContextInstanceRecord.getContextName());
            if (scheduledContextInstanceRecords == null) {
                records.put(scheduledContextInstanceRecord.getContextName(), new ArrayList<>(List.of(scheduledContextInstanceRecord)));
            } else {
                scheduledContextInstanceRecords.add(scheduledContextInstanceRecord);
            }
        }

        // TODO this represents an exception case  - there should never be more than one instance WAITING OR RUNNING
        List<ScheduledContextInstanceRecord> sorted = new ArrayList<>();
        for (String key : records.keySet()) {
            List<ScheduledContextInstanceRecord> mapRecords = records.get(key);
            // get the most recent based on created timestamp
            ScheduledContextInstanceRecord scheduledContextInstanceRecord = mapRecords.stream()
                .sorted(Comparator.comparing(ScheduledContextInstanceRecord::getTimestamp).reversed())
                .collect(Collectors.toList())
                .get(0);
            sorted.add(scheduledContextInstanceRecord);
        }

        LOG.info("Recovering instances for: "
            + sorted.stream().map(ScheduledContextInstanceRecord::getContextName).collect(Collectors.toList()));

        SearchResults<ScheduledContextRecord> scheduledContextRecords = (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

        Map<String, ScheduledContextInstanceRecord> instancesMap
            = sorted.stream().collect(Collectors.toMap(ScheduledContextInstanceRecord::getContextName, record -> record));

        Date now = new Date();
        for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {
            ContextTemplate context = scheduledContextRecord.getContext();
            ScheduledContextInstanceRecord scheduledContextInstanceRecord = instancesMap.get(scheduledContextRecord.getContextName());
            if (scheduledContextInstanceRecord != null) {
                if (outsideOfOperatingWindow(context.getTimeWindowStart(), context.getTimeWindowEnd(), now)) {
                    // do nothing outside of window
                    continue;
                }
                try {
                    ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                    LOG.info(String.format("Recovering instance [%s] id [%s]", contextInstance.getName(), contextInstance.getId()));
                    initialiseContextMachine(context, contextInstance, false);
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
                    this.contextParametersInstanceService, this.contextParametersUpdateService, this.jobLockCacheService, this.scheduledContextService,
                    scheduledContextRecord, this.schedulerJobInstanceService, this.contextInstanceStateChangeEventBroadcaster, this.schedulerJobStateChangeEventBroadcaster
                ));
            }
        }
    }
}
