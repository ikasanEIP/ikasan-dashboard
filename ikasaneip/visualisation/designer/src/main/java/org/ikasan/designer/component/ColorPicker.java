package org.ikasan.designer.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.UUID;

@StyleSheet(" /frontend/org/ikasan/color-picker/spectrum.css")
public class ColorPicker extends TextField {

    String identifier;

    public ColorPicker() {
        init();
    }


    private void init(){
        UI.getCurrent().getPage().addJavaScript("/frontend/org/ikasan/draw2d/jquery.js");
        UI.getCurrent().getPage().addJavaScript("/frontend/org/ikasan/draw2d/jquery-ui.js");
        UI.getCurrent().getPage().addJavaScript("/frontend/org/ikasan/color-picker/spectrum.js");
        this.identifier = UUID.randomUUID().toString();
        this.setId("color-picker");

        Icon icon = VaadinIcon.EYEDROPPER.create();
        icon.setSize("18px");
        icon.setColor("rgba(241, 90, 35, 1.0)");
        icon.getElement().getStyle().set("margin-right", "5px");

        this.setPrefixComponent(icon);
        this.setValueChangeMode(ValueChangeMode.TIMEOUT);

        this.getElement().getStyle().set("font-size", "8pt");
        this.setWidth("155px");

        this.attachSpectrum();
    }

    public void attachSpectrum() {
        getElement().executeJs("$('#color-picker').spectrum({\n" +
            "  togglePaletteOnly: \"true\",\n" +
            "    showPalette: \"true\",\n" +
            "    showAlpha: true,\n" +
            "    preferredFormat: \"rgb\",\n" +
            "    showInput: true,  \n" +
            "    showButtons: false,  \n" +
            "});");
    }
}
