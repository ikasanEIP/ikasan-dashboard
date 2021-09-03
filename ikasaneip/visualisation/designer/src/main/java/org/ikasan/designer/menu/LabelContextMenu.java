package org.ikasan.designer.menu;


import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import org.ikasan.designer.Designer;
import org.ikasan.designer.model.Figure;


public class LabelContextMenu extends Dialog {

    public LabelContextMenu(Designer designer, Figure figure, int x, int y) {
        this.setWidth("250px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", x + "px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", y + "px");

        Select<String> selectFont = new Select<>();
        selectFont.setLabel("Font");
        selectFont.setWidth("90%");

        selectFont.setItems("Arial", "Verdana", "Helvetica", "Tahoma",
            "Trebuchet", "Times", "Georgia", "Garamond",
            "Courier New", "Brush Script");

        selectFont.setValue(figure.getAttributeStringValue("fontFamily"));


        selectFont.addValueChangeListener((HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<Select<String>, String>>)
            selectStringComponentValueChangeEvent -> designer.setFont(selectStringComponentValueChangeEvent.getValue()));

        NumberField fontSize = new NumberField(getTranslation("label.font-size", UI.getCurrent().getLocale()));
        fontSize.setHasControls(true);
        fontSize.setWidth("90%");

        String size = figure.getAttributeStringValue("fontSize");

        fontSize.setValue(Double.parseDouble(size.substring(0, size.length()-2)));

        fontSize.addValueChangeListener((HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<NumberField, Double>>)
            numberFieldDoubleComponentValueChangeEvent -> designer.setFontSize(numberFieldDoubleComponentValueChangeEvent.getValue().intValue() + "pt"));

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setWidthFull();
        layout.add(selectFont, fontSize);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.START, selectFont, fontSize);

        super.setCloseOnOutsideClick(true);

        this.add(layout);
    }
}
