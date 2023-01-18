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
import org.ikasan.spec.scheduled.instance.model.GlobalEventJobInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.QuartzScheduleDrivenJobInstance;
import org.ikasan.spec.scheduled.job.model.*;
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

    /**
     * Base method to adapt a context that contains jobs to a draw2d model. This method will cater for a mix of
     * contexts and jobs to indicate when boundaries between contexts are crossed.
     *
     * @param parentContext
     * @param context
     * @param schedulerJobs
     * @param internalEventDrivenJobMap
     * @return
     */
    protected ArrayList<Object> _adaptJobs(Context parentContext, Context context, Map<String, SchedulerJob> schedulerJobs, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        if(context.getScheduledJobs() != null && !context.getScheduledJobs().isEmpty()) {

            // Determine if any jobs are initiated from a previous or are responsible for initiating a job in a
            // subsequent flow.
            List<ContextTransition> previousContexts = this.getPreviousContextTransitions(parentContext, context, internalEventDrivenJobMap);
            List<ContextTransition> subsequentTransitions = ContextHelper.determineIfJobsTransitionToOtherContexts
                (parentContext, context.getScheduledJobsMap(), context, internalEventDrivenJobMap);

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

            // Create an item for each jobs as well as adding each job to the
            // DefaultDirectedGraph. We delegate to some builder classes that
            // create all the relevant items to be rendered in draw2d.
            jobs.forEach(job -> {
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

                ImageBuilder jobBuilder = diagramBuilder.getImageBuilder()
                    .withId(job.getIdentifier())
                    .withHeight(100)
                    .withWidth(100)
                    .withPath(image)
                    .withUserData(userDataBuilder.build());

                if(schedulerJob instanceof InternalEventDrivenJob || schedulerJob instanceof InternalEventDrivenJobInstance
                    || schedulerJob instanceof GlobalEventJob || schedulerJob instanceof GlobalEventJobInstance) {
                    jobBuilder.withLeftPort()
                        .withRightPort();
                }
                else if(schedulerJob instanceof QuartzScheduleDrivenJob || schedulerJob instanceof QuartzScheduleDrivenJobInstance) {
                    jobBuilder.withRightPort();
                }

                diagramBuilder.addItem(jobBuilder
                    .build());

            });

            // Work out way through the job dependencies and add connections between jobs.
            // This is managed recursively as
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

            // Now delegate to a helper method to deal with the case that jobs within a job plan
            // may be linked by sub contexts.
            List<String> linkingConnections = this.manageLinkingContextTransitions(context, parentContext
                , internalEventDrivenJobMap, diagramBuilder, graph);

            // Manage the case that there are other contexts that the job plan links to.
            this.manageOutboundContextTransitions(subsequentTransitions, diagramBuilder, graph
                , linkingConnections, parentContext);

            // Manage the case that there are other contexts that precede this one and link to it.
            List<String> inboundConnections =  this.manageInboundContextTransitions(previousContexts, diagramBuilder, graph
                , linkingConnections, internalEventDrivenJobMap);

            // Now delegate to the JGraphXAdapter to create the layout
            // of the visualisation.
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
            int depth = this.getGroupingDepth(visualisationLogicalGrouping);
            compactTreeLayout.setIntraCellSpacing(120+(depth*20));
            compactTreeLayout.setInterHierarchySpacing(1000);
            compactTreeLayout.setInterRankCellSpacing(1000);

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

                        ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                        ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

                        double positionedItemCentre = ((PositionedItem) item).getX() + 50;

                        if(context.getScheduledJobsMap()
                            .containsKey(((PositionedItem) item).getId())) {
                            ((Image) item).setComposite(group.getId());
                            // assuming each letter is 8 units long
                            double labelLength = ((SchedulerJob) context.getScheduledJobsMap()
                                .get(((PositionedItem) item).getId())).getJobName().length() * 7.5;

                            Label label = new LabelBuilder().withText(((SchedulerJob) context.getScheduledJobsMap()
                                    .get(((PositionedItem) item).getId())).getJobName())
                                .withX(positionedItemCentre - (labelLength / 2))
                                .withY(((PositionedItem) item).getY() + 110)
                                .withFontSize("14pt")
                                .withComposite(group.getId())
                                .build();

                            group.setUserData(((Image) item).getUserData());
                            label.setUserData(((Image) item).getUserData());

                            labels.add(label);

                            if(inboundConnections.contains(((PositionedItem) item).getId())){
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

                            if(internalEventDrivenJobMap.containsKey(((PositionedItem) item).getId())) {
                                InternalEventDrivenJob internalEventDrivenJob = internalEventDrivenJobMap.get(((PositionedItem) item).getId());

                                if(internalEventDrivenJob.isJobRepeatable()) {
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
                            double labelLength = ((Image) item).getUserData().getContextName().length() * 7.5;

                            ((Image) item).setComposite(group.getId());

                            Label label = new LabelBuilder().withText(((Image) item).getUserData().getContextName())
                                .withX(positionedItemCentre - (labelLength / 2))
                                .withY(((PositionedItem) item).getY() + 110)
                                .withFontSize("14pt")
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

            // Now draw the logic groupings abd context boundaries onto the diagram.
            this.addLogicGroupings(visualisationLogicalGrouping, imageOverlay, cellMap, diagramBuilder);
            this.addContextBoundaries(items, imageOverlay, cellMap, diagramBuilder);

            items.addAll(imageOverlay);
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
     * Base method to adapt a context to a draw2d model.
     *
     * @param context
     * @return
     */
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

        // We simply recursively work our way through the context
        // and all nested contexts and render them into the diagram.
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

                this.addConnection(root.getId(), "bottomHybridSource", branch.getId()
                    , "topHybridTarget", diagramBuilder);

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
        compactTreeLayout.setLevelDistance(200);
        compactTreeLayout.setEdgeRouting(true);
        compactTreeLayout.setNodeDistance(75);
        compactTreeLayout.setGroupPadding(100);

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

        return items;
    }

    /**
     * Recursive method to work our way though a context and all of its child contexts
     * and add them to the diagram.
     *
     * @param context
     * @param graph
     * @param diagramBuilder
     * @param contextInstanceMap
     */
    protected void manageContext(Context context, DefaultDirectedGraph<Object, DefaultEdge> graph,
                                 DiagramBuilder diagramBuilder, Map<String, Context> contextInstanceMap) {

        if(context.getContexts() != null && !context.getContexts().isEmpty()) {
            context.getContexts().forEach(c -> {
                contextInstanceMap.put(((Context)c).getName(), (Context)c);
                graph.addVertex(((Context)c).getName());
                graph.addEdge(context.getName(), ((Context)c).getName());

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

                this.addConnection(context.getName(), "bottomHybridSource", branch.getId()
                    , "topHybridTarget", diagramBuilder);

                this.manageContext((Context)c, graph, diagramBuilder, contextInstanceMap);
            });
        }
    }

    /**
     * Helper method to get the cell map from the JGraphXAdapter which is used to create the
     * layout of the diagram. The cell map is keyed based on the item identifier and contains
     * all coordinate information relating to the layout of the diagram.
     *
     * @param jGraphXAdapter
     * @return
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
     * This method is responsible for recursively creating connections between jobs that reside within
     * any LogicalGroupings.
     *
     * @param context
     * @param jobIdentifier
     * @param logicalGrouping
     * @param diagramBuilder
     * @param graph
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
                else {
                    context.getContexts().forEach(child -> {
                        if(((Context)child).getScheduledJobsMap().containsKey(and.getIdentifier())) {
                        }
                    });
                    graph.addEdge(and.getIdentifier(), jobIdentifier);

                    this.addConnection(and.getIdentifier(), "rightHybridSource"
                        , jobIdentifier, "leftHybridTarget", diagramBuilder);
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

                    this.addConnection(or.getIdentifier(), "rightHybridSource"
                        , jobIdentifier, "leftHybridTarget", diagramBuilder);
                }
            });
        }
    }

    /**
     * This method is responsible for managing any linking contexts within a job plan. Ultimately it delegates to
     * the method getLinkedContexts, which builds a org.ikasan.dashboard.ui.visualisation.scheduler.model.Tree
     * which contains nodes of identifiers of in a tree structure with the root being the start of the linking
     * and nested branches containing the linkage path.
     *
     * Once the tree has been constructed, this method then delegates to addLinkingsToDiagram which recursively
     * adds all linking context transitions to the diagram.
     *
     * @param context
     * @param parentContext
     * @param internalEventDrivenJobMap
     * @param diagramBuilder
     * @param graph
     * @return
     */
    protected List<String> manageLinkingContextTransitions(Context context, Context parentContext, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap,
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
     * Helper method to recursively add all context links to the diagram.
     *
     * @param treeNode
     * @param linkingConnections
     * @param diagramBuilder
     * @param graph
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
            this.addConnection(branch.getParent().getData(), "rightHybridSource",
                branch.getData(), "leftHybridTarget", diagramBuilder);

            graph.addEdge(branch.getParent().getData(), branch.getData());

            // Recursively work our way through the tree.
            this.addLinksToDiagram(branch, linkingConnections, diagramBuilder, graph);
        });
    }

    /**
     * Manage the case where a subsequent context has a job dependency that the current context is
     * responsible for starting a job in the subsequent context.
     *
     * @param contextTransitions
     * @param diagramBuilder
     * @param graph
     * @param linkingConnections
     */
    protected void manageOutboundContextTransitions(List<ContextTransition> contextTransitions, DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object, DefaultEdge> graph,
                                                    List<String> linkingConnections, Context parentContext) {
        List<String> addedContexts = new ArrayList<>();

        List<String> precedingJobIdentifiers = new ArrayList<>();
        contextTransitions.forEach(contextTransition
            -> precedingJobIdentifiers.add(contextTransition.getPrecedingJob().getIdentifier()));

        contextTransitions.forEach(contextTransition -> {
            contextTransition.getContexts().forEach(context -> {
                Context child = ContextHelper.getChildContext(context, parentContext);

                if(child != null) {
                    List<String> jobIdentifiers = (List<String>) child.getScheduledJobs().stream()
                        .map(job -> ((SchedulerJob)job).getIdentifier())
                        .collect(Collectors.toList());

                    if(!jobIdentifiers.stream().anyMatch(element -> precedingJobIdentifiers.contains(element))) return;
                }

                String contextName = context;
                if(!linkingConnections.contains(context)) {
                    context = context + "_out";
                }

                if(!graph.containsVertex(context)) {
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

                this.addConnection(contextTransition.getPrecedingJob().getIdentifier(), "rightHybridSource"
                    , context, "leftHybridTarget", diagramBuilder);

                graph.addEdge(contextTransition.getPrecedingJob().getIdentifier(), context);
            });
        });
    }

    /**
     * Manage the case where a previous context has a job dependency that is the catalyst for a job
     * starting in the context that the diagram is being rendered for.
     *
     * @param contextTransitions
     * @param diagramBuilder
     * @param graph
     * @param linkingConnections
     */
    protected List<String> manageInboundContextTransitions(List<ContextTransition> contextTransitions
        , DiagramBuilder diagramBuilder, DefaultDirectedGraph<Object
        , DefaultEdge> graph, List<String> linkingConnections, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        List<String> addedContexts = new ArrayList<>();
        List<String> subsequentJobIdentifiers = new ArrayList<>();
        contextTransitions.forEach(contextTransition
            -> subsequentJobIdentifiers.add(contextTransition.getPrecedingJob().getIdentifier()));

        contextTransitions.forEach(contextTransition -> {
            contextTransition.getContexts().forEach(context -> {
                String contextName = context;
                if(!linkingConnections.contains(context)) {
                    context = context + "_in";
                }
                if (!addedContexts.contains(context) && ! graph.containsVertex(context)) {
                    graph.addVertex(context);

                    UserDataBuilder userDataBuilder = new UserDataBuilder()
                        .withIdentifier(context)
                        .withItemType(UserData.CONTEXT)
                        .withContextName(contextName);

                    subsequentJobIdentifiers.forEach(id ->
                    {
                        InternalEventDrivenJob internalEventDrivenJob = internalEventDrivenJobMap.get(id);
                        if(internalEventDrivenJob != null && internalEventDrivenJob
                            .getChildContextNames().contains(contextName)) {
                            userDataBuilder.addSubsequentJobIdentifiers(id);
                        }
                    });

                    this.addExternalContext(context, diagramBuilder, userDataBuilder.build());
                }

                this.addConnection(context, "rightHybridSource"
                    , contextTransition.getPrecedingJob().getIdentifier(), "leftHybridTarget", diagramBuilder);

                graph.addEdge(context, contextTransition.getPrecedingJob().getIdentifier());

                addedContexts.add(context);
            });
        });

        return subsequentJobIdentifiers;
    }

    /**
     * Method to get a tree of linked contexts for contexts that have child contexts where job dependencies
     * occur between the parent and any of its children.
     *
     * @param context
     * @param parentContext
     * @param internalEventDrivenJobMap
     * @return
     */
    private Tree<String> getLinkedContexts(Context context, Context parentContext, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        Tree<String> tree = new Tree<>(new TreeNode<>(context.getName()));

        if(!context.getContexts().isEmpty()) {
            Map<String, List<String>> contextSubsequentTransitionMap = new HashMap<>();

            List<String> subsequentTransitions = (List<String>) ContextHelper.determineIfJobsTransitionToOtherContexts
                (parentContext, context.getScheduledJobsMap(), context, internalEventDrivenJobMap)
                .stream()
                .flatMap(contextTransition -> ((ContextTransition)contextTransition).getContexts().stream())
                .filter(contextName -> !contextName.equals(context.getName()))
                .distinct()
                .collect(Collectors.toList());

            context.getContexts().forEach(child -> {
                List<String> childSubsequentTransitions = (List<String>) ContextHelper.determineIfJobsTransitionToOtherContexts
                    (parentContext, ((Context)child).getScheduledJobsMap(), (Context)child, internalEventDrivenJobMap)
                    .stream()
                    .flatMap(contextTransition -> ((ContextTransition)contextTransition).getContexts().stream())
                    .filter(contextName -> !contextName.equals(context.getName()))
                    .distinct()
                    .collect(Collectors.toList());;

                contextSubsequentTransitionMap.put(((Context<?, ?, ?, ?>) child).getName(), childSubsequentTransitions);
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
     * @param internalEventDrivenJobMap
     * @return
     */
    protected List<ContextTransition> getPreviousContextTransitions(Context parentContext, Context context, Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        HashSet<ContextTransition> previousContexts = new HashSet<>();

        context.getScheduledJobs().forEach(job -> {
            List<ContextTransition>  jobsInto = ContextHelper.determineIfJobsTransitionFromOtherContexts(parentContext, ((SchedulerJob)job).getJobName(),
                context.getName(), internalEventDrivenJobMap);

            jobsInto.forEach(jobsOtherContexts -> previousContexts.add(jobsOtherContexts));
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
    protected void addLogicGroupings(VisualisationLogicalGrouping visualisationLogicalGrouping, ArrayList<Object> imageOverlay, Map<String, mxCell> cellMap, DiagramBuilder diagramBuilder) {
        visualisationLogicalGrouping.getNestedGrouping().forEach(nestedVisualisationLogicalGrouping -> {
            this.addLogicGroupings(nestedVisualisationLogicalGrouping, imageOverlay, cellMap, diagramBuilder);

            if(nestedVisualisationLogicalGrouping.getJobIdentifiers().size() > 1 || (!nestedVisualisationLogicalGrouping.getNestedGrouping().isEmpty())) {
                AtomicReference<Double> xMinExtent = new AtomicReference<>();
                xMinExtent.set(-1.0);
                AtomicReference<Double> xMaxExtent = new AtomicReference<>();
                xMaxExtent.set(-1.0);
                AtomicReference<Double> yMinExtent = new AtomicReference<>();
                yMinExtent.set(-1.0);
                AtomicReference<Double> yMaxExtent = new AtomicReference<>();
                yMaxExtent.set(-1.0);

                this.calculateExtents(nestedVisualisationLogicalGrouping, xMinExtent, xMaxExtent, yMinExtent, yMaxExtent, cellMap);

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

                imageOverlay.add(rb.build());
            }
        });
    }

    /**
     * Helper method to draw all transition context boundaries for a given diagram.
     *
     * @param items
     * @param imageOverlay
     * @param cellMap
     * @param diagramBuilder
     */
    protected void addContextBoundaries(ArrayList<Object> items, ArrayList<Object> imageOverlay
        , Map<String, mxCell> cellMap, DiagramBuilder diagramBuilder) {
        items.forEach(item -> {
            if(item instanceof Image) {
                Image image = (Image) item;

                if (image.getUserData() != null
                    && image.getUserData().getItemType() != null
                    && image.getUserData().getItemType().equals(UserData.CONTEXT)) {
                    mxCell cell = cellMap.get(image.getUserData().getIdentifier());

                    this.drawContextBoundary(cell, imageOverlay, image.getUserData(), diagramBuilder, image.getComposite());
                }
            }
        });
    }

    /**
     * Draw a boundary around a transition context icon.
     *
     * @param cell
     * @param imageOverlay
     * @param userData
     */
    protected void drawContextBoundary(mxCell cell, ArrayList<Object> imageOverlay, UserData userData
        , DiagramBuilder diagramBuilder, String groupId) {
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
            .withBgColor(IkasanColours.TRANSPARENT)
            .withColor(IkasanColours.BLACK);

        Rectangle rectangle = rb.build();
        rectangle.setUserData(userData);
        rectangle.setComposite(groupId);

        imageOverlay.add(rectangle);
    }

    /**
     * Method to calculate the x and y coordinate extents of all job within a VisualisationLogicalGrouping.
     *
     * @param visualisationLogicalGrouping
     * @param xMinExtent
     * @param xMaxExtent
     * @param yMinExtent
     * @param yMaxExtent
     * @param cellMap
     */
    protected void calculateExtents(VisualisationLogicalGrouping visualisationLogicalGrouping, AtomicReference<Double> xMinExtent, AtomicReference<Double> xMaxExtent,
                                    AtomicReference<Double> yMinExtent, AtomicReference<Double> yMaxExtent, Map<String, mxCell> cellMap) {
        visualisationLogicalGrouping.getNestedGrouping().forEach(nestedVisualisationLogicalGrouping -> {
            this.calculateExtents(nestedVisualisationLogicalGrouping, xMinExtent, xMaxExtent, yMinExtent, yMaxExtent, cellMap);

            nestedVisualisationLogicalGrouping.getJobIdentifiers().forEach(id -> {
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

        visualisationLogicalGrouping.getJobIdentifiers().forEach(id -> {
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
            image = "frontend/images/global_job.png";
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
            .withPath("frontend/images/external-context.png")
            .withUserData(userData)
            .withLeftPort()
            .withRightPort();

        diagramBuilder.addItem(jobBuilder.build());
    }

    /**
     * Helper method to add a connection between 2 items.
     * @param sourceIdentifier
     * @param sourcePort
     * @param targetIdentifier
     * @param targetPort
     * @param diagramBuilder
     */
    protected void addConnection(String sourceIdentifier, String sourcePort, String targetIdentifier, String targetPort, DiagramBuilder diagramBuilder) {
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
}
