package org.ikasan.dashboard.ui.visualisation.scheduler.dag.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NpmPackage(value = "@ebay/nice-dag-core", version = "1.0.34")
@NpmPackage(value = "lit-fontawesome", version = "0.1.3")
@CssImport(value = "./css/font.css")
@CssImport(value = "./css/ikasan-dag.css")
@JsModule("./dag-connector-flow.js")
@Tag("dag-chart")
@PreserveOnRefresh
public class DagComponent extends VerticalLayout implements HasSize {

    Logger logger = LoggerFactory.getLogger(DagComponent.class);

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private boolean initialised = false;
    private String dagJson;

    private String ikasanDagNodeStyle = "width: 100%; height: 100%; border: 1px solid #8799c1; " +
        "position: relative; border-radius: 10px; display: flex;flex-direction: column;";

    public DagComponent(String dagData) {
        this.dagJson = dagData;
        this.setWidth("100%");
        this.setHeight("100%");
        this.getStyle().set("display", "block");
        this.getStyle().set("overflow","auto");
    }

    /**
     * This method works in combination with dag-connector-flow.js to set up the
     * integration between the Vaadin framework and nice-dag javascript.
     */
    public void initConnector() {
        getElement().setProperty("dagNodes", this.dagJson);
        getElement().setProperty("ikasanDagNodeStyle", this.ikasanDagNodeStyle);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        initConnector();
    }

    public void styleNode() {
        this.getElement().callJsFunction("styleNode", "start");
    }

    public void zoom(double scale) {
        this.getElement().callJsFunction("zoom", scale);
    }

    @ClientCallable
    public void setDag(String dag) {
        logger.info("Received data: " + dag);
        this.dagJson = dag;
    }

    @ClientCallable
    public void openDiagram(String contextId) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.open();
    }
}
