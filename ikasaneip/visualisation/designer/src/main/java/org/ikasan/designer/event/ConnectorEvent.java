package org.ikasan.designer.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectorEvent {
    public static final String CONNECTOR_ADDED_EVENT_TYPE = "CONNECTOR_ADDED";
    public static final String CONNECTOR_REMOVED_EVENT_TYPE = "CONNECTOR_REMOVED";

    private String canvasJson;
    private String eventType;
    private String sourceFigureId;
    private String targetFigureId;

    /**
     * Constructor
     *
     * @param eventType
     * @param sourceFigureId
     * @param targetFigureId
     */
    public ConnectorEvent(@JsonProperty("canvasJson") String canvasJson, @JsonProperty("eventType") String eventType, @JsonProperty("sourceFigureId") String sourceFigureId
        , @JsonProperty("targetFigureId") String targetFigureId) {
        this.canvasJson = canvasJson;
        this.eventType = eventType;
        this.sourceFigureId = sourceFigureId;
        this.targetFigureId = targetFigureId;
    }

    public String getCanvasJson() {
        return canvasJson;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSourceFigureId() {
        return sourceFigureId;
    }

    public String getTargetFigureId() {
        return targetFigureId;
    }
}
