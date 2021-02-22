package org.ikasan.dashboard.ui.home.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.visualisation.component.ModuleFilteringGrid;
import org.ikasan.dashboard.ui.visualisation.component.filter.ModuleSearchFilter;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;

public class StatusWidget extends Div {

    private ModuleFilteringGrid modulesGrid;
    private ModuleMetaDataService moduleMetadataService;

    public StatusWidget(ModuleMetaDataService moduleMetadataService) {
        this.moduleMetadataService = moduleMetadataService;

        this.createStatusView();
    }

    private void createStatusView() {
        this.removeAll();

        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");

        TextField textField = new TextField();
        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);

        Div layout = new Div();
        layout.getElement().getStyle().set("margin-top", "20px");
        layout.getElement().getStyle().set("margin-left", "10px");
        layout.setHeight("50px");
        Label flows = new Label("Flows");
        flows.getElement().getStyle().set("font-size", "16pt");


        textField.getElement().getStyle().set("margin-left", "auto");
        textField.getElement().getStyle().set("float", "right");

        layout.add(flows, textField);

        Div runningDiv = new Div();
        runningDiv.addClassNames("card-counter", "success");
        runningDiv.setHeight("75px");
        runningDiv.setText("10 Running");
        Icon runningIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        runningIcon.getElement().getStyle().set("margin-left", "5px");
        runningIcon.getElement().getStyle().set( "cursor", "pointer");
        runningIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid();
        });
        runningDiv.add(runningIcon);

        Div stoppedDiv = new Div();
        stoppedDiv.addClassNames("card-counter", "primary");
        stoppedDiv.setHeight("75px");
        stoppedDiv.setText("3 Stopped");
        Icon stoppedIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        stoppedIcon.getElement().getStyle().set("margin-left", "5px");
        stoppedIcon.getElement().getStyle().set( "cursor", "pointer");
        stoppedIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid();
        });
        stoppedDiv.add(stoppedIcon);

        Div errorDiv = new Div();
        errorDiv.addClassNames("card-counter", "danger");
        errorDiv.setHeight("75px");
        errorDiv.setText("2 Stopped in Error ");
        Icon errorIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        errorIcon.getElement().getStyle().set("margin-left", "5px");
        errorIcon.getElement().getStyle().set( "cursor", "pointer");
        errorIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid();
        });
        errorDiv.add(errorIcon);

        Div recoveringDiv = new Div();
        recoveringDiv.addClassNames("card-counter", "warning");
        recoveringDiv.setHeight("75px");
        recoveringDiv.setText("1 Recovering ");
        Icon recoveringIcon = VaadinIcon.ARROW_CIRCLE_RIGHT.create();
        recoveringIcon.getElement().getStyle().set("margin-left", "5px");
        recoveringIcon.getElement().getStyle().set( "cursor", "pointer");
        recoveringIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createGrid();
        });
        recoveringDiv.add(recoveringIcon);

        div.add(layout, runningDiv, stoppedDiv, errorDiv, recoveringDiv);

        textField.addValueChangeListener(ev->{
            this.createGrid();
        });

        this.add(div);
    }

    private void createGrid() {
        this.removeAll();
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");

        TextField textField = new TextField();
        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        Div layout = new Div();
        layout.getElement().getStyle().set("margin-top", "20px");
        layout.getElement().getStyle().set("margin-left", "10px");
        layout.setHeight("50px");
        Label flows = new Label("Flows");
        flows.getElement().getStyle().set("font-size", "16pt");


        Icon returnIcon = VaadinIcon.ARROW_CIRCLE_LEFT_O.create();
        returnIcon.getElement().getStyle().set("margin-left", "5px");
        returnIcon.getElement().getStyle().set( "cursor", "pointer");
        returnIcon.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
            this.createStatusView();
        });

        textField.getElement().getStyle().set("margin-left", "auto");
        textField.getElement().getStyle().set("float", "right");

        layout.add(flows, returnIcon, textField);


        // Create a modulesGrid bound to the list
        ModuleSearchFilter moduleSearchFilter = new ModuleSearchFilter();
        modulesGrid = new ModuleFilteringGrid(this.moduleMetadataService, moduleSearchFilter);
        modulesGrid.removeAllColumns();
        modulesGrid.setVisible(true);
        modulesGrid.setWidthFull();
        modulesGrid.setHeight("90%");

        modulesGrid.addColumn(ModuleMetaData::getName)
            .setHeader("Flow Name").setKey("name")
            .setFlexGrow(16);
        modulesGrid.addColumn(TemplateRenderer.<ModuleMetaData>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", ModuleMetaData::getDescription))
            .setHeader(getTranslation("table-header.module-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(32);

        div.add(layout, modulesGrid);

        this.add(div);
    }

}
