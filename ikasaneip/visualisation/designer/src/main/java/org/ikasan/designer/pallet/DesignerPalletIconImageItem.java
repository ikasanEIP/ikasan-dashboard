package org.ikasan.designer.pallet;

import com.vaadin.flow.server.streams.DownloadHandler;

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
     * Constructs a DesignerPalletIconImageItem that represents an icon-based item on the designer pallet.
     *
     * @param downloadHandler the handler used to retrieve or manage the image source as a downloadable resource
     * @param canvasAddAction the action to be executed when the item is added to the canvas
     * @param width the width of the icon in pixels
     * @param height the height of the icon in pixels
     */
    public DesignerPalletIconImageItem(DownloadHandler downloadHandler, CanvasAddAction canvasAddAction, int width, int height) {
        super(downloadHandler, DesignerPalletItemType.ICON, canvasAddAction, width, height);
    }

}
