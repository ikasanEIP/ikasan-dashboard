package org.ikasan.scheduled.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.scheduled.context.model.*;
import org.ikasan.scheduled.event.model.SolrContextualisedScheduledProcessEventImpl;
import org.ikasan.scheduled.event.model.SolrSchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.instance.model.SolrContextInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrContextParameterInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.JobLockInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduledObjectMapperFactory {

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
            .addAbstractTypeMapping(And.class, SolrAndImpl.class)
            .addAbstractTypeMapping(Or.class, SolrOrImpl.class)
            .addAbstractTypeMapping(Not.class, SolrNotImpl.class)
            .addAbstractTypeMapping(ContextTemplate.class, SolrContextTemplateImpl.class)
            .addAbstractTypeMapping(Context.class, SolrContextImpl.class)
            .addAbstractTypeMapping(ContextParameter.class, SolrContextParameterImpl.class)
            .addAbstractTypeMapping(SchedulerJob.class, SolrSchedulerJobImpl.class)
            .addAbstractTypeMapping(JobDependency.class, SolrJobDependencyImpl.class)
            .addAbstractTypeMapping(ContextDependency.class, SolrContextDependencyImpl.class)
            .addAbstractTypeMapping(LogicalGrouping.class, SolrLogicalGroupingImpl.class)
            .addAbstractTypeMapping(LogicalOperator.class, SolrLogicalOperatorImpl.class)
            .addAbstractTypeMapping(ContextInstance.class, SolrContextInstanceImpl.class)
            .addAbstractTypeMapping(SchedulerJobInstance.class, SolrSchedulerJobInstanceImpl.class)
            .addAbstractTypeMapping(ContextParameterInstance.class, SolrContextParameterInstanceImpl.class)
            .addAbstractTypeMapping(JobLock.class, SolrJobLockImpl.class)
            .addAbstractTypeMapping(ContextualisedScheduledProcessEvent.class, SolrContextualisedScheduledProcessEventImpl.class)
            .addAbstractTypeMapping(SchedulerJobInitiationEvent.class, SolrSchedulerJobInitiationEventImpl.class)
            .addAbstractTypeMapping(InternalEventDrivenJob.class, SolrInternalEventDrivenJobImpl.class)
            .addAbstractTypeMapping(List.class, ArrayList.class)
            .addAbstractTypeMapping(Map.class, HashMap.class);

        objectMapper.registerModule(simpleModule);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return objectMapper;
    }
}
