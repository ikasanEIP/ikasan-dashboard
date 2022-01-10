package org.ikasan.dashboard.ui.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.icon.Icon;

public class IconDecorator {

    public static Icon decorate(Icon icon, String tooltip, String size, String colour) {
        icon.getStyle().set("cursor", "pointer");
        icon.setSize(size);
        icon.getStyle().set("color", colour);
        icon.getElement().setAttribute("title", tooltip);

        return icon;
    }
}