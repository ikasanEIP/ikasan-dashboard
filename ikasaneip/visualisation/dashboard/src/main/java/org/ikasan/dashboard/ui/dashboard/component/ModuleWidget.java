package org.ikasan.dashboard.ui.dashboard.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dashboard.DashboardWidget;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLink;
import org.ikasan.dashboard.ui.general.component.DownloadModulesLogDialog;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.component.ModuleFilteringGrid;
import org.ikasan.dashboard.ui.visualisation.component.filter.ModuleSearchFilter;
import org.ikasan.dashboard.ui.visualisation.util.VisualisationType;
import org.ikasan.dashboard.ui.visualisation.view.GraphVisualisationDeepLinkView;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.model.ModuleMetaData;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.DownloadLogFileService;
import org.springframework.security.core.context.SecurityContextHolder;

public class ModuleWidget extends DashboardWidget {

    private ModuleFilteringGrid modulesGrid;
    private ModuleMetaDataService moduleMetadataService;
    private DownloadLogFileService downloadLogFileService;
    private TextField textField;

    public ModuleWidget(ModuleMetaDataService moduleMetadataService, DownloadLogFileService downloadLogFileService) {
        this.moduleMetadataService = moduleMetadataService;
        this.downloadLogFileService = downloadLogFileService;
        this.textField = new TextField();
        this.createGrid();

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(true);
        layout.setWidth("100%");
        H4 modules = new H4(getTranslation("header.modules-and-agents", UI.getCurrent().getLocale()));
        layout.add(modules, textField);

        textField.getElement().getStyle().set("margin-left", "auto");

        this.modulesGrid.init();

        this.setHeaderContent(layout);
        this.setContent(this.modulesGrid);
    }

    private void createGrid() {
        // Create a modulesGrid bound to the list
        ModuleSearchFilter moduleSearchFilter = new ModuleSearchFilter();
        modulesGrid = new ModuleFilteringGrid(this.moduleMetadataService, moduleSearchFilter);
        modulesGrid.removeAllColumns();
        modulesGrid.setVisible(true);
        modulesGrid.setWidthFull();
        modulesGrid.setHeight(45, Unit.VH);

        modulesGrid.addColumn(ModuleMetaData::getName)
            .setHeader(getTranslation("table-header.module-name", UI.getCurrent().getLocale())).setKey("name")
            .setFlexGrow(16);
        modulesGrid.addColumn(LitRenderer.<ModuleMetaData>of("<div style='white-space:normal'>${item.description}</div>")
            .withProperty("description", ModuleMetaData::getDescription))
            .setHeader(getTranslation("table-header.module-description", UI.getCurrent().getLocale()))
            .setKey("description")
            .setFlexGrow(32);
        modulesGrid.addColumn(new ComponentRenderer<>(moduleMetaData -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            String route = RouteConfiguration.forSessionScope()
                .getUrl(GraphVisualisationDeepLinkView.class, VisualisationType.MODULE.name() + ":" + moduleMetaData.getName());
            Anchor link = new Anchor(route, getTranslation("label.view", UI.getCurrent().getLocale()));
            link.setTarget("_blank");
//            add(link);
            horizontalLayout.add(link);
            link.getStyle().set("color", "blue");

            // Add Icon to download log files for modules
            Icon downloadLogFileIcon = IconDecorator.decorate(new Icon(VaadinIcon.FILE_TEXT_O), getTranslation("label.download-module-log-file", UI.getCurrent().getLocale()), "10pt", "rgba(0, 0, 0, 1.0)");
            downloadLogFileIcon.addClickListener(buttonClickEvent -> {
                DownloadModulesLogDialog downloadModulesLogDialog = new DownloadModulesLogDialog(moduleMetaData, downloadLogFileService);
                downloadModulesLogDialog.open();
            });

            // wrap it in a router link
            RouterLink routerLinkLogFile = new RouterLink();
            routerLinkLogFile.add(downloadLogFileIcon);

            ComponentSecurityVisibility.applySecurity((IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication(), routerLinkLogFile,
                SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE,
                SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN,
                SecurityConstants.SCHEDULER_ALL_WRITE,
                SecurityConstants.DASHBOARD_WRITE,
                SecurityConstants.DASHBOARD_ADMIN);

            horizontalLayout.add(routerLinkLogFile);

            return horizontalLayout;
        })).setWidth("90px");

        this.modulesGrid.addGridFiltering(textField, moduleSearchFilter::setModuleNameFilter);
    }
}
