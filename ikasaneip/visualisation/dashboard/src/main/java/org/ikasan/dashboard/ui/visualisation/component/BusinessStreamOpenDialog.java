package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.visualisation.component.filter.BusinessStreamSearchFilter;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;

public class BusinessStreamOpenDialog extends AbstractCloseableResizableDialog {

    private BusinessStreamFilteringGrid businessStreamGrid;

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private ModuleMetaDataService moduleMetaDataService;
    private TextField textField = new TextField();

    private BusinessStreamMetaData businessStreamMetaData;

    public BusinessStreamOpenDialog(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService,
                                    ModuleMetaDataService moduleMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        this.moduleMetaDataService = moduleMetaDataService;
        createGrid();

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        H4 modules = new H4(getTranslation("label.business-streams", UI.getCurrent().getLocale()));
        layout.add(modules, textField);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, textField);

        textField.getElement().getStyle().set("margin-left", "auto");

        this.businessStreamGrid.init();

        super.content.add(layout);
        super.content.add(this.businessStreamGrid);

        this.setWidth("500px");
        this.setHeight("700px");
    }

    private void createGrid() {
        BusinessStreamSearchFilter businessStreamSearchFilter = new BusinessStreamSearchFilter();
        this.businessStreamGrid = new BusinessStreamFilteringGrid(businessStreamMetaDataService,
            businessStreamSearchFilter, this.moduleMetaDataService);

        businessStreamGrid.removeAllColumns();
        businessStreamGrid.setVisible(true);
        businessStreamGrid.setWidthFull();
        businessStreamGrid.setHeight("80%");
        businessStreamGrid.addColumn(TemplateRenderer.<BusinessStreamMetaData>of("<div style='white-space:normal'>[[item.name]]</div>")
            .withProperty("name", BusinessStreamMetaData::getName))
            .setHeader(getTranslation("table-header.business-stream-name", UI.getCurrent().getLocale()))
            .setKey("name")
            .setFlexGrow(16);
        businessStreamGrid.addColumn(TemplateRenderer.<BusinessStreamMetaData>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", BusinessStreamMetaData::getDescription)).setHeader(getTranslation("table-header.business-stream-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(32);

        businessStreamGrid.addItemDoubleClickListener((ComponentEventListener<ItemDoubleClickEvent<BusinessStreamMetaData>>) doubleClickEvent -> {
            this.businessStreamMetaData = doubleClickEvent.getItem();
            this.close();
        });

        this.businessStreamGrid.addGridFiltering(textField, businessStreamSearchFilter::setBusinessStreamNameFilter);
    }

    public BusinessStreamMetaData getBusinessStreamMetaData() {
        return businessStreamMetaData;
    }
}
