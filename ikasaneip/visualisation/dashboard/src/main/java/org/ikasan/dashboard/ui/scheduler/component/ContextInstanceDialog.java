package org.ikasan.dashboard.ui.scheduler.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.systemevent.SystemEventSearchService;

public class ContextInstanceDialog extends AbstractCloseableResizableDialog {
    private ContextInstanceWidget contextInstanceWidget;

    public ContextInstanceDialog(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextInstance contextInstance, ContextTemplate contextTemplate,
                                 SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService, ContextProfileService contextProfileService,
                                 JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService, GlobalEventService globalEventService,
                                 ContextInstanceRegistrationService contextInstanceRegistrationService, ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, SystemEventSearchService systemEventSearchService,
                                 double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        this.contextInstanceWidget = new ContextInstanceWidget(scheduledContextInstanceService, dynamicImagePath
            , moduleMetaDataService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextInstance, contextTemplate, schedulerJobInstanceService, jobInitiationService, contextProfileService
            , jobUtilsService, scheduledContextService, globalEventService, contextInstanceRegistrationService, contextInstanceSchedulerService, systemEventSearchService
            , jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance);

        this.init();
    }

    public ContextInstanceDialog(ScheduledContextInstanceService scheduledContextInstanceService, String dynamicImagePath, ModuleMetaDataService moduleMetaDataService,
                                 ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                 MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService,
                                 LogStreamingService logStreamingService, ContextInstance contextInstance, ContextTemplate contextTemplate,
                                 SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService, ContextProfileService contextProfileService,
                                 JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService, String selectedTab, String jobStatus, GlobalEventService globalEventService,
                                 ContextInstanceRegistrationService contextInstanceRegistrationService, ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService, SystemEventSearchService systemEventSearchService,
                                 double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
        this.contextInstanceWidget = new ContextInstanceWidget(scheduledContextInstanceService, dynamicImagePath
            , moduleMetaDataService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger
            , schedulerJobService, logStreamingService, contextInstance, contextTemplate, schedulerJobInstanceService, jobInitiationService, contextProfileService
            , jobUtilsService, scheduledContextService, selectedTab, jobStatus, null, globalEventService, contextInstanceRegistrationService, contextInstanceSchedulerService,
            systemEventSearchService, jobVisualisationVerticalSpacing, jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance);
        this.init();
    }

    private void init() {
        this.contextInstanceWidget.beforeEnter(null);

        this.setHeight("95vh");
        this.setWidth("90vw");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.add(this.contextInstanceWidget);
        layout.getStyle().set("padding-bottom", "20px");
        layout.getStyle().set("padding-top", "0px");

        super.title.setText(String.format(getTranslation("label.context-instance", UI.getCurrent().getLocale())));

        super.content.getStyle().set("padding-top", "0px");
        super.content.add(layout);
    }
}
