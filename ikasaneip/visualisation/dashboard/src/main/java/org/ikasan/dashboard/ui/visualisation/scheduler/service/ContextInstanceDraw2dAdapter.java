package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.builder.*;
import org.ikasan.designer.model.*;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextInstanceDraw2dAdapter extends Draw2dAdapterBase {

    Logger logger = LoggerFactory.getLogger(ContextInstanceDraw2dAdapter.class);

    public String adaptJobs(Context context, Map<String, SchedulerJob> schedulerJobs) {
            ArrayList<Object> items = super._adaptJobs(context, schedulerJobs);
            this.addStatusRectangles(items, context);

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

    public String adaptContextView(Context context, String canvasJson) {
        try {
            List<LinkedHashMap> values = mapper.readValue(canvasJson, List.class);
            List<Object> positionedItems = new ArrayList<>();

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = mapper.readValue(mapper.writeValueAsBytes(value), Image.class);
                    positionedItems.add(image);
                } else if (value.get("type").equals("draw2d.shape.basic.Rectangle") && value.get("id").toString().startsWith("AND")) {
                    Rectangle rectangle = mapper.readValue(mapper.writeValueAsBytes(value), Rectangle.class);
                    rectangle.setSelectable(false);
                    positionedItems.add(rectangle);
                } else if (value.get("type").equals("draw2d.shape.basic.Rectangle") && value.get("id").toString().startsWith("OR")) {
                    Rectangle rectangle = mapper.readValue(mapper.writeValueAsBytes(value), Rectangle.class);
                    rectangle.setSelectable(false);
                    positionedItems.add(rectangle);
                }
                else if (value.get("type").equals("draw2d.Connection")) {
                    Connection connection = mapper.readValue(mapper.writeValueAsBytes(value), Connection.class);
                    connection.setSelectable(false);
                    connection.setDraggable(false);
                    positionedItems.add(connection);
                }
                else if (value.get("type").equals("draw2d.shape.basic.Label")) {
                    Label label = mapper.readValue(mapper.writeValueAsBytes(value), Label.class);
                    label.setSelectable(false);
                    positionedItems.add(label);
                }
                else if (value.get("type").equals("draw2d.shape.composite.Group")) {
                    Group group = mapper.readValue(mapper.writeValueAsBytes(value), Group.class);
                    group.setSelectable(false);
                    positionedItems.add(group);
                }
            }

            this.addStatusRectangles(positionedItems, context);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(positionedItems);
        }
        catch (IOException e) {
            throw new Draw2dAdapterException(String.format("An exception has occurred attempting enrich jobs with status for" +
                " context instance [%s] to the draw 2d data format", context.getName()), e);
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

                if(context.getScheduledJobsMap().get(((PositionedItem) item).getId()) instanceof SchedulerJobInstance) {
                    rb.withBgColor(StatusColours.getInstanceStatusColour(((SchedulerJobInstance)context.getScheduledJobsMap()
                        .get(((PositionedItem) item).getId())).getStatus()));
                    rb.withColor(StatusColours.getInstanceStatusColour(((SchedulerJobInstance)context.getScheduledJobsMap()
                        .get(((PositionedItem) item).getId())).getStatus()));
                }

                statusRectangles.add(rb.build());
            }
        });

        items.addAll(statusRectangles);
    }

    public String adaptContext(Context context) {
        Map<String, Context> contextMap = ContextHelper.getAllContexts(context);

        ArrayList<Object> items = super._adaptContext(context);

        // For context instances, we add rectngles that can be updated to reflect the
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
