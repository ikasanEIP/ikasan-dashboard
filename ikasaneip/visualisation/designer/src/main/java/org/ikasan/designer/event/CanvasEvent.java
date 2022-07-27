package org.ikasan.designer.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.designer.model.Connection;
import org.ikasan.designer.model.Image;
import org.ikasan.designer.model.Rectangle;

import java.io.IOException;
import java.util.*;

public class CanvasEvent {

    ObjectMapper objectMapper = new ObjectMapper();

    protected String canvasJson;
    protected Map<String, Image> schedulerJobs;
    protected List<Rectangle> orBoundaries;
    protected List<Rectangle> andBoundaries;
    protected List<Connection> connections;

    public CanvasEvent(String canvasJson) {
        this.canvasJson = canvasJson;
        this.schedulerJobs = new HashMap<>();
        this.orBoundaries = new ArrayList<>();
        this.andBoundaries = new ArrayList<>();
        this.connections = new ArrayList<>();

        init();
    }

    private void init() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            List<LinkedHashMap> values = objectMapper.readValue(this.canvasJson, List.class);

            for (LinkedHashMap value : values) {
                if (value.get("type").equals("draw2d.shape.basic.Image")) {
                    Image image = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Image.class);
                    this.schedulerJobs.put(image.getId(), image);
                }
                else if (value.get("type").equals("draw2d.shape.basic.Rectangle") && value.get("id").toString().startsWith("AND")) {
                    Rectangle rectangle = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Rectangle.class);
                    this.andBoundaries.add(rectangle);
                }
                else if (value.get("type").equals("draw2d.shape.basic.Rectangle") && value.get("id").toString().startsWith("OR")) {
                    Rectangle rectangle = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Rectangle.class);
                    this.orBoundaries.add(rectangle);
                }
                else if (value.get("type").equals("draw2d.Connection")) {
                    Connection connection = objectMapper.readValue(objectMapper.writeValueAsBytes(value), Connection.class);
                    this.connections.add(connection);
                }
            }

        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Image> getSchedulerJobs() {
        return schedulerJobs;
    }

    public List<Rectangle> getOrBoundaries() {
        return orBoundaries;
    }

    public List<Rectangle> getAndBoundaries() {
        return andBoundaries;
    }

    public List<Connection> getConnections() {
        return connections;
    }
}
