package org.ikasan.setup.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.setup.model.DashboardPlatformSetup;
import org.ikasan.setup.model.DashboardSetupItem;
import org.ikasan.setup.model.SolrDashboardPlatformSetupImpl;
import org.ikasan.setup.util.SolrSetupObjectMapperFactory;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * Solr implementation of SetupDao.
 */
public class SolrSetupDaoImpl extends SolrDaoBase<DashboardPlatformSetup> implements SetupDao {

    private static final Logger logger = LoggerFactory.getLogger(SolrSetupDaoImpl.class);

    private static final String DASHBOARD_PLATFORM_SETUP = "dashboardPlatformSetup";
    private static final String DASHBOARD_PLATFORM_SETUP_ID = "dashboardPlatformSetup";

    private final JsonMapper objectMapper = SolrSetupObjectMapperFactory.newInstance();

    /**
     * Converts a DashboardPlatformSetup entity to a SolrInputDocument.
     *
     * @param expiry the expiry time in milliseconds
     * @param dashboardPlatformSetup the dashboard platform setup entity
     * @return the SolrInputDocument
     */
    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, DashboardPlatformSetup dashboardPlatformSetup) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, DASHBOARD_PLATFORM_SETUP);
        document.addField(ID, DASHBOARD_PLATFORM_SETUP_ID);

        try {
            document.addField(PAYLOAD_CONTENT, getDashboardPlatformSetupContent(dashboardPlatformSetup.getPlatformSetupItems()));
        } catch (JacksonException e) {
            throw new RuntimeException(String.format("Cannot convert dashboard platform setup to string! [%s]", dashboardPlatformSetup), e);
        }

        document.addField(CREATED_DATE_TIME, dashboardPlatformSetup.getTimestamp());
        document.addField(UPDATED_DATE_TIME, dashboardPlatformSetup.getModifiedTimestamp());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        return document;
    }

    /**
     * Serializes a list of {@code DashboardSetupItem} objects into a JSON string representation.
     *
     * @param dashboardSetupItems a list of {@code DashboardSetupItem} instances representing
     *                               the setup configuration for the dashboard platform
     * @return a JSON string representation of the provided {@code dashboardSetupItems} list
     * @ if an error occurs during JSON serialization
     */
    private String getDashboardPlatformSetupContent(List<DashboardSetupItem> dashboardSetupItems)  {
        return this.objectMapper.writeValueAsString(dashboardSetupItems);
    }

    @Override
    public DashboardPlatformSetup getDashboardPlatformSetup() {
        SolrQuery query = super.buildIdQuery(DASHBOARD_PLATFORM_SETUP_ID, DASHBOARD_PLATFORM_SETUP);

        logger.debug("getDashboardPlatformSetup query: {}", query);

        SearchResults<? extends DashboardPlatformSetup> searchResults = this
            .findByQuery(query, SolrDashboardPlatformSetupImpl.class, 0, 1);

        if (!searchResults.getResultList().isEmpty()) {
            return searchResults.getResultList().get(0);
        } else {
            logger.debug("No dashboard platform setup found, returning null");
            return null;
        }
    }

    @Override
    public void save(DashboardPlatformSetup dashboardPlatformSetup) {
        if (dashboardPlatformSetup == null) {
            throw new IllegalArgumentException("DashboardPlatformSetup cannot be null");
        }

        // Set the modified timestamp
        dashboardPlatformSetup.setModifiedTimestamp(System.currentTimeMillis());

        // If this is a new setup (no timestamp), set the creation timestamp
        if (dashboardPlatformSetup.getTimestamp() == 0) {
            dashboardPlatformSetup.setTimestamp(System.currentTimeMillis());
        }

        // Ensure the ID is set
        if (dashboardPlatformSetup.getId() == null || dashboardPlatformSetup.getId().isEmpty()) {
            dashboardPlatformSetup.setId(DASHBOARD_PLATFORM_SETUP_ID);
        }

        logger.debug("Saving dashboard platform setup: {}", dashboardPlatformSetup);

        // Use the base class save method with DO_NOT_EXPIRE (-1) to persist indefinitely
        super.save(dashboardPlatformSetup);
    }
}
