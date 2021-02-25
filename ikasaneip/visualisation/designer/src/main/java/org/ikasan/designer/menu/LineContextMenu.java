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
        this.setWidth("50px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", x + "px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", y + "px");

        Image noArrow = new Image("frontend/images/line-arrow-none.png", "");
        noArrow.getElement().getStyle().set("cursor", "pointer");
        noArrow.setWidth("200px");
        noArrow.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {

        });

        Image leftArrow = new Image("frontend/images/line-arrow-left.png", "");
        leftArrow.getElement().getStyle().set("cursor", "pointer");
        leftArrow.setWidth("200px");
        leftArrow.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {

        });

        Image rightArrow = new Image("frontend/images/line-arrow-right.png", "");
        rightArrow.getElement().getStyle().set("cursor", "pointer");
        rightArrow.setWidth("200px");
        rightArrow.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {

        });

        Image bothArrow = new Image("frontend/images/line-arrow-both.png", "");
        bothArrow.getElement().getStyle().set("cursor", "pointer");
        bothArrow.setWidth("200px");
        bothArrow.addClickListener((ComponentEventListener<ClickEvent<Image>>) imageClickEvent -> {

        });

//        ColorPicker colorPicker = new ColorPicker();
//        colorPicker.setWidth("200px");


        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.add(noArrow, leftArrow, rightArrow, bothArrow);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, noArrow, leftArrow, rightArrow, bothArrow);

        super.setCloseOnOutsideClick(false);

        this.add(layout);
    }
}
