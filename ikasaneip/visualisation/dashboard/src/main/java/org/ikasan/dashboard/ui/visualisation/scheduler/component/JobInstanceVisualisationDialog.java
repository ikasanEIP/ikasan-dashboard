package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.ikasan.dashboard.ui.general.component.AbstractCloseableResizableDialog;
import org.ikasan.dashboard.ui.scheduler.component.SchedulerStatusDiv;
import org.ikasan.dashboard.ui.scheduler.listener.ContextOpenedListener;
import org.ikasan.dashboard.ui.scheduler.listener.ContextSelectedListener;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.util.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcastListener;
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

public class JobInstanceVisualisationDialog extends AbstractCloseableResizableDialog
    implements ContextInstanceStateChangeEventBroadcastListener, ContextOpenedListener, ContextSelectedListener {

    private Logger logger = LoggerFactory.getLogger(JobInstanceVisualisationDialog.class);

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
    private JobSchedulerInstanceVisualisation jobVisualisation;
    private GlobalEventService globalEventService;
    private SchedulerStatusDiv statusDiv;
    private UI ui;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;

    public JobInstanceVisualisationDialog(ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
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
        this.statusDiv.setStatus(this.contextInstance.getStatus());

        this.layout.add(this.statusDiv);

        this.jobVisualisation = new JobSchedulerInstanceVisualisation("", moduleMetaDataService, scheduledProcessManagementService,
            configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, logStreamingService
            , this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService
            , this.globalEventService, this.scheduledContextInstanceService, this.jobVisualisationVerticalSpacing
            , this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);
        this.jobVisualisation.addContextOpenListener(this);
        this.jobVisualisation.addContextSelectedListener(this);
        this.jobVisualisation.setWidthFull();
        this.jobVisualisation.createSchedulerVisualisation(this.rootContextInstance, this.contextInstance, null);
        this.jobVisualisation.setHeight("100%");
        this.jobVisualisation.setVisible(true);

        this.layout.add(this.jobVisualisation);
        this.title.setText(contextInstance.getName());
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
        if(this.ui != null && this.ui.isAttached() && !ui.isClosing() && ui.getSession() != null) {
            if (event.getContextInstance() != null &&
                event.getContextInstance().getId().equals(this.contextInstance.getId())) {
                this.ui.access(() -> {
                    this.statusDiv.setStatus(event.getNewStatus());
                });
            }
        }
        else {
            ContextInstanceStateChangeEventBroadcaster.unregister(this);
        }
    }

    @Override
    public void contextOpened(Context context) {
        try {
            this.close();
            logger.info("context " + context);
            if(ContextMachineCache.instance().containsInstanceIdentifier(this.rootContextInstance.getId())) {
                this.rootContextInstance = ContextMachineCache.instance().getByContextInstanceId(this.rootContextInstance.getId()).getContext();
            }
            ContextInstance child = ContextHelper.getChildContextInstance(context.getName(), this.rootContextInstance);
            JobInstanceVisualisationDialog jobInstanceVisualisationDialog = new JobInstanceVisualisationDialog(moduleMetaDataService, scheduledProcessManagementService,
                configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, logStreamingService
                , this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService
                , this.scheduledContextInstanceService, contextProfileService, globalEventService, this.jobVisualisationVerticalSpacing
                , this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);
            jobInstanceVisualisationDialog.createSchedulerVisualisation(this.rootContextInstance, child);
            jobInstanceVisualisationDialog.open();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextSelected(String contextName) {
        logger.info("contextName " + contextName);
    }
}
