package org.ikasan.designer;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("div")
@NpmPackage(value = "jsvectormap", version = "1.5.2")
@CssImport("jsvectormap/dist/css/jsvectormap.min.css")
@JavaScript("jsvectormap/dist/js/jsvectormap.min.js")
@JavaScript("jsvectormap/dist/maps/world.js")
public class MapCanvas extends VerticalLayout implements BeforeEnterObserver {

    private Logger logger = LoggerFactory.getLogger(MapCanvas.class);

    public MapCanvas() {
        this.setId("ikasan-world-map");
        UI.getCurrent().getPage().addJavaScript("./org/ikasan/jsvectormap/jsvectormap-connector-flow.js");
        this.setSizeFull();
    }

    /**
     * This method works in combination with designer-connector-flow.js to set up the
     * interaction between the Vaadin framework and draw2d javascript.
     */
    private void initConnector() {
        getUI()
            .orElseThrow(() -> new IllegalStateException(
                "Connector can only be initialized for an attached MapCanvas"))
            .getPage()
            .executeJs("window.Vaadin.Flow.jsvectormap.initLazy($0, $1, $2)",
                getElement(), "name", true);
    }

    @ClientCallable
    private void regionSelected(String code) {
        logger.info("Region selected: " + code);
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Region Selected");
        confirmDialog.setText("I could do some work for you");
        confirmDialog.open();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        initConnector();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {

    }
}
