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
import com.vaadin.flow.data.renderer.TemplateRenderer;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.scheduled.instance.model.SolrContextInstanceSearchFilterImpl;
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
        this.displayContextInstanceIdColumn = displayContextInstanceIdColumn;
    }


    /**
     * Constructor
     *
     * @param contextInstanceService
     */
    public ContextInstanceAuditWidget(ScheduledContextInstanceService contextInstanceService) {

        this.contextInstanceService = contextInstanceService;
    }

    private void init() {
        this.createGrid();

        Div div = new Div();
        div.setSizeFull();


        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");
        HorizontalLayout layout = new HorizontalLayout();


        Button refresh = new Button("Refresh");
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
                JsonViewerDialog dialog = new JsonViewerDialog(scheduledContextInstanceAudit.getProcessEvent());
                dialog.open();
            });

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
                        JsonViewerDialog dialog = new JsonViewerDialog(schedulerJobInitiationEvent);
                        dialog.open();
                    });

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
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            Button button = new Button("Open");
            button.addClickListener(event -> {
                JsonViewerDialog dialog = new JsonViewerDialog(this.contextInstanceService
                    .findAuditRecordById(scheduledContextInstanceAuditRecord.getScheduledContextInstanceAuditAggregate()
                        .getPreviousContextInstanceAuditId()));
                dialog.open();
            });

            horizontalLayout.add(button);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.instance-before", UI.getCurrent().getLocale()))
            .setFlexGrow(1)
            .setResizable(true);

        contextInstanceAuditFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextInstanceAuditRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            Button button = new Button("Open");
            button.addClickListener(event -> {
                JsonViewerDialog dialog = new JsonViewerDialog(this.contextInstanceService
                    .findAuditRecordById(scheduledContextInstanceAuditRecord.getScheduledContextInstanceAuditAggregate()
                        .getUpdatedContextInstanceAuditId()));
                dialog.open();
            });

            horizontalLayout.add(button);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.instance-after", UI.getCurrent().getLocale()))
            .setFlexGrow(1)
            .setResizable(true);

        this.contextInstanceAuditFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextInstanceAuditAggregateRecord>of(
            "<div>[[item.timestamp]]</div>")
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

    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceAuditAggregateSearchFilter.setContextInstanceId(contextInstanceId);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        this.init();
    }
}
