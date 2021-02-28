package org.ikasan.designer.pallet;

import com.vaadin.flow.component.Component;

public interface DesignerPalletItem<COMPONENT extends Component> {

    DesignerPalletItemType getDesignerPalletItemType();

    String getIdentifier();

    int getItemWidth();

    int getItemHeight();

    COMPONENT getComponent();
}
