package org.ikasan.designer.event;

import org.ikasan.designer.model.Figure;
import org.ikasan.designer.pallet.DesignerPalletImageItem;

public abstract class CanvasItemEvent {
    private DesignerPalletImageItem designerPalletImageItem;
    private int clickLocationX;
    private int clickLocationY;
    private Figure figure;

    protected CanvasItemEvent(DesignerPalletImageItem designerPalletImageItem, int clickLocationX,
                              int clickLocationY, Figure figure) {
        this.designerPalletImageItem = designerPalletImageItem;
        this.clickLocationX = clickLocationX;
        this.clickLocationY = clickLocationY;
        this.figure = figure;
    }

    public DesignerPalletImageItem getItem() {
        return designerPalletImageItem;
    }

    public int getClickLocationX() {
        return clickLocationX;
    }

    public int getClickLocationY() {
        return clickLocationY;
    }

    public Figure getFigure() {
        return figure;
    }
}
