package org.ikasan.dashboard.ui.visualisation.scheduler.component;

import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.designer.DesignerCanvas;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContextSchedulerInstanceVisualisation extends SchedulerInstanceVisualisation{

    public ContextSchedulerInstanceVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService, SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService, LogStreamingService logStreamingService, SchedulerJobInstanceService schedulerJobInstanceService, JobInitiationService jobInitiationService, JobUtilsService jobUtilsService, ScheduledContextService scheduledContextService) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService, logStreamingService, schedulerJobInstanceService, jobInitiationService, jobUtilsService, scheduledContextService);
    }

    protected void init() throws IOException {
        if(!initialised && contextInstance != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas("canvas-viewport-"+ UUID.randomUUID().toString(), this.dynamicImagePath, true);

            if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
                this.designerCanvas.setCanvasJson(adapter.adaptContext(contextInstance));
            }

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addCanvasInitialisedListener(this);

            this.designerCanvas.manageClickableItems();

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }
}
