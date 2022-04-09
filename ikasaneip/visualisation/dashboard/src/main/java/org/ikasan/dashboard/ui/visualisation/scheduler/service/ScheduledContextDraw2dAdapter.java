package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import org.ikasan.designer.builder.DiagramBuilder;
import org.ikasan.designer.builder.RectangleBuilder;
import org.ikasan.designer.model.Item;
import org.ikasan.designer.model.PositionedItem;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScheduledContextDraw2dAdapter {

    Logger logger = LoggerFactory.getLogger(ScheduledContextDraw2dAdapter.class);

    public String adapt(ContextInstance contextInstance) {
        DefaultDirectedGraph<Object, DefaultEdge> graph
            = new DefaultDirectedGraph<>(NoEdgeLabel.class);

        DiagramBuilder diagramBuilder = new DiagramBuilder();

        graph.addVertex(contextInstance.getName());

        RectangleBuilder rectangleBuilder = diagramBuilder.getRectangleBuilder();
        rectangleBuilder.withId(contextInstance.getName());

        diagramBuilder.addItem(rectangleBuilder.build());

        contextInstance.getContexts().forEach(c -> {
            graph.addVertex(c.getName());
            graph.addEdge(contextInstance.getName(), c.getName());

            RectangleBuilder rb = diagramBuilder.getRectangleBuilder();
            rb.withId(c.getName());

            diagramBuilder.addItem(rb.build());

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

        items.forEach(item -> {
            if(item instanceof PositionedItem) {
                mxCell cell = cellMap.get(((Item)item).getId());

                ((PositionedItem)item).setX(cell.getGeometry().getX() + 600);
                ((PositionedItem)item).setY(cell.getGeometry().getY() + 600);
            }
        });

        Document image = mxCellRenderer.createSvgDocument(jGraphXAdapter, null, 4, Color.WHITE, null);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);


        try {
            logger.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items));
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }


        try {
            mxUtils.writeFile(mxXmlUtils.getXml(image), "/sandbox/mick/test.svg");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private void manageContext(ContextInstance contextInstance, DefaultDirectedGraph<Object, DefaultEdge> graph,
                               DiagramBuilder diagramBuilder) {
        if(contextInstance.getScheduledJobs() != null && !contextInstance.getScheduledJobs().isEmpty()) {
//            contextInstance.getScheduledJobs().forEach(job -> {
//                graph.addVertex(contextInstance.getName() + job.getJobName());
//                graph.addEdge(contextInstance.getName(), contextInstance.getName() + job.getJobName());
//            });
        }

        if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            contextInstance.getContexts().forEach(c -> {
                graph.addVertex(c.getName());
                graph.addEdge(contextInstance.getName(), c.getName());

                RectangleBuilder rectangleBuilder = diagramBuilder.getRectangleBuilder();
                rectangleBuilder.withId(c.getName());
                diagramBuilder.addItem(rectangleBuilder.build());

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
