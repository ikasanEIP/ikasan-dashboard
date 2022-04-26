package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.builder.ConnectionBuilder;
import org.ikasan.designer.builder.DiagramBuilder;
import org.ikasan.designer.builder.LabelBuilder;
import org.ikasan.designer.builder.RectangleBuilder;
import org.ikasan.designer.model.*;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScheduledContextDraw2dAdapter {

    Logger logger = LoggerFactory.getLogger(ScheduledContextDraw2dAdapter.class);

    public String adaptJobs(ContextInstance contextInstance) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        DiagramBuilder diagramBuilder = new DiagramBuilder();

        if(contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {
            contextInstance.getScheduledJobs().forEach(job -> {
                graph.addVertex(job.getIdentifier());

                String image = "frontend/images/and.png";
                if(this.isTerminalJob(job, contextInstance.getJobDependencies())) {
                    image = "frontend/images/terminal.png";
                }

                diagramBuilder.addItem(diagramBuilder.getImageBuilder()
                    .withId(job.getIdentifier())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath(image)
                    .withLeftAndRightPorts()
                    .build());
            });

            contextInstance.getJobDependencies().forEach(jobDependency -> {
                if (jobDependency.getLogicalGrouping() != null && jobDependency.getLogicalGrouping().getAnd() != null) {
                    jobDependency.getLogicalGrouping().getAnd().forEach(and -> {
                        graph.addEdge(and.getIdentifier(), jobDependency.getJobIdentifier());

                        ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
                        connectionBuilder.withSource(
                            diagramBuilder.getConnectionDetailsBuilder()
                                .withNode(and.getIdentifier())
                                .withPort("rightHybridSource")
                                .build()
                        );

                        connectionBuilder.withTarget(
                            diagramBuilder.getConnectionDetailsBuilder()
                                .withNode(jobDependency.getJobIdentifier())
                                .withPort("leftHybridTarget")
                                .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                                .build()
                        );

                        diagramBuilder.addItem(connectionBuilder.build());
                    });
                }
            });


            JGraphXAdapter<Object, DefaultEdge> jGraphXAdapter
                = new JGraphXAdapter<>(graph);

            this.getCellMap(jGraphXAdapter)
                .entrySet()
                .forEach(entry -> {
                    mxCell cell = entry.getValue();
                    if(cell.isVertex()) {
                        cell.getGeometry().setWidth(100);
                        cell.getGeometry().setHeight(100);
                    }
                });

            mxHierarchicalLayout compactTreeLayout = new mxHierarchicalLayout(jGraphXAdapter);
            compactTreeLayout.setOrientation(SwingConstants.WEST);
            compactTreeLayout.setIntraCellSpacing(120);
            compactTreeLayout.setInterHierarchySpacing(300);
            compactTreeLayout.setInterRankCellSpacing(300);

            compactTreeLayout.execute(jGraphXAdapter.getDefaultParent());

            Map<String, mxCell> cellMap = this.getCellMap(jGraphXAdapter);

            ArrayList<Object> items = diagramBuilder.build();
            ArrayList<Label> labels = new ArrayList<>();
            ArrayList<Object> imageOverlay = new ArrayList<>();

            items.forEach(item -> {
                if (item instanceof Rectangle || item instanceof Image) {
                    mxCell cell = cellMap.get(((Item) item).getId());

                    if (cell != null) {
                        ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                        ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

                        if(isTerminalJob(contextInstance.getScheduledJobsMap().get(((PositionedItem) item).getId())
                            , contextInstance.getJobDependencies())) {
                            imageOverlay.add(diagramBuilder.getCircleBuilder()
                                .withBgColor(StatusColours.getInstanceStatusColour(contextInstance.getScheduledJobsMap()
                                    .get(((PositionedItem) item).getId()).getStatus()))
                                .withId(((PositionedItem) item).getId() + "_status")
                                .withTopAndBottomPorts()
                                .withWidth(100)
                                .withHeight(100)
                                .withStroke(0)
                                .withX(((PositionedItem) item).getX())
                                .withY(((PositionedItem) item).getY()).build());
                        }
                        else {
                            imageOverlay.add(diagramBuilder.getRectangleBuilder()
                                .withBgColor(StatusColours.getInstanceStatusColour(contextInstance.getScheduledJobsMap()
                                    .get(((PositionedItem) item).getId()).getStatus()))
                                .withId(((PositionedItem) item).getId() + "_status")
                                .withTopAndBottomPorts()
                                .withWidth(100)
                                .withHeight(100)
                                .withStroke(0)
                                .withRadius(20)
                                .withX(((PositionedItem) item).getX())
                                .withY(((PositionedItem) item).getY()).build());
                        }

                        labels.add(new LabelBuilder().withText(contextInstance.getScheduledJobsMap()
                            .get(((PositionedItem) item).getId()).getJobName())
                            .withX(((PositionedItem) item).getX() - 15)
                            .withY(((PositionedItem) item).getY() + 110)
                            .build());
                    }
                }
            });

            items.addAll(labels);
            items.addAll(imageOverlay);

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


            String result = null;
            // todo clean this up. still a hack.
            try {
                result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
                logger.debug(result);
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            logger.debug(result);
            return result;
        }

        return null;
    }

    public String adaptContext(ContextInstance contextInstance) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        DiagramBuilder diagramBuilder = new DiagramBuilder();

        graph.addVertex(contextInstance.getName());

        Image root = diagramBuilder.getImageBuilder()
            .withId(contextInstance.getName())
            .withHeight(100)
            .withWidth(100)
            .withPath("frontend/images/trunk.png")
            .withTopAndBottomPorts()
            .build();

        diagramBuilder.addItem(root);

        Map<String, ContextInstance> contextInstanceMap = new HashMap<>();

        contextInstanceMap.put(contextInstance.getName(), contextInstance);

        contextInstance.getContexts().forEach(c -> {
            graph.addVertex(c.getName());
            graph.addEdge(contextInstance.getName(), c.getName());

            contextInstanceMap.put(c.getName(), c);

            Image branch = diagramBuilder.getImageBuilder()
                .withId(c.getName())
                .withHeight(100)
                .withWidth(100)
                .withPath("frontend/images/branch.png")
                .withTopAndBottomPorts()
                .build();

            diagramBuilder.addItem(branch);

            ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
            connectionBuilder.withSource(
                diagramBuilder.getConnectionDetailsBuilder()
                .withNode(root.getId())
                .withPort("bottomHybridSource")
                .build()
            );

            connectionBuilder.withTarget(
                diagramBuilder.getConnectionDetailsBuilder()
                    .withNode(branch.getId())
                    .withPort("topHybridTarget")
                    .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                    .build()
            );

            diagramBuilder.addItem(connectionBuilder.build());

            this.manageContext(c, graph, diagramBuilder, contextInstanceMap);
        });

        JGraphXAdapter<Object, DefaultEdge> jGraphXAdapter
            = new JGraphXAdapter<>(graph);

        this.getCellMap(jGraphXAdapter)
            .entrySet()
            .forEach(entry -> {
                mxCell cell = entry.getValue();
                if(cell.isVertex()) {
                    cell.getGeometry().setWidth(200);
                    cell.getGeometry().setHeight(100);
                }
            });

        mxCompactTreeLayout compactTreeLayout = new mxCompactTreeLayout(jGraphXAdapter);
        compactTreeLayout.setHorizontal(false);
        compactTreeLayout.setLevelDistance(200);
        compactTreeLayout.setEdgeRouting(true);

        compactTreeLayout.execute(jGraphXAdapter.getDefaultParent());

        Map<String, mxCell> cellMap = this.getCellMap(jGraphXAdapter);

        ArrayList<Object> items = diagramBuilder.build();
        ArrayList<Label> labels = new ArrayList<>();

        ArrayList<Object> imageOverlay = new ArrayList<>();

        items.forEach(item -> {
            if(item instanceof Rectangle || item instanceof Image) {
                mxCell cell = cellMap.get(((Item)item).getId());

                if(cell != null) {
                    ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                    ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

                    RectangleBuilder rb = diagramBuilder.getRectangleBuilder();
                    rb.withBgColor(StatusColours.getInstanceStatusColour(contextInstanceMap.get(((PositionedItem) item).getId()).getStatus()))
                        .withId(((PositionedItem) item).getId() + "_status")
                        .withTopAndBottomPorts()
                        .withWidth(90)
                        .withHeight(90)
                        .withX(((PositionedItem)item).getX() + 5)
                        .withY(((PositionedItem)item).getY() + 5);

                    imageOverlay.add(rb.build());

                    labels.add(new LabelBuilder().withText(((PositionedItem)item).getId())
                        .withX(((PositionedItem)item).getX() - 15)
                        .withY(((PositionedItem)item).getY() + 110)
                        .build());
                }
            }
        });

        items.addAll(labels);
        items.addAll(imageOverlay);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


        String result = null;
        // todo clean this up. still a hack.
        try {
            result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
            logger.debug(result);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void manageContext(ContextInstance contextInstance, DefaultDirectedGraph<Object, DefaultEdge> graph,
                               DiagramBuilder diagramBuilder, Map<String, ContextInstance> contextInstanceMap) {

        if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            contextInstance.getContexts().forEach(c -> {
                contextInstanceMap.put(c.getName(), c);
                graph.addVertex(c.getName());
                graph.addEdge(contextInstance.getName(), c.getName());

                Image branch = diagramBuilder.getImageBuilder()
                    .withId(c.getName())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath("frontend/images/branch.png")
                    .withTopAndBottomPorts()
                    .build();

                diagramBuilder.addItem(branch);

                ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
                connectionBuilder.withSource(
                    diagramBuilder.getConnectionDetailsBuilder()
                        .withNode(contextInstance.getName())
                        .withPort("bottomHybridSource")
                        .build()
                );

                connectionBuilder.withTarget(
                    diagramBuilder.getConnectionDetailsBuilder()
                        .withNode(branch.getId())
                        .withPort("topHybridTarget")
                        .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                        .build()
                );

                diagramBuilder.addItem(connectionBuilder.build());

                this.manageContext(c, graph, diagramBuilder, contextInstanceMap);
            });
        }
    }

    private Map<String, mxCell> getCellMap(JGraphXAdapter<Object, DefaultEdge> jGraphXAdapter) {
        Map<String, mxCell> cellMap = new HashMap<>();

        jGraphXAdapter.clearSelection();
        jGraphXAdapter.selectAll();
        Object[] cells = jGraphXAdapter.getSelectionCells();

        for(Object c: cells) {
            mxCell cell = (mxCell)c;

            if(cell.isVertex()) {
                cellMap.put(cell.getValue().toString(), cell);
            }
        }

        return cellMap;
    }

    private boolean isTerminalJob(SchedulerJob schedulerJob, List<JobDependency> jobDependencies) {
        AtomicBoolean isTerminal = new AtomicBoolean(true);

        jobDependencies.forEach(jobDependency -> {
            if(jobDependency.getLogicalGrouping().getAnd() != null) {
                jobDependency.getLogicalGrouping().getAnd().forEach(and -> {
                    if (and.getIdentifier().equals(schedulerJob.getIdentifier())) {
                        isTerminal.set(false);
                    }
                });
            }

            if(jobDependency.getLogicalGrouping().getOr() != null) {
                jobDependency.getLogicalGrouping().getOr().forEach(or -> {
                    if (or.getIdentifier().equals(schedulerJob.getIdentifier())) {
                        isTerminal.set(false);
                    }
                });
            }

            if(jobDependency.getLogicalGrouping().getNot() != null) {
                jobDependency.getLogicalGrouping().getNot().forEach(not -> {
                    if(not.getIdentifier().equals(schedulerJob.getIdentifier())) {
                        isTerminal.set(false);
                    }
                });
            }
        });

        return isTerminal.get();
    }
}
