package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.ui.visualisation.scheduler.model.Tree;
import org.ikasan.dashboard.ui.visualisation.scheduler.model.TreeNode;
import org.ikasan.designer.model.Connection;
import org.ikasan.designer.model.Image;
import org.ikasan.designer.model.PositionedItem;
import org.ikasan.designer.model.Rectangle;
import org.ikasan.job.orchestration.builder.context.ContextTemplateBuilder;
import org.ikasan.job.orchestration.builder.context.JobDependencyBuilder;
import org.ikasan.job.orchestration.builder.context.LogicalGroupingBuilder;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CanvasJsonToContextTemplateAdapter {

    private ObjectMapper objectMapper;

    public CanvasJsonToContextTemplateAdapter() {
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void validate(String canvasJson) throws CanvasJsonValidationException {
        List<PositionedItem> overlappingItems = new ArrayList<>();

        try {
            List<LinkedHashMap> values = objectMapper.readValue(canvasJson, List.class);
            List<PositionedItem> positionedItems = new ArrayList<>();

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Image.class);
                    positionedItems.add(image);
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

    public boolean withinArea(PositionedItem left, PositionedItem right) {
        if(left.getX() < right.getX() &&
            (left.getX() + left.getWidth()) > (right.getX() + right.getWidth()) &&
            left.getY() < right.getY() &&
            (left.getY() + left.getHeight()) > (right.getY() + right.getHeight())) {
            return true;
        }

        return false;
    }

    public ContextTemplate adapt(String contextName, String canvasJson) {
        Map<String, Image> schedulerJobs = new HashMap<>();
        List<Rectangle> orBoundaries = new ArrayList<>();
        List<Rectangle> andBoundaries = new ArrayList<>();
        Map<String, List<Connection>> connections = new HashMap<>();

        try {
            List<LinkedHashMap> values = objectMapper.readValue(canvasJson, List.class);

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Image.class);
                    schedulerJobs.put(image.getId(), image);
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

            return this.buildContextTemplate(contextName, rootTrees, connections, schedulerJobs);

        }
        catch (Exception e) {
            throw new CanvasJsonToContextTemplateAdapterException(e);
        }
    }

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


            if(intersect.size() == 1) {
                contextTemplateBuilder.addJobDependency(contextTemplateBuilder.getJobDependencyBuilder()
                    .withJobName(schedulerJobs.get(intersect.stream().findFirst().get()).getUserData().getJobName())
                    .withAgentName(schedulerJobs.get(intersect.stream().findFirst().get()).getUserData().getAgentName())
                    .withLogicalGrouping(logicalGrouping)
                    .build());

                // clean up connections that have been used in logical grouping
                connections.entrySet().forEach(entry -> {
                    entry.getValue().removeIf(connection -> connection.getTarget().getNode().equals(intersect.stream().findFirst().get()));
                });
                connections.entrySet().removeIf(e -> e.getValue().size() == 0);
            }
        });

        connections.entrySet().forEach(entry -> {
            entry.getValue().forEach(connection -> {
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
            });
        });

        schedulerJobs.entrySet().forEach(entry -> {
            contextTemplateBuilder.addSchedulerJob(contextTemplateBuilder.getSchedulerJobBuilder()
                .withJobName(entry.getValue().getUserData().getJobName())
                .withAgentName(entry.getValue().getUserData().getAgentName())
                .build());
        });

        return contextTemplateBuilder.build();
    }

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
