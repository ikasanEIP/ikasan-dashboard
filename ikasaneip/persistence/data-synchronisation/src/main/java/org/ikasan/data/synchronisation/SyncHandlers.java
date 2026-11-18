package org.ikasan.data.synchronisation;

import org.apache.solr.common.SolrInputDocument;
import org.bson.Document;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.mongo.persistence.error.reporting.model.MongoErrorOccurrence;
import org.ikasan.mongo.persistence.exclusion.model.MongoExclusionEvent;
import org.ikasan.mongo.persistence.hospital.model.MongoExclusionEventAction;
import org.ikasan.mongo.persistence.metrics.model.FlowInvocationMetricImpl;
import org.ikasan.mongo.persistence.metrics.model.MongoFlowInvocationMetric;
import org.ikasan.mongo.persistence.replay.model.MongoReplayAuditEventImpl;
import org.ikasan.mongo.persistence.replay.model.MongoReplayEventImpl;
import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextRecordImpl;
import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextViewRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoSchedulerJobInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoBridgingJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextStartJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextTerminalJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoFileEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoGlobalEventJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoInternalEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheAuditRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoEmailNotificationContextRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoEmailNotificationDetailsRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoNotificationSendAuditRecordImpl;
import org.ikasan.mongo.persistence.scheduled.profile.model.MongoContextProfileRecordImpl;
import org.ikasan.mongo.persistence.security.model.MongoAuthenticationMethodRecord;
import org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalRecord;
import org.ikasan.mongo.persistence.security.model.MongoPolicyRecord;
import org.ikasan.mongo.persistence.security.model.MongoRoleRecord;
import org.ikasan.mongo.persistence.security.model.MongoUserRecord;
import org.ikasan.mongo.persistence.setup.model.MongoDashboardPlatformSetupImpl;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventRecordImpl;
import org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metrics.MetricsDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.security.dao.AuthenticationMethodDao;
import org.ikasan.spec.security.dao.IkasanPrincipalDao;
import org.ikasan.spec.security.dao.PolicyDao;
import org.ikasan.spec.security.dao.RoleDao;
import org.ikasan.spec.security.dao.UserDao;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.security.model.User;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds the {@link SyncHandler} for every Mongo {@code type} that has a Solr counterpart.
 *
 * <p>For most types the Mongo model class implements the interface the Solr DAO saves, so the document is read with
 * the Mongo converter and passed straight to the Solr DAO. Security records and metadata are stored in Mongo in a
 * different shape, so they are read back through their Mongo DAO.
 *
 * <p>Types whose Solr DAO generates a new id on every save (metrics, the job lock cache audit, the context instance
 * audit aggregate and scheduled process events) are not {@link SyncHandler#deletable() deletable}: they are written
 * once, and expire through Solr's own housekeeping.
 */
public final class SyncHandlers {

    private final MongoConverter converter;
    private final SolrDaos solr;
    private final MongoLookups mongo;
    private final Map<String, SyncHandler> handlers = new LinkedHashMap<>();

    private static final JsonMapper METRIC_MAPPER = JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();

    private SyncHandlers(MongoConverter converter, SolrDaos solr, MongoLookups mongo) {
        this.converter = converter;
        this.solr = solr;
        this.mongo = mongo;
    }

    /**
     * @return every handler, keyed by Mongo type
     */
    public static Map<String, SyncHandler> create(MongoConverter converter, SolrDaos solr, MongoLookups mongo) {
        SyncHandlers builder = new SyncHandlers(converter, solr, mongo);
        builder.addAll();
        return Map.copyOf(builder.handlers);
    }

    private void addAll() {
        // ESB entities
        entity(EntityDao.WIRETAP_TYPE, MongoWiretapEventImpl.class, saveAll(solr.wiretap));
        entity(EntityDao.ERROR, MongoErrorOccurrence.class, saveAll(solr.error));
        entity(EntityDao.EXCLUSION, MongoExclusionEvent.class, saveAll(solr.exclusion));
        entity(EntityDao.EXCLUSION_EVENT_ACTION, MongoExclusionEventAction.class, saveAll(solr.hospital));
        replay();
        entity(EntityDao.SYSTEM_EVENT_TYPE, MongoSystemEventRecordImpl.class, saveAll(solr.systemEvent));
        replayAudit();
        insertOnly(MetricsDao.METRIC_ENTITY_TYPE, SyncCollections.IKASAN, this::metric, saveAll(solr.metrics));

        // scheduler
        entity(ScheduledContextDao.SCHEDULED_CONTEXT_TYPE, MongoScheduledContextRecordImpl.class,
            saveAll(solr.scheduledContext));
        entity(ScheduledContextViewDao.SCHEDULED_CONTEXT_VIEW_TYPE, MongoScheduledContextViewRecordImpl.class,
            saveAll(solr.scheduledContextView));
        entity(ContextProfileDao.CONTEXT_PROFILE_TYPE, MongoContextProfileRecordImpl.class, saveAll(solr.contextProfile));
        entity(ScheduledContextInstanceDao.SCHEDULED_CONTEXT_INSTANCE_TYPE, MongoScheduledContextInstanceRecordImpl.class,
            saveAll(solr.scheduledContextInstance));
        entity(ScheduledContextInstanceAuditDao.SCHEDULED_CONTEXT_INSTANCE_AUDIT_TYPE,
            MongoScheduledContextInstanceRecordImpl.class, saveAll(solr.scheduledContextInstanceAudit));
        insertOnly(ScheduledContextInstanceAuditAggregateDao.SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE,
            SyncCollections.IKASAN, read(MongoScheduledContextInstanceAuditAggregateRecordImpl.class),
            saveAll(solr.scheduledContextInstanceAuditAggregate));
        entity(JobLockCacheDao.JOB_LOCK_CACHE_TYPE, MongoJobLockCacheRecordImpl.class, saveAll(solr.jobLockCache));
        insertOnly(JobLockCacheAuditDao.JOB_LOCK_AUDIT_CACHE_TYPE, SyncCollections.IKASAN,
            read(MongoJobLockCacheAuditRecordImpl.class), saveAll(solr.jobLockCacheAudit));
        entity(EmailNotificationContextDao.EMAIL_NOTIFICATION_CONTEXT, MongoEmailNotificationContextRecordImpl.class,
            saveAll(solr.emailNotificationContext));
        entity(EmailNotificationDetailsDao.EMAIL_NOTIFICATION_DETAILS, MongoEmailNotificationDetailsRecordImpl.class,
            saveAll(solr.emailNotificationDetails));
        entity(NotificationSendAuditDao.NOTIFICATION_SEND_AUDIT, MongoNotificationSendAuditRecordImpl.class,
            saveAll(solr.notificationSendAudit));
        insertOnly(SyncCollections.SCHEDULED_PROCESS_EVENT_TYPE, SyncCollections.SCHEDULED_PROCESS_EVENT,
            read(ContextualisedScheduledProcessEventImpl.class), saveAll(solr.scheduledProcessEvent));

        // job definitions
        entity(JobConstants.FILE_EVENT_DRIVEN_JOB, MongoFileEventDrivenJobRecordImpl.class,
            saveAll(solr.fileEventDrivenJob));
        entity(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB, MongoQuartzScheduleDrivenJobRecordImpl.class,
            saveAll(solr.quartzScheduleDrivenJob));
        entity(JobConstants.INTERNAL_EVENT_DRIVEN_JOB, MongoInternalEventDrivenJobRecordImpl.class,
            saveAll(solr.internalEventDrivenJob));
        entity(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE, MongoInternalEventDrivenJobRecordImpl.class,
            saveAll(solr.internalEventDrivenJobTemplate));
        entity(JobConstants.GLOBAL_EVENT_JOB, MongoGlobalEventJobRecordImpl.class, saveAll(solr.globalEventJob));
        entity(JobConstants.CONTEXT_START_JOB, MongoContextStartJobRecordImpl.class, saveAll(solr.contextStartJob));
        entity(JobConstants.CONTEXT_TERMINAL_JOB, MongoContextTerminalJobRecordImpl.class,
            saveAll(solr.contextTerminalJob));
        entity(JobConstants.BRIDGING_JOB, MongoBridgingJobRecordImpl.class, saveAll(solr.bridgingJob));

        // job instances: one model and one Solr DAO for every instance type
        for (String instanceType : List.of(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE,
            JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE,
            JobConstants.GLOBAL_EVENT_JOB_INSTANCE, JobConstants.CONTEXT_START_JOB_INSTANCE,
            JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE, JobConstants.LOCAL_EVENT_JOB_INSTANCE,
            JobConstants.BRIDGING_JOB_INSTANCE)) {
            entity(instanceType, MongoSchedulerJobInstanceRecordImpl.class, saveAll(solr.schedulerJobInstance));
        }

        // metadata, read back through the Mongo DAO
        deletable(ModuleMetadataDao.MODULE_METADATA, doc -> mongo.moduleMetadataDao().findById(id(doc)),
            saveAll(solr.moduleMetadata));
        deletable(BusinessStreamMetadataDao.BUSINESS_STREAM_METADATA,
            doc -> mongo.businessStreamMetadataDao().findById(id(doc)),
            each((BusinessStreamMetaData metaData) -> solr.businessStreamMetadata.save(metaData)));
        deletable(ComponentConfigurationMetadataDao.COMPONENT_CONFIGURATION,
            doc -> mongo.componentConfigurationMetadataDao().findById(id(doc)),
            saveAll(solr.componentConfigurationMetadata));
        entity("dashboardPlatformSetup", MongoDashboardPlatformSetupImpl.class, each((DashboardPlatformSetup setup) -> solr.setup.save(setup)));

        // security, read back through the Mongo DAO by name
        deletable(UserDao.USER_TYPE,
            doc -> mongo.userDao().getUser(converter.read(MongoUserRecord.class, doc).getUsername()),
            each((User user) -> solr.user.save(user)));
        deletable(IkasanPrincipalDao.PRINCIPAL_TYPE,
            doc -> mongo.principalDao().getPrincipalByName(converter.read(MongoIkasanPrincipalRecord.class, doc).getName()),
            each(solr.principal::saveOrUpdatePrincipal));
        deletable(RoleDao.ROLE_TYPE,
            doc -> mongo.roleDao().getRoleByName(converter.read(MongoRoleRecord.class, doc).getName()),
            each(solr.role::saveOrUpdateRole));
        deletable(PolicyDao.POLICY_TYPE,
            doc -> mongo.policyDao().getPolicyByName(converter.read(MongoPolicyRecord.class, doc).getName()),
            each(solr.policy::saveOrUpdatePolicy));
        deletable(AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE,
            doc -> mongo.authenticationMethodDao().getAuthenticationMethodByOrder(
                converter.read(MongoAuthenticationMethodRecord.class, doc).getOrder()),
            each(solr.authenticationMethod::saveOrUpdateAuthenticationMethod));
    }

    /** A type whose Mongo model implements the interface the Solr DAO saves; the Solr id equals the Mongo id. */
    private <S> void entity(String type, Class<? extends S> modelClass, Consumer<List<S>> saver) {
        deletable(type, read(modelClass), saver);
    }

    private <S> void deletable(String type, Function<Document, S> convert, Consumer<List<S>> saver) {
        add(new EntitySyncHandler<>(type, SyncCollections.IKASAN, true, convert, saver));
    }

    private <S> void insertOnly(String type, String collection, Function<Document, S> convert,
                                Consumer<List<S>> saver) {
        add(new EntitySyncHandler<>(type, collection, false, convert, saver));
    }

    private void add(SyncHandler handler) {
        if (handlers.putIfAbsent(handler.type(), handler) != null) {
            throw new IllegalStateException("Duplicate synchronisation handler for type " + handler.type());
        }
    }

    private <S> Function<Document, S> read(Class<? extends S> modelClass) {
        return document -> converter.read(modelClass, document);
    }

    private FlowInvocationMetricImpl metric(Document document) {
        MongoFlowInvocationMetric entity = converter.read(MongoFlowInvocationMetric.class, document);
        return METRIC_MAPPER.readValue(entity.getRawFlowInvocationMetric(), FlowInvocationMetricImpl.class);
    }

    /**
     * Replay audits are stored by both Mongo and Solr as one JSON payload. The Solr DAO can only build that payload
     * from parsed events and gives every document a new id, so the Mongo document is copied field for field instead,
     * keeping its id so deletes and reconciles work.
     */
    private void replayAudit() {
        deletable(EntityDao.REPLAY_AUDIT, document -> {
            MongoReplayAuditEventImpl audit = converter.read(MongoReplayAuditEventImpl.class, document);
            SolrInputDocument solrDocument = new SolrInputDocument();
            solrDocument.addField(EntityFields.ID, audit.getId());
            solrDocument.addField(EntityFields.TYPE, EntityDao.REPLAY_AUDIT);
            solrDocument.addField(EntityFields.PAYLOAD_CONTENT, audit.getReplayAuditJson());
            solrDocument.addField(EntityFields.CREATED_DATE_TIME, audit.getTimestamp());
            solrDocument.setField(EntityFields.EXPIRY, expiry(audit.getExpiry()));
            return solrDocument;
        }, this::saveRaw);
    }

    /**
     * Replay events are copied field for field, as {@code SolrReplayDao} would write them, but keeping the Mongo id:
     * the Solr DAO builds the id from {@code ReplayEvent.getId()}, which {@code MongoReplayEventImpl} cannot return
     * because the Mongo DAO stores replay ids as {@code <module>-replay-<uuid>} (getId() parses them as a number).
     */
    private void replay() {
        deletable(EntityDao.REPLAY, document -> {
            MongoReplayEventImpl replay = converter.read(MongoReplayEventImpl.class, document);
            SolrInputDocument solrDocument = new SolrInputDocument();
            solrDocument.addField(EntityFields.ID, replay.getIdAsString());
            solrDocument.addField(EntityFields.TYPE, EntityDao.REPLAY);
            solrDocument.addField(EntityFields.MODULE_NAME, replay.getModuleName());
            solrDocument.addField(EntityFields.FLOW_NAME, replay.getFlowName());
            solrDocument.addField(EntityFields.EVENT, replay.getEventId());
            solrDocument.addField(EntityFields.PAYLOAD_CONTENT,
                replay.getEventAsString() != null && !replay.getEventAsString().isEmpty()
                    ? replay.getEventAsString()
                    : replay.getEvent() == null ? null : new String(replay.getEvent()));
            solrDocument.addField(EntityFields.PAYLOAD_CONTENT_RAW, replay.getEvent());
            solrDocument.addField(EntityFields.CREATED_DATE_TIME, replay.getTimestamp());
            solrDocument.setField(EntityFields.EXPIRY, expiry(replay.getExpiry()));
            solrDocument.setField(EntityFields.HARVEST_RECEIVED_TIMESTAMP, replay.getHarvestReceivedTimestamp() > 0
                ? replay.getHarvestReceivedTimestamp() : System.currentTimeMillis());
            return solrDocument;
        }, this::saveRaw);
    }

    /** The Mongo expiry if it has one, otherwise the Solr retention from now (as the Solr DAOs do). */
    private long expiry(long mongoExpiry) {
        return mongoExpiry > 0 ? mongoExpiry
            : System.currentTimeMillis() + TimeUnit.DAYS.toMillis(solr.settings().retentionDays());
    }

    private void saveRaw(List<SolrInputDocument> documents) {
        try {
            org.apache.solr.client.solrj.request.UpdateRequest request
                = new org.apache.solr.client.solrj.request.UpdateRequest();
            request.setBasicAuthCredentials(solr.settings().username(), solr.settings().password());
            request.add(documents);
            request.process(solr.solrClient(), SolrConstants.CORE);
            request.commit(solr.solrClient(), SolrConstants.CORE);
        } catch (Exception e) {
            throw new IllegalStateException("Could not write documents to Solr", e);
        }
    }

    private static String id(Document document) {
        return String.valueOf(document.get("_id"));
    }

    /** Save a list through a Solr DAO's {@code save(List)}. */
    private static <S> Consumer<List<S>> saveAll(SolrDaoBase<S> dao) {
        return dao::save;
    }

    /** Adapt a DAO that saves one entity at a time. */
    private static <S> Consumer<List<S>> each(Consumer<S> saveOne) {
        return entities -> entities.forEach(saveOne);
    }
}
