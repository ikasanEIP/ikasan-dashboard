package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.scheduler.view.ContextTemplateManagementView;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.StatusColours;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.scheduled.job.model.SolrSchedulerJobSearchFilterImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class JobSearchDialog extends AbstractCloseableResizableDialog {
    private Grid<SchedulerJobRecord> schedulerJobRecordGrid;

    private DataProvider<SchedulerJobRecord, SchedulerJobSearchFilter> dataProvider;
    private ConfigurableFilterDataProvider<SchedulerJobRecord, Void, SchedulerJobSearchFilter> filteredDataProvider;
    private SchedulerJobService<SchedulerJobRecord> schedulerJobService;
    private SchedulerJobSearchFilter searchFilter = new SolrSchedulerJobSearchFilterImpl();


    public JobSearchDialog(SchedulerJobService<SchedulerJobRecord> schedulerJobService, String jobSearchString) {
        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.searchFilter.setJobNameFilter(jobSearchString);
        super.showResize(false);
        super.title.setText(getTranslation("label.job-search", UI.getCurrent().getLocale()));
        init();
    }


    /**
     * Initialize the scheduler job record grid and set up columns with custom renderers
     * for job name, job type, job plan, running job plan instances, and prepared job plan instances.
     * Also initializes data provider, adds header row with filtering options,
     * and sets up search functionality for job name.
     */
    private void init() {
        this.schedulerJobRecordGrid = new Grid<>();
        this.schedulerJobRecordGrid.setSizeFull();

        this.schedulerJobRecordGrid.addColumn(new ComponentRenderer<>(jobRecord -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                horizontalLayout.add(jobRecord.getJobName());
                return horizontalLayout;
            }))
            .setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setKey("flowName")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(30);
        this.schedulerJobRecordGrid.addColumn(new ComponentRenderer<>(jobRecord -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();
                horizontalLayout.setWidthFull();
                Text text = new Text(SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS_INVERTED.get(jobRecord.getType()));
                horizontalLayout.add(text);
                return horizontalLayout;
            }))
            .setHeader(getTranslation("table-header.job-type", UI.getCurrent().getLocale()))
            .setKey("type")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(20);
        this.schedulerJobRecordGrid.addColumn(new ComponentRenderer<>(jobRecord -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidthFull();
                Button openJobPlanButton = new Button(jobRecord.getContextName());
                openJobPlanButton.getStyle().set("font-size", "8pt");
                openJobPlanButton.setHeight("30px");
                openJobPlanButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) iconClickEvent -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextTemplateManagementView.class
                            , List.of("job",
                                jobRecord.getContextName(),
                                "jobsTab",
                                jobRecord.getJobName()));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
                openJobPlanButton.getElement().getStyle().set("cursor", "pointer");


                openJobPlanButton.getElement().setAttribute("title"
                    , getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));
                verticalLayout.add(openJobPlanButton);
                return verticalLayout;
            }))
            .setHeader(getTranslation("table-header.job-plan", UI.getCurrent().getLocale()))
            .setKey("componentName")
            .setSortable(true)
            .setResizable(true)
            .setFlexGrow(30);
        this.schedulerJobRecordGrid.addColumn(new ComponentRenderer<>(jobRecord -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidthFull();
                ContextMachineCache.instance().getAllByContextName(jobRecord.getContextName())
                    .forEach(contextMachine -> {
                        if(!contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                            Button contextBreakoutButton = new Button(contextMachine.getContext().getId());
                            contextBreakoutButton.getStyle().set("font-size", "8pt");
                            contextBreakoutButton.setHeight("30px");
                            contextBreakoutButton.setId("contextBreakOut");
                            contextBreakoutButton.getElement().getStyle().set("background-color"
                                , StatusColours.getInstanceStatusColour(contextMachine.getContext().getStatus()));
                            if(contextMachine.getContext().getStatus().equals(InstanceStatus.WAITING)) {
                                contextBreakoutButton.getElement().getStyle().set("color", IkasanColours.BLACK);
                            }
                            else {
                                contextBreakoutButton.getElement().getStyle().set("color", IkasanColours.WHITE);
                            }
                            contextBreakoutButton.addClickListener(event -> {
                                String route = RouteConfiguration.forSessionScope()
                                    .getUrl(ContextInstanceView.class,
                                        List.of("job",
                                            contextMachine.getContext().getId() +"_scheduledContextInstance",
                                            "jobsTab",
                                            jobRecord.getJobName()));

                                getUI().ifPresent(ui -> ui.getPage().open(route));
                            });
                            contextBreakoutButton.getElement().getStyle().set("cursor", "pointer");


                            contextBreakoutButton.getElement().setAttribute("title"
                                , getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                            verticalLayout.add(contextBreakoutButton);
                        }
                    });
                return verticalLayout;
            }))
            .setHeader(getTranslation("table-header.running-job-plan-instances", UI.getCurrent().getLocale()))
            .setKey("jobPlanInstances")
            .setResizable(true)
            .setFlexGrow(40);
        this.schedulerJobRecordGrid.addColumn(new ComponentRenderer<>(jobRecord -> {
                VerticalLayout verticalLayout = new VerticalLayout();
                verticalLayout.setWidthFull();
                ContextMachineCache.instance().getAllByContextName(jobRecord.getContextName())
                    .forEach(contextMachine -> {
                        if(contextMachine.getContext().getStatus().equals(InstanceStatus.PREPARED)) {
                            Button contextBreakoutButton = new Button(contextMachine.getContext().getId());
                            contextBreakoutButton.getStyle().set("font-size", "8pt");
                            contextBreakoutButton.setHeight("30px");
                            contextBreakoutButton.setId("contextBreakOut");
                            contextBreakoutButton.getElement().getStyle().set("background-color"
                                , StatusColours.getInstanceStatusColour(contextMachine.getContext().getStatus()));
                            contextBreakoutButton.getElement().getStyle().set("color", "#FFF");
                            contextBreakoutButton.addClickListener(event -> {
                                String route = RouteConfiguration.forSessionScope()
                                    .getUrl(ContextInstanceView.class,
                                        List.of("job",
                                            contextMachine.getContext().getId() +"_scheduledContextInstance",
                                            "jobsTab",
                                            jobRecord.getJobName()));

                                getUI().ifPresent(ui -> ui.getPage().open(route));
                            });
                            contextBreakoutButton.getElement().getStyle().set("cursor", "pointer");


                            contextBreakoutButton.getElement().setAttribute("title"
                                , getTranslation("tooltip.open-job-plan-new-tab", UI.getCurrent().getLocale()));

                            verticalLayout.add(contextBreakoutButton);
                        }
                    });
                return verticalLayout;
            }))
            .setHeader(getTranslation("table-header.prepared-job-plan-instances", UI.getCurrent().getLocale()))
            .setKey("preparedJobPlanInstances")
            .setResizable(true)
            .setFlexGrow(40);

        this.schedulerJobRecordGrid.addClassName("small-header");
        this.initDataProvider();
        HeaderRow headerRow = this.schedulerJobRecordGrid.appendHeaderRow();
        this.addSelectGridFiltering(headerRow, searchFilter::setJobTypeFilter
            , SolrSchedulerJobSearchFilterImpl.JOB_TYPE_MAPPINGS.entrySet().stream()
                .filter(e -> !e.getValue().equals(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE) &&
                        !e.getValue().equals(JobConstants.GLOBAL_EVENT_JOB))
                .collect(Collectors.toSet()), "type");
        this.addGridFiltering(headerRow, searchFilter::setContextSearchFilter, "componentName");

        TextField jobSearchTf = new TextField(getTranslation("label.job-search"));
        jobSearchTf.setWidth("500px");
        jobSearchTf.getElement().getThemeList().add("always-float-label");
        jobSearchTf.setErrorMessage(getTranslation("error.job-name-cannot-be-null"));
        jobSearchTf.setValue(searchFilter.getJobNameFilter());

        Button jobSearchButton = new Button();
        jobSearchButton.getElement().appendChild(VaadinIcon.SEARCH.create().getElement());
        jobSearchButton.addClickListener(event -> {
            if(jobSearchTf.getValue() == null || jobSearchTf.getValue().isEmpty()) {
                jobSearchTf.setInvalid(true);
                return;
            }
            else {
                jobSearchTf.setInvalid(false);
            }
            this.searchFilter.setJobNameFilter(jobSearchTf.getValue());
            this.schedulerJobRecordGrid.getDataProvider().refreshAll();
        });

        HorizontalLayout globalJobSearchLayout = new HorizontalLayout(jobSearchTf, jobSearchButton);

        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();

        verticalLayout.add(globalJobSearchLayout, this.schedulerJobRecordGrid);
        this.content.add(verticalLayout);

        this.setWidth("95vw");
        this.setHeight("95vh");
    }

    /**
     * Adds select filtering functionality to the specified column in the header row of the grid.
     *
     * @param hr The HeaderRow where the filtering component will be added
     * @param setFilter The Consumer functional interface to set the filter function for the column
     * @param options Set of options available for selection in the filter
     * @param columnKey The key of the column where filtering will be applied
     */
    public void addSelectGridFiltering(HeaderRow hr, Consumer<String> setFilter, Set<Map.Entry<String, String>> options, String columnKey) {
        Select<Map.Entry<String, String>> select = new Select<>();
        select.setItems(options);
        select.setWidthFull();
        select.setEmptySelectionAllowed(true);
        select.setItemLabelGenerator(entry -> {
            if(entry == null) {
                return "";
            }

            return entry.getKey();
        });

        select.addValueChangeListener(ev-> {

            if(ev.getValue() != null) {
                setFilter.accept(ev.getValue().getValue());
            }
            else {
                setFilter.accept(null);
            }

            filteredDataProvider.refreshAll();
        });

        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");

        HorizontalLayout layout = new HorizontalLayout(select, filterIcon);
        layout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, filterIcon);

        hr.getCell(this.schedulerJobRecordGrid.getColumnByKey(columnKey)).setComponent(layout);
    }


    /**
     * Adds filtering functionality to the specified column in the header row of the grid.
     *
     * @param hr The HeaderRow where the filtering component will be added
     * @param setFilter The Consumer functional interface to set the filter function for the column
     * @param columnKey The key of the column where filtering will be applied
     */
    private void addGridFiltering(HeaderRow hr, Consumer<String> setFilter, String columnKey) {
        TextField textField = new TextField();
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        textField.setSuffixComponent(filterIcon);
        textField.setWidthFull();

        textField.addValueChangeListener(ev->{

            setFilter.accept(ev.getValue());

            filteredDataProvider.refreshAll();
        });

        hr.getCell(this.schedulerJobRecordGrid.getColumnByKey(columnKey)).setComponent(textField);
    }


    /**
     * Initializes the data provider for the JobSearchDialog.
     *
     * This method sets up the data provider using provided filtering callbacks to retrieve SchedulerJobRecord objects
     * based on user queries, offset, limit, and sorting orders. It also configures the filter and associates it with the
     * SchedulerJobRecordGrid component in the dialog.
     */
    public void initDataProvider() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<SchedulerJobSearchFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            List<SchedulerJobRecord> results = null;

            if(query.getSortOrders().size() > 0) {
                results = this.getResults(filter.get(), offset, limit, query.getSortOrders().get(0).getSorted(),
                    query.getSortOrders().get(0).getDirection().name());
            }
            else {
                results = this.getResults(filter.get(), offset, limit, null, null);
            }

            return results.stream();
        }, query -> {
            Optional<SchedulerJobSearchFilter> filter = query.getFilter();

            List<SchedulerJobRecord> results = this.getResults(filter.get(), -1, -1, null, null);

            return results.size();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.schedulerJobRecordGrid.setDataProvider(filteredDataProvider);
    }

    /**
     * Retrieves a list of SchedulerJobRecord objects based on the provided search filter, offset, limit, sort column,
     * and sort direction.
     *
     * @param filter the filter to apply for the search
     * @param offset the starting index for the results
     * @param limit the maximum number of results to retrieve
     * @param sortColumn the column to sort the results by
     * @param sortDirection the direction in which to sort the results (e.g., ascending or descending)
     * @return a List of SchedulerJobRecord objects based on the filter criteria
     */
    private List<SchedulerJobRecord> getResults(SchedulerJobSearchFilter filter, int offset, int limit, String sortColumn
        , String sortDirection) {
        if(!((IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication())
            .hasGrantedAuthority(SecurityConstants.ALL_AUTHORITY)){
            filter.setContextNames(SecurityUtils
                .getAccessibleJobPlans((IkasanAuthentication)SecurityContextHolder.getContext().getAuthentication())
                .stream().collect(Collectors.toList()));
        }
        filter.setJobTypes(List.of(JobConstants.INTERNAL_EVENT_DRIVEN_JOB, JobConstants.FILE_EVENT_DRIVEN_JOB,
            JobConstants.LOCAL_EVENT_JOB, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB));
        return (List<SchedulerJobRecord>) this.schedulerJobService.findByFilter(filter, limit, offset, sortColumn, sortDirection)
            .getResultList();
    }
}