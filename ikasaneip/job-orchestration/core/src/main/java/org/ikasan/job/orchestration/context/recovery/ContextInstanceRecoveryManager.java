package org.ikasan.job.orchestration.context.recovery;

import static org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker.outsideOfOperatingWindow;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceRecoveryManager {
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceRecoveryManager.class);

    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ScheduledContextService scheduledContextService;
    private InternalEventDrivenJobService internalEventDrivenJobRecordService;
    private String queueDirectory;
    private JobLockCacheService jobLockCacheService;
    private ContextParametersInstanceService contextParametersInstanceService;
    private final SchedulerService schedulerService;
    private ModuleMetaDataService moduleMetadataService;
    private ContextParametersUpdateService<ContextParameterInstance> contextParametersUpdateService;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private boolean usePostConstructs;

    public ContextInstanceRecoveryManager(ScheduledContextInstanceService scheduledContextInstanceService, ScheduledContextService scheduledContextService,
                                          InternalEventDrivenJobService internalEventDrivenJobRecordService, String queueDirectory,
                                          JobLockCacheService jobLockCacheService, ContextParametersInstanceService contextParametersInstanceService,
                                          SchedulerService schedulerService, ModuleMetaDataService moduleMetadataService,
                                          ContextParametersUpdateService contextParametersUpdateService, boolean usePostConstructs) {
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.internalEventDrivenJobRecordService = internalEventDrivenJobRecordService;
        if (this.internalEventDrivenJobRecordService == null) {
            throw new IllegalArgumentException("internalEventDrivenJobRecordService cannot be null!");
        }
        this.queueDirectory = queueDirectory;
        if (this.queueDirectory == null) {
            throw new IllegalArgumentException("queueDirectory cannot be null!");
        }
        this.jobLockCacheService = jobLockCacheService;
        if (this.jobLockCacheService == null) {
            throw new IllegalArgumentException("jobLockCacheService cannot be null!");
        }
        this.contextParametersInstanceService = contextParametersInstanceService;
        if (this.contextParametersInstanceService == null) {
            throw new IllegalArgumentException("contextParametersInstanceService cannot be null!");
        }
        this.schedulerService = schedulerService;
        if (this.schedulerService == null) {
            throw new IllegalArgumentException("schedulerService cannot be null!");
        }
        this.moduleMetadataService = moduleMetadataService;
        if (this.moduleMetadataService == null) {
            throw new IllegalArgumentException("moduleMetadataService cannot be null!");
        }
        this.contextParametersUpdateService = contextParametersUpdateService;
        if (this.contextParametersUpdateService == null) {
            throw new IllegalArgumentException("contextParametersUpdateService cannot be null!");
        }
        this.usePostConstructs = usePostConstructs;
    }

    @PostConstruct
    public void recoverContextInstances() {
        // NOTE: This executes before ContextInstanceSchedulerService
        logger.info("Recovering context instances!");
        if (!usePostConstructs) {
            logger.info("ContextInstanceRecoveryManager not running as usePostConstructs is false");
            return;
        }
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

        SearchResults<ScheduledContextRecord> scheduledContextRecords = (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

        Map<String, ScheduledContextInstanceRecord> instancesMap
            = sorted.stream().collect(Collectors.toMap(ScheduledContextInstanceRecord::getContextName, record -> record));

        // TODO jobLocks might be locked need use the latest one from solr
        Date now = new Date();
        for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {
            ContextTemplate context = scheduledContextRecord.getContext();
            ScheduledContextInstanceRecord scheduledContextInstanceRecord = instancesMap.get(scheduledContextRecord.getContextName());
            if (scheduledContextInstanceRecord != null) {
                ContextInstance contextInstance = scheduledContextInstanceRecord.getContextInstance();
                if (outsideOfOperatingWindow(context.getTimeWindowStart(), context.getTimeWindowEnd(), now)) {
                    // do nothing ContextInstanceSchedulerService will take care of creating outside operating window
                    continue;
                }
                try {
                    Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = getInternalJobs(scheduledContextInstanceRecord);
                    HashMap<String, ModuleMetaData> agents = getAgents(internalEventDrivenJobMap);
                    JobLockCache jobLockCache = getJobLockCache(context);

                    ContextMachine contextMachine = new ContextMachine(context, contextInstance,
                        this.scheduledContextInstanceService, internalEventDrivenJobMap, this.queueDirectory, agents,
                        jobLockCache, this.contextParametersInstanceService);

                    contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
                        this.schedulerService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event);
                    });

                    ContextMachineCache.instance().put(contextMachine);
                } catch (Exception e) {
                    // todo probably want to send a notification here.
                    logger.error(String.format("An error has occurred recovering context instance[%s]!", scheduledContextInstanceRecord.getContextName()), e);
                }
            } else {
                // we have a context record without an instance which should not be the case
                String message = String.format("Context %s does not have an instance. Creating instance now!", scheduledContextRecord.getContextName());
                logger.error(message);
                executor.execute(new ContextInstanceRecoveryBackFiller(
                    scheduledContextRecord, this.scheduledContextInstanceService, this.queueDirectory, this.internalEventDrivenJobRecordService,
                    this.schedulerService, this.moduleMetadataService, this.jobLockCacheService, this.contextParametersInstanceService,
                    this.contextParametersUpdateService, getJobLockCache(context)
                ));
            }
        }
    }

    private JobLockCache getJobLockCache(ContextTemplate context) {
        JobLockCache jobLockCache;
        JobLockCacheRecord jobLockCacheRecord = jobLockCacheService.get();
        if (jobLockCacheRecord == null) {
            // should never happen we are recovering so should exist but just in case
            jobLockCache = JobLockCacheImpl.instance();
            jobLockCache.setJobLockCacheService(jobLockCacheService);
            jobLockCache.addLocks(context.getAllNestedJobLocks());
        } else {
            jobLockCache = jobLockCacheRecord.getJobLockCache();
            jobLockCache.setJobLockCacheService(jobLockCacheService);
            // do not set the locks as should all be in there already
        }
        return jobLockCache;
    }

    private HashMap<String, ModuleMetaData> getAgents(Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();
        internalEventDrivenJobMap.values().forEach(job -> {
            if (!agents.containsKey(job.getAgentName())) {
                ModuleMetaData moduleMetadataById = moduleMetadataService.findById(job.getAgentName());
                if (moduleMetadataById == null) {
                    logger.error("Could not find ModuleMetaData for agent name " + job.getAgentName());
                } else {
                    agents.put(job.getAgentName(), moduleMetadataById);
                }
            }
        });
        return agents;
    }

    private Map<String, InternalEventDrivenJob> getInternalJobs(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults
            = this.internalEventDrivenJobRecordService.findByContext(scheduledContextInstanceRecord.getContextName(), -1, -1);

        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> internalEventDrivenJobRecord.getInternalEventDrivenJob())
            .collect(Collectors.toMap(InternalEventDrivenJob::getIdentifier, Function.identity()));
        return internalEventDrivenJobMap;
    }

}