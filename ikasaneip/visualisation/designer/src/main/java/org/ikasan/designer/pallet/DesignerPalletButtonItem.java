package org.ikasan.designer.pallet;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;


public class DesignerPalletButtonItem extends Button implements DesignerPalletItem<Button> {

    private final DesignerPalletItemType designerPalletItemType = DesignerPalletItemType.ACTION;
    private CanvasAddAction canvasAddAction;

    /**
     * Constructor
     *
     * @param icon
     * @param canvasAddAction
     * @param width
     * @param height
     */
    public DesignerPalletButtonItem(Icon icon, CanvasAddAction canvasAddAction, String width, String height) {
        this.getElement().appendChild(icon.getElement());
        this.canvasAddAction = canvasAddAction;
        this.setWidth(width);
        this.setHeight(height);

    }

    public DesignerPalletItemType getDesignerPalletItemType() {
        return designerPalletItemType;
    }

    public DesignerItemIdentifier getIdentifier() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIdentifier(DesignerItemIdentifier identifier) {
        throw new UnsupportedOperationException();
    }

    public int getItemWidth() {
        throw new UnsupportedOperationException();
    }

    public int getItemHeight() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Button getComponent() {
        return this;
    }
}
