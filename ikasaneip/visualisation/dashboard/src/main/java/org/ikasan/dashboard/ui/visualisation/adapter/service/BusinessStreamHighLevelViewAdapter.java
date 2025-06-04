package org.ikasan.dashboard.ui.visualisation.adapter.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.CanvasJsonToContextTemplateAdapterException;
import org.ikasan.designer.builder.ConnectionBuilder;
import org.ikasan.designer.builder.DiagramBuilder;
import org.ikasan.designer.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BusinessStreamHighLevelViewAdapter {

    private ObjectMapper objectMapper;
    private DiagramBuilder diagramBuilder;

    public BusinessStreamHighLevelViewAdapter() {
        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.diagramBuilder = new DiagramBuilder();
    }

    /**
     * Adapts the given contextName and canvasJson to a ContextTemplate object.
     *
     * @param canvasJson   the canvas JSON representing the context
     * @return the ContextTemplate object
     * @throws CanvasJsonToContextTemplateAdapterException if there is an error while adapting the canvas JSON
     */
    public String adaptView(String canvasJson) {
        try {
            Map<String, Image> images = new HashMap<>();
            Map<String, List<Connection>> connections = new HashMap<>();
            Map<String, Group> groups = new HashMap<>();

            List<LinkedHashMap> values = objectMapper.readValue(canvasJson, List.class);

            Map<String, List<Item>> compositeItems = this.getAllItemsThatBelongToCompositeGroup(values);

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Image.class);
                    if(!image.getPath().contains("repeating.png"))images.put(image.getId(), image);
                }
                else if (value.get("type").equals("draw2d.Connection")) {
                    Connection connection = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Connection.class);

                    if(!connections.containsKey(connection.getSource().getNode())) {
                        connections.put(connection.getSource().getNode(), new ArrayList<>());
                    }

                    connections.get(connection.getSource().getNode()).add(connection);
                }
                else if (value.get("type").equals("draw2d.shape.composite.Group")) {
                    Group group = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Group.class);
                    groups.put(group.getId(), group);
                }
            }

            // Filter to get integrated systems only
            Map<String, Image> integratedSystem = images.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("INTEGRATED_SYSTEM"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            this.getConnectionsBetweenIntegrationSystems(images, connections);

            List<Object> diagramItems = integratedSystem.values().stream().collect(Collectors.toList());
            diagramItems.addAll(this.diagramBuilder.build());

            this.includeGroupsAndAssociatedCompositeItems(compositeItems, integratedSystem, diagramItems, groups);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(diagramItems);
        }
        catch (Exception e) {
            throw new CanvasJsonToContextTemplateAdapterException(e);
        }
    }

    private void includeGroupsAndAssociatedCompositeItems(Map<String, List<Item>> compositeItems
        , Map<String, Image> integratedSystems, List<Object> diagramItems, Map<String, Group> groups) {
        integratedSystems.values().forEach(integratedSystem -> {
            if(integratedSystem.getComposite() != null && !integratedSystem.getComposite().isEmpty()) {
                diagramItems.add(0, groups.get(integratedSystem.getComposite()));
                compositeItems.get(integratedSystem.getComposite()).forEach(item -> {
                    if(!item.getId().equals(integratedSystem.getId())) {
                        diagramItems.add(item);
                    }
                });
            }
        });
    }

    private Map<String, List<Item>> getAllItemsThatBelongToCompositeGroup(List<LinkedHashMap> values) throws IOException {
        Map<String, List<Item>> results = new HashMap<>();

        for (LinkedHashMap value : values) {
            if (value.get("type").equals("draw2d.shape.basic.Image")) {
                Image image = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Image.class);
                if(image.getComposite() != null && !image.getComposite().isEmpty()) {
                    if(!results.containsKey(image.getComposite())) {
                        results.put(image.getComposite(), new ArrayList<>());
                    }

                    results.get(image.getComposite()).add(image);
                }
            }
            else if (value.get("type").equals("draw2d.shape.basic.Label")) {
                Label label = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Label.class);
                if(label.getComposite() != null && !label.getComposite().isEmpty()) {
                    if(!results.containsKey(label.getComposite())) {
                        results.put(label.getComposite(), new ArrayList<>());
                    }

                    results.get(label.getComposite()).add(label);
                }
            }
        }

        return results;
    }

    private void getConnectionsBetweenIntegrationSystems(Map<String, Image> images, Map<String, List<Connection>> connectionsMap) {
        images.values().forEach(image -> {
            if(connectionsMap.containsKey(image.getId())) {
                List<Connection> connections = connectionsMap.get(image.getId());
                connections.forEach(connection -> this.followConnectionsUntilIntegratedSystem(image, connection, connectionsMap));
            }
        });
    }

    private void followConnectionsUntilIntegratedSystem(Image sourceIntegratedSystem, Connection connection, Map<String, List<Connection>> connectionsMap) {
        if(connection.getTarget().getNode().startsWith("INTEGRATED_SYSTEM")) {
            System.out.println("Got source: " + sourceIntegratedSystem.getId() + "Got target: " + connection.getTarget().getNode());
            ConnectionBuilder connectionBuilder = diagramBuilder.getConnectionBuilder();
            Optional<Connection> sourceConnection = connectionsMap.get(sourceIntegratedSystem.getId()).stream()
                .filter(conn -> conn.getSource().getNode().equals(sourceIntegratedSystem.getId()))
                .findFirst();
            connectionBuilder.withSource(
                diagramBuilder.getConnectionDetailsBuilder()
                    .withNode(sourceIntegratedSystem.getId())
                    .withPort(sourceConnection.get().getSource().getPort())
                    .build()
            );
            connectionBuilder.withTarget(
                diagramBuilder.getConnectionDetailsBuilder()
                    .withNode(connection.getTarget().getNode())
                    .withPort(connection.getTarget().getPort())
                    .withDecoration("draw2d.decoration.connection.ArrowDecorator")
                    .build()
            );

            diagramBuilder.addItem(connectionBuilder.build());
        }
        else if (connectionsMap.containsKey(connection.getTarget().getNode())) {
            connectionsMap.get(connection.getTarget().getNode()).forEach(conn -> {
                this.followConnectionsUntilIntegratedSystem(sourceIntegratedSystem, conn, connectionsMap);
            });
        }
    }

}
