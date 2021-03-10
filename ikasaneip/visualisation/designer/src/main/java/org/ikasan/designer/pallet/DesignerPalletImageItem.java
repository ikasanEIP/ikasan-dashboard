package org.ikasan.designer.pallet;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

import java.util.UUID;

public abstract class DesignerPalletImageItem extends Image implements DesignerPalletItem<Image>  {
    private String identifier;
    private DesignerPalletItemType designerPalletItemType;
    private CanvasAddAction canvasAddAction;
    private int itemWidth;
    private int itemHeight;

    /**
     * Constructor
     *
     * @param imageSrc
     * @param designerPalletItemType
     * @param canvasAddAction
     * @param itemWidth
     * @param itemHeight
     */
    public DesignerPalletImageItem(String imageSrc, DesignerPalletItemType designerPalletItemType, CanvasAddAction canvasAddAction, int itemWidth, int itemHeight) {
        super(imageSrc, "");
        this.designerPalletItemType = designerPalletItemType;
        this.canvasAddAction = canvasAddAction;
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
    }

    /**
     * Constructor
     *
     * @param streamResource
     * @param designerPalletItemType
     * @param canvasAddAction
     * @param itemWidth
     * @param itemHeight
     */
    public DesignerPalletImageItem(StreamResource streamResource, DesignerPalletItemType designerPalletItemType, CanvasAddAction canvasAddAction, int itemWidth, int itemHeight) {
        super(streamResource, "");
        this.designerPalletItemType = designerPalletItemType;
        this.canvasAddAction = canvasAddAction;
        this.itemWidth = itemWidth;
        this.itemHeight = itemHeight;
    }

    public void executeCanvasAddAction() {
        if(this.canvasAddAction != null) {
            this.canvasAddAction.execute(this);
        }
    }

    public DesignerPalletItemType getDesignerPalletItemType() {
        return designerPalletItemType;
    }

    public DesignerItemIdentifier getIdentifier() {
        return DesignerItemIdentifier.getIdentifier(identifier);
    }

    @Override
    public void setIdentifier(DesignerItemIdentifier identifier) {
        this.identifier = identifier.toString();
    }

    public int getItemWidth() {
        return itemWidth;
    }

    public int getItemHeight() {
        return itemHeight;
    }

    @Override
    public Image getComponent() {
        return this;
    }
}
