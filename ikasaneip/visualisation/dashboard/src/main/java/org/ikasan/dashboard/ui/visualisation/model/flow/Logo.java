package org.ikasan.dashboard.ui.visualisation.model.flow;


import org.ikasan.designer.pallet.DesignerItemIdentifier;

/**
 * The Logo class extends the Node class and represents a specific type of node
 * that displays a logo image. The logo is initialized with a predefined image path
 * and default coordinates of -1 for both x and y.
 *
 * This class is typically used to add a visual representation of a logo in a frontend
 * design environment.
 */
public class Logo extends Node
{
	public static final String IMAGE = "frontend/images/ikasan-titling-transparent.png";

	/**
     * Constructs a new Logo object with the specified identifier.
     * The logo is initialized with default coordinates (-1, -1)
     * and a predefined image path.
     *
     * @param id the unique identifier for the designer item represented by this logo
     */
    public Logo(DesignerItemIdentifier id)
	{
        super(id, -1, -1, IMAGE);
	}
}
