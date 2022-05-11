package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.scheduler.component.filter.ContextInstanceSearchFilter;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;

public class ContextInstanceGridWidget extends Div {

    private ContextTemplateFilteringGrid contextTemplateFilteringGrid;
    private ScheduledContextService scheduledContextService;
    private TextField textField = new TextField();
    private IkasanAuthentication authentication;

    /**
     * Constructor
     *
     * @param scheduledContextService
     */
    public ContextTemplateWidget(ScheduledContextService scheduledContextService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService) {

        this.scheduledContextService = scheduledContextService;
        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService);

        Div div = new Div();
        div.setSizeFull();


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        textField.setWidth("300px");
        HorizontalLayout headerLayout = new HorizontalLayout();
        H4 contextTemplates = new H4("Context Templates");
        headerLayout.add(contextTemplates);

        HorizontalLayout layout = new HorizontalLayout();
        textField.getElement().getStyle().set("margin-left", "auto");

        Button refresh = new Button("Search");
        refresh.addClickListener(event -> this.contextTemplateFilteringGrid.init());
        refresh.getElement().getStyle().set("margin-right", "auto");

        layout.add(textField, refresh);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, textField);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, refresh);

        div.add(headerLayout, layout, this.contextTemplateFilteringGrid);

        this.contextTemplateFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                            ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                            MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                            LogStreamingService logStreamingService) {
        // Create a modulesGrid bound to the list
        ContextInstanceSearchFilter moduleSearchFilter = new ContextInstanceSearchFilter();
        contextTemplateFilteringGrid = new ContextTemplateFilteringGrid(this.scheduledContextService, moduleSearchFilter);
        contextTemplateFilteringGrid.removeAllColumns();
        contextTemplateFilteringGrid.setVisible(true);
        contextTemplateFilteringGrid.setWidthFull();
        contextTemplateFilteringGrid.setHeight("1000px");


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextInstanceAuditRecord.getContextName());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader("Context Instance Id")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

//        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
//            HorizontalLayout horizontalLayout = new HorizontalLayout();
//
//            Text text = new Text(scheduledContextInstanceAuditRecord.getContext().getDescription());
//
//            horizontalLayout.add(text);
//            return horizontalLayout;
//        })).setHeader("Description")
//            .setResizable(true)
//            .setFlexGrow(5);


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = VaadinIcon.EDIT.create();
            edit.setId("editScheduledJob");
            edit.setSize("14pt");
            edit.getStyle().set("cursor", "pointer");
            edit.getElement().setAttribute("title", getTranslation("tooltip.edit-job", UI.getCurrent().getLocale()));
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ContextTemplateManagementDialog contextTemplateManagementDialog
                    = new ContextTemplateManagementDialog(this.scheduledContextService, dynamicImagePath, moduleMetaDataService
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                    , schedulerJobService, logStreamingService);
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
            export.getElement().setAttribute("title", "Export");
            export.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

            });

            layout.add(export);

            Icon newWindow = VaadinIcon.PLUS_SQUARE_O.create();
            newWindow.setSize("14pt");
            newWindow.getStyle().set("cursor", "pointer");
            newWindow.getElement().setAttribute("title", "Open in New Window");
            newWindow.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {

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
            .setHeader(getTranslation("Created Date/Time", UI.getCurrent().getLocale()))
            .setKey("created")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextRecord>of(
            "<div>[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("Modified Date/Time", UI.getCurrent().getLocale()))
            .setKey("modified")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(3);

        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            //            Button button = new Button("Open");
            //            button.addClickListener(event -> {
            //                JsonViewerDialog dialog = new JsonViewerDialog(scheduledContextInstanceAuditRecord
            //                    .getScheduledContextInstanceAudit().getPreviousContextInstance());
            //                dialog.open();
            //            });
            //
            //            horizontalLayout.add(button);
            return horizontalLayout;


        }))
            .setResizable(true)
            .setHeader("Modified By")
            .setSortable(true)
            .setFlexGrow(2);


        this.contextTemplateFilteringGrid.addGridFiltering(textField, moduleSearchFilter::setContextSearchFilter);
    }
}
