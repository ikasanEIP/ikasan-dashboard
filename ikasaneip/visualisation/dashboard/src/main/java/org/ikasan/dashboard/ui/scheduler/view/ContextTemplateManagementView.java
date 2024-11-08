package org.ikasan.dashboard.ui.scheduler.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.security.SecurityUtils;
import org.ikasan.dashboard.ui.layout.IkasanAppLayout;
import org.ikasan.dashboard.ui.scheduler.component.ContextTemplateManagementWidget;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
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
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.*;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.Map;
import java.util.Set;

@Route(value = "contextTemplateManagement", layout = IkasanAppLayout.class)
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/tree-view.css", themeFor = "vaadin-grid")
@CssImport(value = "./styles/grid-header.css", themeFor = "vaadin-grid")
@PermitAll
@PreserveOnRefresh
public class ContextTemplateManagementView extends VerticalLayout implements BeforeEnterObserver, HasUrlParameter<String>
{
    Logger logger = LoggerFactory.getLogger(ContextTemplateManagementView.class);

    @Resource
    private ConfigurationService configurationRestService;

    @Resource
    private ScheduledProcessManagementService scheduledProcessManagementService;

    @Resource
    private ModuleControlService moduleControlRestService;

    @Resource
    private MetaDataService metaDataRestService;

    @Resource
    private SystemEventLogger systemEventLogger;

    @Resource
    private ScheduledContextInstanceService scheduledContextInstanceService;

    @Resource
    private ScheduledContextService scheduledContextService;

    @Resource
    private SchedulerJobService schedulerJobService;

    @Resource(name = "moduleMetadataService")
    private ModuleMetaDataService moduleMetaDataService;

    @Resource
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Value("${scheduled.job.context.queue.directory}")
    private String queueDirectory;

    @Value("${ikasan.dashboard.zip.working.directory:.}")
    private String zipWorkingDirectory;

    @Value("${job.plan.export.remove.trailing.plan.name.context.after.underscore:true}")
    private boolean removeTrailingPlanNameContextAfterUnderscore;

    @Value("#{${scheduler.job.execution.environment.label}}")
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;

    @Value("${job.plan.interval.multiple:3}")
    private int jobPlanIntervalMultiple;

    @Resource
    private LogStreamingService logStreamingService;

    @Resource
    private JobInitiationService jobInitiationService;

    @Resource
    private ContextProfileService contextProfileService;

    @Resource
    private JobProvisionService jobProvisionService;

    @Resource
    private UserService userService;

    @Resource
    private JobUtilsService jobUtilsService;

    @Resource
    private SecurityService securityService;

    @Resource
    private EmailNotificationDetailsService emailNotificationDetailsService;

    @Resource
    private EmailNotificationContextService emailNotificationContextService;

    @Resource
    private ContextInstanceRegistrationService contextInstanceRegistrationService;

    @Resource
    private ContextInstanceSchedulerService contextInstanceSchedulerService;

    @Resource
    private GlobalEventService globalEventService;

    @Resource
    private SpringCloudConfigRefreshService springCloudConfigRefreshService;

    @Value("${job.visualisation.vertical.spacing:120}")
    protected double jobVisualisationVerticalSpacing;
    @Value("${job.visualisation.horizontal.spacing:400}")
    protected double jobVisualisationHorizontalSpacing;
    @Value("${context.visualisation.level.distance:150}")
    protected double contextVisualisationLevelDistance;
    @Value("${context.visualisation.node.distance:75}")
    protected double contextVisualisationNodeDistance;

    private ContextTemplateManagementWidget contextTemplateManagementWidget;

    private ContextTemplate contextTemplate;

    private String contextName;

    private IkasanAuthentication ikasanAuthentication;

    private boolean isInitialised = false;

    /**
     * Constructor
     */
    public ContextTemplateManagementView() {
        this.setSpacing(false);
        this.setMargin(false);
        this.setSizeFull();

        this.ikasanAuthentication = (IkasanAuthentication) SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Initialise the internals of the object.
     */
    private void init() {
        boolean canAccessAllJobPlans = SecurityUtils.canAccessAllJobPlans(ikasanAuthentication);
        Set<String> accessibleJobPlans = SecurityUtils.getAccessibleJobPlans(ikasanAuthentication);

        if(!canAccessAllJobPlans && !accessibleJobPlans.contains(this.contextTemplate.getName())) {
            UI.getCurrent().getPage().setLocation("/scheduler");
        }
        else {
            this.contextTemplateManagementWidget = new ContextTemplateManagementWidget(scheduledContextService, scheduledContextInstanceService, ""
                , moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
                , schedulerJobService, logStreamingService, contextTemplate, this.schedulerJobInstanceService, this.jobInitiationService, this.contextProfileService
                , this.jobProvisionService, this.userService, this.securityService, this.jobUtilsService, this.zipWorkingDirectory, this.emailNotificationDetailsService
                , this.emailNotificationContextService, this.schedulerJobExecutionEnvironmentLabel, this.globalEventService, this.contextInstanceRegistrationService
                , this.contextInstanceSchedulerService, springCloudConfigRefreshService, this.removeTrailingPlanNameContextAfterUnderscore, this.jobPlanIntervalMultiple
                , this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);

            this.getElement().getStyle().set("padding-top", "0px");
            this.add(this.contextTemplateManagementWidget);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.contextTemplate = this.scheduledContextService.findByName(this.contextName).getContext();
        if(!isInitialised) {
            init();
            isInitialised = true;
        }
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, String param) {
        this.contextName = param;
    }
}

