package org.ikasan.dashboard.ui.visualisation.scheduler.component;


import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
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
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;


public class JobContextMenu extends Dialog {
    private VerticalLayout layout = new VerticalLayout();

    private ModuleMetaDataService moduleMetaDataService;
    private SchedulerJobService schedulerJobService;

    public JobContextMenu(int x, int y, SchedulerJob schedulerJob, SystemEventLogger systemEventLogger,
                          ModuleMetaDataService moduleMetaDataService, ScheduledProcessManagementService scheduledProcessManagementService,
                          ConfigurationService configurationRestService, ModuleControlService moduleControlRestService, MetaDataService metaDataRestService,
                          SchedulerJobService schedulerJobService) {
        this.setWidth("200px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "align-self", "flex-start");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "position", "absolute");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "left", x + "px");
        this.getElement().executeJs("this.$.overlay.$.overlay.style[$0]=$1", "top", y + "px");

        this.moduleMetaDataService = moduleMetaDataService;
        this.schedulerJobService = schedulerJobService;

        layout.setWidthFull();

        super.setCloseOnOutsideClick(true);
        super.setCloseOnEsc(true);
        super.setDraggable(false);

        this.addItem("View Job", event -> {
            SchedulerJob job = this.getSchedulerJob(schedulerJob.getContextId(), schedulerJob.getJobName());
            if(job == null) {
                NotificationHelper.showErrorNotification("Could not locate job to open");
            }
            else if(job instanceof InternalEventDrivenJob) {
                InternalEventDrivenJobDialog internalEventDrivenJobDialog
                    = new InternalEventDrivenJobDialog(null, scheduledProcessManagementService, configurationRestService,
                    moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                internalEventDrivenJobDialog.setJob((InternalEventDrivenJob)schedulerJob, EditMode.READONLY);
            }
            else if(job instanceof FileEventDrivenJob) {
                FileEventJobDialog internalEventDrivenJobDialog
                    = new FileEventJobDialog(null, scheduledProcessManagementService, configurationRestService,
                    moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                internalEventDrivenJobDialog.setJob((FileEventDrivenJob) schedulerJob, EditMode.READONLY);
            }
            else if(job instanceof QuartzScheduleDrivenJob) {
                QuartzDrivenScheduledJobDialog internalEventDrivenJobDialog
                    = new QuartzDrivenScheduledJobDialog(null, scheduledProcessManagementService, configurationRestService,
                    moduleControlRestService, metaDataRestService, systemEventLogger, schedulerJobService);
                internalEventDrivenJobDialog.setJob((QuartzScheduleDrivenJob) schedulerJob, EditMode.READONLY);
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
            NotificationHelper.showUserNotification("Not yet implemented!");
            this.close();
        });
        this.addItem("View Error Log", event -> {
            NotificationHelper.showUserNotification("Not yet implemented!");
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

    private SchedulerJob getSchedulerJob(String contextId, String jobName) {
        SchedulerJobRecord schedulerJobRecord =  this.schedulerJobService.findByContextIdAndJobName(contextId, jobName);

        if(schedulerJobRecord != null) {
            return schedulerJobRecord.getJob();
        }
        else {
            return null;
        }
    }
}
