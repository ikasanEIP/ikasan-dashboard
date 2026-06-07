package org.ikasan.dashboard.ui.visualisation.model.flow;

import org.ikasan.designer.pallet.DesignerItemIdentifier;

public class FlowStartup extends Node {

    public static final String FLOW_MANUAL_IMAGE = "frontend/images/flow-manual.png";
    public static final String FLOW_DISABLED_IMAGE = "frontend/images/flow-disabled.png";
    public static final String FLOW_AUTO_IMAGE = "frontend/images/flow-automatic.png";

    /**
     * Constructs a new Node object with the specified identifier, coordinates, and image.
     *
     * @param id    the unique identifier of the node
     * @param x     the x-coordinate of the node
     * @param y     the y-coordinate of the node
     * @param image the image associated with the node
     */
    private FlowStartup(DesignerItemIdentifier id, int x, int y, String image) {
        super(id, x, y, image);
    }
}
