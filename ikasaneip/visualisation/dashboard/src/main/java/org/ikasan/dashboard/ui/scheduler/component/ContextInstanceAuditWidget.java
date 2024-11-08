package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregate;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;

public class ContextInstanceAuditWidget extends Div {

    private ContextInstanceAuditFilteringGrid contextInstanceAuditFilteringGrid;
    private ScheduledContextInstanceService contextInstanceService;
    private ScheduledContextInstanceAuditAggregateSearchFilter contextInstanceAuditAggregateSearchFilter;
    private boolean displayContextInstanceIdColumn = true;

    /**
     * Constructor
     * @param contextInstanceService
     * @param contextInstanceAuditAggregateSearchFilter
     */
    public ContextInstanceAuditWidget(ScheduledContextInstanceService contextInstanceService
        , ScheduledContextInstanceAuditAggregateSearchFilter contextInstanceAuditAggregateSearchFilter, boolean displayContextInstanceIdColumn) {
        this(contextInstanceService);
        this.contextInstanceAuditAggregateSearchFilter = contextInstanceAuditAggregateSearchFilter;
        if (this.contextInstanceAuditAggregateSearchFilter == null) {
            throw new IllegalArgumentException("contextInstanceAuditAggregateSearchFilter cannot be null!");
        }
        this.displayContextInstanceIdColumn = displayContextInstanceIdColumn;
    }


    /**
     * Constructor
     *
     * @param contextInstanceService
     */
    public ContextInstanceAuditWidget(ScheduledContextInstanceService contextInstanceService) {
        this.contextInstanceService = contextInstanceService;
        if (this.contextInstanceService == null) {
            throw new IllegalArgumentException("contextInstanceService cannot be null!");
        }
    }

    /**
     * Helper method to initialise the widget.
     */
    private void init() {
        this.createGrid();

        Div div = new Div();
        div.setSizeFull();


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");
        HorizontalLayout layout = new HorizontalLayout();


        Button refresh = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()));
        refresh.setIcon(VaadinIcon.REFRESH.create());
        refresh.setIconAfterText(true);
        refresh.addClickListener(event -> this.contextInstanceAuditFilteringGrid.init());
        refresh.getElement().getStyle().set("margin-left", "auto");

        layout.add(refresh);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.END, refresh);

        div.add(layout);
        div.add(this.contextInstanceAuditFilteringGrid);

        this.contextInstanceAuditFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    /**
     * Helper method to create the audit grid.
     */
    private void createGrid() {
        if(this.contextInstanceAuditAggregateSearchFilter == null) {
            this.contextInstanceAuditAggregateSearchFilter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        }
        contextInstanceAuditFilteringGrid = new ContextInstanceAuditFilteringGrid(this.contextInstanceService, this.contextInstanceAuditAggregateSearchFilter);
        contextInstanceAuditFilteringGrid.removeAllColumns();
        contextInstanceAuditFilteringGrid.setVisible(true);
        contextInstanceAuditFilteringGrid.setWidthFull();
        contextInstanceAuditFilteringGrid.setHeight("1000px");

        if(this.displayContextInstanceIdColumn) {
            contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Text text = new Text(scheduledContextInstanceAuditRecord.getContextInstanceId());

                horizontalLayout.add(text);
                return horizontalLayout;
            })).setHeader(getTranslation("table-header.context-instance-id", UI.getCurrent().getLocale()))
                .setFlexGrow(3)
                .setKey("flowName")
                .setSortable(true)
                .setResizable(true);
        }

        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            ScheduledContextInstanceAuditAggregate scheduledContextInstanceAudit = scheduledContextInstanceAuditRecord.getScheduledContextInstanceAuditAggregate();
            Button button = new Button(scheduledContextInstanceAudit.getProcessEvent().getJobName());
            button.addClickListener(event -> {
                JsonViewerDialog dialog = new JsonViewerDialog(scheduledContextInstanceAudit.getProcessEvent()
                    , scheduledContextInstanceAudit.getProcessEvent().getJobName());
                dialog.open();
            });

            ComponentSecurityVisibility.applySecurity(button, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

            horizontalLayout.add(button);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.source-event", UI.getCurrent().getLocale()))
            .setKey("componentName")
            .setFlexGrow(6)
            .setSortable(true)
            .setResizable(true);

        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            VerticalLayout verticalLayout = new VerticalLayout();
            ScheduledContextInstanceAuditAggregate scheduledContextInstanceAudit = scheduledContextInstanceAuditRecord.getScheduledContextInstanceAuditAggregate();

            if(scheduledContextInstanceAudit.getSchedulerJobInitiationEvents() != null
                && !scheduledContextInstanceAudit.getSchedulerJobInitiationEvents().isEmpty()) {
                scheduledContextInstanceAudit.getSchedulerJobInitiationEvents().forEach(schedulerJobInitiationEvent -> {
                    HorizontalLayout horizontalLayout = new HorizontalLayout();
                    Button button = new Button(schedulerJobInitiationEvent.getJobName());
                    button.addClickListener(event -> {
                        JsonViewerDialog dialog = new JsonViewerDialog(schedulerJobInitiationEvent, schedulerJobInitiationEvent.getJobName());
                        dialog.open();
                    });

                    ComponentSecurityVisibility.applySecurity(button, SecurityConstants.ALL_AUTHORITY,
                        SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                        SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

                    horizontalLayout.add(button);
                    verticalLayout.add(horizontalLayout);
                });

            }
            return verticalLayout;
        }))
            .setHeader(getTranslation("table-header.job-raise-events", UI.getCurrent().getLocale()))
            .setFlexGrow(6)
            .setKey("event")
            .setSortable(true)
            .setResizable(true);

        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            VerticalLayout verticalLayout = new VerticalLayout();
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            if(scheduledContextInstanceAuditRecord.getScheduledContextInstanceAuditAggregate()
                .getPreviousContextInstanceAuditId() != null) {
                Button button = new Button(getTranslation("button.open", UI.getCurrent().getLocale()));
                button.addClickListener(event -> {
                    JsonViewerDialog dialog = new JsonViewerDialog(this.contextInstanceService
                        .findAuditRecordById(scheduledContextInstanceAuditRecord.getScheduledContextInstanceAuditAggregate()
                            .getPreviousContextInstanceAuditId()));
                    dialog.open();
                });

                ComponentSecurityVisibility.applySecurity(button, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

                horizontalLayout.add(button);
            }
            else {
                horizontalLayout.add(getTranslation("label.not-captured", UI.getCurrent().getLocale()));
            }
            verticalLayout.add(horizontalLayout);
            verticalLayout.setWidthFull();
            verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, horizontalLayout);
            return verticalLayout;
        })).setHeader(getTranslation("table-header.instance-before", UI.getCurrent().getLocale()))
            .setFlexGrow(1)
            .setResizable(true);

        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            VerticalLayout verticalLayout = new VerticalLayout();
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            if(scheduledContextInstanceAuditRecord.getScheduledContextInstanceAuditAggregate()
                .getUpdatedContextInstanceAuditId() != null) {
                Button button = new Button(getTranslation("button.open", UI.getCurrent().getLocale()));
                button.addClickListener(event -> {
                    JsonViewerDialog dialog = new JsonViewerDialog(this.contextInstanceService
                        .findAuditRecordById(scheduledContextInstanceAuditRecord.getScheduledContextInstanceAuditAggregate()
                            .getUpdatedContextInstanceAuditId()));
                    dialog.open();
                });

                ComponentSecurityVisibility.applySecurity(button, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

                horizontalLayout.add(button);
            }
            else {
                horizontalLayout.add(getTranslation("label.not-captured", UI.getCurrent().getLocale()));
            }
                verticalLayout.add(horizontalLayout);
                verticalLayout.setWidthFull();
                verticalLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, horizontalLayout);
                return verticalLayout;
        })).setHeader(getTranslation("table-header.instance-after", UI.getCurrent().getLocale()))
            .setFlexGrow(1)
            .setResizable(true);

        this.contextInstanceAuditFilteringGrid.addColumn(LitRenderer.<ScheduledContextInstanceAuditAggregateRecord>of(
            "<div>${item.timestamp}</div>")
            .withProperty("timestamp",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setFlexGrow(3)
            .setSortable(true);

        HeaderRow hr = this.contextInstanceAuditFilteringGrid.appendHeaderRow();
        if(this.displayContextInstanceIdColumn) {
            this.contextInstanceAuditFilteringGrid.addGridFiltering(hr, this.contextInstanceAuditAggregateSearchFilter::setContextInstanceId, "flowName");
        }
        this.contextInstanceAuditFilteringGrid.addGridFiltering(hr, this.contextInstanceAuditAggregateSearchFilter::setScheduledProcessEventName, "componentName");
        this.contextInstanceAuditFilteringGrid.addGridFiltering(hr, this.contextInstanceAuditAggregateSearchFilter::setRaisedInitiationEventName, "event");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        this.init();
    }
}
