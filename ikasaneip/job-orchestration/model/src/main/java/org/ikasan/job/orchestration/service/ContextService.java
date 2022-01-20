package org.ikasan.job.orchestration.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

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
}
