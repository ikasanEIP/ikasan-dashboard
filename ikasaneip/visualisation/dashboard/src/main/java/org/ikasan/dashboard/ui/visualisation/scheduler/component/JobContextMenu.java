package org.ikasan.dashboard.ui.visualisation.scheduler.component;


import java.util.List;

import org.ikasan.dashboard.ui.general.component.NotificationHelper;
import org.ikasan.dashboard.ui.scheduler.component.EditMode;
import org.ikasan.dashboard.ui.scheduler.component.FileEventJobDialog;
import org.ikasan.dashboard.ui.scheduler.component.InternalEventDrivenJobDialog;
import org.ikasan.dashboard.ui.scheduler.component.QuartzDrivenScheduledJobDialog;
import org.ikasan.dashboard.ui.util.SystemEventLogger;
import org.ikasan.scheduled.event.service.ScheduledProcessManagementService;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.LogStreamingService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;


public class JobContextMenu extends Dialog {
    private static final Logger LOG = LoggerFactory.getLogger(JobContextMenu.class);

    private VerticalLayout layout = new VerticalLayout();

    private ModuleMetaDataService moduleMetaDataService;
    private SchedulerJobService schedulerJobService;
    private ContextInstance rootContextInstance;
    private LogStreamingService logStreamingService;
    private ContextInstance currentInstance;

    public JobContextMenu(SchedulerJob schedulerJob, SystemEventLogger systemEventLogger,
                          ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                          SchedulerJobService schedulerJobService, ContextInstance rootContextInstance, ContextInstance currentInstance,
                          LogStreamingService logStreamingService) {

        this.setWidth("200px");
//        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
//        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
//        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", x + "px");
//        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", y + "px");

        this.moduleMetaDataService = moduleMetaDataService;
        this.schedulerJobService = schedulerJobService;
        this.rootContextInstance = rootContextInstance;
        this.currentInstance = currentInstance;
        this.logStreamingService = logStreamingService;

        layout.setWidthFull();

        super.setCloseOnOutsideClick(true);
        super.setCloseOnEsc(true);
        super.setDraggable(false);

        this.addItem("View Job", event -> {
            SchedulerJob job = this.getSchedulerJob(schedulerJob.getJobName());
            if(job == null) {
                NotificationHelper.showErrorNotification("Could not locate job to open");
            }
            else if(job instanceof InternalEventDrivenJob) {
                InternalEventDrivenJobDialog internalEventDrivenJobDialog
                    = new InternalEventDrivenJobDialog(null, scheduledProcessManagementService, configurationRestService,
                    moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                internalEventDrivenJobDialog.setJob((InternalEventDrivenJob)job, EditMode.READONLY);
                internalEventDrivenJobDialog.open();
            }
            else if(job instanceof FileEventDrivenJob) {
                FileEventJobDialog fileEventJobDialog
                    = new FileEventJobDialog(null, scheduledProcessManagementService, configurationRestService,
                    moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                fileEventJobDialog.setJob((FileEventDrivenJob) job, EditMode.READONLY);
                fileEventJobDialog.open();
            }
            else if(job instanceof QuartzScheduleDrivenJob) {
                QuartzDrivenScheduledJobDialog quartzDrivenScheduledJobDialog
                    = new QuartzDrivenScheduledJobDialog(null, scheduledProcessManagementService, configurationRestService,
                    moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                quartzDrivenScheduledJobDialog.setJob((QuartzScheduleDrivenJob) job, EditMode.READONLY);
                quartzDrivenScheduledJobDialog.open();
            }
            else {
                NotificationHelper.showErrorNotification("Unknown job type - " + job.getClass().getName());
            }

            this.close();
        });
        this.addItem("Edit Job", event -> {
            NotificationHelper.showUserNotification("Not yet implemented!");
            this.close();
        });
        this.addItem("Execute Job", event -> {
            NotificationHelper.showUserNotification("Not yet implemented!");
            this.close();
        });
        this.addItem("Hold Job", event -> {
            NotificationHelper.showUserNotification("Not yet implemented!");
            this.close();
        });
        this.addItem("Release Job", event -> {
            NotificationHelper.showUserNotification("Not yet implemented!");
            this.close();
        });
        this.addItem("Skip Job", event -> {
            NotificationHelper.showUserNotification("Not yet implemented!");
            this.close();
        });
        this.addItem("View Output Log", event -> {
            streamLog(schedulerJob, false);
        });
        this.addItem("View Error Log", event -> {
            streamLog(schedulerJob, true);
        });

        this.add(layout);
    }

    private void streamLog(SchedulerJob schedulerJob, boolean getErrorLog) {
        // TODO remove all the log info when happy this is working
        List<InstanceStatus> allowedStatuses = List.of(InstanceStatus.COMPLETE, InstanceStatus.RUNNING, InstanceStatus.ERROR);
        InstanceStatus status = this.currentInstance.getStatus();
        LOG.info("Current Instance status: " + status);
        if (allowedStatuses.contains(status)) {
            SchedulerJobInstance schedulerJobInstance = this.currentInstance.getScheduledJobsMap().get(schedulerJob.getIdentifier());
            LOG.info("schedulerJobInstance is " + schedulerJobInstance + " for job identifier " + schedulerJob.getIdentifier());
            ModuleMetaData agent = this.getAgent(schedulerJob.getAgentName());
            LOG.info("agent is " + agent + " for name " + schedulerJob.getAgentName());
            if (schedulerJobInstance != null && schedulerJobInstance.getScheduledProcessEvent() != null && agent != null) {
                ScheduledProcessEvent scheduledProcessEvent = schedulerJobInstance.getScheduledProcessEvent();
                String host = agent.getUrl();
                String endPoint = "/rest/logs";
                String outputLog = getErrorLog ? scheduledProcessEvent.getResultError() : scheduledProcessEvent.getResultOutput();

                LOG.info(String.format("Streaming lof for host %s, endPoint %s, log %s", host, endPoint, outputLog));

                SchedulerJobLogFileViewerDialog dialog = new SchedulerJobLogFileViewerDialog(this.logStreamingService, host, endPoint, outputLog);
                dialog.open();
                this.close();
            }
        } else {
            String message = "There currently is no " + (getErrorLog ? "error" : "output") + " log for the job";
            NotificationHelper.showUserNotification(message);
            this.close();
        }
    }

    public void addItem(String label, ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = new Button(label);
        button.addClickListener(listener);

        this.layout.add(button);
    }

    private ModuleMetaData getAgent(String agentName) {
        return this.moduleMetaDataService.findById(agentName);
    }

    private SchedulerJob getSchedulerJob(String jobName) {
        SchedulerJobRecord schedulerJobRecord = this.schedulerJobService.findByContextIdAndJobName(this.rootContextInstance.getName(), jobName);

        if (schedulerJobRecord != null) {
            return schedulerJobRecord.getJob();
        } else {
            return null;
        }
    }
}
