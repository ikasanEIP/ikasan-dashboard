package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.builder.ImageBuilder;
import org.ikasan.designer.builder.RectangleBuilder;
import org.ikasan.designer.builder.UserDataBuilder;
import org.ikasan.designer.model.Image;
import org.ikasan.designer.model.PositionedItem;
import org.ikasan.designer.model.UserData;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextTemplateDraw2dAdapter extends Draw2dAdapterBase {

    Logger logger = LoggerFactory.getLogger(ContextTemplateDraw2dAdapter.class);

    public String adaptJobs(Context context, Map<String, SchedulerJob> schedulerJobs) {
            try {
                ArrayList<Object> items = super._adaptJobs(context, schedulerJobs);
                this.addStatusRectangles(items, context);

                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
            }
            catch (JsonProcessingException e) {
                throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate jobs for" +
                    " context[%s] to the draw 2d data format", context.getName()), e);
            }
    }

    private void addStatusRectangles(List<Object> items, Context context) {
        ArrayList<Object> statusRectangles = new ArrayList<>();

        items.forEach(item -> {
            if (item instanceof Image) {
                RectangleBuilder rb = diagramBuilder.getRectangleBuilder()
                    .withId(((PositionedItem) item).getId() + "_status")
                    .withWidth(100)
                    .withHeight(100)
                    .withStroke(0)
                    .withRadius(20)
                    .withX(((PositionedItem) item).getX())
                    .withY(((PositionedItem) item).getY());

                statusRectangles.add(rb.build());
            }
        });

        items.addAll(statusRectangles);
    }

    public String adaptContext(Context context) {
        try {
            ArrayList<Object> items = super._adaptContext(context);
            this.addStatusRectangles(items, context);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
        }
        catch (JsonProcessingException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate " +
                "context[%s] to the draw 2d data format", context.getName()), e);
        }
    }

    public String adaptChildContext(Context context) {
        try {
            Image childContext = diagramBuilder.getImageBuilder()
                .withId(context.getName())
                .withHeight(100)
                .withWidth(100)
                .withPath("frontend/images/square_void.png")
                .withTopPort()
                .withBottomPort()
                .withUserData(new UserDataBuilder()
                    .withContextName(context.getName())
                    .withIdentifier(context.getName())
                    .withItemType(UserData.CONTEXT)
                    .build())
                .build();

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(childContext);
        }
        catch (JsonProcessingException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate " +
                "context[%s] to the draw 2d data format", context.getName()), e);
        }
    }

    public String adaptJob(SchedulerJob schedulerJob) {
        String image = getJobImage(schedulerJob);

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

            return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(jobBuilder.build());
        }
        catch (JsonProcessingException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate " +
                "scheduler jon[%s] to the draw 2d data format", schedulerJob.getJobName()), e);
        }
    }
}
