package org.ikasan.designer.event;

/**
 * Told whenever the user changes the canvas (adds, moves, connects or deletes figures, or undoes/redoes such a
 * change), e.g. so an editor knows its diagram has unsaved changes.
 */
@FunctionalInterface
public interface CanvasEditedListener
{
    /** @param command the draw2d command's label, e.g. "Connect Ports", "Move Shape", "Delete Shape" */
    void canvasEdited(String command);
}
