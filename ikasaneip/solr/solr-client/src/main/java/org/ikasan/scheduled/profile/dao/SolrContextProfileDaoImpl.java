package org.ikasan.scheduled.profile.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceRecordImpl;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheDaoImpl;
import org.ikasan.scheduled.profile.model.SolrContextProfileImpl;
import org.ikasan.scheduled.profile.model.SolrContextProfileRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.solr.util.SolrSpecialCharacterEscapeUtil;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class SolrContextProfileDaoImpl extends SolrDaoBase<ContextProfileRecord> implements ContextProfileDao {

    public static final String CONTEXT_PROFILE_TYPE = "contextProfile";
    private static final ObjectMapper OBJECT_MAPPER = ScheduledObjectMapperFactory.newInstance();
    private static final Logger LOG = LoggerFactory.getLogger(SolrJobLockCacheDaoImpl.class);


    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ContextProfileRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, event.getProfileName() + "-" + event.getContextName() + "-" + CONTEXT_PROFILE_TYPE );
        document.addField(TYPE, CONTEXT_PROFILE_TYPE);
        document.addField(MODULE_NAME, event.getProfileName());
        document.addField(COMPONENT_NAME, event.getContextName());
        document.addField(FLOW_NAME, event.getOwner());

        try {
            if(event.getContextProfile() == null) {
                event.setContextProfile(new SolrContextProfileImpl());
            }

            document.addField(PAYLOAD_CONTENT, OBJECT_MAPPER.writeValueAsString(event.getContextProfile()));

            if(event.getAccessRoles() == null) {
                document.addField(ACCESS_ROLES, OBJECT_MAPPER.writeValueAsString(List.of()));
            }
            else {
                document.addField(ACCESS_ROLES, OBJECT_MAPPER.writeValueAsString(event.getAccessRoles()));
            }

            if(event.getAccessUsers() == null) {
                document.addField(ACCESS_USERS, OBJECT_MAPPER.writeValueAsString(List.of()));
            }
            else {
                document.addField(ACCESS_USERS, OBJECT_MAPPER.writeValueAsString(event.getAccessUsers()));
            }

        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException(String.format("Error converting context profile record! [%s]", event));
        }
        if(event.getCreatedDateTime() == 0) {
            document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
        }
        else {
            document.addField(CREATED_DATE_TIME, event.getCreatedDateTime());
        }

        document.setField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.setField(MODIFIED_BY, event.getModifiedBy());
        document.setField(EXPIRY, expiry);

        LOG.debug(String.format("Converted JobLockCacheRecord to SolrDocument[%s]", document));
        return document;
    }

    @Override
    public ContextProfileRecord findById(String id) {
        SearchResults<ContextProfileRecord> searchResults = this.findByQuery(buildIdQuery(id, CONTEXT_PROFILE_TYPE)
            , SolrContextProfileRecordImpl.class, 0, 1);
        return searchResults.getResultList().size() > 0 ? searchResults.getResultList().get(0) : null;
    }

    @Override
    public SearchResults<ContextProfileRecord> findByFilter(ContextProfileSearchFilter filter, int limit, int offset, String sortColumn, String sortOrder) {
        StringBuffer queryString = new StringBuffer();
        queryString.append(TYPE).append(COLON).append(CONTEXT_PROFILE_TYPE)
            .append(AND)
            .append(MODULE_NAME).append(COLON)
            .append(filter.getProfileName() != null && !filter.getProfileName().isEmpty() ? SolrSpecialCharacterEscapeUtil.escape(filter.getProfileName()) : "*")
            .append(AND)
            .append(COMPONENT_NAME).append(COLON)
            .append(filter.getContextName() != null && !filter.getContextName().isEmpty() ? SolrSpecialCharacterEscapeUtil.escape(filter.getContextName()) : "*")
            .append(AND)
            .append(OPEN_BRACKET);

        if(filter.getOwner() != null && !filter.getOwner().isEmpty()) {
            queryString.append(FLOW_NAME).append(COLON)
                .append(SolrSpecialCharacterEscapeUtil.escape(filter.getOwner()))
                .append(OR);
        }

        queryString.append(FLOW_NAME).append(COLON).append(ContextProfileRecord.SYSTEM_OWNER)
            .append(CLOSE_BRACKET);

        if(filter.getUser() != null && !filter.getUser().isEmpty()) {
            queryString.append(AND)
                .append(ACCESS_USERS)
                .append(COLON)
                .append(WILDCARD)
                .append(filter.getUser())
                .append(WILDCARD);
        }

        if(filter.getAccessRoles() != null && !filter.getAccessRoles().isEmpty()) {
            queryString.append(AND)
                .append(ACCESS_ROLES)
                .append(COLON)
                .append(OPEN_BRACKET);

            queryString.append(filter.getAccessRoles().stream()
                .map(role -> WILDCARD + role + WILDCARD)
                .collect(Collectors.joining(OR)));


            queryString.append(CLOSE_BRACKET);
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryString.toString());

        if(sortColumn != null && !sortColumn.isEmpty()) {
            solrQuery.addSort(sortColumn, sortOrder != null && sortOrder.equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            // Default search to created date time descending
            solrQuery.addSort(CREATED_DATE_TIME, SolrQuery.ORDER.desc);
        }

        return this.findByQuery(solrQuery, SolrContextProfileRecordImpl.class, offset, limit);
    }
}
