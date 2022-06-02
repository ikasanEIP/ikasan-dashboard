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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.orchestration.service.context.ContextInstanceHelperService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;

public class ContextInstanceRegistrationServiceImpl extends ContextInstanceHelperService implements ContextInstanceRegistrationService {
    private static final Log LOG = LogFactory.getLog(ContextInstanceRegistrationServiceImpl.class);

    public ContextInstanceRegistrationServiceImpl(String queueDirectory,
                                                  ScheduledContextInstanceService scheduledContextInstanceService,
                                                  SchedulerService schedulerService,
                                                  ModuleMetaDataService moduleMetadataService,
                                                  InternalEventDrivenJobService internalEventDrivenJobService,
                                                  ContextParametersInstanceService contextParametersInstanceService,
                                                  ContextParametersUpdateService contextParametersUpdateService,
                                                  JobLockCacheService jobLockCacheService,
                                                  ScheduledContextService scheduledContextService,
                                                  SchedulerJobInstanceService schedulerJobInstanceService) {
        super(queueDirectory,
            scheduledContextInstanceService,
            schedulerService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService);
    }


    public void deRegister(String contextName) {
        try {
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
            if (contextMachine == null) {
                LOG.error("Could not find context machine for " + contextName);
                throw new RuntimeException("Could not find context machine for " + contextName);
            }

            ContextInstance instance = contextMachine.getContext();
            if (instance == null) {
                LOG.error("Could not find instance in ContextMachine for " + contextName);
                throw new RuntimeException("Could not find instance in ContextMachine for " + contextName);
            }

            saveContextInstance(instance, InstanceStatus.ENDED);
            ContextMachineCache.instance().remove(contextMachine);
        } catch (Exception e) {
            LOG.error(String.format("An error has occurred executing de registering job[%s]", e.getMessage()), e);
            throw new RuntimeException(e);
        }
    }

    public void register(String contextName) {
        try {
            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findById(contextName);
            if (scheduledContextRecord == null) {
                LOG.error("Could not find scheduledContextRecord for " + contextName);
                throw new RuntimeException("Could not find scheduledContextRecord for " + contextName);
            }
            ContextTemplate context = objectMapper.readValue(objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextTemplateImpl.class);
            ContextInstanceImpl contextInstance = objectMapper.readValue(objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextInstanceImpl.class);

            initialiseSchedulerJobInstancesForContext(contextInstance);

            Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = getInternalJobs(contextName);
            HashMap<String, ModuleMetaData> agents = getAgents(internalEventDrivenJobMap);

            // if we are creating new context we add the all the locks
            JobLockCache jobLockCache = JobLockCacheImpl.instance();
            jobLockCache.setJobLockCacheService(jobLockCacheService);
            jobLockCache.addLocks(context.getAllNestedJobLocks());

            ContextMachine contextMachine = new ContextMachine(context, contextInstance, this.scheduledContextInstanceService, internalEventDrivenJobMap,
                this.queueDirectory, agents, jobLockCache, this.contextParametersInstanceService);

            raiseEvent(contextMachine);

            populateParamsWithAgent(contextInstance, agents);

            addSchedulerJobStateChangeEventListener(contextMachine);

            ContextMachineCache.instance().put(contextMachine);
        } catch (Exception e) {
            LOG.error(String.format("An error has occurred executing registering job[%s]", e.getMessage()), e);
            throw new RuntimeException(e);
        }

    }
}
