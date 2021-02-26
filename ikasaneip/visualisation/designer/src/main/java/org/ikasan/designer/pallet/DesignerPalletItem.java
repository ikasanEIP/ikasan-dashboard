package org.ikasan.designer.pallet;

import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.UUID;

public class DesignerPalletItem extends Image {
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
    public DesignerPalletItem(String imageSrc, DesignerPalletItemType designerPalletItemType, CanvasAddAction canvasAddAction, int itemWidth, int itemHeight) {
        super(imageSrc, "");
        this.identifier = UUID.randomUUID().toString();
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
    public DesignerPalletItem(StreamResource streamResource, DesignerPalletItemType designerPalletItemType, CanvasAddAction canvasAddAction, int itemWidth, int itemHeight) {
        super(streamResource, "");
        this.identifier = UUID.randomUUID().toString();
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

    public String getIdentifier() {
        return identifier;
    }

    public int getItemWidth() {
        return itemWidth;
    }

    public int getItemHeight() {
        return itemHeight;
    }
}
