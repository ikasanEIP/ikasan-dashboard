package org.ikasan.mongo.persistence.scheduled.joblock.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoJobLockCacheAuditRecordImpl extends MongoJobLockCacheRecordImpl implements JobLockCacheAuditRecord {
}
