package org.ikasan.job.orchestration.service;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.status.model.ContextJobInstanceStatusWrapper;
import org.ikasan.spec.scheduled.status.model.ContextMachineStatusWrapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ContextService {
    private JsonMapper objectMapper;

    public ContextService() {
        this.objectMapper = ConcurrentObjectMapperFactory.newInstance();
    }

    public boolean isValidJSON(final String json) {
        try {
            this.objectMapper.readValue(json, Object.class);
        }
        catch (JacksonException e) {
            return false;
        }
        return true;
    }

    public ContextTemplate getContextTemplate(String context) throws JacksonException {
        return objectMapper.readValue(context, ContextTemplateImpl.class);
    }

    public String getContextTemplateString(ContextTemplate context) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }

    public ContextInstance getContextInstance(String context) throws JacksonException {
        return objectMapper.readValue(context, ContextInstanceImpl.class);
    }

    public String getContextInstanceString(ContextInstance context) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }

    public String getContextInstanceString(Map<String, ContextInstance> context) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
    }

    public String getSchedulerJobInstance(SchedulerJobInstance schedulerJobInstance) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJobInstance);
    }

    public String getSchedulerJobInstance(Map<String, SchedulerJobInstance> schedulerJobInstance) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJobInstance);
    }

    public SchedulerJob getSchedulerJob(String schedulerJob) throws JacksonException {
        return objectMapper.readValue(schedulerJob, SchedulerJobImpl.class);
    }

    public JobLock getJobLock(String jobLock) throws JacksonException {
        return objectMapper.readValue(jobLock, JobLockImpl.class);
    }

    public String getSchedulerJobString(SchedulerJob schedulerJob) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public QuartzScheduleDrivenJob getQuartzScheduleDrivenJob(String schedulerJob) throws JacksonException {
        return objectMapper.readValue(schedulerJob, QuartzScheduleDrivenJobImpl.class);
    }

    public String getQuartzScheduleDrivenJobString(QuartzScheduleDrivenJob schedulerJob) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public FileEventDrivenJob getFileEventDrivenJob(String schedulerJob) throws JacksonException {
        return objectMapper.readValue(schedulerJob, FileEventDrivenJobImpl.class);
    }

    public String getFileEventDrivenJobString(FileEventDrivenJob schedulerJob) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public InternalEventDrivenJob getInternalEventDrivenJob(String schedulerJob) throws JacksonException {
        return objectMapper.readValue(schedulerJob, InternalEventDrivenJobImpl.class);
    }

    public String getInternalEventDrivenJobString(InternalEventDrivenJob schedulerJob) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public GlobalEventJob getGlobalEventJob(String schedulerJob) throws JacksonException {
        return objectMapper.readValue(schedulerJob, GlobalEventJobImpl.class);
    }

    public String getGlobalEventJobString(GlobalEventJob schedulerJob) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schedulerJob);
    }

    public ContextProfileRecord getContextProfileRecord(String contextProfileRecord) throws JacksonException {
        return objectMapper.readValue(contextProfileRecord, ContextProfileRecordImpl.class);
    }

    public EmailNotificationDetails getEmailNotificationDetails(String emailNotificationDetails) throws JacksonException {
        return objectMapper.readValue(emailNotificationDetails, EmailNotificationDetailsImpl.class);
    }

    public EmailNotificationContext getEmailNotificationContext(String emailNotificationContext) throws JacksonException {
        return objectMapper.readValue(emailNotificationContext, EmailNotificationContextImpl.class);
    }

    public String getContextJobInstanceStatus(ContextJobInstanceStatusWrapper contextJobInstanceStatusWrapper) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextJobInstanceStatusWrapper);
    }

    public String getContextMachineStatus(ContextMachineStatusWrapper contextMachineStatusWrapper) throws JacksonException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextMachineStatusWrapper);
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
