package org.ikasan.scheduled;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.job.orchestration.model.context.*;
import org.ikasan.spec.scheduled.context.model.*;

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
//            .addAbstractTypeMapping(ContextInstance.class, HibernateContextInstanceImpl.class)
//            .addAbstractTypeMapping(SchedulerJobInstance.class, HibernateSchedulerJobInstanceImpl.class)
//            .addAbstractTypeMapping(ContextParameterInstance.class, HibernateContextParameterInstanceImpl.class)
            .addAbstractTypeMapping(JobLock.class, JobLockImpl.class)
//            .addAbstractTypeMapping(JobLockInstance.class, HibernateJobLockInstanceImpl.class)
//            .addAbstractTypeMapping(ScheduledProcessEvent.class, HibernateContextualisedScheduledProcessEventImpl.class)
//            .addAbstractTypeMapping(ContextualisedScheduledProcessEvent.class, HibernateContextualisedScheduledProcessEventImpl.class)
//            .addAbstractTypeMapping(ContextualisedSchedulerJobInitiationEvent.class, HibernateContextualisedSchedulerJobInitiationEventImpl.class)
//            .addAbstractTypeMapping(SchedulerJobInitiationEvent.class, HibernateSchedulerJobInitiationEventImpl.class)
//            .addAbstractTypeMapping(InternalEventDrivenJob.class, HibernateInternalEventDrivenJobImpl.class)
//            .addAbstractTypeMapping(InternalEventDrivenJobInstance.class, HibernateInternalEventDrivenJobInstanceImpl.class)
//            .addAbstractTypeMapping(FileEventDrivenJobInstance.class, HibernateFileEventDrivenJobInstanceImpl.class)
//            .addAbstractTypeMapping(QuartzScheduleDrivenJobInstance.class, HibernateQuartzScheduleDrivenJobInstanceImpl.class)
//            .addAbstractTypeMapping(JobLockHolder.class, HibernateJobLockHolderImpl.class)
//            .addAbstractTypeMapping(ContextProfile.class, HibernateContextProfileImpl.class)
//            .addAbstractTypeMapping(ContextProfileRecord.class, HibernateContextProfileRecordImpl.class)
//            .addAbstractTypeMapping(ReplacementPair.class, HibernateReplacementPairImpl.class)
            .addAbstractTypeMapping(List.class, CopyOnWriteArrayList.class)
            .addAbstractTypeMapping(Map.class, ConcurrentHashMap.class)
            .addAbstractTypeMapping(Set.class, CopyOnWriteArraySet.class);


        objectMapper.registerModule(simpleModule);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }
}
