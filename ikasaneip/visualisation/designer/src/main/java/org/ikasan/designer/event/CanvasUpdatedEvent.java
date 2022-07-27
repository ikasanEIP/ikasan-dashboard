package org.ikasan.designer.event;

public class CanvasUpdatedEvent {
    private String canvasJson;

    public CanvasUpdatedEvent(String canvasJson) {
        this.canvasJson = canvasJson;
    }

    public String getCanvasJson() {
        return canvasJson;
    }
}
