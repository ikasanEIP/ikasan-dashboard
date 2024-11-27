package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinSession;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.scheduler.listener.JobSynchronisationRequiredListener;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.service.ContextService;
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
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.scheduled.visualisation.service.ContextVisualisationLayoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class JobTemplateVisualisationDialog extends AbstractCloseableResizableDialog implements SchedulerJobSelectedListener {

    private Logger logger = LoggerFactory.getLogger(JobTemplateVisualisationDialog.class);

    private VerticalLayout layout;
    private boolean initialised = false;
    private ContextTemplate rootContextTemplate;
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
    private ContextVisualisationLayoutService contextVisualisationLayoutService;

    private ContextService contextService = new ContextService();

    private SchedulerVisualisation schedulerVisualisation;
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;

    private List<JobSynchronisationRequiredListener> jobSynchronisationRequiredListeners = new ArrayList<>();

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;

    private boolean showPrettyFormat;

    public JobTemplateVisualisationDialog(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                          SchedulerJobService schedulerJobService, LogStreamingService logStreamingService,
                                          JobInitiationService jobInitiationService,
                                          ContextProfileService contextProfileService, UserService userService, SecurityService securityService,
                                          JobProvisionService jobProvisionService, ScheduledContextService scheduledContextService,
                                          ContextVisualisationLayoutService contextVisualisationLayoutService,
                                          Map<String, String> schedulerJobExecutionEnvironmentLabel,
                                          double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                          double contextVisualisationLevelDistance, double contextVisualisationNodeDistance,
                                          boolean showPrettyFormat) {
        this.setHeight("98vh");
        this.setWidth("98vw");

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("agent cannot be null!");
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

        this.contextVisualisationLayoutService = contextVisualisationLayoutService;
        if(this.contextVisualisationLayoutService == null) {
            throw new IllegalArgumentException("contextVisualisationLayoutService cannot be null!");
        }

        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;

        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;

        this.showPrettyFormat = showPrettyFormat;

        layout = new VerticalLayout();
        layout.setSizeFull();
        super.content.add(layout);
    }

    /**
     * @param contextTemplate
     */
    public void createSchedulerVisualisation(ContextTemplate rootContextTemplate, ContextTemplate contextTemplate) throws IOException {
        this.rootContextTemplate = rootContextTemplate;
        this.contextTemplate = contextTemplate;
        this.initialised = false;
        initParentNavigation();

        this.schedulerVisualisation = new JobSchedulerVisualisation(this.dynamicImagePath, this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService,
            this.logStreamingService, this.jobInitiationService, this.contextProfileService, this.userService, this.securityService,
            this.jobProvisionService, this.contextVisualisationLayoutService, this.scheduledContextService, this.schedulerJobExecutionEnvironmentLabel, this.jobVisualisationVerticalSpacing,
            this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance, this.showPrettyFormat);
        this.schedulerVisualisation.createSchedulerVisualisation(this.rootContextTemplate, this.contextTemplate, this, this.showPrettyFormat);
        this.jobSynchronisationRequiredListeners.forEach(listener ->
            this.schedulerVisualisation.addJobSynchronisationRequiredListener(listener));

        this.layout.add(this.schedulerVisualisation);
        super.title.setText(this.contextTemplate.getName());
    }

    private void initParentNavigation() {
        HorizontalLayout buttonLayout = new HorizontalLayout();

        if(!initialised && this.contextTemplate != null) {

            ContextTemplate parentContextInstance = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

            Checkbox showFormattedCheck = new Checkbox("Pretty layout");
            showFormattedCheck.setValue(this.showPrettyFormat);
            showFormattedCheck.addClickListener(event -> {
                if (this.contextTemplate != null) {
                    try {
                        this.close();
                        JobTemplateVisualisationDialog contextTemplateVisualisationDialog
                            = new JobTemplateVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService, this.configurationRestService
                            , this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService
                            , this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService
                            , this.contextVisualisationLayoutService, this.schedulerJobExecutionEnvironmentLabel, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing
                            , this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance, showFormattedCheck.getValue());

                        contextTemplateVisualisationDialog.createSchedulerVisualisation(this.rootContextTemplate, this.contextTemplate);
                        contextTemplateVisualisationDialog.open();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            buttonLayout.add(showFormattedCheck);
            buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.BASELINE, showFormattedCheck);

            if(parentContextInstance != null) {
                Button gotoParentButton = new Button(getTranslation("button.go-to-parent", UI.getCurrent().getLocale())
                    + " " + parentContextInstance.getName(), VaadinIcon.ARROW_UP.create());
                gotoParentButton.setIconAfterText(true);
                gotoParentButton.addClickListener(buttonClickEvent -> {
                    if (this.contextTemplate != null && this.rootContextTemplate != null) {
                        this.contextTemplate = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

                        if (this.contextTemplate != null) {
                            try {
                                this.close();
                                ContextTemplateVisualisationDialog contextTemplateVisualisationDialog
                                    = new ContextTemplateVisualisationDialog(this.moduleMetaDataService, this.scheduledProcessManagementService, this.configurationRestService
                                    , this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService
                                    , this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService
                                    , this.contextVisualisationLayoutService, this.schedulerJobExecutionEnvironmentLabel, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing
                                    , this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance, this.showPrettyFormat);

                                contextTemplateVisualisationDialog.createSchedulerVisualisation(this.rootContextTemplate, this.contextTemplate);
                                contextTemplateVisualisationDialog.open();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                ComponentSecurityVisibility.applySecurity(gotoParentButton, SecurityConstants.ALL_AUTHORITY,
                    SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                    SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

                buttonLayout.add(gotoParentButton);
                buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.BASELINE, gotoParentButton);
            }

            Button addContextViewButton = new Button(getTranslation("button.add-as-context-view", UI.getCurrent().getLocale()), VaadinIcon.PLUS.create());
            addContextViewButton.setIconAfterText(true);

            addContextViewButton.addClickListener(event -> {
                ContextViewManagementDialog contextViewManagementDialog = new ContextViewManagementDialog(this.contextProfileService, this.userService,
                    this.securityService, this.systemEventLogger, this.rootContextTemplate.getName(), this.contextTemplate.getName());
                contextViewManagementDialog.open();
            });

            ComponentSecurityVisibility.applySecurity(addContextViewButton, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            MenuBar addJobMenuBar = this.addJobMenuBar();

            ComponentSecurityVisibility.applySecurity(addJobMenuBar, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            Button addAnd = new Button(getTranslation("button.add-and-to-visualisation", UI.getCurrent().getLocale()));
            addAnd.addClickListener(buttonClickEvent -> {
                this.schedulerVisualisation.addAndGrouping();
            });

            ComponentSecurityVisibility.applySecurity(addAnd, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            Button addOr = new Button(getTranslation("button.add-or-to-visualisation", UI.getCurrent().getLocale()));
            addOr.addClickListener(buttonClickEvent -> {
               this.schedulerVisualisation.addOrGrouping();
            });

            ComponentSecurityVisibility.applySecurity(addOr, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            Button jsonButton = new Button(getTranslation("button.view-raw-json", UI.getCurrent().getLocale()), VaadinIcon.CODE.create());
            jsonButton.setIconAfterText(true);
            jsonButton.addClickListener(buttonClickEvent -> {
                JsonViewerDialog jsonViewerDialog = new JsonViewerDialog(this.schedulerVisualisation.getContextTemplate());
                jsonViewerDialog.open();
            });

            ComponentSecurityVisibility.applySecurity(jsonButton, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ);

            Button saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
            saveButton.addClickListener(buttonClickEvent -> {
                this.schedulerVisualisation.save();
            });

            ComponentSecurityVisibility.applySecurity(saveButton, SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);

            buttonLayout.add(addJobMenuBar, addAnd, addOr, jsonButton, addContextViewButton, saveButton);
            buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.BASELINE, addJobMenuBar, addAnd, addOr, jsonButton, addContextViewButton, saveButton);

            this.layout.add(buttonLayout);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

            this.initialised = true;
        }
    }

    private MenuBar addJobMenuBar() {
        MenuBar newJobMenuBar = new MenuBar();
        newJobMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem jobMenu = createIconItem(newJobMenuBar, VaadinIcon.TOOLBOX, getTranslation("menu-item.add-a-job", UI.getCurrent().getLocale()));

        SubMenu addJobSubMenu = jobMenu.getSubMenu();
        MenuItem newJobMenuItem = addJobSubMenu.addItem(getTranslation("menu-item.new-job", UI.getCurrent().getLocale()));
        addJobSubMenu.addItem(getTranslation("menu-item.existing-job", UI.getCurrent().getLocale()), menuItemClickEvent -> {
            SchedulerJobSelectDialog schedulerJobSelectDialog = new SchedulerJobSelectDialog(this.schedulerJobService, this.rootContextTemplate,
                getTranslation("label.select-job", UI.getCurrent().getLocale()), getTranslation("label.select-job", UI.getCurrent().getLocale()));

            schedulerJobSelectDialog.open();
            schedulerJobSelectDialog.addSchedulerJobSelectedListener(this);

            schedulerJobSelectDialog.addOpenedChangeListener(event -> {
               if(!event.isOpened()) {
                   schedulerJobSelectDialog.removeSchedulerJobSelectedListener(this);
               }
            });
        });


        SubMenu jobTypeSubMenu = newJobMenuItem.getSubMenu();
        MenuItem jobTypeMenuItem = jobTypeSubMenu.addItem(getTranslation("menu-item.job-type", UI.getCurrent().getLocale()));
        SubMenu jobTypesSubMenu = jobTypeMenuItem.getSubMenu();

        jobTypesSubMenu.addItem(getTranslation("menu-item.command-execution-job", UI.getCurrent().getLocale()), event -> {
            InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.rootContextTemplate, this.contextTemplate, this.schedulerJobExecutionEnvironmentLabel);
            internalEventDrivenJobDialog.addSchedulerJobSelectedListener(this);

            InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
            internalEventDrivenJob.setContextName(this.rootContextTemplate.getName());

            internalEventDrivenJobDialog.setJob(internalEventDrivenJob, EditMode.NEW);
            this.jobSynchronisationRequiredListeners.forEach(listener ->
                internalEventDrivenJobDialog.addJobSynchronisationRequiredListener(listener));
            internalEventDrivenJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.from-command-execution-job-template", UI.getCurrent().getLocale()), event -> {
                CommandExecutionJobTemplateSelectDialog schedulerJobSelectDialog = new CommandExecutionJobTemplateSelectDialog(this.schedulerJobService,
                    this.rootContextTemplate, "Select Command Execution Job Template", "Select Command Execution Job Template");
                schedulerJobSelectDialog.open();
                schedulerJobSelectDialog.addSchedulerJobSelectedListener(schedulerJob -> {
                    InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                        this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.contextTemplate, this.contextTemplate, this.schedulerJobExecutionEnvironmentLabel);

                    InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
                    internalEventDrivenJob.setContextName(this.contextTemplate.getName());
                    internalEventDrivenJobDialog.setJob((InternalEventDrivenJob) schedulerJob, EditMode.NEW);
                    internalEventDrivenJobDialog.addSchedulerJobSelectedListener(this);

                    this.jobSynchronisationRequiredListeners.forEach(listener ->
                        internalEventDrivenJobDialog.addJobSynchronisationRequiredListener(listener));

                    internalEventDrivenJobDialog.open();
                });
            })
            .getElement()
            .setAttribute("disabled", !ComponentSecurityVisibility.hasAuthorisation(SecurityConstants.ALL_AUTHORITY,
                SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN,
                SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE));
        jobTypesSubMenu.addItem(getTranslation("menu-item.file-watcher-job", UI.getCurrent().getLocale()), event -> {
            FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.contextTemplate.isUseDisplayName());
            fileEventJobDialog.addSchedulerJobSelectedListener(this);

            FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
            fileEventDrivenJob.setContextName(this.rootContextTemplate.getName());

            fileEventJobDialog.setJob(fileEventDrivenJob, EditMode.NEW);
            this.jobSynchronisationRequiredListeners.forEach(listener ->
                fileEventJobDialog.addJobSynchronisationRequiredListener(listener));
            fileEventJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.scheduled-job", UI.getCurrent().getLocale()), event -> {
            QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(null, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService,
                this.contextTemplate.isUseDisplayName());
            quartzDrivenScheduledJobDialog.addSchedulerJobSelectedListener(this);

            QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
            quartzScheduleDrivenJob.setContextName(this.rootContextTemplate.getName());

            quartzDrivenScheduledJobDialog.setJob(quartzScheduleDrivenJob, EditMode.NEW);
            this.jobSynchronisationRequiredListeners.forEach(listener ->
                quartzDrivenScheduledJobDialog.addJobSynchronisationRequiredListener(listener));
            quartzDrivenScheduledJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.global-job", UI.getCurrent().getLocale()), event -> {
            GlobalEventJobDialog globalEventJobDialog = new GlobalEventJobDialog(null, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                this.schedulerJobService, this.contextTemplate.isUseDisplayName());

            globalEventJobDialog.addSchedulerJobSelectedListener(this);

            GlobalEventJob globalEventJob = new GlobalEventJobImpl();
            globalEventJob.setContextName(JobConstants.GLOBAL_EVENT);

            globalEventJobDialog.setJob(globalEventJob, EditMode.NEW);
            globalEventJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.local-event-job", UI.getCurrent().getLocale()), event -> {
            LocalEventJobDialog localEventJobDialog = new LocalEventJobDialog(null, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                this.schedulerJobService, this.contextTemplate.isUseDisplayName());

            localEventJobDialog.addSchedulerJobSelectedListener(this);

            LocalEventJob localEventJob = new LocalEventJobImpl();
            localEventJob.setContextName(this.rootContextTemplate.getName());
            localEventJob.getChildContextNames().add(contextTemplate.getName());

            localEventJobDialog.setJob(localEventJob, EditMode.NEW);
            localEventJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.start-job", UI.getCurrent().getLocale()), event -> {
            Optional<SchedulerJob> startJob = contextTemplate.getScheduledJobs().stream()
                .filter(schedulerJob -> schedulerJob.getAgentName().equals(JobConstants.CONTEXT_START_JOB))
                .findFirst();

            if(startJob.isPresent()) {
                NotificationHelper.showUserNotification(getTranslation("error.start-job-exists-in-context", UI.getCurrent().getLocale()));
                return;
            }

            ContextStartJob contextStartJob = new ContextStartJobImpl();
            contextStartJob.setJobName(this.contextTemplate.getName() + "_START");
            this.jobSelected(contextStartJob);
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.terminal-job", UI.getCurrent().getLocale()), event -> {
            Optional<ContextTerminalJob> terminalJob = ContextHelper.getContextTerminalJobFromContext(contextTemplate);

            if(terminalJob.isPresent()) {
                NotificationHelper.showUserNotification(getTranslation("error.terminal-job-exists-in-context"
                    , UI.getCurrent().getLocale()));
                return;
            }

            ContextTerminalJob contextTerminalJob = new ContextTerminalJobImpl();
            contextTerminalJob.setJobName(this.contextTemplate.getName() + "_TERMINAL");
            this.jobSelected(contextTerminalJob);
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.bridging-job", UI.getCurrent().getLocale()), event -> {
            BridgingJob bridgingJob = new BridgingJobImpl();
            bridgingJob.setJobName(this.contextTemplate.getName() + "_BRIDGING_" + System.currentTimeMillis());
            this.jobSelected(bridgingJob);
        });

        return newJobMenuBar;
    }

    private MenuItem createIconItem(MenuBar menu, VaadinIcon iconName, String label) {
        Icon icon = new Icon(iconName);

        Button menuButton = new Button(label, icon);
        menuButton.setIconAfterText(true);

        MenuItem item = menu.addItem(menuButton);

        return item;
    }

    @Override
    public void jobSelected(SchedulerJob schedulerJob) {
        logger.info(schedulerJob.getIdentifier());

        if(this.contextTemplate.getScheduledJobs()
            .stream()
            .filter(job -> job.getIdentifier().equals(schedulerJob.getIdentifier()))
            .collect(Collectors.toList()).size() > 0) {
            NotificationHelper.showUserNotification(getTranslation("error.job-exists-in-context", UI.getCurrent().getLocale()));
        }
        else {
            this.contextTemplate.getScheduledJobs().add(schedulerJob);
            this.schedulerVisualisation.addJob(schedulerJob);
        }
    }

    public void close() {
        if(this.schedulerVisualisation.isSaveRequired()) {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("header.save-required", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("label.unsaved-diagram", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();

            confirmDialog.addConfirmListener(event -> {
                this.setOpened(false);
            });
        }
        else {
            this.setOpened(false);
        }
    }

    public void addJobSynchronisationRequiredListener(JobSynchronisationRequiredListener listener) {
        this.jobSynchronisationRequiredListeners.add(listener);
    }
}
