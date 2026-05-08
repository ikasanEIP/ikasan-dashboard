package org.ikasan.scheduled.job.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.job.model.SolrSchedulerJobRecordImpl;
import org.ikasan.solr.util.SolrSpecialCharacterEscapeUtil;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SolrSchedulerJobDaoImpl extends SolrDaoBase<SchedulerJobRecord>
    implements SchedulerJobDao<SchedulerJobRecord> {

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrSchedulerJobDaoImpl.class);

    @Override
    public SearchResults<SchedulerJobRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(OPEN_BRACKET);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
        typeBuffer.append(OR);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");
        typeBuffer.append(CLOSE_BRACKET);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrSchedulerJobRecordImpl.class);
    }

    @Override
    public SearchResults<SchedulerJobRecord> findByContext(String contextName, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(OPEN_BRACKET);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");
        queryBuffer.append(CLOSE_BRACKET);
        queryBuffer.append(AND).append(OPEN_BRACKET);

        queryBuffer.append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextName).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(COMPONENT_NAME).append(COLON).append(JobConstants.GLOBAL_EVENT);

        queryBuffer.append(CLOSE_BRACKET);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrSchedulerJobRecordImpl.class, offset, limit);
    }

    @Override
    public SearchResults<SchedulerJobRecord> findByAgent(String agentName, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(OPEN_BRACKET);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");
        queryBuffer.append(CLOSE_BRACKET);
        queryBuffer.append(AND).append(" ").append(MODULE_NAME).append(COLON);
        queryBuffer.append("\"").append(agentName).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrSchedulerJobRecordImpl.class, offset, limit);
    }

    @Override
    public SearchResults<SchedulerJobRecord> findByFilter(SchedulerJobSearchFilter filter, int limit, int offset, String sortColumn, String sortDirection) {
        StringBuffer queryBuffer = new StringBuffer();

        queryBuffer.append(OPEN_BRACKET);
        if((filter.getJobTypeFilter() == null || filter.getJobTypeFilter().isEmpty()) &&
            (filter.getJobTypes() == null || filter.getJobTypes().isEmpty())) {
            queryBuffer.append(OPEN_BRACKET);
            queryBuffer.append(TYPE + COLON);
            queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
            queryBuffer.append(OR).append(" ");
            queryBuffer.append(TYPE + COLON);
            queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
            queryBuffer.append(OR).append(" ");
            queryBuffer.append(TYPE + COLON);
            queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
            queryBuffer.append(OR).append(" ");
            queryBuffer.append(TYPE + COLON);
            queryBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
            queryBuffer.append(OR);
            queryBuffer.append(TYPE + COLON);
            queryBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
            queryBuffer.append(OR);
            queryBuffer.append(TYPE + COLON);
            queryBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
            queryBuffer.append(OR);
            queryBuffer.append(TYPE + COLON);
            queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");
            queryBuffer.append(CLOSE_BRACKET);
        }
        else if (filter.getJobTypeFilter() != null && !filter.getJobTypeFilter().isEmpty()) {
            if(filter.getJobTypeFilter().equals(JobConstants.FILE_EVENT_DRIVEN_JOB)) {
                queryBuffer.append(TYPE + COLON);
                queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
            }
            else if(filter.getJobTypeFilter().equals(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB)) {
                queryBuffer.append(TYPE + COLON);
                queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
            }
            else if(filter.getJobTypeFilter().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB)) {
                queryBuffer.append(TYPE + COLON);
                queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
            }
            else if(filter.getJobTypeFilter().equals(JobConstants.GLOBAL_EVENT_JOB)) {
                queryBuffer.append(TYPE + COLON);
                queryBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
            }
            else if(filter.getJobTypeFilter().equals(JobConstants.CONTEXT_START_JOB)) {
                queryBuffer.append(TYPE + COLON);
                queryBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
            }
            else if(filter.getJobTypeFilter().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                queryBuffer.append(TYPE + COLON);
                queryBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
            }
            else if(filter.getJobTypeFilter().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE)) {
                queryBuffer.append(TYPE + COLON);
                queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");
            }
        }
        else if (filter.getJobTypes() != null && !filter.getJobTypes().isEmpty()) {
            queryBuffer.append(OPEN_BRACKET);
            for (int i = 0; i < filter.getJobTypes().size(); i++) {
                String type = filter.getJobTypes().get(i);
                queryBuffer.append(TYPE + COLON);
                queryBuffer.append("\"").append(type).append("\" ");

                if (i != filter.getJobTypes().size() - 1) {
                    queryBuffer.append(OR).append(" ");
                }
            }
            queryBuffer.append(CLOSE_BRACKET);
        }

        if(filter.getJobNameFilter() != null && !filter.getJobNameFilter().isEmpty()) {
            queryBuffer.append(AND)
                .append(FLOW_NAME)
                .append(COLON)
                .append(WILDCARD)
                .append(SolrSpecialCharacterEscapeUtil.escape(filter.getJobNameFilter()))
                .append(WILDCARD);
        }

        if(filter.getDisplayNameFilter() != null && !filter.getDisplayNameFilter().isEmpty()) {
            queryBuffer.append(AND)
                .append(DISPLAY_NAME)
                .append(COLON)
                .append(WILDCARD)
                .append(SolrSpecialCharacterEscapeUtil.escape(filter.getDisplayNameFilter()))
                .append(WILDCARD);
        }

        if(filter.getNotJobNameInFilter() != null && !filter.getNotJobNameInFilter().isEmpty()) {
            StringBuffer orString = new StringBuffer();
            filter.getNotJobNameInFilter().forEach(jobName ->
                orString.append(jobName).append(OR));

            String finalOr = orString.toString();
            finalOr = finalOr.substring(0, finalOr.lastIndexOf(OR));

            queryBuffer.append(AND)
                .append(" NOT ")
                .append(FLOW_NAME)
                .append(COLON)
                .append(OPEN_BRACKET)
                .append(finalOr)
                .append(CLOSE_BRACKET);
        }

        if((filter.getContextNames() == null || filter.getContextNames().isEmpty())
            && filter.getContextSearchFilter() != null && !filter.getContextSearchFilter().isEmpty()) {
            queryBuffer.append(AND)
                .append(OPEN_BRACKET)
                .append(COMPONENT_NAME)
                .append(COLON)
                .append("\"").append(filter.getContextSearchFilter()).append("\"")
                .append(OR)
                .append(COMPONENT_NAME)
                .append(COLON)
                .append(JobConstants.GLOBAL_EVENT)
                .append(CLOSE_BRACKET);
        }

        if(filter.getContextNames() != null && !filter.getContextNames().isEmpty()) {
            queryBuffer.append(AND).append(OPEN_BRACKET);
            List<String> predicates = new ArrayList<>();
            filter.getContextNames().forEach(name -> predicates.add(new StringBuffer().append(COMPONENT_NAME)
                .append(COLON).append("\"").append(name).append("\"").toString()));
            queryBuffer.append(predicates.stream().collect(Collectors.joining(OR)));
            queryBuffer.append(CLOSE_BRACKET);
        }

        if(filter.isHeld()) {
            queryBuffer.append(AND)
                .append(HELD)
                .append(COLON)
                .append(true);
        }

        if(filter.isSkipped()) {
            queryBuffer.append(AND)
                .append(SKIPPED)
                .append(COLON)
                .append(true);
        }

        if(filter.isTargetResidingContextOnly() != null) {
            queryBuffer.append(AND)
                .append(TARGET_RESIDING_CONTEXT_ONLY)
                .append(COLON)
                .append(filter.isTargetResidingContextOnly().booleanValue());
        }

        if(filter.isParticipatesInLock() != null) {
            queryBuffer.append(AND)
                .append(PARTICIPATES_IN_LOCK)
                .append(COLON)
                .append(filter.isParticipatesInLock());
        }

        queryBuffer.append(CLOSE_BRACKET);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());

        if(sortColumn != null && !sortColumn.isEmpty() && sortDirection != null && !sortDirection.isEmpty()) {
            solrQuery.addSort(sortColumn, sortDirection.equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            solrQuery.addSort(FLOW_NAME, SolrQuery.ORDER.desc);
        }

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrSchedulerJobRecordImpl.class, offset, limit);
    }

    @Override
    public SchedulerJobRecord findById(String id) {
        SolrQuery query = this.buildIdQuery(id);

        logger.debug("query: " + query);

        List<? extends SchedulerJobRecord> beans = this.findByQuery(query, SolrSchedulerJobRecordImpl.class).getResultList();

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }

    @Override
    public SchedulerJobRecord findByContextIdAndJobName(String contextId, String jobName) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(OPEN_BRACKET);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");
        queryBuffer.append(CLOSE_BRACKET);

        queryBuffer.append(AND).append(FLOW_NAME).append(COLON);
        queryBuffer.append("\"").append(jobName).append("\" ");


        queryBuffer.append(AND).append(OPEN_BRACKET);

        queryBuffer.append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextId).append("\" ");
        queryBuffer.append(OR).append(COMPONENT_NAME).append(COLON).append(JobConstants.GLOBAL_EVENT);

        queryBuffer.append(CLOSE_BRACKET);

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());

        List<? extends SchedulerJobRecord> beans = this.findByQuery(solrQuery, SolrSchedulerJobRecordImpl.class).getResultList();

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }

    @Override
    public void delete(SchedulerJobRecord record) {
        super.removeById(record.getType(), record.getId());
    }

    @Override
    public void deleteByAgentName(String agentName) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(OPEN_BRACKET);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
        queryBuffer.append(CLOSE_BRACKET);
        queryBuffer.append(AND).append(" ").append(MODULE_NAME).append(COLON);
        queryBuffer.append("\"").append(agentName).append("\" ");

        super.deleteByQuery(queryBuffer.toString());
    }

    @Override
    public void deleteByContextName(String contextName) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(OPEN_BRACKET);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        queryBuffer.append(OR).append(" ");
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
        queryBuffer.append(OR);
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
        queryBuffer.append(CLOSE_BRACKET);
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextName).append("\" ");

        super.deleteByQuery(queryBuffer.toString());
    }

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SchedulerJobRecord event) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    @Override
    public void save(SchedulerJobRecord event) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    @Override
    public void save(List<SchedulerJobRecord> events) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    protected SolrQuery buildIdQuery(String id)
    {
        StringBuffer idBuffer = new StringBuffer();
        StringBuffer typeBuffer = new StringBuffer();


        idBuffer.append(ID).append(COLON).append("\"").append(id).append("\"");

        typeBuffer.append(OPEN_BRACKET);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
        typeBuffer.append(OR);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
        typeBuffer.append(OR);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
        typeBuffer.append(OR);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE).append("\" ");
        typeBuffer.append(CLOSE_BRACKET);


        StringBuffer bufferFinalQuery = new StringBuffer(idBuffer);

        if(typeBuffer.length() > 0)
        {
            bufferFinalQuery.append(AND).append(typeBuffer);
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(bufferFinalQuery.toString());

        return solrQuery;
    }

    /**
     * Retrieves all the names of agents that are defined as scheduler agents.
     *
     * The method performs a Solr field facet query to extract all unique values
     * from the "id" field of documents matching the query
     * `type:"moduleMetaData" AND payload:"*\"type\":\"SCHEDULER_AGENT\"*"`.
     *
     * @return a list of agent names extracted from the Solr query results.
     */
    @Override
    public List<String> getAllAgentNames() {
        return super.fieldFacetQuery("type:\"moduleMetaData\" AND payload:\"*\\\"type\\\":\\\"SCHEDULER_AGENT\\\"*\"", "id");
    }
}
