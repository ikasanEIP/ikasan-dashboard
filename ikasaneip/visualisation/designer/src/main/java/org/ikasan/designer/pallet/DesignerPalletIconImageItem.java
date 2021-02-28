package org.ikasan.designer.pallet;

import com.vaadin.flow.server.StreamResource;

public class DesignerPalletIconImageItem extends DesignerPalletImageItem {

    /**
     * Constructor
     *
     * @param src
     * @param canvasAddAction
     * @param width
     * @param height
     */
    public DesignerPalletIconImageItem(String src, CanvasAddAction canvasAddAction, int width, int height) {
        super(src, DesignerPalletItemType.ICON, canvasAddAction, width, height);
    }

    /**
     * Constructor
     *
     * @param streamResource
     * @param canvasAddAction
     * @param width
     * @param height
     */
    public DesignerPalletIconImageItem(StreamResource streamResource, CanvasAddAction canvasAddAction, int width, int height) {
        super(streamResource, DesignerPalletItemType.ICON, canvasAddAction, width, height);
    }

}
