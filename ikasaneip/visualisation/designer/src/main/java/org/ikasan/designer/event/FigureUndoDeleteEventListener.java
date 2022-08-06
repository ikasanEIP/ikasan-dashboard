package org.ikasan.designer.event;

public interface FigureUndoDeleteEventListener {

    /**
     * Called when a figureUndoDeleteEvent occurs when an undo of a figure delete occurs.
     *
     * @param figureUndoDeleteEvent
     */
    void undoFigureDeleted(FigureUndoDeleteEvent figureUndoDeleteEvent);
}
