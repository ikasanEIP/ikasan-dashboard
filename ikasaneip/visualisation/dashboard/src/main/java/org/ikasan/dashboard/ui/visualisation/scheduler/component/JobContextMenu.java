package org.ikasan.dashboard.ui.visualisation.scheduler.component;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;


public class JobContextMenu extends Dialog {
    private VerticalLayout layout = new VerticalLayout();

    private ModuleMetaDataService moduleMetaDataService;
    private SchedulerJobService schedulerJobService;
    private ContextInstance rootContextInstance;

    public JobContextMenu(int x, int y, SchedulerJob schedulerJob, SystemEventLogger systemEventLogger,
                          ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                          SchedulerJobService schedulerJobService, ContextInstance rootContextInstance) {
        this.setWidth("200px");
//        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
//        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
//        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", x + "px");
//        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", y + "px");

        this.moduleMetaDataService = moduleMetaDataService;
        this.schedulerJobService = schedulerJobService;
        this.rootContextInstance = rootContextInstance;

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
            SchedulerJobLogFileViewerDialog dialog = new SchedulerJobLogFileViewerDialog();
            dialog.open();
            this.close();
        });
        this.addItem("View Error Log", event -> {
            SchedulerJobLogFileViewerDialog dialog = new SchedulerJobLogFileViewerDialog();
            dialog.open();
            this.close();
        });

        this.add(layout);
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
        SchedulerJobRecord schedulerJobRecord =  this.schedulerJobService.findByContextIdAndJobName(this.rootContextInstance.getName(), jobName);

        if(schedulerJobRecord != null) {
            return schedulerJobRecord.getJob();
        }
        else {
            return null;
        }
    }
}
