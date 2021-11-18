package org.ikasan.designer.pallet;

public class DesignerPalletLineImageItem extends DesignerPalletImageItem {

    /**
     * Constructor
     *
     * @param src
     * @param canvasAddAction
     * @param width
     * @param height
     */
    public DesignerPalletLineImageItem(String src, CanvasAddAction canvasAddAction, int width, int height) {
        super(src, DesignerPalletItemType.LINE, canvasAddAction, width, height);
    }

}
