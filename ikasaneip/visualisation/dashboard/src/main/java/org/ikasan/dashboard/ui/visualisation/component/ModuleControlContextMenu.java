package org.ikasan.dashboard.ui.visualisation.component;


import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


// todo create context menu to control flows
public class ModuleControlContextMenu extends Dialog {

    public ModuleControlContextMenu(int x, int y) {
        this.setWidth("200px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", x + "px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", y + "px");

        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();

        super.setCloseOnOutsideClick(true);

        this.add(layout);
    }
}
