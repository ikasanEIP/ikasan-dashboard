package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.UI;
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
import org.ikasan.dashboard.ui.visualisation.component.ModuleFilteringGrid;
import org.ikasan.dashboard.ui.visualisation.component.filter.ModuleSearchFilter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;

public class ModuleWidget extends Div {

    private ModuleFilteringGrid modulesGrid;
    private ModuleMetaDataService moduleMetadataService;
    private TextField textField;

    public ModuleWidget(ModuleMetaDataService moduleMetadataService) {
        this.moduleMetadataService = moduleMetadataService;
        this.textField = new TextField();
        this.createGrid();

        Div div = new Div();
        div.addClassNames("card-counter");
        div.setHeight("500px");


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4("Modules and Agents");
        layout.add(modules, textField);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, textField);

        textField.getElement().getStyle().set("margin-left", "auto");

        div.add(layout);
        div.add(this.modulesGrid);

        this.modulesGrid.init();

        this.add(div);
    }

    private void createGrid() {
        // Create a modulesGrid bound to the list
        ModuleSearchFilter moduleSearchFilter = new ModuleSearchFilter();
        modulesGrid = new ModuleFilteringGrid(this.moduleMetadataService, moduleSearchFilter);
        modulesGrid.removeAllColumns();
        modulesGrid.setVisible(true);
        modulesGrid.setWidthFull();
        modulesGrid.setHeight("80%");

        modulesGrid.addColumn(ModuleMetaData::getName)
            .setHeader(getTranslation("table-header.module-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(16);
        modulesGrid.addColumn(TemplateRenderer.<ModuleMetaData>of("<div style='white-space:normal'>[[item.description]]</div>")
            .withProperty("description", ModuleMetaData::getDescription))
            .setHeader(getTranslation("table-header.module-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(32);
        modulesGrid.addColumn(new ComponentRenderer<>(moduleMetaData -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            String route = RouteConfiguration.forSessionScope()
                .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.MODULE.name() + ":" + moduleMetaData.getName());
            Anchor link = new Anchor(route, "view");
            link.setTarget("_blank");
            add(link);
            horizontalLayout.add(link);
            link.getStyle().set("color", "blue");

            return horizontalLayout;
        })).setWidth("60px");

        this.modulesGrid.addGridFiltering(textField, moduleSearchFilter::setModuleNameFilter);
    }
}
