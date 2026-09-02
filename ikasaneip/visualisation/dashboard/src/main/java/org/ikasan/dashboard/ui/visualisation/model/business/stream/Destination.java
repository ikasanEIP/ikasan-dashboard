package org.ikasan.dashboard.ui.visualisation.model.business.stream;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

public class Destination extends Node
{
    /**
     * Constructs a new DestinationImpl object.
     *
     * @param id the unique identifier for the destination, represented as a DesignerItemIdentifier
     * @param name the name of the destination
     * @param x the x-coordinate of the destination
     * @param y the y-coordinate of the destination
     */
    public Destination(DesignerItemIdentifier id, String name, int x, int y)
    {
        super(id, x, y);
    }
}
