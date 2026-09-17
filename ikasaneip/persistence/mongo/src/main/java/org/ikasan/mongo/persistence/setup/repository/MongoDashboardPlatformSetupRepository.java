package org.ikasan.mongo.persistence.setup.repository;

import org.ikasan.mongo.persistence.setup.model.MongoDashboardPlatformSetupImpl;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for DashboardPlatformSetup.
 */
public interface MongoDashboardPlatformSetupRepository extends MongoRepository<MongoDashboardPlatformSetupImpl, String> {
}
