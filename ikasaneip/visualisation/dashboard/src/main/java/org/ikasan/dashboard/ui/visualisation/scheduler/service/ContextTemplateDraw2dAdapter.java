package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.designer.builder.ImageBuilder;
import org.ikasan.designer.builder.UserDataBuilder;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ContextTemplateDraw2dAdapter extends Draw2dAdapterBase {

    Logger logger = LoggerFactory.getLogger(ContextTemplateDraw2dAdapter.class);

    public String adaptJobs(Context context, Map<String, SchedulerJob> schedulerJobs) {
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(super._adaptJobs(context, schedulerJobs));
            }
            catch (JsonProcessingException e) {
                throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate jobs for" +
                    " context[%s] to the draw 2d data format", context.getName()), e);
            }
    }

    public String adaptContext(Context context) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(super._adaptContext(context));
        }
        catch (JsonProcessingException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate " +
                "context[%s] to the draw 2d data format", context.getName()), e);
        }
    }

    public String adaptJob(SchedulerJob schedulerJob) {
        String image = "frontend/images/and.png";

        String result = null;
        // todo clean this up. still a hack.
        try {
            ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                .withId((schedulerJob).getIdentifier())
                .withHeight(100)
                .withWidth(100)
                .withPath(image)
                .withUserData(new UserDataBuilder()
                    .withAgentName(schedulerJob.getAgentName())
                    .withJobName(schedulerJob.getJobName())
                    .withIdentifier(schedulerJob.getIdentifier())
                    .build()
                );

            if(schedulerJob instanceof InternalEventDrivenJob) {
                jobBuilder.withLeftPort()
                    .withRightPort();
            }
            else if(schedulerJob instanceof QuartzScheduleDrivenJob) {
                jobBuilder.withRightPort();
            }

            result = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(jobBuilder.build());
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return result;
    }
}
