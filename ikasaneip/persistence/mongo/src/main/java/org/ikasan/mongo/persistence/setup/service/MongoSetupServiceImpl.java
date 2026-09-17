package org.ikasan.mongo.persistence.setup.service;

import org.ikasan.spec.persistence.dao.SetupDao;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.persistence.service.SetupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDB implementation of SetupService.
 */
public class MongoSetupServiceImpl implements SetupService {

    private static final Logger logger = LoggerFactory.getLogger(MongoSetupServiceImpl.class);

    private final SetupDao setupDao;

    /**
     * Constructor
     *
     * @param setupDao the setup DAO implementation
     */
    public MongoSetupServiceImpl(SetupDao setupDao) {
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
