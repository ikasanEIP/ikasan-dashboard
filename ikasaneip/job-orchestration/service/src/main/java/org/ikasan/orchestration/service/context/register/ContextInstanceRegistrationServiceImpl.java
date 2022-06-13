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
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.orchestration.service.context.ContextInstanceServiceBase;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.rest.agent.client.ContextInstancePublicationService;

public class ContextInstanceRegistrationServiceImpl extends ContextInstanceServiceBase implements ContextInstanceRegistrationService {
    private static final Log LOG = LogFactory.getLog(ContextInstanceRegistrationServiceImpl.class);

    public ContextInstanceRegistrationServiceImpl(String queueDirectory,
                                                  ScheduledContextInstanceService scheduledContextInstanceService,
                                                  SchedulerService schedulerService,
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
            schedulerService,
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


    public void deRegister(String contextName) {
        try {
            LOG.info(String.format("De registering context [%s]", contextName));
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
            if (contextMachine == null) {
                LOG.error(String.format("Could not find context machine for [%s]", contextName));
                throw new RuntimeException(String.format("Could not find context machine for [%s]", contextName));
            }

            ContextInstance instance = contextMachine.getContext();
            if (instance == null) {
                LOG.error(String.format("Could not find instance in ContextMachine for [%s]", contextName));
                throw new RuntimeException(String.format("Could not find instance in ContextMachine for [%s]", contextName));
            }

            saveContextInstance(instance, InstanceStatus.ENDED);
            ContextMachineCache.instance().remove(contextMachine);
            contextMachine.teardown();
        } catch (Exception e) {
            LOG.error(String.format("An error has occurred executing de registering job[%s]", e.getMessage()), e);
            throw new RuntimeException(e);
        }
    }

    public void register(String contextName) {
        try {
            LOG.info(String.format("Registering context [%s]", contextName));
            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findById(contextName);
            if (scheduledContextRecord == null) {
                LOG.error(String.format("Could not find scheduledContextRecord for [%s]", contextName));
                throw new RuntimeException(String.format("Could not find scheduledContextRecord for [%s]", contextName));
            }

            ContextTemplate context = objectMapper.readValue(objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextTemplateImpl.class);
            ContextInstanceImpl contextInstance = objectMapper.readValue(objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextInstanceImpl.class);
            initialiseContextMachine(context, contextInstance, true);

        } catch (Exception e) {
            LOG.error(String.format("An error has occurred executing registering job [%s]", e.getMessage()), e);
            throw new RuntimeException(e);
        }

    }
}
