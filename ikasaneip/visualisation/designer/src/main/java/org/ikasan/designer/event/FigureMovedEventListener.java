package org.ikasan.designer.event;

public interface FigureMovedEventListener {

    /**
     * Called when a figureMovedEvent occurs when a figure is moved.
     *
     * @param figureMovedEvent
     */
    void figureMoved(FigureMovedEvent figureMovedEvent);
}
