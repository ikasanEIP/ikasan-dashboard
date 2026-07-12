package org.ikasan.job.orchestration.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.ikasan.job.orchestration.model.context.*;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.*;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsRecordImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class ConcurrentObjectMapperFactory {

    @JsonIgnoreProperties({"mockitoInterceptor", "$$sinon"})
    private interface IgnoreMockitoMixin {}

    /**
     * Create an ObjectMapper instance that can be used in the
     * job orchestration module with all relevant concrete type
     * mappings.
     *
     * @return
     */
    public static JsonMapper newInstance() {
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
            .addAbstractTypeMapping(List.class, CopyOnWriteArrayList.class)
            .addAbstractTypeMapping(JobLockHolder.class, JobLockHolderImpl.class)
            .addAbstractTypeMapping(EmailNotificationDetailsRecord.class, EmailNotificationDetailsRecordImpl.class)
            .addAbstractTypeMapping(EmailNotificationDetails.class, EmailNotificationDetailsImpl.class)
            .addAbstractTypeMapping(ReplacementPair.class, ReplacementPairImpl.class)
            .addAbstractTypeMapping(Map.class, ConcurrentHashMap.class)
            .addAbstractTypeMapping(Set.class, CopyOnWriteArraySet.class);

        return JsonMapper.builder().addModule(simpleModule)
            .addMixIn(Object.class, IgnoreMockitoMixin.class)
            .changeDefaultPropertyInclusion(incl -> incl
                .withContentInclusion(JsonInclude.Include.NON_EMPTY)
                .withValueInclusion(JsonInclude.Include.NON_EMPTY)
            )
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .build();
    }
}
