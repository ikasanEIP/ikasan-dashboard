package org.ikasan.designer.event;

import org.ikasan.designer.model.Figure;
import org.ikasan.designer.pallet.DesignerPalletImageItem;

public class CanvasItemDoubleClickEvent extends CanvasItemEvent {

    /**
     * Constructor
     *
     * @param designerPalletImageItem
     * @param clickLocationX
     * @param clickLocationY
     * @param figure
     */
    public CanvasItemDoubleClickEvent(DesignerPalletImageItem designerPalletImageItem, int clickLocationX,
                                      int clickLocationY, Figure figure) {
        super(designerPalletImageItem, clickLocationX, clickLocationY, figure);
    }
}
