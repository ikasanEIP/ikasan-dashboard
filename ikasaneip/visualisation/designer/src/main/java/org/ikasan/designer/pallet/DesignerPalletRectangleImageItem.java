package org.ikasan.designer.pallet;

public class DesignerPalletRectangleImageItem extends DesignerPalletImageItem {

    /**
     * Constructor
     *
     * @param src
     * @param canvasAddAction
     * @param width
     * @param height
     */
    public DesignerPalletRectangleImageItem(String src, CanvasAddAction canvasAddAction, int width, int height) {
        super(src, DesignerPalletItemType.RECTANGLE, canvasAddAction, width, height);
    }

}
