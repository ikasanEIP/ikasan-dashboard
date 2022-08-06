package org.ikasan.designer.event;

import org.ikasan.designer.model.Figure;

public class FigureUndoDeleteEvent {
    private Figure figure;

    public FigureUndoDeleteEvent(Figure figure) {
        this.figure = figure;
    }

    public Figure getFigure() {
        return figure;
    }
}
