package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.SolrSchedulerJobSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

public class SchedulerJobSelectGridWidget extends Div {

    private SchedulerJobFilteringGrid schedulerJobFilteringGrid;
    private List<SchedulerJobSelectedListener> schedulerJobSelectedListeners = new ArrayList<>();
    private Dialog parent;

    private String jobType = null;

    private SchedulerJobSearchFilter schedulerJobSearchFilter;

    private String jobSelectLabel;

    public SchedulerJobSelectGridWidget(SchedulerJobService schedulerJobService, ContextTemplate contextTemplate, Dialog parent
        , SchedulerJobSearchFilter schedulerJobSearchFilter, String jobSelectLabel) {
        this.schedulerJobSearchFilter = schedulerJobSearchFilter;
        this.parent = parent;
        this.jobSelectLabel = jobSelectLabel;

        init(schedulerJobService, contextTemplate);
    }

    /**
     * Constructor
     */
    public SchedulerJobSelectGridWidget(SchedulerJobService schedulerJobService, ContextTemplate contextTemplate, Dialog parent
        , String jobSelectLabel) {

        this.parent = parent;
        this.schedulerJobSearchFilter = new SolrSchedulerJobSearchFilterImpl();
        this.jobSelectLabel = jobSelectLabel;
        init(schedulerJobService, contextTemplate);
    }

    private void init(SchedulerJobService schedulerJobService, ContextTemplate contextTemplate) {
        this.createGrid(schedulerJobService, contextTemplate);

        this.schedulerJobFilteringGrid.init();

        H3 label = new H3(this.jobSelectLabel);
        label.getElement().getStyle().set("margin-top", "10px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.add(label, this.createButtonLayout(), this.schedulerJobFilteringGrid);

        this.add(layout);
        this.setSizeFull();
    }

    private void createGrid(SchedulerJobService schedulerJobService, ContextTemplate contextTemplate) {
        schedulerJobFilteringGrid = new SchedulerJobFilteringGrid(schedulerJobService, schedulerJobSearchFilter);
        schedulerJobFilteringGrid.getElement().getStyle().set("margin-top", "40px");
        schedulerJobFilteringGrid.removeAllColumns();
        schedulerJobFilteringGrid.setVisible(true);
        schedulerJobFilteringGrid.setWidthFull();
        schedulerJobFilteringGrid.setHeight("75vh");
        schedulerJobFilteringGrid.setContextName(contextTemplate.getName());


        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getJobName());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("flowName")
            .setFlexGrow(3);

        schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS_INVERTED.get(schedulerJobRecord.getType()));

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.job-type", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setSortable(true)
            .setKey("type")
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(LitRenderer.<SchedulerJobRecord>of(
            "<div>[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(LitRenderer.<SchedulerJobRecord>of(
            "<div>[[item.modified]]</div>")
            .withProperty("modified",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.schedulerJobFilteringGrid.addColumn(new ComponentRenderer<>(schedulerJobRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(schedulerJobRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
        .setResizable(true)
        .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
        .setSortable(true)
        .setFlexGrow(1);

        this.schedulerJobFilteringGrid.addItemDoubleClickListener(event -> {
            this.schedulerJobSelectedListeners.forEach(listener -> listener.jobSelected(event.getItem().getJob()));
            if(parent != null) {
                parent.close();
            }
        });

        HeaderRow hr = schedulerJobFilteringGrid.appendHeaderRow();
        this.schedulerJobFilteringGrid.addGridFiltering(hr, schedulerJobSearchFilter::setJobNameFilter, "flowName");

        if(this.jobType == null) {
            this.schedulerJobFilteringGrid.addSelectGridFiltering(hr, schedulerJobSearchFilter::setJobTypeFilter
                , SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet(), "type");
        }

    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setPadding(false);
        buttonLayout.getElement().getStyle().set("position", "absolute");
        buttonLayout.getElement().getStyle().set("right", "30px");
        buttonLayout.getElement().getStyle().set("margin-top", "0px");
        Button refreshButton = this.createRefreshButton();
        buttonLayout.add(refreshButton);

        return buttonLayout;
    }

    private Button createRefreshButton() {
        Button refreshJobsButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshJobsButton.setIconAfterText(true);

        refreshJobsButton.addClickListener(event -> this.schedulerJobFilteringGrid.init());

        return refreshJobsButton;
    }

    public void addSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.add(listener);
    }

    public void removeSchedulerJobSelectedListener(SchedulerJobSelectedListener listener) {
        this.schedulerJobSelectedListeners.remove(listener);
    }
}
