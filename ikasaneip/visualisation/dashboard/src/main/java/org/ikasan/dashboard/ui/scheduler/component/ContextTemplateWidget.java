package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.StreamResource;
import org.ikasan.dashboard.ui.scheduler.util.ContextExportZipUtils;
import org.ikasan.dashboard.ui.scheduler.view.ContextTemplateManagementView;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.context.model.ScheduledContextSearchFilterImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.service.ContextUploadInitialisationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.ikasan.dashboard.ui.scheduler.util.ContextExportZipUtils.getExportZipFileName;

public class ContextTemplateWidget extends Div {

    private ContextTemplateFilteringGrid contextTemplateFilteringGrid;
    private ScheduledContextService scheduledContextService;
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
                                 JobInitiationService jobInitiationService, String zipWorkingDirectory, ContextUploadInitialisationService contextUploadInitialisationService) {

        this.scheduledContextService = scheduledContextService;
        this.schedulerJobService = schedulerJobService;
        this.zipWorkingDirectory = zipWorkingDirectory;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, scheduledContextInstanceService, schedulerJobInstanceService, jobInitiationService);

        Div div = new Div();
        div.setSizeFull();

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        HorizontalLayout headerLayout = new HorizontalLayout();
        H4 contextTemplates = new H4("Context Templates");
        headerLayout.add(contextTemplates);

        Button addContextButton = new Button("Add Context And Jobs");
        addContextButton.addClickListener(buttonClickEvent -> {
            ContextImportFileDialog importer = new ContextImportFileDialog(contextUploadInitialisationService);
            importer.open();
        });

        div.add(headerLayout, addContextButton, this.contextTemplateFilteringGrid);

        this.contextTemplateFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                              MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                              LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                              JobInitiationService jobInitiationService) {
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
            })).setHeader("Context Name")
            .setKey("moduleName")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextRecord.getContext().getDescription());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader("Description")
            .setResizable(true)
            .setFlexGrow(5);


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = VaadinIcon.EDIT.create();
            edit.setId("editScheduledJob");
            edit.setSize("14pt");
            edit.getStyle().set("cursor", "pointer");
            edit.getElement().setAttribute("title", getTranslation("tooltip.edit-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ContextTemplateManagementDialog contextTemplateManagementDialog
                    = new ContextTemplateManagementDialog(this.scheduledContextService, scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                    , schedulerJobService, logStreamingService, scheduledContextRecord.getContext(), schedulerJobInstanceService, jobInitiationService);
                contextTemplateManagementDialog.open();
            });

            layout.add(edit);

            Icon view = VaadinIcon.EYE.create();
            view.setSize("14pt");
            view.getStyle().set("cursor", "pointer");
            view.getElement().setAttribute("title", getTranslation("tooltip.view-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication, view, SecurityConstants.SCHEDULER_READ);

            view.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(view);

            Icon delete = VaadinIcon.TRASH.create();
            delete.setSize("14pt");
            delete.getStyle().set("cursor", "pointer");
            delete.getElement().setAttribute("title", getTranslation("tooltip.delete-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication, delete, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            layout.add(delete);

            delete.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            Icon clone = VaadinIcon.COPY.create();
            clone.setSize("14pt");
            clone.getStyle().set("cursor", "pointer");
            clone.getElement().setAttribute("title", getTranslation("tooltip.clone-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication, delete, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            layout.add(clone);

            clone.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            Icon chart = VaadinIcon.CHART.create();
            chart.setSize("14pt");
            chart.getStyle().set("cursor", "pointer");
            chart.getElement().setAttribute("title", getTranslation("tooltip.job-statistics", UI.getCurrent().getLocale()));
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(chart);

            Icon export = VaadinIcon.DOWNLOAD_ALT.create();
            export.setSize("14pt");
            export.getStyle().set("cursor", "pointer");
            export.getStyle().set("color", "black");
            export.getElement().setAttribute("title", "Export Context and Jobs");
            export.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

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
                    return null;
                }
            });

            FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
            exportWrapper.wrapComponent(export);

            layout.add(exportWrapper);

            Icon newWindow = VaadinIcon.EXTERNAL_LINK.create();
            newWindow.setSize("14pt");
            newWindow.getStyle().set("cursor", "pointer");
            newWindow.getElement().setAttribute("title", "Open in New Window");
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
}
