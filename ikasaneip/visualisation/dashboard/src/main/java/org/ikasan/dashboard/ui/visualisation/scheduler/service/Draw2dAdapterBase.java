package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.designer.builder.*;
import org.ikasan.designer.model.*;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Draw2dAdapterBase {

    protected ObjectMapper mapper = new ObjectMapper();
    protected DiagramBuilder diagramBuilder = new DiagramBuilder();
    protected double jobMaxXExtent = 0;
    protected double jobMaxYExtent = 0;

    public Draw2dAdapterBase() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    protected ArrayList<Object> _adaptJobs(Context context, Map<String, SchedulerJob> schedulerJobs) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {
            context.getScheduledJobs().forEach(job -> {
                graph.addVertex(((SchedulerJob)job).getIdentifier());

                String image = getJobImage(schedulerJobs.get(((SchedulerJob)job).getJobName()));

                ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                    .withId(((SchedulerJob)job).getIdentifier())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath(image)
                    .withUserData(new UserDataBuilder()
                        .withAgentName(((SchedulerJob) job).getAgentName())
                        .withJobName(((SchedulerJob) job).getJobName())
                        .withIdentifier(((SchedulerJob) job).getIdentifier())
                        .build()
                    );

                SchedulerJob schedulerJob = schedulerJobs.get(((SchedulerJob) job).getJobName());

                if(schedulerJob instanceof InternalEventDrivenJob) {
                    jobBuilder.withLeftPort()
                        .withRightPort();
                }
                else if(schedulerJob instanceof QuartzScheduleDrivenJob) {
                    jobBuilder.withRightPort();
                }

                diagramBuilder.addItem(jobBuilder
                    .build());

            });

            Grouping grouping = new Grouping();

            context.getJobDependencies().forEach(jobDependency -> {
                if(((JobDependency)jobDependency).getLogicalGrouping() != null) {
                    this.manageLogicalGroupings(((JobDependency)jobDependency).getJobIdentifier(),
                        ((JobDependency)jobDependency).getLogicalGrouping(), diagramBuilder, graph);

                    this.getAllJobsInGrouping(((JobDependency)jobDependency).getLogicalGrouping(),
                        grouping);
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
            ArrayList<Object> imageOverlay = new ArrayList<>();
            ArrayList<Object> labels = new ArrayList<>();
            ArrayList<Object> groups = new ArrayList<>();

            items.forEach(item -> {
                if (item instanceof Image) {
                    mxCell cell = cellMap.get(((Item) item).getId());

                    if (cell != null) {
                        GroupBuilder groupBuilder = new GroupBuilder();
                        Group group = groupBuilder.build();

                        ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                        ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

                        double positionedItemCentre = ((PositionedItem) item).getX() + 50;
                        ((Image) item).setComposite(group.getId());

                        // assuming each letter is 8 units long
                        double labelLength = ((SchedulerJob) context.getScheduledJobsMap()
                            .get(((PositionedItem) item).getId())).getJobName().length() * 8;

                        Label label = new LabelBuilder().withText(((SchedulerJob) context.getScheduledJobsMap()
                            .get(((PositionedItem) item).getId())).getJobName())
                            .withX(positionedItemCentre - (labelLength / 2))
                            .withY(((PositionedItem) item).getY() + 110)
                            .withFontSize("14pt")
                            .withComposite(group.getId())
                            .build();

                        labels.add(label);
                        groups.add(group);
                    }
                }
            });

            this.addLogicGroupings(grouping, imageOverlay, cellMap, diagramBuilder);
            items.addAll(imageOverlay);
            items.addAll(labels);
            items.addAll(groups);

            return items;
        }

        return null;
    }

    protected ArrayList<Object> _adaptContext(Context context) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        DiagramBuilder diagramBuilder = new DiagramBuilder();

        graph.addVertex(context.getName());

        Image root = diagramBuilder.getImageBuilder()
            .withId(context.getName())
            .withHeight(100)
            .withWidth(100)
            .withPath("frontend/images/leaf_black.png")
            .withTopPort()
            .withBottomPort()
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
                .withPath("frontend/images/leaf_black.png")
                .withTopPort()
                .withBottomPort()
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
        compactTreeLayout.setNodeDistance(75);
        compactTreeLayout.setGroupPadding(100);

        compactTreeLayout.execute(jGraphXAdapter.getDefaultParent());

        Map<String, mxCell> cellMap = this.getCellMap(jGraphXAdapter);

        ArrayList<Object> items = diagramBuilder.build();
        ArrayList<Label> labels = new ArrayList<>();
        ArrayList<Object> groups = new ArrayList<>();

        items.forEach(item -> {
            if(item instanceof Rectangle || item instanceof Image) {
                mxCell cell = cellMap.get(((Item)item).getId());

                if(cell != null) {
                    GroupBuilder groupBuilder = new GroupBuilder();
                    Group group = groupBuilder.build();

                    ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                    ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

                    double positionedItemCentre = ((PositionedItem) item).getX() + 50;
                    ((PositionedItem) item).setComposite(group.getId());

                    // assuming each letter is 8 units long
                    double labelLength = ((PositionedItem)item).getId().length() * 8;

                    Label label = new LabelBuilder().withText(((PositionedItem)item).getId())
                        .withX(positionedItemCentre - (labelLength / 2))
                        .withY(((PositionedItem)item).getY() + 110)
                        .withFontSize("14pt")
                        .withComposite(group.getId())
                        .build();

                    labels.add(label);
                    groups.add(group);
                }
            }
        });

        items.addAll(groups);
        items.addAll(labels);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return items;
    }

    protected void manageContext(Context contextInstance, DefaultDirectedGraph<Object, DefaultEdge> graph,
                               DiagramBuilder diagramBuilder, Map<String, Context> contextInstanceMap) {

        if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            contextInstance.getContexts().forEach(c -> {
                contextInstanceMap.put(((Context)c).getName(), (Context)c);
                graph.addVertex(((Context)c).getName());
                graph.addEdge(contextInstance.getName(), ((Context)c).getName());

                Image branch = diagramBuilder.getImageBuilder()
                    .withId(((Context)c).getName())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath("frontend/images/leaf_black.png")
                    .withTopPort()
                    .withBottomPort()
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

    protected Map<String, mxCell> getCellMap(JGraphXAdapter<Object, DefaultEdge> jGraphXAdapter) {
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

    protected void manageLogicalGroupings(String jobIdentifier, LogicalGrouping logicalGrouping, DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object, DefaultEdge> graph) {
        if(logicalGrouping.getLogicalGrouping() != null) {
            manageLogicalGroupings(jobIdentifier, logicalGrouping.getLogicalGrouping(), diagramBuilder, graph);
        }

        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            logicalGrouping.getAnd().forEach(and -> {
                if(and.getLogicalGrouping() != null) {
                    manageLogicalGroupings(jobIdentifier, and.getLogicalGrouping(), diagramBuilder, graph);
                }
                else {
                    graph.addEdge(and.getIdentifier(), jobIdentifier);

                    ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
                    connectionBuilder.withSource(
                        diagramBuilder.getConnectionDetailsBuilder()
                            .withNode(and.getIdentifier())
                            .withPort("rightHybridSource")
                            .build()
                    );

                    connectionBuilder.withTarget(
                        diagramBuilder.getConnectionDetailsBuilder()
                            .withNode(jobIdentifier)
                            .withPort("leftHybridTarget")
                            .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                            .build()
                    );

                    diagramBuilder.addItem(connectionBuilder.build());
                }
            });
        }

        if(logicalGrouping.getOr() != null && !logicalGrouping.getOr().isEmpty()) {
            logicalGrouping.getOr().forEach(or -> {
                if(or.getLogicalGrouping() != null) {
                    manageLogicalGroupings(jobIdentifier, or.getLogicalGrouping(), diagramBuilder, graph);
                }
                else {
                    graph.addEdge(or.getIdentifier(), jobIdentifier);

                    ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
                    connectionBuilder.withSource(
                        diagramBuilder.getConnectionDetailsBuilder()
                            .withNode(or.getIdentifier())
                            .withPort("rightHybridSource")
                            .build()
                    );

                    connectionBuilder.withTarget(
                        diagramBuilder.getConnectionDetailsBuilder()
                            .withNode(jobIdentifier)
                            .withPort("leftHybridTarget")
                            .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                            .build()
                    );

                    diagramBuilder.addItem(connectionBuilder.build());
                }
            });
        }
    }

    protected void getAllJobsInGrouping(LogicalGrouping logicalGrouping, Grouping grouping) {
        Grouping nestedGrouping = null;
        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            nestedGrouping = new Grouping();
            nestedGrouping.setType("AND");
            grouping.getNestedGrouping().add(nestedGrouping);
            for (And and : logicalGrouping.getAnd()) {

                if (and.getIdentifier() != null) {
                    nestedGrouping.getJobIdentifiers().add(and.getIdentifier());
                }

                if (and.getLogicalGrouping() != null) {
                    getAllJobsInGrouping(and.getLogicalGrouping(), nestedGrouping);
                }
            }
        }

        if(logicalGrouping.getOr() != null && !logicalGrouping.getOr().isEmpty()) {
            nestedGrouping = new Grouping();
            nestedGrouping.setType("OR");
            grouping.getNestedGrouping().add(nestedGrouping);
            for (Or or : logicalGrouping.getOr()) {
                if(or.getIdentifier() != null) {
                    nestedGrouping.getJobIdentifiers().add(or.getIdentifier());
                }

                if (or.getLogicalGrouping() != null) {
                    getAllJobsInGrouping(or.getLogicalGrouping(), nestedGrouping);
                }
            }
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            getAllJobsInGrouping(logicalGrouping.getLogicalGrouping(), nestedGrouping != null ? nestedGrouping : grouping);
        }
    }

    protected void addLogicGroupings(Grouping grouping, ArrayList<Object> imageOverlay, Map<String, mxCell> cellMap, DiagramBuilder diagramBuilder) {
        grouping.getNestedGrouping().forEach(nestedGrouping -> {
            this.addLogicGroupings(nestedGrouping, imageOverlay, cellMap, diagramBuilder);

            if(nestedGrouping.getJobIdentifiers().size() > 1 || (nestedGrouping.getJobIdentifiers().size() > 0 && !nestedGrouping.getNestedGrouping().isEmpty())) {
                AtomicReference<Double> xMinExtent = new AtomicReference<>();
                xMinExtent.set(-1.0);
                AtomicReference<Double> xMaxExtent = new AtomicReference<>();
                xMaxExtent.set(-1.0);
                AtomicReference<Double> yMinExtent = new AtomicReference<>();
                yMinExtent.set(-1.0);
                AtomicReference<Double> yMaxExtent = new AtomicReference<>();
                yMaxExtent.set(-1.0);

                this.calculateExtents(nestedGrouping, xMinExtent, xMaxExtent, yMinExtent, yMaxExtent, cellMap);

                RectangleBuilder rb = diagramBuilder.getRectangleBuilder()
                    .withWidth(xMaxExtent.get() - xMinExtent.get() + 160)
                    .withHeight(yMaxExtent.get() - yMinExtent.get() + 160)
                    .withStroke(3)
                    .withRadius(10)
                    .withX(xMinExtent.get() - 30)
                    .withY(yMinExtent.get() - 30)
                    .withSelectable(true)
                    .withDraggable(true)
                    .withResizable(true)
                    .withBgColor("rgba(27,27,27,0.0)");

                if (nestedGrouping.getType().equals("AND")) {
                    rb.withId("AND-" + UUID.randomUUID().toString())
                        .withColor(IkasanColours.SCHEDULER_AND)
                        .withDasharray("--");
                } else {
                    rb.withId("OR-" + UUID.randomUUID().toString())
                        .withColor(IkasanColours.SCHEDULER_OR)
                        .withDasharray("--..");
                }

                imageOverlay.add(rb.build());
            }
        });
    }

    protected void calculateExtents(Grouping grouping, AtomicReference<Double> xMinExtent, AtomicReference<Double> xMaxExtent,
                                  AtomicReference<Double> yMinExtent, AtomicReference<Double> yMaxExtent, Map<String, mxCell> cellMap) {
        grouping.getNestedGrouping().forEach(nestedGrouping -> {
            this.calculateExtents(nestedGrouping, xMinExtent, xMaxExtent, yMinExtent, yMaxExtent, cellMap);

            nestedGrouping.getJobIdentifiers().forEach(id -> {
                mxCell cell = cellMap.get(id);

                if (xMinExtent.get() == -1) xMinExtent.set(cell.getGeometry().getX() + 600);
                else if (xMinExtent.get() > cell.getGeometry().getX() + 600) {
                    xMinExtent.set(cell.getGeometry().getX() + 600);
                }
                if (xMaxExtent.get() == -1) xMaxExtent.set(cell.getGeometry().getX() + 600);
                else if (xMaxExtent.get() < cell.getGeometry().getX() + 600) {
                    xMaxExtent.set(cell.getGeometry().getX() + 600);
                }
                if (yMinExtent.get() == -1) yMinExtent.set(cell.getGeometry().getY() + 600);
                else if (yMinExtent.get() > cell.getGeometry().getY() + 600) {
                    yMinExtent.set(cell.getGeometry().getY() + 600);
                }
                if (yMaxExtent.get() == -1) yMaxExtent.set(cell.getGeometry().getY() + 600);
                else if (yMaxExtent.get() < cell.getGeometry().getY() + 600) {
                    yMaxExtent.set(cell.getGeometry().getY() + 600);
                }

                if(cell.getGeometry().getY() + 600 > this.jobMaxYExtent) {
                    this.jobMaxYExtent = cell.getGeometry().getY() + 600;
                }

                if(cell.getGeometry().getX() + 600 > this.jobMaxXExtent) {
                    this.jobMaxXExtent = cell.getGeometry().getX() + 600;
                }
            });
        });

        grouping.getJobIdentifiers().forEach(id -> {
            mxCell cell = cellMap.get(id);

            if (xMinExtent.get() == -1) xMinExtent.set(cell.getGeometry().getX() + 600);
            else if (xMinExtent.get() > cell.getGeometry().getX() + 600) {
                xMinExtent.set(cell.getGeometry().getX() + 600);
            }
            if (xMaxExtent.get() == -1) xMaxExtent.set(cell.getGeometry().getX() + 600);
            else if (xMaxExtent.get() < cell.getGeometry().getX() + 600) {
                xMaxExtent.set(cell.getGeometry().getX() + 600);
            }
            if (yMinExtent.get() == -1) yMinExtent.set(cell.getGeometry().getY() + 600);
            else if (yMinExtent.get() > cell.getGeometry().getY() + 600) {
                yMinExtent.set(cell.getGeometry().getY() + 600);
            }
            if (yMaxExtent.get() == -1) yMaxExtent.set(cell.getGeometry().getY() + 600);
            else if (yMaxExtent.get() < cell.getGeometry().getY() + 600) {
                yMaxExtent.set(cell.getGeometry().getY() + 600);
            }

            if(cell.getGeometry().getY() + 600 > this.jobMaxYExtent) {
                this.jobMaxYExtent = cell.getGeometry().getY() + 600;
            }

            if(cell.getGeometry().getX() + 600 > this.jobMaxXExtent) {
                this.jobMaxXExtent = cell.getGeometry().getX() + 600;
            }
        });

        xMinExtent.set(xMinExtent.get() - 15);
        xMaxExtent.set(xMaxExtent.get() + 15);
        yMinExtent.set(yMinExtent.get() - 15);
        yMaxExtent.set(yMaxExtent.get() + 15);
    }

    protected String getJobImage(SchedulerJob schedulerJob) {
        String image = "frontend/images/command_black.png";

        if(schedulerJob instanceof FileEventDrivenJob) {
            image = "frontend/images/file_black.png";
        }
        else if(schedulerJob instanceof QuartzScheduleDrivenJob) {
            image = "frontend/images/time_black.png";
        }

        return image;
    }
}
