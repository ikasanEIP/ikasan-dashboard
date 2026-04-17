package org.ikasan.relational.persistence.scheduled.instance.service;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstancesInitialisationParameters;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hibernate/PostgreSQL implementation of SchedulerJobInstanceService.
 *
 * This service provides business logic for managing scheduler job instances using
 * Hibernate persistence. It delegates to DAO layer for data access and provides
 * service-level operations like job initialization, status aggregation, etc.
 */
public class HibernateSchedulerJobInstanceServiceImpl implements SchedulerJobInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(HibernateSchedulerJobInstanceServiceImpl.class);

    private final SchedulerJobInstanceDao schedulerJobInstanceDao;

    /**
     * Constructor with required dependencies
     *
     * @param schedulerJobInstanceDao DAO for job instance operations
     */
    public HibernateSchedulerJobInstanceServiceImpl(SchedulerJobInstanceDao schedulerJobInstanceDao) {
        this.schedulerJobInstanceDao = schedulerJobInstanceDao;
        if (this.schedulerJobInstanceDao == null) {
            throw new IllegalArgumentException("schedulerJobInstanceDao cannot be null!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SchedulerJobInstanceRecord findById(String id) {
        logger.debug("Finding job instance by id: {}", id);
        return this.schedulerJobInstanceDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SchedulerJobInstanceRecord findByContextIdJobNameChildContextName(
            String uuid, String jobName, String childContextName) {
        logger.debug("Finding job instance by context={}, job={}, childContext={}",
            uuid, jobName, childContextName);

        // Query by context instance ID and job name, then filter by child context name
        SearchResults<SchedulerJobInstanceRecord> results =
            this.schedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(uuid, -1, -1, null, null);

        return results.getResultList().stream()
            .filter(record -> jobName.equals(record.getJobName()))
            .filter(record -> {
                if (childContextName == null) {
                    return record.getChildContextName() == null;
                }
                return childContextName.equals(record.getChildContextName());
            })
            .findFirst()
            .orElse(null);
    }

    @Override
    @Transactional
    public void save(SchedulerJobInstanceRecord scheduledContextInstanceRecord) {
        logger.debug("Saving job instance: {}", scheduledContextInstanceRecord.getId());
        this.schedulerJobInstanceDao.save(scheduledContextInstanceRecord);
    }

    @Override
    @Transactional
    public void save(List<SchedulerJobInstanceRecord> scheduledContextInstanceRecords) {
        logger.debug("Saving {} job instances", scheduledContextInstanceRecords.size());
        for (SchedulerJobInstanceRecord record : scheduledContextInstanceRecords) {
            this.schedulerJobInstanceDao.save(record);
        }
    }

    @Override
    @Transactional
    public void update(SchedulerJobInstance schedulerJobInstance) {
        logger.debug("Updating job instance: {}", schedulerJobInstance.getJobName());

        // Find the existing record
        String contextInstanceId = schedulerJobInstance.getContextInstanceId();
        String jobName = schedulerJobInstance.getJobName();
        String childContextName = schedulerJobInstance.getChildContextName();

        SchedulerJobInstanceRecord record = findByContextIdJobNameChildContextName(
            contextInstanceId, jobName, childContextName);

        if (record == null) {
            throw new IllegalArgumentException(
                String.format("Job instance not found for context=%s, job=%s, childContext=%s",
                    contextInstanceId, jobName, childContextName));
        }

        // Update the job instance within the record
        record.setSchedulerJobInstance(schedulerJobInstance);
        record.setStatus(schedulerJobInstance.getStatus().name());

        // Save the updated record
        this.schedulerJobInstanceDao.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(
            String contextInstanceId, int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Getting job instances by context instance id: {}", contextInstanceId);
        return this.schedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(
            contextInstanceId, limit, offset, sortField, sortDirection);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(
            String contextName, int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Getting job instances by context name: {}", contextName);
        return this.schedulerJobInstanceDao.getSchedulerJobInstancesByContextName(
            contextName, limit, offset, sortField, sortDirection);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(
            SchedulerJobInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Getting job instances by filter");
        return this.schedulerJobInstanceDao.getScheduledContextInstancesByFilter(
            filter, limit, offset, sortField, sortDirection);
    }

    @Override
    @Transactional
    public List<SchedulerJobInstance> initialiseSchedulerJobInstancesForContext(
            ContextTemplate contextTemplate,
            ContextInstance contextInstance,
            SchedulerJobInstancesInitialisationParameters parameters)
            throws SchedulerJobInstanceInitialisationException {
        logger.info("Initializing job instances for context: {}", contextTemplate.getName());

        // This is a complex operation that would need the full job initialization logic
        // For now, marking as not implemented
        throw new UnsupportedOperationException(
            "initialiseSchedulerJobInstancesForContext requires full job initialization logic - not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstances(
            List<String> contextInstanceIds) {
        logger.debug("Getting job status counts for {} context instances", contextInstanceIds.size());

        // This would require aggregate queries across context instances
        // For now, marking as not implemented
        throw new UnsupportedOperationException(
            "getJobStatusCountForContextInstances requires aggregate query implementation - not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(
            List<String> contextInstanceIds) {
        logger.debug("Getting job status counts (with duplication consideration) for {} context instances",
            contextInstanceIds.size());

        // This would require complex aggregate queries
        // For now, marking as not implemented
        throw new UnsupportedOperationException(
            "getJobStatusCountForContextInstancesConsiderNonTargetedDuplication requires " +
            "complex aggregate query implementation - not yet implemented");
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, InternalEventDrivenJobInstance> getCommandExecutionJobsForContextInstance(
            String contextInstanceId) {
        logger.debug("Getting command execution jobs for context instance: {}", contextInstanceId);

        SearchResults<SchedulerJobInstanceRecord> results =
            this.schedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(
                contextInstanceId, -1, -1, null, null);

        Map<String, InternalEventDrivenJobInstance> commandJobs = new HashMap<>();

        for (SchedulerJobInstanceRecord record : results.getResultList()) {
            SchedulerJobInstance jobInstance = record.getSchedulerJobInstance();
            if (jobInstance instanceof InternalEventDrivenJobInstance) {
                InternalEventDrivenJobInstance internalJob = (InternalEventDrivenJobInstance) jobInstance;
                commandJobs.put(internalJob.getIdentifier(), internalJob);
            }
        }

        return commandJobs;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, InternalEventDrivenJobInstance> getCommandExecutionJobsForContextInstanceChildContext(
            String contextInstanceId) {
        logger.debug("Getting command execution jobs (child context) for context instance: {}", contextInstanceId);

        SearchResults<SchedulerJobInstanceRecord> results =
            this.schedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(
                contextInstanceId, -1, -1, null, null);

        Map<String, InternalEventDrivenJobInstance> commandJobs = new HashMap<>();

        for (SchedulerJobInstanceRecord record : results.getResultList()) {
            SchedulerJobInstance jobInstance = record.getSchedulerJobInstance();
            if (jobInstance instanceof InternalEventDrivenJobInstance) {
                InternalEventDrivenJobInstance internalJob = (InternalEventDrivenJobInstance) jobInstance;
                String key = internalJob.getIdentifier() + "-" +
                    (internalJob.getChildContextName() != null ? internalJob.getChildContextName() : "");
                commandJobs.put(key, internalJob);
            }
        }

        return commandJobs;
    }

    @Override
    @Transactional
    public List<SchedulerJobInstanceRecord> holdJobsWithinContext(
            ContextInstance contextInstance, String childContextName) {
        logger.info("Holding jobs within context: {}, childContext: {}",
            contextInstance.getName(), childContextName);

        SearchResults<SchedulerJobInstanceRecord> results =
            this.schedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(
                contextInstance.getId(), -1, -1, null, null);

        List<SchedulerJobInstanceRecord> heldJobs = results.getResultList().stream()
            .filter(record -> childContextName == null || childContextName.equals(record.getChildContextName()))
            .peek(record -> {
                SchedulerJobInstance jobInstance = record.getSchedulerJobInstance();
                if (jobInstance != null) {
                    jobInstance.setHeld(true);
                    record.setSchedulerJobInstance(jobInstance);
                    this.schedulerJobInstanceDao.save(record);
                }
            })
            .collect(Collectors.toList());

        logger.info("Held {} jobs", heldJobs.size());
        return heldJobs;
    }

    @Override
    @Transactional
    public List<SchedulerJobInstanceRecord> getJobsToReleaseWithinContext(
            ContextInstance contextInstance, String childContextName) {
        logger.info("Releasing jobs within context: {}, childContext: {}",
            contextInstance.getName(), childContextName);

        SearchResults<SchedulerJobInstanceRecord> results =
            this.schedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(
                contextInstance.getId(), -1, -1, null, null);

        List<SchedulerJobInstanceRecord> releasedJobs = results.getResultList().stream()
            .filter(record -> childContextName == null || childContextName.equals(record.getChildContextName()))
            .filter(record -> {
                SchedulerJobInstance jobInstance = record.getSchedulerJobInstance();
                return jobInstance != null && jobInstance.isHeld();
            })
            .peek(record -> {
                SchedulerJobInstance jobInstance = record.getSchedulerJobInstance();
                if (jobInstance != null) {
                    jobInstance.setHeld(false);
                    record.setSchedulerJobInstance(jobInstance);
                    this.schedulerJobInstanceDao.save(record);
                }
            })
            .collect(Collectors.toList());

        logger.info("Released {} jobs", releasedJobs.size());
        return releasedJobs;
    }

    @Override
    @Transactional
    public void deleteSchedulerJobInstances(String contextInstanceId) {
        logger.info("Deleting all job instances for context instance: {}", contextInstanceId);
        this.schedulerJobInstanceDao.deleteSchedulerJobInstances(contextInstanceId);
    }
}
