package org.ikasan.setup.service;

import org.ikasan.setup.dao.SetupDao;
import org.ikasan.setup.model.DashboardPlatformSetup;
import org.ikasan.spec.solr.SolrServiceBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solr implementation of SetupService.
 */
public class SolrSetupServiceImpl extends SolrServiceBase implements SetupService {

    private static final Logger logger = LoggerFactory.getLogger(SolrSetupServiceImpl.class);

    private SetupDao setupDao;

    /**
     * Constructor
     *
     * @param setupDao the setup DAO implementation
     */
    public SolrSetupServiceImpl(SetupDao setupDao) {
        this.setupDao = setupDao;
        if (this.setupDao == null) {
            throw new IllegalArgumentException("setupDao cannot be null!");
        }
    }

    @Override
    public DashboardPlatformSetup getDashboardPlatformSetup() {
        logger.debug("Retrieving dashboard platform setup");
        return setupDao.getDashboardPlatformSetup();
    }

    @Override
    public void save(DashboardPlatformSetup dashboardPlatformSetup) {
        if (dashboardPlatformSetup == null) {
            throw new IllegalArgumentException("DashboardPlatformSetup cannot be null");
        }

        logger.debug("Saving dashboard platform setup: {}", dashboardPlatformSetup);

        setupDao.save(dashboardPlatformSetup);
    }
}
