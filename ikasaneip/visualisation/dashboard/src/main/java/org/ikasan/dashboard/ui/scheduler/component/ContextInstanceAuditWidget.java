package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.scheduler.component.filter.ContextInstanceSearchFilter;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAudit;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;

public class ContextInstanceAuditWidget extends Div {

    private ContextInstanceAuditFilteringGrid contextInstanceAuditFilteringGrid;
    private ScheduledContextInstanceService contextInstanceService;
    private TextField textField = new TextField();

    /**
     * Constructor
     *
     * @param contextInstanceService
     */
    public ContextInstanceAuditWidget(ScheduledContextInstanceService contextInstanceService) {

        this.contextInstanceService = contextInstanceService;
        this.createGrid();

        Div div = new Div();
        div.setSizeFull();


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        textField.setPrefixComponent(icon);
        textField.setWidth("300px");
        HorizontalLayout layout = new HorizontalLayout();
        H4 modules = new H4("Context Instance Audit");

        textField.getElement().getStyle().set("margin-left", "auto");

        Button refresh = new Button("Refresh");
        refresh.addClickListener(event -> this.contextInstanceAuditFilteringGrid.init());
        refresh.getElement().getStyle().set("margin-left", "auto");

        layout.add(modules, textField, refresh);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.START, modules);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, textField);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, refresh);

        div.add(layout);
        div.add(this.contextInstanceAuditFilteringGrid);

        this.contextInstanceAuditFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    private void createGrid() {
        // Create a modulesGrid bound to the list
        ContextInstanceSearchFilter moduleSearchFilter = new ContextInstanceSearchFilter();
        contextInstanceAuditFilteringGrid = new ContextInstanceAuditFilteringGrid(this.contextInstanceService, moduleSearchFilter);
        contextInstanceAuditFilteringGrid.removeAllColumns();
        contextInstanceAuditFilteringGrid.setVisible(true);
        contextInstanceAuditFilteringGrid.setWidthFull();
        contextInstanceAuditFilteringGrid.setHeight("1000px");


        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextInstanceAuditRecord.getContextInstanceId());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader("Context Instance Id")
        .setFlexGrow(3);


        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            ScheduledContextInstanceAudit scheduledContextInstanceAudit = scheduledContextInstanceAuditRecord.getScheduledContextInstanceAudit();
            Button button = new Button(scheduledContextInstanceAudit.getProcessEvent().getJobName());
            button.addClickListener(event -> {
                JsonViewerDialog dialog = new JsonViewerDialog(scheduledContextInstanceAudit.getProcessEvent());
                dialog.open();
            });

            horizontalLayout.add(button);
            return horizontalLayout;
        })).setHeader("Source Event").setFlexGrow(6);

        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            VerticalLayout verticalLayout = new VerticalLayout();
            ScheduledContextInstanceAudit scheduledContextInstanceAudit = scheduledContextInstanceAuditRecord.getScheduledContextInstanceAudit();

            if(scheduledContextInstanceAudit.getSchedulerJobInitiationEvents() != null
                && !scheduledContextInstanceAudit.getSchedulerJobInitiationEvents().isEmpty()) {
                scheduledContextInstanceAudit.getSchedulerJobInitiationEvents().forEach(schedulerJobInitiationEvent -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();
                    Button button = new Button(schedulerJobInitiationEvent.getJobName());
                    button.addClickListener(event -> {
                        JsonViewerDialog dialog = new JsonViewerDialog(schedulerJobInitiationEvent);
                        dialog.open();
                    });

                    horizontalLayout.add(button);
                    verticalLayout.add(horizontalLayout);
                });

            }

            return verticalLayout;
        })).setHeader("Job Raise Event/s").setFlexGrow(6);

        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            Button button = new Button("Open");
            button.addClickListener(event -> {
                JsonViewerDialog dialog = new JsonViewerDialog(scheduledContextInstanceAuditRecord
                    .getScheduledContextInstanceAudit().getPreviousContextInstance());
                dialog.open();
            });

            horizontalLayout.add(button);
            return horizontalLayout;


        })).setHeader("Instance Before").setFlexGrow(1);

        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            Button button = new Button("Open");
            button.addClickListener(event -> {
                JsonViewerDialog dialog = new JsonViewerDialog(scheduledContextInstanceAuditRecord
                    .getScheduledContextInstanceAudit().getUpdatedContextInstance());
                dialog.open();
            });

            horizontalLayout.add(button);
            return horizontalLayout;
        })).setHeader("Instance After").setFlexGrow(1);

        this.contextInstanceAuditFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceAuditRecord>of(
            "<div>[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.timestamp", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true).setFlexGrow(3);

        this.contextInstanceAuditFilteringGrid.addGridFiltering(textField, moduleSearchFilter::setContextSearchFilter);
    }
}
