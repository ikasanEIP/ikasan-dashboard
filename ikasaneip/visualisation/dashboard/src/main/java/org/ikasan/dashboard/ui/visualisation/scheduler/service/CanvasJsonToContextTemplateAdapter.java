package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.ui.visualisation.scheduler.model.Tree;
import org.ikasan.dashboard.ui.visualisation.scheduler.model.TreeNode;
import org.ikasan.designer.model.*;
import org.ikasan.job.orchestration.builder.context.ContextTemplateBuilder;
import org.ikasan.job.orchestration.builder.context.JobDependencyBuilder;
import org.ikasan.job.orchestration.builder.context.LogicalGroupingBuilder;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.job.model.JobConstants;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CanvasJsonToContextTemplateAdapter {

    private ObjectMapper objectMapper;

    public CanvasJsonToContextTemplateAdapter() {
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Validates the given canvas JSON for overlapping items.
     *
     * @param canvasJson the canvas JSON to validate
     * @throws CanvasJsonValidationException if the canvas JSON contains overlapping items
     */
    public void validate(String canvasJson) throws CanvasJsonValidationException {
        List<PositionedItem> overlappingItems = new ArrayList<>();

        try {
            List<LinkedHashMap> values = objectMapper.readValue(canvasJson, List.class);
            List<PositionedItem> positionedItems = new ArrayList<>();

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Image.class);
                    if(!image.getPath().contains("repeating.png")) positionedItems.add(image);
                } else if (value.get("type").equals("draw2d.shape.basic.Rectangle") && value.get("id").toString().startsWith("AND")) {
                    Rectangle rectangle = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Rectangle.class);
                    positionedItems.add(rectangle);
                } else if (value.get("type").equals("draw2d.shape.basic.Rectangle") && value.get("id").toString().startsWith("OR")) {
                    Rectangle rectangle = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Rectangle.class);
                    positionedItems.add(rectangle);
                }

            }

            positionedItems.forEach(rectangle -> {
                positionedItems.forEach(positionedItem -> {
                    if(rectangle.getX() < positionedItem.getX() + positionedItem.getWidth() &&
                        rectangle.getX() + rectangle.getWidth() > positionedItem.getX() &&
                        rectangle.getY() < positionedItem.getY() + positionedItem.getHeight() &&
                        rectangle.getY() + rectangle.getHeight() > positionedItem.getY()) {

                        if(!withinArea(rectangle, positionedItem) && !withinArea(positionedItem, rectangle) && !positionedItem.equals(rectangle)) {
                            if(!overlappingItems.contains(rectangle)) {
                                overlappingItems.add(rectangle);
                            }
                        }
                    }
                });
            });
        }
        catch (Exception e) {
           throw new CanvasJsonValidationException("Exception validating canvas JSON!", e);
        }

        if(!overlappingItems.isEmpty()) {
            throw new CanvasJsonValidationException("The canvas JSON contains overlapping items!!", overlappingItems);
        }
    }

    /**
     * Checks if the left PositionedItem is within the area of the right PositionedItem.
     *
     * @param left  the left PositionedItem
     * @param right the right PositionedItem
     * @return true if the left PositionedItem is within the area of the right PositionedItem, otherwise false
     */
    private boolean withinArea(PositionedItem left, PositionedItem right) {
        if(left.getX() < right.getX() &&
            (left.getX() + left.getWidth()) > (right.getX() + right.getWidth()) &&
            left.getY() < right.getY() &&
            (left.getY() + left.getHeight()) > (right.getY() + right.getHeight())) {
            return true;
        }

        return false;
    }

    /**
     * Adapts the parent context template with the provided canvas JSON data
     *
     * @param contextTemplate The original context template to adapt
     * @param canvasJson The JSON data for the canvas
     * @return The adapted context template
     */
    public ContextTemplate adaptParentContextTemplate(ContextTemplate contextTemplate, String canvasJson) {
        Tree<Image> sortedTree = this.adaptParentView(contextTemplate.getName(), canvasJson);

        Map<String, Context> contexts = ContextHelper.getAllContexts(contextTemplate);
        this.setOrdinalsOnContexts(contexts, sortedTree.getRoot());

        return contextTemplate;
    }

    /**
     * Sets ordinals on Context objects based on the order of branches in a TreeNode.
     *
     * @param contexts a Map containing context names as keys and corresponding Context objects as values
     * @param treeNode a TreeNode representing the tree structure with Image data in the branches
     */
    private void setOrdinalsOnContexts(Map<String, Context> contexts, TreeNode<Image> treeNode) {
        for (int i=0; i<treeNode.getBranches().size(); i++) {
            contexts.get(treeNode.getBranches().get(i)
                .getData().getUserData().getContextName()).setOrdinal(i);

            setOrdinalsOnContexts(contexts, treeNode.getBranches().get(i));
        }
    }

    /**
     * Adapts the given contextName and canvasJson to a ContextTemplate object.
     *
     * @param contextName  the name of the context
     * @param canvasJson   the canvas JSON representing the context
     * @return the ContextTemplate object
     * @throws CanvasJsonToContextTemplateAdapterException if there is an error while adapting the canvas JSON
     */
    protected Tree<Image> adaptParentView(String contextName, String canvasJson) {
        Map<String, Image> contexts = new HashMap<>();
        Map<String, List<Connection>> connections = new HashMap<>();

        try {
            List<LinkedHashMap> values = objectMapper.readValue(canvasJson, List.class);

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Image.class);
                    if(!image.getPath().contains("repeating.png"))contexts.put(image.getId(), image);
                }
                else if (value.get("type").equals("draw2d.Connection")) {
                    Connection connection = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Connection.class);

                    if(!connections.containsKey(connection.getSource().getNode())) {
                        connections.put(connection.getSource().getNode(), new ArrayList<>());
                    }

                    connections.get(connection.getSource().getNode()).add(connection);
                }
            }

            Connection parentConnection = connections.get(contextName).get(0);
            TreeNode<Image> parent = new TreeNode<>(contexts.get(parentConnection.getSource().getNode()));
            Tree<Image> contextTree  = new Tree<>(parent);

            this.buildContextTree(parent, contexts, connections);

            return contextTree;
        }
        catch (Exception e) {
            throw new CanvasJsonToContextTemplateAdapterException(e);
        }
    }


    /**
     * Builds a context tree starting from the provided TreeNode and recursively adds branches based on the connections.
     *
     * @param treeNode     the initial TreeNode representing the context
     * @param contexts     a map of context names to Image objects
     * @param connections  a map of context names to lists of Connection objects
     */
    private void buildContextTree(TreeNode<Image> treeNode, Map<String, Image> contexts, Map<String, List<Connection>> connections) {
        if(connections.get(treeNode.getData().getUserData().getContextName()) == null) return;
        connections.get(treeNode.getData().getUserData().getContextName()).forEach(target -> {
            TreeNode<Image> child = new TreeNode<>(contexts.get(target.getTarget().getNode()));
            if(child != null) {
                treeNode.addBranch(child);
                this.buildContextTree(child, contexts, connections);
                treeNode.getBranches().sort((a, b) -> a.getData().getX() < b.getData().getX() ? -1 : 1);
            }
        });
    }

    /**
     * Adapts the given contextName and canvasJson to a ContextTemplate object.
     *
     * @param contextName  the name of the context
     * @param canvasJson   the canvas JSON representing the context
     * @return the ContextTemplate object
     * @throws CanvasJsonToContextTemplateAdapterException if there is an error while adapting the canvas JSON
     */
    public ContextTemplate adapt(String contextName, String canvasJson) {
        Map<String, Image> schedulerJobs = new HashMap<>();
        List<Rectangle> orBoundaries = new ArrayList<>();
        List<Rectangle> andBoundaries = new ArrayList<>();
        Map<String, List<Connection>> connections = new HashMap<>();
        Map<String, Group> externalContexts = new HashMap<>();

        try {
            List<LinkedHashMap> values = objectMapper.readValue(canvasJson, List.class);

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Image.class);
                    if(!image.getPath().contains("repeating.png"))schedulerJobs.put(image.getId(), image);
                }
                else if (value.get("type").equals("draw2d.shape.composite.Group")) {
                    Group group = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Group.class);
                    if(group.getUserData().getItemType() != null && group.getUserData().getItemType().equals("CONTEXT")) {
                        externalContexts.put(group.getUserData().getContextName(), group);
                    }
                }
                else if (value.get("type").equals("draw2d.shape.basic.Rectangle") && value.get("id").toString().startsWith("AND")) {
                    Rectangle rectangle = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Rectangle.class);
                    rectangle.setResizable(true);
                    andBoundaries.add(rectangle);
                }
                else if (value.get("type").equals("draw2d.shape.basic.Rectangle") && value.get("id").toString().startsWith("OR")) {
                    Rectangle rectangle = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Rectangle.class);
                    rectangle.setResizable(true);
                    orBoundaries.add(rectangle);
                }
                else if (value.get("type").equals("draw2d.Connection")) {
                    Connection connection = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Connection.class);

                    if(!connections.containsKey(connection.getSource().getNode())) {
                        connections.put(connection.getSource().getNode(), new ArrayList<>());
                    }

                    connections.get(connection.getSource().getNode()).add(connection);
                }
            }

            List<Rectangle> aggregatedLogicRectangles = new ArrayList<>();
            aggregatedLogicRectangles.addAll(orBoundaries);
            aggregatedLogicRectangles.addAll(andBoundaries);

            aggregatedLogicRectangles.sort(Comparator.comparingDouble(Rectangle::getY));

            Map<String, Tree<PositionedItem>> logicNestingTrees = new HashMap<>();

            aggregatedLogicRectangles.forEach(rectangle -> {
                Tree<PositionedItem> nestedChildren = new Tree<>(new TreeNode<>(rectangle));
                this.buildTree(nestedChildren.getRoot(), rectangle, aggregatedLogicRectangles);

                logicNestingTrees.put(rectangle.getId(), nestedChildren);
            });

            List<Tree<PositionedItem>> rootTrees = this.getRootTrees(logicNestingTrees);

            rootTrees.forEach(entry -> {
                entry.pruneTree();
                this.addJobsToTree(entry.getRoot(), schedulerJobs);
            });

            ContextTemplate c = this.buildContextTemplate(contextName, rootTrees, connections, schedulerJobs);

            Map<String, List<JobDependency>> jdMap = new HashMap<>();
            c.getJobDependencies().forEach(jobDependency -> {
                if(jdMap.containsKey(jobDependency.getJobIdentifier())) {
                    jdMap.get(jobDependency.getJobIdentifier()).add(jobDependency);
                }
                else {
                    ArrayList jobDependencies = new ArrayList();
                    jobDependencies.add(jobDependency);
                    jdMap.put(jobDependency.getJobIdentifier(), jobDependencies);
                }
            });

            // This fragment of code detects where there are parallel
            // jobs that have not be placed in an AND job grouping and
            // applies the relevant logic to wrap them up in the appropriate
            // logical grouping. In effect making the default behaviour being
            // that jobs that are defined concurrently will run in a logical
            // AND.
            jdMap.entrySet().forEach(jobDependencies -> {
                if(jobDependencies.getValue().size() > 1) {
                    AtomicBoolean allSingleAnds = new AtomicBoolean(true);

                    jobDependencies.getValue().forEach(jobDependency -> {
                        if(jobDependency.getLogicalGrouping() != null
                            && jobDependency.getLogicalGrouping().getAnd() != null
                            && jobDependency.getLogicalGrouping().getAnd().size() > 1) {
                            allSingleAnds.set(false);
                        }
                    });

                    if(allSingleAnds.get()) {
                        ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();
                        JobDependencyBuilder jobDependencyBuilder = contextTemplateBuilder.getJobDependencyBuilder();
                        jobDependencyBuilder.withAgentName(jobDependencies.getKey()
                            .substring(0, jobDependencies.getKey().indexOf("-")));
                        jobDependencyBuilder.withJobName(jobDependencies.getKey()
                            .substring(jobDependencies.getKey().indexOf("-") + 1, jobDependencies.getKey().length()));

                        LogicalGroupingBuilder logicalGroupingBuilder = contextTemplateBuilder.getLogicalGroupingBuilder();

                        jobDependencies.getValue().forEach(jobDependency
                            -> jobDependency.getLogicalGrouping().getAnd().forEach(and -> logicalGroupingBuilder.addAnd(and)));

                        jobDependencyBuilder.withLogicalGrouping(logicalGroupingBuilder.build());

                        c.setJobDependencies(c.getJobDependencies().stream()
                            .filter(jobDependency -> !jobDependency.getJobIdentifier().equals(jobDependencies.getKey()))
                            .collect(Collectors.toList()));

                        c.getJobDependencies().add(jobDependencyBuilder.build());
                    }
                }
            });

            return c;

        }
        catch (Exception e) {
            throw new CanvasJsonToContextTemplateAdapterException(e);
        }
    }

    /**
     * Builds a ContextTemplate object based on the provided parameters.
     *
     * @param contextName     the name of the context
     * @param rootTrees       the list of root trees representing the context
     * @param connections     the map of connections between nodes
     * @param schedulerJobs   the map of scheduler jobs
     * @return the ContextTemplate object
     */
    private ContextTemplate buildContextTemplate(String contextName, List<Tree<PositionedItem>> rootTrees, Map<String, List<Connection>> connections
        , Map<String, Image> schedulerJobs) {
        ContextTemplateBuilder contextTemplateBuilder = new ContextTemplateBuilder();
        contextTemplateBuilder.withName(contextName);
        Map<String, JobDependencyBuilder> jobDependencyBuilderMap = new HashMap<>();

        connections.entrySet().forEach(entry -> {
            entry.getValue().forEach(connection -> {
                jobDependencyBuilderMap.put(connection.getTarget().getNode(),
                    contextTemplateBuilder.getJobDependencyBuilder()
                        .withAgentName(connection.getTarget().getNode())
                        .withJobName(connection.getTarget().getNode()));
            });
        });

        List<LogicalGrouping> logicalGroupings = new ArrayList<>();

        rootTrees.forEach(tree -> logicalGroupings.add(this.manageLogicalGrouping
            (tree.getRoot(), contextTemplateBuilder, connections, jobDependencyBuilderMap)));

        logicalGroupings.forEach(logicalGrouping -> {
            List<String> identifiers = new ArrayList<>();
            this.getTargetJobForLogicalGrouping(logicalGrouping, identifiers);

            List<List<String>> all = new ArrayList<>();
            identifiers.forEach(identifier -> {
                if(connections.containsKey(identifier)) {
                    all.add(connections.get(identifier)
                        .stream()
                        .map(connection -> connection.getTarget().getNode())
                        .collect(Collectors.toList()));
                }
            });

            Set<String> intersect = this.intersect(all);

            intersect.forEach(identifier -> {
                contextTemplateBuilder.addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                    .withJobName(schedulerJobs.get(identifier).getUserData().getJobName())
                    .withAgentName(schedulerJobs.get(identifier).getUserData().getAgentName())
                    .withLogicalGrouping(logicalGrouping)
                    .build());

                // clean up connections that have been used in logical grouping
                connections.entrySet().forEach(entry -> {
                    entry.getValue().removeIf(connection -> connection.getTarget().getNode().equals(identifier));
                });
                connections.entrySet().removeIf(e -> e.getValue().size() == 0);
            });
        });

        Map<String, List<Connection>> inboundContexts = connections.entrySet().stream()
            .filter(entry -> entry.getKey().endsWith("_in"))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (existing, replacement) -> existing));

        Optional<Image> startJob = schedulerJobs.values().stream()
            .filter(job -> job.getUserData().getAgentName() != null
                && job.getUserData().getAgentName().equals(JobConstants.CONTEXT_START_JOB))
            .findFirst();

        if(startJob.isPresent() && !inboundContexts.isEmpty()) {
            JobDependencyBuilder jobDependencyBuilder = contextTemplateBuilder.getJobDependencyBuilder();
            jobDependencyBuilder.withJobName(startJob.get().getUserData().getJobName())
                .withAgentName(startJob.get().getUserData().getAgentName());

            LogicalGroupingBuilder logicalGroupingBuilder = contextTemplateBuilder.getLogicalGroupingBuilder();

            inboundContexts.values().forEach(ibc -> ibc.forEach(connection -> {
                logicalGroupingBuilder.addAnd(contextTemplateBuilder.getJobAndBuilder()
                    .withAgentName(JobConstants.CONTEXT_TERMINAL_JOB)
                    .withJobName(schedulerJobs.get(connection.getSource().getNode()).getUserData().getPreviousJobIdentifiers().get(0))
                    .build());

                contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                    .withJobName(schedulerJobs.get(connection.getSource().getNode()).getUserData().getPreviousJobIdentifiers().get(0))
                    .withAgentName(JobConstants.CONTEXT_TERMINAL_JOB)
                    .build());
            }));

            jobDependencyBuilder.withLogicalGrouping(logicalGroupingBuilder.build());
            contextTemplateBuilder.addJobDependency(jobDependencyBuilder.build());
        }

        inboundContexts.keySet().forEach(key -> connections.remove(key));

        connections.entrySet().forEach(entry -> {
            entry.getValue().forEach(connection -> {
                if(schedulerJobs.get(connection.getTarget().getNode()).getUserData().getJobName() != null &&
                    schedulerJobs.get(connection.getTarget().getNode()).getUserData().getAgentName() != null &&
                    schedulerJobs.get(entry.getKey()).getUserData().getAgentName() != null &&
                    schedulerJobs.get(entry.getKey()).getUserData().getJobName() != null) {
                    contextTemplateBuilder.addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                        .withJobName(schedulerJobs.get(connection.getTarget().getNode()).getUserData().getJobName())
                        .withAgentName(schedulerJobs.get(connection.getTarget().getNode()).getUserData().getAgentName())
                        .withLogicalGrouping(contextTemplateBuilder.getLogicalGroupingBuilder()
                            .addAnd(contextTemplateBuilder.getJobAndBuilder()
                                .withAgentName(schedulerJobs.get(entry.getKey()).getUserData().getAgentName())
                                .withJobName(schedulerJobs.get(entry.getKey()).getUserData().getJobName())
                                .build())
                            .build())
                        .build());
                }
            });
        });

        schedulerJobs.entrySet().forEach(entry -> {
            if(entry.getValue().getUserData().getJobName() != null &&
                entry.getValue().getUserData().getAgentName() != null) {
                contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                    .withJobName(entry.getValue().getUserData().getJobName())
                    .withAgentName(entry.getValue().getUserData().getAgentName())
                    .build());
            }
        });

        return contextTemplateBuilder.build();
    }

    /**
     * Manages the logical grouping of items in a tree node.
     *
     * @param node                       the tree node containing the items
     * @param contextTemplateBuilder     the context template builder
     * @param connections                the map of connections between nodes
     * @param jobDependencyBuilderMap    the map of job dependency builders
     * @return the logical grouping after managing the items
     */
    private LogicalGrouping manageLogicalGrouping(TreeNode<PositionedItem> node, ContextTemplateBuilder contextTemplateBuilder, Map<String, List<Connection>> connections,
                                       Map<String, JobDependencyBuilder> jobDependencyBuilderMap) {
        LogicalGroupingBuilder logicalGroupingBuilder = contextTemplateBuilder.getLogicalGroupingBuilder();

        if(node.getData().getId().startsWith("AND")) {
            this.manageAnd(logicalGroupingBuilder, node.getBranches(), contextTemplateBuilder, connections, jobDependencyBuilderMap);
        }
        else if(node.getData().getId().startsWith("OR")) {
            this.manageOr(logicalGroupingBuilder, node.getBranches(), contextTemplateBuilder, connections, jobDependencyBuilderMap);
        }

        return logicalGroupingBuilder.build();
    }

    /**
     * Recursively retrieves the target job identifiers for a LogicalGrouping.
     *
     * @param logicalGrouping - the LogicalGrouping object to retrieve target job identifiers from
     * @param identifiers - a list to store the target job identifiers
     */
    private void getTargetJobForLogicalGrouping(LogicalGrouping logicalGrouping, List<String> identifiers) {
        if(logicalGrouping.getOr() != null) {
            logicalGrouping.getOr().forEach(or -> {
                if(or.getIdentifier() != null) {
                    identifiers.add(or.getIdentifier());
                }

                if(or.getLogicalGrouping() != null) {
                    getTargetJobForLogicalGrouping(or.getLogicalGrouping(), identifiers);
                }
            });
        }

        if(logicalGrouping.getAnd() != null) {
            logicalGrouping.getAnd().forEach(and -> {
                if(and.getIdentifier() != null) {
                    identifiers.add(and.getIdentifier());
                }

                if(and.getLogicalGrouping() != null) {
                    getTargetJobForLogicalGrouping(and.getLogicalGrouping(), identifiers);
                }
            });
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            getTargetJobForLogicalGrouping(logicalGrouping.getLogicalGrouping(), identifiers);
        }
    }

    /**
     * Manages the logical grouping of items in a tree node by adding 'AND' logical groupings to the ContextTemplateBuilder.
     *
     * @param logicalGroupingBuilder The LogicalGroupingBuilder object to add the 'AND' logical groupings to.
     * @param branches               The list of TreeNode objects representing the branches of the tree node.
     * @param contextTemplateBuilder The ContextTemplateBuilder object to retrieve and build the JobAndBuilder with.
     * @param connections            The map of connections between nodes.
     * @param jobDependencyBuilderMap The map of job dependency builders.
     */
    private void manageAnd(LogicalGroupingBuilder logicalGroupingBuilder, List<TreeNode<PositionedItem>> branches, ContextTemplateBuilder contextTemplateBuilder,
                           Map<String, List<Connection>> connections, Map<String, JobDependencyBuilder> jobDependencyBuilderMap) {
        branches.forEach(branch -> {
            if(branch.isLeaf()) {
                logicalGroupingBuilder.addAnd(contextTemplateBuilder.getJobAndBuilder()
                    .withAgentName(branch.getData().getUserData().getAgentName())
                    .withJobName(branch.getData().getUserData().getJobName())
                    .build());
            }
            else {
                if(branch.getData().getId().startsWith("AND")) {
                    LogicalGroupingBuilder andLogicalGroupingBuilder = contextTemplateBuilder.getLogicalGroupingBuilder();
                    this.manageAnd(andLogicalGroupingBuilder, branch.getBranches(), contextTemplateBuilder, connections, jobDependencyBuilderMap);
                    logicalGroupingBuilder.addAnd(contextTemplateBuilder.getJobAndBuilder().withLogicalGrouping(andLogicalGroupingBuilder.build()).build());
                }
                else if(branch.getData().getId().startsWith("OR")) {
                    LogicalGroupingBuilder orLogicalGroupingBuilder = contextTemplateBuilder.getLogicalGroupingBuilder();
                    manageOr(orLogicalGroupingBuilder, branch.getBranches(), contextTemplateBuilder, connections, jobDependencyBuilderMap);
                    logicalGroupingBuilder.addAnd(contextTemplateBuilder.getJobAndBuilder().withLogicalGrouping(orLogicalGroupingBuilder.build()).build());
                }
            }
        });
    }

    /**
     * Manages the logical grouping of items in a tree node by adding 'OR' logical groupings to the ContextTemplateBuilder.
     *
     * @param logicalGroupingBuilder   The LogicalGroupingBuilder object to add the 'OR' logical groupings to.
     * @param branches                 The list of TreeNode objects representing the branches of the tree node.
     * @param contextTemplateBuilder   The ContextTemplateBuilder object to retrieve and build the JobOrBuilder with.
     * @param connections              The map of connections between nodes.
     * @param jobDependencyBuilderMap  The map of job dependency builders.
     */
    private void manageOr(LogicalGroupingBuilder logicalGroupingBuilder, List<TreeNode<PositionedItem>> branches, ContextTemplateBuilder contextTemplateBuilder,
                           Map<String, List<Connection>> connections, Map<String, JobDependencyBuilder> jobDependencyBuilderMap) {
        branches.forEach(branch -> {
            if(branch.isLeaf()) {
                logicalGroupingBuilder.addOr(contextTemplateBuilder.getJobOrBuilder()
                    .withAgentName(branch.getData().getUserData().getAgentName())
                    .withJobName(branch.getData().getUserData().getJobName())
                    .build());
            }
            else {
                if(branch.getData().getId().startsWith("AND")) {
                    LogicalGroupingBuilder andLogicalGroupingBuilder = contextTemplateBuilder.getLogicalGroupingBuilder();
                    this.manageAnd(andLogicalGroupingBuilder, branch.getBranches(), contextTemplateBuilder, connections, jobDependencyBuilderMap);
                    logicalGroupingBuilder.addOr(contextTemplateBuilder.getJobOrBuilder().withLogicalGrouping(andLogicalGroupingBuilder.build()).build());
                }
                else if(branch.getData().getId().startsWith("OR")) {
                    LogicalGroupingBuilder orLogicalGroupingBuilder = contextTemplateBuilder.getLogicalGroupingBuilder();
                    manageOr(orLogicalGroupingBuilder, branch.getBranches(), contextTemplateBuilder, connections, jobDependencyBuilderMap);
                    logicalGroupingBuilder.addOr(contextTemplateBuilder.getJobOrBuilder().withLogicalGrouping(orLogicalGroupingBuilder.build()).build());
                }
            }
        });
    }

    /**
     * Helper method to build a tree. The purpose of this class is to convert a visual model to a logical model. The visual
     * model is represented on a 2 dimensional reference frame (x and y) and the construction of the tree is performed by
     * determining the relative position of nested boundaries within that reference frame. Recursion is the obvious choice
     * when working with hierarchies of data such as nested logical groupings, and it is employed to build the tree.
     *
     * @param node
     * @param parent
     * @param potentialChildren
     */
    private void buildTree(TreeNode<PositionedItem> node, Rectangle parent, List<Rectangle> potentialChildren) {
        potentialChildren.forEach(potentialChild -> {
            if(parent.getX() < potentialChild.getX() &&
                (parent.getX() + parent.getWidth()) > (potentialChild.getX() + potentialChild.getWidth()) &&
                parent.getY() < potentialChild.getY() &&
                (parent.getY() + parent.getHeight()) > (potentialChild.getY() + potentialChild.getHeight())) {

                TreeNode<PositionedItem> child = new TreeNode<>(potentialChild);
                node.addBranch(child);

                this.buildTree(child, potentialChild, potentialChildren);
            }
        });
    }

    /**
     * Helper method to get all trees that are deemed to be root trees.
     *
     * @param allTrees
     * @return
     */
    private List<Tree<PositionedItem>> getRootTrees(Map<String, Tree<PositionedItem>> allTrees) {
        List<Tree<PositionedItem>> finalRootTrees = new ArrayList<>();

        allTrees.entrySet().forEach(entry -> {
            if(this.isRootTree(entry.getValue(), allTrees)) {
                finalRootTrees.add(entry.getValue());
            }
        });

        return finalRootTrees;
    }

    /**
     * Helper method to determine if a tree is a root tree. A tree is deemed to be a root tree if it
     * does not appear in another tree.
     *
     * @param potentialRoot
     * @param allTrees
     * @return
     */
    private boolean isRootTree(Tree<PositionedItem> potentialRoot, Map<String, Tree<PositionedItem>> allTrees) {
        AtomicBoolean isRootTree = new AtomicBoolean(true);

        allTrees.values().forEach(tree -> {
            if (tree.containsTree(potentialRoot)) {
                isRootTree.set(false);
            }
        });

        return isRootTree.get();
    }

    /**
     * Helper method to add all the jobs to the tree. Once again this method employs recursion and also determines if
     * a job falls withing a logical boundary based upon the 2 dimensional reference frames it resides within on the
     * canvas.
     *
     * @param node
     * @param schedulerJobs
     */
    private void addJobsToTree(TreeNode<PositionedItem> node, Map<String, Image> schedulerJobs) {
        if(node.isLeaf()) {
            this.addJobsIfWithinBoundary(node, schedulerJobs);
        }
        else {
            this.addJobsIfWithinBoundaryAndNotInAnyChildren(node, schedulerJobs).forEach(positionedItem
                -> node.addBranch(new TreeNode<>(positionedItem)));
        }

        node.getBranches().forEach(branch -> {
            if(branch.isLeaf()) {
                this.addJobsIfWithinBoundary(branch, schedulerJobs);
            }
            else {
                this.addJobsToTree(branch, schedulerJobs);
            }
        });
    }

    /**
     * Helper method to add a job to a node if the job is positioned within the rectangle's boundary that is
     * associated with the node.
     *
     * @param node
     * @param schedulerJobs
     */
    private void addJobsIfWithinBoundary(TreeNode<PositionedItem> node, Map<String, Image> schedulerJobs) {
        schedulerJobs.entrySet().forEach(entry -> {
            if(node.getData().getX() < entry.getValue().getX() &&
                (node.getData().getX() + node.getData().getWidth()) > (entry.getValue().getX() + entry.getValue().getWidth()) &&
                node.getData().getY() < entry.getValue().getY() &&
                (node.getData().getY() + node.getData().getHeight()) > (entry.getValue().getY() + entry.getValue().getHeight())) {
                node.addBranch(new TreeNode<>(entry.getValue()));
            }
        });
    }


    /**
     * Adds jobs to the withinThisBoundary list if they are within the boundary of the given node
     * and not already present in any children of the node.
     *
     * @param node           the tree node to check for jobs within its boundary
     * @param schedulerJobs  the map of scheduler jobs
     * @return the list of jobs within the boundary and not in any children of the node
     */
    private ArrayList<PositionedItem> addJobsIfWithinBoundaryAndNotInAnyChildren(TreeNode<PositionedItem> node, Map<String, Image> schedulerJobs) {
        ArrayList<PositionedItem> withinThisBoundary = new ArrayList<>();
        schedulerJobs.entrySet().forEach(entry -> {
            if(node.getData().getX() < entry.getValue().getX() &&
                (node.getData().getX() + node.getData().getWidth()) > (entry.getValue().getX() + entry.getValue().getWidth()) &&
                node.getData().getY() < entry.getValue().getY() &&
                (node.getData().getY() + node.getData().getHeight()) > (entry.getValue().getY() + entry.getValue().getHeight())) {
                withinThisBoundary.add(entry.getValue());
            }
        });

        node.getBranches().forEach(branch -> {
            removeJobIfWithinChildChild(branch, withinThisBoundary, schedulerJobs);
        });

        return withinThisBoundary;
    }

    /**
     * Removes scheduler jobs from the withinThisBoundary list if they are within the boundary of the given node.
     * It recursively checks all the child nodes of the given node.
     *
     * @param node                 the tree node to check for jobs within its boundary
     * @param withinThisBoundary   the list of jobs within the boundary
     * @param schedulerJobs        the map of scheduler jobs
     */
    private void removeJobIfWithinChildChild(TreeNode<PositionedItem> node, ArrayList<PositionedItem> withinThisBoundary, Map<String, Image> schedulerJobs) {
        schedulerJobs.entrySet().forEach(entry -> {
            if(node.getData().getX() < entry.getValue().getX() &&
                (node.getData().getX() + node.getData().getWidth()) > (entry.getValue().getX() + entry.getValue().getWidth()) &&
                node.getData().getY() < entry.getValue().getY() &&
                (node.getData().getY() + node.getData().getHeight()) > (entry.getValue().getY() + entry.getValue().getHeight())) {
                withinThisBoundary.remove(entry.getValue());
            }
        });

        if(!node.isLeaf()) {
            node.getBranches().forEach(branch -> {
                removeJobIfWithinChildChild(branch, withinThisBoundary, schedulerJobs);
            });
        }
    }

    /**
     * Returns the intersection of all sets of strings in the given list of connections.
     *
     * @param connections the list of connections, each containing a list of strings
     * @return the set containing the intersection of all sets of strings
     */
    public Set<String> intersect(List<List<String>> connections) {
        if(connections.isEmpty()) {
            return Set.of();
        }

        HashSet<String> intersectionSet = new HashSet<>(connections.get(0));

        for (int i = 1; i < connections.size(); i++)
        {
            HashSet<String> set = new HashSet<>(connections.get(i));

            intersectionSet.retainAll(set);
        }

        return intersectionSet;
    }
}
