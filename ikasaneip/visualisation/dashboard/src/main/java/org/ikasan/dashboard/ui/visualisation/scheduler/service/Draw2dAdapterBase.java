package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.model.mxCell;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.designer.builder.ConnectionBuilder;
import org.ikasan.designer.builder.DiagramBuilder;
import org.ikasan.designer.builder.RectangleBuilder;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Or;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Draw2dAdapterBase {

    protected ObjectMapper mapper = new ObjectMapper();
    protected DiagramBuilder diagramBuilder = new DiagramBuilder();
    protected double jobMaxXExtent = 0;
    protected double jobMaxYExtent = 0;

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

            if(!nestedGrouping.getJobIdentifiers().isEmpty()) {
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
                    .withTopAndBottomPorts()
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
}
