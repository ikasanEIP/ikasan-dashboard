package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JavaScript;

@JavaScript("https://cdnjs.cloudflare.com/ajax/libs/d3/4.2.2/d3.min.js")
@Tag("map")
public class D3MapView extends Component implements HasSize
{

    public D3MapView()
    {
        this.setId("map-canvas");
    }

    private void initConnector()
    {
        this.setSizeFull();
        com.vaadin.flow.component.page.Page page = getUI()
            .orElseThrow(() -> new IllegalStateException(
                "Connector can only be initialized for an attached D3MapView"))
            .getPage();
        // the connector script is a static resource (frontend/html/map.js), loaded as the designer loads its scripts;
        // it defines window.Vaadin.Flow.mapConnector
        page.addJavaScript("/frontend/html/map.js");
        page.executeJs("window.Vaadin.Flow.mapConnector.initLazy($0)", getElement());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {
        super.onAttach(attachEvent);
        initConnector();
    }
}
