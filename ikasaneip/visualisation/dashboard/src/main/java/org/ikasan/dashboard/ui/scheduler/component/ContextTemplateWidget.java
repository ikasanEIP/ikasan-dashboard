package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.general.component.ProgressIndicatorDialog;
import org.ikasan.dashboard.ui.scheduler.util.ContextTemplateEnableDisableEventBroadcaster;
import org.ikasan.dashboard.ui.scheduler.view.ContextInstanceView;
import org.ikasan.dashboard.ui.scheduler.view.ContextTemplateManagementView;
import org.ikasan.dashboard.ui.util.*;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.orchestration.service.context.util.ContextExportZipUtils;
import org.ikasan.scheduled.context.model.ScheduledContextSearchFilterImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.security.service.authentication.IkasanAuthentication;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.job.service.SpringCloudConfigRefreshService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClientException;
import org.vaadin.olli.FileDownloadWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ContextTemplateWidget extends Div {

    private Registration contextEnableBroadcasterRegistration;
    private ContextTemplateFilteringGrid contextTemplateFilteringGrid;
    private ScheduledContextService scheduledContextService;
    private ContextProfileService contextProfileService;
    private JobProvisionService jobProvisionService;
    private IkasanAuthentication authentication;
    private JobUtilsService jobUtilsService;
    private SchedulerJobService schedulerJobService;
    private String zipWorkingDirectory;
    private ContextInstanceRegistrationService contextInstanceRegistrationService;
    private EmailNotificationDetailsService emailNotificationDetailsService;
    private EmailNotificationContextService emailNotificationContextService;
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;
    private SubMenu activeContextSubMenu;
    private SpringCloudConfigRefreshService springCloudConfigRefreshService;

    /**
     * Constructor
     *
     * @param scheduledContextService
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param scheduledContextInstanceService
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param zipWorkingDirectory
     * @param contextProvisionService
     * @param contextProfileService
     * @param jobProvisionService
     * @param userService
     * @param securityService
     * @param jobUtilsService
     * @param provisionJobs
     * @param contextInstanceRegistrationService
     */
    public ContextTemplateWidget(ScheduledContextService scheduledContextService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                                 JobInitiationService jobInitiationService, String zipWorkingDirectory, ContextProvisionService contextProvisionService,
                                 ContextProfileService contextProfileService, JobProvisionService jobProvisionService, UserService userService,
                                 SecurityService securityService, JobUtilsService jobUtilsService, boolean provisionJobs, ContextInstanceRegistrationService contextInstanceRegistrationService,
                                 EmailNotificationDetailsService emailNotificationDetailsService, EmailNotificationContextService emailNotificationContextService,
                                 Map<String, String> schedulerJobExecutionEnvironmentLabel, SpringCloudConfigRefreshService springCloudConfigRefreshService) {

        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if (this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }
        this.zipWorkingDirectory = zipWorkingDirectory;
        if (this.zipWorkingDirectory == null) {
            throw new IllegalArgumentException("zipWorkingDirectory cannot be null!");
        }
        this.contextProfileService = contextProfileService;
        if (this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }
        this.jobProvisionService = jobProvisionService;
        if (this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if (this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }
        this.emailNotificationDetailsService = emailNotificationDetailsService;
        if (this.emailNotificationDetailsService == null) {
            throw new IllegalArgumentException("emailNotificationDetailsService cannot be null!");
        }
        this.emailNotificationContextService = emailNotificationContextService;
        if (this.emailNotificationContextService == null) {
            throw new IllegalArgumentException("emailNotificationContextService cannot be null!");
        }
        this.springCloudConfigRefreshService = springCloudConfigRefreshService;
        if (this.springCloudConfigRefreshService == null) {
            throw new IllegalArgumentException("springCloudConfigRefreshService cannot be null!");
        }

        this.schedulerJobExecutionEnvironmentLabel = schedulerJobExecutionEnvironmentLabel;

        this.authentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
        this.createGrid(dynamicImagePath, moduleMetaDataService
            , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, scheduledContextInstanceService, schedulerJobInstanceService, jobInitiationService
            , userService, securityService);

        Div div = new Div();
        div.setSizeFull();

        Icon icon = VaadinIcon.SEARCH.create();
        icon.setSize("12pt");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidth("100%");
        H4 contextTemplates = new H4(getTranslation("label.job-plans", UI.getCurrent().getLocale()));
        headerLayout.add(contextTemplates);

        HorizontalLayout actionButtonLayout = new HorizontalLayout();
        actionButtonLayout.setMargin(false);

        MenuBar quickAccessMenu = this.createQuickAccessMenu();

        Icon uploadIcon = VaadinIcon.UPLOAD_ALT.create();
        Button uploadJobPlan = new Button(getTranslation("button.upload-job-plan", UI.getCurrent().getLocale()), uploadIcon);
        uploadJobPlan.setIconAfterText(true);
        uploadJobPlan.addClickListener(buttonClickEvent -> {
            ContextImportFileDialog importer = new ContextImportFileDialog(contextProvisionService, provisionJobs);
            importer.open();
        });

        ComponentSecurityVisibility.applySecurity(authentication, uploadJobPlan
            , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN);

        Icon newContextIcon = VaadinIcon.PLUS.create();
        Button newContextButton = new Button(getTranslation("button.new-job-plan", UI.getCurrent().getLocale()), newContextIcon);
        newContextButton.setIconAfterText(true);
        newContextButton.addClickListener(buttonClickEvent -> {
            ContextTemplateDialog contextTemplateDialog = new ContextTemplateDialog(this.scheduledContextService, this.schedulerJobService
                , getTranslation("label.new-context-template", UI.getCurrent().getLocale()), true);
            contextTemplateDialog.open();
            contextTemplateDialog.addOpenedChangeListener(dialogOpenedChangeEvent -> this.updateActiveContextMenu());
            contextTemplateDialog.addOpenedChangeListener(dialogOpenedChangeEvent -> {
                if (!dialogOpenedChangeEvent.isOpened()) {
                    this.contextTemplateFilteringGrid.getDataProvider().refreshAll();
                }
            });
        });

        ComponentSecurityVisibility.applySecurity(authentication, newContextButton
            , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN);

        Icon refreshIcon = VaadinIcon.REFRESH.create();
        Button refreshContextParamButton = new Button(getTranslation("button.refresh-job-params", UI.getCurrent().getLocale()), refreshIcon);
        refreshContextParamButton.setIconAfterText(true);
        refreshContextParamButton.addClickListener(buttonClickEvent -> {
            try {
                springCloudConfigRefreshService.actuatorRefresh();
                NotificationHelper.showUserNotification(getTranslation("message.refresh-job-params-successful", UI.getCurrent().getLocale()));
            } catch (RestClientException e) {
                NotificationHelper.showUserNotification(getTranslation("message.refresh-job-params-unsuccessful", UI.getCurrent().getLocale()));
            }
        });

        ComponentSecurityVisibility.applySecurity(authentication, refreshContextParamButton
            , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
            , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
            , SecurityConstants.SCHEDULER_ALL_ADMIN);
        
        actionButtonLayout.add(refreshContextParamButton, newContextButton, uploadJobPlan, quickAccessMenu);
        actionButtonLayout.getElement().getStyle().set("position", "absolute");
        actionButtonLayout.getElement().getStyle().set("right", "30px");

        headerLayout.add(actionButtonLayout);

        div.add(headerLayout, this.contextTemplateFilteringGrid);

        this.contextTemplateFilteringGrid.init();

        this.add(div);
        this.setSizeFull();
    }

    /**
     * Method to create the job plan grid.
     *
     * @param dynamicImagePath
     * @param moduleMetaDataService
     * @param scheduledProcessManagementService
     * @param configurationRestService
     * @param moduleControlRestService
     * @param metaDataRestService
     * @param systemEventLogger
     * @param schedulerJobService
     * @param logStreamingService
     * @param scheduledContextInstanceService
     * @param schedulerJobInstanceService
     * @param jobInitiationService
     * @param userService
     * @param securityService
     */
    private void createGrid(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                              ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                              MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                              LogStreamingService logStreamingService, ScheduledContextInstanceService scheduledContextInstanceService, SchedulerJobInstanceService schedulerJobInstanceService,
                              JobInitiationService jobInitiationService, UserService userService, SecurityService securityService) {
        // Create a modulesGrid bound to the list
        ScheduledContextSearchFilter contextSearchFilter = new ScheduledContextSearchFilterImpl();
        contextTemplateFilteringGrid = new ContextTemplateFilteringGrid(this.scheduledContextService, contextSearchFilter);
        contextTemplateFilteringGrid.removeAllColumns();
        contextTemplateFilteringGrid.setVisible(true);
        contextTemplateFilteringGrid.setWidthFull();
        contextTemplateFilteringGrid.setHeight("1000px");


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
                HorizontalLayout horizontalLayout = new HorizontalLayout();

                Text text = new Text(scheduledContextRecord.getContextName());

                horizontalLayout.add(text);
                return horizontalLayout;
            })).setHeader(getTranslation("table-header.context-name", UI.getCurrent().getLocale()))
            .setKey("moduleName")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(4);

        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextRecord.getContext().getDescription());

            horizontalLayout.add(text);
            return horizontalLayout;
        })).setHeader(getTranslation("table-header.context-description", UI.getCurrent().getLocale()))
            .setResizable(true)
            .setFlexGrow(8);


        contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout layout = new HorizontalLayout();

            Icon edit = IconDecorator.decorate(new Icon(VaadinIcon.MODAL), getTranslation("tooltip.manage-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            edit.setId("editScheduledJob");
            ComponentSecurityVisibility.applySecurity(this.authentication,  edit, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE
                , SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_READ, SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE
                , SecurityConstants.SCHEDULER_ALL_READ);

            edit.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                ContextTemplateManagementDialog contextTemplateManagementDialog
                    = new ContextTemplateManagementDialog(this.scheduledContextService, scheduledContextInstanceService, dynamicImagePath, moduleMetaDataService
                    , scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                    , schedulerJobService, logStreamingService, scheduledContextRecord.getContext(), schedulerJobInstanceService, jobInitiationService, this.contextProfileService
                    , this.jobProvisionService, userService, securityService, this.jobUtilsService, this.zipWorkingDirectory, this.emailNotificationDetailsService
                    , this.emailNotificationContextService, this.schedulerJobExecutionEnvironmentLabel, this.contextInstanceRegistrationService
                );
                contextTemplateManagementDialog.open();
            });

            layout.add(edit);

            Icon delete = IconDecorator.decorate(new Icon(VaadinIcon.TRASH), getTranslation("tooltip.delete-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, delete, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN, SecurityConstants.SCHEDULER_ALL_ADMIN);

            layout.add(delete);

            delete.addClickListener(iconClickEvent -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader(getTranslation("confirm-dialog.delete-context-template-header"
                    , UI.getCurrent().getLocale()));
                confirmDialog.setText(getTranslation("confirm-dialog.delete-context-template-body"
                    , UI.getCurrent().getLocale()));
                confirmDialog.setCancelable(true);
                confirmDialog.addConfirmListener(confirmEvent -> {
                    ProgressIndicatorDialog dialog = new ProgressIndicatorDialog(false);
                    dialog.setWidth("700px");
                    dialog.setHeight("250px");
                    dialog.open(getTranslation("progress-dialog.delete-context-template-header", UI.getCurrent().getLocale()),
                        getTranslation("progress-dialog.delete-context-template-body", UI.getCurrent().getLocale()));

                    final UI current = UI.getCurrent();
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        try {
                            this.jobProvisionService.removeJobs(scheduledContextRecord.getContextName());
                            this.schedulerJobService.deleteByContextName(scheduledContextRecord.getContextName());
                            this.scheduledContextService.deleteContext(scheduledContextRecord.getContextName());
                            this.contextInstanceRegistrationService.deRegister(scheduledContextRecord.getContextName());
                            this.emailNotificationDetailsService.deleteByContextName(scheduledContextRecord.getContextName());
                            this.emailNotificationContextService.deleteByContextName(scheduledContextRecord.getContextName());

                            current.access(() -> {
                                this.contextTemplateFilteringGrid.getDataProvider().refreshAll();
                                this.updateActiveContextMenu();

                                NotificationHelper.showUserNotification(getTranslation("notification.context-deleted-successfully"
                                    , UI.getCurrent().getLocale()));
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            current.access(() -> {
                                NotificationHelper.showUserNotification(getTranslation("error.delete-context-template"
                                    , UI.getCurrent().getLocale()));
                            });
                        }
                        finally {
                            current.access(() -> {
                                dialog.close();
                            });
                        }
                    });
                });
                confirmDialog.open();
            });

            Icon clone = IconDecorator.decorate(new Icon(VaadinIcon.COPY), getTranslation("tooltip.clone-context", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, clone, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE);
            clone.addClickListener(iconClickEvent -> {
                CloneContextTemplateDialog cloneContextTemplateDialog = new CloneContextTemplateDialog(this.scheduledContextService
                    , this.contextTemplateFilteringGrid, this.contextInstanceRegistrationService, this.schedulerJobService
                    , this.contextProfileService, scheduledContextRecord);

                cloneContextTemplateDialog.addOpenedChangeListener(event -> this.updateActiveContextMenu());
                cloneContextTemplateDialog.open();
            });

            layout.add(clone);

            Icon chart = IconDecorator.decorate(new Icon(VaadinIcon.CHART), getTranslation("tooltip.contexts-statistics", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, chart, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);
            chart.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                UnderConstructionDialog underConstructionDialog = new UnderConstructionDialog();
                underConstructionDialog.open();
            });

            layout.add(chart);

            Icon export = IconDecorator.decorate(new Icon(VaadinIcon.DOWNLOAD_ALT), getTranslation("tooltip.export-jobs-and-associated-artifacts", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, export, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);
            StreamResource streamResource = new StreamResource(ContextExportZipUtils.getExportZipFileName(scheduledContextRecord.getContextName()), () -> {
                try {
                    ByteArrayOutputStream byteArrayOutputStream = ContextExportZipUtils.createZipFile(
                        scheduledContextRecord.getContext(),
                        this.zipWorkingDirectory,
                        this.schedulerJobService,
                        this.emailNotificationDetailsService,
                        this.emailNotificationContextService,
                        this.contextProfileService,
                        50 // limit to loop searching solr
                    );
                    return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                } catch (Exception e) {
                    e.printStackTrace();
                    NotificationHelper.showErrorNotification(getTranslation("error.download-context", UI.getCurrent().getLocale()));
                    return null;
                }
            });

            FileDownloadWrapper exportWrapper = new FileDownloadWrapper(streamResource);
            exportWrapper.wrapComponent(export);

            layout.add(exportWrapper);

            Icon newWindow = IconDecorator.decorate(new Icon(VaadinIcon.EXTERNAL_LINK), getTranslation("tooltip.open-in-new-window", UI.getCurrent().getLocale()), "16pt", "rgba(0, 0, 0, 1.0)");
            ComponentSecurityVisibility.applySecurity(this.authentication, export, SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ADMIN
                , SecurityConstants.SCHEDULER_ALL_ADMIN, SecurityConstants.SCHEDULER_ALL_WRITE, SecurityConstants.SCHEDULER_ALL_READ, SecurityConstants.SCHEDULER_READ);
            newWindow.addClickListener((ComponentEventListener<ClickEvent<Icon>>) iconClickEvent -> {
                String route = RouteConfiguration.forSessionScope()
                    .getUrl(ContextTemplateManagementView.class, scheduledContextRecord.getContextName());

                getUI().ifPresent(ui -> ui.getPage().open(route));
            });

            layout.add(newWindow);

            layout.setWidth("250px");
            return layout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.actions", UI.getCurrent().getLocale()))
            .setFlexGrow(4);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">[[item.date]]</div>")
            .withProperty("date",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getTimestamp())))
            .setHeader(getTranslation("table-header.created-date-time", UI.getCurrent().getLocale()))
            .setKey("timestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(TemplateRenderer.<ScheduledContextRecord>of(
            "<div style=\"word-wrap:normal; white-space:normal\">[[item.modified]]</div>")
            .withProperty("modified",
                ikasanSolrDocument -> DateFormatter.instance().getFormattedDate(ikasanSolrDocument.getModifiedTimestamp())))
            .setHeader(getTranslation("table-header.modified-date-time", UI.getCurrent().getLocale()))
            .setKey("modifiedTimestamp")
            .setResizable(true)
            .setSortable(true)
            .setFlexGrow(2);

        this.contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            Text text = new Text(scheduledContextRecord.getModifiedBy());

            horizontalLayout.add(text);
            return horizontalLayout;
        }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.modified-by", UI.getCurrent().getLocale()))
            .setSortable(true)
            .setFlexGrow(1);

        this.contextTemplateFilteringGrid.addColumn(new ComponentRenderer<>(scheduledContextRecord -> {
                Button enabled = new Button(getTranslation("button.enabled", UI.getCurrent().getLocale()));
                Button disabled = new Button(getTranslation("button.disabled", UI.getCurrent().getLocale()));
                if(!scheduledContextRecord.isDisabled()){
                    enabled.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_COMPLETE);
                    enabled.getElement().getStyle().set("color", IkasanColours.WHITE);
                    disabled.getElement().getStyle().remove("background-color");
                    disabled.getElement().getStyle().remove("color");
                    enabled.setEnabled(false);
                }
                else {
                    enabled.getElement().getStyle().remove("background-color");
                    enabled.getElement().getStyle().remove("color");
                    disabled.getElement().getStyle().set("background-color", IkasanColours.SCHEDULER_ERROR);
                    disabled.getElement().getStyle().set("color", IkasanColours.WHITE);
                    disabled.setEnabled(false);
                }

                UI ui = UI.getCurrent();

                enabled.addClickListener(event -> {
                    ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);
                    progressIndicatorDialog.setWidth("550px");
                    progressIndicatorDialog.open(getTranslation("progress-dialog.enable-context-template-header", UI.getCurrent().getLocale())
                        , getTranslation("progress-dialog.enable-context-template-text", UI.getCurrent().getLocale()));

                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        try {
                            ContextTemplate contextTemplate = scheduledContextRecord.getContext();
                            ScheduledContextRecord refreshedScheduledContextRecord = this.scheduledContextService.findByName(contextTemplate.getName());
                            contextTemplate = refreshedScheduledContextRecord.getContext();
                            contextTemplate.setDisabled(false);
                            refreshedScheduledContextRecord.setContext(contextTemplate);
                            scheduledContextRecord.setModifiedBy(authentication.getName());
                            this.jobProvisionService.provisionJobs(this.getSchedulerJobForContext(contextTemplate.getName())
                                , this.authentication.getName());
                            this.scheduledContextService.save(refreshedScheduledContextRecord);
                            this.contextInstanceRegistrationService.register(contextTemplate.getName());
                            contextTemplateFilteringGrid.getDataProvider().refreshAll();
                            this.updateActiveContextMenu();
                            ContextTemplateEnableDisableEventBroadcaster.broadcast(scheduledContextRecord.getContext());
                            progressIndicatorDialog.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            if(ui != null && ui.isAttached()) {
                                ui.access(() -> {
                                    progressIndicatorDialog.close();
                                    NotificationHelper.showErrorNotification(getTranslation("error.enabling-context", UI.getCurrent().getLocale()));
                                });
                            }
                        }
                    });
                });

                ComponentSecurityVisibility.applyEnabledSecurity(authentication, enabled
                    , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
                    , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
                    , SecurityConstants.SCHEDULER_ALL_ADMIN);

                disabled.addClickListener(event -> {
                    ConfirmDialog confirmDialog = new ConfirmDialog();
                    confirmDialog.setHeader(getTranslation("confirm.disable-context-header", UI.getCurrent().getLocale()));
                    confirmDialog.setText(getTranslation("confirm.disable-context-body", UI.getCurrent().getLocale()));
                    confirmDialog.setCancelable(true);
                    confirmDialog.open();

                    confirmDialog.addConfirmListener(confirmEvent -> {
                        ProgressIndicatorDialog progressIndicatorDialog = new ProgressIndicatorDialog(false);
                        progressIndicatorDialog.setWidth("550px");
                        progressIndicatorDialog.open(getTranslation("progress-dialog.disable-context-template-header", UI.getCurrent().getLocale())
                            , getTranslation("progress-dialog.disable-context-template-text", UI.getCurrent().getLocale()));

                        Executor executor = Executors.newSingleThreadExecutor();
                        executor.execute(() -> {
                            try {
                                ContextTemplate contextTemplate = scheduledContextRecord.getContext();
                                contextTemplate.setDisabled(true);
                                scheduledContextRecord.setContext(contextTemplate);
                                scheduledContextRecord.setModifiedBy(authentication.getName());
                                this.jobProvisionService.removeJobs(contextTemplate.getName());
                                ContextMachine contextMachine = ContextMachineCache.instance()
                                    .getByContextName(contextTemplate.getName());
                                if (contextMachine != null) {
                                    ContextMachineCache.instance().remove(contextMachine);
                                    contextMachine.teardown();
                                }
                                this.scheduledContextService.save(scheduledContextRecord);
                                contextTemplateFilteringGrid.getDataProvider().refreshAll();
                                this.updateActiveContextMenu();
                                ContextTemplateEnableDisableEventBroadcaster.broadcast(scheduledContextRecord.getContext());
                                progressIndicatorDialog.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                                if(ui != null && ui.isAttached()) {
                                    ui.access(() -> {
                                        progressIndicatorDialog.close();
                                        NotificationHelper.showErrorNotification(getTranslation("error.disabling-context", UI.getCurrent().getLocale()));
                                    });
                                }
                            }
                        });
                    });
                });

                ComponentSecurityVisibility.applyEnabledSecurity(authentication, disabled
                    , SecurityConstants.ALL_AUTHORITY, SecurityConstants.SCHEDULER_ADMIN
                    , SecurityConstants.SCHEDULER_WRITE, SecurityConstants.SCHEDULER_ALL_WRITE
                    , SecurityConstants.SCHEDULER_ALL_ADMIN);

                HorizontalLayout buttons = new HorizontalLayout();
                buttons.setWidth("200px");
                buttons.add(enabled, disabled);

                return buttons;
            }))
            .setResizable(true)
            .setHeader(getTranslation("table-header.enabled-disabled", UI.getCurrent().getLocale()))
            .setSortable(false)
            .setKey("isDisabled")
            .setFlexGrow(2);

        HeaderRow hr = contextTemplateFilteringGrid.appendHeaderRow();
        this.contextTemplateFilteringGrid.addGridFiltering(hr, contextSearchFilter::setContextName, "moduleName");
    }

    /**
     * Helper method to create the quick action menu bar.
     *
     * @return
     */
    private MenuBar createQuickAccessMenu() {
        MenuBar quickStartMenuBar = new MenuBar();
        quickStartMenuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        MenuItem quickAccess = createQuickActionMenuItem(quickStartMenuBar
            , VaadinIcon.COG, getTranslation("menu-item.quick-access", UI.getCurrent().getLocale()));

        SubMenu activeContextInstancesSubMenu = quickAccess.getSubMenu();
        MenuItem activeContexts = activeContextInstancesSubMenu
            .addItem(getTranslation("menu-item.active-contexts", UI.getCurrent().getLocale()));
        this.activeContextSubMenu = activeContexts.getSubMenu();
        this.updateActiveContextMenu();

        return quickStartMenuBar;
    }

    /**
     * Helper method to get all the scheduler jobs associated with the context.
     *
     * @param contextName
     * @return
     */
    private List<SchedulerJob> getSchedulerJobForContext(String contextName) {
        List< SchedulerJobRecord> schedulerJobRecordList = this.schedulerJobService
            .findByContext(contextName, -1, -1).getResultList();

        return schedulerJobRecordList.stream()
            .map(schedulerJobRecord -> schedulerJobRecord.getJob())
            .collect(Collectors.toList());
    }

    /**
     * Helper method to update the contents of the active context menu.
     */
    private void updateActiveContextMenu() {
        this.activeContextSubMenu.removeAll();
        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(authentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(authentication);

        ContextMachineCache.instance().contextNames().forEach(name -> {
            if (canAccessAllJobPlans || accessibleJobPlans.contains(name)) {
                this.activeContextSubMenu.addItem(name, itemClickEvent -> {
                    String route = RouteConfiguration.forSessionScope()
                        .getUrl(ContextInstanceView.class, ContextMachineCache.instance()
                            .getByContextName(name).getContext().getId() + "_scheduledContextInstance");

                    getUI().ifPresent(ui -> ui.getPage().open(route));
                });
            }
        });
    }

    /**
     * Helper method to create the quick action menu item.
     *
     * @param menu
     * @param iconName
     * @param label
     * @return
     */
    private MenuItem createQuickActionMenuItem(MenuBar menu, VaadinIcon iconName, String label) {
        Icon icon = new Icon(iconName);
        Button quickAccessButton = new Button(label, icon);
        quickAccessButton.setIconAfterText(true);

        MenuItem item = menu.addItem(quickAccessButton);

        return item;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        this.contextEnableBroadcasterRegistration = ContextTemplateEnableDisableEventBroadcaster.register(flowState -> {
            if(ui.isAttached()) {
                ui.access(() -> this.contextTemplateFilteringGrid.getDataProvider().refreshAll());
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if(this.contextEnableBroadcasterRegistration != null) {
            this.contextEnableBroadcasterRegistration.remove();
            this.contextEnableBroadcasterRegistration = null;
        }
    }
}
