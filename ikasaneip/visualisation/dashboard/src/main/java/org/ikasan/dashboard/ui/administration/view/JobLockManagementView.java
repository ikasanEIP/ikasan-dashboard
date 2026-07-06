package org.ikasan.dashboard.ui.administration.view;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.broadcast.JobLockCacheEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.service.JobLockCacheManagementServiceImpl;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Route(value = "jobLockView", layout = IkasanAppLayout.class)
@UIScope
@PageTitle("Ikasan - Administration Job Lock Cache Management")
@PermitAll
@PreserveOnRefresh
@Component
public class JobLockManagementView extends VerticalLayout implements BeforeEnterObserver, JobLockCacheEventLocalBroadcastListener
{
    private Logger logger = LoggerFactory.getLogger(JobLockManagementView.class);

    private DataProvider<EnvironmentCacheItem, JobLockFilter> dataProvider;
    private ConfigurableFilterDataProvider<EnvironmentCacheItem, Void, JobLockFilter> filteredDataProvider;

    private JobLockFilter searchFilter;
    private Grid<EnvironmentCacheItem> environmentCacheItemGrid;
    private IkasanAuthentication ikasanAuthentication;
    private JobLockCacheManagementService jobLockCacheManagementService = new JobLockCacheManagementServiceImpl();

    @Autowired
    private SystemEventLogger systemEventLogger;

    /**
     * Constructor
     */
    public JobLockManagementView()
    {
        super();
    }

    /**
     * Initializes the job lock management view by setting up the necessary UI components,
     * including the grid layout, columns, headers, and button actions for refreshing and releasing locked jobs.
     */
    protected void init()
    {
        this.ikasanAuthentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.environmentCacheItemGrid = new Grid<>();
        this.environmentCacheItemGrid.setSizeFull();
        this.setSizeFull();

        this.searchFilter = new JobLockFilter();

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();

        headerLayout.add(new VerticalLayout(new H4(getTranslation("header.job-lock-management", UI.getCurrent().getLocale()))));

        Button refreshButton = new Button(getTranslation("button.refresh", UI.getCurrent().getLocale()), VaadinIcon.REFRESH.create());
        refreshButton.addClickListener(event -> this.environmentCacheItemGrid.getDataProvider().refreshAll());

        VerticalLayout buttonWrapperLayout = new VerticalLayout();
        buttonWrapperLayout.setWidthFull();
        HorizontalLayout buttonLayout = new HorizontalLayout(refreshButton);
        buttonLayout.setHeightFull();
        buttonWrapperLayout.add(buttonLayout);
        buttonWrapperLayout.setHorizontalComponentAlignment(Alignment.END, buttonLayout);

        headerLayout.add(buttonWrapperLayout);

        this.add(headerLayout, environmentCacheItemGrid);

        UI ui = UI.getCurrent();

        this.environmentCacheItemGrid.addColumn(new ComponentRenderer<>(
                environmentCacheItem -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(environmentCacheItem.getEnvironment());

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.job-lock-environment", UI.getCurrent().getLocale()))
            .setKey("environment")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);
        this.environmentCacheItemGrid.addColumn(new ComponentRenderer<>(
                environmentCacheItem -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(environmentCacheItem.getJobLockHolder().getLockName());

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.lock-name", UI.getCurrent().getLocale()))
            .setKey("lockName")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(4);
        this.environmentCacheItemGrid.addColumn(new ComponentRenderer<>(
                environmentCacheItem -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    verticalLayout.add(String.valueOf(environmentCacheItem.getJobLockHolder().getLockCount()));

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.lock-count", UI.getCurrent().getLocale()))
            .setKey("lockCount")
            .setSortable(true)
            .setResizable(true)
            .setFlexGrow(1);
        this.environmentCacheItemGrid.addColumn(new ComponentRenderer<>(
                environmentCacheItem -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    if(!environmentCacheItem.getJobLockHolder().getLockHolders().isEmpty()) {
                        environmentCacheItem.getJobLockHolder().getLockHolders().forEach(jlh -> {
                            String jobIdentifier = jlh.substring(0, jlh.indexOf(":"));
                            String context = jlh.substring(jlh.lastIndexOf(":") + 1, jlh.length());
                            Button lockHolderButton = new Button(jobIdentifier);
                            lockHolderButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_RUNNING);
                            lockHolderButton.getElement().getStyle().set("color", IkasanColours.WHITE);
                            lockHolderButton.getElement().getStyle().set("margin-bottom", "5px");
                            lockHolderButton.setIcon(VaadinIcon.SITEMAP.create());

                            Button releaseLockedJobButton = new Button(getTranslation("button.job-lock-cache-release", UI.getCurrent().getLocale()));
                            releaseLockedJobButton.setIcon(VaadinIcon.HANDS_UP.create());
                            releaseLockedJobButton.getElement().getStyle().set("margin-bottom", "5px");
                            releaseLockedJobButton.getElement().getStyle().set("margin-left", "auto");
                            releaseLockedJobButton.getElement().setAttribute("title", getTranslation("tooltip.job-lock-cache-release", UI.getCurrent().getLocale()));
                            releaseLockedJobButton.addClickListener(event -> {
                                ConfirmDialog confirmDialog = new ConfirmDialog();
                                confirmDialog.setHeader(getTranslation("confirm-dialog.job-lock-cache-release-header", UI.getCurrent().getLocale()));
                                confirmDialog.setText(getTranslation("confirm-dialog.job-lock-cache-release-text", UI.getCurrent().getLocale()));
                                confirmDialog.setCancelable(true);
                                confirmDialog.open();

                                confirmDialog.addConfirmListener(confirmEvent -> {
                                    ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);

                                    progressIndicatorDialog.open(getTranslation("message.releasing-locked-scheduler-job-header")
                                        , getTranslation("message.releasing-locked-scheduler-job-test"));

                                    Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("JobLockCacheDialog"));
                                    executor.execute(() -> {
                                        boolean error = false;
                                        try {
                                            this.jobLockCacheManagementService.releaseLockedJob(jobIdentifier, context, environmentCacheItem.getEnvironment());
                                            systemEventLogger.logEvent(SystemEventConstants.LOCKED_JOB_RELEASE
                                                , String.format("Released lock on job [%s] child context [%s], environment [%s]."
                                                    , jobIdentifier, context, environmentCacheItem.getEnvironment()), ikasanAuthentication.getName());
                                        } catch (Exception e) {
                                            error = true;
                                            ui.access(() -> NotificationHelper.showErrorNotification((String.format(getTranslation
                                                ("error.unable-to-release-jocked-job"), jobIdentifier))));
                                        }

                                        if (!error) {
                                            ui.access(() -> {
                                                this.environmentCacheItemGrid.getDataProvider().refreshAll();
                                                NotificationHelper.showUserNotification(String.format(getTranslation
                                                    ("message.locked-job-successfully-released"), jobIdentifier));
                                            });
                                        }
                                        environmentCacheItemGrid.getDataProvider().refreshAll();
                                        progressIndicatorDialog.close();
                                    });
                                });
                            });


                            HorizontalLayout lockHolderButtonsLayout = new HorizontalLayout();
                            lockHolderButtonsLayout.setWidthFull();
                            lockHolderButtonsLayout.add(lockHolderButton, releaseLockedJobButton);
                            lockHolderButtonsLayout.setAlignSelf(FlexComponent.Alignment.END, releaseLockedJobButton);

                            verticalLayout.add(lockHolderButtonsLayout);
                        });
                    }
                    else {
                        verticalLayout.add(new Text(getTranslation("label.no-current-lock-holders", UI.getCurrent().getLocale())));
                    }

                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.current-lock-holders", UI.getCurrent().getLocale()))
            .setKey("lockHolders")
            .setSortable(true)
            .setResizable(true)
            .setFlexGrow(9);
        this.environmentCacheItemGrid.addColumn(new ComponentRenderer<>(
                environmentCacheItem -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    VerticalLayout queuedJobsLayout = new VerticalLayout();
                    queuedJobsLayout.setWidthFull();
                    queuedJobsLayout.setPadding(false);

                    if(!environmentCacheItem.getJobLockHolder().getSchedulerJobInitiationEventWaitQueue().isEmpty()) {
                        environmentCacheItem.getJobLockHolder().getSchedulerJobInitiationEventWaitQueue().forEach(schedulerJobInitiationEvent -> {
                            HorizontalLayout queuedJobLayout = new HorizontalLayout();
                            queuedJobLayout.setWidthFull();
                            queuedJobLayout.setPadding(false);
                            Button queuedJobButton = new Button(schedulerJobInitiationEvent.getSchedulerJobInitiationEvent()
                                .getInternalEventDrivenJob().getJobName());

                            queuedJobButton.setIcon(VaadinIcon.SITEMAP.create());
                            queuedJobButton.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_LOCK_QUEUED);
                            queuedJobButton.getElement().getStyle().set("color", IkasanColours.WHITE);
                            queuedJobButton.getElement().getStyle().set("margin-bottom", "5px");
                            queuedJobButton.getElement().setAttribute("title",
                                getTranslation("label.context-name") + ": " + schedulerJobInitiationEvent.getSchedulerJobInitiationEvent()
                                    .getInternalEventDrivenJob().getContextName() +
                                    "\n" + getTranslation("label.child-context-name") + ": " + schedulerJobInitiationEvent.getSchedulerJobInitiationEvent()
                                    .getInternalEventDrivenJob().getChildContextName() +
                                    "\n" + getTranslation("label.context-instance-id") + ": " + schedulerJobInitiationEvent.getSchedulerJobInitiationEvent()
                                    .getContextInstanceId());

                            Button removeQueuedJobButton = new Button(getTranslation("button.job-lock-cache-remove", UI.getCurrent().getLocale()));
                            removeQueuedJobButton.setIcon(VaadinIcon.TRASH.create());
                            removeQueuedJobButton.getElement().getStyle().set("margin-bottom", "5px");
                            removeQueuedJobButton.getElement().getStyle().set("margin-left", "auto");
                            removeQueuedJobButton.getElement().setAttribute("title", getTranslation("tooltip.job-lock-cache-remove", UI.getCurrent().getLocale()));
                            removeQueuedJobButton.addClickListener(event -> {
                                ConfirmDialog confirmDialog = new ConfirmDialog();
                                confirmDialog.setHeader(getTranslation("confirm-dialog.job-lock-cache-remove-header", UI.getCurrent().getLocale()));
                                confirmDialog.setText(getTranslation("confirm-dialog.job-lock-cache-remove-text", UI.getCurrent().getLocale()));
                                confirmDialog.setCancelable(true);
                                confirmDialog.open();

                                confirmDialog.addConfirmListener(confirmEvent -> {
                                    ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);

                                    progressIndicatorDialog.open(getTranslation("message.removing-queued-scheduler-job-header")
                                        , getTranslation("message.removing-queued-scheduler-job-test"));

                                    Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("JobLockCacheDialog"));
                                    executor.execute(() -> {
                                        this.jobLockCacheManagementService.removeQueuedSchedulerJobInitiationEvent
                                            (schedulerJobInitiationEvent.getSchedulerJobInitiationEvent(), environmentCacheItem.getEnvironment());

                                        ui.access(() -> NotificationHelper.showUserNotification(String.format(getTranslation("message.queued-job-successfully-removed-from-job-lock-queue")
                                            , schedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getJobName())));
                                        systemEventLogger.logEvent(SystemEventConstants.QUEUED_JOB_DEQUEUED
                                            , String.format("Removed job [%s] from job lock wait queue for job plan [%s], job plan instance id [%s]."
                                                , schedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getJobName()
                                                , schedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getContextName()
                                                , schedulerJobInitiationEvent.getSchedulerJobInitiationEvent().getContextInstanceId()), ikasanAuthentication.getName());
                                        progressIndicatorDialog.close();
                                        environmentCacheItemGrid.getDataProvider().refreshAll();
                                    });
                                });
                            });
                            queuedJobLayout.add(queuedJobButton, removeQueuedJobButton);
                            queuedJobLayout.setAlignSelf(FlexComponent.Alignment.END, removeQueuedJobButton);
                            queuedJobsLayout.add(queuedJobLayout);
                        });
                    }
                    else {
                        verticalLayout.add(new Text(getTranslation("label.no-queued-jobs", UI.getCurrent().getLocale())));
                    }
                    verticalLayout.add(queuedJobsLayout);
                    return verticalLayout;
                }))
            .setHeader(getTranslation("table-header.queued-jobs-waiting-for-lock", UI.getCurrent().getLocale()))
            .setKey("queuedJobs")
            .setSortable(true)
            .setResizable(true)
            .setFlexGrow(9);
        this.environmentCacheItemGrid.addColumn(new ComponentRenderer<>(
                environmentCacheItem -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    Button endSessionButton = new Button(getTranslation("button.reset"));
                    endSessionButton.addClickListener(event -> {
                        ConfirmDialog confirmDialog = new ConfirmDialog();
                        confirmDialog.setHeader(getTranslation("confirm-dialog.reset-job-lock-header", UI.getCurrent().getLocale()));
                        confirmDialog.setText(getTranslation("confirm-dialog.reset-job-lock-text", UI.getCurrent().getLocale()));
                        confirmDialog.setCancelable(true);
                        confirmDialog.setConfirmText(getTranslation("button.ok"));
                        confirmDialog.setCancelText(getTranslation("button.cancel"));
                        confirmDialog.open();

                        confirmDialog.addConfirmListener(confirmEvent -> {
                            JobLockCacheImpl.instance().resetLock(environmentCacheItem.getJobLockHolder().getLockName(),
                                environmentCacheItem.getEnvironment());
                            NotificationHelper.showUserNotification(getTranslation("notification.job-lock-reset"));
                            this.environmentCacheItemGrid.getDataProvider().refreshAll();
                        });
                    });
                    verticalLayout.add(endSessionButton);

                    return verticalLayout;
                }))
            .setKey("endSession")
            .setSortable(false)
            .setFlexGrow(1);

        this.populateGrid();
    }

    /**
     * Populates the grid with data by setting up the data provider based on filtering callbacks.
     * Retrieves results based on filter, offset, limit, and sort orders specified in the query.
     * Sets up grid filtering components for environment and job lock name.
     *
     * @see JobLockFilter
     * @see EnvironmentCacheItem
     */
    private void populateGrid() {
        dataProvider = DataProvider.fromFilteringCallbacks(query -> {
            Optional<JobLockFilter> filter = query.getFilter();

            // The index of the first item to load
            int offset = query.getOffset();

            // The number of items to load
            int limit = query.getLimit();

            List<EnvironmentCacheItem> results;

            if(query.getSortOrders().size() > 0) {
                results = this.getResults(filter.get(), offset, limit, query.getSortOrders().get(0).getSorted(),
                    query.getSortOrders().get(0).getDirection().name());
            }
            else {
                results = this.getResults(filter.get(), offset, limit, null, null);
            }

            return results.stream();
        }, query -> {
            Optional<JobLockFilter> filter = query.getFilter();

            List<EnvironmentCacheItem> results = this.getResults(filter.get(), -1, -1, null, null);

            return results.size();
        });

        filteredDataProvider = dataProvider.withConfigurableFilter();
        filteredDataProvider.setFilter(this.searchFilter);

        this.environmentCacheItemGrid.setDataProvider(filteredDataProvider);

        HeaderRow hr = this.environmentCacheItemGrid.appendHeaderRow();
        this.addGridFiltering(hr, this.searchFilter::setEnvironment, "environment");
        this.addGridFiltering(hr, this.searchFilter::setJobLockName, "lockName");
    }

    /**
     * Adds grid filtering functionality by creating a text field with a filter icon as a suffix component.
     * The text field listens for value changes and triggers the provided setFilter consumer function with the new value.
     * It then refreshes the data provider to reflect the filtered results.
     *
     * @param hr The HeaderRow where the filtering component should be added.
     * @param setFilter The consumer function to set the filter value based on the text field input.
     * @param columnKey The key of the column to filter in the grid.
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

        hr.getCell(this.environmentCacheItemGrid.getColumnByKey(columnKey)).setComponent(textField);
    }

    /**
     * Retrieves a filtered and sorted list of EnvironmentCacheItem objects based on the provided criteria.
     *
     * @param jobLockFilter The filter criteria to apply to the results.
     * @param offset The starting index of the results to retrieve.
     * @param limit The maximum number of results to return.
     * @param sortColumn The column by which to sort the results.
     * @param sortOrder The order in which to sort the results (ASCENDING or DESCENDING).
     * @return A list of EnvironmentCacheItem objects that meet the filtering and sorting criteria.
     */
    private List<EnvironmentCacheItem> getResults(JobLockFilter jobLockFilter, int offset, int limit, String sortColumn, String sortOrder) {
        List<EnvironmentCacheItem> items = new ArrayList<>();

        try {
            for (String env : JobLockCacheImpl.instance().getEnvironments()) {
                for (JobLockHolder jobLockHolder : JobLockCacheImpl.instance()
                    .getJobLockCacheData(env).getJobLocksByLockName().values()) {
                    items.add(new EnvironmentCacheItem(env, jobLockHolder));
                }
            }

            if(jobLockFilter != null) {
                if(jobLockFilter.getEnvironment() != null && !jobLockFilter.getEnvironment().isEmpty()) {
                    items = items.stream()
                        .filter(item -> item.getEnvironment().toLowerCase().contains(jobLockFilter.getEnvironment().toLowerCase()))
                        .collect(Collectors.toList());
                }

                if(jobLockFilter.getJobLockName() != null && !jobLockFilter.getJobLockName().isEmpty()) {
                    items = items.stream()
                        .filter(item ->item.getJobLockHolder().getLockName()
                            .toLowerCase().contains(jobLockFilter.getJobLockName().toLowerCase()))
                        .collect(Collectors.toList());
                }
            }

            if(sortColumn != null && sortOrder != null) {
                items = items.stream().sorted((o1, o2) -> {
                    if (sortOrder.equals("ASCENDING")) {
                        if(sortColumn.equals("environment")) {
                            return o1.getEnvironment().toLowerCase().compareTo(o2.getEnvironment().toLowerCase());
                        }
                        else if(sortColumn.equals("lockName")) {
                            return o1.getJobLockHolder().getLockName().toLowerCase()
                                .compareTo(o2.getJobLockHolder().getLockName().toLowerCase());
                        }
                        else if(sortColumn.equals("lockCount")) {
                             return o1.getJobLockHolder().getLockCount() < o2.getJobLockHolder().getLockCount() ? 1 : -1;
                        }
                        else if(sortColumn.equals("lockHolders")) {
                            return o1.getJobLockHolder().getLockHolders().size() < o2.getJobLockHolder().getLockHolders().size() ? 1 : -1;
                        }
                        else if(sortColumn.equals("queuedJobs")) {
                            return o1.getJobLockHolder().getSchedulerJobInitiationEventWaitQueue().size()
                                < o2.getJobLockHolder().getSchedulerJobInitiationEventWaitQueue().size() ? 1 : -1;
                        }
                    }
                    else if (sortOrder.equals("DESCENDING")) {
                        if(sortColumn.equals("environment")) {
                            return o2.getEnvironment().toLowerCase().compareTo(o1.getEnvironment().toLowerCase());
                        }
                        else if(sortColumn.equals("lockName")) {
                            return o2.getJobLockHolder().getLockName().toLowerCase()
                                .compareTo(o1.getJobLockHolder().getLockName().toLowerCase());
                        }
                        else if(sortColumn.equals("lockCount")) {
                            return o2.getJobLockHolder().getLockCount() < o1.getJobLockHolder().getLockCount() ? 1 : -1;
                        }
                        else if(sortColumn.equals("lockHolders")) {
                            return o2.getJobLockHolder().getLockHolders().size()
                                < o1.getJobLockHolder().getLockHolders().size() ? 1 : -1;
                        }
                        else if(sortColumn.equals("queuedJobs")) {
                            return o2.getJobLockHolder().getSchedulerJobInitiationEventWaitQueue().size()
                                < o1.getJobLockHolder().getSchedulerJobInitiationEventWaitQueue().size() ? 1 : -1;
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
            logger.error("An error has occurred attempting to resolve job lock cache items!", e);
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

        if(this.environmentCacheItemGrid == null) {
            init();
        }
    }

    private class JobLockFilter {
        private String environment;
        private String jobLockName;

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public String getJobLockName() {
            return jobLockName;
        }

        public void setJobLockName(String jobLockName) {
            this.jobLockName = jobLockName;
        }
    }

    private class EnvironmentCacheItem {
        private String environment;
        private JobLockHolder jobLockHolder;

        public EnvironmentCacheItem(String environment, JobLockHolder jobLockHolder) {
            this.environment = environment;
            this.jobLockHolder = jobLockHolder;
        }

        /**
         * Retrieves the current environment that the software is running in.
         *
         * @return the current environment as a String
         */
        public String getEnvironment() {
            return environment;
        }

        /**
         * Retrieves the JobLockHolder associated with the current environment cache item.
         *
         * @return the JobLockHolder object representing the lock holder for the job
         */
        public JobLockHolder getJobLockHolder() {
            return jobLockHolder;
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        JobLockCacheEventBroadcaster.instance().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        JobLockCacheEventBroadcaster.instance().unregister(this);
    }


    @Override
    public void receiveBroadcast(JobLockCacheEvent event) {
        this.environmentCacheItemGrid.getDataProvider().refreshAll();
    }
}
