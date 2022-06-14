package org.ikasan.dashboard.ui.scheduler.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.annotation.UIScope;
import org.ikasan.dashboard.ui.scheduler.component.ContextInstanceWidget;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Push
@Route(value = "contextInstance")
@UIScope
@Component
@CssImport("./styles/dashboard-view.css")
@CssImport(value="./styles/chart-styling.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@CssImport(value="./styles/live-errors.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
public class ContextInstanceView extends VerticalLayout implements BeforeEnterObserver, HasUrlParameter<String>
{
    Logger logger = LoggerFactory.getLogger(ContextInstanceView.class);

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

    @Value("${scheduled.job.context.queue.directory}")
    private String queueDirectory;

    @Resource
    private LogStreamingService logStreamingService;

    @Resource
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Resource
    private JobInitiationService jobInitiationService;

    private ContextInstanceWidget contextInstanceWidget;

    private ContextTemplate contextTemplate;
    private ContextInstance contextInstance;

    private String contextInstanceId;

    /**
     * Constructor
     */
    public ContextInstanceView() {
        this.setSpacing(false);
        this.setMargin(false);
    }

    /**
     * Initialise the internals of the object.
     */
    private void init() {
        this.contextInstanceWidget = new ContextInstanceWidget(scheduledContextInstanceService, ""
            , moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextInstance, contextTemplate, this.schedulerJobInstanceService, this.jobInitiationService);

        H2 contextTemplateManagementLabel = new H2(String.format(getTranslation("label.context-instance", UI.getCurrent().getLocale())));
        this.add(contextTemplateManagementLabel, this.contextInstanceWidget);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        this.contextInstance = scheduledContextInstanceService.findById(this.contextInstanceId).getContextInstance();
        this.contextTemplate = this.scheduledContextService.findByName(this.contextInstance.getName()).getContext();
        init();
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, String param) {
        this.contextInstanceId = param;
    }
}

