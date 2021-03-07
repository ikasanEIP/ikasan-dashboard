package org.ikasan.designer.menu;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.designer.Designer;


public class LineContextMenu extends Dialog {

    public LineContextMenu(Designer designer, int x, int y) {
        this.setWidth("200px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", x + "px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", y + "px");

        Image noArrow = new Image("frontend/images/line-arrow-none.png", "");
        noArrow.getElement().getStyle().set("cursor", "pointer");
        noArrow.setWidth("180px");
        noArrow.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            designer.setLineTargetDecorator("NONE");
            designer.setLineSourceDecorator("NONE");
            this.close();
        });

        Image leftArrow = new Image("frontend/images/line-arrow-left.png", "");
        leftArrow.getElement().getStyle().set("cursor", "pointer");
        leftArrow.setWidth("180px");
        leftArrow.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            designer.setLineTargetDecorator("NONE");
            designer.setLineSourceDecorator("ARROW");
            this.close();
        });

        Image rightArrow = new Image("frontend/images/line-arrow-right.png", "");
        rightArrow.getElement().getStyle().set("cursor", "pointer");
        rightArrow.setWidth("180px");
        rightArrow.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            designer.setLineTargetDecorator("ARROW");
            designer.setLineSourceDecorator("NONE");
            this.close();
        });

        Image bothArrow = new Image("frontend/images/line-arrow-both.png", "");
        bothArrow.getElement().getStyle().set("cursor", "pointer");
        bothArrow.setWidth("180px");
        bothArrow.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {
            designer.setLineTargetDecorator("ARROW");
            designer.setLineSourceDecorator("ARROW");
            this.close();
        });

        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.add(noArrow, leftArrow, rightArrow, bothArrow);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, noArrow, leftArrow, rightArrow, bothArrow);

        super.setCloseOnOutsideClick(true);

        this.add(layout);
    }
}
