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
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.context.support.UiApplicationContextUtils;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ContextTemplateDraw2dAdapter extends Draw2dAdapterBase {

    Logger logger = LoggerFactory.getLogger(ContextTemplateDraw2dAdapter.class);

    public String adaptJobs(Context context) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                graph.addVertex(((SchedulerJob)job).getIdentifier());

                String image = "frontend/images/and.png";
//                if(this.isTerminalJob((SchedulerJob)job, context.getJobDependencies())) {
//                    image = "frontend/images/terminal.png";
//                }

                diagramBuilder.addItem(diagramBuilder.getImageBuilder()
                    .withId(((SchedulerJob)job).getIdentifier())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath(image)
                    .withLeftAndRightPorts()
                    .withUserData(new UserDataBuilder()
                        .withAgentName(((SchedulerJob) job).getAgentName())
                        .withJobName(((SchedulerJob) job).getJobName())
                        .withIdentifier(((SchedulerJob) job).getIdentifier())
                        .build()
                    ).build());
            });

            List<Grouping> groupings = new ArrayList<>();

            Grouping grouping = new Grouping();

            context.getJobDependencies().forEach(jobDependency -> {
                if(((JobDependency)jobDependency).getLogicalGrouping() != null) {
                    this.manageLogicalGroupings(((JobDependency)jobDependency).getJobIdentifier(),
                        ((JobDependency)jobDependency).getLogicalGrouping(), diagramBuilder, graph);

//                    context.getScheduledJobs().forEach(job -> {

                        this.getAllJobsInGrouping(((JobDependency)jobDependency).getLogicalGrouping(),
                            grouping);

                        if(grouping.getJobIdentifiers().size() > 1) {
                            groupings.add(grouping);
                        }
//                    });
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
            compactTreeLayout.setInterHierarchySpacing(1000);
            compactTreeLayout.setInterRankCellSpacing(1000);

            compactTreeLayout.execute(jGraphXAdapter.getDefaultParent());

            Map<String, mxCell> cellMap = this.getCellMap(jGraphXAdapter);

            ArrayList<Object> items = diagramBuilder.build();
//            ArrayList<Label> labels = new ArrayList<>();
            ArrayList<Object> imageOverlay = new ArrayList<>();
//            ArrayList<Object> groups = new ArrayList<>();

            items.forEach(item -> {
                if (item instanceof Rectangle || item instanceof Image) {

//                    GroupBuilder groupBuilder = new GroupBuilder();
//                    Group group = groupBuilder.build();
//
//                    groups.add(group);group

                    mxCell cell = cellMap.get(((Item) item).getId());

                    if (cell != null) {
                        ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                        ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

//                        if(isTerminalJob((SchedulerJob) context.getScheduledJobsMap().get(((PositionedItem) item).getId())
//                            , context.getJobDependencies())) {
//                            CircleBuilder cb = diagramBuilder.getCircleBuilder()
//                                .withId(((PositionedItem) item).getId() + "_status")
//                                .withTopAndBottomPorts()
//                                .withWidth(100)
//                                .withHeight(100)
//                                .withStroke(0)
//                                .withX(((PositionedItem) item).getX())
//                                .withY(((PositionedItem) item).getY())
//                                .withComposite(group.getId());
//
//                            if(context.getScheduledJobsMap().get(((PositionedItem) item).getId()) instanceof SchedulerJobInstance) {
//                                cb.withBgColor(StatusColours.getInstanceStatusColour(((SchedulerJobInstance)context.getScheduledJobsMap()
//                                    .get(((PositionedItem) item).getId())).getStatus()));
//                            }
//                            else {
//                                cb.withBgColor(IkasanColours.IKASAN_ORANGE_50);
//                            }
//
//                            imageOverlay.add(cb.build());
//                        }
//                        else {
//                            RectangleBuilder rb = diagramBuilder.getRectangleBuilder()
//                                .withId(((PositionedItem) item).getId() + "_status")
//                                .withTopAndBottomPorts()
//                                .withWidth(100)
//                                .withHeight(100)
//                                .withStroke(0)
//                                .withRadius(20)
//                                .withComposite(group.getId())
//                                .withX(((PositionedItem) item).getX())
//                                .withY(((PositionedItem) item).getY());
//
//                            if(context.getScheduledJobsMap().get(((PositionedItem) item).getId()) instanceof SchedulerJobInstance) {
//                              rb.withBgColor(StatusColours.getInstanceStatusColour(((SchedulerJobInstance)context.getScheduledJobsMap()
//                                    .get(((PositionedItem) item).getId())).getStatus()));
//                            }
//                            else {
//                                rb.withBgColor(IkasanColours.IKASAN_ORANGE_50);
//                            }
//
//                            imageOverlay.add(rb.build());
//                        }

//                        labels.add(new LabelBuilder().withText(((SchedulerJob) context.getScheduledJobsMap()
//                            .get(((PositionedItem) item).getId())).getJobName())
//                            .withX(((PositionedItem) item).getX() - 15)
//                            .withY(((PositionedItem) item).getY() + 110)
//                            .withComposite(group.getId())
//                            .build());
                    }

//                    ((PositionedItem) item).setComposite(group.getId());
                }
            });


            super.addLogicGroupings(grouping, imageOverlay, cellMap, diagramBuilder);

//            items.addAll(labels);
            items.addAll(imageOverlay);
//            items.addAll(groups);

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

        public String adaptContext(Context context) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        DiagramBuilder diagramBuilder = new DiagramBuilder();

        graph.addVertex(context.getName());

        Image root = diagramBuilder.getImageBuilder()
            .withId(context.getName())
            .withHeight(100)
            .withWidth(100)
            .withPath("frontend/images/trunk.png")
            .withTopAndBottomPorts()
            .build();

        diagramBuilder.addItem(root);

        Map<String, Context> contextMap = new HashMap<>();

        contextMap.put(context.getName(), context);

        context.getContexts().forEach(c -> {
            graph.addVertex(((Context)c).getName());
            graph.addEdge(context.getName(), ((Context)c).getName());

            contextMap.put(((Context)c).getName(), (Context)c);

            Image branch = diagramBuilder.getImageBuilder()
                .withId(((Context)c).getName())
                .withHeight(100)
                .withWidth(100)
                .withPath("frontend/images/branch.png")
                .withTopAndBottomPorts()
                .withLeftAndRightPorts()
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

            this.manageContext((Context) c, graph, diagramBuilder, contextMap);
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
        ArrayList<Object> groups = new ArrayList<>();

        items.forEach(item -> {
            if(item instanceof Rectangle || item instanceof Image) {
                mxCell cell = cellMap.get(((Item)item).getId());

                if(cell != null) {
                    GroupBuilder groupBuilder = new GroupBuilder();
                    Group group = groupBuilder.build();

                    groups.add(group);

                    ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                    ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

                    RectangleBuilder rb = diagramBuilder.getRectangleBuilder();
                    rb.withId(((PositionedItem) item).getId() + "_status")
                        .withTopAndBottomPorts()
                        .withWidth(90)
                        .withHeight(90)
                        .withX(((PositionedItem)item).getX() + 5)
                        .withY(((PositionedItem)item).getY() + 5)
                        .withComposite(group.getId());

                    if(contextMap.get(((PositionedItem) item).getId()) instanceof ContextInstance) {
                        rb.withBgColor(StatusColours.getInstanceStatusColour(((ContextInstance)contextMap.get(((PositionedItem) item).getId())).getStatus()));
                    }
                    else {
                        rb.withBgColor(IkasanColours.IKASAN_ORANGE_50);
                    }

                    imageOverlay.add(rb.build());

                    labels.add(new LabelBuilder().withText(((PositionedItem)item).getId())
                        .withX(((PositionedItem)item).getX() - 15)
                        .withY(((PositionedItem)item).getY() + 110)
                        .withComposite(group.getId())
                        .build());

                    ((PositionedItem) item).setComposite(group.getId());
                }
            }
        });

//        items.addAll(imageOverlay);
        items.addAll(groups);
        items.addAll(labels);

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

    private void manageContext(Context contextInstance, DefaultDirectedGraph<Object, DefaultEdge> graph,
                               DiagramBuilder diagramBuilder,Map<String, Context> contextInstanceMap) {

        if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            contextInstance.getContexts().forEach(c -> {
                contextInstanceMap.put(((Context)c).getName(), (Context)c);
                graph.addVertex(((Context)c).getName());
                graph.addEdge(contextInstance.getName(), ((Context)c).getName());

                Image branch = diagramBuilder.getImageBuilder()
                    .withId(((Context)c).getName())
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

                this.manageContext((Context)c, graph, diagramBuilder, contextInstanceMap);
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

            if(jobDependency.getLogicalGrouping() != null && jobDependency.getLogicalGrouping().getAnd() != null) {
                jobDependency.getLogicalGrouping().getAnd().forEach(and -> {
                    if (and.getIdentifier().equals(schedulerJob.getIdentifier())) {
                        isTerminal.set(false);
                    }
                });
            }

            if(jobDependency.getLogicalGrouping() != null && jobDependency.getLogicalGrouping().getOr() != null) {
                jobDependency.getLogicalGrouping().getOr().forEach(or -> {
                    if (or.getIdentifier().equals(schedulerJob.getIdentifier())) {
                        isTerminal.set(false);
                    }
                });
            }

            if(jobDependency.getLogicalGrouping() != null && jobDependency.getLogicalGrouping().getNot() != null) {
                jobDependency.getLogicalGrouping().getNot().forEach(not -> {
                    if(not.getIdentifier().equals(schedulerJob.getIdentifier())) {
                        isTerminal.set(false);
                    }
                });
            }
        });

        return isTerminal.get();
    }

    public String adaptJob(SchedulerJob schedulerJob) {
        String image = "frontend/images/and.png";

        String result = null;
        // todo clean this up. still a hack.
        try {
            result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.diagramBuilder.getImageBuilder()
                .withId(schedulerJob.getIdentifier())
                .withHeight(100)
                .withWidth(100)
                .withX(super.jobMaxXExtent)
                .withY(super.jobMaxYExtent)
                .withPath(image)
                .withLeftAndRightPorts()
                .withUserData(new UserDataBuilder()
                    .withIdentifier(schedulerJob.getIdentifier())
                    .withJobName(schedulerJob.getJobName())
                    .withAgentName(schedulerJob.getAgentName())
                    .build())
                .build());
            logger.debug(result);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return result;
    }
}
