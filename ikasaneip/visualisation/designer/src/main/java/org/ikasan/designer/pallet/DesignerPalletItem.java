package org.ikasan.designer.pallet;

import com.vaadin.flow.component.Component;

public interface DesignerPalletItem<COMPONENT extends Component> {

    DesignerPalletItemType getDesignerPalletItemType();

    DesignerItemIdentifier getIdentifier();

    void setIdentifier(DesignerItemIdentifier identifier);

    int getItemWidth();

    int getItemHeight();

    COMPONENT getComponent();
}
