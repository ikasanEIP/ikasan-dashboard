package org.ikasan.mongo.persistence.scheduled.event.dao;

import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.mongo.persistence.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.mongo.persistence.scheduled.event.repository.MongoScheduledProcessEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of ScheduledProcessEventDao.
 *
 * @author Ikasan Development Team
 */
public class MongoScheduledProcessEventDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledProcessEventDao.class);

    private static final String SCHEDULED_PROCESS_EVENT = "scheduledProcessEvent";

    private final MongoScheduledProcessEventRepository repository;
    private final MongoTemplate mongoTemplate;
    private final int daysToKeep;
    private final JsonMapper objectMapper;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template
     * @param daysToKeep the number of days to keep events
     */
    public MongoScheduledProcessEventDao(MongoScheduledProcessEventRepository repository,
                                         MongoTemplate mongoTemplate,
                                         int daysToKeep) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.daysToKeep = daysToKeep;
        this.objectMapper = JsonMapper.builder().build();
    }

    /**
     * Save a single ScheduledProcessEvent
     *
     * @param event the event to save
     */
    public void save(ContextualisedScheduledProcessEventImpl event) {
        ContextualisedScheduledProcessEventImpl entity = convertToEntity(event);
        repository.save(entity);
        logger.debug("Saved scheduled process event with id: {}", entity.getId());
    }

    /**
     * Save multiple ScheduledProcessEvents
     *
     * @param events the list of events to save
     */
    public void save(List<ContextualisedScheduledProcessEventImpl> events) {
        List<ContextualisedScheduledProcessEventImpl> entities = events.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
        repository.saveAll(entities);
        logger.debug("Saved {} scheduled process events", events.size());
    }

    /**
     * Get all distinct agent names
     *
     * @return list of distinct agent names
     */
    public List<String> getAllAgentNames() {
        logger.debug("Getting all distinct agent names");

        Query query = new Query();
        query.fields().include("agentName");

        List<ContextualisedScheduledProcessEventImpl> events = mongoTemplate.find(query, ContextualisedScheduledProcessEventImpl.class);

        return events.stream()
                .map(ContextualisedScheduledProcessEventImpl::getAgentName)
                .filter(name -> name != null && !name.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get scheduled process events by agent and date range
     *
     * @param agent the agent name
     * @param startTime the start time in milliseconds
     * @param endTime the end time in milliseconds
     * @return search results containing the events
     */
    public ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> getScheduleProcessEvents(
            String agent, long startTime, long endTime) {

        logger.debug("Getting scheduled process events for agent: {}, startTime: {}, endTime: {}",
                agent, startTime, endTime);

        long queryStartTime = System.currentTimeMillis();

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (agent != null && !agent.isEmpty()) {
            criteriaList.add(Criteria.where("agentName").is(agent));
        }

        criteriaList.add(Criteria.where("fireTime").gte(startTime).lte(endTime));

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Sort by fireTime descending
        query.with(Sort.by(Sort.Direction.DESC, "fireTime"));

        // Get total count
        long totalCount = mongoTemplate.count(query, ContextualisedScheduledProcessEventImpl.class);

        // Execute query
        List<ContextualisedScheduledProcessEventImpl> results = mongoTemplate.find(query, ContextualisedScheduledProcessEventImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled process events (total: {})", results.size(), totalCount);

        return new ScheduledProcessEventSearchResults<>(results, totalCount, queryTime);
    }

    /**
     * Get scheduled process events with complex filtering
     *
     * @param accessibleModules list of accessible module/agent names
     * @param startTime start time in milliseconds
     * @param endTime end time in milliseconds
     * @param filter filter string to search in event JSON
     * @param failuresOnly if true, return only failed events
     * @param start pagination start index
     * @param limit pagination limit
     * @param sortOrder sort order ("asc" or "desc")
     * @return search results containing the events
     */
    public ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> getScheduleProcessEvents(
            List<String> accessibleModules, long startTime, long endTime, String filter,
            boolean failuresOnly, int start, int limit, String sortOrder) {

        logger.debug("Getting scheduled process events with filter - modules: {}, startTime: {}, endTime: {}, filter: {}, failuresOnly: {}, start: {}, limit: {}, sortOrder: {}",
                accessibleModules, startTime, endTime, filter, failuresOnly, start, limit, sortOrder);

        long queryStartTime = System.currentTimeMillis();

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Filter by date range
        criteriaList.add(Criteria.where("fireTime").gte(startTime).lte(endTime));

        // Filter by accessible modules
        if (accessibleModules != null && accessibleModules.size() > 0) {
            criteriaList.add(Criteria.where("agentName").in(accessibleModules));
        } else if (accessibleModules != null && accessibleModules.size() == 0) {
            // No accessible modules - return empty results
            criteriaList.add(Criteria.where("agentName").is("NOTAVALIDMODULENAME"));
        }

        // Filter by failures only
        if (failuresOnly) {
            criteriaList.add(Criteria.where("successful").is(false));
        }

        // Filter by search term in JSON payload
        if (filter != null && !filter.isEmpty()) {
            try {
                // Convert event to JSON and search within it
                // We'll use a regex pattern to search across multiple fields
                List<Criteria> filterCriteria = new ArrayList<>();
                String regex = ".*" + Pattern.quote(filter) + ".*";

                // Search in key text fields
                filterCriteria.add(Criteria.where("commandLine").regex(regex, "i"));
                filterCriteria.add(Criteria.where("outcome").regex(regex, "i"));
                filterCriteria.add(Criteria.where("resultOutput").regex(regex, "i"));
                filterCriteria.add(Criteria.where("resultError").regex(regex, "i"));
                filterCriteria.add(Criteria.where("jobName").regex(regex, "i"));
                filterCriteria.add(Criteria.where("jobGroup").regex(regex, "i"));
                filterCriteria.add(Criteria.where("agentName").regex(regex, "i"));

                criteriaList.add(new Criteria().orOperator(filterCriteria.toArray(new Criteria[0])));
            } catch (Exception e) {
                logger.warn("Error applying filter: {}", filter, e);
            }
        }

        // Apply all criteria
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Get total count before pagination
        long totalCount = mongoTemplate.count(query, ContextualisedScheduledProcessEventImpl.class);

        // Apply sorting
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        query.with(Sort.by(direction, "fireTime"));

        // Apply pagination
        if (limit > 0) {
            int page = start / limit;
            Pageable pageable = PageRequest.of(page, limit);
            query.with(pageable);
        } else {
            query.skip(start);
        }

        // Execute query
        List<ContextualisedScheduledProcessEventImpl> results = mongoTemplate.find(query, ContextualisedScheduledProcessEventImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled process events (total: {}, queryTime: {}ms)",
                results.size(), totalCount, queryTime);

        return new ScheduledProcessEventSearchResults<>(results, totalCount, queryTime);
    }

    /**
     * Get scheduled process events by agent, job group, and job name
     *
     * @param agentName the agent name (optional)
     * @param jobGroupName the job group name (optional)
     * @param jobName the job name (optional)
     * @param startTime start time in milliseconds
     * @param endTime end time in milliseconds
     * @param start pagination start index
     * @param limit pagination limit
     * @param sortOrder sort order ("asc" or "desc")
     * @return search results containing the events
     */
    public ScheduledProcessEventSearchResults<ContextualisedScheduledProcessEventImpl> getScheduleProcessEvents(
            String agentName, String jobGroupName, String jobName, long startTime, long endTime,
            int start, int limit, String sortOrder) {

        logger.debug("Getting scheduled process events - agent: {}, jobGroup: {}, jobName: {}, startTime: {}, endTime: {}, start: {}, limit: {}, sortOrder: {}",
                agentName, jobGroupName, jobName, startTime, endTime, start, limit, sortOrder);

        long queryStartTime = System.currentTimeMillis();

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // Filter by agent name
        if (agentName != null && !agentName.isEmpty()) {
            criteriaList.add(Criteria.where("agentName").is(agentName));
        }

        // Filter by job group
        if (jobGroupName != null && !jobGroupName.isEmpty()) {
            criteriaList.add(Criteria.where("jobGroup").is(jobGroupName));
        }

        // Filter by job name
        if (jobName != null && !jobName.isEmpty()) {
            criteriaList.add(Criteria.where("jobName").is(jobName));
        }

        // Filter by date range
        criteriaList.add(Criteria.where("fireTime").gte(startTime).lte(endTime));

        // Apply all criteria
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Get total count before pagination
        long totalCount = mongoTemplate.count(query, ContextualisedScheduledProcessEventImpl.class);

        // Apply sorting
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        query.with(Sort.by(direction, "fireTime"));

        // Apply pagination
        if (limit > 0) {
            int page = start / limit;
            Pageable pageable = PageRequest.of(page, limit);
            query.with(pageable);
        } else {
            query.skip(start);
        }

        // Execute query
        List<ContextualisedScheduledProcessEventImpl> results = mongoTemplate.find(query, ContextualisedScheduledProcessEventImpl.class);

        long queryTime = System.currentTimeMillis() - queryStartTime;

        logger.debug("Found {} scheduled process events (total: {}, queryTime: {}ms)",
                results.size(), totalCount, queryTime);

        return new ScheduledProcessEventSearchResults<>(results, totalCount, queryTime);
    }

    /**
     * Delete expired scheduled process events
     * Deletes events older than the configured retention period based on fireTime
     */
    public void deleteExpired() {
        long expiryThreshold = System.currentTimeMillis() - (daysToKeep * TimeUnit.DAYS.toMillis(1));
        repository.deleteByFireTimeLessThan(expiryThreshold);
        logger.debug("Deleted scheduled process events with fireTime before: {}", expiryThreshold);
    }

    /**
     * Convert ScheduledProcessEvent to entity
     *
     * @param event the event to convert
     * @return the entity ready for persistence
     */
    private ContextualisedScheduledProcessEventImpl convertToEntity(ContextualisedScheduledProcessEventImpl event) {
        // ContextualisedScheduledProcessEventImpl is used as-is
        // Generate a unique ID if not already set
        if (event.getId() == null) {
            // Use fireTime and UUID to generate a unique Long ID
            long uniqueId = System.currentTimeMillis() + UUID.randomUUID().getMostSignificantBits();
            try {
                java.lang.reflect.Field idField = ContextualisedScheduledProcessEventImpl.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(event, Math.abs(uniqueId));
            } catch (Exception e) {
                logger.warn("Could not set ID on ContextualisedScheduledProcessEventImpl", e);
            }
        }
        // Note: The model doesn't have an expiry field - TTL will need to be managed via MongoDB TTL index
        return event;
    }
}
