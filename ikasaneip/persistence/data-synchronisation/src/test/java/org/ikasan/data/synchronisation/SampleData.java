package org.ikasan.data.synchronisation;

import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.instance.QuartzScheduleDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceRecordImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheAuditRecordImpl;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.instance.BridgingJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextStartJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextTerminalJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.FileEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.GlobalEventJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.LocalEventJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditRecordImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.BridgingJobImpl;
import org.ikasan.job.orchestration.model.job.BridgingJobRecordImpl;
import org.ikasan.job.orchestration.model.job.ContextStartJobImpl;
import org.ikasan.job.orchestration.model.job.ContextStartJobRecordImpl;
import org.ikasan.job.orchestration.model.job.ContextTerminalJobImpl;
import org.ikasan.job.orchestration.model.job.ContextTerminalJobRecordImpl;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobRecordImpl;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.job.orchestration.model.job.GlobalEventJobRecordImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobRecordImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobRecordImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextRecordImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsRecordImpl;
import org.ikasan.job.orchestration.model.notification.NotificationSendAuditImpl;
import org.ikasan.job.orchestration.model.notification.NotificationSendAuditRecordImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.mongo.persistence.business.stream.metadata.model.BusinessStreamMetaDataImpl;
import org.ikasan.mongo.persistence.configuration.metadata.model.MongoConfigurationMetaData;
import org.ikasan.mongo.persistence.configuration.metadata.model.MongoConfigurationParameterMetaData;
import org.ikasan.mongo.persistence.module.metadata.model.MongoModuleMetaDataImpl;
import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextViewRecordImpl;
import org.ikasan.mongo.persistence.security.dao.MongoIkasanPrincipalDaoImpl;
import org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl;
import org.ikasan.mongo.persistence.setup.model.MongoDashboardPlatformSetupImpl;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.persistence.dao.SetupDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.job.dao.BridgingJobDao;
import org.ikasan.spec.scheduled.job.dao.ContextStartJobDao;
import org.ikasan.spec.scheduled.job.dao.ContextTerminalJobDao;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.GlobalEventJobDao;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.security.dao.IkasanPrincipalDao;
import org.ikasan.mongo.persistence.error.reporting.model.MongoErrorOccurrence;
import org.ikasan.mongo.persistence.exclusion.model.MongoExclusionEvent;
import org.ikasan.mongo.persistence.hospital.model.MongoExclusionEventAction;
import org.ikasan.mongo.persistence.metrics.model.FlowInvocationMetricImpl;
import org.ikasan.mongo.persistence.replay.model.MongoReplayAuditEventImpl;
import org.ikasan.mongo.persistence.replay.model.MongoReplayEventImpl;
import org.ikasan.mongo.persistence.scheduled.event.dao.MongoScheduledProcessEventDao;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoInternalEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.security.dao.MongoAuthenticationMethodDaoImpl;
import org.ikasan.mongo.persistence.security.dao.MongoPolicyDaoImpl;
import org.ikasan.mongo.persistence.security.dao.MongoRoleDaoImpl;
import org.ikasan.mongo.persistence.security.model.MongoAuthenticationMethodImpl;
import org.ikasan.mongo.persistence.security.model.MongoPolicyImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleImpl;
import org.ikasan.mongo.persistence.security.model.MongoUserImpl;
import org.ikasan.mongo.persistence.systemevent.model.MongoSystemEventRecordImpl;
import org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.metrics.MetricsDao;
import org.ikasan.spec.replay.ReplayAuditEvent;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.security.dao.UserDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Writes sample entities through the real Mongo DAOs, so the documents in Mongo are exactly what the application
 * writes.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
final class SampleData {

    private final TestEnvironment env;

    SampleData(TestEnvironment env) {
        this.env = env;
    }

    void wiretap(String identifier) {
        MongoWiretapEventImpl event = new MongoWiretapEventImpl();
        event.setId(identifier);
        event.setModuleName("module");
        event.setFlowName("flow");
        event.setComponentName("component");
        event.setEventId("event-" + identifier);
        event.setTimestamp(System.currentTimeMillis());
        event.setEvent("{\"message\":\"" + identifier + "\"}");
        env.bean("wiretapEntityDao", EntityDao.class).save(event);
    }

    void error(String uri) {
        env.bean("errorReportingServiceEntityDao", EntityDao.class).save(new MongoErrorOccurrence(uri, "module",
            "flow", "component", "action", "detail", "message", "java.lang.RuntimeException", "life", "related",
            "event", System.currentTimeMillis()));
    }

    void exclusion(String errorUri) {
        MongoExclusionEvent event = new MongoExclusionEvent("module", "flow", "identifier", "event",
            System.currentTimeMillis(), errorUri);
        env.bean("exclusionEventEntityDao", EntityDao.class).save(event);
    }

    void exclusionEventAction(String errorUri) {
        MongoExclusionEventAction action = new MongoExclusionEventAction("module", "flow", errorUri, "user",
            "resubmit", "event", System.currentTimeMillis(), "comment");
        env.bean("hospitalEntityDao", EntityDao.class).save(action);
    }

    void replay(String eventId) {
        MongoReplayEventImpl event = new MongoReplayEventImpl();
        event.setModuleName("module");
        event.setFlowName("flow");
        event.setEventId(eventId);
        event.setEvent("event".getBytes());
        event.setEventAsString("event");
        event.setTimestamp(System.currentTimeMillis());
        env.bean("replayEntityDao", EntityDao.class).save(event);
    }

    void replayAudit(String id) {
        MongoReplayAuditEventImpl audit = new MongoReplayAuditEventImpl();
        audit.setId(id);
        audit.setSuccess(true);
        audit.setResultMessage("ok");
        audit.setTimestamp(System.currentTimeMillis());
        env.bean("replayAuditEntityDao", EntityDao.class).save(audit);
    }

    void systemEvent(long id) {
        MongoSystemEventRecordImpl event = new MongoSystemEventRecordImpl();
        event.setSystemEventId(id);
        event.setModuleName("module");
        event.setActor("admin");
        event.setAction("CREATE");
        event.setSubject("subject");
        event.setTimestamp(new Date());
        env.bean("systemEventEntityDao", EntityDao.class).save(event);
    }

    void metric(String flowName) {
        FlowInvocationMetricImpl metric = new FlowInvocationMetricImpl();
        metric.setModuleName("module");
        metric.setFlowName(flowName);
        metric.setInvocationStartTime(1000L);
        metric.setInvocationEndTime(2000L);
        metric.setFinalAction("COMPLETE");
        env.bean("metricsDao", MetricsDao.class).save(metric);
    }

    void jobLockCache(String environment) {
        JobLockCacheRecordImpl record = new JobLockCacheRecordImpl();
        record.setEnvironment(environment);
        record.setJobLockCache(new JobLockCacheDataImpl());
        env.bean("jobLockCacheDao", JobLockCacheDao.class).save(record);
    }

    void internalEventDrivenJob(String jobName) {
        InternalEventDrivenJobImpl job = new InternalEventDrivenJobImpl();
        job.setAgentName("agent");
        job.setJobName(jobName);
        job.setContextName("context");
        job.setDisplayName("Display " + jobName);
        MongoInternalEventDrivenJobRecordImpl record = new MongoInternalEventDrivenJobRecordImpl();
        record.setId(JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_agent_" + jobName + "_context");
        record.setAgentName("agent");
        record.setJobName(jobName);
        record.setContextName("context");
        record.setInternalEventDrivenJob(job);
        env.bean("internalEventDrivenJobRecordDao", InternalEventDrivenJobDao.class).save(record);
    }

    void jobInstance(String jobName) {
        QuartzScheduleDrivenJobInstanceImpl instance = new QuartzScheduleDrivenJobInstanceImpl();
        instance.setJobName(jobName);
        instance.setContextName("context");
        instance.setContextInstanceId("context-instance");
        instance.setChildContextName("child");
        instance.setStatus(InstanceStatus.WAITING);
        SchedulerJobInstanceRecordImpl record = new SchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName("context");
        record.setContextInstanceId("context-instance");
        record.setSchedulerJobInstance(instance);
        env.bean("schedulerJobInstanceDao", SchedulerJobInstanceDao.class).save(record);
    }

    void scheduledProcessEvent(String jobName) {
        ContextualisedScheduledProcessEventImpl event = new ContextualisedScheduledProcessEventImpl();
        event.setAgentName("agent");
        event.setJobGroup("group");
        event.setJobName(jobName);
        event.setFireTime(System.currentTimeMillis());
        event.setSuccessful(true);
        event.setOutcome(null);
        event.setCommandLine("command");
        env.context.getBean(MongoScheduledProcessEventDao.class).save(event);
    }

    void policy(String name) {
        MongoPolicyImpl policy = new MongoPolicyImpl();
        policy.setName(name);
        policy.setDescription("policy " + name);
        policy.setCreatedDateTime(new Date());
        policy.setUpdatedDateTime(new Date());
        env.context.getBean(MongoPolicyDaoImpl.class).saveOrUpdatePolicy(policy);
    }

    void role(String name) {
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName(name);
        role.setDescription("role " + name);
        env.context.getBean(MongoRoleDaoImpl.class).saveOrUpdateRole(role);
    }

    void authenticationMethod(String name, long order) {
        MongoAuthenticationMethodImpl method = new MongoAuthenticationMethodImpl();
        method.setName(name);
        method.setOrder(order);
        method.setMethod("ldap");
        env.context.getBean(MongoAuthenticationMethodDaoImpl.class).saveOrUpdateAuthenticationMethod(method);
    }

    void user(String username) {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername(username);
        user.setPassword("password");
        user.setEmail(username + "@example.com");
        user.setFirstName("First");
        user.setSurname("Last");
        user.setDepartment("Department");
        user.setEnabled(true);
        env.bean("mongoUserDao", UserDao.class).save(user);
    }

    // ---- scheduler: contexts, views, profiles, instances and audits ----

    void scheduledContext(String contextName) {
        ContextTemplateImpl template = new ContextTemplateImpl();
        template.setName(contextName);
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        record.setContextName(contextName);
        record.setContext(template);
        record.setTimestamp(System.currentTimeMillis());
        env.bean("scheduledContextDao", ScheduledContextDao.class).save(record);
    }

    void scheduledContextView(String contextName) {
        MongoScheduledContextViewRecordImpl record = new MongoScheduledContextViewRecordImpl();
        record.setParentContextName("parent");
        record.setContextName(contextName);
        record.setContextView("{\"view\":\"" + contextName + "\"}");
        record.setTimestamp(System.currentTimeMillis());
        env.bean("scheduledContextViewDao", ScheduledContextViewDao.class).save(record);
    }

    void contextProfile(String profileName) {
        ContextProfileImpl profile = new ContextProfileImpl();
        profile.setDefaultContext("context");
        profile.setSubContexts(List.of("child"));
        ContextProfileRecordImpl record = new ContextProfileRecordImpl();
        record.setProfileName(profileName);
        record.setContextName("context");
        record.setOwner("admin");
        record.setContextProfile(profile);
        record.setAccessGroups(List.of("group"));
        record.setAccessUsers(List.of("admin"));
        record.setCreatedDateTime(System.currentTimeMillis());
        record.setModifiedDateTime(System.currentTimeMillis());
        env.bean("contextProfileDao", ContextProfileDao.class).save(record);
    }

    private static ContextInstanceImpl contextInstance(String contextInstanceId) {
        ContextInstanceImpl instance = new ContextInstanceImpl();
        instance.setId(contextInstanceId);
        instance.setName("context");
        instance.setStartTime(System.currentTimeMillis());
        return instance;
    }

    private static <R extends ScheduledContextInstanceRecordImpl> R contextInstanceRecord(R record,
                                                                                         String contextInstanceId) {
        record.setContextName("context");
        record.setContextInstanceId(contextInstanceId);
        record.setContextInstance(contextInstance(contextInstanceId));
        record.setStatus("RUNNING");
        record.setTimestamp(System.currentTimeMillis());
        return record;
    }

    void scheduledContextInstance(String contextInstanceId) {
        env.bean("scheduledContextInstanceDao", ScheduledContextInstanceDao.class)
            .save(contextInstanceRecord(new ScheduledContextInstanceRecordImpl(), contextInstanceId));
    }

    void scheduledContextInstanceAudit(String contextInstanceId) {
        env.bean("scheduledContextInstanceAuditDao", ScheduledContextInstanceAuditDao.class)
            .save(contextInstanceRecord(new ScheduledContextInstanceAuditRecordImpl(), contextInstanceId));
    }

    void scheduledContextInstanceAuditAggregate(String contextInstanceId) {
        ScheduledContextInstanceAuditAggregateRecordImpl record = new ScheduledContextInstanceAuditAggregateRecordImpl();
        record.setContextName("context");
        record.setContextInstanceId(contextInstanceId);
        record.setScheduledContextInstanceAuditAggregate(new ScheduledContextInstanceAuditAggregateImpl());
        record.setStatus("RUNNING");
        record.setJobType("quartzScheduleDrivenJob");
        env.bean("scheduledContextInstanceAuditAggregateDao", ScheduledContextInstanceAuditAggregateDao.class)
            .save(record);
    }

    void jobLockCacheAudit() {
        JobLockCacheAuditRecordImpl record = new JobLockCacheAuditRecordImpl();
        record.setJobLockCache(new JobLockCacheDataImpl());
        env.bean("jobLockCacheAuditDao", JobLockCacheAuditDao.class).save(record);
    }

    // ---- notifications ----

    void emailNotificationContext(String contextName) {
        EmailNotificationContextImpl context = new EmailNotificationContextImpl();
        context.setContextName(contextName);
        context.setMonitorTypes(List.of("ERROR"));
        context.setEmailSendTo(List.of("ops@example.com"));
        EmailNotificationContextRecordImpl record = new EmailNotificationContextRecordImpl();
        record.setContextName(contextName);
        record.setEmailNotificationContext(context);
        env.bean("emailNotificationContextDao", EmailNotificationContextDao.class).save(record);
    }

    void emailNotificationDetails(String jobName) {
        EmailNotificationDetailsImpl details = new EmailNotificationDetailsImpl();
        details.setJobName(jobName);
        details.setContextName("context");
        details.setChildContextName("child");
        details.setMonitorType("ERROR");
        details.setEmailSendTo(List.of("ops@example.com"));
        details.setEmailSubject("subject");
        EmailNotificationDetailsRecordImpl record = new EmailNotificationDetailsRecordImpl();
        record.setEmailNotificationDetails(details);
        env.bean("emailNotificationDetailsDao", EmailNotificationDetailsDao.class).save(record);
    }

    void notificationSendAudit(String jobName) {
        NotificationSendAuditRecordImpl record = new NotificationSendAuditRecordImpl();
        record.setNotificationSendAudit(new NotificationSendAuditImpl(jobName, "context-instance", "context",
            "ERROR", "email", true));
        env.bean("notificationSendAuditDao", NotificationSendAuditDao.class).save(record);
    }

    // ---- job definitions ----

    private static <J extends SchedulerJobImpl> J job(J job, String jobName) {
        job.setAgentName("agent");
        job.setJobName(jobName);
        job.setContextName("context");
        job.setDisplayName("Display " + jobName);
        return job;
    }

    void fileEventDrivenJob(String jobName) {
        FileEventDrivenJobRecordImpl record = new FileEventDrivenJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName(jobName);
        record.setContextName("context");
        record.setDisplayName("Display " + jobName);
        record.setFileEventDrivenJob(job(new FileEventDrivenJobImpl(), jobName));
        env.bean("fileEventDrivenJobRecordDao", FileEventDrivenJobDao.class).save(record);
    }

    void quartzScheduleDrivenJob(String jobName) {
        QuartzScheduleDrivenJobRecordImpl record = new QuartzScheduleDrivenJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName(jobName);
        record.setContextName("context");
        record.setDisplayName("Display " + jobName);
        record.setQuartzScheduleDrivenJob(job(new QuartzScheduleDrivenJobImpl(), jobName));
        env.bean("quartzScheduleDrivenJobRecordDao", QuartzScheduleDrivenJobDao.class).save(record);
    }

    void internalEventDrivenJobTemplate(String jobName) {
        InternalEventDrivenJobImpl job = new InternalEventDrivenJobImpl();
        job.setAgentName("agent");
        job.setJobName(jobName);
        job.setContextName("context");
        job.setDisplayName("Display " + jobName);
        InternalEventDrivenJobRecordImpl record = new InternalEventDrivenJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName(jobName);
        record.setContextName("context");
        record.setInternalEventDrivenJob(job);
        env.bean("internalEventDrivenJobTemplateDao", InternalEventDrivenJobDao.class).save(record);
    }

    void globalEventJob(String jobName) {
        GlobalEventJobRecordImpl record = new GlobalEventJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName(jobName);
        record.setContextName("context");
        record.setGlobalEventJob(job(new GlobalEventJobImpl(), jobName));
        env.bean("globalEventJobRecordDao", GlobalEventJobDao.class).save(record);
    }

    void contextStartJob(String jobName) {
        ContextStartJobRecordImpl record = new ContextStartJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName(jobName);
        record.setContextName("context");
        record.setContextStartJob(job(new ContextStartJobImpl(), jobName));
        env.bean("contextStartJobDao", ContextStartJobDao.class).save(record);
    }

    void contextTerminalJob(String jobName) {
        ContextTerminalJobRecordImpl record = new ContextTerminalJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName(jobName);
        record.setContextName("context");
        record.setContextTerminalJob(job(new ContextTerminalJobImpl(), jobName));
        env.bean("contextTerminalJobDao", ContextTerminalJobDao.class).save(record);
    }

    void bridgingJob(String jobName) {
        BridgingJobRecordImpl record = new BridgingJobRecordImpl();
        record.setAgentName("agent");
        record.setJobName(jobName);
        record.setContextName("context");
        record.setBridgingJob(job(new BridgingJobImpl(), jobName));
        env.bean("bridgingJobRecordDao", BridgingJobDao.class).save(record);
    }

    // ---- job instances, one per instance type ----

    private void jobInstance(SchedulerJobInstanceImpl instance, String jobName) {
        instance.setJobName(jobName);
        instance.setAgentName("agent");
        instance.setContextName("context");
        instance.setContextInstanceId("context-instance");
        instance.setChildContextName("child");
        instance.setStatus(InstanceStatus.WAITING);
        SchedulerJobInstanceRecordImpl record = new SchedulerJobInstanceRecordImpl();
        record.setJobName(jobName);
        record.setContextName("context");
        record.setContextInstanceId("context-instance");
        record.setSchedulerJobInstance(instance);
        env.bean("schedulerJobInstanceDao", SchedulerJobInstanceDao.class).save(record);
    }

    /** One job instance of every instance type except the quartz one {@link #jobInstance(String)} writes. */
    void otherJobInstances() {
        List<Supplier<SchedulerJobInstanceImpl>> instances = List.of(FileEventDrivenJobInstanceImpl::new,
            InternalEventDrivenJobInstanceImpl::new, GlobalEventJobInstanceImpl::new,
            ContextStartJobInstanceImpl::new, ContextTerminalJobInstanceImpl::new, LocalEventJobInstanceImpl::new,
            BridgingJobInstanceImpl::new);
        for (Supplier<SchedulerJobInstanceImpl> instance : instances) {
            SchedulerJobInstanceImpl created = instance.get();
            jobInstance(created, created.getClass().getSimpleName());
        }
    }

    // ---- metadata, setup and principals ----

    void moduleMetadata(String moduleName) {
        MongoModuleMetaDataImpl module = new MongoModuleMetaDataImpl();
        module.setName(moduleName);
        module.setType(ModuleType.INTEGRATION_MODULE);
        module.setDescription("module " + moduleName);
        module.setUrl("http://localhost:8080");
        module.setFlows(new ArrayList<>());
        env.bean("moduleMetadataDao", ModuleMetadataDao.class).save(List.of(module));
    }

    void businessStreamMetadata(String id) {
        BusinessStreamMetaDataImpl metaData = new BusinessStreamMetaDataImpl();
        metaData.setId(id);
        metaData.setName("stream " + id);
        metaData.setDescription("description");
        metaData.setJson("{\"name\":\"stream " + id + "\"}");
        env.bean("businessStreamMetadataDao", BusinessStreamMetadataDao.class).save(metaData);
    }

    void componentConfigurationMetadata(String configurationId) {
        MongoConfigurationMetaData metaData = new MongoConfigurationMetaData(configurationId,
            List.of(new MongoConfigurationParameterMetaData(1L, "name", "value", "description", "java.lang.String")),
            "description", "implementingClass");
        env.bean("componentConfigurationMetadataDao", ComponentConfigurationMetadataDao.class)
            .save(List.of(metaData));
    }

    void dashboardPlatformSetup() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setPlatformSetupItems(new ArrayList<>());
        env.bean("mongoSetupDao", SetupDao.class).save(setup);
    }

    void principal(String name) {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
        principal.setName(name);
        principal.setType("user");
        principal.setDescription("principal " + name);
        principal.setRoles(new HashSet<>());
        principal.setCreatedDateTime(new Date());
        principal.setUpdatedDateTime(new Date());
        env.context.getBean(MongoIkasanPrincipalDaoImpl.class).saveOrUpdatePrincipal(principal);
    }

    /** One of everything above. */
    void oneOfEach() {
        wiretap("1");
        error("error-1");
        exclusion("exclusion-1");
        exclusionEventAction("action-1");
        replay("replay-1");
        replayAudit("audit-1");
        systemEvent(1L);
        metric("flow-1");
        jobLockCache("env-1");
        internalEventDrivenJob("job-1");
        jobInstance("job-1");
        scheduledProcessEvent("job-1");
        policy("policy-1");
        role("role-1");
        authenticationMethod("ldap-1", 1L);
        user("user-1");

        scheduledContext("context-1");
        scheduledContextView("context-1");
        contextProfile("profile-1");
        scheduledContextInstance("context-instance-1");
        scheduledContextInstanceAudit("context-instance-1");
        scheduledContextInstanceAuditAggregate("context-instance-1");
        jobLockCacheAudit();
        emailNotificationContext("context-1");
        emailNotificationDetails("job-1");
        notificationSendAudit("job-1");
        // job names differ per type where the Mongo ids would otherwise collide
        fileEventDrivenJob("file-job-1");
        quartzScheduleDrivenJob("quartz-job-1");
        internalEventDrivenJobTemplate("template-job-1");
        globalEventJob("global-job-1");
        contextStartJob("start-job-1");
        contextTerminalJob("terminal-job-1");
        bridgingJob("bridging-job-1");
        otherJobInstances();
        moduleMetadata("module-1");
        businessStreamMetadata("stream-1");
        componentConfigurationMetadata("configuration-1");
        dashboardPlatformSetup();
        principal("principal-1");
    }

    /** The types {@link #oneOfEach()} writes to the ikasan collection. */
    static final List<String> TYPES = List.of(EntityDao.WIRETAP_TYPE, EntityDao.ERROR, EntityDao.EXCLUSION,
        EntityDao.EXCLUSION_EVENT_ACTION, EntityDao.REPLAY, EntityDao.REPLAY_AUDIT, EntityDao.SYSTEM_EVENT_TYPE,
        MetricsDao.METRIC_ENTITY_TYPE, JobLockCacheDao.JOB_LOCK_CACHE_TYPE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB,
        JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE, "securityPolicy", "securityRole",
        "securityAuthenticationMethod", "securityUser",
        ScheduledContextDao.SCHEDULED_CONTEXT_TYPE, ScheduledContextViewDao.SCHEDULED_CONTEXT_VIEW_TYPE,
        ContextProfileDao.CONTEXT_PROFILE_TYPE, ScheduledContextInstanceDao.SCHEDULED_CONTEXT_INSTANCE_TYPE,
        ScheduledContextInstanceAuditDao.SCHEDULED_CONTEXT_INSTANCE_AUDIT_TYPE,
        ScheduledContextInstanceAuditAggregateDao.SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE,
        JobLockCacheAuditDao.JOB_LOCK_AUDIT_CACHE_TYPE, EmailNotificationContextDao.EMAIL_NOTIFICATION_CONTEXT,
        EmailNotificationDetailsDao.EMAIL_NOTIFICATION_DETAILS, NotificationSendAuditDao.NOTIFICATION_SEND_AUDIT,
        JobConstants.FILE_EVENT_DRIVEN_JOB, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB,
        JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE, JobConstants.GLOBAL_EVENT_JOB,
        JobConstants.CONTEXT_START_JOB, JobConstants.CONTEXT_TERMINAL_JOB, JobConstants.BRIDGING_JOB,
        JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE,
        JobConstants.GLOBAL_EVENT_JOB_INSTANCE, JobConstants.CONTEXT_START_JOB_INSTANCE,
        JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE, JobConstants.LOCAL_EVENT_JOB_INSTANCE,
        JobConstants.BRIDGING_JOB_INSTANCE, ModuleMetadataDao.MODULE_METADATA,
        BusinessStreamMetadataDao.BUSINESS_STREAM_METADATA, ComponentConfigurationMetadataDao.COMPONENT_CONFIGURATION,
        "dashboardPlatformSetup", IkasanPrincipalDao.PRINCIPAL_TYPE);

    static List<ReplayAuditEvent> none() {
        return List.of();
    }
}
