package org.ikasan.designer.event;

import org.ikasan.designer.model.Figure;

public class FigureMovedEvent {
    private Figure figure;

    public FigureMovedEvent(Figure figure) {
        this.figure = figure;
    }

    public Figure getFigure() {
        return figure;
    }
}
