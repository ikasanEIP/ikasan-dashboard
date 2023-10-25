package org.ikasan.job.orchestration.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.job.orchestration.model.context.*;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.*;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;

import java.util.*;

public class ObjectMapperFactory {

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
            .addAbstractTypeMapping(Context.class, ContextImpl.class)
            .addAbstractTypeMapping(ContextParameter.class, ContextParameterImpl.class)
            .addAbstractTypeMapping(SchedulerJob.class, SchedulerJobImpl.class)
            .addAbstractTypeMapping(SchedulerJobLockParticipant.class, SchedulerJobLockParticipantImpl.class)
            .addAbstractTypeMapping(JobDependency.class, JobDependencyImpl.class)
            .addAbstractTypeMapping(ContextDependency.class, ContextDependencyImpl.class)
            .addAbstractTypeMapping(LogicalGrouping.class, LogicalGroupingImpl.class)
            .addAbstractTypeMapping(LogicalOperator.class, LogicalOperatorImpl.class)
            .addAbstractTypeMapping(ContextInstance.class, ContextInstanceImpl.class)
            .addAbstractTypeMapping(SchedulerJobInstance.class, SchedulerJobInstanceImpl.class)
            .addAbstractTypeMapping(ContextParameterInstance.class, ContextParameterInstanceImpl.class)
            .addAbstractTypeMapping(ScheduledProcessEvent.class, ContextualisedScheduledProcessEventImpl.class)
            .addAbstractTypeMapping(ContextualisedScheduledProcessEvent.class, ContextualisedScheduledProcessEventImpl.class)
            .addAbstractTypeMapping(ContextualisedSchedulerJobInitiationEvent.class, ContextualisedSchedulerJobInitiationEventImpl.class)
            .addAbstractTypeMapping(SchedulerJobInitiationEvent.class, SchedulerJobInitiationEventImpl.class)
            .addAbstractTypeMapping(InternalEventDrivenJob.class, InternalEventDrivenJobImpl.class)
            .addAbstractTypeMapping(InternalEventDrivenJobInstance.class, InternalEventDrivenJobInstanceImpl.class)
            .addAbstractTypeMapping(QuartzScheduleDrivenJob.class, QuartzScheduleDrivenJobImpl.class)
            .addAbstractTypeMapping(FileEventDrivenJob.class, FileEventDrivenJobImpl.class)
            .addAbstractTypeMapping(GlobalEventJob.class, GlobalEventJobImpl.class)
            .addAbstractTypeMapping(ContextProfileRecord.class, ContextProfileRecordImpl.class)
            .addAbstractTypeMapping(ContextProfile.class, ContextProfileImpl.class)
            .addAbstractTypeMapping(ContextBundle.class, ContextBundleImpl.class)
            .addAbstractTypeMapping(JobLock.class, JobLockImpl.class)
            .addAbstractTypeMapping(JobLockInstance.class, JobLockInstanceImpl.class)
            .addAbstractTypeMapping(List.class, ArrayList.class)
            .addAbstractTypeMapping(JobLockHolder.class, JobLockHolderImpl.class)
            .addAbstractTypeMapping(Map.class, HashMap.class)
            .addAbstractTypeMapping(Set.class, HashSet.class);

        objectMapper.registerModule(simpleModule);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return objectMapper;
    }
}
