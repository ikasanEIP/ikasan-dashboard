package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.*;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;

import java.util.Map;

public class ContextTemplateManagementDialog extends AbstractCloseableResizableDialog {
    private ContextTemplateManagementWidget contextTemplateManagementWidget;

    /**
     * Constructor for ContextTemplateManagementDialog class.
     *
     * @param scheduledContextService The service for scheduled context.
     * @param scheduledContextInstanceService The service for scheduled context instances.
     * @param dynamicImagePath The path for dynamic images.
     * @param moduleMetaDataService The service for module metadata.
     * @param scheduledProcessManagementService The service for scheduled process management.
     * @param configurationRestService The configuration service.
     * @param moduleControlRestService The module control service.
     * @param metaDataRestService The metadata service.
     * @param systemEventLogger The system event logger service.
     * @param schedulerJobService The scheduler job service.
     * @param logStreamingService The log streaming service.
     * @param contextTemplate The context template.
     * @param schedulerJobInstanceService The scheduler job instance service.
     * @param jobInitiationService The job initiation service.
     * @param contextProfileService The context profile service.
     * @param jobProvisionService The job provision service.
     * @param userService The user service.
     * @param securityService The security service.
     * @param jobUtilsService The job utilities service.
     * @param zipWorkingDirectory The working directory for ZIP files.
     * @param emailNotificationDetailsService The service for email notification details.
     * @param emailNotificationContextService The service context for email notification.
     * @param schedulerJobExecutionEnvironmentLabel The labels for scheduler job execution environment.
     * @param globalEventService The global event service.
     * @param contextInstanceRegistrationService The service for context instance registration.
     * @param contextInstanceSchedulerService The service for context instance scheduler.
     * @param springCloudConfigRefreshService The service for Spring Cloud configuration refresh.
     * @param removeTrailingPlanNameContextAfterUnderscore Boolean to remove trailing plan name context after underscore.
     * @param jobPlanIntervalMultiple The job plan interval multiple.
     * @param jobVisualisationVerticalSpacing The vertical spacing for job visualization.
     * @param jobVisualisationHorizontalSpacing The horizontal spacing for job visualization.
     * @param contextVisualisationLevelDistance The level distance for context visualization.
     * @param contextVisualisationNodeDistance The node distance for context visualization.
     */
    public ContextTemplateManagementDialog(ScheduledContextService scheduledContextService, ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                           ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                           MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                           LogStreamingService logStreamingService, ContextTemplate contextTemplate, SchedulerJobInstanceService schedulerJobInstanceService,
                                           JobInitiationService jobInitiationService, ContextProfileService contextProfileService, JobProvisionService jobProvisionService, UserService userService,
                                           SecurityService securityService, JobUtilsService jobUtilsService, String zipWorkingDirectory, EmailNotificationDetailsService emailNotificationDetailsService,
                                           EmailNotificationContextService emailNotificationContextService, Map<String, String> schedulerJobExecutionEnvironmentLabel, GlobalEventService globalEventService,
                                           ContextInstanceRegistrationService contextInstanceRegistrationService, ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, SpringCloudConfigRefreshService springCloudConfigRefreshService,
                                           boolean removeTrailingPlanNameContextAfterUnderscore, int jobPlanIntervalMultiple, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing,
                                           double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        this.contextTemplateManagementWidget = new ContextTemplateManagementWidget(scheduledContextService, scheduledContextInstanceService, dynamicImagePath
            , moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextTemplate, schedulerJobInstanceService, jobInitiationService, contextProfileService, jobProvisionService
            , userService, securityService, jobUtilsService, zipWorkingDirectory, emailNotificationDetailsService, emailNotificationContextService, schedulerJobExecutionEnvironmentLabel, globalEventService
            , contextInstanceRegistrationService, contextInstanceSchedulerService, springCloudConfigRefreshService, removeTrailingPlanNameContextAfterUnderscore, jobPlanIntervalMultiple
            , jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance);

        this.setHeight("95vh");
        this.setWidth("90vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(this.contextTemplateManagementWidget);
        layout.getStyle().set("padding-bottom", "20px");
        layout.getStyle().set("padding-top", "0px");

        super.content.getElement().getStyle().set("padding-top", "0px");
        super.title.setText(String.format(getTranslation("label.context-template-management", UI.getCurrent().getLocale())));

        super.content.add(layout);
    }
}
