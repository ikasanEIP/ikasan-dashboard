package org.ikasan.designer.event;

import org.ikasan.designer.model.Figure;

public class JobMouseOverEvent {
    private Figure figure;

    /**
     * Represents a mouse over event for a figure.
     * This event is triggered when the mouse cursor is moved over a figure.
     *
     * @param figure The Figure that the mouse is over.
     */
    public JobMouseOverEvent(Figure figure) {
        this.figure = figure;
    }

    /**
     * Retrieves the Figure associated with the JobMouseOverEvent.
     *
     * @return The Figure that the mouse is over.
     */
    public Figure getFigure() {
        return figure;
    }
}
