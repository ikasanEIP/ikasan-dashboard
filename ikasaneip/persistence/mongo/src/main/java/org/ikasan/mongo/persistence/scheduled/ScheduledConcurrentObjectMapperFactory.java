package org.ikasan.mongo.persistence.scheduled;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.context.*;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.QuartzScheduleDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.QuartzScheduleDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class ScheduledConcurrentObjectMapperFactory {

    /**
     * Create an ObjectMapper instance that can be used in the
     * job orchestration module with all relevant concrete type
     * mappings.
     *
     * @return
     */
    public static ObjectMapper newInstance() {
        ObjectMapper objectMapper = new ObjectMapper();
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(And.class, AndImpl.class)
            .addAbstractTypeMapping(Or.class, OrImpl.class)
            .addAbstractTypeMapping(Not.class, NotImpl.class)
            .addAbstractTypeMapping(ContextTemplate.class, ContextTemplateImpl.class)
//            .addAbstractTypeMapping(Context.class, ContextImpl.class)
            .addAbstractTypeMapping(ContextParameter.class, ContextParameterImpl.class)
//            .addAbstractTypeMapping(SchedulerJob.class, HibernateSchedulerJobImpl.class)
//            .addAbstractTypeMapping(SchedulerJobLockParticipant.class, HibernateSchedulerJobLockParticipantImpl.class)
            .addAbstractTypeMapping(JobDependency.class, JobDependencyImpl.class)
            .addAbstractTypeMapping(ContextDependency.class, ContextDependencyImpl.class)
            .addAbstractTypeMapping(LogicalGrouping.class, LogicalGroupingImpl.class)
            .addAbstractTypeMapping(LogicalOperator.class, LogicalOperatorImpl.class)
            .addAbstractTypeMapping(ContextInstance.class, ContextInstanceImpl.class)
            .addAbstractTypeMapping(SchedulerJobInstance.class, QuartzScheduleDrivenJobInstanceImpl.class)
            .addAbstractTypeMapping(QuartzScheduleDrivenJobInstance.class, QuartzScheduleDrivenJobInstanceImpl.class)
//            .addAbstractTypeMapping(ContextParameterInstance.class, HibernateContextParameterInstanceImpl.class)
            .addAbstractTypeMapping(JobLock.class, JobLockImpl.class)
            .addAbstractTypeMapping(BridgingJob.class, BridgingJobImpl.class)
            .addAbstractTypeMapping(ContextStartJob.class, ContextStartJobImpl.class)
            .addAbstractTypeMapping(ContextTerminalJob.class, ContextTerminalJobImpl.class)
            .addAbstractTypeMapping(FileEventDrivenJob.class, FileEventDrivenJobImpl.class)
            .addAbstractTypeMapping(QuartzScheduleDrivenJob.class, QuartzScheduleDrivenJobImpl.class)
            .addAbstractTypeMapping(GlobalEventJob.class, GlobalEventJobImpl.class)
            .addAbstractTypeMapping(InternalEventDrivenJob.class, InternalEventDrivenJobImpl.class)
//            .addAbstractTypeMapping(JobLockInstance.class, HibernateJobLockInstanceImpl.class)
//            .addAbstractTypeMapping(ScheduledProcessEvent.class, HibernateContextualisedScheduledProcessEventImpl.class)
//            .addAbstractTypeMapping(ContextualisedScheduledProcessEvent.class, HibernateContextualisedScheduledProcessEventImpl.class)
//            .addAbstractTypeMapping(ContextualisedSchedulerJobInitiationEvent.class, HibernateContextualisedSchedulerJobInitiationEventImpl.class)
//            .addAbstractTypeMapping(SchedulerJobInitiationEvent.class, HibernateSchedulerJobInitiationEventImpl.class)
//            .addAbstractTypeMapping(InternalEventDrivenJob.class, HibernateInternalEventDrivenJobImpl.class)
//            .addAbstractTypeMapping(InternalEventDrivenJobInstance.class, HibernateInternalEventDrivenJobInstanceImpl.class)
//            .addAbstractTypeMapping(FileEventDrivenJobInstance.class, HibernateFileEventDrivenJobInstanceImpl.class)
//            .addAbstractTypeMapping(QuartzScheduleDrivenJobInstance.class, HibernateQuartzScheduleDrivenJobInstanceImpl.class)
            .addAbstractTypeMapping(JobLockHolder.class, JobLockHolderImpl.class)
            .addAbstractTypeMapping(JobLockCacheData.class, JobLockCacheDataImpl.class)
            .addAbstractTypeMapping(SchedulerJobLockParticipant.class, SchedulerJobLockParticipantImpl.class)
            .addAbstractTypeMapping(ContextualisedSchedulerJobInitiationEvent.class, ContextualisedSchedulerJobInitiationEventImpl.class)
            .addAbstractTypeMapping(SchedulerJobInitiationEvent.class, SchedulerJobInitiationEventImpl.class)
            .addAbstractTypeMapping(EmailNotificationContext.class, EmailNotificationContextImpl.class)
            .addAbstractTypeMapping(ContextProfile.class, ContextProfileImpl.class)
//            .addAbstractTypeMapping(ContextProfileRecord.class, HibernateContextProfileRecordImpl.class)
//            .addAbstractTypeMapping(ReplacementPair.class, HibernateReplacementPairImpl.class)
//            .addAbstractTypeMapping(ModuleMetaData.class, HibernateModuleMetaDataImpl.class)
//            .addAbstractTypeMapping(FlowMetaData.class, HibernateFlowMetaDataImpl.class)
//            .addAbstractTypeMapping(FlowElementMetaData.class, HibernateFlowElementMetaDataImpl.class)
//            .addAbstractTypeMapping(Transition.class, HibernateTransitionImpl.class)
//            .addAbstractTypeMapping(DecoratorMetaData.class, HibernateDecoratorMetaDataImpl.class)
            .addAbstractTypeMapping(List.class, CopyOnWriteArrayList.class)
            .addAbstractTypeMapping(Map.class, ConcurrentHashMap.class)
            .addAbstractTypeMapping(Set.class, CopyOnWriteArraySet.class);


        objectMapper.registerModule(simpleModule);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }
}
