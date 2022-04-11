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

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.spec.scheduled.context.service.ContextStatusService;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class ContextStatusServiceImpl implements ContextStatusService {

    @Override
    public String getContextStatus(String instanceName, String contextName) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(instanceName);
        validateContextMachine(contextMachine, instanceName);

        InstanceStatus instanceStatus = contextMachine.getContextStatus(contextName);
        validateInstance(instanceStatus, instanceName, contextName, null);

        return instanceStatus.name();
    }

    @Override
    public String getContextStatusForJob(String instanceName, String contextName, String jobIdentifier) {
        ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(instanceName);
        validateContextMachine(contextMachine, instanceName);

        InstanceStatus instanceStatus = contextMachine.getJobStatus(contextName, jobIdentifier);
        validateInstance(instanceStatus, instanceName, contextName, jobIdentifier);

        return instanceStatus.name();
    }

    private void validateInstance(InstanceStatus instanceStatus, String instanceName, String contextName, String jobIdentifier) {
        if (instanceStatus == null) {
            String errorMessage = jobIdentifier == null
                ? String.format("Could not find context %s in context machine %s", contextName, instanceName)
                : String.format("Could not find job identifier %s for context %s in context machine %s", jobIdentifier, contextName, instanceName);
            throw new ContextStatusServiceException(errorMessage);
        }
    }

    private void validateContextMachine(ContextMachine contextMachine, String instanceName) {
        if (contextMachine == null) {
            throw new ContextStatusServiceException(String.format("Could not find context machine for instance %s", instanceName));
        }
    }
}
