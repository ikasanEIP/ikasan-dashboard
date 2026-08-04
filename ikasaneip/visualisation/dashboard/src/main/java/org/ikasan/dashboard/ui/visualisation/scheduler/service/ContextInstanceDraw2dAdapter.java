package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.builder.*;
import org.ikasan.designer.model.*;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ContextInstanceDraw2dAdapter extends Draw2dAdapterBase {

    Logger logger = LoggerFactory.getLogger(ContextInstanceDraw2dAdapter.class);

    /**
     * Constructs a new ContextInstanceDraw2dAdapter object.
     * This class is a draw2d adapter for context instance visualization and provides methods for adapting the context instance.
     */
    public ContextInstanceDraw2dAdapter() {
        super();
    }

    /**
     * Constructor for the ContextInstanceDraw2dAdapter class.
     *
     * @param jobVisualisationVerticalSpacing   the vertical spacing to be used for job visualisation
     * @param jobVisualisationHorizontalSpacing the horizontal spacing to be used for job visualisation
     * @param contextVisualisationLevelDistance the distance between levels in the context visualisation
     * @param contextVisualisationNodeDistance  the distance between nodes in the context visualisation
     */
    public ContextInstanceDraw2dAdapter(double jobVisualisationVerticalSpacing
        , double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        super(jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing
            , contextVisualisationLevelDistance, contextVisualisationNodeDistance);
    }

    /**
     * Adapts scheduler jobs to a draw2d compatible data format for context visualization.
     * This method processes job data, applies visual characteristics, and ensures various
     * items within the adapted dataset are either selectable and draggable or not, based on their type.
     *
     * @param parentContext the parent context associated with the current context
     * @param context the current context for which the jobs are being visualized
     * @param schedulerJobs a map of job identifiers to their corresponding scheduler job objects
     * @param schedulerJobsMapByIdentifier a map of alternate job identifiers to scheduler job objects
     * @param schedulerJobsImageMap a map of job identifiers to their visual representations as images
     * @param logicalBoundaries a map of job identifiers to their respective logical boundaries as rectangles
     *
     * @return a JSON-formatted string representation of the adapted jobs and visual components
     * @throws Draw2dAdapterException if an error occurs while processing or translating the jobs into the draw2d format
     */
    public String adaptJobs(Context parentContext, Context context, Map<String, SchedulerJob> schedulerJobs
        , Map<String, SchedulerJob> schedulerJobsMapByIdentifier, Map<String, Image> schedulerJobsImageMap, Map<String, Rectangle> logicalBoundaries) {

            ArrayList<Object> items = super._adaptJobs(parentContext, context, schedulerJobs
                , schedulerJobsMapByIdentifier, schedulerJobsImageMap, logicalBoundaries);
            this.addStatusRectangles(items, context, parentContext);

            items.forEach(item -> {
                if(!(item instanceof Image)) {
                    ((Item)item).setSelectable(false);
                    ((Item)item).setDraggable(false);
                }
            });

            try {
               return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
            }
            catch (JsonProcessingException e) {
                throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate jobs for" +
                    " context[%s] to the draw 2d data format", context.getName()), e);
            }
    }

    /**
     * Adds status rectangles to the provided list of items based on the context and parent context.
     * This method processes items in the list, checking their type and corresponding status,
     * and generates status rectangles which are appended to the item list.
     *
     * @param items         the list of objects to which status rectangles will be added
     * @param context       the current context used to determine the status of items
     * @param parentContext the parent context used to fetch additional context information
     */
    private void addStatusRectangles(List<Object> items, Context context, Context parentContext) {
        ArrayList<Object> statusRectangles = new ArrayList<>();

        items.forEach(item -> {
            if (item instanceof Image) {

                if(context.getScheduledJobsMap().get(((PositionedItem) item).getId()) instanceof SchedulerJobInstance) {
                    SchedulerJobInstance instance = (SchedulerJobInstance)context.getScheduledJobsMap()
                        .get(((PositionedItem) item).getId());

                    if(!instance.getAgentName().equals(JobConstants.CONTEXT_START_JOB) &&
                        !instance.getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                        RectangleBuilder rb = diagramBuilder.getRectangleBuilder()
                            .withId(((PositionedItem) item).getId() + "_status")
                            .withWidth(100)
                            .withHeight(100)
                            .withStroke(0)
                            .withRadius(20)
                            .withX(((PositionedItem) item).getX())
                            .withY(((PositionedItem) item).getY());

                        rb.withBgColor(instance.isErrorAcknowledged() != null ?
                            StatusColours.getInstanceStatusColour(instance.getStatus(), instance.isErrorAcknowledged())
                            :StatusColours.getInstanceStatusColour(instance.getStatus()));
                        rb.withColor(instance.isErrorAcknowledged() != null ?
                            StatusColours.getInstanceStatusColour(instance.getStatus(), instance.isErrorAcknowledged())
                            :StatusColours.getInstanceStatusColour(instance.getStatus()));

                        statusRectangles.add(rb.build());
                    }
                }
                else if(((Image) item).getUserData().getItemType().equals(UserData.CONTEXT)){
                    RectangleBuilder rb = diagramBuilder.getRectangleBuilder()
                        .withId(((PositionedItem) item).getUserData().getContextName() + "_status")
                        .withWidth(100)
                        .withHeight(100)
                        .withStroke(0)
                        .withRadius(12)
                        .withX(((PositionedItem) item).getX())
                        .withY(((PositionedItem) item).getY());

                    ContextInstance contextInstance = ContextHelper.getChildContextInstance(((Image) item).getUserData().getContextName(), (ContextInstance) parentContext);
                    rb.withBgColor(StatusColours.getInstanceStatusColour(contextInstance.getStatus()));
                    rb.withColor(StatusColours.getInstanceStatusColour(contextInstance.getStatus()));

                    statusRectangles.add(rb.build());
                }
            }
        });

        items.addAll(statusRectangles);
    }

    /**
     * Adapts the given context into a JSON-formatted string representation compatible with the draw2d visualization framework.
     * This method processes the context hierarchy and visual elements, such as images, to include additional status indicators
     * in the form of rectangles representing context statuses.
     *
     * @param context the context to be adapted into the draw2d format
     * @return a JSON-formatted string representation of the adapted context with status indicators
     * @throws Draw2dAdapterException if an error occurs during the adaptation or JSON processing
     */
    public String adaptContext(Context context) {
        Map<String, Context> contextMap = ContextHelper.getAllContexts(context);

        ArrayList<Object> items = super._adaptContext(context);

        // For context instances, we add rectangles that can be updated to reflect the
        // context status.
        ArrayList<Object> statusRectangles = new ArrayList<>();
        items.forEach(item -> {
            if(item instanceof Image) {
                RectangleBuilder rb = diagramBuilder.getRectangleBuilder();
                rb.withId(((PositionedItem) item).getId() + "_status")
                    .withWidth(98)
                    .withHeight(98)
                    .withRadius(20)
                    .withX(((PositionedItem)item).getX()+1)
                    .withY(((PositionedItem)item).getY()+1);

                if(contextMap.get(((PositionedItem) item).getId()) instanceof ContextInstance) {
                    rb.withBgColor(StatusColours.getInstanceStatusColour(((ContextInstance)contextMap.get(((PositionedItem) item).getId())).getStatus()));
                    rb.withColor(StatusColours.getInstanceStatusColour(((ContextInstance)contextMap.get(((PositionedItem) item).getId())).getStatus()));
                }

                statusRectangles.add(rb.build());
            }
        });

        items.addAll(statusRectangles);

        try {
           return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
        }
        catch (JsonProcessingException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate context[%s] to the draw 2d data format", context.getName()), e);
        }
    }
}
