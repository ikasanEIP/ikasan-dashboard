package org.ikasan.dashboard.ui.general.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;

//@HtmlImport("frontend://styles/shared-styles.html")
//@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
//@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
//@Theme(themeClass = Material.class)
@PreserveOnRefresh
@Route(value = "pageNotFound")
@UIScope
@org.springframework.stereotype.Component
public class PageNotFoundView extends VerticalLayout
{
    public PageNotFoundView() {
        init();
    }


    private void init()
    {
        Image hospitalImage = new Image("/frontend/images/mr-squid-head.png", "");
        hospitalImage.setHeight("400px");

        H3 hospitalLabel = new H3(getTranslation("error.page-not-found", UI.getCurrent().getLocale()));

        this.setSizeFull();
        this.setSpacing(true);
        this.setMargin(true);
        this.add(hospitalImage, hospitalLabel);
        this.setHorizontalComponentAlignment(Alignment.CENTER, hospitalImage, hospitalLabel);
    }


}
