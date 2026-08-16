package org.ikasan.mongo.persistence.scheduled.joblock.model;

import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "jobLockCacheAuditRecord")
public class MongoJobLockCacheAuditRecordImpl extends MongoJobLockCacheRecordImpl implements JobLockCacheAuditRecord {
}
