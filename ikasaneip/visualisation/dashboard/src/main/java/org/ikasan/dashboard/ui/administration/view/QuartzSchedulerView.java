package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.DashboardContextNavigator;
import org.ikasan.dashboard.ui.util.DateFormatter;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Route(value = "quartzSchedulerView", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Administration Quartz Scheduler")
@PermitAll
@PreserveOnRefresh
public class QuartzSchedulerView extends VerticalLayout implements BeforeEnterObserver
{
    private Logger logger = LoggerFactory.getLogger(QuartzSchedulerView.class);

    private DataProvider<Trigger, TriggerFilter> dataProvider;
    private ConfigurableFilterDataProvider<Trigger, Void, TriggerFilter> filteredDataProvider;

    @Resource
    private Scheduler scheduler;

    private TriggerFilter searchFilter;

    private Grid<Trigger> triggerGrid;

    /**
     * Constructor
     */
    public QuartzSchedulerView()
    {
        super();
    }

    protected void init()
    {
        this.triggerGrid = new Grid<>();
        this.triggerGrid.setSizeFull();
        this.setSizeFull();

        this.searchFilter = new TriggerFilter();

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();

        headerLayout.add(new VerticalLayout(new H4(getTranslation("header.quartz-scheduler-jobs", UI.getCurrent().getLocale()))));

        Button refreshButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(event -> this.triggerGrid.getDataProvider().refreshAll());

        VerticalLayout buttonLayout = new VerticalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.add(refreshButton);
        buttonLayout.setHorizontalComponentAlignment(Alignment.END, refreshButton);

        headerLayout.add(buttonLayout);

        this.add(headerLayout, triggerGrid);

        this.triggerGrid.addColumn(new ComponentRenderer<>(
                trigger -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(new Text(trigger.getKey().getName()));

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setKey("jobName")
            .setSortable(true)
            .setFlexGrow(1);
        this.triggerGrid.addColumn(new ComponentRenderer<>(
                trigger -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(new Text(trigger.getKey().getGroup()));

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.job-group", UI.getCurrent().getLocale()))
            .setKey("jobType")
            .setSortable(true)
            .setFlexGrow(1);
        this.triggerGrid.addColumn(new ComponentRenderer<>(
                trigger -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    if(trigger.getNextFireTime() != null) {
                        verticalLayout.add(new Text(DateFormatter.instance()
                            .getFormattedDate(trigger.getNextFireTime().getTime())));
                    }

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.next-fire-time", UI.getCurrent().getLocale()))
            .setKey("nextFireTime")
            .setSortable(true)
            .setFlexGrow(1);
        this.triggerGrid.addColumn(new ComponentRenderer<>(
                trigger -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    if(trigger.getPreviousFireTime() != null) {
                        verticalLayout.add(new Text(DateFormatter.instance()
                            .getFormattedDate(trigger.getPreviousFireTime().getTime())));
                    }

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.previous-fire-time", UI.getCurrent().getLocale()))
            .setKey("previousFireTime")
            .setSortable(true)
            .setFlexGrow(1);

        this.populateGrid();
    }

    private void populateGrid() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<TriggerFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            List<Trigger> results;

            if(query.getSortOrders().size() > 0) {
                results = this.getResults(filter.get(), offset, limit, query.getSortOrders().get(0).getSorted(),
                    query.getSortOrders().get(0).getDirection().name());
            }
            else {
                results = this.getResults(filter.get(), offset, limit, null, null);
            }

            return results.stream();
        }, query -> {
            Optional<TriggerFilter> filter = query.getFilter();

            List<Trigger> results = this.getResults(filter.get(), -1, -1, null, null);

            return results.size();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.triggerGrid.setDataProvider(filteredDataProvider);

        HeaderRow hr = this.triggerGrid.appendHeaderRow();
        this.addGridFiltering(hr, this.searchFilter::setJobName, "jobName");
        this.addGridFiltering(hr, this.searchFilter::setJobType, "jobType");
    }

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

        hr.getCell(this.triggerGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    private List<Trigger> getResults(TriggerFilter triggerFilter, int offset, int limit, String sortColumn, String sortOrder) {
        List<Trigger> items = new ArrayList<>();

        try {
            for (String groupName : scheduler.getJobGroupNames()) {

                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
                    items.add(triggers.get(0));
                }
            }

            if(triggerFilter != null) {
                if(triggerFilter.getJobName() != null && !triggerFilter.getJobName().isEmpty()) {
                    items = items.stream()
                        .filter(item -> item.getKey().getName().toLowerCase().contains(triggerFilter.getJobName().toLowerCase()))
                        .collect(Collectors.toList());
                }

                if(triggerFilter.getJobType() != null && !triggerFilter.getJobType().isEmpty()) {
                    items = items.stream()
                        .filter(item -> item.getKey().getGroup().toLowerCase().contains(triggerFilter.getJobType().toLowerCase()))
                        .collect(Collectors.toList());
                }
            }

            if(sortColumn != null && sortOrder != null) {
                items = items.stream().sorted((o1, o2) -> {
                    if (sortOrder.equals("ASCENDING")) {
                        if(sortColumn.equals("jobName")) {
                            return o1.getKey().getName().toLowerCase().compareTo(o2.getKey().getName().toLowerCase());
                        }
                        else if(sortColumn.equals("jobType")) {
                            return o1.getKey().getGroup().toLowerCase().compareTo(o2.getKey().getGroup().toLowerCase());
                        }
                        else if(sortColumn.equals("nextFireTime")) {
                            return o1.getNextFireTime().compareTo(o2.getNextFireTime());
                        }
                        else if(sortColumn.equals("previousFireTime")) {
                            if(o1.getPreviousFireTime() != null && o2.getPreviousFireTime() != null) {
                                return o1.getPreviousFireTime().compareTo(o2.getPreviousFireTime());
                            }
                            else {
                                return -1;
                            }
                        }
                    }
                    else if (sortOrder.equals("DESCENDING")) {
                        if(sortColumn.equals("jobName")) {
                            return o2.getKey().getName().toLowerCase().compareTo(o1.getKey().getName().toLowerCase());
                        }
                        else if(sortColumn.equals("jobType")) {
                            return o2.getKey().getGroup().toLowerCase().compareTo(o1.getKey().getGroup().toLowerCase());
                        }
                        else if(sortColumn.equals("nextFireTime")) {
                            return o2.getNextFireTime().compareTo(o1.getNextFireTime());
                        }
                        else if(sortColumn.equals("previousFireTime")) {
                            if(o1.getPreviousFireTime() != null && o2.getPreviousFireTime() != null) {
                                return o2.getPreviousFireTime().compareTo(o1.getPreviousFireTime());
                            }
                            else {
                                return -1;
                            }
                        }
                    }

                    return 0;
                }).collect(Collectors.toList());
            }

            if(offset >= 0 && limit > 0 && offset + limit >= items.size()) {
                items = items.subList(offset, items.size());
            }
            else if(offset >= 0 && limit > 0 && offset + limit < items.size()) {
                items = items.subList(offset, offset + limit);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        return items;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent)
    {
        if(!ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.SCHEDULER_READ, SecurityConstants.SCHEDULER_WRITE,
            SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_ALL_WRITE,
            SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_DEV_READ, SecurityConstants.SCHEDULER_DEV_WRITE,
            SecurityConstants.SCHEDULER_DEV_ADMIN, SecurityConstants.ALL_AUTHORITY)) {
            DashboardContextNavigator.navigateToLandingPage();
            return;
        }

        if(this.triggerGrid == null) {
            init();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent)
    {

    }

    private class TriggerFilter {
        private String jobName;
        private String jobType;

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }

        public String getJobType() {
            return jobType;
        }

        public void setJobType(String jobType) {
            this.jobType = jobType;
        }
    }
}
