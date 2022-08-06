package org.ikasan.designer.event;

public interface FigureDeleteEventListener {

    /**
     * Called when a figureDeleteEvent occurs when a figure is deleted.
     *
     * @param figureDeleteEvent
     */
    void figureDeleted(FigureDeleteEvent figureDeleteEvent);
}
