package org.ikasan.scheduler.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.ikasan.scheduler.core.model.context.*;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.scheduler.core.model.instance.ContextParameterInstanceImpl;
import org.ikasan.scheduler.core.model.instance.SchedulerJobInstanceImpl;
import org.ikasan.scheduler.core.model.job.SchedulerJobImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

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
            .addAbstractTypeMapping(JobDependency.class, JobDependencyImpl.class)
            .addAbstractTypeMapping(ContextDependency.class, ContextDependencyImpl.class)
            .addAbstractTypeMapping(LogicalGrouping.class, LogicalGroupingImpl.class)
            .addAbstractTypeMapping(LogicalOperator.class, LogicalOperatorImpl.class)
            .addAbstractTypeMapping(ContextInstance.class, ContextInstanceImpl.class)
            .addAbstractTypeMapping(SchedulerJobInstance.class, SchedulerJobInstanceImpl.class)
            .addAbstractTypeMapping(ContextParameterInstance.class, ContextParameterInstanceImpl.class);

        objectMapper.registerModule(simpleModule);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return objectMapper;
    }
}
