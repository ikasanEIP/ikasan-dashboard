package org.ikasan.dashboard.ui.scheduler.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobTemplateVisualisationDialog;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.scheduled.job.model.SolrSchedulerJobSearchFilterImpl;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobLockManagementDialog extends AbstractCloseableResizableDialog implements SchedulerJobSelectedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLockManagementDialog.class);
    private ContextTemplate contextTemplate;
    private String dynamicImagePath = ".";
    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private SchedulerJobService schedulerJobService;
    private LogStreamingService logStreamingService;
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;
    private UserService userService;
    private SecurityService securityService;

    private JobProvisionService jobProvisionService;
    private ScheduledContextService scheduledContextService;

    private ComboBox<JobLock> comboBox;
    private Grid<SchedulerJob> grid;

    private TextField filterTf = new TextField();

    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private String initialJobLocks = "";

    public JobLockManagementDialog(ContextTemplate contextTemplate, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                   ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                   MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                   SchedulerJobService schedulerJobService, LogStreamingService logStreamingService,
                                   JobInitiationService jobInitiationService,
                                   ContextProfileService contextProfileService, UserService userService, SecurityService securityService,
                                   JobProvisionService jobProvisionService,
                                   ScheduledContextService scheduledContextService) {
        this.contextTemplate = contextTemplate;
        if(this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }

        this.scheduledProcessManagementService = scheduledProcessManagementService;
        if(this.scheduledProcessManagementService == null) {
            throw new IllegalArgumentException("scheduledProcessManagementService cannot be null!");
        }

        this.configurationRestService = configurationRestService;
        if(this.configurationRestService == null) {
            throw new IllegalArgumentException("configurationRestService cannot be null!");
        }

        this.moduleControlRestService = moduleControlRestService;
        if(this.moduleControlRestService == null) {
            throw new IllegalArgumentException("moduleControlRestService cannot be null!");
        }

        this.metaDataRestService = metaDataRestService;
        if(this.metaDataRestService == null) {
            throw new IllegalArgumentException("metaDataRestService cannot be null!");
        }

        this.systemEventLogger = systemEventLogger;
        if(this.systemEventLogger == null) {
            throw new IllegalArgumentException("systemEventLogger cannot be null!");
        }

        this.schedulerJobService = schedulerJobService;
        if(this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.logStreamingService = logStreamingService;
        if(this.logStreamingService == null) {
            throw new IllegalArgumentException("logStreamingService cannot be null!");
        }

        this.jobInitiationService = jobInitiationService;
        if(this.jobInitiationService == null) {
            throw new IllegalArgumentException("jobInitiationService cannot be null!");
        }

        this.contextProfileService = contextProfileService;
        if(this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }

        this.userService = userService;
        if(this.userService == null) {
            throw new IllegalArgumentException("userService cannot be null!");
        }

        this.securityService = securityService;
        if(this.securityService == null) {
            throw new IllegalArgumentException("securityService cannot be null!");
        }

        this.jobProvisionService = jobProvisionService;
        if(this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.init();
    }

    private void init() {

        try {
            this.initialJobLocks = this.objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(this.contextTemplate.getJobLocks());
        }
        catch (JsonProcessingException e) {
            LOGGER.info("Could not initialise job locks string for audit!");
        }

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        grid = new Grid<>();
        grid.addColumn(SchedulerJob::getJobName)
            .setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setKey("jobName")
            .setFlexGrow(8);
        grid.addColumn(new ComponentRenderer<>(
                job -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    if(job != null && job.getChildContextNames() != null) {
                        job.getChildContextNames().forEach(context -> {
                            Icon visualisation = IconDecorator.decorate(new Icon(VaadinIcon.SITEMAP), getTranslation("tooltip.open-visualisation", UI.getCurrent().getLocale())
                                , "14pt", "rgba(0, 0, 0, 1.0)");
                            Button contextButton = new Button(context);
                            contextButton.getElement().getStyle().set("font-size", "9pt");
                            contextButton.getElement().getStyle().set("color", "rgba(0, 0, 0, 1.0)");
                            contextButton.getElement().getStyle().set("margin-bottom", "5px");
                            contextButton.setIcon(visualisation);
                            contextButton.addClickListener(event -> {
                                try {
                                    JobTemplateVisualisationDialog jobTemplateVisualisationDialog = new JobTemplateVisualisationDialog(moduleMetaDataService, scheduledProcessManagementService,
                                        configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService,
                                        jobInitiationService, contextProfileService, userService, securityService,
                                        jobProvisionService, scheduledContextService);
                                    jobTemplateVisualisationDialog.createSchedulerVisualisation(contextTemplate, ContextHelper.getChildContextTemplate(context, contextTemplate));
                                    jobTemplateVisualisationDialog.open();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    NotificationHelper.showErrorNotification(getTranslation("error.cannot-open-visualisation", UI.getCurrent().getLocale()));
                                }

                            });

                            verticalLayout.add(contextButton);
                        });
                    }
                    return verticalLayout;
                }
            ))
            .setHeader(getTranslation("table-header.residing-contexts", UI.getCurrent().getLocale()))
            .setFlexGrow(8);
        grid.addColumn(new ComponentRenderer<>(job -> {
                VerticalLayout buttonLayout = new VerticalLayout();
                buttonLayout.setWidth("100%");

                Icon delete = IconDecorator.decorate(new Icon(VaadinIcon.TRASH), getTranslation("tooltip.delete-job-from-job-lock"
                    , UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                delete.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    JobLock jobLock = this.comboBox.getValue();
                    job.getChildContextNames().forEach(child -> {
                        List<SchedulerJob> jobs = jobLock.getJobs().get(child);
                        if(jobs!=null) {
                            jobs.remove(job);
                        }
                    });

                    if(this.saveContextTemplate()) {
                        this.updateScheduledJob(job, false);
                        this.populateGrid(jobLock, filterTf.getValue());
                        NotificationHelper.showUserNotification(String.format(getTranslation("notification.job-removed-from-lock"
                            , UI.getCurrent().getLocale()), job.getJobName(), jobLock.getName()));
                    }
                });

                buttonLayout.add(delete);
                buttonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, delete);

                return buttonLayout;
            }
        ))
        .setFlexGrow(1);

        grid.setHeight("400px");
        grid.setWidthFull();

        HeaderRow hr = grid.appendHeaderRow();
        this.addGridFiltering(hr, "jobName");

        comboBox = new ComboBox<>(getTranslation("label.job-lock", UI.getCurrent().getLocale()));
        comboBox.setHelperText(getTranslation("label.select-a-job-lock", UI.getCurrent().getLocale()));
        comboBox.setWidth("100%");
        comboBox.setItems(contextTemplate.getJobLocks());
        comboBox.setItemLabelGenerator(JobLock::getName);

        IntegerField lockCountTf = new IntegerField(getTranslation("label.lock-count", UI.getCurrent().getLocale()));
        lockCountTf.setHasControls(true);
        lockCountTf.setMin(1);
        lockCountTf.setEnabled(false);
        lockCountTf.addValueChangeListener(event -> {
           if(event.getValue() != null && event.getOldValue() != null && comboBox.getValue() != null) {
               this.updateLockCount(lockCountTf.getValue());
           }
        });

        Button newJobLockButton = new Button(getTranslation("button.new-job-lock", UI.getCurrent().getLocale()), VaadinIcon.LOCK.create());
        newJobLockButton.setIconAfterText(true);
        newJobLockButton.addClickListener(event -> {
            NewJobLockDialog newJobLockDialog = new NewJobLockDialog(this.contextTemplate.getJobLocksMap());
            newJobLockDialog.open();

            newJobLockDialog.addOpenedChangeListener(closeEvent -> {
                if(!closeEvent.isOpened() && newJobLockDialog.getLockName() != null) {
                    JobLock jobLock = new JobLockImpl();
                    jobLock.setName(newJobLockDialog.getLockName());
                    jobLock.setLockCount(newJobLockDialog.getLockCount());
                    jobLock.setJobs(new HashMap<>());

                    this.contextTemplate.getJobLocks().add(jobLock);
                    this.contextTemplate.getJobLocksMap().put(jobLock.getName(), jobLock);
                    comboBox.setItems(contextTemplate.getJobLocks());

                    comboBox.setValue(jobLock);
                    this.saveContextTemplate();
                }
            });
        });

        layout.add(newJobLockButton);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, newJobLockButton);

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setHeight("180px");
        formLayout.setResponsiveSteps(
            // Use four columns by default
            new FormLayout.ResponsiveStep("0", 4)
        );
        H3 jobLockManagementLabel = new H3(getTranslation("label.job-lock-management", UI.getCurrent().getLocale()));
        jobLockManagementLabel.getElement().getStyle().set("margin-top", "10px");
        formLayout.add(jobLockManagementLabel, 4);
        formLayout.add(comboBox, 2);
        formLayout.add(lockCountTf, 1);

        Button deleteJobLockButton = new Button(getTranslation("button.delete-selected-job-lock", UI.getCurrent().getLocale()), VaadinIcon.TRASH.create());
        deleteJobLockButton.setIconAfterText(true);
        deleteJobLockButton.setEnabled(false);
        deleteJobLockButton.addClickListener(event -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("confirm-dialog.header-delete-job-lock", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("confirm-dialog.body-delete-job-lock", UI.getCurrent().getLocale()));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(confirmEvent -> {
                this.contextTemplate.getJobLocks().remove(this.comboBox.getValue());
                this.contextTemplate.getJobLocksMap().remove(this.comboBox.getValue().getName());
                this.saveContextTemplate();
                this.comboBox.setItems(this.contextTemplate.getJobLocks());
                grid.setItems(new ArrayList<>());
                lockCountTf.setValue(null);
            });
        });

        VerticalLayout deleteButtonLayout = new VerticalLayout();
        deleteButtonLayout.setSpacing(false);
        deleteButtonLayout.setPadding(false);
        deleteButtonLayout.add(deleteJobLockButton);
        deleteButtonLayout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, deleteJobLockButton);
        formLayout.add(deleteButtonLayout, 1);

        Button addJobButton = new Button(getTranslation("button.add-job-to-lock", UI.getCurrent().getLocale()), VaadinIcon.PLUS.create());
        addJobButton.setIconAfterText(true);
        addJobButton.setEnabled(false);
        addJobButton.addClickListener(event -> {
            SchedulerJobSearchFilter filter = new SolrSchedulerJobSearchFilterImpl();
            filter.setJobTypeFilter(JobConstants.INTERNAL_EVENT_DRIVEN_JOB);
            filter.setParticipatesInLock(Boolean.FALSE);
            FilteredSchedulerJobSelectDialog filteredSchedulerJobSelectDialog
                = new FilteredSchedulerJobSelectDialog(this.schedulerJobService, this.contextTemplate, filter
                    , getTranslation("label.select-job", UI.getCurrent().getLocale()), getTranslation("label.select-job", UI.getCurrent().getLocale()));
            filteredSchedulerJobSelectDialog.open();
            filteredSchedulerJobSelectDialog.addSchedulerJobSelectedListener(this);
        });

        layout.add(formLayout, addJobButton, grid);

        comboBox.addValueChangeListener(event -> {
            lockCountTf.setValue(null);
            if(event.getValue() != null) {
                lockCountTf.setEnabled(true);
                lockCountTf.setValue(event.getValue().getLockCount());
                this.populateGrid(event.getValue(), filterTf.getValue());
                addJobButton.setEnabled(true);
                deleteJobLockButton.setEnabled(true);
            }
            else {
                lockCountTf.setEnabled(false);
                addJobButton.setEnabled(false);
                deleteJobLockButton.setEnabled(false);
            }
        });


        Button okButton = new Button(getTranslation("button.done", UI.getCurrent().getLocale()));
        okButton.addClickListener(event -> {
            this.close();
        });

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.add(okButton);

        layout.add(buttonLayout);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, buttonLayout);

        super.content.add(layout);
        super.title.setText(getTranslation("label.manage-job-locks", UI.getCurrent().getLocale()));

        super.showResize(false);
        super.setResizable(false);

        super.setHeight("80vh");
        super.setWidth("90vw");
    }

    public void addGridFiltering(HeaderRow hr, String columnKey) {
        Icon filterIcon = VaadinIcon.FILTER.create();
        filterIcon.setSize("12pt");
        filterTf.setSuffixComponent(filterIcon);
        filterTf.setWidthFull();

        filterTf.addValueChangeListener(ev->{
            this.populateGrid(comboBox.getValue(), filterTf.getValue());
        });

        hr.getCell(grid.getColumnByKey(columnKey)).setComponent(filterTf);
    }

    @Override
    public void jobSelected(SchedulerJob schedulerJob) {
        JobLock jobLock = this.comboBox.getValue();

        this.contextTemplate.getJobLocksMap().put(jobLock.getName(), jobLock);
        this.contextTemplate.getJobLocks().remove(jobLock);

        schedulerJob.getChildContextNames().forEach(child -> {
            if(jobLock.getJobs().containsKey(child)) {
                jobLock.getJobs().get(child).add(schedulerJob);
            }
            else {
                ArrayList<SchedulerJob> jobs = new ArrayList<>();
                jobs.add(schedulerJob);
                jobLock.getJobs().put(child, jobs);
            }
        });

        this.contextTemplate.getJobLocks().add(jobLock);
        if(this.saveContextTemplate()) {
            this.updateScheduledJob(schedulerJob, true);
            this.populateGrid(jobLock, filterTf.getValue());
            NotificationHelper.showUserNotification(String.format(getTranslation("notification.job-added-to-lock", UI.getCurrent().getLocale())
                , schedulerJob.getJobName(), jobLock.getName()));
        }
    }

    public void updateLockCount(int lockCount) {
        JobLock jobLock = this.comboBox.getValue();
        jobLock.setLockCount(lockCount);
        this.saveContextTemplate();
    }

    public void setJobLock(String jobLockName) {
        this.contextTemplate.getJobLocks().forEach(lock -> {
            if(lock.getName().equals(jobLockName)) {
                this.comboBox.setValue(lock);
            }
        });
    }

    private void populateGrid(JobLock jobLock, String filter) {
        Map<String, SchedulerJob> jobMap = new HashMap<>();
        jobLock.getJobs().entrySet().forEach(entry -> {
            entry.getValue().forEach(job -> {
                if (!jobMap.containsKey(job.getIdentifier())) {
                    job.setChildContextNames(new ArrayList<>());
                    jobMap.put(job.getIdentifier(), job);
                }

                jobMap.get(job.getIdentifier()).getChildContextNames().add(entry.getKey());
            });
        });

        grid.setItems(jobMap.values().stream()
                .filter(job -> {
                    if(filter != null && !filter.isEmpty()) {
                        return job.getJobName().toLowerCase().contains(filter.toLowerCase());
                    }

                    return true;
                })
                .collect(Collectors.toList()));
    }

    private boolean saveContextTemplate() {
        try {
            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findByName(this.contextTemplate.getName());
            scheduledContextRecord.setContext(this.contextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);

            String newLocksString = "";

            try {
                newLocksString = this.objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(this.contextTemplate.getJobLocks());
            }
            catch (JsonProcessingException e) {
                LOGGER.info("Could not initialise job locks string for audit!");
            }

            StringBuffer auditMessage = new StringBuffer();
            auditMessage.append(String.format("Context[%s] Job Lock Modifications\n", this.contextTemplate.getName()));
            auditMessage.append("Before\n").append(this.initialJobLocks);
            auditMessage.append("\nAfter\n").append(newLocksString);
            this.systemEventLogger.logEvent(SystemEventConstants.CONTEXT_JOB_LOCKS_MODIFICATION, auditMessage.toString(),
                SecurityContextHolder.getContext().getAuthentication().getName());

            this.initialJobLocks = newLocksString;
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification(getTranslation("notification.error-saving-job-locks", UI.getCurrent().getLocale()));
            return false;
        }
    }

    public void updateScheduledJob(SchedulerJob schedulerJob, boolean inlock) {
        SchedulerJobRecord schedulerJobRecord = this.schedulerJobService.findByContextNameAndJobName(schedulerJob.getContextName(), schedulerJob.getJobName());
        InternalEventDrivenJob internalEventDrivenJob = (InternalEventDrivenJob) schedulerJobRecord.getJob();
        internalEventDrivenJob.setParticipatesInLock(inlock);
        this.schedulerJobService.saveInternalEventDrivenJob(internalEventDrivenJob,
            SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
