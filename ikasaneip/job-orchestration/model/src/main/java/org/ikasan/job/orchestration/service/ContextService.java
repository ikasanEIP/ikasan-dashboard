package org.ikasan.job.orchestration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;

import java.util.concurrent.atomic.AtomicReference;

public class ContextService {
    private ObjectMapper objectMapper;

    public ContextService() {
        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    public ContextTemplate getContextTemplate(String context) throws JsonProcessingException {
        return objectMapper.readValue(context, ContextTemplateImpl.class);
    }

    public String getContextTemplateString(ContextTemplate context) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }

    public ContextInstance getContextInstance(String context) throws JsonProcessingException {
        return objectMapper.readValue(context, ContextInstanceImpl.class);
    }

    public String getContextInstanceString(ContextInstance context) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }

    public SchedulerJob getSchedulerJob(String schedulerJob) throws JsonProcessingException {
        return objectMapper.readValue(schedulerJob, SchedulerJobImpl.class);
    }

    public JobLock getJobLock(String jobLock) throws JsonProcessingException {
        return objectMapper.readValue(jobLock, JobLockImpl.class);
    }

    public String getSchedulerJobString(SchedulerJob schedulerJob) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public QuartzScheduleDrivenJob getQuartzScheduleDrivenJob(String schedulerJob) throws JsonProcessingException {
        return objectMapper.readValue(schedulerJob, QuartzScheduleDrivenJobImpl.class);
    }

    public String getQuartzScheduleDrivenJobString(QuartzScheduleDrivenJob schedulerJob) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public FileEventDrivenJob getFileEventDrivenJob(String schedulerJob) throws JsonProcessingException {
        return objectMapper.readValue(schedulerJob, FileEventDrivenJobImpl.class);
    }

    public String getFileEventDrivenJobString(FileEventDrivenJob schedulerJob) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public InternalEventDrivenJob getInternalEventDrivenJob(String schedulerJob) throws JsonProcessingException {
        return objectMapper.readValue(schedulerJob, InternalEventDrivenJobImpl.class);
    }

    public String getInternalEventDrivenJobString(InternalEventDrivenJob schedulerJob) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public ContextProfileRecord getContextProfileRecord(String contextProfileRecord) throws JsonProcessingException {
        return objectMapper.readValue(contextProfileRecord, ContextProfileRecordImpl.class);
    }

    public ContextTemplate getParent(ContextTemplate context, ContextTemplate currentContext) {
        AtomicReference<ContextTemplate> parent = new AtomicReference<>();

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                if (c.getName().equals(currentContext.getName())) {
                    parent.set(context);
                }
            });

            if(parent.get() != null) {
                return parent.get();
            }
            else {
                context.getContexts().forEach(c ->
                {
                    ContextTemplate parentContext = getParent(c, currentContext);

                    if(parentContext != null) {
                        parent.set(parentContext);
                    }
                });
            }
        }

        return parent.get();
    }

    public ContextInstance getParent(ContextInstance context, ContextInstance currentContext) {
        AtomicReference<ContextInstance> parent = new AtomicReference<>();

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                if (c.getName().equals(currentContext.getName())) {
                    parent.set(context);
                }
            });

            if(parent.get() != null) {
                return parent.get();
            }
            else {
                context.getContexts().forEach(c ->
                {
                    ContextInstance parentContext = getParent(c, currentContext);

                    if(parentContext != null) {
                        parent.set(parentContext);
                    }
                });
            }
        }

        return parent.get();
    }
}
