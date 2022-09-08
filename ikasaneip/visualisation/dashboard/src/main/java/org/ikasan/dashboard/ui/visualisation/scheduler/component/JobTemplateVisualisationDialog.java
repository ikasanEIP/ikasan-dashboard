package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.*;
import org.ikasan.dashboard.ui.scheduler.listener.SchedulerJobSelectedListener;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.job.orchestration.service.ContextService;
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
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private JobInitiationService jobInitiationService;
    private ContextProfileService contextProfileService;
    private UserService userService;
    private SecurityService securityService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private JobProvisionService jobProvisionService;
    private ScheduledContextService scheduledContextService;

    private ContextService contextService = new ContextService();

    private SchedulerVisualisation schedulerVisualisation;

    public JobTemplateVisualisationDialog(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                          MetaDataService metaDataRestService, SystemEventLogger systemEventLogger,
                                          SchedulerJobService schedulerJobService, LogStreamingService logStreamingService,
                                          SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService,
                                          ContextProfileService contextProfileService, UserService userService, SecurityService securityService,
                                          ScheduledContextInstanceService scheduledContextInstanceService, JobProvisionService jobProvisionService,
                                          ScheduledContextService scheduledContextService) {
        this.setHeight("90%");
        this.setWidth("90%");

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

        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if(this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
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

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }

        this.jobProvisionService = jobProvisionService;
        if(this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

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

        this.schedulerVisualisation = new SchedulerVisualisation(this.dynamicImagePath, this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService,
            this.logStreamingService, this.schedulerJobInstanceService, this.jobInitiationService, this.contextProfileService, this.userService, this.securityService,
            this.scheduledContextInstanceService, this.jobProvisionService, this.scheduledContextService);
        this.schedulerVisualisation.createSchedulerVisualisation(this.rootContextTemplate, this.contextTemplate, this, true);

        this.layout.add(this.schedulerVisualisation);
        super.title.setText(this.contextTemplate.getName());
    }

    private void initParentNavigation() {
        HorizontalLayout buttonLayout = new HorizontalLayout();

        if(!initialised && this.contextTemplate != null) {

            ContextTemplate parentContextInstance = contextService.getParent(this.rootContextTemplate, this.contextTemplate);

            if(parentContextInstance != null) {
                Button gotoParentButton = new Button("Go to Parent - " + parentContextInstance.getName(), VaadinIcon.ARROW_UP.create());
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
                                    , this.schedulerJobInstanceService, this.jobInitiationService, this.contextProfileService, this.userService, this.securityService,
                                    this.scheduledContextInstanceService, this.jobProvisionService, this.scheduledContextService);

                                contextTemplateVisualisationDialog.createSchedulerVisualisation(this.rootContextTemplate, this.contextTemplate);
                                contextTemplateVisualisationDialog.open();

                                this.close();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                buttonLayout.add(gotoParentButton);
                buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.BASELINE, gotoParentButton);
            }

            Button addContextViewButton = new Button("Add As Context View", VaadinIcon.PLUS.create());
            addContextViewButton.setIconAfterText(true);
//            addContextViewButton.getElement().getStyle().set("position", "absolute");
//            addContextViewButton.getElement().getStyle().set("right", "45px");

            addContextViewButton.addClickListener(event -> {
                ContextViewManagementDialog contextViewManagementDialog = new ContextViewManagementDialog(this.contextProfileService, this.userService,
                    this.securityService, this.systemEventLogger, this.rootContextTemplate.getName(), this.contextTemplate.getName());
                contextViewManagementDialog.open();
            });

            MenuBar createNewJobMenuBar = this.createNewJobMenuBar();
//            createNewJobMenuBar.getElement().getStyle().set("position", "absolute");
//            createNewJobMenuBar.getElement().getStyle().set("right", "250px");

            Button addAnd = new Button("Add AND Grouping &&");
            addAnd.addClickListener(buttonClickEvent -> {
                this.schedulerVisualisation.addAndGrouping();
            });
            Button addOr = new Button("Add OR Grouping ||");
            addOr.addClickListener(buttonClickEvent -> {
               this.schedulerVisualisation.addOrGrouping();
            });
            Button jsonButton = new Button("JSON", VaadinIcon.CODE.create());
            jsonButton.setIconAfterText(true);
            jsonButton.addClickListener(buttonClickEvent -> {
                JsonViewerDialog jsonViewerDialog = new JsonViewerDialog(this.schedulerVisualisation.getContextTemplate());
                jsonViewerDialog.open();
            });
            Button saveButton = new Button("Save");
            saveButton.addClickListener(buttonClickEvent -> {
                this.schedulerVisualisation.save();
            });


            buttonLayout.add(createNewJobMenuBar, addAnd, addOr, jsonButton, addContextViewButton, saveButton);
            buttonLayout.setVerticalComponentAlignment(FlexComponent.Alignment.BASELINE, createNewJobMenuBar, addAnd, addOr, jsonButton, addContextViewButton, saveButton);

            this.layout.add(buttonLayout);
            layout.setHorizontalComponentAlignment(FlexComponent.Alignment.END, buttonLayout);

            this.initialised = true;
        }
    }

    private MenuBar createNewJobMenuBar() {
        MenuBar newJobMenuBar = new MenuBar();
        newJobMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem quickAccess = createIconItem(newJobMenuBar, VaadinIcon.TOOLBOX, getTranslation("menu-item.add-a-job", UI.getCurrent().getLocale()));

        SubMenu addJobSubMenu = quickAccess.getSubMenu();
        MenuItem newJobMenuItem = addJobSubMenu.addItem(getTranslation("menu-item.new-job", UI.getCurrent().getLocale()));
        addJobSubMenu.addItem(getTranslation("menu-item.existing-job", UI.getCurrent().getLocale()), menuItemClickEvent -> {
            SchedulerJobSelectDialog schedulerJobSelectDialog = new SchedulerJobSelectDialog(this.scheduledContextInstanceService, this.dynamicImagePath, this.moduleMetaDataService,
                this.scheduledProcessManagementService, this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger,
                this.schedulerJobService, this.logStreamingService, this.rootContextTemplate, this.jobInitiationService, this.jobProvisionService);

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
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);
            internalEventDrivenJobDialog.addSchedulerJobSelectedListener(this);

            InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();
            internalEventDrivenJob.setContextId(this.rootContextTemplate.getName());

            internalEventDrivenJobDialog.setJob(internalEventDrivenJob, EditMode.NEW);
            internalEventDrivenJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.file-watcher-job", UI.getCurrent().getLocale()), event -> {
            FileEventJobDialog fileEventJobDialog = new FileEventJobDialog(null, this.scheduledProcessManagementService, this.configurationRestService,
                this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);
            fileEventJobDialog.addSchedulerJobSelectedListener(this);

            FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
            fileEventDrivenJob.setContextId(this.rootContextTemplate.getName());

            fileEventJobDialog.setJob(fileEventDrivenJob, EditMode.NEW);

            fileEventJobDialog.open();
        });
        jobTypesSubMenu.addItem(getTranslation("menu-item.scheduled-job", UI.getCurrent().getLocale()), event -> {
            QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog = new QuartzDrivenScheduledJobDialog(null, this.scheduledProcessManagementService,
                this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.schedulerJobService);
            quartzDrivenScheduledJobDialog.addSchedulerJobSelectedListener(this);

            QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
            quartzScheduleDrivenJob.setContextId(this.rootContextTemplate.getName());

            quartzDrivenScheduledJobDialog.setJob(quartzScheduleDrivenJob, EditMode.NEW);

            quartzDrivenScheduledJobDialog.open();
        });

        return newJobMenuBar;
    }

    private MenuItem createIconItem(MenuBar menu, VaadinIcon iconName, String label) {
        Icon icon = new Icon(iconName);

        Button menuButton = new Button(label, icon);
        menuButton.setIconAfterText(true);

        MenuItem item = menu.addItem(menuButton);
//        item.getElement().getStyle().set("padding", "0px");
//        item.getElement().getStyle().set("padding-right", "5px");

        return item;
    }

    @Override
    public void jobSelected(SchedulerJob schedulerJob) {
        logger.info(schedulerJob.getIdentifier());

        if(this.contextTemplate.getScheduledJobs()
            .stream()
            .filter(job -> job.getIdentifier().equals(schedulerJob.getIdentifier()))
            .collect(Collectors.toList()).size() > 0) {
            NotificationHelper.showUserNotification("Cannot add this job as it already exists in this context!");
            return;
        }
        else {

            this.schedulerVisualisation.addJob(schedulerJob);
        }
    }
}
