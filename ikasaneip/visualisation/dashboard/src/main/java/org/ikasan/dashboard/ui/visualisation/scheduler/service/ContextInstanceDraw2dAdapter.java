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

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        try {
           return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
        }
        catch (JsonProcessingException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting to translate context[%s] to the draw 2d data format", context.getName()), e);
        }
    }
}
