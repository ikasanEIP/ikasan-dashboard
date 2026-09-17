package org.ikasan.mongo.persistence.setup.dao;

import org.ikasan.mongo.persistence.setup.model.MongoDashboardPlatformSetupImpl;
import org.ikasan.mongo.persistence.setup.repository.MongoDashboardPlatformSetupRepository;
import org.ikasan.spec.persistence.dao.SetupDao;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Optional;

/**
 * MongoDB implementation of SetupDao.
 */
public class MongoSetupDaoImpl implements SetupDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoSetupDaoImpl.class);

    private static final String DASHBOARD_PLATFORM_SETUP = "dashboardPlatformSetup";
    private static final String DASHBOARD_PLATFORM_SETUP_ID = "dashboardPlatformSetup";

    private final MongoDashboardPlatformSetupRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template
     */
    public MongoSetupDaoImpl(MongoDashboardPlatformSetupRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        if (this.repository == null) {
            throw new IllegalArgumentException("repository cannot be null!");
        }
        if (this.mongoTemplate == null) {
            throw new IllegalArgumentException("mongoTemplate cannot be null!");
        }
    }

    @Override
    public DashboardPlatformSetup getDashboardPlatformSetup() {
        logger.debug("Retrieving dashboard platform setup with ID: {}", DASHBOARD_PLATFORM_SETUP_ID);

        Optional<MongoDashboardPlatformSetupImpl> result = repository.findById(DASHBOARD_PLATFORM_SETUP_ID);

        if (result.isPresent()) {
            logger.debug("Found dashboard platform setup: {}", result.get());
            return result.get();
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

        dashboardPlatformSetup.setId(DASHBOARD_PLATFORM_SETUP_ID);
        dashboardPlatformSetup.setType(DASHBOARD_PLATFORM_SETUP);

        logger.debug("Saving dashboard platform setup: {}", dashboardPlatformSetup);

        // Convert to MongoDB implementation if necessary
        MongoDashboardPlatformSetupImpl mongoEntity;
        if (dashboardPlatformSetup instanceof MongoDashboardPlatformSetupImpl) {
            mongoEntity = (MongoDashboardPlatformSetupImpl) dashboardPlatformSetup;
        } else {
            // Copy data to MongoDB entity
            mongoEntity = new MongoDashboardPlatformSetupImpl();
            mongoEntity.setId(dashboardPlatformSetup.getId());
            mongoEntity.setType(dashboardPlatformSetup.getType());
            mongoEntity.setPlatformSetupItems(dashboardPlatformSetup.getPlatformSetupItems());
            mongoEntity.setTimestamp(dashboardPlatformSetup.getTimestamp());
            mongoEntity.setModifiedTimestamp(dashboardPlatformSetup.getModifiedTimestamp());
        }

        repository.save(mongoEntity);
        logger.debug("Successfully saved dashboard platform setup");
    }
}
