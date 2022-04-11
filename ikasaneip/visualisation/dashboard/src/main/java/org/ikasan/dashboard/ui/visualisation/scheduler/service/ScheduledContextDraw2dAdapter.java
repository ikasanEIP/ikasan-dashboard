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
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScheduledContextDraw2dAdapter {

    Logger logger = LoggerFactory.getLogger(ScheduledContextDraw2dAdapter.class);

    public String adaptJobs(ContextInstance contextInstance) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        DiagramBuilder diagramBuilder = new DiagramBuilder();

        if(contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {
            contextInstance.getScheduledJobs().forEach(job -> {
                graph.addVertex(job.getIdentifier());

                diagramBuilder.addItem(diagramBuilder.getRectangleBuilder().withId(job.getIdentifier())
                    .withBgColor(StatusColours.getInstanceStatusColour(job.getStatus())).build());
            });

            contextInstance.getJobDependencies().forEach(jobDependency -> {
                if (jobDependency.getLogicalGrouping() != null && jobDependency.getLogicalGrouping().getAnd() != null) {
                    jobDependency.getLogicalGrouping().getAnd().forEach(and
                        -> {
                        graph.addEdge(and.getIdentifier(), jobDependency.getJobIdentifier());

                        ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
                        connectionBuilder.withSource(
                            diagramBuilder.getConnectionDetailsBuilder()
                                .withNode(and.getIdentifier())
                                .withPort("hybridSource")
                                .build()
                        );

                        connectionBuilder.withTarget(
                            diagramBuilder.getConnectionDetailsBuilder()
                                .withNode(jobDependency.getJobIdentifier())
                                .withPort("hybridTarget")
                                .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                                .build()
                        );

                        diagramBuilder.addItem(connectionBuilder.build());
                    });
                }
            });


            JGraphXAdapter<Object, DefaultEdge> jGraphXAdapter
                = new JGraphXAdapter<>(graph);

            mxHierarchicalLayout compactTreeLayout = new mxHierarchicalLayout(jGraphXAdapter);
            compactTreeLayout.setOrientation(SwingConstants.WEST);
            compactTreeLayout.setIntraCellSpacing(40);
            compactTreeLayout.setInterHierarchySpacing(200);
            compactTreeLayout.setInterRankCellSpacing(200);
//            compactTreeLayout.setLevelDistance(200);
//            compactTreeLayout.setEdgeRouting(true);true

            compactTreeLayout.execute(jGraphXAdapter.getDefaultParent());

            Map<String, mxCell> cellMap = this.getCellMap(jGraphXAdapter);

            ArrayList<Object> items = diagramBuilder.build();
            ArrayList<Label> labels = new ArrayList<>();

            items.forEach(item -> {
                if (item instanceof Rectangle || item instanceof Image) {
                    mxCell cell = cellMap.get(((Item) item).getId());

                    if (cell != null) {
                        ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                        ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

                        labels.add(new LabelBuilder().withText(((PositionedItem) item).getId())
                            .withX(((PositionedItem) item).getX() - 15)
                            .withY(((PositionedItem) item).getY() + 80)
                            .build());
                    }
                }
            });

            items.addAll(labels);
            //
//            Document image = mxCellRenderer.createSvgDocument(jGraphXAdapter, null, 4, Color.WHITE, null);
//    //        ObjectMapper mapper = new ObjectMapper();
//    //        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
//    //
//    //
//    //        String result = null;
//    //        try {
//    //            result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
//    //            logger.info(result);
//    //        }
//    //        catch (JsonProcessingException e) {
//    //            e.printStackTrace();
//    //        }
//    //
//    ////
//            try {
//                mxUtils.writeFile(mxXmlUtils.getXml(image), "/sandbox/mick/" + contextInstance.getName() + ".svg");
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }


            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


            String result = null;
            // todo clean this up. still a hack.
            try {
                result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
                logger.info(result);
            }
            catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            logger.info(result);
            return result;
        }

        return null;
    }

    public String adaptContext(ContextInstance contextInstance) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        DiagramBuilder diagramBuilder = new DiagramBuilder();

        graph.addVertex(contextInstance.getName());

        RectangleBuilder rectangleBuilder = diagramBuilder.getRectangleBuilder();
        rectangleBuilder.withId(contextInstance.getName())
            .withBgColor(StatusColours.getInstanceStatusColour(contextInstance.getStatus()));

        Rectangle root = rectangleBuilder.build();
        diagramBuilder.addItem(root);

        contextInstance.getContexts().forEach(c -> {
            graph.addVertex(c.getName());
            graph.addEdge(contextInstance.getName(), c.getName());

            RectangleBuilder rb = diagramBuilder.getRectangleBuilder();
            rb.withId(c.getName()).withBgColor(StatusColours.getInstanceStatusColour(c.getStatus()));

            Rectangle branch = rb.build();

            diagramBuilder.addItem(branch);

            ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
            connectionBuilder.withSource(
                diagramBuilder.getConnectionDetailsBuilder()
                .withNode(root.getId())
                .withPort("hybridSource")
                .build()
            );

            connectionBuilder.withTarget(
                diagramBuilder.getConnectionDetailsBuilder()
                    .withNode(branch.getId())
                    .withPort("hybridTarget")
                    .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                    .build()
            );

            diagramBuilder.addItem(connectionBuilder.build());

            this.manageContext(c, graph, diagramBuilder);
        });

        JGraphXAdapter<Object, DefaultEdge> jGraphXAdapter
            = new JGraphXAdapter<>(graph);

        mxCompactTreeLayout compactTreeLayout = new mxCompactTreeLayout(jGraphXAdapter);
        compactTreeLayout.setHorizontal(false);
        compactTreeLayout.setLevelDistance(200);
        compactTreeLayout.setEdgeRouting(true);

        compactTreeLayout.execute(jGraphXAdapter.getDefaultParent());

        Map<String, mxCell> cellMap = this.getCellMap(jGraphXAdapter);

        ArrayList<Object> items = diagramBuilder.build();
        ArrayList<Label> labels = new ArrayList<>();

        items.forEach(item -> {
            if(item instanceof Rectangle || item instanceof Image) {
                mxCell cell = cellMap.get(((Item)item).getId());

                if(cell != null) {
                    ((PositionedItem) item).setX(cell.getGeometry().getX() + 600);
                    ((PositionedItem) item).setY(cell.getGeometry().getY() + 600);

                    labels.add(new LabelBuilder().withText(((PositionedItem)item).getId())
                        .withX(((PositionedItem)item).getX() - 15)
                        .withY(((PositionedItem)item).getY() + 80)
                        .build());
                }
            }
        });

        items.addAll(labels);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


        String result = null;
        // todo clean this up. still a hack.
        try {
            result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
            logger.info(result);
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void manageContext(ContextInstance contextInstance, DefaultDirectedGraph<Object, DefaultEdge> graph,
                               DiagramBuilder diagramBuilder) {

        if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            contextInstance.getContexts().forEach(c -> {
                graph.addVertex(c.getName());
                graph.addEdge(contextInstance.getName(), c.getName());

                RectangleBuilder rectangleBuilder = diagramBuilder.getRectangleBuilder();
                rectangleBuilder.withId(c.getName())
                .withBgColor(StatusColours.getInstanceStatusColour(c.getStatus()));

                Rectangle branch = rectangleBuilder.build();

                diagramBuilder.addItem(branch);

                ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
                connectionBuilder.withSource(
                    diagramBuilder.getConnectionDetailsBuilder()
                        .withNode(contextInstance.getName())
                        .withPort("hybridSource")
                        .build()
                );

                connectionBuilder.withTarget(
                    diagramBuilder.getConnectionDetailsBuilder()
                        .withNode(branch.getId())
                        .withPort("hybridTarget")
                        .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                        .build()
                );

                diagramBuilder.addItem(connectionBuilder.build());

                this.manageContext(c, graph, diagramBuilder);
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
}
