package org.ikasan.scheduled.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.ikasan.scheduled.context.model.*;
import org.ikasan.scheduled.event.model.SolrContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.event.model.SolrContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.event.model.SolrSchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.instance.model.*;
import org.ikasan.scheduled.job.model.*;
import org.ikasan.scheduled.profile.model.SolrContextProfileImpl;
import org.ikasan.scheduled.profile.model.SolrContextProfileRecordImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.ReplacementPair;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public class ScheduledConcurrentObjectMapperFactory {

    /**
     * Create an JsonMapper instance that can be used in the
     * job orchestration module with all relevant concrete type
     * mappings.
     *
     * @return
     */
    public static JsonMapper newInstance() {
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(And.class, SolrAndImpl.class)
            .addAbstractTypeMapping(Or.class, SolrOrImpl.class)
            .addAbstractTypeMapping(Not.class, SolrNotImpl.class)
            .addAbstractTypeMapping(ContextTemplate.class, SolrContextTemplateImpl.class)
            .addAbstractTypeMapping(Context.class, SolrContextImpl.class)
            .addAbstractTypeMapping(ContextParameter.class, SolrContextParameterImpl.class)
            .addAbstractTypeMapping(SchedulerJob.class, SolrSchedulerJobImpl.class)
            .addAbstractTypeMapping(SchedulerJobLockParticipant.class, SolrSchedulerJobLockParticipantImpl.class)
            .addAbstractTypeMapping(JobDependency.class, SolrJobDependencyImpl.class)
            .addAbstractTypeMapping(ContextDependency.class, SolrContextDependencyImpl.class)
            .addAbstractTypeMapping(LogicalGrouping.class, SolrLogicalGroupingImpl.class)
            .addAbstractTypeMapping(LogicalOperator.class, SolrLogicalOperatorImpl.class)
            .addAbstractTypeMapping(ContextInstance.class, SolrContextInstanceImpl.class)
            .addAbstractTypeMapping(SchedulerJobInstance.class, SolrSchedulerJobInstanceImpl.class)
            .addAbstractTypeMapping(ContextParameterInstance.class, SolrContextParameterInstanceImpl.class)
            .addAbstractTypeMapping(JobLock.class, SolrJobLockImpl.class)
            .addAbstractTypeMapping(JobLockInstance.class, SolrJobLockInstanceImpl.class)
            .addAbstractTypeMapping(ScheduledProcessEvent.class, SolrContextualisedScheduledProcessEventImpl.class)
            .addAbstractTypeMapping(ContextualisedScheduledProcessEvent.class, SolrContextualisedScheduledProcessEventImpl.class)
            .addAbstractTypeMapping(ContextualisedSchedulerJobInitiationEvent.class, SolrContextualisedSchedulerJobInitiationEventImpl.class)
            .addAbstractTypeMapping(SchedulerJobInitiationEvent.class, SolrSchedulerJobInitiationEventImpl.class)
            .addAbstractTypeMapping(InternalEventDrivenJob.class, SolrInternalEventDrivenJobImpl.class)
            .addAbstractTypeMapping(InternalEventDrivenJobInstance.class, SolrInternalEventDrivenJobInstanceImpl.class)
            .addAbstractTypeMapping(FileEventDrivenJobInstance.class, SolrFileEventDrivenJobInstanceImpl.class)
            .addAbstractTypeMapping(QuartzScheduleDrivenJobInstance.class, SolrQuartzScheduleDrivenJobInstanceImpl.class)
            .addAbstractTypeMapping(JobLockHolder.class, SolrJobLockHolderImpl.class)
            .addAbstractTypeMapping(ContextProfile.class, SolrContextProfileImpl.class)
            .addAbstractTypeMapping(ContextProfileRecord.class, SolrContextProfileRecordImpl.class)
            .addAbstractTypeMapping(ReplacementPair.class, SolrReplacementPairImpl.class)
            .addAbstractTypeMapping(List.class, CopyOnWriteArrayList.class)
            .addAbstractTypeMapping(Map.class, ConcurrentHashMap.class)
            .addAbstractTypeMapping(Set.class, CopyOnWriteArraySet.class);


        return JsonMapper.builder().addModule(simpleModule)
            .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL)
                .withValueInclusion(JsonInclude.Include.NON_NULL))
            .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_EMPTY)
                .withValueInclusion(JsonInclude.Include.NON_EMPTY))
            .configure(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();
    }
}
