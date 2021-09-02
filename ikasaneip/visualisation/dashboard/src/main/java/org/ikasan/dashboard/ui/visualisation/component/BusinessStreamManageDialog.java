package org.ikasan.dashboard.ui.visualisation.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.ItemDoubleClickEvent;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.TableButton;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.filter.BusinessStreamSearchFilter;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;

public class BusinessStreamManageDialog extends AbstractCloseableResizableDialog {

    private BusinessStreamFilteringGrid businessStreamGrid;

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private TextField textField = new TextField();

    private BusinessStreamMetaData businessStreamMetaData;
    private ModuleMetaDataService moduleMetaDataService;

    public BusinessStreamManageDialog(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService,
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
            businessStreamSearchFilter, moduleMetaDataService);

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
        businessStreamGrid.addColumn(new ComponentRenderer<>(businessStreamMetaData->
        {
            Button editButton = new TableButton(VaadinIcon.EDIT.create());
            editButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
            {
                BusinessStreamUploadDialog uploadDialog = new  BusinessStreamUploadDialog(businessStreamMetaData, this.businessStreamMetaDataService);
                uploadDialog.open();

            });

            ComponentSecurityVisibility.applySecurity(editButton, SecurityConstants.PLATORM_CONFIGURATON_ADMIN,
                SecurityConstants.PLATORM_CONFIGURATON_WRITE, SecurityConstants.ALL_AUTHORITY);

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(editButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, editButton);
            return layout;
        })).setWidth("30px");
        businessStreamGrid.addColumn(new ComponentRenderer<>(businessStreamMetaData->
        {
            Button downloadButton = new TableButton(VaadinIcon.DOWNLOAD.create());
            StreamResource streamResource = new StreamResource(businessStreamMetaData.getName().concat(".json")
                , () -> new ByteArrayInputStream(businessStreamMetaData.getJson().getBytes()));

            FileDownloadWrapper buttonWrapper = new FileDownloadWrapper(streamResource);
            buttonWrapper.wrapComponent(downloadButton);

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(buttonWrapper);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonWrapper);
            return layout;
        })).setWidth("30px");
        businessStreamGrid.addColumn(new ComponentRenderer<>(businessStreamMetaData->
        {
            Button deleteButton = new TableButton(VaadinIcon.TRASH.create());
            deleteButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent ->
            {
                this.businessStreamMetaDataService.delete(businessStreamMetaData.getId());
                this.businessStreamGrid.getDataProvider().refreshAll();
            });

            ComponentSecurityVisibility.applySecurity(deleteButton, SecurityConstants.PLATORM_CONFIGURATON_ADMIN,
                SecurityConstants.PLATORM_CONFIGURATON_WRITE, SecurityConstants.ALL_AUTHORITY);

            VerticalLayout layout = new VerticalLayout();
            layout.setSizeFull();
            layout.add(deleteButton);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, deleteButton);
            return layout;
        })).setWidth("30px");

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
