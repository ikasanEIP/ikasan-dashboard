package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.IconDecorator;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobTemplateVisualisationDialog;
import org.ikasan.job.orchestration.model.context.JobLockImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
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
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobLockManagementDialog extends AbstractCloseableResizableDialog implements SchedulerJobSelectedListener {
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

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        grid = new Grid<>();
        grid.addColumn(SchedulerJob::getJobName)
            .setHeader(getTranslation("table-header.job-name", UI.getCurrent().getLocale()))
            .setFlexGrow(8);
        grid.addColumn(new ComponentRenderer<>(
                job -> {
                    VerticalLayout verticalLayout = new VerticalLayout();
                    verticalLayout.setWidth("100%");
                    verticalLayout.setSpacing(false);
                    verticalLayout.setPadding(false);

                    if(job != null && job.getChildContextNames() != null) {
                        job.getChildContextNames().forEach(context -> {
                            Icon visualisation = IconDecorator.decorate(new Icon(VaadinIcon.SITEMAP), getTranslation("tooltip.open-visualisation", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
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
                                    // todo error message
                                }

                            });

                            verticalLayout.add(contextButton);
                        });
                    }
                    return verticalLayout;
                }
            ))
            .setHeader(getTranslation("table-header.select", UI.getCurrent().getLocale()))
            .setFlexGrow(8);
        grid.addColumn(new ComponentRenderer<>(job -> {
                VerticalLayout buttonLayout = new VerticalLayout();
                buttonLayout.setWidth("100%");

                Icon delete = IconDecorator.decorate(new Icon(VaadinIcon.TRASH), getTranslation("tooltip.delete-job-template", UI.getCurrent().getLocale()), "14pt", "rgba(0, 0, 0, 1.0)");
                delete.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                    JobLock jobLock = this.comboBox.getValue();
                    job.getChildContextNames().forEach(child -> {
                        List<SchedulerJob> jobs = jobLock.getJobs().get(child);
                        if(jobs!=null) {
                            jobs.remove(job);
                        }
                    });
                    if(this.saveContextTemplate()) {
                        this.populateGrid(jobLock);
                        NotificationHelper.showUserNotification(String.format("Job [%s] removed from job lock [%s].", job.getJobName(), jobLock.getName()));
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

        comboBox = new ComboBox<>("Job Locks");
        comboBox.setWidth("100%");
        comboBox.setItems(contextTemplate.getJobLocks());
        comboBox.setItemLabelGenerator(JobLock::getName);

        TextField lockCountTf = new TextField("Lock Count");

        Button newJobLockButton = new Button("New Job Lock", VaadinIcon.LOCK.create());
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
                }
            });
        });

        layout.add(newJobLockButton);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, newJobLockButton);

        FormLayout formLayout = new FormLayout();
        formLayout.add(comboBox, lockCountTf);

        Button addJobButton = new Button("Add Job to Lock", VaadinIcon.PLUS.create());
        addJobButton.setIconAfterText(true);
        addJobButton.setEnabled(false);
        addJobButton.addClickListener(event -> {
            InternalEventDrivenJobSelectDialog internalEventDrivenJobSelectDialog
                = new InternalEventDrivenJobSelectDialog(this.schedulerJobService, this.contextTemplate);
            internalEventDrivenJobSelectDialog.open();
            internalEventDrivenJobSelectDialog.addSchedulerJobSelectedListener(this);
        });

        layout.add(formLayout, addJobButton, grid);

        comboBox.addValueChangeListener(event -> {
            if(event.getValue() != null) {
                lockCountTf.setValue(String.valueOf(event.getValue().getLockCount()));
                this.populateGrid(event.getValue());
                addJobButton.setEnabled(true);
            }
            else {
                addJobButton.setEnabled(false);
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
        super.title.setText(getTranslation("table-header.context-name", UI.getCurrent().getLocale()));

        super.showResize(false);
        super.setResizable(false);

        super.setHeight("800px");
        super.setWidth("1500px");
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
            this.populateGrid(jobLock);
            NotificationHelper.showUserNotification(String.format("Job [%s] added to job lock [%s].", schedulerJob.getJobName(), jobLock.getName()));
        }
    }

    private void populateGrid(JobLock jobLock) {
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

        grid.setItems(jobMap.values().stream().collect(Collectors.toList()));
    }

    private boolean saveContextTemplate() {
        try {
            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findByName(this.contextTemplate.getName());
            scheduledContextRecord.setContext(this.contextTemplate);
            this.scheduledContextService.save(scheduledContextRecord);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            NotificationHelper.showErrorNotification("An error has occurred saving the jobs locks. Please contact Ikasan support.");
            return false;
        }
    }
}
