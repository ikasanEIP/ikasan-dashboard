package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.component.SchedulerStatusDiv;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JobInstanceSplitVisualisationDialog extends AbstractCloseableResizableDialog implements ContextInstanceStateChangeEventLocalBroadcastListener {

    private Logger logger = LoggerFactory.getLogger(JobInstanceSplitVisualisationDialog.class);

    private VerticalLayout layout;

    private boolean initialised = false;

    private ContextInstance rootContextInstance;
    private ContextInstance contextInstance;

    private String dynamicImagePath = ".";

    private ModuleMetaDataService moduleMetaDataService;
    private ScheduledProcessManagementService scheduledProcessManagementService;
    private ConfigurationService configurationRestService;
    private ModuleControlService moduleControlRestService;
    private MetaDataService metaDataRestService;
    private SystemEventLogger systemEventLogger;
    private LogStreamingService logStreamingService;
    private SchedulerJobInstanceService schedulerJobInstanceService;
    private JobInitiationService jobInitiationService;
    private JobUtilsService jobUtilsService;
    private ScheduledContextService scheduledContextService;
    private ContextProfileService contextProfileService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SplitContextInstanceVisualisation splitContextInstanceVisualisation;
    private GlobalEventService globalEventService;
    private SchedulerStatusDiv statusDiv;
    private UI ui;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;

    public JobInstanceSplitVisualisationDialog(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                               ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                               SystemEventLogger systemEventLogger, LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                                               JobInitiationService jobInitiationService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                                               ScheduledContextInstanceService scheduledContextInstanceService, ContextProfileService contextProfileService, GlobalEventService globalEventService,
                                               double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance) {
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

        this.jobUtilsService = jobUtilsService;
        if(this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }

        this.scheduledContextService = scheduledContextService;
        if(this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }

        this.contextProfileService = contextProfileService;
        if(this.contextProfileService == null) {
            throw new IllegalArgumentException("contextProfileService cannot be null!");
        }

        this.globalEventService = globalEventService;
        if(this.globalEventService == null) {
            throw new IllegalArgumentException("globalEventService cannot be null!");
        }

        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;

        layout = new VerticalLayout();
        this.layout.getStyle().set("padding-top", "0px");
        this.layout.getStyle().set("padding-bottom", "10px");
        layout.setHeight("98vh");
        super.content.setHeight("96vh");
        super.content.add(layout);
    }

    /**
     * Create the visualisation.
     *
     * @param contextInstance
     */
    public void createSchedulerVisualisation(ContextInstance rootContextInstance, ContextInstance contextInstance) throws IOException {
        this.rootContextInstance = rootContextInstance;
        this.contextInstance = contextInstance;
        this.initialised = false;

        this.statusDiv = new SchedulerStatusDiv();
        this.statusDiv.setHeight("20px");
        this.statusDiv.setWidth("100%");
        this.statusDiv.getElement().getStyle().set("font-size", "12pt");
        this.statusDiv.setStatus(this.rootContextInstance.getStatus());

        this.layout.add(this.statusDiv);

        this.splitContextInstanceVisualisation = new SplitContextInstanceVisualisation(this.scheduledContextInstanceService, this.moduleMetaDataService, this.scheduledProcessManagementService,
            this.configurationRestService, this.moduleControlRestService, this.metaDataRestService, this.systemEventLogger, this.logStreamingService, rootContextInstance, this.schedulerJobInstanceService,
            this.jobInitiationService, this.contextProfileService, this.jobUtilsService, this.scheduledContextService, this.globalEventService, this.jobVisualisationVerticalSpacing, this.jobVisualisationHorizontalSpacing,
            this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);
        this.splitContextInstanceVisualisation.initialiseVisualisation();
        this.splitContextInstanceVisualisation.setVisible(true);
        this.splitContextInstanceVisualisation.contextOpened(contextInstance);
        this.splitContextInstanceVisualisation.contextSelected(contextInstance.getName());

        this.layout.add(this.splitContextInstanceVisualisation);
        this.title.setText(rootContextInstance.getName());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();

        ContextInstanceStateChangeEventBroadcaster.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;

        ContextInstanceStateChangeEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        if (event.getContextInstance() != null &&
            event.getContextInstance().getId().equals(this.rootContextInstance.getId())) {
            if(this.ui != null && this.ui.isAttached()) {
                this.ui.access(() -> {
                    this.statusDiv.setStatus(event.getNewStatus());
                });
            }
        }
    }
}
