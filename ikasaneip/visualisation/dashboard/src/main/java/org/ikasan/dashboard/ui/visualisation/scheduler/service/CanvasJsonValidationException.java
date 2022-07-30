package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.designer.model.PositionedItem;

import java.util.List;

public class CanvasJsonValidationException extends Exception {
    private List<PositionedItem> overlappingItems;

    public CanvasJsonValidationException(String message, List<PositionedItem> overlappingItems) {
        super(message);
        this.overlappingItems = overlappingItems;
    }

    public CanvasJsonValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public List<PositionedItem> getOverlappingItems() {
        return overlappingItems;
    }
}
