package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import com.vaadin.flow.component.UI;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.security.service.SecurityService;
import org.ikasan.security.service.UserService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class ContextSchedulerVisualisation extends SchedulerVisualisation {

    /**
     * Constructs a ContextSchedulerVisualisation object with the provided parameters.
     *
     * @param dynamicImagePath the dynamic image path
     * @param moduleMetaDataService service for module metadata
     * @param scheduledProcessManagementService service for scheduled process management
     * @param configurationRestService configuration REST service
     * @param moduleControlRestService module control REST service
     * @param metaDataRestService metadata REST service
     * @param systemEventLogger logger for system events
     * @param schedulerJobService job scheduler service
     * @param logStreamingService streaming service for log data
     * @param jobInitiationService service for job initiation
     * @param contextProfileService context profile service
     * @param userService user service
     * @param securityService security service
     * @param jobProvisionService job provision service
     * @param scheduledContextService service for scheduled contexts
     * @param schedulerJobExecutionEnvironmentLabel map of scheduler job execution environment labels
     * @param jobVisualisationVerticalSpacing vertical spacing for job visualisation
     * @param jobVisualisationHorizontalSpacing horizontal spacing for job visualisation
     * @param contextVisualisationLevelDistance distance between context visualisation levels
     * @param contextVisualisationNodeDistance distance between context visualisation nodes
     */
    public ContextSchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService
        , ScheduledProcessManagementService scheduledProcessManagementService, ConfigurationService configurationRestService
        , ModuleControlService moduleControlRestService, MetaDataService metaDataRestService, SystemEventLogger systemEventLogger
        , SchedulerJobService schedulerJobService, LogStreamingService logStreamingService, JobInitiationService jobInitiationService
        , ContextProfileService contextProfileService, UserService userService, SecurityService securityService
        , JobProvisionService jobProvisionService, ScheduledContextService scheduledContextService
        , Map<String, String> schedulerJobExecutionEnvironmentLabel, double jobVisualisationVerticalSpacing, double jobVisualisationHorizontalSpacing
        , double contextVisualisationLevelDistance, double contextVisualisationNodeDistance, boolean showPrettyFormattedDiagram) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService
            , moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService
            , jobInitiationService, contextProfileService, userService, securityService, jobProvisionService, scheduledContextService
            , schedulerJobExecutionEnvironmentLabel, jobVisualisationVerticalSpacing
            , jobVisualisationHorizontalSpacing, contextVisualisationLevelDistance, contextVisualisationNodeDistance
            , showPrettyFormattedDiagram);
    }

    /**
     * Initializes the DesignerCanvas with the specified UI object.
     *
     * @param ui the UI object used for initialization
     * @throws IOException if an I/O error occurs
     */
    protected void init(UI ui) throws IOException {
        if(!initialised && contextTemplate != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas(this, null, "canvas-viewport-"+ UUID.randomUUID().toString()
                , this.dynamicImagePath, !this.edit, ui, true);
            this.designerCanvas.addCanvasInitialisedListener(this);

            this.designerCanvas.setCanvasJson(adapter.adaptContext(contextTemplate));

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addConnectorEventListener(this);
            this.designerCanvas.addCanvasUpdatedListener(this);
            this.designerCanvas.addFigureDeleteEventListeners(this);
            this.designerCanvas.addFigureUndoDeleteEventListeners(this);

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }
}
