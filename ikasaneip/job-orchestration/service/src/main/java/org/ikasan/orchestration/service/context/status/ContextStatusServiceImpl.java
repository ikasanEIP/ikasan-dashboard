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

package org.ikasan.orchestration.service.context.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.status.*;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.service.ContextStatusService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceStatus;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceStatusWrapper;
import org.ikasan.spec.scheduled.status.model.ContextMachineStatus;
import org.ikasan.spec.scheduled.status.model.ContextMachineStatusWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextStatusServiceImpl implements ContextStatusService<ContextMachine> {

    /**
     * Get that status of all instances from the Context machine
     * @param includePrepared - true to include prepared and not active context instance
     * @return ContextMachineStatusWrapperImpl in Json
     * @throws JsonProcessingException if issue transforming to Json
     */
    @Override
    public String getJsonContextMachineStatus(boolean includePrepared) throws JsonProcessingException {
        Map<String, ContextMachine> mapAllContextMachine = ContextMachineCache.instance().getContextInstanceByContextInstanceIdCache();
        ContextMachineStatusWrapper contextMachineStatusWrapper = new ContextMachineStatusWrapperImpl();
        List<ContextMachineStatus> contextMachineStatusList = new ArrayList<>();

        for (Map.Entry<String, ContextMachine> contextMachineEntry : mapAllContextMachine.entrySet()) {
            if(!includePrepared && contextMachineEntry.getValue().getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                continue;
            }
            ContextMachineStatusImpl status = new ContextMachineStatusImpl();
            status.setContextName(contextMachineEntry.getValue().getContext().getName());
            status.setContextInstanceId(contextMachineEntry.getValue().getContext().getId());
            status.setInstanceStatus(contextMachineEntry.getValue().getContext().getStatus());
            contextMachineStatusList.add(status);
        }

        contextMachineStatusWrapper.setContextMachineStatusList(contextMachineStatusList);

        ContextService contextService = new ContextService();
        String json = contextService.getContextMachineStatus(contextMachineStatusWrapper);
        // return empty string if json is null
        return (json == null) ? "" : json;
    }

    /**
     * Get the status of all jobs in the Context Machine
     * @param instanceStatus - optional value to search for a particular instance status. Set to null to return everything
     * @param mapAllContextMachine - option map of context machine to found the status on. Set to null or empty to return bring back all active instances
     * @return ContextJobInstanceStatusWrapperImpl in JSON
     * @throws JsonProcessingException if issue transforming to Json
     */
    @Override
    public String getJsonContextJobStatus(InstanceStatus instanceStatus, Map<String, ContextMachine> mapAllContextMachine) throws JsonProcessingException {
        if (mapAllContextMachine == null || mapAllContextMachine.isEmpty()) {
            // Return Empty
            return "";
        }
        ContextJobInstanceStatusWrapper contextJobInstanceStatusWrapper = new ContextJobInstanceStatusWrapperImpl();
        List<ContextJobInstanceStatus> contextJobInstanceStatusList = new ArrayList<>();

        for (Map.Entry<String, ContextMachine> contextMachineEntry : mapAllContextMachine.entrySet()) {
            if (!contextMachineEntry.getValue().getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                ContextJobInstanceStatus contextJobInstanceStatus = ContextHelper.getContextJobInstanceStatus(contextMachineEntry.getValue().getContext(), contextMachineEntry.getValue().getInternalEventDrivenJobInstancesMap());
                // Only keep job status based on instanceStatus
                if (instanceStatus != null) {
                    contextJobInstanceStatus.getJobDetails().removeIf(list -> !list.getInstanceStatus().equals(instanceStatus));
                }
                contextJobInstanceStatusList.add(contextJobInstanceStatus);
            }
        }

        contextJobInstanceStatusWrapper.setJobPlans(contextJobInstanceStatusList);

        ContextService contextService = new ContextService();
        String json = contextService.getContextJobInstanceStatus(contextJobInstanceStatusWrapper);
        // return empty string if json is null
        return (json == null) ? "" : json;
    }

    @Override
    public String getContextStatus(String instanceName, String contextName) {
        Map<String, String> contextInstanceStatusMap = new HashMap<>();
        List<ContextMachine> contextMachineList = ContextMachineCache.instance().getAllRunningByContextName(instanceName);
        validateContextMachine(contextMachineList, instanceName);
        for (ContextMachine contextMachine : contextMachineList) {
            validateContextMachine(contextMachine, instanceName);
            InstanceStatus instanceStatus = contextMachine.getContextStatus(contextName);
            validateInstance(instanceStatus, instanceName, contextName, null);
            contextInstanceStatusMap.put(contextMachine.getContext().getId(), instanceStatus.name());
        }
        return contextInstanceStatusMap.toString();
    }

    @Override
    public String getContextStatusForJob(String instanceName, String contextName, String jobIdentifier) {
        Map<String, String> contextInstanceStatusMap = new HashMap<>();
        List<ContextMachine> contextMachineList = ContextMachineCache.instance().getAllRunningByContextName(instanceName);
        validateContextMachine(contextMachineList, instanceName);
        for (ContextMachine contextMachine : contextMachineList) {
            validateContextMachine(contextMachine, instanceName);
            InstanceStatus instanceStatus = contextMachine.getJobStatus(contextName, jobIdentifier);
            validateInstance(instanceStatus, instanceName, contextName, jobIdentifier);
            contextInstanceStatusMap.put(contextMachine.getContext().getId(), instanceStatus.name());
        }
        return contextInstanceStatusMap.toString();
    }

    /**
     * Get the status of an instance context or a child context in JSON format. Will not return any Prepared instances
     * @param instanceName Context name assigned to the ContextMachineCache
     * @param contextName Context or Child Context to search on
     * @return Mapped json representation of the current state of the Context Instance at the time of request
     * @throws JsonProcessingException if issue transforming to Json
     */
    @Override
    public String getJsonContextStatus(String instanceName, String contextName) throws JsonProcessingException {
        Map<String, ContextInstance> contextInstanceStatusMap = new HashMap<>();
        List<ContextMachine> contextMachineList = ContextMachineCache.instance().getAllRunningByContextName(instanceName);
        validateContextMachine(contextMachineList, instanceName);
        for (ContextMachine contextMachine : contextMachineList) {
            validateContextMachine(contextMachine, instanceName);
            ContextInstance contextInstance = contextMachine.getContext(instanceName);
            validateContextInstance(contextInstance, instanceName);
            ContextInstance contextInstanceForStatus = ContextHelper.getChildContextInstance(contextName, contextInstance);
            validateContextInstance(contextInstanceForStatus, contextName);
            contextInstanceStatusMap.put(contextMachine.getContext().getId(), contextInstance);
        }

        ContextService contextService = new ContextService();
        String json = contextService.getContextInstanceString(contextInstanceStatusMap);
        // return empty string if json is null
        return (json == null) ? "" : json;
    }

    /**
     * Get the status of a Scheduler Job Instance in JSON format. Will not return any Prepared instances
     * @param instanceName Context name assigned to the ContextMachineCache
     * @param contextName Context or Child Context to search on
     * @param jobName the job name to find the state for
     * @return Mapped json representation of the current state of the Scheduler Job Instance at the time of request
     * @throws JsonProcessingException if issue transforming to Json
     */
    @Override
    public String getJsonContextStatusForJob(String instanceName, String contextName, String jobName) throws JsonProcessingException {
        Map<String, SchedulerJobInstance> schedulerJobInstanceMap = new HashMap<>();
        List<ContextMachine> contextMachineList = ContextMachineCache.instance().getAllRunningByContextName(instanceName);
        validateContextMachine(contextMachineList, instanceName);
        for (ContextMachine contextMachine : contextMachineList) {
            validateContextMachine(contextMachine, instanceName);
            ContextInstance contextInstance = contextMachine.getContext(instanceName);
            validateContextInstance(contextInstance, instanceName);
            SchedulerJobInstance schedulerJobInstance = ContextHelper.getSchedulerJobInstance(jobName, contextName, contextInstance);
            validateSchedulerJobInstance(schedulerJobInstance, instanceName, contextName, jobName);
            schedulerJobInstanceMap.put(contextMachine.getContext().getId(), schedulerJobInstance);
        }

        ContextService contextService = new ContextService();
        String json = contextService.getSchedulerJobInstance(schedulerJobInstanceMap);
        // return empty string if json is null
        return (json == null) ? "" : json;

    }

    private void validateInstance(InstanceStatus instanceStatus, String instanceName, String contextName, String jobIdentifier) {
        if (instanceStatus == null) {
            String errorMessage = jobIdentifier == null
                ? String.format("Could not find context %s in context machine %s", contextName, instanceName)
                : String.format("Could not find job identifier %s for context %s in context machine %s", jobIdentifier, contextName, instanceName);
            throw new ContextStatusServiceException(errorMessage);
        }
    }

    private void validateContextMachine(List<ContextMachine> contextMachineList, String instanceName) {
        if (contextMachineList == null || contextMachineList.isEmpty()) {
            throw new ContextStatusServiceException(String.format("Could not find context machine for instance %s", instanceName));
        }
    }

    private void validateContextMachine(ContextMachine contextMachine, String instanceName) {
        if (contextMachine == null) {
            throw new ContextStatusServiceException(String.format("Could not find context machine for instance %s", instanceName));
        }
    }

    private void validateContextInstance(ContextInstance contextInstance, String contextName) {
        if (contextInstance == null) {
            throw new ContextStatusServiceException(String.format("Could not find context instance for context %s", contextName));
        }
    }

    private void validateSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance, String instanceName, String contextName, String jobIdentifier) {
        if (schedulerJobInstance == null) {
            String errorMessage = jobIdentifier == null
                ? String.format("Could not find context %s in context machine %s", contextName, instanceName)
                : String.format("Could not find job identifier %s for context %s in context machine %s", jobIdentifier, contextName, instanceName);
            throw new ContextStatusServiceException(errorMessage);
        }
    }
}
