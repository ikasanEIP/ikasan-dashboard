package org.ikasan.designer.event;

import org.ikasan.designer.model.Figure;

public class FigureDeleteEvent {
    private Figure figure;

    public FigureDeleteEvent(Figure figure) {
        this.figure = figure;
    }

    public Figure getFigure() {
        return figure;
    }
}
