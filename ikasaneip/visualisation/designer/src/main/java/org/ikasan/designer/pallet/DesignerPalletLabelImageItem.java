package org.ikasan.designer.pallet;

public class DesignerPalletLabelImageItem extends DesignerPalletImageItem {

    /**
     * Constructor
     *
     * @param src
     * @param canvasAddAction
     * @param width
     * @param height
     */
    public DesignerPalletLabelImageItem(String src, CanvasAddAction canvasAddAction, int width, int height) {
        super(src, DesignerPalletItemType.LABEL, canvasAddAction, width, height);
    }

}
