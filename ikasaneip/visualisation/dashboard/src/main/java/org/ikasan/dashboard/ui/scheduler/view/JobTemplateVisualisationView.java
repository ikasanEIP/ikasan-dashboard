package org.ikasan.dashboard.ui.scheduler.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.scheduler.listener.JobPlanSaveRequiredListener;
import org.ikasan.dashboard.ui.scheduler.listener.JobSynchronisationRequiredListener;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.ComponentSecurityVisibility;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SecurityConstants;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.ContextTemplateVisualisationDialog;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobSchedulerVisualisation;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.SchedulerVisualisation;
import org.ikasan.job.orchestration.model.job.*;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.job.orchestration.util.ContextHelper;
//import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
//import org.ikasan.security.service.SecurityService;
//import org.ikasan.security.service.UserService;
//import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.security.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.security.PermitAll;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "jobTemplateManagement", layout = IkasanAppLayout.class)
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/tree-view.css", themeFor = "vaadin-grid")
@CssImport(value = "./styles/grid-header.css", themeFor = "vaadin-grid")
@PermitAll
@PreserveOnRefresh
public class JobTemplateVisualisationView extends VerticalLayout
    implements BeforeEnterObserver, BeforeLeaveObserver, HasUrlParameter<String>, SchedulerJobSelectedListener, JobPlanSaveRequiredListener {

    private Logger logger = LoggerFactory.getLogger(JobTemplateVisualisationView.class);

    private VerticalLayout layout;
    private boolean initialised = false;
    private ContextTemplate rootContextTemplate;
    private ContextTemplate contextTemplate;
    private String dynamicImagePath = ".";

    @Autowired
    @Qualifier("moduleMetadataService")
    private ModuleMetaDataService moduleMetaDataService;
    @Autowired
    private ConfigurationService configurationRestService;
    @Autowired
    private ModuleControlService moduleControlRestService;
    @Autowired
    private MetaDataService metaDataRestService;
    @Autowired
    private SystemEventLogger systemEventLogger;
    @Autowired
    private SchedulerJobService schedulerJobService;
    @Autowired
    private LogStreamingService logStreamingService;
    @Autowired
    private JobInitiationService jobInitiationService;
    @Autowired
    private ContextProfileService contextProfileService;
    @Autowired
    private UserService userService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private JobProvisionService jobProvisionService;
    @Autowired
    private ScheduledContextService scheduledContextService;

    private ContextService contextService = new ContextService();

    private SchedulerVisualisation schedulerVisualisation;
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;

    private List<JobSynchronisationRequiredListener> jobSynchronisationRequiredListeners = new ArrayList<>();

    @Value("${job.visualisation.vertical.spacing:120}")
    protected double jobVisualisationVerticalSpacing;
    @Value("${job.visualisation.horizontal.spacing:400}")
    protected double jobVisualisationHorizontalSpacing;
    @Value("${context.visualisation.level.distance:150}")
    protected double contextVisualisationLevelDistance;
    @Value("${context.visualisation.node.distance:75}")
    protected double contextVisualisationNodeDistance;

    private String contextName;
    private String childContextName;

    private boolean showPrettyFormat = false;

    private boolean isInitialised = false;

    private Button saveButton;

    /**
     * Constructs a new instance of the JobTemplateVisualisationView.
     *
     * This view provides a visual representation of job templates in the context
     * of scheduling and job execution management. It initializes necessary UI components
     * and layouts to ensure full-size rendering within the container.
     *
     * The constructor initializes the following:
     * - A mapping for the scheduler job execution environment label.
     * - A full-size vertical layout to act as the primary container for UI components.
     * - The size of the view to occupy the full parent container.
     */
    public JobTemplateVisualisationView() {
        this.schedulerJobExecutionEnvironmentLabel = new HashMap<>();
        layout = new VerticalLayout();
        layout.setSizeFull();
        this.add(layout);
        this.setSizeFull();
    }

    /**
     * Creates a visual representation of the job scheduler hierarchy and configurations
     * by initializing and rendering the scheduler visualisation component. This method
     * links the provided context templates to the visual representation and ensures
     * event listeners and layout components are properly configured.
     *
     * @param rootContextTemplate the root context template that represents the top-level
     *                             structure of the scheduler configuration
     * @param contextTemplate the specific context template to be visualized, representing
     *                        a more granular level within the scheduler hierarchy
     * @throws IOException if an I/O error occurs during the visualisation creation process
     */
    public void createSchedulerVisualisation(ContextTemplate rootContextTemplate, ContextTemplate contextTemplate) throws IOException {
        this.rootContextTemplate = rootContextTemplate;
        this.contextTemplate = contextTemplate;
        this.initialised = false;
        initParentNavigation();

        this.schedulerVisualisation = new JobSchedulerVisualisation(this.dynamicImagePath, this.moduleMetaDataService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService,
            this.logStreamingService, this.jobInitiationService, this.contextProfileService, this.userService, this.securityService,
            this.jobProvisionService, this.scheduledContextService, this.schedulerJobExecutionEnvironmentLabel, this.jobVisualisationVerticalSpacing,
            this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance, this.showPrettyFormat);
        this.schedulerVisualisation.createSchedulerVisualisation(this.rootContextTemplate, this.contextTemplate, null, true);
        this.jobSynchronisationRequiredListeners.forEach(listener ->
            this.schedulerVisualisation.addJobSynchronisationRequiredListener(listener));
        this.schedulerVisualisation.addJobPlanSaveRequiredListener(this);

        this.layout.add(this.schedulerVisualisation);
    }

    /**
     * Initializes the parent navigation components and provides functionality for navigating between
     * context templates and their parent contexts within the user interface. This method manages
     * the creation and configuration of buttons to handle both auto-formatted and user-generated
     * layouts, as well as navigation to the parent context when applicable.
     *
     * The method performs the following tasks:
     * 1. Creates a horizontal layout to hold navigation buttons.
     * 2. Adds buttons for switching between "Auto Layout" and "User Layout" visualizations
     *    with confirmation dialogs to warn users about potential unsaved changes.
     * 3. Configures a "Go to Parent" button allowing navigation to the parent context template.
     *    It verifies if the user has unsaved changes in the current scheduler visualization
     *    and prompts them to save or confirm their decision before proceeding.
     * 4. Leverages confirmation dialogs to notify the user when changes to the context structure
     *    or navigation actions are about to take place.
     * 5. Ensures proper handling of parent context retrieval using the `ContextService`
     *    implementation. If a parent exists, it initializes and opens a context visualization
     *    dialog with the respective parent data.
     * 6. Handles security configuration for rendering buttons by applying role-based access
     *    control to ensure buttons are visible/accessible only to users with the appropriate permissions.
     *
     * The method relies on external services and objects such as `ContextService`, `ConfirmDialog`,
     * and `UI` for functionality. If the necessary context templates and services are not initialized,
     * the method exits early without performing any action.
     */
    private void initParentNavigation() {
        HorizontalLayout buttonLayout = new HorizontalLayout();

        if(!initialised && this.contextTemplate != null) {

            ContextTemplate parentContextInstance = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

            Button autoFormattedButton = new Button(getTranslation("label.show-auto-layout"));
            autoFormattedButton.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader("Change Formatting");
                confirmDialog.setText("You are about to change the formatting. Any unsaved changes will be lost. Are you sure you'd like to proceed?");
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();
                confirmDialog.addConfirmListener(confirmEvent -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(JobTemplateVisualisationView.class
                            , List.of(
                                rootContextTemplate.getName(),
                                contextTemplate.getName(),
                                String.valueOf(true)));

                    getUI().ifPresent(ui -> ui.getPage().open(route));

                    UI.getCurrent().getPage().executeJs("window.close();");

                    // Optionally close the current Vaadin UI instance on the server side
                    UI.getCurrent().close();
                });
            });
            Button userGeneratedLayout = new Button(getTranslation("label.show-user-layout"));
            userGeneratedLayout.addClickListener(event -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.change-formatting-header"));
                confirmDialog.setText(getTranslation("confirm-dialog.change-formatting-body"));
                confirmDialog.setConfirmText(getTranslation("button.ok"));
                confirmDialog.setCancelText(getTranslation("button.cancel"));
                confirmDialog.setCancelable(true);
                confirmDialog.open();
                confirmDialog.addConfirmListener(confirmEvent -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(JobTemplateVisualisationView.class
                            , List.of(
                                rootContextTemplate.getName(),
                                contextTemplate.getName(),
                                String.valueOf(false)));

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                    UI.getCurrent().getPage().executeJs("window.close();");

                    // Optionally close the current Vaadin UI instance on the server side
                    UI.getCurrent().close();
                });
            });
            buttonLayout.add(autoFormattedButton, userGeneratedLayout);
            buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.BASELINE, autoFormattedButton, userGeneratedLayout);

            if(parentContextInstance != null) {
                Button gotoParentButton = new Button(getTranslation("button.go-to-parent", UI.getCurrent().getLocale())
                    + " " + parentContextInstance.getName(), VaadinIcon.ARROW_UP.create());
                gotoParentButton.setIconAfterText(true);
                gotoParentButton.addClickListener(buttonClickEvent -> {
                    if (this.contextTemplate != null && this.rootContextTemplate != null) {
                        ContextTemplate parent = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

                        if (parent != null) {
                            if(this.schedulerVisualisation.isSaveRequired()) {
                                ConfirmDialog confirmDialog = new ConfirmDialog();
                                confirmDialog.setHeader(getTranslation("header.save-required", UI.getCurrent().getLocale()));
                                confirmDialog.setText(getTranslation("label.unsaved-diagram", UI.getCurrent().getLocale()));
                                confirmDialog.setConfirmText(getTranslation("button.ok"));
                                confirmDialog.setCancelText(getTranslation("button.cancel"));
                                confirmDialog.setCancelable(true);
                                confirmDialog.open();
                                confirmDialog.addConfirmListener(event -> {
                                    try {
                                        ContextTemplateVisualisationDialog contextTemplateVisualisationDialog
                                            = new ContextTemplateVisualisationDialog(this.moduleMetaDataService, this.configurationRestService
                                            , this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService
                                            , this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService
                                            , this.schedulerJobExecutionEnvironmentLabel, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing
                                            , this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance, this.showPrettyFormat);

                                        contextTemplateVisualisationDialog.createSchedulerVisualisation(this.rootContextTemplate, parent);
                                        contextTemplateVisualisationDialog.open();
                                    }
                                    catch (IOException e) {
                                        logger.error("An error has occurred opening context template visualisation {}", parent.getName(), e);
                                    }
                                });
                            }
                            else {
                                try {
                                    ContextTemplateVisualisationDialog contextTemplateVisualisationDialog
                                        = new ContextTemplateVisualisationDialog(this.moduleMetaDataService, this.configurationRestService
                                        , this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.logStreamingService
                                        , this.jobInitiationService, this.contextProfileService, this.userService, this.securityService, this.jobProvisionService, this.scheduledContextService
                                        , this.schedulerJobExecutionEnvironmentLabel, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing
                                        , this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance, this.showPrettyFormat);

                                    contextTemplateVisualisationDialog.createSchedulerVisualisation(this.rootContextTemplate, parent);
                                    contextTemplateVisualisationDialog.open();
                                }
                                catch (IOException e) {
                                    logger.error("An error has occurred opening context template visualisation {}", parent.getName(), e);
                                }
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

            this.saveButton = new Button(getTranslation("button.save", UI.getCurrent().getLocale()));
            this.saveButton.addClickListener(buttonClickEvent -> {
                this.schedulerVisualisation.save();
                this.contextTemplate = schedulerVisualisation.getContextTemplate();
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

    /**
     * Creates and configures a menu bar with options to add or manage jobs.
     * This includes creating new jobs of various types (e.g., command execution jobs, file-watcher jobs)
     * as well as selecting existing jobs from predefined categories.
     *
     * The returned menu bar includes nested submenus for different job types and configurations, each
     * linked to appropriate dialogs for user interaction. Listeners are added for specific events such
     * as job selection and synchronization requirements.
     *
     * @return a configured {@link MenuBar} instance with job management options.
     */
    private MenuBar addJobMenuBar() {
        MenuBar newJobMenuBar = new MenuBar();
        newJobMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem jobMenu = createIconItem(newJobMenuBar, VaadinIcon.TOOLBOX, getTranslation("menu-item.add-a-job", UI.getCurrent().getLocale()));

        SubMenu addJobSubMenu = jobMenu.getSubMenu();
        MenuItem newJobMenuItem = addJobSubMenu.addItem(getTranslation("menu-item.new-job", UI.getCurrent().getLocale()));
        MenuItem existingMenu = addJobSubMenu.addItem(getTranslation("menu-item.existing-job", UI.getCurrent().getLocale()));
        SubMenu existingJobSubMenu = existingMenu.getSubMenu();

        existingJobSubMenu.addItem(getTranslation("menu-item.local-event-job", UI.getCurrent().getLocale()), menuItemClickEvent -> {
            LocalEventJobSelectDialog localEventJobSelectDialog = new LocalEventJobSelectDialog(this.rootContextTemplate);
            localEventJobSelectDialog.open();
            localEventJobSelectDialog.addSchedulerJobSelectedListener(this);

            localEventJobSelectDialog.addOpenedChangeListener(event -> {
                if(!event.isOpened()) {
                    localEventJobSelectDialog.removeSchedulerJobSelectedListener(this);
                }
            });
        });

        existingJobSubMenu.addItem(getTranslation("menu-item.other", UI.getCurrent().getLocale()), menuItemClickEvent -> {
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
            InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(null, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.rootContextTemplate, this.contextTemplate,
                this.schedulerJobExecutionEnvironmentLabel, true);
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
                    InternalEventDrivenJobDialog internalEventDrivenJobDialog = new InternalEventDrivenJobDialog(null, this.configurationRestService,
                        this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.contextTemplate, this.contextTemplate,
                        this.schedulerJobExecutionEnvironmentLabel, true);

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
            FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(null, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService, this.contextTemplate.isUseDisplayName(),
                this.rootContextTemplate, this.contextTemplate, true);
            fileEventJobDialog.addSchedulerJobSelectedListener(this);

            FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
            fileEventDrivenJob.setContextName(this.rootContextTemplate.getName());

            fileEventJobDialog.setJob(fileEventDrivenJob, EditMode.NEW);
            this.jobSynchronisationRequiredListeners.forEach(listener ->
                fileEventJobDialog.addJobSynchronisationRequiredListener(listener));
            fileEventJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.scheduled-job", UI.getCurrent().getLocale()), event -> {
            QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(null,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService,
                this.contextTemplate.isUseDisplayName(), this.contextTemplate, true);
            quartzDrivenScheduledJobDialog.addSchedulerJobSelectedListener(this);

            QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
            quartzScheduleDrivenJob.setContextName(this.rootContextTemplate.getName());

            quartzDrivenScheduledJobDialog.setJob(quartzScheduleDrivenJob, EditMode.NEW);
            this.jobSynchronisationRequiredListeners.forEach(listener ->
                quartzDrivenScheduledJobDialog.addJobSynchronisationRequiredListener(listener));
            quartzDrivenScheduledJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.global-job", UI.getCurrent().getLocale()), event -> {
            GlobalEventJobDialog globalEventJobDialog = new GlobalEventJobDialog(null,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                this.schedulerJobService, this.contextTemplate.isUseDisplayName());

            globalEventJobDialog.addSchedulerJobSelectedListener(this);

            GlobalEventJob globalEventJob = new GlobalEventJobImpl();
            globalEventJob.setContextName(JobConstants.GLOBAL_EVENT);

            globalEventJobDialog.setJob(globalEventJob, EditMode.NEW);
            globalEventJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.local-event-job", UI.getCurrent().getLocale()), event -> {
            LocalEventJobDialog localEventJobDialog = new LocalEventJobDialog(this.systemEventLogger
                , this.schedulerJobService, this.rootContextTemplate, this.contextTemplate);

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

    /**
     * Creates a new {@link MenuItem} with an icon and label to be added to the provided {@link MenuBar}.
     * The icon is positioned before the label in the menu item.
     *
     * @param menu the {@link MenuBar} to which the new menu item will be added
     * @param iconName the {@link VaadinIcon} to be used as the icon for the menu item
     * @param label the text label for the menu item
     * @return the created {@link MenuItem} instance added to the specified {@link MenuBar}
     */
    private MenuItem createIconItem(MenuBar menu, VaadinIcon iconName, String label) {
        Icon icon = new Icon(iconName);

        Button menuButton = new Button(label, icon);
        menuButton.setIconAfterText(true);

        MenuItem item = menu.addItem(menuButton);

        return item;
    }

    @Override
    public void jobSelected(SchedulerJob schedulerJob) {
        if(!this.contextTemplate.getScheduledJobs()
            .stream()
            .filter(job -> job.getIdentifier().equals(schedulerJob.getIdentifier()))
            .collect(Collectors.toList()).isEmpty()) {
            NotificationHelper.showUserNotification(getTranslation("error.job-exists-in-context", UI.getCurrent().getLocale()));
        }
        else {
            this.contextTemplate.getScheduledJobs().add(schedulerJob);
            this.schedulerVisualisation.addJob(schedulerJob);
        }
    }

    public void addJobSynchronisationRequiredListener(JobSynchronisationRequiredListener listener) {
        this.jobSynchronisationRequiredListeners.add(listener);
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @WildcardParameter String param) {
        String[] params = param.split("/");
        this.contextName = params[0];
        this.childContextName = params[1];

        if(params.length == 3) {
            this.showPrettyFormat = Boolean.parseBoolean(params[2]);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if(!this.isInitialised) {
            ScheduledContextRecord record = this.scheduledContextService.findByName(contextName);
            ContextTemplate child = ContextHelper.getChildContextTemplate(childContextName, record.getContext());

            try {
                this.createSchedulerVisualisation(record.getContext(), child);
                this.isInitialised = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent beforeLeaveEvent) {
        if(schedulerVisualisation.isSaveRequired()) {
            BeforeLeaveEvent.ContinueNavigationAction continueNavigationAction = beforeLeaveEvent.postpone();
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader(getTranslation("header.save-required", UI.getCurrent().getLocale()));
            confirmDialog.setText(getTranslation("label.unsaved-diagram", UI.getCurrent().getLocale()));
            confirmDialog.setConfirmText(getTranslation("button.ok"));
            confirmDialog.setCancelText(getTranslation("button.cancel"));
            confirmDialog.setCancelable(true);
            confirmDialog.open();
            confirmDialog.addConfirmListener(confirmEvent -> continueNavigationAction.proceed());
        }
    }

    @Override
    public void jobPlanSaveRequired(boolean isSavedRequired) {
        if(isSavedRequired) {
            this.saveButton.getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
            this.saveButton.getStyle().set("color", IkasanColours.WHITE);
        }
        else {
            this.saveButton.getStyle().set("background-color", IkasanColours.WHITE);
            this.saveButton.getStyle().set("color", IkasanColours.IKASAN_ORANGE);
        }
    }
}
