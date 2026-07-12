package org.ikasan.dashboard.ui.visualisation.scheduler.dag.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PreserveOnRefresh;
import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.dashboard.ui.visualisation.scheduler.component.JobInstanceVisualisationDialog;
import org.ikasan.job.orchestration.broadcast.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NpmPackage(value = "@ebay/nice-dag-core", version = "1.0.34")
@NpmPackage(value = "lit-fontawesome", version = "0.1.3")
@CssImport(value = "./css/font.css")
@CssImport(value = "./css/ikasan-dag.css")
@JsModule("./dag-connector-flow.js")
@Tag("dag-chart")
@PreserveOnRefresh
public class DagComponent extends VerticalLayout implements HasSize, ContextInstanceStateChangeEventLocalBroadcastListener {

    Logger logger = LoggerFactory.getLogger(DagComponent.class);

    private ModuleMetaDataService moduleMetaDataService;
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
    private GlobalEventService globalEventService;
    private ContextInstance parentContextInstance;

    private UI ui;

    private double jobVisualisationVerticalSpacing;
    private double jobVisualisationHorizontalSpacing;
    private double contextVisualisationLevelDistance;
    private double contextVisualisationNodeDistance;
    private String dagJson;
    private double scale = 1;

    private String ikasanDagNodeStyle = "width: 100%; height: 100%; border: 1px solid #8799c1; " +
        "position: relative; border-radius: 10px; display: flex;flex-direction: column;title: 'test hover';";

    public DagComponent(String dagData, ModuleMetaDataService moduleMetaDataService,
                        ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                        SystemEventLogger systemEventLogger, LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService,
                        JobInitiationService jobInitiationService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService,
                        ScheduledContextInstanceService scheduledContextInstanceService, ContextProfileService contextProfileService, GlobalEventService globalEventService,
                        double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing, double contextVisualisationLevelDistance, double contextVisualisationNodeDistance,
                        ContextInstance parentContextInstance) {
        this.dagJson = dagData;
        if(this.dagJson == null) {
            throw new IllegalArgumentException("dagJson cannot be null!");
        }

        this.moduleMetaDataService = moduleMetaDataService;
        if(this.moduleMetaDataService == null) {
            throw new IllegalArgumentException("agent cannot be null!");
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

        this.parentContextInstance = parentContextInstance;
        if(this.parentContextInstance == null) {
            throw new IllegalArgumentException("parentContextInstance cannot be null!");
        }

        this.jobVisualisationVerticalSpacing = jobVisualisationVerticalSpacing;
        this.jobVisualisationHorizontalSpacing = jobVisualisationHorizontalSpacing;
        this.contextVisualisationLevelDistance = contextVisualisationLevelDistance;
        this.contextVisualisationNodeDistance = contextVisualisationNodeDistance;
        this.setWidth("100%");
        this.setHeight("100%");
        this.getStyle().set("display", "block");
        this.getStyle().set("overflow","auto");
    }

    /**
     * This method works in combination with dag-connector-flow.js to set up the
     * integration between the Vaadin framework and nice-dag javascript.
     */
    public void initConnector() {
        getElement().setProperty("dagNodes", this.dagJson);
        getElement().setProperty("ikasanDagNodeStyle", this.ikasanDagNodeStyle);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        initConnector();

        this.ui = attachEvent.getUI();

        ContextInstanceStateChangeEventBroadcaster.register(this);
    }

    public void styleNode(String nodeId, String colour) {
        this.getElement().callJsFunction("styleNode", nodeId, colour);
    }

    public void zoom(double scale) {
        this.getElement().callJsFunction("zoom", scale);
    }

    @ClientCallable
    public void setDag(String dag) {
        logger.info("Received data: " + dag);
        this.dagJson = dag;
    }

    @ClientCallable
    public String getStatusColour(String contextName) {
        ContextInstance contextInstance = ContextHelper.getChildContextInstance(contextName, this.parentContextInstance);
        if(contextInstance != null) {
            return this.getStatusColour(contextInstance.getStatus());
        }

        return IkasanColours.SCHEDULER_WAITING;
    }

    @ClientCallable
    public void openDiagram(String contextId) {
        try {
            this.refreshContextInstance();
            ContextInstance child = ContextHelper.getChildContextInstance(contextId, this.parentContextInstance);
            JobInstanceVisualisationDialog jobInstanceVisualisationDialog = new JobInstanceVisualisationDialog(moduleMetaDataService,
                configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, logStreamingService
                , this.schedulerJobInstanceService, this.jobInitiationService, this.jobUtilsService, this.scheduledContextService
                , this.scheduledContextInstanceService, contextProfileService, globalEventService, this.jobVisualisationVerticalSpacing
                , this.jobVisualisationHorizontalSpacing, this.contextVisualisationLevelDistance, this.contextVisualisationNodeDistance);
            jobInstanceVisualisationDialog.createSchedulerVisualisation(parentContextInstance, child);
            jobInstanceVisualisationDialog.open();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;

        ContextInstanceStateChangeEventBroadcaster.unregister(this);
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        this.refreshContextInstance();
        ContextInstance child = ContextHelper.getChildContextInstance(event.getContextInstance().getName()
            , this.parentContextInstance);
        child.setStatus(event.getNewStatus());
        this.styleNode(event.getContextInstance().getName()
            , this.getStatusColour(event.getNewStatus()));
    }

    private void refreshContextInstance() {
        ContextMachine machine = ContextMachineCache.instance().getByContextInstanceId(this.parentContextInstance.getId());
        if(machine != null) {
            ContextInstance refreshed = machine.getContext();
            if(refreshed != null) this.parentContextInstance = refreshed;
        }
    }

    private String getStatusColour(InstanceStatus status) {
        if(status == null || status.equals(InstanceStatus.WAITING)) {
            return IkasanColours.SCHEDULER_WAITING;
        }
        else if(status.equals(InstanceStatus.RUNNING)) {
            return IkasanColours.SCHEDULER_RUNNING;
        }
        else if(status.equals(InstanceStatus.COMPLETE)) {
            return IkasanColours.SCHEDULER_COMPLETE;
        }
        else if(status.equals(InstanceStatus.ERROR)) {
            return IkasanColours.SCHEDULER_ERROR;
        }

        return IkasanColours.SCHEDULER_WAITING;
    }
}
