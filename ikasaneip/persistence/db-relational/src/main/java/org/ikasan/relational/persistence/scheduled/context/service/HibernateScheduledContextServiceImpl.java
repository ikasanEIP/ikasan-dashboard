package org.ikasan.relational.persistence.scheduled.context.service;

import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.relational.persistence.scheduled.context.model.HibernateScheduledContextRecordLiteImpl;
import org.ikasan.spec.scheduled.context.ScheduledContextRecordLite;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.relational.persistence.scheduled.context.model.HibernateScheduledContextRecordImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextService.
 *
 * This service provides business logic for managing scheduled contexts using
 * Hibernate persistence. It delegates to DAO layer for data access and adds
 * service-level operations like cloning, enabling/disabling jobs, etc.
 */
public class HibernateScheduledContextServiceImpl implements ScheduledContextService {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextServiceImpl.class);

    private final ScheduledContextDao scheduledContextDao;
    private final ScheduledContextViewDao scheduledContextViewDao;

    /**
     * Constructor with required dependencies
     *
     * @param scheduledContextDao     DAO for scheduled context operations
     * @param scheduledContextViewDao DAO for context view operations
     */
    public HibernateScheduledContextServiceImpl(ScheduledContextDao scheduledContextDao,
                                                 ScheduledContextViewDao scheduledContextViewDao) {
        this.scheduledContextDao = scheduledContextDao;
        if (this.scheduledContextDao == null) {
            throw new IllegalArgumentException("scheduledContextDao cannot be null!");
        }
        this.scheduledContextViewDao = scheduledContextViewDao;
        if (this.scheduledContextViewDao == null) {
            throw new IllegalArgumentException("scheduledContextViewDao cannot be null!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<? extends ScheduledContextRecord> findAll() {
        logger.debug("Finding all scheduled contexts");
        return this.scheduledContextDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<? extends ScheduledContextRecord> findAll(int limit, int offset) {
        logger.debug("Finding all scheduled contexts with limit={}, offset={}", limit, offset);
        return this.scheduledContextDao.findAll(limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextRecord> findByFilter(ScheduledContextSearchFilter filter,
                                                               int limit,
                                                               int offset,
                                                               String sortColumn,
                                                               String sortOrder) {
        logger.debug("Finding scheduled contexts by filter: {}", filter);
        return this.scheduledContextDao.findByFilter(filter, limit, offset, sortColumn, sortOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextRecordLite> findByFilterLite(ScheduledContextSearchFilter filter,
                                                                       int limit,
                                                                       int offset,
                                                                       String sortColumn,
                                                                       String sortOrder) {
        logger.debug("Finding scheduled contexts (lite) by filter: {}", filter);

        SearchResults<ScheduledContextRecord> searchResults =
            this.findByFilter(filter, limit, offset, sortColumn, sortOrder);

        List<ScheduledContextRecordLite> records = searchResults.getResultList().stream()
            .map(scheduledContextRecord -> {
                ScheduledContextRecordLite scheduledContextRecordLite = new HibernateScheduledContextRecordLiteImpl();
                scheduledContextRecordLite.setId(scheduledContextRecord.getId());
                scheduledContextRecordLite.setContextName(scheduledContextRecord.getContextName());
                scheduledContextRecordLite.setDescription(scheduledContextRecord.getContext().getDescription());
                scheduledContextRecordLite.setTimestamp(scheduledContextRecord.getTimestamp());
                scheduledContextRecordLite.setModifiedBy(scheduledContextRecord.getModifiedBy());
                scheduledContextRecordLite.setModifiedTimestamp(scheduledContextRecord.getModifiedTimestamp());
                scheduledContextRecordLite.setDisabled(scheduledContextRecord.isDisabled());
                scheduledContextRecordLite.setQuartzScheduleDrivenJobsDisabledForContext(
                    scheduledContextRecord.isQuartzScheduleDrivenJobsDisabledForContext());

                return scheduledContextRecordLite;
            })
            .collect(Collectors.toList());

        return new SearchResultsImpl<>(records,
            searchResults.getTotalNumberOfResults(),
            searchResults.getQueryResponseTime());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextRecord findById(String id) {
        logger.debug("Finding scheduled context by id: {}", id);
        return this.scheduledContextDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextRecord findByName(String name) {
        logger.debug("Finding scheduled context by name: {}", name);
        return this.scheduledContextDao.findByName(name);
    }

    @Override
    @Transactional
    public void save(ScheduledContextRecord scheduledContextRecord) {
        logger.debug("Saving scheduled context: {}", scheduledContextRecord.getContextName());
        this.scheduledContextDao.save(scheduledContextRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextViewRecord getContextView(String parentContextName, String contextName) {
        logger.debug("Getting context view for parent={}, context={}", parentContextName, contextName);
        return scheduledContextViewDao.getContextView(parentContextName, contextName);
    }

    @Override
    @Transactional
    public void saveContextView(ScheduledContextViewRecord contextView) {
        logger.debug("Saving context view");
        this.scheduledContextViewDao.save(contextView);
    }

    @Override
    @Transactional
    public void deleteContext(String contextName) {
        logger.debug("Deleting context: {}", contextName);
        this.scheduledContextDao.deleteContext(contextName);
    }

    @Override
    @Transactional
    public ScheduledContextRecord cloneContext(String contextName, String clonedContextName) {
        logger.info("Cloning context from '{}' to '{}'", contextName, clonedContextName);

        // Find the source context
        ScheduledContextRecord sourceRecord = findByName(contextName);
        if (sourceRecord == null) {
            throw new IllegalArgumentException("Source context not found: " + contextName);
        }

        // Check if target context already exists
        ScheduledContextRecord existingRecord = findByName(clonedContextName);
        if (existingRecord != null) {
            throw new IllegalArgumentException("Target context already exists: " + clonedContextName);
        }

        // Create a deep copy of the context template
        ContextTemplate clonedTemplate = cloneContextTemplate(sourceRecord.getContext(), clonedContextName);

        // Create new scheduled context record
        ScheduledContextRecord clonedRecord = createScheduledContextRecord(clonedContextName, clonedTemplate);
        clonedRecord.setModifiedBy("system"); // Default, should be overridden by caller if needed

        // Save the cloned context
        save(clonedRecord);

        logger.info("Successfully cloned context from '{}' to '{}'", contextName, clonedContextName);
        return clonedRecord;
    }

    @Override
    @Transactional
    public void enableScheduledJobs(ContextTemplate contextTemplate, String modifiedBy) {
        logger.info("Enabling scheduled jobs for context: {}", contextTemplate.getName());
        this.enableDisableScheduledJobs(contextTemplate, modifiedBy, false);
    }

    @Override
    @Transactional
    public void disableScheduledJobs(ContextTemplate contextTemplate, String modifiedBy) {
        logger.info("Disabling scheduled jobs for context: {}", contextTemplate.getName());
        this.enableDisableScheduledJobs(contextTemplate, modifiedBy, true);
    }

    /**
     * Helper method to enable/disable scheduled jobs.
     *
     * @param contextTemplate the context template to update
     * @param modifiedBy      the user making the change
     * @param disabled        true to disable, false to enable
     */
    private void enableDisableScheduledJobs(ContextTemplate contextTemplate, String modifiedBy, boolean disabled) {
        contextTemplate.setQuartzScheduleDrivenJobsDisabledForContext(disabled);
        ScheduledContextRecord scheduledContextRecord = this.findByName(contextTemplate.getName());

        if (scheduledContextRecord == null) {
            throw new IllegalArgumentException("Context not found: " + contextTemplate.getName());
        }

        scheduledContextRecord.setContext(contextTemplate);
        scheduledContextRecord.setModifiedBy(modifiedBy);
        this.save(scheduledContextRecord);
    }

    /**
     * Create a new ScheduledContextRecord instance.
     * This method needs to be implemented based on the concrete implementation class available.
     *
     * @param contextName     the name of the context
     * @param contextTemplate the context template
     * @return a new ScheduledContextRecord instance
     */
    private ScheduledContextRecord createScheduledContextRecord(String contextName, ContextTemplate contextTemplate) {
        HibernateScheduledContextRecordImpl record =
            new HibernateScheduledContextRecordImpl(contextName);
        record.setContext(contextTemplate);
        return record;
    }

    /**
     * Clone a context template with a new name.
     *
     * @param sourceTemplate    the source template to clone
     * @param clonedContextName the name for the cloned context
     * @return a cloned context template
     */
    private ContextTemplate cloneContextTemplate(ContextTemplate sourceTemplate, String clonedContextName) {
        try {
            // Use Jackson ObjectMapper for deep cloning
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                ScheduledConcurrentObjectMapperFactory.newInstance();

            // Serialize to JSON and deserialize back to create a deep copy
            String json = mapper.writeValueAsString(sourceTemplate);
            ContextTemplate clonedTemplate = mapper.readValue(json, ContextTemplate.class);

            // Update the name to the cloned context name
            clonedTemplate.setName(clonedContextName);

            logger.debug("Successfully cloned context template from '{}' to '{}'",
                sourceTemplate.getName(), clonedContextName);

            return clonedTemplate;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Failed to clone context template", e);
            throw new RuntimeException("Failed to clone context template: " + e.getMessage(), e);
        }
    }
}
