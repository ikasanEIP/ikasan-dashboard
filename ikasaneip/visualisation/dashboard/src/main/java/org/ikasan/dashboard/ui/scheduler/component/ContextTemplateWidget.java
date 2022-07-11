package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.util.ContextExportZipUtils;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.scheduler.view.ContextTemplateManagementView;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.scheduled.context.model.ScheduledContextSearchFilterImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.ikasan.dashboard.ui.scheduler.util.ContextExportZipUtils.getExportZipFileName;

public class ContextTemplateWidget extends Div {

    private ContextTemplateFilteringGrid contextTemplateFilteringGrid;
    private ScheduledContextService scheduledContextService;
    private ContextProfileService contextProfileService;
    private JobProvisionService jobProvisionService;
    private IkasanAuthentication authentication;

    private SchedulerJobService schedulerJobService;
    private String zipWorkingDirectory;

    /**
     * Constructor
     *
     * @param scheduledContextService
     */
    public ContextTemplateWidget(ScheduledContextService scheduledContextService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                                 JobInitiationService jobInitiationService, String zipWorkingDirectory, ContextProvisionService contextProvisionService,
                                 ContextProfileService contextProfileService, JobProvisionService jobProvisionService, UserService userService,
                                 SecurityService securityService) {

        this.scheduledContextService = scheduledContextService;
        this.schedulerJobService = schedulerJobService;
        this.zipWorkingDirectory = zipWorkingDirectory;
        this.contextProfileService = contextProfileService;
        this.jobProvisionService = jobProvisionService;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, scheduledContextInstanceService, schedulerJobInstanceService, jobInitiationService
            , userService, securityService);

        Div div = new Div();
        div.setSizeFull();

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidth("100%");
        H4 contextTemplates = new H4("Context Templates");
        headerLayout.add(contextTemplates);

        HorizontalLayout actionButtonLayout = new HorizontalLayout();
        actionButtonLayout.setMargin(false);

        MenuBar quickAccessMenu = this.createQuickAccessMenu();

        Icon uploadIcon = VaadinIcon.UPLOAD_ALT.create();
        Button addContextButton = new Button("Upload Context",uploadIcon);
        addContextButton.setIconAfterText(true);
        addContextButton.addClickListener(buttonClickEvent -> {
            ContextImportFileDialog importer = new ContextImportFileDialog(contextProvisionService);
            importer.open();
        });

        Icon newContextIcon = VaadinIcon.PLUS.create();
        Button newContextButton = new Button("New Context",newContextIcon);
        newContextButton.setIconAfterText(true);
        newContextButton.addClickListener(buttonClickEvent -> {
            UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
            underConstructionDialog.open();
        });

        actionButtonLayout.add(newContextButton, addContextButton, quickAccessMenu);
        actionButtonLayout.getElement().getStyle().set("position", "absolute");
        actionButtonLayout.getElement().getStyle().set("right", "30px");

        headerLayout.add(actionButtonLayout);

        div.add(headerLayout, this.contextTemplateFilteringGrid);

        this.contextTemplateFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                              MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                              LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                              JobInitiationService jobInitiationService, UserService userService, SecurityService securityService) {
        // Create a modulesGrid bound to the list
        ScheduledContextSearchFilter contextSearchFilter = new ScheduledContextSearchFilterImpl();
        contextTemplateFilteringGrid = new ContextTemplateFilteringGrid(this.scheduledContextService, contextSearchFilter);
        contextTemplateFilteringGrid.removeAllColumns();
        contextTemplateFilteringGrid.setVisible(true);
        contextTemplateFilteringGrid.setWidthFull();
        contextTemplateFilteringGrid.setHeight("1000px");


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Text text = new Text(scheduledContextRecord.getContextName());

                horizontalLayout.add(text);
                return horizontalLayout;
            })).setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale()))
            .setKey("moduleName")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextRecord.getContext().getDescription());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.context-description", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setFlexGrow(5);


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = IconDecorator.decorate(new Icon(VaadinIcon.EDIT), getTranslation("tooltip.manage-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            edit.setId("editScheduledJob");
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ContextTemplateManagementDialog contextTemplateManagementDialog
                    = new ContextTemplateManagementDialog(this.scheduledContextService, scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                    , schedulerJobService, logStreamingService, scheduledContextRecord.getContext(), schedulerJobInstanceService, jobInitiationService, this.contextProfileService
                    , this.jobProvisionService, userService, securityService);
                contextTemplateManagementDialog.open();
            });

            layout.add(edit);

            Icon view = IconDecorator.decorate(new Icon(VaadinIcon.EYE), getTranslation("tooltip.view-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, view, SecurityConstants.SCHEDULER_READ);

            view.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            layout.add(view);

            Icon delete = IconDecorator.decorate(new Icon(VaadinIcon.TRASH), getTranslation("tooltip.delete-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, delete, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            layout.add(delete);

            delete.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            Icon clone = IconDecorator.decorate(new Icon(VaadinIcon.COPY), getTranslation("tooltip.clone-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, clone, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            layout.add(clone);

            clone.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.contexts-statistics", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            layout.add(chart);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("tooltip.export-jobs-and-associated-artifacts", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            StreamResource streamResource = new StreamResource(getExportZipFileName(scheduledContextRecord.getContextName()), () -> {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = ContextExportZipUtils.createZipFile(
                        scheduledContextRecord.getContext(),
                        this.zipWorkingDirectory,
                        this.schedulerJobService,
                        50 // limit to loop searching solr
                    );
                    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.download-context", UI.getCurrent().getLocale()));
                    return null;
                }
            });

            FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
            exportWrapper.wrapComponent(export);

            layout.add(exportWrapper);

            Icon newWindow = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("tooltip.open-in-new-window", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            newWindow.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextTemplateManagementView.class, scheduledContextRecord.getContextName());

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            layout.add(newWindow);

            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextRecord>of(
            "<div>[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextRecord>of(
            "<div>[[item.modified]]</div>")
            .withProperty("modified",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
            .setSortable(true)
            .setFlexGrow(1);

        HeaderRow hr = contextTemplateFilteringGrid.appendHeaderRow();
        this.contextTemplateFilteringGrid.addGridFiltering(hr, contextSearchFilter::setContextName, "moduleName");
    }

    private MenuBar createQuickAccessMenu() {
        MenuBar quickStartMenuBar = new MenuBar();
        quickStartMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem quickAccess = createQuickAccessButton(quickStartMenuBar, VaadinIcon.COG, getTranslation("menu-item.quick-access", UI.getCurrent().getLocale()));

        SubMenu activeContextInstancesSubMenu = quickAccess.getSubMenu();
        MenuItem activeContexts = activeContextInstancesSubMenu.addItem(getTranslation("menu-item.active-contexts", UI.getCurrent().getLocale()));
        SubMenu activeContextSubMenu = activeContexts.getSubMenu();

        ContextMachineCache.instance().contextNames().forEach(name ->
            activeContextSubMenu.addItem(name, menuItemClickEvent -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextInstanceView.class, ContextMachineCache.instance()
                        .getByContextName(name).getContext().getId()+"_scheduledContextInstance");

                getUI().ifPresent(ui -> ui.getPage().open(route));
            })
        );

        return quickStartMenuBar;
    }

    private MenuItem createQuickAccessButton(MenuBar menu, VaadinIcon iconName, String label) {
        Icon icon = new Icon(iconName);
        Button quickAccessButton = new Button(label, icon);
        quickAccessButton.setIconAfterText(true);

        MenuItem item = menu.addItem(quickAccessButton);

        return item;
    }
}
