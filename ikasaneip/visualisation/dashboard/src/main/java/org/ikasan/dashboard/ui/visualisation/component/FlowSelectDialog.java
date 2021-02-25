package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.visualisation.component.filter.FlowSearchFilter;
import org.ikasan.dashboard.ui.visualisation.model.designer.business.stream.Flow;
import org.ikasan.spec.metadata.ModuleMetaDataService;

public class FlowSelectDialog extends AbstractCloseableResizableDialog {

    private FlowFilteringGrid flowsGrid;
    private ModuleMetaDataService moduleMetadataService;
    private TextField textField;
    private Flow flow = null;

    public FlowSelectDialog(ModuleMetaDataService moduleMetadataService) {
        super.title.setText("Please select a flow");
        this.moduleMetadataService = moduleMetadataService;
        this.textField = new TextField();
        this.createGrid();

        Div div = new Div();
        div.getElement().getStyle().set("margin-top", "0px");
        div.getElement().getStyle().set("margin-left", "15px");
        div.getElement().getStyle().set("margin-right", "15px");
        div.getElement().getStyle().set("margin-bottom", "15px");
        div.setHeight("500px");

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4("Flows");
        layout.add(modules, textField);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, textField);

        textField.getElement().getStyle().set("margin-left", "auto");

        div.add(layout);
        div.add(this.flowsGrid);

        this.flowsGrid.init();

        super.content.removeClassName("dialog-content");
        super.content.setAlignItems(FlexComponent.Alignment.START);

        super.showResize(false);

        this.add(div);
        this.setHeight("550px");
        this.setWidth("700px");
    }

    private void createGrid() {
        // Create a modulesGrid bound to the list
        FlowSearchFilter flowSearchFilter = new FlowSearchFilter();
        flowsGrid = new FlowFilteringGrid(this.moduleMetadataService, flowSearchFilter);
        flowsGrid.removeAllColumns();
        flowsGrid.setVisible(true);
        flowsGrid.setWidthFull();
        flowsGrid.setHeight("80%");

        flowsGrid.addColumn(Flow::getModuleName)
            .setHeader(getTranslation("table-header.module-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(16);
        flowsGrid.addColumn(TemplateRenderer.<Flow>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", Flow::getFlowName))
            .setHeader(getTranslation("table-header.flow-name", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(32);

        this.flowsGrid.addGridFiltering(textField, flowSearchFilter::setModuleNameFilter);
        this.flowsGrid.addGridFiltering(textField, flowSearchFilter::setFlowNameFilter);

        flowsGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<Flow>>)
            doubleClickEvent -> {
                this.flow = doubleClickEvent.getItem();
                this.close();
            });
    }

    public Flow getFlow() {
        return flow;
    }
}
