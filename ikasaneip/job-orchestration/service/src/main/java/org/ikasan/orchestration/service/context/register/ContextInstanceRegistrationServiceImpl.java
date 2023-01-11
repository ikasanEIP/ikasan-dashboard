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
package org.ikasan.orchestration.service.context.register;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.orchestration.service.context.ContextInstanceServiceBase;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.quartz.JobExecutionContext;

public class ContextInstanceRegistrationServiceImpl extends ContextInstanceServiceBase implements ContextInstanceRegistrationService {
    private static final Log LOG = LogFactory.getLog(ContextInstanceRegistrationServiceImpl.class);

    public ContextInstanceRegistrationServiceImpl(String queueDirectory,
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
     * Remove the all contextInstance associated with this context name, all jobsDetails & triggers.
     * This will be invoked, for example, by the UI
     * @param contextName / plan for which we need to deregister.
     */
    @Override
    public void deRegisterByName(String contextName) {
        for(ContextMachine contextMachine : ContextMachineCache.instance().getAllByContextName(contextName)) {
            deRegisterById(contextMachine.getContext().getId(), null);
        }
        contextInstanceSchedulerService.removeJob(contextName);
    }

    /**
     * Remove the contextInstance associated with the context instance ID
     * This will be invoked from a plan end cron trigger
     * @param contextInstanceId / plan for which we need to deregister.
     */
    @Override
    public void deRegisterById(String contextInstanceId, JobExecutionContext context) {
        if (context != null) {
            contextInstanceSchedulerService.removeEndJobTrigger(context.getTrigger());
        }
        final ContextMachine contextMachine = ContextMachineCache.instance().getByContextInstanceId(contextInstanceId);
        if (contextMachine == null) {
            LOG.info(String.format("Could not find context machine for context Instance ID [%s], so therefore nothing to de-register.", contextInstanceId));
            return;
        }
        LOG.info(String.format("De registering context Instance ID [%s], plan name [%s]", contextInstanceId, contextMachine.getContext().getName()));

        final ContextInstance instance = contextMachine.getContext();
        if (instance == null) {
            String messages = String.format("Could not find instance in ContextMachine for context Instance ID [%s]", contextInstanceId);
            LOG.error(messages);
            throw new RuntimeException(messages);
        }

        removeAgentInstances(instance);
        saveContextInstance(instance, InstanceStatus.ENDED);
        super.jobLockCacheInitialisationService.removeJobLocksFromCache(instance);
        ContextMachineCache.instance().remove(contextMachine);
        try {
            contextMachine.teardown();
        } catch (Exception e) {
            LOG.error(String.format("An error has occurred executing de registering job[%s]", e.getMessage()), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a new instance of the plan and register it.
     * This will be invoked when a plan start trigger fires.
     * It will fire even if the plan is disabled, but will not create a new context.
     * @param contextName i.e. plan to create instance for
     */
    @Override
    public void register(String contextName) {
        ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findById(contextName);
        if (scheduledContextRecord == null) {
            final String message = String.format("Could not find scheduledContextRecord for context name [%s]", contextName);
            LOG.error(message);
            throw new RuntimeException(message);
        }

        if (scheduledContextRecord.isDisabled()) {
            LOG.info(String.format("Context name [%s] is disabled and will not be registered!", contextName));
            return;
        }

        LOG.info(String.format("Registering context [%s]", contextName));
        try {
            byte[] scheduledContextRecordContext = objectMapper.writeValueAsBytes(scheduledContextRecord.getContext());
            ContextTemplate context = objectMapper.readValue(scheduledContextRecordContext, ContextTemplateImpl.class);
            ContextInstanceImpl contextInstance = objectMapper.readValue(scheduledContextRecordContext, ContextInstanceImpl.class);
            if(!this.fallsWithinCronBlackoutWindows(contextInstance.getBlackoutWindowCronExpressions(), contextInstance.getTimezone())
                && !this.fallsWithinDateTimeBlackoutRanges(contextInstance.getBlackoutWindowDateTimeRanges(), contextInstance.getTimezone())) {
                initialiseContextMachine(context, contextInstance, true);
                contextInstanceSchedulerService.registerEndJobAndTrigger(contextInstance.getName(), contextInstance.getTimeWindowEnd(), contextInstance.getTimezone(), contextInstance.getId());
                LOG.info(String.format("Registering context instance [%s] for context [%s]", contextInstance.getId(), contextName));
            }
            else {
                LOG.info(String.format("Context name [%s] falls withing a blackout time window and will not be registered!", contextName));
            }

        } catch (Exception e) {
            LOG.error(String.format("An error has occurred executing registering job [%s]", e.getMessage()), e);
            throw new RuntimeException(e);
        }
    }
}
