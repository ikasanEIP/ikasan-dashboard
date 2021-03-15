package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.ui.visualisation.component.BusinessStreamFilteringGrid;
import org.ikasan.dashboard.ui.visualisation.component.filter.BusinessStreamSearchFilter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.spec.metadata.BusinessStreamMetaData;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;


@CssImport("./styles/dashboard-view.css")
public class BusinessStreamWidget extends Div {

    private BusinessStreamFilteringGrid businessStreamGrid;

    private BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService;
    private TextField textField = new TextField();

    public BusinessStreamWidget(BusinessStreamMetaDataService<BusinessStreamMetaData> businessStreamMetaDataService) {
        this.businessStreamMetaDataService = businessStreamMetaDataService;
        createGrid();
        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4("Business Streams");
        layout.add(modules, textField);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, textField);

        textField.getElement().getStyle().set("margin-left", "auto");

        div.add(layout);
        div.add(this.businessStreamGrid);

        this.businessStreamGrid.init();

        this.add(div);
    }

    private void createGrid() {
        BusinessStreamSearchFilter businessStreamSearchFilter = new BusinessStreamSearchFilter();
        this.businessStreamGrid = new BusinessStreamFilteringGrid(businessStreamMetaDataService,
            businessStreamSearchFilter);

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

        businessStreamGrid.addColumn(new ComponentRenderer<>(businessStreamMetaData -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            String route = RouteConfiguration.forSessionScope()
                .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.BUSINESS_STREAM.name() + ":" + businessStreamMetaData.getName());
            Anchor link = new Anchor(route, "view");
            link.setTarget("_blank");
            add(link);
            horizontalLayout.add(link);
            link.getStyle().set("color", "blue");

            return horizontalLayout;
        })).setWidth("60px");


        this.businessStreamGrid.addGridFiltering(textField, businessStreamSearchFilter::setBusinessStreamNameFilter);
    }

}
