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
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.*;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class Draw2dAdapterBase {
    Logger logger = LoggerFactory.getLogger(Draw2dAdapterBase.class);

    protected ObjectMapper mapper = new ObjectMapper();
    protected DiagramBuilder diagramBuilder = new DiagramBuilder();
    protected double jobMaxXExtent = 0;
    protected double jobMaxYExtent = 0;

    protected double jobVisualisationVerticalSpacing = 120;
    protected double jobVisualisationHorizontalSpacing = 400;

    protected double contextVisualisationLevelDistance = 200;
    protected double contextVisualisationNodeDistance = 75;
    protected int fontSize = 14;

    public static final String CONNECTOR_BOTTOM_HYBRID_SOURCE = "bottomHybridSource";
    public static final String CONNECTOR_TOP_HYBRID_TARGET = "topHybridTarget";
    public static final String CONNECTOR_RIGHT_HYBRID_SOURCE = "rightHybridSource";
    public static final String CONNECTOR_LEFT_HYBRID_TARGET = "leftHybridTarget";

    /**
     * Constructs a new Draw2dAdapterBase object.
     * This class is a base class for Draw2D adapters and provides utility methods for drawing objects.
     */
    public Draw2dAdapterBase() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * This method is the constructor for the Draw2dAdapterBase class.
     *
     * @param jobVisualisationVerticalSpacing    the vertical spacing to be used for job visualisation
     * @param jobVisualisationHorizontalSpacing  the horizontal spacing to be used for job visualisation
     * @param contextVisualisationLevelDistance  the distance between levels in the context visualisation
     * @param contextVisualisationNodeDistance   the distance between nodes in the context visualisation
     */
    public Draw2dAdapterBase(double jobVisualisationVerticalSpacing,
                             double jobVisualisationHorizontalSpacing,
                             double contextVisualisationLevelDistance,
                             double contextVisualisationNodeDistance) {
       this();
       this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
       this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
       this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
       this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;
    }


    /**
     * Adapts jobs from the provided context to prepare them for visual representation and job scheduling.
     *
     * @param parentContext The parent context containing overarching scheduling information.
     * @param context The current context containing job details to be adapted.
     * @param schedulerJobs A mapping of job names to their corresponding {@code SchedulerJob} instances.
     * @param schedulerJobsMap A mapping of job identifiers to {@code SchedulerJob} instances for cross-reference within the context.
     * @param schedulerJobsImageMap A mapping of job identifiers to {@code Image} objects for visual representation.
     * @param logicalBoundaries A mapping of visual representation identifiers to their {@code Rectangle} boundaries.
     * @return An {@code ArrayList} containing adapted job objects prepared for further processing or visualization.
     */
    protected ArrayList<Object> _adaptJobs(Context parentContext, Context context, Map<String, SchedulerJob> schedulerJobs,
                                           Map<String, SchedulerJob> schedulerJobsMap, Map<String, Image> schedulerJobsImageMap,
                                           Map<String, Rectangle> logicalBoundaries) {
        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {
            this.setDiagramVisualisationLayoutConfiguration(parentContext);
            // Determine if any jobs are initiated from a previous or are responsible for initiating a job in a
            // subsequent flow.
            List<ContextTransition> previousContexts = this.getPreviousContextTransitions(parentContext, context, schedulerJobsMap);
            List<ContextTransition> subsequentTransitions = ContextHelper.determineIfJobsTransitionToOtherContexts
                (parentContext, context.getScheduledJobsMap(), context, schedulerJobsMap);

            // We create the draw2d items in parallel to creating
            // an instance of a DefaultDirectedGraph. The DefaultDirectedGraph
            // is used to provide us with the layout of the visualisation
            // that is constructed.
            DefaultDirectedGraph<Object, DefaultEdge> graph
                = new DefaultDirectedGraph<>(NoEdgeLabel.class);

            // We now inspect all job dependencies and create an instance
            // of a convenience class VisualisationLogicalGrouping which is used to
            // render the boundaries to represent the logic in the
            // job plan.
            VisualisationLogicalGrouping visualisationLogicalGrouping = new VisualisationLogicalGrouping();

            if(context.getJobDependencies() != null) {
                context.getJobDependencies().forEach(jobDependency -> {
                    if (((JobDependency) jobDependency).getLogicalGrouping() != null) {
                        this.getAllJobsInGrouping(((JobDependency) jobDependency).getLogicalGrouping(),
                            visualisationLogicalGrouping);
                    }
                });
            }

            // Get a handle to all the jobs
            List<SchedulerJob> jobs = this.getSchedulerJobsFromGrouping(context, visualisationLogicalGrouping);

            context.getScheduledJobs().forEach(job -> {
                if(!jobs.contains(job)) {
                    jobs.add((SchedulerJob) job);
                }
            });

            List<SchedulerJob> jobsToFilter = new ArrayList<>();
            Map<String, String> contextTerminalJobs = new HashMap<>();
            previousContexts.forEach(contextTransition -> {
                // we don't want to represent previous jobs that are terminal jobs in our diagrams
                if(contextTransition.getPrecedingJob().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                    jobsToFilter.add(contextTransition.getPrecedingJob());
                    contextTransition.getContexts().forEach(c -> {
                        contextTerminalJobs.put(c, contextTransition.getPrecedingJob().getJobName());
                    });
                }
            });

            // remove any filtered jobs if there are any.
            List<SchedulerJob> finalJobs = jobs.stream().filter(job -> {
                if(job == null) return false;
                AtomicBoolean keep = new AtomicBoolean(true);
                jobsToFilter.forEach(terminal -> {
                    if(terminal.getIdentifier().equals(job.getIdentifier())) {
                        keep.set(false);
                    }
                });
                return keep.get();
            }).collect(Collectors.toList());

            // Create an item for each jobs as well as adding each job to the
            // DefaultDirectedGraph. We delegate to some builder classes that
            // create all the relevant items to be rendered in draw2d.
            finalJobs.forEach(job -> {
                graph.addVertex(job.getIdentifier());

                SchedulerJob schedulerJob = schedulerJobs.get(job.getJobName());
                String image = getJobImage(schedulerJob);

                // User data is an important concept as it allows us
                // to attach any data that we like to an item in a draw2d
                // model. When an item is selected from the draw2d palette
                // we receive the user data and can use that for contextual
                // purposes.
                UserDataBuilder userDataBuilder = new UserDataBuilder()
                    .withAgentName(job.getAgentName())
                    .withJobName(job.getJobName())
                    .withIdentifier(job.getIdentifier());

                int imageHeight = 100;
                int imageWidth = 100;
                boolean userDataSet = false;
                if(schedulerJob instanceof InternalEventDrivenJob || schedulerJob instanceof InternalEventDrivenJobInstance) {
                    userDataBuilder.withItemType(UserData.INTERNAL_EVENT_DRIVEN_JOB);
                    userDataSet = true;
                }
                else if(schedulerJob instanceof FileEventDrivenJob || schedulerJob instanceof FileEventDrivenJobInstance) {
                    userDataBuilder.withItemType(UserData.FILE_EVENT_DRIVEN_JOB);
                    userDataSet = true;
                }
                else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
                    userDataBuilder.withItemType(UserData.QUARTZ_EVENT_DRIVEN_JOB);
                    userDataSet = true;
                }
                else if(schedulerJob instanceof GlobalEventJob || schedulerJob instanceof GlobalEventJobInstance) {
                    userDataBuilder.withItemType(UserData.GLOBAL_EVENT_DRIVEN_JOB);
                    userDataSet = true;
                }
                else if(schedulerJob instanceof ContextStartJob || schedulerJob instanceof ContextStartJobInstance
                    || schedulerJob.getAgentName().equals(UserData.CONTEXT_START_JOB)) {
                    userDataBuilder.withItemType(UserData.CONTEXT_START_JOB);
                    userDataSet = true;
                    imageHeight = 50;
                    imageWidth = 50;
                }
                else if(schedulerJob instanceof ContextTerminalJob || schedulerJob instanceof ContextTerminalJobInstance
                    || schedulerJob.getAgentName().equals(UserData.CONTEXT_TERMINAL_JOB)) {
                    userDataBuilder.withItemType(UserData.CONTEXT_TERMINAL_JOB);
                    userDataSet = true;
                    imageHeight = 50;
                    imageWidth = 50;
                }
                else if(schedulerJob instanceof LocalEventJob || schedulerJob instanceof LocalEventJobInstance
                    || schedulerJob.getAgentName().equals(UserData.LOCAL_EVENT_JOB)) {
                    userDataBuilder.withItemType(UserData.LOCAL_EVENT_JOB);
                    userDataSet = true;
                }
                else if(schedulerJob instanceof BridgingJob || schedulerJob instanceof BridgingJobInstance
                    || schedulerJob.getAgentName().equals(UserData.BRIDGING_JOB)) {
                    userDataBuilder.withItemType(UserData.BRIDGING_JOB);
                    userDataSet = true;
                }

                if(userDataSet) {
                    ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                        .withId(job.getIdentifier())
                        .withHeight(imageHeight)
                        .withWidth(imageWidth)
                        .withPath(image)
                        .withUserData(userDataBuilder.build());

                    if (schedulerJob instanceof InternalEventDrivenJob || schedulerJob instanceof InternalEventDrivenJobInstance
                        || schedulerJob instanceof GlobalEventJob || schedulerJob instanceof GlobalEventJobInstance
                        || schedulerJob instanceof ContextStartJob || schedulerJob instanceof ContextStartJobInstance
                        || schedulerJob instanceof ContextTerminalJob || schedulerJob instanceof ContextTerminalJobInstance
                        || schedulerJob instanceof LocalEventJob || schedulerJob instanceof LocalEventJobInstance
                        || schedulerJob instanceof BridgingJob || schedulerJob instanceof BridgingJobInstance) {
                        jobBuilder.withLeftPort()
                            .withRightPort();
                    } else if (schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
                        jobBuilder.withRightPort();
                    }

                    diagramBuilder.addItem(jobBuilder
                        .build());
                }
            });

            // Work out way through the job dependencies and add connections between jobs.
            // This is managed recursively as
            if(context.getJobDependencies() != null) {
                context.getJobDependencies().forEach(jobDependency -> {
                    if (((JobDependency) jobDependency).getLogicalGrouping() != null) {
                        this.manageLogicalGroupings(context, ((JobDependency) jobDependency).getJobIdentifier(),
                            ((JobDependency) jobDependency).getLogicalGrouping(), diagramBuilder, graph);
                    }
                });
            }

            // Now delegate to a helper method to deal with the case that jobs within a job plan
            // may be linked by sub contexts.
            List<String> linkingConnections = this.manageLinkingContextTransitions(context, parentContext
                , schedulerJobsMap, diagramBuilder, graph);

            // Manage the case that there are other contexts that the job plan links to.
            this.manageOutboundContextTransitions(subsequentTransitions, diagramBuilder, graph
                , linkingConnections, parentContext);

            // Manage the case that there are other contexts that precede this one and link to it.
            List<String> inboundConnections =  this.manageInboundContextTransitions(previousContexts, diagramBuilder, graph
                , linkingConnections, schedulerJobsMap);

            // Now delegate to the JGraphXAdapter to create the layout
            // of the visualisation.
            JGraphXAdapter<Object, DefaultEdge> jGraphXAdapter
                = new JGraphXAdapter<>(graph);

            this.getCellMap(jGraphXAdapter)
                .entrySet()
                .forEach(entry -> {
                    mxCell cell = entry.getValue();
                    if(cell.isVertex()) {
                        if(schedulerJobsMap.containsKey(cell.getValue()) &&
                            (schedulerJobsMap.get(cell.getValue()).getAgentName().equals(JobConstants.CONTEXT_START_JOB) ||
                                schedulerJobsMap.get(cell.getValue()).getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB))) {
                            cell.getGeometry().setWidth(50);
                            cell.getGeometry().setHeight(50);
                        }
                        else {
                            cell.getGeometry().setWidth(100);
                            cell.getGeometry().setHeight(100);
                        }
                    }
                });

            mxHierarchicalLayout compactTreeLayout = new mxHierarchicalLayout(jGraphXAdapter);
            compactTreeLayout.setOrientation(SwingConstants.WEST);
            int depth = this.getGroupingDepth(visualisationLogicalGrouping);
            compactTreeLayout.setIntraCellSpacing(this.jobVisualisationVerticalSpacing+(depth*20));
            compactTreeLayout.setInterRankCellSpacing(this.jobVisualisationHorizontalSpacing);

            compactTreeLayout.execute(jGraphXAdapter.getDefaultParent());

            // We get a handle to the cell map from JGraphXAdapter. Each
            // cell is keyed on the relevant item identifier and contains
            // the relevant layout coordinates.
            Map<String, mxCell> cellMap = this.getCellMap(jGraphXAdapter);

            // Get all items from the underlying diagram builder.
            ArrayList<Object> items = diagramBuilder.build();


            // Now go ahead and group items and add labels to the diagram.
            ArrayList<Object> labels = new ArrayList<>();
            ArrayList<Object> groups = new ArrayList<>();

            ArrayList<Object> imageOverlay = new ArrayList<>();

            items.forEach(item -> {
                if (item instanceof Image) {
                    mxCell cell = cellMap.get(((Item) item).getId());

                    if (cell != null) {
                        GroupBuilder groupBuilder = new GroupBuilder();
                        Group group = groupBuilder.build();

                        if(schedulerJobsImageMap.containsKey(((Item) item).getId())) {
                            Image image = schedulerJobsImageMap.get(((Item) item).getId());

                            ((PositionedItem) item).setX(image.getX());
                            ((PositionedItem) item).setY(image.getY());
                        }
                        else {
                            ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                            ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);
                        }

                        double positionedItemCentre = ((PositionedItem) item).getX() + 50;

                        if(context.getScheduledJobsMap()
                            .containsKey(((PositionedItem) item).getId())) {
                            ((Image) item).setComposite(group.getId());
                            // assuming each letter is 8 units long
                            if(!((Image) item).getUserData().getItemType().equals(UserData.CONTEXT_START_JOB) &&
                                !((Image) item).getUserData().getItemType().equals(UserData.CONTEXT_TERMINAL_JOB) &&
                                !((Image) item).getUserData().getItemType().equals(UserData.BRIDGING_JOB)) {
                                double labelLength = ((SchedulerJob) context.getScheduledJobsMap()
                                    .get(((PositionedItem) item).getId())).getJobName().length() * this.fontSize * 0.65;

                                Label label = new LabelBuilder().withText(((SchedulerJob) context.getScheduledJobsMap()
                                        .get(((PositionedItem) item).getId())).getJobName())
                                    .withX(positionedItemCentre - (labelLength / 2))
                                    .withY(((PositionedItem) item).getY() + 110)
                                    .withFontSize(this.fontSize+"pt")
                                    .withComposite(group.getId())
                                    .build();

                                group.setUserData(((Image) item).getUserData());
                                label.setUserData(((Image) item).getUserData());

                                labels.add(label);
                            }

                            if(inboundConnections.contains(((PositionedItem) item).getId())){
                                if(!((Image) item).getUserData().getItemType().equals(UserData.CONTEXT_START_JOB) &&
                                    !((Image) item).getUserData().getItemType().equals(UserData.CONTEXT_TERMINAL_JOB) &&
                                    !((Image) item).getUserData().getItemType().equals(UserData.LOCAL_EVENT_JOB)) {
                                    Circle eventCircle = new CircleBuilder()
                                        .withColor(IkasanColours.BLACK)
                                        .withBgColor(IkasanColours.SCHEDULER_EVENT_YELLOW)
                                        .withWidth(200)
                                        .withHeight(200)
                                        .withX(((PositionedItem) item).getX() - 50)
                                        .withY(((PositionedItem) item).getY() - 50)
                                        .withComposite(group.getId())
                                        .build();

                                    imageOverlay.add(eventCircle);
                                }
                            }

                            if(schedulerJobsMap.containsKey(((PositionedItem) item).getId())) {
                                SchedulerJob internalEventDrivenJob = schedulerJobsMap.get(((PositionedItem) item).getId());

                                if(internalEventDrivenJob instanceof InternalEventDrivenJob && ((InternalEventDrivenJob) internalEventDrivenJob).isJobRepeatable()) {
                                    ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                                        .withId(UUID.randomUUID().toString())
                                        .withWidth(30)
                                        .withHeight(30)
                                        .withX(((PositionedItem) item).getX() + 35)
                                        .withY(((PositionedItem) item).getY() - 50)
                                        .withPath("frontend/images/repeating.png")
                                        .withUserData(new UserDataBuilder().withItemType(UserData.REPEATABLE).build())
                                        .withComposite(group.getId());

                                    imageOverlay.add(jobBuilder.build());
                                }
                            }

                        }
                        else if(((Image) item).getUserData().getItemType().equals(UserData.CONTEXT)) {
                            double labelLength = ((Image) item).getUserData().getContextName().length() * this.fontSize * 0.65;

                            ((Image) item).setComposite(group.getId());
                            if(contextTerminalJobs.containsKey(((Image) item).getUserData().getContextName())) {
                                ArrayList<String> previousIdentifiers = new ArrayList<>();
                                previousIdentifiers.add(contextTerminalJobs.get(((Image) item).getUserData().getContextName()));
                                ((Image) item).getUserData().setPreviousJobIdentifiers(previousIdentifiers);
                            }
                            Label label = new LabelBuilder().withText(((Image) item).getUserData().getContextName())
                                .withX(positionedItemCentre - (labelLength / 2))
                                .withY(((PositionedItem) item).getY() + 110)
                                .withFontSize(this.fontSize+"pt")
                                .withComposite(group.getId())
                                .build();

                            group.setUserData(((Image) item).getUserData());
                            label.setUserData(((Image) item).getUserData());

                            labels.add(label);
                        }

                        groups.add(group);
                    }
                }
            });

            if(parentContext.isRenderLogicalBoundaries() == null || parentContext.isRenderLogicalBoundaries()) {
                // Now draw the logic groupings abd context boundaries onto the diagram.
                if(logicalBoundaries != null) {
                    imageOverlay.addAll(logicalBoundaries.values());
                }
                else {
                    this.addLogicGroupings(visualisationLogicalGrouping, imageOverlay, cellMap, diagramBuilder, schedulerJobsImageMap);
                }
            }

            if(parentContext.isRenderOrLogicalBoundariesOnly()) {
                imageOverlay.forEach(image -> {
                    // We are only going to add the OR image overlay boundaries to the
                    // diagram when renderOrLogicalBoundariesOnly is set to true.
                    if(image instanceof Rectangle && ((Rectangle)image).getId().startsWith("OR")) {
                        items.add(image);
                    }
                });
            }
            else {
                items.addAll(imageOverlay);
            }
            items.addAll(labels);
            items.addAll(groups);

            List<Object> finalItems = new ArrayList<>();
            Map<String, Image> contextMap = new HashMap<>();
            items.forEach(item -> {
                if(item instanceof Image) {
                    if(((Image)item).getUserData() != null &&
                        ((Image)item).getUserData().getItemType()!= null &&
                        ((Image)item).getUserData().getItemType().equals(UserData.CONTEXT)) {
                        if(!contextMap.containsKey(((Image)item).getUserData().getContextName())) {
                            contextMap.put(((Image)item).getUserData().getContextName(), ((Image)item));
                        }
                        else {
                            contextMap.get(((Image)item).getUserData().getContextName()).getUserData().getPreviousJobIdentifiers()
                                .addAll(((Image)item).getUserData().getPreviousJobIdentifiers());
                            contextMap.get(((Image)item).getUserData().getContextName()).getUserData().getSubsequentJobIdentifiers()
                                .addAll(((Image)item).getUserData().getSubsequentJobIdentifiers());
                        }
                    }
                }
                else {
                    finalItems.add(item);
                }
            });

            finalItems.addAll(contextMap.values());

            return items;
        }

        return null;
    }


    /**
     * Adapts the given context into a visual representation by building a structured graph,
     * diagram, and associated metadata. This method processes the main context and its nested
     * sub-contexts recursively to generate corresponding graphical elements and layout information.
     *
     * @param context The root context to adapt into a structured representation. It contains
     *                sub-contexts, identifiers, and metadata utilized for generating the graph
     *                and diagram layout.
     * @return A list of objects representing the items, groups, and labels derived from the
     *         adapted context. This list includes both visual items (such as images and
     *         rectangles) and associated metadata elements (such as labels and group definitions).
     */
    protected ArrayList<Object> _adaptContext(Context context) {
        this.setDiagramVisualisationLayoutConfiguration(context);
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        DiagramBuilder diagramBuilder = new DiagramBuilder();

        graph.addVertex(context.getName());

        Image root = diagramBuilder.getImageBuilder()
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

        diagramBuilder.addItem(root);

        Map<String, Context> contextMap = new HashMap<>();
        contextMap.put(context.getName(), context);

        // We simply recursively work our way through the context
        // and all nested contexts and render them into the diagram.
        if(context.getContexts() != null) {
            context.getContexts().sort((a, b) -> ((Context)a).getOrdinal() < ((Context)b).getOrdinal() ? -1 : 1);
            context.getContexts().forEach(c -> {
                graph.addVertex(((Context) c).getName());
                graph.addEdge(context.getName(), ((Context) c).getName());

                contextMap.put(((Context) c).getName(), (Context) c);

                Image branch = diagramBuilder.getImageBuilder()
                    .withId(((Context) c).getName())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath("frontend/images/context-icon.png")
                    .withTopPort()
                    .withBottomPort()
                    .withUserData(new UserDataBuilder()
                        .withContextName(((Context) c).getName())
                        .withIdentifier(((Context) c).getName())
                        .withItemType(UserData.CONTEXT)
                        .build())
                    .build();

                diagramBuilder.addItem(branch);

                this.addConnection(root.getId(), CONNECTOR_BOTTOM_HYBRID_SOURCE, branch.getId()
                    , CONNECTOR_TOP_HYBRID_TARGET, diagramBuilder);

                this.manageContext((Context) c, graph, diagramBuilder, contextMap);
            });
        }

        // Now delegate to the JGraphXAdapter to create the layout
        // of the visualisation.
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
        compactTreeLayout.setLevelDistance((int)this.contextVisualisationLevelDistance);
        compactTreeLayout.setEdgeRouting(true);
        compactTreeLayout.setNodeDistance((int)this.contextVisualisationNodeDistance);

        compactTreeLayout.execute(jGraphXAdapter.getDefaultParent());

        // We get a handle to the cell map from JGraphXAdapter. Each
        // cell is keyed on the relevant item identifier and contains
        // the relevant layout coordinates.
        Map<String, mxCell> cellMap = this.getCellMap(jGraphXAdapter);

        // Get all items from the underlying diagram builder.
        ArrayList<Object> items = diagramBuilder.build();

        // Now go ahead and group items and add labels to the diagram.
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


                    double labelLength = ((Image) item).getUserData().getContextName().length() * this.fontSize * 0.65;

                    Label label = new LabelBuilder().withText(((PositionedItem)item).getId())
                        .withX(positionedItemCentre - (labelLength / 2))
                        .withY(((PositionedItem)item).getY() + 110)
                        .withFontSize(this.fontSize + "pt")
                        .withComposite(group.getId())
                        .build();

                    labels.add(label);
                    groups.add(group);
                }
            }
        });

        items.addAll(groups);
        items.addAll(labels);

        return items;
    }


    /**
     * Manages a given context by recursively processing its child contexts, updating the graph structure,
     * generating diagram items, and storing context information in a map. This method processes the provided
     * context and its hierarchy to build a directed graph and populate a diagram representation.
     *
     * @param context            The root context to be managed. This context may have child contexts that are
     *                           retrieved and processed recursively.
     * @param graph              A directed graph representing the relationship between contexts. Vertices and
     *                           edges are added to this graph to reflect the structure of the hierarchy.
     * @param diagramBuilder     The builder object used to construct and populate the diagram representation.
     *                           Diagram items are created and added to the builder based on the context structure.
     * @param contextInstanceMap A map that stores the relationship between context names and their corresponding
     *                           context instances. This map is updated with all processed contexts for later reference.
     */
    protected void manageContext(Context context, DefaultDirectedGraph<Object, DefaultEdge> graph,
                                 DiagramBuilder diagramBuilder, Map<String, Context> contextInstanceMap) {

        if(context.getContexts() != null && !context.getContexts().isEmpty()) {
            context.getContexts().sort((a, b) -> ((Context)a).getOrdinal() < ((Context)b).getOrdinal() ? -1 : 1);
            context.getContexts().forEach(c -> {
                contextInstanceMap.put(((Context)c).getName(), (Context)c);
                graph.addVertex(((Context)c).getName());
                graph.addEdge(context.getName(), ((Context)c).getName());

                Image branch = diagramBuilder.getImageBuilder()
                    .withId(((Context)c).getName())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath("frontend/images/context-icon.png")
                    .withTopPort()
                    .withBottomPort()
                    .withUserData(new UserDataBuilder()
                        .withContextName(((Context) c).getName())
                        .withIdentifier(((Context) c).getName())
                        .withItemType(UserData.CONTEXT)
                        .build())
                    .build();

                diagramBuilder.addItem(branch);

                this.addConnection(context.getName(), CONNECTOR_BOTTOM_HYBRID_SOURCE, branch.getId()
                    , CONNECTOR_TOP_HYBRID_TARGET, diagramBuilder);

                this.manageContext((Context)c, graph, diagramBuilder, contextInstanceMap);
            });
        }
    }


    /**
     * Retrieves a map of vertex cells from the given JGraphXAdapter.
     * The key in the returned map corresponds to the string representation
     * of the cell value and the value is the associated mxCell object.
     * Only vertex cells are included in the map.
     *
     * @param jGraphXAdapter the JGraphXAdapter instance containing the graph structure
     * @return a map containing vertex cells with their values as keys
     */
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


    /**
     * Recursively processes the logical grouping structure and manages the connections between job identifiers
     * in the directed graph, based on the relationships defined within the logical grouping object.
     *
     * @param context         The context object holding information about the scheduled jobs and other relevant data.
     * @param jobIdentifier   The identifier of the job currently being processed.
     * @param logicalGrouping The logical grouping object that defines the hierarchical grouping and connections.
     * @param diagramBuilder  The diagram builder used to create and manage the visual representation of the connections.
     * @param graph           The directed graph structure representing logical connections between job identifiers.
     */
    protected void manageLogicalGroupings(Context context, String jobIdentifier, LogicalGrouping logicalGrouping
        , DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object, DefaultEdge> graph) {
        if(logicalGrouping.getLogicalGrouping() != null) {
            manageLogicalGroupings(context, jobIdentifier, logicalGrouping.getLogicalGrouping(), diagramBuilder, graph);
        }

        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            logicalGrouping.getAnd().forEach(and -> {
                if(and.getLogicalGrouping() != null) {
                    manageLogicalGroupings(context, jobIdentifier, and.getLogicalGrouping(), diagramBuilder, graph);
                }
                else if(and.getIdentifier() != null
                    && context.getScheduledJobsMap().containsKey(and.getIdentifier())
                    && context.getScheduledJobsMap().containsKey(jobIdentifier)
                    && (!((SchedulerJob)context.getScheduledJobsMap().get(jobIdentifier))
                        .getAgentName().equals(JobConstants.CONTEXT_START_JOB))
                ) {
                    if(graph.containsVertex(and.getIdentifier()) && graph.containsVertex(jobIdentifier)) {
                        graph.addEdge(and.getIdentifier(), jobIdentifier);
                        this.addConnection(and.getIdentifier(), CONNECTOR_RIGHT_HYBRID_SOURCE
                            , jobIdentifier, CONNECTOR_LEFT_HYBRID_TARGET, diagramBuilder);
                    }
                }
            });
        }

        if(logicalGrouping.getOr() != null && !logicalGrouping.getOr().isEmpty()) {
            logicalGrouping.getOr().forEach(or -> {
                if(or.getLogicalGrouping() != null) {
                    manageLogicalGroupings(context, jobIdentifier, or.getLogicalGrouping(), diagramBuilder, graph);
                }
                else if (or.getIdentifier() != null){
                    graph.addEdge(or.getIdentifier(), jobIdentifier);

                    this.addConnection(or.getIdentifier(), CONNECTOR_RIGHT_HYBRID_SOURCE
                        , jobIdentifier, CONNECTOR_LEFT_HYBRID_TARGET, diagramBuilder);
                }
            });
        }
    }

    /**
     * This method is responsible for managing any linking contexts within a job plan. Ultimately it delegates to
     * the method getLinkedContexts, which builds a org.ikasan.dashboard.ui.visualisation.scheduler.model.Tree
     * which contains nodes of identifiers of in a tree structure with the root being the start of the linking
     * and nested branches containing the linkage path.
     * Once the tree has been constructed, this method then delegates to addLinkingsToDiagram which recursively
     * adds all linking context transitions to the diagram.
     *
     * @param context the current context containing information about transitions and states
     * @param parentContext the parent context of the current context, used for hierarchical linking
     * @param internalEventDrivenJobMap a map of job identifiers to event-driven scheduler jobs
     * @param diagramBuilder an object responsible for constructing diagrams based on the context and relationships
     * @param graph a directed graph representing relationships and dependencies between objects
     * @return a list of strings representing created linking connections
     */
    protected List<String> manageLinkingContextTransitions(Context context, Context parentContext, Map<String, SchedulerJob> internalEventDrivenJobMap,
                                                           DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object, DefaultEdge> graph) {
        List<String> linkingConnections = new ArrayList<>();

        if(!context.getContexts().isEmpty()) {
            Tree<String> tree = getLinkedContexts(context, parentContext, internalEventDrivenJobMap);

            tree.getRoot().getBranches().forEach(branch
                -> this.addLinksToDiagram(branch, linkingConnections, diagramBuilder, graph));
        }

        return linkingConnections;
    }

    /**
     * Recursively adds links and constructs connections from a tree structure to a diagram, while updating the internal graph and diagram builder.
     *
     * @param treeNode The current node in the tree to process links for. It contains branches that may need to be added to the diagram.
     * @param linkingConnections A list to collect and track connections being created. It accumulates the identifiers of connected nodes.
     * @param diagramBuilder The builder object responsible for constructing and managing the diagram representation to which links are added.
     * @param graph The directed graph that maintains the relationships (edges) between nodes (vertices) for the processed tree structure.
     */
    protected void addLinksToDiagram(TreeNode<String> treeNode, List<String> linkingConnections, DiagramBuilder diagramBuilder
        , DefaultDirectedGraph<Object, DefaultEdge> graph) {
        treeNode.getBranches().forEach(branch -> {
            linkingConnections.add(branch.getData());
            if(!graph.containsVertex(branch.getParent().getData())) {
                // Add the parent to the diagram if it does not exist.
                graph.addVertex(branch.getParent().getData());
                linkingConnections.add(branch.getParent().getData());

                UserDataBuilder userDataBuilder = new UserDataBuilder()
                    .withIdentifier(branch.getParent().getData())
                    .withItemType(UserData.CONTEXT)
                    .withContextName(branch.getParent().getData());

                this.addExternalContext(branch.getParent().getData(), diagramBuilder, userDataBuilder.build());
            }
            graph.addVertex(branch.getData());

            // Add the branch to the diagram.
            UserDataBuilder userDataBuilder = new UserDataBuilder()
                .withIdentifier(branch.getData())
                .withItemType(UserData.CONTEXT)
                .withContextName(branch.getData());

            this.addExternalContext(branch.getData(), diagramBuilder, userDataBuilder.build());

            // Now connect the parent to the branch.
            this.addConnection(branch.getParent().getData(), CONNECTOR_RIGHT_HYBRID_SOURCE,
                branch.getData(), CONNECTOR_TOP_HYBRID_TARGET, diagramBuilder);

            graph.addEdge(branch.getParent().getData(), branch.getData());

            // Recursively work our way through the tree.
            this.addLinksToDiagram(branch, linkingConnections, diagramBuilder, graph);
        });
    }

    /**
     * Manages the transitions of outbound contexts and establishes connections between jobs, contexts, and diagrams.
     * This method processes context transitions to generate and add context vertices to the graph, while ensuring proper linking
     * and relationships are maintained based on preceding job identifiers and contextual data.
     *
     * @param contextTransitions List of {@code ContextTransition} objects that represent the transitions between contexts
     *                           and their associated jobs.
     * @param diagramBuilder     The {@code DiagramBuilder} instance used to build and modify the diagram representation.
     * @param graph              The {@code DefaultDirectedGraph} used as the structural representation of contexts and jobs.
     * @param linkingConnections List of strings representing context connection identifiers that should be maintained.
     * @param parentContext      The {@code Context} representing the parent context in the hierarchy for child context resolution.
     */
    protected void manageOutboundContextTransitions(List<ContextTransition> contextTransitions, DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object, DefaultEdge> graph,
                                                    List<String> linkingConnections, Context parentContext) {
        List<String> addedContexts = new ArrayList<>();

        List<String> precedingJobIdentifiers = new ArrayList<>();
        contextTransitions.forEach(contextTransition
            -> precedingJobIdentifiers.add(contextTransition.getPrecedingJob().getIdentifier()));

        contextTransitions.forEach(contextTransition -> {
            if(!contextTransition.getPrecedingJob().getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
                contextTransition.getContexts().forEach(context -> {
                    Context child = ContextHelper.getChildContext(context, parentContext);

                    if (child != null) {
                        List<String> jobIdentifiers = (List<String>) child.getScheduledJobs().stream()
                            .map(job -> ((SchedulerJob) job).getIdentifier())
                            .collect(Collectors.toList());

                        if (!jobIdentifiers.stream().anyMatch(element -> precedingJobIdentifiers.contains(element)))
                            return;
                    }

                    String contextName = context;
                    if (!linkingConnections.contains(context)) {
                        context = context + "_out";
                    }

                    if (!graph.containsVertex(context)) {
                        graph.addVertex(context);

                        if (!addedContexts.contains(context)) {
                            UserDataBuilder userDataBuilder = new UserDataBuilder()
                                .withIdentifier(context)
                                .withItemType(UserData.CONTEXT)
                                .withContextName(contextName);

                            precedingJobIdentifiers.forEach(id -> userDataBuilder.addPreviousJobIdentifiers(id));

                            this.addExternalContext(context, diagramBuilder, userDataBuilder.build());
                            addedContexts.add(context);
                        }
                    }

                    this.addConnection(contextTransition.getPrecedingJob().getIdentifier(), CONNECTOR_RIGHT_HYBRID_SOURCE
                        , context, CONNECTOR_LEFT_HYBRID_TARGET, diagramBuilder);

                    graph.addEdge(contextTransition.getPrecedingJob().getIdentifier(), context);
                });
            }
        });
    }

    /**
     * Manages the inbound context transitions, adding context nodes and connections to the specified directed graph,
     * updating the diagram builder for visual representation, and maintaining subsequent job identifiers.
     *
     * @param contextTransitions a list of context transitions, each representing the relationship between preceding
     *                            and subsequent jobs and the associated contexts.
     * @param diagramBuilder the diagram builder instance used to visually represent the context transitions.
     * @param graph the directed graph where context nodes and their connections will be added.
     * @param linkingConnections a list of connection identifiers indicating existing context links to avoid duplication.
     * @param schedulerJobsMap a map of scheduler job identifiers to their corresponding `SchedulerJob` instances, which
     *                         contain metadata for job contexts and configurations.
     * @return a list of identifiers for subsequent jobs that occurred in context transitions.
     */
    protected List<String> manageInboundContextTransitions(List<ContextTransition> contextTransitions
        , DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object
        , DefaultEdge> graph, List<String> linkingConnections, Map<String, SchedulerJob> schedulerJobsMap) {
        List<String> addedContexts = new ArrayList<>();
        List<String> subsequentJobIdentifiers = new ArrayList<>();
        contextTransitions.forEach(contextTransition
            -> {
            if(!contextTransition.getPrecedingJob().getAgentName().equals(JobConstants.CONTEXT_START_JOB)
                && !contextTransition.getPrecedingJob().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                subsequentJobIdentifiers.add(contextTransition.getPrecedingJob().getIdentifier());
            }
            else {
                subsequentJobIdentifiers.add(contextTransition.getSubsequentJob().getIdentifier());
            }
        });

        contextTransitions.forEach(contextTransition -> {
            if(!contextTransition.getPrecedingJob().getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
                contextTransition.getContexts().forEach(context -> {
                    String contextName = context;
                    if (!linkingConnections.contains(context)) {
                        context = context + "_in";
                    }
                    if (!addedContexts.contains(context) && !graph.containsVertex(context)) {
                        graph.addVertex(context);

                        UserDataBuilder userDataBuilder = new UserDataBuilder()
                            .withIdentifier(context)
                            .withItemType(UserData.CONTEXT)
                            .withContextName(contextName);

                        subsequentJobIdentifiers.forEach(id ->
                        {
                            SchedulerJob internalEventDrivenJob = schedulerJobsMap.get(id);
                            if (internalEventDrivenJob != null && internalEventDrivenJob
                                .getChildContextNames().contains(contextName)) {
                                userDataBuilder.addSubsequentJobIdentifiers(id);
                            }
                        });

                        this.addExternalContext(context, diagramBuilder, userDataBuilder.build());
                    }

                    if (!contextTransition.getPrecedingJob().getAgentName().equals(JobConstants.CONTEXT_START_JOB)
                        && !contextTransition.getPrecedingJob().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                        this.addConnection(context, CONNECTOR_RIGHT_HYBRID_SOURCE
                            , contextTransition.getPrecedingJob().getIdentifier(), CONNECTOR_LEFT_HYBRID_TARGET, diagramBuilder);

                        graph.addEdge(context, contextTransition.getPrecedingJob().getIdentifier());
                    } else {
                        this.addConnection(context, CONNECTOR_RIGHT_HYBRID_SOURCE
                            , contextTransition.getSubsequentJob().getIdentifier(), CONNECTOR_LEFT_HYBRID_TARGET, diagramBuilder);

                        if (graph.containsVertex(contextTransition.getSubsequentJob().getIdentifier())) {
                            graph.addEdge(context, contextTransition.getSubsequentJob().getIdentifier());
                        }
                    }

                    addedContexts.add(context);
                });
            }
        });

        return subsequentJobIdentifiers;
    }

    /**
     *
     */
    private Tree<String> getLinkedContexts(Context context, Context parentContext, Map<String, SchedulerJob> schedulerJobMap) {
        Tree<String> tree = new Tree<>(new TreeNode<>(context.getName()));

        if(!context.getContexts().isEmpty()) {
            Map<String, List<String>> contextSubsequentTransitionMap = new HashMap<>();

            List<String> subsequentTransitions = (List<String>) ContextHelper.determineIfJobsTransitionToOtherContexts
                (parentContext, context.getScheduledJobsMap(), context, schedulerJobMap)
                .stream()
                .flatMap(contextTransition -> ((ContextTransition)contextTransition).getContexts().stream())
                .filter(contextName -> !contextName.equals(context.getName()))
                .distinct()
                .collect(Collectors.toList());

            context.getContexts().forEach(child -> {
                this.getAllChildContextTransitions(context, (Context) child, parentContext, contextSubsequentTransitionMap, schedulerJobMap);
            });

            AtomicReference<TreeNode<String>> node = new AtomicReference<>();
            subsequentTransitions.forEach(s -> {
                TreeNode<String> n = new TreeNode<>(s);
                if(node.get() == null) {
                    tree.getRoot().addBranch(n);
                }
                else {
                    node.get().addBranch(n);
                }

                followContextTransitionPath(s, n, contextSubsequentTransitionMap);
            });
        }

        return tree;
    }

    /**
     * Recursively gets all child context transitions for the given context.
     *
     * @param theContext                  The top-level context to start from.
     * @param context                     The current context being processed.
     * @param parentContext               The parent context of the current context.
     * @param contextSubsequentTransitionMap A map to store the subsequent transitions for each child context.
     * @param schedulerJobMap             A map of scheduler jobs.
     */
    private void getAllChildContextTransitions(Context theContext, Context context, Context parentContext, Map<String, List<String>> contextSubsequentTransitionMap
        , Map<String, SchedulerJob> schedulerJobMap) {
        context.getContexts().forEach(child -> {
            List<String> childSubsequentTransitions = (List<String>) ContextHelper.determineIfJobsTransitionToOtherContexts
                    (parentContext, ((Context)child).getScheduledJobsMap(), (Context)child, schedulerJobMap)
                .stream()
                .flatMap(contextTransition -> ((ContextTransition)contextTransition).getContexts().stream())
                .filter(contextName -> !contextName.equals(context.getName()) && !contextName.equals(theContext.getName()))
                .distinct()
                .collect(Collectors.toList());;

            ((Context<?, ?, ?, ?>) child).getContexts().forEach(c
                -> getAllChildContextTransitions(theContext, c, (Context) child, contextSubsequentTransitionMap, schedulerJobMap));

            if(!((Context<?, ?, ?, ?>) child).getName().equals(theContext.getName())) {
                contextSubsequentTransitionMap.put(((Context<?, ?, ?, ?>) child).getName(), childSubsequentTransitions);
            }
        });
    }

    /**
     * Method to recursively follow transitions between contexts.
     *
     *
     * @param contextName
     * @param node
     * @param contextSubsequentTransitionMap
     */
    protected void followContextTransitionPath(String contextName, TreeNode<String> node,
        Map<String, List<String>> contextSubsequentTransitionMap) {

        ArrayList<String> processedContexts = new ArrayList<>();

        this._followContextTransitionPath(contextName, node, contextSubsequentTransitionMap
            , processedContexts);
    }

    /**
     * Method to recursively follow transitions between contexts.
     *
     *
     * @param contextName
     * @param node
     * @param contextSubsequentTransitionMap
     */
    protected void _followContextTransitionPath(String contextName, TreeNode<String> node,
                                               Map<String, List<String>> contextSubsequentTransitionMap, ArrayList<String> processedContexts) {

        if(processedContexts.contains(contextName)) {
            return;
        }
        else {
            processedContexts.add(contextName);
        }

        if(contextSubsequentTransitionMap.containsKey(contextName)) {
            contextSubsequentTransitionMap.get(contextName).forEach(childContextName -> {
                TreeNode<String> treeNode = new TreeNode<>(childContextName);
                node.addBranch(treeNode);

                _followContextTransitionPath(childContextName, treeNode
                    , contextSubsequentTransitionMap, processedContexts);
            });
        }

    }

    /**
     * Method to determine if a previous context transitions to the one being rendered in the diagram.
     *
     * @param parentContext
     * @param context
     * @param schedulerJobsMap
     * @return
     */
    protected List<ContextTransition> getPreviousContextTransitions(Context parentContext, Context context, Map<String, SchedulerJob> schedulerJobsMap) {
        HashSet<ContextTransition> previousContexts = new HashSet<>();

        context.getScheduledJobs().forEach(job -> {
            List<ContextTransition>  jobsInto = ContextHelper.determineIfJobsTransitionFromOtherContexts(parentContext, ((SchedulerJob)job).getJobName(),
                context.getName(), schedulerJobsMap);

            jobsInto.forEach(jobsOtherContexts -> {
                if(jobsOtherContexts.getSubsequentJob().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB)) {
                    LinkedList<List<SchedulerJob>> trace = ContextHelper.traceJobThroughContext
                        (parentContext, jobsOtherContexts.getSubsequentJob().getJobName(), context.getName());
                    if(!trace.isEmpty()) {
                        List<SchedulerJob> subsequentJobs = trace.get(0);
                        if(!subsequentJobs.isEmpty()) {
                            jobsOtherContexts.setSubsequentJob(subsequentJobs.get(0));
                        }
                    }
                }
                previousContexts.add(jobsOtherContexts);
            });
        });

        return previousContexts.stream().filter(contextTransition -> !contextTransition.getContexts().isEmpty()).collect(Collectors.toList());
    }

    /**
     * Helper method to get all jobs in a logical grouping.
     *
     * @param logicalGrouping
     * @param grouping
     */
    protected void getAllJobsInGrouping(LogicalGrouping logicalGrouping, VisualisationLogicalGrouping grouping) {
        VisualisationLogicalGrouping nestedVisualisationLogicalGrouping = null;
        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            nestedVisualisationLogicalGrouping = new VisualisationLogicalGrouping();
            nestedVisualisationLogicalGrouping.setType("AND");
            grouping.getNestedGrouping().add(nestedVisualisationLogicalGrouping);
            for (And and : logicalGrouping.getAnd()) {

                if (and.getIdentifier() != null) {
                    nestedVisualisationLogicalGrouping.getJobIdentifiers().add(and.getIdentifier());
                }

                if (and.getLogicalGrouping() != null) {
                    getAllJobsInGrouping(and.getLogicalGrouping(), nestedVisualisationLogicalGrouping);
                }
            }
        }

        if(logicalGrouping.getOr() != null && !logicalGrouping.getOr().isEmpty()) {
            nestedVisualisationLogicalGrouping = new VisualisationLogicalGrouping();
            nestedVisualisationLogicalGrouping.setType("OR");
            grouping.getNestedGrouping().add(nestedVisualisationLogicalGrouping);
            for (Or or : logicalGrouping.getOr()) {
                if(or.getIdentifier() != null) {
                    nestedVisualisationLogicalGrouping.getJobIdentifiers().add(or.getIdentifier());
                }

                if (or.getLogicalGrouping() != null) {
                    getAllJobsInGrouping(or.getLogicalGrouping(), nestedVisualisationLogicalGrouping);
                }
            }
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            getAllJobsInGrouping(logicalGrouping.getLogicalGrouping(), nestedVisualisationLogicalGrouping != null ? nestedVisualisationLogicalGrouping : grouping);
        }
    }

    /**
     * Add all logical groupings to the diagram.
     *
     * @param visualisationLogicalGrouping
     * @param imageOverlay
     * @param cellMap
     * @param diagramBuilder
     */
    protected void addLogicGroupings(VisualisationLogicalGrouping visualisationLogicalGrouping, ArrayList<Object> imageOverlay
        , Map<String, mxCell> cellMap, DiagramBuilder diagramBuilder, Map<String, Image> schedulerJobsImageMap) {
        visualisationLogicalGrouping.getNestedGrouping().forEach(nestedVisualisationLogicalGrouping -> {
            this.addLogicGroupings(nestedVisualisationLogicalGrouping, imageOverlay, cellMap, diagramBuilder, schedulerJobsImageMap);

            if(nestedVisualisationLogicalGrouping.getJobIdentifiers().size() > 1 || (!nestedVisualisationLogicalGrouping.getNestedGrouping().isEmpty())) {
                AtomicReference<Double> xMinExtent = new AtomicReference<>();
                xMinExtent.set(-1.0);
                AtomicReference<Double> xMaxExtent = new AtomicReference<>();
                xMaxExtent.set(-1.0);
                AtomicReference<Double> yMinExtent = new AtomicReference<>();
                yMinExtent.set(-1.0);
                AtomicReference<Double> yMaxExtent = new AtomicReference<>();
                yMaxExtent.set(-1.0);

                this.calculateExtents(nestedVisualisationLogicalGrouping, xMinExtent, xMaxExtent, yMinExtent, yMaxExtent, cellMap, schedulerJobsImageMap);

                if(xMinExtent.get() == -1.0
                    && xMaxExtent.get() == -1.0
                    && yMinExtent.get() == -1.0
                    && yMaxExtent.get() == -1.0) {
                    return;
                }

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

                if (nestedVisualisationLogicalGrouping.getType().equals("AND")) {
                    rb.withId("AND-" + UUID.randomUUID())
                        .withColor(IkasanColours.SCHEDULER_AND)
                        .withDasharray("--");
                } else {
                    rb.withId("OR-" + UUID.randomUUID())
                        .withColor(IkasanColours.SCHEDULER_OR)
                        .withDasharray("--..");
                }

                Rectangle rectangle = rb.build();

                if(imageOverlay.stream().filter(r -> r.equals(rectangle)).count() == 0) {
                    imageOverlay.add(rectangle);
                }
            }
        });
    }



    /**
     * Calculates the minimum and maximum extents (x and y) for a given visualisation
     * logical grouping and its related job identifiers. This method also adjusts
     * the geometry of cells based on scheduler job images and updates the maximum
     * extents of the jobs as required.
     *
     * @param visualisationLogicalGrouping The logical grouping object containing nested
     *                                     groupings and job identifiers to evaluate.
     * @param xMinExtent                   An AtomicReference holding the minimum x-axis extent,
     *                                     which will be updated during calculation.
     * @param xMaxExtent                   An AtomicReference holding the maximum x-axis extent,
     *                                     which will be updated during calculation.
     * @param yMinExtent                   An AtomicReference holding the minimum y-axis extent,
     *                                     which will be updated during calculation.
     * @param yMaxExtent                   An AtomicReference holding the maximum y-axis extent,
     *                                     which will be updated during calculation.
     * @param cellMap                      A map containing job identifiers as keys and their
     *                                     corresponding mxCell objects as values.
     * @param schedulerJobsImageMap        A map containing job identifiers as keys and their
     *                                     corresponding Image objects as values, used for
     *                                     adjusting cell geometry.
     */
    protected void calculateExtents(VisualisationLogicalGrouping visualisationLogicalGrouping, AtomicReference<Double> xMinExtent, AtomicReference<Double> xMaxExtent,
                                    AtomicReference<Double> yMinExtent, AtomicReference<Double> yMaxExtent, Map<String, mxCell> cellMap, Map<String, Image> schedulerJobsImageMap) {
        visualisationLogicalGrouping.getNestedGrouping().forEach(nestedVisualisationLogicalGrouping -> {
            this.calculateExtents(nestedVisualisationLogicalGrouping, xMinExtent, xMaxExtent, yMinExtent, yMaxExtent, cellMap, schedulerJobsImageMap);

            nestedVisualisationLogicalGrouping.getJobIdentifiers().forEach(id -> {
                mxCell cell = cellMap.get(id);

                if(schedulerJobsImageMap.containsKey(id)) {
                    cell.getGeometry().setX(schedulerJobsImageMap.get(id).getX()-600);
                    cell.getGeometry().setY(schedulerJobsImageMap.get(id).getY()-600);
                }

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

        visualisationLogicalGrouping.getJobIdentifiers().forEach(id -> {
            mxCell cell = cellMap.get(id);
            if(cell == null) return;

            if(schedulerJobsImageMap.containsKey(id)) {
                cell.getGeometry().setX(schedulerJobsImageMap.get(id).getX()-600);
                cell.getGeometry().setY(schedulerJobsImageMap.get(id).getY()-600);
            }

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

        if(xMinExtent.get() == -1.0
            && xMaxExtent.get() == -1.0
            && yMinExtent.get() == -1.0
            && yMaxExtent.get() == -1.0) {
            return;
        }

        xMinExtent.set(xMinExtent.get() - 15);
        xMaxExtent.set(xMaxExtent.get() + 15);
        yMinExtent.set(yMinExtent.get() - 15);
        yMaxExtent.set(yMaxExtent.get() + 15);
    }

    /**
     * Helper method to get the relevant image for a job.
     *
     * @param schedulerJob
     * @return
     */
    protected String getJobImage(SchedulerJob schedulerJob) {
        String image = "frontend/images/command_black.png";

        if(schedulerJob instanceof FileEventDrivenJob || schedulerJob instanceof FileEventDrivenJobInstance) {
            image = "frontend/images/file_black.png";
        }
        else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
            image = "frontend/images/time_black.png";
        }
        else if(schedulerJob instanceof GlobalEventJob || schedulerJob instanceof GlobalEventJobInstance) {
            image = "frontend/images/global-job.png";
        }
        else if(schedulerJob instanceof ContextStartJobInstance || schedulerJob instanceof ContextStartJob) {
            image = "frontend/images/start-job.png";
        }
        else if(schedulerJob instanceof ContextTerminalJob || schedulerJob instanceof ContextStartJobInstance) {
            image = "frontend/images/terminal-job.png";
        }
        else if(schedulerJob instanceof LocalEventJob || schedulerJob instanceof LocalEventJobInstance) {
            image = "frontend/images/local-event-job.png";
        }
        else if(schedulerJob instanceof BridgingJob || schedulerJob instanceof BridgingJobInstance) {
            image = "frontend/images/bridging-job.png";
        }

        return image;
    }

    /**
     * Recursively get all jobs from a VisualisationLogicalGrouping.
     *
     * @param context
     * @param visualisationLogicalGrouping
     * @return
     */
    protected List<SchedulerJob> getSchedulerJobsFromGrouping(Context context, VisualisationLogicalGrouping visualisationLogicalGrouping) {
        ArrayList<SchedulerJob> schedulerJobs = new ArrayList<>();

        _getSchedulerJobsFromGrouping(context, visualisationLogicalGrouping, schedulerJobs);

        return schedulerJobs;
    }

    /**
     * Recursively get all jobs from a VisualisationLogicalGrouping.
     *
     * @param context
     * @param visualisationLogicalGrouping
     * @param schedulerJobs
     */
    private void _getSchedulerJobsFromGrouping(Context context, VisualisationLogicalGrouping visualisationLogicalGrouping, List<SchedulerJob> schedulerJobs) {
        visualisationLogicalGrouping.getJobIdentifiers().forEach(id -> {
            if(!schedulerJobs.contains(context.getScheduledJobsMap().get(id))) {
                schedulerJobs.add((SchedulerJob) context.getScheduledJobsMap().get(id));
            }
        });

        if(visualisationLogicalGrouping.getNestedGrouping() != null) {
            visualisationLogicalGrouping.getNestedGrouping().forEach(nested -> {
                _getSchedulerJobsFromGrouping(context, nested, schedulerJobs);
            });
        }
    }

    /**
     * Helper method to recursively calculate the depth of a VisualisationLogicalGrouping.
     *
     * @param visualisationLogicalGrouping
     * @return
     */
    protected int getGroupingDepth(VisualisationLogicalGrouping visualisationLogicalGrouping) {
        AtomicInteger depth = new AtomicInteger(0);
        _getGroupingDepth(visualisationLogicalGrouping, depth);

        return depth.get();
    }

    /**
     * Helper method to recursively calculate the depth of a VisualisationLogicalGrouping.
     *
     * @param visualisationLogicalGrouping
     * @param depth
     */
    private void _getGroupingDepth(VisualisationLogicalGrouping visualisationLogicalGrouping, AtomicInteger depth) {
        boolean depthIncrement = false;

        for (VisualisationLogicalGrouping nested : visualisationLogicalGrouping.getNestedGrouping()) {
            if(!depthIncrement) {
                depth.getAndIncrement();
                depthIncrement = true;
            }
            _getGroupingDepth(nested, depth);
        }
    }

    /**
     * Helper method to add an external context to the diagram.
     *
     * @param identifier
     * @param diagramBuilder
     */
    protected void addExternalContext(String identifier, DiagramBuilder diagramBuilder, UserData userData) {
        ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
            .withId(identifier)
            .withHeight(100)
            .withWidth(100)
            .withPath("frontend/images/context-icon.png")
            .withUserData(userData)
            .withLeftPort()
            .withRightPort();

        diagramBuilder.addItem(jobBuilder.build());
    }


    protected void addConnection(String sourceIdentifier, String sourcePort, String targetIdentifier, String targetPort
        , DiagramBuilder diagramBuilder) {
        ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
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

        diagramBuilder.addItem(connectionBuilder.build());
    }

    protected void addConnectionWithLabel(String sourceIdentifier, String sourcePort, String targetIdentifier, String targetPort
        , DiagramBuilder diagramBuilder, String labelText, double labelX, double labelY) {
        ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
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

        diagramBuilder.addItem(connectionBuilder.build());

        if(labelText != null && !labelText.isEmpty()) {
            int maxLength = labelText.length();
            if (labelText.contains("\n")) {
                maxLength = Arrays.stream(labelText.split("\n"))
                    .mapToInt(String::length)
                    .max()
                    .orElse(labelText.length());
            }
            double labelLength = maxLength * 8 * 0.65;

            Label label = new LabelBuilder().withText(labelText)
                .withX(labelX - labelLength - 25)
                .withY(labelY)
                .withFontSize("pt")
                .build();
            diagramBuilder.addItem(label);
        }
    }

    /**
     * Sets the layout configuration for diagram visualization.
     *
     * @param context the context containing the layout configuration properties
     */
    protected void setDiagramVisualisationLayoutConfiguration(Context context) {
        if(context.getContextVisualisationLevelDistance() != null) {
            this.contextVisualisationLevelDistance = context.getContextVisualisationLevelDistance();
        }

        if(context.getContextVisualisationNodeDistance() != null) {
            this.contextVisualisationNodeDistance = context.getContextVisualisationNodeDistance();
        }

        if(context.getJobVisualisationHorizontalSpacing() != null) {
            this.jobVisualisationHorizontalSpacing = context.getJobVisualisationHorizontalSpacing();
        }

        if(context.getJobVisualisationVerticalSpacing() != null) {
            this.jobVisualisationVerticalSpacing = context.getJobVisualisationVerticalSpacing();
        }

        if(context.getVisualisationFontSize() != null) {
            this.fontSize = context.getVisualisationFontSize();
        }
    }
}
