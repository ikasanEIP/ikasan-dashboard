package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamFilteringGrid;
import org.ikasan.dashboard.ui.visualisation.component.filter.BusinessStreamSearchFilter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.spec.metadata.model.BusinessStreamMetaData;
import org.ikasan.spec.metadata.service.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;


public class BusinessStreamWidget extends DashboardWidget {

    private BusinessStreamFilteringGrid businessStreamGrid;

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private ModuleMetaDataService moduleMetaDataService;
    private final TextField textField = new TextField();

    public BusinessStreamWidget(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService,
                                ModuleMetaDataService moduleMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        this.moduleMetaDataService = moduleMetaDataService;
        createGrid();

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(true);
        layout.setWidth("100%");
        H4 modules = new H4(getTranslation("header.business-streams", UI.getCurrent().getLocale()));
        layout.add(modules, textField);
        textField.getStyle().set("margin-left", "auto");

        this.businessStreamGrid.init();

        super.setHeaderContent(layout);
        super.setContent(businessStreamGrid);
    }

    private void createGrid() {
        BusinessStreamSearchFilter businessStreamSearchFilter = new BusinessStreamSearchFilter();
        this.businessStreamGrid = new BusinessStreamFilteringGrid(businessStreamMetaDataService,
            businessStreamSearchFilter, moduleMetaDataService);

        businessStreamGrid.removeAllColumns();
        businessStreamGrid.setVisible(true);
        businessStreamGrid.setWidthFull();
        this.businessStreamGrid.setHeight(45, Unit.VH);
        businessStreamGrid.addColumn(LitRenderer.<BusinessStreamMetaData>of("<div style='white-space:normal'>${item.name}</div>")
            .withProperty("name", BusinessStreamMetaData::getName))
            .setHeader(getTranslation("table-header.business-stream-name", UI.getCurrent().getLocale()))
            .setKey("name")
            .setFlexGrow(16);
        businessStreamGrid.addColumn(LitRenderer.<BusinessStreamMetaData>of("<div style='white-space:normal'>${item.description}</div>")
            .withProperty("description", BusinessStreamMetaData::getDescription)).setHeader(getTranslation("table-header.business-stream-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(32);

        businessStreamGrid.addColumn(new ComponentRenderer<>(businessStreamMetaData -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            String route = RouteConfiguration.forSessionScope()
                .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.BUSINESS_STREAM.name() + ":" + businessStreamMetaData.getName());
            Anchor link = new Anchor(route, getTranslation("label.view", UI.getCurrent().getLocale()));
            link.setTarget("_blank");
            horizontalLayout.add(link);
            link.getStyle().set("color", "blue");

            return horizontalLayout;
        })).setWidth("60px");


        this.businessStreamGrid.addGridFiltering(textField, businessStreamSearchFilter::setBusinessStreamNameFilter);
    }

}
