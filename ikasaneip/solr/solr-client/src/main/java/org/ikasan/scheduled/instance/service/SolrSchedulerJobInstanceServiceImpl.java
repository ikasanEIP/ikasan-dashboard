package org.ikasan.scheduled.instance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditAggregateDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobDaoImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrGlobalEventJobImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class SolrSchedulerJobInstanceServiceImpl implements SchedulerJobInstanceService {

    private Logger logger = LoggerFactory.getLogger(SolrSchedulerJobInstanceServiceImpl.class);

    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    private SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao;
    private SolrScheduledContextInstanceAuditAggregateDaoImpl solrScheduledContextInstanceAuditAggregateDao;
    private SolrSchedulerJobDaoImpl solrSchedulerJobDao;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;
    private boolean useLegacyJobStatusCount;

    public SolrSchedulerJobInstanceServiceImpl(SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao,
                                               SolrScheduledContextInstanceAuditAggregateDaoImpl solrScheduledContextInstanceAuditAggregateDao,
                                               SolrSchedulerJobDaoImpl solrSchedulerJobDao,
                                               ScheduledContextInstanceService scheduledContextInstanceService,
                                               Map<String, String> schedulerJobExecutionEnvironmentLabel,
                                               boolean useLegacyJobStatusCount) {
        this.solrSchedulerJobInstanceDao = solrSchedulerJobInstanceDao;
        if (solrSchedulerJobInstanceDao == null) {
            throw new IllegalArgumentException("solrSchedulerJobInstanceDao cannot be null!");
        }
        this.solrScheduledContextInstanceAuditAggregateDao = solrScheduledContextInstanceAuditAggregateDao;
        if (solrScheduledContextInstanceAuditAggregateDao == null) {
            throw new IllegalArgumentException("solrScheduledContextInstanceAuditAggregateDao cannot be null!");
        }
        this.solrSchedulerJobDao = solrSchedulerJobDao;
        if (solrSchedulerJobDao == null) {
            throw new IllegalArgumentException("solrSchedulerJobDao cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;
        this.useLegacyJobStatusCount = useLegacyJobStatusCount;
    }

    @Override
    public SchedulerJobInstanceRecord findById(String id) {
        return this.solrSchedulerJobInstanceDao.findById(id);
    }

    @Override
    public SchedulerJobInstanceRecord findByContextIdJobNameChildContextName(String uuid, String jobName, String childContextName) {
        SchedulerJobInstanceSearchFilter schedulerJobInstanceSearchFilter = new SolrSchedulerJobInstanceSearchFilterImpl();
        schedulerJobInstanceSearchFilter.setChildContextName(childContextName);
        schedulerJobInstanceSearchFilter.setJobName(jobName);
        schedulerJobInstanceSearchFilter.setContextInstanceId(uuid);

        SearchResults<SchedulerJobInstanceRecord> jobs = this.getScheduledContextInstancesByFilter(schedulerJobInstanceSearchFilter,
            1, 0, null, null);

        if(jobs.getTotalNumberOfResults() > 1) {
            logger.warn("SchedulerJobInstance search returned more than one result for Context ID[{}], Job Name[{}] and Child Context Name[{}]",
                uuid, jobName, childContextName);
        }

        if(jobs.getResultList().size() > 0) {
            return jobs.getResultList().get(0);
        }

        return null;
    }

    @Override
    public void save(SchedulerJobInstanceRecord scheduledContextInstanceRecord) {
        this.solrSchedulerJobInstanceDao.save(scheduledContextInstanceRecord);
    }

    @Override
    public void save(List<SchedulerJobInstanceRecord> scheduledContextInstanceRecords) {
        this.solrSchedulerJobInstanceDao.save(scheduledContextInstanceRecords);
    }

    @Override
    public void update(SchedulerJobInstance schedulerJobInstance) {
        SchedulerJobInstanceSearchFilter filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(schedulerJobInstance.getContextInstanceId());
        filter.setJobName(schedulerJobInstance.getJobName());
        filter.setChildContextName(schedulerJobInstance.getChildContextName());

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.solrSchedulerJobInstanceDao
            .getScheduledContextInstancesByFilter(filter, 1, 0, null, null);

        SchedulerJobInstanceRecord record = searchResults.getResultList().get(0);

        record.setStatus(schedulerJobInstance.getStatus().name());
        SchedulerJobInstance persistedInstance = record.getSchedulerJobInstance();
        persistedInstance.setScheduledProcessEvent(schedulerJobInstance.getScheduledProcessEvent());
        persistedInstance.setStatus(schedulerJobInstance.getStatus());
        persistedInstance.setHeld(schedulerJobInstance.isHeld());
        record.setSchedulerJobInstance(persistedInstance);
        record.setModifiedTimestamp(System.currentTimeMillis());
        // todo sort out modified by
        record.setModifiedBy("ContextMachine");

        this.solrSchedulerJobInstanceDao.save(record);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String contextInstanceId, int limit, int offset, String sortField, String sortDirection) {
        return this.solrSchedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(contextInstanceId, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String contextName, int limit, int offset, String sortField, String sortDirection) {
        return this.solrSchedulerJobInstanceDao.getSchedulerJobInstancesByContextName(contextName, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(SchedulerJobInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        return this.solrSchedulerJobInstanceDao.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
    }

    @Override
    public List<SchedulerJobInstance> initialiseSchedulerJobInstancesForContext(ContextTemplate contextTemplate, ContextInstance contextInstance
        , SchedulerJobInstancesInitialisationParameters schedulerJobInstancesInitialisationParameters) throws SchedulerJobInstanceInitialisationException {
        try {
            SearchResults<? extends SchedulerJobRecord> schedulerJobRecordSearchResults = this.solrSchedulerJobDao.findByContext(contextInstance.getName()
                , 0, 0);

            if (schedulerJobRecordSearchResults.getTotalNumberOfResults() == 0) {
                return new ArrayList<>();
            }

            schedulerJobRecordSearchResults = this.solrSchedulerJobDao.findByContext(contextInstance.getName()
                , (int) schedulerJobRecordSearchResults.getTotalNumberOfResults(), 0);

            List<SchedulerJobInstance> schedulerJobInstances = new ArrayList<>();
            for (SchedulerJobRecord schedulerJobRecord : schedulerJobRecordSearchResults.getResultList()) {
                if(schedulerJobRecord.getJob() instanceof SolrFileEventDrivenJobImpl) {
                    schedulerJobInstances.add(objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                        , SolrFileEventDrivenJobInstanceImpl.class));
                }
                else if(schedulerJobRecord.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                    InternalEventDrivenJobInstance internalEventDrivenJobInstance = objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                        , SolrInternalEventDrivenJobInstanceImpl.class);

                    if(schedulerJobInstancesInitialisationParameters.isInitialiseWithJobsOnHold()) {
                        internalEventDrivenJobInstance.setStatus(InstanceStatus.ON_HOLD);
                        internalEventDrivenJobInstance.setHeld(true);

                        if(internalEventDrivenJobInstance.getChildContextNames() != null) {
                            Map<String, Boolean> heldContexts = new HashMap<>();
                            internalEventDrivenJobInstance.getChildContextNames()
                                .forEach(name -> heldContexts.put(name, Boolean.TRUE));

                            internalEventDrivenJobInstance.setHeldContexts(heldContexts);
                        }
                    }

                    // replaces the execution environment attribute with the value from config-repo if it matches, else leave what we already have.
                    if (StringUtils.isNotBlank(internalEventDrivenJobInstance.getExecutionEnvironmentProperties())) {
                        if (schedulerJobExecutionEnvironmentLabel != null && schedulerJobExecutionEnvironmentLabel.size() != 0) {
                            String keyFromJob = internalEventDrivenJobInstance.getExecutionEnvironmentProperties();
                            if(schedulerJobExecutionEnvironmentLabel.containsKey(keyFromJob)) {
                                internalEventDrivenJobInstance.setExecutionEnvironmentProperties(schedulerJobExecutionEnvironmentLabel.get(keyFromJob));
                            }
                        }
                    }

                    if(schedulerJobRecord.isSkipped()) {
                        internalEventDrivenJobInstance.setSkip(true);
                        internalEventDrivenJobInstance.setStatus(InstanceStatus.SKIPPED);
                    }
                    else {
                        internalEventDrivenJobInstance.setSkip(false);
                    }

                    schedulerJobInstances.add(internalEventDrivenJobInstance);
                }
                else if(schedulerJobRecord.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                    schedulerJobInstances.add(objectMapper.readValue(objectMapper.writeValueAsBytes(schedulerJobRecord.getJob())
                        , SolrQuartzScheduleDrivenJobInstanceImpl.class));
                }
                else if(schedulerJobRecord.getJob() instanceof SolrGlobalEventJobImpl) {
                    GlobalEventJobInstance globalEventJobInstance = objectMapper.readValue
                        (objectMapper.writeValueAsBytes(schedulerJobRecord.getJob()), SolrGlobalEventJobInstanceImpl.class);
                    if(schedulerJobRecord.isSkipped()) {
                        globalEventJobInstance.setSkip(true);
                        globalEventJobInstance.setStatus(InstanceStatus.SKIPPED);
                    }
                    else {
                        globalEventJobInstance.setSkip(false);
                    }
                    schedulerJobInstances.add(globalEventJobInstance);
                }
            }

            StopWatch stopWatch = new StopWatch();

            stopWatch.start();
            // We are automating the creation of the context start job instances by dipping into the
            // context template via the ContextHelper and extracting all of the context start jobs.
            schedulerJobInstances.addAll(ContextHelper.getContextStartJobsFromContext(contextTemplate).stream()
                .map(contextStartJob -> {
                    ContextStartJobInstance contextStartJobInstance = new SolrContextStartJobInstanceImpl();
                    contextStartJobInstance.setJobName(contextStartJob.getJobName());
                    contextStartJobInstance.setContextName(contextTemplate.getName());
                    contextStartJobInstance.setContextInstanceId(contextInstance.getId());
                    contextStartJobInstance.setOrdinal(contextStartJob.getOrdinal());
                    return contextStartJobInstance;
                }).collect(Collectors.toList()));
            stopWatch.stop();
            logger.info("Time to load start jobs: " + stopWatch.getTime());

            stopWatch.reset();
            stopWatch.start();
            // We are automating the creation of the context terminal job instances by dipping into the
            // context template via the ContextHelper and extracting all of the context terminal jobs.
            schedulerJobInstances.addAll(ContextHelper.getContextTerminalJobsFromContext(contextTemplate).stream()
                .map(contextTerminalJob -> {
                    ContextTerminalJobInstance contextTerminalJobInstance = new SolrContextTerminalJobInstanceImpl();
                    contextTerminalJobInstance.setJobName(contextTerminalJob.getJobName());
                    contextTerminalJobInstance.setContextName(contextTemplate.getName());
                    contextTerminalJobInstance.setContextInstanceId(contextInstance.getId());
                    contextTerminalJobInstance.setOrdinal(contextTerminalJob.getOrdinal());
                    return contextTerminalJobInstance;
                }).collect(Collectors.toList()));
            stopWatch.stop();
            logger.info("Time to load terminal jobs: " + stopWatch.getTime());

            stopWatch.reset();
            stopWatch.start();
            // We are automating the creation of the local event job instances by dipping into the
            // context template via the ContextHelper and extracting all of the local event jobs.
            schedulerJobInstances.addAll(ContextHelper.getLocalEventJobsFromContext(contextTemplate).stream()
                .map(localEventJob -> {
                    LocalEventJobInstance localEventJobInstance = new SolrLocalEventJobInstanceImpl();
                    localEventJobInstance.setJobName(localEventJob.getJobName());
                    localEventJobInstance.setContextName(contextTemplate.getName());
                    localEventJobInstance.setContextInstanceId(contextInstance.getId());
                    localEventJobInstance.setOrdinal(localEventJob.getOrdinal());
                    return localEventJobInstance;
                }).collect(Collectors.toList()));

            stopWatch.stop();
            logger.info("Time to load start local event jobs: " + stopWatch.getTime());

            stopWatch.reset();
            stopWatch.start();
            // We are automating the creation of the bridging job instances by dipping into the
            // context template via the ContextHelper and extracting all of the bridging jobs.
            schedulerJobInstances.addAll(ContextHelper.getBridgingJobsFromContext(contextTemplate).stream()
                .map(bridgingJob -> {
                    BridgingJobInstance bridgingJobInstance = new SolrBridgingJobInstanceImpl();
                    bridgingJobInstance.setJobName(bridgingJob.getJobName());
                    bridgingJobInstance.setContextName(contextTemplate.getName());
                    bridgingJobInstance.setContextInstanceId(contextInstance.getId());
                    bridgingJobInstance.setOrdinal(bridgingJob.getOrdinal());
                    return bridgingJobInstance;
                }).collect(Collectors.toList()));

            stopWatch.stop();
            logger.info("Time to load start bridging jobs: " + stopWatch.getTime());

            Map<String, SchedulerJobInstance> schedulerJobInstanceMap = schedulerJobInstances.stream()
                .collect(toMap(SchedulerJobInstance::getIdentifier, Function.identity(), (key1, key2)-> key2));

            List<SchedulerJobInstance> contextualisedSchedulerJobInstances = new ArrayList<>();

            // We are going to contextualise the
            contextInstance.getAllSchedulerJobInstances().forEach(schedulerJobInstance -> {
                SchedulerJobInstance instance = schedulerJobInstanceMap.get(schedulerJobInstance.getIdentifier());

                if(instance != null) {
                    SchedulerJobInstance contextualisedInstance = (SchedulerJobInstance)SerializationUtils.clone(instance);
                    contextualisedInstance.setChildContextName(schedulerJobInstance.getChildContextName());
                    contextualisedInstance.setContextInstanceId(contextInstance.getId());

                    if(instance instanceof InternalEventDrivenJobInstance) {

                        if (instance.getSkippedContexts().containsKey(schedulerJobInstance.getChildContextName())
                            && instance.getSkippedContexts().get(schedulerJobInstance.getChildContextName())) {
                            contextualisedInstance.setSkip(true);
                            contextualisedInstance.setStatus(InstanceStatus.SKIPPED);
                        }
                        else if (instance.getHeldContexts().containsKey(schedulerJobInstance.getChildContextName())
                            && instance.getHeldContexts().get(schedulerJobInstance.getChildContextName())) {
                            contextualisedInstance.setHeld(true);
                            contextualisedInstance.setStatus(InstanceStatus.ON_HOLD);
                        }
                    }
                    else if(instance instanceof GlobalEventJobInstance
                        && instance.getSkippedContexts().containsKey(contextInstance.getName())) {
                        contextualisedInstance.setSkip(true);
                        contextualisedInstance.setStatus(InstanceStatus.SKIPPED);
                    }
                    contextualisedSchedulerJobInstances.add(contextualisedInstance);
                }
            });

            List<SchedulerJobInstanceRecord> schedulerJobInstanceRecords = new ArrayList<>();

            contextualisedSchedulerJobInstances.forEach(job -> {
                SchedulerJobInstanceRecord instanceRecord = new SolrSchedulerJobInstanceRecordImpl();
                instanceRecord.setContextName(job.getContextName());
                instanceRecord.setJobName(job.getJobName());
                instanceRecord.setStatus(job.getStatus().toString());
                instanceRecord.setTimestamp(System.currentTimeMillis());
                instanceRecord.setContextInstanceId(contextInstance.getId());
                instanceRecord.setChildContextName(job.getChildContextName());
                if(job instanceof InternalEventDrivenJobInstance) {
                    instanceRecord.setParticipatesInLock(((InternalEventDrivenJobInstance)job).isParticipatesInLock());
                    instanceRecord.setTargetResidingContextOnly(((InternalEventDrivenJobInstance)job).isTargetResidingContextOnly());
                }
                instanceRecord.setSchedulerJobInstance(job);

                schedulerJobInstanceRecords.add(instanceRecord);
            });

            this.solrSchedulerJobInstanceDao.save(schedulerJobInstanceRecords);

            return contextualisedSchedulerJobInstances;
        }
        catch (IOException e) {
            logger.error(String.format("An exception has occurred " +
                "attempting to initialise scheduler job instances for context[%s]", contextInstance.getName()), e);
            throw new SchedulerJobInstanceInitialisationException(String.format("An exception has occurred " +
                "attempting to initialise scheduler job instances for context[%s]", contextInstance.getName()), e);
        }
    }

    @Override
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstances(List<String> contextInstanceIds) {
        List<ContextInstanceAggregateJobStatus> contextInstanceAggregateJobStatuses;
        if(this.useLegacyJobStatusCount) {
            contextInstanceAggregateJobStatuses = this.solrSchedulerJobInstanceDao
                .getJobStatusCountForContextInstances(contextInstanceIds);
        }
        else {
            contextInstanceAggregateJobStatuses =  this.getJobStatusCountForContextInstancesConsiderNonTargetedDuplication
                (contextInstanceIds);
        }

        Map<String, Map<String, Integer>> repeatingJobStatuses = this.solrScheduledContextInstanceAuditAggregateDao
            .getRepeatingJobStatusCounts(contextInstanceIds);

        contextInstanceAggregateJobStatuses.forEach(contextInstanceAggregateJobStatus -> {
            ScheduledContextInstanceRecord scheduledContextInstanceRecord
                = this.scheduledContextInstanceService.findById(contextInstanceAggregateJobStatus.getContextInstanceId()
                + "_scheduledContextInstance");

            if(scheduledContextInstanceRecord != null) {
                contextInstanceAggregateJobStatus.setContainsRepeatableJobs(scheduledContextInstanceRecord.isContainsRepeatingJobs());
            }

            contextInstanceAggregateJobStatus.setRepeatingJobsStatusCounts
                (repeatingJobStatuses.get(contextInstanceAggregateJobStatus.getContextInstanceId()));
        });

        return contextInstanceAggregateJobStatuses;
    }

    @Override
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(List<String> contextInstanceIds) {
        return this.solrSchedulerJobInstanceDao.getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(contextInstanceIds);
    }

    @Override
    public Map<String, InternalEventDrivenJobInstance> getCommandExecutionJobsForContextInstance(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType("internalEventDrivenJobInstance");
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults
            = this.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> (InternalEventDrivenJobInstance)internalEventDrivenJobRecord.getSchedulerJobInstance())
            .collect(toMap(key -> key.getIdentifier(), Function.identity(), (a1, a2) -> a1));
        return internalEventDrivenJobMap;
    }

    @Override
    public Map<String, InternalEventDrivenJobInstance> getCommandExecutionJobsForContextInstanceChildContext(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults
            = this.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> (InternalEventDrivenJobInstance)internalEventDrivenJobRecord.getSchedulerJobInstance())
            .collect(toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity()));
        return internalEventDrivenJobMap;
    }

    @Override
    public List<SchedulerJobInstanceRecord> holdJobsWithinContext(ContextInstance contextInstance, String childContextName) {
        ContextInstance childContext = ContextHelper.getChildContextInstance(childContextName, contextInstance);
        ContextHelper.enrichJobs(childContext);
        Map<String, SchedulerJobInstance> childContextJobs = ContextHelper.getAllJobs(childContext);

        Map<String, InternalEventDrivenJobInstance> commandExecutionJobsForContextInstanceChildContext
            = this.getCommandExecutionJobsForContextInstanceChildContext(contextInstance.getId());

        ContextHelper.holdAllJobs(childContext, commandExecutionJobsForContextInstanceChildContext.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, e -> e.getValue())));

        SchedulerJobInstanceSearchFilter searchFilter = new SolrSchedulerJobInstanceSearchFilterImpl();
        searchFilter.setContextInstanceId(contextInstance.getId());
        searchFilter.setJobType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> searchResults = this.getScheduledContextInstancesByFilter(searchFilter
            , -1, -1, null, null);

        searchFilter = new SolrSchedulerJobInstanceSearchFilterImpl();
        searchFilter.setContextInstanceId(contextInstance.getId());
        searchFilter.setJobType(JobConstants.LOCAL_EVENT_JOB_INSTANCE);
        searchResults.getResultList().addAll(this.getScheduledContextInstancesByFilter(searchFilter
            , -1, -1, null, null).getResultList());

        List<SchedulerJobInstanceRecord> updatedRecords = new ArrayList<>();
        searchResults.getResultList().forEach(schedulerJobInstanceRecord -> {
            if (schedulerJobInstanceRecord.getStatus().equals(InstanceStatus.WAITING.name()) &&
                (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof LocalEventJobInstance ||
                    childContextJobs.containsKey(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier()
                        + schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName()))) {

                SchedulerJobInstance job = childContextJobs
                    .get(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier()
                        + schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());

                if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof LocalEventJobInstance) {
                    schedulerJobInstanceRecord.setStatus(InstanceStatus.ON_HOLD.name());
                    SchedulerJobInstance instance
                        = schedulerJobInstanceRecord.getSchedulerJobInstance();
                    instance.setStatus(InstanceStatus.ON_HOLD);
                    instance.setHeld(true);

                    if(instance instanceof LocalEventJobInstance) {
                        instance.setChildContextNames(ContextHelper.getContextsWhereJobNameMatchResides
                            (contextInstance, instance.getJobName()));
                    }

                    if (instance.getChildContextNames() != null) {
                        HashMap<String, Boolean> heldContexts = new HashMap<>();
                        instance.getChildContextNames().forEach(name -> {
                            heldContexts.put(name, Boolean.TRUE);
                            ContextHelper.getSchedulerJobInstance(instance.getJobName(), name,
                                contextInstance).setStatus(InstanceStatus.ON_HOLD);
                            ContextHelper.getSchedulerJobInstance(instance.getJobName(), name,
                                contextInstance).setHeld(true);
                        });
                        instance.setHeldContexts(heldContexts);
                    }

                    schedulerJobInstanceRecord.setSchedulerJobInstance(instance);
                    updatedRecords.add(schedulerJobInstanceRecord);
                }
                else if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                    !((InternalEventDrivenJobInstance)schedulerJobInstanceRecord.getSchedulerJobInstance()).isTargetResidingContextOnly() ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName().equals(job.getChildContextName())) {
                    schedulerJobInstanceRecord.setStatus(InstanceStatus.ON_HOLD.name());
                    InternalEventDrivenJobInstance instance
                        = (InternalEventDrivenJobInstance) schedulerJobInstanceRecord.getSchedulerJobInstance();
                    instance.setStatus(InstanceStatus.ON_HOLD);
                    instance.setHeld(true);

                    if (instance.getChildContextNames() != null) {
                        HashMap<String, Boolean> heldContexts = new HashMap<>();
                        instance.getChildContextNames().forEach(name -> {
                            heldContexts.put(name, Boolean.TRUE);
                        });
                        instance.setHeldContexts(heldContexts);
                    }

                    schedulerJobInstanceRecord.setSchedulerJobInstance(instance);

                    updatedRecords.add(schedulerJobInstanceRecord);
                }
            }
        });

        if(updatedRecords.size() > 0) {
            this.save(updatedRecords);
        }

        return updatedRecords;
    }

    public List<SchedulerJobInstanceRecord> getJobsToReleaseWithinContext(ContextInstance contextInstance, String childContextName) {
        ContextInstance childContext = ContextHelper.getChildContextInstance(childContextName, contextInstance);
        ContextHelper.enrichJobs(childContext);
        Map<String, SchedulerJobInstance> contextJobs = ContextHelper.getAllJobs(childContext);

        SchedulerJobInstanceSearchFilter searchFilter = new SolrSchedulerJobInstanceSearchFilterImpl();
        searchFilter.setContextInstanceId(contextInstance.getId());
        searchFilter.setJobType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> searchResults = this.getScheduledContextInstancesByFilter(searchFilter
            , -1, -1, null, null);

        searchFilter = new SolrSchedulerJobInstanceSearchFilterImpl();
        searchFilter.setContextInstanceId(contextInstance.getId());
        searchFilter.setJobType(JobConstants.LOCAL_EVENT_JOB_INSTANCE);
        searchResults.getResultList().addAll(this.getScheduledContextInstancesByFilter(searchFilter
            , -1, -1, null, null).getResultList());

        List<SchedulerJobInstanceRecord> updatedRecords = new ArrayList<>();
        searchResults.getResultList().forEach(schedulerJobInstanceRecord -> {
            if (schedulerJobInstanceRecord.getStatus().equals(InstanceStatus.ON_HOLD.name())
                && contextJobs.containsKey(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier()
                + schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName())) {
                SchedulerJobInstance job = contextJobs
                    .get(schedulerJobInstanceRecord.getSchedulerJobInstance().getIdentifier()
                        +schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName());

                if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof LocalEventJobInstance ||
                    (schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance &&
                    !((InternalEventDrivenJobInstance)schedulerJobInstanceRecord.getSchedulerJobInstance()).isTargetResidingContextOnly() ||
                    schedulerJobInstanceRecord.getSchedulerJobInstance().getChildContextName().equals(job.getChildContextName()))) {

                    updatedRecords.add(schedulerJobInstanceRecord);
                }
            }
        });

        return updatedRecords;
    }

    @Override
    public void deleteSchedulerJobInstances(String contextInstanceId) {
        this.solrSchedulerJobInstanceDao.deleteSchedulerJobInstances(contextInstanceId);
    }
}
