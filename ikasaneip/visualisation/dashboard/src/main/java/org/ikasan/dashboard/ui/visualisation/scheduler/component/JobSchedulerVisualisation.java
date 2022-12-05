package org.ikasan.dashboard.ui.visualisation.scheduler.component;

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
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JobSchedulerVisualisation extends SchedulerVisualisation {

    public JobSchedulerVisualisation(String dynamicImagePath, ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                                     ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                                     SystemEventLogger systemEventLogger, SchedulerJobService schedulerJobService, LogStreamingService logStreamingService, JobInitiationService jobInitiationService,
                                     ContextProfileService contextProfileService, UserService userService, SecurityService securityService, JobProvisionService jobProvisionService,
                                     ScheduledContextService scheduledContextService, Map<String, String> schedulerJobExecutionEnvironmentLabel) {
        super(dynamicImagePath, moduleMetaDataService, scheduledProcessManagementService, configurationRestService, moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService
            , logStreamingService, jobInitiationService, contextProfileService, userService, securityService, jobProvisionService, scheduledContextService, schedulerJobExecutionEnvironmentLabel);
    }

    protected void init() throws IOException {
        if(!initialised && contextTemplate != null) {

            if (this.designerCanvas != null) {
                this.removeAll();
            }

            this.designerCanvas = new DesignerCanvas(this, null, "canvas-viewport-"+ UUID.randomUUID().toString(), this.dynamicImagePath, !this.edit);
            this.designerCanvas.addCanvasInitialisedListener(this);

            if(contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
                if(this.scheduledContextViewRecord == null) {
                    SearchResults<SchedulerJobRecord> jobs = this.schedulerJobService.findByContext(parentContextTemplate.getName(), -1, -1);

                    Map<String, SchedulerJob> schedulerJobs = jobs.getResultList().stream()
                        .map(record -> record.getJob())
                        .collect(Collectors.toMap(SchedulerJob::getJobName, Function.identity()));

                    this.designerCanvas.setCanvasJson(adapter.adaptJobs(contextTemplate, schedulerJobs));
                }
                else {
                    this.designerCanvas.setCanvasJson(this.scheduledContextViewRecord.getContextView());
                }
            }

            this.designerCanvas.addCanvasItemDoubleClickEventListener(this);
            this.designerCanvas.addCanvasItemRightClickEventListener(this);
            this.designerCanvas.addConnectorEventListener(this);
            this.designerCanvas.addCanvasUpdatedListener(this);
            this.designerCanvas.addFigureDeleteEventListeners(this);
            this.designerCanvas.addFigureUndoDeleteEventListeners(this);
            this.designerCanvas.addCanvasItemSingleClickEventListener(this);

            this.add(initCanvasActions(), designerCanvas);

            this.initialised = true;
        }
    }
}
