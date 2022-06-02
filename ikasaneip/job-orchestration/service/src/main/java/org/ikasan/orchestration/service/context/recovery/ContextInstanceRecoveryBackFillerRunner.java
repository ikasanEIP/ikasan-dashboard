package org.ikasan.orchestration.service.context.recovery;

import java.util.HashMap;
import java.util.Map;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.orchestration.service.context.ContextInstanceHelperService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceRecoveryBackFillerRunner extends ContextInstanceHelperService implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceRecoveryBackFillerRunner.class);

    private final ScheduledContextRecord scheduledContextRecord;
    private final JobLockCache jobLockCache;

    public ContextInstanceRecoveryBackFillerRunner(String queueDirectory,
                                                   ScheduledContextInstanceService scheduledContextInstanceService,
                                                   SchedulerService schedulerService,
                                                   ModuleMetaDataService moduleMetadataService,
                                                   InternalEventDrivenJobService internalEventDrivenJobService,
                                                   ContextParametersInstanceService contextParametersInstanceService,
                                                   ContextParametersUpdateService contextParametersUpdateService,
                                                   JobLockCacheService jobLockCacheService,
                                                   ScheduledContextService scheduledContextService,
                                                   ScheduledContextRecord scheduledContextRecord,
                                                   JobLockCache jobLockCache,
                                                   SchedulerJobInstanceService schedulerJobInstanceService) {
        super(queueDirectory,
            scheduledContextInstanceService,
            schedulerService, moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextParametersUpdateService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService);

        this.scheduledContextRecord = scheduledContextRecord;
        this.jobLockCache = jobLockCache;
    }

    @Override
    public void run() {
        try {
            LOG.info("Back filling instance for context " + scheduledContextRecord.getContextName());

            ContextInstanceImpl contextInstance = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(this.scheduledContextRecord.getContext()), ContextInstanceImpl.class);

            initialiseSchedulerJobInstancesForContext(contextInstance);

            Map<String, InternalEventDrivenJob> internalJobs = getInternalJobs(this.scheduledContextRecord.getContextName());
            HashMap<String, ModuleMetaData> agents = getAgents(internalJobs);

            ContextMachine contextMachine = new ContextMachine(this.scheduledContextRecord.getContext(), contextInstance,
                this.scheduledContextInstanceService, internalJobs, this.queueDirectory, agents,
                this.jobLockCache, this.contextParametersInstanceService);

            raiseEvent(contextMachine);

            populateParamsWithAgent(contextInstance, agents);

            addSchedulerJobStateChangeEventListener(contextMachine);

            // save it so we do not create it again if restarted
            saveContextInstance(contextInstance, contextInstance.getStatus());

            ContextMachineCache.instance().put(contextMachine);

        } catch (Exception e) {
            LOG.error("Got error back filling context " + this.scheduledContextRecord.getContextName() + ". Error " + e);
        }
    }
}
