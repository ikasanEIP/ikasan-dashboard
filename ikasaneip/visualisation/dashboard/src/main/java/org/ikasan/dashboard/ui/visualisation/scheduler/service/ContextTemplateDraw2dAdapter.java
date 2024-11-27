package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.designer.builder.*;
import org.ikasan.designer.model.Image;
import org.ikasan.designer.model.PositionedItem;
import org.ikasan.designer.model.UserData;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ContextTemplateDraw2dAdapter extends Draw2dAdapterBase {

    Logger logger = LoggerFactory.getLogger(ContextTemplateDraw2dAdapter.class);

    /**
     * The ContextTemplateDraw2dAdapter class is used to adapt jobs and contexts to a specific visual representation.
     * It provides methods for adapting jobs, contexts, and child contexts. It extends the Draw2dAdapterBase class.
     *
     * This class does not have any instance variables or methods. Its purpose is to be instantiated and used as an adapter
     * for adapting jobs and contexts in the SchedulerVisualisation class.
     *
     * This class is part of the ikasan-scheduler-dashboard module.
     */
    public ContextTemplateDraw2dAdapter() {
    }

    /**
     * This method is the constructor for the ContextTemplateDraw2dAdapter class.
     *
     * @param jobVisualisationVerticalSpacing    the vertical spacing to be used for job visualisation
     * @param jobVisualisationHorizontalSpacing  the horizontal spacing to be used for job visualisation
     * @param contextVisualisationLevelDistance  the distance between levels in the context visualisation
     * @param contextVisualisationNodeDistance   the distance between nodes in the context visualisation
     */
    public ContextTemplateDraw2dAdapter(double jobVisualisationVerticalSpacing
        , double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        super(jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing
            , contextVisualisationLevelDistance, contextVisualisationNodeDistance);
    }

    public String adaptJobs(Context parentContext, Context context, Map<String, SchedulerJob> schedulerJobs
        , Map<String, SchedulerJob> schedulerJobsMapByIdentifier, Map<String, Image> schedulerJobsImageMap) {
            try {
                ArrayList<Object> items = super._adaptJobs(parentContext, context, schedulerJobs
                    , schedulerJobsMapByIdentifier, schedulerJobsImageMap);

                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
            }
            catch (JsonProcessingException e) {
                throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate jobs for" +
                    " context[%s] to the draw 2d data format", context.getName()), e);
            }
    }

    /**
     * Adapts a given context to a JSON representation in the draw2d format.
     *
     * @param context The context to be adapted.
     * @return The JSON representation of the context.
     * @throws Draw2dAdapterException If an exception occurs during the adaptation process.
     */
    public String adaptContext(Context context) {
        try {
            ArrayList<Object> items = super._adaptContext(context);
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
                .withPath("frontend/images/context-icon.png")
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

    public String adaptJobPlanContext(Context context) {
        try {
            Image childContext = diagramBuilder.getImageBuilder()
                .withId(context.getName())
                .withHeight(100)
                .withWidth(100)
                .withPath("frontend/images/context-icon.png")
                .withLeftPort()
                .withRightPort()
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
            UserDataBuilder userDataBuilder = new UserDataBuilder()
                .withAgentName(schedulerJob.getAgentName())
                .withJobName(schedulerJob.getJobName())
                .withIdentifier(schedulerJob.getIdentifier());

            if(schedulerJob instanceof InternalEventDrivenJob || schedulerJob instanceof InternalEventDrivenJobInstance) {
                userDataBuilder.withItemType(UserData.INTERNAL_EVENT_DRIVEN_JOB);
            }
            else if(schedulerJob instanceof FileEventDrivenJob || schedulerJob instanceof FileEventDrivenJobInstance) {
                userDataBuilder.withItemType(UserData.FILE_EVENT_DRIVEN_JOB);
            }
            else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
                userDataBuilder.withItemType(UserData.QUARTZ_EVENT_DRIVEN_JOB);
            }
            else if(schedulerJob instanceof GlobalEventJob || schedulerJob instanceof GlobalEventJobInstance) {
                userDataBuilder.withItemType(UserData.GLOBAL_EVENT_DRIVEN_JOB);
            }
            else if(schedulerJob instanceof ContextStartJob || schedulerJob instanceof ContextStartJobInstance
                || schedulerJob.getAgentName().equals(UserData.CONTEXT_START_JOB)) {
                userDataBuilder.withItemType(UserData.CONTEXT_START_JOB);
            }
            else if(schedulerJob instanceof ContextTerminalJob || schedulerJob instanceof ContextTerminalJobInstance
                || schedulerJob.getAgentName().equals(UserData.CONTEXT_TERMINAL_JOB)) {
                userDataBuilder.withItemType(UserData.CONTEXT_TERMINAL_JOB);
            }
            else if(schedulerJob instanceof LocalEventJob || schedulerJob instanceof LocalEventJobInstance
                || schedulerJob.getAgentName().equals(UserData.LOCAL_EVENT_JOB)) {
                userDataBuilder.withItemType(UserData.LOCAL_EVENT_JOB);
            }
            else if(schedulerJob instanceof BridgingJob || schedulerJob instanceof BridgingJobInstance
                || schedulerJob.getAgentName().equals(UserData.BRIDGING_JOB)) {
                userDataBuilder.withItemType(UserData.BRIDGING_JOB);
            }
            ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                .withId((schedulerJob).getIdentifier())
                .withPath(image)
                .withUserData(userDataBuilder.build());

            if(schedulerJob instanceof InternalEventDrivenJob || schedulerJob instanceof GlobalEventJob ||
                schedulerJob instanceof LocalEventJob || schedulerJob instanceof BridgingJob) {
                jobBuilder
                    .withHeight(100)
                    .withWidth(100)
                    .withLeftPort()
                    .withRightPort();
            }
            else if(schedulerJob instanceof QuartzScheduleDrivenJob) {
                jobBuilder
                    .withHeight(100)
                    .withWidth(100)
                    .withRightPort();
            }
            else if(schedulerJob instanceof ContextStartJob) {
                jobBuilder
                    .withHeight(50)
                    .withWidth(50)
                    .withRightPort();
            }
            else if(schedulerJob instanceof ContextTerminalJob) {
                jobBuilder
                    .withHeight(50)
                    .withWidth(50)
                    .withLeftPort();
            }

            return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(jobBuilder.build());
        }
        catch (JsonProcessingException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate " +
                "scheduler job[%s] to the draw 2d data format", schedulerJob.getJobName()), e);
        }
    }

    /**
     * Helper method to add a connection between 2 items.
     * @param sourceIdentifier
     * @param sourcePort
     * @param targetIdentifier
     * @param targetPort
     */
    public String adaptConnection(String sourceIdentifier, String sourcePort, String targetIdentifier, String targetPort) {
        try {
            ConnectionBuilder connectionBuilder = new DiagramBuilder().getConnectionBuilder();
            connectionBuilder.withSource(
                diagramBuilder.getConnectionDetailsBuilder()
                    .withNode(sourceIdentifier)
                    .withPort(sourcePort)
                    .build()
            );
            connectionBuilder.withTarget(
                diagramBuilder.getConnectionDetailsBuilder()
                    .withNode(targetIdentifier)
                    .withPort(targetPort)
                    .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                    .build()
            );

            return mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(connectionBuilder.build());
        }
        catch (JsonProcessingException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate " +
                "Connection to the draw 2d data format"), e);
        }
    }
}
