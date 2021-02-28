package org.ikasan.designer.pallet;

public class DesignerPalletOvalImageItem extends DesignerPalletImageItem {

    /**
     * Constructor
     *
     * @param src
     * @param canvasAddAction
     * @param width
     * @param height
     */
    public DesignerPalletOvalImageItem(String src, CanvasAddAction canvasAddAction, int width, int height) {
        super(src, DesignerPalletItemType.OVAL, canvasAddAction, width, height);
    }

}
