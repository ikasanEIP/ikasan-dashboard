package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.visualisation.scheduler.model.Tree;
import org.ikasan.dashboard.ui.visualisation.scheduler.model.TreeNode;
import org.ikasan.designer.builder.*;
import org.ikasan.designer.model.*;
import org.ikasan.job.orchestration.model.context.ContextTransition;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.instance.model.FileEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.QuartzScheduleDrivenJobInstance;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class Draw2dAdapterBase {

    Logger logger = LoggerFactory.getLogger(ContextInstanceDraw2dAdapter.class);

    protected ObjectMapper mapper = new ObjectMapper();
    protected DiagramBuilder diagramBuilder = new DiagramBuilder();
    protected double jobMaxXExtent = 0;
    protected double jobMaxYExtent = 0;

    public Draw2dAdapterBase() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    protected ArrayList<Object> _adaptJobs(Context parentContext, Context context, Map<String, SchedulerJob> schedulerJobs, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        if(!context.getContexts().isEmpty()) {
            logger.info("Got some child contexts");
            getLinkedContexts(context, parentContext, internalEventDrivenJobMap);
        }
        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {

//            HashSet<ContextTransition> previousContexts = new HashSet<>();
//
//            context.getScheduledJobs().forEach(job -> {
//                List<ContextTransition>  jobsInto = ContextHelper.determineIfJobsTransitionFromOtherContexts(parentContext, ((SchedulerJob)job).getJobName(),
//                    context.getName(), internalEventDrivenJobMap);
//
//                jobsInto.forEach(jobsOtherContexts -> previousContexts.add(jobsOtherContexts));
//            });
//
//            previousContexts.forEach(previousContext -> logger.info("Previous context -> " + previousContext));

            List<ContextTransition> previousContexts = this.getPreviousContextTransitions(parentContext, context, internalEventDrivenJobMap);

            List<ContextTransition> subsequentTransitions = ContextHelper.determineIfJobsTransitionToOtherContexts
                (parentContext, context.getScheduledJobsMap(), context, internalEventDrivenJobMap);

//            subsequentTransitions.forEach(ct -> logger.info("Context transition - " + ct));

            DefaultDirectedGraph<Object, DefaultEdge> graph
                = new DefaultDirectedGraph<>(NoEdgeLabel.class);

            Grouping grouping = new Grouping();

            if(context.getJobDependencies() != null) {
                context.getJobDependencies().forEach(jobDependency -> {
                    if (((JobDependency) jobDependency).getLogicalGrouping() != null) {
                        this.getAllJobsInGrouping(((JobDependency) jobDependency).getLogicalGrouping(),
                            grouping);
                    }
                });
            }

            List<SchedulerJob> jobs = this.getSchedulerJobsFromGrouping(context, grouping);

            context.getScheduledJobs().forEach(job -> {
                if(!jobs.contains(job)) {
                    jobs.add((SchedulerJob) job);
                }
            });

            jobs.forEach(job -> {
                graph.addVertex(job.getIdentifier());

                SchedulerJob schedulerJob = schedulerJobs.get(job.getJobName());
                String image = getJobImage(schedulerJob);

                UserDataBuilder userDataBuilder = new UserDataBuilder()
                    .withAgentName(job.getAgentName())
                    .withJobName(job.getJobName())
                    .withIdentifier(job.getIdentifier());

                if(schedulerJob instanceof InternalEventDrivenJob || schedulerJob instanceof InternalEventDrivenJobInstance) {
                    userDataBuilder.withItemType(UserData.INTERNAL_EVENT_DRIVEN_JOB);
                }
                else if(schedulerJob instanceof FileEventDrivenJob || schedulerJob instanceof FileEventDrivenJobInstance) {
                    userDataBuilder.withItemType(UserData.FILE_EVENT_DRIVEN_JOB);
                }
                else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
                    userDataBuilder.withItemType(UserData.QUARTZ_EVENT_DRIVEN_JOB);
                }

                ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                    .withId(job.getIdentifier())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath(image)
                    .withUserData(userDataBuilder.build());

                if(schedulerJob instanceof InternalEventDrivenJob || schedulerJob instanceof InternalEventDrivenJobInstance) {
                    jobBuilder.withLeftPort()
                        .withRightPort();
                }
                else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
                    jobBuilder.withRightPort();
                }

                diagramBuilder.addItem(jobBuilder
                    .build());

            });

            if(context.getJobDependencies() != null) {
                context.getJobDependencies().forEach(jobDependency -> {
                    if (((JobDependency) jobDependency).getLogicalGrouping() != null) {
                            context.getContexts().forEach(child -> {
                                if(((Context)child).getScheduledJobsMap()
                                    .containsKey(((JobDependency) jobDependency).getJobIdentifier())) {
                                }
                            });
                        this.manageLogicalGroupings(context, ((JobDependency) jobDependency).getJobIdentifier(),
                            ((JobDependency) jobDependency).getLogicalGrouping(), diagramBuilder, graph);
                    }
                });
            }

            List<String> linkingConnections = this.manageLinkingContextTransitions(previousContexts, subsequentTransitions, diagramBuilder, graph);
            this.manageOutboundContextTransitions(subsequentTransitions, diagramBuilder, graph, linkingConnections);
            this.manageInboundContextTransitions(previousContexts, diagramBuilder, graph, linkingConnections);

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
            int depth = this.getGroupingDepth(grouping);
            compactTreeLayout.setIntraCellSpacing(120+(depth*20));
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

                        if(context.getScheduledJobsMap()
                            .containsKey(((PositionedItem) item).getId())) {
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
                        }
                        else if(((Image) item).getUserData().getItemType().equals(UserData.CONTEXT)) {
                            double labelLength = ((Image) item).getUserData().getContextName().length() * 8;

                            Label label = new LabelBuilder().withText(((Image) item).getUserData().getContextName())
                                .withX(positionedItemCentre - (labelLength / 2))
                                .withY(((PositionedItem) item).getY() + 110)
                                .withFontSize("14pt")
                                .withComposite(group.getId())
                                .build();

                            labels.add(label);
                        }

                        groups.add(group);
                    }
                }
            });

            this.addLogicGroupings(grouping, imageOverlay, cellMap, diagramBuilder);
            this.addContextBoundaries(items, imageOverlay, cellMap, diagramBuilder);
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
            .withPath("frontend/images/square_void.png")
            .withTopPort()
            .withBottomPort()
            .withUserData(new UserDataBuilder()
                .withContextName(context.getName())
                .withIdentifier(context.getName())
                .withItemType(UserData.CONTEXT)
                .build())
            .build();

        diagramBuilder.addItem(root);

        Map<String, Context> contextMap = new HashMap<>();

        contextMap.put(context.getName(), context);

        if(context.getContexts() != null) {
            context.getContexts().forEach(c -> {
                graph.addVertex(((Context) c).getName());
                graph.addEdge(context.getName(), ((Context) c).getName());

                contextMap.put(((Context) c).getName(), (Context) c);

                Image branch = diagramBuilder.getImageBuilder()
                    .withId(((Context) c).getName())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath("frontend/images/square_void.png")
                    .withTopPort()
                    .withBottomPort()
                    .withUserData(new UserDataBuilder()
                        .withContextName(((Context) c).getName())
                        .withIdentifier(((Context) c).getName())
                        .withItemType(UserData.CONTEXT)
                        .build())
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
        }

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
                    .withPath("frontend/images/square_void.png")
                    .withTopPort()
                    .withBottomPort()
                    .withUserData(new UserDataBuilder()
                        .withContextName(((Context) c).getName())
                        .withIdentifier(((Context) c).getName())
                        .withItemType(UserData.CONTEXT)
                        .build())
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

    protected void manageLogicalGroupings(Context context, String jobIdentifier, LogicalGrouping logicalGrouping, DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object, DefaultEdge> graph) {
        if(logicalGrouping.getLogicalGrouping() != null) {
            manageLogicalGroupings(context, jobIdentifier, logicalGrouping.getLogicalGrouping(), diagramBuilder, graph);
        }

        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            logicalGrouping.getAnd().forEach(and -> {
                if(and.getLogicalGrouping() != null) {
                    manageLogicalGroupings(context, jobIdentifier, and.getLogicalGrouping(), diagramBuilder, graph);
                }
                else {
                    context.getContexts().forEach(child -> {
                        if(((Context)child).getScheduledJobsMap().containsKey(and.getIdentifier())) {
                        }
                    });
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
                    manageLogicalGroupings(context, jobIdentifier, or.getLogicalGrouping(), diagramBuilder, graph);
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

    protected List<String> manageLinkingContextTransitions(List<ContextTransition> inboundContextTransitions, List<ContextTransition> outboundContextTransitions,
                                                           DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object, DefaultEdge> graph) {
        List<String> linkingConnections = new ArrayList<>();
        inboundContextTransitions.forEach(in -> {
            in.getContexts().forEach(inContext -> {
                outboundContextTransitions.forEach(out -> {
                    out.getContexts().forEach(outContext -> {
                        if(inContext.equals(outContext)) {
                            logger.info("Got linking connection!");
//                            linkingConnections.add(outContext);
//                            graph.addVertex(outContext);
//
//                            UserDataBuilder userDataBuilder = new UserDataBuilder()
//                                .withIdentifier(outContext)
//                                .withItemType(UserData.CONTEXT)
//                                .withContextName(outContext);
//
//                            //subsequentJobIdentifiers.forEach(id -> userDataBuilder.addSubsequentJobIdentifiers(id));
//
//                            ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
//                                .withId(outContext)
//                                .withHeight(100)
//                                .withWidth(100)
//                                .withPath("frontend/images/external-context.png")
//                                .withUserData(userDataBuilder.build())
//                                .withLeftPort()
//                                .withRightPort();
//
//                            diagramBuilder.addItem(jobBuilder.build());
//
//                            ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
//                            connectionBuilder.withSource(
//                                diagramBuilder.getConnectionDetailsBuilder()
//                                    .withNode(outContext)
//                                    .withPort("rightHybridSource")
//                                    .build()
//                            );
//                            connectionBuilder.withTarget(
//                                diagramBuilder.getConnectionDetailsBuilder()
//                                    .withNode(in.getPrecedingJob().getIdentifier())
//                                    .withPort("leftHybridTarget")
//                                    .withDecoration("draw2d.decoration.connection.ArrowDecorator")
//                                    .build()
//                            );
//
//                            diagramBuilder.addItem(connectionBuilder.build());
//
//                            graph.addEdge(outContext, in.getPrecedingJob().getIdentifier());
//
//                            connectionBuilder = diagramBuilder.getConnectionBuilder();
//                            connectionBuilder.withSource(
//                                diagramBuilder.getConnectionDetailsBuilder()
//                                    .withNode(out.getPrecedingJob().getIdentifier())
//                                    .withPort("rightHybridSource")
//                                    .build()
//                            );
//                            connectionBuilder.withTarget(
//                                diagramBuilder.getConnectionDetailsBuilder()
//                                    .withNode(outContext)
//                                    .withPort("leftHybridTarget")
//                                    .withDecoration("draw2d.decoration.connection.ArrowDecorator")
//                                    .build()
//                            );
//
//                            diagramBuilder.addItem(connectionBuilder.build());
//
//                            graph.addEdge(out.getPrecedingJob().getIdentifier(), outContext);
                        }
                    });
                });
            });
        });

        return linkingConnections;
    }

    protected void manageOutboundContextTransitions(List<ContextTransition> contextTransitions, DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object, DefaultEdge> graph,
                                                    List<String> linkingConnections) {
        List<String> addedContexts = new ArrayList<>();

        List<String> precedingJobIdentifiers = new ArrayList<>();
        contextTransitions.forEach(contextTransition
            -> precedingJobIdentifiers.add(contextTransition.getPrecedingJob().getIdentifier()));

        contextTransitions.forEach(contextTransition -> {
            contextTransition.getContexts().forEach(context -> {
                if(!linkingConnections.contains(context)) {
                    graph.addVertex(context + "_out");

                    if (!addedContexts.contains(context + "_out")) {
                        UserDataBuilder userDataBuilder = new UserDataBuilder()
                            .withIdentifier(context + "_out")
                            .withItemType(UserData.CONTEXT)
                            .withContextName(context);

                        precedingJobIdentifiers.forEach(id -> userDataBuilder.addPreviousJobIdentifiers(id));

                        ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                            .withId(context + "_out")
                            .withHeight(100)
                            .withWidth(100)
                            .withPath("frontend/images/external-context.png")
                            .withUserData(userDataBuilder.build())
                            .withLeftPort()
                            .withRightPort();

                        diagramBuilder.addItem(jobBuilder.build());
                        addedContexts.add(context + "_out");
                    }

                    ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
                    connectionBuilder.withSource(
                        diagramBuilder.getConnectionDetailsBuilder()
                            .withNode(contextTransition.getPrecedingJob().getIdentifier())
                            .withPort("rightHybridSource")
                            .build()
                    );

                    connectionBuilder.withTarget(
                        diagramBuilder.getConnectionDetailsBuilder()
                            .withNode(context + "_out")
                            .withPort("leftHybridTarget")
                            .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                            .build()
                    );

                    diagramBuilder.addItem(connectionBuilder.build());

                    graph.addEdge(contextTransition.getPrecedingJob().getIdentifier(), context + "_out");
                }
            });
        });
    }

    protected void manageInboundContextTransitions(List<ContextTransition> contextTransitions, DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object
        , DefaultEdge> graph, List<String> linkingConnections) {
        List<String> addedContexts = new ArrayList<>();
        List<String> subsequentJobIdentifiers = new ArrayList<>();
        contextTransitions.forEach(contextTransition
            -> subsequentJobIdentifiers.add(contextTransition.getPrecedingJob().getIdentifier()));

        contextTransitions.forEach(contextTransition -> {
            contextTransition.getContexts().forEach(context -> {
                if(!linkingConnections.contains(context)) {
                    if (!addedContexts.contains(context + "_in")) {
                        graph.addVertex(context + "_in");

                        UserDataBuilder userDataBuilder = new UserDataBuilder()
                            .withIdentifier(context + "_in")
                            .withItemType(UserData.CONTEXT)
                            .withContextName(context);

                        subsequentJobIdentifiers.forEach(id -> userDataBuilder.addSubsequentJobIdentifiers(id));

                        ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                            .withId(context + "_in")
                            .withHeight(100)
                            .withWidth(100)
                            .withPath("frontend/images/external-context.png")
                            .withUserData(userDataBuilder.build())
                            .withLeftPort()
                            .withRightPort();

                        diagramBuilder.addItem(jobBuilder.build());
                    }

                    ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
                    connectionBuilder.withSource(
                        diagramBuilder.getConnectionDetailsBuilder()
                            .withNode(context + "_in")
                            .withPort("rightHybridSource")
                            .build()
                    );
                    connectionBuilder.withTarget(
                        diagramBuilder.getConnectionDetailsBuilder()
                            .withNode(contextTransition.getPrecedingJob().getIdentifier())
                            .withPort("leftHybridTarget")
                            .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                            .build()
                    );

                    diagramBuilder.addItem(connectionBuilder.build());

                    graph.addEdge(context + "_in", contextTransition.getPrecedingJob().getIdentifier());

                    addedContexts.add(context);
                }
            });
        });
    }

    private Tree<String> getLinkedContexts(Context context, Context parentContext, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        LinkedList<String> path = new LinkedList<>();
        if(!context.getContexts().isEmpty()) {
            logger.info("Got some child contexts");
            logger.info("******************* " + context.getName() + " ********************\n");
            List<String> previousContexts
                = this.getPreviousContextTransitions(parentContext, context, internalEventDrivenJobMap)
                    .stream()
                    .flatMap(contextTransition -> contextTransition.getContexts().stream())
                    .collect(Collectors.toList());

            previousContexts.forEach(ct -> logger.info("Previous Context transition - " + ct));

            List<String> subsequentTransitions = ContextHelper.determineIfJobsTransitionToOtherContexts
                (parentContext, context.getScheduledJobsMap(), context, internalEventDrivenJobMap)
                .stream()
                .flatMap(contextTransition -> contextTransition.getContexts().stream())
                .collect(Collectors.toList());

            subsequentTransitions.forEach(ct -> logger.info("Subsequent Context transition - " + ct));
            context.getContexts().forEach(child -> {
                logger.info("######################## " + ((Context)child).getName() + " ########################\n");
                List<ContextTransition> previousContexts2 = this.getPreviousContextTransitions(parentContext, (Context)child, internalEventDrivenJobMap);

                previousContexts2.forEach(ct -> logger.info("Previous Context transition - " + ct));

                List<ContextTransition> subsequentTransitions2 = ContextHelper.determineIfJobsTransitionToOtherContexts
                    (parentContext, ((Context)child).getScheduledJobsMap(), (Context)child, internalEventDrivenJobMap);

                subsequentTransitions2.forEach(ct -> logger.info("Subsequent Context transition - " + ct));

                logger.info("######################## END " + ((Context)child).getName() + " ########################\n");
            });

            logger.info("******************* END" + context.getName() + " ********************\n");
        }

        return new Tree<>(new TreeNode<>(""));
    }

    protected List<ContextTransition> getPreviousContextTransitions(Context parentContext, Context context, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        HashSet<ContextTransition> previousContexts = new HashSet<>();

        context.getScheduledJobs().forEach(job -> {
            List<ContextTransition>  jobsInto = ContextHelper.determineIfJobsTransitionFromOtherContexts(parentContext, ((SchedulerJob)job).getJobName(),
                context.getName(), internalEventDrivenJobMap);

            jobsInto.forEach(jobsOtherContexts -> previousContexts.add(jobsOtherContexts));
        });

        return previousContexts.stream().filter(contextTransition -> !contextTransition.getContexts().isEmpty()).collect(Collectors.toList());
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

            if(nestedGrouping.getJobIdentifiers().size() > 1 || (!nestedGrouping.getNestedGrouping().isEmpty())) {
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
                    .withWidth(xMaxExtent.get() - xMinExtent.get() + 360)
                    .withHeight(yMaxExtent.get() - yMinExtent.get() + 160)
                    .withStroke(3)
                    .withRadius(10)
                    .withX(xMinExtent.get() - 130)
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

    protected void addContextBoundaries(ArrayList<Object> items, ArrayList<Object> imageOverlay, Map<String, mxCell> cellMap, DiagramBuilder diagramBuilder) {
        items.forEach(item -> {

            if(item instanceof Image) {
                Image image = (Image) item;

                if (image.getUserData() != null
                    && image.getUserData().getItemType() != null
                    && image.getUserData().getItemType().equals(UserData.CONTEXT)) {
                    mxCell cell = cellMap.get(image.getUserData().getIdentifier());

                    RectangleBuilder rb = diagramBuilder.getRectangleBuilder()
                        .withWidth(200)
                        .withHeight(200)
                        .withStroke(3)
                        .withRadius(10)
                        .withX(cell.getGeometry().getX() + 550)
                        .withY(cell.getGeometry().getY() + 550)
                        .withSelectable(false)
                        .withDraggable(false)
                        .withResizable(false)
                        .withDasharray("--")
                        .withBgColor(IkasanColours.LIGHT_GREY)
                        .withColor(IkasanColours.BLACK);

                    imageOverlay.add(rb.build());
                }
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

        if(schedulerJob instanceof FileEventDrivenJob || schedulerJob instanceof FileEventDrivenJobInstance) {
            image = "frontend/images/file_black.png";
        }
        else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
            image = "frontend/images/time_black.png";
        }

        return image;
    }

    protected List<SchedulerJob> getSchedulerJobsFromGrouping(Context context, Grouping grouping) {
        ArrayList<SchedulerJob> schedulerJobs = new ArrayList<>();

        _getSchedulerJobsFromGrouping(context, grouping, schedulerJobs);

        return schedulerJobs;
    }

    private void _getSchedulerJobsFromGrouping(Context context, Grouping grouping, List<SchedulerJob> schedulerJobs) {

        grouping.getJobIdentifiers().forEach(id -> {
            if(!schedulerJobs.contains(context.getScheduledJobsMap().get(id))) {
                schedulerJobs.add((SchedulerJob) context.getScheduledJobsMap().get(id));
            }
        });

        if(grouping.getNestedGrouping() != null) {
            grouping.getNestedGrouping().forEach(nested -> {
                _getSchedulerJobsFromGrouping(context, nested, schedulerJobs);
            });
        }
    }

    protected int getGroupingDepth(Grouping grouping) {
        AtomicInteger depth = new AtomicInteger(0);
        _getGroupingDepth(grouping, depth);

        return depth.get();
    }

    private void _getGroupingDepth(Grouping grouping, AtomicInteger depth) {
        boolean depthIncrement = false;

        for (Grouping nested : grouping.getNestedGrouping()) {
            if(!depthIncrement) {
                depth.getAndIncrement();
                depthIncrement = true;
            }
            _getGroupingDepth(nested, depth);
        }
    }
}
